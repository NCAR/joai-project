<?xml version="1.0"?>
<xsl:stylesheet 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
	xmlns:a="http://www.dlese.org/Metadata/annotation" 
	xmlns:dc="http://purl.org/dc/elements/1.1/" 
    xmlns:nsdl_dc="http://ns.nsdl.org/nsdl_dc_v1.02/"
    xmlns:dct="http://purl.org/dc/terms/"
    xmlns:ieee="http://www.ieee.org/xsd/LOMv1p0"
    xmlns:xsd="http://www.w3.org/2001/XMLSchema"
	exclude-result-prefixes=" xsi a xsd" 
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
<!--To transform selected data (content) of certain (selected) annotation collections within the Digital Library for Earth System Education (DLESE) to NSDL-DC-->

<!--B. LICENSE INFORMATION and CREDITS-->
<!-- *****************************************************-->
<!--Date created: 2007-08-28 by Katy Ginger, DLESE Program Center, University Corporation for Atmospheric Research (UCAR)-->
<!--Last modified: 2007-08-28 by Katy Ginger-->
<!--License information:
		Copyright (c) 2002-200
		University Corporation for Atmospheric Research (UCAR)
		P.O. Box 3000, Boulder, CO 80307, United States of America
		email: dlesesupport@ucar.edu.
		All rights reserved
This XML tranformation, written in XSLT 1.0 and XPATH 1.0, are free software; you can redistribute them and/or modify them under the terms of the GNU General Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any later version.  These XML instance documents are distributed in the hope that they will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this project; if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA -->


<!--C. ASSUMPTIONS-->
<!-- **************************************-->
<!--Applies to certain DLESE annotation 1.0.00 types and data and ADN  0.6.50 metadata records-->
<!--Assumes content is present in annotation required fields and does not check for the presence of it-->
<!--Assumes annotations are textual unless otherwise specified in the status attribute-->
<!--Content for DC identifier is the URL of the DLESE annotated resource or if the resource is no longer in DLESE, a URL of www.dlese.org is used.-->
<!--Construct a NSDL_DC metadata record by getting the resource title, description, identifier from the appropriate ADN metadata record. Use the annotation record to complete the NSDL_DC fields of relation or conformsTo-->
<!--If the resource being annotated by the DLESE annotation record is no longer in DLESE, then use a generic title, description and identifier of DLESE, www.dlese.org so that a title, description and identifier are always written-->

	<xsl:output method="xml" indent="yes" encoding="UTF-8"/>


<!--D. VARIABLES used throughout the transform-->
<!-- *****************************************************-->
<!--variables for accessing DLESE id prefixes files-->
<xsl:variable name="DDSWSID">http://www.dlese.org/dds/services/ddsws1-0?verb=GetRecord&amp;id=</xsl:variable>	


<!--E. TRANSFORMATION CODE-->
<!-- **************************************-->
	<xsl:template match="a:annotationRecord">
		<nsdl_dc:nsdl_dc schemaVersion="1.02.010" xmlns:nsdl_dc="http://ns.nsdl.org/nsdl_dc_v1.02/" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dct="http://purl.org/dc/terms/" xmlns:ieee="http://www.ieee.org/xsd/LOMv1p0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://ns.nsdl.org/nsdl_dc_v1.02/ http://ns.nsdl.org/schemas/nsdl_dc/nsdl_dc_v1.02.xsd">

		<!--itemID indicates the DLESE resource being annotated; this is the resource ID (later turned into a URL) that is used for the dc:identifier field-->
			<xsl:variable name="id">
				<xsl:value-of select="a:itemID"/>
			</xsl:variable>

		<!--check to see if in DLESE as a ADN record-->
		<!--reading ADN records in webservices-->					
			<xsl:variable name="adn">
				<xsl:value-of select="document(concat($DDSWSID, $id))//metaMetadata/catalogEntries/catalog/@entry" />
			</xsl:variable>
		
		<!--if in DLESE, then grab ADN primary URL because it resolves the itemID above to a URL-->
			<xsl:variable name="identifier">
				<xsl:choose>
					<xsl:when test="$id = $adn">
						<xsl:value-of select="document(concat($DDSWSID, $id))//technical/online/primaryURL"/>
					</xsl:when>
					<xsl:otherwise>
						<xsl:text>NaN</xsl:text>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:variable>

<!--type of annotation-->
			<xsl:variable name="annoType">
				<xsl:value-of select="a:annotation/a:type"/>
			</xsl:variable>
		
<!--dc:title-->
			<xsl:element name="dc:title">
				<xsl:choose>
					<xsl:when test="$identifier ='NaN' ">
						<xsl:text>Digital Library for Earth System Education (DLESE)</xsl:text>
					</xsl:when>
					<xsl:otherwise> 
						<xsl:value-of select="document(concat($DDSWSID, $id))//general/title"/>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:element>
	
<!--dc:description-->
			<xsl:element name="dc:description">
				<xsl:choose>
					<xsl:when test="$identifier ='NaN' ">
						<xsl:text>DLESE is a comprehensive online source for geoscience education, aggregating a wide variety of pedagogically sound, technologically robust, and scientifically accurate resources, collections of resources, datasets, services and communications to support inquiry-based, active, student-centered learning about the Earth system.</xsl:text>
					</xsl:when>
					<xsl:otherwise> 
						<xsl:value-of select="document(concat($DDSWSID, $id))//general/description"/>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:element>

<!--dc:identifier-->
			<xsl:element name="dc:identifier">
				<xsl:choose>
					<xsl:when test="$identifier ='NaN' ">
						<xsl:text>http://www.dlese.org</xsl:text>
					</xsl:when>
					<xsl:otherwise> 
						<xsl:value-of select="$identifier"/>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:element>
			
<!--dct:conformsTo and dc:relation-->
<!--if a valid identifier exists, check to see if there is dct:conformsTo and dc:relation info to transform-->
<!--for dc:relation: transform if both annotation.content.url and annotation.content.description are present; if only a description is present do not write a relation field-->
<!--for dct:conformsTo: transform if annoType equals educational standard; otherwise use dc:relation-->
			<xsl:if test="not($identifier ='NaN' )">
				<xsl:choose>
					<xsl:when test="$annoType='Educational standard' ">
						<xsl:if test="string-length(a:annotation/a:content/a:description) > 0">
							<xsl:element name="dct:conformsTo">
								<xsl:value-of select="a:annotation/a:content/a:description"/>
							</xsl:element>
						</xsl:if>
						<xsl:if test="string-length(a:annotation/a:content/a:url) > 0">
							<xsl:element name="dct:conformsTo">
								<xsl:attribute name="xsi:type">dct:URI</xsl:attribute>
								<xsl:value-of select="a:annotation/a:content/a:url"/>
							</xsl:element>							
						</xsl:if>
					</xsl:when>
					<xsl:otherwise>
						<xsl:if test="string-length(a:annotation/a:content/a:description) > 0 and string-length(a:annotation/a:content/a:url) > 0">
							<xsl:element name="dc:relation">
								<xsl:value-of select="a:annotation/a:content/a:description"/>
							</xsl:element>
							<xsl:element name="dc:relation">
								<xsl:attribute name="xsi:type">dct:URI</xsl:attribute>
								<xsl:value-of select="a:annotation/a:content/a:url"/>
							</xsl:element>
						</xsl:if>					
						<xsl:if test="string-length(a:annotation/a:content/a:description) > 0 and string-length(a:annotation/a:content/a:url) = 0">
							<xsl:element name="dc:relation">
								<xsl:text>http://www.dlese.org/library/view_annotation.do?type&#61;tt&amp;id&#61;</xsl:text>
								<xsl:value-of select="$id"/>
								<xsl:text>&amp;annoId&#61;</xsl:text>
								<xsl:value-of select="a:service/a:recordID"/>
							</xsl:element>
						</xsl:if>					
					</xsl:otherwise>
				</xsl:choose>
			</xsl:if>
			

<!--dc:type-->
			<xsl:choose>
				<xsl:when test="$identifier ='NaN' ">
					<xsl:element name="dc:type">
						<xsl:text>Collection</xsl:text>
					</xsl:element>
				</xsl:when>
				<xsl:otherwise> 
			<!--	variable for ADN educational.resourceTypes.resourceType-->
					<xsl:variable name="allresourceTypes">
						<xsl:for-each select="document(concat($DDSWSID, $id))//educational/resourceTypes/resourceType">
							<xsl:value-of select="."/>
						</xsl:for-each>
					</xsl:variable>
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
				</xsl:otherwise>
			</xsl:choose>


			<xsl:choose>
				<xsl:when test="$identifier ='NaN' ">
					<xsl:element name="dct:audience">Learner</xsl:element>
					<xsl:element name="dct:audience">General Public</xsl:element>
					<xsl:element name="dct:audience">Educator</xsl:element>
				</xsl:when>
				<xsl:otherwise> 	
			<!--	variable for ADN educational.audiences.audience.gradeRange-->
					<xsl:variable name="allgrades">
						<xsl:for-each select="document(concat($DDSWSID, $id))//educational/audiences/audience/gradeRange">
							<xsl:value-of select="."/>
						</xsl:for-each>
					</xsl:variable>
			
			<!--dct:educationLevel-->
			<!--maps ADN gradeRange terms to the NSDL list of terms-->
 
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
				</xsl:otherwise>
			</xsl:choose>

<!--end nsdl_dc:nsdl_dc-->
		</nsdl_dc:nsdl_dc>
	</xsl:template>

<!--F. TEMPLATES TO APPLY-->
<!--*********************************-->
<!--organized in alphabetical order-->



<!--end of templates-->	

</xsl:stylesheet>