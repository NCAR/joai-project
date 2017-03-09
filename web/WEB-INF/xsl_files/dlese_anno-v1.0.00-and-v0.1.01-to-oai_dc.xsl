<?xml version="1.0"?>

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:a="http://www.dlese.org/Metadata/annotation" 

xmlns:b="http://www.annotation.dlese"

xmlns:dc="http://purl.org/dc/elements/1.1/" 

exclude-result-prefixes=" xsi a b" 

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

<!--To transform the Digital Library for Earth System Education (DLESE) annotation metadata records to simple Dublin Core that uses the Open Archives Initiative (OAI) namespace-->





<!--B. LICENSE INFORMATION and CREDITS-->

<!-- *****************************************************-->

<!--Date created: 2006-03-31 by Katy Ginger, University Corporation for Atmospheric Research (UCAR)-->

<!--Last modified: 2006-04-11 by Katy Ginger-->

<!--License information:

		Copyright (c) 2007 Digital Learning Sciences

		University Corporation for Atmospheric Research (UCAR)

		P.O. Box 3000, Boulder, CO 80307, United States of America

		All rights reserved

This XML tranformation, written in XSLT 1.0 and XPATH 1.0, are free software; you can redistribute them and/or modify them under the terms of the GNU General Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any later version.  These XML instance documents are distributed in the hope that they will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this project; if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA -->





<!--C. ASSUMPTIONS mode = one-->

<!-- **************************************-->

<!--Applies to DLESE annotation metadata format, version 1.0.00 records-->

<!--Assumes content is present in annotation required fields and does not check for the presence of it-->

<!--Assumes annotations are textual unless otherwise specified in the status attribute-->

<!--Content for DC identifier is always written following a progression of logic; the end result may sometimes not be a URL but rather a local catalog record id number that has meaning locally-->

<!--Content for DC identifier, is always written following a progression of logic; the end result may sometimes just be a local catalog record id number that has meaning locally-->

<!--Content for DC format is only written if annotation status or annotation content URL exists-->



<!--C. ASSUMPTIONS mode = zero-->

<!-- **************************************-->

<!--Applies to DLESE annotation metadata format, version 0.1.01 records-->

<!--Assumes content is present in annotation required feilds and does not check for the presence of it-->

<!--Assumes annotations are textual unless otherwise specified in the status attribute-->

<!--Content for DC identifier is always written following a progression of logic; the end result may sometimes not be a URL but rather a local catalog record id number that has meaning locally-->

<!--Content for DC identifier, is always written following a progression of logic; the end result may sometimes just be a local catalog record id number that has meaning locally-->

<!--Content for DC format is only written if annotation status or annotation content URL exists-->





	<xsl:output method="xml" indent="yes" encoding="UTF-8"/>





<!--D. VARIABLES used throughout the transform-->

<!-- *****************************************************-->

<!--variables for accessing DLESE id prefixes files-->

<xsl:variable name="DDSWSID">http://www.dlese.org/dds/services/ddsws1-0?verb=GetRecord&amp;id=</xsl:variable>	



	<xsl:template match="*|/">

		<xsl:apply-templates select="b:annotationRecord" mode="zero"/>

		<xsl:apply-templates select="a:annotationRecord" mode="one"/>

	</xsl:template>







<!--E. TRANSFORMATION CODE mode one-->

<!-- **************************************-->

	<xsl:template match="a:annotationRecord" mode="one">

		<xsl:element name="oai_dc:dc" namespace="http://www.openarchives.org/OAI/2.0/oai_dc/">

			<xsl:attribute name="xsi:schemaLocation">http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd</xsl:attribute>



<!--dc:title-->

<!--title is not required metadata, check to see if data is present-->

			<xsl:if test="string-length(a:annotation/a:title) > 0">

				<xsl:element name="dc:title">

					<xsl:value-of select="a:annotation/a:title"/>

				</xsl:element>

			</xsl:if>

	

<!--dc:publisher from the service name -->

			<xsl:element name="dc:publisher">

				<xsl:value-of select="a:service/a:name"/>

			</xsl:element>

			

<!--dc: publisher, dc:creator and dc:contributor from contributor-->

			<xsl:apply-templates select="a:annotation/a:contributors/a:contributor" mode="one"/>

			<!--see template CONTRIBUTOR-->

	

<!--dc:subject - no annotation element really fits, not including dc:subject -->

	



<!--dc:description-->

<!--annotation fields: description, rating content URL and context URL are used to construct data for DC description-->

<!--content is written for DC description based on the following logic-->



<!--Case 1. content URL exists and context URL does not exist-->

<!--means DC identifier is the content URL; do not use the content URL in description-->

<!--case 1a: description/rating exists: DC description data is then description/rating and a constructed URL from the annotation itemID-->

<!--case 1b: description/rating does not exist: DC description data is then a constructed URL from the annotation itemID-->



<!--Case 2. content URL exists and context URL exists-->

<!--means DC identifier is the content URL; do not use the content URL in DC description-->

<!--case 2a: description/rating exists: DC description data is then description/rating and the context URL-->

<!--case 2b: description/rating does not exist: DC description data is then the context URL-->



<!--Case 3. content URL does not exist and context URL does not exist-->

<!--means DC identifier is a constructed URL; do not use the constructed URL in DC description-->

<!--case 3a: description/rating exists: DC description data is then the description/rating only-->

<!--case 3b: description/rating does not exist: case can't happen since either description/rating or content URL is required annotation metadata-->



<!--Case 4. content URL does not exist and context URL does exist-->

<!--means DC identifier is the context URL; do not use the context URL in DC description-->

<!--case 4a: description/rating exists: DC description data is then description/rating and a constructed URL from the annotation itemID-->

<!--case 4b: description/rating does not exist: case can't happen since either description/rating or content URL is required annotation metadata-->



<!--NOTE: Any annotation itemID that is not a DLESE ADN or news and opportunities record nor a URL will just be a copied id number. This can affect any of the cases that use a constructed URL from the annotation itemID-->



			<xsl:element name="dc:description">



				<xsl:choose>

<!--Case 1: content URL exists and context URL does not exist-->

					<xsl:when test="string-length(a:annotation/a:content/a:url) > 0 and string-length(a:annotation/a:context) =0">

						<xsl:if test="string-length(a:annotation/a:content/a:description)>0">

							<xsl:value-of select="concat(a:annotation/a:content/a:description, ' ')"/>

						</xsl:if>

						<xsl:if test="string-length(a:annotation/a:content/a:rating)>0">

							<xsl:value-of select="concat('This resource has a ', a:annotation/a:content/a:rating, ' rating. ')"/>

						</xsl:if>

						<xsl:apply-templates select="a:itemID" mode="one">

							<xsl:with-param name="leaderString">

								<xsl:text>The resource being annotated is: </xsl:text>

							</xsl:with-param>

						</xsl:apply-templates>

					</xsl:when>



<!--Case 2: content URL exists and context URL exists-->

					<xsl:when test="string-length(a:annotation/a:content/a:url) > 0 and string-length(a:annotation/a:context) >0">

						<xsl:if test="string-length(a:annotation/a:content/a:description)>0">

							<xsl:value-of select="concat(a:annotation/a:content/a:description, ' ')"/>

						</xsl:if>

						<xsl:if test="string-length(a:annotation/a:content/a:rating)>0">

							<xsl:value-of select="concat('This resource has a ', a:annotation/a:content/a:rating, ' rating. ')"/>

						</xsl:if>

						<xsl:value-of select="concat('The page of the resource being annotated is: ', a:annotation/a:context)"/>

					</xsl:when>



<!--Case 3: content URL does not exist and context URL does not exist-->

					<xsl:when test="string-length(a:annotation/a:content/a:url) = 0 and string-length(a:annotation/a:context) =0">

						<xsl:if test="string-length(a:annotation/a:content/a:description)>0">

							<xsl:value-of select="concat(a:annotation/a:content/a:description, ' ')"/>

						</xsl:if>

						<xsl:if test="string-length(a:annotation/a:content/a:rating)>0">

							<xsl:value-of select="concat('This resource has a ', a:annotation/a:content/a:rating, ' rating. ')"/>

						</xsl:if>

					</xsl:when>



<!--Case 4: content URL does not exist and context URL exists-->

					<xsl:when test="string-length(a:annotation/a:content/a:url) = 0 and string-length(a:annotation/a:context) >0">

						<xsl:if test="string-length(a:annotation/a:content/a:description)>0">

							<xsl:value-of select="concat(a:annotation/a:content/a:description, ' ')"/>

						</xsl:if>

						<xsl:if test="string-length(a:annotation/a:content/a:rating)>0">

							<xsl:value-of select="concat('This resource has a ', a:annotation/a:content/a:rating, ' rating. ')"/>

						</xsl:if>

						<xsl:apply-templates select="a:itemID" mode="one">

							<xsl:with-param name="leaderString">

								<xsl:text>The resource being annotated is: </xsl:text>

							</xsl:with-param>

						</xsl:apply-templates>

					</xsl:when>

				</xsl:choose>			



			</xsl:element>





<!--dc:format - size -->

<!--annotation metadata does not have size information-->





<!--dc:format-->

<!--use the annotation fields of format and content URL to determine mimetype-->

<!--if annotation format and content URL are not present, then DC:format - mimetype is not written-->



<!--dc:format - mimetype - using annotation format-->

<!--dc:format allows the terms: text, multipart, message, application, image, audio, video or model to be used by themselves. -->	

<!--if format is empty do not write DC format using this method-->

			<xsl:if test="string-length(a:annotation/a:format) > 0">

				<xsl:element name="dc:format">

					<xsl:choose>

						<xsl:when test="contains(., 'Text')">

								<xsl:text>text</xsl:text> 

						</xsl:when>

						<xsl:when test="contains(., 'Audio')">

								<xsl:text>audio</xsl:text> 

						</xsl:when>

						<xsl:when test="contains(., 'Graphical')">

							<xsl:text>image</xsl:text> 

						</xsl:when>

						<xsl:when test="contains(., 'Video')">

							<xsl:text>video</xsl:text>

						</xsl:when>	

					</xsl:choose>

				</xsl:element>

			</xsl:if>



			

	

<!--dc:format  - mimetype using content URL file extentsion-->

<!--if content URL is empty do not write DC format using this method-->

<!--if content URL has data but falls through the tests below, assume a mime type of text/html-->

			<xsl:if test="string-length(a:annotation/a:content/a:url) > 0">

				<xsl:element name="dc:format">

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

				</xsl:element>

			</xsl:if>





<!--dc:type using the format field to map to the Dublin Core Metadata Initiative (DCMI) type vocabulary-->

<!--DCMI type vocabulary being use: http://dublincore.org/schemas/xmls/qdc/2006/01/06/dcmitype.xsd-->

<!--if format is empty do not write DC type using the Dublin Core vocabulary-->

			<xsl:if test="string-length(a:annotation/a:format) > 0">

				<xsl:element name="dc:type">

					<xsl:choose>

						<xsl:when test="contains(., 'Text')">

								<xsl:text>Text</xsl:text> 

						</xsl:when>

						<xsl:when test="contains(., 'Audio')">

								<xsl:text>Sound</xsl:text> 

						</xsl:when>

						<xsl:when test="contains(., 'Graphical')">

							<xsl:text>Image</xsl:text> 

						</xsl:when>

						<xsl:when test="contains(., 'Video')">

							<xsl:text>MovingImage</xsl:text>

						</xsl:when>	

					</xsl:choose>

				</xsl:element>

			</xsl:if>



<!--dc:type using annotation type verbatim-->

			<xsl:element name="dc:type">

				<xsl:value-of select="a:annotation/a:type"/>

			</xsl:element>



<!--dc:date using date attribute on contributor-->

<!--annotation requires the date attribute on contributor; but there may be multiple contriubtors-->

<!--the following method writes DC date using the contributor tag set with the first occurrence of date-->

			<xsl:element name="dc:date">

				<xsl:value-of select="a:annotation/a:contributors/a:contributor/@date"/>

			</xsl:element>



<!--dc:identifier-->

<!--DLESE and other catalog record number ids do not have meaning as stand alone text except when used locally. Therefore convert the DLESE catalog record number id to a DLESE web address. This web address will not work for identifiers that reference resources by catalog record number ids that are not in DLESE-->

<!--content is written for DC identifier with the following priority-->

<!--1. use annotation content URL first-->

<!--2. use annotation context URL second (assuming no content URL exists)-->

<!--3. if annotaiton item ID is a DLESE id number, that has an ADN metadata record, construct a URL to the DLESE full description(assuming no content or context URL exists)-->

<!--4. if the annotation item ID is not a DLESE id number, then just copy the item id (assuming no content or context URL exists)-->

			<xsl:element name="dc:identifier">

				<xsl:choose>

					<xsl:when test="string-length(a:annotation/a:content/a:url) > 0">

						<xsl:value-of select="a:annotation/a:content/a:url"/>

					</xsl:when>

					<xsl:when test="string-length(a:annotation/a:context) > 0">

						<xsl:value-of select="a:annotation/a:context"/>

					</xsl:when>

					<xsl:otherwise>

						<!--whenever a content or context URL does not exist process the itemID-->

						<xsl:apply-templates select="a:itemID" mode="one">

							<xsl:with-param name="leaderString">

								<xsl:text></xsl:text>

							</xsl:with-param>

						</xsl:apply-templates>

					</xsl:otherwise>

				</xsl:choose>			

			</xsl:element>



<!--dc:source-->

<!-- DLESE annotation 1.0 does not have source, therefore not using source at all-->



<!--dc:language-->

<!-- annotation 1.0.00 does not collect language information. -->





<!--dc:rights-->

<!--annotation 1.0.00 does not collect rights information-->



<!--dc:coverage and dc:spatial general information-->

<!-- annotation 1.0.00 does not collect coverage or spatial information-->



<!--end oai_dc:dc-->

		</xsl:element>

	</xsl:template>



<!--F. TEMPLATES TO APPLY-->

<!--*********************************-->

<!--organized in alphabetical order-->

<!--1. CONTRIBUTOR selects DC creator, publisher or contributor based on the role of the contributor-->

<!--2. ITEMID - constructs data for DC identifier and DC description using annotation itemID-->

<!--3. ORGANIZATION - writes the organization information for DC creator, publisher or contributor-->

<!--4. PERSON writes person information for DC creator, publisher or contributor-->



<!--1. CONTRIBUTOR template-->

<!-- Contributor is required data; however contributors may choose not to share their information, so need to check for share="true" -->

<!-- Contact does not contribute to the content of the resource, so not using -->

	<xsl:template match="a:contributor" mode="one">

		<xsl:if test="./@share='true'">

			<xsl:choose>

				<xsl:when test="./@role='Author'">

					<xsl:element name="dc:creator">

						<xsl:apply-templates select="a:person" mode="one"/>

						<xsl:apply-templates select="a:organization" mode="one"/>

					</xsl:element>

				</xsl:when>

				

				<xsl:when test="./@role='Contributor' or ./@role='College educator' or ./@role='Educator' or ./@role='Elementary educator' or ./@role='Evaluator' or ./@role='High school educator' or ./@role='Librarian' or ./@role='Middle school educator' or ./@role='Scientist' or ./@role='Student'">

					<xsl:element name="dc:contributor">

						<xsl:apply-templates select="a:person" mode="one"/>

						<xsl:apply-templates select="a:organization" mode="one"/>

					</xsl:element>

				</xsl:when>

				

				<xsl:when test="./@role='Publisher' ">

					<xsl:element name="dc:publisher">

						<xsl:apply-templates select="a:person" mode="one"/>

						<xsl:apply-templates select="a:organization" mode="one"/>

					</xsl:element>

				</xsl:when>

			</xsl:choose>

		</xsl:if>

	</xsl:template>





<!--2. ITEMID template-->

<xsl:template match="a:itemID" mode="one">

<!--template processes the itemID in order to generate content for DC identifier and DC description-->

<!--the processing logic is:-->

<!--make an output variable inDLESE to capture the output if annotation itemID is in DLESE-->

<!--if annotation itemID is in DLESE (either an ADN record or a news-opps record), then contruct a URL using the full description pages for ADN or news-opps-->

<!--if the annotation itemID is not in DLESE, the output variable inDLESE does not have content; then construct output that just uses the item id (this means the item id has local meaning only and the viability of the DC identifier or DC description outside of local use is not really feasible unless for some reason annotation itemID is already a URL-->



<!--passed in information; param must be declared before variables-->

	<xsl:param name="leaderString"/>

	

<!--use a variable to know which is current processing node (.) and to make comparisons-->

	<xsl:variable name="id">

		<xsl:value-of select="." />

	</xsl:variable>



<!--output variable-->

	<xsl:variable name="inDLESE">



<!--checking to see if in DLESE as a adn record-->

<!--reading adn records in webservices-->					

		<xsl:variable name="adn">

			<xsl:value-of select="document(concat($DDSWSID, $id))//metaMetadata/catalogEntries/catalog/@entry" />

		</xsl:variable>

		<xsl:if test="$id = $adn">

			<xsl:value-of select="concat($leaderString, 'http://www.dlese.org/dds/catalog_', $id, '.htm' )"/>

		</xsl:if>

	

<!--checking to see if in DLESE as a news-opps record-->

<!--reading news-opps records in webservices-->					

		<xsl:variable name="news">

			<xsl:value-of select="document(concat($DDSWSID, $id))//recordID" />

		</xsl:variable>

		<xsl:if test="$id = $news">

			<xsl:value-of select="concat($leaderString, 'http://www.dlese.org/news_opportunities/description_full.jsp?id=', $id, '&amp;q=' )"/>

		</xsl:if>

	</xsl:variable>



	<xsl:choose>

		<xsl:when test="string-length($inDLESE) = 0">

			<xsl:value-of select="concat($leaderString, $id)"/>

		</xsl:when>

		<xsl:otherwise>

			<xsl:value-of select="$inDLESE"/>

		</xsl:otherwise>

	</xsl:choose>

</xsl:template>



<!--3. ORGANIZATION template-->

	<xsl:template match="a:organization" mode="one">

		<xsl:choose>

			<xsl:when test="string-length(a:instDept)>0">

				<xsl:value-of select="concat(a:instDept,', ',a:instName)"/>

			</xsl:when>

			<xsl:otherwise>

				<xsl:value-of select="a:instName"/>

			</xsl:otherwise>

		</xsl:choose>

	</xsl:template>



<!--4. PERSON template-->

	<xsl:template match="a:person" mode="one">

		<xsl:value-of select="concat(a:nameFirst,' ',a:nameLast)"/>

	</xsl:template>

<!--end of templates-->	  







<!--E. TRANSFORMATION CODE mode zero-->

<!-- ************************************************-->

  <xsl:template match="b:annotationRecord" mode="zero">

		<xsl:element name="oai_dc:dc" namespace="http://www.openarchives.org/OAI/2.0/oai_dc/">

			<xsl:attribute name="xsi:schemaLocation">http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd</xsl:attribute>	





<!--dc:title-->

<!--title is not required metadata; check to see if data is present-->

			<xsl:if test="string-length(b:item/b:title) > 0">

				<xsl:element name="dc:title">

					<xsl:value-of select="b:item/b:title"/>

				</xsl:element>	

			</xsl:if>





<!--dc:publisher from the service name -->

			<xsl:element name="dc:publisher">

				<xsl:value-of select="b:service/b:name"/>

			</xsl:element>	





<!--dc: publisher, dc:creator and dc:contributor from contributor-->

			<xsl:apply-templates select="b:item/b:contributors/b:contributor" mode="zero"/>

			<!--see template CONTRIBUTOR-->



<!--dc:subject - no annotation element really fits, not including dc:subject -->





<!--dc:description-->

<!--annotation fields: description, content URL and context URL are used to construct data for DC description-->

<!--content is written for DC description based on the following logic-->



<!--Case 1. content URL exists and context URL does not exist-->

<!--case 1a: description exists: DC description data is then description and a constructed URL from the annotation itemID-->

<!--case 1b: description does not exist: DC description data is then a constructed URL from the annotation itemID -->



<!--Case 2. content URL exists and context URL exists-->

<!--case 2a: description exists: DC description data is then description and the context URL-->

<!--case 2b: description does not exist: DC description data is then the context URL-->



<!--Case 3. content URL does not exist and context URL does not exist-->

<!--case 3a: description exists: DC description data is then the description only-->

<!--case 3b: description does not exist: case can't happen since either description or content URL is required annotation metadata-->



<!--Case 4. content URL does not exist and context URL does exist-->

<!--case 4a: description exists: DC description data is then description and a constructed URL from the annotation itemID-->

<!--case 4b: description does not exist: case can't happen since either description or content URL is required annotation metadata-->



<!--NOTE: Any annotation itemID that is not a DLESE ADN or news and opportunities record nor a URL will just be a copied id number. This can affect any of the cases that use a constructed URL from the annotation itemID-->



			<xsl:element name="dc:description">



				<xsl:choose>

<!--Case 1: content URL exists and context URL does not exist-->

					<xsl:when test="string-length(b:item/b:content/b:url) > 0 and string-length(b:item/b:context) =0">

						<xsl:if test="string-length(b:item/b:content/b:description)>0">

							<xsl:value-of select="concat(b:item/b:content/b:description, ' ')"/>

						</xsl:if>

						<xsl:apply-templates select="b:item/b:itemID" mode="zero">

							<xsl:with-param name="leaderString">

								<xsl:text>The resource being annotated is: </xsl:text>

							</xsl:with-param>

						</xsl:apply-templates>

					</xsl:when>



<!--Case 2: content URL exists and context URL exists-->

					<xsl:when test="string-length(b:item/b:content/b:url) > 0 and string-length(b:item/b:context) >0">

						<xsl:if test="string-length(b:item/b:content/b:description)>0">

							<xsl:value-of select="concat(b:item/b:content/b:description, ' ')"/>

						</xsl:if>

						<xsl:value-of select="concat('The page of the resource being annotated is: ', b:item/b:context)"/>

					</xsl:when>



<!--Case 3: content URL does not exist and context URL does not exist-->

					<xsl:when test="string-length(b:item/b:content/b:url) = 0 and string-length(b:item/b:context) =0">

						<xsl:value-of select="b:item/b:content/b:description"/>

					</xsl:when>



<!--Case 4: content URL does not exist and context URL exists-->

					<xsl:when test="string-length(b:item/b:content/b:url) = 0 and string-length(b:item/b:context) >0">

						<xsl:if test="string-length(b:item/b:content/b:description)>0">

							<xsl:value-of select="concat(b:item/b:content/b:description, ' ')"/>

						</xsl:if>

						<xsl:apply-templates select="b:item/b:itemID" mode="zero">

							<xsl:with-param name="leaderString">

								<xsl:text>The resource being annotated is: </xsl:text>

							</xsl:with-param>

						</xsl:apply-templates>

					</xsl:when>

				</xsl:choose>			



			</xsl:element>







<!--dc:format - size -->

<!--annotation metadata does not have size information-->



<!--dc:format-->

<!--use the annotation fields of status and content URL to determine mimetype-->

<!--if annotation status and content URL are not present, then DC:format - mimetype is not written-->



<!--dc:format - mimetype - using status-->

<!--dc:format allows the terms: text, multipart, message, application, image, audio, video or model to be used by themselves. -->	

<!--if status is empty do not write DC format using this method-->

			<xsl:if test="string-length(b:item/b:statusOf/@status) > 0">

				<xsl:element name="dc:format">

					<xsl:choose>

						<xsl:when test="contains(b:item/b:statusOf/@status, 'Text')">

							<xsl:text>text</xsl:text> 

						</xsl:when>

						<xsl:when test="contains(b:item/b:statusOf/@status, 'Audio')">

							<xsl:text>audio</xsl:text> 

						</xsl:when>

						<xsl:when test="contains(b:item/b:statusOf/@status, 'Graphical')">

							<xsl:text>image</xsl:text> 

						</xsl:when>

					</xsl:choose>

				</xsl:element>

			</xsl:if>





<!--dc:fomat - mimetype - using content URL file extension-->

<!--if content URL is empty do not write DC format using this method-->

<!--if content URL has data but falls through the tests below, assume a mime type of text/html-->

			<xsl:if test="string-length(b:item/b:content/b:url) > 0">

				<xsl:element name="dc:format">

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

				</xsl:element>

			</xsl:if>





<!--dc:type using the status field to map to the Dublin Core Metadata Initiative (DCMI) type vocabulary-->

<!--DCMI type vocabulary being use: http://dublincore.org/schemas/xmls/qdc/2006/01/06/dcmitype.xsd-->

<!--if status is empty do not write DC type using the Dublin Core vocabulary-->

			<xsl:if test="string-length(b:item/b:statusOf/@status) > 0">

				<xsl:element name="dc:type">

					<xsl:choose>

						<xsl:when test="contains(b:item/b:statusOf/@status, 'Text')">

							<xsl:text>Text</xsl:text> 

						</xsl:when>

						<xsl:when test="contains(b:item/b:statusOf/@status, 'Audio')">

							<xsl:text>Sound</xsl:text> 

						</xsl:when>

						<xsl:when test="contains(b:item/b:statusOf/@status, 'Graphical')">

							<xsl:text>Image</xsl:text> 

						</xsl:when>

					</xsl:choose>

				</xsl:element>

			</xsl:if>

		

<!--dc:type using annotation type verbatim-->

			<xsl:element name="dc:type">

				<xsl:value-of select="b:item/b:type"/>

			</xsl:element>





<!--dc:date using date attribute on contributor-->

<!--annotation requires the date attribute on contributor; but there may be multiple contriubtors-->

<!--the following method writes DC date using the contributor tag set with the first occurrence of date-->

			<xsl:element name="dc:date">

				<xsl:value-of select="b:item/b:contributors/b:contributor/@date"/>

			</xsl:element>





<!--dc:identifier-->

<!--DLESE and other catalog record number ids do not have meaning as stand alone text except when used locally. Therefore convert the DLESE catalog record number id to a DLESE web address. This web address will not work for identifiers that reference resources by catalog record number ids that are not in DLESE-->

<!--content is written for DC identifier with the following priority-->

<!--1. use annotation content URL first-->

<!--2. use annotation context URL second (assuming no content URL exists)-->

<!--3. if annotaiton item ID is a DLESE id number, that has an ADN metadata record, construct a URL to the DLESE full description(assuming no content or context URL exists)-->

<!--4. if the annotation item ID is not a DLESE id number, then just copy the item id (assuming no content or context URL exists)-->

			<xsl:element name="dc:identifier">

				<xsl:choose>

					<xsl:when test="string-length(b:item/b:content/b:url) > 0">

						<xsl:value-of select="b:item/b:content/b:url"/>

					</xsl:when>

					<xsl:when test="string-length(b:item/b:context) > 0">

						<xsl:value-of select="b:item/b:context"/>

					</xsl:when>

					<xsl:otherwise>

						<!--whenever a content or context URL does not exist process the itemID-->

						<xsl:apply-templates select="b:item/b:itemID" mode="zero">

							<xsl:with-param name="leaderString">

								<xsl:text></xsl:text>

							</xsl:with-param>

						</xsl:apply-templates>

					</xsl:otherwise>

				</xsl:choose>			

			</xsl:element>





<!--dc:source-->	

<!-- annotation does not have source-->



<!--dc:language-->

<!-- annotation does not have language of the resource-->



<!--dc:rights-->

<!--annotation does not collect rights information-->

	

<!--dc:coverage-->

<!--annotation does not have coverage information-->



<!--end oai_dc:dc-->

		</xsl:element>

	</xsl:template>







<!--F. TEMPLATES TO APPLY-->

<!-- **************************************-->

<!--organized in alphabetical order-->

<!--1. CONTRIBUTOR selects DC creator, publisher or contributor based on the role of the contributor-->

<!--2. ITEMID - constructs data for DC identifier and DC description using annotation itemID-->

<!--3. ORGANIZATION - writes the organization information for DC creator, publisher or contributor-->

<!--4. PERSON writes person information for DC creator, publisher or contributor-->





<!--1. CONTRIBUTOR template-->

<!-- the term contact does not map to dc:publisher, dc:contributor or dc:autor; ignore term contact -->

	<xsl:template match="b:contributor" mode="zero">

		<xsl:choose>

			<xsl:when test="./@role='Author'">

				<xsl:element name="dc:creator">

					<xsl:apply-templates select="b:person" mode="zero"/>

					<xsl:apply-templates select="b:organization" mode="zero"/>

				</xsl:element>

			</xsl:when>



			<xsl:when test="./@role='Contributor' or ./@role='Editor' or ./@role='Educator' or ./@role='Evaluator' ">

				<xsl:element name="dc:contributor">

					<xsl:apply-templates select="b:person" mode="zero"/>

					<xsl:apply-templates select="b:organization" mode="zero"/>

				</xsl:element>

			</xsl:when>



			<xsl:when test="./@role='Publisher' ">

				<xsl:element name="dc:publisher">

					<xsl:apply-templates select="b:person" mode="zero"/>

					<xsl:apply-templates select="b:organization" mode="zero"/>

				</xsl:element>

			</xsl:when>

		</xsl:choose>

	</xsl:template>			





<!--2. ITEMID template-->

<xsl:template match="b:itemID" mode="zero">

<!--template processes the itemID in order to generate content for DC identifier and DC description-->

<!--the processing logic is:-->

<!--make an output variable inDLESE to capture the output if annotation itemID is in DLESE-->

<!--if annotation itemID is in DLESE (either an ADN record or a news-opps record), then contruct a URL using the full description pages for ADN or news-opps-->

<!--if the annotation itemID is not in DLESE, the output variable inDLESE does not have content; then construct output that just uses the item id (this means the item id has local meaning only and the viability of the DC identifier or DC description outside of local use is not really feasible unless for some reason annotation itemID is already a URL-->



<!--passed in information; param must be declared before variables-->

	<xsl:param name="leaderString"/>

	

<!--use a variable to know which is current processing node (.) and to make comparisons-->

	<xsl:variable name="id">

		<xsl:value-of select="." />

	</xsl:variable>



<!--output variable-->

	<xsl:variable name="inDLESE">



<!--checking to see if in DLESE as a adn record-->

<!--reading adn records in webservices-->					

		<xsl:variable name="adn">

			<xsl:value-of select="document(concat($DDSWSID, $id))//metaMetadata/catalogEntries/catalog/@entry" />

		</xsl:variable>

		<xsl:if test="$id = $adn">

			<xsl:value-of select="concat($leaderString, 'http://www.dlese.org/dds/catalog_', $id, '.htm' )"/>

		</xsl:if>

	

<!--checking to see if in DLESE as a news-opps record-->

<!--reading news-opps records in webservices-->					

		<xsl:variable name="news">

			<xsl:value-of select="document(concat($DDSWSID, $id))//recordID" />

		</xsl:variable>

		<xsl:if test="$id = $news">

			<xsl:value-of select="concat($leaderString, 'http://www.dlese.org/news_opportunities/description_full.jsp?id=', $id, '&amp;q=' )"/>

		</xsl:if>

	</xsl:variable>



	<xsl:choose>

		<xsl:when test="string-length($inDLESE) = 0">

			<xsl:value-of select="concat($leaderString, $id)"/>

		</xsl:when>

		<xsl:otherwise>

			<xsl:value-of select="$inDLESE"/>

		</xsl:otherwise>

	</xsl:choose>

</xsl:template>





<!--3. ORGANIZATION template-->

	<xsl:template match="b:organization" mode="zero">

		<xsl:choose>

			<xsl:when test="string-length(b:instDept)>0">

				<xsl:value-of select="concat(b:instDept,', ',b:instName)"/>

			</xsl:when>

			<xsl:otherwise>

				<xsl:value-of select="b:instName"/>

			</xsl:otherwise>

		</xsl:choose>

	</xsl:template>			





<!--4. PERSON template-->

	<xsl:template match="b:person" mode="zero">

		<xsl:value-of select="concat(b:nameFirst,' ',b:nameLast)"/>

	</xsl:template>			





</xsl:stylesheet>