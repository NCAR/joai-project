<%@ include file="TagLibIncludes.jsp" %>
<script language="JavaScript"> 
<c:set var="menNum" value="0"/>
var Menu1 = new Array ()
var subMenu1 = new Array ()

	Menu1[${menNum}] = new Array("Overview", '<c:url value="/"/>',"_top", "left")
	 subMenu1[${menNum}] = new Array()
	<c:set var="menNum" value="${menNum + 1}"/>
	 
	Menu1[${menNum}] = new Array ("Search", '<c:url value="/search.jsp"/>',"_top", "left")
	 subMenu1[${menNum}] = new Array()
	<c:set var="menNum" value="${menNum + 1}"/>
	
	Menu1[${menNum}] = new Array ("Explore", '<c:url value="/oaisearch.do"/>',"_top", "left")
	 subMenu1[${menNum}] = new Array()
	<c:set var="menNum" value="${menNum + 1}"/> 
	
	<c:if test="${initParam.hideAdminMenus != 'true'}">
		Menu1[${menNum}] = new Array("Data Provider", '<c:url value="/admin/provider_status_setup.jsp"/>',"_top", "left")
		 subMenu1[${menNum}] = new Array()
		 subMenu1[${menNum}][0] = new Array ("Setup and Status", '<c:url value="/admin/provider_status_setup.jsp"/>',"_top")
		 subMenu1[${menNum}][1] = new Array ("Repository Information and Administration", '<c:url value="/admin/data-provider-info.do"/>',"_top")	 
		 subMenu1[${menNum}][2] = new Array ("Metadata Files Configuration", '<c:url value="/admin/data-provider.do"/>',"_top")
		 subMenu1[${menNum}][3] = new Array ("Sets Configuration", '<c:url value="/admin/data-provider/sets.do"/>',"_top")
		 subMenu1[${menNum}][4] = new Array ("Admin search", '<c:url value="/admin/query.do"/>',"_top")	 
		 //subMenu1[${menNum}][5] = new Array ("ODL Search Explorer", '<c:url value="/oaisearch.do?show=odl"/>',"_top")
		 //subMenu1[${menNum}][6] = new Array ("ODL Search Client", '<c:url value="/odl_search_client.jsp"/>',"_top")
		 <c:set var="menNum" value="${menNum + 1}"/>
		 
	   Menu1[${menNum}] = new Array("Harvester", '<c:url value="/admin/harvester.do"/>',"_top", "left")
		 subMenu1[${menNum}] = new Array()
		 subMenu1[${menNum}][0] = new Array ("Setup and Status", '<c:url value="/admin/harvester.do"/>',"_top")
		 subMenu1[${menNum}][1] = new Array ("Harvest History and Progress", '<c:url value="/admin/harvestreport.do"/>?q=doctype:0harvestlog+AND+!repositoryname:%22One+time+harvest%22&s=0&report=Harvest+History+and+Progress', "_top")
	   <c:set var="menNum" value="${menNum + 1}"/>
   </c:if>
  
   Menu1[${menNum}] = new Array("Documentation", '<c:url value="/docs/"/>',"_top", "left")
     subMenu1[${menNum}] = new Array()
     subMenu1[${menNum}][0] = new Array ("Data Provider Documentation", '<c:url value="/docs/provider.jsp"/>',"_top")
     subMenu1[${menNum}][1] = new Array ("Harvester Documentation", '<c:url value="/docs/harvester.jsp"/>',"_top")
     subMenu1[${menNum}][2] = new Array ("Frequently Asked Questions (FAQ)", '<c:url value="/docs/faq.jsp"/>',"_top")	 
     subMenu1[${menNum}][3] = new Array ("ODL Search Specification", '<c:url value="/docs/odlsearch.do"/>',"_top")
     subMenu1[${menNum}][4] = new Array ("Installing jOAI", '<c:url value="/docs/INSTALL.md"/>',"_top")
     subMenu1[${menNum}][5] = new Array ("Configuring jOAI", '<c:url value="/docs/configuring_joai.jsp"/>',"_top")
     subMenu1[${menNum}][6] = new Array ("About jOAI", '<c:url value="/docs/about.jsp"/>',"_top")
   <c:set var="menNum" value="${menNum + 1}"/>  
     indicator = "<img src='${pageContext.request.contextPath}/images/tridown.gif' border='0'>" // Symbol to show if a sub menu is present (subIndicate must be to set to 1)
                                                    // Use standard HTML <img> tag. You can use a character instead of an image. 
                                                    // e.g.      indicator = ">"
</script>
<script type="text/javascript" src='${pageContext.request.contextPath}/browser.js'></script>
<script type="text/javascript" src='${pageContext.request.contextPath}/menu.js'></script>
<script type="text/javascript" src='${pageContext.request.contextPath}/oai_script.js'></script>

<link rel="stylesheet" type="text/css" href='${pageContext.request.contextPath}/oai_styles.css'>

