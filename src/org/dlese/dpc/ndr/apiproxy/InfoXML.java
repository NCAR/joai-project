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
package org.dlese.dpc.ndr.apiproxy;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.XPath;
import org.jaxen.SimpleNamespaceContext;
import java.util.*;

/**
 * 
 * This class represents the abstraction of the return data from the NDR.
 * 
 * @author kmaull
 *
 */
public class InfoXML {
	
	private Document infoxmlDoc = null;
	private Element content 	= null;
	private String contentRaw	= "";
	
	private String errorString 	= null;
	private boolean hasErrors 	= false;
	
	
	/**
	 * Constructs the infoXML abstraction based on the input
	 * given.
	 * 
	 * @param _xml
	 */
	public InfoXML ( String _xml )
	{
		try {
			// TODO : BARFS on the xsi: attribute on return saying :
			//			org.dom4j.DocumentException: Error on line 1 of document  : 
			//			The prefix "xsi" for attribute "xsi:schemaLocation" associated with 
			//			an element type "NSDLDataRepository" is not bound. 
			//			Nested exception: The prefix "xsi" for attribute "xsi:schemaLocation" 
			//			associated with an element type "NSDLDataRepository" is not bound.

			this.contentRaw = _xml;
			this.infoxmlDoc = DocumentHelper.parseText( this.contentRaw );
						
			XPath xpath = DocumentHelper.createXPath( "//ndr:error" ); // for error node checks

			// TODO : though the default namespace is NOT called NDR,
			// http://www.xslt.com/html/xsl-list/2005-03/msg01059.html indicates we must give
			// it one if we are to use it in an XPath.  How fun is that!
			SimpleNamespaceContext ns = new SimpleNamespaceContext();
			ns.addNamespace("ndr", "http://ns.nsdl.org/ndr/response_v1.00/");
			
			xpath.setNamespaceContext(ns);
			
			// select the error node
	    	Element error = (Element)xpath.selectSingleNode(this.infoxmlDoc);
	    	
	    	if ( error != null )
	    	{
	    		this.hasErrors = true;
	    		this.errorString = error.getText();
	    	}
	    	
		} catch ( Exception e ) {
			this.contentRaw = _xml;
			this.errorString = "Fatal error in InfoXML construction.";
			// e.printStackTrace();
		}		
	}
	
	/**
	 * Determine if the InfoXML is an error response.
	 * 
	 * @return - true if the InfoXML is an error 
	 */
	public boolean hasErrors() {
		return this.hasErrors;
	}
	
	/**
	 * Get the error from the InfoXML object.
	 * 
	 * @return - the error string in the <error> section of the return XML 
	 */
	public String getError()
	{
		return this.errorString;
	}
	
	/**
	 * Get the handle for the return object.
	 * 
	 * @return - the handle of the non-error return XML
	 */
	public String getHandle()
	{
		if ( this.hasErrors() )
			return null;
		else {
			return getTagValue( "handle", "ndr", "http://ns.nsdl.org/ndr/response_v1.00/" );
		}
	}
	
	public int getCount()
	{
		if ( this.hasErrors() )
			return 0;
		else {
			try {
				return Integer.parseInt (getTagValue( "count", "ndr", "http://ns.nsdl.org/ndr/response_v1.00/" ));
			} catch (Throwable t) {}
			return 0;
		}
	}
	
	public String getRequestUrl () 
	{
		if ( this.hasErrors() )
			return null;
		else {
			return getTagValue( "requestURL", "ndr", "http://ns.nsdl.org/ndr/response_v1.00/" );
		}
	}	
	
	public Element getResultData () {
		if (this.hasErrors() )
			return null;
		else {
			return getTagElement ("resultData", "ndr", "http://ns.nsdl.org/ndr/response_v1.00/" );
		}
	}
	
	public List getHandleList () {
		List handles = new ArrayList();
		Element resultData = getResultData();
		if (resultData != null) {
			Element handleList = resultData.element ("handleList");
			if (handleList != null) {
				for (Iterator i=handleList.elementIterator("handle");i.hasNext();)
					handles.add ( ((Element)i.next()).getText());
			}
		}
		return handles;
	}
					
		
	
	public String getCommand () {
		String requestUrl = this.getRequestUrl();
		if (requestUrl != null) {
			String [] splits = requestUrl.split ("/");
			return splits[splits.length - 1];
		}
		return null;
	}
	
	/**
	 * Get the raw response string InfoXML from the server.
	 * 
	 * @return - the response string from the server
	 */
	public String getResponse()
	{
		if ( this.content == null )
			return this.contentRaw;
		else return this.content.asXML();
	}	
	
	///*\*/*\*/*\*/*\*/*\*/*\*/* Private Convenience Functions *\*/*\*/*\*/*\*/*\*/*\*/*\*/*///

	/**
	 * Get the text contents of the given tag. 
	 * 
	 * @return - the string contents of the tag
	 */
	private String getTagValue( String _tag, String _namespacePrefix , String _namespaceURI )
	{	
		// TODO : 09/06/06 - this function is designed to provide 
		// an introduction to the INFO XML abstraction
		String objectHandle = null; // caller is responsible for handling null
		
		try {

			Element element = this.getTagElement(_tag, _namespacePrefix, _namespaceURI);
			if ( element != null )
				objectHandle = element.getTextTrim();

		}  catch ( Exception e ) {
			e.printStackTrace();
		}
		
		return objectHandle;		
	}
	
	private Element getTagElement (String _tag, String _namespacePrefix, String _namespaceURI ) {
		
		Element tagElement = null; // caller is responsible for handling null
		
		try {
			// TODO  this isn't complete, as we can have any arbitrary tag and values for them infinitely deep
			XPath xpath = DocumentHelper.createXPath( "//" + ( _namespacePrefix != null ? _namespacePrefix + ":" : "" ) + _tag.trim() );
			SimpleNamespaceContext ns = new SimpleNamespaceContext();
			ns.addNamespace( _namespacePrefix, _namespaceURI );  
			xpath.setNamespaceContext(ns);
	
			tagElement = ((Element)xpath.selectSingleNode(this.infoxmlDoc));
			
		}  catch ( Exception e ) {
			e.printStackTrace();
		}
		
		return tagElement;		
	}
	
}
