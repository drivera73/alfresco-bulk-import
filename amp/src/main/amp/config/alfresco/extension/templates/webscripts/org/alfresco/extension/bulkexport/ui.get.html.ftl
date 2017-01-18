[#ftl]
<!DOCTYPE html>
<!--[if lt IE 7]>      <html class="no-js lt-ie9 lt-ie8 lt-ie7"> <![endif]-->
<!--[if IE 7]>         <html class="no-js lt-ie9 lt-ie8"> <![endif]-->
<!--[if IE 8]>         <html class="no-js lt-ie9"> <![endif]-->
<!--[if gt IE 8]><!--> <html class="no-js"> <!--<![endif]-->
<html>
  <head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <title>Bulk Export Tool</title>
    <meta name="description" content="Bulk Export Tool">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    [#-- 3rd Party Stuff --]
    <link rel="stylesheet" href="${url.context}/scripts/bulk/jquery/1.12.0/jquery-ui.min.css">
    <script src="${url.context}/scripts/bulk/jquery/2.2.4/jquery-2.2.4.min.js"></script>
    <script src="${url.context}/scripts/bulk/jquery/1.12.0/jquery-ui.min.js"></script>
    <script src="${url.context}/scripts/bulk/loglevel/1.4.0/loglevel.min.js"></script>
    <script src="${url.context}/scripts/bulkimport/modernizr-3.3.1.min.js"></script>
    <link rel="stylesheet" href="${url.context}/css/bulkimport/normalize.css">
    <link rel="stylesheet" href="${url.context}/css/bulkimport/main.css">
  </head>
  <body>
    <!--[if lt IE 9]>
        <p class="browsehappy">You are using an <strong>outdated</strong> browser. Please <a href="http://browsehappy.com/">upgrade your browser</a> to improve your experience.</p>
    <![endif]-->
    <div class="container">
      <div class="block">
        <img style="margin:15px;vertical-align:middle" src="${url.context}/images/bulkexport/logo.png" alt="Bulk Export Tool" />
      </div>
      <div class="block">
        <h1><strong>Bulk Export Tool</strong></h1>
      </div>
    </div>

    <form id="initiateBulkExportForm" action="${url.serviceContext}/bulk/export/initiate" method="post" charset="utf-8">
      <fieldset><legend>Bulk Export Settings</legend>
        <p><label for="sourceLocation">Source Location:</label> <input type="text" id="sourceLocation" name="sourceLocation" size="80" required/></p>
        <p><label for="exportLocation">Export Location:</label> <input type="text" id="exportLocation" name="exportLocation" size="80" required/></p>
        <table border="0">
          <tr>
            <td>
              <label for="scapeExported">Ignore Exported:</label> <input type="checkbox" id="scapeExported" name="scapeExported" checked/>
            </td>
            <td>
              <label for="exportVersions">Export Versions:</label> <input type="checkbox" id="exportVersions" name="exportVersions" checked/>
            </td>
          </tr>
          <tr>
            <td>
              <label for="includeContent">Include Content:</label> <input type="checkbox" id="includeContent" name="includeContent"/>
            </td>
            <td>
              <label for="includeMetadata">Include Metadata:</label> <input type="checkbox" id="includeMetadata" name="includeMetadata" checked/>
            </td>
          </tr>
          <tr>
            <td>
              <label for="foldersOnly">Folders Only:</label> <input type="checkbox" id="foldersOnly" name="foldersOnly"/>
            </td>
            <td>
              <label for="truncatePath">Truncate Path:</label> <input type="checkbox" id="truncatePath" name="truncatePath" checked/>
            </td>
          </tr>
          <tr>
            <td>
              <label for="revisionHead">Revision Head:</label> <input type="checkbox" id="revisionHead" name="revisionHead"/>
            </td>
            <td>
              <label for="useNodeCache">Use Node Cache:</label> <input type="checkbox" id="useNodeCache" name="useNodeCache"/>
            </td>
          </tr>
          <tr>
            <td>
              <label for="checkSum">Checksum:</label> <select name="checkSum">
                <option value="MD5">MD5</option>
                <option value="SHA-128">SHA-128</option>
                <option value="SHA-256" selected>SHA-256</option>
                <option value="SHA-512">SHA-512</option></select>
            </td>
            <td>&nbsp;
            </td>
          </tr>
        </table>
        <p><label for="documentProperty">Document Export Property:</label> <input type="text" id="documentProperty" name="documentProperty" size="80" value="{http://www.armedia.com/model/documentum/1.0}i_chronicle_id,{http://www.armedia.com/model/documentum/1.0}r_object_id"/></p>
        <p><label for="folderProperty">Folder Export Property:</label> <input type="text" id="folderProperty" name="folderProperty" size="80" value="{http://www.armedia.com/model/documentum/1.0}r_object_id"/></p>
        <p><label for="batchSize">Batch Size:</label> <input type="text" id="batchSize" name="batchSize" size="3" value="100"/></p>
        <p><label for="threadCount">No. of Threads:</label> <input type="text" id="threadCount" name="threadCount" size="2" value="4"/></p>
      </fieldset>
      <p>

      <p><button class="button green" type="submit" name="submit">&#9658; Initiate Bulk Export Activities</button></p>
    </form>
    <script>
      [#-- Target field autocomplete --]
      $(function() {
        $('#sourceLocation').autocomplete({
          source: '${url.service}/../import/ajax/suggest/spaces.json',
          minLength: 2
        });
      });
    </script>
  </body>
</html>
