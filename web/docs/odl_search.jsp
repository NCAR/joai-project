<%@ include file="../TagLibIncludes.jsp" %>
<c:set var="rm" value="${applicationScope.repositoryManager}"/>
<%@ include file="../baseUrl.jsp" %>

<html>

<head>
<title>ODL Search Specification</title>
<c:import url="../head.jsp"/>

<script language="JavaScript">
	var BASE_URL = "${myBaseUrl}";
	var advanced = "off";
	<logic:match name="opsf" property="showAdvanced" value="true">
		advanced = "on";
	</logic:match>		
</script>

</head>

<body id="odlDocsPage">
<div align="justify"><a name="top"></a>
  <c:import url="../top.jsp?sec=doc"/>
  <!-- content begins-->
  
</div>
<h1 align="justify">ODL Search Specification</h1>
<p align="justify"><a href="index.jsp#joai_provider"></a>
The jOAI ODL Search Specification is a Web service available in jOAI that allows clients to perform textual and fielded searches over the metadata 
in the data provider.
After issuing a search request, clients receive the matching metadata records back within 
the standard <a href="http://www.openarchives.org/OAI/2.0/openarchivesprotocol.htm#ListRecords">ListRecords</a> 
or <a href="http://www.openarchives.org/OAI/2.0/openarchivesprotocol.htm#ListIdentifiers">ListIdentifiers</a> 
response containers.           
The metadata may then be used in a custom search user interface or processed by remote clients on the fly.</p>
<p align="justify">The <a href="<c:url value='/search.jsp'/>">Search</a> page in jOAI is implemented using ODL search and can be customized and easily installed to another location or remote Web server as described below in the section <a href="#searchClient">Search client template</a>.</p>
<p>
jOAI ODL search is an extension to the OAI protocol that supports searching similar to the 
<a href="http://oai.dlib.vt.edu/odl/">Open Digital Library (ODL)</a> search specification (odlsearch1). 
</p>

<hr align="JUSTIFY">
<h3 align="justify">Searching</h3>
<p align="justify">To perform a jOAI ODL search, clients <em>must</em> provide a search query in             the set argument of either a <a href="http://www.openarchives.org/OAI/2.0/openarchivesprotocol.htm#ListRecords">ListRecords</a> or <a href="http://www.openarchives.org/OAI/2.0/openarchivesprotocol.htm#ListIdentifiers">ListIdentifiers</a> request. The set argument <em>must</em> conform to the following syntax: dleseodlsearch/[query string]/[set]/[offset]/[length]             where "dleseodlsearch" is the exact string dleseodlsearch,             "query string" indicates a list of keywords or Lucene query upon which to             search, "set" indicates the set over which to search, "offset"             indicates the offset into the results list to begin the results, and             "length" indicates the total number of results to return.             To search over all sets, clients <em>must</em> supply the string "null"             in the set field. Clients <em>must</em> escape all spaces in query string            and set with a plus (+) symbol and all slashes (/) with %2F. The default boolean logic is AND. To request             a query using boolean OR, clients <em>must</em> supply the exact string             "OR" between each term in the query string.</p>
  <p align="justify"><em>Examples: </em></p>
  <p align="justify">".../provider?verb=ListRecords&amp;metadataPrefix=oai_dc&amp;set=dleseodlsearch/ocean/null/0/10"             - Performs a text search for the term "ocean" across all sets             in the repository, returning matching results numbers 0 through 10.</p>
  <p align="justify">".../provider?verb=ListRecords&amp;metadataPrefix=oai_dc&amp;set=dleseodlsearch/ocean/null/10/10"             - Performs a text search for the word "ocean" across all sets             in the repository, returning matching results numbers 10 through 20.</p>
  <p align="justify">".../provider?verb=ListRecords&amp;metadataPrefix=oai_dc&amp;set=dleseodlsearch/ocean+weather/dcc/0/10"             - Performs a text search for the terms "ocean" AND "weather"             across the set dcc, returning matching results numbers 0 through 10.</p>
  <p align="justify">".../provider?verb=ListRecords&amp;metadataPrefix=oai_dc&amp;set=dleseodlsearch/ocean+OR+weather/dcc/0/10"             - Performs a text search for the terms "ocean" OR "weather"             across the set dcc, returning matching results numbers 0 through 10.</p>
  <p align="justify">".../provider?verb=ListRecords&amp;metadataPrefix=oai_dc&amp;set=dleseodlsearch/%2Ftext%2F%2Fdc%2Ftitle:ocean/dcc/0/10" - Performs a text search for the terms "ocean" in the search field '/text//dc/title' across the set dcc, returning matching results numbers 0 through 10.</p>
  <hr align="JUSTIFY">
  <h3>Search fields and query syntax </h3>
  <p>The jOAI index provides separate search fields for all data that reside in the records in the repository. <em>Standard</em>, <em>XPath</em>, and <em>Custom</em> search fields and  query syntax are described in detail in the Digital Discovery System (DDS) <a href="http://www.dlese.org/dds/services/ddsws1-1/service_specification.jsp#availableSearchFields">Search API Documentation</a> (see the sections titled 'Search fields' and 'Example search queries'). jOAI and DDS share the same repository code base and thus the available search fields, query syntax, and related processing are the same.</p>
  <hr align="JUSTIFY">
<h3 align="justify">ODL search explorer</h3>  
  
  <p align="justify">Use the form below to construct 
  and submit sample ODL search queries to the data provider. 
  Enter a query string into the search interface. Then click 
  Search to view the XML response and see the ODL request that appears in the address (URL) bar.</p>

		<%-- ODL search UI --%>
		<table>
			<form name="odlSearchForm" action="javascript:mkOdlSearch()">
			<tr>
				<td nowrap>
					Enter keyword:
				</td>
				<td colspan=2 nowrap>
					<input type="text" name="query" size="25">
					<select name="verb">
						<option value="ListIdentifiers">ListIdentifiers</option>
						<option value="ListRecords" selected>ListRecords</option>
					</select>
					<input type="button" value="Search" onClick="mkOdlSearch()">
					<%-- <input type="button" value="validate" onClick="mkOdlSearch('rt=validate')"> --%>
				</td>
			</tr>
			
			<tr>
				<td>&nbsp;</td>
				<td colspan=2 nowrap>
					Search in set:
					<select name="sets">
						<option value=' -- All -- '> -- All -- </option>
						<c:forEach items="${rm.oaiSets}" var="setSpec">
							<option value='${setSpec}'>${setSpec}</option>	
						</c:forEach>
					</select>					
					<%-- <select name="sets">
						<option value=' -- All -- '> -- All -- </option>			
						<logic:iterate name="opsf" property="availableSets" id="set" >
							<option value='<bean:write name="set"/>'><bean:write name="set"/></option>	
						</logic:iterate>
					</select> --%>				
					Format:
					<select name="formats">			
						<logic:iterate name="opsf" property="availableFormats" id="format" >
							<option value='<bean:write name="format"/>'><bean:write name="format"/></option>	
						</logic:iterate>
					</select>
				</td>
			</tr>
			<logic:match name="opsf" property="showAdvanced" value="true">
			<tr>
				<td>&nbsp;</td>
				<td align=right>
					Records modified since:
				</td>
				<td>
					<select name="from">
						<option value='none'> -- No value -- </option>			
						<logic:iterate name="opsf" property="utcDates" id="date" >
							<option value='<bean:write name="date" property="date"/>'><bean:write name="date" property="label"/></option>	
						</logic:iterate>
					</select>
					&nbsp;[ <a href="oaisearch.do">update time</a> ]
				</td>
			</tr>
			<tr>
				<td>&nbsp;</td>
				<td align=right>
					Records modified before:
				</td>
				<td>
					<select name="until">
						<option value='none'> -- No value -- </option>			
						<logic:iterate name="opsf" property="utcDates" id="date" >
							<option value='<bean:write name="date" property="date"/>'><bean:write name="date" property="label"/></option>	
						</logic:iterate>
					</select>
					&nbsp;[ <a href="oaisearch.do">update time</a> ]
				</td>
			</tr>
			</logic:match>
			</form>
		</table>  
  
  
<hr align="JUSTIFY">
<h3 align="justify">Flow control</h3>
<p align="justify">All state is embedded in the search query string, giving clients control over response flow.   	  Clients can "page through" a set of results by issuing the same request in succession  	  and incrementing the offset parameter by the desired quanta. For example, a client wishing to   	  iterate through three pages of results for a search on the term ocean, retrieving ten  	  records per page, would issue the following  	  three queries:</p>
<p align="justify">".../provider?verb=ListRecords&amp;metadataPrefix=oai_dc&amp;set=dleseodlsearch/ocean/null/0/10"</p>
<p align="justify">".../provider?verb=ListRecords&amp;metadataPrefix=oai_dc&amp;set=dleseodlsearch/ocean/null/10/10"</p>
<p align="justify">".../provider?verb=ListRecords&amp;metadataPrefix=oai_dc&amp;set=dleseodlsearch/ocean/null/20/10"</p>
<p align="justify">At the end of each response an empty resumptionToken element is provided for the client that contains the  	  attributes completeListSize and cursor. The completeListSize attribute shows the total number of records   	  in the repository that match the given query. The cursor reflects the offset into the result set where the current   	  response container begins.</p>
<hr align="JUSTIFY">
<h3 align="justify"><a name="searchClient"></a>Search client template </h3>
<p>jOAI includes an ODL search client template that comes installed with the software as part of the <a href='<c:url value="/search.jsp"/>'>Search</a> page and can be modified within the software or copied and moved to any JSP container. The search template can provide a search for the  local data provider installation or any other DLESE  jOAI v2.0.4 or later data provider.</p>
<p>To use the template, modify or copy the file located at <span class="code">$CATALINA_HOME/webapps/oai/odl_search_client.jsp</span> (<span class="code">$CATALINA_HOME</span> refers to the location of the Tomcat installation for jOAI) and follow the instructions provided in the file. </p>
<p>As you code your JSP page(s) you may find this tutorial useful regarding making JSP pages that use UTF-8 Unicode: <a href="http://java.sun.com/developer/technicalArticles/Intl/MultilingualJSP/">Developing Multilingual Web Applications Using JavaServer Pages Technology</a>.</p>
<p>Note that because ODL search is an extension to the standard OAI protocol, the search template works only when pointed to a DLESE jOAI v2.0.4 or later data provider.</p>
<p>&nbsp;</p>
<hr align="JUSTIFY">
<div align="justify">
  <c:import url="../bottom.jsp"/>
  
</div>
</body>
</html>

