<%@ include file="../../TagLibIncludes.jsp" %>
<%@ page import="org.dlese.dpc.repository.*" %>

<%-- This needs to be saved here since var 'index' is conflicted below --%>
<c:set var="indexLocation" value="${index.indexLocation}"/>

<c:set var="rm" value="${applicationScope.repositoryManager}"/>


<c:set var="title" value="Metadata Files Configuration"/>	


<html:html>

<!-- $Id: display_metadata_directories.jsp,v 1.46 2009/12/02 21:41:06 jweather Exp $ -->

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
.style1 {color: #FF0000}
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
							<li class="actions"><bean:write name="msg"/><a href="data-provider.do?command=showIndexingMessages">Check and refresh indexing status messages again</a></li>								
					</html:messages>			
					<li class="actions"><a href="data-provider.do">OK</a> (close messages)</li>
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
						<a href="data-provider.do?command=showIndexingMessages">Check most recent indexing status</a> for information about the progress of the indexing process. 
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
					<li class="actions"><a href="data-provider.do">OK</a> (close messages)</li>
				</ul>
			</c:if>			
			
		</td>
	  </tr>
	</table><br><br>
	</logic:messagesPresent>	

	<p>Add metadata files to the data provider by adding directories of files. Columns can be sorted by clicking on column titles.  
	
	<p>
	  <form action="metadata_dir-view.do" method="get" style="margin-bottom:8px">
				<input type="submit" value="Add metadata directory"/>
				Add metadata files to the repository.
	  </form>	
	</p>

	
	  <c:set var="totalFiles" value="0"/>
	  <c:set var="totalIndexed" value="0"/>
	  <c:set var="totalDeleted" value="0"/>
	  <c:set var="totalErrors" value="0"/>
	  <c:set var="totalIndexEntries" value="${index.numDocs}"/>
	  
	  <table id="form" cellpadding="6" cellspacing="1" border="0">
        <%-- ######## Collections UI ######## --%>
		<html:form action="/admin/data-provider" method="GET">
		<tr id="headrow"> 
          <td align="center" nowrap>
		  	<logic:equal name="raf" property="sortSetsBy" value="collection">	
				<a href='data-provider.do?sortSetsBy=collection' title="Refresh" style="text-decoration: none; color: #000000"><b>Metadata Directory</b></a> <a href="javascript:popupHelp('<c:out value="${pageContext.request.contextPath}"/>','dm_directory')" class="helpp"><img src="<c:url value='/images/help.gif'/>" border=0></a>
			</logic:equal>
		  	<logic:notEqual name="raf" property="sortSetsBy" value="collection">	
				<a href='data-provider.do?sortSetsBy=collection' title="Sort by name of directory"><b>Metadata Directory</b></a> <a href="javascript:popupHelp('<c:out value="${pageContext.request.contextPath}"/>','dm_directory')" class="helpp"><img src="<c:url value='/images/help.gif'/>" border=0></a>
			</logic:notEqual>			
		  </td>
		  <td align="center" nowrap>
		  	<logic:equal name="raf" property="sortSetsBy" value="format">
				<a href='data-provider.do?sortSetsBy=format' title="Refresh" style="text-decoration: none; color: #000000"><b>Format</b></a><a href="javascript:popupHelp('<c:out value="${pageContext.request.contextPath}"/>','dm_format')" class="helpp"> <img src="<c:url value='/images/help.gif'/>" border=0>  </a>
			</logic:equal>
		  	<logic:notEqual name="raf" property="sortSetsBy" value="format">	
				<a href='data-provider.do?sortSetsBy=format' title="Sort by format"><b>Format</b></a><a href="javascript:popupHelp('<c:out value="${pageContext.request.contextPath}"/>','dm_format')" class="helpp"> <img src="<c:url value='/images/help.gif'/>" border=0>  </a>
			</logic:notEqual>		  		
		  </td>
		  <td align="center" nowrap>
		  	<logic:equal name="raf" property="sortSetsBy" value="numFiles">
				<table>
			<tr>
			<td valign="top"><a href='data-provider.do?sortSetsBy=numFiles' title="Refresh" style="text-decoration: none; color: #000000"><b>Num<br>Files</b></a>

			</td>
			<td valign="top"><a href="javascript:popupHelp('<c:out value="${pageContext.request.contextPath}"/>','dm_num')" class="helpp"> <img src="<c:url value='/images/help.gif'/>" border=0>  </a>
			</td>
			</tr>
			</table>
			</logic:equal>
		  	<logic:notEqual name="raf" property="sortSetsBy" value="numFiles">	
			<table>
			<tr>
			<td valign="top"><a href='data-provider.do?sortSetsBy=numFiles' title="Sort by number of files"><b>Num<br>Files</b></a>

			</td>
			<td valign="top"><a href="javascript:popupHelp('<c:out value="${pageContext.request.contextPath}"/>','dm_num')" class="helpp"> <img src="<c:url value='/images/help.gif'/>" border=0>  </a>
			</td>
			</tr>
			</table>
			
			</logic:notEqual>		  
		  </td>
		  <td align="center" nowrap>
		  	<logic:equal name="raf" property="sortSetsBy" value="numIndexed">
			<table>
			<tr>
			<td valign="top"><a href='data-provider.do?sortSetsBy=numIndexed' title="Refresh" style="text-decoration: none; color: #000000"><div align="center"><b>Num<br>Ready</b></div></a>

			</td>
			<td valign="top"><a href="javascript:popupHelp('<c:out value="${pageContext.request.contextPath}"/>','dm_indexed')" class="helpp"> <img src="<c:url value='/images/help.gif'/>" border=0>  </a>
			</td>
			</tr>
			</table>
				
			</logic:equal>
		  	<logic:notEqual name="raf" property="sortSetsBy" value="numIndexed">	
			<table>
			<tr>
			<td valign="top"><a href='data-provider.do?sortSetsBy=numIndexed' title="Sort by number of files indexed"><div align="center"><b>Num<br>Ready</b></div></a>
			</td>
			<td valign="top">				<a href="javascript:popupHelp('<c:out value="${pageContext.request.contextPath}"/>','dm_indexed')" class="helpp"> <img src="<c:url value='/images/help.gif'/>" border=0></a>

			</td>
			</tr>
			</table>
			</logic:notEqual>		  		  
		  </td>
		  
		  <td align="center" nowrap>
		  	<logic:equal name="raf" property="sortSetsBy" value="numDeleted">
			<table>
			<tr>
			<td valign="top"><a href='data-provider.do?sortSetsBy=numDeleted' title="Refresh" style="text-decoration: none; color: #000000"><div align="center"><b>Num<br>Deleted</b></div></a>

			</td>
			<td valign="top"><a href="javascript:popupHelp('<c:out value="${pageContext.request.contextPath}"/>','dm_deleted')" class="helpp"> <img src="<c:url value='/images/help.gif'/>" border=0>  </a>
			</td>
			</tr>
			</table>
				
			</logic:equal>
		  	<logic:notEqual name="raf" property="sortSetsBy" value="numDeleted">	
			<table>
			<tr>
			<td valign="top"><a href='data-provider.do?sortSetsBy=numDeleted' title="Sort by number of files deleted"><div align="center"><b>Num<br>Deleted</b></div></a>
			</td>
			<td valign="top">				<a href="javascript:popupHelp('<c:out value="${pageContext.request.contextPath}"/>','dm_deleted')" class="helpp"> <img src="<c:url value='/images/help.gif'/>" border=0></a>

			</td>
			</tr>
			</table>
			</logic:notEqual>		  		  
		  </td>		  
		  
		  <td align="center" nowrap>
		  	<logic:equal name="raf" property="sortSetsBy" value="numIndexingErrors">
				<table>
			<tr>
			<td valign="top"><a href='data-provider.do?sortSetsBy=numIndexingErrors' title="Refresh" style="text-decoration: none; color: #000000"><div align="center"><b>Indexing<br>Errors</b></div></a>

			</td>
			<td valign="top"><a href="javascript:popupHelp('<c:out value="${pageContext.request.contextPath}"/>','dm_errors')" class="helpp"> <img src="<c:url value='/images/help.gif'/>" border=0>  </a>
			</td>
			</tr>
			</table>
				
			</logic:equal>
		  	<logic:notEqual name="raf" property="sortSetsBy" value="numIndexingErrors">
			<table>
			<tr>
			<td valign="top"><a href='data-provider.do?sortSetsBy=numIndexingErrors' title="Sort by number of files with indexing errors"><div align="center"><b>Indexing<br>Errors</b></div></a>

			</td>
			<td valign="top"><a href="javascript:popupHelp('<c:out value="${pageContext.request.contextPath}"/>','dm_errors')" class="helpp"> <img src="<c:url value='/images/help.gif'/>" border=0>  </a>
			</td>
			</tr>
			</table>
			</logic:notEqual>		  			  
		  </td>		  
		  <td align="center" nowrap>
		  	<logic:equal name="raf" property="sortSetsBy" value="status">
				<table>
			<tr>
			<td valign="top"><a href='data-provider.do?sortSetsBy=status' title="Refresh" style="text-decoration: none; color: #000000"><div align="center"><b>Provider<br>Access</b></div></a>

			</td>
			<td valign="top"><a href="javascript:popupHelp('<c:out value="${pageContext.request.contextPath}"/>','dm_provider')" class="helpp"> <img src="<c:url value='/images/help.gif'/>" border=0>  </a>
			</td>
			</tr>
			</table>
			</logic:equal>
		  	<logic:notEqual name="raf" property="sortSetsBy" value="status">	
					<table>
			<tr>
			<td valign="top"><a href='data-provider.do?sortSetsBy=status' title="Sort by status"><div align="center"><b>Provider<br>Access</b></div></a>

			</td>
			<td valign="top"><a href="javascript:popupHelp('<c:out value="${pageContext.request.contextPath}"/>','dm_provider')" class="helpp"> <img src="<c:url value='/images/help.gif'/>" border=0>  </a>
			</td>
			</tr>
			</table>
			</logic:notEqual>		  
		  </td>
		  <td align="center" nowrap>
			<b>Action</b><a href="javascript:popupHelp('<c:out value="${pageContext.request.contextPath}"/>','dm_action')" class="helpp"> <img src="<c:url value='/images/help.gif'/>" border=0>  </a>
		  </td>			    	  
		</tr>
		</html:form>	
			
		<c:choose>
			<c:when test="${empty raf.sets}">
				<tr id="formrow"> 
				  <td colspan="8"> <font color="gray"> <b>No metadata directories specified</b> 
					</font> </td> 
				 </tr>
			</c:when>
			<c:otherwise>
				<logic:iterate id="set" name="raf" property="sets" indexId="index">
				  
				<c:set var="totalFiles" value="${totalFiles + set.numFiles}"/>
				<c:set var="totalIndexed" value="${totalIndexed + set.numIndexed}"/>
				<c:set var="totalDeleted" value="${totalDeleted + set.numDeleted}"/>		  
				<c:set var="totalErrors" value="${totalErrors + set.numIndexingErrors}"/>				
				  
				<tr id="formrow">
				  <td nowrap>
					<b>${set.name}</b>
					<br/>
					<c:choose>
						<c:when test="${fn:length(set.directory) > 50}">
							${fn:substring(set.directory,0,17)}
							...
							${fn:substring(set.directory,fn:length(set.directory)-30,fn:length(set.directory))}
						</c:when>
						<c:otherwise>
							${set.directory}
						</c:otherwise>
					</c:choose>				
				  </td>
				  <%-- <td>${set.setSpec}</td> --%>
				  <td align="center">${set.format}</td>
				  <td align="center"><fmt:formatNumber type="number" value="${set.numFiles}"/></td>
				  <td align="center">
					<logic:greaterThan name="set" property="numIndexed" value="0">
						<c:set value="key" target="${raf}" property="field" />
						<c:set value="${set.setSpec}" target="${raf}" property="value" />
						<c:url var="searchLnk" value='query.do'>
							<c:param name="q" value=''/>
							<c:param name="rq" value='collection:0${set.setSpec} AND !error:true AND deleted:false'/>
							<c:param name="title" value='Records for: ${set.name}'/>
							<c:param name="selSetSpecs" value=' -- All --'/>
							<c:param name="sfmts" value=' -- All --'/>
							<c:param name="selStatus" value=' -- All --'/>
							<c:param name="s" value="0"/>
						</c:url>							
						<a href='${searchLnk}' title='Browse records for "${set.name}"'><fmt:formatNumber type="number" value="${set.numIndexed}"/></a>
					</logic:greaterThan> 		  
					<logic:equal name="set" property="numIndexed" value="0">	
						0
					</logic:equal>	 
				  </td>
				  <td align="center">
					<logic:greaterThan name="set" property="numDeleted" value="0">
						<c:set value="key" target="${raf}" property="field" />
						<c:set value="${set.setSpec}" target="${raf}" property="value" />
						<c:url var="searchLnk" value='query.do'>
							<c:param name="q" value=''/>
							<c:param name="rq" value='collection:0${set.setSpec} AND !error:true AND deleted:true'/>
							<c:param name="title" value='Deleted records: ${set.name}'/>
							<c:param name="selSetSpecs" value=' -- All --'/>
							<c:param name="sfmts" value=' -- All --'/>
							<c:param name="selStatus" value=' -- All --'/>
							<c:param name="s" value="0"/>
						</c:url>						
						<a href='${searchLnk}' title='Browse deleted records for "${set.name}"'><fmt:formatNumber type="number" value="${set.numDeleted}"/></a>
					</logic:greaterThan> 		  
					<logic:equal name="set" property="numDeleted" value="0">	
						0
					</logic:equal>	 
				  </td>		  
				  <td align="center">
					<logic:greaterThan name="set" property="numIndexingErrors" value="0">
						<c:url var="reportLnk" value='report.do'>
							<c:param name="q" value='docdirenc:${f:encodeToTerm(set.directory)} AND error:true'/>
							<c:param name="s" value="0"/>
							<c:param name="report">Indexing Errors: ${set.name}</c:param>
						</c:url>
						<a href='${reportLnk}'
							title='Show indexing errors for &quot;${set.name}&quot;'><fmt:formatNumber type="number" value="${set.numIndexingErrors}"/></a>
					</logic:greaterThan> 		  
					<logic:equal name="set" property="numIndexingErrors" value="0">	
						0
					</logic:equal>	  
				  </td>
				  <td align="center" nowrap>
					<%-- Enable / Disable Collection --%>
					<c:choose>
						<c:when test="${set.enabled == 'true'}">
							<font color="#333333"><b>Enabled</b></font><br/> 
	
							<form 	action="data-provider.do" method="POST" class="inlineForm"
									title="Disable access to '${set.name}'"
									onsubmit="return confirm('Are you sure you want to disable access to \'${f:jsEncode(set.name)}\'?')">
								<input name="currentIndex" value="${index}" type="hidden" />
								<input name="currentSetName" value="${set.name}" type="hidden" />
								<input name="currentSetSpec" value="${set.setSpec}" type="hidden" />
								<input name="disableSet" value="t" type="hidden" />
								<input name="setUid" value="${set.uniqueID}" type="hidden" />
								<input type="submit" name="button" value="Disable" class="smallButton"/>
							</form>
						
						</c:when>
						<c:otherwise>
							<font color="gray">Disabled</font><br/> 
	
							<form 	action="data-provider.do" method="POST" class="inlineForm"
									title="Enable access to '${set.name}'"
									onsubmit="return confirm('Are you sure you want to enable access to \'${f:jsEncode(set.name)}\'?')">
								<input name="currentIndex" value="${index}" type="hidden" />
								<input name="currentSetName" value="${set.name}" type="hidden" />
								<input name="currentSetSpec" value="${set.setSpec}" type="hidden" />
								<input name="enableSet" value="t" type="hidden" />
								<input name="setUid" value="${set.uniqueID}" type="hidden" />
								<input type="submit" name="button" value="Enable" class="smallButton"/>
							</form>
						</c:otherwise>
					</c:choose>
				  </td>
				  <td align="center" nowrap>
				  
					<form 	action="data-provider.do" method="POST" style="padding: 0px; margin: 0px"
							title="Reindex the files in this directory"
							onsubmit="return confirm('${set.numIndexed > 0 ? 'Reindex' : 'Index'} the files for \'${f:jsEncode(set.name)}\'?')">
						<input name="command" value="reindexCollection" type="hidden" />
						<input name="key" value="${set.setSpec}" type="hidden" />
						<input name="indexAll" value="false" type="hidden" />
						<input name="displayName" value="${set.name}" type="hidden" />
						<input name="button" value="Reindex" type="hidden" />
						<input type="submit" name="button" value="Reindex" class="smallButton" style="width:80px" />
					</form>	
					
					<form 	action="data-provider.do" method="POST" style="padding: 0px; margin: 3px"
							title="Remove this directory from the repository index"
							onsubmit="return confirm('Remove the records for \'${f:jsEncode(set.name)}\' from the repository index (the directory of files will be preserved)?')">
						<input name="command" value="removeSetBySetSpec" type="hidden" />
						<input name="setSpec" value="${set.setSpec}" type="hidden" />
						<input name="button" value="Remove" type="hidden" />
						<input type="submit" name="button" value="Remove" class="smallButton" style="width:80px" />
					</form>	

					<form 	action="metadata_dir-view.do" method="POST" style="padding: 0px; margin: 0px"
							title="Edit the settings for this directory">
						<input name="edit" value="${set.setSpec}" type="hidden" />
						<input name="dirPath" value="${set.directory}" type="hidden" />
						<input name="button" value="Edit settings" type="hidden" />
						<input type="submit" name="button" value="Edit settings" class="smallButton" style="width:80px" />
					</form>	
		
				  </td>		  
				 </tr>
				</logic:iterate>
			</c:otherwise>
		</c:choose>
		<c:set var="numDeletedDocsNotFromAnyDirectory" value="${rm.numDeletedDocsNotFromAnyDirectory}"/>
		<c:set var="totalDeleted" value="${totalDeleted + numDeletedDocsNotFromAnyDirectory}"/>
		<c:if test="${numDeletedDocsNotFromAnyDirectory > 0}">
			<tr id="headrow"> 
			  <td align="left" colspan="4" nowrap>
				<b>Deleted records not in any directory &nbsp;</b>
				<a href="javascript:popupHelp('<c:out value="${pageContext.request.contextPath}"/>','dm_numDeletedDocsNotFromAnyDirectory')" class="helpp"><img src="<c:url value='/images/help.gif'/>" border=0></a>
			  </td>
				<c:url var="searchLnk" value='query.do'>
					<c:param name="q" value=''/>
					<c:param name="title" value='Deleted records not in any directory'/>
					<c:param name="selSetSpecs" value=' -- All --'/>
					<c:param name="sfmts" value=' -- All --'/>
					<c:param name="selStatus" value='deletedNotInDir'/>
					<c:param name="s" value="0"/>
				</c:url>
			  <td align="center" nowrap>
			  	<a href='${searchLnk}' style="text-decoration:underline" title='Browse all deleted records not in any directory'><fmt:formatNumber type="number" value="${numDeletedDocsNotFromAnyDirectory}"/></a>
			  </td>
			  <td colspan="3" nowrap>
				&nbsp;		  		  
			  </td>  
			</tr>
		</c:if>

		<c:if test="${not empty raf.sets || numDeletedDocsNotFromAnyDirectory > 0}">
			<tr id="headrow">
			  <%-- <td align="left" colspan="1" nowrap>
				<c:set var="numDirs" value="${fn:length(raf.sets)}"/>
				${numDirs} ${numDirs == 1 ? 'Directory' : 'Directories'}
			  </td>	 --%>	
			  <td align="right" colspan="2" nowrap>
				Totals: &nbsp;
			  </td>
	
			  <td align="center" nowrap>
				<fmt:formatNumber type="number" value="${totalFiles}"/>		  
			  </td>
			  <td align="center" nowrap>
				<c:choose>
					<c:when test="${totalIndexed > 0}">
						<c:url var="searchLnk" value='query.do'>
							<c:param name="q" value=''/>
							<c:param name="title" value='All records, not deleted'/>
							<c:param name="selSetSpecs" value=' -- All --'/>
							<c:param name="sfmts" value=' -- All --'/>
							<c:param name="selStatus" value='false'/>
							<c:param name="s" value="0"/>
						</c:url>					
						<a href='${searchLnk}' style="text-decoration:underline" title='Browse all indexed records, not deleted'><fmt:formatNumber type="number" value="${totalIndexed}"/></a>
					</c:when>
					<c:otherwise>
						<fmt:formatNumber type="number" value="${totalIndexed}"/>
					</c:otherwise>
				</c:choose>
			  </td>
			  
			  <td align="center" nowrap>
				<c:choose>
					<c:when test="${totalDeleted > 0}">
						<c:url var="searchLnk" value='query.do'>
							<c:param name="q" value=''/>
							<c:param name="title" value='All deleted records'/>
							<c:param name="selSetSpecs" value=' -- All --'/>
							<c:param name="sfmts" value=' -- All --'/>
							<c:param name="selStatus" value='true'/>
							<c:param name="s" value="0"/>
						</c:url>					
						<a href='${searchLnk}' style="text-decoration:underline" title='Browse all deleted records'><fmt:formatNumber type="number" value="${totalDeleted}"/></a>
					</c:when>
					<c:otherwise>
						<fmt:formatNumber type="number" value="${totalDeleted}"/>
					</c:otherwise>
				</c:choose>		  
			  </td>		  
			  
			  <td align="center" nowrap>
				<c:choose>
					<c:when test="${totalErrors > 0}">
						<a href='report.do?q=error:true&s=0&report=Indexing+Errors' style="text-decoration:underline" title='Browse all errors'><fmt:formatNumber type="number" value="${totalErrors}"/></a>
					</c:when>
					<c:otherwise>
						<fmt:formatNumber type="number" value="${totalErrors}"/>
					</c:otherwise>
				</c:choose>			  		  
			  </td>		  
			  <td align="center" colspan="2">
				Index contains<br/> <fmt:formatNumber type="number" value="${totalIndexEntries}"/> entries		  
			  </td>
					  
			</tr>
		</c:if>
						
	  </table>
	 	<p>
  <form action="metadata_dir-view.do" method="get" style="margin-bottom:8px">
			<input type="submit" value="Add metadata directory"/>
			Add metadata files to the repository.
  </form>	
	</p>

	
	<div style="padding: 15px">&nbsp;</div>
	<hr height="1"/>
	
	<%-- Links to index admin --%>  
	<table align="top" cellpadding="0" cellspacing="0">	
		<tr>		
			<td>
			  <a name="index"></a><h3>Files index administration</h3>			
			</td>
		</tr>		
		<tr>
			<td>			  
				  <p>
				  The data provider maintains an index of all metadata files in the repository. 
				  Each file needs to be indexed before it is ready for harvesting or searching. 
				  An index is created automatically each time a new directory of files is 
				  <c:choose>
						<c:when test="${rm.updateFrequency > 0}">
							<c:set var="upDisp"><%@ include file="../../updateFrequency.jsp" %></c:set>
							configured. The index is <i>updated automatically every ${fn:trim(upDisp)}</i>* to reflect 
							changes whenever metadata files are added, modified or deleted.
							The index can also be updated manually at any time below.
						</c:when>
						<c:otherwise>
							configured, and can be updated manually at any time 
							using the buttons provided below to relect any files 
							you have added, modified or deleted.<sup>1</sup>
						</c:otherwise>
				   </c:choose>			  
				   </p>
				  
				  <table width="100%" cellpadding="0" cellspacing="8" border="0">
					<tr valign="top">
						<td>
							<form action="data-provider.do" 
								onsubmit="return confirm( 'This action will update the index for all files in the directories configured above. The process may take several minutes to complete. Continue?' )"
								method="post" style="margin-top:4px;"> 
								
							  <div align="left">
							    <input type="submit" name="command" value="Reindex all files"/>
								<input type="hidden" name="indexAll" value="false"/>
						      </div>
					  </form>					  
					  	</td>
						<td>
							Synchronize the index 
							to reflect files that have been added, modified or deleted. This process occurs
							in the background without disrupting access to the data provider.						
						</td>						
					</tr>
				</table>
				
		        <table width="100%" cellpadding="0" cellspacing="8" border="0">
                  <tr valign="top">
                    <td width="20%">
						<FORM>
						  <div align="left">
							<INPUT TYPE="BUTTON"  VALUE="Check most recent indexing status" ONCLICK="window.location.href='data-provider.do?command=showIndexingMessages'">
						  </div>
						</FORM>
					</td>
                    <td width="80%"> Obtain information about the progress of the indexing process.</td>
                  </tr>
                </table>	
				<logic:greaterThan name="raf" property="numIndexingErrors" value="0">
					<table width="100%" border="0" cellspacing="8">
					  <tr>
						<td> Note: Some records had errors during indexing. </td>
					  </tr>
					  <tr valign="top">
						<td><input name="BUTTON" type="BUTTON" onClick="window.location.href='report.do?q=error:true&s=0&report=Files+that+could+not+be+indexed+due+to+errors'"  value="See indexing errors">
						Display list of metadata files with their associated indexing errors. </td>
					  </tr>
					</table>
				</logic:greaterThan>				
				
				<ul id="sh" style="list-style-type: disc;">
					<li>
						<a href="javascript:toggleVisibility('aio');toggleVisibility('sh');">Show advanced index operations</a>
					</li>
				</ul>
				
				  <table id="aio" width="100%" cellpadding="0" cellspacing="8" border="0" style="background-color:#eeeeee; display:none">
					<tr valign="top">
						<td colspan="2">
							<b>Advanced index operations</b>
							&nbsp; <a href="javascript:toggleVisibility('aio');toggleVisibility('sh');">Hide</a>
						</td>
					</tr>					
				  	<tr valign="top">
						<td>
							<form action="data-provider.do" 
								onsubmit="return confirm( 'This action will instruct the indexer to stop at its current stage of progress. Continue?' )"
								method="post" style="margin-top:4px;">
								
							  <div align="left">
							    <input type="hidden" name="command" value="stopIndexing"/>
							    <input type="submit" value="Stop the indexer"/>
						      </div>
					  </form>					  
					  </td>
						<td>
							Stop the indexer at 
							its current stage of operation, if the indexer is currently running.
							Indexing will begin again automatically at the next scheduled update or may be started
							manually by clicking 'Reindex all files' above.						</td>
					</tr>
					<tr valign="top">
						<td>
							<form action="data-provider.do" 
								onsubmit="return confirm( 'Warning! This action will DELETE and rebuild the index. All items will become temporarily unavailable until they are reindexed. Continue?' )"
								method="post" style="margin-top:4px;">
								
							  <div align="left">
							    <input type="submit" value="Reset the index"/>
							    <input type="hidden" name="command" value="rebuildIndex"/>
						      </div>
					  </form>					  </td>
						<td>
							Delete and rebuild the 
							index from scratch. All items become unavailable until they are reindexed. 
							Deleted records will be removed and incremental harvests will be valid
							only for full harvests begun from this time forward. Perform this action only if 
							the index is corrupt or can not be updated properly using the 'Reindex all files' action.						</td>
					</tr>
				</table>

			</td>
		</tr>
	  <tr>
	    <td>&nbsp;</td>
      </tr>		
	  <tr>
	    <td>&nbsp;</td>
      </tr>
	  <tr>
	    <td>* To change the frequency by which the index is automatically synchronized with the files,
		edit the context parameter 'updateFrequency' in the servlet container configuration.
		See <a href='<c:url value="/docs/configuring_joai.jsp"/>#settings'>Configure software settings</a>.</td>
      </tr>
	  <tr>
	    <td>&nbsp;</td>
      </tr> 
	</table>	
	<hr height="1"/>	
	
	
	
<table height="100">
  <tr> 
    <td>&nbsp;</td>
  </tr>
</table>

<%-- Include style/menu templates --%>
<%@ include file="../../bottom.jsp" %>

<table height="100">
  <tr> 
    <td>&nbsp;</td>
  </tr>
</table>
</body>
</html:html>



