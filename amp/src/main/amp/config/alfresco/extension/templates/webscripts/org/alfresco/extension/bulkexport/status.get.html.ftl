[#ftl]

<!DOCTYPE HTML>
<!--[if lt IE 7]>      <html class="no-js lt-ie9 lt-ie8 lt-ie7"> <![endif]-->
<!--[if IE 7]>         <html class="no-js lt-ie9 lt-ie8"> <![endif]-->
<!--[if IE 8]>         <html class="no-js lt-ie9"> <![endif]-->
<!--[if gt IE 8]><!--> <html class="no-js"> <!--<![endif]-->
<html>
<head>
    <meta charset="utf-8">
    <link href='//fonts.googleapis.com/css?family=Open+Sans:400italic,600italic,400,600' rel='stylesheet' type='text/css'>
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <title>Bulk Export Status</title>
    <meta name="description" content="Bulk Export Status">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    [#-- 3rd Party Stuff --]
    <link rel="stylesheet" href="${url.context}/scripts/bulk/jquery/1.12.0/jquery-ui.min.css">
    <script src="${url.context}/scripts/bulk/jquery/2.2.4/jquery-2.2.4.min.js"></script>
    <script src="${url.context}/scripts/bulk/jquery/1.12.0/jquery-ui.min.js"></script>
    <script src="${url.context}/scripts/bulk/loglevel/1.4.0/loglevel.min.js"></script>
    <script src="${url.context}/scripts/bulkexport/bulkexport.js"></script>
</head>
<body>
    <!--[if lt IE 9]>
        <p class="browsehappy">You are using an <strong>outdated</strong> browser. Please <a href="http://browsehappy.com/">upgrade your browser</a> to improve your experience.</p>
    <![endif]-->
    <div class="container">
      <div class="block">
        <img style="margin:15px;vertical-align:middle" src="${url.context}/images/bulkexport/logo.png" alt="Bulk Export Status" />
      </div>
      <div class="block">
        <h1><strong>Bulk Export Status</strong></h1>
      </div>
    </div>

    <h3>Details</h3>
    <div>
      <table border="1" callspacing="0" cellpadding="1" width="80%">
        <thead>
          <tr>
            <th>Name</th>
            <th>Value</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td width="25%">Current Status:</td>
            <td id="currentStatus" width="75%">[#if beStatus?? && beStatus.currentStatus??]${(beStatus.currentStatus!"n/a")?html}[#else]n/a[/#if]</td>
          </tr>
          <tr>
            <td>Initiating User:</td>
            <td>[#if beStatus?? && beStatus.initiatingUserId??]${(beStatus.initiatingUserId!"n/a")?html}[#else]n/a[/#if]</td>
          </tr>
          <tr>
            <td>Source:</td>
            <td>[#if beStatus?? && beStatus.sourceName??]${(beStatus.sourceName!"n/a")?html}[#else]n/a[/#if]</td>
          </tr>
          <tr>
            <td>Start Date:</td>
            <td>[#if beStatus?? && beStatus.startDate??]${beStatus.startDate?datetime}[#else]n/a[/#if]</td>
          </tr>
          <tr>
            <td>End Date:</td>
            <td id="detailsEndDate">[#if beStatus?? && beStatus.endDate??]${beStatus.endDate?datetime}[#else]n/a[/#if]</td>
          </tr>
          <tr>
            <td>Duration:</td>
            <td id="detailsDuration">[#if beStatus?? && beStatus.duration??]${(beStatus.duration!"n/a")?html}[#else]n/a[/#if]</td>
          </tr>
        </tbody>
      </table>
    </div>
    <h3>Batches</h3>
    <div>
        <table border="1" callspacing="0" cellpadding="1" width="80%">
        <thead>
          <tr>
            <th>Counter</th>
            <th>Batch Count</th>
            <th>Completed</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td>Folders</td>
      [#if beStatus?? && beStatus.targetCounters??]
        [#assign counterValue = beStatus.targetCounters['Folder Batches submitted'].Count]
      [#else]
          [#assign counterValue = '']
      [/#if]
            <td id="folderBatchesSubmitted">${counterValue}</td>
            
      [#if beStatus?? && beStatus.targetCounters??]
        [#assign counterValue = beStatus.targetCounters['Folder Batches completed'].Count]
      [#else]
          [#assign counterValue = '']
      [/#if]
            <td id="folderBatchesCompleted">${counterValue}</td>
          </tr>
          <tr>
            <td>Documents</td>
      [#if beStatus?? && beStatus.targetCounters??]
        [#assign counterValue = beStatus.targetCounters['Document Batches submitted'].Count]
      [#else]
          [#assign counterValue = '']
      [/#if]
            <td id="documentBatchesSubmitted">${counterValue}</td>
      [#if beStatus?? && beStatus.targetCounters??]
        [#assign counterValue = beStatus.targetCounters['Document Batches completed'].Count]
      [#else]
          [#assign counterValue = '']
      [/#if]
            <td id="documentBatchesCompleted">${counterValue}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <h3>Counters</h3>
    <div>
        <table border="1" callspacing="0" cellpadding="1" width="80%">
        <thead>
          <tr>
            <th>Counter</th>
            <th>Discovered</th>
            <th>Completed</th>
            <th>Errors</th>
            <th>Skipped</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td>Total Nodes</td>
      [#if beStatus?? && beStatus.targetCounters??]
        [#assign counterValue = beStatus.targetCounters['Nodes submitted'].Count]
      [#else]
          [#assign counterValue = '']
      [/#if]
            <td id="totalNodesSubmitted">${counterValue}</td>
      [#if beStatus?? && beStatus.targetCounters??]
        [#assign counterValue = beStatus.targetCounters['Nodes completed'].Count]
      [#else]
          [#assign counterValue = '']
      [/#if]
            <td id="totalNodesCompleted">${counterValue}</td>
            <td>&nbsp;</td>
            <td>&nbsp;</td>
          </tr>
          <tr>
            <td>Folders</td>
      [#if beStatus?? && beStatus.targetCounters??]
        [#assign counterValue = beStatus.targetCounters['Folders submitted'].Count]
      [#else]
          [#assign counterValue = '']
      [/#if]
            <td id="foldersSubmitted">${counterValue}</td>
      [#if beStatus?? && beStatus.targetCounters??]
        [#assign counterValue = beStatus.targetCounters['Folders completed'].Count]
      [#else]
          [#assign counterValue = '']
      [/#if]
            <td id="foldersCompleted">${counterValue}</td>
      [#if beStatus?? && beStatus.targetCounters??]
        [#assign counterValue = beStatus.targetCounters['Folders errors'].Count]
      [#else]
          [#assign counterValue = '']
      [/#if]
            <td id="foldersErrors">${counterValue}</td>
      [#if beStatus?? && beStatus.targetCounters??]
        [#assign counterValue = beStatus.targetCounters['Folders skipped'].Count]
      [#else]
          [#assign counterValue = '']
      [/#if]
            <td id="foldersSkipped">${counterValue}</td>
          </tr>
          <tr>
            <td>Folders Metadata</td>
      [#if beStatus?? && beStatus.targetCounters??]
        [#assign counterValue = beStatus.targetCounters['Folders submitted'].Count]
      [#else]
          [#assign counterValue = '']
      [/#if]
            <td id="foldersMetadataSubmitted">${counterValue}</td>
      [#if beStatus?? && beStatus.targetCounters??]
        [#assign counterValue = beStatus.targetCounters['Folders metadata completed'].Count]
      [#else]
          [#assign counterValue = '']
      [/#if]
            <td id="foldersMetadataCompleted">${counterValue}</td>
      [#if beStatus?? && beStatus.targetCounters??]
        [#assign counterValue = beStatus.targetCounters['Folders metadata errors'].Count]
      [#else]
          [#assign counterValue = '']
      [/#if]
            <td id="foldersMetadataErrors">${counterValue}</td>
      [#if beStatus?? && beStatus.targetCounters??]
        [#assign counterValue = beStatus.targetCounters['Folders metadata skipped'].Count]
      [#else]
          [#assign counterValue = '']
      [/#if]
            <td id="foldersMetadataSkipped">${counterValue}</td>
          </tr>
          <tr>
            <td>Documents</td>
      [#if beStatus?? && beStatus.targetCounters??]
        [#assign counterValue = beStatus.targetCounters['Documents submitted'].Count]
      [#else]
          [#assign counterValue = '']
      [/#if]
            <td id="documentsSubmitted">${counterValue}</td>
      [#if beStatus?? && beStatus.targetCounters??]
        [#assign counterValue = beStatus.targetCounters['Documents completed'].Count]
      [#else]
          [#assign counterValue = '']
      [/#if]
            <td id="documentsCompleted">${counterValue}</td>
      [#if beStatus?? && beStatus.targetCounters??]
        [#assign counterValue = beStatus.targetCounters['Documents errors'].Count]
      [#else]
          [#assign counterValue = '']
      [/#if]
            <td id="documentsErrors">${counterValue}</td>
      [#if beStatus?? && beStatus.targetCounters??]
        [#assign counterValue = beStatus.targetCounters['Documents skipped'].Count]
      [#else]
          [#assign counterValue = '']
      [/#if]
            <td id="documentsSkipped">${counterValue}</td>
          </tr>
          <tr>
            <td>Documents Metadata</td>
      [#if beStatus?? && beStatus.targetCounters??]
        [#assign counterValue = beStatus.targetCounters['Documents submitted'].Count]
      [#else]
          [#assign counterValue = '']
      [/#if]
            <td id="documentsMetadaSubmitted">${counterValue}</td>
      [#if beStatus?? && beStatus.targetCounters??]
        [#assign counterValue = beStatus.targetCounters['Documents metadata completed'].Count]
      [#else]
          [#assign counterValue = '']
      [/#if]
            <td id="documentsMetadataCompleted">${counterValue}</td>
      [#if beStatus?? && beStatus.targetCounters??]
        [#assign counterValue = beStatus.targetCounters['Documents metadata errors'].Count]
      [#else]
          [#assign counterValue = '']
      [/#if]
            <td id="documentsMetadataErrors">${counterValue}</td>
      [#if beStatus?? && beStatus.targetCounters??]
        [#assign counterValue = beStatus.targetCounters['Documents metadata skipped'].Count]
      [#else]
          [#assign counterValue = '']
      [/#if]
            <td id="documentsMetadataSkipped">${counterValue}</td>
          </tr>
        </tbody>
      </table>
      </div>
      <div>
        <table border="1" callspacing="0" cellpadding="1" width="80%">
        <thead>
          <tr>
            <th>Counter</th>
            <th>Discovered</th>
            <th>Completed</th>
            <th>Errors</th>
            <th>Skipped</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td>Versions</td>
      [#if beStatus?? && beStatus.targetCounters??]
        [#assign counterValue = beStatus.targetCounters['Versions submitted'].Count]
      [#else]
          [#assign counterValue = '']
      [/#if]
            <td id="versionsSubmitted">${counterValue}</td>
      [#if beStatus?? && beStatus.targetCounters??]
        [#assign counterValue = beStatus.targetCounters['Versions completed'].Count]
      [#else]
          [#assign counterValue = '']
      [/#if]
            <td id="versionsCompleted">${counterValue}</td>
      [#if beStatus?? && beStatus.targetCounters??]
        [#assign counterValue = beStatus.targetCounters['Versions errors'].Count]
      [#else]
          [#assign counterValue = '']
      [/#if]
            <td id="versionsErrors">${counterValue}</td>
      [#if beStatus?? && beStatus.targetCounters??]
        [#assign counterValue = beStatus.targetCounters['Versions skipped'].Count]
      [#else]
          [#assign counterValue = '']
      [/#if]
            <td id="versionsSkipped">${counterValue}</td>
          </tr>
          <tr>
            <td>Versions Metadata</td>
      [#if beStatus?? && beStatus.targetCounters??]
        [#assign counterValue = beStatus.targetCounters['Versions submitted'].Count]
      [#else]
          [#assign counterValue = '']
      [/#if]
            <td id="versionsMetadaSubmitted">${counterValue}</td>
      [#if beStatus?? && beStatus.targetCounters??]
        [#assign counterValue = beStatus.targetCounters['Versions metadata completed'].Count]
      [#else]
          [#assign counterValue = '']
      [/#if]
            <td id="versionsMetadataCompleted">${counterValue}</td>
      [#if beStatus?? && beStatus.targetCounters??]
        [#assign counterValue = beStatus.targetCounters['Versions metadata errors'].Count]
      [#else]
          [#assign counterValue = '']
      [/#if]
            <td id="versionsMetadataErrors">${counterValue}</td>
      [#if beStatus?? && beStatus.targetCounters??]
        [#assign counterValue = beStatus.targetCounters['Versions metadata skipped'].Count]
      [#else]
          [#assign counterValue = '']
      [/#if]
            <td id="versionsMetadataSkipped">${counterValue}</td>
          </tr>
        </tbody>
      </table>
    </div>
    
    <p>
    <a id="initiateAnotherImport" style="display:none" href="${url.serviceContext}/bulk/export/ui">Initiate another?</a>

    <script type="text/javascript">
      $(document).ready(function() {
        initStatus('${url.context?js_string}', '${url.serviceContext?js_string}');
      });
    </script>
    </body>
</html>
