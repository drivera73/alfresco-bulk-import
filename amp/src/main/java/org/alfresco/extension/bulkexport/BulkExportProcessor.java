package org.alfresco.extension.bulkexport;

import java.util.List;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class BulkExportProcessor implements Runnable {
	private static Log LOG = LogFactory.getLog(BulkExportProcessor.class);

	private BulkExportService bulkExportService;
	private WriteableBulkExportStatus bulkExportStatus;

	private String userId = null;
	private String source;
	private boolean truncatePath;
	private String exportLocation;
	private boolean scapeExported;
	private boolean exportVersions;
	private boolean revisionHead;
	private boolean useNodeCache;
	private boolean includeContent;
	private List<String> documentProperties;
	private List<String> folderProperties;
	private boolean foldersOnly;
	private String checksum;
	private boolean includeMetadata;
	private int batchSize;
	private int threadCount;

	public BulkExportProcessor(final String userId, final String source, final boolean truncatePath,
			final String exportLocation, final boolean scapeExported, final boolean exportVersions,
			final boolean revisionHead, final boolean useNodeCache, final boolean includeContent,
			final boolean foldersOnly, final String checksum, final boolean includeMetadata,
			final List<String> folderProperties, final List<String> documentProperties,
			final BulkExportService bulkExportService, final WriteableBulkExportStatus bulkExportStatus, int batchSize, int threadCount) {
		this.userId = userId;
		this.source = source;
		this.truncatePath = truncatePath;
		this.exportLocation = exportLocation;
		this.scapeExported = scapeExported;
		this.exportVersions = exportVersions;
		this.revisionHead = revisionHead;
		this.useNodeCache = useNodeCache;
		this.includeContent = includeContent;
		this.documentProperties = documentProperties;
		this.folderProperties = folderProperties;
		this.foldersOnly = foldersOnly;
		this.checksum = checksum;
		this.includeMetadata = includeMetadata;
		this.bulkExportService = bulkExportService;
		this.bulkExportStatus = bulkExportStatus;
		this.batchSize = batchSize;
		this.threadCount = threadCount;
	}

	@Override
	public void run() {
		RunAsWork<Object> postProcessing = new RunAsWork<Object>() {
			public Object doWork() throws Exception {
				LOG.debug("Worker thread started");
				LOG.debug("UserId: " + userId);
				LOG.debug("Export Location: " + exportLocation);

				LOG.debug("Update Status");
				bulkExportStatus.postProcessingStarted(userId, source);

				LOG.debug("Invoke processBulkExport()");
				bulkExportService.processBulkExport(userId, source, truncatePath, exportLocation, scapeExported,
						exportVersions, revisionHead, useNodeCache, includeContent, foldersOnly, checksum,
						includeMetadata, folderProperties, documentProperties, batchSize, threadCount);
				bulkExportStatus.postProcessingComplete();
				return null;
			}
		};
		AuthenticationUtil.runAs(postProcessing, userId);
	}
}
