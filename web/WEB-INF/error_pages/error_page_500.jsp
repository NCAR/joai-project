<%-- Source the necessary tag libraries --%>
<%@ page language="java" isErrorPage="true" %>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="f" uri="http://www.dlese.org/dpc/dds/tags/dleseELFunctions" %>

<html>
<head>
	<title>Internal server error (500)</title>
<c:import url="../../head.jsp"/>
	
	<%-- Output Java exception details, if available --%>
	<% if (exception != null) { %>
		<!-- Server debugging information:  
		<% exception.printStackTrace(new java.io.PrintWriter(out)); %> -->
	<% } else { %>
		<!--  No stack trace available. -->
	<% } %>	
</head>
<body>
<c:import url="../../top.jsp"/>

<h1>Internal server error</h1>

<p>We're sorry, the server encountered a problem and could not complete your request.</p>

<br/><br/><br/>

<c:import url="../../bottom.jsp"/>

</body>
</html>

