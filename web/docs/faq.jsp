<%@ page contentType="text/html; charset=UTF-8" %>
<%@ include file="../TagLibIncludes.jsp" %>
<%@ include file="../baseUrl.jsp" %>
<c:set var="rm" value="${applicationScope.repositoryManager}"/>

<html>

<head>
<title>Frequently Asked Questions (FAQ)</title>
<c:import url="../head.jsp"/>

<style type="text/css">
<!--
.style2 {padding-top:15px; font-family: "Courier New", Courier, mono; font-weight: bold; }
-->
</style>
</head>

<body>
<a name="top"></a><c:import url="../top.jsp?sec=doc"/>
<!-- content begins-->
<h1>Frequently Asked Questions (FAQ)</h1>
<p>This FAQ includes the following sections: </p>
<table border="0" align="left" cellpadding="0" cellspacing="0">
  <tr>
    <td align="left"><a href="faq.jsp#provider">Data Provider FAQ</a></td>
  </tr>
  <tr>
    <td align="left"><a href="faq.jsp#harvester">Harvester FAQ</a></td>
  </tr>
  <tr>
    <td align="left"><a href="faq.jsp#general">General FAQ</a></td>
  </tr> 
</table>
<p>&nbsp;</p>
<p>&nbsp;</p>
<p>&nbsp;</p>
<h3><a name="provider" id="provider"></a>Data Provider FAQ</h3>
<p><strong>Can I create a custom search page for the items in the data provider?</strong></p>
<p>Yes. The <a href="<c:url value='/search.jsp'/>">Search</a> page can be customized and easily installed to another location or remote Web server as described in the <a href="<c:url value='/docs/odlsearch.do'/>">ODL Search Specification</a> page. </p>
<p><strong>What is different between the 'Search' and 'Admin search' pages?</strong></p>
<p> The <a href="<c:url value='/search.jsp'/>">Search</a> page is intended to be accessed by the general public. It provides search 
  over the full text found in all files that can be disseminated in oai_dc format. 
  This includes files that reside natively in oai_dc format as well as files that 
  can be converted to oai_dc as described in <a href="<c:url value='/docs/provider.jsp#convertors'/>">Providing 
  files in multiple formats</a>. In addition, only files that are enabled in the 
  <a href="<c:url value='/admin/data-provider.do'/>">Metadata Files Configuration</a> 
  page are available for search. </p>
<p>The <a href="<c:url value='/admin/query.do'/>">Admin search</a>  page resides in the administration portion of the software and its access is intended to be restricted to trusted users as described in the <a href="<c:url value='/docs/configuring_joai.jsp'/>">Configuring jOAI</a> page. It provides 
  search over the full text found in all XML files that are configured in the 
  data provider. Files are searchable and viewable even when public access to 
  them has been disabled in the <a href="<c:url value='/admin/data-provider.do'/>">Metadata 
  Files Configuration</a> page. Admin search also provides options to search by 
set, available format, and record attribute (not deleted, deleted, etc.).</p>
<p><strong>If my records reside in a database,  can I use jOAI to implement an OAI data provider for my repository?</strong></p>
<p>The jOAI data provider allows XML files from a file system to be exposed as items in an OAI data repository. To expose records that reside in a database, write a routine to export the records to XML files  at regular intervals such as once a day or once a week, depending on how often the records change.  Then <a href="<c:url value='/docs/provider.jsp#provsetup'/>">setup the data provider</a> to monitor the  directory or directories where the files are exported to. See also <a href="<c:url value='/docs/provider.jsp#directories'/>">preparing files for serving</a>.</p>
<p>After the initial set of records have been exported from the database,  files should be modified or deleted only when the corresponding database record has been updated or deleted. jOAI will monitor the files and provide them to harvesters according to the OAI-PMH.</p>
<p><strong>Does jOAI support selective or incremental harvesting?</strong></p>
<p>Yes. After performing an initial full harvest of the repository, harvesters may use <a href="http://www.openarchives.org/OAI/2.0/openarchivesprotocol.htm#Datestamp">datestamps</a> to request only those records that have changed or been deleted since that last time of harvest, which can greatly reduce the number of records transferred over the network over time. The data provider implements deleted records and datestamps in accordance with the OAI-PMH to support  selective and incremental harvests.</p>
<p><strong>If I remove a file, can I add it back at a later time?</strong></p>
<p>When a file is removed from a directory that is being monitored by the data 
  provider, it's record will be changed to status deleted, and harvesters will 
  be notified that the record has been deleted the next time they harvest from 
  the data provider. At a later time, if a file is added back with the same unique 
  ID as a deleted record (regardless of the directory), the data provider will 
  replace it with the new one, and it's status will no longer be deleted. Harvesters 
  will then receive the new record the next time they harvest from the data provider. 
</p>
<p><strong>What happens if I accidentally create two files with the same ID?</strong></p>
<p>When jOAI imports a new or modified file into it's index, a check is performed to see if there is an existing record with the same unique ID. If the ID already exists, an error will be reported under 'Indexing Errors' in the the <a href="<c:url value='/admin/data-provider.do'/>">Metadata Files Configuration</a> page, and the file will not be imported into the repository index. To fix the problem the file must either be removed from the directory or a unique ID should be assigned to the file, as described under <a href="<c:url value='/docs/provider.jsp#directories'/>">preparing files for serving</a>.</p>
<p><strong>My records have indexing errors when I put them in the data provider. What's wrong?</strong></p>
<p>  XML files that contain text that was copied and pasted from tools such as Microsoft Word often contain invalid characters such as dashes or copyright symbols that are improperly encoded. These 'bad characters' can trigger the XML processors in the software to issue an error. Files must contain well-formed XML and should use UTF-8 encoding. Character references, rather than entity references, should also be used for special characters, as required by the OAI protocol <a href="http://www.openarchives.org/OAI/openarchivesprotocol.html#XMLResponse">XML response format</a>.</p>
<p><strong>How many records can the data provider scale to?</strong></p>
<p>The jOAI data provider is designed for small to medium size data repositories. 
  The software has been tested successfully with repositories up to 300,000 records. 
  The number of records the software can support depends on the amount of memory 
  available to the Java JVM, the speed of the host machine and the size of the 
  individual records in the repository.</p>
<p><strong>The baseURL that is shown uses the local machine name, but it should use the domain name for the server instead. How can I change it?</strong></p>
<p>The base URL that is shown on the <a href="<c:url value='/'/>">front page</a> 
  and <a href="<c:url value='/admin/data-provider-info.do'/>">Repository Information 
  page</a> of jOAI and elsewhere reflects the URL that was entered into the web 
  browser when connecting to the software. For example, if a user accesses jOAI 
  using the web address http://localhost:8080${pageContext.request.contextPath}, 
  the baseURL will be shown as 'http://localhost:8080${pageContext.request.contextPath}/${initParam.dataProviderBaseUrlPathEnding}'. 
  If the user connects to the same instance of jOAI using the Internet address 
  http://myserver.somewhere.edu${pageContext.request.contextPath}, the baseURL 
  will be shown as 'http://myserver.somewhere.edu${pageContext.request.contextPath}/${initParam.dataProviderBaseUrlPathEnding}'. 
</p>
<p>&nbsp;</p>
<p><a href="faq.jsp#top"> <img src="<c:url value='/images/arrowup.gif'/>" alt="top" width="9" height="11" border="0" title="back to top"></a></p>
<hr>
<h3><a name="harvester" id="harvester"></a>Harvester FAQ</h3>
<p><strong>Where are the harvested records and zip archives saved to?</strong></p>
<p>The harvester saves the records that are harvested into individual files on 
  the file system, one record per file. Files are saved to either a default directory 
  (which is named based upon the name of the provider and optionally the set that 
  is being harvested) or a specific directory that was specified when <a href="<c:url value='/docs/harvester.jsp#how'/>">setting 
  up</a> the harvest. Each harvest is then packaged into a zip archive. </p>
<p> To determine where files and zip archives were saved to after a harvest has 
  occurred, go to the <a href="<c:url value='/admin/harvester.do'/>">Harvester 
  Setup and Status</a> page, then click on 'View harvest history' for a given 
  harvest. This brings up the detailed history of harvests and shows the full 
  directory path to the harvested files and zip archives.</p>
<p>Each time a harvest is performed for a given harvest configuration, files in 
  the harvest directory may be added, updated or deleted by the harvester depending 
  on the outcome of the harvest. If configured for zipping, at the conclusion of each harvest that results 
  in a change to one or more files, a new zip archive is created, and a maximum 
  of three zip archives are preserved at any given time. Each zip archive contains 
  the exact time of the harvest in its name. The zip archives for each harvest 
  may be downloaded directly from the <a href="<c:url value='/admin/harvester.do'/>">Harvester 
  Setup and Status</a> page or accessed from the file system. </p>
<p><strong>Can I use jOAI to harvest records into a database?</strong></p>
<p>There are two ways in which the harvester may be used to import records into 
  a database.</p>
<p>The first method, which uses the jOAI web application, requires two parts. 
  First, configure the jOAI harvester to save files to a convenient directory 
  at regular intervals, such as once a day. Then, write a routine to monitor the 
  file directory and add, update or delete the corresponding records to the database 
  when changes occur in the files. </p>
<p>Another method is to use the <a href="<c:url value='/docs/javadoc/org/dlese/dpc/oai/harvester/Harvester.html'/>">Harvester 
  API</a> from within native Java code to perform harvests and import metadata 
  records directly into a database.</p>
<p><strong>Does the harvester support selective or incremental harvesting?</strong></p>
<p>Yes. When an automatic harvest is conducted at regular intervals, the harvester 
  checks if the data provider supports deleted records. If deletions are supported, 
  a <a href="http://www.openarchives.org/OAI/2.0/openarchivesprotocol.htm#Datestamp">selective 
  harvest</a> is performed by requesting and synchronizing only those records 
  that have been added, modified or deleted since the previous harvest. If deletions 
  are not supported, a full harvest is performed by deleting all previously harvested 
  records and harvesting all records from scratch.</p>
<p>Similarly when a manual harvest is performed, clicking 'New' performs a selective 
  harvest while clicking 'All' deletes all previously harvested files and performs 
  a full harvest from scratch.</p>
<p><strong>The files that are saved by the harvester include characters like '%3A' 
  in their names. Why is that?</strong></p>
<p>When the harvester saves records, it places each record in a single file, which 
  is named using the OAI identifier associated with the record that was harvested. 
  Reserved characters such as the colon ':' are encoded using hexadecimal values 
  in order to ensure the file name is valid on the file system. For example, if 
  the OAI identifier for a given harvested record is oai:dlese.org:123-ABC, the 
  file will be named oai%3Adlese.org%3A123-ABC.xml. The hexadecimal characters 
  can be converted back to the original form as needed. </p>
<p><strong>How can I provide records that I harvest?</strong></p>
<p>Currently it is a two step process to make records that are harvested available 
  through the data provider. First, harvest the records to a convenient file directory. 
  Second, configure the data provider to point to the same file directory. As 
  new records are added, modified or deleted by the harvester, these changes will 
  be reflected and passed along in the data provider.</p>
<p><strong>Can I search over and view the records I harvest?</strong></p>
<p>The harvester portion of the software does not currently support searching 
  and viewing harvested records directly. However, by configuring harvested records 
  in the data provider as mentioned above, the records will become searchable 
  and viewable in the 'Search' and 'Admin Search' pages.</p>
<p>&nbsp;</p>
<p><a href="faq.jsp#top"> <img src="<c:url value='/images/arrowup.gif'/>" alt="top" width="9" height="11" border="0" title="back to top"></a></p>
<hr>
<h3><a name="general" id="general"></a>General FAQ</h3>
<p><strong>Is it possible to customize  the CSS, HTML or other features of the jOAI user interface?</strong></p>
<p>Yes. jOAI is rendered using CSS, HTML (which is generated by JSP), and JavaScript. Simply modify the .css, .jsp or .js files as desired to change the look-and-feel of the application. Tip: The struts-config.xml file found in WEB-INF describes what .jsp files are used to render certain portions of the application. The file head.jsp defines what items appear in the jOAI menus.</p>
<p><strong>Can I configure jOAI to store  my settings and data in a permanent location for backup or reinstallation purposes?</strong></p>
<p>Yes. jOAI saves it's configuration files and stored data inside file directories. By default, these are located inside the WEB-INF directory of the jOAI installation. To store these in a global directory, set the repositoryData and harvesterData configuration parameters to a point to a directory of your choice. See the section titled 'Configure software settings' in the <a href="configuring_joai.jsp">Configuring jOAI</a> page for details. </p>
<p><strong>When upgrading or reinstalling jOAI, how do I preserve the the settings, indexes and  files for the data provider and harvester?</strong></p>
<p>If you have previously configured jOAI to store it's configuration files and data in a global directory as described above, you can simply stop Tomcat, upgrade or reinstall the jOAI software (oai.war) and start Tomcat again. Then visit the jOAI  admin and search pages to confirm that the settings and indexes have been preserved for the data provider and harvester. In some cases it may be necessary to re-index the files in the data provider for changes to be seen. Before upgrading or reinstalling, be sure to make a backup copy of your settings and data in case you need to revert back for any reason. </p>
<p>If you have not configured jOAI to store it's files in a global directory, follow these steps:</p>
<p>1. Stop Tomcat</p>
<p>2. Move and save the current oai installation to a location outside the webapps directory. (Backup and save a copy).</p>
<p>3. Install the new version of jOAI (put oai.war in webapps, start Tomcat, etc). Tomcat will unpack the new oai.war file.</p>
<p>Then, to restore the previous settings, indexes and files:</p>
<p>4. Stop tomcat again.</p>
<p>5. In the new webapps/oai/WEB-INF directory, replace the two directories 'repository_settings_and_data' and 'harvester_settings_and_data' with the ones saved from the previous installation.</p>
<p>6. Start Tomcat.</p>
<p>7. Visit the jOAI  admin and search pages to confirm that the settings and indexes have been preserved for the data provider and harvester.</p>
<p><strong>Can jOAI be configured to run through an Apache web server (httpd)?</strong></p>
<p>Yes. Running jOAI through an <a href="http://httpd.apache.org/">Apache web 
  server</a> provides additional functionality that is not available through <a href="http://tomcat.apache.org/">Tomcat</a> 
  alone. For example, Apache provides robust support for SSL, user authorization 
  and authentication, access control by IP address, virtual host support, web 
  logging, URL redirection, and other functionality. By configuring Tomcat to 
  run through Apache, all of Apache's functionality becomes available. This may 
  be especially convenient for web administrators who are already familiar with 
  Apache.</p>
<p>One of two Apache modules  may be used to connect Apache with Tomcat: mod_proxy or mod_jk. Choose  one or the other:</p>
<ul><li>mod_proxy - Information for setting up mod_proxy is provided in the <a href="http://httpd.apache.org/docs/2.0/mod/mod_proxy.html">Apache Module mod_proxy documentation</a>, with additional configuration information specific to Tomcat provided in the <a href="http://tomcat.apache.org/tomcat-5.5-doc/proxy-howto.html">Tomcat proxy how to documentation</a> (proxyName and proxyPort attributes must be added to  the non-SSL and SSL HTTP &lt;Connector&gt; elements in Tomcat's server.xml to ensure that URLs in jOAI and other Web applications that rely on the <code>ServletRequest.getServerName() and related Java </code> methods will resolve properly when using mod_proxy).</li>
  <li>mod_jk - Information for setting up mod_jk is provided in the <a href="http://tomcat.apache.org/connectors-doc/">Apache Tomcat Connector documentation</a>.</li>
</ul>
<p>After setting up mod_proxy or mod_jk, a typical configuration scenario would be to use Apache to provide SSL encryption, 
  user authorization and authentication for all pages that reside in the admin 
  area of the software (e.g. https://oai.somewhere.edu/oai/admin*), while leaving 
  all other public jOAI pages open. This scenario provides a relatively secure 
  way to restrict access to the administrative functions of the software to trusted 
  users while leaving access to the data provider, search and other public pages 
  open. </p>
<p>Another scenario might be to restrict access to the software or portions of 
  the software by requestors IP address.</p>
<p>See the <a href="http://httpd.apache.org/docs/">Apache documentation</a> for 
  a list of available features and configuration information.</p>
<p><strong>Can jOAI be integrated into an existing Web application? </strong></p>
<p>Yes. It is recommended that jOAI be run as a stand-alone Web application, however it is possible to integrate it into an existing Web application. Either the data provider, the harvester or both can be configured. Here is a general outline of how this may be done:</p>
<blockquote>
  <p>1. Copy the configuration from web.xml:</p>
  <ul>
    <li>All &lt;servlet&gt; elements OAIProviderServlet, OAIHarvesterServlet and action.</li>
    <li>All &lt;context-param&gt; elements</li>
    <li>All &lt;filter&gt; and &lt;filter-mapping&gt; elements</li>
    <li>All &lt;servlet-mapping&gt; elements</li>
    <li>All &lt;taglib&gt; elements</li>
    <li>Optionally, copy over the &lt;welcome-file-list&gt; and &lt;error-page&gt; elements</li>
  </ul>
  <p>2. Copy over files struts-config.xml, users.xml, validation.xml, validator-rules.xml from /WEB-INF to your application.</p>
  <p>3. Copy over all JAR files from /WEB-INF/libs (some may not be required)</p>
  <p>4. Copy over directories /WEB-INF/classes, /WEB-INF/tlds, /WEB-INF/xsl_files and /WEB-INF/conf. Optionally /WEB-INF/error_pages (if configured in web.xml).</p>
  <p>5. Copy over all .jsp, .js and .css files from the root and the /oai_requests, /admin, /docs (optional), /images directories.</p>
  <ul>
    <li>Optionally edit, add and remove jsp and css files as needed. The OAI protocol is handled by the pages found in /oai_requests. Administration is handled by the pages found in /admin. The WEB-INF/struts-config.xml file is used to configure URL paths to the JSP pages that handle them, via the Action.</li>
  </ul>
</blockquote>
<p>&nbsp; </p>
<p>&nbsp;</p>
<p><a href="faq.jsp#top"><img src="<c:url value='/images/arrowup.gif'/>" alt="top" width="9" height="11" border="0" title="back to top"></a></p>
<hr>
<c:import url="../bottom.jsp"/> 
</body>
</html>

