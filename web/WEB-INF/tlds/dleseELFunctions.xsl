<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet 	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
					xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
					xmlns:el="http://java.sun.com/xml/ns/j2ee"
					xmlns:fn="http://www.w3.org/2005/02/xpath-functions"
					version="1.0">
  <xsl:output method="html" version="4.0"/>

  <xsl:template match="/el:taglib">
    <html>
      <head>
        <title><xsl:value-of select="el:display-name"/></title>
		<style type="text/css">
            code {
              font-size: 10pt;
			  font-family: courier;
            }
			.specs {
				padding-left:30px;
			}
        </style>
      </head>
      <body>
	  	<h1><xsl:value-of select="el:display-name"/></h1>
		<p><xsl:value-of select="el:description"/></p>
		
		<p>
			To use the EL functions, include the jOAI Java class library in the web applicatation classpath.
			Then reference the functions library in JSP pages using the following directive:
			<div style="padding:8px">
				<nobr><code>&lt;%@ taglib prefix=&quot;f&quot; uri=&quot;http://www.dlese.org/dpc/dds/tags/dleseELFunctions&quot; %&gt;</code></nobr>
			</div>
		</p>
		<p>
			See the 
			<a href="http://docs.oracle.com/javaee/1.4/tutorial/doc/JSTL.html" target="_blank">JavaServer Pages Standard Tag Library (JSTL) Tutorial</a>.
		</p>		
		<br/>
		<table border="1" width="100%" cellpadding="3" cellspacing="0" summary="">
			<tr bgcolor="#ccccff" class="TableHeadingColor">
				<th align="left" colspan="2">		
					<font size="+2">Functions</font>
				</th>
			</tr>
		</table>
		<hr height="1"/>
		<xsl:apply-templates select="el:function"/>		
		<br/><br/>
      </body>
    </html>
  </xsl:template>
  
	<xsl:template match="el:function">
		<div style="padding-bottom:8px">
			<h3><xsl:value-of select="el:name"/></h3>
			<p><xsl:value-of select="el:description"/></p>
			<div class="specs">
				<xsl:if test="string-length(el:example) > 0">
					<p>
						<b>Example JSP:</b> 
							<div class="specs">	
								<code><xsl:value-of select="el:example"/></code>
							</div>
					</p>
				</xsl:if>
				<p>
					<b>Function is mapped to:</b>
						<div class="specs">
							Method: 
							<div class="specs">
									<xsl:choose>
										<xsl:when test="starts-with(el:function-class,'org.dlese.dpc')">
											<xsl:element name="a">
												<xsl:attribute name="href"><xsl:value-of select="translate(el:function-class,'.','/')"/>.html#<xsl:value-of select="substring-after(normalize-space(el:function-signature),' ')"/></xsl:attribute>
												<xsl:value-of select="normalize-space(el:function-signature)"/>
											</xsl:element>
										</xsl:when>
										<xsl:otherwise>
											<xsl:value-of select="el:function-signature"/>
										</xsl:otherwise>
									</xsl:choose>
							</div>
							In class:
							<div class="specs">
								<xsl:value-of select="el:function-class"/>											
							</div>							
						</div>
				</p>
			</div>
		</div>
		<hr height="1"/>
	</xsl:template>

</xsl:stylesheet>
