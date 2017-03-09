<%@ page language="java" %>
<jsp:useBean id="hrf" class="org.dlese.dpc.oai.harvester.action.form.HarvestReportForm" scope="request"/>
<%@ taglib uri="/WEB-INF/tlds/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/tlds/struts-logic.tld" prefix="logic" %>

<!-- JSTL tags -->
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page isELIgnored ="false" %>

<html:html>
<head>
<title><c:out value="${hrf.reportTitle}" /></title>

<c:import url="../head.jsp"/>

<script>
	// Override the method below to set the focus to the appropriate field.
	function sf(){}	
</script>
</head>
<body onLoad=sf()>
<c:import url="../top.jsp?sec=harvester"/>

	<%-- ######## Display messages, if present ######## --%>
	<logic:messagesPresent> 		
	<table width="90%" bgcolor="#000000" cellspacing="1" cellpadding="8">
	  <tr bgcolor="ffffff"> 
		<td>
			<b>Messages:</b>
			<ul>
				<html:messages id="msg" property="message"> 
					<li><c:out value="${msg}" /></li>									
				</html:messages>
				<html:messages id="msg" property="error"> 
					<li><font color=red>Error: <c:out value="${msg}" /></font></li>									
				</html:messages>
			</ul>
			<blockquote>[ <a href="harvestreport.do?report=noreport">OK</a> ]</blockquote>
		</td>
	  </tr>
	</table>		
	<br><br>
	</logic:messagesPresent>
	
<table width="95%">

  <tr>
	<td colspan=2>
	 
        <h1><c:out value="${hrf.reportTitle}" /> 
	</h1>	
	<INPUT name="BUTTON" TYPE="BUTTON" ONCLICK="window.location.href='harvester.do'"  VALUE="Back to harvester setup and status"><p>

	</td>
  </tr>
  
  
  <c:if test="${hrf.numResults == 0}">
  <logic:present parameter="q">
	  <tr>
		<td colspan=2>
		<logic:notPresent name="hrf" property="rq"> <p>
		No matching harvest log entries. 
		<br>
		</logic:notPresent>
		<%-- <logic:present name="hrf" property="rq">
		You searched for <font color="blue"><c:out value="${hrf.rq}" escapeXml="false" /></font>.
		There are no records in this report that match that query. 
		</logic:present> --%>		
		</td>
	  </tr>
  </logic:present>
  </c:if>
 

 <%-- Search within results --%>
 <%-- <% if(hrf.getRq() != null || !hrf.getNumResults().equals("0")) { %> 
  <tr>
	<td colspan=2 nowrap>	 
  	<html:form action="/admin/report" method="GET">
		<script>function sf(){document.reportForm.rq.focus();}</script>
		
		Search within this report: <html:text property="rq" size="45"/>
		<c:forEach var="param" items="${hrf.nrqParams}">
			<input type='hidden' name='<c:out value="${param.name}" escapeXml="false"/>' value='<c:out value="${param.val}" escapeXml="false" />'>
		</c:forEach>
		<input type="hidden" name="s" value="0">	

		<html:submit value="Search"/>
		<br>

	</html:form>
	</td>
	</tr>
	<% } %> --%>

	
  <c:if test="${hrf.numResults != 0}"> 
   
   <tr>
	<td colspan=2 style="padding-top:15px" nowrap>		
		<%-- Pager --%>
		<nobr>
		<logic:present name="hrf" property="prevResultsUrl">
			<a href='harvestreport.do?<c:out value="${hrf.prevResultsUrl}" />'><img src='../images/arrow_left.gif' width='16' height='13' border='0' alt='Previous results page'/></a>
		</logic:present>
		Showing log entries <c:out value="${hrf.start}" /> - <c:out value="${hrf.end}"/> out of <c:out value="${hrf.numResults}"/>&nbsp;
		<logic:present name="hrf" property="nextResultsUrl">
			<a href='harvestreport.do?<c:out value="${hrf.nextResultsUrl}"/>'><img src='../images/arrow_right.gif' width='16' height='13' border='0' alt='Next results page'/></a>
		</logic:present>
	</td>
  </tr>   
   <tr>
	<td colspan=2 nowrap>	
	Most recent harvest shown first. New harvests may take a few seconds to appear. <p>
			<INPUT name="BUTTON" TYPE="BUTTON" ONCLICK="window.location.href='<c:out value="?${pageContext.request.queryString}" escapeXml="false" />'"  VALUE="Refresh this page to check for changes">

	</td>
  </tr>
  <logic:iterate id="result" name="hrf" property="results" indexId="index" offset="<%= hrf.getOffset() %>" length="<%= hrf.getLength() %>">  
	  <tr>
		<td colspan="2">		
			<hr noshade><br>
							
			<%-- Display harvest log entries --%>
			<c:if test="${result.docReader.readerType == 'HarvestLogReader'}">
				<c:if test="${!empty result.docReader.repositoryName}">
					<div style="color:#333333; font-weight: bold">
						<c:if test="${result.docReader.repositoryName != 'One-time harvest'}">
							<c:out value="${result.docReader.repositoryName}" escapeXml="false"/>
						</c:if>
						 <c:if test="${result.docReader.repositoryName == 'One-time harvest'}">
							<c:out value="${result.docReader.repositoryName}" escapeXml="false" />
						</c:if>
					</div>
				</c:if>
				<c:choose>
					<c:when test="${result.docReader.entryType == 'inprogress'}">
						<font color="green">This harvest is currently in progress</font>
						at <c:out value="${result.docReader.logDate}" escapeXml="false" />
						<a href='<c:out value="?${pageContext.request.queryString}" escapeXml="false" />'>Refresh this page to check for changes</a>.<br>
						<i>Most recent status:</i> 
						<c:out value="${result.docReader.logMessage}" escapeXml="false" /><br>
					</c:when>
					<c:otherwise>
						<c:choose>
							<c:when test="${result.docReader.entryType == 'completedsuccessful'}">
								This harvest completed successfully. One or more records have been updated.<br>
							</c:when>
							<c:when test="${result.docReader.entryType == 'completederroroai' && result.docReader.oaiErrorCode == 'noRecordsMatch'}">
								This harvest produced no matching records.
								<c:choose>
									<c:when test="${not empty result.docReader.fromDate}">
										No records have changed since the previous harvest.
									</c:when>
									<c:when test="${not empty result.docReader.set && empty result.docReader.untilDate}">
										The set <i>${result.docReader.set}</i> may be empty.
									</c:when>
								</c:choose>
								<br>
							</c:when>
							<c:when test="${result.docReader.entryType == 'completederroroai'}">
								This harvest produced zero results.
								<!--The data provider returned the following OAI code:
								<c:out value="${result.docReader.logMessage}" escapeXml="false" />--><br>
							</c:when>							
							<c:when test="${result.docReader.entryType == 'completederrorserious'}">
								This harvest was not successful.
								<c:out value="${result.docReader.logMessage}" escapeXml="false" /><br>
							</c:when>
							<c:otherwise>
								
							</c:otherwise>
						</c:choose>
						<i>Time harvest began:</i> ${result.docReader.startDate}.
						Harvest duration: ${not empty result.docReader.harvestDuration ? result.docReader.harvestDuration : 'Unknown'}.
						<br>
					</c:otherwise>
				</c:choose>
				<i>Base URL:</i> <c:out value="${result.docReader.baseUrl}" escapeXml="false" />
				<c:if test="${!empty result.docReader.set}">
					<i>SetSpec:</i> <c:out value="${result.docReader.set}" escapeXml="false" />
				</c:if>
				<br/>
				<i>Request made for:</i>
				<c:choose>
					<c:when test="${not empty result.docReader.fromDate}">
						Records modified since ${result.docReader.fromDate}.
					</c:when>
					<c:otherwise>
						All records.
					</c:otherwise>
				</c:choose>
				<br/>
				<c:if test="${not empty result.docReader.supportedGranularity || not empty result.docReader.deletedRecordSupport}">
					<div>
						<i>This data provider: </i>
						<c:if test="${not empty result.docReader.supportedGranularity}">
							Supports date granularity by ${result.docReader.supportedGranularity};
						</c:if>							
						<c:if test="${not empty result.docReader.deletedRecordSupport}">
							Has ${result.docReader.deletedRecordSupport} support for deleted records.
						</c:if>
					</div>
				</c:if>				
				<c:if test="${result.docReader.entryType == 'completedsuccessful' && not empty result.docReader.numHarvestedRecords}">
					<i>Total number of records harvested:</i> <c:out value="${result.docReader.numHarvestedRecords}" escapeXml="false" /><br>
				</c:if>
			
				<!--<c:if test="${!empty result.docReader.numResumptionTokens}">
					<i>Resumption tokens issued:</i> <c:out value="${result.docReader.numResumptionTokens}" escapeXml="false" /><br>
				</c:if>-->
				<c:if test="${!empty result.docReader.numHarvestedRecords}">
					<c:if test="${result.docReader.numHarvestedRecords > 0}">
						<c:choose>
							<c:when test="${result.docReader.entryType == 'inprogress'}">
								<i>Files being saved to:</i><br>
							</c:when>
							<c:when test="${result.docReader.entryType == 'completedsuccessful'}">
								<i>Files saved to:</i><br>
							</c:when>
							<c:otherwise>
								<i>Files located at:</i><br>
							</c:otherwise>
						</c:choose>
						<div style="padding-left:10px"><c:out value="${result.docReader.harvestDir}" escapeXml="false"/></div>
					</c:if>
					<c:if test="${not empty result.docReader.zipFilePath}">
						<i>Files zipped to:</i><br>
							<div style="padding-left:10px"><c:out value="${result.docReader.zipFilePath}" escapeXml="false"/></div>
					</c:if>
				</c:if>
			
			</c:if>
			
			
		</td>
	  </tr>   
  </logic:iterate>
  	   
	   <tr>
		<td colspan=2 nowrap>		
			<%-- Pager --%>
			<logic:present name="hrf" property="prevResultsUrl">
				<a href='harvestreport.do?<c:out value="${hrf.prevResultsUrl}" />'><img src='../images/arrow_left.gif' width='16' height='13' border='0' alt='Previous results page'/></a>
			</logic:present>
			&nbsp;Results: <c:out value="${hrf.start}" /> - <c:out value="${hrf.end}" /> out of <c:out value="${hrf.numResults}" />&nbsp;
			<logic:present name="hrf" property="nextResultsUrl">
				<a href='harvestreport.do?<c:out value="${hrf.nextResultsUrl}" />'><img src='../images/arrow_right.gif' width='16' height='13' border='0' alt='Next results page'/></a>
			</logic:present>	
		</td>
	  </tr>
	  
  </c:if>
</table>

<c:if test="${hrf.numResults > 4}">
<br><p>	<INPUT name="BUTTON" TYPE="BUTTON" ONCLICK="window.location.href='harvester.do'"  VALUE="Back to harvester setup and status"></p>
</c:if>

<p>
<INPUT name="BUTTON" TYPE="BUTTON" ONCLICK="window.location.href='<c:out value="?${pageContext.request.queryString}" escapeXml="false" />'"  VALUE="Refresh this page to check for changes">

<c:import url="../bottom.jsp"/>

</body>
</html:html>
