/*
	Copyright 2017 Digital Learning Sciences (DLS) at the
	University Corporation for Atmospheric Research (UCAR),
	P.O. Box 3000, Boulder, CO 80307

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at

	http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
*/
package org.dlese.dpc.xml;

/**
 *  Utilities for working with XSL.
 *
 * @author     John Weatherley
 */
public class XSLUtils {
	
	private static final String removeNamespacesXSL =
		"<xsl:stylesheet version='1.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>" +
			"<xsl:output method='xml' indent='no'/>" +
			
			"<xsl:template match='/|comment()|processing-instruction()'>" +
				"<xsl:copy>" +
				  "<xsl:apply-templates/>" +
				"</xsl:copy>" +
			"</xsl:template>" +
			
			"<xsl:template match='*'>" +
				"<xsl:element name='{local-name()}'>" +
				  "<xsl:apply-templates select='@*|node()'/>" +
				"</xsl:element>" +
			"</xsl:template>" +
			
			"<xsl:template match='@*'>" +
				"<xsl:attribute name='{local-name()}'>" +
				  "<xsl:value-of select='.'/>" +
				"</xsl:attribute>" +
			"</xsl:template>" +
		"</xsl:stylesheet>";

		
	// This one works but throws an error "The child axis starting at an attribute node will never select anything" from the saxon xslt 2 processor
	private static final String removeNamespacesXSLOFF =
		"<xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform' version='1.0' >\n" +
		
			"<xsl:template match='@*' >\n" +
				"<xsl:attribute name='{local-name()}' >\n" +
					"<xsl:value-of select='.' />\n" +
				"</xsl:attribute>\n" +
				"<xsl:apply-templates/>\n" +
			"</xsl:template>\n" +
			
			"<xsl:template match ='*' >\n" +
				"<xsl:element name='{local-name()}' >\n" +
					"<xsl:apply-templates select='@* | node()' />\n" +
				"</xsl:element>\n" +
			"</xsl:template>\n" +
			
		"</xsl:stylesheet>";		

	/**
	 *  Gets an XSL style sheet that removes all namespaces from an XML document. With namespaces removed, the
	 *  XPath syntax necessary to work with the document is greatly simplified.
	 *
	 * @return    An XSL style sheet that removes all namespaces from an XML document
	 */
	public final static String getRemoveNamespacesXSL() {
		return removeNamespacesXSL;
	}
}

