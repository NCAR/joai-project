<?xml version="1.0"?>
<xsl:stylesheet 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xsi ="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:d="http://adn.dlese.org"
    xmlns:dc="http://purl.org/dc/elements/1.1/"
    xmlns:nsdl_dc="http://ns.nsdl.org/nsdl_dc_v1.02/"
    xmlns:dct="http://purl.org/dc/terms/"
    xmlns:ieee="http://www.ieee.org/xsd/LOMv1p0"
    exclude-result-prefixes="xsi d dc xsi nsdl_dc dct ieee"
    version="1.0">

<!--ORGANIZATION OF THIS FILE-->
<!-- **************************************-->
<!--The top half of this file, before the break with 5 lines of blank comments, is the apply-templates logic. This section is organized in document order of an ADN  item-level metadata record. The bottom half, after the break with 5 lines of blank comments, are the templates. The templates are organized in alphabetical order.-->

<!--ASSUMPTIONS-->
<!-- **************************************-->
<!--1. The transform is run over only ADN version 0.6.50 records a collection considers to be accessioned-->
<!--2. Unless otherwise indicated in this stylesheet, the transform applies to both ADN online and offline resources-->
<!--3. Only the ADN large bounding box and its associated places, not detailed geometries, are transformed-->
<!--4. ADN relations.relation.kind=DLESE:Has thumbnail is not transformed-->
<!--5. ADN objectInSpace tags are not transformed-->
<!--6. When ADN named time periods are transformed only the first named time period is transformed, all others are ignored; nsdl_dc does not allow for more than one named time period-->
<!--7. Any ADN timeAD.begin.date or timeAD.end.date with a value of 'Present' is not transformed for the time scheme of W3CDTF or dct:Period. It it transformed under the plain dct:temporal tags-->

	<xsl:output method="xml" indent="yes" encoding="UTF-8"/>

<!--VARIABLES used throughout the transform-->
<!-- **********************************************************-->
<!--variable for adding a line return-->
	<xsl:variable name="newline">
<xsl:text>
</xsl:text>
	</xsl:variable>

<!--variable for adding a space-->
	<xsl:variable name="percent">

		<xsl:text>%20</xsl:text>
	</xsl:variable>


	<xsl:template match="d:itemRecord">
<!--Could not get the namespaces to work with the xsl:element command-->
<!--		<xsl:element name="nsdl_dc:nsdl_dc" >
			<xsl:attribute name="schemaVersion">1.02.000</xsl:attribute>
			<xsl:attribute name="xsi:schemaLocation">http://ns.nsdl.org/nsdl_dc_v1.02/ http://ns.nsdl.org/schemas/nsdl_dc/nsdl_dc_v1.02.xsd</xsl:attribute>
			<xsl:attribute name="hi">
			<xsl:text disable-output-escaping="yes">xmlns:dc=&quot;http://purl.org/dc/elements/1.1/&quot; xmlns:dct=&quot;http://purl.org/dc/terms/&quot; xmlns:ieee=&quot;http://www.ieee.org/xsd/LOMv1p0&quot;</xsl:text></xsl:attribute> -->

		<xsl:text disable-output-escaping="yes">&lt;nsdl_dc:nsdl_dc schemaVersion=&quot;1.02.000&quot;  xmlns:nsdl_dc=&quot;http://ns.nsdl.org/nsdl_dc_v1.02/&quot; xmlns:dc=&quot;http://purl.org/dc/elements/1.1/&quot; xmlns:dct=&quot;http://purl.org/dc/terms/&quot; xmlns:ieee=&quot;http://www.ieee.org/xsd/LOMv1p0&quot; xmlns:xsi=&quot;http://www.w3.org/2001/XMLSchema-instance&quot; xsi:schemaLocation=&quot;http://ns.nsdl.org/nsdl_dc_v1.02/ http://ns.nsdl.org/schemas/nsdl_dc/nsdl_dc_v1.02.xsd&quot;&gt;</xsl:text>

		<xsl:value-of select="$newline"/>

<!--dc:title-->
		<xsl:element name="dc:title">
			<xsl:value-of select="d:general/d:title"/>
		</xsl:element>	

<!--dc:creator - person-->
		<xsl:apply-templates select="d:lifecycle/d:contributors/d:contributor/d:person" mode="creator"/>
		<xsl:apply-templates select="d:lifecycle/d:contributors/d:contributor/d:organization" mode="creator"/>
		<!--see template PERSON mode=CREATOR-->

		<!--see template ORGANIZATION mode=CREATOR-->

<!--dc:subject - DLESE-->
		<xsl:apply-templates select="d:general/d:subjects/d:subject" mode="DLESE"/>
		<!--see template SUBJECT mode=DLESE-->

<!--dc:subject - from keywords-->
		<xsl:apply-templates select="d:general/d:keywords/d:keyword" mode="keywords"/>
		<!--see template SUBJECT mode=KEYWORDS-->

<!--dc:subject - type=nsdl_dc:GEM-->
<!--to prevent nsdl_dc:GEM entries of 'Science', 'Earth science' and 'Physical science' from appearing so many times, grab all the contents of ADN subject tags and if it contains any of the following listed below, then make a GEM entry-->

<!--for single mappings of ADN subject (e.g. Agriculture) apply the subject template-->

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

<!--for nsdl_dc:GEM: other terms-->
		<xsl:apply-templates select="d:general/d:subjects/d:subject" mode="nsdl_dc:GEM"/>
		<!--see template SUBJECT mode=nsdl_dc:GEM-->

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

<!--test to see if any date information is present; if so, this code will grab the date of the first date attribute only-->
		<xsl:if test="string-length($alldates)>0">
			<xsl:element name="dc:date">
				<xsl:attribute name="xsi:type">dct:W3CDTF</xsl:attribute>
				<xsl:value-of select="d:lifecycle/d:contributors/d:contributor/@date"/>

			</xsl:element>	
		</xsl:if>
<!--end: dc:date-->

<!--dc:description-->
<!--determine if the resource is online or offline; if offline, concatenate ADN general.description and ADN technical.offline.accessInformation-->

		<xsl:choose>
			<xsl:when test="string-length(d:technical/d:offline/d:accessInformation)>0">
				<xsl:element name="dc:description">
					<xsl:value-of select="concat(d:general/d:description,' This is an offline resource with the following access information: ',d:technical/d:offline/d:accessInformation)"/>
				</xsl:element>

				<xsl:element name="dct:accessRights">
					<xsl:value-of select="d:technical/d:offline/d:accessInformation"/>
				</xsl:element>
			</xsl:when>
			<xsl:otherwise>
				<xsl:element name="dc:description">
					<xsl:value-of select="d:general/d:description"/>
				</xsl:element>
			</xsl:otherwise>

		</xsl:choose>


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
	
<!--dc:extent -->
<!--no dc:extent - size for ADN offline resources-->
		<xsl:if test="string-length(d:technical/d:online/d:size) > 0">
			<xsl:element name="dct:extent">
				<xsl:value-of select="d:technical/d:online/d:size"/>
			</xsl:element>

		</xsl:if>

<!--dc:format - size-->
<!--no dc:format - size for ADN offline resources-->
		<xsl:if test="string-length(d:technical/d:online/d:size) > 0">
			<xsl:element name="dc:format">
				<xsl:value-of select="d:technical/d:online/d:size"/>
			</xsl:element>
		</xsl:if>

<!--dc:format - general information-->

<!--use the combination of technical.online.mediums.medium and the technical.online.primaryURL to determine dc:format-->
<!--no dc:format - plain or dc:format- type=dct:IMT for ADN offline resources-->

<!--dc:format - plain - information-->
<!--dc:format - plain and dct:IMT use these terms: text, multipart, message, application, image, audio, video or model and the mime type list from the NSDL mime type list at: http://ns.nsdl.org/schemas/mime_type/mime_type_v1.00.xsd, and any free text that the ADN record has-->	
<!--dc:format - plain  using ADN mediums; allows dc:format to repeat-->
<!--since medium repeats use a template-->
		<xsl:apply-templates select="d:technical/d:online/d:mediums/d:medium" mode="plain"/>
		<xsl:apply-templates select="d:technical/d:online/d:mediums/d:medium" mode="adn"/>
		<!--see template MEDIUM mode PLAIN-->
		<!--see template MEDIUM mode ADN-->
		
<!--	dc:format - plain using ADN primaryURL-->

<!--use a template because want to use the same code again for dc:format - dct:IMT-->
		<xsl:apply-templates select="d:technical/d:online" mode="plain"/>
		<!--see template ONLINE mode PLAIN-->
		
<!--dc:format - dct:IMT - information-->
<!--dc:format - type=dct:IMT must select from NSDL list at http://ns.nsdl.org/schemas/mime_type/mime_type_v1.00.xsd-->
<!--dc:format - dct:IMT using ADN mediums; allows dc:format to repeat-->		
<!--since medium repeats use a template-->
		<xsl:apply-templates select="d:technical/d:online/d:mediums/d:medium" mode="dct:IMT"/>
		<!--see template MEDIUM mode DCT:IMT-->
		
<!--	dc:format - dct:IMT using ADN primaryURL-->
<!--use a template because want to use the same code again for dc:format - dct:IMT-->

		<xsl:apply-templates select="d:technical/d:online" mode="dct:IMT"/>
		<!--see template ONLINE mode DC:IMT-->
		
<!--dc:medium-->
<!--ADN does not collect medium information yet for either online or offline resources-->


<!--dc:type - general information-->
<!--use ADN educational.resourceTypes.resourceType twice to complete dc:type plain and dct:DCMI-->
<!--dc:type - plain-->
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
				<!--see template RESOURCETYPE-->
			</xsl:otherwise>

		</xsl:choose>

<!--dc:type - dct:DCMI-->
<!--vocabulary mapping is necessary-->
<!--determine if the ADN metadata record refers to an online or offline resource-->
<!--to prevent duplicate dc:type -dct:DCMI tags (like Image or Text) from appearing, make a variable and test it-->

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
					<xsl:attribute name="xsi:type">dct:DCMIType</xsl:attribute>
					<xsl:text>Collection</xsl:text>
				</xsl:element>

			</xsl:when>

<!--dc:type: Dataset-->
			<xsl:when test="contains($allresourceTypes, 'DLESE:Data')">
				<xsl:element name="dc:type">
					<xsl:attribute name="xsi:type">dct:DCMIType</xsl:attribute>
					<xsl:text>Dataset</xsl:text>
				</xsl:element>
			</xsl:when>

<!--dc:type: Event-->
			<xsl:when test="contains($allresourceTypes, 'Webcast')">
				<xsl:element name="dc:type">
					<xsl:attribute name="xsi:type">dct:DCMIType</xsl:attribute>
					<xsl:text>Event</xsl:text>
				</xsl:element>
			</xsl:when>

<!--dc:type: Image-->
			<xsl:when test="contains($allresourceTypes, 'DLESE:Visual')">
				<xsl:element name="dc:type">
					<xsl:attribute name="xsi:type">dct:DCMIType</xsl:attribute>
					<xsl:text>Image</xsl:text>
				</xsl:element>
			</xsl:when>

<!--dc:type: InteractiveResource-->

			<xsl:when test="contains($allresourceTypes, 'DLESE:Learning materials') or contains($allresourceTypes, 'Calculation')">
				<xsl:element name="dc:type">
					<xsl:attribute name="xsi:type">dct:DCMIType</xsl:attribute>
					<xsl:text>InteractiveResource</xsl:text>
				</xsl:element>
			</xsl:when>

<!--dc:type: Service-->
			<xsl:when test="contains($allresourceTypes, 'DLESE:Service')">

				<xsl:element name="dc:type">
					<xsl:attribute name="xsi:type">dct:DCMIType</xsl:attribute>
					<xsl:text>Service</xsl:text>
				</xsl:element>
			</xsl:when>

<!--dc:type: Software-->
			<xsl:when test="contains($allresourceTypes, 'Code') or contains($allresourceTypes, 'Software')">
				<xsl:element name="dc:type">

					<xsl:attribute name="xsi:type">dct:DCMIType</xsl:attribute>
					<xsl:text>Software</xsl:text>
				</xsl:element>
			</xsl:when>

<!--dc:type: Sound-->
			<xsl:when test="contains($allresourceTypes, 'Audio book') or contains($allresourceTypes, 'Lecture') or contains($allresourceTypes, 'Music') or contains($allresourceTypes, 'Oral') or contains($allresourceTypes, 'Radio') or contains($allresourceTypes, 'Sound')">
				<xsl:element name="dc:type">
					<xsl:attribute name="xsi:type">dct:DCMIType</xsl:attribute>

					<xsl:text>Sound</xsl:text>
				</xsl:element>
			</xsl:when>

<!--dc:type: Text-->
			<xsl:when test="contains($allresourceTypes, 'DLESE:Text')">
				<xsl:element name="dc:type">
					<xsl:attribute name="xsi:type">dct:DCMIType</xsl:attribute>
					<xsl:text>Text</xsl:text>

				</xsl:element>
			</xsl:when>

<!--dc:type: PhysicalPbject-->
			<xsl:when test="string-length(d:technical/d:offline)>0">
				<xsl:element name="dc:type">
					<xsl:attribute name="xsi:type">dct:DCMIType</xsl:attribute>
					<xsl:text>PhysicalObject</xsl:text>
				</xsl:element>

			</xsl:when>
		</xsl:choose>

<!--dc:type - nsdl_dc-->
<!--ADN does not collect dc:type - nsdl_dc resource type information yet for either online or offline resources-->

<!--dc:identifier - plain-->
<!--all ADN records (online and offline) have a number that can be used as an indentifier for that collection-->
<!--		<xsl:element name="dc:identifier">
			<xsl:value-of select="concat(d:metaMetadata/d:catalogEntries/d:catalog, ': ', d:metaMetadata/d:catalogEntries/d:catalog/@entry)"/>
		</xsl:element> -->

<!--dc:identifier - dct:URI-->
<!--only online ADN resource will have a dc:identifier - dct:URI tag-->
<!-- do an if test to exclude offline resources-->
		<xsl:if test="string-length(d:technical/d:online/d:primaryURL) > 0">

			<xsl:element name="dc:identifier">
				<xsl:attribute name="xsi:type">dct:URI</xsl:attribute>
				<!--test to see if url has blank spaces; if it does, fix them because type URI cannot have spaces-->
				<!--this testing assumes a maximum of 2 blank spaces in the url-->
				<xsl:choose>
					<xsl:when test="contains(d:technical/d:online/d:primaryURL, ' ')">
					<!--fix the first blank space occurrence-->
					<xsl:variable name="buildurl">

						<xsl:value-of select="concat(substring-before(d:technical/d:online/d:primaryURL, ' '), '%20', substring-after(d:technical/d:online/d:primaryURL, ' '))"/>
					</xsl:variable>
					<!--test and fix for a 2nd blank space occurrence-->
					<xsl:choose>
						<xsl:when test="contains($buildurl, ' ')">
							<!--this is the write if there are 2 blank spaces-->
							<xsl:value-of select="concat(substring-before($buildurl, ' '), '%20', substring-after($buildurl, ' '))"/>
						</xsl:when>
						<xsl:otherwise>

							<!--this is the write if there is only 1 blank space-->
							<xsl:value-of select="$buildurl"/>
						</xsl:otherwise>
					</xsl:choose>
<!--next line works on all blank spaces at once but only replaces with a % rather thana %20-->
<!--W3C note: If the third argument string is longer than the second argument string, then excess characters are ignored-->
<!--						<xsl:value-of select="translate(d:technical/d:online/d:primaryURL, ' ', '%20')"/>		-->
					</xsl:when>
					<xsl:otherwise>
						<!--this is the write if there are no blank spaces-->

						<xsl:value-of select="d:technical/d:online/d:primaryURL"/>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:element>
		</xsl:if>
		
<!--dc:identifier - dct:ISBN-->
<!--ADN does not collect-->

<!--dc:identifier - dct:ISSN-->
<!--ADN does not collect-->

<!--dc:source plain and dct:URI-->		
<!--ADN does not collect dc:source information yet for either online or offline resources-->

<!--dc:language plain-->
<!--ADN does not collect dc:language- plain information-->

<!--dc:language - dct:ISO639-2-->
		<xsl:element name="dc:language">
			<xsl:attribute name="xsi:type">dct:ISO639-2</xsl:attribute>
			<xsl:value-of select="d:general/d:language"/>
		</xsl:element>

<!--dc:language - dct:RFC3066-->
<!--ADN does not collect-->
	
<!--dc:rights-->
		<xsl:element name="dc:rights">
			<xsl:value-of select="d:rights/d:description"/>
		</xsl:element>
	
<!--dc:coverage and dc:spatial general information-->
<!--only ADN large bounding box and associated placenames, not detailed geometries, are transformed-->
<!--put ADN large bound box placenames in dc:coverage-->
<!--put ADN large bound box coordinates in dc:spatial - xsi:type=dct:Box-->
<!-- ADN larg bound box event names are not transformed -->

<!--dc:coverage-->
		<xsl:apply-templates select="d:geospatialCoverages/d:geospatialCoverage/d:boundBox/d:bbPlaces/d:place"/>
		<!--see template PLACE-->

<!--dct:spatial - dct:Box-->
<!--no template used since only occurs once in ADN-->
		<xsl:if test="string-length(d:geospatialCoverages/d:geospatialCoverage/d:boundBox)>0">
			<xsl:element name="dct:spatial">
				<xsl:attribute name="xsi:type">dct:Box</xsl:attribute>
				<xsl:value-of select="concat('northlimit=', d:geospatialCoverages/d:geospatialCoverage/d:boundBox/d:northCoord, '; southlimit=', d:geospatialCoverages/d:geospatialCoverage/d:boundBox/d:southCoord, '; westlimit=', d:geospatialCoverages/d:geospatialCoverage/d:boundBox/d:westCoord, '; eastlimit=', d:geospatialCoverages/d:geospatialCoverage/d:boundBox/d:eastCoord, '; units=signed decimal degrees')"/>

			</xsl:element>
		</xsl:if>


<!--dct:temporal general information-->
<!--use dct:temporal xsi:type=dct:Period for ADN timeAD and named time period; template TIMEAD mode DCT:PERIOD-->
<!--use dct:temporal xsi:type=dct:W3CDTF for ADN timeAD; template TIMEAD mode DCT:W3CDTF-->
<!--use dc:temporal for ADN timeAD and named time periods; template TIMEAD mode PLAIN-->
<!--use dct:temporal for ADN timeBC and named time period in timeBC; template TIMEBC-->
<!--use dct:temporal for ADN timeRelative; see template TIMERELATIVE-->

<!--process ADN timeAD tags first-->
<!--dct:temporal xsi:type=dct:Period-->
		<xsl:apply-templates select="d:temporalCoverages/d:timeAndPeriod/d:timeInfo/d:timeAD" mode="dct:Period"/>

		<!--see template TIMEAD mode DCT:PERIOD-->

<!--dct:temporal xsi:type=dct:W3CDTF-->
		<xsl:apply-templates select="d:temporalCoverages/d:timeAndPeriod/d:timeInfo/d:timeAD" mode="dct:W3CDTF"/>
		<!--see template TIMEAD mode DCT:W3CDTF-->
		
<!--dct:temporal - plain-->
		<xsl:apply-templates select="d:temporalCoverages/d:timeAndPeriod/d:timeInfo/d:timeAD" mode="plain"/>
		<!--see template TIMEAD mode PLAIN-->

<!--end processing of ADN timeAD-->

<!--process ADN timeBC-->
<!--dct:temporal-->
		<xsl:apply-templates select="d:temporalCoverages/d:timeAndPeriod/d:timeInfo/d:timeBC"/>
		<!--see template TIMEBC-->

<!--process ADN timeRelative-->		
<!--dct:temporal-->
		<xsl:apply-templates select="d:temporalCoverages/d:timeAndPeriod/d:timeInfo/d:timeRelative"/>
		<!--see template TIMERELATIVE-->

		
<!--dct relation information-->
<!--ADN DLESE:Has thumbnail is not transformed-->

		<xsl:apply-templates select="d:relations/d:relation//@kind"/>
		<!--see template KIND-->

<!--dct:conformsTo - using ADN standards information-->
		<xsl:apply-templates select="d:educational/d:contentStandards/d:contentStandard"/>
		<!--see template CONTENTSTANDARD-->


<!--dct:audience-->
		<xsl:apply-templates select="d:educational/d:audiences/d:audience/d:beneficiary"/>
		<!--see template BENEFICIARY-->

<!--dct:mediator-->
		<xsl:apply-templates select="d:educational/d:audiences/d:audience/d:toolFor"/>
		<!--see template TOOLFOR-->

<!--dct:educationalLevel-->
		<xsl:apply-templates select="d:educational/d:audiences/d:audience/d:gradeRange"/>
		<!--see template GRADERANGE-->
		
<!--ieee:interactivityType-->
		<xsl:if test="string-length(d:educational/d:interactivityType) >0">
			<xsl:element name="ieee:interactivityType">

				<xsl:value-of select="substring-after(d:educational/d:interactivityType, 'LOM:')"/>
			</xsl:element>
		</xsl:if>
<!--ieee:interactivityLevel-->
		<xsl:if test="string-length(d:educational/d:interactivityLevel) >0">
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
		<xsl:value-of select="$newline"/>
		<xsl:text disable-output-escaping="yes">&lt;/nsdl_dc:nsdl_dc&gt;</xsl:text>

	</xsl:template>


<!--begin TEMPLATES TO APPLY. In alphabetical order by ADN field name-->
<!--**********************************************************************************************-->
<!--**********************************************************************************************-->
<!--**********************************************************************************************-->
<!--**********************************************************************************************-->
<!--**********************************************************************************************-->
<!--**********************************************************************************************-->

<!--1.   BENEFICARY writes dct:audience-->
<!--2.   CONTENTSTANDARD writes dct:conformsTo-->
<!--3.   GRADERANGE writes dct:educationLevel-->
<!--4.   KIND writes dct:isVersionOf, dct:hasVersion, etc.-->
<!--5.   MEDIUM writes the tag content for templates MEDIUM mode=ADN, MEDIUM mode=plain, MEDIUM mode=DCT:IMT-->

<!--6.   MEDIUM mode=ADN writes dc:format if the medium tag exists in the ADN record-->
<!--7.   MEDIUM mode=DCT:IMT writes dc:format xsi:type="dct:IMT"-->
<!--8.   MEDIUM mode=plain writes dc:format using the NSDL controlled vocab list. (need to verify this 2004-08-31)-->
<!--9.   ONLINE mode=DCT:IMT writes dc:format xsi:type="dct:IMT" based on the ADN primary URL-->
<!--10. ONLINE mode=plain writes dc:format based on the ADN primary URL-->
<!--11. ORGANIZATION mode=contributor writes dc:contributor-->
<!--12. ORGANIZATION mode=creator writes dc:creator-->
<!--13. ORGANIZATION mode=publisher writes dc:publisher-->
<!--14. PERSON mode=contributor writes dc:contributor-->
<!--15. PERSON mode=creator writes dc:creator-->
<!--16. PERSON mode=pubisher writes dc:publisher-->
<!--17. PLACE writes dc:coverage-->
<!--18. PRIMARYURL determines the mime type of the ADN primary URL but does not write a DC element--> 
<!--19. RELATIONS writes the xsi:type="dct:URI" attribute and tag content of dct:IsVersionOf, dct:hasVersion etc. from template 4.-->
<!--20. RESOURCETYPE writes dc:type-->
<!--21. SUBJECT mode=DLESE writes dc:subject from ADN subject--> 
<!--22. SUBJECT mode=keywords writes dc:subject from ADN keywords-->

<!--23. SUBJECT mode=nsdl_dc:GEM writes dc:subject xsi:type="nsdl_dc:GEM"-->
<!--24. TIMEAD mode=dct:period writes dct:temporal xsi:type="dct:Period"-->
<!--25. TIMEAD mode=dct:W3CDTF writes dct:temporal xsi:type="dct:W3CDTF"-->
<!--26. TIMEAD mode=plain writes dc:temporal-->
<!--27. TIMEBC writes dct:temporal-->
<!--28. TIMERELATIVE writes dct:temporal-->
<!--29. TOOLFOR writes dct:mediator-->

<!--1. BENEFICIARY template-->
	<xsl:template match="d:beneficiary">
		<xsl:element name="dct:audience">
			<xsl:value-of select="substring-after(., ':')"/>
		</xsl:element>

	</xsl:template>

<!--2. CONTENTSTANDARD template-->
	<xsl:template match="d:contentStandard">
		<xsl:element name="dct:conformsTo">
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

<!--3. GRADERANGE template-->
	<xsl:template match="d:gradeRange">
		<xsl:choose>
			<xsl:when test="contains(., 'supplied') or contains(., 'Not applicable')"/>
			<xsl:otherwise>
				<xsl:element name="dct:educationLevel">
					<xsl:value-of select="substring-after(., ':')"/>
				</xsl:element>
			</xsl:otherwise>

		</xsl:choose>
	</xsl:template>

<!--4. KIND template-->
	<xsl:template match="d:relations/d:relation//@kind">

		<xsl:choose>
<!--call-template halts processing of current template in order to call and complete anther template; then processing of the current template is resumed; if did not do this would get the unwnted result of all the mediums (mime types) being in a single dc:format tag-->

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
			<xsl:when test="contains(., 'References') or contains(., 'Is based on') or contains(., 'Is associated with')">
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

<!--7. MEDIUM template mode=DCT:IMT-->
	<xsl:template match="d:medium" mode="dct:IMT">
		<!--when using DCT:IMT, the resultant tag can not be blank. So check to make sure there is valid content to work with first. If not valid, the new dc:format dct:IMT tag does not get written-->
		<xsl:if test="contains(., 'application') or contains(., 'message') or contains(., 'video') or contains(., 'audio') or contains(., 'image') or contains(., 'model') or contains(., 'multipart') or contains(., 'text')">
			<xsl:element name="dc:format">
			<!--call-template halts processing of current template in order to call and complete anther template; then processing of the current template is resumed; if did not do this would get the unwnted result of all the mediums (mime types) being in a single dc:format tag-->
				<xsl:attribute name="xsi:type">dct:IMT</xsl:attribute>
				<xsl:call-template name="d:medium"/>

			</xsl:element>
		</xsl:if>
	</xsl:template>

<!--8. MEDIUM template mode=PLAIN-->
	<xsl:template match="d:medium" mode="plain">
<!--this template is intended to use the NSDL mimetype list. So if the data of the mimetype is not on the NSDL list, the resultant tag should not be blank. So check to make sure there is valid content to work with first. If not valid, the new dc:format-plain tag does not get written-->
		<xsl:if test="contains(., 'application') or contains(., 'message') or contains(., 'video') or contains(., 'audio') or contains(., 'image') or contains(., 'model') or contains(., 'multipart') or contains(., 'text')">
			<xsl:element name="dc:format">
			<!--call-template halts processing of current template in order to call and complete anther template; then processing of the current template is resumed; if did not do this would get the unwanted result of all the mediums (mime types) being in a single dc:format tag-->

				<xsl:call-template name="d:medium"/>
			</xsl:element>
		</xsl:if>
	</xsl:template>

<!--9. ONLINE template mode=DCT:IMT-->
	<xsl:template match="d:technical/d:online" mode="dct:IMT">
		<xsl:element name="dc:format">
			<xsl:attribute name="xsi:type">dct:IMT</xsl:attribute>

<!--use apply-templates here rather than call-template; when tried to use call-template, the primaryURL template could not be found. Since there is only 1 primaryURL and you can not use / in call-template, apply-templates works fine then.-->
			<xsl:apply-templates select="d:primaryURL"/>
			<!--see template PRIMARYURL-->
		</xsl:element>
	</xsl:template>

<!--10. ONLINE template mode=PLAIN-->
	<xsl:template match="d:technical/d:online" mode="plain">
		<xsl:element name="dc:format">
<!--use apply-templates here rather than call-template; when tried to use call-template, the primaryURL template could not be found. Since there is only 1 primaryURL and you can not use / in call-template, apply-templates works fine then.-->
			<xsl:apply-templates select="d:primaryURL"/>

			<!--see template PRIMARYURL-->
		</xsl:element>
	</xsl:template>


<!--11. ORGANIZATION template mode=CONTRIBUTOR-->
	<xsl:template match="d:organization" mode="contributor">
		<xsl:if test="../@role='Contributor' or ../@role='Editor'">
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

<!--12. ORGANIZATION template mode=CREATOR-->
	<xsl:template match="d:organization" mode="creator">
		<xsl:if test="../@role='Author' or ../@role='Principal Investigator'">
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

<!--13. ORGANIZATION template mode=PUBLISHER-->
	<xsl:template match="d:organization" mode="publisher">

		<xsl:if test="../@role='Publisher'">
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

<!--14. PERSON template mode=CONTRIBUTOR-->
	<xsl:template match="d:person" mode="contributor">
		<xsl:if test="../@role='Contributor' or ../@role='Editor'">
			<xsl:element name="dc:contributor">
				<xsl:value-of select="concat(d:nameFirst,' ',d:nameLast)"/>

			</xsl:element>
		</xsl:if>
	</xsl:template>			

<!--15. PERSON template mode=CREATOR-->
	<xsl:template match="d:person" mode="creator">
		<xsl:if test="../@role='Author' or ../@role='Principal Investigator'">
			<xsl:element name="dc:creator">
				<xsl:value-of select="concat(d:nameFirst,' ',d:nameLast)"/>
			</xsl:element>

		</xsl:if>
	</xsl:template>			

<!--16. PERSON template mode=PUBLISHER-->
	<xsl:template match="d:person" mode="publisher">
		<xsl:if test="../@role='Publisher'">
			<xsl:element name="dc:publisher">
				<xsl:value-of select="concat(d:nameFirst,' ',d:nameLast)"/>
			</xsl:element>
		</xsl:if>

	</xsl:template>	

<!--17. PLACE template-->
	<xsl:template match="d:place">
		<xsl:element name="dc:coverage">
			<xsl:value-of select="d:name"/>
		</xsl:element>
	</xsl:template>
	
<!--18. PRIMARYURL template -->
<!--makes an assumption of text/html if no match is found-->
	<xsl:template match="d:primaryURL">

		<xsl:choose>
			<xsl:when test="contains(., '.pdf')">
				<xsl:text>application/pdf</xsl:text>
			</xsl:when>											
			<xsl:when test="contains(., '.mov')">
				<xsl:text>video/quicktime</xsl:text>
			</xsl:when>											
			<xsl:when test="contains(., '.mpg')">
				<xsl:text>video/quicktime</xsl:text>

			</xsl:when>											
			<xsl:when test="contains(., '.jpg')">
				<xsl:text>image/jpeg</xsl:text>
			</xsl:when>											
			<xsl:when test="contains(., '.jpeg')">
				<xsl:text>image/jpeg</xsl:text>
			</xsl:when>											
			<xsl:when test="contains(., '.JPG')">
				<xsl:text>image/jpeg</xsl:text>

			</xsl:when>											
			<xsl:when test="contains(., '.gif')">
				<xsl:text>image/gif</xsl:text>
			</xsl:when>											
			<xsl:when test="contains(., '.txt')">
				<xsl:text>text/plain</xsl:text>
			</xsl:when>											
			<xsl:otherwise>
				<xsl:text>text/html</xsl:text>

			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>			

<!--19. RELATIONS template-->
	<xsl:template name="relations">
		<xsl:attribute name="xsi:type">
			<xsl:text>dct:URI</xsl:text>
		</xsl:attribute>
		<xsl:value-of select="../@url"/>

		<xsl:value-of select="../@entry"/>
	</xsl:template>	

<!--20. RESOURCETYPE template-->
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

<!--21. SUBJECT template mode=DLESE-->
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

<!--22. SUBJECT template mode=KEYWORDS-->
	<xsl:template match="d:keyword" mode="keywords">
		<xsl:element name="dc:subject">
			<xsl:value-of select="."/>

		</xsl:element>
	</xsl:template>			

<!--23. SUBJECT template mode=NSDL_DC:GEM-->
	<xsl:template match="d:subject" mode="nsdl_dc:GEM">
		<xsl:if test="contains(., 'Agriculture') or 
						contains(., 'Biology') or
						contains(., 'Ecology') or
						contains(., 'Educational') or
						contains(., 'History') or
						contains(., 'Mathematics') or
						contains(., 'Paleontology') or
						contains(., 'Physics') or
						contains(., 'Space') or
						contains(., 'Technology')">
			<xsl:element name="dc:subject">
				<xsl:attribute name="xsi:type">nsdl_dc:GEM</xsl:attribute>
				<xsl:choose>

<!--NOTE: not mapped: Forestry, Policy issues, soil science-->
<!--still test for Agriculture, Biology, Mathematics, Paleontology, Physics,  Space and Technology even though they are already be present from the DLESE subject list, because now they will be associated with the GEM type-->
<!--Agriculture-->
					<xsl:when test="contains(., 'Agriculture')">
						<xsl:text>Agriculture</xsl:text>
					</xsl:when>
<!--Biology-->
					<xsl:when test="contains(., 'Biology')">
						<xsl:text>Biology</xsl:text>
					</xsl:when>

<!--Ecology-->
					<xsl:when test="contains(., 'Ecology')">
						<xsl:text>Ecology</xsl:text>
					</xsl:when>
<!--Education-->
					<xsl:when test="contains(., 'Educational')">
						<xsl:text>Education (General)</xsl:text>
					</xsl:when>
<!--History of science-->

					<xsl:when test="contains(., 'History')">
						<xsl:text>History of science</xsl:text>
					</xsl:when>
<!--Mathematics-->
					<xsl:when test="contains(., 'Mathematics')">
						<xsl:text>Mathematics</xsl:text>
					</xsl:when>
<!--Natural history-->
					<xsl:when test="contains(., 'Paleontology')">

						<xsl:text>Natural history</xsl:text>
					</xsl:when>
<!--Paleontology-->
					<xsl:when test="contains(., 'Paleontology')">
						<xsl:text>Paleontology</xsl:text>
					</xsl:when>
<!--Physics-->
					<xsl:when test="contains(., 'Physics')">
						<xsl:text>Physics</xsl:text>

					</xsl:when>
<!--Astronomy-->
					<xsl:when test="contains(., 'Space')">
						<xsl:text>Astronomy</xsl:text>
					</xsl:when>
<!--Space-->
					<xsl:when test="contains(., 'Space')">
						<xsl:text>Space sciences</xsl:text>
					</xsl:when>

<!--Technology-->
					<xsl:when test="contains(., 'Technology')">
						<xsl:text>Technology</xsl:text>
					</xsl:when>
				</xsl:choose>
			</xsl:element>
			<xsl:value-of select="$newline"/>
		</xsl:if>
	</xsl:template>	 		


<!--24. TIMEAD template mode DCT:PERIOD-->
	<xsl:template match="d:timeAD" mode="dct:Period">
<!--has the term 'Present' been used in the ADN fields of begin and end date; if so need to write the value without a W3CDTF scheme because 'Present' is not W3CDTF compliant; don't transform or write the 'Present' value-->
		<xsl:choose>
			<xsl:when test="d:begin/@date='Present' or d:end/@date='Present'"/>
			<xsl:otherwise>
				<xsl:element name="dct:temporal">
					<xsl:attribute name="xsi:type">dct:Period</xsl:attribute>
		<!--determine if there is a named time period in order to make the tag content correct-->

					<xsl:choose>
						<xsl:when test="string-length(../../d:periods)>0">
							<xsl:value-of select="concat('start=', d:begin/@date, ';end=', d:end/@date, ';scheme=W3CDTF;name=', ../../d:periods/d:period/d:name)"/>
						</xsl:when>
						<xsl:otherwise>
							<xsl:value-of select="concat('start=', d:begin/@date, ';end=', d:end/@date, ';scheme=W3CDTF')"/>
						</xsl:otherwise>
					</xsl:choose>	
				</xsl:element>

			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

<!--25. TIMEAD template mode DCT:W3CDTF-->
	<xsl:template match="d:timeAD" mode="dct:W3CDTF">
<!--has the term 'Present' been used in the ADN fields of begin and end date; if so need to write the value without a W3CDTF  scheme because 'Present' is not W3CDTF compliant; don't transform or write the 'Present' value-->
		<xsl:choose>
			<xsl:when test="d:begin/@date='Present' or d:end/@date='Present'"/>
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

<!--26. TIMEAD template mode PLAIN-->
	<xsl:template match="d:timeAD" mode="plain">
		<xsl:element name="dct:temporal">
<!--determine if there is a named time period in order to make the tag content correct-->
<!--when ADN named time periods are transformed only the first named time period is transformed, all others are ignored; nsdl_dc does not allow for more than one named time period-->
			<xsl:choose>
				<xsl:when test="string-length(../../d:periods)>0">

<!--has the term 'Present' been used in the ADN fields of begin and end date; if so need to write the value without a W3CDTF  scheme because 'Present' is not W3CDTF compliant-->
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
<!--has the term 'Present' been used in the ADN fields of begin and end date; if so need to write the value without a W3CDTF  scheme because 'Present' is not W3CDTF compliant-->
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
	</xsl:template>

<!--27. TIMEBC template-->
	<xsl:template match="d:timeBC">
		<xsl:element name="dct:temporal">

<!--determine if there is a named time period in order to make the tag content correct-->
<!--when ADN named time periods are transformed only the first named time period is transformed, all others are ignored; nsdl_dc does not allow for more than one named time period-->
			<xsl:choose>
				<xsl:when test="string-length(../../d:periods)>0">
					<xsl:value-of select="concat('start=', d:begin, ' BC;end=', d:end, ' BC;name=', ../../d:periods/d:period/d:name)"/>
				</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="concat('start=', d:begin, ' BC;end=', d:end, ' BC')"/>
				</xsl:otherwise>
			</xsl:choose>	
		</xsl:element>

	</xsl:template>

<!--28. TIMERELATIVE template-->
	<xsl:template match="d:timeRelative">
		<xsl:element name="dct:temporal">
<!--determine if there is a named time period in order to make the tag content correct-->
<!--when ADN named time periods are transformed only the first named time period is transformed, all others are ignored; nsdl_dc does not allow for more than one named time period-->
			<xsl:choose>
				<xsl:when test="string-length(../../d:periods)>0">
					<xsl:value-of select="concat('start=', d:begin, ' ', d:begin/@units, ';end=', d:end, ' ', d:end/@units, ';name=', ../../d:periods/d:period/d:name)"/>
				</xsl:when>

				<xsl:otherwise>
					<xsl:value-of select="concat('start=', d:begin, ' ', d:begin/@units, ';end=', d:end, ' ', d:end/@units)"/>
				</xsl:otherwise>
			</xsl:choose>	
		</xsl:element>
	</xsl:template>

<!--29. TOOLFOR template-->
	<xsl:template match="d:toolFor">
		<xsl:element name="dct:mediator">

			<xsl:value-of select="substring-after(., ':')"/>
		</xsl:element>
	</xsl:template>

</xsl:stylesheet>

