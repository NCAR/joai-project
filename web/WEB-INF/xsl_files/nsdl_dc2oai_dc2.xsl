<?xml version="1.0" encoding="UTF-8"?>
<!-- =============================================================  
 "nsdl_dc2oai_dc2" stylesheet 

Transform nsdl_dc v1.0.2 metadata to oai_dc by dumbing down Qualified Dublin Core Refinements to their equivalent simple DC elements; leaving the simple DC elements alone, and dropping all other elements. Uses the substitutionGroup attributes from the XML Schema to find the equivalent simple DC elements for the refined elements.

This is an alternate transform based on  Tom Habing's stylesheet at http://dli.grainger.uiuc.edu/publications/metadatacasestudy/dc_schemas/DCDD2.xsl
	<dc:title>DCDD2.xsl, thabing@uiuc.edu</dc:title>
	<dc:creator>Tom Habing</dc:creator>
	<dc:date>2002-07-08</dc:date>

Author:  Naomi Dushay 
Created: 03/17/06  
================================================================ -->
<xsl:stylesheet version="1.0" 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
	xmlns:xs="http://www.w3.org/2001/XMLSchema" 
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xmlns:nsdl_dc="http://ns.nsdl.org/nsdl_dc_v1.02/" 
	xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" 
	xmlns:dc="http://purl.org/dc/elements/1.1/" 
	xmlns:dct="http://purl.org/dc/terms/" 
	xmlns:ieee="http://www.ieee.org/xsd/LOMv1p0" 
	
	exclude-result-prefixes="#default xsi nsdl_dc oai_dc dct ieee">

	<xsl:output method="xml" encoding="UTF-8" indent="yes" media-type="text/xml"/>

	<xsl:variable name="DCTERMS_SCHEMA" select="document('http://ns.nsdl.org/schemas/dc/dcterms_v1.01.xsd')"/>
	
	<xsl:template match="nsdl_dc:nsdl_dc">
    		<oai_dc:dc xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd">
			 <xsl:apply-templates select="./*" />
		</oai_dc:dc>
	</xsl:template>

	<xsl:template match="dct:*">
		<xsl:if test="$DCTERMS_SCHEMA//xs:element[@name=local-name(current())]">
			<xsl:variable name="substitutionGroup" select="$DCTERMS_SCHEMA//xs:element[@name=local-name(current())]/@substitutionGroup" />
			<xsl:if test="$substitutionGroup != 'dc:any' and starts-with($substitutionGroup, 'dc')">
				<xsl:element namespace="http://purl.org/dc/elements/1.1/" name="{$substitutionGroup}">
					<xsl:value-of select="."/>
				</xsl:element>
			</xsl:if>
		</xsl:if>
	</xsl:template>

	<xsl:template match="dc:*">
<!-- Note: this approach outputs dct and nsdl_dc namespace declarations in XMLSpy
		<xsl:copy>
			<xsl:value-of select="."/>
		</xsl:copy>
-->
		<xsl:element name="dc:{local-name()}">
          	<xsl:value-of select="."/>
          </xsl:element>
	</xsl:template>

	<!-- no mapping from nsdl_dc's ieee elements to Simple DC elements -->
	<xsl:template match="ieee:*">
	</xsl:template>


</xsl:stylesheet>
