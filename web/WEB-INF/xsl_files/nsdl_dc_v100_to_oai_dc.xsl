<?xml version="1.0" encoding="UTF-8"?>
<!-- =============================================================  
 "nsdl_dc_v100_to_oai_dc" stylesheet 

Transform nsdl_dc v1.0.0 metadata to oai_dc by dumbing down Qualified Dublin Core Refinements to their equivalent simple DC elements; leaving the simple DC elements alone, and dropping all other elements. Looks for explicitly named elements in nsdl_dc and xforms them.

Note that this transform will only work with the correct version of nsdl_dc (v1.0.0).

Authors:  Tim Cornwell and Naomi Dushay 
Created: 03/17/06  
================================================================ -->
<xsl:stylesheet version="1.0" 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
    xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/"
    xmlns:nsdl_dc="http://ns.nsdl.org/nsdl_dc_v1.00"
    xmlns:dc="http://purl.org/dc/elements/1.1/" 
    xmlns:dct="http://purl.org/dc/terms/"
    xmlns:ieee="http://www.ieee.org/xsd/LOMv1p0" 

    exclude-result-prefixes="#default nsdl_dc dct ieee">
     
	<xsl:output method="xml" encoding="UTF-8" indent="yes" media-type="text/xml" />

	<xsl:strip-space elements="*"/>

	<xsl:template match="nsdl_dc:nsdl_dc">
    		<oai_dc:dc xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd">
			 <xsl:apply-templates select="./*" />
		</oai_dc:dc>
	</xsl:template>

	<!-- simple dc elements that have no refinements (they may have encoding schemes, but that doesn't affect the element name -->
	<xsl:template match="dc:contributor | dc:creator | dc:language | dc:publisher | dc:source | dc:subject | dc:type">
<!-- Note: this approach outputs dct and nsdl_dc namespace declarations in XMLSpy
		<xsl:copy>
			<xsl:value-of select="."/>
		</xsl:copy>
-->
		<xsl:element name="dc:{local-name()}">
          	<xsl:value-of select="."/>
          </xsl:element>
	</xsl:template>

	<!-- simple dc elements that have refinements -->
	<!-- coverage -->
	<xsl:template match="dc:coverage | dct:spatial | dct:temporal">
		<xsl:element name="dc:coverage">
          	<xsl:value-of select="."/>
          </xsl:element>
	</xsl:template>
		
	<!-- date -->
	<xsl:template match="dc:date | dct:available | dct:created | dct:dateAccepted | dct:dateCopyrighted | dct:dateSubmitted | dct:issued | dct:modified | dct:valid">
		<xsl:element name="dc:date">
          	<xsl:value-of select="."/>
          </xsl:element>
	</xsl:template>
	
	<!-- description -->
	<xsl:template match = "dc:description | dct:abstract | dct:tableOfContents">
		<xsl:element name="dc:description">
          	<xsl:value-of select="."/>
          </xsl:element>
	</xsl:template>
	
	<!-- format -->
	<xsl:template match="dc:format | dct:extent | dct:medium">
		<xsl:element name="dc:format">
          	<xsl:value-of select="."/>
          </xsl:element>
	</xsl:template>
	
	<!-- identifier -->
	<xsl:template match="dc:identifier | dct:bibliographicCitation">
		<xsl:element name="dc:identifier">
          	<xsl:value-of select="."/>
          </xsl:element>
	</xsl:template>
	
	<!-- relation -->
	<xsl:template match="dc:relation | dct:conformsTo | dct:hasFormat | dct:hasPart | dct:hasVersion | dct:isFormatOf | dct:isPartOf | dct:isReferencedBy | dct:isReplacedBy | dct:isRequiredBy | dct:isVersionOf | dct:references | dct:replaces | dct:requires">
		<xsl:element name="dc:relation">
          	<xsl:value-of select="."/>
          </xsl:element>
	</xsl:template>

	<!-- rights -->
	<xsl:template match="dc:rights | dct:accessRights | dct:license">
		<xsl:element name="dc:rights">
          	<xsl:value-of select="."/>
          </xsl:element>
	</xsl:template>

	<!-- title -->
	<xsl:template match="dc:title | dct:alternative">
		<xsl:element name="dc:title">
          	<xsl:value-of select="."/>
          </xsl:element>
	</xsl:template>


	<!-- dc terms elements that are not strictly refinements of dc elements-->

	<!-- accessibility -->
	<xsl:template match="dct:accessibility">
	</xsl:template>
	
	<!-- accrualMethod -->
	<xsl:template match="dct:accrualMethod">
	</xsl:template>
	
	<!-- accrualPeriodicity -->
	<xsl:template match="dct:accrualPeriodicity">
	</xsl:template>
	
	<!-- accrualPolicy -->
	<xsl:template match="dct:accrualPolicy">
	</xsl:template>
	
	<!-- audience -->
	<xsl:template match="dct:audience">
	</xsl:template>
	<xsl:template match="dct:educationLevel">
	</xsl:template>
	<xsl:template match="dct:mediator">
	</xsl:template>
		
	<!-- instructionalMethod -->
	<xsl:template match="dct:instructionalMethod">
	</xsl:template>
	
	<!-- provenance -->
	<xsl:template match="dct:provenance">
	</xsl:template>
	
	<!-- rightsHolder -->
	<xsl:template match="dct:rightsHolder">
	</xsl:template>


	<!-- ieee elements -->

	<!-- interactivityLevel -->
	<xsl:template match="ieee:interactivityLevel">
	</xsl:template>
	
	<!-- interactivityType -->
	<xsl:template match="ieee:interactivityType">
	</xsl:template>
		
	<!-- typicalLearningTime -->
	<xsl:template match="ieee:typicalLearningTime">
	</xsl:template>

</xsl:stylesheet>
