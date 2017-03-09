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
package org.dlese.dpc.repository.action.form;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.validator.ValidatorForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.util.MessageResources;

import java.io.Serializable;

/**
 *  Bean for values used to add/edit a metadata directory for the OAI repository.
 *
 * @author     John Weatherley
 * @version    $Id: MetadataDirectoryInfoForm.java,v 1.3 2009/03/20 23:33:54 jweather Exp $
 */
public final class MetadataDirectoryInfoForm extends ValidatorForm implements Serializable {
	private String dirNickname = null;
	private String dirPath = null;
	private String dirMetadataFormat = null;
	private String metadataSchema = null;
	private String metadataNamespace = null;


	/**  Constructor for the MetadataDirectoryInfoForm Bean object */
	public MetadataDirectoryInfoForm() {
		//System.out.println("MetadataDirectoryInfoForm() ");
	}



	/**
	 *  Gets the dirNickname attribute of the MetadataDirectoryInfoForm object
	 *
	 * @return    The dirNickname value
	 */
	public String getDirNickname() {
		return dirNickname;
	}


	/**
	 *  Gets the dirPath attribute of the MetadataDirectoryInfoForm object
	 *
	 * @return    The dirPath value
	 */
	public String getDirPath() {
		return dirPath;
	}


	/**
	 *  Gets the dirMetadataFormat attribute of the MetadataDirectoryInfoForm object
	 *
	 * @return    The dirMetadataFormat value
	 */
	public String getDirMetadataFormat() {
		return dirMetadataFormat;
	}


	/**
	 *  Gets the metadataSchema attribute of the MetadataDirectoryInfoForm object
	 *
	 * @return    The metadataSchema value
	 */
	public String getMetadataSchema() {
		return metadataSchema;
	}


	/**
	 *  Gets the metadataNamespace attribute of the MetadataDirectoryInfoForm object
	 *
	 * @return    The metadataNamespace value
	 */
	public String getMetadataNamespace() {
		return metadataNamespace;
	}


	/**
	 *  Sets the dirNickname attribute of the MetadataDirectoryInfoForm object
	 *
	 * @param  value  The new dirNickname value
	 */
	public void setDirNickname(String value) {
		dirNickname = ( value == null ? null : value.trim() );
	}


	/**
	 *  Sets the dirPath attribute of the MetadataDirectoryInfoForm object
	 *
	 * @param  value  The new dirPath value
	 */
	public void setDirPath(String value) {
		dirPath = ( value == null ? null : value.trim() );
	}


	/**
	 *  Sets the dirMetadataFormat attribute of the MetadataDirectoryInfoForm object
	 *
	 * @param  value  The new dirMetadataFormat value
	 */
	public void setDirMetadataFormat(String value) {
		dirMetadataFormat = ( value == null ? null : value.trim() );
	}


	/**
	 *  Sets the metadataSchema attribute of the MetadataDirectoryInfoForm object
	 *
	 * @param  value  The new metadataSchema value
	 */
	public void setMetadataSchema(String value) {
		metadataSchema = ( value == null ? null : value.trim() );
	}


	/**
	 *  Sets the metadataNamespace attribute of the MetadataDirectoryInfoForm object
	 *
	 * @param  value  The new metadataNamespace value
	 */
	public void setMetadataNamespace(String value) {
		metadataNamespace = ( value == null ? null : value.trim() );
	}

}


