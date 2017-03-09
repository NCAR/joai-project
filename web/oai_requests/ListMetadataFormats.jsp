@BLANK@<?xml version="1.0" encoding="UTF-8" ?><%@ page contentType="text/xml; charset=UTF-8" %><jsp:useBean id="rf" class="org.dlese.dpc.repository.action.form.RepositoryForm" scope="request"/><%@ include file="../TagLibIncludes.jsp" %>
<%-- Begin OAI head tag --%>
<bean:write name="rf" property="rootOpenTag" filter="false"/>
<responseDate><dt:timeZone id="tz">utc</dt:timeZone><dt:format pattern="yyyy-MM-dd'T'HH:mm:ss'Z'" timeZone="tz"><dt:currentTime/></dt:format></responseDate>
<%=rf.getOAIRequestTag(request)%>
<%-- End OAI head tag --%>
<ListMetadataFormats><c:forEach items="${rf.metadataFormats}" var="metadataFormat">
  <metadataFormat><c:set var="metadataPrefix" value="${metadataFormat.value.metadataPrefix}"/>
    <metadataPrefix>${metadataPrefix}</metadataPrefix>
    <schema>${rf.metadataSchemaURLs[metadataPrefix]}</schema>
    <metadataNamespace>${rf.metadataNamespaces[metadataPrefix]}</metadataNamespace>
  </metadataFormat></c:forEach>
</ListMetadataFormats>

<bean:write name="rf" property="rootCloseTag" filter="false"/>

<%-- Set the response type --%>
<resp:setContentType>text/xml; charset=UTF-8</resp:setContentType>
<req:equalsParameter name="rt" match="text">
  <resp:setContentType>text/plain; charset=UTF-8</resp:setContentType>
</req:equalsParameter>
<req:equalsParameter name="rt" match="validate">
  <resp:setContentType>text/html; charset=UTF-8</resp:setContentType>
</req:equalsParameter> 


