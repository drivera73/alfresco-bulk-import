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
package org.alfresco.extension.bulkexport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.alfresco.extension.bulkexport.controller.CacheGeneratedException;
import org.alfresco.extension.bulkexport.controller.Engine;
import org.alfresco.extension.bulkexport.dao.AlfrescoExportDao;
import org.alfresco.extension.bulkexport.dao.AlfrescoExportDaoImpl;
import org.alfresco.extension.bulkexport.model.FileFolder;
import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;
import org.apache.commons.lang3.text.StrTokenizer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class has a function to start the export process data contained in the repository.
 * 
 * @author Denys G. Santos (gsdenys@gmail.com)
 * @version 1.0.1
 */
public class Export extends AbstractWebScript 
{
    Log log = LogFactory.getLog(Export.class);

    /** Alfresco {@link ServiceRegistry} populated by Spring Framework. */
    protected ServiceRegistry serviceRegistry;
    
    /** Data Access Object to Alfresco Repository. */
    protected AlfrescoExportDao dao;
    
    /** File and folder manager. */
    protected FileFolder fileFolder;
    
    /** Engine of system */
    protected Engine engine;

	private Repository repositoryHelper;
    
    /**
     * Method to start program execution. 
     * 
     * @param req  The HTTP request parameter
     * @param res  The HTTP response parameter
     * @throws IOException
     */
    public void execute(WebScriptRequest req, WebScriptResponse res) throws IOException 
    {
        log.debug("execute");
        
        StopWatch timer = new StopWatch();

        //get URL parameters
        String nodeRef = req.getParameter("nodeRef");
        String base = req.getParameter("base");

        boolean scapeExported = false;
        boolean exportVersions = false;
        boolean revisionHead = false;
        boolean useNodeCache = false;
        boolean includeContent = true;
        List<String> documentProperties = new ArrayList<String>();
        List<String> folderProperties = new ArrayList<String>();
        boolean truncatePath = false;
        boolean foldersOnly = false;
        String checksum = null;
        String mode = null;
        String source = null;

        if (req.getParameter("source") != null) {
        	source = req.getParameter("source");
        	if (source.startsWith("workspace://")) {
        		nodeRef = source;
        	} else {
        		// looks to be a path
        		NodeRef companyHome = repositoryHelper.getCompanyHome();
        		
	            List<String> pathElements = new StrTokenizer(source, '/').getTokenList();
	 
	            try {
					nodeRef = serviceRegistry.getFileFolderService().resolveNamePath(companyHome, pathElements).getNodeRef().toString();
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					res.getWriter().write("*****************************************************************************************************\n");
                    res.getWriter().write("** ERROR occurred:\n");
                    res.getWriter().write("** Cannot locate source path in Repository: " + source + "\n");
                    res.getWriter().write("*****************************************************************************************************\n\n\n");
				}
        	}
        }
        
        if (req.getParameter("mode") != null) {
        	mode = req.getParameter("mode");
        	if (mode.equals("CALIENTE")) {
        		scapeExported = true;
        		exportVersions = true;
        		String folderProperty="{http://www.armedia.com/model/documentum/1.0}r_object_id";
        		folderProperties = new ArrayList<String>(Arrays.asList(folderProperty.split(",")));
        		String documentProperty="{http://www.armedia.com/model/documentum/1.0}i_chronicle_id,{http://www.armedia.com/model/documentum/1.0}r_object_id";
        		documentProperties = new ArrayList<String>(Arrays.asList(documentProperty.split(",")));
        		includeContent = false;
        		truncatePath = true;
        		foldersOnly = false;
        		checksum = "SHA-256";
        	}
        }
        
        if (req.getParameter("ignoreExported") != null)
        {
            if(req.getParameter("ignoreExported").equals("true")) 
            {
                scapeExported = true;
            }
        }

        // if a node has revisions, then export them as well
        if (req.getParameter("exportVersions") != null)
        {
            if(req.getParameter("exportVersions").equals("true")) 
            {
                exportVersions = true;
            } else {
            	exportVersions = false;
            }
        }

        // If this option is defined as true then all revisions are numbered
        // otherwise the bulk importer revisions are used (head is not named
        // with a revision)
        if (req.getParameter("revisionHead") != null)
        {
            if(req.getParameter("revisionHead").equals("true")) 
            {
                revisionHead = true;
            }
        }

        // If set to true then read a node.cache in the export directory as opposed to rescanning for nodes to export.
        // 
        if (req.getParameter("useNodeCache") != null)
        {
            if(req.getParameter("useNodeCache").equals("true")) 
            {
                useNodeCache = true;
            }
        }
        
        if (req.getParameter("includeContent") != null)
        {
            if(req.getParameter("includeContent").equals("false")) 
            {
            	includeContent = false;
            } else {
            	includeContent = true;
            }
        }
        
        if (req.getParameter("truncatePath") != null)
        {
            if(req.getParameter("truncatePath").equals("true")) 
            {
            	truncatePath = true;
            } else {
            	truncatePath = false;
            }
        }
        
        if (req.getParameter("foldersOnly") != null)
        {
            if(req.getParameter("foldersOnly").equals("true")) 
            {
            	foldersOnly = true;
            } else {
            	foldersOnly = false;
            }
        }
        
        
        if (req.getParameter("documentProperty") != null) {
        	String documentProperty = req.getParameter("documentProperty");
        	// convert to List
        	documentProperties = new ArrayList<String>(Arrays.asList(documentProperty.split(",")));
        }
        
        if (req.getParameter("folderProperty") != null) {
        	String folderProperty = req.getParameter("folderProperty");
        	folderProperties = new ArrayList<String>(Arrays.asList(folderProperty.split(",")));
        }
        
        if (req.getParameter("checksum") != null) {
        	checksum = req.getParameter("checksum");
        }
        
        //init variables
        dao = new AlfrescoExportDaoImpl(this.serviceRegistry);
        dao.setTruncatePath(truncatePath);
        fileFolder = new FileFolder(res, base, scapeExported);
        engine = new Engine(dao, fileFolder, exportVersions, revisionHead, useNodeCache, includeContent, documentProperties, folderProperties, foldersOnly, checksum);
        
        NodeRef nf = null;


        log.info("Bulk Export started");

        try
        {
        	if (NodeRef.isNodeRef(nodeRef)) {
        		if (serviceRegistry.getNodeService().exists(new NodeRef(nodeRef))) {
        			nf = dao.getNodeRef(nodeRef);
                    engine.execute(nf);
                    res.getWriter().write("Export finished Successfully\n");
        		} else {
        			res.getWriter().write("*****************************************************************************************************\n");
                    res.getWriter().write("** ERROR occurred:\n");
                    res.getWriter().write("** Node Reference does node exist: " + nodeRef + "\n");
                    res.getWriter().write("*****************************************************************************************************\n\n\n");
        		}
        	} else {
        		res.getWriter().write("*****************************************************************************************************\n");
                res.getWriter().write("** ERROR occurred:\n");
                res.getWriter().write("** Invalid Node Reference: " + nodeRef + "\n");
                res.getWriter().write("*****************************************************************************************************\n\n\n");
        	}
        } 
        catch (CacheGeneratedException e)
        {
            res.getWriter().write("*****************************************************************************************************\n");
            res.getWriter().write("** No Export performed - Cache file generated only - re-run to use cache file\n");
            res.getWriter().write("*****************************************************************************************************\n\n\n");
        }
        catch (Exception e) 
        {
            log.error("Error found during Export (Reason): " + e.toString() + "\n");
            e.printStackTrace();
            res.getWriter().write("*****************************************************************************************************\n");
            res.getWriter().write("** ERROR occurred:\n");
            res.getWriter().write("** " + e.toString() + "\n");
            res.getWriter().write("*****************************************************************************************************\n\n\n");
        }

        //
        // writes will not appear until the script is finished, flush does not help
        //
        res.getWriter().write("Performed Export with the following Parameters :\n"); 
        res.getWriter().write("   export folder   : " + base + "\n");
        if (source != null) {
        	res.getWriter().write("   source to export  : " + source + "\n");
        } else {
        	res.getWriter().write("   node to export  : " + nodeRef + "\n");
        }
        res.getWriter().write("   ignore exported : " + scapeExported + "\n");
        res.getWriter().write("   export versions : " + exportVersions + "\n");
        res.getWriter().write("   include content : " + includeContent + "\n");
        res.getWriter().write("   folders only : " + foldersOnly + "\n");
        res.getWriter().write("   checksum : " + checksum + "\n");
        res.getWriter().write("   document property : " + documentProperties + "\n");
        res.getWriter().write("   folder property : " + folderProperties + "\n");
        res.getWriter().write("   truncate path : " + truncatePath + "\n");
        res.getWriter().write("   bulk import revision scheme: " + !revisionHead +"\n");

        long duration = timer.elapsedTime();
        long seconds = TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration));
        long minutes = TimeUnit.MILLISECONDS.toMinutes(duration);
        res.getWriter().write("Export elapsed time: minutes:" + minutes + " , seconds: " + seconds + "\n"); 

        log.info("Bulk Export finished");
    }


    public ServiceRegistry getServiceRegistry() 
    {
        return serviceRegistry;
    }


    public void setServiceRegistry(ServiceRegistry serviceRegistry) 
    {
        this.serviceRegistry = serviceRegistry;
    }
    
    public void setRepositoryHelper(Repository repositoryHelper) {
		this.repositoryHelper = repositoryHelper;
	}
}
