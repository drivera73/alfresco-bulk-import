package org.alfresco.extension.bulkimport.source.fs.cache;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.alfresco.extension.bulkimport.source.fs.FilesystemBulkImportItem;
import org.alfresco.extension.bulkimport.source.fs.FilesystemBulkImportItemVersion;
import org.alfresco.extension.bulkimport.source.fs.MetadataLoader;
import org.alfresco.repo.content.ContentStore;
import org.alfresco.service.ServiceRegistry;
import org.apache.commons.logging.Log;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder =
	{
		"directory", "sourceName", "sourcePath", "targetName", "targetPath", "versions"
	}
)
@XmlRootElement(name = "item")
public class CacheItem
{
	@XmlElement(name = "directory", required = true)
	protected boolean directory;

	@XmlElement(name = "sourceName", required = true)
	protected String sourceName;

	@XmlElement(name = "sourcePath", required = false)
	protected String sourcePath;

	@XmlElement(name = "targetName", required = true)
	protected String targetName;

	@XmlElement(name = "targetPath", required = false)
	protected String targetPath;

	@XmlElementWrapper(name = "versions", required = true)
	@XmlElement(name = "version", required = true)
	protected List<CacheItemVersion> versions;

	public FilesystemBulkImportItem generate(final File            basePath,
                                             final ServiceRegistry serviceRegistry,
                                             final ContentStore    contentStore,
                                             final MetadataLoader  metadataLoader)
	{
		NavigableSet<FilesystemBulkImportItemVersion> versions = new TreeSet<>();

		if (this.versions != null)
		{
			for (CacheItemVersion v : this.versions)
			{
				versions.add(v.generate(basePath, serviceRegistry, contentStore, metadataLoader));
			}
		}

		return new FilesystemBulkImportItem(sourceName, targetName, directory, sourcePath, targetPath, versions);
	}

	public static File canonicalize(File f, Log log)
	{
		if (f == null) return null;
		try
		{
			f = f.getCanonicalFile();
		}
		catch (IOException e)
		{
			if ((log != null) && log.isDebugEnabled())
			{
				log.debug(String.format("Failed to canonicalize the file [%s]", f.getPath()), e);
			}
		}
		finally
		{
			f = f.getAbsoluteFile();
		}
		return f;
	}

	public static File canonicalize(File f)
	{
		return canonicalize(f, null);
	}

	@Override
	public String toString() {
		return String.format("CacheItem [directory=%s, sourcePath=%s, sourceName=%s, targetPath=%s, targetName=%s, versions=%s]",
			directory, sourcePath, sourceName, targetPath, targetName, versions);
	}
}