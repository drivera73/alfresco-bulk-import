package org.alfresco.extension.bulkimport.source.fs.cache;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.Unmarshaller;
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
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder =
	{
		"directory", "name", "fsRelativePath", "relativePath", "sourceName", "sourcePath", "targetName", "targetPath", "versions"
	}
)
@XmlRootElement(name = "item")
public class CacheItem
{
	private static final Pattern VERSION_SUFFIX = Pattern.compile("^.*(\\.v(\\d+(?:\\.\\d+)?))$");

	private static final String METADATA_SUFFIX = ".metadata.properties.xml";

	private static final int METADATA_SUFFIX_LENGTH = METADATA_SUFFIX.length();

	@XmlElement(name = "directory", required = true)
	protected boolean directory;

	/* Support the old version scan index */
	@XmlElement(name = "name", required = false)
	protected String name;

	@XmlElement(name = "fsRelativePath", required = false)
	protected String fsRelativePath;

	@XmlElement(name = "relativePath", required = false)
	protected String relativePath;

	/* Support the new version scan index */
	@XmlElement(name = "sourceName", required = false)
	protected String sourceName;

	@XmlElement(name = "sourcePath", required = false)
	protected String sourcePath;

	@XmlElement(name = "targetName", required = false)
	protected String targetName;

	@XmlElement(name = "targetPath", required = false)
	protected String targetPath;

	@XmlElementWrapper(name = "versions", required = true)
	@XmlElement(name = "version", required = true)
	protected List<CacheItemVersion> versions;

    protected void afterUnmarshal(Unmarshaller u, Object parent)
    {
        if (!StringUtils.isEmpty(name) && ((fsRelativePath != null) || (relativePath != null)))
        {
        	populateFromOldVersion();
        }
    }
    
    private String extractName(String name)
    {
		// The source name will be the basename for the source file (minus extension)
		String sourceName = FilenameUtils.getName(name);

		// If it ends with vXX or vXX.XX, remove the version tag...
		Matcher m = VERSION_SUFFIX.matcher(sourceName);
		if (m.matches())
		{
			sourceName = sourceName.substring(0, m.start(1));
		}

		// If it's a metadata file, then remove the suffix
		if (sourceName.endsWith(METADATA_SUFFIX))
		{
			sourceName = sourceName.substring(0, sourceName.length() - METADATA_SUFFIX_LENGTH);
		}

		// Return the resulting name
		return sourceName;
    }

    private void populateFromOldVersion()
    {
    	// Old XML version... convert!

    	// It's OK to assume we'll have at least one version - that's how it's built
		CacheItemVersion v = versions.get(0);

		// It'll have at least one of content and metadata, else what's the reason for its existence?
		sourceName = extractName(v.getContent());
		if (StringUtils.isEmpty(sourceName)) sourceName = extractName(v.getMetadata());

    	sourcePath = fsRelativePath;

    	// These two are straight shots
    	targetName = name;
    	targetPath = relativePath;

    	// Remove old values in case someone marshalls the object
    	name = null;
    	fsRelativePath = null;
    	relativePath = null;
    }

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