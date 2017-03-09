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
package org.dlese.dpc.util;

import org.htmlparser.Node;
import org.htmlparser.tags.*;
import org.htmlparser.Parser;
import org.htmlparser.util.*;
import org.htmlparser.visitors.TagFindingVisitor;
import org.htmlparser.visitors.TextExtractingVisitor;

/**
 *  The HTMLParser class contains methods which allow an HTML document to be parsed. These
 *  methods allow text in the document to be extracted, as well as the contents of Meta tags
 *  Header (h1 , h2, h3, .. h6) tags, the Title tag, all the links in the page etc. 
 *
 * Example html document at http://www.abc.org: (for help with explaining the methods in this API)
 * <html>
 * <head>
 * <title> ABC.ORG's MAIN PAGE</title>
 * <meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
 * <META NAME="keywords" CONTENT="DLESE, digital, library, earth, system, education, science, ocean, atmosphere, 
 *  resources, weather, volcano, hurricane, earthquake, clouds, precipitation, acid, rain, snow, river, lake, biology, data, 
 *  dataset, curriculum, teacher, student, browse, find, grade level, subject, standards, national, nsf">
 *  <META NAME="description" CONTENT="ABC home page">
 *  <META NAME="creator" CONTENT="Sonal Bhushan, John Smith">
 *  <META NAME="organization" CONTENT="ABC Program Center">
 *  <META NAME="doctype" CONTENT="ABC webpage">
 *  </head>
 *  <body>
 *  <h1>Welcome to ABC.ORG.</h1> 
 *  <h3>Hurricane season is here!</h3> 
 *  <img src="abc.gif" alt="abc logo">
 *  <br>
 *  Whether directly affected or not, students can benefit from the engaging learning experiences these dramatic events can provide. Keep abreast of the current storm at the <a href="http://www.nhc.noaa.gov/"> Tropical Prediction Center</a>, where you can view advisories, maps and forecast tracks. <p>
 * Middle school students can learn about hurricane science and safety with the <a href=" http://meted.ucar.edu/hurrican/strike/index.htm"> Hurricane Strike</a> module, while more advanced students can utilize the <b>multimedia technology of the online meteorology guide <a href="http://ww2010.atmos.uiuc.edu/(Gh)/guides/mtr/hurr/home.rxml" title="hurricane page">Hurricanes</a></b>. <p>
 * One of ABC's newest collections, the <a href=" http://www.dlese.org/dds/query.do?q=hurricane&ky=00h&s=0" title="nasa page"> NASA Scientific Visualization Studio</a>, offers data, images and animations from previous Atlantic storms.
 * 
 * </body>
 * </html>
 *
 * @author    Sonal Bhushan
 */
public class HTMLParser {
	private Parser parser;


	/**
	 *  Constructor of an HTMLParser object
	 *
	 * @param  resourcelocn         either a URL or the name of an HTML file 
	 * @exception  ParserException  
	 * e.g.:
         * HTMLParser hp = new HTMLParser("http://www.dlese.org");
         * HTMLParser hp2 = new HTMLParser(testthis.htm);
	 */
	public HTMLParser(String resourcelocn) throws ParserException {
		parser = new Parser(resourcelocn);
	}


	/**
	 * Constructor of an HTMLParser object
	 *
	 * @param  htmlcontent          String containing the HTML to be parsed
	 * @param  charset              if null, the default encoding is used
	 * @exception  ParserException  
	 */
	public HTMLParser(String htmlcontent, String charset) throws ParserException {
		parser = Parser.createParser(htmlcontent, charset);
	}


	/**
	 * returns all the text in the html page which is contained within header tags (which includes <h1> - <h6>). If none of these tags are present in the page, it returns an empty string. 
	 * e.g. :
         * HTMLParser hp = new HTMLParser("http://www.abc.org");
         * System.out.println(hp.getHeaderText());
         * This prints out the following : 
         * Welcome to ABC.ORG Hurricane season is here!
	 *
	 * @return                      text in the header tags in the html document 
	 * @exception  ParserException  
	 */
	public String getHeaderText() throws ParserException {
		String headerText = "";

		String[] tagsToBeFound = {"H1", "H2", "H3", "H4", "H5", "H6"};
		TagFindingVisitor visitor = new TagFindingVisitor(tagsToBeFound);
		parser.visitAllNodesWith(visitor);
		Node[] allh1Tags = visitor.getTags(0);
		Node[] allh2Tags = visitor.getTags(1);
		Node[] allh3Tags = visitor.getTags(2);
		Node[] allh4Tags = visitor.getTags(3);
		Node[] allh5Tags = visitor.getTags(4);
		Node[] allh6Tags = visitor.getTags(5);

		int i;
		for (i = 0; i < allh1Tags.length; i++)
			headerText = headerText + " " + allh1Tags[i].toPlainTextString();
		for (i = 0; i < allh2Tags.length; i++)
			headerText = headerText + " " + allh2Tags[i].toPlainTextString();
		for (i = 0; i < allh3Tags.length; i++)
			headerText = headerText + " " + allh3Tags[i].toPlainTextString();
		for (i = 0; i < allh4Tags.length; i++)
			headerText = headerText + " " + allh4Tags[i].toPlainTextString();
		for (i = 0; i < allh5Tags.length; i++)
			headerText = headerText + " " + allh5Tags[i].toPlainTextString();
		for (i = 0; i < allh6Tags.length; i++)
			headerText = headerText + " " + allh6Tags[i].toPlainTextString();

		parser.reset();

		return headerText;
	}


	/**
	 * returns the title of the HTML page , i.e. the text enclosed by the <title> </title> tag. If this tag is not present in the page, it returns an empty string. 
	 * e.g. :
         * HTMLParserhp = new HTMLParser("http://www.abc.org");
         * System.out.println(hp.getTitleText());
         * This prints out the following : 
         * 
         *  ABC.ORG's MAIN PAGE
	 *
	 * @return                      text in the title tag(s) in the html doc.
	 * @exception  ParserException  
	 */
	public String getTitleText() throws ParserException {
		String titleText = "";

		String[] tagToBeFound = {"TITLE"};
		TagFindingVisitor visitor = new TagFindingVisitor(tagToBeFound);
		parser.visitAllNodesWith(visitor);
		Node[] allTitleTags = visitor.getTags(0);

		for (int i = 0; i < allTitleTags.length; i++)
			titleText = titleText + " " + allTitleTags[i].toPlainTextString();

		parser.reset();
		return titleText;
	}


	/**
	 *  returns true if the html document contains a Meta tag with a name equal to mname , otherwise returns false
	 *  e.g. :
         *  HTMLParser hp = new HTMLParser("http://www.abc.org");
         *  boolean containskeywords = hp.hasMetaTagName("keywords");
         *  boolean containsxyz = hp.hasMetaTagName("xyz");
         *  In this code, containskeywords will be true, and containsxyz will be false. 
	 *
	 * @param  name                 name of the Meta Tag
	 * @return                      true or false, if this tag is present or not
	 * @exception  ParserException  
	 */
	public boolean hasMetaTagName(String name) throws ParserException {
		boolean hasIt = false;

		String[] tagToBeFound = {"META"};
		TagFindingVisitor visitor = new TagFindingVisitor(tagToBeFound);
		parser.visitAllNodesWith(visitor);
		Node[] allMTags = visitor.getTags(0);

		for (int i = 0; i < allMTags.length; i++) {
			MetaTag metatag = (MetaTag) allMTags[i];

			if (name.equalsIgnoreCase(metatag.getMetaTagName())) {
				hasIt = true;
				break;
			}
		}

		parser.reset();
		return hasIt;
	}


	/**
         * returns the content of the Meta tag whose name equals mname. If such a tag does not exist, returns an empty string. 
	 * E.g. :
         * HTMLParser hp = new HTMLParser("http://www.abc.org");
         * if (hp.hasMetaTagName("organization"))
         * {
         *    System.out.println(hp.getMetaTagContentByName("organization"));
         * }
         * This prints out the following : 
	 * 
         * ABC Program Center
	 *
	 * @param  name                 name of the Meta Tag
	 * @return                      The value of this meta tag
	 * @exception  ParserException  
	 */
	public String getMetaTagContentByName(String name) throws ParserException {
		String MetaTagContent = "";

		String[] tagToBeFound = {"META"};
		TagFindingVisitor visitor = new TagFindingVisitor(tagToBeFound);
		parser.visitAllNodesWith(visitor);
		Node[] allMTags = visitor.getTags(0);

		for (int i = 0; i < allMTags.length; i++) {
			MetaTag metatag = (MetaTag) allMTags[i];

			if (name.equals(metatag.getMetaTagName())) {
				MetaTagContent = metatag.getMetaContent();
				break;
			}
		}

		parser.reset();
		return MetaTagContent;
	}


	/**
	 *  returns a String array of all the links in the html document. 
	 *
	 * @return                      a string array of all the links
	 * @exception  ParserException  
	 */
	public String[] getAllLinks() throws ParserException {

		String[] tagToBeFound = {"A"};
		TagFindingVisitor visitor = new TagFindingVisitor(tagToBeFound);
		parser.visitAllNodesWith(visitor);
		Node[] allLinkTags = visitor.getTags(0);
		String[] allLinks = new String[allLinkTags.length];
		for (int i = 0; i < allLinkTags.length; i++) {
			LinkTag l = (LinkTag) allLinkTags[i];
			allLinks[i] = l.extractLink();
		}
		parser.reset();
		return allLinks;
	}


	/**
	 * returns a String containing all the text within the title attribute of all the links in the html document 
	 *
	 * @return                      all the text within the title attribute of all the links in the doc.
	 * @exception  ParserException  
	 */
	public String getLinkTitles() throws ParserException {
		String title = "";

		String[] tagToBeFound = {"A"};
		TagFindingVisitor visitor = new TagFindingVisitor(tagToBeFound);
		parser.visitAllNodesWith(visitor);
		Node[] allLinkTags = visitor.getTags(0);

		for (int i = 0; i < allLinkTags.length; i++) {
			LinkTag l = (LinkTag) allLinkTags[i];
			String titletext = l.getAttribute("TITLE");
			if (titletext != null)
				title = title + " " + titletext;
		}

		parser.reset();
		return title;
	}


	/**
	 *  returns a String containing all the text within the alt attribute of all the img tags in the html document 
	 *
	 * @return                      all the text within the alt attribute of all the img tahs in the html doc
	 * @exception  ParserException  
	 */
	public String getImgAlts() throws ParserException {
		String alt = "";

		String[] tagToBeFound = {"IMG"};
		TagFindingVisitor visitor = new TagFindingVisitor(tagToBeFound);
		parser.visitAllNodesWith(visitor);
		Node[] allImgTags = visitor.getTags(0);

		for (int i = 0; i < allImgTags.length; i++) {
			ImageTag imagetag = (ImageTag) allImgTags[i];
			String alttext = imagetag.getAttribute("ALT");
			if (alttext != null)
				alt = alt + " " + alttext;
		}

		parser.reset();
		return alt;
	}


	/**
	 *  returns the text of the whole html document, stripped of all the HTML tags. This text also includes the text within
         *  the alt attribute of all the img tags, as well as the text within the title attribute of all the link tags. 
	 *
	 * @return                      The wholeText value
	 * @exception  ParserException  
	 */
	public String getWholeText() throws ParserException {
		String wholetext = "";

		TextExtractingVisitor visitor = new TextExtractingVisitor();
		parser.visitAllNodesWith(visitor);
		wholetext = visitor.getExtractedText();

		parser.reset();
		wholetext = wholetext + this.getImgAlts();
		parser.reset();
		wholetext = wholetext + this.getLinkTitles();
		return wholetext;
	}
}

