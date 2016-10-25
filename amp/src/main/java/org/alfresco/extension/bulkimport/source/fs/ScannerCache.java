package org.alfresco.extension.bulkimport.source.fs;

import java.io.File;

import org.alfresco.extension.bulkimport.BulkImportCallback;
import org.alfresco.extension.bulkimport.source.BulkImportSourceStatus;

public interface ScannerCache {

	boolean scanFiles(File baseDir, BulkImportCallback callback, BulkImportSourceStatus importStatus)
		throws InterruptedException;

	boolean scanFolders(File baseDir, BulkImportCallback callback, BulkImportSourceStatus importStatus)
		throws InterruptedException;

}