<%@ page contentType="text/html; charset=UTF-8" %>
<%@ include file="../../TagLibIncludes.jsp" %><head>
<c:set var="rm" value="${applicationScope.repositoryManager}"/>

<c:set var="title">
Admin Search</c:set>


<jsp:useBean id="queryForm" class="org.dlese.dpc.action.form.SimpleQueryForm" scope="session"/>

<html:html>
<title>${title}</title>
<META http-equiv="Content-Type" content="text/html; charset=UTF-8">
<%-- Include style/menu templates --%>
<%@ include file="../../head.jsp" %>

<script>
	// Override the method below to set the focus to the appropriate field.
	function sf(){}	
</script>
</head>
<body onLoad=sf()>

<%-- Include style/menu templates --%>
<c:import url="../top.jsp?sec=provider" />

<h1>${title}</h1>

<p>
	Search and view items in the data provider. Free-text searches
	operate over the full text found in all indexed metadata files. 
	Searches may be limited by keywords, sets, metadata format, and/or record attributes. 
</p>

	<%-- ######## Display messages, if present ######## --%>
	<logic:messagesPresent> 		
	<table width="90%" bgcolor="#000000" cellspacing="1" cellpadding="8">
	  <tr bgcolor="ffffff"> 
		<td>
			<b>Messages:</b>
			<ul>
				<html:messages id="msg" property="message"> 
					<li><bean:write name="msg"/></li>									
				</html:messages>
				<html:messages id="msg" property="error"> 
					<li><font color=red>Error: <bean:write name="msg"/></font></li>									
				</html:messages>
			</ul>
			<blockquote>[ <a href="query.do">OK</a> ]</blockquote>
		</td>
	  </tr>
	</table>		
	</br></br>
	</logic:messagesPresent>

<%-- Search form --%>
<table>
	  <html:form action="/admin/query" method="GET">
	  <script>function sf(){document.queryForm.q.focus();}</script>
	  <tr>
		<td>
			Enter a search:
		</td>
		<td colspan="3">
			<html:text property="q" size="35"/>
			<input type="hidden" name="s" value="0">
			<input type="hidden" name="rq" value="!doctype:0errordoc">
			<html:submit value="Search"/>
		</td>
	  </tr>
	  <logic:present name="queryForm" property="collections">
	  <tr valign=top>			
		<td align=left>	
			Search set(s):
		</td>
		<td align=left>
			<html:select property="selSetSpecs" size="5" multiple="t">
				<html:options  name="queryForm" property="oaiSets" />
			</html:select>
		</td>
		<td align=left>	
			Format(s):
		</td>
		<td align=left>
			<html:select property="sfmts" size="5" multiple="t">
				<html:options  name="queryForm" property="formats" labelProperty="formatLabels"/>
			</html:select>
		</td>
		<td align=left>	
			Record</br>
			Attribute(s):
		</td>		
		<td align=left>
			<html:select property="selStatus" size="5" multiple="t">
				<html:options  name="queryForm" property="status" labelProperty="statusLabels"/>
			</html:select>
		</td>  	  	
	  </tr>
	  </logic:present>
	  </html:form>
</table>	
	
	
<%-- Results table --%>	
<table>

  <logic:equal name="queryForm" property="numResults" value="0">
  <logic:present parameter="q">
  <tr>
	<td colspan=2>
	Your search ${empty queryForm.q ? '':'for'} <font color=blue>${queryForm.q}</font>
	had no matches.
	</td>
  </tr>
  </logic:present>
  </logic:equal>
  
  <logic:notEqual name="queryForm" property="numResults" value="0">
  <tr>
	<td colspan=2>
		<c:if test="${not empty param.title}">
			<h3 style="padding-top:8px">${param.title}</h3>
		</c:if>
		<div style="margin-top:8px;margin-bottom:8px">
			Your search ${empty queryForm.q ? '':'for'} <font color=blue>${queryForm.q}</font>
			had <fmt:formatNumber type="number" value="${queryForm.numResults}" /> matches.
		</div>
	</td>
  </tr> 
   <tr>
	<td colspan=2 nowrap>
		<%-- Pager --%>
		<c:set var="pager">
			<logic:present name="queryForm" property="prevResultsUrl">
				<a href='query.do?<bean:write name="queryForm" property="prevResultsUrl"/>'><img src='../images/arrow_left.gif' width='16' height='13' border='0' alt='Previous results page'/></a>
			</logic:present>
			&nbsp;Results: <fmt:formatNumber type="number" value="${queryForm.start}" /> - <fmt:formatNumber type="number" value="${queryForm.end}" /> out of <fmt:formatNumber type="number" value="${queryForm.numResults}" />&nbsp;
			<logic:present name="queryForm" property="nextResultsUrl">                                      
				<a href='query.do?<bean:write name="queryForm" property="nextResultsUrl"/>'><img src='../images/arrow_right.gif' width='16' height='13' border='0' alt='Next results page'/></a>
			</logic:present>
		</c:set>
		${pager}
	</td>
  </tr>   
  
  <logic:iterate id="result" name="queryForm" property="results" indexId="index" offset="<%= queryForm.getOffset() %>" length="<%= queryForm.getLength() %>">  
	  <tr>
		<td colspan=2>		
			<hr noshade><br/>
			
			<c:choose>
			
				<%-- --------- Display item-level records such as ADN --------- --%>
				<c:when test="${result.docReader.readerType == 'ItemDocReader'}">
					<font size=+1><b><bean:write name="result" property="docReader.title" filter="false" /></b></font></br>	
					<a href='<bean:write name="result" property="docReader.url" filter="false" />'><bean:write name="result" property="docReader.url" filter="false" /></a></br>											
					<div style="padding-top:4px;padding-bottom:8px;">
						<bean:write name="result" property="docReader.description" filter="true" />
					</div>
					<logic:notMatch name="result" property="docReader.deleted" value="true">
					<logic:notEmpty name="result" property="docReader.keywordsDisplay">
						<b>Keywords:</b> &nbsp;<bean:write name="result" property="docReader.keywordsDisplay" filter="false" /></br>				
					</logic:notEmpty> 				
					<logic:notEmpty name="result" property="docReader.gradeRanges">
						<b>Grade ranges:</b> &nbsp;
						<bean:write name="result" property="docReader.gradeRanges[0]"/><logic:iterate id="gr" name="result" 
						property="docReader.gradeRanges" offset="1">,
						<bean:write name="gr"/></logic:iterate></br>				
					</logic:notEmpty>
					<logic:notEmpty name="result" property="docReader.resourceTypes">
						<b>Resource types:</b> &nbsp;
						<bean:write name="result" property="docReader.resourceTypes[0]"/><logic:iterate id="re" name="result" 
						property="docReader.resourceTypes" offset="1">,
						<bean:write name="re"/></logic:iterate></br>							
					</logic:notEmpty>
					<logic:notEmpty name="result" property="docReader.subjects">
						<b>Subjects:</b> &nbsp;
						<bean:write name="result" property="docReader.subjects[0]"/><logic:iterate id="su" name="result" 
						property="docReader.subjects" offset="1">,
						<bean:write name="su"/></logic:iterate></br>										
					</logic:notEmpty>
					<logic:notEmpty name="result" property="docReader.errorStrings">
						<b>ID mapper errors:</b> &nbsp;
						<bean:write name="result" property="docReader.errorStrings[0]" filter="false" /><logic:iterate id="errorString" 
						name="result" property="docReader.errorStrings" offset="1">,
						<bean:write name="errorString" filter="false" /></logic:iterate></br>				
					</logic:notEmpty>
					</logic:notMatch>
				</c:when>			
				
				<%-- --------- Display annotation records --------- --%>
				<c:when test="${result.docReader.readerType == 'DleseAnnoDocReader'}">
					<logic:notEmpty name="result" property="docReader.title">
						<div>
								<font size="+1"><b><bean:write name="result" property="docReader.title" filter="false" /></b></font>						
								<logic:notEmpty name="result" property="docReader.url"><br/>		 
									<a href='<bean:write name="result" property="docReader.url" filter="false" />'><bean:write name="result" property="docReader.url" filter="false" /></a>										
									<logic:match name="result" property="docReader.status" value="completed">
										(completed)
									</logic:match>
									<logic:match name="result" property="docReader.status" value="in progress">
										(in progress)
									</logic:match>						
								</logic:notEmpty>						
							</div>									
					</logic:notEmpty>
					<logic:empty name="result" property="docReader.title">
						<div> 
							<font size=+1><b>Annotation</b></font>
							<logic:notEmpty name="result" property="docReader.url"><br/>		 
								<a href='<bean:write name="result" property="docReader.url" filter="false" />'><bean:write name="result" property="docReader.url" filter="false" /></a>					
							</logic:notEmpty>							
						</div>			
					</logic:empty>
					
					<div style="padding-top:6px">
						<logic:notEmpty name="result" property="docReader.description">
							<b>Description:</b> &nbsp;<bean:write name="result" property="docReader.description" filter="true" /></br>
						</logic:notEmpty>				
						<b>ID of annotated item:</b> &nbsp;<bean:write name="result" property="docReader.itemId" filter="false" /></br>
						<b>Annotation type:</b> &nbsp;<bean:write name="result" property="docReader.type" filter="false" /></br>							
						<b>Annotation pathway:</b> &nbsp;<bean:write name="result" property="docReader.pathway" filter="false" /></br>
						<logic:notEmpty name="result" property="docReader.status">
							<b>Annotation status:</b> &nbsp;<bean:write name="result" property="docReader.status" filter="false" /></br>
						</logic:notEmpty>
					</div>
				</c:when>

				<%-- --------- Display collection records --------- --%>
				<c:when test="${result.docReader.readerType == 'DleseCollectionDocReader'}">
					<font size=+1><b><bean:write name="result" property="docReader.shortTitle" filter="false" /></b></font></br>
					
					<logic:notEmpty name="result" property="docReader.collectionUrl">
						<a href='<bean:write name="result" property="docReader.collectionUrl" filter="false" />'><bean:write name="result" property="docReader.collectionUrl" filter="false" /></a> (collection URL)<br/>			
					</logic:notEmpty>
					<a href='<bean:write name="result" property="docReader.scopeUrl" filter="false" />'><bean:write name="result" property="docReader.scopeUrl" filter="false" /></a> (scope statement)	<br/>	
					
					<div style="margin-top:4px">
						<b>Description:</b> &nbsp;<bean:write name="result" property="docReader.description" filter="true" />
					</div>			
				</c:when>
				
				<%-- --------- Display news records --------- --%>
				<c:when test="${result.docReader.readerType == 'NewsOppsDocReader'}">
					<div>		
						<font size=+1><b>${result.docReader.title}</b></font>					
						
						<c:if test="${not empty result.docReader.announcementUrl}">
						</br>
							<a href='${result.docReader.announcementUrl}'>${result.docReader.announcementUrl}</a></br>			
						</c:if>					
					</div>
					<div style="padding-top:4px;padding-bottom:8px;">
						${result.docReader.description}
					</div>
					
					<div>
						<c:forEach items="${result.docReader.announcements}" varStatus="status" var="value"> 
							<c:if test="${status.first}"><div class="searchResultValues"><b>Announcement type:</b> &nbsp;</c:if>
							<c:if test="${not status.last}">${value},</c:if>
							<c:if test="${status.last}">${value}</div></c:if>
						</c:forEach>					
						<c:forEach items="${result.docReader.keywords}" varStatus="status" var="value"> 
							<c:if test="${status.first}"><div class="searchResultValues"><b>Keywords:</b> &nbsp;</c:if>
							<c:if test="${not status.last}">${value},</c:if>
							<c:if test="${status.last}">${value}</div></c:if>
						</c:forEach>
						<c:forEach items="${result.docReader.topics}" varStatus="status" var="value"> 
							<c:if test="${status.first}"><div class="searchResultValues"><b>Topics:</b> &nbsp;</c:if>
							<c:if test="${not status.last}">${value},</c:if>
							<c:if test="${status.last}">${value}</div></c:if>
						</c:forEach>
						<c:forEach items="${result.docReader.audiences}" varStatus="status" var="value"> 
							<c:if test="${status.first}"><div class="searchResultValues"><b>Audience:</b> &nbsp;</c:if>
							<c:if test="${not status.last}">${value},</c:if>
							<c:if test="${status.last}">${value}</div></c:if>
						</c:forEach>
						<c:forEach items="${result.docReader.diversities}" varStatus="status" var="value"> 
							<c:if test="${status.first}"><div class="searchResultValues"><b>Diversities:</b> &nbsp;</c:if>
							<c:if test="${not status.last}">${value},</c:if>
							<c:if test="${status.last}">${value}</div></c:if>
						</c:forEach>
						<c:forEach items="${result.docReader.sponsors}" varStatus="status" var="value"> 
							<c:if test="${status.first}"><div class="searchResultValues"><b>Sponsors:</b> &nbsp;</c:if>
							<c:if test="${not status.last}">${value},</c:if>
							<c:if test="${status.last}">${value}</div></c:if>
						</c:forEach>					
						<c:forEach items="${result.docReader.cityStates}" varStatus="status" var="value"> 
							<c:if test="${status.first}"><div class="searchResultValues"><b>Locations:</b> &nbsp;</c:if>
							<c:if test="${not status.last}">${value},</c:if>
							<c:if test="${status.last}">${value}</div></c:if>
						</c:forEach>
						 <c:set var="date" value="${result.docReader.recordCreationtDate}"/>
						<c:if test="${not empty date}">
							<div class="searchResultValues"><b>Record creation date:</b> &nbsp;
								<fmt:formatDate value="${date}" pattern="yyyy-MM-dd"/>
							</div>
						</c:if>						
						<c:set var="date" value="${result.docReader.eventStartDate}"/>
						<c:if test="${not empty date}">
							<div class="searchResultValues"><b>Event start date:</b> &nbsp;
								<fmt:formatDate value="${date}" pattern="yyyy-MM-dd"/>
							</div>
						</c:if>
						<c:set var="date" value="${result.docReader.eventStopDate}"/>
						<c:if test="${not empty date}">
							<div class="searchResultValues"><b>Event stop date:</b> &nbsp;
								<fmt:formatDate value="${date}" pattern="yyyy-MM-dd"/>
							</div>
						</c:if>									
					</div>							
				</c:when>
				
				<%-- --------- Display formats that are available in Dublin Core --------- --%>
				<c:when test="${not empty result.docReader.oaiDublinCoreXml || not empty result.docReader.nsdlDublinCoreXml}">
					<%-- Parse the XML, removing namespaces to create the DOM --%>
					<c:catch var="xmlParseError">
						<c:choose>
							<c:when test="${not empty result.docReader.oaiDublinCoreXml}">
								<x:transform xslt="${f:removeNamespacesXsl()}" xml="${result.docReader.oaiDublinCoreXml}" var="myRec"/>
								<x:set var="dcRecord" select="$myRec/dc"/>
							</c:when>
							<c:otherwise>
								<x:transform xslt="${f:removeNamespacesXsl()}" xml="${result.docReader.nsdlDublinCoreXml}" var="myRec"/>
								<x:set var="dcRecord" select="$myRec/nsdl_dc"/>							
							</c:otherwise>
						</c:choose>
					</c:catch>
					<c:choose>
						<c:when test="${not empty xmlParseError}">
							<div style="color:red;">Error parsing available OAI DC: ${xmlParseError}</div>
							<% ((Throwable)pageContext.getAttribute("xmlParseError")).printStackTrace();  %>
						</c:when>
						<c:otherwise>
							<c:set var="title">
								<x:out select="$dcRecord/title"/>
							</c:set>						
							<c:set var="url">
								<x:out select="$dcRecord/identifier[starts-with(text(),'http')] | identifier[starts-with(text(),'ftp')]"/>
							</c:set>
							<c:set var="description">
								<x:out select="$dcRecord/description"/>
							</c:set>							
							
							<c:if test="${not empty title}">
								<font size=+1><b>${title}</b></font></br>	
							</c:if>
							<c:if test="${not empty url}">
								<a href='${url}'>${url}</a></br>
							</c:if>
							<c:if test="${not empty description}">
								<div style="padding-top:4px;padding-bottom:8px;">
									${description}
								</div>
							</c:if>
						</c:otherwise>
					</c:choose>
				</c:when>
				
				<%-- --------- Unknown file/record type --------- --%>
				<c:otherwise>
					<font size=+1><b><bean:write name="result" property="docReader.id" filter="false" /></b></font></br>

				</c:otherwise>	
			</c:choose>
			
			<%-- --------- Display info common to all record formats --------- --%>
			<div style="padding-top:4px">
				<b>ID:</b>&nbsp; ${result.docReader.id}<br/>										
				
				<b>Native file format:</b>&nbsp; <bean:write name="result" property="docReader.nativeFormat" filter="false" /><br/>
				<b>File location:</b>&nbsp; <bean:write name="result" property="docReader.docsource" filter="false" /><br/>								
				<b>File last modified:</b>&nbsp; <bean:write name="result" property="docReader.lastModifiedString" filter="false" /><br/>
				
				<b>OAI datestamp:</b>&nbsp; <bean:write name="result" property="docReader.oaiLastModifiedString" filter="false" /> ( <bean:write name="result" property="docReader.oaiDatestamp" filter="false" /> )<br/>
				<b>OAI identifier:</b>&nbsp; ${rm.oaiIdPrefix}${result.docReader.id}<br/>
				<c:if test="${not empty result.docReader.oaiSets}">
					<b>Is a member of set${fn:length(result.docReader.oaiSets) > 1 ? 's' : ''}:</b>&nbsp;
					<c:forEach items="${result.docReader.oaiSets}" var="set" varStatus="i">
						${set}${i.last ? '' : ','}
					</c:forEach>
					<br/>
				</c:if>					
				<logic:present name="result" property="docReader.validationReport">
					<b>Validation error:</b>&nbsp; <font color=red><bean:write name="result" property="docReader.validationReport" filter="false" /></font><br/>
				</logic:present>
				<div>
					<c:choose>
						<c:when test="${result.docReader.deleted == 'true'}">
							<b>Status:</b>&nbsp; <span style="color:red; font-size:80%;">DELETED</span>.
							${result.docReader.isMyCollectionDisabled || rm.providerStatus == 'DISABLED' ? 'Harvesters will <i>not</i> be notified:' : 'Harvesters will be notified.'}
						</c:when>
						<c:otherwise>
							<b>Status:</b>&nbsp; ${result.docReader.isMyCollectionDisabled || rm.providerStatus == 'DISABLED' ? 'Not available for harvesting:' : 'Ready for harvest.'}
						</c:otherwise>
					</c:choose>
					${result.docReader.isMyCollectionDisabled ? 'Metadata directory for file is disabled.' : ''}
					${rm.providerStatus == 'DISABLED' ? 'Data provider is disabled.' : ''}
				</div>
				<div>
					<b>Available formats:</b>&nbsp;</br>
					<div style="padding-left:10px; font-size:80%;">
					<logic:iterate id="format" name="result" property="docReader.availableFormats">
						<nobr>
							<b><bean:write name="format" filter="false" />:</b> 
							[ <a href='query.do?id=<bean:write name="result" property="docReader.id" filter="false" />&metadataFormat=<bean:write name="format" filter="false" />'>view</a> | 
							<a href='query.do?id=<bean:write name="result" property="docReader.id" filter="false" />&metadataFormat=<bean:write name="format" filter="false" />&rt=validate'>validate</a> | 
							<a href='../${initParam.dataProviderBaseUrlPathEnding}?verb=GetRecord&metadataPrefix=<bean:write name="format" filter="false" />&identifier=${rm.oaiIdPrefix}${f:URLEncoder(result.docReader.id)}' title="Issue the GetRecord request to this provider" alt="Issue the GetRecord request to this provider">GetRecord</a> ] &nbsp;&nbsp;			
						</nobr>
					</logic:iterate>
					</div>
			    </div>
			
			
			<%-- Display web log entries --%>
			<%-- <logic:match name="result" property="docReader.readerType" value="WebLogReader">
				<b>Requested URL:</b> <bean:write name="result" property="docReader.requestUrl" filter="false" /></br>
				<b>Date of request:</b> <bean:write name="result" property="docReader.requestDate" filter="false" /></br>
				<b>Requesting host:</b> <bean:write name="result" property="docReader.remoteHost" filter="false" /></br>
			</logic:match> --%>
			
			
			
			<%-- Display errors --%>
			<%-- <logic:match name="result" property="docReader.readerType" value="ErrorDocReader">
				<font size=+1><b>File: <bean:write name="result" property="docReader.fileName" filter="false" /></b></font></br>				
				<b>This file could not be indexed due to errors and will not be served by this provider.</b></br>
				<b>File location:</b> <bean:write name="result" property="docReader.docsource" filter="false" /></br>
				<b>File last modified:</b> <bean:write name="result" property="docReader.lastModifiedString" filter="false" /></br>
				<b>Exception type:</b> <bean:write name="result" property="docReader.exceptionName" filter="false" /></br>
				<b>Error message:</b> <font color=red><bean:write name="result" property="docReader.errorMsg" filter="false" /></font></br>								
				[ <a href='../display.do?file=<bean:write name="result" property="docReader.docsource" filter="false" />'>view file</a> ]
				[ <a href='../display.do?file=<bean:write name="result" property="docReader.docsource" filter="false" />&rt=validate'>validate file</a> ]
			</logic:match>	 --%>		
			
		</td>
	</tr> 
  </logic:iterate>
  
   <tr>
   	<td colspan="2">
	 <hr noshade></br>
	</td>
	</tr>
   <tr>
	<td colspan=2 nowrap>		
		<%-- Pager --%>
		${pager}	
	</td>
  </tr>  
  </logic:notEqual> 
</table>
	
<%-- Include style/menu templates --%>
<%@ include file="../../bottom.jsp" %>
</body>
</html:html>

