<?xml version="1.0"?>

<xsl:stylesheet 

    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"

    xmlns:xsi ="http://www.w3.org/2001/XMLSchema-instance"

    xmlns:d="http://adn.dlese.org"

    exclude-result-prefixes="xsi d" 

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

<!--To transform the Digital Library for Earth System Education (DLESE) ADN metadata records to a brief DLESE metadata format called briefmeta-->



<!--B. LICENSE INFORMATION and CREDITS-->

<!-- *****************************************************-->

<!--Date created: 2003 by Katy Ginger, University Corporation for Atmospheric Research (UCAR)-->

<!--Last modified: 2006-04-07 by Katy Ginger-->

<!--License information:

		Copyright (c) 2007 Digital Learning Sciences

		University Corporation for Atmospheric Research (UCAR)

		P.O. Box 3000, Boulder, CO 80307, United States of America

		All rights reserved

These XML tranformation written in XSLT 1.0 and XPATH 1.0 are free software; you can redistribute them and/or modify them under the terms of the GNU General Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any later version.  These XML instance documents are distributed in the hope that they will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this project; if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA -->



    

<!--C. ASSUMPTIONS-->

<!-- **************************************-->

<!--Overarching assumption. The metadata field only appears if it contains data-->

<!--1. Applies to DLESE ADN metadata format, version 0.6.50 records-->

<!--2. Only applies to ADN online resources-->





	<xsl:output method="xml" indent="yes" encoding="UTF-8"/>





<!--E. TRANSFORMATION CODE-->

<!-- **********************************************************-->

	<xsl:template match="d:itemRecord">

		<xsl:element name="briefRecord" namespace="http://www.dlese.org/Metadata/briefmeta">

			<xsl:attribute name="xsi:schemaLocation">http://www.dlese.org/Metadata/briefmeta http://www.dlese.org/Metadata/briefmeta/0.1.01/brief-record.xsd</xsl:attribute>	



<!--title-->

		<xsl:element name="title" namespace="http://www.dlese.org/Metadata/briefmeta">

			<xsl:value-of select="d:general/d:title"/>

		</xsl:element>	



<!--description-->

<!--determine if the resource is online or offline; if offline, concatenate ADN general.description and ADN technical.offline.accessInformation-->

		<xsl:element name="description" namespace="http://www.dlese.org/Metadata/briefmeta">

			<xsl:value-of select="d:general/d:description"/>

		</xsl:element>





<!--url - using ADN primary url-->

<!--only online ADN resource will have a dc:identifier - dct:URI tag-->

		<xsl:element name="url" namespace="http://www.dlese.org/Metadata/briefmeta">

			<xsl:value-of select="d:technical/d:online/d:primaryURL"/>

		</xsl:element>



<!--subject - DLESE-->

<!--	variable for ADN general.subjects - in case the only value is the default value-->

			<xsl:variable name="allsubjects">

				<xsl:for-each select="d:general/d:subjects/d:subject">

					<xsl:value-of select="."/>

				</xsl:for-each>

			</xsl:variable>

	

<!--test for the presence of content in DLESE-IMS general.keywords; do this to prevent the ADN:general.keywords tag from appearing if it doesn't need to-->

			<xsl:choose >

				<xsl:when test="string($allsubjects)='DLESE:To be supplied'"/>

				<xsl:otherwise>

					<xsl:element name="subjects" namespace="http://www.dlese.org/Metadata/briefmeta">

						<xsl:apply-templates select="d:general/d:subjects/d:subject" mode="DLESE"/>

						<!--see template SUBJECT mode=DLESE-->

					</xsl:element>

				</xsl:otherwise>

			</xsl:choose>



<!--subject - from keywords-->

<!--		<xsl:apply-templates select="d:general/d:keywords/d:keyword" mode="keywords"/>-->

		<!--see template SUBJECT mode=KEYWORDS-->





<!--gradeRanges-->

<!--	variable for ADN educational.audeinces.audience.gradeRange - in case the only value is the default value-->

			<xsl:variable name="allgradeRanges">

				<xsl:for-each select="d:educational/d:audiences/d:audience/d:gradeRange">

					<xsl:value-of select="."/>

				</xsl:for-each>

			</xsl:variable>

	

			<xsl:choose >

				<xsl:when test="string($allgradeRanges)='DLESE:To be supplied'"/>

				<xsl:when test="string($allgradeRanges)='DLESE:Not applicable'"/>

				<xsl:otherwise>

					<xsl:element name="gradeRanges" namespace="http://www.dlese.org/Metadata/briefmeta">

						<xsl:apply-templates select="d:educational/d:audiences/d:audience/d:gradeRange"/>

						<!--see template RESOURCETYPE -->

					</xsl:element>

				</xsl:otherwise>

			</xsl:choose>





<!--resourceTypes-->

<!--no vocabulary mapping is necessary-->

<!--determine if the ADN metadata record refers to an online or offline resource-->

		<xsl:choose>

			<xsl:when test="string-length(d:technical/d:offline)>0">

				<xsl:element name="resourceTypes">

					<xsl:element name="resourceType">

						<xsl:text>DLESE:Physical object</xsl:text>

					</xsl:element>

				</xsl:element>

			</xsl:when>

			<xsl:otherwise>



<!--	variable for ADN educational.resourceTypes.resourceType - in case the only value is the default value-->

				<xsl:variable name="allresourceTypes">

					<xsl:for-each select="d:educational/d:resourceTypes/d:resourceType">

						<xsl:value-of select="."/>

					</xsl:for-each>

				</xsl:variable>

	

<!--test for the presence of content in DLESE-IMS general.keywords; do this to prevent the ADN:general.keywords tag from appearing if it doesn't need to-->

				<xsl:choose >

					<xsl:when test="string($allresourceTypes)='DLESE:To be supplied'"/>

					<xsl:otherwise>

						<xsl:element name="resourceTypes" namespace="http://www.dlese.org/Metadata/briefmeta">

							<xsl:apply-templates select="d:educational/d:resourceTypes/d:resourceType"/>

							<!--see template RESOURCETYPE -->

						</xsl:element>

					</xsl:otherwise>

				</xsl:choose>

			</xsl:otherwise>

		</xsl:choose>



<!--end briefRecord-->

		</xsl:element>

	</xsl:template>





<!--E. TEMPLATES TO APPLY (alphabetical order)-->

<!--**********************************************************-->

<!--1. GRADERANGE writes ADN grade range information-->

<!--2. RESOURCETYPE writes ADN resource types-->

<!--3. SUBJECT template mode=DLESE writes ADN subject but removes the leading 'DLESE:'-->

<!--4. SUBJECT template mode=KEYWORDS writes ADN keywords -->



<!--1. GRADERANGE template-->

	<xsl:template match="d:gradeRange">

		<xsl:choose>

			<xsl:when test="contains(., 'supplied') or contains(., 'Not applicable')"/>

			<xsl:otherwise>

				<xsl:element name="gradeRange" namespace="http://www.dlese.org/Metadata/briefmeta">

					<xsl:value-of select="."/>

				</xsl:element>

			</xsl:otherwise>

		</xsl:choose>

	</xsl:template>





<!--2. RESOURCETYPE template-->

	<xsl:template match="d:resourceType">

		<xsl:choose>

			<xsl:when test="contains(.,'supplied')"/>

			<xsl:otherwise>

				<xsl:element name="resourceType" namespace="http://www.dlese.org/Metadata/briefmeta">

					<xsl:value-of select="."/>

				</xsl:element>

			</xsl:otherwise>

		</xsl:choose>

	</xsl:template>



<!--3. SUBJECT template mode=DLESE-->

	<xsl:template match="d:subject" mode="DLESE">

		<xsl:choose>

			<xsl:when test="contains(.,'supplied')"/>

			<xsl:otherwise>

				<xsl:element name="subject" namespace="http://www.dlese.org/Metadata/briefmeta">

					<xsl:value-of select="."/>

				</xsl:element>

			</xsl:otherwise>

		</xsl:choose>

</xsl:template>			



<!--4. SUBJECT template mode=KEYWORDS-->

	<xsl:template match="d:keyword" mode="keywords">

		<xsl:element name="subject" namespace="http://www.dlese.org/Metadata/briefmeta">

			<xsl:value-of select="."/>

		</xsl:element>

	</xsl:template>			



</xsl:stylesheet>

