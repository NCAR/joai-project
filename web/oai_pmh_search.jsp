<%@ include file="TagLibIncludes.jsp" %>

<c:set var="rm" value="${applicationScope.repositoryManager}"/>
<%@ include file="baseUrl.jsp" %>

<html:html>

<c:set var="title" value="Explore the Data Provider"/>

<head>
<title>${title}</title>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
	
<%-- Include style/menu templates --%>
<%@ include file="../../head.jsp" %>
	
<script language="JavaScript">
	var BASE_URL = "${myBaseUrl}";
	var advanced = "off";
	<logic:match name="opsf" property="showAdvanced" value="true">
		advanced = "on";
	</logic:match>		
</script>

</head>

<body onLoad="document.getRecordForm.identifier.focus()">
  <%-- Include style/menu templates --%>
  <c:import url="top.jsp?sec=provider" />

  

<table cellpadding="6" cellspacing="0"> 
  <tr> 
    <td> 
		  
	  <h1>${title}</h1>
	  Submit OAI-PMH requests to the data provider using the forms below 
	  and view or validate the XML responses. 
	  This page assumes familiarity with <a href="http://www.openarchives.org/OAI/2.0/openarchivesprotocol.htm">OAI-PMH requests</a>.
	  
	  <br/>
	  <br/>
	  The baseURL for the data provider is: ${myBaseUrl}
	  <br/>
	  <br/>

	  <html:errors/>				
			

	<table>		
		<%-- ListIdentifiers, ListRecords --%>		
		<tr>
			<td colspan=3>
				<a href="http://www.openarchives.org/OAI/2.0/openarchivesprotocol.htm#ListIdentifiers" class="boldlink" title="See description of ListIdentifiers">ListIdentifiers</a>		
				and
				<a href="http://www.openarchives.org/OAI/2.0/openarchivesprotocol.htm#ListRecords" class="boldlink" title="See description of ListRecords">ListRecords</a>
			</td>
		</tr>
		<tr>
			<td nowrap>
				Choose verb:
			</td>
			<form name="listRecordsIdentifers" action='javascript:mkListRecordsIdentifers("")'>
			<td nowrap colspan=2>
				<select name="verb">
					<option value="ListIdentifiers">ListIdentifiers</option>
					<option value="ListRecords">ListRecords</option>					
				</select>
				&nbsp;Return set:
				<select name="sets">
					<option value=' -- All -- '> -- All -- </option>
					<c:forEach items="${rm.oaiSets}" var="setSpec">
						<option value='${setSpec}'>${setSpec}</option>	
					</c:forEach>
				</select>				
				&nbsp;Format:
				<select name="formats">			
					<logic:iterate name="opsf" property="availableFormats" id="format" >
						<option value='<bean:write name="format"/>'><bean:write name="format"/></option>	
					</logic:iterate>
				</select>
				<input title="View the ListRecords or ListIdentifiers response" type="button" value="view" onClick='mkListRecordsIdentifers("")'>
				<input title="Validate the ListRecords or ListIdentifiers response" type="button" value="validate" onClick='mkListRecordsIdentifers("rt=validate")'>
			</td>		
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
			</form>			

		<tr>
			<td colspan=3 height=20>&nbsp;
				
			</td>
		</tr>
		</table>
		
		<%-- GetRecord --%>
		<table>
		<form name="getRecordForm" action='javascript:mkGetRecord("")'>
		<tr>
			<td colspan=3 nowrap>
				<a href="http://www.openarchives.org/OAI/2.0/openarchivesprotocol.htm#GetRecord" title="See description of GetRecord" class="boldlink"><b>GetRecord</b></a>
			</td>
		</tr>
		<tr>
			<td nowrap>
				Enter ID:
			</td>
			<td nowrap colspan=2>
				<input type="text" name="identifier" size="30">
				<input title="View the GetRecord response" type="button" value="view" onClick='mkGetRecord("")'>
				<input title="Validate the GetRecord response" type="button" value="validate" onClick='mkGetRecord("rt=validate")'>
			</td>
		</tr>
		<tr>
			<td>&nbsp;</td>
			<td nowrap colspan=2>			
				Return in format:
				<select name="formats">			
					<logic:iterate name="opsf" property="availableFormats" id="format" >
						<option value='<bean:write name="format"/>'><bean:write name="format"/></option>	
					</logic:iterate>
				</select>
			</td>
		</tr>
		</form>	
		</table>
		
				


		
		<%-- ResumptionToken --%>		
		<table>
		<tr>
			<td colspan=3 height=20>&nbsp;
				
			</td>
		</tr>		
		<tr>
			<td colspan=3>
				<a href="http://www.openarchives.org/OAI/2.0/openarchivesprotocol.htm#FlowControl" class="boldlink" title="See description of flow control and resumptionToken">ResumptionToken</a>
			</td>
		</tr>
		<tr>
			<td nowrap>
				Enter a token:
			</td>
			<form name="resumptionForm" action="javascript:mkResume("")">
			<td colspan=2>
				<input type="text" name="resumptionToken" size="30">
				<select name="verb">
					<option value="ListIdentifiers">ListIdentifiers</option>
					<option value="ListRecords">ListRecords</option>
				</select>
				<input title="Resume and view the ListRecords or ListIdentifiers response" type="button" value="view" onClick="mkResume('')">
				<input title="Resume and validate the ListRecords or ListIdentifiers response" type="button" value="validate" onClick="mkResume('rt=validate')">
			</td>
			</form>	
		</tr>

		<tr>
			<td colspan=3 height=20>&nbsp;
				
			</td>
		</tr>

		<%-- Identify --%>		
		<tr>
			<td colspan=3>
				<a href="http://www.openarchives.org/OAI/2.0/openarchivesprotocol.htm#Identify" class="boldlink" title="See description of Identify">Identify</a>
			</td>
		</tr>
		<tr>
			<form name="Identify" action="">
			<td colspan=3>
				<input title="View the Identify response" type="button" value="view" onClick='window.location = BASE_URL + "?verb=Identify"'>
				<input title="Validate the Identify response" type="button" value="validate" onClick='window.location = BASE_URL + "?verb=Identify&rt=validate"'>
			</td>
			</form>	
		</tr>			
		<tr>
			<td colspan=3 height=20>&nbsp;
				
			</td>
		</tr>		
		
		<%-- ListSets --%>		
		<tr>
			<td colspan=3>
				<a href="http://www.openarchives.org/OAI/2.0/openarchivesprotocol.htm#ListSets" class="boldlink" title="See description of ListSets">ListSets</a>
			</td>
		</tr>
		<tr>
			<form name="ListSets" action="">
			<td colspan=3>
				<input title="View the ListSets response" type="button" value="view" onClick='window.location = BASE_URL + "?verb=ListSets"'>
				<input title="Validate the ListSets response" type="button" value="validate" onClick='window.location = BASE_URL + "?verb=ListSets&rt=validate"'>
			</td>
			</form>	
		</tr>		

		<tr>
			<td colspan=3 height=20>&nbsp;
				
			</td>
		</tr>
		
		<%-- ListMetadataFormats --%>		
		<tr>
			<td colspan=3>
				<a href="http://www.openarchives.org/OAI/2.0/openarchivesprotocol.htm#ListMetadataFormats" class="boldlink" title="See description of ListMetadataFormats">ListMetadataFormats</a>
			</td>
		</tr>
		<tr>
			<form name="ListMetadataFormats" action="">
			<td colspan=3>
				<input title="View the ListMetadataFormats response" type="button" value="view" onClick='window.location = BASE_URL + "?verb=ListMetadataFormats"'>
				<input title="Validate the ListMetadataFormats response" type="button" value="validate" onClick='window.location = BASE_URL + "?verb=ListMetadataFormats&rt=validate"'>
			</td>
			</form>	
		</tr>				
	</table>
	

		</td>
	</tr>
</table>


	  
<table height="10">
  <tr> 
    <td>&nbsp;</td>
  </tr>
</table>

  <%-- Include style/menu templates --%>
  <%@ include file="../../bottom.jsp" %>
</body>
</html:html>

