<!DOCTYPE html>
<!--[if lt IE 7]>      <html class="no-js lt-ie9 lt-ie8 lt-ie7"> <![endif]-->
<!--[if IE 7]>         <html class="no-js lt-ie9 lt-ie8"> <![endif]-->
<!--[if IE 8]>         <html class="no-js lt-ie9"> <![endif]-->
<!--[if gt IE 8]><!--> <html class="no-js"> <!--<![endif]-->
<html>
  <head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <title>Bulk Export Initiate</title>
    <meta name="description" content="Bulk Export Initiate">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <link rel="stylesheet" href="${url.context}/scripts/bulk/jquery/1.12.0/jquery-ui.min.css">
    <script src="${url.context}/scripts/bulk/jquery/2.2.4/jquery-2.2.4.min.js"></script>
    <script src="${url.context}/scripts/bulk/jquery/1.12.0/jquery-ui.min.js"></script>
    <script src="${url.context}/scripts/bulk/loglevel/1.4.0/loglevel.min.js"></script>
    <script src="${url.context}/scripts/bulkimport/modernizr-3.3.1.min.js"></script>
    
  </head>
  <body>
    <!--[if lt IE 9]>
        <p class="browsehappy">You are using an <strong>outdated</strong> browser. Please <a href="http://browsehappy.com/">upgrade your browser</a> to improve your experience.</p>
    <![endif]-->
    <fieldset><legend>Log</legend>
	    <div class="container">
	      <#if activityLog??>
	        <#list activityLog as log> ${log} </#list>
	      </#if>
	    </div>
    </fieldset>
    
    <fieldset><legend>Errors</legend>
	    <div class="container">
	      <#if errorLog??>
	        <#list errorLog as error> ${error} <br /></#list>
	      </#if>
	    </div>
    </fieldset>
    
    <fieldset><legend>Results</legend>
    Processed <#if nodecount??>${nodecount}<#else>0</#if> nodes
    Elapsed time: minutes: <#if durationmins??>${durationmins}<#else>0</#if>, seconds <#if duration??>${duration}<#else>0</#if>
    </fieldset>

	<div>
    	<a href="${url.serviceContext}/bulk/export/ui">Initiate another?</a>
    </div>
  </body>
</html>

