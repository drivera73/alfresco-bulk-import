/**
 *  This file is part of Alfresco Bulk Export Tool.
 *
 *  Alfresco Bulk Export Tool is free software: you can redistribute it
 *  and/or modify it under the terms of the GNU General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  Alfresco Bulk Export Tool  is distributed in the hope that it will be
 *  useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with Alfresco Bulk Export Tool. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.extension.bulkexport.model;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.alfresco.extension.bulkexport.BulkExportStatus;
import org.alfresco.extension.bulkexport.WriteableBulkExportStatus;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tika.io.IOUtils;
import org.springframework.extensions.webscripts.WebScriptResponse;

import com.armedia.caliente.tools.xml.XmlProperties;


/**
 * This class manage the files and folders creation
 *
 * @author Denys G. Santos (gsdenys@gmail.com)
 * @version 1.0.1
 */
public class FileFolder
{
	Log log = LogFactory.getLog(FileFolder.class);

	public static enum FileFolderStatus {
	    OK,
	    FAIL,
	    SKIPPED,
	    UNKNOWN;
	}
	
	/** {@link String} interface to web page for displaying messages
	 *  server
	 */
	private WebScriptResponse ui;
	List<String> errorLog = new ArrayList<String>();
	
	/** {@link String} path to export data location in Alfresco
	 *  server
	 */
	private String basePath;

	/** {@link Boolean} value to avaliate if ovewrite content
	 * exported or no
	 */
	private boolean scapeExported;

	/**
	 * File Folder default builder
	 *
	 * @param basePath
	 */
	public FileFolder(WebScriptResponse ui, String basePath, boolean scapeExported)
	{
		log.debug("debug enabled for FileFolder");
		this.basePath = basePath;
		this.scapeExported = scapeExported;
		this.ui = ui;
	}

	public FileFolder(String basePath, boolean scapeExported)
	{
		log.debug("debug enabled for FileFolder");
		this.basePath = basePath;
		this.scapeExported = scapeExported;
		this.ui = null;
	}
	
	public String basePath()
	{
		return this.basePath;
	}

	/**
	 * Create a new Folder in a {@link String} path
	 *
	 * @param path Path of Alfresco folder
	 */
	public FileFolderStatus createFolder(String path) throws Exception
	{
		path = this.basePath + path;
		log.debug("createFolder path to create : " + path);
		FileFolderStatus ok = FileFolderStatus.UNKNOWN;
		try
		{
			File dir = new File(path);
			if (!dir.exists())
			{
				if (!dir.mkdirs())
				{
					log.error("createFolder failed to create path : " + path);
				}
				else
				{
					log.debug("createFolder path : " + path);
					ok = FileFolderStatus.OK;
				}
			}
		} catch (SecurityException e) {
			errorLog.add(e.toString());
			e.printStackTrace();
			if (this.ui != null) {
				ui.getWriter().write(e.toString());
			}
			ok = FileFolderStatus.FAIL;
		} catch (Exception e)
		{
			errorLog.add(e.toString());
			e.printStackTrace();
			if (this.ui != null) {
				ui.getWriter().write(e.toString());
			}
			ok = FileFolderStatus.FAIL;
		}
		return ok;
	}


	/**
	 * Create a new file in the {@link String} path
	 *
	 * @param filePath Path of file
	 * @throws IOException
	 */
	private void createFile (String filePath) throws Exception
	{
		log.debug("createFile = " + filePath);

		File f=new File(filePath);

		try
		{
			if(!f.exists())
			{
			  if (!f.getParentFile().exists())
			  {
				  if (!f.getParentFile().mkdirs())
				  {
					  log.error("failed to create folder : " + f.getParentFile().getPath());
				  }
				  else
				  {
					  log.debug("created folder : " + f.getParentFile().getPath());
				  }
			  }
			  f.createNewFile();
			}
		}
		catch (Exception e)
		{
			errorLog.add(e.toString());
			e.printStackTrace();
			if (this.ui != null) {
				ui.getWriter().write(e.toString());
			}
		}
		log.debug("createFile filepath done");
	}


	/**
	 * Create XML File
	 *
	 * @param filePath Path of file
	 * @return {@link String} Name of file
	 * @throws Exception
	 */
	private String createXmlFile(String filePath, String revision) throws Exception
	{
		String fp = filePath + ".metadata.properties.xml";

		if (revision != null) {
			fp = fp + ".v" + revision;
		}
		this.createFile(fp);

		return fp;
	}


	/**
	 * create content file
	 *
	 * @param out
	 * @param filePath
	 * @throws IOException
	 */
	public void insertFileContent (ByteArrayOutputStream out, String filePath) throws Exception
	{
		log.debug("insertFileContent");
		filePath = this.basePath + filePath;

		log.debug("insertFileContent filepath = " + filePath);
		if(this.isFileExist(filePath) && this.scapeExported)
		{
			log.debug("insertFileContent ignore file");
			return;
		}

		this.createFile(filePath);

		try
		{
			FileOutputStream output = new FileOutputStream(filePath);
			output.write(out.toByteArray());
			output.flush();
			output.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * construct full file path and make directory if it does not exist
	 *
	 * @param filePath
	 * @throws IOException
	 */
	public String createFullPath(String filePath) throws Exception
	{
		log.debug("createFullPath");
		filePath = this.basePath + filePath;

		log.debug("createFullPath filepath = " + filePath);
		if(this.isFileExist(filePath) && this.scapeExported)
		{
			log.debug("createFullPath ignore file");
			return filePath;
		}

		File f=new File(filePath);

		try
		{
			if(!f.exists())
			{
			  if (!f.getParentFile().exists())
			  {
				  if (!f.getParentFile().mkdirs())
				  {
					  log.error("failed to create folder : " + f.getParentFile().getPath());
				  }
				  else
				  {
					  log.debug("created folder : " + f.getParentFile().getPath());
				  }
			  }
			}
		}
		catch (Exception e)
		{
			errorLog.add(e.toString());
			e.printStackTrace();
			if (this.ui != null) {
				ui.getWriter().write(e.toString());
			}
		}

		return filePath;
	}


	/**
	 * Insert Content Properties in the XML File
	 *
	 * @param type The type of node
	 * @param aspects The aspect {@link List} of node in {@link String} format
	 * @param properties The properties {@link Map} of node in {@link String} format
	 * @param filePath The path of file
	 * @throws Exception
	 */
	public FileFolderStatus insertFileProperties(String type, List<String> aspects, Map<String, String> properties, String filePath, String revision) throws Exception
	{
		filePath = this.basePath + filePath;
		FileFolderStatus ok = FileFolderStatus.UNKNOWN;
		if (revision == null) {
			if(this.isFileExist(filePath) && this.isFileExist(filePath + ".metadata.properties.xml") && this.scapeExported)
			{
				log.info("Skipping " + filePath + ".metadata.properties.xml: Already exists");
				return FileFolderStatus.SKIPPED;
			}
		} else {
			// check if revision version exists
			if (filePath.endsWith(".v" + revision))
			{
				filePath = filePath.substring(0, filePath.indexOf(".v" + revision));
			}
			if(this.isFileExist(filePath) && this.isFileExist(filePath + ".metadata.properties.xml.v" + revision) && this.scapeExported)
			{
				log.info("Skipping " + filePath + ".metadata.properties.xml: Already exists");
				return FileFolderStatus.SKIPPED;
			}
		}


		properties.put("type", type);
		properties.put("aspects", StringUtils.join(aspects, ','));

		try
		{
			String fp = this.createXmlFile(filePath, revision);
			File file = new File(fp);

			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));

			try
			{
				XmlProperties.saveToXML(properties, out, null);
				ok = FileFolderStatus.OK;
			}
			finally
			{
				IOUtils.closeQuietly(out);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			ok = FileFolderStatus.FAIL;
		}
		return ok;
	}

	/**
	 * Method to see if file already exists
	 *
	 * @param path The {@link String} path of file
	 * @return {@link Boolean}
	 */
	private boolean isFileExist(String path)
	{
		File f=new File(path);

		if(f.exists())
		{
		  return true;
		}

		return false;
	}
	
	public List<String> getErrorLog() {
		return errorLog;
	}
	
	public static void updateFolderCounters(FileFolderStatus ffs, WriteableBulkExportStatus bulkExportStatus) {
		switch (ffs) {
        case OK:
        	bulkExportStatus.incrementTargetCounter(BulkExportStatus.TARGET_COUNTER_TOTAL_FOLDERS_COMPLETE);
        	break;
        case FAIL:
        	bulkExportStatus.incrementTargetCounter(BulkExportStatus.TARGET_COUNTER_TOTAL_FOLDERS_ERRORS);
        	break;
        case SKIPPED:
        	bulkExportStatus.incrementTargetCounter(BulkExportStatus.TARGET_COUNTER_TOTAL_FOLDERS_SKIPPED);
        	break;
        case UNKNOWN:
        	bulkExportStatus.incrementTargetCounter(BulkExportStatus.TARGET_COUNTER_TOTAL_FOLDERS_UNKNOWN);
        	break;
        default:
        	break;	
        }
	}
	
	public static void updateFolderMetadataCounters(FileFolderStatus ffs, WriteableBulkExportStatus bulkExportStatus) {
		switch (ffs) {
        case OK:
        	bulkExportStatus.incrementTargetCounter(BulkExportStatus.TARGET_COUNTER_TOTAL_FOLDERS_METADATA_COMPLETE);
        	break;
        case FAIL:
        	bulkExportStatus.incrementTargetCounter(BulkExportStatus.TARGET_COUNTER_TOTAL_FOLDERS_METADATA_ERRORS);
        	break;
        case SKIPPED:
        	bulkExportStatus.incrementTargetCounter(BulkExportStatus.TARGET_COUNTER_TOTAL_FOLDERS_METADATA_SKIPPED);
        	break;
        case UNKNOWN:
        	bulkExportStatus.incrementTargetCounter(BulkExportStatus.TARGET_COUNTER_TOTAL_FOLDERS_METADATA_UNKNOWN);
        	break;
        default:
        	break;	
        }
	}
	
	public static void updateDocumentCounters(FileFolderStatus ffs, WriteableBulkExportStatus bulkExportStatus) {
		switch (ffs) {
        case OK:
        	bulkExportStatus.incrementTargetCounter(BulkExportStatus.TARGET_COUNTER_TOTAL_DOCUMENTS_COMPLETE);
        	break;
        case FAIL:
        	bulkExportStatus.incrementTargetCounter(BulkExportStatus.TARGET_COUNTER_TOTAL_DOCUMENTS_ERRORS);
        	break;
        case SKIPPED:
        	bulkExportStatus.incrementTargetCounter(BulkExportStatus.TARGET_COUNTER_TOTAL_DOCUMENTS_SKIPPED);
        	break;
        case UNKNOWN:
        	bulkExportStatus.incrementTargetCounter(BulkExportStatus.TARGET_COUNTER_TOTAL_DOCUMENTS_UNKNOWN);
        	break;
        default:
        	break;	
        }
	}
	
	public static void updateDocumentMetadataCounters(FileFolderStatus ffs, WriteableBulkExportStatus bulkExportStatus) {
		switch (ffs) {
        case OK:
        	bulkExportStatus.incrementTargetCounter(BulkExportStatus.TARGET_COUNTER_TOTAL_DOCUMENTS_METADATA_COMPLETE);
        	break;
        case FAIL:
        	bulkExportStatus.incrementTargetCounter(BulkExportStatus.TARGET_COUNTER_TOTAL_DOCUMENTS_METADATA_ERRORS);
        	break;
        case SKIPPED:
        	bulkExportStatus.incrementTargetCounter(BulkExportStatus.TARGET_COUNTER_TOTAL_DOCUMENTS_METADATA_SKIPPED);
        	break;
        case UNKNOWN:
        	bulkExportStatus.incrementTargetCounter(BulkExportStatus.TARGET_COUNTER_TOTAL_DOCUMENTS_METADATA_UNKNOWN);
        	break;
        default:
        	break;	
        }
	}
	
	public static void updateDocumentVersionCounters(FileFolderStatus ffs, WriteableBulkExportStatus bulkExportStatus) {
		switch (ffs) {
        case OK:
        	bulkExportStatus.incrementTargetCounter(BulkExportStatus.TARGET_COUNTER_TOTAL_DOCUMENTS_VERSION_COMPLETE);
        	break;
        case FAIL:
        	bulkExportStatus.incrementTargetCounter(BulkExportStatus.TARGET_COUNTER_TOTAL_DOCUMENTS_VERSION_ERRORS);
        	break;
        case SKIPPED:
        	bulkExportStatus.incrementTargetCounter(BulkExportStatus.TARGET_COUNTER_TOTAL_DOCUMENTS_VERSION_SKIPPED);
        	break;
        case UNKNOWN:
        	bulkExportStatus.incrementTargetCounter(BulkExportStatus.TARGET_COUNTER_TOTAL_DOCUMENTS_VERSION_UNKNOWN);
        	break;
        default:
        	break;	
        }
	}
	
	public static void updateDocumentVersionMetadataCounters(FileFolderStatus ffs, WriteableBulkExportStatus bulkExportStatus) {
		switch (ffs) {
        case OK:
        	bulkExportStatus.incrementTargetCounter(BulkExportStatus.TARGET_COUNTER_TOTAL_DOCUMENTS_VERSION_METADATA_COMPLETE);
        	break;
        case FAIL:
        	bulkExportStatus.incrementTargetCounter(BulkExportStatus.TARGET_COUNTER_TOTAL_DOCUMENTS_VERSION_METADATA_ERRORS);
        	break;
        case SKIPPED:
        	bulkExportStatus.incrementTargetCounter(BulkExportStatus.TARGET_COUNTER_TOTAL_DOCUMENTS_VERSION_METADATA_SKIPPED);
        	break;
        case UNKNOWN:
        	bulkExportStatus.incrementTargetCounter(BulkExportStatus.TARGET_COUNTER_TOTAL_DOCUMENTS_VERSION_METADATA_UNKNOWN);
        	break;
        default:
        	break;	
        }
	}
}