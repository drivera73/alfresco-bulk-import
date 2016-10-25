package org.alfresco.extension.bulkimport.source.fs.cache;

import java.io.File;
import java.math.BigDecimal;

import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.alfresco.extension.bulkimport.source.fs.FilesystemBulkImportItemVersion;
import org.alfresco.extension.bulkimport.source.fs.MetadataLoader;
import org.alfresco.repo.content.ContentStore;
import org.alfresco.service.ServiceRegistry;

@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "itemVersion.t", propOrder =
	{
		"number", "content", "metadata"
	}
)
public class CacheItemVersion
{
	@XmlElement(required = true)
	protected String number;

	@XmlTransient
	protected BigDecimal numberBd;

	@XmlElement(required = true)
	protected String content;

	@XmlElement(required = true)
	protected String metadata;

	protected void afterUnmarshal(Unmarshaller unmarshaller, Object parent)
	{
		if (this.number == null)
		{
			this.numberBd = null;
		}
		else
		{
			this.numberBd = new BigDecimal(this.number);
		}
	}

	protected void beforeMarshal(Marshaller marshaller)
	{
		if (this.numberBd == null)
		{
			this.number = null;
		}
		else
		{
			this.number = this.numberBd.toString();
		}
	}

	public BigDecimal getNumber()
	{
		return this.numberBd;
	}

	public void setNumber(BigDecimal number)
	{
		this.numberBd = number;
	}

	public String getContent()
	{
		return content;
	}

	public void setContent(String content)
	{
		this.content = content;
	}

	public String getMetadata()
	{
		return metadata;
	}

	public void setMetadata(String metadata)
	{
		this.metadata = metadata;
	}

	public FilesystemBulkImportItemVersion generate(File baseDir, ServiceRegistry serviceRegistry, ContentStore contentStore, MetadataLoader metadataLoader)
	{
		final File content = new File(baseDir, this.content);
		final File metadata = new File(baseDir, this.metadata);
		return new FilesystemBulkImportItemVersion(serviceRegistry, contentStore, metadataLoader, numberBd, content, metadata);
	}

	@Override
	public String toString() {
		return String.format("CacheItemVersion [number=%s, content=%s, metadata=%s]", number, content, metadata);
	}
}