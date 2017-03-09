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
package org.dlese.dpc.dds.action.form;

import org.dlese.dpc.index.*;
import org.dlese.dpc.dds.*;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.util.MessageResources;
import org.dlese.dpc.vocab.MetadataVocab;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.io.*;
import java.text.*;

/**
 *@author    John Weatherley
 */
public final class DDSAdminForm extends ActionForm {

	private String message = null;
	private SimpleLuceneIndex index = null;
	private MetadataVocab vocab = null;
	private boolean showNumChanged = false;
	private boolean showVocabMessages = false;
	private boolean showVocabErrors = false;

	/**
	 *  Constructor for the DDSAdminForm object
	 */
	public DDSAdminForm() {}


	
	public void setIndex(SimpleLuceneIndex index)
	{
		prtln("setIndex()");
		this.index = index;
	}
	
	public void setMetadataVocab(MetadataVocab vocab)
	{
		prtln("setMetadataVocab()");
		this.vocab = vocab;
	}
	
	/**
	 *  Sets the message attribute of the DDSAdminForm object
	 *
	 *@param  message  The new message value
	 */
	public void setMessage( String message ) {
		this.message = message;
	}

	/**
	 *  Gets the message attribute of the DDSAdminForm object
	 */
	public String getMessage() {
		return message;
	}

	/**
	 *  Gets the lastSyncTime attribute of the DDSAdminForm object
	 */
	public String getLastSyncTime() {
		return "";
		/* long ut = fileIndexingService.getLastSyncTime();
		if ( ut == 0 ) {
			return null;
		}
		else {
			return new SimpleDateFormat( "MMM d, yyyy 'at' h:mm:ss a zzz" ).format( new Date( ut ) );
		} */
	}

	/**
	 *  Gets the lastIndexModifiedTime attribute of the DDSAdminForm object
	 */
	/* public String getLastIndexModifiedTimez() {
		if(index == null)
			return "";
		long ut = index.getLastModifiedTime();
		if ( ut == 0 || getNumRecords() == null ) {
			return null;
		}
		else {
			return new SimpleDateFormat( "MMM d, yyyy 'at' h:mm:ss a zzz" ).format( new Date( ut ) );
		}
	} */

	
	public String getNumIndexingErrors()
	{
		List errors = index.listDocs("error","true");
		if(errors != null)
			 return Long.toString( errors.size() );
		else
			return "0";
	}

	public String getNumValidationErrors()
	{
		List errors = index.listDocs("valid","false");
		if(errors != null)
			 return Long.toString( errors.size() );
		else
			return "0";
	}
	
	/**
	 *  Gets the updateFrequency attribute of the DDSAdminForm object
	 */
	public String getUpdateFrequency() {
		return "";
	}

	/**
	 *  Gets the numRecordsToDelete attribute of the DDSAdminForm object
	 */
	public String getNumRecordsToDelete() {
		return "";
	}

	/**
	 *  Gets the numRecordsToAdd attribute of the DDSAdminForm object
	 */
	public String getNumRecordsToAdd() {
		return "";
	}

	/**
	 *  Gets the numRecordsToReplace attribute of the DDSAdminForm object
	 */
	public String getNumRecordsToReplace() {
		return "";
	}

	/**
	 *  Gets the showNumChanged attribute of the DDSAdminForm object
	 */
	public String getShowNumChanged() {
		if ( showNumChanged ) {
			return "true";
		}
		else {
			return null;
		}
	}

	/**
	 *  Sets the showNumChanged attribute of the DDSAdminForm object
	 *
	 *@param  b  The new showNumChanged value
	 */
	public void setShowNumChanged( boolean b ) {
		showNumChanged = b;
	}


	/**
	 *  Gets the numRecords attribute of the DDSAdminForm object
	 */
	public String getNumRecords() {
		if ( index != null && index.getNumDocs() != -1 ) {
			return Integer.toString( index.getNumDocs() );
		}
		else {
			return null;
		}
	}

	public String getNumGoodRecords() {
		if ( index != null && index.getNumDocs() != -1 ) {		
			return Integer.toString( index.getNumDocs("collection:0* -error:true -valid:false)") );
		}
		else {
			return null;
		}
	}	
	
	/**
	 *  Gets the vocabMessage attribute of the DDSAdminForm object
	 */
	public ArrayList getVocabMessages() {
		ArrayList msgs = vocab.getMessages();
		if ( msgs == null || msgs.isEmpty() ) {
			return null;
		}
		return msgs;
	}

	/**
	 *  Gets the vocabErrors attribute of the DDSAdminForm object
	 */
	public ArrayList getVocabErrors() {
		ArrayList err = vocab.getErrors();
		if ( err == null || err.isEmpty() ) {
			return null;
		}
		return err;
	}

	/**
	 *  Sets the showVocabUpdates attribute of the DDSAdminForm object
	 *
	 *@param  bool  The new showVocabUpdates value
	 */
	public void setShowVocabMessages( boolean bool ) {
		showVocabMessages = bool;
	}

	/**
	 *  Gets the showVocabUpdates attribute of the DDSAdminForm object
	 */
	public String getShowVocabMessages() {
		if ( showVocabMessages ) {
			return "true";
		}
		else {
			return null;
		}
	}

	/**
	 *  Sets the showVocabErrors attribute of the DDSAdminForm object
	 *
	 *@param  bool  The new showVocabErrors value
	 */
	public void setShowVocabErrors( boolean bool ) {
		showVocabErrors = bool;
	}

	/**
	 *  Gets the showVocabErrors attribute of the DDSAdminForm object
	 */
	public String getShowVocabErrors() {
		if ( showVocabErrors ) {
			return "true";
		}
		else {
			return null;
		}
	}


	//================================================================

	/**
	 *  DESCRIPTION
	 *
	 *@param  s  DESCRIPTION
	 */
	protected void prtln( String s ) {
		System.out.println( s );
	}
}


