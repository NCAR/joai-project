<?xml version="1.0"?>
<xsl:stylesheet 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xsi ="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:d="http://adn.dlese.org"
    xmlns:dc="http://purl.org/dc/elements/1.1/"
    xmlns:nsdl_dc="http://ns.nsdl.org/nsdl_dc_v1.02/"
    xmlns:dct="http://purl.org/dc/terms/"
    xmlns:ieee="http://www.ieee.org/xsd/LOMv1p0"
    xmlns:ws="http://www.dlese.org/Metadata/ddsws"
    xmlns:xsd="http://www.w3.org/2001/XMLSchema"
    exclude-result-prefixes="xsi d ws xsd"
    version="1.0">

<!--ORGANIZATION OF THIS FILE-->
<!-- **************************************-->
<!--This file is organized into the following sections: Purpose, License information and credits, Assumptions, Other stylesheets, Variables, Transformation code, Templates-->

<!--PURPOSE-->
<!-- **************************************-->
<!--To transform ADN 0.6.50 metadata records to the NSDL_DC version 1.02 format-->

<!--LICENSE INFORMATION and CREDITS-->
<!-- *****************************************************-->
<!--Date created: 2006-10-26 by Katy Ginger, University Corporation for Atmospheric Research (UCAR)-->
<!--Last modified: 2007-08-18 by Katy Ginger-->
<!--License information:
		Copyright (c) 2006 University Corporation for Atmospheric Research (UCAR)
		P.O. Box 3000, Boulder, CO 80307, United States of America
		email: dlesesupport@ucar.edu.
		All rights reserved
This XML tranformation, written in XSLT 1.0 and XPATH 1.0, are free software; you can redistribute them and/or modify them under the terms of the GNU General Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any later version.  These XML instance documents are distributed in the hope that they will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this project; if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA -->

<!--ASSUMPTIONS-->
<!-- **************************************-->
<!--1. Unless otherwise indicated in this stylesheet, the transform applies to both ADN online and offline resources-->
<!--2. Only the ADN large bounding box and its associated places and events, not detailed geometries, are transformed-->
<!--3. ADN relations.relation.kind=DLESE:Has thumbnail is not transformed-->
<!--4. ADN objectInSpace tags are not transformed-->
<!--5. When ADN named time periods are transformed only the first named time period is transformed, all others are ignored; the encoding scheme does not allow for more than one named time period-->
<!--6. Any ADN timeAD.begin.date or timeAD.end.date with a value of 'Present' are transformed using the dct:Period encoding scheme not the W3CDTF scheme-->
<!--7. Assumes a mime type of text/html from the URL if the URL has content but does not match any of the listed mime types-->
<!--8. Does not assume ADN records are valid so there could be ADN elements with no content present-->
<!--9. Generally checks are made to ensure ADN elements have content before being transformed. In the case of ADN elements with controlled vocabularies, sometimes additional checks are performed to ensure the data is part of the controlled vocabulary (to help reduce the possibility of transforming nonsense). If these checks are not successful occassionally, nonsense is transformed and a DC element is written-->
<!--10. Complete data checking when ADN records are not valid is not possible within the scope of this transform so some nonsense that does not fit desired encoding schemes can creep into the transformed output-->
<!--11. ADN educational.audiences.audience.instructionalGoal  or typicalAgeRange is not transformed-->		
<!--12. When possible NSES standards are transformed to use the ASN identifiers; Geography standards are transformed and outputted as text-->
<!--13. Process and teaching standards are not transformed-->
<!--14. ADN simple places, event and temporal information are transformed-->
<!--15. Extra descriptions associated with each major ADN section are not transformed; only the general description-->
<!--16. For resource type, if content is not part of the ADN vocabulary, the content does not get transformed-->


	<xsl:output method="xml" indent="yes" encoding="UTF-8"/>


<!--VARIABLES used throughout the transform-->
<!-- **********************************************************-->
<!--variable for accessing DLESE webservice when given a DLESE library id-->
<!--webservice 1-1 has namespaces unlike 1-0 so account for them-->
	<xsl:variable name="DDSWSID">http://www.dlese.org/dds/services/ddsws1-1?verb=GetRecord&amp;id=</xsl:variable>	
	<xsl:variable name="reqVocabURL">http://www.dlese.org/Metadata/adn-item/0.6.50/vocabs/requirementTypeDLESE.xsd</xsl:variable>
	<xsl:variable name="teachDVocabURL">http://www.dlese.org/Metadata/adn-item/0.6.50/vocabs/teachingMethodDLESE.xsd</xsl:variable>
	<xsl:variable name="teachGVocabURL">http://www.dlese.org/Metadata/adn-item/0.6.50/vocabs/teachingMethodGEM.xsd</xsl:variable>
	<xsl:variable name="subjVocabURL">http://www.dlese.org/Metadata/adn-item/0.6.50/vocabs/subjectDLESE.xsd</xsl:variable>
	<xsl:variable name="toolDVocabURL">http://www.dlese.org/Metadata/adn-item/0.6.50/vocabs/toolForDLESE.xsd</xsl:variable>
	<xsl:variable name="toolGVocabURL">http://www.dlese.org/Metadata/adn-item/0.6.50/vocabs/toolForGEM.xsd</xsl:variable>
	<xsl:variable name="benDVocabURL">http://www.dlese.org/Metadata/adn-item/0.6.50/vocabs/beneficiaryDLESE.xsd</xsl:variable>
	<xsl:variable name="benGVocabURL">http://www.dlese.org/Metadata/adn-item/0.6.50/vocabs/beneficiaryGEM.xsd</xsl:variable>
	<xsl:variable name="NSESVocabURL">http://www.dlese.org/Metadata/adn-item/0.6.50/vocabs/standardsNSEScontent.xsd</xsl:variable>
	<xsl:variable name="NCGEVocabURL">http://www.dlese.org/Metadata/adn-item/0.6.50/vocabs/standardsNCGE.xsd</xsl:variable>
	<xsl:variable name="NCTMVocabURL">http://www.dlese.org/Metadata/adn-item/0.6.50/vocabs/standardsNCTMcontent.xsd</xsl:variable>
	<xsl:variable name="NETSVocabURL">http://www.dlese.org/Metadata/adn-item/0.6.50/vocabs/standardsNETScontent.xsd</xsl:variable>
	<xsl:variable name="commonVocabURL">http://www.dlese.org/Metadata/adn-item/0.6.50/vocabs/standardsAAASbenchmarksContentCommon.xsd</xsl:variable>
	<xsl:variable name="designVocabURL">http://www.dlese.org/Metadata/adn-item/0.6.50/vocabs/standardsAAASbenchmarksContentDesigned.xsd</xsl:variable>
	<xsl:variable name="habitsVocabURL">http://www.dlese.org/Metadata/adn-item/0.6.50/vocabs/standardsAAASbenchmarksContentHabits.xsd</xsl:variable>
	<xsl:variable name="historyocabURL">http://www.dlese.org/Metadata/adn-item/0.6.50/vocabs/standardsAAASbenchmarksContentHistorical.xsd</xsl:variable>
	<xsl:variable name="humanVocabURL">http://www.dlese.org/Metadata/adn-item/0.6.50/vocabs/standardsAAASbenchmarksContentHuman.xsd</xsl:variable>
	<xsl:variable name="livingVocabURL">http://www.dlese.org/Metadata/adn-item/0.6.50/vocabs/standardsAAASbenchmarksContentLiving.xsd</xsl:variable>
	<xsl:variable name="mathVocabURL">http://www.dlese.org/Metadata/adn-item/0.6.50/vocabs/standardsAAASbenchmarksContentMath.xsd</xsl:variable>
	<xsl:variable name="physicalVocabURL">http://www.dlese.org/Metadata/adn-item/0.6.50/vocabs/standardsAAASbenchmarksContentPhysical.xsd</xsl:variable>
	<xsl:variable name="scienceVocabURL">http://www.dlese.org/Metadata/adn-item/0.6.50/vocabs/standardsAAASbenchmarksContentScience.xsd</xsl:variable>
	<xsl:variable name="techVocabURL">http://www.dlese.org/Metadata/adn-item/0.6.50/vocabs/standardsAAASbenchmarksContentTechnology.xsd</xsl:variable>
	<xsl:variable name="asnIdsURL">http://www.dlese.org/Metadata/documents/xml/ADN-to-ASN-NSES-mappings.xml</xsl:variable>
	<xsl:variable name="asnNCGEids">http://www.dlese.org/Metadata/documents/xml/ADN-to-ASN-NCGE-mappings.xml</xsl:variable>

<!--TRANSFORMATION CODE-->
<!-- **************************************-->
	<xsl:template match="d:itemRecord">
		<nsdl_dc:nsdl_dc schemaVersion="1.02.010" xmlns:nsdl_dc="http://ns.nsdl.org/nsdl_dc_v1.02/" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dct="http://purl.org/dc/terms/" xmlns:ieee="http://www.ieee.org/xsd/LOMv1p0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://ns.nsdl.org/nsdl_dc_v1.02/ http://ns.nsdl.org/schemas/nsdl_dc/nsdl_dc_v1.02.xsd">
		
		

<!--dc:title-->
		<xsl:if test="string-length(d:general/d:title) > 0">
			<xsl:element name="dc:title">
				<xsl:value-of select="d:general/d:title"/>
			</xsl:element>	
		</xsl:if>

<!--dc:subject - from subjects-->
<!--send DLESE subjects if using gem?-->
		<xsl:apply-templates select="d:general/d:subjects/d:subject" mode="subjects"/>
		<!--see template SUBJECT mode=SUBJECTS-->

<!--dc:subject - from keywords-->
		<xsl:apply-templates select="d:general/d:keywords/d:keyword" mode="keywords"/>
		<!--see template SUBJECT mode=KEYWORDS-->

<!--dc:subject - type=nsdl_dc:GEM-->
<!--to prevent nsdl_dc:GEM entries of 'Science', 'Earth science' and 'Physical science'  etc. from appearing so many times, grab all the contents of ADN subject tags and if it contains any of the following listed below, then make a GEM entry-->
<!--NOTE: ADN subjects not mapped to any GEM term: Forestry, Other-->

<!--	variable for ADN general.subjects.subject-->
		<xsl:variable name="allsubjects">
			<xsl:for-each select="d:general/d:subjects/d:subject">
				<xsl:value-of select="."/>
			</xsl:for-each>
		</xsl:variable>
<!--for nsdl_dc:GEM: Science-->
		<xsl:if test="contains($allsubjects, 'Atmospheric') or 
						contains($allsubjects, 'Biology') or
						contains($allsubjects, 'oceanography') or
						contains($allsubjects, 'Chemistry') or
						contains($allsubjects, 'Climatology') or
						contains($allsubjects, 'Cryology') or
						contains($allsubjects, 'Environmental science') or
						contains($allsubjects, 'Geochemistry') or
						contains($allsubjects, 'Geology') or
						contains($allsubjects, 'Geophysics') or
						contains($allsubjects, 'Hydrology') or
						contains($allsubjects, 'Mineralogy') or
						contains($allsubjects, 'Natural hazards') or
						contains($allsubjects, 'Paleontology') or
						contains($allsubjects, 'Physics') or
						contains($allsubjects, 'Soil') or
						contains($allsubjects, 'Space') or
						contains($allsubjects, 'geology')">
			<xsl:element name="dc:subject">
				<xsl:attribute name="xsi:type">nsdl_dc:GEM</xsl:attribute>
				<xsl:text>Science</xsl:text>
			</xsl:element>
		</xsl:if>
			
<!--for nsdl_dc:GEM: Earth science-->
		<xsl:if test="contains($allsubjects, 'Atmospheric') or 
						contains($allsubjects, 'Biology') or
						contains($allsubjects, 'oceanography') or
						contains($allsubjects, 'Climatology') or
						contains($allsubjects, 'Cryology') or
						contains($allsubjects, 'Environmental science') or
						contains($allsubjects, 'Geochemistry') or
						contains($allsubjects, 'Geologic time') or
						contains($allsubjects, 'Geology') or
						contains($allsubjects, 'Geophysics') or
						contains($allsubjects, 'Hydrology') or
						contains($allsubjects, 'Mineralogy') or
						contains($allsubjects, 'Natural hazards') or
						contains($allsubjects, 'Paleontology') or
						contains($allsubjects, 'Physical geography') or
						contains($allsubjects, 'Space') or
						contains($allsubjects, 'geology')">
			<xsl:element name="dc:subject">
				<xsl:attribute name="xsi:type">nsdl_dc:GEM</xsl:attribute>
				<xsl:text>Earth science</xsl:text>
			</xsl:element>
		</xsl:if>

<!--for nsdl_dc:GEM: Physical sciences-->
		<xsl:if test="contains($allsubjects, 'Atmospheric') or 
						contains($allsubjects, 'Climatology') or
						contains($allsubjects, 'Cryology') or
						contains($allsubjects, 'Environmental science') or
						contains($allsubjects, 'Geochemistry') or
						contains($allsubjects, 'Geologic time') or
						contains($allsubjects, 'Geology') or
						contains($allsubjects, 'Geophysics') or
						contains($allsubjects, 'Hydrology') or
						contains($allsubjects, 'Mineralogy') or
						contains($allsubjects, 'Natural hazards') or
						contains($allsubjects, 'Paleontology') or
						contains($allsubjects, 'Physical geography') or
						contains($allsubjects, 'Physical oceanography') or
						contains($allsubjects, 'Space') or
						contains($allsubjects, 'geology')">
			<xsl:element name="dc:subject">
				<xsl:attribute name="xsi:type">nsdl_dc:GEM</xsl:attribute>
				<xsl:text>Physical sciences</xsl:text>
			</xsl:element>
		</xsl:if>

<!--for nsdl_dc:GEM: Meteorology-->
		<xsl:if test="contains($allsubjects, 'Atmospheric') or 
						contains($allsubjects, 'Climatology')">
			<xsl:element name="dc:subject">
				<xsl:attribute name="xsi:type">nsdl_dc:GEM</xsl:attribute>
				<xsl:text>Meteorology</xsl:text>
			</xsl:element>
		</xsl:if>

<!--for nsdl_dc:GEM: Geology-->
<!--still test Geology, even though it will be present form the DLESE subject list, because now it is part of the GEM type-->
		<xsl:if test="contains($allsubjects, 'Geology') or
						contains($allsubjects, 'Geochemistry') or
						contains($allsubjects, 'Geophysics') or
						contains($allsubjects, 'Geologic time') or
						contains($allsubjects, 'Paleontology') or
						contains($allsubjects, 'Mineralogy') or
						contains($allsubjects, 'geology')">
			<xsl:element name="dc:subject">
				<xsl:attribute name="xsi:type">nsdl_dc:GEM</xsl:attribute>
				<xsl:text>Geology</xsl:text>
			</xsl:element>
		</xsl:if>

<!--for nsdl_dc:GEM: Geography-->
		<xsl:if test="contains($allsubjects, 'geography')">
			<xsl:element name="dc:subject">
				<xsl:attribute name="xsi:type">nsdl_dc:GEM</xsl:attribute>
				<xsl:text>Geography</xsl:text>
			</xsl:element>
		</xsl:if>
			
<!--for nsdl_dc:GEM: Oceanogrpahy-->
		<xsl:if test="contains($allsubjects, 'oceanography')">
			<xsl:element name="dc:subject">
				<xsl:attribute name="xsi:type">nsdl_dc:GEM</xsl:attribute>
				<xsl:text>Oceanography</xsl:text>
			</xsl:element>
		</xsl:if>
				
<!--for nsdl_dc:GEM: Chemistry-->
<!--still test Chemistry, even though it will be present form the DLESE subject list, because now it is part of the GEM type-->
		<xsl:if test="contains($allsubjects, 'Chemistry') or
						contains($allsubjects, 'Chemical oceanography') or
						contains($allsubjects, 'Geochemistry')">
			<xsl:element name="dc:subject">
				<xsl:attribute name="xsi:type">nsdl_dc:GEM</xsl:attribute>
				<xsl:text>Chemistry</xsl:text>
			</xsl:element>
		</xsl:if>

<!--for nsdl_dc:GEM: Agriculture-->
		<xsl:if test="contains(., 'Agriculture')">
			<xsl:element name="dc:subject">
				<xsl:attribute name="xsi:type">nsdl_dc:GEM</xsl:attribute>
				<xsl:text>Agriculture</xsl:text>
			</xsl:element>
		</xsl:if>

<!--for nsdl_dc:GEM: Biology-->
		<xsl:if test="contains(., 'Biology')">
			<xsl:element name="dc:subject">
				<xsl:attribute name="xsi:type">nsdl_dc:GEM</xsl:attribute>
				<xsl:text>Biology</xsl:text>
			</xsl:element>
		</xsl:if>

<!--for nsdl_dc:GEM: Ecology-->
		<xsl:if test="contains(., 'Ecology')">
			<xsl:element name="dc:subject">
				<xsl:attribute name="xsi:type">nsdl_dc:GEM</xsl:attribute>
				<xsl:text>Ecology</xsl:text>
			</xsl:element>
		</xsl:if>

<!--for nsdl_dc:GEM: Education-->
		<xsl:if test="contains(., 'Educational')">
			<xsl:element name="dc:subject">
				<xsl:attribute name="xsi:type">nsdl_dc:GEM</xsl:attribute>
				<xsl:text>Education (General)</xsl:text>
			</xsl:element>
		</xsl:if>

<!--for nsdl_dc:GEM: History of science-->
		<xsl:if test="contains(., 'History')">
			<xsl:element name="dc:subject">
				<xsl:attribute name="xsi:type">nsdl_dc:GEM</xsl:attribute>
				<xsl:text>History of science</xsl:text>
			</xsl:element>
		</xsl:if>

<!--for nsdl_dc:GEM: Mathematics-->
		<xsl:if test="contains(., 'Mathematics')">
			<xsl:element name="dc:subject">
				<xsl:attribute name="xsi:type">nsdl_dc:GEM</xsl:attribute>
				<xsl:text>Mathematics</xsl:text>
			</xsl:element>
		</xsl:if>

<!--for nsdl_dc:GEM: Natural history-->
		<xsl:if test="contains(., 'Paleontology')">
			<xsl:element name="dc:subject">
				<xsl:attribute name="xsi:type">nsdl_dc:GEM</xsl:attribute>
				<xsl:text>Natural history</xsl:text>
			</xsl:element>
		</xsl:if>

<!--for nsdl_dc:GEM: Paleontology-->
		<xsl:if test="contains(., 'Paleontology')">
			<xsl:element name="dc:subject">
				<xsl:attribute name="xsi:type">nsdl_dc:GEM</xsl:attribute>
				<xsl:text>Paleontology</xsl:text>
			</xsl:element>
		</xsl:if>

<!--for nsdl_dc:GEM: Physics-->
		<xsl:if test="contains(., 'Physics')">
			<xsl:element name="dc:subject">
				<xsl:attribute name="xsi:type">nsdl_dc:GEM</xsl:attribute>
				<xsl:text>Physics</xsl:text>
			</xsl:element>
		</xsl:if>

<!--for nsdl_dc:GEM: Astronomy-->
		<xsl:if test="contains(., 'Space')">
			<xsl:element name="dc:subject">
				<xsl:attribute name="xsi:type">nsdl_dc:GEM</xsl:attribute>
				<xsl:text>Astronomy</xsl:text>
			</xsl:element>
		</xsl:if>

<!--for nsdl_dc:GEM: Space-->
		<xsl:if test="contains(., 'Space')">
			<xsl:element name="dc:subject">
				<xsl:attribute name="xsi:type">nsdl_dc:GEM</xsl:attribute>
				<xsl:text>Space sciences</xsl:text>
			</xsl:element>
		</xsl:if>

<!--for nsdl_dc:GEM: Technology-->
		<xsl:if test="contains(., 'Technology')">
			<xsl:element name="dc:subject">
				<xsl:attribute name="xsi:type">nsdl_dc:GEM</xsl:attribute>
				<xsl:text>Technology</xsl:text>
			</xsl:element>
		</xsl:if>
<!--end: for nsdl_dc:GEM-->
<!--end dc:subject-->

<!--dc:date-->
<!--since ADN has many lifecycle.contributors.contributor.date values and these are not required, determine if any are present using a variable to grab all of them-->
 
<!--	variable for ADN lifecycle.contributors.contributor.date-->
		<xsl:variable name="alldates">
			<xsl:for-each select="d:lifecycle/d:contributors/d:contributor/@date">
				<xsl:value-of select="."/>
			</xsl:for-each>
		</xsl:variable>
		
		<xsl:if test="string-length($alldates)>0">
		<!--variable to grab the date of the first occurring date attribute only-->
			<xsl:variable name="dateStr" select="string(normalize-space(d:lifecycle/d:contributors/d:contributor/@date))"/>
			<xsl:variable name="lowercase" select="translate($dateStr, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')"/>
<!-- variables for format checking -->
			<xsl:variable name="yearStr" select="substring($lowercase, 1, 4)"/>
			<xsl:variable name="firstSep" select="substring($lowercase, 5, 1)"/>
			<xsl:variable name="monthStr" select="substring($lowercase, 6, 2)"/>
			<xsl:variable name="secondSep" select="substring($lowercase, 8, 1)"/>
			<xsl:variable name="dayStr" select="substring($lowercase, 9, 2)"/>
	
<!--checking for xsd:gYear (but assuming in CCYY format because to hard to do more) -->
<!--checking for xsd:gYearMonth (but assuming in CCYY-MM format because to hard to do more) -->
<!--checking for xsd:date (but assuming in CCYY-MM-DD format because to hard to do more) -->
<!--NOTE: not checking for correct association of day with month (i.e. 2-30, 9-31 would be 'valid' here )-->
<!--NOTE: xsd:dateTime is valid, not checking for it -->	
<!--NOTE: anything else does not get transformed-->						
			<xsl:if test="(string-length($lowercase) = '4' and not(string(number($yearStr)) = 'NaN')) or 
							(string-length($lowercase) = '7' and not(string(number($yearStr)) = 'NaN') and ($firstSep = '-') and
											not (string(number($monthStr)) = 'NaN') and ($monthStr &gt; '0' ) and ($monthStr &lt; '13' )) or
							(string-length($lowercase) = '10' and	not (string(number($yearStr)) = 'NaN')  and 
											($firstSep = '-') and not (string(number($monthStr)) = 'NaN') and
											 ($monthStr &gt; '0' ) and ($monthStr &lt; '13' ) and ($secondSep = '-') and
											 not (string(number($dayStr)) = 'NaN') and ($dayStr &gt; '0' ) and ($dayStr &lt; '32' ))">
				<xsl:element name="dc:date">
					<xsl:attribute name="xsi:type">dct:W3CDTF</xsl:attribute>
					<xsl:value-of select="d:lifecycle/d:contributors/d:contributor/@date"/>
				</xsl:element>	
			</xsl:if>
		</xsl:if>	
<!--end: dc:date-->

<!--dc:description-->
		<xsl:choose>
<!--for description, concatenate ADN general.description, technical.offline.objectDescription and technical.offline.accessInformation-->

<!--an offline resource with access information, a general description and an object description-->
			<xsl:when test="string-length(d:technical/d:offline/d:accessInformation)>0 and  string-length(d:technical/d:offline/d:objectDescription)>0 and string-length(d:general/d:description)>0">
				<xsl:element name="dc:description">
					<xsl:value-of select="concat(d:general/d:description,' ', d:technical/d:offline/d:objectDescription,' This is an offline resource with the following access information: ',d:technical/d:offline/d:accessInformation)"/>
				</xsl:element>
			</xsl:when>
<!--an offline resource with a general description and an object description-->
			<xsl:when test="string-length(d:technical/d:offline/d:objectDescription)>0 and string-length(d:general/d:description)>0">
				<xsl:element name="dc:description">
					<xsl:value-of select="concat(d:general/d:description,' ', d:technical/d:offline/d:objectDescription)"/>
				</xsl:element>
			</xsl:when>
<!--an offline resource with access information, a general description-->
			<xsl:when test="string-length(d:technical/d:offline/d:accessInformation)>0 and string-length(d:general/d:description)>0">
				<xsl:element name="dc:description">
					<xsl:value-of select="concat(d:general/d:description,' This is an offline resource with the following access information: ',d:technical/d:offline/d:accessInformation)"/>
				</xsl:element>
			</xsl:when>
<!--an offline resource with access information and an object description-->
			<xsl:when test="string-length(d:technical/d:offline/d:accessInformation)>0 and  string-length(d:technical/d:offline/d:objectDescription)>0">
				<xsl:element name="dc:description">
					<xsl:value-of select="concat(d:technical/d:offline/d:objectDescription,' This is an offline resource with the following access information: ',d:technical/d:offline/d:accessInformation)"/>
				</xsl:element>
			</xsl:when>
<!--an online or offline resource with just a general.description-->
			<xsl:when test="string-length(d:general/d:description)>0">
				<xsl:element name="dc:description">
					<xsl:value-of select="d:general/d:description"/>
				</xsl:element>
			</xsl:when>
<!--if there is no no content in any of the above elements do nothing-->
			<xsl:otherwise/>
		</xsl:choose>

<!--ADN has the notion of a contact in addition to creator, publisher or contributor; however, contact is not mapped-->
<!--dc:creator - person-->
		<xsl:apply-templates select="d:lifecycle/d:contributors/d:contributor/d:person" mode="creator"/>
		<xsl:apply-templates select="d:lifecycle/d:contributors/d:contributor/d:organization" mode="creator"/>
		<!--see template PERSON mode=CREATOR-->
		<!--see template ORGANIZATION mode=CREATOR-->

<!--dc:publisher-->
		<xsl:apply-templates select="d:lifecycle/d:contributors/d:contributor/d:person" mode="publisher"/>
		<xsl:apply-templates select="d:lifecycle/d:contributors/d:contributor/d:organization" mode="publisher"/>
		<!--see template PERSON mode=PUBLISHER-->
		<!--see template ORGANIZATION mode=PUBLISHER-->
	
<!--dc:contributor-->
		<xsl:apply-templates select="d:lifecycle/d:contributors/d:contributor/d:person" mode="contributor"/>
		<xsl:apply-templates select="d:lifecycle/d:contributors/d:contributor/d:organization" mode="contributor"/>
		<!--see template PERSON mode=CONTRIBUTOR-->
		<!--see template ORGANIZATION mode=CONTRIBUTOR-->
	

<!--dc:format-->
<!--dc:format is defined as the physical or digital manifestation of the resource. Format may include the media-type (mime), dimensions, size and duration and format may be used to determine the software, hardware or other equipment needed to display or operate the resource-->
<!--there is no dc:format - plain or dc:format - type=dct:IMT for ADN offline resources-->
<!--use the combination of technical.online.mediums.medium and the technical.online.primaryURL to determine dc:format concept of mime type and write it as the dc:format element with a dct:IMT type attribute-->
<!--use technical.online.requirements and technical.online.otherRequirements to determine dc:format concept of software or hardware (or other equipment) needed to operate or display the resource and write it as a plain dc:format element-->
<!--use technical.online.size to determine dc:formt concept of size and write it as the dct:extent element-->

<!--dc:format using dct:extent (size)-->
<!--no dct:extent - size for ADN offline resources-->
		<xsl:if test="string-length(d:technical/d:online/d:size) > 0">
			<xsl:element name="dct:extent">
				<xsl:value-of select="d:technical/d:online/d:size"/>
			</xsl:element>
		</xsl:if>

<!--dc:format using dct:medium (medium)-->
<!--use for offline resources only because the DC definition is the material or physical carrier of the resource; e.g. bronze for a sculpture-->
<!--however, ADN does not collect medium information for offline resources; so do not create a dct:medium element-->

<!--dc:format - type=dct:IMT using ADN mediums--> 
<!--must select from NSDL list at http://ns.nsdl.org/schemas/mime_type/mime_type_v1.00.xsd-->
<!--since mediums is free text in ADN, create a variable and test and only write dc:format - type=dct:IMT as needed at the broad mime type category (i.e. text, application, video, audio, image, model, multipart or message) because do not know if the finer free text level (e.g. text/html) in ADN medium would be an accepted term in the mime type vocabulary-->

<!--	variable for ADN technical.online.mediums/medium-->
		<xsl:variable name="mediums">
			<xsl:for-each select="d:technical/d:online/d:mediums/d:medium">
				<xsl:value-of select="."/>
			</xsl:for-each>
		</xsl:variable>
		<xsl:variable name="allmediums" select="translate($mediums, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')"/>

<!--test to see if ADN medium information is present, if so write the dc:format type=dct:IMT at a broad mime type category-->
<!--the set of if statements rather than a template eliminates repeating output elements with the same content but allows for repeating output elements with different content-->
<!--this has been verified as working-->
		<xsl:if test="contains($allmediums, 'application')">
			<xsl:element name="dc:format">
				<xsl:attribute name="xsi:type">dct:IMT</xsl:attribute>
				<xsl:text>application</xsl:text>
			</xsl:element>
		</xsl:if> 
		<xsl:if test="contains($allmediums, 'message')">
			<xsl:element name="dc:format">
				<xsl:attribute name="xsi:type">dct:IMT</xsl:attribute>
				<xsl:text>message</xsl:text>
			</xsl:element>
		</xsl:if> 
		<xsl:if test="contains($allmediums, 'video')">
			<xsl:element name="dc:format">
				<xsl:attribute name="xsi:type">dct:IMT</xsl:attribute>
				<xsl:text>video</xsl:text>
			</xsl:element>
		</xsl:if> 
		<xsl:if test="contains($allmediums, 'audio')">
			<xsl:element name="dc:format">
				<xsl:attribute name="xsi:type">dct:IMT</xsl:attribute>
				<xsl:text>audio</xsl:text>
			</xsl:element>
		</xsl:if> 
		<xsl:if test="contains($allmediums, 'image')">
			<xsl:element name="dc:format">
				<xsl:attribute name="xsi:type">dct:IMT</xsl:attribute>
				<xsl:text>image</xsl:text>
			</xsl:element>
		</xsl:if> 
		<xsl:if test="contains($allmediums, 'model')">
			<xsl:element name="dc:format">
				<xsl:attribute name="xsi:type">dct:IMT</xsl:attribute>
				<xsl:text>model</xsl:text>
			</xsl:element>
		</xsl:if> 
		<xsl:if test="contains($allmediums, 'multipart')">
			<xsl:element name="dc:format">
				<xsl:attribute name="xsi:type">dct:IMT</xsl:attribute>
				<xsl:text>multipart</xsl:text>
			</xsl:element>
		</xsl:if> 
		<xsl:if test="contains($allmediums, 'text')">
			<xsl:element name="dc:format">
				<xsl:attribute name="xsi:type">dct:IMT</xsl:attribute>
				<xsl:text>text</xsl:text>
			</xsl:element>
		</xsl:if> 
		
		
<!--dc:format - type=dct:IMT using ADN primaryURL-->
		<xsl:if test="string-length(d:technical/d:online/d:primaryURL) > 0">
			<xsl:call-template name="mimetype"/>
		</xsl:if>

<!--dc:format - plain using ADN technical.requirements.requirement-->
<!--since ADN technical.requirements.requirement is a compound repeating tag, use a template-->
		<xsl:apply-templates select="d:technical/d:online/d:requirements/d:requirement"/>
		<!--see template REQUIREMENT-->

<!--dc:format - plain using ADN technical.otherRequirements.otherRequirement-->
<!--since ADN technical.otherRequirements.otherRequirement is a compound repeating tag, use a template-->
		<xsl:apply-templates select="d:technical/d:online/d:otherRequirements/d:otherRequirement"/>
		<!--see template OTHER REQUIREMENT-->
	
<!--dc:type-->
<!--use ADN educational.resourceTypes.resourceType and map to NSDL vocab; this excludes ADN terms from output-->
<!--vocabulary mapping is necessary-->
<!--to prevent duplicate tags from appearing, make a variable and test it-->

<!--	variable for ADN educational.resourceTypes.resourceType-->
		<xsl:variable name="allresourceTypes">
			<xsl:for-each select="d:educational/d:resourceTypes/d:resourceType">
				<xsl:value-of select="."/>
			</xsl:for-each>
		</xsl:variable>
	
<!--dc:type - plain-->
<!--includes those ADN resource type terms that do not map to the DCMI type or NSDL_DC type-->
<!--this includes the ADN terms of Calculation or conversion tool, Code, Software, Scientific visualization because they do not map to NSDL_DC-->
<!--included sound, text:book and audio:book here as well because they do not map well into NSDL_DC either-->

<!--dc:type - plain: Calculation or conversion tool-->
		<xsl:if test="contains($allresourceTypes, 'Calculation or conversion tool')">
			<xsl:element name="dc:type">
				<xsl:text>Calculation or Conversion Tool</xsl:text>
			</xsl:element>
		</xsl:if>
<!--dc:type - plain: Code-->
		<xsl:if test="contains($allresourceTypes, 'Code')">
			<xsl:element name="dc:type">
				<xsl:text>Code</xsl:text>
			</xsl:element>
		</xsl:if>
<!--dc:type - plain: Software-->
		<xsl:if test="contains($allresourceTypes, 'Software')">
			<xsl:element name="dc:type">
				<xsl:text>Software</xsl:text>
			</xsl:element>
		</xsl:if>
<!--dc:type - plain: Scientific visualization-->
		<xsl:if test="contains($allresourceTypes, 'Scientific visualization')">
			<xsl:element name="dc:type">
				<xsl:text>Scientific Visualization</xsl:text>
			</xsl:element>
		</xsl:if>
<!--dc:type - plain: Audio book-->
		<xsl:if test="contains($allresourceTypes, 'Audio book')">
			<xsl:element name="dc:type">
				<xsl:text>Audio Book</xsl:text>
			</xsl:element>
		</xsl:if>
<!--dc:type - plain: Book-->
		<xsl:if test="contains($allresourceTypes, 'Book')">
			<xsl:element name="dc:type">
				<xsl:text>Book</xsl:text>
			</xsl:element>
		</xsl:if>
<!--dc:type - plain: Sound-->
		<xsl:if test="contains($allresourceTypes, 'Sound')">
			<xsl:element name="dc:type">
				<xsl:text>Sound</xsl:text>
			</xsl:element>
		</xsl:if>

<!--dc:type - dct:DCMI -->
<!--maps ADN resource type terms to the DCMI list of terms-->
<!--dc:type - dct:DCMI: Collection-->
		<xsl:if test="contains($allresourceTypes, 'DLESE:Portal') or contains($allresourceTypes, 'Clearinghouse')">
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">dct:DCMIType</xsl:attribute>
				<xsl:text>Collection</xsl:text>
			</xsl:element>
		</xsl:if>
<!--dc:type - dct:DCMI: Dataset-->
		<xsl:if test="contains($allresourceTypes, 'DLESE:Data')">
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">dct:DCMIType</xsl:attribute>
				<xsl:text>Dataset</xsl:text>
			</xsl:element>
		</xsl:if>
<!--dc:type - dct:DCMI: Event-->
		<xsl:if test="contains($allresourceTypes, 'Webcast')">
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">dct:DCMIType</xsl:attribute>
				<xsl:text>Event</xsl:text>
			</xsl:element>
		</xsl:if>
<!--dc:type - dct:DCMI: Image-->
		<xsl:if test="contains($allresourceTypes, 'DLESE:Visual')">
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">dct:DCMIType</xsl:attribute>
				<xsl:text>Image</xsl:text>
			</xsl:element>
		</xsl:if>
<!--dc:type - dct:DCMI: InteractiveResource-->
		<xsl:if test="contains($allresourceTypes, 'DLESE:Learning') or contains($allresourceTypes, 'Calculation')">
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">dct:DCMIType</xsl:attribute>
				<xsl:text>InteractiveResource</xsl:text>
			</xsl:element>
		</xsl:if>
<!--dc:type - dct:DCMI: Service-->
		<xsl:if test="contains($allresourceTypes, 'DLESE:Service')">
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">dct:DCMIType</xsl:attribute>
				<xsl:text>Service</xsl:text>
			</xsl:element>
		</xsl:if>
<!--dc:type - dct:DCMI: Software-->
		<xsl:if test="contains($allresourceTypes, 'Code') or contains($allresourceTypes, 'Software')">
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">dct:DCMIType</xsl:attribute>
				<xsl:text>Software</xsl:text>
			</xsl:element>
		</xsl:if>
<!--dc:type - dct:DCMI: Sound-->
		<xsl:if test="contains($allresourceTypes, 'Audio book') or contains($allresourceTypes, 'Lecture') or contains($allresourceTypes, 'Music') or contains($allresourceTypes, 'Oral') or contains($allresourceTypes, 'Radio') or contains($allresourceTypes, 'Sound')">
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">dct:DCMIType</xsl:attribute>
				<xsl:text>Sound</xsl:text>
			</xsl:element>
		</xsl:if>
<!--dc:type - dct:DCMI: Text-->
		<xsl:if test="contains($allresourceTypes, 'DLESE:Text')">
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">dct:DCMIType</xsl:attribute>
				<xsl:text>Text</xsl:text>
			</xsl:element>
		</xsl:if>
<!--dc:type - dct:DCMI: PhysicalObject-->
		<xsl:if test="string-length(d:technical/d:offline/d:accessInformation)>0 or string-length(d:technical/d:offline/d:objectDescription)>0 ">
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">dct:DCMIType</xsl:attribute>
				<xsl:text>PhysicalObject</xsl:text>
			</xsl:element>
		</xsl:if>
<!--end dc:type - dct:DCMI-->

<!--dc:type - nsdl_dc:-->
<!--maps ADN resource type terms to the NSDL list of terms-->
<!--vocabulary mapping is necessary-->
<!--to prevent duplicate dc:type - nsdl_dc tags from appearing, test the $allresourceTypes variable-->
<!--if this becomes required then need to map terms that appear in dc:type - plain somehow)-->

<!--dc:type - nsdl_dc: Audio/Visual:Voice Recording-->
		<xsl:if test="contains($allresourceTypes, 'Audio book') or contains($allresourceTypes, 'Oral history')">
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Audio/Visual</xsl:text>
			</xsl:element>
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Voice Recording</xsl:text>
			</xsl:element>
		</xsl:if>
<!--dc:type - nsdl_dc: Audio/Visual; Event:Broadcast-->
		<xsl:if test="contains($allresourceTypes, 'webcast') or contains($allresourceTypes, 'Radio broadcast')">
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Audio/Visual</xsl:text>
			</xsl:element>
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Event</xsl:text>
			</xsl:element>
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Broadcast</xsl:text>
			</xsl:element>
		</xsl:if>
<!--dc:type - nsdl_dc: Audio/Visual:Movie/Animation-->
		<xsl:if test="contains($allresourceTypes, 'Video')">
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Audio/Visual</xsl:text>
			</xsl:element>
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Movie/Animation</xsl:text>
			</xsl:element>
		</xsl:if>
<!--dc:type - nsdl_dc: Instructional Material:Lecture/Presentation; Instructional Material:Demonstration-->
		<xsl:if test="contains($allresourceTypes, 'Presentation or demonstration')">
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Instructional Material</xsl:text>
			</xsl:element>
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Lecture/Presentation</xsl:text>
			</xsl:element>
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Demonstration</xsl:text>
			</xsl:element>
		</xsl:if>
<!--dc:type - nsdl_dc: Instructional Material:Lecture/Presentation-->
		<xsl:if test="contains($allresourceTypes, 'Lecture')">
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Instructional Material</xsl:text>
			</xsl:element>
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Lecture/Presentation</xsl:text>
			</xsl:element>
		</xsl:if>
<!--dc:type - nsdl_dc: Audio/Visual:Music-->
		<xsl:if test="contains($allresourceTypes, 'Music')">
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Audio/Visual</xsl:text>
			</xsl:element>
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Music</xsl:text>
			</xsl:element>
		</xsl:if>
<!--dc:type - nsdl_dc: Audio/Visual:Sound-->
		<xsl:if test="contains($allresourceTypes, 'Sound')">
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Audio/Visual</xsl:text>
			</xsl:element>
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Sound</xsl:text>
			</xsl:element>
		</xsl:if>
<!--dc:type - nsdl_dc: Assessment Material-->
		<xsl:if test="contains($allresourceTypes, 'Assessment')">
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Assessment Material</xsl:text>
			</xsl:element>
		</xsl:if>
<!--dc:type - nsdl_dc: Instructional Material:Case Study-->
		<xsl:if test="contains($allresourceTypes, 'Case study')">
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Instructional Material</xsl:text>
			</xsl:element>
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Case Study</xsl:text>
			</xsl:element>
		</xsl:if>
<!--dc:type - nsdl_dc: Instructional Material:Lesson/Lesson Plan-->
		<xsl:if test="contains($allresourceTypes, 'Lesson')">
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Instructional Material</xsl:text>
			</xsl:element>
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Lesson/Lesson Plan</xsl:text>
			</xsl:element>
		</xsl:if>
<!--dc:type - nsdl_dc: Instructional Material:Activity-->
		<xsl:if test="contains($allresourceTypes, 'activity')">
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Instructional Material</xsl:text>
			</xsl:element>
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Activity</xsl:text>
			</xsl:element>
		</xsl:if>
<!--dc:type - nsdl_dc: Instructional Material:Experiment/Lab Activity-->
		<xsl:if test="contains($allresourceTypes, 'Lab')">
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Instructional Material</xsl:text>
			</xsl:element>
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Experiment/Lab Activity</xsl:text>
			</xsl:element>
		</xsl:if>
<!--dc:type - nsdl_dc: Instructional Material:Unit of Instruction-->
		<xsl:if test="contains($allresourceTypes, 'Module or unit')">
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Instructional Material</xsl:text>
			</xsl:element>
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Unit of Instruction</xsl:text>
			</xsl:element>
		</xsl:if>
<!--dc:type - nsdl_dc: Instructional Material:Course-->
		<xsl:if test="contains($allresourceTypes, 'Course')">
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Instructional Material</xsl:text>
			</xsl:element>
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Course</xsl:text>
			</xsl:element>
		</xsl:if>
<!--dc:type - nsdl_dc: Instructional Material:Curriculum-->
		<xsl:if test="contains($allresourceTypes, 'Curriculum')">
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Instructional Material</xsl:text>
			</xsl:element>
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Curriculum</xsl:text>
			</xsl:element>
		</xsl:if>
<!--dc:type - nsdl_dc: Instructional Material:Instructor Guide/Manual-->
		<xsl:if test="contains($allresourceTypes, 'Field trip guide') or contains($allresourceTypes, 'Instructor guide')">
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Instructional Material</xsl:text>
			</xsl:element>
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Instructor Guide/Manual</xsl:text>
			</xsl:element>
		</xsl:if>
<!--dc:type - nsdl_dc: Instructional Material:Problem Set-->
		<xsl:if test="contains($allresourceTypes, 'Problem set')">
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Instructional Material</xsl:text>
			</xsl:element>
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Problem Set</xsl:text>
			</xsl:element>
		</xsl:if>
<!--dc:type - nsdl_dc: Project-->
		<xsl:if test="contains($allresourceTypes, 'Project')">
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Instructional Material</xsl:text>
			</xsl:element>
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Project</xsl:text>
			</xsl:element>
		</xsl:if>
<!--dc:type - nsdl_dc: Instructional Material:Syllabus-->
		<xsl:if test="contains($allresourceTypes, 'Syllabus')">
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Instructional Material</xsl:text>
			</xsl:element>
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Syllabus</xsl:text>
			</xsl:element>
		</xsl:if>
<!--dc:type - nsdl_dc: Instructional Material:Tutorial-->
		<xsl:if test="contains($allresourceTypes, 'Tutorial')">
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Instructional Material</xsl:text>
			</xsl:element>
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Tutorial</xsl:text>
			</xsl:element>
		</xsl:if>
<!--dc:type - nsdl_dc: Instructional Material:Field Trip-->
		<xsl:if test="contains($allresourceTypes, 'Virtual field trip')">
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Instructional Material</xsl:text>
			</xsl:element>
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Field Trip</xsl:text>
			</xsl:element>
		</xsl:if>
<!--dc:type - nsdl_dc: Reference Material:Artifact-->
		<xsl:if test="contains($allresourceTypes, 'Physical object')">
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Reference Material</xsl:text>
			</xsl:element>
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Specimen</xsl:text>
			</xsl:element>
		</xsl:if>
<!--dc:type - nsdl_dc: Community:Ask-an-Expert-->
		<xsl:if test="contains($allresourceTypes, 'Ask an expert')">
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Community</xsl:text>
			</xsl:element>
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Ask-an-Expert</xsl:text>
			</xsl:element>
		</xsl:if>
<!--dc:type - nsdl_dc: Community:Forum-->
		<xsl:if test="contains($allresourceTypes, 'Forum') or contains($allresourceTypes, 'Message board')">
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Community</xsl:text>
			</xsl:element>
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Forum</xsl:text>
			</xsl:element>
		</xsl:if>
<!--dc:type - nsdl_dc: Community:Listserv-->
		<xsl:if test="contains($allresourceTypes, 'Listserv')">
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Community</xsl:text>
			</xsl:element>
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Listserv</xsl:text>
			</xsl:element>
		</xsl:if>
<!--dc:type - nsdl_dc: Tool:Search Engine-->
		<xsl:if test="contains($allresourceTypes, 'Search engine')">
			<xsl:element name="dc:type">
				<xsl:text>Tool</xsl:text>
			</xsl:element>
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Search Engine</xsl:text>
			</xsl:element>
		</xsl:if>
<!--dc:type - nsdl_dc: Tool:Software-->
		<xsl:if test="contains($allresourceTypes, 'Software')">
			<xsl:element name="dc:type">
				<xsl:text>Tool</xsl:text>
			</xsl:element>
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Software</xsl:text>
			</xsl:element>
		</xsl:if>
<!--dc:type - nsdl_dc: Reference Material:Abstract-->
		<xsl:if test="contains($allresourceTypes, 'Abstract')">
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Reference Material</xsl:text>
			</xsl:element>
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Abstract</xsl:text>
			</xsl:element>
		</xsl:if>
<!--dc:type - nsdl_dc: Instructional Material:Textbook-->
		<xsl:if test="contains($allresourceTypes, 'Text:Book')">
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Instructional Material</xsl:text>
			</xsl:element>
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Textbook</xsl:text>
			</xsl:element>
		</xsl:if>
<!--dc:type - nsdl_dc: Tool:Form-->
		<xsl:if test="contains($allresourceTypes, 'Calculation')">
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Tool</xsl:text>
			</xsl:element>
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Form</xsl:text>
			</xsl:element>
		</xsl:if>
<!--dc:type - nsdl_dc: Reference Material-->
		<xsl:if test="contains($allresourceTypes, 'Reference')">
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Reference Material</xsl:text>
			</xsl:element>
		</xsl:if>
<!--dc:type - nsdl_dc: Reference Material:Glossary/Index-->
		<xsl:if test="contains($allresourceTypes, 'Glossary') or contains($allresourceTypes, 'Index or bibliography')">
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Reference Material</xsl:text>
			</xsl:element>
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Glossary/Index</xsl:text>
			</xsl:element>
		</xsl:if>
<!--dc:type - nsdl_dc: Reference Material:Article-->
		<xsl:if test="contains($allresourceTypes, 'Journal article')">
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Reference Material</xsl:text>
			</xsl:element>
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Article</xsl:text>
			</xsl:element>
		</xsl:if>
<!--dc:type - nsdl_dc: Reference Material:Periodical-->
		<xsl:if test="contains($allresourceTypes, 'Periodical')">
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Reference Material</xsl:text>
			</xsl:element>
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Periodical</xsl:text>
			</xsl:element>
		</xsl:if>
<!--dc:type - nsdl_dc: Reference Material:Policy-->
		<xsl:if test="contains($allresourceTypes, 'Policy')">
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Reference Material</xsl:text>
			</xsl:element>
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Policy</xsl:text>
			</xsl:element>
		</xsl:if>
<!--dc:type - nsdl_dc: Reference Material:Proceedings-->
		<xsl:if test="contains($allresourceTypes, 'Proceedings')">
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Reference Material</xsl:text>
			</xsl:element>
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Proceedings</xsl:text>
			</xsl:element>
		</xsl:if>
<!--dc:type - nsdl_dc: Reference Material:Proposal-->
		<xsl:if test="contains($allresourceTypes, 'Proposal')">
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Reference Material</xsl:text>
			</xsl:element>
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Proposal</xsl:text>
			</xsl:element>
		</xsl:if>
<!--dc:type - nsdl_dc: Reference Material:Report-->
		<xsl:if test="contains($allresourceTypes, 'Report')">
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Reference Material</xsl:text>
			</xsl:element>
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Report</xsl:text>
			</xsl:element>
		</xsl:if>
<!--dc:type - nsdl_dc: Reference Material:Thesis/Dissertation-->
		<xsl:if test="contains($allresourceTypes, 'Thesis')">
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Reference Material</xsl:text>
			</xsl:element>
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Thesis/Dissertation</xsl:text>
			</xsl:element>
		</xsl:if>
<!--dc:type - nsdl_dc: Audio/Visual:Illustration-->
		<xsl:if test="contains($allresourceTypes, 'illustration')">
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Audio/Visual</xsl:text>
			</xsl:element>
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Illustration</xsl:text>
			</xsl:element>
		</xsl:if>
<!--dc:type - nsdl_dc: Audio/Visual:Map-->
		<xsl:if test="contains($allresourceTypes, 'Map')">
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Audio/Visual</xsl:text>
			</xsl:element>
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Map</xsl:text>
			</xsl:element>
		</xsl:if>
<!--dc:type - nsdl_dc: Audio/Visual:Photograph-->
		<xsl:if test="contains($allresourceTypes, 'Photograph')">
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Audio/Visual</xsl:text>
			</xsl:element>
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Photograph</xsl:text>
			</xsl:element>
		</xsl:if>
<!--dc:type - nsdl_dc: Dataset:Remotely Sensed Data-->
		<xsl:if test="contains($allresourceTypes, 'Remotely sensed')">
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Dataset</xsl:text>
			</xsl:element>
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Remotely Sensed Data</xsl:text>
			</xsl:element>
		</xsl:if>
<!--dc:type - nsdl_dc: Dataset-->
		<xsl:if test="contains($allresourceTypes, 'Modeled dataset')">
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Dataset</xsl:text>
			</xsl:element>
		</xsl:if>
<!--dc:type - nsdl_dc: Dataset:Observed Data-->
		<xsl:if test="contains($allresourceTypes, 'In situ dataset')">
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Dataset</xsl:text>
			</xsl:element>
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Observed Data</xsl:text>
			</xsl:element>
		</xsl:if>
<!--dc:type - nsdl_dc: Audio/Visual:Movie/Animation-->
		<xsl:if test="contains($allresourceTypes, 'Scientific visualization')">
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Audio/Visual</xsl:text>
			</xsl:element>
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Movie/Animation</xsl:text>
			</xsl:element>
		</xsl:if>
<!--dc:type - nsdl_dc: Tool:Code-->
		<xsl:if test="contains($allresourceTypes, 'Code')">
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Tool</xsl:text>
			</xsl:element>
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Code</xsl:text>
			</xsl:element>
		</xsl:if>


<!--dc:identifier - using ADN id numbers-->
<!--all ADN records (online and offline) have a number that can be used as an indentifier for that collection-->
<!--		<xsl:element name="dc:identifier">
			<xsl:value-of select="concat(d:metaMetadata/d:catalogEntries/d:catalog, ': ', d:metaMetadata/d:catalogEntries/d:catalog/@entry)"/>
		</xsl:element> -->

<!--dc:identifier - using ADN technical.online.primaryURL field-->
<!--outputs only dc:identifier not dc:identifier - dct:URI because not doing any checks to ensure meeting dct:URI requirements-->
<!--that is, no checking for spaces, double protocols (e.g. http:http://), lowercase etc.-->
<!--NSDL has a fairly robust process using stylesheets and java code to convert to dct:URI; therefore let NSDL do it-->
<!--only online ADN resources have dc:identifier-->
<!--do an if test to exclude offline resources-->
		<xsl:if test="string-length(d:technical/d:online/d:primaryURL) > 0">
			<xsl:element name="dc:identifier">
				<xsl:value-of select="d:technical/d:online/d:primaryURL"/>
			</xsl:element>
		</xsl:if>
		
<!--dc:identifier - dct:ISBN-->
<!--ADN does not collect-->

<!--dc:identifier - dct:ISSN-->
<!--ADN does not collect-->

<!--dc: source using ADN relations.relation.idEntry and relation.relations.urlEntry-->		
<!--dc:source definition is a reference to a resource form which the present resource is derived. The present resource may be derived from the source resource in whole or part. Include info about a resource that is related intellectually to the described resource but does not fit easily into a relation element-->
<!--dc:source can be text or a URL; the intent here is to have it be a URL-->
<!--ADN collects dc:source because it has the concept of 'Is based on' which is not part of the typical dc:relation concepts-->

<!--use ADN relations.relation.idEntry and urlEntry when the kind attribute has a value of 'Is based on' to output dc:source as a URL-->
<!--outputs only dc:source not dc:source - dct:URI because not doing any checks to ensure meeting dct:URI requirements-->
<!--use DLESE webservice to resolve id numbers to URLs-->

<!--ADN relations.relation.urlEntry can repeat, so use a template-->
		<xsl:apply-templates select="d:relations/d:relation/d:urlEntry" mode="source"/>
		
<!--ADN relations.relation.idEntry can repeat, so use a template-->
		<xsl:apply-templates select="d:relations/d:relation/d:idEntry" mode="source"/>


<!--dc:language plain-->
		<xsl:if test="string-length(d:general/d:language) > 0">
			<xsl:element name="dc:language">
				<xsl:value-of select="d:general/d:language"/>
			</xsl:element>
		</xsl:if>

<!--dc:language - dct:ISO639-2-->
<!--ADN does not collect-->

<!--dc:language - dct:RFC3066-->
<!--ADN does not collect-->

	
<!--dc:rights-->
		<xsl:if test="string-length(d:rights/d:description) > 0">
			<xsl:element name="dc:rights">
				<xsl:value-of select="d:rights/d:description"/>
			</xsl:element>
		</xsl:if>
	
<!--dc:coverage and dc:spatial general information-->
<!--only ADN large bounding box and associated placenames, not detailed geometries, are transformed-->
<!--put ADN large bound box placenames in dc:coverage-->
<!--put ADN large bound box coordinates in dc:spatial - xsi:type=dct:Box-->

<!--dc:coverage using places and events-->
		<xsl:apply-templates select="d:geospatialCoverages/d:geospatialCoverage/d:boundBox/d:bbPlaces/d:place"/>
		<xsl:apply-templates select="d:geospatialCoverages/d:geospatialCoverage/d:boundBox/d:bbEvents/d:event"/>
		<xsl:apply-templates select="d:geospatialCoverages/d:geospatialCoverage/d:detGeos/d:detGeo/d:detPlaces/d:place"/>
		<xsl:apply-templates select="d:geospatialCoverages/d:geospatialCoverage/d:detGeos/d:detGeo/d:detEvents/d:event"/>
		<!--see template PLACE and EVENT-->

		<xsl:apply-templates select="d:general/d:simplePlacesAndEvents/d:placeAndEvent"/>
		<!--see template PLACE and EVENT - SIMPLE-->

<!--dct:spatial - dct:Box-->
<!--no template used since only occurs once in ADN-->
<!--only a test to see if data is present not to see if data is correct-->
		<xsl:if test="string-length(d:geospatialCoverages/d:geospatialCoverage/d:boundBox)>0">
			<xsl:element name="dct:spatial">
				<xsl:attribute name="xsi:type">dct:Box</xsl:attribute>
				<xsl:value-of select="concat('northlimit=', d:geospatialCoverages/d:geospatialCoverage/d:boundBox/d:northCoord, '; southlimit=', d:geospatialCoverages/d:geospatialCoverage/d:boundBox/d:southCoord, '; westlimit=', d:geospatialCoverages/d:geospatialCoverage/d:boundBox/d:westCoord, '; eastlimit=', d:geospatialCoverages/d:geospatialCoverage/d:boundBox/d:eastCoord, '; units=signed decimal degrees')"/>
			</xsl:element>
		</xsl:if>


<!--dct:temporal general information-->
<!--use dct:temporal xsi:type=dct:Period for ADN timeAD and named time period; template TIMEAD mode DCT:PERIOD-->
<!--use dct:temporal xsi:type=dct:W3CDTF for ADN timeAD; template TIMEAD mode DCT:W3CDTF-->
<!--use dct:temporal xsi:type=dct:Period for ADN timeBC and named time period in timeBC; template TIMEBC-->
<!--use dct:temporal xsi:type=dct:Period for ADN timeRelative; see template TIMERELATIVE-->

<!--dct:temporal xsi:type=dct:Period for timeAD-->
		<xsl:apply-templates select="d:temporalCoverages/d:timeAndPeriod/d:timeInfo/d:timeAD" mode="dct:Period"/>
		<!--see template TIMEAD mode DCT:PERIOD-->

<!--dct:temporal xsi:type=dct:W3CDTF for timeAD-->
		<xsl:apply-templates select="d:temporalCoverages/d:timeAndPeriod/d:timeInfo/d:timeAD" mode="dct:W3CDTF"/>
		<!--see template TIMEAD mode DCT:W3CDTF-->
		
<!--dct:temporal - xsi:type=dct:Period for timeBC-->
		<xsl:apply-templates select="d:temporalCoverages/d:timeAndPeriod/d:timeInfo/d:timeBC"/>
		<!--see template TIMEBC-->

<!--dct:temporal - xsi:type=dct:Period for timeRelative-->
		<xsl:apply-templates select="d:temporalCoverages/d:timeAndPeriod/d:timeInfo/d:timeRelative"/>
		<!--see template TIMERELATIVE-->

<!--dct:temporal for simpleTemporalCoverages-->
		<xsl:apply-templates select="d:general/d:simpleTemporalCoverages/d:description"/>
		<!--see template TEMPORAL DESCRIPTION-->


<!--dct relation information-->
<!--ADN DLESE:Has thumbnail is not transformed-->

<!--ADN relations.relation.urlEntry can repeat, so use a template-->
		<xsl:apply-templates select="d:relations/d:relation/d:urlEntry" mode="relations"/>
<!--see template URL ENTRY mode=RELATIONS-->
		
<!--ADN relations.relation.idEntry can repeat, so use a template-->
		<xsl:apply-templates select="d:relations/d:relation/d:idEntry" mode="relations"/>
<!--see template ID ENTRY mode=RELATIONS-->


<!--dct:conformsTo - using ADN standards information-->
		<xsl:apply-templates select="d:educational/d:contentStandards/d:contentStandard"/>
		<!--see template CONTENTSTANDARD-->

<!--dct:audience-->
		<xsl:apply-templates select="d:educational/d:audiences/d:audience/d:beneficiary"/>
		<!--see template BENEFICIARY-->

<!--dct:mediator-->
		<xsl:apply-templates select="d:educational/d:audiences/d:audience/d:toolFor"/>
		<!--see template TOOLFOR-->

<!--dct:educationLevel-->
<!--maps ADN gradeRange terms to the NSDL list of terms-->

<!--	variable for ADN educational.audiences.audience.gradeRange-->
		<xsl:variable name="allgrades">
			<xsl:for-each select="d:educational/d:audiences/d:audience/d:gradeRange">
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
		
<!--dct:instructionalMethod-->
		<xsl:apply-templates select="d:educational/d:audiences/d:audience/d:teachingMethods/d:teachingMethod"/>
		<!--see template TEACHINGMETHOD-->
		

<!--ieee:interactivityType-->
		<xsl:if test="string-length(d:educational/d:interactivityType) >0 and starts-with(d:educational/d:interactivityType, 'LOM:')">
			<xsl:element name="ieee:interactivityType">
				<xsl:value-of select="substring-after(d:educational/d:interactivityType, 'LOM:')"/>
			</xsl:element>
		</xsl:if>
<!--ieee:interactivityLevel-->
		<xsl:if test="string-length(d:educational/d:interactivityLevel) >0 and starts-with(d:educational/d:interactivityLevel, 'LOM:')">
			<xsl:element name="ieee:interactivityLevel">
				<xsl:value-of select="substring-after(d:educational/d:interactivityLevel, 'LOM:')"/>
			</xsl:element>
		</xsl:if>

<!--ieee:typicalLearningTime-->
		<xsl:if test="string-length(d:educational/d:audiences/d:audience/d:typicalUseTime) >0">
			<xsl:element name="ieee:typicalLearningTime">
				<xsl:value-of select="d:educational/d:audiences/d:audience/d:typicalUseTime"/>
			</xsl:element>
		</xsl:if>
		
<!--end nsdl:dc-->
		</nsdl_dc:nsdl_dc>
	</xsl:template>


<!--TEMPLATES. In alphabetical order generally by ADN element name-->
<!--1.   BENEFICARY writes dct:audience-->
<!--2.   CONTENTSTANDARD writes dct:conformsTo-->
<!--3.   ID-ENTRY template; mode=RELATIONS determines if ADN relation.relation.idEntry.entry exists in the library using webservices-->
<!--4.   ID-ENTRY template; mode=SOURCE writes dc:source when ADN relations.relation.idEntry.kind='Is based on'-->
<!--5.   KIND writes dct:isVersionOf, dct:hasVersion, etc.-->
<!--6.	ORGANIZATION mode=contributor writes dc:contributor-->
<!--7. 	ORGANIZATION mode=creator writes dc:creator-->
<!--8. 	ORGANIZATION mode=publisher writes dc:publisher-->
<!--9. 	OTHER REQUIREMENT writes dc:format-->
<!--10. PERSON mode=contributor writes dc:contributor-->
<!--11. PERSON mode=creator writes dc:creator-->
<!--12. PERSON mode=pubisher writes dc:publisher-->
<!--13.	PLACE  and EVENT writes dc:coverage from ADN bounding box place and event info-->
<!--14.	PLACE  and EVENT - SIMPLE writes dc:coverage and dct:temporal from ADN simple (general) place and event info-->
<!--15. RELATIONS writes URL content (using existing or webservices) of dct:IsVersionOf, dct:hasVersion etc. from ID template.-->
<!--16. REQUIREMENT writes dc:format from ADN technical requirements-->
<!--17. SUBJECT mode=subjects writes dc:subject from ADN subjects--> 
<!--18. SUBJECT mode=keywords writes dc:subject from ADN keywords-->
<!--19.	TEACHINGMETHOD writes dct:intructionalMethod-->
<!--20.	TEMPORAL DESCRIPTION writes dct:temporal from ADN simple temporal coverages-->
<!--21. TIMEAD mode=dct:period writes dct:temporal xsi:type="dct:Period"-->
<!--22. TIMEAD mode=dct:W3CDTF writes dct:temporal xsi:type="dct:W3CDTF"-->
<!--23. TIMEBC writes dct:temporal xsi:type="dct:Period"-->
<!--24. TIMERELATIVE writes dct:temporal xsi:type="dct:Period"-->
<!--25. TOOLFOR writes dct:mediator-->
<!--26.	URL-ENTRY template; mode=RELATIONS determines whether ADN relation.relation.iurlEntry.url has content-->
<!--27. URL-ENTRY template; mode=SOURCE writes dc:source when ADN relations.relation.urlEntry.kind='Is based on'-->


<!--1. BENEFICIARY template-->
	<xsl:template match="d:beneficiary">
<!--compares the ADN value against the controlled vocabulary and only transforms data if the content is in the vocabulary-->
		<xsl:variable name="current">
			<xsl:value-of select="."/>
		</xsl:variable>
		<!--get DLESE ADN terms-->
		<xsl:for-each select="document($benDVocabURL)//xsd:restriction/xsd:enumeration">
			<xsl:variable name="vocab">
				<xsl:value-of select="@value"/>
			</xsl:variable>
			<xsl:if test="$current = $vocab">
				<xsl:element name="dct:audience">
					<xsl:value-of select="substring-after($current, ':')"/>
				</xsl:element>
			</xsl:if>
		</xsl:for-each>
		<!--get GEM ADN terms-->
		<xsl:for-each select="document($benGVocabURL)//xsd:restriction/xsd:enumeration">
			<xsl:variable name="vocab">
				<xsl:value-of select="@value"/>
			</xsl:variable>
			<xsl:if test="$current = $vocab">
				<xsl:element name="dct:audience">
					<xsl:value-of select="substring-after($current, ':')"/>
				</xsl:element>
			</xsl:if>
		</xsl:for-each>
	</xsl:template>


<!--2. CONTENTSTANDARD template-->
	<xsl:template match="d:contentStandard">
<!--compares the ADN value against the controlled vocabulary and only transforms data if the content is in the vocabulary-->
		<xsl:variable name="current">
			<xsl:value-of select="."/>
		</xsl:variable>
		<xsl:for-each select="document($asnIdsURL)//mapping">
			<xsl:variable name="vocab">
				<xsl:value-of select="."/>
			</xsl:variable>
			<xsl:variable name="asnID">
				<xsl:value-of select="@asnIdentifier"/>
			</xsl:variable>
			<xsl:if test="$current = $vocab">
				<xsl:element name="dct:conformsTo">
					<xsl:attribute name="xsi:type">dct:URI</xsl:attribute>
					<xsl:value-of select="$asnID"/>
				</xsl:element>
			</xsl:if>
		</xsl:for-each>

		<xsl:for-each select="document($asnNCGEids)//mapping">
			<xsl:variable name="vocab">
				<xsl:value-of select="."/>
			</xsl:variable>
			<xsl:variable name="asnID">
				<xsl:value-of select="@asnIdentifier"/>
			</xsl:variable>
			<xsl:if test="$current = $vocab">
				<xsl:element name="dct:conformsTo">
					<xsl:attribute name="xsi:type">dct:URI</xsl:attribute>
					<xsl:value-of select="$asnID"/>
				</xsl:element>
			</xsl:if>
		</xsl:for-each>

<!--archived 2007-08-18 because ASN identifiers now exist for the National Geography Standards; so use code above-->
<!--		<xsl:for-each select="document($NCGEVocabURL)//xsd:restriction/xsd:enumeration">
			<xsl:variable name="vocab">
				<xsl:value-of select="@value"/>
			</xsl:variable>
			<xsl:if test="$current = $vocab">
				<xsl:element name="dct:conformsTo">
						<xsl:value-of select="concat('Supports National Council for Geographic Education (NCGE) standard: ', substring-after($current, ':' ))"/>
				</xsl:element>
			</xsl:if>
		</xsl:for-each>-->
<!--end archive-->

		<xsl:for-each select="document($NETSVocabURL)//xsd:restriction/xsd:enumeration">
			<xsl:variable name="vocab">
				<xsl:value-of select="@value"/>
			</xsl:variable>
			<xsl:if test="$current = $vocab">
				<xsl:element name="dct:conformsTo">
						<xsl:value-of select="concat('Supports National Educational Technology Standards (NETS): ', substring-after($current, ':' ))"/>
				</xsl:element>
			</xsl:if>
		</xsl:for-each>
		<xsl:for-each select="document($NCTMVocabURL)//xsd:restriction/xsd:enumeration">
			<xsl:variable name="vocab">
				<xsl:value-of select="@value"/>
			</xsl:variable>
			<xsl:if test="$current = $vocab">
				<xsl:element name="dct:conformsTo">
						<xsl:value-of select="concat('Supports National Council of Teachers of Mathemeatics (NCTM): ', substring-after($current, ':' ))"/>
				</xsl:element>
			</xsl:if>
		</xsl:for-each>
		<xsl:for-each select="document($commonVocabURL)//xsd:restriction/xsd:enumeration">
			<xsl:variable name="vocab">
				<xsl:value-of select="@value"/>
			</xsl:variable>
			<xsl:if test="$current = $vocab">
				<xsl:element name="dct:conformsTo">
						<xsl:value-of select="concat('Supports American Association for the Advancement of Science (AAAS) Benchmarks: ', substring-after($current, ':' ))"/>
				</xsl:element>
			</xsl:if>
		</xsl:for-each>
		<xsl:for-each select="document($designVocabURL)//xsd:restriction/xsd:enumeration">
			<xsl:variable name="vocab">
				<xsl:value-of select="@value"/>
			</xsl:variable>
			<xsl:if test="$current = $vocab">
				<xsl:element name="dct:conformsTo">
						<xsl:value-of select="concat('Supports American Association for the Advancement of Science (AAAS) Benchmarks: ', substring-after($current, ':' ))"/>
				</xsl:element>
			</xsl:if>
		</xsl:for-each>
		<xsl:for-each select="document($habitsVocabURL)//xsd:restriction/xsd:enumeration">
			<xsl:variable name="vocab">
				<xsl:value-of select="@value"/>
			</xsl:variable>
			<xsl:if test="$current = $vocab">
				<xsl:element name="dct:conformsTo">
						<xsl:value-of select="concat('Supports American Association for the Advancement of Science (AAAS) Benchmarks: ', substring-after($current, ':' ))"/>
				</xsl:element>
			</xsl:if>
		</xsl:for-each>
		<xsl:for-each select="document($historyocabURL)//xsd:restriction/xsd:enumeration">
			<xsl:variable name="vocab">
				<xsl:value-of select="@value"/>
			</xsl:variable>
			<xsl:if test="$current = $vocab">
				<xsl:element name="dct:conformsTo">
						<xsl:value-of select="concat('Supports American Association for the Advancement of Science (AAAS) Benchmarks: ', substring-after($current, ':' ))"/>
				</xsl:element>
			</xsl:if>
		</xsl:for-each>
		<xsl:for-each select="document($humanVocabURL)//xsd:restriction/xsd:enumeration">
			<xsl:variable name="vocab">
				<xsl:value-of select="@value"/>
			</xsl:variable>
			<xsl:if test="$current = $vocab">
				<xsl:element name="dct:conformsTo">
						<xsl:value-of select="concat('Supports American Association for the Advancement of Science (AAAS) Benchmarks: ', substring-after($current, ':' ))"/>
				</xsl:element>
			</xsl:if>
		</xsl:for-each>
		<xsl:for-each select="document($livingVocabURL)//xsd:restriction/xsd:enumeration">
			<xsl:variable name="vocab">
				<xsl:value-of select="@value"/>
			</xsl:variable>
			<xsl:if test="$current = $vocab">
				<xsl:element name="dct:conformsTo">
						<xsl:value-of select="concat('Supports American Association for the Advancement of Science (AAAS) Benchmarks: ', substring-after($current, ':' ))"/>
				</xsl:element>
			</xsl:if>
		</xsl:for-each>
		<xsl:for-each select="document($mathVocabURL)//xsd:restriction/xsd:enumeration">
			<xsl:variable name="vocab">
				<xsl:value-of select="@value"/>
			</xsl:variable>
			<xsl:if test="$current = $vocab">
				<xsl:element name="dct:conformsTo">
						<xsl:value-of select="concat('Supports American Association for the Advancement of Science (AAAS) Benchmarks: ', substring-after($current, ':' ))"/>
				</xsl:element>
			</xsl:if>
		</xsl:for-each>
		<xsl:for-each select="document($physicalVocabURL)//xsd:restriction/xsd:enumeration">
			<xsl:variable name="vocab">
				<xsl:value-of select="@value"/>
			</xsl:variable>
			<xsl:if test="$current = $vocab">
				<xsl:element name="dct:conformsTo">
						<xsl:value-of select="concat('Supports American Association for the Advancement of Science (AAAS) Benchmarks: ', substring-after($current, ':' ))"/>
				</xsl:element>
			</xsl:if>
		</xsl:for-each>
		<xsl:for-each select="document($scienceVocabURL)//xsd:restriction/xsd:enumeration">
			<xsl:variable name="vocab">
				<xsl:value-of select="@value"/>
			</xsl:variable>
			<xsl:if test="$current = $vocab">
				<xsl:element name="dct:conformsTo">
						<xsl:value-of select="concat('Supports American Association for the Advancement of Science (AAAS) Benchmarks: ', substring-after($current, ':' ))"/>
				</xsl:element>
			</xsl:if>
		</xsl:for-each>
		<xsl:for-each select="document($techVocabURL)//xsd:restriction/xsd:enumeration">
			<xsl:variable name="vocab">
				<xsl:value-of select="@value"/>
			</xsl:variable>
			<xsl:if test="$current = $vocab">
				<xsl:element name="dct:conformsTo">
						<xsl:value-of select="concat('Supports American Association for the Advancement of Science (AAAS) Benchmarks: ', substring-after($current, ':' ))"/>
				</xsl:element>
			</xsl:if>
		</xsl:for-each>
	</xsl:template>
	
<!--3. ID-ENTRY template; mode=RELATIONS-->
<!--assumes the id numbers the resource is based on are also using ADN metadata records that are online resources that have URLs-->
<!--need to verify that the id number is actually in the library and returns content; use the webservice-->
<!--webservice 1-1 has namespaces unlike 1-1 so account for them-->
	<xsl:template match="d:idEntry" mode="relations">
		<xsl:if test="(starts-with(./@kind, 'DLESE:') or starts-with(./@kind, 'DC:')) and string-length(document(concat($DDSWSID, ./@entry))//d:technical/d:online/d:primaryURL)>0">
			<xsl:apply-templates select="./@kind"/>
		</xsl:if>		
	</xsl:template>
	
<!--4. ID-ENTRY template; mode=SOURCE-->
<!--determine if the kind attriubte has a value of 'Is based on'-->
<!--assumes the id numbers the resource is based on are also using ADN metadata records that are online resources that have URLs-->
<!--webservice 1-1 has namespaces unlike 1-1 so account for them-->
	<xsl:template match="d:idEntry" mode="source">
		<xsl:if test="contains(./@kind, 'Is based on') and string-length(./@entry) > 0">
			<xsl:element name="dc:source">
				<xsl:value-of select="document(concat($DDSWSID, ./@entry))//d:technical/d:online/d:primaryURL"/>
			</xsl:element>
		</xsl:if>		
	</xsl:template>

<!--5. KIND template-->
<!--does not map ADN relations.relation//@kind when kind = 'Is based on' because Is based on is mapped into the dc:source element-->
<!--does not map ADN relations.relation//@kind when kind = 'Has thumbnail'-->
<!--call-template halts processing of current template in order to call and complete anther template; then processing of the current template is resumed; if did not do this would get the unwnted result of all the mediums (mime types) being in a single dc:format tag-->
	<xsl:template match="d:relations/d:relation//@kind">
		<xsl:choose>
<!--dct:isVersionOf-->
			<xsl:when test="contains(., 'Is version of')">
				<xsl:element name="dct:isVersionOf">
					<xsl:call-template name="relations"/>
				</xsl:element>
			</xsl:when>
<!--dct:hasVersion-->
			<xsl:when test="contains(., 'Has version')">
				<xsl:element name="dct:hasVersion">
					<xsl:call-template name="relations"/>
				</xsl:element>
			</xsl:when>
<!--dct:isReplacedBy-->
			<xsl:when test="contains(., 'Is replaced by')">
				<xsl:element name="dct:isReplacedBy">
					<xsl:call-template name="relations"/>
				</xsl:element>
			</xsl:when>
<!--dct:replaces-->
			<xsl:when test="contains(., 'Replaces')">
				<xsl:element name="dct:replaces">
					<xsl:call-template name="relations"/>
				</xsl:element>
			</xsl:when>
<!--dct:isRequiredBy-->
			<xsl:when test="contains(., 'Is required by')">
				<xsl:element name="dct:isRequiredBy">
					<xsl:call-template name="relations"/>
				</xsl:element>
			</xsl:when>
<!--dct:requires-->
			<xsl:when test="contains(., 'Requires')">
				<xsl:element name="dct:requires">
					<xsl:call-template name="relations"/>
				</xsl:element>
			</xsl:when>
<!--dct:isPartOf-->
			<xsl:when test="contains(., 'Is part of')">
				<xsl:element name="dct:isPartOf">
					<xsl:call-template name="relations"/>
				</xsl:element>
			</xsl:when>
<!--dct:hasPart-->
			<xsl:when test="contains(., 'Has part')">
				<xsl:element name="dct:hasPart">
					<xsl:call-template name="relations"/>
				</xsl:element>
			</xsl:when>
<!--dct:isReferencedby-->
			<xsl:when test="contains(., 'Is referenced by') or contains(., 'Is basis for')">
				<xsl:element name="dct:isReferencedBy">
					<xsl:call-template name="relations"/>
				</xsl:element>
			</xsl:when>
<!--dct:references-->
			<xsl:when test="contains(., 'References') or contains(., 'Is associated with')">
				<xsl:element name="dct:references">
					<xsl:call-template name="relations"/>
				</xsl:element>
			</xsl:when>
<!--dct:isFormatOf-->
			<xsl:when test="contains(., 'Is format of')">
				<xsl:element name="dct:isFormatOf">
					<xsl:call-template name="relations"/>
				</xsl:element>
			</xsl:when>
<!--dct:hasFormat-->
			<xsl:when test="contains(., 'Has format')">
				<xsl:element name="dct:hasFormat">
					<xsl:call-template name="relations"/>
				</xsl:element>
			</xsl:when>
<!--dct:conformsTo-->
			<xsl:when test="contains(., 'Conforms to')">
				<xsl:element name="dct:conformsTo">
					<xsl:call-template name="relations"/>
				</xsl:element>
			</xsl:when>
		</xsl:choose>
	</xsl:template>

<!--6. ORGANIZATION template mode=CONTRIBUTOR-->
	<xsl:template match="d:organization" mode="contributor">
		<xsl:if test="(../@role='Contributor' or ../@role='Editor') and string-length(d:instName) > 0">
			<xsl:element name="dc:contributor">
				<xsl:choose>
					<xsl:when test="string-length(d:instDept)>0">
						<xsl:value-of select="concat(d:instDept,', ',d:instName)"/>
					</xsl:when>
					<xsl:otherwise>
						<xsl:value-of select="d:instName"/>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:element>
		</xsl:if>
	</xsl:template>			

<!--7. ORGANIZATION template mode=CREATOR-->
	<xsl:template match="d:organization" mode="creator">
		<xsl:if test="(../@role='Author' or ../@role='Principal Investigator') and string-length(d:instName) > 0">
			<xsl:element name="dc:creator">
				<xsl:choose>
					<xsl:when test="string-length(d:instDept)>0">
						<xsl:value-of select="concat(d:instDept,', ',d:instName)"/>
					</xsl:when>
					<xsl:otherwise>
						<xsl:value-of select="d:instName"/>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:element>
		</xsl:if>
	</xsl:template>			

<!--8. ORGANIZATION template mode=PUBLISHER-->
	<xsl:template match="d:organization" mode="publisher">
		<xsl:if test="(../@role='Publisher') and string-length(d:instName) > 0">
			<xsl:element name="dc:publisher">
				<xsl:choose>
					<xsl:when test="string-length(d:instDept)>0">
						<xsl:value-of select="concat(d:instDept,', ',d:instName)"/>
					</xsl:when>
					<xsl:otherwise>
						<xsl:value-of select="d:instName"/>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:element>
		</xsl:if>
	</xsl:template>			

<!--9. OTHER REQUIREMENT template-->
<!--putting the dc:format tag inside the when tag eliminates the possibility of writing a blank tag, but it does not eliminate writing the same tag multiple times if the same ADN data occurs multiple times-->
	<xsl:template match="d:otherRequirement">
		<xsl:choose>
			<xsl:when test="string-length(./d:otherType)>0 and string-length(./d:minimumVersion)>0 and string-length(./d:maximumVersion)>0">
				<xsl:element name="dc:format">
					<xsl:value-of select="concat(./d:otherType, ' with the following min/max version information: ', ./d:minimumVersion, ', ', ./d:maximumVersion)"/>	
				</xsl:element>
			</xsl:when>
			<xsl:when test="string-length(./d:otherType) > 0 and string-length(./d:minimumVersion) > 0">
				<xsl:element name="dc:format">
					<xsl:value-of select="concat(./d:otherType, ' with the following minimum version information: ', ./d:minimumVersion)"/>	
				</xsl:element>
			</xsl:when>
			<xsl:when test="string-length(./d:otherType) > 0 and string-length(./d:maximumVersion) > 0">
				<xsl:element name="dc:format">
					<xsl:value-of select="concat(./d:otherType, ' with the following maximum version information: ', ./d:maximumVersion)"/>	
				</xsl:element>
			</xsl:when>
			<xsl:when test="string-length(./d:otherType) > 0">
				<xsl:element name="dc:format">
					<xsl:value-of select="./d:otherType"/>	
				</xsl:element>
			</xsl:when>
		</xsl:choose>
	</xsl:template>

<!--10. PERSON template mode=CONTRIBUTOR-->
	<xsl:template match="d:person" mode="contributor">
		<xsl:if test="(../@role='Contributor' or ../@role='Editor') and string-length(d:nameFirst)>0 and string-length(d:nameLast)>0">
			<xsl:element name="dc:contributor">
				<xsl:value-of select="concat(d:nameFirst,' ',d:nameLast)"/>
			</xsl:element>
		</xsl:if>
	</xsl:template>			

<!--11. PERSON template mode=CREATOR-->
	<xsl:template match="d:person" mode="creator">
		<xsl:if test="(../@role='Author' or ../@role='Principal Investigator') and string-length(d:nameFirst)>0 and string-length(d:nameLast)>0">
			<xsl:element name="dc:creator">
				<xsl:value-of select="concat(d:nameFirst,' ',d:nameLast)"/>
			</xsl:element>
		</xsl:if>
	</xsl:template>			

<!--12. PERSON template mode=PUBLISHER-->
	<xsl:template match="d:person" mode="publisher">
		<xsl:if test="(../@role='Publisher') and string-length(d:nameFirst)>0 and string-length(d:nameLast)>0">
			<xsl:element name="dc:publisher">
				<xsl:value-of select="concat(d:nameFirst,' ',d:nameLast)"/>
			</xsl:element>
		</xsl:if>
	</xsl:template>	

<!--13. PLACE  and EVENT template-->
	<xsl:template match="d:place | d:event">
		<xsl:if test="string-length(d:name)>0">
			<xsl:element name="dc:coverage">
				<xsl:value-of select="d:name"/>
			</xsl:element>
		</xsl:if>
	</xsl:template>

<!--14. PLACE  and EVENT - SIMPLE template-->
	<xsl:template match="d:placeAndEvent">
		<xsl:if test="string-length(d:place)>0">
			<xsl:element name="dc:coverage">
				<xsl:value-of select="d:place"/>
			</xsl:element>
		</xsl:if>
		<xsl:if test="string-length(d:event)>0">
			<xsl:element name="dct:temporal">
				<xsl:value-of select="d:event"/>
			</xsl:element>
		</xsl:if>
	</xsl:template>
	
<!--15. RELATIONS template-->
<!--webservice 1-1 has namespaces unlike 1-0 so account for them-->
	<xsl:template name="relations">
		<xsl:value-of select="../@url"/>
		<xsl:value-of select="document(concat($DDSWSID, ../@entry))//d:technical/d:online/d:primaryURL"/>
	</xsl:template>

<!--16. REQUIREMENT template-->
<!--putting the dc:format tag inside the when tag eliminates the possibility of writing a blank tag, but it does not eliminate writing the same tag multiple times if the same ADN data occurs multiple times-->
<!--from the ADN terms only write the leaf part - the text after the 2nd colon; so use substrings-->
<!--do not include the ADN generic leaf terms like the one listed in the when statement below-->
	<xsl:template match="d:requirement">
<!--have to use variables or content and comparison tests won't work-->
		<xsl:variable name="current">
			<xsl:value-of select="d:reqType"/>
		</xsl:variable>
		<xsl:variable name="min">
			<xsl:value-of select="d:minimumVersion"/>
		</xsl:variable>
		<xsl:variable name="max">
			<xsl:value-of select="d:maximumVersion"/>
		</xsl:variable>

<!--compare current requirement (i.e. the one being processed against each term in the ADN vocabulary list by using a for-each loop-->
		<xsl:for-each select="document($reqVocabURL)//xsd:restriction/xsd:enumeration">
			<xsl:variable name="vocab">
				<xsl:value-of select="@value"/>
			</xsl:variable>

			<xsl:choose>
				<xsl:when test="contains($current, 'No specific technical requirements') or contains($current, 'More specific technical requirements') or contains($current, 'Technical information not easily determined')"/>
				<xsl:otherwise>
					<xsl:choose>
						<xsl:when test="$current=$vocab and string-length($min)>0 and string-length($max)>0">
							<xsl:element name="dc:format">
								<xsl:value-of select="concat(substring-after(substring-after($current, 'DLESE:'), ':'), ' with the following min/max version information: ', $min, ', ', $max)"/>	
							</xsl:element>
						</xsl:when>
						<xsl:when test="$current = $vocab and string-length($min) > 0">
							<xsl:element name="dc:format">
								<xsl:value-of select="concat(substring-after(substring-after($current, 'DLESE:'), ':'), ' with the following minimum version information: ', $min)"/>	
							</xsl:element>
						</xsl:when>
						<xsl:when test="$current = $vocab and string-length($max) > 0">
							<xsl:element name="dc:format">
								<xsl:value-of select="concat(substring-after(substring-after($current, 'DLESE:'), ':'), ' with the following maximum version information: ', $max)"/>	
							</xsl:element>
						</xsl:when>
						<xsl:when test="$current = $vocab and string-length($min)=0 and string-length($max)=0">
							<xsl:element name="dc:format">
								<xsl:value-of select="substring-after(substring-after($current, 'DLESE:'), ':')"/>	
							</xsl:element>
						</xsl:when>
					</xsl:choose>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:for-each>
	</xsl:template>


<!--17. SUBJECT template mode=SUBJECTS-->
	<xsl:template match="d:subject" mode="subjects">
		<xsl:variable name="current">
			<xsl:value-of select="."/>
		</xsl:variable>
		<xsl:for-each select="document($subjVocabURL)//xsd:restriction/xsd:enumeration">
			<xsl:variable name="vocab">
				<xsl:value-of select="@value"/>
			</xsl:variable>
			<xsl:if test="$current = $vocab and not(contains($current, 'Other'))">
				<xsl:element name="dc:subject">
					<xsl:value-of select="substring-after($current, ':')"/>
				</xsl:element>
			</xsl:if>
		</xsl:for-each>
	</xsl:template>			

<!--18. SUBJECT template mode=KEYWORDS-->
	<xsl:template match="d:keyword" mode="keywords">
		<xsl:if test="string-length(.) > 0">
			<xsl:element name="dc:subject">
				<xsl:value-of select="."/>
			</xsl:element>
		</xsl:if>

	</xsl:template>			

<!--19. TEACHINGMETHOD template-->
<!--compares the ADN value against the controlled vocabulary and only transforms data if the content is in the vocabulary-->
	<xsl:template match="d:teachingMethod">
		<xsl:variable name="current">
			<xsl:value-of select="."/>
		</xsl:variable>
		<!--get DLESE ADN terms-->
		<xsl:for-each select="document($teachDVocabURL)//xsd:restriction/xsd:enumeration">
			<xsl:variable name="vocab">
				<xsl:value-of select="@value"/>
			</xsl:variable>
			<xsl:if test="$current = $vocab">
				<xsl:element name="dct:instructionalMethod">
					<xsl:value-of select="substring-after($current, ':')"/>
				</xsl:element>
			</xsl:if>
		</xsl:for-each>
		<!--get GEM ADN terms-->
		<xsl:for-each select="document($teachGVocabURL)//xsd:restriction/xsd:enumeration">
			<xsl:variable name="vocab">
				<xsl:value-of select="@value"/>
			</xsl:variable>
			<xsl:if test="$current = $vocab">
				<xsl:element name="dct:instructionalMethod">
					<xsl:value-of select="substring-after($current, ':')"/>
				</xsl:element>
			</xsl:if>
		</xsl:for-each>
	</xsl:template>

<!--20. TEMPORAL DESCRIPTION template-->
	<xsl:template match="d:description">
		<xsl:if test="string-length(.)>0">
			<xsl:element name="dct:temporal">
				<xsl:value-of select="."/>
			</xsl:element>
		</xsl:if>
	</xsl:template>

<!--21. TIMEAD template mode DCT:PERIOD-->
	<xsl:template match="d:timeAD" mode="dct:Period">
<!--if the ADN vocabulary term 'Present' is used for begin and end date, write the value without a W3CDTF scheme because 'Present' is not W3CDTF compliant-->
<!--only the first occurrence of a name element is transformed because the encoding scheme of DCT:PERIOD does not allow multiple names. So a template is not used to pick up all occurrences of name-->
		<xsl:if test="string-length(d:begin/@date)>0 and string-length(d:end/@date)>0">
			<xsl:element name="dct:temporal">
				<xsl:attribute name="xsi:type">dct:Period</xsl:attribute>
				<xsl:choose>
					<!--testing only the first name for data because only be transforming the first name-->
					<xsl:when test="string-length(../../d:periods/d:period/d:name)>0">
						<xsl:choose>
							<xsl:when test="d:begin/@date='Present' or d:end/@date='Present'">
								<xsl:value-of select="concat('start=', d:begin/@date, ';end=', d:end/@date, ';name=', ../../d:periods/d:period/d:name)"/>
							</xsl:when>
							<xsl:otherwise>
								<xsl:value-of select="concat('start=', d:begin/@date, ';end=', d:end/@date, ';scheme=W3CDTF;name=', ../../d:periods/d:period/d:name)"/>
							</xsl:otherwise>
						</xsl:choose>
					</xsl:when>
					<xsl:otherwise>
						<xsl:choose>
							<xsl:when test="d:begin/@date='Present' or d:end/@date='Present'">
								<xsl:value-of select="concat('start=', d:begin/@date, ';end=', d:end/@date)"/>
							</xsl:when>
							<xsl:otherwise>
								<xsl:value-of select="concat('start=', d:begin/@date, ';end=', d:end/@date, ';scheme=W3CDTF')"/>
							</xsl:otherwise>
						</xsl:choose>
					</xsl:otherwise>
				</xsl:choose>	
			</xsl:element>
		</xsl:if>
	</xsl:template>

<!--22. TIMEAD template mode DCT:W3CDTF-->
	<xsl:template match="d:timeAD" mode="dct:W3CDTF">
<!--if the ADN vocabulary term 'Present' is used for begin and end date, do not transform because 'Present' is not W3CDTF compliant-->
<!--if data is present, this aassumes it is W3CDTF compliant and transforms it-->
		<xsl:choose>
			<xsl:when test="d:begin/@date='Present' or d:end/@date='Present' or string-length(d:begin/@date)=0 or string-length(d:end/@date)=0"/>
			<xsl:otherwise>
				<xsl:element name="dct:temporal">
					<xsl:attribute name="xsi:type">dct:W3CDTF</xsl:attribute>
					<xsl:value-of select="d:begin/@date"/>
				</xsl:element>
				<xsl:element name="dct:temporal">
					<xsl:attribute name="xsi:type">dct:W3CDTF</xsl:attribute>
					<xsl:value-of select="d:end/@date"/>
				</xsl:element>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

<!--23. TIMEBC template-->
	<xsl:template match="d:timeBC">
<!--only the first occurrence of a name element is transformed because the encoding scheme of DCT:PERIOD does not allow multiple names. So a template is not used to pick up all occurrences of name-->
		<xsl:if test="string-length(d:begin)>0 and string-length(d:end)>0">
			<xsl:element name="dct:temporal">
				<xsl:attribute name="xsi:type">dct:Period</xsl:attribute>
				<xsl:choose>
					<!--testing only the first name for data because only be transforming the first name-->
					<xsl:when test="string-length(../../d:periods/d:period/d:name)>0">
						<xsl:value-of select="concat('start=', d:begin, ' BC;end=', d:end, ' BC;name=', ../../d:periods/d:period/d:name)"/>
					</xsl:when>
					<xsl:otherwise>
						<xsl:value-of select="concat('start=', d:begin, ' BC;end=', d:end, ' BC')"/>
					</xsl:otherwise>
				</xsl:choose>	
			</xsl:element>
		</xsl:if>
	</xsl:template>

<!--24. TIMERELATIVE template-->
	<xsl:template match="d:timeRelative">
<!--only the first occurrence of a name element is transformed because the encoding scheme of DCT:PERIOD does not allow multiple names. So a template is not used to pick up all occurrences of name-->
		<xsl:if test="string-length(d:begin)>0 and string-length(d:end)>0">
			<xsl:element name="dct:temporal">
				<xsl:attribute name="xsi:type">dct:Period</xsl:attribute>
				<xsl:choose>
					<xsl:when test="string-length(../../d:periods/d:period/d:name)>0">
						<xsl:value-of select="concat('start=', d:begin, ' ', d:begin/@units, ';end=', d:end, ' ', d:end/@units, ';name=', ../../d:periods/d:period/d:name)"/>
					</xsl:when>
					<xsl:otherwise>
						<xsl:value-of select="concat('start=', d:begin, ' ', d:begin/@units, ';end=', d:end, ' ', d:end/@units)"/>
					</xsl:otherwise>
				</xsl:choose>	
			</xsl:element>
		</xsl:if>
	</xsl:template>

<!--25. TOOLFOR template-->
	<xsl:template match="d:toolFor">
<!--compares the ADN value against the controlled vocabulary and only transforms data if the content is in the vocabulary-->
		<xsl:variable name="current">
			<xsl:value-of select="."/>
		</xsl:variable>
		<!--get DLESE ADN terms-->
		<xsl:for-each select="document($toolDVocabURL)//xsd:restriction/xsd:enumeration">
			<xsl:variable name="vocab">
				<xsl:value-of select="@value"/>
			</xsl:variable>
			<xsl:if test="$current = $vocab">
				<xsl:element name="dct:mediator">
					<xsl:value-of select="substring-after($current, ':')"/>
				</xsl:element>
			</xsl:if>
		</xsl:for-each>
		<!--get GEM ADN terms-->
		<xsl:for-each select="document($toolGVocabURL)//xsd:restriction/xsd:enumeration">
			<xsl:variable name="vocab">
				<xsl:value-of select="@value"/>
			</xsl:variable>
			<xsl:if test="$current = $vocab">
				<xsl:element name="dct:mediator">
					<xsl:value-of select="substring-after($current, ':')"/>
				</xsl:element>
			</xsl:if>
		</xsl:for-each>
	</xsl:template>

<!--26. URL-ENTRY template; mode=RELATIONS-->
<!--determine if the kind and url attriubte has content; if both have content, then output the appropriate dc:relation using dct:terms element-->
<!--outputs only dct:terms not dct:tems with the dct:URI attribute because not doing any checks to ensure meeting dct:URI requirements-->
<!--NSDL has checks in place to convert URLs to dct:URI-->
	<xsl:template match="d:urlEntry" mode="relations">
		<xsl:if test="(starts-with(./@kind, 'DLESE:') or starts-with(./@kind, 'DC:')) and string-length(./@url) > 0">
			<xsl:apply-templates select="./@kind"/>
		</xsl:if>
	</xsl:template>

<!--27. URL-ENTRY template; mode=SOURCE-->
<!--determine if the kind attriubte has a value of 'Is based on'-->
	<xsl:template match="d:urlEntry" mode="source">
		<xsl:if test="contains(./@kind, 'Is based on') and string-length(./@url) > 0">
			<xsl:element name="dc:source">
				<xsl:value-of select="./@url"/>
			</xsl:element>
		</xsl:if>
	</xsl:template>

<!--28. MIMETYPE -->
<!--  The following template was copied from: <xsl:import href="mimetypes-w-xsi-type.xsl"/> -->
	<xsl:template name="mimetype">
		<xsl:variable name="urlstring" select="substring-after(., '://')"/>
		<xsl:variable name="restofURL" select="substring-after($urlstring, '/')"/>
		<xsl:variable name="lowercase" select="translate($restofURL, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')"/>
		<xsl:element name="dc:format">
			<xsl:choose>

				<xsl:when test="contains($lowercase, '.ppt')">
					<xsl:text>application/vnd.ms-powerpoint</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.xls')">
					<xsl:text>application/vnd.ms-excel</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.abs')">
					<xsl:text>audio/x-mpeg</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.ai')">
					<xsl:attribute name="xsi:type">dct:IMT</xsl:attribute>
					<xsl:text>application/postscript</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.aif')">
					<xsl:text>audio/x-aiff</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.aifc')">
					<xsl:text>audio/x-aiff</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.aiff')">
					<xsl:text>audio/x-aiff</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.aim')">
					<xsl:text>application/x-aim</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.art')">
					<xsl:text>image/x-jg</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.asf')">
					<xsl:text>video/x-ms-asf</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.asx')">
					<xsl:text>video/x-ms-asf</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.au')">
					<xsl:attribute name="xsi:type">dct:IMT</xsl:attribute>
					<xsl:text>audio/basic</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.avi')">
					<xsl:text>video/x-msvideo</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.avx')">
					<xsl:text>video/x-rad-screenplay</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.bcpio')">
					<xsl:text>application/x-bcpio</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.bin')">
					<xsl:attribute name="xsi:type">dct:IMT</xsl:attribute>
					<xsl:text>application/octet-stream</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.bmp')">
					<xsl:text>image/bmp</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.body')">
					<xsl:text>text/html</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.cdf')">
					<xsl:text>application/x-cdf</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.cer')">
					<xsl:text>application/x-x509-ca-cert</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.class')">
					<xsl:text>application/java</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.cpio')">
					<xsl:text>application/x-cpio</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.csh')">
					<xsl:text>application/x-csh</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.css')">
					<xsl:attribute name="xsi:type">dct:IMT</xsl:attribute>
					<xsl:text>text/css</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.dib')">
					<xsl:text>image/bmp</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.doc')">
					<xsl:attribute name="xsi:type">dct:IMT</xsl:attribute>
					<xsl:text>application/msword</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.dtd')">
					<xsl:attribute name="xsi:type">dct:IMT</xsl:attribute>
					<xsl:text>application/xml-dtd</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.dv')">
					<xsl:text>video/x-dv</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.dvi')">
					<xsl:text>application/x-dvi</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.eps')">
					<xsl:attribute name="xsi:type">dct:IMT</xsl:attribute>
					<xsl:text>application/postscript</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.etx')">
					<xsl:text>text/x-setext</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.exe')">
					<xsl:attribute name="xsi:type">dct:IMT</xsl:attribute>
					<xsl:text>application/octet-stream</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.gif')">
					<xsl:attribute name="xsi:type">dct:IMT</xsl:attribute>
					<xsl:text>image/gif</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.gtar')">
					<xsl:text>application/x-gtar</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.gz')">
					<xsl:text>application/x-gzip</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.hdf')">
					<xsl:text>application/x-hdf</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.hqx')">
					<xsl:text>application/mac-binhex40</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.htc')">
					<xsl:text>text/x-component</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.htm')">
					<xsl:attribute name="xsi:type">dct:IMT</xsl:attribute>
					<xsl:text>text/html</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.html')">
					<xsl:attribute name="xsi:type">dct:IMT</xsl:attribute>
					<xsl:text>text/html</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.hqx')">
					<xsl:text>application/mac-binhex40</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.ief')">
					<xsl:attribute name="xsi:type">dct:IMT</xsl:attribute>
					<xsl:text>image/ief</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.jad')">
					<xsl:text>text/vnd.sun.j2me.app-descriptor</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.jar')">
					<xsl:text>application/java-archive</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.java')">
					<xsl:text>text/plain</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.jnlp')">
					<xsl:text>application/x-java-jnlp-file</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.jpe')">
					<xsl:attribute name="xsi:type">dct:IMT</xsl:attribute>
					<xsl:text>image/jpeg</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.jpeg')">
					<xsl:attribute name="xsi:type">dct:IMT</xsl:attribute>
					<xsl:text>image/jpeg</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.jpg')">
					<xsl:attribute name="xsi:type">dct:IMT</xsl:attribute>
					<xsl:text>image/jpeg</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.js')">
					<xsl:text>text/javascript</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.kar')">
					<xsl:text>audio/x-midi</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.latex')">
					<xsl:text>application/x-latex</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.m3u')">
					<xsl:text>audio/x-mpegurl</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.mac')">
					<xsl:text>image/x-macpaint</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.man')">
					<xsl:text>application/x-troff-man</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.me')">
					<xsl:text>application/x-troff-me</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.mid')">
					<xsl:text>audio/x-midi</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.midi')">
					<xsl:text>audio/x-midi</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.mif')">
					<xsl:text>application/x-mif</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.mov')">
					<xsl:attribute name="xsi:type">dct:IMT</xsl:attribute>
					<xsl:text>video/quicktime</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.movie')">
					<xsl:text>video/x-sgi-movie</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.mp1')">
					<xsl:text>audio/x-mpeg</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.mp2')">
					<xsl:text>audio/x-mpeg</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.mp3')">
					<xsl:text>audio/x-mpeg</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.mpa')">
					<xsl:text>audio/x-mpeg</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.mpe')">
					<xsl:attribute name="xsi:type">dct:IMT</xsl:attribute>
					<xsl:text>video/mpeg</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.mpeg')">
					<xsl:attribute name="xsi:type">dct:IMT</xsl:attribute>
					<xsl:text>video/mpeg</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.mpega')">
					<xsl:text>audio/x-mpeg</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.mpg')">
					<xsl:attribute name="xsi:type">dct:IMT</xsl:attribute>
					<xsl:text>video/mpeg</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.mpv2')">
					<xsl:text>video/mpeg2</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.ms')">
					<xsl:text>application/x-wais-source</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.nc')">
					<xsl:text>application/x-netcdf</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.oda')">
					<xsl:text>application/oda</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.pbm')">
					<xsl:text>image/x-portable-bitmap</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.pct')">
					<xsl:text>image/pict</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.pdf')">
					<xsl:attribute name="xsi:type">dct:IMT</xsl:attribute>
					<xsl:text>application/pdf</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.pgm')">
					<xsl:text>image/x-portable-graymap</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.pic')">
					<xsl:text>image/pict</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.pict')">
					<xsl:text>image/pict</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.pls')">
					<xsl:text>audio/x-scpls</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.png')">
					<xsl:attribute name="xsi:type">dct:IMT</xsl:attribute>
					<xsl:text>image/png</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.pnm')">
					<xsl:text>image/x-portable-anymap</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.pnt')">
					<xsl:text>image/x-macpaint</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.ppm')">
					<xsl:text>image/x-portable-pixmap</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.ps')">
					<xsl:attribute name="xsi:type">dct:IMT</xsl:attribute>
					<xsl:text>application/postscript</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.psd')">
					<xsl:text>image/x-photoshop</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.qt')">
					<xsl:attribute name="xsi:type">dct:IMT</xsl:attribute>
					<xsl:text>video/quicktime</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.qti')">
					<xsl:text>image/x-quicktime</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.qtif')">
					<xsl:text>image/x-quicktime</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.ras')">
					<xsl:text>image/x-cmu-raster</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.rgb')">
					<xsl:text>image/x-rgb</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.rm')">
					<xsl:text>application/vnd.rn-realmedia</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.roff')">
					<xsl:text>application/x-troff</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.rtf')">
					<xsl:attribute name="xsi:type">dct:IMT</xsl:attribute>
					<xsl:text>text/rtf</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.rtx')">
					<xsl:attribute name="xsi:type">dct:IMT</xsl:attribute>
					<xsl:text>text/richtext</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.sh')">
					<xsl:text>application/x-sh</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.shar')">
					<xsl:text>application/x-shar</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.smf')">
					<xsl:text>audio/x-midi</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.snd')">
					<xsl:attribute name="xsi:type">dct:IMT</xsl:attribute>
					<xsl:text>audio/basic</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.src')">
					<xsl:text>application/x-wais-source</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.sv4cpio')">
					<xsl:text>application/x-sv4cpio</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.sv4crc')">
					<xsl:text>application/x-sv4crc</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.swf')">
					<xsl:text>application/x-shockwave-flash</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.t')">
					<xsl:text>application/x-troff</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.tar')">
					<xsl:text>application/x-tar</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.tcl')">
					<xsl:text>application/x-tcl</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.tex')">
					<xsl:text>application/x-tex</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.texi')">
					<xsl:text>application/x-texinfo</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.texinfo')">
					<xsl:text>application/x-texinfo</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.tif')">
					<xsl:attribute name="xsi:type">dct:IMT</xsl:attribute>
					<xsl:text>image/tiff</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.tiff')">
					<xsl:attribute name="xsi:type">dct:IMT</xsl:attribute>
					<xsl:text>image/tiff</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.tr')">
					<xsl:text>application/x-troff</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.tsv')">
					<xsl:attribute name="xsi:type">dct:IMT</xsl:attribute>
					<xsl:text>text/tab-separated-values</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.txt')">
					<xsl:attribute name="xsi:type">dct:IMT</xsl:attribute>
					<xsl:text>text/plain</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.ulw')">
					<xsl:attribute name="xsi:type">dct:IMT</xsl:attribute>
					<xsl:text>audio/basic</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.ustar')">
					<xsl:text>application/x-ustar</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.xbm')">
					<xsl:text>image/x-xbitmap</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.xpm')">
					<xsl:text>image/x-xpixmap</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.xwd')">
					<xsl:text>image/x-xwindowdump</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.wav')">
					<xsl:text>audio/x-wav</xsl:text>
				</xsl:when>
<!-- Wireless Bitmap -->
				<xsl:when test="contains($lowercase, '.wbmp')">
					<xsl:attribute name="xsi:type">dct:IMT</xsl:attribute>
					<xsl:text>image/vnd.wap.wbmp</xsl:text>
				</xsl:when>
<!-- WML Source -->
				<xsl:when test="contains($lowercase, '.wml')">
					<xsl:text>text/vnd.wap.wml</xsl:text>
				</xsl:when>
<!-- Compiled WML -->
				<xsl:when test="contains($lowercase, '.wmlc')">
					<xsl:text>application/vnd.wap.wmlc</xsl:text>
				</xsl:when>
<!-- WML Script Source -->
				<xsl:when test="contains($lowercase, '.wmls')">
					<xsl:text>text/vnd.wap.wmls</xsl:text>
				</xsl:when>
<!-- Compiled WML Script -->
				<xsl:when test="contains($lowercase, '.wmlscriptc')">
					<xsl:text>application/vnd.wap.wmlscriptc</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.wrl')">
					<xsl:attribute name="xsi:type">dct:IMT</xsl:attribute>
					<xsl:text>model/vrml</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.Z')">
					<xsl:text>application/x-compress</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.z')">
					<xsl:text>application/x-compress</xsl:text>
				</xsl:when>

				<xsl:when test="contains($lowercase, '.zip')">
					<xsl:attribute name="xsi:type">dct:IMT</xsl:attribute>
					<xsl:text>application/zip</xsl:text>
				</xsl:when>

				<xsl:otherwise>
					<xsl:attribute name="xsi:type">dct:IMT</xsl:attribute>
					<xsl:text>text/html</xsl:text>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:element>
	</xsl:template>
</xsl:stylesheet>
