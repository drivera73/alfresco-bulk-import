package org.alfresco.extension.bulkexport;

public interface WriteableBulkExportStatus extends BulkExportStatus {
	void setCurrentlyProcessing(String name);
	void preregisterTargetCounters(String[] counterNames);
	void incrementTargetCounter(String counterName);
	void incrementTargetCounter(String counterName, long value);
	void postProcessingStarted(String initiatingId, final String source);
	void postProcessingComplete();
}
