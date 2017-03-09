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
package org.dlese.dpc.repository;

import java.io.*;
import java.util.*;

/**
 *  Data structure that holds information about a metadata format.
 *
 * @author    John Weatherley
 */
public class MetadataFormatInfo implements Serializable {
	private String metadataPrefix = "";
	private String schema = "";
	private String metadataNamespace = "";



	/**
	 *  Constructor for the MetadataFormatInfo object
	 */
	public MetadataFormatInfo() { }



	/**
	 *  Constructor for the MetadataFormatInfo object
	 *
	 * @param  metadataPrefix     The metadataPrefix.
	 * @param  schema             The schema.
	 * @param  metadataNamespace  The metadataNamespace.
	 */
	public MetadataFormatInfo(
			String metadataPrefix,
			String schema,
			String metadataNamespace) {
		this.metadataPrefix = metadataPrefix.trim();
		this.schema = schema.trim();
		this.metadataNamespace = metadataNamespace.trim();
	}



	/**
	 *  Gets the metadataPrefix attribute of the MetadataFormatInfo object
	 *
	 * @return    The metadataPrefix value
	 */
	public String getMetadataPrefix() {
		return metadataPrefix;
	}



	/**
	 *  Sets the metadataPrefix attribute of the MetadataFormatInfo object
	 *
	 * @param  val  The new metadataPrefix value
	 */
	public void setMetadataPrefix(String val) {
		metadataPrefix = val.trim();
	}



	/**
	 *  Gets the metadataNamespace attribute of the MetadataFormatInfo object
	 *
	 * @return    The metadataNamespace value
	 */
	public String getMetadataNamespace() {
		return metadataNamespace;
	}



	/**
	 *  Sets the metadataNamespace attribute of the MetadataFormatInfo object
	 *
	 * @param  val  The new metadataNamespace value
	 */
	public void setMetadataNamespace(String val) {
		metadataNamespace = val.trim();
	}


	/**
	 *  Gets the schema attribute of the MetadataFormatInfo object
	 *
	 * @return    The schema value
	 */
	public String getSchema() {
		return schema;
	}


	/**
	 *  Sets the schema attribute of the MetadataFormatInfo object
	 *
	 * @param  val  The new schema value
	 */
	public void setSchema(String val) {
		schema = val.trim();
	}




	/**
	 *  Provides a String representataion for this MetadataFormatInfo. This method
	 *  may be used for debugging to see what is in the MetadataFormatInfo. This
	 *  method is also used it the {@link #equals(Object)} method.
	 *
	 * @return    String describing all data in the SetInfo.
	 */
	public String toString() {
		StringBuffer ret =
				new StringBuffer(
				"metadataPrefix: " + metadataPrefix +
				"; schema: " + schema +
				"; metadataNamespace:" + metadataNamespace);
		return ret.toString();
	}


	/**
	 *  Checks equality of two MetadataFormatInfo objects.
	 *
	 * @param  o  The MetadataFormatInfo to compare to this.
	 * @return    True iff the compared object is equal.
	 */
	public boolean equals(Object o) {
		if (o == null || !(o instanceof SetInfo))
			return false;
		try {
			return this.toString().equals(o.toString());
		} catch (Throwable e) {
			// Catch null pointer...
			return false;
		}
	}

}

