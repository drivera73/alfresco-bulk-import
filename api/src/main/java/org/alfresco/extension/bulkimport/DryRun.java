package org.alfresco.extension.bulkimport;

import java.io.PrintWriter;
import java.io.StringWriter;
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
import org.alfresco.extension.bulkimport.source.BulkImportTools;
import org.apache.commons.lang3.StringUtils;
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
	private String errorReport = null;

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
		errorReport = null;
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
		errorReport = null;
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
	
	private String generateErrorReport()
	{
		if (!hasFaults())
		{
			return "No Faults";
		}

		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		
		pw.printf("Source Path: [%s]%n", BulkImportTools.getCompleteSourcePath(this.item));
		pw.printf("Target Path: [%s]%n", BulkImportTools.getCompleteTargetPath(this.item));
		pw.printf("%n");
		if (!this.itemFaults.isEmpty())
		{
			pw.printf("General Faults:%n");
			pw.printf("%s%n", StringUtils.repeat('=', 40));
			for (Fault f : this.itemFaults)
			{
				pw.printf("\t%s\t%s%n", f.getTimeStampStr(), f.getInfo());
			}
			pw.printf("%s%n%n", StringUtils.repeat('=', 40));
		}
		if (!versionFaults.isEmpty())
		{
			pw.printf("Version Faults:%n");
			pw.printf("%s%n", StringUtils.repeat('=', 40));
			for (BigDecimal v : versionFaults.keySet())
			{
				pw.printf("%n\tVersion %s:%n", v);
				pw.printf("\t%s%n", StringUtils.repeat('=', 30));
				for (Fault f : versionFaults.get(v))
				{
					pw.printf("\t\t%s\t%s%n", f.getTimeStampStr(), f.getInfo());
				}
				pw.printf("\t%s%n", StringUtils.repeat('=', 30));
			}
		}
		return sw.toString();
	}

	public String getErrorReport()
	{
		if (this.errorReport == null)
		{
			this.errorReport = generateErrorReport();
		}
		return this.errorReport;
	}
}