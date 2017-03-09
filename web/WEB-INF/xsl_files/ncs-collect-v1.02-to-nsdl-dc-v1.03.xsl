<?xml version="1.0"?>
<xsl:stylesheet 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xsi ="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:dc="http://purl.org/dc/elements/1.1/"
    xmlns:nsdl_dc="http://ns.nsdl.org/nsdl_dc_v1.03/"
    xmlns:ncs="http://ns.nsdl.org/ncs"
    xmlns:dct="http://purl.org/dc/terms/"
    xmlns:ieee="http://www.ieee.org/xsd/LOMv1p0"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
	exclude-result-prefixes="xsi ncs"
    version="1.0">

<!--PURPOSE-->
<!-- **************************************-->
<!--To transform ncs_collect version 1.02 metadata records to the nsdl_dc version 1.03 format.-->

<!--HISTORY-->
<!-- **************************************-->
<!--2007-08-10:correct audience to be nsdlAudience-->

<!--LICENSE INFORMATION and CREDITS-->
<!-- *****************************************************-->
<!--Date created: 2006-12-12 by Katy Ginger, University Corporation for Atmospheric Research (UCAR)-->
<!--Last modified: 2007-08-10 by Katy Ginger-->
<!--Changes: to break nsdl-ncs-to-dc-and-dc-to-ncs-v1.02.xsl into 2 separate transforms and remove the extra resulting namespaces-->
<!--License information:
		Copyright (c) 2006 University Corporation for Atmospheric Research (UCAR)
		P.O. Box 3000, Boulder, CO 80307, United States of America
		email: dlesesupport@ucar.edu.
		All rights reserved-->

	<xsl:output method="xml" indent="yes" encoding="UTF-8"/>


<!--VARIABLES used throughout the transform-->
<!-- **********************************************************-->
	<xsl:variable name="asnIdsURL">http://ns.nsdl.org/ncs/xml/NCS-to-ASN-NSES-mappings.xml</xsl:variable>
	<xsl:variable name="asnText">http://purl.org/ASN/</xsl:variable>
	
<!--TRANSFORMATION CODE-->
<!-- **************************************-->
	<xsl:template match="*|/">
		<xsl:apply-templates select="ncs:record"/>
	</xsl:template>

<!--TRANSFORMATION CODE for nsdl_ncs to nsdl_dc-->
<!-- ********************************************************-->
	<xsl:template match="ncs:record">
		<nsdl_dc:nsdl_dc schemaVersion="1.03.000" xmlns:nsdl_dc="http://ns.nsdl.org/nsdl_dc_v1.03/" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dct="http://purl.org/dc/terms/" xmlns:ieee="http://www.ieee.org/xsd/LOMv1p0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://ns.nsdl.org/nsdl_dc_v1.03/ http://ns.nsdl.org/schemas/nsdl_dc/nsdl_dc_v1.03.xsd">


<!--dc:identifier; doesn't really need a template but easy to do-->
		<xsl:apply-templates select="ncs:general/ncs:url" mode="process"> 
			<xsl:with-param name="tag">dc:identifier</xsl:with-param>
			<xsl:with-param name="att">dct:URI</xsl:with-param>
		</xsl:apply-templates>

<!--dc:title-->
		<xsl:apply-templates select="ncs:general/ncs:title" mode="process">
			<xsl:with-param name="tag">dc:title</xsl:with-param>
		</xsl:apply-templates>

<!--dct:alternative-->
		<xsl:apply-templates select="ncs:general/ncs:alternative" mode="process">
			<xsl:with-param name="tag">dct:alternative</xsl:with-param>
		</xsl:apply-templates>

<!--dct:abstract-->
		<xsl:apply-templates select="ncs:general/ncs:abstract" mode="process">
			<xsl:with-param name="tag">dct:abstract</xsl:with-param>
		</xsl:apply-templates>

<!--dc:description-->
		<xsl:apply-templates select="ncs:general/ncs:description" mode="process">
			<xsl:with-param name="tag">dc:description</xsl:with-param>
		</xsl:apply-templates>

<!--dc:subject-->
		<xsl:apply-templates select="ncs:general/ncs:subject" mode="process">
			<xsl:with-param name="tag">dc:subject</xsl:with-param>
		</xsl:apply-templates>

<!--dc:subject using gemSubject; 2007-10-22 no longer needed because not supporting a subject controlled vocabulary-->
<!--		<xsl:apply-templates select="ncs:general/ncs:subjects/ncs:gemSubject" mode="process">
			<xsl:with-param name="tag">dc:subject</xsl:with-param>
			<xsl:with-param name="att">nsdl_dc:GEM</xsl:with-param>
		</xsl:apply-templates>-->

<!--dc:subject using otherSubject; 2007-10-22 no longer needed because not supporting a subject controlled vocabulary-->
<!--		<xsl:apply-templates select="ncs:general/ncs:subjects/ncs:otherSubject" mode="process">
			<xsl:with-param name="tag">dc:subject</xsl:with-param>
		</xsl:apply-templates>-->

<!--dct:abstract-->
		<xsl:apply-templates select="ncs:general/ncs:abstract" mode="process">
			<xsl:with-param name="tag">dct:abstract</xsl:with-param>
		</xsl:apply-templates>

<!--dct:tableOfContents-->
		<xsl:apply-templates select="ncs:general/ncs:tableOfContents" mode="process">
			<xsl:with-param name="tag">dct:tableOfContents</xsl:with-param>
		</xsl:apply-templates>

<!--dct:bibliographicCitation-->
		<xsl:apply-templates select="ncs:general/ncs:bibliographicCitation" mode="process">
			<xsl:with-param name="tag">dct:bibliographicCitation</xsl:with-param>
		</xsl:apply-templates>

<!--dc:language using languages.ISOcode-->
		<xsl:apply-templates select="ncs:general/ncs:languages/ncs:code" mode="process">
			<xsl:with-param name="tag">dc:language</xsl:with-param>
			<xsl:with-param name="att">dct:ISO639-2</xsl:with-param>
		</xsl:apply-templates>

<!--dc:language using languages.RFCcode-->
		<xsl:apply-templates select="ncs:general/ncs:languages/ncs:code" mode="process">
			<xsl:with-param name="tag">dc:language</xsl:with-param>
			<xsl:with-param name="att">dct:RFC3066</xsl:with-param>
		</xsl:apply-templates>

<!--dc:language using languages.otherLanguage-->
		<xsl:apply-templates select="ncs:general/ncs:languages/ncs:otherLanguage" mode="process">
			<xsl:with-param name="tag">dc:language</xsl:with-param>
		</xsl:apply-templates>

<!--dct:educationLevel using nsdlEdLevel-->
		<xsl:apply-templates select="ncs:educational/ncs:educationLevels/ncs:nsdlEdLevel" mode="process">
			<xsl:with-param name="tag">dct:educationLevel</xsl:with-param>
			<xsl:with-param name="att">nsdl_dc:NSDLEdLevel</xsl:with-param>
		</xsl:apply-templates>

<!--dct:educationLevel using otherEdLevel-->
		<xsl:apply-templates select="ncs:educational/ncs:educationLevels/ncs:otherEdLevel" mode="process"> 
			<xsl:with-param name="tag">dct:educationLevel</xsl:with-param>
		</xsl:apply-templates>

<!--dc:type using dcmiType-->
		<xsl:apply-templates select="ncs:educational/ncs:types/ncs:dcmiType" mode="process"> 
			<xsl:with-param name="tag">dc:type</xsl:with-param>
			<xsl:with-param name="att">dct:DCMIType</xsl:with-param>
		</xsl:apply-templates>

<!--dc:type using nsdlType-->
		<xsl:apply-templates select="ncs:educational/ncs:types/ncs:nsdlType" mode="process"> 
			<xsl:with-param name="tag">dc:type</xsl:with-param>
			<xsl:with-param name="att">nsdl_dc:NSDLType</xsl:with-param>
		</xsl:apply-templates>

<!--dc:type using otherType-->
		<xsl:apply-templates select="ncs:educational/ncs:types/ncs:otherType" mode="process"> 
			<xsl:with-param name="tag">dc:type</xsl:with-param>
		</xsl:apply-templates>

<!--dct:audience using nsdlAudience-->
		<xsl:apply-templates select="ncs:educational/ncs:audiences/ncs:nsdlAudience" mode="process"> 
			<xsl:with-param name="tag">dct:audience</xsl:with-param>
			<xsl:with-param name="att">nsdl_dc:NSDLAudience</xsl:with-param>
		</xsl:apply-templates>

<!--dct:audience using otherAudience-->
		<xsl:apply-templates select="ncs:educational/ncs:audiences/ncs:otherAudience" mode="process"> 
			<xsl:with-param name="tag">dct:audience</xsl:with-param>
		</xsl:apply-templates>

<!--dct:mediator using mediator-->
		<xsl:apply-templates select="ncs:educational/ncs:mediators/ncs:mediator" mode="process"> 
			<xsl:with-param name="tag">dct:mediator</xsl:with-param>
		</xsl:apply-templates>
		
<!--ieee:interactivityLevel using LOM-->
		<xsl:apply-templates select="ncs:educational/ncs:interactivityLevel/ncs:LOM" mode="process"> 
			<xsl:with-param name="tag">ieee:interactivityLevel</xsl:with-param>
		</xsl:apply-templates>
		
<!--ieee:interactivityLevel using description-->
		<xsl:apply-templates select="ncs:educational/ncs:interactivityLevel/ncs:description" mode="process"> 
			<xsl:with-param name="tag">ieee:interactivityLevel</xsl:with-param>
		</xsl:apply-templates>

<!--ieee:interactivityType using LOM-->
		<xsl:apply-templates select="ncs:educational/ncs:interactivityType/ncs:LOM" mode="process"> 
			<xsl:with-param name="tag">ieee:interactivityType</xsl:with-param>
		</xsl:apply-templates>

<!--ieee:interactivityType using description-->
		<xsl:apply-templates select="ncs:educational/ncs:interactivityType/ncs:description" mode="process"> 
			<xsl:with-param name="tag">ieee:interactivityType</xsl:with-param>
		</xsl:apply-templates>
		
<!--ieee:typicalLearningTime using duration-->
		<xsl:apply-templates select="ncs:educational/ncs:typicalLearningTime/ncs:duration" mode="process"> 
			<xsl:with-param name="tag">ieee:typicalLearningTime</xsl:with-param>
		</xsl:apply-templates>

<!--ieee:typicalLearningTime using description-->
		<xsl:apply-templates select="ncs:educational/ncs:typicalLearningTime/ncs:description" mode="process"> 
			<xsl:with-param name="tag">ieee:typicalLearningTime</xsl:with-param>
		</xsl:apply-templates>

<!--dct:accessibility-->
		<xsl:apply-templates select="ncs:educational/ncs:accessibility" mode="process"> 
			<xsl:with-param name="tag">dct:accessibility</xsl:with-param>
		</xsl:apply-templates>

<!--dct:instructionalMethod-->
		<xsl:apply-templates select="ncs:educational/ncs:instructionalMethods/ncs:method" mode="process"> 
			<xsl:with-param name="tag">dct:instructionalMethod</xsl:with-param>
		</xsl:apply-templates>

<!--dct:conformsTo using asnID-->
		<xsl:apply-templates select="ncs:educational/ncs:standards/ncs:asnID" mode="process"> 
			<xsl:with-param name="tag">dct:conformsTo</xsl:with-param>
			<xsl:with-param name="att">dct:URI</xsl:with-param>
		</xsl:apply-templates>

<!--dct:conformsTo using url-->
		<xsl:apply-templates select="ncs:educational/ncs:standards/ncs:url" mode="process"> 
			<xsl:with-param name="tag">dct:conformsTo</xsl:with-param>
			<xsl:with-param name="att">dct:URI</xsl:with-param>
		</xsl:apply-templates>

<!--dct:conformsTo using otherStandard-->
		<xsl:apply-templates select="ncs:educational/ncs:standards/ncs:otherStandard" mode="process"> 
			<xsl:with-param name="tag">dct:conformsTo</xsl:with-param>
		</xsl:apply-templates>

<!--dct:conformsTo using NSESstandard-->
		<xsl:apply-templates select="ncs:educational/ncs:standards/ncs:NSESstandard"/> 

<!--dc:contributor-->
		<xsl:apply-templates select="ncs:contributions/ncs:contributors/ncs:contributor" mode="process"> 
			<xsl:with-param name="tag">dc:contributor</xsl:with-param>
		</xsl:apply-templates>

<!--dc:creator-->
		<xsl:apply-templates select="ncs:contributions/ncs:creators/ncs:creator" mode="process"> 
			<xsl:with-param name="tag">dc:creator</xsl:with-param>
		</xsl:apply-templates>

<!--dc:publisher-->
		<xsl:apply-templates select="ncs:contributions/ncs:publishers/ncs:publisher" mode="process"> 
			<xsl:with-param name="tag">dc:publisher</xsl:with-param>
		</xsl:apply-templates>

<!--dc:rights-->
		<xsl:apply-templates select="ncs:rights/ncs:rights" mode="process"> 
			<xsl:with-param name="tag">dc:rights</xsl:with-param>
		</xsl:apply-templates>

<!--dct:rightsHolders-->
		<xsl:apply-templates select="ncs:rights/ncs:rightsHolders/ncs:rightsHolder" mode="process"> 
			<xsl:with-param name="tag">dct:rightsHolder</xsl:with-param>
		</xsl:apply-templates>

<!--dct:accessRights using meansOfAccess-->
		<xsl:apply-templates select="ncs:rights/ncs:accessRights/ncs:meansOfAccess" mode="process"> 
			<xsl:with-param name="tag">dct:accessRights</xsl:with-param>
			<xsl:with-param name="att">nsdl_dc:NSDLAccess</xsl:with-param>
		</xsl:apply-templates>

<!--dct:accessRights using url-->
		<xsl:apply-templates select="ncs:rights/ncs:accessRights/ncs:url" mode="process"> 
			<xsl:with-param name="tag">dct:accessRights</xsl:with-param>
			<xsl:with-param name="att">dct:URI</xsl:with-param>
		</xsl:apply-templates>

<!--dct:accessRights using description-->
		<xsl:apply-templates select="ncs:rights/ncs:accessRights/ncs:description" mode="process"> 
			<xsl:with-param name="tag">dct:accessRights</xsl:with-param>
		</xsl:apply-templates>

<!--dct:provenance-->
		<xsl:apply-templates select="ncs:rights/ncs:provenance" mode="process"> 
			<xsl:with-param name="tag">dct:provenance</xsl:with-param>
		</xsl:apply-templates>

<!--dct:license using url-->
		<xsl:apply-templates select="ncs:rights/ncs:license/ncs:url" mode="process"> 
			<xsl:with-param name="tag">dct:license</xsl:with-param>
			<xsl:with-param name="att">dct:URI</xsl:with-param>
		</xsl:apply-templates>

<!--dct:license using description-->
		<xsl:apply-templates select="ncs:rights/ncs:license/ncs:description" mode="process"> 
			<xsl:with-param name="tag">dct:license</xsl:with-param>
		</xsl:apply-templates>

<!--dct:accrualMethod-->
		<xsl:apply-templates select="ncs:rights/ncs:accrualMethod" mode="process"> 
			<xsl:with-param name="tag">dct:accrualMethod</xsl:with-param>
		</xsl:apply-templates>

<!--dct:accrualPeriodicity-->
		<xsl:apply-templates select="ncs:rights/ncs:accrualPeriodicity" mode="process"> 
			<xsl:with-param name="tag">dct:accrualPeriodicity</xsl:with-param>
		</xsl:apply-templates>

<!--dct:accrualPolicy-->
		<xsl:apply-templates select="ncs:rights/ncs:accrualPolicy" mode="process"> 
			<xsl:with-param name="tag">dct:accrualPolicy</xsl:with-param>
		</xsl:apply-templates>

<!--dc:relation using url-->
		<xsl:apply-templates select="ncs:relations/ncs:relations/ncs:url" mode="process"> 
			<xsl:with-param name="tag">dc:relation</xsl:with-param>
			<xsl:with-param name="att">dct:URI</xsl:with-param>
		</xsl:apply-templates>

<!--dc:relation using description-->
		<xsl:apply-templates select="ncs:relations/ncs:relations/ncs:other" mode="process"> 
			<xsl:with-param name="tag">dc:relation</xsl:with-param>
		</xsl:apply-templates>

<!--dc:source using url-->
		<xsl:apply-templates select="ncs:relations/ncs:sources/ncs:url" mode="process"> 
			<xsl:with-param name="tag">dc:source</xsl:with-param>
			<xsl:with-param name="att">dct:URI</xsl:with-param>
		</xsl:apply-templates>

<!--dc:source using description-->
		<xsl:apply-templates select="ncs:relations/ncs:sources/ncs:other" mode="process"> 
			<xsl:with-param name="tag">dc:source</xsl:with-param>
		</xsl:apply-templates>

<!--dct:isFormatOf using url-->
		<xsl:apply-templates select="ncs:relations/ncs:isFormatOf/ncs:url" mode="process"> 
			<xsl:with-param name="tag">dct:isFormatOf</xsl:with-param>
			<xsl:with-param name="att">dct:URI</xsl:with-param>
		</xsl:apply-templates>

<!--dct:isFormatOf using description-->
		<xsl:apply-templates select="ncs:relations/ncs:isFormatOf/ncs:other" mode="process"> 
			<xsl:with-param name="tag">dct:isFormatOf</xsl:with-param>
		</xsl:apply-templates>

<!--dct:hasFormat using url-->
		<xsl:apply-templates select="ncs:relations/ncs:hasFormat/ncs:url" mode="process"> 
			<xsl:with-param name="tag">dct:hasFormat</xsl:with-param>
			<xsl:with-param name="att">dct:URI</xsl:with-param>
		</xsl:apply-templates>

<!--dct:hasFormat using description-->
		<xsl:apply-templates select="ncs:relations/ncs:hasFormat/ncs:other" mode="process"> 
			<xsl:with-param name="tag">dct:hasFormat</xsl:with-param>
		</xsl:apply-templates>

<!--dct:isPartOf using url-->
		<xsl:apply-templates select="ncs:relations/ncs:isPartOf/ncs:url" mode="process"> 
			<xsl:with-param name="tag">dct:isPartOf</xsl:with-param>
			<xsl:with-param name="att">dct:URI</xsl:with-param>
		</xsl:apply-templates>

<!--dct:isPartOf using description-->
		<xsl:apply-templates select="ncs:relations/ncs:isPartOf/ncs:other" mode="process"> 
			<xsl:with-param name="tag">dct:isPartOf</xsl:with-param>
		</xsl:apply-templates>

<!--dct:hasPart using url-->
		<xsl:apply-templates select="ncs:relations/ncs:hasPart/ncs:url" mode="process"> 
			<xsl:with-param name="tag">dct:hasPart</xsl:with-param>
			<xsl:with-param name="att">dct:URI</xsl:with-param>
		</xsl:apply-templates>

<!--dct:hasPart using description-->
		<xsl:apply-templates select="ncs:relations/ncs:hasPart/ncs:other" mode="process"> 
			<xsl:with-param name="tag">dct:hasPart</xsl:with-param>
		</xsl:apply-templates>

<!--dct:isReferencedBy using url-->
		<xsl:apply-templates select="ncs:relations/ncs:isReferencedBy/ncs:url" mode="process"> 
			<xsl:with-param name="tag">dct:isReferencedBy</xsl:with-param>
			<xsl:with-param name="att">dct:URI</xsl:with-param>
		</xsl:apply-templates>

<!--dct:isReferencedBy using description-->
		<xsl:apply-templates select="ncs:relations/ncs:isReferencedBy/ncs:other" mode="process"> 
			<xsl:with-param name="tag">dct:isReferencedBy</xsl:with-param>
		</xsl:apply-templates>

<!--dct:references using url-->
		<xsl:apply-templates select="ncs:relations/ncs:references/ncs:url" mode="process"> 
			<xsl:with-param name="tag">dct:references</xsl:with-param>
			<xsl:with-param name="att">dct:URI</xsl:with-param>
		</xsl:apply-templates>

<!--dct:references using description-->
		<xsl:apply-templates select="ncs:relations/ncs:references/ncs:other" mode="process"> 
			<xsl:with-param name="tag">dct:references</xsl:with-param>
		</xsl:apply-templates>

<!--dct:isReplacedBy using url-->
		<xsl:apply-templates select="ncs:relations/ncs:isReplacedBy/ncs:url" mode="process"> 
			<xsl:with-param name="tag">dct:isReplacedBy</xsl:with-param>
			<xsl:with-param name="att">dct:URI</xsl:with-param>
		</xsl:apply-templates>

<!--dct:isReplacedBy using description-->
		<xsl:apply-templates select="ncs:relations/ncs:isReplacedBy/ncs:other" mode="process"> 
			<xsl:with-param name="tag">dct:isReplacedBy</xsl:with-param>
		</xsl:apply-templates>

<!--dct:replaces using url-->
		<xsl:apply-templates select="ncs:relations/ncs:replaces/ncs:url" mode="process"> 
			<xsl:with-param name="tag">dct:replaces</xsl:with-param>
			<xsl:with-param name="att">dct:URI</xsl:with-param>
		</xsl:apply-templates>

<!--dct:replaces using description-->
		<xsl:apply-templates select="ncs:relations/ncs:replaces/ncs:other" mode="process"> 
			<xsl:with-param name="tag">dct:replaces</xsl:with-param>
		</xsl:apply-templates>

<!--dct:isRequiredBy using url-->
		<xsl:apply-templates select="ncs:relations/ncs:isRequiredBy/ncs:url" mode="process"> 
			<xsl:with-param name="tag">dct:isRequiredBy</xsl:with-param>
			<xsl:with-param name="att">dct:URI</xsl:with-param>
		</xsl:apply-templates>

<!--dct:isRequiredBy using description-->
		<xsl:apply-templates select="ncs:relations/ncs:isRequiredBy/ncs:other" mode="process"> 
			<xsl:with-param name="tag">dct:isRequiredBy</xsl:with-param>
		</xsl:apply-templates>

<!--dct:requires using url-->
		<xsl:apply-templates select="ncs:relations/ncs:requires/ncs:url" mode="process"> 
			<xsl:with-param name="tag">dct:requires</xsl:with-param>
			<xsl:with-param name="att">dct:URI</xsl:with-param>
		</xsl:apply-templates>

<!--dct:requires using description-->
		<xsl:apply-templates select="ncs:relations/ncs:requires/ncs:other" mode="process"> 
			<xsl:with-param name="tag">dct:requires</xsl:with-param>
		</xsl:apply-templates>

<!--dct:isVersionOf using url-->
		<xsl:apply-templates select="ncs:relations/ncs:isVersionOf/ncs:url" mode="process"> 
			<xsl:with-param name="tag">dct:isVersionOf</xsl:with-param>
			<xsl:with-param name="att">dct:URI</xsl:with-param>
		</xsl:apply-templates>

<!--dct:isVersionOf using description-->
		<xsl:apply-templates select="ncs:relations/ncs:isVersionOf/ncs:other" mode="process"> 
			<xsl:with-param name="tag">dct:isVersionOf</xsl:with-param>
		</xsl:apply-templates>

<!--dct:hasVersion using url-->
		<xsl:apply-templates select="ncs:relations/ncs:hasVersion/ncs:url" mode="process"> 
			<xsl:with-param name="tag">dct:hasVersion</xsl:with-param>
			<xsl:with-param name="att">dct:URI</xsl:with-param>
		</xsl:apply-templates>

<!--dct:hasVersion using description-->
		<xsl:apply-templates select="ncs:relations/ncs:hasVersion/ncs:other" mode="process"> 
			<xsl:with-param name="tag">dct:hasVersion</xsl:with-param>
		</xsl:apply-templates>

<!--dct:extent-->
		<xsl:apply-templates select="ncs:technical/ncs:extent" mode="process"> 
			<xsl:with-param name="tag">dct:extent</xsl:with-param>
		</xsl:apply-templates>

<!--dct:medium-->
		<xsl:apply-templates select="ncs:technical/ncs:medium" mode="process"> 
			<xsl:with-param name="tag">dct:medium</xsl:with-param>
		</xsl:apply-templates>

<!--dc:format using mimetype-->
		<xsl:apply-templates select="ncs:technical/ncs:mimetypes/ncs:mimetype" mode="process"> 
			<xsl:with-param name="tag">dc:format</xsl:with-param>
			<xsl:with-param name="att">dct:IMT</xsl:with-param>
		</xsl:apply-templates>

<!--dc:format using format-->
		<xsl:apply-templates select="ncs:technical/ncs:format" mode="process"> 
			<xsl:with-param name="tag">dc:format</xsl:with-param>
		</xsl:apply-templates>

<!--dc:date using W3Cdate-->
		<xsl:apply-templates select="ncs:dates/ncs:date/ncs:W3Cdate" mode="process"> 
			<xsl:with-param name="tag">dc:date</xsl:with-param>
		</xsl:apply-templates>

<!--dc:date using otherDate-->
		<xsl:apply-templates select="ncs:dates/ncs:date/ncs:otherDate" mode="process"> 
			<xsl:with-param name="tag">dc:date</xsl:with-param>
		</xsl:apply-templates>

<!--dct:created using W3Cdate-->
		<xsl:apply-templates select="ncs:dates/ncs:created/ncs:W3Cdate" mode="process"> 
			<xsl:with-param name="tag">dct:created</xsl:with-param>
		</xsl:apply-templates>

<!--dct:created using otherDate-->
		<xsl:apply-templates select="ncs:dates/ncs:created/ncs:otherDate" mode="process"> 
			<xsl:with-param name="tag">dct:created</xsl:with-param>
		</xsl:apply-templates>

<!--dct:available using W3Cdate-->
		<xsl:apply-templates select="ncs:dates/ncs:available/ncs:W3Cdate" mode="process"> 
			<xsl:with-param name="tag">dct:available</xsl:with-param>
		</xsl:apply-templates>

<!--dct:available using otherDate-->
		<xsl:apply-templates select="ncs:dates/ncs:available/ncs:otherDate" mode="process"> 
			<xsl:with-param name="tag">dct:available</xsl:with-param>
		</xsl:apply-templates>

<!--dct:issued using W3Cdate-->
		<xsl:apply-templates select="ncs:dates/ncs:issued/ncs:W3Cdate" mode="process"> 
			<xsl:with-param name="tag">dct:issued</xsl:with-param>
		</xsl:apply-templates>

<!--dct:issued using otherDate-->
		<xsl:apply-templates select="ncs:dates/ncs:issued/ncs:otherDate" mode="process"> 
			<xsl:with-param name="tag">dct:issued</xsl:with-param>
		</xsl:apply-templates>

<!--dct:modified using W3Cdate-->
		<xsl:apply-templates select="ncs:dates/ncs:modified/ncs:W3Cdate" mode="process"> 
			<xsl:with-param name="tag">dct:modified</xsl:with-param>
		</xsl:apply-templates>

<!--dct:modified using otherDate-->
		<xsl:apply-templates select="ncs:dates/ncs:modified/ncs:otherDate" mode="process"> 
			<xsl:with-param name="tag">dct:modified</xsl:with-param>
		</xsl:apply-templates>

<!--dct:valid using W3Cdate-->
		<xsl:apply-templates select="ncs:dates/ncs:valid/ncs:W3Cdate" mode="process"> 
			<xsl:with-param name="tag">dct:valid</xsl:with-param>
		</xsl:apply-templates>

<!--dct:valid using otherDate-->
		<xsl:apply-templates select="ncs:dates/ncs:valid/ncs:otherDate" mode="process"> 
			<xsl:with-param name="tag">dct:valid</xsl:with-param>
		</xsl:apply-templates>

<!--dct:dateAccepted using W3Cdate-->
		<xsl:apply-templates select="ncs:dates/ncs:dateAccepted/ncs:W3Cdate" mode="process"> 
			<xsl:with-param name="tag">dct:dateAccepted</xsl:with-param>
		</xsl:apply-templates>

<!--dct:dateAccepted using otherDate-->
		<xsl:apply-templates select="ncs:dates/ncs:dateAccepted/ncs:otherDate" mode="process"> 
			<xsl:with-param name="tag">dct:dateAccepted</xsl:with-param>
		</xsl:apply-templates>

<!--dct:dateCopyrighted using W3Cdate-->
		<xsl:apply-templates select="ncs:dates/ncs:dateCopyrighted/ncs:W3Cdate" mode="process"> 
			<xsl:with-param name="tag">dct:dateCopyrighted</xsl:with-param>
		</xsl:apply-templates>

<!--dct:dateCopyrighted using otherDate-->
		<xsl:apply-templates select="ncs:dates/ncs:dateCopyrighted/ncs:otherDate" mode="process"> 
			<xsl:with-param name="tag">dct:dateCopyrighted</xsl:with-param>
		</xsl:apply-templates>

<!--dct:dateSubmitted using W3Cdate-->
		<xsl:apply-templates select="ncs:dates/ncs:dateSubmitted/ncs:W3Cdate" mode="process"> 
			<xsl:with-param name="tag">dct:dateSubmitted</xsl:with-param>
		</xsl:apply-templates>

<!--dct:dateSubmitted using otherDate-->
		<xsl:apply-templates select="ncs:dates/ncs:dateSubmitted/ncs:otherDate" mode="process"> 
			<xsl:with-param name="tag">dct:dateSubmitted</xsl:with-param>
		</xsl:apply-templates>

<!--dct:temporal using W3Cdate-->
		<xsl:apply-templates select="ncs:dates/ncs:temporal/ncs:W3Cdate" mode="process"> 
			<xsl:with-param name="tag">dct:temporal</xsl:with-param>
		</xsl:apply-templates>

<!--dct:temporal using otherDate-->
		<xsl:apply-templates select="ncs:dates/ncs:temporal/ncs:otherDate" mode="process"> 
			<xsl:with-param name="tag">dct:temporal</xsl:with-param>
		</xsl:apply-templates>

<!--dc:coverage using W3Cdate-->
		<xsl:apply-templates select="ncs:dates/ncs:coverage/ncs:W3Cdate" mode="process"> 
			<xsl:with-param name="tag">dc:coverage</xsl:with-param>
		</xsl:apply-templates>

<!--dc:coverage using otherDate-->
		<xsl:apply-templates select="ncs:dates/ncs:coverage/ncs:otherDate" mode="process"> 
			<xsl:with-param name="tag">dc:coverage</xsl:with-param>
		</xsl:apply-templates>

<!--dc:coverage using coverage-->
		<xsl:apply-templates select="ncs:coverages/ncs:coverage" mode="process"> 
			<xsl:with-param name="tag">dc:coverage</xsl:with-param>
		</xsl:apply-templates>

<!--dct:spatial-->
		<xsl:apply-templates select="ncs:coverages/ncs:spatial" mode="process"> 
			<xsl:with-param name="tag">dct:spatial</xsl:with-param>
		</xsl:apply-templates>

<!--dct:temporal from under coverages-->
		<xsl:apply-templates select="ncs:coverages/ncs:temporal" mode="process"> 
			<xsl:with-param name="tag">dct:temporal</xsl:with-param>
		</xsl:apply-templates>


<!--dc:coverage using box-->
		<xsl:apply-templates select="ncs:coverages/ncs:box">
			<xsl:with-param name="tag">dc:coverage</xsl:with-param>
			<xsl:with-param name="att">dct:Box</xsl:with-param>
		</xsl:apply-templates>

<!--dc:coverage using point-->
		<xsl:apply-templates select="ncs:coverages/ncs:point">
			<xsl:with-param name="tag">dc:coverage</xsl:with-param>
			<xsl:with-param name="att">dct:Point</xsl:with-param>
		</xsl:apply-templates>

		</nsdl_dc:nsdl_dc><!--end nsdl_dc:nsdl_dc element-->
	</xsl:template>



<!--TEMPLATES for nsdl_ncs to nsdl_dc-->
<!-- ****************************************-->
<!--PROCESS:writes all tag sets that are not a content standard, box or point-->
<!--BOX or POINT: writes the coverages.box and coverages.point tag sets-->
<!--NSESCONTENTSTANDARD: writes the educational.standards.NSESstandard and sometimes the educational.standards.asnID tag sets-->

<!--PROCESS template-->
	<xsl:template match="node()" name="process" mode="process">
<!--note: in templates, params must be declared before variables-->
<!--note: for the with-param tag to work above, a param tag must exist and be present here in the template being called-->
		<xsl:param name="tag"/>
		<xsl:param name="att"/>
		<xsl:param name="string" select="."/>
		<xsl:param name="type" select="./@type"/>
		

		<xsl:choose>
			<xsl:when test="local-name()='W3Cdate' ">
				<xsl:variable name="dateVar" select="."/>
		
				<xsl:if test="string-length($dateVar)>0">
				<!--variable to grab the date of the first occurring date attribute only-->
					<xsl:variable name="dateStr" select="string(normalize-space($dateVar))"/>
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
					<xsl:choose>
						<xsl:when test="(string-length($lowercase) = '4' and not(string(number($yearStr)) = 'NaN')) or 
							(string-length($lowercase) = '7' and not(string(number($yearStr)) = 'NaN') and ($firstSep = '-') and
											not (string(number($monthStr)) = 'NaN') and ($monthStr &gt; '0' ) and ($monthStr &lt; '13' )) or
							(string-length($lowercase) = '10' and	not (string(number($yearStr)) = 'NaN')  and 
											($firstSep = '-') and not (string(number($monthStr)) = 'NaN') and
											 ($monthStr &gt; '0' ) and ($monthStr &lt; '13' ) and ($secondSep = '-') and
											 not (string(number($dayStr)) = 'NaN') and ($dayStr &gt; '0' ) and ($dayStr &lt; '32' ))">
											 
							<xsl:element name="{$tag}">
								<xsl:attribute name="xsi:type">dct:W3CDTF</xsl:attribute>	
								<xsl:value-of select="$string"/>
							</xsl:element>
						</xsl:when>
						<xsl:otherwise>
							<xsl:element name="{$tag}">
								<xsl:value-of select="$string"/>
							</xsl:element>						
						</xsl:otherwise>
					</xsl:choose>
				</xsl:if>	
			</xsl:when>
			
<!--2007-07-03: new stuff specific to ncs_collect to nsdl_dc only-->
<!--2007-07-03: this when case was added so that <nsdlEdLevel>Pre-K to 12</nsdlEdLevel>gets transformed to <dct:educationLevel>Pre-K to 12</dct:educationLevel> and not <dct:educationLevel xsi:type="nsdl_dc:NSDLEdLevel">Pre-K to 12</dct:educationLevel> because the term Pre-K to 12 is not part of the NSDL_DC education level controlled vocabulary. It is present present in ncs_collection only to facilitate the creation of collection record information.-->
			<xsl:when test="local-name()='nsdlEdLevel' ">
				<xsl:if test="string-length($string) > 0">
					<xsl:element name="{$tag}">
						<xsl:if test="string-length($att) > 0 and not($string ='Pre-K to 12') ">
							<xsl:attribute name="xsi:type">
								<xsl:value-of select="$att"/>
							</xsl:attribute>							
						</xsl:if>
						<xsl:value-of select="$string"/>
					</xsl:element>
				</xsl:if>
			</xsl:when>
<!--end 2007-07-03 addition-->

			<xsl:otherwise>
				<xsl:if test="string-length($string) > 0">
					<xsl:element name="{$tag}">
						<xsl:if test="string-length($att) > 0">
							<xsl:attribute name="xsi:type">
								<xsl:value-of select="$att"/>
							</xsl:attribute>	
						</xsl:if>
						<xsl:if test="string-length($type) > 0">
							<xsl:attribute name="xsi:type">
								<xsl:value-of select="$type"/>
							</xsl:attribute>	
						</xsl:if>
						<xsl:value-of select="$string"/>
					</xsl:element>
				</xsl:if>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>	

<!--BOX or POINT template-->
<xsl:template match="ncs:coverages/ncs:box | ncs:coverages/ncs:point">
	<!--nsdl_ncs does not have an element for units (for indicating the units for northlimit, southlimit etc), it is assumed that the units are then 'signed decimal degrees'. Since nsdl_ncs enforces this in the cataloging tool, do no create or write out a variable for units--> 
	<!--the translate function is used to force the nsdl_ncs element names of northLimit and southLimit etc. to be lowercase like northlimit and southlimit etc. because the words used in the Dublin Core encoding scheme for box are all lowercase.-->
		<xsl:param name="tag"/>
		<xsl:param name="att"/>		

<!--< character not allowed in test statements, so use a combinatio of not and ceiling functions-->
		<xsl:variable name="boxORpoint">
			<xsl:for-each select="./*">
				<xsl:choose>
					<xsl:when test="local-name()='northLimit'  or local-name()='southLimit' or local-name()='north' ">
						<xsl:if test="floor(.)>=-90 and not(ceiling(.)>90)">
							<xsl:value-of select="concat(translate(local-name(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), '=', ., ';')"/>
						</xsl:if>
					</xsl:when>
					<xsl:when test="local-name()='westLimit'  or local-name()='eastLimit' or local-name()='east' ">
						<xsl:if test="floor(.)>=-180 and not(ceiling(.)>180)">
							<xsl:value-of select="concat(translate(local-name(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), '=', ., ';')"/>
						</xsl:if>
					</xsl:when>
					<xsl:when test="local-name()='upLimit'  or local-name()='downLimit' or local-name()='elevation'  ">
						<xsl:if test="boolean(number(.))">
							<xsl:value-of select="concat(translate(local-name(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), '=', ., ';')"/>
						</xsl:if>
					</xsl:when>
					<xsl:otherwise>
						<xsl:if test="string-length(.)>0"><!--would apply to the remaining box elements-->
							<xsl:value-of select="concat(translate(local-name(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), '=', ., ';')"/>
						</xsl:if>
					</xsl:otherwise>	
				</xsl:choose>
			</xsl:for-each>
		</xsl:variable>

		<xsl:if test="string-length($boxORpoint)>0">
			<xsl:element name="{$tag}">
				<xsl:attribute name="xsi:type">
					<xsl:value-of select="$att"/>
				</xsl:attribute>	
				<xsl:value-of select="$boxORpoint"/>
			</xsl:element>
		</xsl:if>
	</xsl:template>	
	
<!--NSESCONTENTSTANDARD template-->
	<xsl:template match="ncs:NSESstandard">
<!--compares the nsdl_ncs value against the controlled vocabulary and only transforms data if the content is in the vocabulary-->
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
	</xsl:template>
</xsl:stylesheet>