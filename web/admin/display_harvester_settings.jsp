<%@ page language="java" %>
<%-- <jsp:useBean id="haf" class="org.dlese.dpc.oai.harvester.action.form.HarvesterAdminForm" scope="request"/> --%>
<%@ taglib uri="/WEB-INF/tlds/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/tlds/struts-logic.tld" prefix="logic" %>
<!-- JSTL tags -->
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ page isELIgnored ="false" %>

<html:html>

<head>
<title>jOAI Harvester Setup and Status</title>

<!-- Step ONE : IMPORT "head.jsp" -->
<c:import url="../head.jsp"/>
</head>

<body>
<!-- STEP TWO: IMPORT "top.jsp" -->
<c:import url="../top.jsp?sec=harvester"/>

	<!-- CONTENT GOES HERE -->

	  <h1>Harvester Setup and Status</h1>
	  
	  <%-- ######## Display messages, if present ######## --%>
	<logic:messagesPresent> 		
	<table width="100%" bgcolor="#000000" cellspacing="1" cellpadding="8">
	  <tr bgcolor="ffffff"> 
		<td>
			<ul>
				<html:messages id="msg" property="message"> 
					<li><c:out value="${msg}" /></li>									
				</html:messages>
				<html:messages id="msg" property="error"> 
					<li><font color=red>Error: <c:out value="${msg}" /></font></li>									
				</html:messages>
				<html:messages id="msg" property="harvestErr"> 
					<li><c:out value="${msg}" /></li>										
				</html:messages>
				<html:messages id="msg" property="runHarvest"> 
					<li><c:out value="${msg}" /></li>
					<li>Click to <a href='harvestreport.do?q=uid:<c:out value="${haf.lastRunHarvest.uid}" />&s=0&report=History+and+status+of+harvests+for+<c:out value="${haf.lastRunHarvest.baseURL}" />'>view the harvest status report for this harvest</a></li>										
				</html:messages>
				<html:messages id="msg" property="runOneTimeHarvest"> 
					<li><c:out value="${msg}" /></li>
					<li>Click to <a href='harvestreport.do?q=uid:<c:out value="${haf.lastOneTimeHarvest.uid}" />&s=0&report=Most+recent+one-time+harvest'>view the harvest status report for this harvest</a></li>										
				</html:messages>
				<logic:messagesPresent property="validationError"> 
					<li><font color="red">Please fix the error(s) <a href="#oneTime">shown below</a> and click &quot;Harvest&quot; or 
					<a href="harvester.do">Cancel.</a></font></li>									
				</logic:messagesPresent>				
			</ul>
			
			&nbsp; &nbsp; [ <a href="harvester.do">OK</a> ]
			
		</td>
	  </tr>
	</table>		
	<br><br>
	</logic:messagesPresent>
	
	<%-- <logic:messagesPresent property="harvestErr">
		<html:messages id="msg" property="harvestErr"> 
			<font color="red">Harvest result: <c:out value="${msg}" /></font><br><br>										
		</html:messages>					
	</logic:messagesPresent> --%>
	

		
			

	<c:set var="defdir">
			<c:out value="${haf.defDir}" />
	</c:set>
		
		<h3>Setup</h3>
		Add a harvest to get metadata XML files from OAI data providers. If only <em>providing</em> metadata files, harvester set up is not necessary.</p>
		<table width="84%" border="0">
          <tr>
            <td width="14%">
			<html:form 	action="/admin/harvester"  
			method="GET">
			<html:submit property="button" value="Add new harvest" /> Create a harvest to get files and to specify when and where the harvest is performed.
			<html:hidden property="scheduledHarvest" value="add"/>
			</html:form></td>
         
          </tr>
  </table>
		
		
	<c:if test="${!empty haf.scheduledHarvests}">
		
		<h3>Status</h3>
	<table border="0">
		  <tr>
            <td><INPUT name="BUTTON" TYPE="BUTTON" ONCLICK="window.location.href='harvestreport.do?q=doctype:0harvestlog+AND+!repositoryname:%22One+time+harvest%22&s=0&report=Harvest+History+and+Progress'"  VALUE="View harvest history and progress"></td>
		    <td><div align="left">View a listing of all past harvests performed, current harvests in progress and their details. </div></td>
	      </tr>
        </table> <br>

		<%--	<p>Records from harvests will be saved in directories by baseURL, set and format inside the following directory:<br>
			<c:out value="${haf.shHarvestedDataDir}" />&nbsp;[&nbsp;<a href="harvester.do?editShHarvestedDataDir=edit">Edit</a>&nbsp;]									
			</p>--%>
			<table id="form" cellpadding="6" cellspacing="1" width="100%" border="0">

				<tr id="headrow">
			 	  <td width="34%"> <div align="center"><b>Harvest Repository</b></div></td>
				  <td width="14%"> <div align="center"><b>Metadata<br>
				    Format</b><a href="javascript:popupHelp('<c:out value="${pageContext.request.contextPath}"/>','disp_format')" class="helpp">   <img src='<c:out value="${pageContext.request.contextPath}" />/images/help.gif' border=0></a></div></td>
				  <td width="13%"> <div align="center"><b>SetSpec</b><a href="javascript:popupHelp('<c:out value="${pageContext.request.contextPath}"/>','disp_setspec')" class="helpp">   <img src='<c:out value="${pageContext.request.contextPath}" />/images/help.gif' border=0></a></div></td>
				  <td width="13%"> <div align="center"><b>Harvest Interval</b><a href="javascript:popupHelp('<c:out value="${pageContext.request.contextPath}"/>','disp_interval')" class="helpp">   <img src='<c:out value="${pageContext.request.contextPath}" />/images/help.gif' border=0></a></div></td>
					<td width="13%"> <div align="center"><b>Manually<br>Harvest</b><a href="javascript:popupHelp('<c:out value="${pageContext.request.contextPath}"/>','disp_manual')" class="helpp">   <img src='<c:out value="${pageContext.request.contextPath}" />/images/help.gif' border=0></a></div></td>
				  <td width="13%"> <div align="center"><b>Harvest Settings</b><a href="javascript:popupHelp('<c:out value="${pageContext.request.contextPath}"/>','disp_settings')" class="helpp">   <img src='<c:out value="${pageContext.request.contextPath}" />/images/help.gif' border=0></a></div></td>					
				</tr>
			
				<c:forEach var="scheduledHarvest" items="${haf.scheduledHarvests}">

				<c:set var="zipped">
					<c:out value="${pageContext.request.contextPath}"/>/${initParam.zippedHarvestsDirectory}/<c:out value="${scheduledHarvest.zipLatest}" />
				</c:set>
				
				<c:set var="emptyzipped">
					<c:out value="${pageContext.request.contextPath}"/>/${initParam.zippedHarvestsDirectory}/
				</c:set>
				
				
				<c:set var="one">
					<c:out value="${pageContext.request.contextPath}"/>/${initParam.zippedHarvestsDirectory}/<c:out value="${scheduledHarvest.backupOne}" />
				</c:set>
				<c:set var="two">
					<c:out value="${pageContext.request.contextPath}"/>/${initParam.zippedHarvestsDirectory}/<c:out value="${scheduledHarvest.backupTwo}" />
				</c:set>
				<c:set var="three">
					<c:out value="${pageContext.request.contextPath}"/>/${initParam.zippedHarvestsDirectory}/<c:out value="${scheduledHarvest.backupThree}" />
				</c:set>
				<c:set var="numH">
						<c:out value="${scheduledHarvest.numHarvestedLast}"/>
			    </c:set>
				  
				<c:set var="warn">
					<c:out value="${scheduledHarvest.warnR}" />
				</c:set>
				 
				  
				
				  	
			
								
				<tr id="formrow">
				  <td width="34%"><table width="100%" border="0" cellspacing="4">
                    <tr>
                      <td><b>
                        <c:out value="${scheduledHarvest.repositoryName}" />
                      </b></td>
                    </tr>
                    <tr>
                      <td><b>Base URL</b>:
                        <c:out value="${scheduledHarvest.baseURL}" />
                          <br>
                        <b>Harvested to</b>:
                        <c:out value="${scheduledHarvest.harvestDir}" />
                          <br>
                          <c:if test='${scheduledHarvest.lastHarvestTime != null}'> <b>Last harvest: </b>
                              <c:out value="${numH}" />
                            files,  
                            <c:out value="${scheduledHarvest.lastHarvestTime}" />
                            <br>
                          </c:if>
                          <c:if test='${scheduledHarvest.isZipPresent == "true"}'>
						  	<c:if test="${zipped != emptyzipped}"><b>Download zipped harvest</b>:
                            <c:choose>
                                <c:when test='${warn}'> 
									<a href="javascript:more('<c:out value="${zipped}"/>', '<c:out value="${scheduledHarvest.harvestDir}" />')">Most recent </a></c:when>
                                <c:otherwise> <a href="${fn:replace(zipped,'\\','/')}">Most recent</a> </c:otherwise>
                            </c:choose>
                              <c:choose>
                                <c:when test='${scheduledHarvest.backupTwo == ""}'> </c:when>
                                <c:otherwise> |
                                  <c:choose>
                                      <c:when test='${warn}'> <a href="javascript:more('<c:out value="${two}"/>', '
                                  <c:out value="${scheduledHarvest.harvestDir}" />
                                  ')">Older </a></c:when>
                                      <c:otherwise> <a href="${fn:replace(two,'\\','/')}">Older</a> </c:otherwise>
                                  </c:choose>
                                    <c:choose>
                                      <c:when test='${scheduledHarvest.backupThree == ""}'> </c:when>
                                      <c:otherwise> |
                                        <c:choose>
                                            <c:when test='${warn}'> <a href="javascript:more('<c:out value="${three}"/>', '
                                  <c:out value="${scheduledHarvest.harvestDir}" />
                                  ')">Oldest </a> </c:when>
                                            <c:otherwise> <a href="${fn:replace(three,'\\','/')}">Oldest</a> </c:otherwise>
                                        </c:choose>
                                      </c:otherwise>
                                    </c:choose>
                                
                                </c:otherwise>
                              </c:choose>
                              <br>
                          </c:if>
			  </c:if>
                        <a href='harvestreport.do?q=uid:<c:out value="${scheduledHarvest.uid}" />&s=0&report=Harvest+History+and+Progress+for+<c:out value="${scheduledHarvest.baseURL}" />'>View harvest history and progress</a></td>
                    </tr>
                  </table></td>
				  <td width="14%">
				<div align="center"> <c:out value="${scheduledHarvest.metadataPrefix}" /></div>				 </td>
				 <td>
				 <c:choose>
				   <c:when test='${scheduledHarvest.setSpecHtml != "&nbsp;"}'>
					      <div align="center">   <c:out value="${scheduledHarvest.setSpecHtml}" escapeXml="false"/> </div>
				   </c:when>
				   <c:otherwise>
				   <div align="center"> -</div>
				   </c:otherwise>
				   </c:choose>				 </td>
				 
				 <td>
				 <c:choose>
				   <c:when test='${scheduledHarvest.enabledDisabled != "disabled"}'>
					    <div align="center">Automatic<br>
							<c:choose>
								<c:when test="${not empty scheduledHarvest.runAtTime}">
									(Every ${scheduledHarvest.harvestingInterval == 1 ? '' : scheduledHarvest.harvestingInterval} ${scheduledHarvest.intervalGranularityLabel}
									at 
									<nobr>${scheduledHarvest.runAtTimeDisplay})</nobr>
								</c:when>
								<c:otherwise>
									(Every ${scheduledHarvest.harvestingInterval} ${scheduledHarvest.intervalGranularityLabel})
								</c:otherwise>
							</c:choose>
						  </div>
				          </br>
		           </c:when>
					<c:otherwise>
					<div align="center">Manual</div> 
					</c:otherwise>
				</c:choose>				 </td>
				 
				 
				 
				 <td width="13%">
				 
					<div align="center">
						<nobr>
							<form 	action="harvester.do" method="POST" class="inlineForm"
									title="Harvest records that are new since the previous harvest"
									onsubmit="return confirm('Harvest \'<c:out value="${scheduledHarvest.repositoryNameEscaped}" />\' now and request only those files that have changed since the last harvest?')">
								<input type="hidden" name="scheduledHarvest" value="runHarvest"/>
								<input type="hidden" name="mySes" value="<c:out value="${haf.mySes}" />"/>
								<input type="hidden" name="shUid" value='<c:out value="${scheduledHarvest.uid}"/>'/>
								<input type="submit" name="button" value="New" class="smallButton" style="width:45px" />
							</form>
							<form 	action="harvester.do" method="POST" class="inlineForm"
									title="Harvest all records"
									onsubmit="return confirm('Harvest all files from \'<c:out value="${scheduledHarvest.repositoryNameEscaped}" />\' and replace any existing files that were previously harvested?')">
								<input type="submit" name="button" value=" All " class="smallButton" style="width:45px" />
								<input type="hidden" name="doAll" value="t"/>
								<input type="hidden" name="scheduledHarvest" value="runHarvest"/>
								<input type="hidden" name="mySes" value="<c:out value="${haf.mySes}" />"/>
								<input type="hidden" name="shUid" value='<c:out value="${scheduledHarvest.uid}"/>'/>
							</form>
						</nobr>
						
						<br>
					 </div>				 
				 </td>
				 
				 <td width="13%">
					 <div align="center">
					 	<nobr>
							<form 	action="harvester.do" method="POST" class="inlineForm"
									title="Edit these settings">
								<input type="hidden" name="scheduledHarvest" value="edit"/>
								<input type="hidden" name="shUid" value='<c:out value="${scheduledHarvest.uid}"/>'/>
								<input type="submit" name="button" value="Edit" class="smallButton" style="width:45px"/>
							</form>
							<form 	action="harvester.do" method="POST" class="inlineForm"
									title="Delete this scheduled harvest"
									onsubmit="return confirm('Are you sure you want to delete the harvest for \'<c:out value="${scheduledHarvest.repositoryNameEscaped}" />\'?')">
								<input type="submit" name="button" value="Delete" class="smallButton" style="width:45px"/>
								<input type="hidden" name="scheduledHarvest" value="delete"/>
								<input type="hidden" name="shUid" value='<c:out value="${scheduledHarvest.uid}"/>'/>
							</form>
						</nobr>
					  </div>				 
				</td>
			    </tr>
			</c:forEach>
		  </table> 
			<br>
</c:if>
		
		<c:if test="${!empty haf.scheduledHarvests}">
		<html:form 	action="/admin/harvester"  
			method="GET">
			<html:submit property="button" value="Add new harvest" />
			<html:hidden property="scheduledHarvest" value="add"/>
		</html:form>
		<INPUT name="BUTTON" TYPE="BUTTON" ONCLICK="window.location.href='harvestreport.do?q=doctype:0harvestlog+AND+!repositoryname:%22One+time+harvest%22&s=0&report=Harvest+History+and+Progress'"  VALUE="View harvest history and progress">
		</c:if>

<!-- END OF YOUR CONTENT -->

<!-- STEP THREE : IMPORT "bottom.jsp" -->
<c:import url="../bottom.jsp"/>

</body>
</html:html>

