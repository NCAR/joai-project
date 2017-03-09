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
package org.dlese.dpc.vocab.action.form;

import org.apache.struts.action.*;
import org.dlese.dpc.vocab.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.*;

/**
 *  A Struts Form bean for presenting vocab configuration information
 *
 * @author    Ryan Deardorff
 */
public class VocabAdminForm extends ActionForm {
	protected MetadataVocab vocab = null;
	protected String loaderFeedback = "";

	/**
	 *  Sets the loaderFeedback attribute of the VocabAdminForm object
	 *
	 * @param  loaderFeedback  The new loaderFeedback value
	 */
	public void setLoaderFeedback( String loaderFeedback ) {
		this.loaderFeedback = loaderFeedback;
	}

	/**
	 *  Gets the loaderFeedback attribute of the VocabAdminForm object
	 *
	 * @return    The loaderFeedback value
	 */
	public String getLoaderFeedback() {
		return loaderFeedback;
	}
}

