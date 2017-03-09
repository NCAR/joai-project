<?xml version="1.0"?>

<xsl:stylesheet 

    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"

    xmlns:xsi ="http://www.w3.org/2001/XMLSchema-instance"

    xmlns:d="http://collection.dlese.org"

    xmlns:f="http://www.dlese.org/Metadata/fields"

    xmlns:dc="http://purl.org/dc/elements/1.1/"

    exclude-result-prefixes="xsi d f" 

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

<!--To transform the Digital Library for Earth System Education (DLESE) collection metadata records to simple Dublin Core that uses the Open Archives Initiative (OAI) namespace-->





<!--B. LICENSE INFORMATION and CREDITS-->

<!-- *****************************************************-->

<!--Date created: 2006-03-31 by Katy Ginger, University Corporation for Atmospheric Research (UCAR)-->

<!--Last modified: 2006-04-11 by Katy Ginger-->

<!--License information:

		Copyright (c) 2007 Digital Learning Sciences

		University Corporation for Atmospheric Research (UCAR)

		P.O. Box 3000, Boulder, CO 80307, United States of America

		All rights reserved

These XML tranformation written in XSLT 1.0 and XPATH 1.0 are free software; you can redistribute them and/or modify them under the terms of the GNU General Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any later version.  These XML instance documents are distributed in the hope that they will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this project; if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA -->



    

<!--C. ASSUMPTIONS-->

<!-- **************************************-->

<!--Overarching assumption. The metadata field only appears if it contains data-->

<!--1. Applies to DLESE collection metadata format, version 1.0.00 records-->

<!--2. Assumes a single policy URL when in actuality there may be multiple. The first URL is used-->

<!--3. Assumes content is present in collection required feilds and does not check for the presence of it-->





	<xsl:output method="xml" indent="yes" encoding="UTF-8"/>



<!--D. VARIABLES used throughout the transform-->

<!-- ****************************************************-->

<!--variables for accessing DLESE id prefixes files-->

<xsl:variable name="COLLECT">http://www.dlese.org/dds/services/ddsws1-0?verb=GetRecord&amp;id=</xsl:variable>	





<!--E. TRANSFORMATION CODE-->

<!-- **********************************************************-->

	<xsl:template match="d:collectionRecord">

		<xsl:element name="oai_dc:dc" namespace="http://www.openarchives.org/OAI/2.0/oai_dc/">

			<xsl:attribute name="xsi:schemaLocation">http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd</xsl:attribute>	





<!--dc:title-->

<!--since title is required metadata, no need to check to see if data is present-->

			<xsl:element name="dc:title">

				<xsl:value-of select="d:general/d:fullTitle"/>

			</xsl:element>	



<!--dc:creator-->

			<xsl:apply-templates select="d:lifecycle/d:contributors/d:contributor"/> 

			<!--see template CONTRIBUTOR-->



<!--dc:subject - from subject field-->

			<xsl:apply-templates select="d:general/d:subjects/d:subject" mode="DLESE"/>

			<!--see template SUBJECT mode=DLESE-->



<!--dc:subject - from keyword field-->

			<xsl:apply-templates select="d:general/d:keywords/d:keyword" mode="keywords"/> 

			<!--see template SUBJECT mode=KEYWORDS-->



<!--dc:date-->

<!--collection metadata has date information associated with metadata creation, not with collection creation necessarily-->

<!--therefore transform no date information-->



<!--dc:description-->

<!--a single dc:description field is constructed from the following collection fields: description and gradeRange-->

<!--use a variable to grab all grade ranges so they can be concatenated onto the end of the description field as a comma separated list-->

<!--only a single grade range field can occur in an audience field-->

<!--if the last grade range position is DLESE:To be supplied or DLESE:Not applicable then the comma separated list will end with a comma; otherwise it will be fine; did not do extra programming to rid this end comma situation-->

<!--end comma situation when the last grade range is not DLESE:To be supplied or DLESE:Not applicable are handled gracefully with the comma not appearing-->

<!--repeated grade ranges are not suppressed, except the DLESE:To be supplied and DLESE:Not applicable-->

			<xsl:variable name="allgradeRanges">

				<xsl:for-each select="d:general/d:gradeRanges/d:gradeRange [position() !=last()]">

					<xsl:if test="not(contains(., 'supplied')) and not(contains(., 'Not applicable'))">

						<xsl:value-of select="concat(substring-after(., 'DLESE:'), ', ')"/>

					</xsl:if>

				</xsl:for-each>

				<xsl:for-each select="d:general/d:gradeRanges/d:gradeRange [position() =last()]">

						<xsl:if test="not(contains(., 'supplied')) and not(contains(., 'Not applicable'))">

							<xsl:value-of select="substring-after(., 'DLESE:')"/>

						</xsl:if>

				</xsl:for-each>

			</xsl:variable>



<!--check to see if $allgradeRanges has any data; it may not if a record only had the grade range values of DLESE:To be supplied or DLESE:Not applicable-->

			<xsl:element name="dc:description">

				<xsl:choose>

					<xsl:when test="string-length($allgradeRanges) > 0">

						<xsl:value-of select="concat(d:general/d:description, ' Educational levels: ', $allgradeRanges)"/>

					</xsl:when>

					<xsl:otherwise>

						<xsl:value-of select="d:general/d:description"/>

					</xsl:otherwise>

				</xsl:choose>

			</xsl:element>



		

<!--dc:format - size -->

<!--collection metadata does not have size information-->



<!--dc:format - mimetype-->

<!--DC format should be the mimetype of the DC identifier URL, but DC identifier is written as a contructed URL back to DLESE; so text/html would apply but not really helpful or adding information-->

<!--since collections can have all different types of mimetypes, do not write DC format-->





<!--dc:type-->

<!--no vocabulary mapping is necessary since this is collection metadata-->

<!--use Dublin Core Metadata Initiative (DCMI) vocab term of Collection-->



			<xsl:element name="dc:type">

				<xsl:text>Collection</xsl:text>

			</xsl:element>

	



<!--dc:identifier using collection ID number and collection key-->

<!--if collection metadata is in DLESE, construct a URL back to DLESE-->

<!--not using collection location since it is not required and often is not an appropriate landing page to see resources in a collection-->

<!--not using collection policy URL since it is not appropropiate landing page of a collection either-->

<!--if the collection ID number is not in DLESE, DC identifier is just written with the id number becaue it may have meaning locally-->



<!--use a variables to know which is current processing node (.) and to make comparisons-->

			<xsl:element name="dc:identifier">

				<xsl:variable name="key">

					<xsl:value-of select="d:access/d:key" />

				</xsl:variable>

			

				<xsl:variable name="id">

					<xsl:value-of select="d:metaMetadata/d:catalogEntries/d:catalog/@entry" />

				</xsl:variable>





<!--output variable-->

				<xsl:variable name="inDLESE">

<!--use DLESE webservices to determine if id is in DLESE-->					

					<xsl:variable name="collect">

						<xsl:value-of select="document(concat($COLLECT, $id))//metaMetadata/catalogEntries/catalog/@entry" />

					</xsl:variable>

<!--checking to see collection record id is in DLESE as a collection record-->

					<xsl:if test="$id = $collect">

						<xsl:value-of select="concat('http://www.dlese.org/dds/histogram.do?group=subject&amp;key=', $key)"/>		

					</xsl:if>

				</xsl:variable>



				<xsl:choose>

					<xsl:when test="string-length($inDLESE) > 0">

						<xsl:value-of select="$inDLESE"/>

					</xsl:when>

					<xsl:otherwise>

						<xsl:value-of select="$id"/>

					</xsl:otherwise>

				</xsl:choose>

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

			<xsl:element name="dc:rights">

				<xsl:value-of select="concat('Consult the collection scope statement at: ', d:general/d:policies/d:policy/@url)"/>

			</xsl:element>

	

<!--dc:coverage-->

<!--collection metadata does not have coverage information-->

		

<!--dc:relation-->

<!--use collection location as a related resource-->

<!--collection location is not required metadata, check to see if present-->

			<xsl:if test="string-length(d:access/d:collectionLocation) > 0">

				<xsl:element name="dc:relation">

					<xsl:value-of select="d:access/d:collectionLocation"/>

				</xsl:element>

			</xsl:if>



<!--end oai_dc:dc-->

		</xsl:element>

	</xsl:template>





<!--F. TEMPLATES TO APPLY (alphabetical order)-->

<!--**********************************************************-->

<!--1. CONTRIBUTOR selects DC creator, publisher or contributor based on the ADN contributor role-->

<!--2. GRADERANGE writes DC description last sentence with ADN grade range information-->

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





<!--2. GRADERANGE template-->

	<xsl:template match="d:gradeRange">

		<xsl:choose>

			<xsl:when test="contains(., 'supplied') or contains(., 'Not applicable')"/>

			<xsl:otherwise>

				<xsl:element name="dc:description">

					<xsl:value-of select="concat('Educational level is: ', substring-after(., ':'))"/>

				</xsl:element>

			</xsl:otherwise>

		</xsl:choose>

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

