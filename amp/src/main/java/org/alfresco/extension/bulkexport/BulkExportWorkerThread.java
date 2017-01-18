package org.alfresco.extension.bulkexport;

import java.util.List;

import org.alfresco.extension.bulkexport.controller.Engine;
import org.alfresco.extension.bulkexport.dao.AlfrescoExportDao;
import org.alfresco.extension.bulkexport.dao.AlfrescoExportDaoImpl;
import org.alfresco.extension.bulkexport.model.FileFolder;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class BulkExportWorkerThread implements Runnable {

	Log log = LogFactory.getLog(BulkExportWorkerThread.class);
	
	List<NodeRef> batch;
	FileFolder fileFolder;
	boolean exportVersions;
	boolean revisionHead;
	boolean useNodeCache;
	boolean includeContent;
	List<String> documentProperties;
	List<String> folderProperties;
	boolean foldersOnly;
	String checksum;
	boolean truncatePath;
	boolean scapeExported;
	String exportLocation;
	String userId;
	NodeRef source;
	WriteableBulkExportStatus bulkExportStatus;
	ServiceRegistry serviceRegistry;
	
	public BulkExportWorkerThread(final String userId, final NodeRef source, final List<NodeRef> batch, final ServiceRegistry serviceRegistry, final String exportLocation, final boolean scapeExported,
			final boolean exportVersions, final boolean revisionHead, final boolean useNodeCache,
			final boolean includeContent, final List<String> documentProperties, final List<String> folderProperties, final boolean foldersOnly,
			final String checksum, final boolean truncatePath, final WriteableBulkExportStatus bulkExportStatus) {
		this.batch = batch;
		this.exportVersions = exportVersions;
		this.revisionHead = revisionHead;
		this.useNodeCache = useNodeCache;
		this.includeContent = includeContent;
		this.documentProperties = documentProperties;
		this.folderProperties = folderProperties;
		this.foldersOnly = foldersOnly;
		this.checksum = checksum;
		this.truncatePath = truncatePath;
		this.bulkExportStatus = bulkExportStatus;
		this.serviceRegistry = serviceRegistry;
		this.scapeExported = scapeExported;
		this.exportLocation = exportLocation;
		this.userId = userId;
		this.source = source;
	}

	@Override
	public void run() {
		AuthenticationUtil.runAs(new RunAsWork<Object>()
        {
            @Override
            public Object doWork()
                throws Exception
            {
            	try {
        			AlfrescoExportDao dao = new AlfrescoExportDaoImpl(serviceRegistry);
        	        dao.setTruncatePath(truncatePath);
        	        FileFolder fileFolder = new FileFolder(exportLocation, scapeExported);
        			Engine engine = new Engine(dao, fileFolder, exportVersions, revisionHead, useNodeCache, includeContent, documentProperties, folderProperties, foldersOnly, checksum, bulkExportStatus);
        			engine.execute(batch, source);
        		} catch (Exception ex) {
        			ex.printStackTrace();
        		}
                return(null);
            }
        }, userId);

	}
}
