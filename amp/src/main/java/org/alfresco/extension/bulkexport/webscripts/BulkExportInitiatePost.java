package org.alfresco.extension.bulkexport.webscripts;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.extension.bulkexport.BulkExport;
import org.alfresco.extension.bulkexport.BulkExportService;
import org.alfresco.extension.bulkexport.WriteableBulkExportStatus;
import org.alfresco.extension.bulkexport.controller.Engine;
import org.alfresco.extension.bulkexport.dao.AlfrescoExportDao;
import org.alfresco.extension.bulkexport.model.FileFolder;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.lang3.text.StrTokenizer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class BulkExportInitiatePost extends DeclarativeWebScript {
	private static Log LOG = LogFactory.getLog(BulkExportInitiatePost.class);
	
	private Repository repositoryHelper;
	private ServiceRegistry serviceRegistry;
	private BulkExport bulkExport;
	private BulkExportService bulkExportService;
	private WriteableBulkExportStatus bulkExportStatus;
	
	private final String PARAM_SOURCELOCATION = "sourceLocation";
	private final String PARAM_EXPORTLOCATION = "exportLocation";
	private final String PARAM_SCAPEEXPORTED = "scapeExported";
	private final String PARAM_EXPORTVERSIONS = "exportVersions";
	private final String PARAM_INCLUDECONTENT = "includeContent";
	private final String PARAM_FOLDERSONLY = "foldersOnly";
	private final String PARAM_TRUNCATEPATH = "truncatePath";
	private final String PARAM_REVISIONHEAD = "revisionHead";
	private final String PARAM_USENODECACHE = "useNodeCache";
	private final String PARAM_CHECKSUM = "checkSum";
	private final String PARAM_DOCUMENTPROPERTY = "documentProperty";
	private final String PARAM_FOLDERPROPERTY = "folderProperty";
	private final String PARAM_INCLUDEMETADATA = "includeMetadata";
	private final String PARAM_BATCHSIZE = "batchSize";
	private final String PARAM_THREADCOUNT = "threadCount";
	
	private final String CHECKBOX_ON = "on";
	
	private final static String WEBSCRIPT_URI_BULKEXPORT_STATUS = "/bulk/export/status";
	private final static int DEFAULT_BATCHSIZE = 50;
	private final static int DEFAULT_THREADCOUNT = 4;
	
	@Override
	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
		Map<String, Object> model = new HashMap<String, Object>();
        boolean scapeExported = false;
        boolean exportVersions = false;
        boolean revisionHead = false;
        boolean useNodeCache = false;
        boolean includeContent = false;
        boolean includeMetadata = true;
        List<String> documentProperties = new ArrayList<String>();
        List<String> folderProperties = new ArrayList<String>();
        boolean truncatePath = true;
        boolean foldersOnly = false;
        String checksum = null;
        String source = null;
        String nodeRefStr = null;
        String exportLocation = null;
        int batchSize = DEFAULT_BATCHSIZE;
        int threadCount = DEFAULT_THREADCOUNT;
        
        List<String> unsupportedParameters = new ArrayList<String>();
        List<String> errorLog = new ArrayList<String>();
        
		long startTime = System.currentTimeMillis();
		model.put("startTimeMS", startTime);
		
		boolean ok = true;
		for (final String parameterName : req.getParameterNames()) {
			switch (parameterName) {
			case PARAM_EXPORTLOCATION:
				exportLocation = req.getParameter(PARAM_EXPORTLOCATION);
				if (exportLocation == null) {
					errorLog.add("Export Location has not been specified.  Bulk Export does not know where to export to");
				} else {
					// check to see if this path is accessible and writeable
					File el = new File(exportLocation);
					if (!el.exists()) {
						errorLog.add("Export Location [" + exportLocation + "] does not exist");
						ok = false;
					} else if (!el.canWrite()) {
						errorLog.add("Export Location [" + exportLocation + "] is not writeable");
						ok = false;
					}
				}
				LOG.debug(String.format("Param: %s - Value: %s", PARAM_EXPORTLOCATION, exportLocation));
				model.put(PARAM_EXPORTLOCATION, exportLocation);
				break;
			case PARAM_SOURCELOCATION:
				nodeRefStr = req.getParameter(PARAM_SOURCELOCATION);
	        	if (nodeRefStr.startsWith("workspace://")) {
	        		source = nodeRefStr;
	        	} else {
	        		// looks to be a path
	        		NodeRef companyHome = repositoryHelper.getCompanyHome();
	        		
		            List<String> pathElements = new StrTokenizer(nodeRefStr, '/').getTokenList();
		 
		            try {
						source = serviceRegistry.getFileFolderService().resolveNamePath(companyHome, pathElements).getNodeRef().toString();
					} catch (FileNotFoundException e) {
						// TODO Auto-generated catch block
						errorLog.add("Cannot locate source path in Repository: " + nodeRefStr);
						ok = false;
					}
	        	}
	        	LOG.debug(String.format("Param: %s - Value: %s", PARAM_SOURCELOCATION, source));
	        	model.put(PARAM_SOURCELOCATION, source);
				break;
			case PARAM_SCAPEEXPORTED:
				String scapeExportedStr = req.getParameter(PARAM_SCAPEEXPORTED);
				if (scapeExportedStr != null && scapeExportedStr.equals(CHECKBOX_ON)) {
					scapeExported = true;
				} else {
					scapeExported = false;
				}
				LOG.debug(String.format("Param: %s - Value: %s", PARAM_SCAPEEXPORTED, scapeExported));
				model.put(PARAM_SCAPEEXPORTED, scapeExportedStr);
				break;
			case PARAM_EXPORTVERSIONS:
				String exportVersionsStr = req.getParameter(PARAM_EXPORTVERSIONS);
				if (exportVersionsStr != null && exportVersionsStr.equals(CHECKBOX_ON)) {
					exportVersions = true;
				} else {
					exportVersions = false;
				}
				LOG.debug(String.format("Param: %s - Value: %s", PARAM_EXPORTVERSIONS, exportVersions));
				model.put(PARAM_EXPORTVERSIONS, exportVersionsStr);
				break;
			case PARAM_INCLUDECONTENT:
				String includeContentStr = req.getParameter(PARAM_INCLUDECONTENT);
				if (includeContentStr != null && includeContentStr.equals(CHECKBOX_ON)) {
					includeContent = true;
				} else {
					includeContent = false;
				}
				LOG.debug(String.format("Param: %s - Value: %s", PARAM_INCLUDECONTENT, includeContent));
				model.put(PARAM_INCLUDECONTENT, includeContentStr);
				break;
			case PARAM_INCLUDEMETADATA:
				String includeMetadataStr = req.getParameter(PARAM_INCLUDEMETADATA);
				if (includeMetadataStr != null && includeMetadataStr.equals(CHECKBOX_ON)) {
					includeMetadata = true;
				} else {
					includeMetadata = false;
				}
				LOG.debug(String.format("Param: %s - Value: %s", PARAM_INCLUDEMETADATA, includeMetadata));
				model.put(PARAM_INCLUDEMETADATA, includeMetadataStr);
				break;
			case PARAM_FOLDERSONLY:
				String foldersOnlyStr = req.getParameter(PARAM_FOLDERSONLY);
				if (foldersOnlyStr != null && foldersOnlyStr.equals(CHECKBOX_ON)) {
					foldersOnly = true;
				} else {
					foldersOnly = false;
				}
				LOG.debug(String.format("Param: %s - Value: %s", PARAM_FOLDERSONLY, foldersOnly));
				model.put(PARAM_FOLDERSONLY, foldersOnlyStr);
				break;
			case PARAM_TRUNCATEPATH:
				String truncatePathStr = req.getParameter(PARAM_TRUNCATEPATH);
				if (truncatePathStr != null && truncatePathStr.equals(CHECKBOX_ON)) {
					truncatePath = true;
				} else {
					truncatePath = false;
				}
				LOG.debug(String.format("Param: %s - Value: %s", PARAM_TRUNCATEPATH, truncatePath));
				model.put(PARAM_TRUNCATEPATH, truncatePathStr);
				break;
			case PARAM_REVISIONHEAD:
				String revisionHeadStr = req.getParameter(PARAM_REVISIONHEAD);
				if (revisionHeadStr != null && revisionHeadStr.equals(CHECKBOX_ON)) {
					revisionHead = true;
				} else {
					revisionHead = false;
				}
				LOG.debug(String.format("Param: %s - Value: %s", PARAM_REVISIONHEAD, revisionHead));
				model.put(PARAM_REVISIONHEAD, revisionHeadStr);
				break;
			case PARAM_USENODECACHE:
				String useNodeCacheStr = req.getParameter(PARAM_USENODECACHE);
				if (useNodeCacheStr != null && useNodeCacheStr.equals(CHECKBOX_ON)) {
					useNodeCache = true;
				} else {
					useNodeCache = false;
				}
				LOG.debug(String.format("Param: %s - Value: %s", PARAM_USENODECACHE, useNodeCache));
				model.put(PARAM_USENODECACHE, useNodeCacheStr);
				break;
			case PARAM_CHECKSUM:
				checksum = req.getParameter(PARAM_CHECKSUM);
				break;
			case PARAM_DOCUMENTPROPERTY:
				String documentProperty = req.getParameter(PARAM_DOCUMENTPROPERTY);
				if (documentProperty != null && documentProperty.length() > 0) {
					documentProperties = new ArrayList<String>(Arrays.asList(documentProperty.split(",")));
				}
				LOG.debug(String.format("Param: %s - Value: %s", PARAM_DOCUMENTPROPERTY, documentProperties));
				model.put(PARAM_DOCUMENTPROPERTY, documentProperty);
				break;
			case PARAM_FOLDERPROPERTY:
				String folderProperty = req.getParameter(PARAM_FOLDERPROPERTY);
				if (folderProperty != null && folderProperty.length() > 0) {
					folderProperties = new ArrayList<String>(Arrays.asList(folderProperty.split(",")));
				}
				LOG.debug(String.format("Param: %s - Value: %s", PARAM_FOLDERPROPERTY, folderProperties));
				model.put(PARAM_FOLDERPROPERTY, folderProperty);
				break;
			case PARAM_BATCHSIZE:
				String bs = req.getParameter(PARAM_BATCHSIZE);
				try {
					batchSize = Integer.parseInt(bs);
				} catch (NumberFormatException ex) {
					LOG.error("Invalid Batch Size specified: " + bs + " - defaulting to: " + DEFAULT_BATCHSIZE);
					batchSize = DEFAULT_BATCHSIZE;
				}
				break;
			case PARAM_THREADCOUNT:
				String tc = req.getParameter(PARAM_THREADCOUNT);
				try {
					threadCount = Integer.parseInt(tc);
				} catch (NumberFormatException ex) {
					LOG.error("Invalid number of threads specified: " + tc + " - defaulting to: " + DEFAULT_THREADCOUNT);
					threadCount = DEFAULT_THREADCOUNT;
				}
				break;
			default:
				unsupportedParameters.add(parameterName);
			}
		}
       
		if (ok) {
			//init variables
			LOG.debug("We have the required arguments, start Bulk Export Thread");
			LOG.debug("Running thread as: " + AuthenticationUtil.getRunAsUser());
	        bulkExport.start(AuthenticationUtil.getRunAsUser(), source, truncatePath, exportLocation, scapeExported, exportVersions, revisionHead, useNodeCache, includeContent, foldersOnly, checksum, includeMetadata, folderProperties, documentProperties, bulkExportService, bulkExportStatus, batchSize, threadCount);
		} else {
			LOG.debug("Missing required arguments or invalid paths");
			for (String l : errorLog) {
				LOG.error(l);
			}
			model.put("errorLog", errorLog);
		}
		if (unsupportedParameters.size() > 0) {
			model.put("unsupported", unsupportedParameters);
		}
		long endTime = System.currentTimeMillis();
		model.put("endTimeMS", endTime);
		if (ok) {
			status.setCode(Status.STATUS_MOVED_TEMPORARILY);
	        status.setRedirect(true);
	        status.setLocation(req.getServiceContextPath() + WEBSCRIPT_URI_BULKEXPORT_STATUS);
		}
		return model;
	}
	
    public void setServiceRegistry(ServiceRegistry serviceRegistry) 
    {
        this.serviceRegistry = serviceRegistry;
    }
    
    public void setRepositoryHelper(Repository repositoryHelper) {
		this.repositoryHelper = repositoryHelper;
	}
    
    public void setBulkExportService(BulkExportService bulkExportService) {
    	this.bulkExportService = bulkExportService;
    }
    
    public void setBulkExportStatus(WriteableBulkExportStatus bulkExportStatus) {
    	this.bulkExportStatus = bulkExportStatus;
    }
    
    public void setBulkExport(BulkExport bulkExport) {
    	this.bulkExport = bulkExport;
    }
}
