<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix='dds' uri='http://www.dlese.org/dpc/dds/tags' %>
<%@ taglib prefix="x" uri="http://java.sun.com/jsp/jstl/xml" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page isELIgnored ="false" %>

<html>

<head>
<c:set var="title" value="Configuring jOAI" />

<title>${title}</title>
<c:import url="../head.jsp"/>
<style type="text/css">
<!--
.style2 {padding-top:15px; font-family: "Courier New", Courier, mono; font-weight: bold; }
-->
</style>
</head>

<body>
<c:import url="../top.jsp?sec=doc"/>
<!-- content begins-->

<a name="top"></a><h1>${title}</h1>

<p>This page describes options for configuring and customizing your jOAI  software.  These instructions assume the software has already been installed according to the <a href='<c:url value="/docs/INSTALL.txt"/>'>installation instructions</a>.</p>
<p>&nbsp;</p>
<hr align="JUSTIFY">
<h3 align="justify"><a name="searchClient"></a>Create a custom search page </h3>
<p>A search client template is included with jOAI that can be used to implement a custom search page for your data provider. See the <a href="<c:url value='/docs/odlsearch.do#searchClient'/>">search client template</a> section in the ODL documentation for information about using and customizing the template. </p>
<p>&nbsp;</p>
<hr>
<h3><a name="accessControl"></a>Enable access control in Tomcat (BASIC Auth)</h3>
<p>Restrict access to the software administrative pages so that the public does not
have access to sensitive information or can change administrative settings. To enable 
BASIC Auth protection for the administrative pages, do the following (Tomcat v7 or v8):</p>
<p>1. Uncomment the 'security-constraint' and 'login-config' elements found 
in the oai "<span class="code">WEB-INF/web.xml</span>" file (if needed). Assuming a default installation, this
would be located at <span class="code">$CATALINA_HOME/webapps/oai/WEB-INF/web.xml</span> (<span class="code">$CATALINA_HOME</span> refers to the location of the Tomcat installation).</p>
<p>2. Edit the default '<span class="code">$CATALINA_HOME/conf/tomcat-users.xml</span>' file and define a role named '<span class="code">oai_admin</span>'
    and one or more users that are assigned to this role. See the documentation in the <span class="code">tomcat-users.xml</span>
    file for details.</p>
<p>3. Start or restart Tomcat. </p>
<p>4. (recommended) BASIC auth does not encrypt user names and passwords sent over the Internet. To enable encryption,
    configure jOAI to run under https/ssl. You may use Apache Module <span class="code">mod_proxy_ajp</span>
    (or the older <span class="code">mod_jk</span>) to proxy the jOAI webapp through your Apache HTTP server and
    configure https/ssl to the jOAI administrative pages that way
    (see Apache HTTP server docs for details).</p>
<p><br>
</p>
<hr>
<h3><a name="utf8"></a>Configure UTF-8 URI encoding for Tomcat</h3>
<p> When Tomcat decodes the URLs it receives from a browser, it normally uses ISO-8859-1 character encoding. This causes problems in the forms throughout jOAI when UTF-8 characters are used as input.</p>
<ol>
  <li>Edit the $tomcat/conf/server.xml file at the line where the HTTP Connector is defined, which should look something like<br>
    <br>
    <span class="code">&lt;Connector port=&quot;8080&quot; ... /&gt;</span><br>
    <br>
  </li>
  <li>Add the attribute <span class="code">URIEncoding=&quot;UTF-8&quot;</span><br>
    <br>
    <span class="code">&lt;Connector port=&quot;8080&quot; URIEncoding=&quot;UTF-8&quot; ... /&gt;</span><br>
    <br>
  </li>
  <li>Restart Tomcat </li>
</ol>
<p>If you are using mod_jk to proxy between Apache and Tomcat, you should also apply this setting for the AJP connector in your Apache mod_jk configuration:</p>
<blockquote>
  <p>
    <span class="code">&lt;Connector port=&quot;8009&quot; protocol=&quot;AJP/1.3&quot; URIEncoding=&quot;UTF-8&quot;/&gt;</span><br>
    <br>
    <span class="code">JkOptions +ForwardURICompatUnparsed</span></p>
</blockquote>
<p>&nbsp; </p>
<hr>
<h3>Configure multiple formats in the data provider </h3>
<p>The jOAI data provider can disseminate any given metadata file in multiple formats. See <a href='<c:url value="/docs/provider.jsp#convertors"/>'>Providing files in multiple formats</a> for information about configuring this functionality.</p>
<p>&nbsp;</p>
<hr>
<h3><a name="settings"></a>Configure software settings</h3>
<p>Change certain settings and behaviors in the data provider  and harvester by editing the following parameters in the &quot;web.xml&quot; or &quot;server.xml&quot; files. Changes made in web.xml are overwritten when the software is re-installed. Changes made in server.xml are not overwritten. The following assumes the use of at least Tomcat 5.5.x or 6.x. </p>
<p>To make &quot;web.xml&quot; changes, use the OAI &quot;WEB-INF/web.xml&quot; file. Assuming a default installation, this would be located at <span class="code">$CATALINA_HOME/webapps/oai/WEB-INF/web.xml</span> (<span class="code">$CATALINA_HOME</span> refers to the location of the Tomcat installation).</p>
<p>To make &quot;server.xml&quot; changes, use the file in the Tomcat &quot;conf&quot; directory. Set up a &lt;Context&gt; definition inside the &lt;Host&gt; element for jOAI, and add context parameters as needed. For example:</p>
<p>
<pre class="code">
	&lt;Context path=&quot;/oai&quot; docBase=&quot;oai&quot; debug=&quot;0&quot; reloadable=&quot;true&quot;&gt;
		&lt;Parameter
			name=&quot;repositoryData&quot;
			value=&quot;/path/to/repository_settings_and_data&quot;
			override=&quot;false&quot; /&gt;
		&lt;Parameter
			name=&quot;harvesterData&quot;
			value=&quot;/path/to/harvester_settings_and_data&quot;
			override=&quot;false&quot; /&gt;
	&lt;/Context&gt;</pre>
<p>Any changes to these parameters requires a restart of Tomcat.</p>
<p class="style2">&nbsp;</p>
<p><span class="style2">repositoryData</span> </p>
<p>This parameter defines where the repository indexes and configuration files are stored on disc. These include the index of metadata files, metadata files configuration, sets information, repository name and email and other persistent data used by the data provider. By default, these are stored inside the WEB-INF directory of the jOAI installation. Set this to a directory outside of the Tomcat installation to make it easy to upgrade or reinstall jOAI while preserving your settings. You may backup and restore this directory to preserve a snapshot of data and settings. This directory must be on a local, non-network drive. </p>
<blockquote>
  <p class="code"> &lt;Parameter name=&quot;repositoryData&quot; value=&quot;/oai_data/joai_settings_and_data/repository_settings_and_data&quot; override=&quot;false&quot;/&gt; </p>
</blockquote>
<p>The value should be an absolute directory path.</p>
<p class="style2">harvesterData</p>
<p>This parameter defines where harvester indexes and configuration files are stored on disc. These include an index of harvest history, the harvest configuration and other persistent data used by the harvester. By default, these are stored inside the WEB-INF directory of the jOAI installation. Set this to a directory outside of the Tomcat installation to make it easy to upgrade or reinstall jOAI while preserving your settings. You may backup and restore this directory to preserve a snapshot of data and settings.This directory must be on a local, non-network drive.</p>
<blockquote>
  <p class="code">&lt;Parameter name=&quot;harvesterData&quot; value=&quot;/oai_data/joai_settings_and_data/harvester_settings_and_data&quot; override=&quot;false&quot;/&gt;</p>
</blockquote>
<p>The value should be an absolute directory path.</p>
<p class="style2">updateFrequency</p>
<p>This parameter defines the indexing interval for synching the data provider index with the files. 
</p>
<blockquote>
<p class="code">
&lt;Parameter name=&quot;updateFrequency&quot; value=&quot;1440&quot; override=&quot;false&quot;/&gt;
</p>
</blockquote>
<p>The value is in minutes. Set to <span class="code">0</span> to disable automatic indexing and allow manual indexing only.</p>
<p class="style2">hideAdminMenus</p>
<p>This parameter may be used to hide the 'Data Provider' and 'Harvester' administrative menus in the user interface, which may be desirable for some public installations of the software. Note that this only hides but does not disable or restrict access to those pages. See <a href="#accessControl">Enableing access control in Tomcat</a>. </p>
<blockquote>
  <p class="code"> &lt;Parameter name=&quot;hideAdminMenus&quot; value=&quot;true&quot; override=&quot;false&quot;/&gt; </p>
</blockquote>
<p>Set to 'true' to hide the administrative menus from users. </p>
<p class="style2">dataProviderAccessLogLevel</p>
<p>This parameter  controls when to log client (harvester) requests made to the data provider. The log is stored as an index and can be viewed from the <a href="<c:url value='/admin/provider_status_setup.jsp'/>">provider status</a> page (see Reports). The index is stored on disc and over time can grow large. To reduce or eliminate the write operations to this index use the 'FinalResumption' or 'NoLog' settings. </p>
<blockquote>
  <p class="code"> &lt;Parameter name=&quot;dataProviderAccessLogLevel&quot; value=&quot;FinalResumption&quot; override=&quot;false&quot;/&gt; </p>
</blockquote>
<p>Set to 'Full' to log all client interactions with the data provider. Set to 'FinalResumption' to log only client requests for the final segment of a ListRecords or ListIdentifiers request, indicating a complete harvest. Set to 'NoLog' to have no log  written for client interactions with the data provider (this is the default if not set).</p>
<p class="style2">zippedHarvestsDirectory</p>
<p>This parameter defines where zipped harvest files are saved to. The path specified should be relative to the context root.<br>
An absolute path may also be specified, however this will mean the zip files will not be available for download via the web-based UI.</p>
<blockquote>
  <p class="code">&lt;Parameter name=&quot;zippedHarvestsDirectory&quot; value=&quot;admin/zipped_harvests&quot; override=&quot;false&quot;/&gt;</p>
</blockquote>
<p class="style2">serverUrl</p>
<p>This parameter defines the scheme, hostname and port for the server, which is displayed in the base URL for the 
data provider and elsewhere, for example http://www.example.org or http://www.example.org:8080.
If the value '[determine-from-client]' is supplied (default), then the scheme, hostname and port number will be
determined by examining the URL that was requested by the client. Note that when using mod_proxy with Apache, Tomcat must be configured properly in order for this mechanism to work. Refer to the <a href="http://tomcat.apache.org/tomcat-5.5-doc/proxy-howto.html">Tomcat proxy how to documentation</a> for information about configuring Tomcat for use with mod_proxy.</p>
<blockquote>
  <p class="code">&lt;Parameter name=&quot;serverUrl&quot; value=&quot;http://www.example.org&quot; override=&quot;false&quot;/&gt;</p>
</blockquote>
<p>&nbsp;</p>
<p><strong>Additional configuration opations </strong></p>
<p>See the jOAI web.xml file for notes and comments that describe additional configuration options. </p>
<p>&nbsp;</p>
<hr>
<h3>Source Code</h3>
<p>See the <a href="<c:url value='/docs/BUILD_INSTRUCTIONS.txt'/>">build instructions</a> for detailed infromation about obtaining the source code and building jOAI using Ant.</p>
<h3>&nbsp;</h3>
<p>&nbsp;</p>
<c:import url="../bottom.jsp"/>

</body>
</html>


