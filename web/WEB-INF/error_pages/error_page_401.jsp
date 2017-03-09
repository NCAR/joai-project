<%@ include file="../../TagLibIncludes.jsp" %>


<%-- The following is necessary to make BASIC authorization/401 work properly in Tomcat: --%>
<% 
	response.addHeader("WWW-Authenticate", "BASIC realm=\"DLESE\"");
	response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
%>

<html>
<head>
	<title>Unauthorized (401)</title>
	<c:import url="../../head.jsp"/>
</head>
<body>
<c:import url="../../top.jsp"/>

	<!-- CONTENT GOES HERE -->
	<h1>Unauthorized</h1>
	
	<p>You are not authorized to view this page.</p>

	<br/><br/><br/>

<c:import url="../../bottom.jsp"/>
</body>
</html>
