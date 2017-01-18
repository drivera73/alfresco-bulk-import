package org.alfresco.extension.bulkimport.source;

import org.apache.commons.lang3.StringUtils;

public class BulkImportTools {

    public static String getRelativeSourcePath(final BulkImportItem<?> item)
    {
    	String path = item.getSourceRelativePathOfParent();
    	if (path == null) path = "";
    	return path;
    }

    public static String getCompleteSourcePath(final BulkImportItem<?> item)
    {
    	String parentPath = getRelativeSourcePath(item);
    	if (!StringUtils.isEmpty(parentPath)) parentPath = String.format("%s/", parentPath);
    	return String.format("%s%s", parentPath, item.getSourceName());
    }

    public static String getRelativeTargetPath(final BulkImportItem<?> item)
    {
    	String path = item.getTargetRelativePathOfParent();
    	if (path == null) path = getRelativeSourcePath(item);
    	return path;
    }

    public static String getCompleteTargetPath(final BulkImportItem<?> item)
    {
    	String parentPath = getRelativeTargetPath(item);
    	if (!StringUtils.isEmpty(parentPath)) parentPath = String.format("%s/", parentPath);
    	return String.format("%s%s", parentPath, item.getTargetName());
    }
}