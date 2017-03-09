<?xml version="1.0"?>
<xsl:stylesheet 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xsi ="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:xsd="http://www.w3.org/2001/XMLSchema"
    xmlns:dc="http://purl.org/dc/elements/1.1/"
    xmlns:nsdl_dc="http://ns.nsdl.org/nsdl_dc_v1.02/"
    xmlns:dct="http://purl.org/dc/terms/"
    xmlns:ieee="http://www.ieee.org/xsd/LOMv1p0"
    xmlns:ncs="http://ns.nsdl.org/ncs"
    xmlns:dl="http://collection.dlese.org"
    xmlns:dds="http://www.dlese.org/Metadata/ddsws"
	exclude-result-prefixes="ncs dl dc nsdl_dc dct ieee"
    version="1.0">

<!--PURPOSE-->
<!-- **************************************-->
<!--To transform ncs_collect version 1.02 metadata records to the dlese_collect 1.0.00 format.-->

<!--HISTORY-->
<!-- **************************************-->

<!--LICENSE INFORMATION and CREDITS-->
<!-- *****************************************************-->
<!--Date created: 2010-02-09 by Katy Ginger, University Corporation for Atmospheric Research (UCAR)-->
<!--License information: See below.-->

	<xsl:output method="xml" indent="yes" encoding="UTF-8"/>

<!--VARIABLES used throughout the transform-->
<!-- **********************************************************-->
<xsl:variable name="DDSWSID">http://ncs.nsdl.org/mgr/services/ddsws1-1?verb=GetRecord&amp;id=</xsl:variable>	


<!--TRANSFORMATION CODE for nsdl_ncs to nsdl_dc-->
<!-- ********************************************************-->
	<xsl:template match="ncs:record">
		<collectionRecord xmlns="http://collection.dlese.org" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		xsi:schemaLocation="http://collection.dlese.org http://www.dlese.org/Metadata/collection/1.0.00/collection.xsd">
	
			<general>

<!--fullTitle using title-->
				<fullTitle>
					<xsl:value-of select="ncs:general/ncs:title"/>
				</fullTitle>

<!--shortTitle using title-->
				<shortTitle>
					<xsl:value-of select="ncs:general/ncs:title"/>
				</shortTitle>

<!--description using description-->
				<description>
					<xsl:value-of select="ncs:general/ncs:description"/>
				</description>
				<language>en</language>
				<subjects>
					<subject>DLESE:Other</subject>
				</subjects>
				<gradeRanges>
					<gradeRange>DLESE:To be supplied</gradeRange>
				</gradeRanges>
				<cost>DLESE:Unknown</cost>
				<policies>
					<policy/>
				</policies>
			</general>
			<lifecycle>
				<contributors>
					<contributor date="2010-02-14" role="Contact">
						<person>
							<nameFirst></nameFirst>
							<nameLast></nameLast>
							<instName></instName>
							<emailPrimary>dlesesupport@ucar.edu</emailPrimary>
						</person>
					</contributor>
				</contributors>
		</lifecycle>
		<approval>
			<collectionStatuses>
				<collectionStatus date="2010-03-09" state="Accessioned"/>
			</collectionStatuses>
			<contributors>
				<contributor date="2010-02-14" role="Collection start approver">
					<person>
						<nameFirst></nameFirst>
						<nameLast></nameLast>
						<instName></instName>
						<emailPrimary>dlesesupport@ucar.edu</emailPrimary>
					</person>
				</contributor>
			</contributors>
		</approval>
		<access>
			<xsl:element name="key" namespace="http://collection.dlese.org">
				<xsl:attribute name="static">true</xsl:attribute>
				<xsl:attribute name="redistribute">false</xsl:attribute>
				<xsl:attribute name="libraryFormat">
<!--identifying the metadata format for the DLESE collection record-->
				<xsl:choose>
<!--when there is an OAI format; assume it is written nicely as nsdl_dc or oai_dc-->
					<xsl:when test="string-length(ncs:collection/ncs:ingest/ncs:oai/@format) > 0">
						<xsl:value-of select="ncs:collection/ncs:ingest/ncs:oai/@format"/>
					</xsl:when>
<!--for the special record that is the NSDL collection of collection records; this when clause must come before the next when clause otherwise this collection will be assigned a format of nsdl_dc rather than ncs_collect-->
					<xsl:when test="ncs:general/ncs:recordID = 'NAB-000-000-000-001' ">
						<xsl:text>ncs_collect</xsl:text>
					</xsl:when>
<!--when there is no OAI harvest format assume nsdl_dc because it would come from the NCS or WFI as nsdl_dc-->
					<xsl:when test="string-length(ncs:collection/ncs:ingest/ncs:oai/@format) = 0 ">
						<xsl:text>nsdl_dc</xsl:text>
					</xsl:when>
				</xsl:choose>
				</xsl:attribute>
<!--figuring out the key-->
<!--grab the ncs_collect record ID and then use the DDSWS to look up this id and grab the DLESE collection key-->
				<xsl:variable name="id">
					<xsl:value-of select="ncs:general/ncs:recordID"/>
				</xsl:variable>
		
<!--grabbing the key for the DLESE collection record-->					
				<xsl:value-of select="document(concat($DDSWSID, $id))//dds:record/dds:head/dds:collection/@key" />

			</xsl:element> <!--end key element-->
  
			<drc>false</drc>
			</access>
			<metaMetadata>
				<catalogEntries>
<!--enter the NSDL collection record id as the DLESE collection record id-->
					<xsl:element name="catalog" namespace="http://collection.dlese.org">
						<xsl:attribute name="entry">
							<xsl:value-of select="ncs:general/ncs:recordID"/>
						</xsl:attribute>
					</xsl:element><!--end catalog element-->
				</catalogEntries>
				<dateInfo created="2010-02-09" lastModified="2010-02-14T09:30:47" deaccessioned="2010-02-14" accessioned="2010-02-14"/>
				<statusOf status="Done"/>
				<language>en-us</language>
				<scheme>Digital Library for Earth System Education (DLESE) Collection Metadata</scheme>
				<copyright/>
				<termsOfUse URI=""/>
			</metaMetadata>
		</collectionRecord>
	</xsl:template>
</xsl:stylesheet>
<!--LICENSE AND COPYRIGHT
The contents of this file are subject to the Educational Community License v1.0 (the "License"); you may not use this file except in compliance with the License. You should obtain a copy of the License from http://www.opensource.org/licenses/ecl1.php. Files distributed under the License are distributed on an "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for the specific language governing rights and limitations under the License. Copyright 2002-2009 by Digital Learning Sciences, University Corporation for Atmospheric Research (UCAR). All rights reserved.-->