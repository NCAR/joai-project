<?xml version="1.0"?>
<xsl:stylesheet 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xsi ="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:dc="http://purl.org/dc/elements/1.1/"
    xmlns:nsdl_dc="http://ns.nsdl.org/nsdl_dc_v1.02/"
    xmlns:ieee="http://www.ieee.org/xsd/LOMv1p0"
    xmlns:dct="http://purl.org/dc/terms/"
    xmlns:larr="http://ns.nsdl.org/ncs/lar"
	xmlns:lar="http://ns.nsdl.org/schemas/dc/lar"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
	exclude-result-prefixes="xsi xs larr"
    version="1.1">

<!--PURPOSE: to transform LAR version 1.00 records into the nsdl_dc version 1.02.020 metadata records-->
<!--CREATION: 2012-02-20 by Katy Ginger, University Corporation for Atmospheric Research (UCAR)-->
<!--ASSUMPTIONS: Assumes required LAR elements are present and does not check to see if they are present-->
<!--HISTORY: none-->

	<xsl:output method="xml" indent="yes" encoding="UTF-8"/>

<!--TRANSFORMATION CODE-->
<!-- ************************************************** -->
	<xsl:template match="*|/">
		<xsl:apply-templates select="larr:record"/>
	</xsl:template>

<!--TRANSFORMATION CODE-->
<!-- ********************************************************-->
	<xsl:template match="larr:record">
		<nsdl_dc:nsdl_dc schemaVersion="1.02.020" xmlns:nsdl_dc="http://ns.nsdl.org/nsdl_dc_v1.02/" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dct="http://purl.org/dc/terms/" xmlns:lar="http://ns.nsdl.org/schemas/dc/lar" xmlns:ieee="http://www.ieee.org/xsd/LOMv1p0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://ns.nsdl.org/nsdl_dc_v1.02/ http://ns.nsdl.org/schemas/nsdl_dc/nsdl_dc_v1.02.xsd">

<!--lar:readiness; just assign Fully redy because using the LAR framework-->
<lar:readiness xsi:type="lar:Ready">Fully ready</lar:readiness>

<!--dc:identifier; doesn't really need a template but easy to do-->
		<xsl:apply-templates select="larr:identifier" mode="process">
			<xsl:with-param name="tag">dc:identifier</xsl:with-param>
			<xsl:with-param name="att">dct:URI</xsl:with-param>
		</xsl:apply-templates>

<!--dc:relation using partnerURL-->
		<xsl:apply-templates select="larr:partnerURL" mode="process">
			<xsl:with-param name="tag">dc:relation</xsl:with-param>
			<xsl:with-param name="att">nsdl_dc:NSDLPartnerURL</xsl:with-param>
		</xsl:apply-templates>

<!--dc:title-->
		<xsl:apply-templates select="larr:title" mode="process">
			<xsl:with-param name="tag">dc:title</xsl:with-param>
		</xsl:apply-templates>

<!--dct:alternative: does not have-->
<!--dct:abstract: does not have-->

<!--dc:description-->
		<xsl:apply-templates select="larr:description" mode="process">
			<xsl:with-param name="tag">dc:description</xsl:with-param>
		</xsl:apply-templates>

<!--lar:moreDescription-->
		<xsl:apply-templates select="larr:moreDescription" mode="process">
			<xsl:with-param name="tag">lar:moreDescription</xsl:with-param>
		</xsl:apply-templates>

<!--dc:subject using subject-->
		<xsl:apply-templates select="larr:subject" mode="process">
			<xsl:with-param name="tag">dc:subject</xsl:with-param>
		</xsl:apply-templates>

<!--dc:subject using partnerSubject-->
		<xsl:apply-templates select="larr:partnerSubject" mode="partnersubject"/>


<!--dc:subject using keyword-->
		<xsl:apply-templates select="larr:keyword" mode="process">
			<xsl:with-param name="tag">dc:subject</xsl:with-param>
		</xsl:apply-templates>

<!--dct:abstract: does not have-->
<!--dct:tableOfContents: does not have-->
<!--dct:bibliographicCitation: does not have-->

<!--dc:language-->
		<xsl:apply-templates select="larr:language" mode="process">
			<xsl:with-param name="tag">dc:language</xsl:with-param>
		</xsl:apply-templates>

<!--dc:format-->
		<xsl:apply-templates select="larr:format" mode="process">
			<xsl:with-param name="tag">dc:format</xsl:with-param>
		</xsl:apply-templates>

<!--lar:accessMode-->
		<xsl:apply-templates select="larr:accessMode" mode="process">
			<xsl:with-param name="tag">lar:accessMode</xsl:with-param>
			<xsl:with-param name="att">lar:ModeAcc</xsl:with-param>
		</xsl:apply-templates>

<!--dc:extent using fileSize-->
		<xsl:apply-templates select="larr:fileSize" mode="filesize">
			<xsl:with-param name="tag">dct:extent</xsl:with-param>
		</xsl:apply-templates>

<!--dc:extent using mediaRunTime-->
		<xsl:apply-templates select="larr:mediaRunTime" mode="process">
			<xsl:with-param name="tag">dct:extent</xsl:with-param>
		</xsl:apply-templates>

<!--dct:educationLevel-->
<!--LAR uses the NSDL 1.02.020 vocab-->
		<xsl:apply-templates select="larr:educationLevel" mode="process">
			<xsl:with-param name="tag">dct:educationLevel</xsl:with-param>
			<xsl:with-param name="att">nsdl_dc:NSDLEdLevel</xsl:with-param>
		</xsl:apply-templates>

<!--dct:audience-->
		<xsl:apply-templates select="larr:audienceRefinement" mode="audience"> 
			<xsl:with-param name="tag">dct:audience</xsl:with-param>
			<xsl:with-param name="att">nsdl_dc:NSDLAudience</xsl:with-param>
		</xsl:apply-templates>

<!--dc:type-->
		<xsl:apply-templates select="larr:type" mode="resource"> 
			<xsl:with-param name="tag">dc:type</xsl:with-param>
			<xsl:with-param name="att">nsdl_dc:NSDLType</xsl:with-param>
		</xsl:apply-templates>

<!--lar:readingLevel-->
		<xsl:apply-templates select="larr:readingLevel" mode="process">
			<xsl:with-param name="tag">lar:readingLevel</xsl:with-param>
		</xsl:apply-templates>

<!--lar:readingGradeLevel-->
		<xsl:apply-templates select="larr:readingGradeLevel" mode="process">
			<xsl:with-param name="tag">lar:readingGradeLevel</xsl:with-param>
			<xsl:with-param name="att">lar:ReadGrade</xsl:with-param>
		</xsl:apply-templates>

<!--dct:conformsTo-->
		<xsl:apply-templates select="larr:standard/larr:alignment/larr:id" mode="process2"> 
			<xsl:with-param name="tag">dct:conformsTo</xsl:with-param>
		</xsl:apply-templates>

<!--dc:relation and other relation elements using relateResource-->
		<xsl:apply-templates select="larr:relatedResource" mode="relation"/>

<!--dc:date and other date elements using date-->
		<xsl:apply-templates select="larr:date" mode="date"/>

<!--dc:creator, dc:publisher and dc:contributor using contributor-->
		<xsl:apply-templates select="larr:contributor" mode="contributor"/>

<!--dct:accessRights-->
		<xsl:apply-templates select="larr:accessRestrictions" mode="access"> 
			<xsl:with-param name="tag">dct:accessRights</xsl:with-param>
			<xsl:with-param name="att">nsdl_dc:NSDLAccess</xsl:with-param>
		</xsl:apply-templates>

<!--dc:rights-->
		<xsl:apply-templates select="larr:copyright" mode="process">
			<xsl:with-param name="tag">dc:rights</xsl:with-param>
		</xsl:apply-templates>

<!--dct:license-->
		<xsl:apply-templates select="larr:license/larr:name" mode="rights">
			<xsl:with-param name="tag">dct:license</xsl:with-param>
			<xsl:with-param name="att">dct:URI</xsl:with-param>
		</xsl:apply-templates>

<!--lar:licenseProperty-->
		<xsl:apply-templates select="larr:license/larr:property" mode="process">
			<xsl:with-param name="tag">lar:licenseProperty</xsl:with-param>
			<xsl:with-param name="att">lar:LicProp</xsl:with-param>
		</xsl:apply-templates>

<!--dct:rightsHolder-->
		<xsl:apply-templates select="larr:copyrightHolder" mode="rights">
			<xsl:with-param name="tag">dct:rightsHolder</xsl:with-param>
			<xsl:with-param name="att">dct:URI</xsl:with-param>
		</xsl:apply-templates>

<!--lar:metadataTerms using collection builder provide LAR field-->
		<xsl:apply-templates select="larr:metadataTerms" mode="terms">
			<xsl:with-param name="tag">lar:metadataTerms</xsl:with-param>
			<xsl:with-param name="att">dct:URI</xsl:with-param>
		</xsl:apply-templates>

<!--lar:metadataTerm using NSDL statement-->
		<xsl:element name="lar:metadataTerms">
			<xsl:text>The National Science Digital Library (NSDL), located at the University Corporation for Atmospheric Research (UCAR), provides these metadata terms: These data and metadata may not be reproduced, duplicated, copied, sold, or otherwise exploited for any commercial purpose that is not expressly permitted by NSDL. More information is available at: http://nsdl.org/help/terms-of-use.</xsl:text>
		</xsl:element>
		</nsdl_dc:nsdl_dc><!--end nsdl_dc:nsdl_dc element-->
	</xsl:template>




<!--TEMPLATES for lar to nsdl_dc-->
<!-- ****************************************-->
<!--PROCESS and PROCESS2 write all tag sets that do not have their own templates-->

<!--ACCESS template-->
	<xsl:template match="node()" name="access" mode="access">
<!--note: in templates, params must be declared before variables-->
<!--note: for the with-param tag to work above, a param tag must exist and be present here in the template being called-->
		<xsl:param name="tag"/>
		<xsl:param name="string" select="."/>
		<xsl:param name="att"/>
		
		<xsl:if test="string-length($string) > 0">
			<xsl:element name="{$tag}">
				<xsl:if test="string-length($att) > 0">
					<xsl:attribute name="xsi:type">
						<xsl:value-of select="$att"/>
					</xsl:attribute>	
				</xsl:if>
				<xsl:choose>
					<xsl:when test="$string = 'Free access with user action' ">
						<xsl:text>Free access with registration</xsl:text>
					</xsl:when>
					<xsl:otherwise>
						<xsl:value-of select="$string"/>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:element>
		</xsl:if>
	</xsl:template>	

<!--AUDIENCE template-->
	<xsl:template match="node()" name="audience" mode="audience">
<!--note: in templates, params must be declared before variables-->
<!--note: for the with-param tag to work above, a param tag must exist and be present here in the template being called-->
		<xsl:param name="tag"/>
		<xsl:param name="string" select="."/>
		<xsl:param name="att"/>

		<xsl:if test="string-length($string) > 0">
			<xsl:choose>
				<xsl:when test="$string = 'Educator and learner' ">
					<xsl:element name="{$tag}">
							<xsl:attribute name="xsi:type">
								<xsl:value-of select="$att"/>
							</xsl:attribute>	
						<xsl:text>Educator</xsl:text>
					</xsl:element>
					<xsl:element name="{$tag}">
							<xsl:attribute name="xsi:type">
								<xsl:value-of select="$att"/>
							</xsl:attribute>	
						<xsl:text>Learner</xsl:text>
					</xsl:element>
				</xsl:when>
				<xsl:otherwise>
					<xsl:element name="{$tag}">
						<xsl:if test="string-length($att) > 0">
							<xsl:attribute name="xsi:type">
								<xsl:value-of select="$att"/>
							</xsl:attribute>	
						</xsl:if>
						<xsl:value-of select="$string"/>
					</xsl:element>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:if>
	</xsl:template>	

<!--CONTRIBUTOR template-->
	<xsl:template match="node()" name="contributor" mode="contributor">
<!--note: in templates, params must be declared before variables-->
<!--note: for the with-param tag to work above, a param tag must exist and be present here in the template being called-->
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

<!--DATE template-->
	<xsl:template match="node()" name="date" mode="date">
<!--note: in templates, params must be declared before variables-->
<!--note: for the with-param tag to work above, a param tag must exist and be present here in the template being called-->
		<xsl:param name="string" select="."/>
		<xsl:param name="type" select="./@type"/>

		<xsl:if test="string-length($string) > 0">
			<xsl:choose>
				<xsl:when test="$type = 'Created' ">
					<xsl:element name="dct:created">
						<xsl:attribute name="xsi:type">dct:W3CDTF</xsl:attribute>
						<xsl:value-of select="$string"/>
					</xsl:element>
				</xsl:when>
				<xsl:when test="$type = 'Published' ">
					<xsl:element name="dc:date">
						<xsl:attribute name="xsi:type">dct:W3CDTF</xsl:attribute>
						<xsl:value-of select="$string"/>
					</xsl:element>
				</xsl:when>
				<xsl:when test="$type = 'Modified' ">
					<xsl:element name="dct:modified">
						<xsl:attribute name="xsi:type">dct:W3CDTF</xsl:attribute>
						<xsl:value-of select="$string"/>
					</xsl:element>
				</xsl:when>
			</xsl:choose>
		</xsl:if>
	</xsl:template>	

<!--FILESIZE template-->
	<xsl:template match="node()" name="filesize" mode="filesize">
<!--note: in templates, params must be declared before variables-->
<!--note: for the with-param tag to work above, a param tag must exist and be present here in the template being called-->
		<xsl:param name="tag"/>
		<xsl:param name="string" select="."/>
		<xsl:param name="type" select="./@units"/>

		<xsl:if test="string-length($string) > 0">
			<xsl:element name="{$tag}">
				<xsl:value-of select="concat($string, ' ', $type)"/>
			</xsl:element>
		</xsl:if>
	</xsl:template>	

<!--PARTNER SUBJECT template-->
	<xsl:template match="node()" mode="partnersubject">
<!--this template is called to process the partnerSubject nodes-->
<!--this template then calls another template to process the string within each element-->
<!--since only approved vocabularies are allowed in partnerSubject; strip off the vocab owner label (i.e. after first :)-->
	<xsl:param name="string" select="substring-after(., ':')"/>
		<xsl:call-template name="processpartnersubject" >
			<xsl:with-param name="tag">dc:subject</xsl:with-param>
			<xsl:with-param name="term">
				<xsl:value-of select="$string"/>
			</xsl:with-param>			
		</xsl:call-template>		
	</xsl:template>	

<!--PROCESS PARTNER SUBJECT template-->
	<xsl:template name="processpartnersubject">
		<xsl:param name="tag"/>
		<xsl:param name="string" select="substring-after(., ':' )"/>
		<xsl:choose>
			<xsl:when test="contains($string, ':') ">
				<xsl:element name="{$tag}">
					<xsl:value-of select="substring-before($string, ':')"/>
				</xsl:element>
			</xsl:when>
			<xsl:otherwise> 
				<xsl:element name="{$tag}">
					<xsl:value-of select="$string"/>
				</xsl:element>	
			</xsl:otherwise>			
		</xsl:choose>
		<xsl:if test="contains($string, ':') ">
			<xsl:call-template name="processpartnersubject" >
				<xsl:with-param name="tag">dc:subject</xsl:with-param>
				<xsl:with-param name="string">
					<xsl:value-of select="substring-after($string, ':' )"/>
				</xsl:with-param>	
			</xsl:call-template>
		</xsl:if>
	</xsl:template>

<!--PROCESS template with type-->
	<xsl:template match="node()" name="process" mode="process">
<!--note: in templates, params must be declared before variables-->
<!--note: for the with-param tag to work above, a param tag must exist and be present here in the template being called-->
		<xsl:param name="tag"/>
		<xsl:param name="att"/>
		<xsl:param name="string" select="."/>
		<xsl:param name="type" select="./@type"/>

		<xsl:if test="string-length($string) > 0">
			<xsl:element name="{$tag}">
				<xsl:if test="string-length($att) > 0">
					<xsl:attribute name="xsi:type">
						<xsl:value-of select="$att"/>
					</xsl:attribute>	
				</xsl:if>
				<xsl:choose>
					<xsl:when test="string-length($type) > 0">
						<xsl:value-of select="concat($type, ': ', $string)"/>
					</xsl:when>
					<xsl:otherwise>
						<xsl:value-of select="$string"/>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:element>
		</xsl:if>
	</xsl:template>	

<!--PROCESS2 template without type-->
	<xsl:template match="node()" name="process2" mode="process2">
<!--note: in templates, params must be declared before variables-->
<!--note: for the with-param tag to work above, a param tag must exist and be present here in the template being called-->
		<xsl:param name="tag"/>
		<xsl:param name="att"/>
		<xsl:param name="string" select="."/>

		<xsl:if test="string-length($string) > 0">
			<xsl:element name="{$tag}">
				<xsl:if test="string-length($att) > 0">
					<xsl:attribute name="xsi:type">
						<xsl:value-of select="$att"/>
					</xsl:attribute>	
				</xsl:if>
				<xsl:value-of select="$string"/>
			</xsl:element>
		</xsl:if>
	</xsl:template>	

<!--RELATION template-->
	<xsl:template match="node()" mode="relation">
<!--note: in templates, params must be declared before variables-->
<!--note: for the with-param tag to work above, a param tag must exist and be present here in the template being called-->
		<xsl:param name="tag"/>
		<xsl:param name="type" select="./@type"/>
		<xsl:param name="string" select="./@URL"/>
		
		<xsl:if test="string-length($string) > 0">
			<xsl:choose>
				<xsl:when test="$type = 'Has a Related Resource Of' or $type = 'Is a Related Resource Of' ">
					<xsl:element name="dc:relation">
						<xsl:value-of select="$string"/>
					</xsl:element>
				</xsl:when>
				<xsl:when test="$type = 'Has Part' ">
					<xsl:element name="dct:hasPart">
						<xsl:value-of select="$string"/>
					</xsl:element>
				</xsl:when>
				<xsl:when test="$type = 'Is Part Of' ">
					<xsl:element name="dct:isPartOf">
						<xsl:value-of select="$string"/>
					</xsl:element>
				</xsl:when>
				<xsl:when test="$type = 'Is Referenced By' ">
					<xsl:element name="dct:isReferencedBy">
						<xsl:value-of select="$string"/>
					</xsl:element>
				</xsl:when>
				<xsl:when test="$type = 'References' ">
					<xsl:element name="dct:references">
						<xsl:value-of select="$string"/>
					</xsl:element>
				</xsl:when>
				<xsl:when test="$type = 'Has Version' ">
					<xsl:element name="dct:hasVersion">
						<xsl:value-of select="$string"/>
					</xsl:element>
				</xsl:when>
				<xsl:when test="$type = 'Is Version Of' ">
					<xsl:element name="dct:isVersionOf">
						<xsl:value-of select="$string"/>
					</xsl:element>
				</xsl:when>
				<xsl:when test="$type = 'Requires' ">
					<xsl:element name="dct:requires">
						<xsl:value-of select="$string"/>
					</xsl:element>
				</xsl:when>
			</xsl:choose>
		</xsl:if>
	</xsl:template>	

<!--RESOURCE TYPE template-->
<!--There are several LAR term values that need to be mapped to NSDL_DC term values-->
	<xsl:template match="node()" name="resource" mode="resource">
<!--note: in templates, params must be declared before variables-->
<!--note: for the with-param tag to work above, a param tag must exist and be present here in the template being called-->
		<xsl:param name="tag"/>
		<xsl:param name="att"/>
		<xsl:param name="string" select="."/>

		<xsl:if test="string-length($string) > 0">
			<xsl:element name="{$tag}">
				<xsl:if test="string-length($att) > 0">
					<xsl:attribute name="xsi:type">
						<xsl:value-of select="$att"/>
					</xsl:attribute>	
				</xsl:if>
				<xsl:choose>
					<xsl:when test="$string = 'Animation/Movie' ">
						<xsl:text>Movie/Animation</xsl:text>
					</xsl:when>
					<xsl:when test="$string = 'Data' ">
						<xsl:text>Dataset</xsl:text>
					</xsl:when>
					<xsl:when test="$string = 'Numerical/Computer Model' ">
						<xsl:text>Numerical Model</xsl:text>
					</xsl:when>
					<xsl:when test="$string = 'Tool/Software' ">
						<xsl:text>Software</xsl:text>
					</xsl:when>
					<xsl:when test="$string = 'Unit' ">
						<xsl:text>Unit of Instruction</xsl:text>
					</xsl:when>
					<xsl:otherwise>
						<xsl:value-of select="$string"/>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:element>
		</xsl:if>
	</xsl:template>	

<!--RIGHTS template-->
	<xsl:template match="node()" mode="rights">
<!--note: in templates, params must be declared before variables-->
<!--note: for the with-param tag to work above, a param tag must exist and be present here in the template being called-->
		<xsl:param name="tag"/>
		<xsl:param name="att"/>
		<xsl:param name="string" select="."/>
		<xsl:param name="string2" select="./@URL"/>
		<xsl:if test="string-length($string) > 0">
			<xsl:element name="{$tag}">
				<xsl:value-of select="$string"/>
			</xsl:element>
		</xsl:if>
		<xsl:if test="string-length($string2) > 0">
			<xsl:element name="{$tag}">
				<xsl:attribute name="xsi:type">
					<xsl:value-of select="$att"/>
				</xsl:attribute>	
				<xsl:value-of select="$string2"/>
			</xsl:element>
		</xsl:if>
	</xsl:template>	


<!--TERMS template-->
	<xsl:template match="node()" mode="terms">
<!--note: in templates, params must be declared before variables-->
<!--note: for the with-param tag to work above, a param tag must exist and be present here in the template being called-->
		<xsl:param name="tag"/>
		<xsl:param name="string" select="."/>
		<xsl:param name="string2" select="./@URL"/>
		<xsl:param name="string3" select="./@holder"/>
		<xsl:if test="string-length($string) > 0">
			<xsl:element name="{$tag}">
				<xsl:choose>
					<xsl:when test="string-length($string2) > 0">
						<xsl:value-of select="concat('The following entity, ', $string3, ', has claims on the use of this metadata. This claim is as follows: ', $string, ' The entity provided more information at: ', $string2)"/>
					</xsl:when>
					<xsl:otherwise>
						<xsl:value-of select="concat('The following entity, ', $string3, ', has claims on the use of this metadata. This claim is as follows: ', $string)"/>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:element>
		</xsl:if>
	</xsl:template>	


</xsl:stylesheet>
<!--LICENSE AND COPYRIGHT
The contents of this file are subject to the Educational Community License v1.0 (the "License"); you may not use this file except in compliance with the License. You should obtain a copy of the License from http://www.opensource.org/licenses/ecl1.php. Files distributed under the License are distributed on an "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for the specific language governing rights and limitations under the License. Copyright 2002-2009 by Digital Learning Sciences, University Corporation for Atmospheric Research (UCAR). All rights reserved.-->