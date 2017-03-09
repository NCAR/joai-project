<%-- 
	This is the header for all oai requests. The format of the request tag
	differs if there are errors or not.
	Note: all library imports are inherited from the page that includes this 
--%>
<bean:message key="oaipmh.xmldeclaration"/>
<bean:message key="oaipmh.rootopen"/>
<responseDate><dt:timeZone id="tz">utc</dt:timeZone><dt:format pattern="yyyy-MM-dd'T'HH:mm:ss'Z'" timeZone="tz"><dt:currentTime/></dt:format></responseDate>
<%=providerBean.getOAIRequestTag(request)%>

