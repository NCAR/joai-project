<?xml version="1.0"?>
<xsl:stylesheet 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xsi ="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:d="http://collection.dlese.org"
    xmlns:f="http://www.dlese.org/Metadata/fields"
    xmlns:dc="http://purl.org/dc/elements/1.1/"
    xmlns:nsdl_dc="http://ns.nsdl.org/nsdl_dc_v1.02/"
    xmlns:dct="http://purl.org/dc/terms/"
    xmlns:ieee="http://www.ieee.org/xsd/LOMv1p0"
    xmlns:xsd="http://www.w3.org/2001/XMLSchema"
	exclude-result-prefixes=" xsi d f xsd" 
    version="1.0">

<!--ORGANIZATION OF THIS FILE-->
<!-- **************************************-->
<!--This file is organized into the following sections:
A. Purpose
B. License information and credits
C. Assumptions
D. Global variables
E. Transformation code
F. Templates to apply (in alphabetical order)-->


<!--A. PURPOSE-->
<!-- **************************************-->
<!--To transform the Digital Library for Earth System Education (DLESE) collection metadata records to NSDL-DC-->


<!--B. LICENSE INFORMATION and CREDITS-->
<!-- *****************************************************-->
<!--Date created: 2007-08-29 by Katy Ginger, DLESE Program Center, University Corporation for Atmospheric Research (UCAR)-->
<!--Last modified: 2007-08-29 by Katy Ginger-->
<!--License information:
		Copyright (c) 2002-2007
		University Corporation for Atmospheric Research (UCAR)
		P.O. Box 3000, Boulder, CO 80307, United States of America
		email: dlesesupport@ucar.edu.
		All rights reserved
These XML tranformation written in XSLT 1.0 and XPATH 1.0 are free software; you can redistribute them and/or modify them under the terms of the GNU General Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any later version.  These XML instance documents are distributed in the hope that they will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this project; if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA -->

    
<!--C. ASSUMPTIONS-->
<!-- **************************************-->
<!--Overarching assumption. The metadata field only appears if it contains data-->
<!--1. Applies to DLESE collection metadata format, version 1.0.00 records-->
<!--2. Assumes a single policy URL when in actuality there may be multiple. The first URL is used-->
<!--3. Assumes content is present in collection required feilds and does not check for the presence of it-->
<!--4. Assumes the DLESE collection records are the done and published ones that are in DLESE-->


	<xsl:output method="xml" indent="yes" encoding="UTF-8"/>


<!--E. TRANSFORMATION CODE-->
<!-- **********************************************************-->
	<xsl:template match="d:collectionRecord">
		<nsdl_dc:nsdl_dc schemaVersion="1.02.010" xmlns:nsdl_dc="http://ns.nsdl.org/nsdl_dc_v1.02/" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dct="http://purl.org/dc/terms/" xmlns:ieee="http://www.ieee.org/xsd/LOMv1p0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://ns.nsdl.org/nsdl_dc_v1.02/ http://ns.nsdl.org/schemas/nsdl_dc/nsdl_dc_v1.02.xsd">


<!--dc:title-->
<!--since title is required metadata, no need to check to see if data is present-->
			<xsl:element name="dc:title">
				<xsl:value-of select="d:general/d:fullTitle"/>
			</xsl:element>	

<!--dc:creator-->
<!--do not use-->

<!--dc:contributor-->
<!--use collection builder as contributor since the URL being cataloged is a URL in DLESE-->
			<xsl:apply-templates select="d:lifecycle/d:contributors/d:contributor"/> 
			<!--see template CONTRIBUTOR-->
			
<!--dc:publisher-->
<!--2007-10-10: Since now using collection location as the identifier and not the DLESE browse page; there is no identifiable pulisher like DLESE-->
<!--			<xsl:element name="dc:publisher">Digital Library for Earth System Education (DLESE)/ University Corporation for Atmospheric Research (UCAR)</xsl:element>-->
			
<!--dc:subject - from subject field-->
			<xsl:apply-templates select="d:general/d:subjects/d:subject" mode="DLESE"/>
			<!--see template SUBJECT mode=DLESE-->

<!--dc:subject - from keyword field-->
			<xsl:apply-templates select="d:general/d:keywords/d:keyword" mode="keywords"/> 
			<!--see template SUBJECT mode=KEYWORDS-->

<!--dc:date-->
<!--collection metadata has date information associated with metadata creation, not with collection creation necessarily-->
<!--therefore transform no date information-->

<!--dc:description - from description nsdl-dc-->
			<xsl:element name="dc:description">
				<xsl:value-of select="d:general/d:description"/>
			</xsl:element>	

<!--dc:format - size -->
<!--collection metadata does not have size information-->

<!--dc:format - mimetype-->
<!--DC format should be the mimetype of the DC identifier URL, but DC identifier is written as a constructed URL back to DLESE; so text/html would apply but not really helpful or adding information-->
<!--since collections can have all different types of mimetypes, do not write DC format-->


<!--dc:type-->
<!--no vocabulary mapping is necessary since this is collection metadata-->
<!--use Dublin Core Metadata Initiative (DCMI) vocab term of Collection-->

			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">dct:DCMIType</xsl:attribute>
				<xsl:text>Collection</xsl:text>
			</xsl:element>
	

<!--dc:identifier use collection location-->
<!--decision: 2007-10-10: made all DLESE collection records have a collection location which is sometimes the collection browse in DLESE itself-->
<!--so decided to use collection location even though it may not be the best place to land to see resoures in a collection-->
<!--do not us collection policy URL since it is not appropropiate landing page of a collection either-->

			<xsl:element name="dc:identifier">
				<xsl:attribute name="xsi:type">dct:URI</xsl:attribute>
				<xsl:value-of select="d:access/d:collectionLocation"/>
			</xsl:element>

<!--dc:source-->		
<!--ADN does not collect dc:source information yet for either online or offline resources-->

<!--dc:language-->
			<xsl:element name="dc:language">
				<xsl:value-of select="d:general/d:language"/>
			</xsl:element>

<!--dc:rights-->
<!--collection metadata has a collection scope statement URL that includes rights and terms if the collection is in DLESE-->
<!--there is no collection metadata field for rights specifically; use collection poliy URL to write DC rights-->
<!--assumes there is only one policy hyperlink provided-->
<!--decision: 2007-10-10: Since now using collection location as the NSDL_DC identifier, using collection scope statement as the right information is probably not a very good to do; so do not provide rights information since generally don't have it for the colleciton location-->
<!--			<xsl:element name="dc:rights">
				<xsl:attribute name="xsi:type">dct:URI</xsl:attribute>
				<xsl:value-of select="d:general/d:policies/d:policy/@url"/>
			</xsl:element>-->
	
<!--dc:coverage-->
<!--collection metadata does not have coverage information-->
		
<!--dc:relation-->
<!--make the view of the collection within DLESE a related resource-->
			<xsl:element name="dc:relation">
				<xsl:value-of select="concat('http://www.dlese.org/dds/histogram.do?group=subject&amp;key=', d:access/d:key)"/>			
			</xsl:element>

<!--dct:educationLevel-->
<!--maps gradeRange terms to the NSDL list of terms-->
<!--variable for gradeRange-->
		<xsl:variable name="allgrades">
			<xsl:for-each select="d:general/d:gradeRanges/d:gradeRange">
				<xsl:value-of select="."/>
			</xsl:for-each>
		</xsl:variable>

<!--to prevent duplicate dc:educationLevel - nsdl_dc tags from appearing, test the $allgrades variable-->
<!--dct:educationLevel - nsdl_dc: Elementary School-->
		<xsl:if test="contains($allgrades, 'Elementary') or contains($allgrades, 'elementary')">
			<xsl:element name="dct:educationLevel">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLEdLevel</xsl:attribute>
				<xsl:text>Elementary School</xsl:text>
			</xsl:element>
		</xsl:if>

<!--dct:educationLevel - nsdl_dc: Middle School-->
		<xsl:if test="contains($allgrades, 'Middle') or contains($allgrades, 'middle')">
			<xsl:element name="dct:educationLevel">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLEdLevel</xsl:attribute>
				<xsl:text>Middle School</xsl:text>
			</xsl:element>
		</xsl:if>

<!--dct:educationLevel - nsdl_dc: High School-->
		<xsl:if test="contains($allgrades, 'High') or contains($allgrades, 'high')">
			<xsl:element name="dct:educationLevel">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLEdLevel</xsl:attribute>
				<xsl:text>High School</xsl:text>
			</xsl:element>
		</xsl:if>

<!--dct:educationLevel - nsdl_dc: Undergraduate Lower Division-->
		<xsl:if test="contains($allgrades, 'Lower') or contains($allgrades, 'lower')">
			<xsl:element name="dct:educationLevel">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLEdLevel</xsl:attribute>
				<xsl:text>Higher Education</xsl:text>
			</xsl:element>
			<xsl:element name="dct:educationLevel">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLEdLevel</xsl:attribute>
				<xsl:text>Undergraduate (Lower Division)</xsl:text>
			</xsl:element>
		</xsl:if>

<!--dct:educationLevel - nsdl_dc: Undergraduate Upper Division-->
		<xsl:if test="contains($allgrades, 'Upper') or contains($allgrades, 'upper')">
			<xsl:element name="dct:educationLevel">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLEdLevel</xsl:attribute>
				<xsl:text>Higher Education</xsl:text>
			</xsl:element>
			<xsl:element name="dct:educationLevel">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLEdLevel</xsl:attribute>
				<xsl:text>Undergraduate (Upper Division)</xsl:text>
			</xsl:element>
		</xsl:if>

<!--dct:educationLevel - nsdl_dc: Graduate -->
		<xsl:if test="contains($allgrades, 'Graduate') or contains($allgrades, 'graduate') or contains($allgrades, 'professional') or contains($allgrades, 'Professional') ">
			<xsl:element name="dct:educationLevel">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLEdLevel</xsl:attribute>
				<xsl:text>Higher Education</xsl:text>
			</xsl:element>
			<xsl:element name="dct:educationLevel">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLEdLevel</xsl:attribute>
				<xsl:text>Graduate/Professional</xsl:text>
			</xsl:element>
		</xsl:if>

<!--dct:educationLevel - plain - Informal education-->
		<xsl:if test="contains($allgrades, 'Informal') or contains($allgrades, 'informal')">
			<xsl:element name="dct:educationLevel">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLEdLevel</xsl:attribute>
				<xsl:text>Informal Education</xsl:text>
			</xsl:element>
		</xsl:if>

<!--dct:educationLevel - plain - General public-->
		<xsl:if test="contains($allgrades, 'general') or contains($allgrades, 'General') or contains($allgrades, 'public') or contains($allgrades, 'Public')">
			<xsl:element name="dct:educationLevel">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLEdLevel</xsl:attribute>
				<xsl:text>Informal Education</xsl:text>
			</xsl:element>
			<xsl:element name="dct:educationLevel">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLEdLevel</xsl:attribute>
				<xsl:text>General Public</xsl:text>
			</xsl:element>
		</xsl:if>

<!--end nsdl_dc:nsdl_dc-->
		</nsdl_dc:nsdl_dc>
	</xsl:template>


<!--F. TEMPLATES TO APPLY (alphabetical order)-->
<!--**********************************************************-->
<!--1. CONTRIBUTOR selects DC creator, publisher or contributor based on the ADN contributor role-->
<!--3. ORGANIZATION writes organization content for either DC contributor, creator or publisher-->
<!--4. PERSON writes person content for either DC contributor, creator or publisher-->
<!--5. SUBJECT template mode=DLESE writes DC subject from ADN subject but removes the leading 'DLESE:'-->
<!--6. SUBJECT template mode=KEYWORDS writes DC subject from ADN keywords -->


<!--1. CONTRIBUTOR template-->
<!--the term contact is not transformed since it is not a contributor, creator or publisher-->
<!--collection metadata only contact and responsible party as contributor types-->
<!--transform both to DC contributor because they may not be authors, publishers or creators-->
	<xsl:template match="d:contributor">
		<xsl:element name="dc:contributor">
			<xsl:apply-templates select="d:person"/>
			<xsl:apply-templates select="d:organization"/>
		</xsl:element>
	</xsl:template>			

<!--3. ORGANIZATION template-->
	<xsl:template match="d:organization">
		<xsl:choose>
			<xsl:when test="string-length(d:instDept)>0">
				<xsl:value-of select="concat(d:instDept,', ',d:instName)"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="d:instName"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>			


<!--4. PERSON template-->
	<xsl:template match="d:person">
		<xsl:value-of select="concat(d:nameFirst,' ',d:nameLast)"/>
	</xsl:template>			



<!--5. SUBJECT template mode=DLESE-->
	<xsl:template match="d:subject" mode="DLESE">
		<xsl:choose>
			<xsl:when test="contains(.,'Other') or contains(.,'supplied')"/>
			<xsl:otherwise>
				<xsl:element name="dc:subject">
					<xsl:value-of select="substring-after(.,'DLESE:')"/>
				</xsl:element>
			</xsl:otherwise>
		</xsl:choose>
</xsl:template>			

<!--6. SUBJECT template mode=KEYWORDS-->
	<xsl:template match="d:keyword" mode="keywords">
		<xsl:element name="dc:subject">
			<xsl:value-of select="."/>
		</xsl:element>
	</xsl:template>			

</xsl:stylesheet>