@BLANK@<?xml version="1.0" encoding="UTF-8" ?><%@ page contentType="text/xml; charset=UTF-8" %><jsp:useBean id="rf" class="org.dlese.dpc.repository.action.form.RepositoryForm" scope="request"/><%@ include file="../TagLibIncludes.jsp" %><%@ page import = "org.dlese.dpc.index.*" import = "org.dlese.dpc.index.reader.*" %>

<%-- Begin OAI head tag --%>

<bean:write name="rf" property="rootOpenTag" filter="false"/>

<responseDate><dt:timeZone id="tz">utc</dt:timeZone><dt:format pattern="yyyy-MM-dd'T'HH:mm:ss'Z'" timeZone="tz"><dt:currentTime/></dt:format></responseDate>

<%=rf.getOAIRequestTag(request)%>

<%-- End OAI head tag --%>

<ListRecords>

  <logic:iterate name="rf" property="results" id="result" offset="<%= rf.getResultsOffset() %>" length="<%= rf.getResultsLength() %>">

    <% 

		FileIndexingServiceDocReader rdr = (FileIndexingServiceDocReader)((ResultDoc)result).getDocReader();

		String metadata = "";

		if(!rdr.isDeleted())

			metadata = ((XMLDocReader)rdr).getXmlFormat(rf.getRequestedFormat(), true); 

		if(metadata!= null) 

	{ %>

  <record>

    <logic:notMatch name="result" property="docReader.deleted" value="true"><header></logic:notMatch><logic:match name="result" property="docReader.deleted" value="true"><header status="deleted"></logic:match>

      <identifier><bean:write name="rf" property="oaiIdPfx" filter="true"/><bean:write name="result" property="docReader.id" filter="true"/></identifier>

      <datestamp><bean:write name="result" property="docReader.oaiDatestamp" filter="true"/></datestamp>

	  <c:forEach items="${result.docReader.oaiSets}" 

	    var="set"> <setSpec>${set}</setSpec>

	  </c:forEach></header>

    <logic:notMatch name="result" property="docReader.deleted" value="true"><metadata>

    <%= metadata /* output the record */ %> 

    </metadata></logic:notMatch>

  </record><% } %></logic:iterate><bean:write name="rf" property="resumptionToken" filter="false"/>

</ListRecords>



<bean:write name="rf" property="rootCloseTag" filter="false"/> 



<%-- Set the response type --%>

<resp:setContentType>text/xml; charset=UTF-8</resp:setContentType>

<req:equalsParameter name="rt" match="text">

  <resp:setContentType>text/plain; charset=UTF-8</resp:setContentType>

</req:equalsParameter>

<req:equalsParameter name="rt" match="validate">

  <resp:setContentType>text/html; charset=UTF-8</resp:setContentType>

</req:equalsParameter> 





