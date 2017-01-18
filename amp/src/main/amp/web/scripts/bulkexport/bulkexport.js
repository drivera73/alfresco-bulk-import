//var logLevel = log.levels.DEBUG;  // See http://pimterry.github.io/loglevel/ for details

// Global variables
var statusURI;
var pauseURI;
var resumeURI;
var stopURI;
var webAppContext;
var previousData;
var currentData;
var importStatusTimer;
var refreshTextTimer;

//log.setLevel(logLevel);

function initStatus(alfrescoWebAppContext, alfrescoWebScriptContext)
{
  statusURI     = alfrescoWebScriptContext + "/bulk/export/status.json";
  pauseURI      = alfrescoWebScriptContext + "/bulk/export/pause";
  resumeURI     = alfrescoWebScriptContext + "/bulk/export/resume";
  stopURI       = alfrescoWebScriptContext + "/bulk/export/stop";
  webAppContext = alfrescoWebAppContext;

  currentData = getStatusInfo();  // Pull down an initial set of status info

  if (currentData != null && currentData.inProgress === false)
  {
    // If the import completed before the page even loaded, update the text area then bomb out
    favicon.change(webAppContext + "/images/bulkexport/logo.png");
    refreshTextElements(currentData);
  }
  else
  {
    //log.debug('Post Process in progress, starting UI.');
    startImportStatusTimer();
    startRefreshTextTimer();
  }
}


/*
 * Get status information via an AJAX call
 */
function getStatusInfo()
{
  //log.debug('Retrieving post process status information...');

  $.getJSON(statusURI, function(data)
  {
      try
      {
        previousData = currentData;
        currentData  = data;
      }
      catch (e)
      {
        //log.error('Exception while retrieving status information: ' + e);
      }

      if (currentData != null)
      {
        document.getElementById("currentStatus").textContent = currentData.processingState;
        
        // If we're idle, stop the world
        if (currentData.inProgress === false || currentData.inProgress == undefined || currentData.inProgress == "n/a")
        {
          //log.debug('Post Process complete, shutting down UI.');

          // Kill all the spinners, charts and timers
          //agent.stop();

          if (importStatusTimer        != null) { clearInterval(importStatusTimer);        importStatusTimer        = null; }
          if (refreshTextTimer         != null) { clearInterval(refreshTextTimer);         refreshTextTimer         = null; }
          
          // Update the text one last time
          refreshTextElements(currentData);
          // Hide buttons and show initiate another import link
          //hideElement(document.getElementById("pauseImportButton"));
          //hideElement(document.getElementById("resumeImportButton"));
          //hideElement(document.getElementById("stopImportButton"));
          showElement(document.getElementById("initiateAnotherImport"), false);
        }
        else  // We're not idle, so update stuff
        {
        	//agent.animate();
        }
      }
      else
      {
        showElement(document.getElementById("initiateAnotherImport"), false);
        //log.warn('No data received from server.');
      }
  });
}

/*
 * Refresh all of the text elements on the page.
 */
function refreshTextElements(cd)
{
  //log.debug('Refreshing text elements...');

  if (cd != null)
  {
    // Status
    document.getElementById("currentStatus").textContent = cd.processingState;
    //document.getElementById("detailsStatus").style.color = stateToColour(cd.processingState);


    // End date
    if (cd.endDate) {
        document.getElementById("detailsEndDate").textContent = cd.endDate;
    }
    
    // Duration
    if (cd.duration) document.getElementById("detailsDuration").textContent = cd.duration;

    // Counters
    if (cd.targetCounters) {
    	document.getElementById("folderBatchesSubmitted").textContent = cd.targetCounters['Folder Batches submitted'].Count;
	    document.getElementById("folderBatchesCompleted").textContent = cd.targetCounters['Folder Batches completed'].Count;
	    document.getElementById("documentBatchesSubmitted").textContent = cd.targetCounters['Document Batches submitted'].Count;
	    document.getElementById("documentBatchesCompleted").textContent = cd.targetCounters['Document Batches completed'].Count;
	    document.getElementById("totalNodesSubmitted").textContent = cd.targetCounters['Nodes submitted'].Count;
	    document.getElementById("totalNodesCompleted").textContent = cd.targetCounters['Nodes completed'].Count;
	    document.getElementById("foldersSubmitted").textContent = cd.targetCounters['Folders submitted'].Count;
	    document.getElementById("foldersCompleted").textContent = cd.targetCounters['Folders completed'].Count;
	    document.getElementById("foldersSkipped").textContent = cd.targetCounters['Folders skipped'].Count;
	    document.getElementById("foldersErrors").textContent = cd.targetCounters['Folders errors'].Count;
	    document.getElementById("foldersMetadataSubmitted").textContent = cd.targetCounters['Folders submitted'].Count;
	    document.getElementById("foldersMetadataCompleted").textContent = cd.targetCounters['Folders metadata completed'].Count;
	    document.getElementById("foldersMetadataSkipped").textContent = cd.targetCounters['Folders metadata skipped'].Count;
	    document.getElementById("foldersMetadataErrors").textContent = cd.targetCounters['Folders metadata errors'].Count;
	    document.getElementById("documentsSubmitted").textContent = cd.targetCounters['Documents submitted'].Count;
	    document.getElementById("documentsCompleted").textContent = cd.targetCounters['Documents completed'].Count;
	    document.getElementById("documentsSkipped").textContent = cd.targetCounters['Documents skipped'].Count;
	    document.getElementById("documentsErrors").textContent = cd.targetCounters['Documents errors'].Count;
	    document.getElementById("documentsMetadataCompleted").textContent = cd.targetCounters['Documents metadata completed'].Count;
	    document.getElementById("documentsMetadataSkipped").textContent = cd.targetCounters['Documents metadata skipped'].Count;
	    document.getElementById("documentsMetadataErrors").textContent = cd.targetCounters['Documents metadata errors'].Count;
	    document.getElementById("versionsSubmitted").textContent = cd.targetCounters['Versions submitted'].Count;
	    document.getElementById("versionsCompleted").textContent = cd.targetCounters['Versions completed'].Count;
	    document.getElementById("versionsSkipped").textContent = cd.targetCounters['Versions skipped'].Count;
	    document.getElementById("versionsErrors").textContent = cd.targetCounters['Versions errors'].Count;
	    document.getElementById("versionsMetadataCompleted").textContent = cd.targetCounters['Versions metadata completed'].Count;
	    document.getElementById("versionsMetadataSkipped").textContent = cd.targetCounters['Versions metadata skipped'].Count;
	    document.getElementById("versionsMetadataErrors").textContent = cd.targetCounters['Versions metadata errors'].Count;
    }
  }
}

/*
 * Start the timer that periodically pulls the import status info down
 */
function startImportStatusTimer()
{
  //log.debug('Starting import status timer...');

  var getImportStatus = function()
  {
    getStatusInfo();
  };

  importStatusTimer = setInterval(getImportStatus, 1000)
}

/*
 * Start the timer that refreshes the details section of the page
 */
function startRefreshTextTimer()
{
  //log.debug('Starting refresh text timer...');

  var refreshText = function()
  {
    refreshTextElements(currentData);
  };

  refreshTextTimer = setInterval(refreshText, 2000)
}

function hideElement(element)
{
  element.style.display = "none";
}


function showElement(element, inline)
{
  if (inline)
  {
    element.style.display = "inline";
  }
  else
  {
    element.style.display = "inline-block";
  }
}