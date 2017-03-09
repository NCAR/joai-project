<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:d="http://www.dlese.org/"
    xmlns:dc="http://purl.org/dc/elements/1.1/"  
    exclude-result-prefixes="d"
    version="1.0">

	<xsl:output method="xml" encoding="UTF-8"/>

	<xsl:variable name="newline">
	<xsl:text>
	</xsl:text>
	</xsl:variable>

	<xsl:template match="*|/">
	     <xsl:apply-templates/>
	</xsl:template> 
  
    <xsl:template match="*|/">
         <xsl:copy>
             <xsl:for-each select="@*">
             <!-- to preserve attributes in the input xml document; so that they appear in the output xml documet; e.g. URI -->
                 <xsl:copy/>
             </xsl:for-each>
             <xsl:apply-templates/>
         </xsl:copy>
     </xsl:template>

</xsl:stylesheet>
