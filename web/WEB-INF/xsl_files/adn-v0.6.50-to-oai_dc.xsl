<?xml version="1.0"?>

<xsl:stylesheet 

    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"

    xmlns:xsi ="http://www.w3.org/2001/XMLSchema-instance"

    xmlns:d="http://adn.dlese.org"

    xmlns:dc="http://purl.org/dc/elements/1.1/"

    exclude-result-prefixes="xsi d " 

    version="1.0">



<!--ORGANIZATION OF THIS FILE-->

<!-- **************************************-->

<!--This file is organized into the following sections:

A. Purpose

B. License information and credits

C. Assumptions

D. Transformation code

E. Templates to apply (in alphabetical order)-->





<!--A. PURPOSE-->

<!-- **************************************-->

<!--To transform the Digital Library for Earth System Education (DLESE) ADN metadata records to simple Dublin Core that uses the Open Archives Initiative (OAI) namespace-->





<!--B. LICENSE INFORMATION and CREDITS-->

<!-- *****************************************************-->

<!--Date created: 2003-08-18 by Katy Ginger, University Corporation for Atmospheric Research (UCAR)-->

<!--Last modified: 2006-03-07 by Katy Ginger-->

<!--License information:

		Copyright (c) 2007 Digital Learning Sciences

		University Corporation for Atmospheric Research (UCAR)

		P.O. Box 3000, Boulder, CO 80307, United States of America

		All rights reserved

These XML tranformation written in XSLT 1.0 and XPATH 1.0 are free software; you can redistribute them and/or modify them under the terms of the GNU General Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any later version.  These XML instance documents are distributed in the hope that they will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this project; if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA -->



    

<!--C. ASSUMPTIONS-->

<!-- **************************************-->

<!--Overarching assumption. The metadata field only appear in the ADN metadata record if it contains data-->

<!--1. Applies to DLESE ADN metadata format, version 0.6.50 records-->

<!--2. Assumes content is present in ADN required feilds and does not check for the presence of it-->

<!--3. Unless otherwise indicated in this stylesheet, the transform applies to both ADN online and offline resources-->

<!--4. Transforms only ADN large bounding box and it associated placenames, not detailed geometries-->

<!--5. ADN relations.relation.kind=DLESE:Has thumbnail is not transformed-->

<!--6. ADN objectInSpace tags are not transformed-->

<!--7. Transforms only the first named time perion (temporalCoverages.timeAndPeriod.periods.name); all others are ignored because nsdl_dc does not allow for more than one named time period; so do the same for oai_dc-->

<!--8. ADN toolfor and beneficiary fields are not transformed-->

<!--9. ADN content standards are mapped to DC subject-->



	<xsl:output method="xml" indent="yes" encoding="UTF-8"/>





<!--D. TRANSFORMATION CODE-->

<!-- **********************************************************-->

	<xsl:template match="d:itemRecord">

		<xsl:element name="oai_dc:dc" namespace="http://www.openarchives.org/OAI/2.0/oai_dc/">

			<xsl:attribute name="xsi:schemaLocation">http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd</xsl:attribute>	





<!--dc:title-->

<!--since title is required metadata in ADN, no need to check to see if data is present-->

		<xsl:element name="dc:title">

			<xsl:value-of select="d:general/d:title"/>

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



<!--dc:subject - from content standards field-->

		<xsl:apply-templates select="d:educational/d:contentStandards/d:contentStandard"/>

		<!--see template CONTENTSTANDARD-->





<!--dc:date-->

<!--since ADN has many lifecycle.contributors.contributor.date values and these are not required, determine if any are present using a variable to grab all of them-->

 

<!--	variable for ADN lifecycle.contributors.contributor.date-->

		<xsl:variable name="alldates">

			<xsl:for-each select="d:lifecycle/d:contributors/d:contributor/@date">

				<xsl:value-of select="."/>

			</xsl:for-each>

		</xsl:variable>



<!--test to see if any date information is present-->

<!--if data is present; grab the date associated with the 1st occurrence of the date attribute on any contributor tag set-->

		<xsl:if test="string-length($alldates)>0">

			<xsl:element name="dc:date">

				<xsl:value-of select="d:lifecycle/d:contributors/d:contributor/@date"/>

			</xsl:element>	

		</xsl:if>

<!--end: dc:date-->



<!--dc:description-->

<!--a single dc:description field is constructed from the following ADN fields: general.description, technical.offline.accessInformation and 

educational.audiences.audience.gradeRange-->



<!--use a variable to gather up the fields that make description-->

<!--determine if the resource is online or offline-->

<!--a resource is offline if ADN technical.offline.accessInformation has data-->

<!--if the resource is offline, concatenate ADN general.description and ADN technical.offline.accessInformation-->

		<xsl:variable name="descr">

			<xsl:choose>

				<xsl:when test="string-length(d:technical/d:offline/d:accessInformation)>0">

					<xsl:value-of select="concat(d:general/d:description,' This is an offline resource with the following access information: ', d:technical/d:offline/d:accessInformation)"/>

				</xsl:when>

				<xsl:otherwise>

					<xsl:value-of select="d:general/d:description"/>

				</xsl:otherwise>

			</xsl:choose>

		</xsl:variable>





<!--use a variable to grab all grade ranges so they can be concatenated onto the end of the description field as a comma separated list-->

<!--only a single grade range field can occur in an audience field-->

<!--if the last grade range position is DLESE:To be supplied or DLESE:Not applicable then the comma separated list will end with a comma; otherwise it will be fine; did not do extra programming to rid this end comma situation-->

<!--end comma situation when the last grade range is not DLESE:To be supplied or DLESE:Not applicable are handled gracefully with the comma not appearing-->

<!--repeated grade ranges are not suppressed, except the DLESE:To be supplied and DLESE:Not applicable-->

		<xsl:variable name="allgradeRanges">

			<xsl:for-each select="d:educational/d:audiences/d:audience [position() !=last()]">

				<xsl:if test="not(contains(d:gradeRange, 'supplied')) and not(contains(d:gradeRange, 'Not applicable'))">

					<xsl:value-of select="concat(substring-after(d:gradeRange, 'DLESE:'), ', ')"/>

				</xsl:if>

			</xsl:for-each>

			<xsl:for-each select="d:educational/d:audiences/d:audience [position() =last()]">

				<xsl:if test="not(contains(d:gradeRange, 'supplied')) and not(contains(d:gradeRange, 'Not applicable'))">

					<xsl:value-of select="substring-after(d:gradeRange, 'DLESE:')"/>

				</xsl:if>

			</xsl:for-each>

		</xsl:variable>



<!--create description using the variable above but check to see if $allgradeRanges has any data; it may not if a record only had the grade range values of DLESE:To be supplied or DLESE:Not applicable-->

		<xsl:element name="dc:description">

			<xsl:choose>

				<xsl:when test="string-length($allgradeRanges) > 0">

					<xsl:value-of select="concat($descr, ' Educational levels: ', $allgradeRanges)"/>

				</xsl:when>

				<xsl:otherwise>

					<xsl:value-of select="$descr"/>

				</xsl:otherwise>

			</xsl:choose>

		</xsl:element>

	

<!--dc:format - size-->

<!--no dc:format - size for ADN offline resources-->

		<xsl:if test="string-length(d:technical/d:online/d:size) > 0">

			<xsl:element name="dc:format">

				<xsl:value-of select="d:technical/d:online/d:size"/>

			</xsl:element>

		</xsl:if>



<!--dc:format - general information-->

<!--use the combination of technical.online.mediums.medium and the technical.online.primaryURL to determine dc:format-->

<!--no dc:format for ADN offline resources-->



<!--dc:format - mime type-->

<!--dc:format use these terms: text, multipart, message, application, image, audio, video or model and the mime type list from the NSDL mime type list at: http://ns.nsdl.org/schemas/mime_type/mime_type_v1.00.xsd, and any free text that the ADN record has-->	



<!--dc:format - using ADN mediums; allows dc:format to repeat-->

<!--since medium repeats use a template-->

		<xsl:apply-templates select="d:technical/d:online/d:mediums/d:medium" mode="plain"/>

		<xsl:apply-templates select="d:technical/d:online/d:mediums/d:medium" mode="adn"/>

		<!--see template MEDIUM mode PLAIN-->

		<!--see template MEDIUM mode ADN-->

		

<!--dc:format - using ADN primaryURL-->

<!--use a template because may want to use the same code again -->

		<xsl:apply-templates select="d:technical/d:online" mode="plain"/>

		<!--see template ONLINE mode PLAIN-->

		



<!--dc:type-->

<!--no vocabulary mapping is necessary-->

<!--determine if the ADN metadata record refers to an online or offline resource-->

		<xsl:choose>

			<xsl:when test="string-length(d:technical/d:offline)>0">

				<xsl:element name="dc:type">

					<xsl:text>PhysicalObject</xsl:text>

				</xsl:element>

			</xsl:when>

			<xsl:otherwise>

				<xsl:apply-templates select="d:educational/d:resourceTypes/d:resourceType"/>

				<!--see template RESOURCETYPE -->

			</xsl:otherwise>

		</xsl:choose>



<!--dc:type-->

<!--vocabulary mapping is necessary-->

<!--determine if the ADN metadata record refers to an online or offline resource-->

<!--to prevent duplicate dc:type make a variable and test it-->



<!--	variable for ADN educational.resourceTypes.resourceType-->

		<xsl:variable name="allresourceTypes">

			<xsl:for-each select="d:educational/d:resourceTypes/d:resourceType">

				<xsl:value-of select="."/>

			</xsl:for-each>

		</xsl:variable>

	

		<xsl:choose> 



<!--dc:type: Collection-->

			<xsl:when test="contains($allresourceTypes, 'DLESE:Portal')">

				<xsl:element name="dc:type">

					<xsl:text>Collection</xsl:text>

				</xsl:element>

			</xsl:when>



<!--dc:type: Dataset-->

			<xsl:when test="contains($allresourceTypes, 'DLESE:Data')">

				<xsl:element name="dc:type">

					<xsl:text>Dataset</xsl:text>

				</xsl:element>

			</xsl:when>



<!--dc:type: Event-->

			<xsl:when test="contains($allresourceTypes, 'Webcast')">

				<xsl:element name="dc:type">

					<xsl:text>Event</xsl:text>

				</xsl:element>

			</xsl:when>



<!--dc:type: Image-->

			<xsl:when test="contains($allresourceTypes, 'DLESE:Visual')">

				<xsl:element name="dc:type">

					<xsl:text>Image</xsl:text>

				</xsl:element>

			</xsl:when>



<!--dc:type: InteractiveResource-->

			<xsl:when test="contains($allresourceTypes, 'DLESE:Learning materials') or contains($allresourceTypes, 'Calculation')">

				<xsl:element name="dc:type">

					<xsl:text>InteractiveResource</xsl:text>

				</xsl:element>

			</xsl:when>



<!--dc:type: Service-->

			<xsl:when test="contains($allresourceTypes, 'DLESE:Service')">

				<xsl:element name="dc:type">

					<xsl:text>Service</xsl:text>

				</xsl:element>

			</xsl:when>



<!--dc:type: Software-->

			<xsl:when test="contains($allresourceTypes, 'Code') or contains($allresourceTypes, 'Software')">

				<xsl:element name="dc:type">

					<xsl:text>Software</xsl:text>

				</xsl:element>

			</xsl:when>



<!--dc:type: Sound-->

			<xsl:when test="contains($allresourceTypes, 'Audio book') or contains($allresourceTypes, 'Lecture') or contains($allresourceTypes, 'Music') or contains($allresourceTypes, 'Oral') or contains($allresourceTypes, 'Radio') or contains($allresourceTypes, 'Sound')">

				<xsl:element name="dc:type">

					<xsl:text>Sound</xsl:text>

				</xsl:element>

			</xsl:when>



<!--dc:type: Text-->

			<xsl:when test="contains($allresourceTypes, 'DLESE:Text')">

				<xsl:element name="dc:type">

					<xsl:text>Text</xsl:text>

				</xsl:element>

			</xsl:when>



<!--dc:type: PhysicalObject-->

			<xsl:when test="string-length(d:technical/d:offline)>0">

				<xsl:element name="dc:type">

					<xsl:text>PhysicalObject</xsl:text>

				</xsl:element>

			</xsl:when>

		</xsl:choose>



<!--dc:identifier - using ADN catalog record id numbers-->

<!--all ADN records (online and offline) have a catalog record number that can be used as an indentifier for that collection-->

<!--but the catalog record number does not have meaning outside DLESE, so do not transform-->

<!--	<xsl:element name="dc:identifier">

			<xsl:value-of select="concat(d:metaMetadata/d:catalogEntries/d:catalog, ': ', d:metaMetadata/d:catalogEntries/d:catalog/@entry)"/>

		</xsl:element>-->



<!--dc:identifier - using ADN primary urls-->

<!--only online ADN resource will have a dc:identifier-->

<!--simple DC does not deal with the attribute dct:URI, so no worry about spaces in urls-->

<!-- do an if test to exclude offline resources-->

		<xsl:if test="string-length(d:technical/d:online/d:primaryURL) > 0">

			<xsl:element name="dc:identifier">

				<xsl:value-of select="d:technical/d:online/d:primaryURL"/>

			</xsl:element>

		</xsl:if>



<!--dc:source-->		

<!--ADN does not collect dc:source information yet for either online or offline resources-->



<!--dc:language-->

		<xsl:element name="dc:language">

			<xsl:value-of select="d:general/d:language"/>

		</xsl:element>



<!--dc:rights-->

		<xsl:element name="dc:rights">

			<xsl:value-of select="d:rights/d:description"/>

		</xsl:element>

	

<!--dc:coverage -general spatial information-->

<!--only ADN large bounding box and associated placenames, not detailed geometries, are transformed-->

<!--put ADN large bound box placenames in dc:coverage-->

<!--put ADN large bound box coordinates in dc:spatial - xsi:type=dct:Box-->

<!-- ADN larg bound box event names are not transformed -->



<!--dc:coverage - for ADN boundbox placenames-->

		<xsl:apply-templates select="d:geospatialCoverages/d:geospatialCoverage/d:boundBox/d:bbPlaces/d:place"/>

		<!--see template PLACE-->



<!--dc:coverage - for ADN boundbox coordinates-->

<!--no template used since only occurs once in ADN-->

		<xsl:if test="string-length(d:geospatialCoverages/d:geospatialCoverage/d:boundBox)>0">

			<xsl:element name="dc:coverage">

				<xsl:value-of select="concat('northlimit=', d:geospatialCoverages/d:geospatialCoverage/d:boundBox/d:northCoord, '; southlimit=', d:geospatialCoverages/d:geospatialCoverage/d:boundBox/d:southCoord, '; westlimit=', d:geospatialCoverages/d:geospatialCoverage/d:boundBox/d:westCoord, '; eastlimit=', d:geospatialCoverages/d:geospatialCoverage/d:boundBox/d:eastCoord, '; units=signed decimal degrees')"/>

			</xsl:element>

		</xsl:if> 





<!--dc:coverage - general time information-->

<!--use dc:coverage for ADN timeAD and named time periods; template TIMEAD-->

<!--use dc:coverage for ADN timeBC and named time period in timeBC; template TIMEBC-->

<!--use dc:coverage for ADN timeRelative; see template TIMERELATIVE-->



<!--dc:coverage for ADN timeAD-->

		<xsl:apply-templates select="d:temporalCoverages/d:timeAndPeriod/d:timeInfo/d:timeAD"/>

		<!--see template TIMEAD-->



<!--dc:coverage for ADN timeBC-->

		<xsl:apply-templates select="d:temporalCoverages/d:timeAndPeriod/d:timeInfo/d:timeBC"/>

		<!--see template TIMEBC-->



<!--process ADN timeRelative-->		

		<xsl:apply-templates select="d:temporalCoverages/d:timeAndPeriod/d:timeInfo/d:timeRelative"/>

		<!--see template TIMERELATIVE-->



		

<!--dc:relation-->

<!--ADN DLESE:Has thumbnail is not transformed-->

		<xsl:apply-templates select="d:relations/d:relation//@kind"/>

		<!--see template KIND-->





<!--end oai_dc:dc-->

		</xsl:element>

	</xsl:template>





<!--E. TEMPLATES TO APPLY (alphabetical order)-->

<!--**********************************************************-->

<!--1.   CONTENTSTANDARD writes DC subject with ADN content standards information-->

<!--2.   CONTRIBUTOR selects DC creator, publisher or contributor based on the ADN contributor role-->

<!--3.   GRADERANGE writes DC description last sentence with ADN grade range information-->

<!--4.   KIND writes DC relation with the kind of relationship being established-->

<!--5.   MEDIUM writes DC format mimetype by mapping free text of ADN medium to different mime types -->

<!--6.   MEDIUM template mode=ADN writes DC format mimetype using the ADN content of medium verbatim-->

<!--7.   MEDIUM template mode=PLAIN writes DC format mimetype but only the first part of the mime type like application, text, etc. by analyzing the free text of ADN medum; then calls MEDIUM to write the actual content-->

<!--8.   ONLINE template mode=PLAIN writes DC format tag and then calls PRIMARYURL to determine tag content-->

<!--9.   ORGANIZATION writes organization content for either DC contributor, creator or publisher-->

<!--10. PERSON writes person content for either DC contributor, creator or publisher-->

<!--11. PLACE writes DC coverage with the all placenames associated with the ADN large bounding box-->

<!--12. PRIMARYURL writes DC format mimetypes determining mime type from the ADN URL -->

<!--13. RELATIONS writes DC relation URL information with ADN relation inforamtion and substitutes URLs for catalog record number ids-->

<!--14. RESOURCETYPE writes DC type using the ADN resource type  -->

<!--15. SUBJECT template mode=DLESE writes DC subject from ADN subject but removes the leading 'DLESE:'-->

<!--16. SUBJECT template mode=KEYWORDS writes DC subject from ADN keywords -->

<!--17. TIMEAD writes DC coverage when time is AD-->

<!--18. TIMEBC writes DC coverage when time is BC-->

<!--19. TIMERELATIVE writes DC coverage when time is relative-->



<!--1. CONTENTSTANDARD template-->

	<xsl:template match="d:contentStandard">

		<xsl:element name="dc:subject">

			<xsl:choose>

<!--NSES-->

				<xsl:when test="contains(., 'NSES:')">

					<xsl:value-of select="concat('Supports National Science Education Standards (NSES): ', substring-after(., ':' ))"/>

				</xsl:when>

<!--AAAS-->

				<xsl:when test="contains(., 'AAASbenchmarks:')">

					<xsl:value-of select="concat('Supports American Association for the Advancement of Science (AAAS) Benchmarks: ', substring-after(., ':' ))"/>

				</xsl:when>

<!--NCGE-->

				<xsl:when test="contains(., 'NCGE:')">

					<xsl:value-of select="concat('Supports National Council for Geographic Education (NCGE) standard: ', substring-after(., ':' ))"/>

				</xsl:when>

<!--NCTM-->

				<xsl:when test="contains(., 'NCTM:')">

					<xsl:value-of select="concat('Supports National Council of Teachers of Mathemeatics (NCTM) standardE: ', substring-after(., ':' ))"/>

				</xsl:when>

<!--NETS-->

				<xsl:when test="contains(., 'NETS:')">

					<xsl:value-of select="concat('Supports National Educational Technology Standards (NETS): ', substring-after(., ':' ))"/>

				</xsl:when>

			</xsl:choose>

		</xsl:element>

	</xsl:template>



<!--2. CONTRIBUTOR template-->

<!--the term contact is not transformed since it is not a contributor, creator or publisher-->

	<xsl:template match="d:contributor">

		<xsl:choose>

			<xsl:when test="./@role='Contributor' or ./@role='Editor' ">

				<xsl:element name="dc:contributor">

					<xsl:apply-templates select="d:person"/>

					<xsl:apply-templates select="d:organization"/>

				</xsl:element>

			</xsl:when>



			<xsl:when test="./@role='Author' or ./@role='Principal Investigator' ">

				<xsl:element name="dc:creator">

					<xsl:apply-templates select="d:person"/>

					<xsl:apply-templates select="d:organization"/>

				</xsl:element>

			</xsl:when>



			<xsl:when test="./@role='Publisher' ">

				<xsl:element name="dc:publisher">

					<xsl:apply-templates select="d:person"/>

					<xsl:apply-templates select="d:organization"/>

				</xsl:element>

			</xsl:when>

		</xsl:choose>

	</xsl:template>			





<!--3. GRADERANGE template-->

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



<!--4. KIND template-->

	<xsl:template match="d:relations/d:relation//@kind">



		<xsl:choose>

<!--call-template halts processing of current template in order to call and complete anther template; then processing of the current template is resumed; if did not do this would get the unwnted result of all the mediums (mime types) being in a single dc:format tag-->



<!--dc:relation - isVersionOf-->

			<xsl:when test="contains(., 'Is version of')">

				<xsl:element name="dc:relation">

					<xsl:text>isVersionOf: </xsl:text>

					<xsl:call-template name="relations"/>

				</xsl:element>

			</xsl:when>

<!--dc:relation  hasVersion-->

			<xsl:when test="contains(., 'Has version')">

				<xsl:element name="dc:relation">

					<xsl:text>hasVersion: </xsl:text>

					<xsl:call-template name="relations"/>

				</xsl:element>

			</xsl:when>

<!--dc:relation- isReplacedBy-->

			<xsl:when test="contains(., 'Is replaced by')">

				<xsl:element name="dc:relation">

					<xsl:text>isReplacedBy: </xsl:text>

					<xsl:call-template name="relations"/>

				</xsl:element>

			</xsl:when>

<!--dc:relation - replaces-->

			<xsl:when test="contains(., 'Replaces')">

				<xsl:element name="dc:relation">

					<xsl:text>replaces: </xsl:text>

					<xsl:call-template name="relations"/>

				</xsl:element>

			</xsl:when>

<!--dc:relation - isRequiredBy-->

			<xsl:when test="contains(., 'Is required by')">

				<xsl:element name="dc:relation">

					<xsl:text>isRequiredBy: </xsl:text>

					<xsl:call-template name="relations"/>

				</xsl:element>

			</xsl:when>

<!--dc:relation - requires-->

			<xsl:when test="contains(., 'Requires')">

				<xsl:element name="dc:relation">

					<xsl:text>requires: </xsl:text>

					<xsl:call-template name="relations"/>

				</xsl:element>

			</xsl:when>

<!--dc:relation - isPartOf-->

			<xsl:when test="contains(., 'Is part of')">

				<xsl:element name="dc:relation">

					<xsl:text>isPartOf: </xsl:text>

					<xsl:call-template name="relations"/>

				</xsl:element>

			</xsl:when>

<!--dc:relation - hasPart-->

			<xsl:when test="contains(., 'Has part')">

				<xsl:element name="dc:relation">

					<xsl:text>hasPart: </xsl:text>

					<xsl:call-template name="relations"/>

				</xsl:element>

			</xsl:when>

<!--dc:relation - isReferencedby-->

			<xsl:when test="contains(., 'Is referenced by') or contains(., 'Is basis for')">

				<xsl:element name="dc:relation">

					<xsl:text>isReferencedBy: </xsl:text>

					<xsl:call-template name="relations"/>

				</xsl:element>

			</xsl:when>

<!--dc:relation - references-->

			<xsl:when test="contains(., 'References') or contains(., 'Is based on') or contains(., 'Is associated with')">

				<xsl:element name="dc:relation">

					<xsl:text>references: </xsl:text>				

					<xsl:call-template name="relations"/>

				</xsl:element>

			</xsl:when>

<!--dc:relation - isFormatOf-->

			<xsl:when test="contains(., 'Is format of')">

				<xsl:element name="dc:relation">

					<xsl:text>isFormatOf: </xsl:text>

					<xsl:call-template name="relations"/>

				</xsl:element>

			</xsl:when>

<!--dc:relation - hasFormat-->

			<xsl:when test="contains(., 'Has format')">

				<xsl:element name="dc:relation">

					<xsl:text>hasFormat: </xsl:text>

					<xsl:call-template name="relations"/>

				</xsl:element>

			</xsl:when>

<!--dc:relation - conformsTo-->

			<xsl:when test="contains(., 'Conforms to')">

				<xsl:element name="dc:relation">

					<xsl:text>conformsTo: </xsl:text>

					<xsl:call-template name="relations"/>

				</xsl:element>

			</xsl:when>



		</xsl:choose>

	</xsl:template>



<!--5. MEDIUM template-->

<!--since ADN technical.online.mediums.medium is free text, only an educated guess for dc:format is possible-->

<!--does not assume default value if no match is found-->	

	<xsl:template name="d:medium">

		<xsl:choose>

<!--application test-->

			<xsl:when test="contains(., 'application')">

				<xsl:text>application</xsl:text>

			</xsl:when>

<!--message test-->

			<xsl:when test="contains(., 'message')">

				<xsl:text>message</xsl:text>

			</xsl:when>

<!--video test-->

			<xsl:when test="contains(., 'video')">

				<xsl:text>video</xsl:text>

			</xsl:when>

<!--audio test-->

			<xsl:when test="contains(., 'audio')">

				<xsl:text>audio</xsl:text>

			</xsl:when>

<!--image test-->

			<xsl:when test="contains(., 'image')">

				<xsl:text>image</xsl:text>

			</xsl:when>

<!--model test-->

			<xsl:when test="contains(., 'model')">

				<xsl:text>model</xsl:text>

			</xsl:when>

<!--multipart test-->

			<xsl:when test="contains(., 'multipart')">

				<xsl:text>multipart</xsl:text>

			</xsl:when>

<!--text test-->

			<xsl:when test="contains(., 'text')">

				<xsl:text>text</xsl:text>

			</xsl:when>

		</xsl:choose>

	</xsl:template>			



<!--6. MEDIUM template mode=ADN-->

	<xsl:template match="d:medium" mode="adn">

		<xsl:element name="dc:format">

			<xsl:value-of select="."/>

		</xsl:element>

	</xsl:template>





<!--7. MEDIUM template mode=PLAIN-->

	<xsl:template match="d:medium" mode="plain">

<!--this template is intended to use the NSDL mimetype list. So if the data of the mimetype is not on the NSDL list, the resultant tag should not be blank. So check to make sure there is valid content to work with first. If not valid, the new dc:format-plain tag does not get written-->

		<xsl:if test="contains(., 'application') or contains(., 'message') or contains(., 'video') or contains(., 'audio') or contains(., 'image') or contains(., 'model') or contains(., 'multipart') or contains(., 'text')">

			<xsl:element name="dc:format">

			<!--call-template halts processing of current template in order to call and complete anther template; then processing of the current template is resumed; if did not do this would get the unwanted result of all the mediums (mime types) being in a single dc:format tag-->

				<xsl:call-template name="d:medium"/>

			</xsl:element>

		</xsl:if>

	</xsl:template>





<!--8. ONLINE template mode=PLAIN-->

	<xsl:template match="d:technical/d:online" mode="plain">

		<xsl:element name="dc:format">

<!--use apply-templates here rather than call-template; when tried to use call-template, the primaryURL template could not be found. Since there is only 1 primaryURL and you can not use / in call-template, apply-templates works fine then.-->

			<xsl:apply-templates select="d:primaryURL"/>

			<!--see template PRIMARYURL-->

		</xsl:element>

	</xsl:template>





<!--9. ORGANIZATION template-->

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





<!--10. PERSON template-->

	<xsl:template match="d:person">

		<xsl:value-of select="concat(d:nameFirst,' ',d:nameLast)"/>

	</xsl:template>			





<!--11. PLACE template-->

	<xsl:template match="d:place">

		<xsl:element name="dc:coverage">

			<xsl:value-of select="d:name"/>

		</xsl:element>

	</xsl:template>

	

<!--12. PRIMARYURL template -->

<!--makes an assumption of text/html if no match is found-->

	<xsl:template match="d:primaryURL">

		<xsl:choose>

			<xsl:when test="contains(., '.JPG')">

				<xsl:text>image/jpeg</xsl:text>

			</xsl:when>											



			<xsl:when test="contains(., '.GIF')">

				<xsl:text>image/gif</xsl:text>

			</xsl:when>											



			<xsl:when test="contains(., '.TXT')">

				<xsl:text>text/plain</xsl:text>

			</xsl:when>											



			<xsl:when test="contains(., '.abs')">

				<xsl:text>audio/x-mpeg</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.ai')">

				<xsl:text>application/postscript</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.aif')">

				<xsl:text>audio/x-aiff</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.aifc')">

				<xsl:text>audio/x-aiff</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.aiff')">

				<xsl:text>audio/x-aiff</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.aim')">

				<xsl:text>application/x-aim</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.art')">

				<xsl:text>image/x-jg</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.asf')">

				<xsl:text>video/x-ms-asf</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.asx')">

				<xsl:text>video/x-ms-asf</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.au')">

				<xsl:text>audio/basic</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.avi')">

				<xsl:text>video/x-msvideo</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.avx')">

				<xsl:text>video/x-rad-screenplay</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.bcpio')">

				<xsl:text>application/x-bcpio</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.bin')">

				<xsl:text>application/octet-stream</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.bmp')">

				<xsl:text>image/bmp</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.body')">

				<xsl:text>text/html</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.cdf')">

				<xsl:text>application/x-cdf</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.cer')">

				<xsl:text>application/x-x509-ca-cert</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.class')">

				<xsl:text>application/java</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.cpio')">

				<xsl:text>application/x-cpio</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.csh')">

				<xsl:text>application/x-csh</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.css')">

				<xsl:text>text/css</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.dib')">

				<xsl:text>image/bmp</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.doc')">

				<xsl:text>application/msword</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.dtd')">

				<xsl:text>text/plain</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.dv')">

				<xsl:text>video/x-dv</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.dvi')">

				<xsl:text>application/x-dvi</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.eps')">

				<xsl:text>application/postscript</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.etx')">

				<xsl:text>text/x-setext</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.exe')">

				<xsl:text>application/octet-stream</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.gif')">

				<xsl:text>image/gif</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.gtar')">

				<xsl:text>application/x-gtar</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.gz')">

				<xsl:text>application/x-gzip</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.hdf')">

				<xsl:text>application/x-hdf</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.hqx')">

				<xsl:text>application/mac-binhex40</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.htc')">

				<xsl:text>text/x-component</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.htm')">

				<xsl:text>text/html</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.html')">

				<xsl:text>text/html</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.hqx')">

				<xsl:text>application/mac-binhex40</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.ief')">

				<xsl:text>image/ief</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.jad')">

				<xsl:text>text/vnd.sun.j2me.app-descriptor</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.jar')">

				<xsl:text>application/java-archive</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.java')">

				<xsl:text>text/plain</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.jnlp')">

				<xsl:text>application/x-java-jnlp-file</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.jpe')">

				<xsl:text>image/jpeg</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.jpeg')">

				<xsl:text>image/jpeg</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.jpg')">

				<xsl:text>image/jpeg</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.js')">

				<xsl:text>text/javascript</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.kar')">

				<xsl:text>audio/x-midi</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.latex')">

				<xsl:text>application/x-latex</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.m3u')">

				<xsl:text>audio/x-mpegurl</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.mac')">

				<xsl:text>image/x-macpaint</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.man')">

				<xsl:text>application/x-troff-man</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.me')">

				<xsl:text>application/x-troff-me</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.mid')">

				<xsl:text>audio/x-midi</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.midi')">

				<xsl:text>audio/x-midi</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.mif')">

				<xsl:text>application/x-mif</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.mov')">

				<xsl:text>video/quicktime</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.movie')">

				<xsl:text>video/x-sgi-movie</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.mp1')">

				<xsl:text>audio/x-mpeg</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.mp2')">

				<xsl:text>audio/x-mpeg</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.mp3')">

				<xsl:text>audio/x-mpeg</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.mpa')">

				<xsl:text>audio/x-mpeg</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.mpe')">

				<xsl:text>video/mpeg</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.mpeg')">

				<xsl:text>video/mpeg</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.mpega')">

				<xsl:text>audio/x-mpeg</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.mpg')">

				<xsl:text>video/mpeg</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.mpv2')">

				<xsl:text>video/mpeg2</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.ms')">

				<xsl:text>application/x-wais-source</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.nc')">

				<xsl:text>application/x-netcdf</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.oda')">

				<xsl:text>application/oda</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.pbm')">

				<xsl:text>image/x-portable-bitmap</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.pct')">

				<xsl:text>image/pict</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.pdf')">

				<xsl:text>application/pdf</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.pgm')">

				<xsl:text>image/x-portable-graymap</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.pic')">

				<xsl:text>image/pict</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.pict')">

				<xsl:text>image/pict</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.pls')">

				<xsl:text>audio/x-scpls</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.png')">

				<xsl:text>image/png</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.pnm')">

				<xsl:text>image/x-portable-anymap</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.pnt')">

				<xsl:text>image/x-macpaint</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.ppm')">

				<xsl:text>image/x-portable-pixmap</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.ps')">

				<xsl:text>application/postscript</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.psd')">

				<xsl:text>image/x-photoshop</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.qt')">

				<xsl:text>video/quicktime</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.qti')">

				<xsl:text>image/x-quicktime</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.qtif')">

				<xsl:text>image/x-quicktime</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.ras')">

				<xsl:text>image/x-cmu-raster</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.rgb')">

				<xsl:text>image/x-rgb</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.rm')">

				<xsl:text>application/vnd.rn-realmedia</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.roff')">

				<xsl:text>application/x-troff</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.rtf')">

				<xsl:text>application/rtf</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.rtx')">

				<xsl:text>text/richtext</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.sh')">

				<xsl:text>application/x-sh</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.shar')">

				<xsl:text>application/x-shar</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.smf')">

				<xsl:text>audio/x-midi</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.snd')">

				<xsl:text>audio/basic</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.src')">

				<xsl:text>application/x-wais-source</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.sv4cpio')">

				<xsl:text>application/x-sv4cpio</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.sv4crc')">

				<xsl:text>application/x-sv4crc</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.swf')">

				<xsl:text>application/x-shockwave-flash</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.t')">

				<xsl:text>application/x-troff</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.tar')">

				<xsl:text>application/x-tar</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.tcl')">

				<xsl:text>application/x-tcl</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.tex')">

				<xsl:text>application/x-tex</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.texi')">

				<xsl:text>application/x-texinfo</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.texinfo')">

				<xsl:text>application/x-texinfo</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.tif')">

				<xsl:text>image/tiff</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.tiff')">

				<xsl:text>image/tiff</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.tr')">

				<xsl:text>application/x-troff</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.tsv')">

				<xsl:text>text/tab-separated-values</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.txt')">

				<xsl:text>text/plain</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.ulw')">

				<xsl:text>audio/basic</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.ustar')">

				<xsl:text>application/x-ustar</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.xbm')">

				<xsl:text>image/x-xbitmap</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.xpm')">

				<xsl:text>image/x-xpixmap</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.xwd')">

				<xsl:text>image/x-xwindowdump</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.wav')">

				<xsl:text>audio/x-wav</xsl:text>

			</xsl:when>

<!-- Wireless Bitmap -->

			<xsl:when test="contains(., '.wbmp')">

				<xsl:text>image/vnd.wap.wbmp</xsl:text>

			</xsl:when>

<!-- WML Source -->

			<xsl:when test="contains(., '.wml')">

				<xsl:text>text/vnd.wap.wml</xsl:text>

			</xsl:when>

<!-- Compiled WML -->

			<xsl:when test="contains(., '.wmlc')">

				<xsl:text>application/vnd.wap.wmlc</xsl:text>

			</xsl:when>

<!-- WML Script Source -->

			<xsl:when test="contains(., '.wmls')">

				<xsl:text>text/vnd.wap.wmls</xsl:text>

			</xsl:when>

<!-- Compiled WML Script -->

			<xsl:when test="contains(., '.wmlscriptc')">

				<xsl:text>application/vnd.wap.wmlscriptc</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.wrl')">

				<xsl:text>x-world/x-vrml</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.Z')">

				<xsl:text>application/x-compress</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.z')">

				<xsl:text>application/x-compress</xsl:text>

			</xsl:when>



			<xsl:when test="contains(., '.zip')">

				<xsl:text>application/zip</xsl:text>

			</xsl:when>

			<xsl:otherwise>

				<xsl:text>text/html</xsl:text>

			</xsl:otherwise>

					</xsl:choose>

	</xsl:template>			



<!--13. RELATIONS template-->

	<xsl:template name="relations">

		<xsl:value-of select="../@url"/>

		<!--DLESE catalog record number ids do not have meaning as stand alone text; convert the catalog record number id to a DLESE web address. This web address will not work for relations that reference resources by catalog record number id that are not in DLESE-->

		<xsl:value-of select="concat('http://www.dlese.org/dds/catalog_', ../@entry, '.htm')"/>

	</xsl:template>	



<!--14. RESOURCETYPE template-->

	<xsl:template match="d:resourceType">

		<xsl:choose>

			<xsl:when test="contains(.,'supplied')"/>

			<xsl:otherwise>

				<xsl:element name="dc:type">

					<xsl:value-of select="substring-after(., 'DLESE:')"/>

				</xsl:element>

			</xsl:otherwise>

		</xsl:choose>

	</xsl:template>



<!--15. SUBJECT template mode=DLESE-->

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



<!--16. SUBJECT template mode=KEYWORDS-->

	<xsl:template match="d:keyword" mode="keywords">

		<xsl:element name="dc:subject">

			<xsl:value-of select="."/>

		</xsl:element>

	</xsl:template>			



<!--17. TIMEAD template-->

	<xsl:template match="d:timeAD">

		<xsl:element name="dc:coverage">

<!--determine if there is a named time period -->

<!--when ADN named time periods are transformed only the first named time period is transformed, all others are ignored; nsdl_dc does not allow for more than one named time period; so do the same for oai_dc-->

			<xsl:choose>

				<xsl:when test="string-length(../../d:periods)>0">

					<xsl:value-of select="concat('start=', d:begin/@date, '; end=', d:end/@date, '; name=', ../../d:periods/d:period/d:name)"/>

				</xsl:when>

				<xsl:otherwise>

					<xsl:value-of select="concat('start=', d:begin/@date, '; end=', d:end/@date)"/>

				</xsl:otherwise>

			</xsl:choose>	

		</xsl:element>

	</xsl:template>



<!--18. TIMEBC template-->

	<xsl:template match="d:timeBC">

		<xsl:element name="dc:coverage">

<!--determine if there is a named time period-->

<!--when ADN named time periods are transformed only the first named time period is transformed, all others are ignored; nsdl_dc does not allow for more than one named time period; so do the same for oai_dc-->

			<xsl:choose>

				<xsl:when test="string-length(../../d:periods)>0">

					<xsl:value-of select="concat('start=', d:begin, ' BC; end=', d:end, ' BC; name=', ../../d:periods/d:period/d:name)"/>

				</xsl:when>

				<xsl:otherwise>

					<xsl:value-of select="concat('start=', d:begin, ' BC; end=', d:end, ' BC')"/>

				</xsl:otherwise>

			</xsl:choose>	

		</xsl:element>

	</xsl:template>



<!--19. TIMERELATIVE template-->

	<xsl:template match="d:timeRelative">

		<xsl:element name="dc:coverage">

<!--determine if there is a named time period in order to make the tag content correct-->

<!--when ADN named time periods are transformed only the first named time period is transformed, all others are ignored; nsdl_dc does not allow for more than one named time period-->

			<xsl:choose>

				<xsl:when test="string-length(../../d:periods)>0">

					<xsl:value-of select="concat('start=', d:begin, ' ', d:begin/@units, '; end=', d:end, ' ', d:end/@units, '; name=', ../../d:periods/d:period/d:name)"/>

				</xsl:when>

				<xsl:otherwise>

					<xsl:value-of select="concat('start=', d:begin, ' ', d:begin/@units, '; end=', d:end, ' ', d:end/@units)"/>

				</xsl:otherwise>

			</xsl:choose>	

		</xsl:element>

	</xsl:template>





</xsl:stylesheet>

