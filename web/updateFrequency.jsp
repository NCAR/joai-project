<%-- Outputs the index update frequency in a user-readable time. 
Assumes variable rm contains the RepositoryManager --%>
<c:choose>
	<c:when test="${rm.updateFrequency > 60}">
		<fmt:formatNumber value="${rm.updateFrequency/60}" pattern="#"/> 
		hour${rm.updateFrequency/60 >= 2 ? 's' : ''}
		<c:if test="${rm.updateFrequency%60 != 0}">
			and 
			<fmt:formatNumber value="${rm.updateFrequency%60}" pattern="#"/>
			minute${rm.updateFrequency%60 >= 2 ? 's' : ''}
		</c:if>
	</c:when>
	<c:otherwise>
		${rm.updateFrequency} minute${rm.updateFrequency == 1 ? '' : 's'}
	</c:otherwise>
</c:choose>
