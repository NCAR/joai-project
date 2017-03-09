@BLANK@<?xml version="1.0" encoding="UTF-8" ?><%@ page contentType="text/xml; charset=UTF-8" %><jsp:useBean id="rf" class="org.dlese.dpc.repository.action.form.RepositoryForm" scope="request"/><%@ page language="java" %><%@ taglib uri="/WEB-INF/tlds/struts-bean.tld" prefix="bean" %><%@ taglib uri="/WEB-INF/tlds/struts-html.tld" prefix="html" %><%@ taglib uri="/WEB-INF/tlds/struts-logic.tld" prefix="logic" %><%@ taglib uri="/WEB-INF/tlds/response.tld" prefix="resp" %><%@ taglib uri="/WEB-INF/tlds/request.tld" prefix="req" %><%@ taglib uri="/WEB-INF/tlds/datetime.tld" prefix="dt" %><%@ include file="../TagLibIncludes.jsp" %>

<%-- Begin OAI head tag --%>

<bean:write name="rf" property="rootOpenTag" filter="false"/>

<responseDate><dt:timeZone id="tz">utc</dt:timeZone><dt:format pattern="yyyy-MM-dd'T'HH:mm:ss'Z'" timeZone="tz"><dt:currentTime/></dt:format></responseDate>

<%=rf.getOAIRequestTag(request)%>

<%-- End OAI head tag --%><x:parse var="listSetsConfXml" xml="${applicationScope.repositoryManager.listSetsConfigXml}"/>

<ListSets>

	<x:forEach select="$listSetsConfXml/ListSets/set" var="set">

		<set> <c:set var="setSpec"><x:out select="$set/setSpec"/></c:set>

			<setSpec>${setSpec}</setSpec>	

			<setName><x:out select="$set/setName"/></setName>

			<setDescription>

				<oai_dc:dc xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd">

					<x:forEach select="$set/setDescription/description"><dc:description><x:out select="."/></dc:description>

					</x:forEach><dc:description>This set contains <bean:write name="repositoryManager" property="numRecordsInSet(${setSpec})" filter="false"/> records</dc:description>

					<x:forEach select="$set/setDescription/identifier"><dc:identifier><x:out select="."/></dc:identifier>

					</x:forEach>

				</oai_dc:dc>

			</setDescription>

		</set>

	</x:forEach>

</ListSets>



<bean:write name="rf" property="rootCloseTag" filter="false"/>



<%-- Set the response type --%>

<resp:setContentType>text/xml; charset=UTF-8</resp:setContentType>

<req:equalsParameter name="rt" match="text">

  <resp:setContentType>text/plain; charset=UTF-8</resp:setContentType>

</req:equalsParameter>

<req:equalsParameter name="rt" match="validate">

  <resp:setContentType>text/html; charset=UTF-8</resp:setContentType>

</req:equalsParameter> 



