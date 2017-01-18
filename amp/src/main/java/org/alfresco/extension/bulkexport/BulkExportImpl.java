package org.alfresco.extension.bulkexport;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class BulkExportImpl implements BulkExport {
	private static Log LOG = LogFactory.getLog(BulkExportImpl.class);

	private Thread processorThread;
	private final static String PROCESSOR_THREAD_NAME = "BulkExport-Processor";
	private WriteableBulkExportStatus bulkExportStatus = null;

	@Override
	public void start(final String userId, final String source, final boolean truncatePath, final String exportLocation,
			final boolean scapeExported, final boolean exportVersions, final boolean revisionHead,
			final boolean useNodeCache, final boolean includeContent, final boolean foldersOnly, final String checksum,
			final boolean includeMetadata, final List<String> folderProperties, final List<String> documentProperties,
			final BulkExportService bulkExportService, final WriteableBulkExportStatus bulkExportStatus, int batchSize, int threadCount) {
		LOG.debug("Starting Bulk Export thread");
		this.bulkExportStatus = bulkExportStatus;
		processorThread = new Thread(new BulkExportProcessor(userId, source, truncatePath, exportLocation,
				scapeExported, exportVersions, revisionHead, useNodeCache, includeContent, foldersOnly, checksum,
				includeMetadata, folderProperties, documentProperties, bulkExportService, bulkExportStatus, batchSize, threadCount));
		processorThread.setName(PROCESSOR_THREAD_NAME);
		processorThread.setDaemon(true);
		processorThread.start();
	}

	@Override
	public BulkExportStatus getStatus() {
		return this.bulkExportStatus;
	}
}
