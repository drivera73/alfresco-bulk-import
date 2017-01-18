/**
 *  This file is part of Alfresco Bulk Export Tool.
 * 
 *  Alfresco Bulk Export Tool is free software: you can redistribute it 
 *  and/or modify it under the terms of the GNU General Public License as
 *  published by the Free Software Foundation, either version 3 of the 
 *  License, or (at your option) any later version.
 *
 *  Alfresco Bulk Export Tool  is distributed in the hope that it will be 
 *  useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General 
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along 
 *  with Alfresco Bulk Export Tool. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.extension.bulkexport.controller;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.ArrayList;
import java.util.Collections;
import java.io.*;

import org.alfresco.extension.bulkexport.BulkExportStatus;
import org.alfresco.extension.bulkexport.BulkExportWorkerThread;
import org.alfresco.extension.bulkexport.WriteableBulkExportStatus;
import org.alfresco.extension.bulkexport.dao.AlfrescoExportDao;
import org.alfresco.extension.bulkexport.dao.NodeRefRevision;
import org.alfresco.extension.bulkexport.model.FileFolder;
import org.alfresco.extension.bulkexport.model.FileFolder.FileFolderStatus;
import org.alfresco.model.ApplicationModel;
import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tika.io.IOUtils;

import com.google.common.collect.Lists;


/**
 * This classe is a engine of systems
 * 
 * @author Denys G. Santos (gsdenys@gmail.com)
 * @version 1.0.1
 */
public class Engine 
{
    Log log = LogFactory.getLog(Engine.class);

    public static final String CALIENTE_MODEL_URL = "http://www.armedia.com/model/armedia/1.0";
    public static final String CALIENTE_MODEL_PREFIX = "arm";
	public static final QName ASPECT_CALIENTE_VDOCREFERENCE = QName.createQName(CALIENTE_MODEL_URL, "vdocReference");
	public static final QName PROP_CALIENTE_VDOCREFERENCEID = QName.createQName(CALIENTE_MODEL_URL, "vdocReferenceId");
	
    /** Data Access Object */
    private AlfrescoExportDao dao;
    
    /** File and folder manager */
    private FileFolder fileFolder;

    private boolean exportVersions;

    /** If true the the head revision will be named, eg. if head revision is 1.4 then filename will contain the revision. 
     * This behaviour is not how the bulk importer expects revisions */
    private boolean revisionHead;

    /** if true the look for a cache containing a list of all nodes to export
     * */
    private boolean useNodeCache;
    
    private boolean includeContent;
    
    private List<QName> documentProperties = new ArrayList<QName>();
    private List<QName> folderProperties = new ArrayList<QName>();
    private boolean foldersOnly = false;
    private String checksum = null;
    private WriteableBulkExportStatus bulkExportStatus = null;
    
    private List<NodeRef> folderNodes = new ArrayList<NodeRef>();
    private List<NodeRef> documentNodes = new ArrayList<NodeRef>();
    
    /**
     * Engine Default Builder
     * 
     * @param dao Data Access Object
     * @param fileFolder File and Folder magager
     */
    public Engine(AlfrescoExportDao dao, FileFolder fileFolder, boolean exportVersions, boolean revisionHead, boolean useNodeCache, boolean includeContent, List<String> documentProperties, List<String> folderProperties, boolean foldersOnly, String checksum)
    {
        this.dao =  dao;
        this.fileFolder = fileFolder;
        this.exportVersions = exportVersions;
        this.revisionHead = revisionHead;
        this.useNodeCache = useNodeCache;
        this.includeContent = includeContent;
        this.foldersOnly = foldersOnly;
        if (!documentProperties.isEmpty()) {
        	for (String s : documentProperties) {
        		this.documentProperties.add(QName.createQName(s));
        	}
        	this.documentProperties.add(ContentModel.PROP_NAME);
        }
        if (!folderProperties.isEmpty()) {
        	for (String s : folderProperties) {
        		this.folderProperties.add(QName.createQName(s));
        	}
        	this.folderProperties.add(ContentModel.PROP_NAME);
        }
        this.checksum = checksum;
    }

    public Engine(AlfrescoExportDao dao, FileFolder fileFolder, boolean exportVersions, boolean revisionHead, boolean useNodeCache, boolean includeContent, List<String> documentProperties, List<String> folderProperties, boolean foldersOnly, String checksum, WriteableBulkExportStatus bulkExportStatus)
    {
    	log.debug("Setting up Engine");
        this.dao =  dao;
        this.fileFolder = fileFolder;
        this.exportVersions = exportVersions;
        this.revisionHead = revisionHead;
        this.useNodeCache = useNodeCache;
        this.includeContent = includeContent;
        this.foldersOnly = foldersOnly;
        
        log.debug("Setup variables");
        if (!documentProperties.isEmpty()) {
        	log.debug("Document properties are not null");
        	for (String s : documentProperties) {
        		this.documentProperties.add(QName.createQName(s));
        	}
        	this.documentProperties.add(ContentModel.PROP_NAME);
        }
        if (!folderProperties.isEmpty()) {
        	log.debug("Folder properties are not null");
        	for (String s : folderProperties) {
        		this.folderProperties.add(QName.createQName(s));
        	}
        	this.folderProperties.add(ContentModel.PROP_NAME);
        }
        this.checksum = checksum;
        this.bulkExportStatus = bulkExportStatus;
        
        log.debug("Engine created");
    }
    
    /**
     * Recursive method to export alfresco nodes to file system 
     * 
     * @param nodeRef
     */
    public List<NodeRef> execute(NodeRef nodeRef) throws Exception 
    {    
        // case node is folder create a folder and execute recursively 
        // other else create file 
        log.debug("execute (noderef)");
        
        this.dao.setExportNodeRef(nodeRef);
        this.dao.setFolderProperties(folderProperties);
        if(!this.dao.isNodeIgnored(nodeRef.toString()))
        {    
            log.info("Find all nodes to export (no history)");
            List<NodeRef> allNodes = getNodesToExport(nodeRef);
            log.info("Folder Nodes to export = " + folderNodes.size());
            exportNodes(folderNodes);

            //exportNodes(documentNodes);
        }
        log.debug("execute (noderef) finished");
        
        return documentNodes;
    }
    
    public void execute(List<NodeRef> nodeRefs, NodeRef nodeRef) throws Exception {
    	log.debug("Batch size of: " + nodeRefs.size() + " to export");
    	this.dao.setFolderProperties(folderProperties);
    	this.dao.setExportNodeRef(nodeRef);
    	exportNodes(nodeRefs);
    	bulkExportStatus.incrementTargetCounter(BulkExportStatus.TARGET_COUNTER_DOCUMENT_BATCHES_COMPLETE);
    }
    
    private List<NodeRef> getNodesToExport(NodeRef rootNode) throws Exception 
    {
        List<NodeRef> nodes = null;
        if (useNodeCache)
        {
            nodes = retrieveNodeListFromCache(rootNode);
        }

        if (nodes == null)
        {
            nodes = findAllNodes(rootNode);
            storeNodeListToCache(rootNode, nodes);
            if (useNodeCache)
            {
                log.info("Generated Cached Node list");
                throw new CacheGeneratedException("Generated Cached Node List Only");
            }
        }
        else
        {
            log.info("Using Cached Node list");
        }

        return nodes;
    }

    private String nodeFileName(NodeRef rootNode)
    {
        File fname = new File(fileFolder.basePath(), rootNode.getId() + ".cache");
        return fname.getPath();
    }

    private void storeNodeListToCache(NodeRef rootNode, List<NodeRef> list) throws Exception 
    {
        // get a better name
        FileOutputStream fos= new FileOutputStream(nodeFileName(rootNode));
        ObjectOutputStream oos= new ObjectOutputStream(fos);
        oos.writeObject(list);
        oos.close();
        fos.close();
    }

    private List<NodeRef> retrieveNodeListFromCache(NodeRef rootNode) throws Exception 
    {
        try
        {
            FileInputStream fis = new FileInputStream(nodeFileName(rootNode));
        	ObjectInputStream ois = null;
            try
            {
            	ois = new ObjectInputStream(fis);
                @SuppressWarnings("unchecked")
				List<NodeRef> list = (List<NodeRef>) ois.readObject();
                return list;
            }
            finally
            {
            	IOUtils.closeQuietly(ois);
            	IOUtils.closeQuietly(fis);
            }
        }
        catch (FileNotFoundException e)
        {
            // this exception means we have no noelist cache - we just ignore and continue
            log.debug("could not open nodelist cache file");
        }
        return null;
    }

    /**
     * Recursive find of all item head nodes from a given node ref
     * 
     * @param nodeRef
     */
    private List<NodeRef> findAllNodes(NodeRef nodeRef) throws Exception 
    {    
        List<NodeRef> nodes = new ArrayList<NodeRef>();

        log.debug("findAllNodes (noderef)");
        
        if(!this.dao.isNodeIgnored(nodeRef.toString()))
        {    
        	if (bulkExportStatus != null) {
            	bulkExportStatus.incrementTargetCounter(BulkExportStatus.TARGET_COUNTER_TOTAL_NODES_SUBMITTED);
            }
            if(this.dao.isFolder(nodeRef))
            {
            	if (bulkExportStatus != null) {
                	bulkExportStatus.incrementTargetCounter(BulkExportStatus.TARGET_COUNTER_TOTAL_FOLDERS_SUBMITTED);
                }
                nodes.add(nodeRef); // add folder as well
                folderNodes.add(nodeRef);
                List<NodeRef> children= this.dao.getChildren(nodeRef);
                for (NodeRef child : children) 
                {            
                    nodes.addAll(this.findAllNodes(child));
                }
            } 
            else 
            {
            	if (bulkExportStatus != null) {
                	bulkExportStatus.incrementTargetCounter(BulkExportStatus.TARGET_COUNTER_TOTAL_DOCUMENTS_SUBMITTED);
                }
                nodes.add(nodeRef);
                documentNodes.add(nodeRef);
            }
        }     

        log.debug("findAllNodes (noderef) finished");
        return nodes;
    }

    private void exportHeadRevision(NodeRef nodeRef) throws Exception
    {
        this.createFile(nodeRef);
    }

    private void exportFullRevisionHistory(NodeRef nodeRef) throws Exception
    {
        Map<String,NodeRefRevision> nodes = this.dao.getNodeRefHistory(nodeRef.toString());
        if (nodes != null)
        {
            List<String> sortedKeys = new ArrayList<>(nodes.keySet());

            Collections.sort(sortedKeys, new VersionNumberComparator());
            if (sortedKeys.size() < 1)
            {
                throw new Exception("no revisions available");
            }

            String headRevision = (String)sortedKeys.get(sortedKeys.size()-1);

            for (String revision : nodes.keySet()) 
            {
                NodeRefRevision nodeRevision = nodes.get(revision);
                this.createFile(nodeRef, nodeRevision.node, revision, headRevision == revision);
            }
            bulkExportStatus.incrementTargetCounter(BulkExportStatus.TARGET_COUNTER_TOTAL_DOCUMENTS_VERSION_SUBMITTED, nodes.size() - 1);
        }
        else
        {
            // no revision history so lets just create the most recent revision
            log.debug("execute (noderef) no revision history found, dump node as head revision");
            this.createFile(nodeRef, nodeRef, "1.0", true);
        }
    }

    /**
     * Iterate over nodes to export, and do appropriate action
     * 
     * @param nodesToExport
     */
    private void exportNodes(List<NodeRef> nodesToExport) throws Exception 
    {
        final int NODES_TO_PROCESS = 100;

        //int logCount = nodesToExport.size();

        for (NodeRef nodeRef : nodesToExport) 
        {
            //logCount--;
            if(this.dao.isFolder(nodeRef))
            {
                this.createFolder(nodeRef);
            } 
            else
            {
            	if (!foldersOnly) {
	                if (exportVersions)
	                {
	                	log.debug("Export all the versions");
	                    exportFullRevisionHistory(nodeRef);
	                }
	                else
	                {
	                	log.debug("Export head node only");
	                    exportHeadRevision(nodeRef);
	                }
            	}
            }

            //if (logCount % NODES_TO_PROCESS == 0)
            //{
            //    log.info("Remaining Parent Nodes to process " + logCount);
            //}
        }
    }
    
    
    /**
     * Create file (Document and Bulk XML Meta data)
     * 
     * @param file 
     * @throws Exception
     */
    private void createFile(NodeRef headNode, NodeRef file, String revision, boolean isHeadRevision) throws Exception 
    {
        String path = null;
        if (revision == null)
        {
            log.error("createFile (headNode: "+headNode.toString() + " , filenode: )"+file.toString()+" , revision: " + revision + ")");
            throw new Exception("revision for node was not found");
        }

        path = this.dao.getPath(this.dao.getParentNodeRef(headNode));
        Serializable name = null;
        if (documentProperties.isEmpty()) {
        	name = this.dao.getProperty(headNode, ContentModel.PROP_NAME);
        } else {
        	List<QName> newDocumentProperties = new ArrayList<QName>();
        	if (this.dao.hasAspect(headNode, ASPECT_CALIENTE_VDOCREFERENCE)) {
        		newDocumentProperties.add(PROP_CALIENTE_VDOCREFERENCEID);
    		}
        	newDocumentProperties.addAll(documentProperties);
			// loop through potential properties
			for (QName property : newDocumentProperties) {
	    		if (this.dao.hasProperty(headNode, property)) {
		    		try {
						name =  this.dao.getProperty(headNode, property);
						if (name == null || "".equals(name)) {
							continue;
						}
						break;
					} catch (Exception e) {
						log.error("Could not retrieve property: " + property);
					}
	    		}
			}
        }
        if (!revisionHead && isHeadRevision) {
        	path = path + "/" + name;
        	revision = null;
        } else {
        	// trim the path to make this OOTB compatible - needs to be made a flag
        	if (revision.contains(".")) {
        		revision = revision.substring(0, revision.indexOf("."));
        	}
        	path = path + "/" + name + ".v" + revision;
        }

        doCreateFile(file, path, revision);
    }

    private void createFile(NodeRef file) throws Exception 
    {
        String path = null;
        path = this.dao.getPath(file);
        doCreateFile(file, path, null);
    }

    private void doCreateFile(NodeRef file, String path, String revision) throws Exception 
    {
        //get Informations
        log.debug("doCreateFile (noderef)");

        // need these variables out of the try scope for debugging purposes when the exception is thrown
        String type = null;
        List<String> aspects = null;
        Map<String, String> properties = null;
        
        FileFolderStatus ok = FileFolderStatus.UNKNOWN;
        try
        {
        	type = this.dao.getType(file);
        	if (!type.equals("arm:reference")) {
	            String fname = this.fileFolder.createFullPath(path);
	            log.debug("doCreateFile file =" + fname);
	            if (includeContent) {
		            if (this.dao.getContentAndStoreInFile(file, fname) == false)
		            {
		                log.debug("doCreateFile ignore this file");
		                if (revision == null) {
		                	FileFolder.updateDocumentCounters(FileFolderStatus.SKIPPED, bulkExportStatus);
		                } else {
		                	FileFolder.updateDocumentVersionCounters(FileFolderStatus.SKIPPED, bulkExportStatus);
		                }
		                return;
		            } else {
		            	if (revision == null) {
		            		FileFolder.updateDocumentCounters(FileFolderStatus.OK, bulkExportStatus);
		            	} else {
		                	FileFolder.updateDocumentVersionCounters(FileFolderStatus.OK, bulkExportStatus);
		                }
		            }
	            }
	            
	            aspects = this.dao.getAspectsAsString(file);
	            properties = this.dao.getPropertiesAsString(file);
	            
	            //Create Files
	            log.debug("TYPE: " + type);
	            if (checksum != null && !type.equals(ApplicationModel.TYPE_FILELINK)) {
	            	String hex = this.dao.getChecksum(file, checksum);
	            	if (hex != null) {
	            		properties.put("streamChecksum", hex);
	            	}
	            }
	            ok = this.fileFolder.insertFileProperties(type, aspects, properties, path, revision);
	            if (revision == null) {
	            	FileFolder.updateDocumentMetadataCounters(ok, bulkExportStatus);
	            } else {
                	FileFolder.updateDocumentVersionMetadataCounters(ok, bulkExportStatus);
                }
	            type = null;
	            properties = null;
	            aspects = null;
        	} else {
        		log.info("Skipping arm:reference type");
        		FileFolder.updateDocumentMetadataCounters(FileFolderStatus.SKIPPED, bulkExportStatus);
        	}
        }
        catch (Exception e) 
        {
        	if (revision == null) {
        		FileFolder.updateDocumentMetadataCounters(FileFolderStatus.FAIL, bulkExportStatus);
        	} else {
        		FileFolder.updateDocumentVersionMetadataCounters(FileFolderStatus.FAIL, bulkExportStatus);
        	}
            // for debugging purposes
            log.error("doCreateFile failed for noderef = " + file.toString());
            e.printStackTrace();
        }
    }
    
    
    /**
     * Create Folder and XML Metadata
     * 
     * @param file
     * @throws Exception
     */
    private void createFolder(NodeRef folder) throws Exception 
    {
        //Get Data
        log.debug("createFolder");
        String path = this.dao.getPath(folder);
        
//        if (!folderProperties.isEmpty()) {
//        	// loop through potential properties
//			for (QName property : folderProperties) {
//	    		if (this.dao.hasProperty(folder, property)) {
//		    		try {
//						path = path + "/" + this.dao.getProperty(folder, property);
//						break;
//					} catch (Exception e) {
//						log.error("Could not retrieve property: " + property);
//					}
//	    		}
//			}
//        }
        
        log.debug("createFolder path="+path);
        String type = this.dao.getType(folder);
        log.debug("createFolder type="+type);
        List<String> aspects = this.dao.getAspectsAsString(folder);
        Map<String, String> properties = this.dao.getPropertiesAsString(folder);
        
        //Create Folder and XMl Metadata
        FileFolderStatus ok = this.fileFolder.createFolder(path);
        FileFolder.updateFolderCounters(ok, bulkExportStatus);
        ok = this.fileFolder.insertFileProperties(type, aspects, properties, path, null);
        FileFolder.updateFolderMetadataCounters(ok, bulkExportStatus);
    }
}
