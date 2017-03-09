<%@ taglib uri="/WEB-INF/tlds/struts-html.tld" prefix="html" %>
<!-- JSTL tags -->
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page isELIgnored ="false" %>

<html:html>

<head>
<title>jOAI Help</title>
<link rel="stylesheet" type="text/css" href='${pageContext.request.contextPath}/oai_styles.css'>
</head>

<body>
<table cellpadding="0" cellspacing="0" width="100%" border="0"><tr><td width="463"><img src='<c:out value="${pageContext.request.contextPath}"/>/images/bannerPopUp.jpg' alt=" " width="463" height="43" border="0" usemap="#topMap" /></td>
   <td class="backgroundLinePopUp" align="right"><div class="hiddentext">.</div></td></tr></table>
<table><tr><td>

	<c:if test="${not empty param.default_location}">
	<b>Save files from this harvest</b> - Choose either the default location or indicate the location to save the harvested files. 
<p>
The default location is: <br><i> <c:out value="${param.default_location}" /></i><p>
	</c:if>

	
	<c:if test="${not empty param.help}">
	
	<c:set var="selectiveHarvestText">
		When an automatic harvest is conducted, the harvester checks if the data provider 
		supports deleted records. If deletions are supported, a <a href="http://www.openarchives.org/OAI/2.0/openarchivesprotocol.htm#Datestamp" target="_blank">selective harvest</a>
		is performed by requesting 
		and synchronizing only those records that have been added, modified or deleted since the previous harvest. 
		If deletions are not supported, a full harvest is performed by deleting all previously harvested records 
		and harvesting all records from scratch.
	</c:set>
	
	<!-- Display Harvester settings -->
	<c:if test="${param.help == 'disp_format'}">
			<b>Metadata Format</b> - The name of the metadata format of the files being harvested. 
			In the OAI protocol, this is know as the <a href="http://www.openarchives.org/OAI/2.0/openarchivesprotocol.htm#MetadataNamespaces" target="_blank">metadataPrefix</a>.
			<p>
			Examples: <i>adn</i> or <i>oai_dc</i>
		</c:if>
	<c:if test="${param.help == 'disp_setspec'}">
			<b>SetSpec</b> - The name that identifies a subgroup of metadata files within an external OAI provider. 
			<p>Example: <i>curl</i>
		</c:if>
		<c:if test="${param.help == 'disp_interval'}">
			<b>Harvest Interval</b> - The harvest method, either manual or automatic, and the time interval. 
			<p>Example: <i>Automatic (Every day at 1:15 PM MDT).</i></p>
			
			<p>${selectiveHarvestText}</p>			
		</c:if>
		<c:if test="${param.help == 'disp_manual'}">
				<b>Manually harvest</b> - An immediate harvest is initiated by selecting <b><i>New</i></b> or <b><i>All</i></b>.  
				<p>A <b><i>New</i></b>  harvest requests only those files that have been added, updated or deleted since the last harvest,
				synchronizing any changes with the previous files that have been saved from the data provider. 
				This is know as an incremental or <a href="http://www.openarchives.org/OAI/2.0/openarchivesprotocol.htm#Datestamp" target="_blank">selective harvest</a>.
				<p>An <b><i>All</i></b> harvest first deletes all previosly harvested files and then
				conducts a fresh harvest of all files from the data provider.
			</c:if>
	<c:if test="${param.help == 'disp_settings'}">
			<b>Harvest Settings</b> - Allows edit access to (or total deletion of) harvest repository information. 
			<p><b><i>Edit</i></b> provides access to change the Repository name, Repository base URL, SetSpec, Metadata format, 
			Harvest interval and Save files location.<p> <b><i>Delete</i></b> removes the harvest.
		</c:if>
	
	<!-- Edit Harvester Settings -->
		<c:if test="${param.help == 'name'}">
			<b>Repository name</b> - A name to describe the data provider to be harvested. 
			<p>Example: <i>DLESE Repository</i>
		</c:if>
		<c:if test="${param.help == 'url'}">
			<b>Repository base URL</b> - A web address beginning with http:// that is the access point to the data provider to be harvested. 
			<p>Example: <i>http://some.group.org/oai/provider</i>

		</c:if>
		<c:if test="${param.help == 'set'}">
		<b>SetSpec</b> - The name that identifies a subgroup of metadata files within the external OAI provider. Naming a set is optional. This name must match the SetSpec used by the provider being harvested. 
		<p>Use the OAI ListSets request to find available SetSpecs at the provider. <p>The OAI ListSets request looks like: <br>
<i>http://some/provider.org/base/url?verb=ListSets</i><p>To form this request, concatenate together the [base URL] + [?verb=ListSets]

		<p>Example: <i>comet</i>


		
		
		
		</c:if>
		<c:if test="${param.help == 'metadata'}">
		<b>Metadata format being harvested</b> - The name of the metadata format of the files to harvest. This metadata format must match the 
		name used by the provider being harvested. 
		<p>Use the OAI ListMetadataFormats request to find available metadata formats at the provider.<p>The OAI ListMetadataFormats request looks like:
		<br><i>http://some.provider.org/base/url?verb=ListMetadataFormats</i><p>
		To form this request, concatenate together the [base URL] + [?verb=ListMetadataFormats] 
		<p>Example: <i>adn</i>
		


		</c:if>
		<c:if test="${param.help == 'regular'}">
			<b>Harvest automatically at regular intervals</b> - 
			An automatic harvest is performed at scheduled times when this option is selected. 
			
			<p>Set the time interval (days/hours/minutes/seconds) to perform the harvest.
			When selecting days for the interval, enter the time of day to begin the
			harvest using 24 hour time, for example 1:15 (1:15 AM) or 23:15 (11:15 PM).
			</p>
			
			<p>Example: <i>every 1 day(s) beginning at time 1:15</i></p>
			<p>${selectiveHarvestText}</p>			
		</c:if>
		<c:if test="${param.help == 'split'}">
		<b>When saving files</b> - Choose whether to keep saved files as a single group or split into sets. This option is active only if 
		a setSpec is not specified.<p>
		Files being split by set are saved in a directory named after the setSpec.
		</c:if>

		<!-- Edit Repository Page -->
		<c:if test="${param.help == 'repinfo_repname'}">
			<b>Repository name</b> - The formal name for the collection of all metadata files being provided. 
			<p>Example: <i>ABC Repository</i>
		</c:if>
		<c:if test="${param.help == 'repinfo_adminemail'}">
			<b>Administrator's email address</b> - An email address to contact about questions, comments, or issues regarding the repository.
			<p>Example: <i>person@some.org</i>
		</c:if>
		<c:if test="${param.help == 'repinfo_namespace'}">
			<b>Namespace identifier</b> -  A label in the form of a web address or domain name that uniquely identifies the repository.
			If specified, the namespace identifier is used to compose the OAI Identifier for items in the repository. See the
			<a href="http://www.openarchives.org/OAI/2.0/guidelines-oai-identifier.htm" target="_blank">OAI Identifier Format</a> guidelines
			for more information.
			<p>Example: <i>www.dlese.org</i>
			
		</c:if>
		<c:if test="${param.help == 'repinfo_baseurl'}">
			<b>Base URL</b> - Specifies the Internet host and port, and optionally a path, of the HTTP server for this repository
			data provider.
			Harvesters that wish to harvest metadata from the repository must use this Base URL. 
			
			<p>Note that the base URL that is shown reflects the URL that was entered into 
			the web browser when connecting to the software. For example, if a user accesses jOAI using 
			the web address http://localhost:8080${pageContext.request.contextPath}, the baseURL will be shown as 
			'http://localhost:8080${pageContext.request.contextPath}/${initParam.dataProviderBaseUrlPathEnding}'. If the user connects 
			to the same instance of jOAI using the Internet address http://myserver.somewhere.edu${pageContext.request.contextPath}, the 
			baseURL will be shown as 
			'http://myserver.somewhere.edu${pageContext.request.contextPath}/${initParam.dataProviderBaseUrlPathEnding}'. </p>
			
			<p>Example: <i>http://www.dlese.org/oai/provider</i>
			
		</c:if>
		
		<c:if test="${param.help == 'dontzipfiles'}">
			<b>Post-processing of files</b> - 
			Choose whether to have the resulting harvested files zipped into a single archive and made available for download
			after the harvest has completed. The zip archive is made available for download from the 
			Harvester Setup and Status page.			
		</c:if>		
		
		<c:if test="${param.help == 'repinfo_repdesc'}">
			<b>Repository description</b> - A narrative that describes the purpose, content, rights, history, etc. of the repository.
			<p>Example: <i>To provide Earth System content.</i>
		</c:if>
		
		<!-- Add/Edit Metadata Directory Page -->
		<c:if test="${param.help == 'metdir_nickname'}">
			<b>Nickname</b> - A label for a group of files from a particular directory. Typical nicknames are based on the content of the metadata files, the directory name or the organization name.
			<p>
			Example 1 - content: <i>Earthquake Files</i> <br>
			Example 2 - organization: <i>U.S. Geological Survey (USGS) Files</i> <br>
			Example 3 - directory: <i>mydirname files</i> 
			
		</c:if>
		<c:if test="${param.help == 'metdir_path'}">
			<b>Path</b> - The file location of the metadata files. 
			<p>Example: <i>/my/metadata</i>
		</c:if>
		<c:if test="${param.help == 'metdir_namespace'}">
			<b>Metadata namespace</b> - An XML label, written as a web address, which is used to distinguish and identify where markup has come from. 
			In general this can be found near the top of an XML instance document for the given format.
			If the format is recognized by the software this field will be filled in automatically.
			<p>Example: <i>For the metadata format of oai_dc, the namespace is http://openarchives.org/OAI/2.0/oai_dc/</i>
		</c:if>
		<c:if test="${param.help == 'metdir_schema'}">
			<b>Metadata schema</b> - The location of the XML schema file that defines the structure of the metadata files. XML schema files usually have a file extension
			ending in .xsd. In general this can be found near the top of an XML instance document for the given format.
			If the format is recognized by the software this field will be filled in automatically.
			<p>Example: <i>http://www.dlese.org/metadata/someschema.xsd</i>
		</c:if>
		
		<!-- display metadata directories page -->
		<c:if test="${param.help == 'dm_action'}">
			<b>Action</b> - Functions that index files again, remove the metadata directory or allow information about the metadata directory
			(e.g. name, format, etc.) to be changed. 
		</c:if>
		<c:if test="${param.help == 'dm_provider'}">
			<b>Data Provider Access</b> - A toggle that enables or disables the metadata directory so that metadata files may or may not be 
			harvested or publicly searched. Metadata files are still accessible in the Admin Search. 
		</c:if>
		<c:if test="${param.help == 'dm_directory'}">
			<b>Metadata Directory</b> - The name and path to the directory of metadata files.
		</c:if>		
		<c:if test="${param.help == 'dm_num'}">
			<b>Num Files</b> - The number of files in the metadata directory.
		</c:if>
		<c:if test="${param.help == 'dm_errors'}">
			<b>Indexing Errors</b> - The number of files in the metadata directory that could not be indexed. Click the number to view the indexing
			errors. 
		</c:if>
		<c:if test="${param.help == 'dm_indexed'}">
			<b>Num Ready</b> - The number of files in the metadata directory that have been indexed and are ready for 
			harvesting. Click the number to view the records for the files.
		</c:if>
		<c:if test="${param.help == 'dm_deleted'}">
			<b>Num Deleted</b> - The number of files that have been removed from the 
			configured metadata directory and set to status deleted in the index.
			These files will be advertised to harvesters as deleted at their next update.			
			Click the number to view the records for the deleted files.
			
			<p>At a later time, if a file is added back with the same unique ID as a deleted record 
			(regardless of the directory), the data provider will replace it with the new one, 
			and it's status will no longer be deleted. Harvesters will then receive the new record the 
			next time they harvest from the data provider.</p>  
		</c:if>	
		<c:if test="${param.help == 'dm_numDeletedDocsNotFromAnyDirectory'}">
			<b>Deleted records not in any directory </b> - The number of records in the data provider that are 
			not in any configured directory. When a directory is removed from the data provider configuration using the "Remove"
			command, the records for those files are set to status deleted. 
			Harvesters will be notified that these files have been deleted.
			Click the number to view the records for the deleted files.
		</c:if>			
		<c:if test="${param.help == 'dm_format'}">
			<b>Format</b> - The metadata format for the files. 
			In the OAI protocol, this is know as the <a href="http://www.openarchives.org/OAI/2.0/openarchivesprotocol.htm#MetadataNamespaces" target="_blank">metadataPrefix</a>. 
			The metadataPrefix may be any combination of <a href="http://www.ietf.org/rfc/rfc2396.txt" target="_blank">URI <i>unreserved</i> characters</a>, such as
			letters, numbers, underscores and dashes.
			<p>
			Examples:</br></br>
			<table border="1">
				<tr>
					<td><i>oai_dc</i></td><td>Dublin Core format</td>
				</tr>
				<tr>
					<td><i>adn</i></td><td>ADN format</td>
				</tr>
				<tr>					
					<td><i>dlese_anno</i></td><td>DLESE annotation format</td>
				</tr>
				<tr>					
					<td><i>dlese_collect</i></td><td>DLESE collection format</td>
				</tr>
				<tr>					
					<td><i>news_opps</i></td><td>DLESE news and opportunities format</td>
				</tr>
			</table>
		</c:if>
		
		
		<!-- Set Config page-->
		<c:if test="${param.help == 'set_name'}">
			<b>Set name</b> - A descriptive name for a group of metadata files that are a subgroup of all metadata files in the repository. 
			<p>
			Example: <i>DLESE Community Collection</i>
		</c:if>
		<c:if test="${param.help == 'set_setspec'}">
			<b>SetSpec</b> - A very short name or label that identifies the subgroup of metadata files; 
			harvesters may use a setSpec to identify and get the correct set of metadata files from providers.
			<br/><br/>
			The SetSpec may include letters, numbers, colons and a few other
			characters but must not include spaces. 
			<p>Example: <i>dcc</i>
		</c:if>
		<c:if test="${param.help == 'set_desc'}">
			<b>Set description</b> - Narrative that describes the purpose, content, rights or history of the subgroup of metadata files. 
			<p>Example: <i>This set contains files that describe earthquakes.</i>
		</c:if>
		<c:if test="${param.help == 'set_url'}">
			<b>Set URL</b> - Web address at which more information about the subgroup of metadata files can be obtained.
			<p>Example: <i>http://more.info.org</i>
		</c:if>
		<c:if test="${param.help == 'set_records'}">
			<b>Records Ready</b> - The number of records in the set that have been indexed and are ready for harvesting. 
			Click the number to view the records.
		</c:if>
		<c:if test="${param.help == 'set_records_deleted'}">
			<b>Records Deleted</b> - The number of records in this set that have been deleted and 
			will be advertised to harvesters as deleted at their next update.
			Click the number to view the records.
		</c:if>		
		<c:if test="${param.help == 'set_step1'}">
			<p><b>Step 1</b> - Create a subgroup of metadata files from the repository; that is, at least do one of these options:</p>
			<ul>
			<li>
				Select all files and then either constrain (Step 2) or exclude (Step 3) files
			</li>
			
			<li>
				Pick files in a particular metadata format (if the repository only contains one metadata format then either constrain (Step 2) or exclude (Step 3) some files)
			</li>
			<li>
				Add directories of files (if all directories are added, then either constrain (Step 2) or exclude (Step 3) some files)
			</li>				
		</c:if>
		<c:if test="${param.help == 'set_step2'}">
			<p><b>Step 2</b> - Specify criteria (constraints) that the subgroup of metadata files must have. 
				If Step 1 includes all files, then either constrain (Step 2) or exclude (Step 3) some files.
			</p>		
		</c:if>
		<c:if test="${param.help == 'set_step3'}">
			<p><b>Step 3</b> - Specify criteria that removes metadata files from the subgroup. If Step 1 includes all files, then either constrain (Step 2) or exclude (Step 3) some files.
			</p>		
		</c:if>
		<c:if test="${param.help == 'set_terms_phrases'}">
			<p><b>Terms and Phrases</b> - 	Create a comma separated list of words or phrases like the following examples:
			</p>
			<ul>
				<p>
					Example 1 - terms: <i>ocean, salinity, spray, currents</i>
				</p>
	
				<p>
					Example 2 - phrases: <i>pacific ocean, atlantic ocean</i>
				</p>
	
				<p>
					Example 3 - terms and phrases: <i>temporal, changes, temporal changes, temporal changes in streamflow</i>
				</p>
			</ul>
		</c:if>
		<c:if test="${param.help == 'set_lucene_query'}">
			<p><b>Lucene Query</b> - Create a Lucene query like the following examples:
			</p>
			<ul>
				<p>
					Example 1 - search the resource creator field: 
					<ul><i>creator:Doe creator:Jane</i></ul>
				</p>
	
				<p>
					Example 2 - search keywords in a title: <ul><i>title:ocean AND title:rain</i></ul> <ul><p> - or equivilently - </p></ul> <ul><i>title:(ocean rain)</i></ul>
				</p>
	
				<p>
					Example 3 - search by resource creator and the 'keywords' field: <ul><i>creator:Doe AND keyword:ocean</i></ul>
				</p>
			</ul>
			<p>
				&#187; <a href="http://www.dlese.org/dds/services/ddsws1-1/service_specification.jsp#availableSearchFields" target="_blank">Read about the available search fields and search syntax</a>.
			</p>
		</c:if>			
	</c:if>
	
	<c:if test="${not empty param.more}">
		<b>Please note</b> that this harvest consists of a large number of files. Since you are downloading it in
	 zipped format, the zip file may not contain the correct number of files if you open it on a Windows-based system. If that happens, 
	you can try unzipping this zip file in a Unix/Linux-based system from the command line, or access the latest harvested files directly at the 
	location to which they were harvested. That location is  <br>
	<c:out value="${param.locn}" /> . <br> 

	
	</td></tr>
	<tr><td>
	<a href='<c:out value="${param.url}" />'>Download Harvest in Zipped format</a>
	</td></tr>
	
	
	</c:if>



	
	</td></tr><tr><td><br>
	<a href="javascript:onClick=window.close()">Close window</a>
	</td></tr></table>
</body>
</html:html>

