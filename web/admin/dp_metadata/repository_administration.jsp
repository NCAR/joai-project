<%@ include file="../../TagLibIncludes.jsp" %>
<%@ include file="../../baseUrl.jsp" %>
<%@ page import="org.dlese.dpc.repository.*" %>

<%-- This needs to be saved here since var 'index' is conflicted below --%>
<c:set var="indexLocation" value="${index.indexLocation}"/>

<c:set var="rm" value="${applicationScope.repositoryManager}"/>


<c:set var="title" value="Repository Information and Administration"/>

<html:html>

<!-- $Id: repository_administration.jsp,v 1.18 2006/10/03 23:16:44 jweather Exp $ -->

<head>
<title>${title}</title>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">


<style type="text/css">
	/* Override the 'suggested' background color behind the resources's title */
	LI.actions {
		padding-bottom:8px;
		list-style-type:disc;
	}
	LI {
		padding-bottom:3px;
		list-style-type:circle;
	}	
</style>

<%-- Include style/menu templates --%>
<%@ include file="../../head.jsp" %>
<style type="text/css">
<!--
.style1 {color: #FF0000}
-->
</style>
</head>

<body>

<%-- Include style/menu templates --%>
<c:import url="../../top.jsp?sec=provider" />
	
	<h1>${title}</h1>

	  
	<%-- ####### Display messages, if present ####### --%>
	<logic:messagesPresent> 		
	<table width="100%" bgcolor="#000000" cellspacing="1" cellpadding="8">
	  <tr bgcolor="ffffff"> 
		<td>
			<%-- <html:messages id="msg" property="showIndexMessagingLink"> 
				Indexing messages are displayed below<br><br>
			</html:messages>	 --%>		
			
			<%-- Default messages display... --%>
			<%-- <ul>
				<html:messages id="msg"> 
						<li class="actions"><bean:write name="msg"/></li>								
				</html:messages>			
			</ul> --%>

			
			<c:if test="${param.command == 'showIndexingMessages'}">
				<b>Actions:</b>
				
				<ul>
					<html:messages id="msg" property="showIndexMessagingLink"> 
							<li class="actions"><bean:write name="msg"/><a href="data-provider-info.do?command=showIndexingMessages">Check and refresh indexing status messages again</a></li>								
					</html:messages>			
					<li class="actions"><a href="data-provider-info.do">OK</a> (close messages)</li>
				</ul>
			</c:if>
			
			<b>${param.command == 'showIndexingMessages' ? 'Indexing status messages:' : 'Messages'}</b>			
			<ul>
				<html:messages id="msg" property="message"> 
					<li><bean:write name="msg"/></li>									
				</html:messages>
				<html:messages id="msg" property="error"> 
					<li><font color=red>Error: <bean:write name="msg"/></font></li>									
				</html:messages>
				<html:messages id="msg" property="showIndexMessagingLink">
					
					<li>
						<a href="data-provider-info.do?command=showIndexingMessages">Check most recent indexing status</a> for information about the progress of the indexing process. 
					</li>
					<%-- <logic:greaterThan name="raf" property="numIndexingErrors" value="0">
						<li>Some records had errors during indexing.
						<a href="report.do?q=error:true&s=0&report=Files+that+could+not+be+indexed+due+to+errors">See 
						list of errors</a></li>
					</logic:greaterThan> --%>
				</html:messages>		
			</ul>
			
			<c:if test="${param.command != 'showIndexingMessages'}">
				<b>Actions:</b>
				
				<ul>
					<li class="actions"><a href="data-provider-info.do">OK</a> (close messages)</li>
				</ul>
			</c:if>			
			
		</td>
	  </tr>
	</table><br><br>
	</logic:messagesPresent>	

	
	  
	  
	<p><a name="repository"></a><h3>Repository information</h3></p>
	
	<p>
  <form method="post" action="update_repository_info.do" style="margin-bottom:8px">
			<input type="submit" value="Edit repository info"/>
				<input type="hidden" name="command" value="updateRepositoryInfo"/>
				Update repository name, administrator's email, repository description or namespace identifier.
				
  </form>
	</p>		

	
	<table id="form" cellpadding="6" cellspacing="1" border="0">
		<tr> 
			  <td align="left" id="headrow" style="font-weight:bold;" nowrap>
				Repository name (required):
				<a href="javascript:popupHelp('<c:out value="${pageContext.request.contextPath}"/>','repinfo_repname')" class="helpp">   <img src='<c:out value="${pageContext.request.contextPath}" />/images/help.gif' border=0></a>			  </td>
			  <td id="formrow">
			  	<c:choose>
					<c:when test="${empty rm.repositoryName}">
						<font color="gray"><b>No repository name specified</b>
					</c:when>
					<c:otherwise>
						${rm.repositoryName}
					</c:otherwise>
				</c:choose>			  
			  </td>
		</tr>
		<tr>
			  <td align="left" id="headrow" style="font-weight:bold;"  nowrap>
				Repository administrator's e-mail (required):
				<a href="javascript:popupHelp('<c:out value="${pageContext.request.contextPath}"/>','repinfo_adminemail')" class="helpp">   <img src='<c:out value="${pageContext.request.contextPath}" />/images/help.gif' border=0></a>
			  </td>
			  <td id="formrow">
			  	<c:choose>
					<c:when test="${empty rm.adminEmails[0]}">
						<font color="gray"><b>No e-mail specified </b>
					</c:when>
					<c:otherwise>
						${rm.adminEmails[0]}
					</c:otherwise>
				</c:choose>
			  </td>
		</tr>
		<tr>
			  <td align="left" id="headrow" style="font-weight:bold;"  nowrap>
				Repository description (optional):
				<a href="javascript:popupHelp('<c:out value="${pageContext.request.contextPath}"/>','repinfo_repdesc')" class="helpp">   <img src='<c:out value="${pageContext.request.contextPath}" />/images/help.gif' border=0></a>
			  </td>
			  <td id="formrow">
			  	<c:choose>
					<c:when test="${empty rm.descriptions[0]}">
						<font color="gray"><b>No description specified</b>
					</c:when>
					<c:otherwise>
						<c:out value="${rm.descriptions[0]}" escapeXml="true"/>
					</c:otherwise>
				</c:choose>					  
			  </td>	
		</tr>
		<tr>
			  <td align="left" id="headrow" style="font-weight:bold;"  nowrap>
				Repository namespace identifier (optional):
				<a href="javascript:popupHelp('<c:out value="${pageContext.request.contextPath}"/>','repinfo_namespace')" class="helpp">   <img src='<c:out value="${pageContext.request.contextPath}" />/images/help.gif' border=0></a>
			  </td>
			  <td id="formrow">
			  	<c:choose>
					<c:when test="${empty rm.repositoryIdentifier}">
						<font color="gray"><b>No namespace identifier specified</b>
					</c:when>
					<c:otherwise>
						${rm.repositoryIdentifier}
					</c:otherwise>
				</c:choose>				  
			  </td>			  
	  </tr>
	  		<tr>
				<td align="left" id="headrow" style="font-weight:bold;"  nowrap>
				Repository base URL (non-editable):
				<a href="javascript:popupHelp('<c:out value="${pageContext.request.contextPath}"/>','repinfo_baseurl')" class="helpp">   <img src='<c:out value="${pageContext.request.contextPath}" />/images/help.gif' border=0></a>
				</td>
				<td id="formrow">
				${myBaseUrl}			  
				</td>			  
	  </tr>
	</table>
	
	  
	<%-- Turn the data provider on/off (enable/disable) --%>
	<table>       
	  <tr>
		<td>
			<br/>  
			<a name="access"></a><h3>Access to the data provider</h3>
		</td>
	  </tr>	
	  <tr>
		<td>
		
			<logic:equal value="ENABLED" property="providerStatus" name="raf">
				<html:form 	action="/admin/data-provider-info.do" 
					onsubmit="return confirm( 'Are you sure you want to disable access to the data provider?' )" 
					method="GET">
					<p>
						Access to the data provider is currently <font color="green"><b>ENABLED</b></font>.
						Metadata files are available for harvesting.					</p>
					<p nobr><html:submit property="statusButton" value="Disable provider access" />
				      Prevent all 
					metadata files in the repository from being harvested.</p>
					<input type="hidden" name="providerStatus" value="DISABLED">
					<input type="hidden" name="command" value="changeProviderStatus">
				</html:form>
			</logic:equal>
			<logic:equal value="DISABLED" property="providerStatus" name="raf">		
				<html:form 	action="/admin/data-provider-info.do" 
					onsubmit="return confirm( 'Are you sure you want to enable access to the data provider?' )" 
					method="GET">
					<p>
						Access to the data provider is currently <font color="red"><b>DISABLED</b></font>.
						Your metadata files are not available for harvesting.
					</p>
					<p nobr><html:submit property="statusButton" value="Enable provider access" /> Allows all metadata files in the 
					repository to be harvested.</p>
						<input type="hidden" name="providerStatus" value="ENABLED">
					<input type="hidden" name="command" value="changeProviderStatus">
				</html:form>
			</logic:equal>		
		
	   </td>
	  </tr>
  </table>	  
	  
	  
	<br/><br/>
	
	<%-- Num records per resumption token --%>
	<a name="response"></a><h3>OAI response length</h3>	
	<p> 
		The following values define the maximum length of the 
		data provider's response to the OAI 
		<a href="http://www.openarchives.org/OAI/2.0/openarchivesprotocol.htm#ListRecords"><code>ListRecords</code></a>
		and
		<a href="http://www.openarchives.org/OAI/2.0/openarchivesprotocol.htm#ListIdentifiers"><code>ListIdentifiers</code></a>
		requests. 
		  
		Reduce the length of these responses if a large number of sets have been defined (which requires more processing), 
		system memory is limited or if metadata files are large.			
		Smaller numbers mean that harvesters must make more requests to harvest the entire metadata repository. 
		Larger numbers mean fewer requests are necessary but require greater system resources by both the data provider and the harvester.
		  
	    <html:form 	action="/admin/data-provider-info.do" method="POST" style="margin-bottom:8px">			
			<input type="hidden" name="numRecordsPerToken" value="t">
			<html:submit property="editNumResults" value="Edit response length" />&nbsp;&nbsp; Change the number of returns in the ListRecords and ListIdentifiers
			responses.
		</html:form>
	</p>	
	
	<table width="40%" border="0" cellpadding="6" cellspacing="1" id="form">
		<tr id="headrow">
			<td><b>
			<div align="center">OAI Request</div>
			</b></td>
			<td><b><div align="center">Maximum<br/>
			Response Length</div>
			</b></td>
		</tr>
		<tr id="formrow" nowrap>
			<td>
			  <div align="center"><a href="http://www.openarchives.org/OAI/2.0/openarchivesprotocol.htm#ListRecords"><code>ListRecords</code></a> </div></td>
			<td><div align="center">${rm.numRecordsResults} records</div></td>		
		</tr>
		<tr id="formrow" nowrap>
			<td>
			  <div align="center"><a href="http://www.openarchives.org/OAI/2.0/openarchivesprotocol.htm#ListIdentifiers"><code>ListIdentifiers</code></a></div></td>
			<td><div align="center">${rm.numIdentifiersResults} identifiers</div></td>		
		</tr>		
  </table>

	 <br/><br/> 
	 
	 
<table height="100">
  <tr> 
    <td>&nbsp;</td>
  </tr>
</table>

<%-- Include style/menu templates --%>
<%@ include file="../../bottom.jsp" %>
</body>
</html:html>


