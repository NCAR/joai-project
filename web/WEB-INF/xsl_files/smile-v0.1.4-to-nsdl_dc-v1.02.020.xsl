<?xml version="1.0"?>
<xsl:stylesheet 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xsi ="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:sm="http://smile.howtosmile.org"
    xmlns:dct="http://purl.org/dc/terms/"
    xmlns:nsdl_dc="http://ns.nsdl.org/nsdl_dc_v1.02/"
	xmlns:dc="http://purl.org/dc/elements/1.1/"
	xmlns:ieee="http://www.ieee.org/xsd/LOMv1p0"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
	exclude-result-prefixes="xsi sm xs"
    version="1.1">

<!--PURPOSE: to transform SMILE version 0.1.4 records into the nsdl_dc version 1.02.020 metadata records-->
<!--CREATION: 2010-03-17 by Katy Ginger, University Corporation for Atmospheric Research (UCAR)-->
<!--HISTORY: none-->

	<xsl:output method="xml" indent="yes" encoding="UTF-8"/>

<!--VARIABLES used throughout the transform-->
<!-- ************************************************** -->
	<xsl:variable name="asnIdsURL">http://ns.nsdl.org/ncs/xml/NCS-to-ASN-NSES-mappings.xml</xsl:variable>
	<xsl:variable name="asnText">http://purl.org/ASN/</xsl:variable>

<!--TRANSFORMATION CODE-->
<!-- ************************************************** -->
	<xsl:template match="*|/">
		<xsl:apply-templates select="sm:smileItem"/>
	</xsl:template>

<!--TRANSFORMATION CODE for SMILE to NSDL_DC-->
<!-- ********************************************************-->
	<xsl:template match="sm:smileItem">
		<nsdl_dc:nsdl_dc schemaVersion="1.02.020" xmlns:nsdl_dc="http://ns.nsdl.org/nsdl_dc_v1.02/" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dct="http://purl.org/dc/terms/" xmlns:ieee="http://www.ieee.org/xsd/LOMv1p0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://ns.nsdl.org/nsdl_dc_v1.02/ http://ns.nsdl.org/schemas/nsdl_dc/nsdl_dc_v1.02.xsd">


<!--apply templates to everything... so that if the field repeats, all repeating elements will be transformed and this still works on fields that don't repeat-->

<!--dc:identifier-->
		<xsl:apply-templates select="sm:activityBasics/sm:url" mode="process">
			<xsl:with-param name="tag">dc:identifier</xsl:with-param>
			<xsl:with-param name="att">dct:URI</xsl:with-param>
		</xsl:apply-templates>

<!--dc:title-->
		<xsl:apply-templates select="sm:activityBasics/sm:title" mode="process">
			<xsl:with-param name="tag">dc:title</xsl:with-param>
		</xsl:apply-templates>

<!--dct:alternative using subtitle-->
		<xsl:apply-templates select="sm:activityBasics/sm:subtitle" mode="process">
			<xsl:with-param name="tag">dct:alternative</xsl:with-param>
		</xsl:apply-templates>

<!--dct:abstract: does not have-->

<!--dc:description-->
		<xsl:apply-templates select="sm:activityBasics/sm:description" mode="process">
			<xsl:with-param name="tag">dc:description</xsl:with-param>
		</xsl:apply-templates>

<!--dc:subject using keyword-->
		<xsl:apply-templates select="sm:activityBasics/sm:keywords/sm:keyword" mode="process">
			<xsl:with-param name="tag">dc:subject</xsl:with-param>
		</xsl:apply-templates>

<!--dc:subject using informalCategory-->
		<xsl:apply-templates select="sm:activityBasics/sm:informalCategories/sm:informalCategory" mode="process">
			<xsl:with-param name="tag">dc:subject</xsl:with-param>
		</xsl:apply-templates>

<!--dc:subject using subject-->
<!--since SMILE subjects have a lead-in SMILE: label, strip this off first before making the template call - does this work?-->
		<xsl:apply-templates select="sm:activityBasics/sm:subjects/sm:subject" mode="subject">
			<xsl:with-param name="tag">dc:subject</xsl:with-param>
		</xsl:apply-templates>



<!--dc:subject - there may be more -->

<!--dct:abstract: does not have-->
<!--dct:tableOfContents: does not have-->
<!--dct:bibliographicCitation: does not have-->

<!--dc:language using resourceLanguage-->
<!--The correct way to express language now is to use the 2-letter ISO639-1 encoding scheme . But NSDL_DC has not been updated to use ISO639-1; it still uses the 3-letter ISO639-2 encoding scheme-->
<!--RFC 3066 was replaced by RFC 4646 (and RFC 4647) in September 2006 so don't use RFC language codes anymore-->
<!--So supply language using the ISO639-2 codes and indicate that these codes are being used.-->
<!--then also process language with the full SMILE language text-->
		<xsl:apply-templates select="sm:activityBasics/sm:resourceLanguages/sm:resourceLanguage" mode="language">
			<xsl:with-param name="tag">dc:language</xsl:with-param>
			<xsl:with-param name="att">dct:ISO639-2</xsl:with-param>
		</xsl:apply-templates>

		<xsl:apply-templates select="sm:activityBasics/sm:resourceLanguages/sm:resourceLanguage" mode="process">
			<xsl:with-param name="tag">dc:language</xsl:with-param>
		</xsl:apply-templates>

<!--dct:educationLevel using ageRange-->
<!--When using the NSDL_DC controlled vocabulary, the best practice is to also select the higher level term. The template that is called will do this-->
<!--dct:audience using ageRange-->
		<xsl:apply-templates select="sm:activityBasics/sm:ageRanges/sm:ageRange" mode="eduLevel">
			<xsl:with-param name="tag">dct:educationLevel</xsl:with-param>
			<xsl:with-param name="tag2">dct:audience</xsl:with-param>
			<xsl:with-param name="att">nsdl_dc:NSDLEdLevel</xsl:with-param>
		</xsl:apply-templates>

<!--dct:audience using targetedStudentPopulations-->
		<xsl:apply-templates select="sm:diversity/sm:targetedStudentPopulations" mode="process">
			<xsl:with-param name="tag">dct:audience</xsl:with-param>
		</xsl:apply-templates>

<!--dct:audience using the default NSDL_DC term of Learner-->
<!--Indicate that the NSDL_DC vocab is being used-->
	<dct:audience xsi:type="nsdl_dc:NSDLAudience">Learner</dct:audience>

<!--dct:mediator using an assigned default of Educator-->
	<dct:mediator>Educator</dct:mediator>

<!--dc:audience using cultureEthnicityGenderSubtopic-->
		<xsl:apply-templates select="sm:diversity/sm:cultureEthnicityGender/sm:cultureEthnicityGenderSubtopic" mode="process">
			<xsl:with-param name="tag">dct:audience</xsl:with-param>
		</xsl:apply-templates>

<!--dc:type using resourceType-->
<!--When using the NSDL_DC controlled vocabulary, the best practice is to also select the higher level term. The template that is called will do this because the SMILE data does not include these higher order terms.-->
		<xsl:apply-templates select="sm:activityBasics/sm:resourceTypes/sm:resourceType" mode="type"> 
			<xsl:with-param name="tag">dc:type</xsl:with-param>
			<xsl:with-param name="att">nsdl_dc:NSDLType</xsl:with-param>
		</xsl:apply-templates>

<!--ieee:interactivityLevel: does not have-->
<!--ieee:interactivityType: does not have-->

<!--ieee:typicalLearningTime: using learningTime-->
		<xsl:apply-templates select="sm:costTimeMaterials/sm:learningTime" mode="process"> 
			<xsl:with-param name="tag">ieee:typicalLearningTime</xsl:with-param>
		</xsl:apply-templates>

<!--dct:accessibility-->
		<xsl:apply-templates select="sm:diversity/sm:accessibility/sm:learnerEssentialAccessModes" mode="process"> 
			<xsl:with-param name="tag">dct:accessibility</xsl:with-param>
			<xsl:with-param name="plus">The learner must be able to: </xsl:with-param>
		</xsl:apply-templates>

<!--dct:instructionalMethod-->
		<xsl:apply-templates select="sm:diversity/sm:learningStyles" mode="process"> 
			<xsl:with-param name="tag">dct:instructionalMethod</xsl:with-param>
		</xsl:apply-templates>

<!--dct:conformsTo using strandMapSmsId-->
		<xsl:apply-templates select="sm:activityBasics/sm:aaasStrandMaps/sm:strandMapSmsId" mode="process"> 
			<xsl:with-param name="tag">dct:conformsTo</xsl:with-param>
		</xsl:apply-templates>

<!--dct:conformsTo using strandBenchmarkSmsId-->
		<xsl:apply-templates select="sm:activityBasics/sm:aaasBenchmarks/sm:BenchmarkSmsId" mode="process"> 
			<xsl:with-param name="tag">dct:conformsTo</xsl:with-param>
		</xsl:apply-templates>

<!--dc:creator using resourceAuthor.person-->
		<xsl:apply-templates select="sm:authorshipRights/sm:resourceAuthors/sm:resourceAuthor/sm:person" mode="process"> 
			<xsl:with-param name="tag">dc:creator</xsl:with-param>
		</xsl:apply-templates>

<!--dc:publisher using resourceAuthor.organization-->
		<xsl:apply-templates select="sm:authorshipRights/sm:resourceAuthors/sm:resourceAuthor/sm:organization/sm:organizationName" mode="process"> 
			<xsl:with-param name="tag">dc:publisher</xsl:with-param>
		</xsl:apply-templates>

<!--dc:creator using resourceAuthor.sourceInstitution-->
		<xsl:apply-templates select="sm:authorshipRights/sm:sourceInstitutions/sm:sourceInstitution " mode="process"> 
			<xsl:with-param name="tag">dc:creator</xsl:with-param>
		</xsl:apply-templates>

<!--dc:contributor using resourceAuthor.sourceCollection-->
		<xsl:apply-templates select="sm:authorshipRights/sm:sourceCollections/sm:sourceCollection " mode="process"> 
			<xsl:with-param name="tag">dc:contributor</xsl:with-param>
		</xsl:apply-templates>

<!--dc:rights using copyrightYear-->
		<xsl:apply-templates select="sm:authorshipRights/sm:copyright/sm:copyrightYear" mode="process"> 
			<xsl:with-param name="tag">dc:rights</xsl:with-param>
			<xsl:with-param name="plus">Copyright </xsl:with-param>
		</xsl:apply-templates>

<!--dc:rights using copyrightHolder-->
		<xsl:apply-templates select="sm:authorshipRights/sm:copyright/sm:copyrightHolder" mode="process"> 
			<xsl:with-param name="tag">dc:rights</xsl:with-param>
			<xsl:with-param name="plus">Copyright </xsl:with-param>
		</xsl:apply-templates>

<!--dct:rightsHolder using copyrightHolder-->
		<xsl:apply-templates select="sm:authorshipRights/sm:copyright/sm:copyrightHolder" mode="process"> 
			<xsl:with-param name="tag">dct:rightsHolder</xsl:with-param>
		</xsl:apply-templates>

<!--dct:license URL using licenseUrl-->
		<xsl:apply-templates select="sm:authorshipRights/sm:copyright/sm:licenseUrl" mode="process"> 
			<xsl:with-param name="tag">dct:license</xsl:with-param>
			<xsl:with-param name="att">dct:URI</xsl:with-param>
		</xsl:apply-templates>

<!--dc:rights using license.allRightsReserved-->
		<xsl:apply-templates select="sm:authorshipRights/sm:copyright/sm:license/sm:allRightsReserved" mode="process"> 
			<xsl:with-param name="tag">dc:rights</xsl:with-param>
		</xsl:apply-templates>

<!--dct:license using license.publicDomain-->
		<xsl:apply-templates select="sm:authorshipRights/sm:copyright/sm:license/sm:publicDomain" mode="process"> 
			<xsl:with-param name="tag">dct:license</xsl:with-param>
		</xsl:apply-templates>

<!--dct:license using license.creativeCommons-->
		<xsl:apply-templates select="sm:authorshipRights/sm:copyright/sm:license/sm:creativeCommons" mode="process"> 
			<xsl:with-param name="tag">dct:license</xsl:with-param>
			<xsl:with-param name="plus">Creative Commons: </xsl:with-param>
		</xsl:apply-templates>

<!--dct:license using license.openSource-->
		<xsl:apply-templates select="sm:authorshipRights/sm:copyright/sm:license/sm:openSource" mode="process"> 
			<xsl:with-param name="tag">dct:license</xsl:with-param>
			<xsl:with-param name="plus">Open Source: </xsl:with-param>
		</xsl:apply-templates>

<!--dct:license URL using license.creativeCommons.url-->
<!--XSLT 1.0 can't apply a template to a XML attribute unless you are comparing the attribute content to something like: msp:lifecycle/msp:contributor[not(@role='Author' or @role='Publisher')]  or if you use a wildcard like sm:authorshipRights/sm:copyright/sm:license/sm:creativeCommons/@*.-->
<!--that is the following won't work:sm:authorshipRights/sm:copyright/sm:license/sm:openSource/@url-->
<!--so this uses the wildcard implementation-->
		<xsl:apply-templates select="sm:authorshipRights/sm:copyright/sm:license/sm:creativeCommons/@*" mode="process"> 
			<xsl:with-param name="tag">dct:license</xsl:with-param>
			<xsl:with-param name="att">dct:URI</xsl:with-param>
		</xsl:apply-templates>

<!--dct:license URL using license.openSource.url-->
<!--XSLT 1.0 can't apply a template to a XML attribute unless you are comparing the attribute content to something like: msp:lifecycle/msp:contributor[not(@role='Author' or @role='Publisher')]  or if you use a wildcard like sm:authorshipRights/sm:copyright/sm:license/sm:creativeCommons/@*.-->
<!--that is the following won't work:sm:authorshipRights/sm:copyright/sm:license/sm:openSource/@url-->
<!--so this uses the wildcard implementation-->
		<xsl:apply-templates select="sm:authorshipRights/sm:copyright/sm:license/sm:openSource/@*" mode="process"> 
			<xsl:with-param name="tag">dct:license</xsl:with-param>
			<xsl:with-param name="att">dct:URI</xsl:with-param>
		</xsl:apply-templates>

<!--dct:accessRights using accessRights-->
<!--SMILE uses the NSDL _DC1.02.020 vocab exactly; no further tweaking required-->
		<xsl:apply-templates select="sm:authorshipRights/sm:accessRights" mode="process"> 
			<xsl:with-param name="tag">dct:accessRights</xsl:with-param>
			<xsl:with-param name="att">nsdl_dc:NSDLAccess</xsl:with-param>
		</xsl:apply-templates>

<!--dct:provenance: does not have-->
<!--dct:accrualMethod: does not have-->
<!--dct:accrualPeriodicity: does not have-->
<!--dct:accrualPolicy: does not have-->
<!--dc:source: does not have-->

<!--dc:relation-->
		<xsl:apply-templates select="sm:activityBasics/sm:relatedUrls/sm:relatedUrl/sm:url[@kind='Foreign Language Version' ] " mode="process"> 
			<xsl:with-param name="tag">dct:hasVersion</xsl:with-param>
			<xsl:with-param name="att">dct:URI</xsl:with-param>
		</xsl:apply-templates>

<!--dct:isPartOf-->
		<xsl:apply-templates select="sm:activityBasics/sm:relatedUrls/sm:relatedUrl/sm:url[@kind='Is part of' ] " mode="process"> 
			<xsl:with-param name="tag">dct:isPartOf</xsl:with-param>
			<xsl:with-param name="att">dct:URI</xsl:with-param>
		</xsl:apply-templates>

<!--dct:hasPart-->
		<xsl:apply-templates select="sm:activityBasics/sm:relatedUrls/sm:relatedUrl/sm:url[@kind='Has part' ] " mode="process"> 
			<xsl:with-param name="tag">dct:hasPart</xsl:with-param>
			<xsl:with-param name="att">dct:URI</xsl:with-param>
		</xsl:apply-templates>

<!--dc:relation: does not have-->
<!--dct:isReferencedBy: does not have-->
<!--dct:references: does not have-->
<!--dct:isFormatOf: does not have-->
<!--dct:hasFormat: does not have-->
<!--dct:isReplacedBy: does not have-->
<!--dct:replaces: does not have-->
<!--dct:isRequiredBy: does not havel-->
<!--dct:requires: does not have-->
<!--dct:isVersionOf: does not have-->

<!--dct:extent: does not have-->
<!--dct:medium: does not have-->

<!--dc:format using url.fileType-->
		<xsl:apply-templates select="sm:activityBasics/sm:url" mode="format"> 
			<xsl:with-param name="tag">dc:format</xsl:with-param>
			<xsl:with-param name="att">dct:IMT</xsl:with-param>
		</xsl:apply-templates>

<!--dc:date: does not have a specific field for the date of resource creation; rather has copyright year-->
<!--so do not complete dc:date rather complete dct:dateCopyrighted and indicate a date format is being used-->
		<xsl:apply-templates select="sm:authorshipRights/sm:copyright/sm:copyrightYear" mode="process"> 
			<xsl:with-param name="tag">dct:dateCopyrighted</xsl:with-param>
			<xsl:with-param name="att">dct:W3CDTF</xsl:with-param>
		</xsl:apply-templates>

<!--dc:date: does not have - see notes above-->
<!--dct:created: does not have-->
<!--dct:available: does not have-->
<!--dct:issued: does not have-->
<!--dct:modified: does not have-->
<!--dct:valid: does not have-->
<!--dct:dateAccepted: does not have-->
<!--dct:dateSubmitted: does not have-->

<!--dct:temporal using time.timeDescription-->
		<xsl:apply-templates select="sm:placeAndTime/sm:time/sm:timeDescription" mode="process"> 
			<xsl:with-param name="tag">dct:temporal</xsl:with-param>
		</xsl:apply-templates>

<!--dct:temporal using time.season-->
		<xsl:apply-templates select="sm:placeAndTime/sm:time/sm:season" mode="process"> 
			<xsl:with-param name="tag">dct:temporal</xsl:with-param>
		</xsl:apply-templates>

<!--dct:temporal using time.date when the year is present-->
<!--template enforces W3C compliancy; so date information is only written if a year is present because when only a month and day are present this is not W3C compliant-->
		<xsl:apply-templates select="sm:placeAndTime/sm:time/sm:date" mode="date"> 
			<xsl:with-param name="tag">dct:temporal</xsl:with-param>
			<xsl:with-param name="att">dct:W3CDTF</xsl:with-param>
		</xsl:apply-templates>

<!--dct:temporal using time.date when the year is NOT present-->
		<xsl:apply-templates select="sm:placeAndTime/sm:time/sm:date" mode="date"> 
			<xsl:with-param name="tag">dct:temporal</xsl:with-param>
		</xsl:apply-templates>

<!--dc:coverage using placeName only-->
		<xsl:apply-templates select="sm:placeAndTime/sm:place/sm:placeName" mode="process"> 
			<xsl:with-param name="tag">dc:coverage</xsl:with-param>
		</xsl:apply-templates>

<!--dc:coverage using place.placeName and place.geographicalCoordinates-->
		<xsl:apply-templates select="sm:placeAndTime/sm:place" mode="coverage"> 
			<xsl:with-param name="tag">dc:coverage</xsl:with-param>
		</xsl:apply-templates>

<!--dc:identifier using isbn-->
<!--because ISBN attribute is being used, XML Spy and XERES validate differently-->
<!--In XML Spy, the following is valid XML content: 'The ISBN number is: 978-1565845411' while in XERES this gives an error.-->
<!--So just assume the value is an ISBN number and do no further data checks-->
<!--Content then looks like 978-1565845411 or ISBN-978-1565845411 and this seems to be valid for both validators-->
		<xsl:apply-templates select="sm:authorshipRights/sm:printedMaterials/sm:printedMaterial/sm:printedMaterialBook/sm:isbn" mode="process"> 
			<xsl:with-param name="tag">dc:identifier</xsl:with-param>
			<xsl:with-param name="att">dct:ISBN</xsl:with-param>
<!--			<xsl:with-param name="plus">The ISBN number is: </xsl:with-param>-->			
		</xsl:apply-templates>

		</nsdl_dc:nsdl_dc><!--end nsdl_dc:nsdl_dc element-->
	</xsl:template>



<!--TEMPLATES for SMILE to nsdl_dc-->
<!-- ****************************************-->
<!--PROCESS:writes all tag sets that are not a content standard, box or point-->
<!--BOX or POINT: writes the coverages.box and coverages.point tag sets-->
<!--NSESCONTENTSTANDARD: writes the educational.standards.NSESstandard and sometimes the educational.standards.asnID tag sets-->


<!--PROCESS template-->
	<xsl:template match="node() | @*" name="process" mode="process">
<!--note: in templates, params must be declared before variables-->
<!--note: for the with-param tag to work above, a param tag must exist and be present here in the template being called-->
		<xsl:param name="tag"/>
		<xsl:param name="att"/>
		<xsl:param name="plus"/>
		<xsl:param name="string" select="."/>
		<xsl:param name="culture">
			<!--because the attribute may or may not appear, check to see if it is present and if so append a colon and space to it for nice text-->
			<xsl:if test="string-length(../@cultureEthnicityGenderGroupName) > 0">
				<xsl:value-of select="concat(../@cultureEthnicityGenderGroupName, ': ' )"/>
			</xsl:if>
		</xsl:param>
		<xsl:param name="person">
			<!--because the person child element may or may not appear, check to see if it is present and if concat together into nice text-->
			<xsl:choose>
				<xsl:when test="string-length(./sm:institutionName) > 0 and string-length(./sm:nameFirst) > 0 and string-length(./sm:nameLast) > 0">
					<xsl:value-of select="concat(./sm:nameFirst, ' ', ./sm:nameLast, ', ', ./sm:institutionName)"/>
				</xsl:when>
				<xsl:when test="string-length(./sm:institutionName) = 0 and string-length(./sm:nameFirst) > 0 and string-length(./sm:nameLast) > 0">
					<xsl:value-of select="concat(./sm:nameFirst, ' ', ./sm:nameLast)"/>
				</xsl:when>
			</xsl:choose>
		</xsl:param>

		<xsl:if test="string-length($string) > 0">
			<xsl:element name="{$tag}">
				<xsl:if test="string-length($att) > 0">
					<xsl:attribute name="xsi:type">
						<xsl:value-of select="$att"/>
					</xsl:attribute>	
				</xsl:if>
				<xsl:choose>
					<xsl:when test="string-length($culture) = 0 and string-length($person) = 0 ">
						<xsl:value-of select="concat($plus, $string)"/>
					</xsl:when>
					<xsl:when test="string-length($culture) > 0">
						<xsl:value-of select="concat($culture, $string)"/>					
					</xsl:when>
					<xsl:when test="string-length($person) > 0 ">
						<xsl:value-of select="$person"/>
					</xsl:when>
				</xsl:choose>
			</xsl:element>
		</xsl:if>
	</xsl:template>	


<!--SUBJECT template-->
	<xsl:template match="node()" mode="subject">
<!--note: in templates, params must be declared before variables-->
<!--note: for the with-param tag to work above, a param tag must exist and be present here in the template being called-->
<!--SMILE subject terms are a maximum of 4 levels like (SMILE:Physical Sciences:Motion and Forces:Center of Gravity)-->
<!--SMILE subject terms are the only colon separated vocab lists in the metadata framework. So rather than making a template that just pulls part the colon stuff, just brute force it out for quickness-->
<!--strip the SMILE: part off immediately as the parameter arrives in this template; then only have to worry about 3 levels but added a 4th for security-->
		<xsl:param name="tag"/>
		<xsl:param name="string" select="substring-after(., ':' )"/>
		<xsl:choose>
			<xsl:when test="contains($string, ':')"> <!--level 1-->
				<xsl:element name="{$tag}">
					<xsl:value-of select="substring-before($string, ':' )"/>
				</xsl:element>
				<xsl:choose>
					<xsl:when test="contains(substring-after($string, ':'), ':')"> <!--2 levels-->
						<xsl:element name="{$tag}">
							<xsl:value-of select="substring-before(substring-after($string, ':'), ':')"/>
						</xsl:element>
						<xsl:choose>
							<xsl:when test="contains(substring-after(substring-after($string, ':'), ':'), ':')"> <!--3 levels-->
								<xsl:element name="{$tag}">
									<xsl:value-of select="substring-before(substring-after(substring-after($string, ':'), ':'), ':')"/>
								</xsl:element>
								<xsl:element name="{$tag}"><!--4 levels-->
									<xsl:value-of select="substring-after(substring-after(substring-after($string, ':'), ':'), ':')"/>
								</xsl:element>
							</xsl:when>
							<xsl:otherwise> <!--3 levels-->
								<xsl:element name="{$tag}">
									<xsl:value-of select="substring-after(substring-after($string, ':'), ':')"/>
								</xsl:element>
							</xsl:otherwise>
						</xsl:choose>
					</xsl:when>
					<xsl:otherwise> <!--2 levels-->
						<xsl:element name="{$tag}">
							<xsl:value-of select="substring-after($string, ':')"/>
						</xsl:element>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:when>
			<xsl:otherwise>
				<xsl:element name="{$tag}"> <!--1 level-->
					<xsl:value-of select="$string"/>
				</xsl:element>				
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>	

<!--LANGUAGE template using ISO639-2 codes-->
	<xsl:template match="node()" name="language" mode="language">
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
					<xsl:when test="$string = 'Arabic' ">ara</xsl:when>
					<xsl:when test="$string = 'Chinese' ">zho</xsl:when>
					<xsl:when test="$string = 'English' ">eng</xsl:when>
					<xsl:when test="$string = 'French' ">fra</xsl:when>
					<xsl:when test="$string = 'German' ">deu</xsl:when>
					<xsl:when test="$string = 'Italian' ">ita</xsl:when>
					<xsl:when test="$string = 'Portuguese' ">por</xsl:when>
					<xsl:when test="$string = 'Russian' ">rus</xsl:when>
					<xsl:when test="$string = 'Spanish' ">spa</xsl:when>		
				</xsl:choose>
			</xsl:element>
		</xsl:if>
	</xsl:template>	

<!--EDUCATIONLEVEL template-->
	<xsl:template match="node()" name="eduLevel" mode="eduLevel">
<!--note: in templates, params must be declared before variables-->
<!--note: for the with-param tag to work above, a param tag must exist and be present here in the template being called-->
		<xsl:param name="tag"/>
		<xsl:param name="tag2"/>
		<xsl:param name="att"/>
		<xsl:param name="string" select="."/>

		<xsl:if test="string-length($string) > 0">
			<xsl:choose>
<!--processing 4-6 years old (PreK-K) for both dct:educationLevel and dct:audience-->
				<xsl:when test="$string = '4-6 years old (PreK-K)' ">
					<xsl:element name="{$tag}">
						<xsl:attribute name="xsi:type">
							<xsl:value-of select="$att"/>
						</xsl:attribute>
						<xsl:text>Pre-Kindergarten</xsl:text>	
					</xsl:element>
					<xsl:element name="{$tag}">
						<xsl:attribute name="xsi:type">
							<xsl:value-of select="$att"/>
						</xsl:attribute>
						<xsl:text>Elementary School</xsl:text>	
					</xsl:element>
					<xsl:element name="{$tag}">
						<xsl:attribute name="xsi:type">
							<xsl:value-of select="$att"/>
						</xsl:attribute>
						<xsl:text>Early Elementary</xsl:text>	
					</xsl:element>
					<xsl:element name="{$tag}">
						<xsl:attribute name="xsi:type">
							<xsl:value-of select="$att"/>
						</xsl:attribute>
						<xsl:text>Kindergarten</xsl:text>	
					</xsl:element>
					<xsl:element name="{$tag}">
						<xsl:attribute name="xsi:type">
							<xsl:value-of select="$att"/>
						</xsl:attribute>
						<xsl:text>Informal Education</xsl:text>	
					</xsl:element>
					<xsl:element name="{$tag}">
						<xsl:attribute name="xsi:type">
							<xsl:value-of select="$att"/>
						</xsl:attribute>
						<xsl:text>Elementary School Programming</xsl:text>	
					</xsl:element>
					<xsl:element name="{$tag2}">
						<xsl:text>4-6 year olds</xsl:text>	
					</xsl:element>
				</xsl:when>

<!--processing 6-8 years old (grades 1-2) for both dct:educationLevel and dct:audience-->
				<xsl:when test="$string = '6-8 years old (grades 1-2)' ">
					<xsl:element name="{$tag}">
						<xsl:attribute name="xsi:type">
							<xsl:value-of select="$att"/>
						</xsl:attribute>
						<xsl:text>Elementary School</xsl:text>	
					</xsl:element>
					<xsl:element name="{$tag}">
						<xsl:attribute name="xsi:type">
							<xsl:value-of select="$att"/>
						</xsl:attribute>
						<xsl:text>Early Elementary</xsl:text>	
					</xsl:element>
					<xsl:element name="{$tag}">
						<xsl:attribute name="xsi:type">
							<xsl:value-of select="$att"/>
						</xsl:attribute>
						<xsl:text>Grade 1</xsl:text>	
					</xsl:element>
					<xsl:element name="{$tag}">
						<xsl:attribute name="xsi:type">
							<xsl:value-of select="$att"/>
						</xsl:attribute>
						<xsl:text>Grade 2</xsl:text>	
					</xsl:element>
					<xsl:element name="{$tag}">
						<xsl:attribute name="xsi:type">
							<xsl:value-of select="$att"/>
						</xsl:attribute>
						<xsl:text>Informal Education</xsl:text>	
					</xsl:element>
					<xsl:element name="{$tag}">
						<xsl:attribute name="xsi:type">
							<xsl:value-of select="$att"/>
						</xsl:attribute>
						<xsl:text>Elementary School Programming</xsl:text>	
					</xsl:element>
					<xsl:element name="{$tag2}">
						<xsl:text>6-8 year olds</xsl:text>	
					</xsl:element>				
				</xsl:when>

<!--processing 8-11 years old (grades 3-5) for both dct:educationLevel and dct:audience-->
				<xsl:when test="$string = '8-11 years old (grades 3-5)' ">
					<xsl:element name="{$tag}">
						<xsl:attribute name="xsi:type">
							<xsl:value-of select="$att"/>
						</xsl:attribute>
						<xsl:text>Elementary School</xsl:text>	
					</xsl:element>
					<xsl:element name="{$tag}">
						<xsl:attribute name="xsi:type">
							<xsl:value-of select="$att"/>
						</xsl:attribute>
						<xsl:text>Upper Elementary</xsl:text>	
					</xsl:element>
					<xsl:element name="{$tag}">
						<xsl:attribute name="xsi:type">
							<xsl:value-of select="$att"/>
						</xsl:attribute>
						<xsl:text>Grade 3</xsl:text>	
					</xsl:element>
					<xsl:element name="{$tag}">
						<xsl:attribute name="xsi:type">
							<xsl:value-of select="$att"/>
						</xsl:attribute>
						<xsl:text>Grade 4</xsl:text>	
					</xsl:element>
					<xsl:element name="{$tag}">
						<xsl:attribute name="xsi:type">
							<xsl:value-of select="$att"/>
						</xsl:attribute>
						<xsl:text>Grade 5</xsl:text>	
					</xsl:element>
					<xsl:element name="{$tag}">
						<xsl:attribute name="xsi:type">
							<xsl:value-of select="$att"/>
						</xsl:attribute>
						<xsl:text>Informal Education</xsl:text>	
					</xsl:element>
					<xsl:element name="{$tag}">
						<xsl:attribute name="xsi:type">
							<xsl:value-of select="$att"/>
						</xsl:attribute>
						<xsl:text>Elementary School Programming</xsl:text>	
					</xsl:element>
					<xsl:element name="{$tag2}">
						<xsl:text>8-11 year olds</xsl:text>	
					</xsl:element>				
				</xsl:when>
				
<!--processing 11-14 years old (grades 6-8) for both dct:educationLevel and dct:audience-->
				<xsl:when test="$string = '11-14 years old (grades 6-8)' ">
					<xsl:element name="{$tag}">
						<xsl:attribute name="xsi:type">
							<xsl:value-of select="$att"/>
						</xsl:attribute>
						<xsl:text>Middle School</xsl:text>	
					</xsl:element>
					<xsl:element name="{$tag}">
						<xsl:attribute name="xsi:type">
							<xsl:value-of select="$att"/>
						</xsl:attribute>
						<xsl:text>Grade 6</xsl:text>	
					</xsl:element>
					<xsl:element name="{$tag}">
						<xsl:attribute name="xsi:type">
							<xsl:value-of select="$att"/>
						</xsl:attribute>
						<xsl:text>Grade 7</xsl:text>	
					</xsl:element>
					<xsl:element name="{$tag}">
						<xsl:attribute name="xsi:type">
							<xsl:value-of select="$att"/>
						</xsl:attribute>
						<xsl:text>Grade 8</xsl:text>	
					</xsl:element>
					<xsl:element name="{$tag}">
						<xsl:attribute name="xsi:type">
							<xsl:value-of select="$att"/>
						</xsl:attribute>
						<xsl:text>Informal Education</xsl:text>	
					</xsl:element>
					<xsl:element name="{$tag}">
						<xsl:attribute name="xsi:type">
							<xsl:value-of select="$att"/>
						</xsl:attribute>
						<xsl:text>Middle School Programming</xsl:text>	
					</xsl:element>
					<xsl:element name="{$tag2}">
						<xsl:text>11-14 year olds</xsl:text>	
					</xsl:element>				
				</xsl:when>

<!--processing 14-18 years old (grades 9-12) for both dct:educationLevel and dct:audience-->
				<xsl:when test="$string = '14-18 years old (grades 9-12)' ">
					<xsl:element name="{$tag}">
						<xsl:attribute name="xsi:type">
							<xsl:value-of select="$att"/>
						</xsl:attribute>
						<xsl:text>High School</xsl:text>	
					</xsl:element>
					<xsl:element name="{$tag}">
						<xsl:attribute name="xsi:type">
							<xsl:value-of select="$att"/>
						</xsl:attribute>
						<xsl:text>Grade 9</xsl:text>	
					</xsl:element>
					<xsl:element name="{$tag}">
						<xsl:attribute name="xsi:type">
							<xsl:value-of select="$att"/>
						</xsl:attribute>
						<xsl:text>Grade 10</xsl:text>	
					</xsl:element>
					<xsl:element name="{$tag}">
						<xsl:attribute name="xsi:type">
							<xsl:value-of select="$att"/>
						</xsl:attribute>
						<xsl:text>Grade 11</xsl:text>	
					</xsl:element>
					<xsl:element name="{$tag}">
						<xsl:attribute name="xsi:type">
							<xsl:value-of select="$att"/>
						</xsl:attribute>
						<xsl:text>Grade 12</xsl:text>	
					</xsl:element>
					<xsl:element name="{$tag}">
						<xsl:attribute name="xsi:type">
							<xsl:value-of select="$att"/>
						</xsl:attribute>
						<xsl:text>Informal Education</xsl:text>	
					</xsl:element>
					<xsl:element name="{$tag}">
						<xsl:attribute name="xsi:type">
							<xsl:value-of select="$att"/>
						</xsl:attribute>
						<xsl:text>High School Programming</xsl:text>	
					</xsl:element>
					<xsl:element name="{$tag2}">
						<xsl:text>14-18 year olds</xsl:text>	
					</xsl:element>				
				</xsl:when>

<!--processing 18 years and older (adult) for both dct:educationLevel and dct:audience-->
				<xsl:when test="$string = '18 years and older (adult)' ">
					<xsl:element name="{$tag}">
						<xsl:attribute name="xsi:type">
							<xsl:value-of select="$att"/>
						</xsl:attribute>
						<xsl:text>Informal Education</xsl:text>	
					</xsl:element>
					<xsl:element name="{$tag}">
						<xsl:attribute name="xsi:type">
							<xsl:value-of select="$att"/>
						</xsl:attribute>
						<xsl:text>General Public</xsl:text>	
					</xsl:element>
					<xsl:element name="{$tag2}">
						<xsl:text>18 years and older individuals</xsl:text>	
					</xsl:element>				
				</xsl:when>
			</xsl:choose>
		</xsl:if>
	</xsl:template>	
	
	<!--TYPE template-->
	<xsl:template match="node()" name="type" mode="type">
<!--note: in templates, params must be declared before variables-->
<!--note: for the with-param tag to work above, a param tag must exist and be present here in the template being called-->
		<xsl:param name="tag"/>
		<xsl:param name="att"/>
		<xsl:param name="string" select="."/>

		<xsl:if test="string-length($string) > 0">
			<xsl:choose>
<!--processing Activity-->
				<xsl:when test="$string = 'Activity' ">
					<xsl:element name="{$tag}">
						<xsl:attribute name="xsi:type">
							<xsl:value-of select="$att"/>
						</xsl:attribute>
						<xsl:text>Instructional Material</xsl:text>	
					</xsl:element>
					<xsl:element name="{$tag}">
						<xsl:attribute name="xsi:type">
							<xsl:value-of select="$att"/>
						</xsl:attribute>
						<xsl:text>Activity</xsl:text>	
					</xsl:element>
				</xsl:when>

<!--processing Demonstration-->
				<xsl:when test="$string = 'Demonstration' ">
					<xsl:element name="{$tag}">
						<xsl:attribute name="xsi:type">
							<xsl:value-of select="$att"/>
						</xsl:attribute>
						<xsl:text>Instructional Material</xsl:text>	
					</xsl:element>
					<xsl:element name="{$tag}">
						<xsl:attribute name="xsi:type">
							<xsl:value-of select="$att"/>
						</xsl:attribute>
						<xsl:text>Demonstration</xsl:text>	
					</xsl:element>
				</xsl:when>

<!--processing Exhibit-->
				<xsl:when test="$string = 'Exhibit' ">
					<xsl:element name="{$tag}">
						<xsl:attribute name="xsi:type">
							<xsl:value-of select="$att"/>
						</xsl:attribute>
						<xsl:text>Event</xsl:text>	
					</xsl:element>
					<xsl:element name="{$tag}">
						<xsl:attribute name="xsi:type">
							<xsl:value-of select="$att"/>
						</xsl:attribute>
						<xsl:text>Exhibit</xsl:text>	
					</xsl:element>
				</xsl:when>
				
<!--processing Experiment/Lab Activity-->
				<xsl:when test="$string = 'Experiment/Lab Activity' ">
					<xsl:element name="{$tag}">
						<xsl:attribute name="xsi:type">
							<xsl:value-of select="$att"/>
						</xsl:attribute>
						<xsl:text>Instructional Material</xsl:text>	
					</xsl:element>
					<xsl:element name="{$tag}">
						<xsl:attribute name="xsi:type">
							<xsl:value-of select="$att"/>
						</xsl:attribute>
						<xsl:text>Experiment/Lab Activity</xsl:text>	
					</xsl:element>
				</xsl:when>

<!--processing Field Trip-->
				<xsl:when test="$string = 'Field Trip' ">
					<xsl:element name="{$tag}">
						<xsl:attribute name="xsi:type">
							<xsl:value-of select="$att"/>
						</xsl:attribute>
						<xsl:text>Instructional Material</xsl:text>	
					</xsl:element>
					<xsl:element name="{$tag}">
						<xsl:attribute name="xsi:type">
							<xsl:value-of select="$att"/>
						</xsl:attribute>
						<xsl:text>Field Trip</xsl:text>	
					</xsl:element>
				</xsl:when>

<!--processing Game-->
				<xsl:when test="$string = 'Game' ">
					<xsl:element name="{$tag}">
						<xsl:attribute name="xsi:type">
							<xsl:value-of select="$att"/>
						</xsl:attribute>
						<xsl:text>Instructional Material</xsl:text>	
					</xsl:element>
					<xsl:element name="{$tag}">
						<xsl:attribute name="xsi:type">
							<xsl:value-of select="$att"/>
						</xsl:attribute>
						<xsl:text>Game</xsl:text>	
					</xsl:element>
				</xsl:when>

<!--processing Lesson/Lesson Plan-->
				<xsl:when test="$string = 'Lesson/Lesson Plan' ">
					<xsl:element name="{$tag}">
						<xsl:attribute name="xsi:type">
							<xsl:value-of select="$att"/>
						</xsl:attribute>
						<xsl:text>Instructional Material</xsl:text>	
					</xsl:element>
					<xsl:element name="{$tag}">
						<xsl:attribute name="xsi:type">
							<xsl:value-of select="$att"/>
						</xsl:attribute>
						<xsl:text>Lesson/Lesson Plan</xsl:text>	
					</xsl:element>
				</xsl:when>

<!--processing Model-->
				<xsl:when test="$string = 'Model' ">
					<xsl:element name="{$tag}">
						<xsl:attribute name="xsi:type">
							<xsl:value-of select="$att"/>
						</xsl:attribute>
						<xsl:text>Instructional Material</xsl:text>	
					</xsl:element>
					<xsl:element name="{$tag}">
						<xsl:attribute name="xsi:type">
							<xsl:value-of select="$att"/>
						</xsl:attribute>
						<xsl:text>Model</xsl:text>	
					</xsl:element>
				</xsl:when>

<!--processing Simulation-->
				<xsl:when test="$string = 'Simulation' ">
					<xsl:element name="{$tag}">
						<xsl:attribute name="xsi:type">
							<xsl:value-of select="$att"/>
						</xsl:attribute>
						<xsl:text>Instructional Material</xsl:text>	
					</xsl:element>
					<xsl:element name="{$tag}">
						<xsl:attribute name="xsi:type">
							<xsl:value-of select="$att"/>
						</xsl:attribute>
						<xsl:text>Simulation</xsl:text>	
					</xsl:element>
				</xsl:when>

<!--processing Workshop-->
				<xsl:when test="$string = 'Workshop' ">
					<xsl:element name="{$tag}">
						<xsl:attribute name="xsi:type">
							<xsl:value-of select="$att"/>
						</xsl:attribute>
						<xsl:text>Event</xsl:text>	
					</xsl:element>
					<xsl:element name="{$tag}">
						<xsl:attribute name="xsi:type">
							<xsl:value-of select="$att"/>
						</xsl:attribute>
						<xsl:text>Workshop</xsl:text>	
					</xsl:element>
				</xsl:when>
			</xsl:choose>
		</xsl:if>
	</xsl:template>	

	<!--FORMAT template-->
	<xsl:template match="node()" name="format" mode="format">
<!--note: in templates, params must be declared before variables-->
<!--note: for the with-param tag to work above, a param tag must exist and be present here in the template being called-->
		<xsl:param name="tag"/>
		<xsl:param name="att"/>
		<xsl:param name="string" select="./@fileType"/>
		<xsl:if test="string-length($string) > 0">
			<xsl:choose>
<!--processing Web page-->
				<xsl:when test="$string = 'Web page' ">
					<xsl:element name="{$tag}">
						<xsl:attribute name="xsi:type">
							<xsl:value-of select="$att"/>
						</xsl:attribute>
						<xsl:text>text/html</xsl:text>	
					</xsl:element>
				</xsl:when>

<!--processing Audio-->
				<xsl:when test="$string = 'Audio' ">
					<xsl:element name="{$tag}">
						<xsl:attribute name="xsi:type">
							<xsl:value-of select="$att"/>
						</xsl:attribute>
						<xsl:text>audio</xsl:text>	
					</xsl:element>
				</xsl:when>

<!--processing Video-->
				<xsl:when test="$string = 'Video' ">
					<xsl:element name="{$tag}">
						<xsl:attribute name="xsi:type">
							<xsl:value-of select="$att"/>
						</xsl:attribute>
						<xsl:text>video</xsl:text>	
					</xsl:element>
				</xsl:when>
				
<!--processing Image-->
				<xsl:when test="$string = 'Image' ">
					<xsl:element name="{$tag}">
						<xsl:attribute name="xsi:type">
							<xsl:value-of select="$att"/>
						</xsl:attribute>
						<xsl:text>image</xsl:text>	
					</xsl:element>
				</xsl:when>

<!--processing Word/Text-->
				<xsl:when test="$string = 'Word/Text' ">
					<xsl:element name="{$tag}">
						<xsl:attribute name="xsi:type">
							<xsl:value-of select="$att"/>
						</xsl:attribute>
						<xsl:text>application/msword</xsl:text>	
					</xsl:element>
				</xsl:when>

<!--processing PDF-->
				<xsl:when test="$string = 'PDF' ">
					<xsl:element name="{$tag}">
						<xsl:attribute name="xsi:type">
							<xsl:value-of select="$att"/>
						</xsl:attribute>
						<xsl:text>application/pdf</xsl:text>	
					</xsl:element>
				</xsl:when>

<!--processing Excel/Spreadsheet-->
				<xsl:when test="$string = 'Excel/Spreadsheet' ">
					<xsl:element name="{$tag}">
						<xsl:attribute name="xsi:type">
							<xsl:value-of select="$att"/>
						</xsl:attribute>
						<xsl:text>application/vnd.ms-excel</xsl:text>	
					</xsl:element>
				</xsl:when>

<!--processing Presentation/Powerpoint-->
				<xsl:when test="$string = 'Presentation/Powerpoint' ">
					<xsl:element name="{$tag}">
						<xsl:attribute name="xsi:type">
							<xsl:value-of select="$att"/>
						</xsl:attribute>
						<xsl:text>application/vnd.ms-powerpoint</xsl:text>	
					</xsl:element>
				</xsl:when>
			</xsl:choose>
		</xsl:if>
	</xsl:template>	

	<!--DATE template-->
<!--template  enforces W3C compliancy; so date information is only written if a year is present because when only a month and day is present this is not W3C compliant-->
<!--for year information, this template assumes it is supplied as a four digit numerical value like 1920, 0835 or 0067-->
	<xsl:template match="node()" name="date" mode="date">
<!--note: in templates, params must be declared before variables-->
<!--note: for the with-param tag to work above, a param tag must exist and be present here in the template being called-->
		<xsl:param name="tag"/>
		<xsl:param name="att"/>
		<xsl:param name="day" select="./sm:day"/>
<!--process the textual month names into the numerical W3C date format to be W3CDTf compliant-->
		<xsl:param name="month">
			<xsl:choose>
				<xsl:when test="./sm:month = 'January' ">01</xsl:when>
				<xsl:when test="./sm:month = 'February' ">02</xsl:when>
				<xsl:when test="./sm:month = 'March' ">03</xsl:when>
				<xsl:when test="./sm:month = 'April' ">04</xsl:when>
				<xsl:when test="./sm:month = 'May' ">05</xsl:when>
				<xsl:when test="./sm:month = 'June' ">06</xsl:when>
				<xsl:when test="./sm:month = 'July' ">07</xsl:when>
				<xsl:when test="./sm:month = 'August' ">08</xsl:when>
				<xsl:when test="./sm:month = 'September' ">09</xsl:when>		
				<xsl:when test="./sm:month = 'October' ">10</xsl:when>		
				<xsl:when test="./sm:month = 'November' ">11</xsl:when>		
				<xsl:when test="./sm:month = 'December' ">12</xsl:when>		
			</xsl:choose>
		</xsl:param>
		<xsl:param name="year" select="./sm:year"/>
		
		<xsl:choose>
			<xsl:when test="string-length($year) = 4">		
				<xsl:element name="{$tag}">
					<xsl:if test="string-length($att) > 0">
						<xsl:attribute name="xsi:type">
							<xsl:value-of select="$att"/>
						</xsl:attribute>	
					</xsl:if>
					<xsl:choose>
						<xsl:when test="string-length($month) > 0 and string-length($day) > 0 ">
							<xsl:value-of select="concat($year, '-', $month, '-', $day)"/>
						</xsl:when>
						<xsl:when test="string-length($month) > 0 and string-length($day) = 0 ">
							<xsl:value-of select="concat($year, '-', $month)"/>					
						</xsl:when>
						<xsl:when test="string-length($month) = 0 and string-length($day) = 0 ">
							<xsl:value-of select="$year"/>
						</xsl:when>
					</xsl:choose>
				</xsl:element>
			</xsl:when>
			<xsl:otherwise>		
				<xsl:element name="{$tag}">
				<!--use the text from month rather than the variable month-->
					<xsl:choose>
						<xsl:when test="string-length($year) = 0 and string-length($month) > 0 and string-length($day) > 0 ">
							<xsl:value-of select="concat(./sm:month, ' ', $day)"/>
						</xsl:when>
						<xsl:when test="string-length($year) = 0 and string-length($month) > 0 and string-length($day) = 0 ">
							<xsl:value-of select="./sm:month"/>					
						</xsl:when>
						<!--year=0; month=0 and day>0 has no meaning so did not do but this writes and empty dct:temporal tag-->
						<xsl:when test="string-length($year) > 0 and string-length($month) > 0 and string-length($day) > 0 ">
							<xsl:value-of select="concat('Year information is: ', $year, '; Month and day information is: ', ./sm:month, ' ', $day)"/>
						</xsl:when>
						<xsl:when test="string-length($year) > 0 and string-length($month) > 0 and string-length($day) = 0 ">
							<xsl:value-of select="concat('Year information is: ', $year, '; Month information is: ', ./sm:month)"/>
						</xsl:when>
						<xsl:when test="string-length($year) > 0 and string-length($month) = 0 and string-length($day) > 0 ">
							<xsl:value-of select="concat('Year information is: ', $year, '; Day information is: ', $day)"/>
						</xsl:when>
						<xsl:when test="string-length($year) > 0 and string-length($month) = 0 and string-length($day) = 0 ">
							<xsl:value-of select="concat('Year information is: ', $year)"/>
						</xsl:when>
					</xsl:choose>
				</xsl:element>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>	

	<!--COVERAGE template-->
	<xsl:template match="node()" name="coverage" mode="coverage">
	<!--assumes all 4 coordinates are present and are numerical values-->
<!--note: in templates, params must be declared before variables-->
<!--note: for the with-param tag to work above, a param tag must exist and be present here in the template being called-->
		<xsl:param name="tag"/>
		<xsl:param name="att"/>
		<xsl:param name="place" select="./sm:placeName"/>
		<xsl:param name="northlimit" select="./sm:geographicalCoordinates/sm:northCoord"/>
		<xsl:param name="southlimit" select="./sm:geographicalCoordinates/sm:southCoord"/>
		<xsl:param name="eastlimit" select="./sm:geographicalCoordinates/sm:eastCoord"/>
		<xsl:param name="westlimit" select="./sm:geographicalCoordinates/sm:westCoord"/>
		
		<xsl:choose>
			<xsl:when test="string-length($northlimit) > 0 and string-length($southlimit) > 0 and string-length($eastlimit) > 0 and string-length($westlimit) > 0 and string-length($place) > 0 ">		
				<xsl:element name="{$tag}">
					<xsl:if test="string-length($att) > 0">
						<xsl:attribute name="xsi:type">
							<xsl:value-of select="$att"/>
						</xsl:attribute>	
					</xsl:if>
					<xsl:value-of select="concat('northlimit=', $northlimit, '; southlimit=', $southlimit, '; westlimit=', $westlimit, '; eastlimit=', $eastlimit, '; name=', $place)"/>
				</xsl:element>
			</xsl:when> 
			<xsl:when test="string-length($northlimit) > 0 and string-length($southlimit) > 0 and string-length($eastlimit) > 0 and string-length($westlimit) > 0 and string-length($place) = 0 ">		
				<xsl:element name="{$tag}">
					<xsl:if test="string-length($att) > 0">
						<xsl:attribute name="xsi:type">
							<xsl:value-of select="$att"/>
						</xsl:attribute>	
					</xsl:if>
					<xsl:value-of select="concat('northlimit=', $northlimit, '; southlimit=', $southlimit, '; westlimit=', $westlimit, '; eastlimit=', $eastlimit)"/>
				</xsl:element>
			</xsl:when> 
		</xsl:choose>
	</xsl:template>
</xsl:stylesheet>
<!--LICENSE AND COPYRIGHT
The contents of this file are subject to the Educational Community License v1.0 (the "License"); you may not use this file except in compliance with the License. You should obtain a copy of the License from http://www.opensource.org/licenses/ecl1.php. Files distributed under the License are distributed on an "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for the specific language governing rights and limitations under the License. Copyright 2002-2009 by Digital Learning Sciences, University Corporation for Atmospheric Research (UCAR). All rights reserved.-->