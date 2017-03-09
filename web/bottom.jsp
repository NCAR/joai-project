<%@ include file="/TagLibIncludes.jsp" %>
</td>
  </tr>
</table>

</td></tr></table>
  <c:set var="ucarToolTip" value="University Corporation for Atmospheric Research (UCAR)"/>  
  <c:set var="nsfToolTip" value="National Science Foundation (NSF)"/>  
  <c:set var="dleseToolTip" value="Digital Library for Earth System Education (DLESE)"/>  
  
  
  <div align="center">

  
   	<img src='${pageContext.request.contextPath}/images/joaifooter.gif' name="Footer" border="0" usemap="#footerMap">
    <map name="footerMap" id="footerMap">
      <area shape="rect" coords="130,1,220,42" href="http://www.ucar.edu" target="_blank" alt="${ucarToolTip}" title="${ucarToolTip}" />
      <area shape="rect" coords="219,1,268,46" href="http://www.nsf.gov/" target="_blank" alt="${nsfToolTip}" title="${nsfToolTip}" />
      <area shape="rect" coords="267,-1,388,42" href="http://www.dlese.org" target="_blank" alt="${dleseToolTip}" title="${dleseToolTip}" />
    </map>
</div>
<br/>

<%-- Import content at the bottom, if configured --%>
<c:if test="${not empty initParam.pageBottomImport}">
	<c:catch>${f:timedImport(initParam.pageBottomImport,1500)}</c:catch>
</c:if>

