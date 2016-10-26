package org.alfresco.extension.bulkimport.source.fs.cache;

import java.io.File;
import java.net.URL;
import java.util.concurrent.atomic.AtomicLong;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import org.alfresco.extension.bulkimport.BulkImportCallback;
import org.alfresco.extension.bulkimport.impl.BulkImportStatusImpl;
import org.alfresco.extension.bulkimport.source.BulkImportItem;
import org.alfresco.extension.bulkimport.source.BulkImportSourceStatus;
import org.alfresco.extension.bulkimport.source.fs.DirectoryAnalyser;
import org.apache.commons.io.output.NullOutputStream;
import org.junit.Assert;
import org.junit.Test;

import com.sun.star.uno.RuntimeException;

import javanet.staxutils.IndentingXMLStreamWriter;

public class XmlScannerCacheTest
{
	private static abstract class TestInstance
	{
		private final boolean expectedIsDirectory;

		TestInstance(boolean expectedIsDirectory)
		{
			this.expectedIsDirectory = expectedIsDirectory;
		}

		protected abstract void runScan(XmlScannerCache cache, File bd, BulkImportCallback expectedCallback, BulkImportSourceStatus status) throws Exception;

		public final void test() throws Exception
		{
			BulkImportSourceStatus status = new BulkImportStatusImpl();
			URL url = Thread.currentThread().getContextClassLoader().getResource("test.xml");
			File bd = new File(url.toURI());
			bd = bd.getCanonicalFile();
			bd = bd.getParentFile();

			final File expectedBaseDirectory = bd;
			final BulkImportCallback expectedCallback = new BulkImportCallback()
			{
				@Override
				public void submit(@SuppressWarnings("rawtypes") BulkImportItem item) throws InterruptedException
				{
					Assert.assertNotNull(item);
					Assert.assertEquals(expectedIsDirectory, item.isDirectory());
				}
			};

			final Marshaller m = JAXBContext.newInstance(CacheItem.class, CacheItemVersion.class).createMarshaller();
			m.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

			final XMLStreamWriter xml = new IndentingXMLStreamWriter(XMLOutputFactory.newInstance().createXMLStreamWriter(new NullOutputStream()));
			xml.writeStartDocument("UTF-8", "1.0");
			xml.writeStartElement("scan");
			xml.flush();

			final AtomicLong total = new AtomicLong(0);
			final AtomicLong expected = new AtomicLong(0);
			final AtomicLong different = new AtomicLong(0);
			final AtomicLong actual = new AtomicLong(0);

			XmlScannerCache cache = new XmlScannerCache(null, null, null)
			{
				@Override
				protected void itemUnmarshalled(CacheItem item, boolean directoryMode)
				{
					// Only count what needs counting...
					total.incrementAndGet();
					(directoryMode == item.directory ? expected : different).incrementAndGet();
				}

				@Override
				protected void process(File baseDirectory, CacheItem cacheItem, BulkImportCallback callback)
					throws InterruptedException
				{
					actual.incrementAndGet();
					Assert.assertNotNull(baseDirectory);
					Assert.assertEquals(expectedBaseDirectory, baseDirectory);
					Assert.assertSame(expectedCallback, callback);
					Assert.assertNotNull(cacheItem);
					Assert.assertEquals(expectedIsDirectory, cacheItem.directory);
					try
					{
						m.marshal(cacheItem, xml);
						xml.flush();
					}
					catch (Exception e)
					{
						throw new RuntimeException("Failed to marshal the XML element", e);
					}
				}
			};

			runScan(cache, expectedBaseDirectory, expectedCallback, status);
			Assert.assertEquals(expected.get(), actual.get());
			Assert.assertEquals(total.get(), expected.get() + different.get());
			xml.writeEndDocument();
			xml.flush();

			String counterName = (expectedIsDirectory ? DirectoryAnalyser.COUNTER_NAME_DIRECTORIES_SCANNED : DirectoryAnalyser.COUNTER_NAME_FILES_SCANNED);
			final long counter = status.getSourceCounter(counterName);
			Assert.assertEquals(expected.get(), counter);
		}
	}

	@Test
	public void testScanFiles() throws Exception
	{
		new TestInstance(false)
		{
			@Override
			protected void runScan(XmlScannerCache cache, File bd, BulkImportCallback expectedCallback, BulkImportSourceStatus status)
				throws Exception
			{
				cache.scanFiles(bd, expectedCallback, status);
			}
		}.test();
	}

	@Test
	public void testScanFolders() throws Exception
	{
		new TestInstance(true)
		{
			@Override
			protected void runScan(XmlScannerCache cache, File bd, BulkImportCallback expectedCallback, BulkImportSourceStatus status)
				throws Exception
			{
				cache.scanFolders(bd, expectedCallback, status);
			}
		}.test();
	}
}