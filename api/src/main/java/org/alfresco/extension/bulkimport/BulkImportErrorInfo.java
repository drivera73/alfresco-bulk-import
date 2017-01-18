package org.alfresco.extension.bulkimport;

import java.util.Date;

import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.lang3.time.DateFormatUtils;

import com.armedia.commons.utilities.Tools;

public class BulkImportErrorInfo
{
	private final Date timeStamp;
	private final String item;
	private final Throwable error;
	
	private static String getString(NodeRef item)
	{
		if (item == null) throw new IllegalArgumentException("Must provide an identifier for the item that raised the error");
		return item.toString();
	}

	public BulkImportErrorInfo(Date timeStamp, String item, Throwable error)
	{
		if (item == null) throw new IllegalArgumentException("Must provide an identifier for the item that raised the error");
		if (error == null) throw new IllegalArgumentException("Must provide the error being raised");
		this.timeStamp = (timeStamp == null ? new Date() : timeStamp);
		this.item = item;
		this.error = error;
	}

	public BulkImportErrorInfo(String item, Throwable error)
	{
		this(null, item, error);
	}

	public BulkImportErrorInfo(Date timeStamp, NodeRef item, Throwable error)
	{
		this(timeStamp, getString(item), error);
	}

	public BulkImportErrorInfo(NodeRef item, Throwable error)
	{
		this(null, getString(item), error);
	}
	
	public final String getItem()
	{
		return this.item;
	}
	
	public final Date getTimeStamp()
	{
		return this.timeStamp;
	}

	public final String getTimeStampStr()
	{
		return DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.format(timeStamp);
	}
	
	public final Throwable getError()
	{
		return this.error;
	}

	public final String getErrorStr()
	{
		if (DryRunException.class.isInstance(this.error))
		{
			return DryRunException.class.cast(error).getDryRun().getErrorReport();
		}
		return Tools.dumpStackTrace(error);
	}
}