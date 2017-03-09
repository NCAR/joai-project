/*
 * Created on Oct 18, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package edu.colorado.bolt.cms;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.xerces.parsers.DOMParser;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;

/**
 *  Servlet that validates Concept Map Service queries.
 *
 * @author     Faisal Ahmad
 * @version    $Id: ValidateQuery.java,v 1.1 2005/11/03 20:46:41 jweather Exp $
 */
public class ValidateQuery extends HttpServlet implements ErrorHandler {
	private String flag = "Valid";
	private String result = "Query \n\n";


	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  request               NOT YET DOCUMENTED
	 * @param  response              NOT YET DOCUMENTED
	 * @exception  ServletException  NOT YET DOCUMENTED
	 * @exception  IOException       NOT YET DOCUMENTED
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doPost(request, response);
	}


	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  request               NOT YET DOCUMENTED
	 * @param  response              NOT YET DOCUMENTED
	 * @exception  ServletException  NOT YET DOCUMENTED
	 * @exception  IOException       NOT YET DOCUMENTED
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String localCsipSchemaLocation = (String) getServletContext().getInitParameter("localCsipSchemaLocation");

		Writer wr = response.getWriter();
		DOMParser parser = new DOMParser();
		String schemaLocation = "http://sms.dlese.org " + getServletContext().getRealPath(localCsipSchemaLocation).replaceAll(" ", "%20");
		BufferedReader queryBuilder = new BufferedReader(request.getReader());
		StringBuffer sb = new StringBuffer();
		String temp = null;
		String query = null;

		flag = "Valid";
		result = "Query \n\n";

		while ((temp = queryBuilder.readLine()) != null)
			sb.append(temp);

		query = sb.toString();

		try {
			parser.setFeature("http://xml.org/sax/features/namespaces", true);
			parser.setFeature("http://xml.org/sax/features/validation", true);
			parser.setFeature("http://apache.org/xml/features/validation/schema", true);
			parser.setProperty("http://apache.org/xml/properties/schema/external-schemaLocation", schemaLocation);
			parser.setErrorHandler(this);

			parser.parse(new InputSource(new BufferedReader(new StringReader(query))));
		} catch (SAXNotRecognizedException e) {} catch (SAXNotSupportedException e) {} catch (SAXException e) {} catch (IOException e) {}

		parser.reset();

		wr.write(flag + " " + result);
	}


	void appendResult(String newLine) {
		result += newLine + "\n";
	}


	/* (non-Javadoc)
	 * @see org.xml.sax.ErrorHandler#warning(org.xml.sax.SAXParseException)
	 */
	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  exception         NOT YET DOCUMENTED
	 * @exception  SAXException  NOT YET DOCUMENTED
	 */
	public void warning(SAXParseException exception) throws SAXException {
		String msg = exception.getMessage();
		int startIndex = msg.indexOf(":");
		startIndex = (startIndex >= 0 ? startIndex : 0);

		appendResult("Warning : " + msg.substring(startIndex));
	}


	/* (non-Javadoc)
	 * @see org.xml.sax.ErrorHandler#error(org.xml.sax.SAXParseException)
	 */
	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  exception         NOT YET DOCUMENTED
	 * @exception  SAXException  NOT YET DOCUMENTED
	 */
	public void error(SAXParseException exception) throws SAXException {
		String msg = exception.getMessage();
		int startIndex = msg.indexOf(":");
		startIndex = (startIndex >= 0 ? startIndex : 0);

		flag = "Invalid";
		appendResult("Error : " + msg.substring(startIndex));
	}


	/* (non-Javadoc)
	 * @see org.xml.sax.ErrorHandler#fatalError(org.xml.sax.SAXParseException)
	 */
	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  exception         NOT YET DOCUMENTED
	 * @exception  SAXException  NOT YET DOCUMENTED
	 */
	public void fatalError(SAXParseException exception) throws SAXException {
		String msg = exception.getMessage();
		int startIndex = msg.indexOf(":");
		startIndex = (startIndex >= 0 ? startIndex : 0);

		flag = "Invalid";
		appendResult("Fatal Error : " + msg.substring(startIndex));
	}
}

