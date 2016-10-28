package org.alfresco.extension.bulkimport.source.fs.cache;

import static org.alfresco.extension.bulkimport.util.LogUtils.debug;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.alfresco.extension.bulkimport.BulkImportCallback;
import org.alfresco.extension.bulkimport.source.BulkImportSourceStatus;
import org.alfresco.extension.bulkimport.source.fs.DirectoryAnalyser;
import org.alfresco.extension.bulkimport.source.fs.MetadataLoader;
import org.alfresco.extension.bulkimport.source.fs.ScannerCache;
import org.alfresco.repo.content.ContentStore;
import org.alfresco.service.ServiceRegistry;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class XmlScannerCache implements ScannerCache
{
    private final static String FILE_CACHE_FILE = "scan.files.xml";
    private final static String FOLDER_CACHE_FILE = "scan.folders.xml";

	protected final Log log = LogFactory.getLog(getClass());

	private final ServiceRegistry        serviceRegistry;
    private final ContentStore           configuredContentStore;
    private final MetadataLoader         metadataLoader;

	public XmlScannerCache(final ServiceRegistry serviceRegistry,
                        final ContentStore    configuredContentStore,
                        final MetadataLoader  metadataLoader)
	{
        this.serviceRegistry        = serviceRegistry;
        this.configuredContentStore = configuredContentStore;
        this.metadataLoader         = metadataLoader;
	}

	private final File getFile(File baseDirectory, String fileName)
	{
    	File f = (baseDirectory != null ? new File(baseDirectory, fileName) : new File(fileName));
    	try
    	{
    		f = f.getCanonicalFile();
    	}
    	catch (IOException e)
    	{
    		// Do nothing...
    		if (debug(log)) log.debug(String.format("Failed to canonicalize the path [%s]", f.getPath()), e);
    	}
    	finally
    	{
    		f = f.getAbsoluteFile();
    	}
    	if (!f.exists()) { if (debug(log)) {log.debug(String.format("Cache file [%s] does not exist", f.getAbsolutePath()));} ;        return null; }
    	if (!f.isFile()) { if (debug(log)) {log.debug(String.format("Cache file [%s] is not a regular file", f.getAbsolutePath()));} ; return null; }
    	if (!f.canRead()) { if (debug(log)) {log.debug(String.format("Cache file [%s] is not readable", f.getAbsolutePath()));} ;      return null; }

    	return f;
	}

	/* (non-Javadoc)
	 * @see org.alfresco.extension.bulkimport.source.fs.cache.ScannerCache#scanFiles(java.io.File, org.alfresco.extension.bulkimport.BulkImportCallback, org.alfresco.extension.bulkimport.source.BulkImportSourceStatus)
	 */
	@Override
	public final boolean scanFiles(final File baseDir, final BulkImportCallback callback, final BulkImportSourceStatus importStatus)
		throws InterruptedException
	{
		try
		{
			return scan(baseDir, FILE_CACHE_FILE, callback, importStatus, false);
		}
		catch (IOException | JAXBException | XMLStreamException e)
		{
			throw new RuntimeException(String.format("Failed to load the file cache from [%s]", baseDir.getAbsolutePath()), e);
		}
	}

	/* (non-Javadoc)
	 * @see org.alfresco.extension.bulkimport.source.fs.cache.ScannerCache#scanFolders(java.io.File, org.alfresco.extension.bulkimport.BulkImportCallback, org.alfresco.extension.bulkimport.source.BulkImportSourceStatus)
	 */
	@Override
	public final boolean scanFolders(final File baseDir, final BulkImportCallback callback, final BulkImportSourceStatus importStatus)
		throws InterruptedException
	{
		try
		{
			return scan(baseDir, FOLDER_CACHE_FILE, callback, importStatus, true);
		}
		catch (IOException | JAXBException | XMLStreamException e)
		{
			throw new RuntimeException(String.format("Failed to load the folder cache at [%s]", baseDir.getAbsolutePath()), e);
		}
	}

	protected void process(File baseDirectory, CacheItem cacheItem, BulkImportCallback callback) throws InterruptedException
	{
		callback.submit(cacheItem.generate(baseDirectory, serviceRegistry, configuredContentStore, metadataLoader));
	}

	private void showProgress(long start, long count, String cacheName, boolean end)
	{
		if ((count % 1000) != 0) return;
		final long duration = System.currentTimeMillis() - start;
		double rate = (1000.0 * count) / (1.0 * duration);
		log.warn(String.format("XML Scanner Cache (%s): %d processed in %s (%.3f/sec%s)",
			cacheName, count, DurationFormatUtils.formatDuration(duration, "HH:mm:ss.SSS"), rate, end ? " - completed" : ""));
	}

	protected void itemUnmarshalled(CacheItem item, boolean directoryMode)
	{
		// In case we want to fix anything...
	}

	private final boolean scan(final File baseDirectory, final String cacheName, final BulkImportCallback callback, final BulkImportSourceStatus importStatus, boolean directoryMode)
		throws InterruptedException, IOException, JAXBException, XMLStreamException
	{
		final File xmlFile = getFile(baseDirectory, cacheName);
		if (xmlFile == null) return false;

		final String counterName = (directoryMode ? DirectoryAnalyser.COUNTER_NAME_DIRECTORIES_SCANNED : DirectoryAnalyser.COUNTER_NAME_FILES_SCANNED);
		final String badCounter = DirectoryAnalyser.COUNTER_NAME_UNREADABLE_ENTRIES;
		final long start = System.currentTimeMillis();
		InputStream in = new FileInputStream(xmlFile);
		long count = 0;
		try
		{
			final XMLStreamReader xml = XMLInputFactory.newInstance().createXMLStreamReader(in);
			try
			{
				// Find and skip the root element
				if (xml.nextTag() != XMLStreamConstants.START_ELEMENT)
				{
					// Empty document or no proper root tag?!?!?
					return false;
				}

				final Unmarshaller u = JAXBContext.newInstance(CacheItem.class, CacheItemVersion.class).createUnmarshaller();
				boolean ret = false;
				try
				{
					while (xml.nextTag() == XMLStreamConstants.START_ELEMENT)
					{
						if (importStatus.isStopping() || Thread.currentThread().isInterrupted()) throw new InterruptedException(Thread.currentThread().getName() + " was interrupted. Terminating early.");

						boolean ok = false;
						try
						{
							final String elementName = xml.getLocalName();
							if (!elementName.equals("item"))
							{
								// Bad element...skip it!
								continue;
							}

							JAXBElement<CacheItem> xmlItem = u.unmarshal(xml, CacheItem.class);
							if (xmlItem != null)
							{
								CacheItem cacheItem = xmlItem.getValue();
								if (cacheItem != null)
								{
									itemUnmarshalled(cacheItem, directoryMode);
									if (cacheItem.directory != directoryMode)
									{
										// This entry failed - expected a file, but got a directory
										continue;
									}
									// Ok...so...generate and submit the item...
									process(baseDirectory, cacheItem, callback);
									showProgress(start, ++count, cacheName, false);
									ok = true;
									for (CacheItemVersion v : cacheItem.versions)
									{
										if (v.content != null) importStatus.incrementSourceCounter(counterName);
										if (v.metadata != null) importStatus.incrementSourceCounter(DirectoryAnalyser.COUNTER_NAME_METADATA_SCANNED);
									}
								}
							}
						}
						finally
						{
							if (ok)
							{
								ret = true;
							}
							else
							{
								importStatus.incrementSourceCounter(badCounter);
							}
						}
					}
				}
				finally
				{
					showProgress(start, count, cacheName, true);
				}

				// We return a value of TRUE if and only if we read at least one entry...if we did,
				// we cannot safely revert to scanning. If we did not, we can safely scan...
				return ret;
			}
			finally
			{
				importStatus.freezeSourceCounter(counterName);
				try
				{
					xml.close();
				}
				catch (XMLStreamException e)
				{
					// Do nothing...irrelevant at this point
				}
			}
		}
		finally
		{
			IOUtils.closeQuietly(in);
		}
	}
}