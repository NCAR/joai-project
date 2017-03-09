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
package org.dlese.dpc.suggest.action;

import org.dlese.dpc.suggest.action.form.*;
import java.util.*;
import java.lang.*;
import java.io.*;
import java.text.*;
import javax.servlet.*;
import javax.servlet.http.*;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.util.MessageResources;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.XMLWriter;
import java.io.*;
import org.dlese.dpc.serviceclients.webclient.*;
import org.dlese.dpc.email.*;

/**
 *  Form submission processor that generates an XML doc based on the form input
 *  values, named as XPath expressions. If no errors occur, the record is
 *  submitted to the DCS system via doPutRecord(), which also inserts a new
 *  record ID.
 *
 * @author    Ryan Deardorff
 */
public final class SuggestXMLAction extends Action {
	private static boolean debug = false;

	// --------------------------------------------------------- Public Methods

	/**
	 *  Process the specified HTTP request, and create the corresponding HTTP
	 *  response (or forward to another web component that will create it). Return
	 *  an <code>ActionForward</code> instance describing where and how control
	 *  should be forwarded, or <code>null</code> if the response has already been
	 *  completed.
	 *
	 * @param  mapping        The ActionMapping used to select this instance
	 * @param  response       The HTTP response we are creating
	 * @param  form           The ActionForm for the given page
	 * @param  req            The HTTP request.
	 * @return                The ActionForward instance describing where and how
	 *      control should be forwarded
	 * @exception  Exception  If error.
	 */
	public ActionForward execute( ActionMapping mapping,
	                              ActionForm form,
	                              HttpServletRequest req,
	                              HttpServletResponse response ) throws Exception {
		SuggestXMLForm suggestForm = (SuggestXMLForm)form;
		ActionErrors errors = new ActionErrors();
		String newRecordId = "";
		try {
			StringBuffer xmlRecord = new StringBuffer();
			Document xmlDoc = DocumentHelper.createDocument();
			Element rootElement = xmlDoc.addElement( req.getParameter( "rootElement" ) );
			String[] atts = req.getParameterValues( "rootElementAttribute" );
			for ( int i = 0; i < atts.length; i++ ) {
				int ind = atts[i].indexOf( "=" );
				if ( ind > -1 ) {
					rootElement.addAttribute( atts[i].substring( 0, ind ),
						atts[i].substring( ind + 1, atts[i].length() ) );
				}
			}
			Enumeration elements = req.getParameterNames();
			HashMap elementAttributes = new HashMap();
			while ( elements.hasMoreElements() ) {
				String newElementName = (String)elements.nextElement();
				int ind = newElementName.indexOf( "/@" );
				if ( ind > -1 ) {
					String elementName = newElementName.substring( 0, ind );
					ArrayList attList = (ArrayList)elementAttributes.get( elementName );
					if ( attList == null ) {
						attList = new ArrayList();
					}
					attList.add( newElementName.substring( ind + 2, newElementName.length() )
						 + "=" + req.getParameter( newElementName ) );
					elementAttributes.put( elementName, attList );
				}
			}
			elements = req.getParameterNames();
			while ( elements.hasMoreElements() ) {
				String elementName = (String)elements.nextElement();
				String[] elementValues = req.getParameterValues( elementName );
				for ( int i = 0; i < elementValues.length; i++ ) {
					suggestForm.setInputValue( elementName, elementValues[i] );
				}
			}
			elements = req.getParameterNames();
			while ( elements.hasMoreElements() ) {
				String newElementName = (String)elements.nextElement();
				if ( !newElementName.startsWith( "rootElement" ) &&
					!newElementName.startsWith( "META_" ) &&
					( newElementName.indexOf( "/@" ) == -1 ) ) {
					String[] newElementValues = req.getParameterValues( newElementName );
					for ( int i = 0; i < newElementValues.length; i++ ) {
						if ( ( newElementValues[i] != null ) && ( newElementValues[i].length() > 0 ) ) {
							ArrayList attList = (ArrayList)elementAttributes.get( newElementName );
							Element newElement = DocumentHelper.makeElement( xmlDoc,
								newElementName + "-" + new Integer( i ).toString() );
							if ( newElementValues[i].equals( "_EMPTY_" ) ) {
								newElementValues[i] = "";
							}
							newElement.setText( newElementValues[i] );
							if ( attList != null ) {
								for ( int j = 0; j < attList.size(); j++ ) {
									String nameValuePair = (String)attList.get( j );
									int ind = nameValuePair.indexOf( "=" );
									if ( ind > -1 ) {
										newElement.addAttribute( nameValuePair.substring( 0, ind ),
											nameValuePair.substring( ind + 1, nameValuePair.length() ) );
									}
								}
							}
						}
					}
				}
			}
			Set attKeys = elementAttributes.keySet();
			Iterator i = attKeys.iterator();
			while ( i.hasNext() ) {
				String key = (String)i.next();
				List nodes = DocumentHelper.selectNodes( key, xmlDoc );
				if ( nodes != null && !nodes.isEmpty() ) {
					Iterator ni = nodes.iterator();
					Element elem = (Element)ni.next();
					if ( elem != null ) {
						ArrayList attList = (ArrayList)elementAttributes.get( key );
						for ( int j = 0; j < attList.size(); j++ ) {
							String nameValuePair = (String)attList.get( j );
							if ( nameValuePair != null ) {
								int ind = nameValuePair.indexOf( "=" );
								if ( ind > -1 ) {
									elem.addAttribute( nameValuePair.substring( 0, ind ),
										nameValuePair.substring( ind + 1, nameValuePair.length() ) );
								}
							}
						}
					}
				}
			}
			elements = req.getParameterNames();
			boolean foundError = false;
			while ( elements.hasMoreElements() ) {
				String newElementName = (String)elements.nextElement();
				if ( newElementName.startsWith( "META_required:" ) ) {
					String requiredField = newElementName.substring( 14, newElementName.length() );
					String requiredRegEx = req.getParameter( newElementName );
					String requiredFeedback = req.getParameter( "META_required_feedback:" + requiredField );
					String userValueOfRequiredField = suggestForm.getInputValue( requiredField );
					if ( !userValueOfRequiredField.matches( requiredRegEx ) ) {
						suggestForm.addErrorMessage( requiredFeedback );
						foundError = true;
					}
				}
			}
			if ( foundError ) {
				return mapping.findForward( "submitError" );
			}
			try {
				StringWriter sw = new StringWriter();
				XMLWriter output = new XMLWriter( sw );
				output.write( xmlDoc );
				output.close();
				suggestForm.setXmlRecord( sw.toString().replaceAll( "(</?[^\\s>]+)\\[\\s*\\d+\\s*\\]", "$1" )
					.replaceAll( "(<[^\\s>]+)\\-\\d+", "$1" ) );
				WebServiceClient webServiceClient = new WebServiceClient( req.getParameter( "META_webServiceClient" ) );
				String xmlFormat = req.getParameter( "META_xmlFormat" );
				String collection = req.getParameter( "META_collection" );
				try {
					newRecordId = webServiceClient.doPutRecord( suggestForm.getXmlRecord(), xmlFormat, collection );
				}
				catch ( WebServiceClientException e ) {
					suggestForm.setServicesSubmissionError( true );
					sendEmailMessages( req, "**ERROR** POSTING RE: ", newRecordId, suggestForm );
					// NOTE: We forward to the "success" page because the user doesn't need to
					// know that the record was not submitted into the DCS.  The record is sent
					// to the Web editor via email, who can then take steps to get it into DCS...
					return mapping.findForward( "submitSuccess" );
				}
			}
			catch ( IOException e ) {
				prtln( "SuggestAction caught IO exception: " + e.getMessage() );
				return mapping.findForward( "submitError" );
			}
			try {
				sendEmailMessages( req, "NEW POSTING RE: ", newRecordId, suggestForm );
			}
			catch ( Exception e ) {
				// Don't let an email send failure report an error to the user
				e.printStackTrace();
			}
			return mapping.findForward( "submitSuccess" );
		}
		catch ( Throwable e ) {
			System.out.println( "SuggestAction caught exception: " + e.getMessage() );
			e.printStackTrace();
			prtln( "SuggestAction caught exception: " + e.getMessage() );
			return mapping.findForward( "submitError" );
		}
	}

	/**
	 *  Description of the Method
	 *
	 * @param  req
	 * @param  subjectPrefix
	 * @param  newRecordId
	 * @param  suggestForm
	 * @exception  Exception
	 */
	private void sendEmailMessages( HttpServletRequest req, String subjectPrefix,
	                                String newRecordId, SuggestXMLForm suggestForm ) throws Exception {
		ServletContext context = servlet.getServletContext();
		// Notify the web editor:
		String notify_email = context.getInitParameter( "notify_email" );
		if ( notify_email != null ) {
			String notify_email_from = context.getInitParameter( "notify_email_from" );
			String regarding = "";
			String regardingInputField = req.getParameter( "META_notify_subject_regarding_input_field" );
			if ( regardingInputField != null ) {
				regarding = req.getParameter( regardingInputField );
			}
			String dcsEditUrl = context.getInitParameter( "notify_dcsEditPrefix" ) + newRecordId;
			new SendEmail( context.getInitParameter( "mail_type" ),
				context.getInitParameter( "mail_server" ) ).doSend( notify_email,
				notify_email_from,
				subjectPrefix + regarding,
				"The following posting has been submitted to the DCS:\n\n"
				 + dcsEditUrl + "\n\n" + suggestForm.getXmlRecord() );
		}
		// Send the user a confirmation "receipt":
		String receipt_email = "";
		String receipt_email_input_field = req.getParameter( "META_receipt_email_input_field" );
		if ( receipt_email_input_field != null ) {
			receipt_email = req.getParameter( receipt_email_input_field );
		}
		if ( receipt_email != null ) {
			new SendEmail( context.getInitParameter( "mail_type" ),
				context.getInitParameter( "mail_server" ) ).doSend( receipt_email,
				context.getInitParameter( "receipt_email_from" ),
				context.getInitParameter( "receipt_subject" ),
				context.getInitParameter( "receipt_body_text" ) );
		}
	}


	// ---------------------- Debug info --------------------

	/**
	 *  Return a string for the current time and date, sutiable for display in log
	 *  files and output to standout:
	 *
	 * @return    The dateStamp value
	 */
	protected final static String getDateStamp() {
		return
			new SimpleDateFormat( "MMM d, yyyy h:mm:ss a zzz" ).format( new Date() );
	}


	/**
	 *  Output a line of text to error out, with datestamp.
	 *
	 * @param  s  The text that will be output to error out.
	 */
	private final void prtlnErr( String s ) {
		System.err.println( getDateStamp() + " " + s );
	}


	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to
	 *  true.
	 *
	 * @param  s  The String that will be output.
	 */
	private final void prtln( String s ) {
		if ( debug ) {
			System.out.println( getDateStamp() + " " + s );
		}
	}


	/**
	 *  Sets the debug attribute of the object
	 *
	 * @param  db  The new debug value
	 */
	public static void setDebug( boolean db ) {
		debug = db;
	}
}

