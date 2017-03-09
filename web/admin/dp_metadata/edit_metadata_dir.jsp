<%@ include file="../../TagLibIncludes.jsp" %><head>
  
<c:set var="title">
	<c:choose>
		<c:when test="${not empty param.edit}">
			Edit Metadata Directory
		</c:when>
		<c:otherwise>
			Add Metadata Directory
		</c:otherwise>
	</c:choose>
</c:set>

    
<c:set var="rm" value="${applicationScope.repositoryManager}"/>
<html:html>
  <title>${title}</title>
    <html:base />


	<%-- Include style/menu templates --%>
	<%@ include file="../../head.jsp" %>

	
	<%-- This generate JavaScript for insert into the <head> section of the edit and add metadata pages --%>
	<script type="text/javascript">
	<!--
		var schemas = new Array();		
		<c:forEach items="${rm.metadataSchemaURLs}" 
			var="schema">schemas['${schema.key}'] = '${schema.value}'; 
		</c:forEach>
		var namespaces = new Array();
		<c:forEach items="${rm.metadataNamespaces}" 
			var="namespaces">namespaces['${namespaces.key}'] = '${namespaces.value}'; 
		</c:forEach>
		
		function setInitial(){
			if( !document.getElementById( 'dirMetadataFormat' ) )
				return;
			initialMetaFormat = document.getElementById( 'dirMetadataFormat' ).value;
		}
		
		function insertVals(){
			if( !document.getElementById( 'dirMetadataFormat' ) )
				return;		
				
			var metaFormat = trim( document.getElementById( 'dirMetadataFormat' ).value );
				
			var schema = schemas[metaFormat];
			var namespace = namespaces[metaFormat];
			/* if(schema == null)
				schema = '';
			if(namespace == null)
				namespace = ''; */
				
			if( schema != null && (document.getElementById( 'metadataSchema' ).value == '' || initialMetaFormat != metaFormat) )
				document.getElementById( 'metadataSchema' ).value = schema;	
			if( namespace != null && (document.getElementById( 'metadataNamespace' ).value == '' || initialMetaFormat != metaFormat) )
				document.getElementById( 'metadataNamespace' ).value = namespace;
			
			updateNs();
		}
		
		function updateNs() {
			if( !document.getElementById( 'dirMetadataFormat' ) 
				|| !document.getElementById( 'metadataSchema' )	
				|| !document.getElementById( 'metadataNamespace' )
				|| !document.getElementById( 'ns_action' ) )
				return;
			
			var metadataSchema = document.getElementById( 'metadataSchema' ).value;
			var metadataNamespace = document.getElementById( 'metadataNamespace' ).value;
			var metaFormat = document.getElementById( 'dirMetadataFormat' ).value;
			if( metaFormat == '' )
				metaFormat = 'this format';
			else
				metaFormat = 'the <i>' + metaFormat + '</i> format';
			
				
			if( metadataSchema == '' && metadataNamespace == '' ) {
				document.getElementById( 'ns_action' ).innerHTML = 'Supply the metadata namespace and schema for ' + metaFormat + ':';
			}
			else if ( metadataNamespace == '' ) {
				document.getElementById( 'ns_action' ).innerHTML = 
					'Supply the metadata namespace and verify the schema that is currently defined (edit if necessary) for ' + metaFormat + ':';
			}
			else if ( metadataSchema == '' ) {
				document.getElementById( 'ns_action' ).innerHTML = 
					'Verify the metadata namespace that is currently defined (edit if necessary) and supply the schema for ' + metaFormat + ':';
			}
			else {
				document.getElementById( 'ns_action' ).innerHTML = 
					'Verify the metadata namespace and schema that is currently defined for ' + metaFormat + ' (edit if necessary):';	
			}
		}
	-->
	</script>
	
  </head>
  <body onLoad="updateNs();">
  
<%-- Include style/menu templates --%>
<c:import url="../../top.jsp?sec=provider" />
  
    <h1>${title}</h1>

	<noscript>
		<p>Note: This page uses JavaScript for certain features. 
			Please enable JavaScript in your
			browser to take advantage of these features.</p>
	</noscript> 
	
	<logic:messagesPresent>
		<p><font color="red"><b>There was an error. Please correct the problem below:</b></font></p>
	</logic:messagesPresent>
	
	
	<%-- Note: To access a propery in a DynaValidatorForm, use: ${metadataDirsForm.map.dirNickname} --%>
	
	<c:set var="setInfo" value="${rm.setInfosHashMap[param.dirPath]}" />
    
	<html:form action="admin/metadata_dir-validate" method="POST" focus="dirNickname" onsubmit="insertVals()" onreset="insertVals()">

	  <p>
	    <c:choose>
	      <c:when test="${not empty param.edit}">
	        <input type="hidden" name="edit" value="${param.edit}"/>
	        <input type="hidden" name="command" value="updateMetadataDir"/>
          </c:when>
	      <c:otherwise>
	        <input type="hidden" name="command" value="addMetadataDir"/>
          </c:otherwise>
        </c:choose>
	   
	
            <p>Complete the required fields of nickname, format and path in order to add metadata files to the repository. <p>
            <em>Note</em>: Editing the namespace and schema location fields affects <strong>all</strong> directory entries using the same namespace and schema. </p>
         
 
	  <table id="form" cellpadding="6" cellspacing="1" border="0">
		<tr id="headrow">
			<td>
				<b>Required Directory information</b>			</td>
		</tr>			
		<tr id="formrow">
			<td>
				<div><b>Nickname for these files:</b> <a href="javascript:popupHelp('<c:out value="${pageContext.request.contextPath}"/>','metdir_nickname')" class="helpp">   <img src='<c:out value="${pageContext.request.contextPath}" />/images/help.gif' border=0></a></div>
				<div><html:text property="dirNickname" size="80" maxlength="80" value="${empty metadataDirsForm.dirNickname ? setInfo.name : metadataDirsForm.dirNickname}"/></div>
				<font color="red"><html:errors property="dirNickname"/></font>			</td>
		</tr>
		<tr id="formrow">
			<td>
				<div><b>Format of files:</b> <a href="javascript:popupHelp('<c:out value="${pageContext.request.contextPath}"/>','dm_format')" class="helpp">   <img src='<c:out value="${pageContext.request.contextPath}" />/images/help.gif' border=0></a></div>

			
				<div>
					<html:text 	property="dirMetadataFormat" 
								value="${empty metadataDirsForm.dirMetadataFormat ? setInfo.format : metadataDirsForm.dirMetadataFormat}" 
								size="15" 
								maxlength="15" 
								styleId="dirMetadataFormat" 
								onfocus="setInitial()" 
								onkeyup="insertVals()"
								onblur="insertVals()"
								onchange="insertVals()"/>
				</div>
				<font color="red"><html:errors property="dirMetadataFormat"/></font>
			</td>
		</tr>		
		
		<tr id="formrow">
			<td>
				<div><b>Path to the directory:</b> <a href="javascript:popupHelp('<c:out value="${pageContext.request.contextPath}"/>','metdir_path')" class="helpp">   <img src='<c:out value="${pageContext.request.contextPath}" />/images/help.gif' border=0></a></div>
				<div><b><html:text property="dirPath" size="80" value="${empty metadataDirsForm.dirPath ? setInfo.directory : metadataDirsForm.dirPath}"/></b></div>
				<font color="red"><html:errors property="dirPath"/></font>			</td>
		</tr>
	</table>
	
	<table id="form" cellpadding="6" cellspacing="1" border="0" style="margin-top:8px">
		<tr id="headrow">
			<td>
				<b><div id="ns_action">Supply the metadata namespace and schema for this format:</div></b>			</td>
		</tr>		
		
		<tr id="formrow">
			<td>
				<div><b>Metadata namespace:</b> <a href="javascript:popupHelp('<c:out value="${pageContext.request.contextPath}"/>','metdir_namespace')" class="helpp">   <img src='<c:out value="${pageContext.request.contextPath}" />/images/help.gif' border=0></a></div>
				<div><html:text property="metadataNamespace" value="${empty metadataDirsForm.metadataNamespace ? rm.metadataNamespaces[setInfo.format] : metadataDirsForm.metadataNamespace}" size="80" styleId="metadataNamespace"/></div>
				<font color="red"><html:errors property="metadataNamespace"/></font>			</td>
		</tr>
		<tr id="formrow">
			<td>
				<div><b>Metadata schema:</b> <a href="javascript:popupHelp('<c:out value="${pageContext.request.contextPath}"/>','metdir_schema')" class="helpp">   <img src='<c:out value="${pageContext.request.contextPath}" />/images/help.gif' border=0></a></div>
				<div><html:text property="metadataSchema" value="${empty metadataDirsForm.metadataSchema ? rm.metadataSchemaURLs[setInfo.format] : metadataDirsForm.metadataSchema}" size="80" styleId="metadataSchema"/></div>
				<font color="red"><html:errors property="metadataSchema"/></font>			</td>
		</tr>
	</table>
		
	<table cellpadding="6" cellspacing="1" border="0">		
		<tr>
			<td>&nbsp;		  </td>
		</tr>		
		<tr>
			<td>		
				  <html:submit>Save</html:submit>
				  <%-- <html:reset>Reset</html:reset> --%>
				  <html:cancel>Cancel</html:cancel>			</td>
	  </tr>
	</table>
 </html:form>

<%-- Include style/menu templates --%>
<%@ include file="../../bottom.jsp" %>  

  </body>
</html:html>
