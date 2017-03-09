<%-- <jsp:useBean id="haf" class="org.dlese.dpc.oai.harvester.action.form.HarvesterAdminForm" scope="request"/> --%>
<%@ page language="java" %>
<%@ taglib uri="/WEB-INF/tlds/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/tlds/struts-logic.tld" prefix="logic" %>

<!-- JSTL tags -->
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="x" uri="http://java.sun.com/jsp/jstl/xml" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="sql" uri="http://java.sun.com/jsp/jstl/sql" %>
<%@ taglib prefix="f" uri="http://www.dlese.org/dpc/dds/tags/dleseELFunctions" %>
<%@ page isELIgnored ="false" %>
<%@ page isELIgnored ="false" %>

<html:html>
<head>
<title>Edit harvester settings</title>
<c:import url="../head.jsp"/>
<script>
	// Override the method below to set the focus to the appropriate field.
	function sf(){}	
	
</script>
</head>

<body id="addEditHPage" onLoad="checkSets('<c:out value="${haf.commonDirs[0]}"/>', '<c:out value="${haf.commonDirs[1]}"/>', '<c:out value="${haf.commonDirs[2]}"/>', '<c:out value="${haf.commonDirs[3]}"/>', '<c:out value="${haf.commonDirs[4]}"/>' )">
<c:import url="../top.jsp?sec=harvester"/>

 
	<html:form action="/admin/harvester" method="POST">
	
	
	<c:choose>
	<c:when test="${param.scheduledHarvest == 'add'}">
		<h1>Add a New Harvest</h1>
	</c:when>
	<c:otherwise>
		<h1>Edit Harvest</h1>
	</c:otherwise>
	</c:choose>

	<%-- ######## Display messages, if present ######## --%>
	<logic:messagesPresent> 		
		<font color="red">Please fix the error(s) shown below and click &quot;Save,&quot; or choose &quot;Cancel.&quot;</font>		
		<br><br>
	</logic:messagesPresent>
	

				  

		<%-- ######## Directory where files are saved ######## --%>
		<logic:present parameter="editHarvestedDataDir">
		<table id="form" cellpadding=10 cellspacing="1">
			<script>function sf(){document.haf.harvestedDataDir.focus();}</script>
			<input type="hidden" name="editHarvestedDataDir" value="save">
			<tr id="editrow">	
				<td nowrap>
					<b>Directory where harvested metadata will be saved</b>:<br>
					<html:text property="harvestedDataDir" size="75"/>
					<logic:messagesPresent property="harvestedDataDir">
						<html:messages id="msg" property="harvestedDataDir"> 
							<br><font color="red">*<c:out value="${msg}"/></font>										
						</html:messages>					
					</logic:messagesPresent>					
				</td>
			</tr>
		</table>
		<table>
			<tr>						
				<td colspan=2>
					<br><b>Information:</b><br>
					Enter a valid directory path on the server where this harvester is installed. 
					All metadata files will be saved into individual sub-directories within this
					directory.
					<br>
				</td>
			</tr>
		</table>
		</logic:present>

		<%-- ######## Directory where scheduled harvest files are saved ######## --%>
		<logic:present parameter="editShHarvestedDataDir">
		<table id="form" cellpadding=10 cellspacing="1">
			<script>function sf(){document.haf.shHarvestedDataDir.focus();}</script>
			<input type="hidden" name="editShHarvestedDataDir" value="save">
			<tr id="editrow">	
				<td nowrap>
					<b>Directory where the metadata files from scheduled harvests will be saved</b>:<br>
					<html:text property="shHarvestedDataDir" size="75"/>
					<logic:messagesPresent property="shHarvestedDataDir">
						<html:messages id="msg" property="shHarvestedDataDir"> 
							<br><font color="red">*<c:out value="${msg}"/></font>										
						</html:messages>					
					</logic:messagesPresent>					
				</td>
			</tr>
		</table>
		<table>
			<tr>						
				<td colspan=2>
					<br><b>Information:</b><br>
					Enter a valid directory path on the server where this harvester is installed. 
					All metadata files from scheduled harvests will be saved into individual sub-directories within this
					directory.
					<br>
				</td>
			</tr>
		</table>
		</logic:present>
		
		

		
		
		<%-- ######## Add/Edit a seheduled harvest ######## --%>
		<logic:present parameter="scheduledHarvest">
		<p>Complete all fields with the details of the harvest being added. If only <i>providing</i> metadata files, harvester set up is not necessary.
</p>
<table>
<tr><td>
		<INPUT name="BUTTON" TYPE="BUTTON" ONCLICK="window.location.href='<c:url value='/docs/harvester.jsp'/>'"  VALUE="View harvester documentation">
		</td>
		<td>Additional information on setting up the harvester is available in the harvester documentation.</td>
		</tr>
		</table>
		<p>
			<table id="form" cellpadding="6" cellspacing="1">

			<script>function sf(){document.haf.shRepositoryName.focus();}</script>
			<input type="hidden" name="shUid" value='<c:out value="${haf.shUid}"/>'>
			<input type="hidden" name="scheduledHarvest" value="save">
			<tr id="formrow">	
				<td id="editrow" nowrap><b>Repository name</b><a href="javascript:popupHelp('<c:out value="${pageContext.request.contextPath}"/>', 'name')" class="helpp">   <img src='<c:out value="${pageContext.request.contextPath}" />/images/help.gif' border=0></a>
				<br><html:text property="shRepositoryName" size="75"/></td>
			</tr>
			<tr id="formrow">
				<td nowrap id="editrow"><b>Repository base URL</b><a href="javascript:popupHelp('<c:out value="${pageContext.request.contextPath}"/>','url')"class="helpp">   <img src='<c:out value="${pageContext.request.contextPath}" />/images/help.gif' border=0></a>
				<br>
					<logic:messagesPresent property="shBaseURL">
						<html:messages id="msg" property="shBaseURL"> 
							<font color="red">*<c:out value="${msg}"/></font><br>										
						</html:messages>					
					</logic:messagesPresent>						
					<html:text property="shBaseURL" size="75"/>		
				</td>
			</tr>
			<tr id="formrow">
				<td nowrap id="editrow"><b>SetSpec (optional)</b><a href="javascript:popupHelp('<c:out value="${pageContext.request.contextPath}"/>','set')" class="helpp">   <img src='<c:out value="${pageContext.request.contextPath}" />/images/help.gif' border=0></a><br>
				<html:text property="shSetSpec" size="75" styleId="setName" onblur="checkText()" onkeyup="checkText()"/></td>
			</tr>
			<tr id="formrow">
				<td nowrap id="editrow"><b>Metadata format being harvested</b><a href="javascript:popupHelp('<c:out value="${pageContext.request.contextPath}"/>','metadata')" class="helpp">   <img src='<c:out value="${pageContext.request.contextPath}" />/images/help.gif' border=0></a><br>
				
					<logic:messagesPresent property="shMetadataPrefix">
						<html:messages id="msg" property="shMetadataPrefix"> 
							<font color="red">*<c:out value="${msg}"/></font><br>										
						</html:messages>					
					</logic:messagesPresent>					
					<html:text property="shMetadataPrefix" size="75"/>
				</td>
			</tr>  
		 
			<tr id="formrow">		
				<td id="editrow" nowrap><html:checkbox property="shEnabledDisabled" value="enabled" styleId="reg" onclick="checkReg()"><b>Harvest automatically at regular intervals</b> ...<a href="javascript:popupHelp('<c:out value="${pageContext.request.contextPath}"/>','regular')" class="helpp">   <img src='<c:out value="${pageContext.request.contextPath}" />/images/help.gif' border=0></a></html:checkbox>
					<br>	
					<div id="regularYes" nowrap>
						<nobr>
						<logic:messagesPresent property="shHarvestingInterval">
							<html:messages id="msg" property="shHarvestingInterval"> 
								<div style="color:red">*<c:out value="${msg}"/></div>										
							</html:messages>					
						</logic:messagesPresent>
						<logic:messagesPresent property="shRunAtTime">
							<html:messages id="msg" property="shRunAtTime"> 
								<div style="color:red">*<c:out value="${msg}"/>.</div>										
							</html:messages>					
						</logic:messagesPresent>							
						<table cellspacing="4" cellpadding="0">
							<tr>
								<td>
									every
								</td>
								<td>
									<html:text property="shHarvestingInterval" size="3" styleId="interval"/>
								</td>
								<td>
									<html:select property="shIntervalGranularity" size="1" styleId="gran" onchange="checkReg()">
										<html:options  name="haf" property="shIntervalGranularityList" 
											labelProperty="shIntervalGranularityListLabels"/>
									</html:select>
								</td>
								<td>
									<span id="runAtTime">
										beginning at time <html:text property="shRunAtTime" size="3" styleId="runAtTimeTB"/>
										
										<span style="font-size:8pt; color:#444444;">(examples: 12:45 or 23:15)</span>
									</span>
								</td>
							</tr>
						</table>
						</nobr>
					</div>
								
				</td>
			</tr>
	
			<c:set var="defdir">
				<c:out value="${haf.defDir}" />
			</c:set>
				
			
			
				<tr id="formrow">	
				<td nowrap id="editrow"><b>Save files from this harvest </b>: <a href="javascript:checkDef('<c:out value='${defdir}' />')" class="helpp">   <img src='<c:out value="${pageContext.request.contextPath}" />/images/help.gif' border=0></a>
				<br>
					<div id="customDir">
					<html:radio property="shDir" value="default" onclick="remove()" >at the default harvest location</html:radio> <br>
					<html:radio property="shDir" value="custom" styleId="radioCustom" onclick="change()">at a location I specify ... </html:radio> 
					<div id="hiddenDir">
					<logic:messagesPresent property="shHarvestDir">
						<html:messages id="msg" property="shHarvestDir"> 
							<font color="red">*<c:out value="${msg}"/></font><br>										
						</html:messages>					
					</logic:messagesPresent>
					<html:text property="shHarvestDir" styleId="cD" size="50"/><br>
					<html:checkbox property="allowDupDir" value="allow" styleId="dupDir">Allow me to choose a location already in use</html:checkbox><br>
					<SELECT NAME="s" onchange="document.haf.cD.value = document.haf.s.options[document.haf.s.selectedIndex].value;document.haf.s.value=''">
					<Option value=" " selected="selected">recently used locations </option>
					</SELECT>
					</div>
					</div>
					
					
					</td>
			</tr>
					
			<tr id="formrow">
				<td nowrap id="editrow">
					<div id="shDontZipFiles">
						<%-- <html:checkbox property="shDontZipFiles" value="true" styleId="shDontZipFiles"><b>Do not zip files after harvesting</b></html:checkbox> --%>
						<b>Post-processing of files</b>: <a href="javascript:popupHelp('<c:out value="${pageContext.request.contextPath}"/>','dontzipfiles')" class="helpp">   <img src='<c:out value="${pageContext.request.contextPath}" />/images/help.gif' border=0></a>
						<br>
						<html:radio property="shDontZipFiles" value="true">Do not zip files</html:radio><br>
						<html:radio property="shDontZipFiles" value="false">Zip files into a single archive for download</html:radio><br>
					</div>
				</td>
			</tr>
					
			<tr id="formrow">
				<td nowrap id="editrow">
					<div id="setsplits">

						<b>When saving files</b>: <a href="javascript:popupHelp('<c:out value="${pageContext.request.contextPath}"/>','split')" class="helpp">   <img src='<c:out value="${pageContext.request.contextPath}" />/images/help.gif' border=0></a>
						<br>
						  <html:radio property="shSet" value="dontsplit">Do not split by set </html:radio><br>
						  <html:radio property="shSet" value="split">Split by set</html:radio><br>
					</div>
				</td>
			</tr>	 
			
			
		
			
			
		</table>

		</logic:present>		
		
		
							
		
		<%-- Footer --%>
		<br>
		<table>		
			<tr>
				<td colspan=2>
				<html:submit value="Save"/>
				<html:button property="cancel" value="Cancel" onclick="window.location='harvester.do'"/>
				</td>
			</tr>
		</table>
					
	</html:form>		
  

<c:import url="../bottom.jsp"/>
</body>
</html:html>
