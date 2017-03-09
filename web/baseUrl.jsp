<c:choose><%-- Implements the same functionality as repositoryManager.getProviderBaseUrl(request)  --%>
	<c:when test="${empty initParam.serverUrl || initParam.serverUrl == '[determine-from-client]'}">
		<c:set var="myBaseUrl">${f:contextUrl(pageContext.request)}/${initParam.dataProviderBaseUrlPathEnding}</c:set>
	</c:when>
	<c:otherwise>
		<c:set var="myBaseUrl">${initParam.serverUrl}${pageContext.request.contextPath}/${initParam.dataProviderBaseUrlPathEnding}</c:set>
	</c:otherwise>
</c:choose>


