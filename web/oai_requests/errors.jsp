@BLANK@<?xml version="1.0" encoding="UTF-8" ?><jsp:useBean id="rf" class="org.dlese.dpc.repository.action.form.RepositoryForm" scope="request"/><%@ page language="java" %><%@ taglib uri="/WEB-INF/tlds/struts-bean.tld" prefix="bean" %><%@ taglib uri="/WEB-INF/tlds/struts-html.tld" prefix="html" %><%@ taglib uri="/WEB-INF/tlds/struts-logic.tld" prefix="logic" %><%@ taglib uri="/WEB-INF/tlds/response.tld" prefix="resp" %><%@ taglib uri="/WEB-INF/tlds/request.tld" prefix="req" %><%@ taglib uri="/WEB-INF/tlds/datetime.tld" prefix="dt" %>
<%-- Begin OAI head tag --%>
<bean:write name="rf" property="rootOpenTag" filter="false"/>
<responseDate><dt:timeZone id="tz">utc</dt:timeZone><dt:format pattern="yyyy-MM-dd'T'HH:mm:ss'Z'" timeZone="tz"><dt:currentTime/></dt:format></responseDate>
<%=rf.getOAIRequestTag(request)%>
<logic:iterate name="rf" property="errors" id="error" >    <error code="<bean:write name="error" property="errorCode"/>"><bean:write name="error" property="message" filter="true"/></error>
</logic:iterate>
<bean:write name="rf" property="rootCloseTag" filter="false"/>

<%-- Set the response type --%> 
<resp:setContentType>text/xml; charset=UTF-8</resp:setContentType>
<req:equalsParameter name="rt" match="text">
  <resp:setContentType>text/plain; charset=UTF-8</resp:setContentType>
</req:equalsParameter>
<req:equalsParameter name="rt" match="validate">
  <resp:setContentType>text/html; charset=UTF-8</resp:setContentType>
</req:equalsParameter> 
