package org.alfresco.extension.bulkimport.source.fs.cache;

import java.io.File;
import java.net.URL;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import org.alfresco.extension.bulkimport.BulkImportCallback;
import org.alfresco.extension.bulkimport.impl.BulkImportStatusImpl;
import org.alfresco.extension.bulkimport.source.BulkImportItem;
import org.alfresco.extension.bulkimport.source.BulkImportSourceStatus;
import org.junit.Assert;
import org.junit.Test;

import com.sun.star.uno.RuntimeException;

import javanet.staxutils.IndentingXMLStreamWriter;

public class XmlScannerCacheTest
{

	@Test
	public void testScanFiles() throws Exception
	{
		BulkImportSourceStatus status = new BulkImportStatusImpl();
		URL url = Thread.currentThread().getContextClassLoader().getResource("test.xml");
		File bd = new File(url.toURI());
		bd = bd.getCanonicalFile();
		bd = bd.getParentFile();

		final File expectedBaseDirectory = bd;
		final boolean expectedIsDirectory = false;
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

		final XMLStreamWriter xml = new IndentingXMLStreamWriter(XMLOutputFactory.newInstance().createXMLStreamWriter(System.out));
		xml.writeStartDocument("UTF-8", "1.0");
		xml.writeStartElement("scan");
		xml.flush();

		XmlScannerCache cache = new XmlScannerCache(null, null, null)
		{
			@Override
			protected void process(File baseDirectory, CacheItem cacheItem, BulkImportCallback callback)
				throws InterruptedException
			{
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

		cache.scanFiles(bd, expectedCallback, status);
		xml.writeEndDocument();
		xml.flush();
	}

	@Test
	public void testScanFolders() throws Exception
	{
		BulkImportSourceStatus status = new BulkImportStatusImpl();
		URL url = Thread.currentThread().getContextClassLoader().getResource("test.xml");
		File bd = new File(url.toURI());
		bd = bd.getCanonicalFile();
		bd = bd.getParentFile();

		final File expectedBaseDirectory = bd;
		final boolean expectedIsDirectory = true;
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

		final XMLStreamWriter xml = new IndentingXMLStreamWriter(XMLOutputFactory.newInstance().createXMLStreamWriter(System.out));
		xml.writeStartDocument("UTF-8", "1.0");
		xml.writeStartElement("scan");
		xml.flush();

		XmlScannerCache cache = new XmlScannerCache(null, null, null)
		{
			@Override
			protected void process(File baseDirectory, CacheItem cacheItem, BulkImportCallback callback)
				throws InterruptedException
			{
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

		cache.scanFolders(bd, expectedCallback, status);
		xml.writeEndDocument();
		xml.flush();
	}
}