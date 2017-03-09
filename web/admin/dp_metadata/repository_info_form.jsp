<%@ include file="../../TagLibIncludes.jsp" %>
<c:set var="rm" value="${applicationScope.repositoryManager}"/>

<c:set var="title">
	Edit Repository Information
</c:set>

<html:html>
  <head>
    <title>${title}</title>
    <html:base />

	<%-- Include style/menu templates --%>
	<%@ include file="../../head.jsp" %>
  </head>

  <body>
  
	<%-- Include style/menu templates --%>
	<c:import url="../../top.jsp?sec=provider" />
  	
	<h1>${title}</h1>

	
	<noscript>
		<p>Note: This page uses JavaScript for certain features. 
			Please enable JavaScript in your
			browser to take advantage of these features.</p>
	</noscript> 
	
	<logic:messagesPresent>
		<c:set var="errorMsg">There was an error. Please correct the problem below:</c:set>	
		<%-- <html:messages property="noDefinitionProvided" id="noDefinitionProvided">
			<c:set var="errorMsg">No definition was provided. Please define at least one option below.</c:set>								
		</html:messages> --%>
		<p><font color="red"><b>${errorMsg}</b></font></p>
	</logic:messagesPresent>
			
	
  	<html:form action="admin/repository_info-validate" method="POST" focus="repositoryName">
	<table id="form" cellpadding="6" cellspacing="1" border="0">
		<tr id="headrow">
			<td>
				<div><b>Repository information</b></div>
			</td>
		</tr>
		<p>
		Complete the required fields of repository name and administrator's email address.
		<br>
		<tr id="formrow">
			<td>		
				<div><b>Repository name:</b> <a href="javascript:popupHelp('<c:out value="${pageContext.request.contextPath}"/>','repinfo_repname')" class="helpp">   <img src='<c:out value="${pageContext.request.contextPath}" />/images/help.gif' border=0></a>
</div>
				<div><html:text property="repositoryName" size="80" maxlength="80" /></div>
				<font color="red"><html:errors property="repositoryName"/></font>
			</td>
		</tr>
		<tr id="formrow">
			<td>
				<div><b>Administor's e-mail address:</b> <a href="javascript:popupHelp('<c:out value="${pageContext.request.contextPath}"/>','repinfo_adminemail')" class="helpp">   <img src='<c:out value="${pageContext.request.contextPath}" />/images/help.gif' border=0></a></div>
				<div><html:text property="adminEmail" size="35"/></div>
				<font color="red"><html:errors property="adminEmail"/></font>
			</td>
		</tr>		
		<tr id="formrow">
			<td>
				<div><b>Repository description (optional):</b> <a href="javascript:popupHelp('<c:out value="${pageContext.request.contextPath}"/>','repinfo_repdesc')" class="helpp">   <img src='<c:out value="${pageContext.request.contextPath}" />/images/help.gif' border=0></a></div>
				<div><b><html:textarea property="repositoryDescription" cols="60" rows="3" /></b></div>
				<font color="red"><html:errors property="repositoryDescription"/></font>
			</td> 
		</tr>
		<tr id="formrow">
			<td>
				<div><b>Namespace identifier (optional):</b> <a href="javascript:popupHelp('<c:out value="${pageContext.request.contextPath}"/>','repinfo_namespace')" class="helpp">   <img src='<c:out value="${pageContext.request.contextPath}" />/images/help.gif' border=0></a></div>
				<div><b><html:text property="namespaceIdentifier" size="35"/></b></div>
				<font color="red"><html:errors property="namespaceIdentifier"/></font>
			</td>
		</tr>
		
	</table>
				
	<table cellpadding="6" cellspacing="1" border="0">	
		<tr>
			<td>	
				<c:choose>
					<c:when test="${not empty param.edit}">
						<input type="hidden" name="edit" value="${param.edit}">
					</c:when>
					<c:otherwise>
						
					</c:otherwise>
				</c:choose>
				
				<html:submit>Save</html:submit>
				<%-- <html:reset>Reset</html:reset> --%>
				<html:cancel>Cancel</html:cancel>
			</td>
		 </tr>
      </table>
	 </html:form>

  
<%-- Include style/menu templates --%>
<%@ include file="../../bottom.jsp" %>  
  </body>
</html:html>
