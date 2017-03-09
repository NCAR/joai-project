<%@ include file="../../TagLibIncludes.jsp" %>

<c:set var="rm" value="${applicationScope.repositoryManager}"/>

<c:set var="title" value="Sets Configuration"/>


<html:html>

<!-- $Id: display_sets.jsp,v 1.16 2009/12/02 21:41:06 jweather Exp $ -->

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
			
			<b>Messages</b>			
			<ul>
				<html:messages id="msg" property="message"> 
					<li><bean:write name="msg"/></li>									
				</html:messages>
				<html:messages id="msg" property="error"> 
					<li><font color=red>Error: <bean:write name="msg"/></font></li>									
				</html:messages>		
			</ul>
			
			<b>Actions:</b>
			
			<ul>
				<li class="actions"><a href="<c:url value="/admin/data-provider/sets.do"/>">OK</a> (close messages)</li>
			</ul>
			
		</td>
	  </tr>
	</table><br><br>
	</logic:messagesPresent>	
	
	<p> 
	A set is a subgroup of the metadata files in a repository. A set is defined using metadata formats, directories, keywords and/or search queries. For more information about how sets are used in the OAI protocol, <a href="http://www.openarchives.org/OAI/2.0/openarchivesprotocol.htm#Set">read about sets</a>.
	</p>

			
	<form style="margin-bottom:12px" method="post" action='<c:url value="/admin/set_definition-view.do"/>'>  
		<input type="submit" value="Define a new set"/>
		Define a  set for the data provider. 
	</form>			
			
	<%-- Display the current sets: --%>
	<c:catch>
		<c:if test="${not empty rm.listSetsConfigXml}">
			<x:parse var="listSetsConfXml" xml="${rm.listSetsConfigXml}"/>
		</c:if>
	</c:catch>
			
  </p>
  <table id="form" cellpadding="6" cellspacing="1" border="0">
		<tr id="headrow"> 
          <td align="center" style="font-weight:bold" nowrap>
		  	Set name<a href="javascript:popupHelp('<c:out value="${pageContext.request.contextPath}"/>','set_name')" class="helpp">   <img src='<c:out value="${pageContext.request.contextPath}" />/images/help.gif' border=0></a>
		  </td>
          <td align="center" style="font-weight:bold" nowrap>
		  	SetSpec<a href="javascript:popupHelp('<c:out value="${pageContext.request.contextPath}"/>','set_setspec')" class="helpp">   <img src='<c:out value="${pageContext.request.contextPath}" />/images/help.gif' border=0></a>
		  </td>
          <td align="center">
		  	<table>
				<tr id="headrow">
					<td align="center" style="font-weight:bold" nowrap>
						Records</br>
						Ready
					</td>
					<td align="left" valign="top">
						<a href="javascript:popupHelp('<c:out value="${pageContext.request.contextPath}"/>','set_records')" class="helpp"> <img src='<c:out value="${pageContext.request.contextPath}" />/images/help.gif' border=0></a>
					</td>
				</tr>
			</table>
		  </td>
          <td align="center">
		  	<table>
				<tr id="headrow">
					<td align="center" style="font-weight:bold" nowrap>
						Records</br>
						Deleted
					</td>
					<td align="left" valign="top">
						<a href="javascript:popupHelp('<c:out value="${pageContext.request.contextPath}"/>','set_records_deleted')" class="helpp"> <img src='<c:out value="${pageContext.request.contextPath}" />/images/help.gif' border=0></a>
					</td>
				</tr>
			</table>
		  </td>		  	  
          <td align="center" style="font-weight:bold" nowrap>
		  	Action
		  </td>
		</tr>
		<c:choose>
			<c:when test="${empty listSetsConfXml}">
				<c:set var="numSets" value="0"/>
			</c:when>
			<c:otherwise>
				<c:set var="numSets">	
					<x:out select="count($listSetsConfXml/ListSets/set)" />
				</c:set>
			</c:otherwise>
		</c:choose>
		<c:choose>
			<c:when test="${numSets > 0}">
				<x:forEach select="$listSetsConfXml/ListSets/set" var="set">
					<tr id="formrow"> 
					  <td>
						<c:set var="setSpec"><x:out select="$set/setSpec"/></c:set>
						<b><x:out select="$set/setName"/></b>
				      </td>
					   <td align="center">
						${setSpec}
					   </td>
					   <td align="center">
					   		<a href='<c:url value="/admin/query.do"/>?q=&selSetSpecs=${setSpec}&s=0&sfmts=+--+All+--&selStatus=false' 
								title='Browse non-deleted records for "<x:out select="$set/setName"/>"'><bean:write name="repositoryManager" property="numRecordsInSet(${setSpec})" filter="false"/></a> 
					   </td>
					   <td align="center">
					   		<a href='<c:url value="/admin/query.do"/>?q=&selSetSpecs=${setSpec}&s=0&sfmts=+--+All+--&selStatus=true' 
								title='Browse deleted records for "<x:out select="$set/setName"/>"'><bean:write name="repositoryManager" property="numDeletedRecordsInSet(${setSpec})" filter="false"/></a> 					   
					   </td>
					   <td align="center" nowrap>
					   		<c:set var="setName"><x:out select="$set/setName" escapeXml="false"/></c:set>
							
							<form 	action="${pageContext.request.contextPath}/admin/update_set_definition.do" method="POST" style="padding: 0px; margin: 0px"
									title="Edit this set definition">
								<input name="edit" value="${setSpec}" type="hidden" />
								<input name="command" value="updateSetDefinition" type="hidden" />
								<input type="submit" name="button" value="Edit" class="smallButton" style="width:70px" />
							</form>	
							
							<form 	action="${pageContext.request.contextPath}/admin/update_set_definition.do" method="POST" style="padding: 0px; margin: 4px"
									title="Delete this set definition"
									onsubmit="return confirm('Are you sure you want to delete the set definition \'${f:jsEncode(setName)}\'?')">
								<input name="setSpec" value="${setSpec}" type="hidden" />
								<input name="setName" value="${setName}" type="hidden" />
								<input name="command" value="deleteSetDefinition" type="hidden" />
								<input type="submit" name="button" value="Delete" class="smallButton" style="width:70px" />
							</form>								
							
					   </td>
					</tr>
				</x:forEach>
			</c:when>
			<c:otherwise>
				<tr id="formrow"> 
					  <td colspan="5">
					  	<font color="gray"><b>No sets configured</b></font>
					  </td>
				</tr>
			</c:otherwise>
		</c:choose>
  </table>	  
	  
  <form style="margin-top:12px" method="post" action='<c:url value="/admin/set_definition-view.do"/>'>  
    <input type="submit" value="Define a new set"/>
  </form>
    
    
  </p>
  <table height="10">
  <tr> 
    <td>&nbsp;</td>
  </tr>
</table>

<%-- Include style/menu templates --%>
<%@ include file="../../bottom.jsp" %>
</body>
</html:html>



