package org.alfresco.extension.bulkexport;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.alfresco.extension.bulkexport.controller.CacheGeneratedException;
import org.alfresco.extension.bulkexport.controller.Engine;
import org.alfresco.extension.bulkexport.dao.AlfrescoExportDaoImpl;
import org.alfresco.extension.bulkexport.model.FileFolder;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.PropertyCheck;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.google.common.collect.Lists;

public class BulkExportServiceImpl implements BulkExportService {

	private static Log LOG = LogFactory.getLog(BulkExportServiceImpl.class);
	private ServiceRegistry serviceRegistry;
	private WriteableBulkExportStatus bulkExportStatus;    

	public void init() {
		LOG.debug("Initializing BulkExportService");

		PropertyCheck.mandatory(this, "serviceRegistry", serviceRegistry);
		PropertyCheck.mandatory(this, "nodeService", bulkExportStatus);

	}
	
	@Override
	public int processBulkExport(String userId, String source, boolean truncatePath, String exportLocation,
			boolean scapeExported, boolean exportVersions, boolean revisionHead, boolean useNodeCache,
			boolean includeContent, boolean foldersOnly, String checksum, boolean includeMetadata,
			List<String> folderProperties, List<String> documentProperties, int batchSize, int threadCount) {
		//init variables
		LOG.info("Establish a few variables");
		LOG.info("batchSize: " + batchSize);
		LOG.info("threadCount: " + threadCount);
		LOG.info("source: " + source);
		LOG.info("exportLocation: " + exportLocation);
		LOG.info("truncatePath: " + truncatePath);
		LOG.info("scapeExported: " + scapeExported);
		LOG.info("exportVersions: " + exportVersions);
		LOG.info("revisionHead: " + revisionHead);
		LOG.info("useNodeCache: " + useNodeCache);
		LOG.info("includeContent: " + includeContent);
		LOG.info("foldersOnly: " + foldersOnly);
		LOG.info("checksum: " + checksum);
		LOG.info("includeMetadata: " + includeMetadata);
        AlfrescoExportDaoImpl dao = new AlfrescoExportDaoImpl(this.serviceRegistry);
        dao.setTruncatePath(truncatePath);
        FileFolder fileFolder = new FileFolder(exportLocation, scapeExported);
        LOG.debug("Create new engine");
        Engine engine = new Engine(dao, fileFolder, exportVersions, revisionHead, useNodeCache, includeContent, documentProperties, folderProperties, foldersOnly, checksum, bulkExportStatus);
        LOG.debug("New Engine created");
        try {
        	LOG.debug("Get NodeRef from dao");
	        NodeRef nf = dao.getNodeRef(source);
	        LOG.debug("Export from source: " + source);
	        List<NodeRef> documentNodes = engine.execute(nf);
	        
	        LOG.info("No. of Document nodes: " + documentNodes.size());
	        List<List<NodeRef>> batches = Lists.partition(documentNodes, batchSize);
	        LOG.info("No. of batchs: " + batches.size());
	        bulkExportStatus.incrementTargetCounter(BulkExportStatus.TARGET_COUNTER_DOCUMENT_BATCHES_SUBMITTED, batches.size());
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            for (List<NodeRef> batch : batches) {
            	workerThread(executor, userId, nf, batch, truncatePath, exportLocation,
            			scapeExported, exportVersions, revisionHead, useNodeCache,
            			includeContent, foldersOnly, checksum, includeMetadata,
            			folderProperties, documentProperties, bulkExportStatus);
            }
            executor.shutdown();
            
         // Wait until all threads are finish 

            while (!executor.isTerminated()) { } System.out.println("\nFinished all threads"); 
            
        } catch (CacheGeneratedException ex) {
        	LOG.error("Cache Exception: " + ex.getMessage());
        } catch (Exception ex) {
        	LOG.error("Exception: " + ex.getMessage());
        }
		return 0;
	}
	
	private void workerThread(Executor executor, String userId, NodeRef source, List<NodeRef> batch, boolean truncatePath, String exportLocation,
			boolean scapeExported, boolean exportVersions, boolean revisionHead, boolean useNodeCache,
			boolean includeContent, boolean foldersOnly, String checksum, boolean includeMetadata,
			List<String> folderProperties, List<String> documentProperties, WriteableBulkExportStatus bulkExportStatus) {
		LOG.info("COLIN----" + truncatePath);
    	BulkExportWorkerThread bewt = new BulkExportWorkerThread(userId, source, batch, this.serviceRegistry, exportLocation, scapeExported, exportVersions, revisionHead, useNodeCache, includeContent, documentProperties, folderProperties, foldersOnly, checksum, truncatePath, bulkExportStatus);
    	executor.execute(bewt);
    }

	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}
	
	public void setBulkExportStatus(WriteableBulkExportStatus bulkExportStatus) {
		this.bulkExportStatus = bulkExportStatus;
	}
}
