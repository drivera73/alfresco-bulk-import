package org.alfresco.extension.bulkexport;

import java.util.List;

public interface BulkExport {
	public void start(final String userId, final String source, final boolean truncatePath, final String exportLocation,
			final boolean scapeExported, final boolean exportVersions, final boolean revisionHead,
			final boolean useNodeCache, final boolean includeContent, final boolean foldersOnly, final String checksum,
			final boolean includeMetadata, final List<String> folderProperties, final List<String> documentProperties,
			final BulkExportService bulkExportService, final WriteableBulkExportStatus bulkExportStatus, int batchSize, int threadCount);

	public BulkExportStatus getStatus();
}
