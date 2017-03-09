<%@ include file="../TagLibIncludes.jsp" %>

<c:set var="rm" value="${applicationScope.repositoryManager}"/>



<html>

<style type="text/css">

<!--

	.errorText {color: #FF0033}

-->

</style>



<head>

<c:set var="title">

Data Provider Setup and Status</c:set>



<title>${title}</title>



<c:import url="../head.jsp"/>



</head>



<body>

<c:import url="../top.jsp?sec=provider"/>

<!-- content begins-->



<h1>${title}</h1>



<h3>Setup </h3>

<p>To make metadata files available, complete the following steps: </p>

<ol>

<li>Enter <a href="<c:url value='/admin/data-provider-info.do'/>">Repository Information</a> (<em>required</em>) </li>

<li>Add <a href="<c:url value='/admin/data-provider.do'/>">Metadata Files</a> (<em>required</em>) </li>

<li>Complete <a href="<c:url value='/admin/data-provider/sets.do'/>">Sets Configuration</a> (<em>optional</em>) </li>

</ol>





<h3 style="padding-top:6px">Status</h3>



<table id="form" cellpadding="6" cellspacing="1" border="0">

  <tr>

    <td valign="top" id="headrow">Repository information</td>

    <td valign="top" id="formrow">

		<c:choose>

			<c:when test="${empty rm.repositoryName || empty rm.adminEmails}">

				<span class="errorText">Incomplete. Supply a repository name and e-mail.</span> 

				<a href="<c:url value='/admin/dp_metadata/repository_info_form.jsp'/>">Edit repository information</a>.

			</c:when>

			<c:otherwise>

				Completed.  <a href="<c:url value='/admin/update_repository_info.do'/>">View or edit repository information</a>.

			</c:otherwise>

		</c:choose>

	</td>

  </tr>

  

  <c:set var="numDeletedDocsNotFromAnyDirectory" value="${rm.numDeletedDocsNotFromAnyDirectory}"/>



  <tr>

    <td valign="top" id="headrow">Metadata files configuration </td>

    <td valign="top" id="formrow">

		<c:choose>

			<c:when test="${ (rm.setInfos == null || fn:length(rm.setInfos) == 0) && numDeletedDocsNotFromAnyDirectory == 0}">

				<span class="errorText">Incomplete. Configure one or more metadata file directories.</span>

				<a href="<c:url value='/admin/metadata_dir-view.do'/>">Add metadata directory</a> to the data provider.

			</c:when>

			<c:otherwise>

				Completed.  

				<a href="<c:url value='/admin/data-provider.do'/>">View or edit metadata files configuration</a>. 

				<br/><nobr>Number of metadata directories: ${fn:length(rm.setInfos)}</nobr>

			</c:otherwise>

		</c:choose>

	</td>

  </tr>

  

   <tr>

    <td valign="top" id="headrow">File index information</td>

	<c:set var="numIndexingErrors" value="${rm.numIndexingErrors}"/>

    <td valign="top" id="formrow" nowrap>

		<div>

			<c:choose>

				<c:when test="${numIndexingErrors > 0}">	

					<span class="errorText">Warning - errors were found in

					<a title="View error messages" href="<c:url value='/admin/report.do'/>?q=error:true&s=0&report=Files+That+Could+Not+be+Indexed+Due+to+Errors"><fmt:formatNumber 

						type="number" value="${numIndexingErrors}" /> file${numIndexingErrors == 1 ? '':'s'}</a>.</span>

					</br>

				</c:when>

				<c:otherwise>

					No indexing errors. 

				</c:otherwise>

			</c:choose>

			<a href="<c:url value='/admin/data-provider.do#index'/>">View indexing options</a>.		

		</div>	

		<div>Total files indexed: <fmt:formatNumber type="number" value="${rm.numNonDeletedDocs}" /></div>

		<c:if test="${numDeletedDocsNotFromAnyDirectory > 0}">

			<div>Total files deleted: <fmt:formatNumber type="number" value="${numDeletedDocsNotFromAnyDirectory}" /></div>

		</c:if>

	</td>

  </tr>

  

	<%-- Determine number of sets: --%>

	<c:catch>

		<c:if test="${not empty rm.listSetsConfigXml}">

			<x:parse var="listSetsConfXml" xml="${rm.listSetsConfigXml}"/>

		</c:if>		

	</c:catch>  

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

	

  <tr>

    <td valign="top" id="headrow">Sets configuration </td>

    <td valign="top" id="formrow">

		<c:choose>

			<c:when test="${numSets == 0}">	

				No sets are defined for the repository. <a href="<c:url value='/admin/set_definition-view.do'/>">Define a set</a> (optional).

			</c:when>

			<c:otherwise>

				Completed. 

				<a href="<c:url value='/admin/data-provider/sets.do'/>">View or edit sets</a>. 

				<br/><nobr>Number of sets defined: ${numSets}</nobr>

			</c:otherwise>

		</c:choose>

	</td>

  </tr>



  <tr>

    <td valign="top" id="headrow">Data provider access</td>

    <td valign="top" id="formrow">

		${rm.providerStatus == 'ENABLED' ? 'Enabled' : 'Disabled'}.

		<a href="<c:url value='/admin/data-provider-info.do'/>#access">Change access status</a>.

	</td>

  </tr>

  

  <tr>

    <td valign="top" id="headrow">Reports </td>

    <td valign="top" id="formrow">

		<a href="<c:url value='/admin/report.do'/>?q=!%22rt%20text%22+AND+!%22rt%20validate%22+AND+!requesturlt%3Adleseodlsearch+AND+requesturlt%3AListRecords&s=0&searchOver=webLogs&report=List+of+Who+Has+Harvested+This+Data+Provider+(ListRecords+Requests)">See list of who has harvested this data provider</a>.



	</td>

  </tr>

</table>





<table height="100">

  <tr> 

    <td>&nbsp;</td>

  </tr>

</table>



<c:import url="../bottom.jsp"/>

</body>

</html>

