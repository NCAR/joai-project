<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:xsi ="http://www.w3.org/2001/XMLSchema-instance"
	xmlns:d="http://adn.dlese.org"
    xmlns:c="http://collection.dlese.org"
    xmlns:dc="http://purl.org/dc/elements/1.1/"
    xmlns:dct="http://purl.org/dc/terms/"
    exclude-result-prefixes="xsi d c"
version="1.0">
<!-- This file is not used directly but the template below is copied into the 
adn-v0.6.50-to-nsdl_dc-v1.02-asn-identifiers.xsl stylesheet. -->

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
