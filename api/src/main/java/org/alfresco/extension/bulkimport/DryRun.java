package org.alfresco.extension.bulkimport;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.extension.bulkimport.source.BulkImportItem;
import org.alfresco.extension.bulkimport.source.BulkImportItemVersion;
import org.apache.commons.lang3.time.DateFormatUtils;

import com.armedia.commons.utilities.Tools;

public class DryRun<V extends BulkImportItemVersion> {
	
	public static class Fault
	{
		private final Date timeStamp;
		private final String info;
		
		public Fault(String info) {
			this.timeStamp = new Date();
			this.info = info;
		}

		public Date getTimeStamp()
		{
			return this.timeStamp;
		}
		
		public String getTimeStampStr()
		{
			return DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.format(this.timeStamp);
		}
		
		public String getInfo()
		{
			return this.info;
		}
	}

	private final BulkImportItem<V> item;
	
	private final List<Fault> itemFaults = new ArrayList<Fault>();
	
	private final Map<BigDecimal, List<Fault>> versionFaults = new LinkedHashMap<>();

	public DryRun(BulkImportItem<V> item)
	{
		this.item = item;
	}
	
	public BulkImportItem<V> getItem()
	{
		return this.item;
	}

	public void addItemFault(String message)
	{
		itemFaults.add(new Fault(message));
	}

	public void addItemFault(Throwable exception)
	{
		addItemFault(Tools.dumpStackTrace(exception));
	}

	public void addVersionFault(V version, String message)
	{
		List<Fault> c = versionFaults.get(version.getVersionNumber());
		if (c == null)
		{
			c = new ArrayList<>();
			versionFaults.put(version.getVersionNumber(), c);
		}
		c.add(new Fault(message));
	}

	public void addVersionFault(V version, Throwable exception)
	{
		addVersionFault(version, Tools.dumpStackTrace(exception));
	}

	public boolean hasFaults()
	{
		return (!itemFaults.isEmpty() || !versionFaults.isEmpty());
	}

	public Collection<Fault> getItemFaults()
	{
		return Collections.unmodifiableCollection(this.itemFaults);
	}

	public Set<BigDecimal> getVersionFaults()
	{
		return Collections.unmodifiableSet(this.versionFaults.keySet());
	}

	public Collection<Fault> getVersionFaults(BigDecimal version)
	{
		return Collections.unmodifiableCollection(this.versionFaults.get(version));
	}
}