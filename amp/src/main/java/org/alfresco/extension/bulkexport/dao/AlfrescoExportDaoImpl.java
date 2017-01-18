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
package org.alfresco.extension.bulkexport.dao;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.alfresco.extension.bulkexport.utils.Checksum;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.ActionModel;
import org.alfresco.repo.publishing.PublishingModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.Path;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionHistory;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.namespace.NamespacePrefixResolver;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * Implementation of {@link AlfrescoExportDao} interface
 * 
 * @author Denys Santos (gsdenys@gmail.com)
 * @version 1.0.1
 */
public class AlfrescoExportDaoImpl implements AlfrescoExportDao 
{

    Log log = LogFactory.getLog(AlfrescoExportDaoImpl.class);
    
    public static final String DOCUMENTUM_MODEL_URL = "http://www.armedia.com/model/documentum/1.0";
	public static final String DOCUMENTUM_MODEL_PREFIX = "dctm";
	public static final QName PROP_I_CHRONICLE_ID = QName.createQName(DOCUMENTUM_MODEL_URL, "i_chronicle_id");
	
    public static final String CALIENTE_MODEL_URL = "http://www.armedia.com/model/armedia/1.0";
    public static final String CALIENTE_MODEL_PREFIX = "arm";
    public static final String VDOCROOT = "vdocRoot";
	public static final String VDOCVERSION = "vdocVersion";
	public static final QName ASPECT_CALIENTE_VDOCROOT = QName.createQName(CALIENTE_MODEL_URL, "vdocRoot");
	public static final QName ASPECT_CALIENTE_VDOCVERSION = QName.createQName(CALIENTE_MODEL_URL, "vdocVersion");
	public static final String STR_ASPECT_CALIENTE_VDOCROOT = String.format("%s:%s", CALIENTE_MODEL_URL, VDOCROOT);
	public static final String STR_ASPECT_CALIENTE_VDOCVERSION = String.format("%s:%s", CALIENTE_MODEL_URL, VDOCVERSION);

    /** Alfresco {@link ServiceRegistry} to Data Access Object */ 
    private ServiceRegistry registry;

    private final NodeService nodeService;
    private final FileFolderService service;
    private final NamespacePrefixResolver nsR;
    private final ContentService contentService;
    private final PermissionService permissionService;
    private final VersionService versionService;
    private final DictionaryService dictionaryService;
    
    private boolean truncatePath = false;
    private NodeRef exportNodeRef = null;
    private List<QName> folderProperties = new ArrayList<QName>();
    
    private Map<NodeRef, String> pathCache = null;
        
    private QName ignoreAspectQname[] = 
    {
            ContentModel.ASPECT_TAGGABLE
    };
    
    private String ignoreAspectPrefix[] = 
    {
            "app"
    };
    
    private QName ignorePropertyQname[] = 
    { 
            ContentModel.PROP_NODE_DBID, 
            ContentModel.PROP_NODE_UUID, 
            ContentModel.PROP_CATEGORIES,
            ContentModel.PROP_CONTENT,
            ContentModel.ASPECT_TAGGABLE
    };
    
    private String[] ignorePropertyPrefix = 
    {
            "app",
            "exif"
    };
    
    private QName[] ignoredType = 
    {
            ContentModel.TYPE_SYSTEM_FOLDER,
            ContentModel.TYPE_LINK,
            ContentModel.TYPE_RATING,
            ActionModel.TYPE_ACTION,
            ActionModel.TYPE_COMPOSITE_ACTION,
            PublishingModel.TYPE_PUBLISHING_QUEUE
    };
    
    
    /**
     * Data Access Object Builder
     * 
     * @param registry Alfresco {@link ServiceRegistry} 
     */
    public AlfrescoExportDaoImpl(ServiceRegistry registry) 
    {
        log.debug("Test debug logging. Congratulation your AMP is working");

        this.registry  = registry;
        if (registry == null) {
        	log.info("service registry is null :(");
        } else {
        	log.info("service registry is not null :");
        }

        nodeService    = this.registry.getNodeService();
        log.info("Fetched node service");
        service        = this.registry.getFileFolderService();
        log.info("Fetched file folder service");
        nsR            = this.registry.getNamespaceService();
        log.info("Fetched nsr service");
        contentService = this.registry.getContentService();
        log.info("Fetched content service");
        permissionService = this.registry.getPermissionService();
        log.info("Fetched permission service");
        versionService = this.registry.getVersionService();
        log.info("Fetched version service");
        dictionaryService = this.registry.getDictionaryService();
        log.info("Fetched dictionary service");
        pathCache = new HashMap<NodeRef, String>();
        
        log.info("dao is ready");
    }
    
    
    /**
     * @see com.alfresco.bulkexport.dao.AlfrescoExportDao#getProperties(java.lang.String)
     */
    public Map<QName, Serializable> getProperties(NodeRef nodeRef) throws Exception 
    {
        Map<QName, Serializable> properties = nodeService.getProperties(nodeRef);
        return properties;
    }
    
    
    /**
     * @see com.alfresco.bulkexport.dao.AlfrescoExportDao#getMetadataAsString(java.lang.String)
     */
    public Map<String, String> getPropertiesAsString(NodeRef nodeRef) throws Exception 
    {
                
        Map<QName, Serializable> properties = this.getProperties(nodeRef);
        
        Map<String, String> props = new HashMap<String, String>();
        Set<QName> qNameSet = properties.keySet();
        
        for (QName qName : qNameSet) 
        {
            //case the qname is in ignored type do nothing will do.
            if(this.isPropertyIgnored(qName))
            {
                continue;
            }

            Serializable obj = properties.get(qName);
            boolean multi = dictionaryService.getProperty(qName).isMultiValued();
            String name = this.getQnameStringFormat(qName);
            String value = this.formatMetadata(obj, multi);

            //put key value in the property list as <prefixOfProperty:nameOfProperty, valueOfProperty>
            props.put(name, value);
        }


        return props;
    }

    
    /**
     * @see com.alfresco.bulkexport.dao.AlfrescoExportDao#getChildren(java.lang.String)
     */
    public List<NodeRef> getChildren(NodeRef nodeRef) throws Exception 
    {
        List<NodeRef> listChildren = new ArrayList<NodeRef>();
       
        List<ChildAssociationRef> children = nodeService.getChildAssocs(nodeRef);
        
        for (ChildAssociationRef childAssociationRef : children) 
        {
            NodeRef child = childAssociationRef.getChildRef();
                
            if(this.isTypeIgnored(nodeService.getType(child)))
            {
                continue;
            }
            
            listChildren.add(new NodeRef(child.toString())); // deep copy
        }
        
        return listChildren;
    }
    

    /**
     * @see com.alfresco.bulkexport.dao.AlfrescoExportDao#getFolderChildren(java.lang.String)
     */
    public List<NodeRef> getFolderChildren(NodeRef nodeRef) throws Exception 
    {
        
        List<FileInfo> folders = service.listFolders(nodeRef);
        
        List<NodeRef> listChildren = new ArrayList<NodeRef>();
        
        for (FileInfo fileInfo : folders) 
        {
            listChildren.add(fileInfo.getNodeRef());
        }
        
        return listChildren;
    }

    
    /**
     * @see com.alfresco.bulkexport.dao.AlfrescoExportDao#getFileChildren(java.lang.String)
     */
    public List<NodeRef> getFileChildren(NodeRef nodeRef) throws Exception 
    {
        List<FileInfo> files = service.listFiles(nodeRef);
        
        List<NodeRef> listChildren = new ArrayList<NodeRef>();
        
        for (FileInfo fileInfo : files) 
        {
            listChildren.add(fileInfo.getNodeRef());
        }
        
        return listChildren;
    }

    
    /**
     * @see com.alfresco.bulkexport.dao.AlfrescoExportDao#getPath(java.lang.String)
     */
/*
    public String getPath(NodeRef nodeRef) throws Exception 
    {
        //get element Path
        Path path = nodeService.getPath(nodeRef);
        
        //get element name 
        Serializable name = nodeService.getProperty(nodeRef, ContentModel.PROP_NAME);
        
        //get element Path as String
        String basePath = path.toDisplayPath(nodeService, permissionService);
        
        return (basePath + "/" + name);
    }
 */
    
    /**
     * @see com.alfresco.bulkexport.dao.AlfrescoExportDao#getPath(java.lang.String)
     */
    public String getPath(NodeRef nodeRef) throws Exception 
    {
    	String basePath = null;
    	String fullpath = null;
    	if (folderProperties.isEmpty()) {
	        //get element Path
	        Path path = nodeService.getPath(nodeRef);
	        
	        //get element Path as String
	        basePath = path.toDisplayPath(nodeService, permissionService); 
	        //get element name 
	        Serializable name = getProperty(nodeRef, ContentModel.PROP_NAME);
	        fullpath = basePath + "/" + name;
    	} else {
    		// magic time!
    		log.debug("getting path from folderProperties");
    		fullpath = createPathFromParents(nodeRef);
    	}
        
        log.debug("new path: " + fullpath);
        return (fullpath);
    }
    
    
    private String createPathFromParents(NodeRef nodeRef) throws Exception {
    	String path = "";
    	
    	if (nodeRef != null) {
    		log.debug("Processing: " + nodeRef.toString());
    		
    		// check to see if we have already cached this path
    		
    		if (pathCache.containsKey(nodeRef)) {
    			path = pathCache.get(nodeRef);
    			return path;
    		}
    		
    		List<QName> newFolderProperties = new ArrayList<QName>();
    		if (nodeService.hasAspect(nodeRef, ASPECT_CALIENTE_VDOCROOT)) {
    			newFolderProperties.add(PROP_I_CHRONICLE_ID);
    		} else if(nodeService.hasAspect(nodeRef, ASPECT_CALIENTE_VDOCVERSION)) {
    			newFolderProperties.add(ContentModel.PROP_NAME);
    		}
    		newFolderProperties.addAll(folderProperties);
    		Map<QName, Serializable> properties = nodeService.getProperties(nodeRef);
	    	if (nodeService.getPrimaryParent(nodeRef) != null) {
	    		if (truncatePath) {
	    			log.debug("Comparing nodes for truncation: " + exportNodeRef + " and " + nodeService.getPrimaryParent(nodeRef));
	    			if (exportNodeRef.equals(nodeRef)) {
	    				log.debug("We have a truncatePath Hit!");
	    				if (!newFolderProperties.isEmpty()) {
	    	    			// loop through potential properties
	    	    			for (QName property : newFolderProperties) {
	    			    		if (properties.containsKey(property)) {
	    				    		try {
	    								path = path + "/" + getProperty(nodeRef, property);
	    								break;
	    							} catch (Exception e) {
	    								log.error("Could not retrieve property: " + property);
	    							}
	    			    		}
	    	    			}
	    	    		}
	    				pathCache.put(nodeRef, path);
	    				return path;
	    			}
	    		}
	    		path = createPathFromParents(nodeService.getPrimaryParent(nodeRef).getParentRef());
	    		if (!newFolderProperties.isEmpty()) {
	    			// loop through potential properties
	    			for (QName property : newFolderProperties) {
			    		if (properties.containsKey(property)) {
				    		try {
								path = path + "/" + getProperty(nodeRef, property);
								break;
							} catch (Exception e) {
								log.error("Could not retrieve property: " + property);
							}
			    		}
	    			}
	    		}
	    	} else {
	    		if (!newFolderProperties.isEmpty()) {
	    			// loop through potential properties
	    			for (QName property : newFolderProperties) {
			    		if (properties.containsKey(property)) {
				    		try {
								path = path + "/" + getProperty(nodeRef, property);
								break;
							} catch (Exception e) {
								log.error("Could not retrieve property: " + property);
							}
			    		}
	    			}
	    		}
	    	}
    	}
    	log.debug("Dynamic path: " + path);
    	pathCache.put(nodeRef, path);
    	return path;
    }
    
    /**
     * @see com.alfresco.bulkexport.dao.AlfrescoExportDao#getContent(java.lang.String)
     */
    public ByteArrayOutputStream getContent(NodeRef nodeRef) throws Exception 
    {
        ContentReader reader = contentService.getReader(nodeRef, ContentModel.PROP_CONTENT);
        if (reader == null)
        {
            // no data for this node
            return null;
        }

        
        InputStream in = reader.getContentInputStream();
        int size = in.available();
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[ (size + 100) ];
        int sizeOut;
        
        while ((sizeOut=in.read(buf)) != -1 ) 
        {
            out.write(buf, 0, sizeOut);
        }
        
        out.flush();
        out.close();
        
        in.close();
        
        
        return out;
    }

    public String getChecksum(NodeRef nodeRef, String sum) {
    	ContentReader reader = contentService.getReader(nodeRef, ContentModel.PROP_CONTENT);
        if (reader == null)
        {
            // no data for this node
            return null;
        }
       
        return Checksum.calculateChecksum(reader.getContentInputStream(), reader.getContentData().getSize(), sum);
    }
    
    public boolean getContentAndStoreInFile(NodeRef nodeRef, String outputFileName) throws Exception 
    {
        ContentReader reader = contentService.getReader(nodeRef, ContentModel.PROP_CONTENT);
        if (reader == null)
        {
            // no data for this node
            return false;
        }
       
        File output = new File(outputFileName);
        reader.getContent(output);

        return true;
    }
    
    /**
     * @see com.alfresco.bulkexport.dao.AlfrescoExportDao#getProperty(java.lang.String, java.lang.String)
     */
    public String getProperty(NodeRef nodeRef, QName propertyQName) throws Exception 
    {
        Serializable value = nodeService.getProperty(nodeRef, propertyQName);
        boolean multi = dictionaryService.getProperty(propertyQName).isMultiValued();
        
        return this.formatMetadata(value, multi);
    }

    
    public boolean hasProperty(NodeRef nodeRef, QName propertyQName) throws Exception
    {
    	boolean rv = false;
    	
    	Map<QName, Serializable> properties = nodeService.getProperties(nodeRef);
    	if (properties.containsKey(propertyQName)) {
    		rv = true;
    	}
    	
    	return rv;
    }
    /**
     * @see com.alfresco.bulkexport.dao.AlfrescoExportDao#getType(java.lang.String)
     */
    public String getType(NodeRef nodeRef) throws Exception 
    {
        QName value = nodeService.getType(nodeRef);
        
        String name = this.getQnameStringFormat(value);
        
        return name;
    }
    
    public boolean hasAspect(NodeRef nodeRef, QName aspect) {
    	boolean rv = false;
    	
    	if (nodeService.hasAspect(nodeRef, aspect)) {
    		rv = true;
    	}
    	return rv;
    }
    
    /**
     * @see com.alfresco.bulkexport.dao.AlfrescoExportDao#getAspects(java.lang.String)
     */
    public List<QName> getAspects(NodeRef nodeRef) throws Exception 
    {
        Set<QName> aspectSet = nodeService.getAspects(nodeRef);
        List<QName> qn = new ArrayList<QName>(aspectSet);
        
        return qn;
    }
    
    
    /**
     * @see com.alfresco.bulkexport.dao.AlfrescoExportDao#getAspectsAsString(java.lang.String)
     */
    public List<String> getAspectsAsString(NodeRef nodeRef) throws Exception 
    {
        List<QName> qn = this.getAspects(nodeRef);
        List<String> str = new ArrayList<String>();
        
        for (QName qName : qn) 
        {            
            if(this.isAspectIgnored(qName)) 
            {
                continue;
            }
            
            String name = this.getQnameStringFormat(qName);
            str.add(name); 
        }
        
        return str;
    }
    
    
    /**
     * @see com.alfresco.bulkexport.dao.AlfrescoExportDao#isFolder(java.lang.String)
     */
    public boolean isFolder(NodeRef nodeRef) throws Exception 
    {
        log.debug("isFolder");

        log.info("inside isFolder");
        if (service == null) {
        	log.info("hmm, service is null :(");
        }
        FileInfo info = service.getFileInfo(nodeRef);
        log.debug("isFolder got file info getName = " + info.getName());
        log.debug("isFolder got file info isFolder = " + info.isFolder());
        log.debug("isFolder return isFolder");
        
        return info.isFolder();
    }
    
    /**
     * @see com.alfresco.bulkexport.dao.AlfrescoExportDao#getNodeRef(java.lang.String)
     */
    public NodeRef getNodeRef(String nodeRef) 
    {
        try
        {
            NodeRef nr = new NodeRef(nodeRef);
            return nr;
        } 
        catch (Exception e) 
        {
            return null;
        }
    }

    /**
     * @see com.alfresco.bulkexport.dao.AlfrescoExportDao#getParentNodeRef(java.lang.String)
     */
    public NodeRef getParentNodeRef(NodeRef nodeRef) 
    {
        try
        {
            NodeRef nr = nodeService.getPrimaryParent(nodeRef).getParentRef();
            return nr;
        } 
        catch (Exception e) 
        {
            return null;
        }
    }
    
    /**
     * @see com.alfresco.bulkexport.dao.AlfrescoExportDao#getNodeRefHistory(java.lang.String)
     */
    public Map<String,NodeRefRevision> getNodeRefHistory(String nodeRef) throws Exception
    {
        log.debug("getNodeRefHistory(nodeRef) nodeRef = " + nodeRef);
        Map<String,NodeRefRevision> nodes = null;

        NodeRef nr = getNodeRef(nodeRef);
        if (nr != null)
        {
            VersionHistory history =  versionService.getVersionHistory(nr);
            if (history == null)
            {
                log.debug("getNodeRefHistory(nodeRef) no history available");
                return nodes;
            }

            Collection<Version> availableVersions = history.getAllVersions();

            if (availableVersions == null)
            {
                log.debug("getNodeRefHistory(nodeRef) no versions found in history");
                return nodes;
            }

            Version last = history.getPredecessor(history.getHeadVersion());
            log.debug("Predecessor: " + last.getVersionLabel());
            String[] parts = last.getVersionLabel().split(Pattern.quote("."));
            int major = parts[0].length();
            
            nodes = new HashMap<String,NodeRefRevision>();
            Iterator iterator = availableVersions.iterator();
            while(iterator.hasNext())
            {
                    Object ov = iterator.next();
                    Version v = (Version)ov; // contains storeRef
                    String checkInComment = v.getDescription();    // check in comment
                    String vlabel = v.getVersionLabel(); // version label eg. 1.1
                    NodeRef frozenNodeRef = v.getFrozenStateNodeRef();   // this contains the revisioned versions
                    NodeRef versionedNodeRef = v.getVersionedNodeRef();  // this is allways the latest revision
                    String frozenNodRef = frozenNodeRef.toString();
                    String headNodeRef  = versionedNodeRef.toString();

                    // this contains a list of all attributes for the Item. We may not need it since we dig them out at a store item id level.
                    Map<String,Serializable>  versionProps = v.getVersionProperties(); 
                    NodeRefRevision revision = new NodeRefRevision();
                    revision.comment = checkInComment;
                    revision.node = frozenNodeRef;

                    String newrevision = vlabel.substring(0, vlabel.indexOf("."));
                    newrevision = StringUtils.leftPad(vlabel.substring(0, vlabel.indexOf(".")), major, '0');
                    nodes.put(newrevision, revision);
                    //
                    // we need to get the comment history as well because this is not available when we get content data and properties....
                    log.debug("getNodeRefHistory(nodeRef) v = " + v.toString());
            }
        }
        return nodes;
    }
    
    
    public boolean isNodeIgnored(String nodeRef) 
    {
        log.debug("isNodeIgnored");
        NodeRef nr = getNodeRef(nodeRef);
        
        QName value = nodeService.getType(nr);
        
        log.debug("isNodeIgnored got service type");
        return isTypeIgnored(value);
    }
    
    // #######################################################################################
    // ####                              PRIVATE METHODS                                   ### 
    // #######################################################################################

    /**
     * Verify if the type qname is ignored 
     * 
     * @param qName
     * @return {@link Boolean}
     */
    private boolean isPropertyIgnored(QName qName) 
    {
        //verify if qname is in ignored
        for (QName qn : this.ignorePropertyQname) 
        {
            if(qn.equals(qName))
            {
                return true;
            }
        }
        
        //verify if qname prefix is in ignored
        //String prefix = qName.getPrefixString();
        String prefix = qName.getPrefixedQName(nsR).getPrefixString();
        for (String str : this.ignorePropertyPrefix) 
        {
            
            //str.equalsIgnoreCase(prefix)
            
            if(prefix.startsWith(str))
            {
                return true;
            }
        }
        
        return false;
    }

    
    /**
     * Verify if the aspect qname is ignored 
     * 
     * @param qName
     * @return {@link Boolean}
     */
    private boolean isAspectIgnored(QName qName) 
    {
        //verify if qname is in ignored
        for (QName qn : this.ignoreAspectQname) 
        {
            if(qn.equals(qName))
            {
                return true;
            }
        }
        
        //verify if qname prefix is in ignored
        //String prefix = qName.getPrefixString();
        String prefix = qName.getPrefixedQName(nsR).getPrefixString();
        for (String str : this.ignoreAspectPrefix) 
        {
            if(prefix.startsWith(str))
            {
                return true;
            }
        }
        
        return false;
    }
    
    
    /**
     * Verify if the tipe qname is ignored 
     * 
     * @param qName
     * @return {@link Boolean}
     */
    private boolean isTypeIgnored(QName qName) 
    {
        //verify if qname is in ignored
        for (QName qn : this.ignoredType) 
        {
            if(qn.equals(qName))
            {
            	log.debug("Ignoring: " + qName.getLocalName());
                return true;
            }
        }
        return false;
    }
    

    /**
     * Return Qname in String Format
     * 
     * @param qName
     * @return {@link String}
     */
    private String getQnameStringFormat(QName qName) throws Exception
    {
        return qName.getPrefixedQName(nsR).getPrefixString();
    }


    private String formatMetadata (Serializable obj)
    {
    	if (obj == null) return "";
    	if (Date.class.isInstance(obj))
    	{
    		Date d = Date.class.cast(obj);
    		// TODO: some sort of pooling, for efficiency
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
    		return format.format(d);
        }

    	return obj.toString();
    }

    /**
     * Format metadata guided by Bulk-Import format
     * 
     * @param obj
     * @return {@link String}
     */
    private String formatMetadata (Serializable obj, boolean isMulti)
    {
    	if (isMulti)
    	{
    		if (List.class.isInstance(obj))
    		{
    			@SuppressWarnings("unchecked")
				List<Serializable> l = (List<Serializable>)obj; 
        		StringBuilder b = new StringBuilder();
        		boolean first = true;
        		for (Serializable s : l)
        		{
        			if (!first) b.append(',');
        			b.append(String.valueOf(s));
        			first = false;
        		}
        		return b.toString();
    		}
    	}

   		return formatMetadata(obj);
    }


	public boolean isTruncatePath() {
		return truncatePath;
	}


	public void setTruncatePath(boolean truncatePath) {
		this.truncatePath = truncatePath;
	}

	public boolean getTruncatePath() {
		return this.truncatePath;
	}

	public NodeRef getExportNodeRef() {
		return exportNodeRef;
	}


	public void setExportNodeRef(NodeRef exportNodeRef) {
		this.exportNodeRef = exportNodeRef;
	}


	public List<QName> getFolderProperties() {
		return folderProperties;
	}


	public void setFolderProperties(List<QName> folderProperties) {
		this.folderProperties = folderProperties;
	}
	
	public ServiceRegistry getServiceRegistry() {
		return this.registry;
	}
}
