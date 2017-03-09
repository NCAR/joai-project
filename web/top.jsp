<%@ include file="TagLibIncludes.jsp" %>
<a name="top"></a>


	  			   
<c:choose>
<c:when test="${param.sec=='harvester'}">

			<c:set var="imagePath">
					<c:out value="${pageContext.request.contextPath}"/>/images/harvester.jpg
			</c:set>
</c:when>
<c:otherwise>
	<c:choose>
	<c:when test="${param.sec=='provider'}">
           	<c:set var="imagePath">
					<c:out value="${pageContext.request.contextPath}"/>/images/provider.jpg
			</c:set>
	</c:when>
	<c:otherwise>
		<c:choose>
		<c:when test="${param.sec=='doc'}">
				<c:set var="imagePath">
					<c:out value="${pageContext.request.contextPath}"/>/images/doc.jpg
			</c:set>

		</c:when>
		<c:otherwise>
			<c:choose>
			<c:when test="${param.sec=='overview'}">
			<c:set var="imagePath">
					<c:out value="${pageContext.request.contextPath}"/>/images/overview.jpg
			</c:set>
			</c:when>
			<c:otherwise>
				<c:set var="imagePath">
					<c:out value="${pageContext.request.contextPath}"/>/images/bannerBlank.jpg
				</c:set>
			</div>
			</c:otherwise>
			</c:choose>
		</c:otherwise>
		</c:choose>
	</c:otherwise>
	</c:choose>
</c:otherwise>
</c:choose>
<map name="topMap" id="topMap">
          <area shape="rect" coords="3,1,136,91" href='<c:out value="${pageContext.request.contextPath}"/>'/>
</map>

   <table cellpadding="0" cellspacing="0" width="100%" border="0"><tr><td width="800"><img src='<c:out value="${imagePath}"/>' alt=" " width="800" height="74" border="0" usemap="#topMap" /></td>
   <td class="backgroundLine" align="right"><div class="hiddentext">.</div></td></tr></table>
<table width="100%" border="0" cellpadding="0" cellspacing="0"><tr><td id="menu"><script type="text/javascript" language="JavaScript">showMenus(1,'Horizontal')</script></td></tr>
<tr>
  <td><table border="0" cellpadding="3" align="center" width="95%">
      <tr>
        <td><br>
