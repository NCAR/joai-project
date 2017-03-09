<%-- 
This jOAI ODL search client template implements a simple search inteface that accesses
the jOAI ODL web service bundled with the jOAI (DLESE OAI) software v2.0.4 or later. This 
client is implemented as a single JSP page that can be modified to provide
a custom look and feel and search options.

This version operates over the oai_dc metadata format.

To install and modify this page, do the following:
1. Copy this page to the desired JSP context.
2. Place each jar file found in the jOAI software lib
directory (WEB-INF/lib) into the WEB-INF/lib folder of
the target context.
3. Edit the variables as described below under 'EDIT...'
4. Customize the page HTML as desired (optional)

This JSP uses the c and x standard JSTL tag libraries.
A tutorial for these tag libraries is available at:
http://java.sun.com/j2ee/1.4/docs/tutorial/doc/index.html
(see chapter 14 - JSTL).

The x tags use XPath to display data from XML.
A nice XPath tutorial is avaialable at:
http://www.zvon.org/xxl/XPathTutorial/General/examples.html

The XPath specification is available at:
http://www.w3.org/TR/xpath 
--%>
<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix='dds' uri='http://www.dlese.org/dpc/dds/tags' %>
<%@ taglib prefix="x" uri="http://java.sun.com/jsp/jstl/xml" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ page isELIgnored ="false" %>


<%-- -------EDIT THE FOLLOWING VARIABLES------- --%>

<%-- Set the absolute Base URL to the jOAI data provider that will provide the search.
For example, replace "${myBaseUrl}" with "http://www.dlese.org/oai/provider".  --%>
<c:set var="odlBaseUrl" value="${myBaseUrl}" />


<%-- ------- Optional edits below this line ------- --%>

<%-- Additional query contraints applied to all searches (optional). Leave black to apply none. --%>
<c:set var="searchConstraints" value="" />

<%-- The number or search results to return per request - change if desired. --%>
<c:set var="numToReturn" value="10" />

<%-- The maximum character length of the URL that gets displayed - change if desired. --%>
<c:set var="maxUrlLength" value="100" />

<%-- -------END EDIT------- --%>



<%-- Make the user's input persistent for their session --%>
<c:if test="${empty s || not empty param.s}">
	<c:set var="s" scope="session">
		<c:if test="${empty param.s}">0</c:if>
		<c:if test="${!empty param.s}"><c:out value="${param.s}"/></c:if>
	</c:set>
</c:if>

<c:if test="${param.q != null}">
	<c:set var="searchTerms" value="${param.q}" scope="session" />
</c:if>

<c:if test="${param.setSpec != null}">
	<c:set var="selectedSetSpec" value="${param.setSpec}" scope="session" />
</c:if>


<%-- Add HTML such as <head>, <title>, <body>, etc. here and at the bottom, if needed --%>


	<%-- CSS used in the template --%>
	<style type="text/css">
      <!--	
		
 		/* A pseudo class 'blackul' for the A tag */
		A.blackul:link, A.blackul:hover, A.blackul:visited, A.blackul:active {
			color: #333333; 
			text-decoration: none; 
			font-weight: bold;
			font-size: 11pt; 	
		}

		.resourceHeading {	
 			padding-top:16px;
 		}
		
		.blackbold {
			font-weight: bold;
			font-size: 11pt; 			
		}

		.elementTitle { 
			font-weight: bold;
			font-size: 10pt;
			color: #333333; 
			padding-left: 5px; 	
			padding-right: 14px;
			padding-top: 8px;
			padding-bottom: 4px;
		} 
		
		.elementTitle, .elementDescription { 
			font-size: 10pt;
			color: #333333; 
			padding-left: 5px; 	
			padding-right: 14px;
			padding-bottom: 4px;
		}
		
		.elementTitle { 
			font-weight: bold;
			padding-top: 8px;
		}
		
		.elementBody { 
			padding-left: 12px; 	
			padding-right: 14px; 		
			padding-bottom: 4px; 
		} 
		.elementBodyNoPad { 
			padding-left: 10px; 	
			padding-right: 14px;
		} 		
      -->
    </style>

	
	
<%-- XSL that removes all namespaces from XML, making xPath easier to work with (used below) --%>
<c:set var="removeNamespacesXsl">
	<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0" >
		<xsl:template match="@*" >
			<xsl:attribute name="{local-name()}" >
				<xsl:value-of select="." />
			</xsl:attribute>
			<xsl:apply-templates/>
		</xsl:template>
		<xsl:template match ="*" >
			<xsl:element name="{local-name()}" >
				<xsl:apply-templates select="@* | node()" />
			</xsl:element>
		</xsl:template>
	</xsl:stylesheet>	
</c:set>	

<%-- Create the sets selection list --%>
<c:catch var="listSetsError">
	<%-- Construct the jOAI ODL web service request (URL) for ListSets --%>
	<c:url value="${odlBaseUrl}" var="odlRequest">
		<c:param name="verb" value="ListSets"/>
	</c:url>
	
	<%-- Perform the web service request --%>	
	<c:import url="${odlRequest}" var="odlXmlOutput" charEncoding="UTF-8" />
	
	<%-- Remove namespaces from the XML and create the DOM --%>
	<x:transform xslt="${removeNamespacesXsl}" xml="${odlXmlOutput}" var="listSets"/>
	
	<%-- Create the sets selection list HTML, if sets are present --%>
	<x:set var="setNodes" select="$listSets/OAI-PMH/ListSets/set"/>
	<x:if select="count($setNodes) > 0">
		<c:set var="setsSelectList">
			<select name="setSpec">
				<option value=''> -- All sets --</option>
				<x:forEach select="$setNodes">
					<c:set var="curSpec"><x:out select="setSpec"/></c:set>
					<c:choose>
						<c:when test="${curSpec == selectedSetSpec}">
							<c:set var="setName"><x:out select="setName"/></c:set>
							<c:set var="selected" value=" selected"/>	
						</c:when>
						<c:otherwise>
							<c:remove var="selected"/>
						</c:otherwise>
					</c:choose>
					<option value='${curSpec}' ${selected}><x:out select="setName"/></option>
				</x:forEach>
			</select>
		</c:set>
	</x:if>
</c:catch>	
	

<%-- The search box HTML --%>
<table cellpadding="6" cellspacing="0">
  <form name='searchBox' method='GET' action='' accept-charset='UTF-8'>
  
  <tr>
	<td align="right">
		Search terms:
	</td>
	<td colspan="4">
		<input type="text" name='q' size='35' value='<c:out value="${searchTerms}"/>'>&nbsp;
		<input type="submit" value="Search">&nbsp;
		<input type="hidden" name="s" value="0"/><br/>
	</td>
  </tr>
  <c:if test="${not empty setsSelectList}">
	  <tr>
		<td align="right">
			Search in:
		</td>
		<td colspan="4">
			${setsSelectList}
		</td>
	  </tr>
	 </c:if>
  </form>
</table>

<br>


<%-- ------- START SEARCH CODE ------- --%>

<c:choose>
	<%-- ----------Full description for a single record---------- --%>	
	<c:when test="${not empty param.fd}">
	
		<c:catch var="serviceError">
			<%-- Construct the jOAI ODL web service request (URL) for GetRecord --%>
			<c:url value="${odlBaseUrl}" var="odlRequest">
				<c:param name="verb" value="GetRecord"/>
				<c:param name="metadataPrefix" value="oai_dc"/>	
				<c:param name="identifier" value="${param.fd}"/>
			</c:url>			

			<%-- Perform the web service request --%>	
			<c:import url="${odlRequest}" var="odlXmlOutput" charEncoding="UTF-8" />
			
			<%-- Remove namespaces from the XML and create the DOM --%>
			<x:transform xslt="${removeNamespacesXsl}" xml="${odlXmlOutput}" var="result"/>
			
			<%-- Configure the keywords input to highlight --%>
			<dds:setKeywordsHighlight keywords='<%= request.getParameter("q") %>' highlightColor='#676cab' />		
		</c:catch>
				
		<div style="padding-left:4px; padding-top:4px; Color : #333333"><b>Resource description</b></div>
		<table border="0" cellspacing="0" cellpadding="0" width="100%" height="1" hspace="0">
			<tr><td bgcolor="#999999" height="1"></td></tr>
		</table>
				
		<%-- Display an error message if appropriate --%>
		<c:if test="${not empty serviceError}">
			<div style="padding-left:6px">	
				<p><b>There was an internal server error. Please try again later.</b></p>
				<!-- 
				Problem: 
					Unable to parse the ODL service response to this request: 
					${odlRequest}
					
				Error message:
					${serviceError}
					
				Stack trace:
				<c:forEach items="${serviceError.stackTrace}" var="trace">
					${trace}
				</c:forEach>
				 -->
			</div>
		</c:if>			
		
		<%-- Select the XML record to display --%>
		<x:set select="$result//*[local-name()='OAI-PMH']/*[local-name()='GetRecord']/*[local-name()='record']/*[local-name()='metadata']/*[local-name()='dc']" var="dcRecord"/>
		
		<%-- Display a message if there no record is available with the given ID --%>
		<c:if test="${empty dcRecord}">
			<div style="padding-left:6px">	
				<br>No information is available for record id &quot;<c:out value="${param.fd}"/>.&quot;
			</div>
		</c:if>
		
		<%-- Display the full record description --%>
		<c:if test="${not empty dcRecord}">
			<div style="padding-top:8px"/>	
			<table  width="100%" cellpadding="4" cellspacing="0">					
				<%-- Title and URL --%>
				<tr class="resourceHeading"> 
					<td class="resourceHeading">
						<c:set var="url">
							<x:out select="$dcRecord/*[local-name()='identifier'][starts-with(text(),'http')] | *[local-name()='identifier'][starts-with(text(),'ftp')]"/>
						</c:set>
						<c:if test="${empty url}">
							<div class="blackbold">
								<x:out select="$dcRecord/*[local-name()='title']" />
							</div>
						</c:if>
						<c:if test="${not empty url}">
							<%-- Limit the URL that is displayed to a set length --%>
							<c:set var="urlLength">
								<x:out select="string-length($dcRecord/*[local-name()='identifier'])"/>
							</c:set>
							<c:if test="${urlLength+0 > maxUrlLength+0}">
								<c:set var="urlDisplay">
									<x:out select="substring($url,1,$maxUrlLength)"/> ...
								</c:set>
							</c:if>
							<c:if test="${urlLength+0 <= maxUrlLength+0}">
								<c:set var="urlDisplay">
									<x:out select="$url"/>
								</c:set>
							</c:if>			
							<a href='<c:out value="${url}" />' class="blackul"><x:out select="$dcRecord/*[local-name()='title']" escapeXml="false" /></a>
							<br><a href='<c:out value="${url}" />'><c:out value="${urlDisplay}" /></a>
						</c:if>		
					</td>
				</tr>
				<tr>
					<td>					
					
						<x:choose>									
							<%-- Note: the XPath string() function returns false if the element exists but is empty --%>
							<x:when select="string($dcRecord/*[local-name()='description'])">
								<x:forEach select="$dcRecord/*[local-name()='description']">							
									<div class="elementDescription">
										<dds:keywordsHighlight>
											<x:out select="." escapeXml="false"/>
										</dds:keywordsHighlight>
									</div>
								</x:forEach>
							</x:when>									
							<x:otherwise>
								<div class="elementBody">
									<c:if test="${empty url}">
										No further information available.
									</c:if>
									<c:if test="${not empty url}">	
										For details see: <a href='<c:out value="${url}" />'><c:out value="${urlDisplay}" /></a>
									</c:if>
								</div>
							</x:otherwise>
						</x:choose>						
						<x:if select="string($dcRecord/subject)">
							<div class="elementTitle">
								<i>Subject:</i>
							</div>						
							<div class="elementBodyNoPad">
								<dds:keywordsHighlight>
									<x:forEach select="$dcRecord/subject" varStatus="i">							
										<x:out select="." escapeXml="false"/>${i.last ? '' : ','}
									</x:forEach>
								</dds:keywordsHighlight>
							</div>
						</x:if>
						<x:if select="string($dcRecord/type)">
							<div class="elementTitle">
								<i>Resource type:</i>
							</div>
							<div class="elementBodyNoPad">
								<dds:keywordsHighlight>
									<x:forEach select="$dcRecord/type" varStatus="i">							
										<x:out select="." escapeXml="false"/>${i.last ? '' : ','}
									</x:forEach>
								</dds:keywordsHighlight>
							</div>													
						</x:if>						
						<x:if select="string($dcRecord/*[local-name()='source'])">
							<div class="elementTitle">
								<i>Source:</i>
							</div>						
							<x:forEach select="$dcRecord/*[local-name()='source']">							
								<div class="elementBodyNoPad">
									<dds:keywordsHighlight>
										<x:out select="." escapeXml="false"/>
									</dds:keywordsHighlight>
								</div>
							</x:forEach>						
						</x:if>	
						<x:if select="string($dcRecord/*[local-name()='relation'])">
							<div class="elementTitle">
								<i>Relation:</i>
							</div>						
							<x:forEach select="$dcRecord/*[local-name()='relation']">							
								<div class="elementBodyNoPad">
									<dds:keywordsHighlight>
										<x:out select="." escapeXml="false"/>
									</dds:keywordsHighlight>
								</div>
							</x:forEach>						
						</x:if>							
						<x:if select="string($dcRecord/*[local-name()='coverage'])">
							<div class="elementTitle">
								<i>Coverage:</i>
							</div>						
							<x:forEach select="$dcRecord/*[local-name()='coverage']">							
								<div class="elementBodyNoPad">
									<dds:keywordsHighlight>
										<x:out select="." escapeXml="false"/>
									</dds:keywordsHighlight>
								</div>
							</x:forEach>						
						</x:if>	
						<x:if select="string($dcRecord/*[local-name()='date'])">
							<div class="elementTitle">
								<i>Date:</i>
							</div>						
							<x:forEach select="$dcRecord/*[local-name()='date']">							
								<div class="elementBodyNoPad">
									<dds:keywordsHighlight>
										<x:out select="." escapeXml="false"/>
									</dds:keywordsHighlight>
								</div>
							</x:forEach>						
						</x:if>	
						<x:if select="string($dcRecord/*[local-name()='format'])">
							<div class="elementTitle">
								<i>Format:</i>
							</div>						
							<x:forEach select="$dcRecord/*[local-name()='format']">							
								<div class="elementBodyNoPad">
									<dds:keywordsHighlight>
										<x:out select="." escapeXml="false"/>
									</dds:keywordsHighlight>
								</div>
							</x:forEach>						
						</x:if>	
						<x:if select="string($dcRecord/*[local-name()='language'])">
							<div class="elementTitle">
								<i>Language:</i>
							</div>						
							<x:forEach select="$dcRecord/*[local-name()='language']">							
								<div class="elementBodyNoPad">
									<dds:keywordsHighlight>
										<x:out select="." escapeXml="false"/>
									</dds:keywordsHighlight>
								</div>
							</x:forEach>						
						</x:if>							
						<x:if select="string($dcRecord/*[local-name()='creator'])">
							<div class="elementTitle">
								<i>Creator:</i>
							</div>						
							<x:forEach select="$dcRecord/*[local-name()='creator']">							
								<div class="elementBodyNoPad">
									<dds:keywordsHighlight>
										<x:out select="." escapeXml="false"/>
									</dds:keywordsHighlight>
								</div>
							</x:forEach>						
						</x:if>	
						<x:if select="string($dcRecord/*[local-name()='contributor'])">
							<div class="elementTitle">
								<i>Contributor:</i>
							</div>						
							<x:forEach select="$dcRecord/*[local-name()='contributor']">							
								<div class="elementBodyNoPad">
									<dds:keywordsHighlight>
										<x:out select="." escapeXml="false"/>
									</dds:keywordsHighlight>
								</div>
							</x:forEach>						
						</x:if>	
						<x:if select="string($dcRecord/*[local-name()='publisher'])">
							<div class="elementTitle">
								<i>Publisher:</i>
							</div>						
							<x:forEach select="$dcRecord/*[local-name()='publisher']">							
								<div class="elementBodyNoPad">
									<dds:keywordsHighlight>
										<x:out select="." escapeXml="false"/>
									</dds:keywordsHighlight>
								</div>
							</x:forEach>						
						</x:if>							
						<x:if select="string($dcRecord/*[local-name()='rights'])">
							<div class="elementTitle">
								<i>Rights management:</i>
							</div>						
							<x:forEach select="$dcRecord/*[local-name()='rights']">							
								<div class="elementBodyNoPad">
									<dds:keywordsHighlight>
										<x:out select="." escapeXml="false"/>
									</dds:keywordsHighlight>
								</div>
							</x:forEach>						
						</x:if>
						<x:if select="string($dcRecord/*[local-name()='identifier'])">
							<div class="elementTitle">
								<i>DC Identifier:</i>
							</div>						
							<x:forEach select="$dcRecord/*[local-name()='identifier']">							
								<div class="elementBodyNoPad">
									<x:choose>
										<x:when select=".[starts-with(text(),'http')] | *[local-name()='identifier'][starts-with(text(),'ftp')]">
											<a href="<x:out select="." />"><x:out select="." escapeXml="false"/></a>
										</x:when>
										<x:otherwise>
											<x:out select="." escapeXml="false"/>
										</x:otherwise>
									</x:choose>
								</div>
							</x:forEach>						
						</x:if>							
						<div class="elementTitle">
							<i>OAI Identifier:</i>
						</div>	
						<div class="elementBodyNoPad">
							${param.fd} 
						</div>
						
						<c:catch>
							<c:remove var="oaiDate"/>
							<c:set var="oaiDateString"><x:out select="$dcRecord/../../header/datestamp" /></c:set>
							<fmt:parseDate pattern="yyyy-MM-dd't'HH:mm:ss'z'" var="oaiDate">${fn:toLowerCase(oaiDateString)}</fmt:parseDate>
						</c:catch>
						
						<c:if test="${oaiDate != null}">
							<div class="elementTitle">
								<i>Metadata last modified on:</i>
							</div>	
							<div class="elementBodyNoPad">
								<nobr><fmt:formatDate value="${oaiDate}" dateStyle="long"/></nobr>
							</div>
						</c:if>
						
						<div class="elementTitle">
							<i>Additional metadata:</i>
						</div>	
						<div class="elementBodyNoPad">
							<nobr>[ <a href="${odlRequest}">View record XML</a> ]</nobr>
						</div>
						
						<br/><br/>
					</td>
				</tr>
			</table>
			<table border="0" cellspacing="0" cellpadding="0" width="100%" height="1" hspace="0">
				<td bgcolor="#999999" height="1"></td>
			</table>
		</c:if>		
	</c:when><%-- End full record description --%>

	
	<%-- --------------Perform the search and display the search results-------------- --%>	
	<c:otherwise>

		<c:if test="${not empty searchTerms || not empty selectedSetSpec}"> 
			<c:catch var="serviceError">
			
			<%-- Query expansion for boosting is configured in WEB-INF/conf/search_fields.properties --%>
			
			<%-- Set up the query string sent to the web service search engine --%>
			<c:if test="${not empty searchConstraints}">
				<c:set var="searchQuery" value="(${empty fn:trim(searchTerms) ? 'allrecords:true' : searchTerms}) AND (${searchConstraints}) AND !deleted:true"/>	
			</c:if>
			<c:if test="${empty searchConstraints}">
				<c:set var="searchQuery" value="(${empty fn:trim(searchTerms) ? 'allrecords:true' : searchTerms}) AND !deleted:true"/>	
			</c:if>		
			
			<%-- Escape the search query and setSpec for the ODL spec. --%>
			<c:set var="escapedSearchQuery">${fn:replace(searchQuery,'/','%2F')}</c:set>
			<c:set var="escapedSetSpec">${empty selectedSetSpec ? 'null' : fn:replace(selectedSetSpec,'/','%2F')}</c:set>
			
			<%-- Construct the jOAI ODL web service request (URL) for Search (ListRecords) --%>
			<c:url value="${odlBaseUrl}" var="odlRequest">
				<c:param name="verb" value="ListRecords"/>
				<c:param name="metadataPrefix" value="oai_dc"/>	
				<c:param name="set" value="dleseodlsearch/${escapedSearchQuery}/${escapedSetSpec}/${s}/${numToReturn}"/>
			</c:url>

			<%-- Perform the web service request --%>	
			<c:import url="${odlRequest}" var="odlXmlOutput" charEncoding="UTF-8" />
			
			<%-- Remove namespaces from the XML and create the DOM --%>
			<x:transform xslt="${removeNamespacesXsl}" xml="${odlXmlOutput}" var="results"/>	
			
			<%-- Determine the number of results, which is empty if there are none --%>
			<c:set var="numResults">
				<x:out select="$results//*[local-name()='OAI-PMH']/*[local-name()='ListRecords']/*[local-name()='resumptionToken']/@completeListSize"/>	
			</c:set>
			
			<%-- Check OAI error code and message: --%>
			<c:set var="errorCode">
				<x:out select="$results//*[local-name()='OAI-PMH']/*[local-name()='error']/@code"/>	
			</c:set>
			<c:set var="errorMsg">
				<x:out select="$results//*[local-name()='OAI-PMH']/*[local-name()='error']"/>	
			</c:set>
			
			<%-- Configure the keywords input to highlight --%>
			<dds:setKeywordsHighlight keywords='<%= request.getParameter("q") %>' highlightColor='#676cab' />
			
			</c:catch>
		</c:if>
		
		<%-- Display an error message if appropriate --%>		
		<c:if test="${not empty serviceError || fn:startsWith(errorCode,'bad')}">
			<div style="padding-left:4px">
				<p><b>There was an internal server error. Please try again later.</b></p>
				<!--
				Problem: 
					Unable to parse the ODL service response to this request: 
					${odlRequest}
					
				Error message:
					${serviceError} ${errorCode} ${errorMsg}
					
				Stack trace:
				<c:forEach items="${serviceError.stackTrace}" var="trace">
					${trace}
				</c:forEach>
				 -->				
			</div>
		</c:if>
				
		<%-- Display a message if there are no results or no keyword input --%>
		<c:if test="${empty numResults}">
		<c:if test="${searchTerms != null}">  
			<table  width="100%" cellpadding="4" cellspacing="0">
				<tr>
					<td>
						<c:choose>
						 <c:when test="${empty fn:trim(searchTerms) && empty fn:trim(selectedSetSpec)}">  
								You did not define a search.
								<x:choose>
									<x:when select="count($setNodes) > 0">
										Please type one or more keywords and/or 
										choose a set from the list and click search.
									</x:when>
									<x:otherwise>
										Please type one or more keywords and click search.
									</x:otherwise>
								</x:choose>
						 </c:when>
						 <c:otherwise>
							<dds:keywordsHighlight truncateString="true">
								<c:choose>
									<c:when test="${not empty fn:trim(searchTerms) && not empty fn:trim(selectedSetSpec)}">
										Your search for <c:out value="${searchTerms}"/> in set ${setName} had no matches.
									</c:when>
									<c:when test="${not empty fn:trim(searchTerms) && empty fn:trim(selectedSetSpec)}">
										Your search for <c:out value="${searchTerms}"/> had no matches.
									</c:when>
									<c:when test="${empty fn:trim(searchTerms) && not empty fn:trim(selectedSetSpec)}">
										Your search for set ${setName} had no matches.
									</c:when>									
									<c:otherwise>
										Your search had no matches.
									</c:otherwise>
								</c:choose>								
							</dds:keywordsHighlight>					  
						 </c:otherwise>
						</c:choose>
					</td>
				</tr>
			</table>
		</c:if>
		</c:if>
		
		
		<%-- Display the search results and pager if there are matching records --%>
		<c:if test="${numResults > 0}">
		
		<%-- Construct the web service request URL to get the next page of results --%>
		<c:url value="" var="nextResultsUrl">
			<c:param name="q" value="${searchTerms}"/>
			<c:param name="s" value="${s + numToReturn}"/>
			<c:if test="${not empty selectedSetSpec}">
				<c:param name="setSpec" value="${selectedSetSpec}"/>
			</c:if>			
		</c:url>
		<%-- Construct the web service request URL to get the previous page of results --%>
		<c:url value="" var="prevResultsUrl">
			<c:param name="q" value="${searchTerms}"/>
			<c:param name="s" value="${s - numToReturn}"/>
			<c:if test="${not empty selectedSetSpec}">
				<c:param name="setSpec" value="${selectedSetSpec}"/>
			</c:if>
		</c:url>
		
		<%-- Create the HTML for the pager, stored in a variable called 'pager' --%>
		<c:set var="pager">
			<nobr>
			<c:if test="${(s - numToReturn) >= 0}">
				<a href='<c:out value="${prevResultsUrl}"/>'>&lt;&lt; Prev</a>
			</c:if>
			<c:if test="${( (s - numToReturn) >= 0 ) && ( (s + numToReturn) < numResults )}">
				&nbsp; - &nbsp;
			</c:if>				
			<c:if test="${(s + numToReturn) < numResults}">
				<a href='<c:out value="${nextResultsUrl}"/>'>Next &gt;&gt;</a>
			</c:if>
			</nobr>
			<br/>
			Showing results <fmt:formatNumber type="number" value="${s +1}" />
			through
			<c:if test="${s + numToReturn > numResults}">
				<fmt:formatNumber type="number" value="${numResults}" />
			</c:if>
			<c:if test="${s + numToReturn <= numResults}">
				<fmt:formatNumber type="number" value="${s + numToReturn}" />
			</c:if>						
			out of <fmt:formatNumber type="number" value="${numResults}" />				
		</c:set>
		
		<table  width="100%" cellpadding="4" cellspacing="0">
			<%-- Pager top --%>
			<tr valign="bottom">
				<td>
					<dds:keywordsHighlight truncateString="true">
						<c:choose>
							<c:when test="${not empty fn:trim(searchTerms) && not empty fn:trim(selectedSetSpec)}">
								Your search for <c:out value="${searchTerms}"/> in set ${setName} had <fmt:formatNumber type="number" value="${numResults}" /> matches.
							</c:when>
							<c:when test="${not empty fn:trim(searchTerms) && empty fn:trim(selectedSetSpec)}">
								Your search for <c:out value="${searchTerms}"/> had <fmt:formatNumber type="number" value="${numResults}" /> matches.
							</c:when>
							<c:when test="${empty fn:trim(searchTerms) && not empty fn:trim(selectedSetSpec)}">
								Your search for set ${setName} had <fmt:formatNumber type="number" value="${numResults}" /> matches.
							</c:when>									
							<c:otherwise>
								Your search had <fmt:formatNumber type="number" value="${numResults}" /> matches.
							</c:otherwise>
						</c:choose>						
					</dds:keywordsHighlight>
				</td>
				<td align="right" valign="bottom">
					<c:out value="${pager}" escapeXml="false" />
					&nbsp;
				</td>		
			</tr>
		
			<tr>
				<td colspan="2">
					<table border="0" cellspacing="0" cellpadding="0" width="100%" height="1" hspace="0">
						<td bgcolor="#999999" height="1"></td>
					</table>	
				</td>
			</tr>
			
			<%-- Display the search results --%>
			<x:forEach select="$results//*[local-name()='OAI-PMH']/*[local-name()='ListRecords']/*[local-name()='record']/*[local-name()='metadata']/*[local-name()='dc']">
		
			<%-- Show the title and URL --%>
			<tr class="resourceHeading"> 
				<td colspan="2" class="resourceHeading">
					<c:set var="url">
						<x:out select="*[local-name()='identifier'][starts-with(text(),'http')] | *[local-name()='identifier'][starts-with(text(),'ftp')]"/>
					</c:set>
					<c:if test="${empty url}">
						<div class="blackbold">
							<x:out select="*[local-name()='title']"  escapeXml="false" />
						</div>
					</c:if>
					<c:if test="${not empty url}">
						<%-- Limit the URL that is displayed to a set length --%>
						<c:set var="urlLength">
							<x:out select="string-length(*[local-name()='identifier'])"/>
						</c:set>
						<c:if test="${urlLength+0 > maxUrlLength+0}">
							<c:set var="urlDisplay">
								<x:out select="substring($url,1,$maxUrlLength)"/> ...
							</c:set>
						</c:if>
						<c:if test="${urlLength+0 <= maxUrlLength+0}">
							<c:set var="urlDisplay">
								<x:out select="$url"/>
							</c:set>
						</c:if>			
						<a href='<c:out value="${url}" />' class="blackul"><x:out select="*[local-name()='title']"  escapeXml="false"  /></a>
						<br><a href='<c:out value="${url}" />'><c:out value="${urlDisplay}" /></a>
					</c:if>			
				</td>					
			</tr>
			
			<%-- Show the description(s) --%>	
			<tr> 
				<td colspan="2">
					<%-- Construct a URL to pull up the full description --%>
					<c:url value="" var="fullDescription">
						<c:param name="q" value="${searchTerms}"/>
						<c:if test="${not empty selectedSetSpec}">
							<c:param name="setSpec" value="${selectedSetSpec}"/>
						</c:if>						
						<c:set var="id">
							<x:out select="../../*[local-name()='header']/*[local-name()='identifier']" />
						</c:set>
						<c:param name="fd" value="${id}"/>
					</c:url>				
					<c:set var="description">
						<x:out select="*[local-name()='description']"/>
					</c:set>			
					<c:if test="${empty description}">
						<div class="elementBody">
							See: <nobr><a href='<c:out value="${fullDescription}"/>'>Full description</a>.</nobr>
						</div>
					</c:if>			
					<c:if test="${not empty description}">
						<div class="elementDescription">
							<dds:keywordsHighlight truncateString="true">
								<c:out value="${description}" escapeXml="false" />
							</dds:keywordsHighlight>
							<nobr><a href='<c:out value="${fullDescription}"/>'>Full description</a>.</nobr>
						</div>						
						<%-- Un-comment below to show all descriptions. --%>
						<%-- <x:forEach select="*[local-name()='description']">
							<div class="elementDescription">
								<dds:keywordsHighlight truncateString="true">
									<x:out select="." />
								</dds:keywordsHighlight>
							</div>
						</x:forEach>  --%>
					</c:if>								
				</td>					
			</tr>	
			</x:forEach>
		
			<tr>
				<td colspan="2">
					<table border="0" cellspacing="0" cellpadding="0" width="100%" height="1" hspace="0">
						<td bgcolor="#999999" height="1"></td>
					</table>	
				</td>
			</tr>	
			
			<%-- Pager bottom --%>
			<tr>
				<td>
					&nbsp;
				</td>
				<td align="right">
					<c:out value="${pager}" escapeXml="false" />
					&nbsp;
				</td>		
			</tr>
		</table>		
		</c:if><%-- End display of results --%>
	</c:otherwise>
</c:choose>

<script language="JavaScript">
	// Set the focus to the search box:
	if(document.searchBox && document.searchBox.q) {
		document.searchBox.q.focus();
	}
</script>

<%-- -------END SEARCH CODE------- --%>

<%-- Add HTML at the end if desired --%>

