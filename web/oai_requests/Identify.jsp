@BLANK@<?xml version="1.0" encoding="UTF-8" ?><%@ page contentType="text/xml; charset=UTF-8" %><jsp:useBean id="rf" class="org.dlese.dpc.repository.action.form.RepositoryForm" scope="request"/><%@ page language="java" %><%@ taglib uri="/WEB-INF/tlds/struts-bean.tld" prefix="bean" %><%@ taglib uri="/WEB-INF/tlds/struts-html.tld" prefix="html" %><%@ taglib uri="/WEB-INF/tlds/struts-logic.tld" prefix="logic" %><%@ taglib uri="/WEB-INF/tlds/response.tld" prefix="resp" %><%@ taglib uri="/WEB-INF/tlds/request.tld" prefix="req" %><%@ taglib uri="/WEB-INF/tlds/datetime.tld" prefix="dt" %>

<%-- Begin OAI head tag --%>

<bean:write name="rf" property="rootOpenTag" filter="false"/>

<responseDate><dt:timeZone id="tz">utc</dt:timeZone><dt:format pattern="yyyy-MM-dd'T'HH:mm:ss'Z'" timeZone="tz"><dt:currentTime/></dt:format></responseDate>

<%=rf.getOAIRequestTag(request)%>

<%-- End OAI head tag --%>

<Identify>

<repositoryName><bean:write name="rf" property="repositoryName"/></repositoryName>

<baseURL><bean:write name="rf" property="baseURL"/></baseURL>

<protocolVersion><bean:write name="rf" property="protocolVersion"/></protocolVersion>

<logic:notEmpty name="rf" property="adminEmails"><logic:iterate id="adminEmail" name="rf" property="adminEmails"><adminEmail><bean:write name="adminEmail" filter="false" /></adminEmail>

</logic:iterate></logic:notEmpty>

<earliestDatestamp><bean:write name="rf" property="earliestDatestamp"/></earliestDatestamp>

<deletedRecord><bean:write name="rf" property="deletedRecord"/></deletedRecord>

<granularity><bean:write name="rf" property="granularity"/></granularity>	

<%-- Iterate over the error code ArrayList --%>

<logic:notEmpty name="rf" property="compressions"><logic:iterate name="rf" property="compressions" id="compression" >

<compression><bean:write name="compression"/></compression></logic:iterate></logic:notEmpty>

<logic:notEmpty name="rf" property="repositoryIdentifier"><description>

<oai-identifier 

   xmlns="http://www.openarchives.org/OAI/2.0/oai-identifier"

   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"

   xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai-identifier

   https://www.openarchives.org/OAI/2.0/oai-identifier.xsd">

  <scheme>oai</scheme> 

  <repositoryIdentifier><bean:write name="rf" property="repositoryIdentifier" filter="false"/></repositoryIdentifier>    

  <delimiter>:</delimiter> 

  <sampleIdentifier>oai:<bean:write name="rf" property="repositoryIdentifier" filter="false"/>:<bean:write name="rf" property="exampleID" filter="false"/></sampleIdentifier>

</oai-identifier>

</description></logic:notEmpty>

<logic:notEmpty name="rf" property="descriptions">

<description>

	<oai_dc:dc xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ https://www.openarchives.org/OAI/2.0/oai_dc.xsd">

		<logic:iterate name="rf" property="descriptions" id="description" ><dc:description><bean:write name="description" filter="true"/></dc:description></logic:iterate>

	</oai_dc:dc>

</description></logic:notEmpty>

</Identify>

<bean:write name="rf" property="rootCloseTag" filter="false"/>



<%-- Set the response type --%>

<resp:setContentType>text/xml; charset=UTF-8</resp:setContentType>

<req:equalsParameter name="rt" match="text">

  <resp:setContentType>text/plain; charset=UTF-8</resp:setContentType>

</req:equalsParameter>

<req:equalsParameter name="rt" match="validate">

  <resp:setContentType>text/html; charset=UTF-8</resp:setContentType>

</req:equalsParameter> 



