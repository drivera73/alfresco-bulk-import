package org.alfresco.extension.bulkexport;

import java.util.Date;
import java.util.Set;

public interface BulkExportStatus {
	public final static String TARGET_COUNTER_TOTAL_NODES_SUBMITTED         = "Nodes submitted";
	public final static String TARGET_COUNTER_TOTAL_NODES_COMPLETE          = "Nodes completed";
	
	public final static String TARGET_COUNTER_TOTAL_FOLDERS_SUBMITTED		= "Folders submitted";
	public final static String TARGET_COUNTER_TOTAL_FOLDERS_COMPLETE		= "Folders completed";
	public final static String TARGET_COUNTER_TOTAL_FOLDERS_ERRORS		    = "Folders errors";
	public final static String TARGET_COUNTER_TOTAL_FOLDERS_SKIPPED		    = "Folders skipped";
	public final static String TARGET_COUNTER_TOTAL_FOLDERS_UNKNOWN         = "Folders unknown";
	public final static String TARGET_COUNTER_TOTAL_FOLDERS_METADATA_COMPLETE		= "Folders metadata completed";
	public final static String TARGET_COUNTER_TOTAL_FOLDERS_METADATA_ERRORS		    = "Folders metadata errors";
	public final static String TARGET_COUNTER_TOTAL_FOLDERS_METADATA_SKIPPED		= "Folders metadata skipped";
	public final static String TARGET_COUNTER_TOTAL_FOLDERS_METADATA_UNKNOWN        = "Folders metadata unknown";
	
	public final static String TARGET_COUNTER_TOTAL_DOCUMENTS_SUBMITTED		= "Documents submitted";
	public final static String TARGET_COUNTER_TOTAL_DOCUMENTS_COMPLETE		= "Documents completed";
	public final static String TARGET_COUNTER_TOTAL_DOCUMENTS_ERRORS		= "Documents errors";
	public final static String TARGET_COUNTER_TOTAL_DOCUMENTS_SKIPPED		= "Documents skipped";
	public final static String TARGET_COUNTER_TOTAL_DOCUMENTS_UNKNOWN       = "Documents unknown";
	public final static String TARGET_COUNTER_TOTAL_DOCUMENTS_METADATA_COMPLETE		= "Documents metadata completed";
	public final static String TARGET_COUNTER_TOTAL_DOCUMENTS_METADATA_ERRORS		= "Documents metadata errors";
	public final static String TARGET_COUNTER_TOTAL_DOCUMENTS_METADATA_SKIPPED		= "Documents metadata skipped";
	public final static String TARGET_COUNTER_TOTAL_DOCUMENTS_METADATA_UNKNOWN      = "Documents metadata unknown";
	
	public final static String TARGET_COUNTER_TOTAL_DOCUMENTS_VERSION_SUBMITTED		= "Versions submitted";
	public final static String TARGET_COUNTER_TOTAL_DOCUMENTS_VERSION_COMPLETE		= "Versions completed";
	public final static String TARGET_COUNTER_TOTAL_DOCUMENTS_VERSION_ERRORS		= "Versions errors";
	public final static String TARGET_COUNTER_TOTAL_DOCUMENTS_VERSION_SKIPPED		= "Versions skipped";
	public final static String TARGET_COUNTER_TOTAL_DOCUMENTS_VERSION_UNKNOWN       = "Versions unknown";
	public final static String TARGET_COUNTER_TOTAL_DOCUMENTS_VERSION_METADATA_COMPLETE		= "Versions metadata completed";
	public final static String TARGET_COUNTER_TOTAL_DOCUMENTS_VERSION_METADATA_ERRORS		= "Versions metadata errors";
	public final static String TARGET_COUNTER_TOTAL_DOCUMENTS_VERSION_METADATA_SKIPPED		= "Versions metadata skipped";
	public final static String TARGET_COUNTER_TOTAL_DOCUMENTS_VERSION_METADATA_UNKNOWN      = "Versions metadata unknown";
	
	public final static String TARGET_COUNTER_FOLDER_BATCHES_SUBMITTED      = "Folder Batches submitted";
	public final static String TARGET_COUNTER_FOLDER_BATCHES_COMPLETE       = "Folder Batches completed";
	public final static String TARGET_COUNTER_DOCUMENT_BATCHES_SUBMITTED    = "Document Batches submitted";
	public final static String TARGET_COUNTER_DOCUMENT_BATCHES_COMPLETE     = "Document Batches completed";

	
	public final static String[] DEFAULT_TARGET_COUNTERS = { TARGET_COUNTER_TOTAL_NODES_SUBMITTED,
			TARGET_COUNTER_TOTAL_NODES_COMPLETE,
			TARGET_COUNTER_TOTAL_FOLDERS_SUBMITTED,
			TARGET_COUNTER_TOTAL_FOLDERS_COMPLETE,
			TARGET_COUNTER_TOTAL_FOLDERS_ERRORS,
			TARGET_COUNTER_TOTAL_FOLDERS_SKIPPED,
			TARGET_COUNTER_TOTAL_FOLDERS_METADATA_COMPLETE,
			TARGET_COUNTER_TOTAL_FOLDERS_METADATA_ERRORS,
			TARGET_COUNTER_TOTAL_FOLDERS_METADATA_SKIPPED,
			TARGET_COUNTER_TOTAL_FOLDERS_METADATA_UNKNOWN,
			TARGET_COUNTER_FOLDER_BATCHES_SUBMITTED,
			TARGET_COUNTER_FOLDER_BATCHES_COMPLETE,
			TARGET_COUNTER_TOTAL_DOCUMENTS_SUBMITTED,
			TARGET_COUNTER_TOTAL_DOCUMENTS_COMPLETE,
			TARGET_COUNTER_TOTAL_DOCUMENTS_ERRORS,
			TARGET_COUNTER_TOTAL_DOCUMENTS_SKIPPED,
			TARGET_COUNTER_TOTAL_DOCUMENTS_METADATA_COMPLETE,
			TARGET_COUNTER_TOTAL_DOCUMENTS_METADATA_ERRORS,
			TARGET_COUNTER_TOTAL_DOCUMENTS_METADATA_SKIPPED,
			TARGET_COUNTER_TOTAL_DOCUMENTS_METADATA_UNKNOWN,
			TARGET_COUNTER_TOTAL_DOCUMENTS_VERSION_SUBMITTED,
			TARGET_COUNTER_TOTAL_DOCUMENTS_VERSION_COMPLETE,
			TARGET_COUNTER_TOTAL_DOCUMENTS_VERSION_ERRORS,
			TARGET_COUNTER_TOTAL_DOCUMENTS_VERSION_SKIPPED,
			TARGET_COUNTER_TOTAL_DOCUMENTS_VERSION_UNKNOWN,
			TARGET_COUNTER_TOTAL_DOCUMENTS_VERSION_METADATA_COMPLETE,
			TARGET_COUNTER_TOTAL_DOCUMENTS_VERSION_METADATA_ERRORS,
			TARGET_COUNTER_TOTAL_DOCUMENTS_VERSION_METADATA_SKIPPED,
			TARGET_COUNTER_TOTAL_DOCUMENTS_VERSION_METADATA_UNKNOWN,
			TARGET_COUNTER_DOCUMENT_BATCHES_SUBMITTED,
			TARGET_COUNTER_DOCUMENT_BATCHES_COMPLETE};
	
	/**
     * @return The userId of the person who initiated the import <i>(will be null if an import has never been run)</i>.
     */
    String getInitiatingUserId();

    /**
     * @return The name of the source used for the active (or previous) import <i>(will be null if an import has never been run)</i>.
     */
    String getSourceName();
    
    /**
     * @return The start date of the import <i>(will be null if an import has never been run)</i>.
     */
    Date getStartDate();
    
    /**
     * @return The end date of the import <i>(will be null if an import has not yet completed)</i>.
     */
    Date getEndDate();
    
    /**
     * @return The duration, in nanoseconds, of the import <i>(will be null if an import has never been run)</i>.
     */
    Long getDurationInNs();
    
    /**
     * @return The duration, in a human-readable textual representation, of the import <i>(will be null if an import has never been run)</i>.
     */
    String getDuration();
    
    /**
     * @return The target counter names, in sorted order <i>(may be null or empty)<i>.
     */
    Set<String> getTargetCounterNames();
    
    /**
     * @param counterName The name of the target counter to retrieve <i>(must not be null, empty or blank)</i>.
     * @return The current value of that counter <i>(will be null if an import isn't in process, or if the counterName doesn't exist)</i>.
     */
    Long getTargetCounter(String counterName);
    
    boolean inProgress();
    boolean isPaused();
    boolean isStopping();
    boolean neverRun();
    boolean succeeded();
    boolean failed();
    boolean stopped();
    String getCurrentStatus();
}
