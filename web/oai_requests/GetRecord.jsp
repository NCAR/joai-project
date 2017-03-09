@BLANK@<?xml version="1.0" encoding="UTF-8" ?><%@ page contentType="text/xml; charset=UTF-8" %><jsp:useBean id="rf" class="org.dlese.dpc.repository.action.form.RepositoryForm" scope="request"/><%@ page language="java" %><%@ taglib uri="/WEB-INF/tlds/struts-bean.tld" prefix="bean" %><%@ taglib uri="/WEB-INF/tlds/struts-html.tld" prefix="html" %><%@ taglib uri="/WEB-INF/tlds/struts-logic.tld" prefix="logic" %><%@ taglib uri="/WEB-INF/tlds/response.tld" prefix="resp" %><%@ taglib uri="/WEB-INF/tlds/request.tld" prefix="req" %><%@ taglib uri="/WEB-INF/tlds/datetime.tld" prefix="dt" %>

<%-- Begin OAI head tag --%>

<bean:write name="rf" property="rootOpenTag" filter="false"/>

<responseDate><dt:timeZone id="tz">utc</dt:timeZone><dt:format pattern="yyyy-MM-dd'T'HH:mm:ss'Z'" timeZone="tz"><dt:currentTime/></dt:format></responseDate>

<%=rf.getOAIRequestTag(request)%>

<%-- End OAI head tag --%>

<GetRecord>

  <record>

  

    <logic:notMatch name="rf" property="deletedStatus" value="true"><header></logic:notMatch><logic:match name="rf" property="deletedStatus" value="true"><header status="deleted"></logic:match>

      <identifier><bean:write name="rf" property="oaiIdPfx" filter="true"/><bean:write name="rf" property="identifier" filter="true"/></identifier>

      <datestamp><bean:write name="rf" property="datestamp" filter="true"/></datestamp>

      <logic:iterate name="rf" property="setSpecs" 

	  id="setSpec" ><setSpec><bean:write name="setSpec" filter="true"/></setSpec> </logic:iterate>	

    </header>

	

    <logic:notMatch name="rf" property="deletedStatus" value="true"><metadata>

	<bean:write name="rf" property="record" filter="false"/>	

    </metadata></logic:notMatch>

  </record>

</GetRecord>



<bean:write name="rf" property="rootCloseTag" filter="false"/>



<%-- Set the response type --%>

<resp:setContentType>text/xml; charset=UTF-8</resp:setContentType>

<req:equalsParameter name="rt" match="text">

  <resp:setContentType>text/plain; charset=UTF-8</resp:setContentType>

</req:equalsParameter>

<req:equalsParameter name="rt" match="validate">

  <resp:setContentType>text/html; charset=UTF-8</resp:setContentType>

</req:equalsParameter> 



