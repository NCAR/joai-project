<%@ include file="../../TagLibIncludes.jsp" %>
<jsp:useBean id="reportForm" class="org.dlese.dpc.action.form.SimpleQueryForm" scope="request"/>

<html:html>
<head>
<title><bean:write name="reportForm" property="reportTitle"/></title>

<%-- Include style/menu templates --%>
<%@ include file="../../head.jsp" %>

<script>
	// Override the method below to set the focus to the appropriate field.
	function sf(){}	
</script>
</head>
<body onLoad=sf()>

<%-- Include style/menu templates --%>
<c:import url="../top.jsp?sec=provider" />

	<h1><bean:write name="reportForm" property="reportTitle"/></h1>

	<%-- ######## Display messages, if present ######## --%>
	<logic:messagesPresent> 		
	<table width="90%" bgcolor="#000000" cellspacing="1" cellpadding="8">
	  <tr bgcolor="ffffff"> 
		<td>
			<b>Messages:</b>
			<ul>
				<html:messages id="msg" property="message"> 
					<li><bean:write name="msg"/></li>									
				</html:messages>
				<html:messages id="msg" property="error"> 
					<li><font color=red>Error: <bean:write name="msg"/></font></li>									
				</html:messages>
			</ul>
			<blockquote>[ <a href="report.do?report=noreport">OK</a> ]</blockquote>
		</td>
	  </tr>
	</table>		
	<br/><br/>
	</logic:messagesPresent>
	
<table width="95%" cellpadding="10">
<%-- 
  <tr>
	<td colspan=2>
	
	Reports:<br/>
	&nbsp;&nbsp;<img src='../images/arrow_right_gray.gif' width='9' height='9' border='0'/>
	<a href="report.do?q=error:true&s=0&report=Files+that+could+not+be+indexed+due+to+errors">Files that could not be indexed due to errors</a><br/>
	
	<br/>Display requests that were issued to this data provider by harvesters, <br/>excluding those 
	submitted through the <a href="../oaisearch.do">Provider explorer</a> page:<br/>
	&nbsp;&nbsp;<img src='../images/arrow_right_gray.gif' width='9' height='9' border='0'/>
	<a href="report.do?q=!%22rt%20text%22+AND+!%22rt%20validate%22+AND+requesturlt%3Averb&s=0&searchOver=webLogs&report=All+OAI-PMH+requests+made+to+this+provider">All OAI-PMH requests made to this provider</a><br/>
	&nbsp;&nbsp;<img src='../images/arrow_right_gray.gif' width='9' height='9' border='0'/>
	<a href="report.do?q=!%22rt%20text%22+AND+!%22rt%20validate%22+AND+requesturlt%3AIdentify&s=0&searchOver=webLogs&report=Identify+requests+made+to+this+provider">Identify requests made to this provider</a><br/>
	&nbsp;&nbsp;<img src='../images/arrow_right_gray.gif' width='9' height='9' border='0'/>
	<a href="report.do?q=!%22rt%20text%22+AND+!%22rt%20validate%22+AND+requesturlt%3AListMetadataFormats&s=0&searchOver=webLogs&report=ListMetadataFormats+requests+made+to+this+provider">ListMetadataFormats requests made to this provider</a><br/>
	&nbsp;&nbsp;<img src='../images/arrow_right_gray.gif' width='9' height='9' border='0'/>
	<a href="report.do?q=!%22rt%20text%22+AND+!%22rt%20validate%22+AND+requesturlt%3AListSets&s=0&searchOver=webLogs&report=ListSets+requests+made+to+this+provider">ListSets requests made to this provider</a><br/>
	&nbsp;&nbsp;<img src='../images/arrow_right_gray.gif' width='9' height='9' border='0'/>
	<a href="report.do?q=!%22rt%20text%22+AND+!%22rt%20validate%22+AND+requesturlt%3AGetRecord&s=0&searchOver=webLogs&report=GetRecord+requests+made+to+this+provider">GetRecord requests made to this provider</a><br/>
	&nbsp;&nbsp;<img src='../images/arrow_right_gray.gif' width='9' height='9' border='0'/>
	<a href="report.do?q=!%22rt%20text%22+AND+!%22rt%20validate%22+AND+!requesturlt%3Adleseodlsearch+AND+requesturlt%3AListIdentifiers&s=0&searchOver=webLogs&report=ListIdentifiers+requests+made+to+this+provider">ListIdentifiers requests made to this provider</a><br/>	
	&nbsp;&nbsp;<img src='../images/arrow_right_gray.gif' width='9' height='9' border='0'/>
	<a href="report.do?q=!%22rt%20text%22+AND+!%22rt%20validate%22+AND+!requesturlt%3Adleseodlsearch+AND+requesturlt%3AListRecords&s=0&searchOver=webLogs&report=ListRecords+requests+made+to+this+provider">ListRecords requests made to this provider</a><br/>
	&nbsp;&nbsp;<img src='../images/arrow_right_gray.gif' width='9' height='9' border='0'/>
	<a href="report.do?q=!%22rt%20text%22+AND+!%22rt%20validate%22+AND+dleseodlsearch+requesturlt%3AListRecords&s=0&searchOver=webLogs&report=ODL+search+requests+made+to+this+provider">ODL search requests made to this provider</a><br/>

	<br/>	
	</td>
  </tr>
 --%>
 
  <logic:equal name="reportForm" property="numResults" value="0">
  <logic:present parameter="q">
	  <tr>
		<td colspan=2>
		<logic:notPresent name="reportForm" property="rq">
		There are no matching records for this report.
		</logic:notPresent>
		<logic:present name="reportForm" property="rq">
		You searched for <font color="blue"><bean:write name="reportForm" property="rq" filter="false"/></font>.
		There are no records in this report that match that query. 
		</logic:present>
		
		</td>
	  </tr>
  </logic:present>
  </logic:equal>
 

 <%-- Search within results --%>
<%--  <% if(reportForm.getRq() != null || !reportForm.getNumResults().equals("0")) { %> 
  <tr>
	<td colspan=2 nowrap>	 
  	<html:form action="/admin/report" method="GET">
		<script>function sf(){document.reportForm.rq.focus();}</script>
		
		Search within this report: <html:text property="rq" size="45"/>
		<logic:iterate name="reportForm" property="nrqParams" id="param">
			<input type='hidden' name='<bean:write name="param" property="name" filter="false"/>' value='<bean:write name="param" property="val" filter="false"/>'>
		</logic:iterate>
		<input type="hidden" name="s" value="0">	

		<html:submit value="Search"/>
		<br/>

	</html:form>
	</td>
	</tr>
	<% } %> --%>

	
  <logic:notEqual name="reportForm" property="numResults" value="0">
   <%-- <INPUT name="BUTTON" TYPE="BUTTON" ONCLICK="window.location.href='<c:url value='/admin/provider_status_setup.jsp'/>'"  VALUE="Back to provider setup and status"> --%>
   <tr>
	<td colspan=2 nowrap>		
		<%-- Pager --%>
		<logic:present name="reportForm" property="prevResultsUrl">
			<a href='report.do?<bean:write name="reportForm" property="prevResultsUrl"/>'><img src='../images/arrow_left.gif' width='16' height='13' border='0' alt='Previous results page'/></a>
		</logic:present>
		&nbsp;Results: <bean:write name="reportForm" property="start"/> - <bean:write name="reportForm" property="end"/> out of <bean:write name="reportForm" property="numResults"/>&nbsp;
		<logic:present name="reportForm" property="nextResultsUrl">
			<a href='report.do?<bean:write name="reportForm" property="nextResultsUrl"/>'><img src='../images/arrow_right.gif' width='16' height='13' border='0' alt='Next results page'/></a>
		</logic:present>
	</td>
  </tr>   
  
  <logic:iterate id="result" name="reportForm" property="results" indexId="index" offset="<%= reportForm.getOffset() %>" length="<%= reportForm.getLength() %>">  
	  <tr>
		<td colspan="2">		
			<hr noshade><br/>
				
			<%-- Display records with errors --%>
			<c:if test="${result.docReader.readerType == 'ErrorDocReader'}">
				<div class="reportDiv">
					<font size=+1><b>File: <c:out value="${result.docReader.fileName}" escapeXml="true"/></b></font>
				</div>
				<div class="reportDiv">
					This file could not be indexed due to errors and is not being served by the data provider.
				</div>
				
				<div class="reportDiv">
					<c:choose>
						<c:when test="${result.docReader.errorDocType == 'dupIdError'}">
								Reason: The ID of this file is '${result.docReader.duplicateId}' and is used by 
								<a href="query.do?q=id:${result.docReader.duplicateId}&s=0&rq=!doctype:0errordoc&selSetSpecs=+--+All+--&sfmts=+--+All+--&selStatus=+--+All+--">another file</a>. The 
								other file is located at <c:out value="${result.docReader.duplicateIdSourceFilePath}" escapeXml="true"/>.
						</c:when>
						<c:otherwise>
								Reason: <c:out value="${result.docReader.errorMsg}" escapeXml="true"/><br/>
<!--  
	Note: File ${result.docReader.fileName} could not be indexed due to Exception type: 
	${result.docReader.exceptionName} 
	
	Stack Trace:
	${result.docReader.stackTrace}
-->
						</c:otherwise>
					</c:choose>
				</div>
				
				<div class="reportDiv">
					<em>File location:</em> <c:out value="${result.docReader.docsource}" escapeXml="true"/>
					<br/>
					<em>File last modified:</em> <bean:write name="result" property="docReader.lastModifiedString" filter="false" />				
				<div class="reportDiv">
					[ <a href='query.do?file=<bean:write name="result" property="docReader.docsourceEncoded" filter="false" />&rt=text'>view file XML</a> ]
					[ <a href='query.do?file=<bean:write name="result" property="docReader.docsourceEncoded" filter="false" />&rt=validate'>validate file XML</a> ]
				</div>	
			</c:if>			

			<%-- Display docs with validation errors (currently not used in this version of the software) --%>
			<logic:present name="result" property="docReader.validationReport">
				<logic:match name="result" property="docReader.readerType" value="XMLDocReader">
					<font size=+1><b>File: <bean:write name="result" property="docReader.fileName" filter="false" /></b></font><br/>				
					<b>This file was indexed but contains validation errors.</b><br/>				
				</logic:match>
				<logic:match name="result" property="docReader.readerType" value="CollectionDocReader">
					<font size=+1><b><bean:write name="result" property="docReader.title" filter="false" /></b></font><br/>
					<a href='<bean:write name="result" property="docReader.url" filter="false" />' target=blank><bean:write name="result" property="docReader.url" filter="false" /></a><br/>
					<b>This record was indexed but contains validation errors.</b><br/>
					<b>Description:</b> &nbsp;<bean:write name="result" property="docReader.description" filter="false" /><br/>
				</logic:match>				
				<b>ID:</b> &nbsp;<bean:write name="result" property="docReader.id" filter="false" /><br/>							
				<b>Set:</b> &nbsp;
					<logic:iterate id="set" name="result" property="docReader.sets">
					<bean:write name="set" filter="false" />
					</logic:iterate><br/>				
				<b>File format:</b> <bean:write name="result" property="docReader.nativeFormat" filter="false" /><br/>
				<b>File location:</b> <bean:write name="result" property="docReader.docsource" filter="false" /><br/>								
				<b>File last modified:</b> <bean:write name="result" property="docReader.lastModifiedString" filter="false" /><br/>
				<b>Validation message:</b> <font color=red><bean:write name="result" property="docReader.validationReport" filter="false" /></font><br/>	
				<logic:iterate id="format" name="result" property="docReader.availableFormats">
				<b><bean:write name="format" filter="false" />:</b> 
				[ <a href='query.do?id=<bean:write name="result" property="docReader.id" filter="false" />&metadataFormat=<bean:write name="format" filter="false" />&rt=text'>view</a> ] 
				[ <a href='query.do?id=<bean:write name="result" property="docReader.id" filter="false" />&metadataFormat=<bean:write name="format" filter="false" />&rt=validate'>validate</a> ] &nbsp;&nbsp;			
				</logic:iterate>
			</logic:present>
			
			<%-- Display web log entries --%>
			<c:if test="${result.docReader.readerType == 'WebLogReader'}">
				<b>Requesting host:</b> ${result.docReader.remoteHost} -
				<a href="http://www.networksolutions.com/whois/results.jsp?ip=${result.docReader.remoteHost}" target="_blank">Perform a WHOIS lookup at NetworkSolutions</a>
				<br/>
				<b>Date of request:</b> ${result.docReader.requestDate}<br/>
				<c:if test="${not empty result.docReader.notes}">
					<b>Details:</b> ${result.docReader.notes}<br/>
				</c:if>
				<b>URL requested:</b> ${result.docReader.requestUrl}				
			</c:if>			
			
		</td>
	  </tr>   
  </logic:iterate>
  
   <tr>
   	<td colspan="2">
	 <hr noshade><br/>
	</td>
	</tr>
   <tr>
	<td colspan=2 nowrap>		
		<%-- Pager --%>
		<logic:present name="reportForm" property="prevResultsUrl">
			<a href='report.do?<bean:write name="reportForm" property="prevResultsUrl"/>'><img src='../images/arrow_left.gif' width='16' height='13' border='0' alt='Previous results page'/></a>
		</logic:present>
		&nbsp;Results: <bean:write name="reportForm" property="start"/> - <bean:write name="reportForm" property="end"/> out of <bean:write name="reportForm" property="numResults"/>&nbsp;
		<logic:present name="reportForm" property="nextResultsUrl">
			<a href='report.do?<bean:write name="reportForm" property="nextResultsUrl"/>'><img src='../images/arrow_right.gif' width='16' height='13' border='0' alt='Next results page'/></a>
		</logic:present>	
	</td>
  </tr>  
  </logic:notEqual> 
</table>


<%-- Include style/menu templates --%>
<%@ include file="../../bottom.jsp" %>

</body>
</html:html>

