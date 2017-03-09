<%@ include file="TagLibIncludes.jsp" %>
<html>

<head>
<title>Search the Data Provider</title>

<META http-equiv="Content-Type" content="text/html; charset=UTF-8">

<c:import url="head.jsp"/>

</head>

<body>
<c:import url="top.jsp?sec=provider"/>
<!-- content begins-->

<h1>Search the Data Provider</h1>

<p>
Search metadata file content in the data provider by entering keywords and/or
selecting a set. This page searches over the full text of all files
that can be dissiminated by the data provider in the Dublin Core (oai_dc) format.
</p><p><p>
<%-- Set the veriable '${myBaseUrl}' used in the ODL search client --%>
<%@ include file="baseUrl.jsp" %>
<%@ include file="odl_search_client.jsp" %>

<table height="100">
  <tr> 
    <td>&nbsp;</td>
  </tr>
</table>

<c:import url="bottom.jsp"/>
</body>
</html>
