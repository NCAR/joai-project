<?xml version="1.0"?>
<xsl:stylesheet 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xsi ="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:n="http://newsopps.dlese.org"
    xmlns:d="http://www.dlese.org/Metadata/fields"
    xmlns:dc="http://purl.org/dc/elements/1.1/"
    xmlns:nsdl_dc="http://ns.nsdl.org/nsdl_dc_v1.02/"
    xmlns:dct="http://purl.org/dc/terms/"
    xmlns:ieee="http://www.ieee.org/xsd/LOMv1p0"
    xmlns:xsd="http://www.w3.org/2001/XMLSchema"
	exclude-result-prefixes="xsi d n xsd" 
    version="1.0">
    
<!--ORGANIZATION OF THIS FILE-->
<!-- **************************************-->
<!--This file is organized into the following sections:
A. Purpose
B. License information and credits
C. Assumptions
D. Variables
E. Transformation code
F. Templates to apply (in alphabetical order)-->

<!--Date created: 2007-08-28 by Katy Ginger, DLESE Program Center, University Corporation for Atmospheric Research (UCAR)-->
<!--Last modified: 2007-08-28 by Katy Ginger-->

<!--A. PURPOSE-->
<!-- **************************************-->
<!--To transform Digital Library for Earth System Education (DLESE) news and opportunities metadata records to NSDL-DC-->


<!--B. LICENSE INFORMATION and CREDITS-->
<!-- *****************************************************-->
<!--License information:
		Copyright (c) 2002-2007
		University Corporation for Atmospheric Research (UCAR)
		P.O. Box 3000, Boulder, CO 80307, United States of America
		email: dlesesupport@ucar.edu.
		All rights reserved
These XML tranformation written in XSLT 1.0 and XPATH 1.0 are free software; you can redistribute them and/or modify them under the terms of the GNU General Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any later version.  These XML instance documents are distributed in the hope that they will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this project; if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA -->


<!--C. ASSUMPTIONS-->
<!-- **************************************-->
<!--Applies to DLESE news and opportunities metadata format, version 1.0.00 records-->
<!--Assumes content is present in required feilds and does not check for the presence of it-->


	<xsl:output method="xml" indent="yes" encoding="UTF-8"/>


<!--D. VARIABLES used throughout the transform-->
<!-- **********************************************************-->
<!--variable for accessing news and opps term definition information-->
	<xsl:variable name="vocabsURL">http://www.dlese.org/Metadata/news-opps/1.0.00/</xsl:variable>	


<!--F. TRANSFORMATION CODE-->
<!-- **********************************************************-->
	<xsl:template match="n:news-oppsRecord">
		<nsdl_dc:nsdl_dc schemaVersion="1.02.010" xmlns:nsdl_dc="http://ns.nsdl.org/nsdl_dc_v1.02/" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dct="http://purl.org/dc/terms/" xmlns:ieee="http://www.ieee.org/xsd/LOMv1p0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://ns.nsdl.org/nsdl_dc_v1.02/ http://ns.nsdl.org/schemas/nsdl_dc/nsdl_dc_v1.02.xsd">


<!--dc:title-->
<!--since title is required metadata in news-opps, no need to check to see if data is present-->
		<xsl:element name="dc:title">
			<xsl:value-of select="n:title"/>
		</xsl:element>	

<!--dc:creator, dc:publisher and dc:contributor from the contributor tag set-->
		<xsl:apply-templates select="n:contributors/n:contributor"/>
		<!--see template CONTRIBUTOR-->

<!--dc:subject - from topic, keyword, audience, diversity-->
		<!--see template DC:SUBJECT-->
		<xsl:apply-templates select="n:topics/n:topic"/>
		<xsl:apply-templates select="n:keywordsn/n:keyword"/>
		<xsl:apply-templates select="n:audiences/n:audience"/>
		<xsl:apply-templates select="n:diversities/n:diversity"/>
		<xsl:apply-templates select="n:keywords/n:keyword"/>

<!--dc:description-->
<!--since description is required metadata in news-opps, no need to check to see if data is present-->
		<xsl:element name="dc:description">
			<xsl:value-of select="n:description"/>
		</xsl:element>	
	
<!--dc:format -->

<!--dc:format - size -->
<!--news-opps metadata does not have size information-->

<!--dc:format allows the terms: text, multipart, message, application, image, audio, video or model to be used by themselves. Generally, select the mime type from the NSDL mime type list at: http://ns.nsdl.org/schemas/mime_type/mime_type_v1.00.xsd-->	
		

<!--dc:format  - mimetype - using announcementURL-->
		<xsl:element name="dc:format">
			<xsl:apply-templates select="n:announcementURL"/>
			<!--see template ANNOUNCEMENTURL-->
		</xsl:element>

<!--dc:type-->

<!--dc:type - plain-->
<!--no vocabulary mapping-->
<!--using news_opps announcement type data verbatim-->
		<xsl:apply-templates select="n:announcements/n:announcement"/>
		<!--see template DC:TYPE-->

<!--for vocabulary mapping, make a variable containing all the announcement types and then test them-->
<!--since just applied template above, current node is n:announcements/n:announcement; so just just '.'-->
		<xsl:variable name="allTypes">
			<xsl:for-each select=".">
				<xsl:value-of select="."/>
			</xsl:for-each>
		</xsl:variable>
		
<!--dc:type using DCMI types-->
<!--vocabulary mapping is necessary-->
<!--using the Dublin Core Metadata Initiative (DCMI) type vocabulary at: http://dublincore.org/schemas/xmls/qdc/2004/06/14/dcmitype.xsd, the best mapping for the news and opps announcement type is to use Text or Event-->

		<!--dc:type: Event-->
		<xsl:if test="contains($allTypes, 'Workshop') or contains($allTypes, 'Conference')">
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">dct:DCMIType</xsl:attribute>
				<xsl:text>Event</xsl:text>
			</xsl:element>
		</xsl:if>

		<!--dc:type: Text-->
		<xsl:if test="contains($allTypes, 'Award, recognition or scholarship') or contains($allTypes, 'Call for participation') or contains($allTypes, 'Careers') or contains($allTypes, 'Job') or contains($allTypes, 'News') or contains($allTypes, 'Learning or research opportunity') or contains($allTypes, 'Grant or proposal')">
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">dct:DCMIType</xsl:attribute>
				<xsl:text>Text</xsl:text>
			</xsl:element>
		</xsl:if>


<!--dc:type using nsdl_dc:NSDLType-->
<!--vocabulary mapping is necessary-->
<!--using the NSDL_DC type vocabulary at: http://ns.nsdl.org/schemas/nsdltype/nsdltype_v1.00.xsd, map the news and opps announcement type; the NSDL vocab is a hierarchy-->
		
		<!--Reference Material-->
		<xsl:if test="contains($allTypes, 'Careers') or contains($allTypes, 'Grant or proposal')">
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Reference Material</xsl:text>
			</xsl:element>
		</xsl:if>

		<!--Career Information-->
		<xsl:if test="contains($allTypes, 'Careers')">
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Career Information</xsl:text>
			</xsl:element>
		</xsl:if>

		<!--Proposal-->
		<xsl:if test="contains($allTypes, 'Grant or proposal')">
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Proposal</xsl:text>
			</xsl:element>					
		</xsl:if>

		<!--Event-->
		<xsl:if test="contains($allTypes, 'Award, recognition or scholarship') or contains($allTypes, 'Call for participation') or contains($allTypes, 'Conference') or contains($allTypes, 'Job') or contains($allTypes, 'News') or contains($allTypes, 'Learning or research opportunity') or contains($allTypes, 'Workshop')">
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Event</xsl:text>
			</xsl:element>
		</xsl:if>

		<!--Award/Recognition/Scholarship -->
		<xsl:if test="contains($allTypes, 'Award, recognition or scholarship')">
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Award/Recognition/Scholarship</xsl:text>
			</xsl:element>										
		</xsl:if>

		<!--Call for Participation-->
		<xsl:if test="contains($allTypes, 'Call for participation')">
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Call for Participation</xsl:text>
			</xsl:element>										
		</xsl:if>

		<!--Conference-->
		<xsl:if test="contains($allTypes, 'Conference')">
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Conference</xsl:text>
			</xsl:element>										
		</xsl:if>

		<!--Job-->
		<xsl:if test="contains($allTypes, 'Job')">
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Job</xsl:text>
			</xsl:element>										
		</xsl:if>

		<!--News-->
		<xsl:if test="contains($allTypes, 'News')">
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>News</xsl:text>
			</xsl:element>										
		</xsl:if>

		<!--Learning/Research Opportunity-->
		<xsl:if test="contains($allTypes, 'Learning or research opportunity')">
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Learning/Research Opportunity</xsl:text>
			</xsl:element>										
		</xsl:if>

		<!--Workshop-->
		<xsl:if test="contains($allTypes, 'Workshop')">
			<xsl:element name="dc:type">
				<xsl:attribute name="xsi:type">nsdl_dc:NSDLType</xsl:attribute>
				<xsl:text>Workshop</xsl:text>
			</xsl:element>										
		</xsl:if>


<!--dc:identifier - using news-opps id numbers-->
<!--news-opps records have a DLESE catalog record numbers that can be used as an indentifier-->
<!--DLESE catalog record numbers do not have much meaning outside DLESE; do not use it as content for the identifier field; use announcement URL instead-->
<!--	<xsl:element name="dc:identifier">
			<xsl:value-of select="n:recordID"/>
		</xsl:element> -->

<!--dc:identifier - using news-opps urls-->
<!--simple Dublin Core does not deal with the attribute dct:URI, so no worry about spaces in urls-->
		<xsl:element name="dc:identifier">
			<xsl:value-of select="n:announcementURL"/>
		</xsl:element>

<!--dc:source-->		
<!--news-opps has a source element under contributors-->
<!--news-opps source is not being used as of 2006-03-31; therefore do not create the dc:source element because there is no content-->

<!--dc:language-->
		<xsl:element name="dc:language">
			<xsl:value-of select="n:language/@resource"/>
		</xsl:element>

<!--dc:rights-->
<!--new-opps does not collect rights information; therefore do not create the dc:rights element because there is no content-->

	
<!--dc:coverage and dc:spatial general information-->
<!--use news-opps location information-->
		<xsl:apply-templates select="n:locations/n:location"/>
		<!--see template LOCATION-->


<!--dc:coverage time information-->
 		<xsl:apply-templates select="n:otherDates/n:otherDate"/>
		<!--see template OTHERDATE-->
		

<!--end nsdl_dc:nsdl_dc-->
		</nsdl_dc:nsdl_dc>
	</xsl:template>


<!--F. TEMPLATES TO APPLY-->
<!--**********************************************************-->
<!--organized in the following alphabetical order-->
<!--1. ANNOUNCEMENTURL - writes DC format mimetype using the announcement URL-->
<!--2. CONTRIBUTOR selects DC creator, publisher or contributor based on the news and opps contributor role-->
<!--3. DC:SUBJECT writes DC subject from keywords, audiene, topic, diversity and announcement-->
<!--3a. DC:TYPE writes DC type from announcement -->
<!--4. LOCATION writes DC coverage by translating the encoded country codes into formal country names -->
<!--5. ORGANIZATION - writes organization information for DC creator, publisher or contributor-->
<!--6. OTHERDATE writes DC coverage date information plus any descriptive information associated with the dates-->
<!--7. PERSON writes person information for DC creator, publisher or contributor-->
<!--8. SOURCE writes source information for DC creator, publisher or contributor-->

<!--1. ANNOUNCEMENTURL template  writes mime type based on the extenstion of the URL-->
<!--makes an assumption of text/html if no match is found-->
	<xsl:template match="n:announcementURL">
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

<!--2. CONTRIBUTOR template-->
	<xsl:template match="n:contributor">
		<xsl:choose>
			<xsl:when test="./@role='Contact'">
				<xsl:element name="dc:contributor">
					<xsl:apply-templates select="n:person"/>
					<xsl:apply-templates select="n:organization"/>
					<xsl:apply-templates select="n:source"/>
				</xsl:element>
			</xsl:when>

			<xsl:when test="./@role='Author'">
				<xsl:element name="dc:creator">
					<xsl:apply-templates select="n:person"/>
					<xsl:apply-templates select="n:organization"/>
					<xsl:apply-templates select="n:source"/>
				</xsl:element>
			</xsl:when>

			<xsl:when test="./@role='Publisher' or ./@role='Sponsor' or ./@role='Newsfeed' ">
				<xsl:element name="dc:publisher">
					<xsl:apply-templates select="n:person"/>
					<xsl:apply-templates select="n:organization"/>
					<xsl:apply-templates select="n:source"/>
				</xsl:element>
			</xsl:when>
		</xsl:choose>
	</xsl:template>			

<!--3. DC:SUBJECT template-->
	<xsl:template match="n:keyword | n:audience | n:topic | n:diversity">
		<xsl:element name="dc:subject">
			<xsl:value-of select="."/>
		</xsl:element>
	</xsl:template>			

<!--3a. DC:TYPE template-->
	<xsl:template match="n:announcement">
		<xsl:element name="dc:type">
			<xsl:value-of select="."/>
		</xsl:element>
	</xsl:template>			


<!--4. LOCATION template-->
	<xsl:template match="n:location">
		<xsl:variable name="countryCode">
			<xsl:value-of select="."/>
		</xsl:variable>
		<xsl:variable name="city">
			<xsl:value-of select="./@city"/>
		</xsl:variable>
		<xsl:for-each select="document(concat($vocabsURL, 'fields/location-no-fields-en-us.xml') )/d:metadataFieldInfo/d:field/d:terms/d:termAndDeftn">
			<xsl:variable name="vocabTerm">
				<xsl:value-of select="./@vocab" /> 
			</xsl:variable>
			<xsl:variable name="country">
				<xsl:value-of disable-output-escaping="yes" select="."/>
			</xsl:variable>
			<xsl:if test="$vocabTerm = $countryCode">
				<xsl:element name="dc:coverage">
					<xsl:choose>
						<xsl:when test="string-length($city) > 0">
							<xsl:value-of disable-output-escaping="no" select="concat($city, ', ', $country)" /> 
						</xsl:when>
						<xsl:otherwise>
							<xsl:value-of disable-output-escaping="no" select="$country" /> 
						</xsl:otherwise>
					</xsl:choose>
				</xsl:element>
			</xsl:if>
		</xsl:for-each>
	</xsl:template>

<!--5. ORGANIZATION template-->
	<xsl:template match="n:organization">
		<xsl:choose>
			<xsl:when test="string-length(n:instDept)>0">
				<xsl:value-of select="concat(n:instDept,', ',n:instName)"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="n:instName"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>			

<!--6. OTHERDATE template-->
	<xsl:template match="n:otherDate">
		<xsl:element name="dc:coverage">
			<xsl:choose>
				<xsl:when test="contains(./@type, 'eventStart')">
					<xsl:choose>
						<xsl:when test="string-length(./@desc) > 0">
							<xsl:value-of select="concat(./@descr, ' ', .)"/>
						</xsl:when>
						<xsl:otherwise>
							<xsl:value-of select="concat('Starts ', .)"/>						
						</xsl:otherwise>
					</xsl:choose>
				</xsl:when>

				<xsl:when test="contains(./@type, 'eventStop')">
					<xsl:choose>
						<xsl:when test="string-length(./@desc) > 0">
							<xsl:value-of select="concat(./@descr, ' ', .)"/>						
						</xsl:when>
						<xsl:otherwise>
							<xsl:value-of select="concat('Stops ', .)"/>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:when>

				<xsl:when test="contains(./@type, 'applyBy')">
					<xsl:choose>
						<xsl:when test="string-length(./@desc) > 0">
							<xsl:value-of select="concat(./@descr, ' ', .)"/>
						</xsl:when>
						<xsl:otherwise>
							<xsl:value-of select="concat('Apply by ', .)"/>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:when>

				<xsl:when test="contains(./@type, 'due')">
					<xsl:choose>
						<xsl:when test="string-length(./@desc) > 0">
							<xsl:value-of select="concat(./@descr, ' ', .)"/>
						</xsl:when>
						<xsl:otherwise>
							<xsl:value-of select="concat('Due ', .)"/>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:when>

			</xsl:choose>
		</xsl:element>
	</xsl:template>			

<!--7. PERSON template-->
	<xsl:template match="n:person">
		<xsl:value-of select="concat(n:nameFirst,' ',n:nameLast)"/>
	</xsl:template>			

<!--8. SOURCE template -->
	<xsl:template match="n:source">
		<xsl:value-of select="."/>
	</xsl:template>			

</xsl:stylesheet>