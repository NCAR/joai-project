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

import java.io.*;
import java.text.*;
import java.util.*;

/**
 * Provides a hashmap-like interface into an XML-based metadata file,
 * hashed by user-defined named keys. The XMLMap stores objects, often
 * strings, which map to a user-defined label. The current version 
 * requires that the mapping be created using a mapping class that has
 * been defined and made available as part of the 
 * <tt>org.dlese.dpc.xml.maps</tt> package, and instantiable via the 
 * <tt>XMLMapFactory</tt> class in this package. A future release will
 * utilize XML-based configuration files instead.
 * <p>
 * Although not strictly required, each implementing class should
 * utilize the <tt>init</tt> method for intialization and subsequently
 * call the <tt>destroy</tt> method in order to release resources.
 * 
 * @author	Dave Deniman
 * @version	0.9b, 05/20/02
 */
public interface XMLMap {

	/**
	 * Should initialize members as required.
	 * @return <tt>true</tt> if intialization successful, <tt>false</tt> otherwise
	 */
	public boolean init();

	/**
	 * Should release resources and call the finalize method.
	 */
	public void destroy();
	
	/**
	 * Use this method to populate the <tt>XMLMap</tt> with the desired named values.
	 */
	public void setMap();
	
	/**
	 * Method to retrieve the list of names used to identify desired values.
	 */
	public List getKeys();

	/**
	 * Method to retrieve the list of values stored in this map.
	 */
	public List getValues();

	/**
	 * Accessor method for retrieving a specific named value.
	 */
	public Object getValue(String name);

	/**
	 * Setter method for updating a specific named value.
	 */
	public void setValue(String name, Object xmlObject);
}
