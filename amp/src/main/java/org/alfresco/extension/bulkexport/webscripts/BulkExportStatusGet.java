package org.alfresco.extension.bulkexport.webscripts;

import java.util.HashMap;
import java.util.Map;

import org.alfresco.extension.bulkexport.BulkExport;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class BulkExportStatusGet extends DeclarativeWebScript {
	
	private final static String RESULT_BE_STATUS = "beStatus";
	private BulkExport bulkExport;
	
	public void setBulkExport(BulkExport bulkExport) {
		this.bulkExport = bulkExport;
	}
	
	/**
     * @see org.alfresco.web.scripts.DeclarativeWebScript#executeImpl(org.alfresco.web.scripts.WebScriptRequest, org.alfresco.web.scripts.Status, org.alfresco.web.scripts.Cache)
     */
    @Override
    protected Map<String, Object> executeImpl(final WebScriptRequest request,
                                              final Status           status,
                                              final Cache            cache)
    {
        Map<String, Object> result = new HashMap<>();
        
        cache.setNeverCache(true);
        
        result.put(RESULT_BE_STATUS, bulkExport.getStatus());
        
        return(result);
    }
}
