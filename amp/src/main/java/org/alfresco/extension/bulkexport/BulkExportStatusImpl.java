package org.alfresco.extension.bulkexport;

import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

public class BulkExportStatusImpl implements WriteableBulkExportStatus {
	private String initiatingUserId = null;
	private String currentlyProcessing = null;
	private ConcurrentMap<String, AtomicLong> targetCounters = new ConcurrentHashMap<>(16);  // Start with a reasonable number of target counter slots
	private String source;
	private Date endDate;
	private Long endNs;
	private Date startDate;
	private Long startNs;
	
	private volatile ProcessingState     state                 = ProcessingState.NEVER_RUN;
    private volatile ProcessingState     priorState            = state;
	
	private final static long NS_PER_MICROSECOND = 1000L;
    private final static long NS_PER_MILLISECOND = NS_PER_MICROSECOND * 1000L;
    private final static long NS_PER_SECOND      = NS_PER_MILLISECOND * 1000L;
    private final static long NS_PER_MINUTE      = NS_PER_SECOND * 60L;
    private final static long NS_PER_HOUR        = NS_PER_MINUTE * 60L;
    private final static long NS_PER_DAY         = NS_PER_HOUR * 24;
	
	@Override
	public void setCurrentlyProcessing(String name) {
		this.currentlyProcessing = name;
	}

	@Override
	public void preregisterTargetCounters(String[] counterNames) {
		if (counterNames != null)
        {
            for (final String counterName : counterNames)
            {
                targetCounters.putIfAbsent(counterName, new AtomicLong(0));
            }
        }
	}

	@Override
	public void incrementTargetCounter(final String counterName) {
		incrementTargetCounter(counterName, 1);
	}

	@Override
	public void incrementTargetCounter(final String counterName, final long value) {
		final AtomicLong previous = targetCounters.putIfAbsent(counterName, new AtomicLong(value));
        
        if (previous != null)
        {
            previous.addAndGet(value);
        }
	}

	@Override
	public String getInitiatingUserId() {
		return(initiatingUserId );
	}

	@Override
	public String getSourceName() {
		return (source);
	}

	@Override
	public Date getStartDate() {
		return (copyDate(startDate));
	}

	@Override
	public Date getEndDate() {
		return (copyDate(endDate));
	}

	@Override
	public Long getDurationInNs() {
		Long result = null;
        
        if (startNs != null)
        {
            if (endNs != null)
            {
                result = Long.valueOf(endNs - startNs);
            }
            else
            {
                result = Long.valueOf(System.nanoTime() - startNs);
            }
        }
        
        return(result);
	}

	@Override
	public String getDuration() {
		return(getHumanReadableDuration(getDurationInNs()));
	}

	@Override
	public Set<String> getTargetCounterNames() {
		return(Collections.unmodifiableSet(new TreeSet<>(targetCounters.keySet())));
	}

	@Override
	public Long getTargetCounter(String counterName) {
		return(targetCounters.get(counterName) == null ? null : targetCounters.get(counterName).get());
	}

	@Override
	public void postProcessingStarted(String initiatingId, String source) {
		this.state = ProcessingState.PROCESSING;
		this.source = source;
		this.initiatingUserId = initiatingId;
		this.targetCounters.clear();
		preregisterTargetCounters(DEFAULT_TARGET_COUNTERS);
		this.endDate = null;
		this.endNs = null;
		this.startDate = new Date();
		this.startNs = Long.valueOf(System.nanoTime());
	}
	
	@Override
	public void postProcessingComplete() 
	{
		this.endNs = Long.valueOf(System.nanoTime());
        this.endDate = new Date();
        this.state = ProcessingState.SUCCEEDED;
	}

	// Private helper methods
    private final Date copyDate(final Date date)
    {
        // Defensively copy the date to prevent shenanigans.  Immutability ftw...
        Date result = null;
        
        if (date != null)
        {
            result = new Date(date.getTime());
        }
        
        return(result);
    }
    
    /**
     * @param durationInNs A duration in nanoseconds (i.e. from System.nanoTime()) <i>(may be null)</i>.
     * @return A human readable string representing that duration as "Ud Vh Wm Xs Y.Zms", "<unknown>" if the duration is null.
     */
    public final static String getHumanReadableDuration(final Long durationInNs)
    {
        return(getHumanReadableDuration(durationInNs, true));
    }
    
    /**
     * @param durationInNs A duration in nanoseconds (i.e. from System.nanoTime()) <i>(may be null)</i>.
     * @param includeMs    Flag indicating whether to include milliseconds or not.
     * @return A human readable string representing that duration as "Ud Vh Wm Xs Y.Zms", "<unknown>" if the duration is null.
     */
    public final static String getHumanReadableDuration(final Long durationInNs, final boolean includeMs)
    {
        String result = null;
        
        if (durationInNs == null)
        {
            result = "<unknown>";
        }
        else
        {
            result = _getHumanReadableDuration(durationInNs.longValue(), includeMs);
        }
        
        return(result);
    }
    
    /**
     * @param durationInNs A duration in nanoseconds (i.e. from System.nanoTime()).
     * @param includeMs    Flag indicating whether to include milliseconds or not.
     * @return A human readable string representing that duration as "Ud Vh Wm Xs Y.Zms".
     */
    public final static String _getHumanReadableDuration(final long durationInNs, final boolean includeMs)
    {
        String result = null;
        
        if (durationInNs <= 0)
        {
            result = "0d 0h 0m 0s" + (includeMs ? " 0.0ms" : "");
        }
        else
        {
            int days         = (int)(durationInNs / NS_PER_DAY);
            int hours        = (int)((durationInNs / NS_PER_HOUR)        % 24);
            int minutes      = (int)((durationInNs / NS_PER_MINUTE)      % 60);
            int seconds      = (int)((durationInNs / NS_PER_SECOND)      % 60);
            int milliseconds = (int)((durationInNs / NS_PER_MILLISECOND) % 1000);
            int microseconds = (int)((durationInNs / NS_PER_MICROSECOND) % 1000);

            // Ternaries, how I love thee...  ;-)
            result = (days > 0                              ? days    + "d " : "") +
                     (days > 0 || hours > 0                 ? String.format("%02dh ", hours)   : "") +
                     (days > 0 || hours > 0 || minutes > 0  ? String.format("%02dm ", minutes) : "") +
                     String.format("%02ds", seconds) +
                     (includeMs ? " " + String.format("%03d.%03dms", milliseconds, microseconds) : "");
        }
        
        return(result);
    }

    @Override public boolean             inProgress()            { return(ProcessingState.PROCESSING.equals(state) || ProcessingState.PAUSED.equals(state) || ProcessingState.STOPPING.equals(state)); }
    @Override public boolean             isPaused()              { return(ProcessingState.PAUSED.equals(state)); }
    @Override public boolean             isStopping()            { return(ProcessingState.STOPPING.equals(state)); }
    @Override public boolean             neverRun()              { return(ProcessingState.NEVER_RUN.equals(state)); }
    @Override public boolean             succeeded()             { return(ProcessingState.SUCCEEDED.equals(state)); }
    @Override public boolean             failed()                { return(ProcessingState.FAILED.equals(state)); }
    @Override public boolean             stopped()               { return(ProcessingState.STOPPED.equals(state)); }
	
	// Private enum for tracking current execution state
    private enum ProcessingState
    {
        PROCESSING, PAUSED, STOPPING,  // In-progress states
        NEVER_RUN, SUCCEEDED, FAILED, STOPPED;  // Not in-progress states
        
        @Override
        public String toString()
        {
            String result = null;
            
            switch (this)
            {
                case PROCESSING:
                    result = "Processing";
                    break;

                case PAUSED:
                    result = "Paused";
                    break;
                    
                case STOPPING:
                    result = "Stopping";
                    break;
                    
                case NEVER_RUN:
                    result = "Never run";
                    break;

                case SUCCEEDED:
                    result = "Succeeded";
                    break;
                    
                case FAILED:
                    result = "Failed";
                    break;
                    
                case STOPPED:
                    result = "Stopped";
                    break;
            }
            
            return(result);
        }        
    }

	@Override
	public String getCurrentStatus() {
		return this.state.toString();
	}
}
