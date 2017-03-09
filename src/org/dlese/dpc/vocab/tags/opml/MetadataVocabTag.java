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
package org.dlese.dpc.vocab.tags.opml;

import org.dlese.dpc.vocab.MetadataVocab;
import org.dlese.dpc.vocab.MetadataVocabOPML;
import org.dlese.dpc.util.strings.StringUtil;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;
import javax.servlet.jsp.PageContext;

/**
 *  Default tag handler for rendering controlled vocabularies. All other vocab
 *  tags should extend this! This is the predecessor to the terms/groups system,
 *  using OPML (and "format/version/audience/language" groupings instead of
 *  "system.interface.language"), and providing a cleaner and more thorough set
 *  of methods for accessing mappings between metadata vocabulary encoded IDs
 *  and their term names.
 *
 * @author    Ryan Deardorff
 */
public class MetadataVocabTag extends TagSupport {

	protected String audience = "";
	protected String metaFormat = "";
	protected String metaVersion = "";
	protected String language = "";
	protected String system = "";
	protected String field = "";
	protected String subGroup = "";
	protected MetadataVocab vocab;
	protected StringUtil stringUtil = new StringUtil();

	/**
	 *  Audience is a user group, i.e. "default", "community", or "cataloger"
	 *
	 * @param  audience  The new audience value
	 */
	public void setAudience( String audience ) {
		this.audience = audience;
	}

	/**
	 *  MetaFormat is the metadata format, i.e. "adn", "news_opps", or
	 *  "dlese_collect"
	 *
	 * @param  metaFormat  The new metaFormat value
	 */
	public void setMetaFormat( String metaFormat ) {
		this.metaFormat = metaFormat;
		metaVersion = "";
	}

	/**
	 *  Language will allow for support of internationalization. "en-us" indicates
	 *  the United States version of English.
	 *
	 * @param  language  The new language value
	 */
	public void setLanguage( String language ) {
		this.language = language;
	}


	/**
	 *  Sets the field attribute of the MetadataVocabTag object
	 *
	 * @param  field  The new field value
	 */
	public void setField( String field ) {
		this.field = field;
	}

	/**
	 *  SubGroup is used to reference a particular spot WITHIN the vocabulary
	 *  hierarchy, for example, setting "Visual" for a call to the resource types
	 *  vocab would return only those values within the "Visual" sub-grouping as
	 *  defined in the OPML.
	 *
	 * @param  subGroup  The new subGroup value
	 */
	public void setSubGroup( String subGroup ) {
		this.subGroup = subGroup;
	}

	/**
	 *  Get the vocab object from the page context and expand system to be a
	 *  concatenation of system, interface, and language
	 *
	 * @param  pageContext
	 * @exception  JspException
	 */
	public void setupTag( PageContext pageContext ) throws JspException {
		String contextAttributeName = (String)pageContext.getServletContext().getInitParameter( "metadataVocabInstanceAttributeName" );
		vocab = (MetadataVocab)pageContext.findAttribute( contextAttributeName );
		if ( vocab == null ) {
			System.out.println( "Looked for vocab in " + contextAttributeName );
			throw new JspException( "Vocabulary not found" );
		}
		else {
			try {
				metaVersion = vocab.getCurrentVersion( metaFormat );
			}
			catch ( Exception e ) {
				new JspException( "No current version found for metadata framework " + metaFormat );
			}
			system = metaFormat + "/" + metaVersion + "/" + audience + "/" + language + "/" + field;
			( (MetadataVocabOPML)vocab ).setCurrentTree( system, subGroup );
		}
	}

	/**
	 *  Release from memory
	 */
	public void release() {
		super.release();
	}
}

