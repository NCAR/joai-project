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
package org.dlese.dpc.suggest.action.form;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.util.MessageResources;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.io.*;
import java.text.*;

/**
 *  A Struts Form bean for handling submission of XML records based on
 *  XPath-name HTML form inputs
 *
 * @author    Ryan Deardorff
 */
public class SuggestXMLForm extends ActionForm implements Serializable {
	private String xmlRecord = "";
	private String errorMessage = "";
	private HashMap inputValues = new HashMap();
	private ArrayList errorMessages = new ArrayList();
	private boolean servicesSubmissionError = false;

	/**
	 *  Sets the xmlRecord attribute of the SubmitNewsOppsForm object
	 *
	 * @param  xmlRecord  The new xmlRecord value
	 */
	public void setXmlRecord( String xmlRecord ) {
		this.xmlRecord = xmlRecord;
	}

	/**
	 *  Gets the servicesSubmissionError attribute of the SuggestXMLForm object
	 *
	 * @return    The servicesSubmissionError value
	 */
	public boolean getServicesSubmissionError() {
		return servicesSubmissionError;
	}

	/**
	 *  Sets the servicesSubmissionError attribute of the SuggestXMLForm object
	 *
	 * @param  value  The new servicesSubmissionError value
	 */
	public void setServicesSubmissionError( boolean value ) {
		servicesSubmissionError = value;
	}

	/**
	 *  Gets the xmlRecord attribute of the SubmitNewsOppsForm object
	 *
	 * @return    The xmlRecord value
	 */
	public String getXmlRecord() {
		return xmlRecord;
	}

	/**
	 *  Sets the errorMessage attribute of the SubmitNewsOppsForm object
	 *
	 * @param  errorMessage  The new errorMessage value
	 */
	public void setErrorMessage( String errorMessage ) {
		this.errorMessage = errorMessage;
	}

	/**
	 *  Gets the errorMessage attribute of the SubmitNewsOppsForm object
	 *
	 * @return    The errorMessage value
	 */
	public String getErrorMessage() {
		if ( errorMessages.size() > 0 ) {
			StringBuffer ret = new StringBuffer( "<ul>" );
			for ( int i = 0; i < errorMessages.size(); i++ ) {
				ret.append( "<li>" + (String)errorMessages.get( i ) + "</li>" );
			}
			ret.append( "</ul>" );
			return ret.toString();
		}
		return errorMessage;
	}

	/**
	 *  Adds a feature to the ErrorMessage attribute of the SuggestXMLForm object
	 *
	 * @param  errorMessage  The feature to be added to the ErrorMessage attribute
	 */
	public void addErrorMessage( String errorMessage ) {
		errorMessages.add( errorMessage );
	}

	/**
	 *  Gets the inputValue attribute of the SuggestXMLForm object
	 *
	 * @param  inputName
	 * @return            The inputValue value
	 */
	public String getInputValue( String inputName ) {
		String ret = (String)inputValues.get( inputName );
		if ( ret == null ) {
			return "";
		}
		return ret;
	}

	/**
	 *  Gets the inputValues attribute of the SuggestXMLForm object
	 *
	 * @param  inputName
	 * @return            The inputValues value
	 */
	public String getInputValues( String inputName ) {
		String ret = getInputValue( inputName );
		if ( ret.indexOf( "|" ) > -1 ) {
			return ret;
		}
		if ( ret.length() == 0 ) {
			return "";
		}
		return "|" + ret + "|";
	}

	/**
	 *  Sets the inputValue attribute of the SuggestXMLForm object
	 *
	 * @param  inputName   The new inputValue value
	 * @param  inputValue  The new inputValue value
	 */
	public void setInputValue( String inputName, String inputValue ) {
		String currentValue = (String)inputValues.get( inputName );
		if ( currentValue != null ) {
			// Multiple input values means checkboxes, etc. so collect in a way
			// that is compatible with DLESE controlledVocabs.xsl transform
			// parameters (seperated by '|')
			if ( !currentValue.startsWith( "|" ) ) {
				currentValue = "|" + currentValue;
			}
			currentValue = currentValue + "|" + inputValue + "|";
			inputValues.put( inputName, currentValue );
		}
		else {
			inputValues.put( inputName, inputValue );
		}
	}
}

