<?xml version="1.0" encoding="UTF-8"?>
<!--written by Julianne 11/7/12-->

<xsl:stylesheet version="2.0" 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
    xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/"
    xmlns:dc="http://purl.org/dc/elements/1.1/" 
	xmlns:lar="http://ns.nsdl.org/ncs/lar"
    exclude-result-prefixes="lar xsi xsl">
	
	<xsl:output method="xml" indent="yes" encoding="UTF-8"/>
	
	<xsl:strip-space elements="*"/>	
	
	<xsl:template match="lar:record">
		<oai_dc:dc xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd">
			<xsl:apply-templates select="./*" />
			<xsl:comment>The National Science Digital Library (NSDL), located at the University Corporation for Atmospheric Research (UCAR), provides these metadata terms: These data and metadata may not be reproduced, duplicated, copied, sold, or otherwise exploited for any commercial purpose that is not expressly permitted by NSDL.</xsl:comment>
	<xsl:text>
	</xsl:text>
			<xsl:comment>The National Science Digital Library (NSDL) has normalized this metadata record for use across its systems and services.</xsl:comment>
	<xsl:text>
	</xsl:text>
		</oai_dc:dc>
	</xsl:template>
	
	<xsl:template match="lar:date">
		<xsl:element name="dc:date">
          	<xsl:value-of select="."/>
          </xsl:element>
	</xsl:template>

	<xsl:template match = "lar:description | lar:moreDescription">
		<xsl:element name="dc:description">
          	<xsl:value-of select="."/>
          </xsl:element>
	</xsl:template>
	
	<xsl:template match = "lar:contributor">
		<xsl:param name="string" select="."/>
		<xsl:param name="type" select="./@role"/>
		<xsl:if test="string-length($string) > 0">
			<xsl:choose>
				<xsl:when test="$type = 'Creator' ">
					<xsl:element name="dc:creator">
						<xsl:value-of select="$string"/>
					</xsl:element>
				</xsl:when>
				<xsl:when test="$type = 'Editor' or $type = 'Collection Developer' or $type = 'Funder' ">
					<xsl:element name="dc:contributor">
						<xsl:value-of select="$string"/>
					</xsl:element>
				</xsl:when>
				<xsl:when test="$type = 'Publisher' ">
					<xsl:element name="dc:publisher">
						<xsl:value-of select="$string"/>
					</xsl:element>
				</xsl:when>
			</xsl:choose>
		</xsl:if>
	</xsl:template>
	
	<xsl:template match="lar:format">
		<xsl:element name="dc:format">
          	<xsl:value-of select="."/>
          </xsl:element>
	</xsl:template>
	
	<xsl:template match="lar:type">
		<xsl:element name="dc:type">
          	<xsl:value-of select="."/>
          </xsl:element>
	</xsl:template>
	
	<xsl:template match="lar:subject | lar:keyword">
		<xsl:element name="dc:subject">
          	<xsl:value-of select="."/>
          </xsl:element>
	</xsl:template>
    
    <xsl:template match="lar:partnerSubject" name="tokenize">
        <xsl:param name="separator" select="':'"/>
        <xsl:for-each select="tokenize(.,$separator)">
			<xsl:if test="not(starts-with(., 'MSP2')) and not(starts-with(., 'Compadre'))">
                <dc:subject>
					<xsl:value-of select="normalize-space(.)"/>
                </dc:subject>
			</xsl:if> 
        </xsl:for-each>
    </xsl:template>	
	
	<xsl:template match="lar:language">
		<xsl:element name="dc:language">
          	<xsl:value-of select="."/>
          </xsl:element>
	</xsl:template>
	
	<xsl:template match="lar:identifier | lar:partnerID | lar:resourceHandle">
		<xsl:element name="dc:identifier">
          	<xsl:value-of select="."/>
          </xsl:element>
	</xsl:template>
	
	<xsl:template match="lar:relatedResource">
		<xsl:param name="string" select="./@URL"/>
		<xsl:element name="dc:relation">
			<xsl:value-of select="$string"/>
		</xsl:element>
	</xsl:template>	
	
	<xsl:template match="lar:copyright | lar:license/lar:name | lar:accessRestrictions">
		<xsl:element name="dc:rights">
          	<xsl:value-of select="."/>
          </xsl:element>
	</xsl:template>
	
	<xsl:template match="lar:copyrightHolder">
	</xsl:template>	

	<xsl:template match="lar:title">
		<xsl:element name="dc:title">
          	<xsl:value-of select="."/>
          </xsl:element>
	</xsl:template>
	
	<xsl:template match="lar:accessMode">
	</xsl:template>
	
	<xsl:template match="lar:fileSize">
	</xsl:template>
	
	<xsl:template match="lar:mediaRunTime">
	</xsl:template>
	
	<xsl:template match="lar:educationLevel">
	</xsl:template>
	
	<xsl:template match="lar:audienceRefinement">
	</xsl:template>
	
	<xsl:template match="lar:readingLevel">
	</xsl:template>
	
	<xsl:template match="lar:standard">
	</xsl:template>
	
	<xsl:template match="lar:otherIdentifier">
	</xsl:template>
	
	<xsl:template match="lar:license/lar:property">
	</xsl:template>
	
	<xsl:template match="lar:license/lar:explanation">
	</xsl:template>
	
	<xsl:template match="lar:metadataTerms">
	</xsl:template>
	
	<xsl:template match="lar:recordID">
	</xsl:template>
	
	<xsl:template match="lar:recordDate">
	</xsl:template>
	
	<xsl:template match="lar:readingGradeLevel">
	</xsl:template>
	
	<xsl:template match="lar:partnerURL">
	</xsl:template>
	
	<xsl:template match=" lar:metadataHandle">
	</xsl:template>
	
</xsl:stylesheet>
