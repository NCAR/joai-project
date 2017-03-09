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
package org.dlese.dpc.ndr.dcsapi;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Map;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.XPath;
import org.dom4j.Document;

/**
 * @author Jonathan Ostwald
 */
public class DataStreamWrapper {
	private HashMap dataStreams    = null;

	/**
	 * TODO - make the internal representation nodes, not Strings
	 * 
	 * @param _xml
	 */
	public DataStreamWrapper() {
		
		this.dataStreams = new HashMap ();
	}
	
	public Set getDataStreams () {
		return this.dataStreams.entrySet();
	}
	
	public Iterator getFormats () {
		return this.dataStreams.keySet().iterator();
	}
	
	public String getMeta (String _format) {
		Element meta = DocumentHelper.createElement ("meta");
		meta.add(this.getDataStream( _format).createCopy());
		return meta.asXML();
	}
	
	/**
	 * 
	 * @param _metadataDocument
	 * @throws Exception
	 */
	public void setDataStream( String _format, Element _metadataDocument ) throws Exception {	
		if (_format == null || _format.trim().length() == 0)
			throw new Exception ("setDataStream got empty format");
		if (_metadataDocument == null)
			this.dataStreams.put (_format, null);
		else
			this.dataStreams.put (_format, _metadataDocument.createCopy());
	}
 
	public void setDataStream( String _format, String _metadataXML ) throws Exception {	
		if (_metadataXML == null)
			this.dataStreams.put (_format, null);
		else {
			Document doc = DocumentHelper.parseText(_metadataXML);
			this.setDataStream(_format, doc.getRootElement());
		}
	}
	
	
	/**
	 * Get the metadata for this wrapper object.
	 * 
	 * @return - the Metadata element for this wrapper
	 */
	public Element getDataStream( String _format) {
		return (Element)this.dataStreams.get(_format);
	}
}
