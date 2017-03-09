<%@ page contentType="text/html; charset=UTF-8" %>
<%@ include file="../TagLibIncludes.jsp" %>
<%@ include file="../baseUrl.jsp" %>
<c:set var="rm" value="${applicationScope.repositoryManager}"/>

<html>

<head>
<title>jOAI Data Provider Documentation</title>
<c:import url="../head.jsp"/>

</head>

<body>
<a name="top"></a><c:import url="../top.jsp?sec=doc"/>
<!-- content begins-->
<h1>Data Provider Documentation</h1>
<p>This documentation includes the following sections: </p>
<table border="0" align="left" cellpadding="0" cellspacing="0">
  <tr>
    <td align="left"><a href="provider.jsp#overview">Overview</a></td>
  </tr>
  <tr>
    <td align="left"><a href="provider.jsp#provsetup">Data provider setup </a></td>
  </tr>
  <tr>
    <td align="left"><a href="provider.jsp#directories">Preparing files for serving </a></td>
  </tr>
  <tr>
    <td align="left"><a href="provider.jsp#convertors">Providing files in multiple formats </a></td>
  </tr>
  <tr>
    <td align="left"><a href="provider.jsp#register">Register your data provider</a></td>
  </tr>    
</table>

<p>&nbsp;</p>
<p>&nbsp;</p>
<p>&nbsp;</p>
<p>&nbsp;</p>
<h3><a name="overview"></a>Overview</h3>
<p>The jOAI data provider allows XML files from a file system to be exposed as items in an OAI data repository and made available for harvesting by others using the OAI-PMH. After pointing the software to one or more file directories, the software  monitors the XML files inside, adding, updating or deleting them from the OAI repository as files are added, updated or deleted from the directories. Remote harvesters that monitor the OAI data repository can  effectively mirror the files  or harvest them as needed. jOAI can provide any XML format as long as the XML in the file is well formed.</p>
<p>The jOAI data provider implements protocol version 2.0. It uses resumption 
  tokens for <a href="http://www.openarchives.org/OAI/2.0/openarchivesprotocol.htm#FlowControl"
  >flow control</a> in the <a href="http://www.openarchives.org/OAI/2.0/openarchivesprotocol.htm#ListIdentifiers" >ListIdentifiers</a> 
  and <a href="http://www.openarchives.org/OAI/2.0/openarchivesprotocol.htm#ListRecords">ListRecords</a> 
  responses, supports selective harvesting by <a
  href="http://www.openarchives.org/OAI/2.0/openarchivesprotocol.htm#SelectiveHarvestingandDatestamps">date</a> 
  or <a href="http://www.openarchives.org/OAI/2.0/openarchivesprotocol.htm#SelectiveHarvestingandSets">set</a>, 
  provides gzip <a href="http://www.openarchives.org/OAI/2.0/openarchivesprotocol.htm#ResponseCompression">response 
  compression</a> and other protocol features.</p>
<p>See the <a href="<c:url value='/docs/faq.jsp#provider'/>">Data Provider FAQ</a> 
  for additional information.</p>
<hr>
<a name="provsetup"></a>
<h3>Data provider setup</h3>
<p>There are five steps necessary to make metadata files available through the jOAI data provider: </p>
<p>1. Install the jOAI software on a system in a servlet container such as <a href="http://tomcat.apache.org/">Apache Tomcat</a>. </p>
<p>See <a href="INSTALL.txt">INSTALL.txt</a> for installation instructions. If reading this page, most likely this step has been completed. </p>
<p>2. Complete the <a href="<c:url value='/admin/data-provider-info.do'/>">Repository Information</a> by clicking 'Edit repository info' in the Repository Information and Administration page and then:</p>
<ul>
  <li>Enter a repository name (<em>required</em>) </li>
    <li>Include an administrators e-mail address (<em>required</em>) </li>
<li>    Provide a namespace identifier (<em>optional but strongly recommended</em>) </li>
  <li>Provide a description (<em>optional</em>) </li>
</ul>
<p>The <strong>namespace-identifier</strong> is similar to an Internet domain name, for example &quot;dlese.org&quot; or &quot;project.dlese.org.&quot; If specified, the namespace identifier is used to compose the OAI Identifier for items in the repository. See the
			<a href="http://www.openarchives.org/OAI/2.0/guidelines-oai-identifier.htm">OAI Identifier Format</a> guidelines
			for more information. </p>
<p>Leave the <strong>description</strong> blank if not using. </p>
<p>3. Complete the <a href="<c:url value='/admin/data-provider.do'/>">Metadata Files Configuration</a> in the Metadata Files Configuration page by clicking &quot;Add metadata directory&quot; to add one or more metadata directories to the repository. For each directory: </p>
<ul>
  <li>Enter an appropriate nickname for the directory of files (<em>required</em>) </li>
  <li>Provide the metadata format (metadataPrefix) of the files (<em>required</em>) </li>
  <li>Enter the complete directory path to the metadata files (<em>required</em>)</li>
  <li>Enter the metadata namespace and schema for the format (<em>optional but recommended</em>) </li>
</ul>
<p>The <strong>directory of files</strong> must contain XML files that conform to the rules described below under <a href="#directories">Preparing files for serving</a>.</p>
<p>The <strong>metadata format</strong>  may be any metadata (or data) format. 
			In the OAI protocol, the format specifier is know as the <a href="http://www.openarchives.org/OAI/2.0/openarchivesprotocol.htm#MetadataNamespaces">metadataPrefix</a>. 
			The metadataPrefix may be any combination of <a href="http://www.ietf.org/rfc/rfc2396.txt">URI <i>unreserved</i> characters</a>, such as
			letters, numbers, underscores and dashes.
<blockquote>
  <p>
			  Examples:
  </p>

<table border="0">
				<tr>
					<td class="code">oai_dc</td>
					<td>- Dublin Core format</td>
				</tr>
				<tr>
					<td class="code">adn</td>
					<td>- ADN format</td>
				</tr>
				<tr>					
					<td class="code">dlese_anno</td>
					<td>- DLESE annotation format</td>
				</tr>
				<tr>					
					<td class="code">dlese_collect</td>
					<td>- DLESE collection format</td>
				</tr>
				<tr>					
					<td class="code">news_opps</td>
					<td>- DLESE news and opportunities format</td>
				</tr>
</table>
</blockquote>

<p>			In general, the <b>metadata namespace and schema</b>  can be found near the top of an XML file for the given format.
			If the format is recognized by the software these fields will be filled in automatically. </p>
<p>Tip: To test your jOAI installation you may configure your data provider to serve the enclosed sample reocrds:</p>
<ul>
  <li>For the path to the directory, enter: <code>{TOMCAT_HOME}/webapps/oai/WEB-INF/sample_metadata</code> <br>
    (replace <code>{TOMCAT_HOME}</code> with the absolute path to your tomcat installation).
  <li>For the metadataPrefix, enter <code>adn</code>
</ul>
<p>4. After completing step 3, the software automatically indexes the metadata files, which may take several minutes to complete. Once the files are indexed, the metadata is available for harvesting, for browsing using the OAI protocol via the <a href="<c:url value='/oaisearch.do'/>">Explorer</a> page and for textual searching  using the <a href="/oai/search.jsp">Search</a> or <a href="<c:url value='/admin/query.do'/>">Admin search</a> pages. The  <a href="<c:url value='/admin/data-provider.do'/>">Metadata Files Configuration</a> shows information about the status of the files and indexing process.  If  metadata files are added, modified or deleted at a later time, the software automatically detects these changes and adds, deletes or re-indexes them
every 
  <%@ include file="../updateFrequency.jsp" %>. 
 The index can also be updated manually at any time from the <a href="<c:url value='/admin/data-provider.do#index'/>">Files index administration</a> area.</p>
<p>5. Complete <a href="<c:url value='/admin/data-provider/sets.do'/>">Sets Configuration</a>. This step is <em>optional</em>. Define a new set and then: </p>
<ul>
  <li>Enter a set name (<em>required </em>) </li>
  <li>Enter a setSpec (<em>required </em>) </li>
  <li>Provide a set description (<em>optional </em>) </li>
  <li>Add records to the newly created set by defining which records to include in the set (<em>required </em>) </li>
</ul>
<p>The <strong>set name </strong> is a descriptive name for a group of metadata files that are a subgroup of all metadata files in the repository, for example &quot;DLESE Community Collection&quot;.</p>
<p>The <strong>setSpec </strong> is a unique name or label that identifies the subgroup of metadata files; harvesters may use a setSpec to identify and get the correct set of metadata files from providers. A setSpec example is &quot;dcc.&quot;</p>
<p>Limit the number files in a set by specifying certain directories, metadata formats or search criteria. </p>
<p>The optional <strong>description </strong> field contains information about the content, purpose, rights or history of the provider. Leave the description field blank if no information is available. </p>
<p>After completing the steps above, metadata files are available for harvesting by others. </p>
<p>This software supports the Open Archives Initiative Protocol for Metadata Harvesting (OAI-PMH), version 2.0. Detailed information about the protocol is outside the scope of this documentation. For background information on the OAI-PMH, please refer to the official <a href="http://www.openarchives.org/OAI/2.0/openarchivesprotocol.htm" >OAI-PMH documentation</a> and to additional information and tutorials available through the <a href="http://www.openarchives.org">Open Archives Initiative</a>. </p>
<p><a href="provider.jsp#top"> <img src="<c:url value='/images/arrowup.gif'/>" alt="top" width="9" height="11" border="0" title="back to top"></a></p>
<hr>
<h3><a name="directories"></a>Preparing files for serving</h3>
<p>jOAI monitors each directory of files that is configured in the system and automatically adds, updates or deletes items  from the OAI repository as files are added, updated or deleted from the directories. 
<c:choose>
  <c:when test="${rm.updateFrequency > 0}">
		After the initial configuration, the synchronization between the files and the OAI repository occurs automatically every 
		<%@ include file="../updateFrequency.jsp" %> 
	  or may be <a href="<c:url value='/admin/data-provider.do#index'/>">synchronized manually</a> at any time.
  </c:when>  
</c:choose>



</p>
<p>To ensure proper operation, files must follow these conventions: </p>
<ul>
  <li>Each file must be a well-formed XML instance document.</li>
  <li>Each file must contain a single record, which  corresponds to a single item in the OAI repository.</li>
  <li>All files within a given directory must be of the same metadata (XML) format.</li>
  <li>Each file name must end with a .xml file extension. </li>
  <li>The file name up to the .xml file extension must indicate a <a href="http://www.openarchives.org/OAI/2.0/openarchivesprotocol.htm#UniqueIdentifier" >unique identifier</a> for the record*.  For example, if the file is named <span class="code">abc-123.xml</span>, the identifier for the record will be <span class="code">abc-123</span> (the identifier is used as the local identifier portion of the <a href="http://www.openarchives.org/OAI/2.0/guidelines-oai-identifier.htm">OAI Identifier Format</a> in the OAI protocol). Identifiers in jOAI are <em>not</em> case sensitive. *Note that for files in the <span class="code">adn</span>, <span class="code">dlese_anno</span>, <span class="code">dlese_collect</span> and <span class="code">news_opps</span> formats, the file name is not used and instead the identifier must be indicated in the proper location in the file's XML.</li>
  <li>Identifiers must be unique across all files configured in the data provider. It is an error to have two or more files, regardless of format, with the same identifier.</li>
  <li>Reserved characters must be encoded with hex substitutes. For example to indicate a slash / in the identifier use %2F in the file name. Reserved characters and their hex substitutes are:
    <blockquote>
      <p><code>&quot;/&quot;, &quot;%2F&quot;<br>
&quot;?&quot;, &quot;%3F&quot;<br>
&quot;#&quot;, &quot;%23&quot;<br>
&quot;=&quot;, &quot;%3D&quot;<br>
&quot;&amp;&quot;, &quot;%26&quot;<br>
&quot;:&quot;, &quot;%3A&quot;<br>
&quot;;&quot;, &quot;%3B&quot;<br>
&quot; &quot;, &quot;%20&quot;<br>
&quot;+&quot;, &quot;%2B&quot;</code></p>
    </blockquote>
  </li>
  <li>A change in the file modification date will update the OAI datestamp and initiate a transfer of the record from the data provider to the harvester. For network efficiency, the file modification date should  change only when the content of the file is modified.</li>
  <li>The XML files must be encoded using  the UTF-8 representation of Unicode. Character references, rather than entity references, must used. See the <a href="http://www.openarchives.org/OAI/openarchivesprotocol.html#XMLResponse">XML response format</a> specification for the OAI protocol.</li>
</ul>
<blockquote>
  <blockquote>&nbsp;</blockquote>
</blockquote>
<p><a href="provider.jsp#top"><img src="<c:url value='/images/arrowup.gif'/>" alt="top" width="9" height="11" border="0" title="back to top"></a></p>
<hr>
<h3><a name="convertors"></a>Provide test records </h3>
<p>To test your jOAI installation, configure your data provider to serve the enclosed sample records.</p>
<p>&nbsp;</p>
<p>&nbsp;</p>
<hr>
<h3><a name="convertors"></a>Providing files in multiple formats </h3>
<p>jOAI can disseminate   any given metadata file in multiple formats. For example, a file that resides in the <span class="code">adn</span> format can also be disseminated to harvesters in the <span class="code">oai_dc</span> format. This is done using  metadata format converters. Several metadata format converters come pre-configured in the software as detailed below. New converters can be configured and implemented using an XSL stylesheet or a  custom Java class that converts metadata from its native XML format to  another XML format. Once a format converter is configured, all files in the native format will be disseminated  in either the native or converted format depending on which format is requested by the harvester. </p>
<p>The software comes pre-configured with the following metadata format converters (plus others):</p>
<table border="1" cellspacing="0" cellpadding="0">
  <tr>
    <td><strong>Native XML format &nbsp; &nbsp;</strong></td>
    <td><strong>Converted XML format&nbsp; &nbsp;</strong></td>
  </tr>
  <tr>
    <td class="code">nsdl_dc&nbsp; &nbsp;</td>
    <td class="code">oai_dc&nbsp; &nbsp;</td>
  </tr>    
  <tr>
    <td class="code">adn&nbsp; &nbsp;</td>
    <td><span class="code">oai_dc</span>, <span class="code">nsdl_dc</span>, <span class="code">briefmeta</span> &nbsp; &nbsp;</td>
  </tr>
  <tr>
    <td class="code">dlese_anno&nbsp; &nbsp;</td>
    <td class="code">oai_dc&nbsp; &nbsp;</td>
  </tr>
  <tr>
    <td class="code">dlese_collect&nbsp; &nbsp;</td>
    <td class="code">oai_dc&nbsp; &nbsp;</td>
  </tr>
  <tr>
    <td class="code">news_opps&nbsp; &nbsp;</td>
    <td class="code">oai_dc&nbsp; &nbsp;</td>
  </tr>
</table>
<p>To configure additional metadata format converters, do the following: </p>
<p>1. Create or obtain an XSL stylesheet or Java class that performs the desired format conversion from one XML format to another. The converter takes XML in the native format as its input and must generate XML in the converted format as its output.  For Java converters, the class must implement the <a href="<c:url value='/docs/javadoc/org/dlese/dpc/xml/XMLFormatConverter.html'/>" class="code">XMLFormatConverter</a> Interface. </p>
<p>2. If using an XSL stylesheet to perform the conversion, place it in the &quot;xsl_files&quot; directory located in the &quot;WEB-INF&quot; directory of the OAI software context. If using a Java class to perform the conversion, place  the class binary anywhere within the classpath of the servlet container. </p>
<p>3. Edit the&quot;web.xml&quot; file located in the &quot;WEB-INF&quot; directory and add a context-param element to configure each format converter (see the existing ones for examples).When configuring an XSL converter,  the param-name element must start with the string with &quot;xslconverter,&quot; followed by additional descriptive text. When configuring a Java class converter, the param-name element must start with the string with &quot;javaconverter,&quot; followed by additional descriptive text.  Each param-name must be unique; otherwise it will not be recognized. 
<p>For the param-value field, supply a string of the form 
<p class="code">[convertername] | [from format] | [to format] 
<p>where convertername is either the name of an XSL file or a fully qualified Java class and the &quot;to&quot; and &quot;from&quot; formats are metadataPrefixes for the given formats. </p>
<p>For example, an XSL stylesheet named myDCConverter.xsl that converts from ADN to Dublin Core, the param-value would be </p>
<p>&quot;<span class="code">myDCConverter.xsl|adn|oai_dc</span>&quot; (quotes omitted). </p>
<p>For a Java class converter by the full name org.institution.converter.MyDCConverter, the param-value would be </p>
<p>&quot;<span class="code">org.institution.converter.MyDCConverter|adn|oai_dc</span>&quot; (quotes omitted).</p>
<p>An example of a complete context-param configuration for a format converter looks like the following:</p>

<pre class="code">
&lt;context-param&gt; 
  &lt;param-name&gt;xslconverter - adn to oai_dc converter&lt;/param-name&gt; 
  &lt;param-value&gt;adn-v0.6.50-to-oai_dc.xsl|adn|oai_dc&lt;/param-value&gt; 
&lt;/context-param&gt; </pre>
<p>4. After configuring the web.xml file and placing the converter in the appropriate location, start or restart the software. The software automatically recognizes the converter and adds the new format to its list of available formats and exposed in response to the OAI <a href="http://www.openarchives.org/OAI/2.0/openarchivesprotocol.htm#ListMetadataFormats" class="code">ListMetadataFormats</a> request. </p>
<p>Tip: The format converter module caches the converted metadata to disk for 
  increased performance. These converted files may be accessed locally. Accessed 
  files are cached in the &quot;WEB-INF/repository_data/converted_xml_cache&quot; 
  directory. </p>
<p><a href="provider.jsp#top"> <img src="<c:url value='/images/arrowup.gif'/>" alt="top" width="9" height="11" border="0" title="back to top"></a></p>
<hr>
<h3><a name="register"></a>Register your data provider</h3>
<p>After you have set up your data provider you may wish to register it with the Open Archives Initiative. 
Doing so will add your data provider to the <a href="http://www.openarchives.org/Register/BrowseSites">list of OAI conforming repositories</a>. 
To register, see the <a href="http://www.openarchives.org/Register/BrowseSites">Data Provider Validation and Registration page</a>.
</p>
<p><a href="provider.jsp#top"> <img src="<c:url value='/images/arrowup.gif'/>" alt="top" width="9" height="11" border="0" title="back to top"></a></p>
<c:import url="../bottom.jsp"/> 
</body>
</html>

