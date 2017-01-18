package org.alfresco.extension.bulkexport;

import java.util.List;

public interface BulkExportService {
	public int processBulkExport(String userId, String source, boolean truncatePath, String exportLocation,
			boolean scapeExported, boolean exportVersions, boolean revisionHead, boolean useNodeCache,
			boolean includeContent, boolean foldersOnly, String checksum, boolean includeMetadata,
			List<String> folderProperties, List<String> documentProperties, int batchSize, int threadCount);
}
