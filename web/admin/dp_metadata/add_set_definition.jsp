<%@ include file="../../TagLibIncludes.jsp" %>
<c:set var="rm" value="${applicationScope.repositoryManager}"/>


<c:set var="title">
	<c:choose>
		<c:when test="${not empty param.edit}">
			Edit Set: ${setDefinitionsForm.setName}
		</c:when>
		<c:otherwise>
			Define A New Set
		</c:otherwise>
	</c:choose>
</c:set>

<html:html>
  <head>
    <title>${title}</title>
    <html:base />

	<%-- Include style/menu templates --%>
	<%@ include file="../../head.jsp" %>
				
	<style type="text/css">
		ul.mouseOverList{	
			margin:0;
			padding:0;
			float:left;
		}
		li.mouseOver{
			border-width:1px;
			border-style:solid;
			border-color:#C1C1C1;
			cursor: default;
		
			background-color:#E6E6E6;
			padding:0;
			padding-right:10px;
			margin:0;
			margin-top:2px;			
			list-style-type:none;
		}
		li.mouseOut{
			border-width:1px;
			border-style:solid;
			border-color:#f5de98;
			
			padding:0;
			padding-right:10px;
			margin:0;
			margin-top:2px;
			list-style-type:none;			
		}					
	</style>	
	
	<%-- JavaScript for these forms: --%>
	<script type="text/javascript">
	<!--
	
		function rollbg(chosen, ID) {
			var elm = document.getElementById(ID);
			if(elm) {
				if(chosen == "over") {
					elm.className="mouseOver";
				}
				else {
					elm.className="mouseOut";
				}
			}
		}
		
		function toggleCheckbox(ID){
			var elm = document.getElementById(ID);
			if(elm) {
				if(elm.checked)
					elm.checked = false;
				else
					elm.checked = true;
			}
		}
		
		function check(ID){
			var elm = document.getElementById(ID);
			if(elm)
				elm.checked = true;
		}	
			
			
		function show( ID ) {
			var elm = document.getElementById( ID );
			if ( elm != null )
				elm.style.display = '';
		}
		
		function hide( ID ) {
			var elm = document.getElementById( ID );
			if ( elm != null )
				elm.style.display = 'none';
		}
		
		function cleanForm() {
			return;
			if( document.setDefinitionsForm ){
				var radio = document.getElementById('include_radio_1');
				if(radio != null && radio.checked) {
					clearInput( document.setDefinitionsForm.includedFormat );
					clearInput( document.setDefinitionsForm.includedDirs[0] );
				}
			}
		}
		
		function clearInput( elm ) {
			return;
			if(elm && elm != null) {
				elm.value='';
			}
		}
		
	-->
	</script>	
  </head>

  <%-- <body bgcolor="white" onload="updateNs();"> --%>
  <body id="setsConfigPage">
  
	<%-- Include style/menu templates --%>
	<c:import url="../../top.jsp?sec=provider" />
  	
  	<h1>${title}</h1>
	
	<noscript>
		<p>Note: This page uses JavaScript for certain important features. 
			Please enable JavaScript in your browser.</p>
	</noscript> 
	
	<logic:messagesPresent>
		<c:set var="errorMsg">There is an <strong>error</strong>. </c:set>
		<p><font color="red">${errorMsg}</font></p>
	</logic:messagesPresent>


	<html:messages property="noDefinitionProvided" id="noDefinitionProvided">
			<div style="color:red">
			    The set definition includes all metadata files in the repository. A set is defined as a <i>subgroup</i> of the metadata files in a repository. 
				Create a <i>subgroup</i> of metadata files. That is, at least do one of these options: </div>
			<div>
				<ul>
					<li style="color:red;">
						Select all files and then either constrain (Step 2) or exclude (Step 3) some files
					</li>
					
					<li style="color:red;">
						Pick files in a particular metadata format (if the repository only contains one metadata format and it is selected then either constrain (Step 2) or exclude (Step 3) some files)
					</li>
					<li style="color:red;">
						Add directories of files (if all directories are added, then either constrain (Step 2) or exclude (Step 3) some files)
					</li>
				</ul>
			</div>
	</html:messages>
	
	<%-- <font color="red"><html:errors property="noDefinitionProvided"/></font> --%>
	
	<%-- Note: To access a propery in a DynaValidatorForm, use: ${addMetadataDirsForm.map.dirNickname} --%>
	<p>Complete the required fields of Set name and SetSpec. Then complete steps 1-3.</p>
	
	
  	<html:form action="admin/set_definition-validate" method="POST" focus="setName" onsubmit="cleanForm();">
	<table bgcolor="#666666" cellpadding="6" cellspacing="1" border="0">
		<tr id="headrow">
			<td>
						<div><b>Set information</b></div>
			</td>
		</tr>
		<tr id="formrow">
			<td>		
				<div><b>Set name:</b><a href="javascript:popupHelp('<c:out value="${pageContext.request.contextPath}"/>','set_name')" class="helpp">   <img src='<c:out value="${pageContext.request.contextPath}" />/images/help.gif' border=0></a>
				</div>
				<div><html:text property="setName" size="80" /></div>
				<font color="red"><html:errors property="setName"/></font>
			</td>
		</tr>
		<tr id="formrow">
			<td>
				<div><b>SetSpec:</b><a href="javascript:popupHelp('<c:out value="${pageContext.request.contextPath}"/>','set_setspec')" class="helpp">   <img src='<c:out value="${pageContext.request.contextPath}" />/images/help.gif' border=0></a></div>
				<div><html:text property="setSpec" size="18" /></div>
				<font color="red">
					<html:errors property="setSpec"/>
					<html:errors property="setSpecAlreadyInUse"/>
					<html:errors property="setSpecSyntax"/>
				</font>
			</td>
		</tr>		
		<tr id="formrow">
			<td>
				<div><b>Set description (optional):</b><a href="javascript:popupHelp('<c:out value="${pageContext.request.contextPath}"/>','set_desc')" class="helpp">   <img src='<c:out value="${pageContext.request.contextPath}" />/images/help.gif' border=0></a></div>
				<div><b><html:textarea property="setDescription" cols="80" rows="3" /></b></div>
				<font color="red"><html:errors property="setDescription"/></font>
			</td> 
		</tr>
		<tr id="formrow">
			<td>
				<div><b>URL to additional set information (optional):</b><a href="javascript:popupHelp('<c:out value="${pageContext.request.contextPath}"/>','set_url')" class="helpp">   <img src='<c:out value="${pageContext.request.contextPath}" />/images/help.gif' border=0></a></div>
				<div><b><html:text property="setURL" size="80"/></b></div>
				<font color="red"><html:errors property="setURL" /></font>
			</td>
		</tr>
	</table>
		
	<br/><br/>
	<p>
		Define which files are part of this set in steps 1-3.
	</p>
	
	<table bgcolor="#666666" cellpadding="6" cellspacing="1" border="0">
		<tr id="headrow">
			<td>
				<div><b>Step 1: Choose files to include in this set</b><a href="javascript:popupHelp('<c:out value="${pageContext.request.contextPath}"/>','set_step1')" class="helpp">   <img src='<c:out value="${pageContext.request.contextPath}" />/images/help.gif' border=0></a></div>
			</td>
		</tr>
		<tr id="formrow">
			<td>
				<c:set var="hasIndludedDir" value="false"/>
				<c:forEach items="${setDefinitionsForm.includedDirs}" var="item">
					<c:if test="${not empty item}">
						<c:set var="hasIndludedDir" value="true"/>
					</c:if>
				</c:forEach>
				<c:choose>
					<c:when test="${not empty setDefinitionsForm.includedFormat}">
						<c:set var="includeRadio" value="includedFormat"/>
					</c:when>
					<c:when test="${hasIndludedDir == 'true'}">
						<c:set var="includeRadio" value="includedDirs"/>
					</c:when>
					<c:otherwise>
						<c:set var="includeRadio" value="includeAll"/>
					</c:otherwise>					
				</c:choose>
				
				
				<div style="padding-bottom:6px">
					<input type="radio" id="include_radio_1" name="include_radio" value="include_radio_1" ${includeRadio == 'includeAll' ? 'checked' : ''}/>
					<label for="include_radio_1" class="myClass">All files</label>
				</div> 
				<div style="padding-bottom:6px"> - or -</div>
				<div style="padding-bottom:6px">
					<input type="radio" id="include_radio_2" name="include_radio" value="include_radio_2" ${includeRadio == 'includedFormat' ? 'checked' : ''}/>
					<label for="include_radio_2" class="myClass">Files in this native format:</label>
					
					<html:select property="includedFormat" onkeypress="check('include_radio_2');" onclick="check('include_radio_2');">
						<html:option value="">-- Choose format --</html:option>
						<c:forEach items="${rm.configuredFormats}" var="format">
							<html:option value="${format}">${format}</html:option>
						</c:forEach>
						<c:if test="${setDefinitionsForm.includedFormatNotAvailable}">
							<html:option value="${setDefinitionsForm.includedFormat}">${setDefinitionsForm.includedFormat} *[no longer in repository]</html:option>
						</c:if>						
					</html:select>
					<div style="color:red">
						<c:if test="${setDefinitionsForm.includedFormatNotAvailable}">
							<div>* Warning: This format is no longer configured in the repository</div>
						</c:if>
						<html:errors property="includedFormat"/>
					</div>
				</div>
				<div style="padding-bottom:6px"> - or -</div>
								
				
				<div>
					<input type="radio" id="include_radio_3" name="include_radio" value="include_radio_3" ${includeRadio == 'includedDirs' ? 'checked' : ''}/>
					<label for="include_radio_3" class="myClass">Files in these directories:</label>
					
					<div style="padding-left:25px">
						<ul class="mouseOverList">
						
						<%-- Display dirs that are defined in the set but are no longer in the repository --%>
						<c:forEach items="${setDefinitionsForm.includedDirsNotInRepository}" var="missingDir" varStatus="i">
							<div style="padding-top:2px">
								<li id="incl-dir-missing-${i.index}" class="mouseOut" onClick="toggleCheckbox('includedDirs-missing-${i.index}');check('include_radio_3');" onMouseOver="rollbg('over','incl-dir-missing-${i.index}')" onMouseOut="rollbg('out','incl-dir-missing-${i.index}')">
								<table cellpadding="0" cellspacing="0" border="0">
									<tr>
										<td valign="top">
											<html:multibox 
												property="includedDirs" 
												value="${missingDir}"
												styleId="includedDirs-missing-${i.index}"
												onclick="toggleCheckbox('includedDirs-missing-${i.index}');check('include_radio_3');"/>
										</td>
										<td valign="top" style="padding-left:8px">
											<c:set var="dirLabel">
											<c:choose>
												<c:when test="${fn:length(missingDir) > 100}">
													${fn:substring(missingDir,0,47)}
													...
													${fn:substring(missingDir,fn:length(missingDir)-48,fn:length(missingDir))}
												</c:when>
												<c:otherwise>
													${missingDir}
												</c:otherwise>
											</c:choose>
											</c:set>
											
											${dirLabel}
											<div style="color:red">
												<div>* Warning: This directory is no longer configured in the repository</div>
												<html-el:errors property="includedDirs[${i.index}]"/>
											</div>
										</td>
									</tr>
								</table>
								</li>
							</div>						
						</c:forEach>
						
						<%-- Display available dirs --%>
						<c:forEach items="${setDefinitionsForm.setInfos}" var="setInfo" varStatus="i">
							<div style="padding-top:2px">
								<li id="incl-dir-${i.index}" class="mouseOut" onClick="toggleCheckbox('includedDirs-${i.index}');check('include_radio_3');" onMouseOver="rollbg('over','incl-dir-${i.index}')" onMouseOut="rollbg('out','incl-dir-${i.index}')">
								<table cellpadding="0" cellspacing="0" border="0">
									<tr>
										<td valign="top">
											<html:multibox 
												property="includedDirs" 
												value="${setInfo.directory}"
												styleId="includedDirs-${i.index}"
												onclick="toggleCheckbox('includedDirs-${i.index}');check('include_radio_3');"/>
										</td>
										<td valign="top" style="padding-left:8px">
											<c:set var="dirLabel">
											${setInfo.name}
											(${setInfo.format})<br/>
											<c:choose>
												<c:when test="${fn:length(setInfo.directory) > 100}">
													${fn:substring(setInfo.directory,0,47)}
													...
													${fn:substring(setInfo.directory,fn:length(setInfo.directory)-48,fn:length(setInfo.directory))}
												</c:when>
												<c:otherwise>
													${setInfo.directory}
												</c:otherwise>
											</c:choose>
											</c:set>
											
											${dirLabel}
											<div style="color:red"><html-el:errors property="includedDirs[${i.index}]"/></div>
										</td>
									</tr>
								</table>
								</li>
							</div>
						</c:forEach>
						</ul>
					</div>
					<%-- End dir display --%>
					
				</div>
			</td>
		</tr>
	</table>

	<br/><br/>
		
	<table bgcolor="#666666" cellpadding="6" cellspacing="1" border="0">
		<tr id="headrow"> 
			<td>
				<c:choose>
					<c:when test="${not empty setDefinitionsForm.includedTerms || not empty setDefinitionsForm.includedQuery}">
						<c:set var="limitRadio" value="hasLimit"/>
					</c:when>
					<c:otherwise>
						<c:set var="limitRadio" value="noLimit"/>
					</c:otherwise>					
				</c:choose>			
			
				<div><b>Step 2: Specify criteria to constrain the set</b><a href="javascript:popupHelp('<c:out value="${pageContext.request.contextPath}"/>','set_step2')" class="helpp">   <img src='<c:out value="${pageContext.request.contextPath}" />/images/help.gif' border=0></a></div>
				
			</td>
		</tr>
		
		<tr id="formrow">
			<td>

				<div style="padding-bottom:6px">
					<input type="radio" id="limit_radio_1" name="limit_radio" value="limit_radio_1" ${limitRadio == 'noLimit' ? 'checked' : ''}/>
					<label for="limit_radio_1" class="myClass">Do not constrain what is included</label>
				</div>
				
				<div style="padding-bottom:6px"> - or -</div>

				<div style="padding-bottom:6px">
					<input type="radio" id="limit_radio_2" name="limit_radio" value="limit_radio_2" ${limitRadio == 'hasLimit' ? 'checked' : ''}/>
					<label for="limit_radio_2" class="myClass">Constrain what is included as follows:</label>
				</div>
				
				<div style="padding-left:25px">
					<div>
					Only include files that contain the following terms or phrases (enter comma-separated list):
					<a href="javascript:popupHelp('<c:out value="${pageContext.request.contextPath}"/>','set_terms_phrases')" class="helpp"> <img src='<c:out value="${pageContext.request.contextPath}" />/images/help.gif' border=0></a>
					</div>
					<div><b><html:textarea property="includedTerms" cols="80" rows="3" onkeypress="check('limit_radio_2');" onclick="check('limit_radio_2');"/></b></div>
					<font color="red"><html:errors property="includedTerms"/></font>
					
					<br/>
					
					<div>
						Only include files that match the following Lucene query (advanced):
						<a href="javascript:popupHelp('<c:out value="${pageContext.request.contextPath}"/>','set_lucene_query')" class="helpp"> <img src='<c:out value="${pageContext.request.contextPath}" />/images/help.gif' border=0></a>
					</div>
					<div><b><html:textarea property="includedQuery" cols="80" rows="3" onkeypress="check('limit_radio_2');" onclick="check('limit_radio_2');"/></b></div>
					<font color="red"><html:errors property="includedQuery"/></font>
				</div>
			</td>
		</tr>
	</table>

	<br/><br/>

	<table bgcolor="#666666" cellpadding="6" cellspacing="1" border="0">
		<tr id="headrow">
			<td>
				<c:set var="hasExdludedDir" value="false"/>
				<c:forEach items="${setDefinitionsForm.excludedDirs}" var="item">
					<c:if test="${not empty item}">
						<c:set var="hasExdludedDir" value="true"/>
					</c:if>
				</c:forEach>
				<c:choose>
					<c:when test="${hasExdludedDir == 'true' || not empty setDefinitionsForm.excludedTerms || not empty setDefinitionsForm.excludedQuery}">
						<c:set var="excludeRadio" value="hasExcludes"/>
					</c:when>
					<c:otherwise>
						<c:set var="excludeRadio" value="noExcludes"/>
					</c:otherwise>					
				</c:choose>			
			
			
				<div><b>Step 3: Exclude files from this set</b><a href="javascript:popupHelp('<c:out value="${pageContext.request.contextPath}"/>','set_step3')" class="helpp">   <img src='<c:out value="${pageContext.request.contextPath}" />/images/help.gif' border=0></a></div>

			</td>
		</tr>		
		
		<tr id="formrow">
			<td>		
			
				<div style="padding-bottom:6px">
					<input type="radio" id="exclude_radio_1" name="exclude_radio" value="exclude_radio_1" ${excludeRadio == 'noExcludes' ? 'checked' : ''}/>
					<label for="exclude_radio_1" class="myClass">Do not exclude any files</label>
				</div>			
			
				<div style="padding-bottom:6px"> - or -</div>
								
				<div style="padding-bottom:6px">
					<input type="radio" id="exclude_radio_2" name="exclude_radio" value="exclude_radio_2" ${excludeRadio == 'hasExcludes' ? 'checked' : ''}/>
					<label for="exclude_radio_2" class="myClass">Exclude files that meet these criteria:</label>
				</div>
				
				<div style="padding-left:25px">
					<table cellpadding="0" cellspacing="0" border="0">
						<tr>
							<td>					
								<div>Exclude files in these directories:</div>
				
								<div style="padding-left:20px">
									<ul class="mouseOverList">
									 
									<%-- Display dirs that are defined in the set but are no longer in the repository --%>
									<c:forEach items="${setDefinitionsForm.excludedDirsNotInRepository}" var="missingDir" varStatus="i">
										<div style="padding-top:2px">
											<li id="excl-dir-missing-${i.index}" class="mouseOut" onClick="toggleCheckbox('excludedDirs-missing-${i.index}');check('exclude_radio_2');" onMouseOver="rollbg('over','excl-dir-missing-${i.index}')" onMouseOut="rollbg('out','excl-dir-missing-${i.index}')">
											<table cellpadding="0" cellspacing="0" border="0">
												<tr>
													<td valign="top">
														<html:multibox 
															property="excludedDirs" 
															value="${missingDir}"
															styleId="excludedDirs-missing-${i.index}"
															onclick="toggleCheckbox('excludedDirs-missing-${i.index}');check('exclude_radio_2');"/>
													</td>
													<td valign="top" style="padding-left:8px">
														<c:set var="dirLabel">
														<c:choose>
															<c:when test="${fn:length(missingDir) > 100}">
																${fn:substring(missingDir,0,47)}
																...
																${fn:substring(missingDir,fn:length(missingDir)-48,fn:length(missingDir))}
															</c:when>
															<c:otherwise>
																${missingDir}
															</c:otherwise>
														</c:choose>
														</c:set>
														
														${dirLabel}
														<div style="color:red">
															<div>* Warning: This directory is no longer configured in the repository</div>
															<html-el:errors property="excludedDirs[${i.index}]"/>
														</div>
													</td>
												</tr>
											</table>
											</li>
										</div>						
									</c:forEach>
									
									<%-- Display available dirs --%>
									<c:forEach items="${rm.setInfos}" var="setInfo" varStatus="i">
										<div style="padding-top:2px">
											<li id="excl-dir-${i.index}" class="mouseOut" onClick="toggleCheckbox('excludedDirs-${i.index}');check('exclude_radio_2');" onMouseOver="rollbg('over','excl-dir-${i.index}')" onMouseOut="rollbg('out','excl-dir-${i.index}')">
											<table cellpadding="0" cellspacing="0" border="0">
												<tr>
													<td valign="top">
														<html:multibox 
															property="excludedDirs" 
															value="${setInfo.directory}"
															styleId="excludedDirs-${i.index}"
															onclick="toggleCheckbox('excludedDirs-${i.index}');check('exclude_radio_2');"/>
													</td>
													<td valign="top" style="padding-left:8px">
														<c:set var="dirLabel">
														${setInfo.name}
														(${setInfo.format})<br/>
														<c:choose>
															<c:when test="${fn:length(setInfo.directory) > 100}">
																${fn:substring(setInfo.directory,0,47)}
																...
																${fn:substring(setInfo.directory,fn:length(setInfo.directory)-48,fn:length(setInfo.directory))}
															</c:when>
															<c:otherwise>
																${setInfo.directory}
															</c:otherwise>
														</c:choose>
														</c:set>
														
														${dirLabel}
														<div style="color:red"><html-el:errors property="excludedDirs[${i.index}]"/></div>
													</td>
												</tr>
											</table>
											</li>
										</div>
									</c:forEach>
									</ul>
								</div>
								<%-- End dir display --%>
								
							</td>
						</tr>
						<tr>
							<td>						
								<div style="padding-top:14px">
									Exclude files that contain the following terms or phrases (enter comma-separated list):
									<a href="javascript:popupHelp('<c:out value="${pageContext.request.contextPath}"/>','set_terms_phrases')" class="helpp"> <img src='<c:out value="${pageContext.request.contextPath}" />/images/help.gif' border=0></a>
								</div>
								<div><b><html:textarea property="excludedTerms" cols="80" rows="3" onkeypress="check('exclude_radio_2');" onclick="check('exclude_radio_2');"/></b></div>
								<font color="red"><html:errors property="excludedTerms"/></font>
								
								<br/>
								
								<div>
									Exclude files that match the following Lucene query (advanced):
									<a href="javascript:popupHelp('<c:out value="${pageContext.request.contextPath}"/>','set_lucene_query')" class="helpp"> <img src='<c:out value="${pageContext.request.contextPath}" />/images/help.gif' border=0></a>
								</div>
								<div><b><html:textarea property="excludedQuery" cols="80" rows="3" onkeypress="check('exclude_radio_2');" onclick="check('exclude_radio_2');"/></b></div>
								<font color="red"><html:errors property="excludedQuery"/></font>
					
							</td>
						</tr>
					</table>
				</div>

			</td>
		</tr>
	</table>

	<br/><br/>
			
	<table cellpadding="6" cellspacing="1" border="0">	
		<tr>
			<td>	
				<c:choose>
					<c:when test="${not empty param.edit}">
						<input type="hidden" name="edit" value="${param.edit}">
					</c:when>
					<c:otherwise>
						
					</c:otherwise>
				</c:choose>
				
				<html:submit>Save</html:submit>
				<%-- <html:reset>Reset</html:reset> --%>
				<html:cancel>Cancel</html:cancel>
			</td>
		 </tr>
      </table>
	 </html:form>

  
<%-- Include style/menu templates --%>
<%@ include file="../../bottom.jsp" %>  
  </body>
</html:html>

