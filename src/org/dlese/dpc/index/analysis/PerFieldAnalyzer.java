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
package org.dlese.dpc.index.analysis;

import org.dlese.dpc.index.SimpleLuceneIndex;
import java.io.Reader;
import java.util.*;

import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

/**
 *  This Analyzer is used to facilitate scenarios where different fields require different analysis
 *  techniques. Use {@link #addAnalyzer} to add a non-default Analyzer or {@link #addAnalyzersInBundle} to
 *  provide a ResourceBundle to configure Analyzers on a field name basis. The ResourceBundle should contain
 *  className=field1,field2,... pairs, where the field names are a comma-separated list, for example:<p>
 *
 *  <pre>
 *org.dlese.dpc.index.analysis.SnowballAnalyzer=stems,titlestems
 *</pre>
 *
 * @author    John Weatherley
 */
public class PerFieldAnalyzer extends Analyzer {
	
	private Analyzer defaultAnalyzer;
	private Map analyzerMap = new HashMap();
	
	private org.apache.lucene.analysis.standard.StandardAnalyzer standardAnalyzer = new org.apache.lucene.analysis.standard.StandardAnalyzer(SimpleLuceneIndex.getLuceneVersion());
	
	// Define the global, default analyzers for common fields:
	public final static String TEXT_ANALYZER = "org.apache.lucene.analysis.standard.StandardAnalyzer";
	public final static String KEYWORD_ANALYZER = "org.apache.lucene.analysis.KeywordAnalyzer";
	public final static String STEMS_ANALYZER = "org.dlese.dpc.index.analysis.SnowballAnalyzer";	
	
	// These default analyzers should match the above:
	private Analyzer defaultTextFieldAnalyzer = standardAnalyzer;
	private Analyzer defaultKeywordFieldAnalyzer = new org.apache.lucene.analysis.KeywordAnalyzer();
	private Analyzer defaultStemsFieldAnalyzer = new org.dlese.dpc.index.analysis.SnowballAnalyzer();


	/**
	 *  Constructs with the given Analyzer to use as a default for fields not otherwise configured. If null, a
	 *  {@link org.apache.lucene.analysis.standard.StandardAnalyzer} will be used as the default.
	 *
	 * @param  defaultAnalyzer  Any fields not specifically defined to use a different analyzer will use the one
	 *      provided here.
	 */
	public PerFieldAnalyzer(Analyzer defaultAnalyzer) {
		prtln("PerFieldAnalyzer() constructor");
		if (defaultAnalyzer == null)
			this.defaultAnalyzer = new StandardAnalyzer(SimpleLuceneIndex.getLuceneVersion());
		this.defaultAnalyzer = defaultAnalyzer;
	}


	/**
	 *  Constructs using a {@link org.apache.lucene.analysis.standard.StandardAnalyzer} as the default for fields
	 *  not otherwise configured.
	 */
	public PerFieldAnalyzer() {
		prtln("PerFieldAnalyzer() constructor");
		defaultAnalyzer = new StandardAnalyzer(SimpleLuceneIndex.getLuceneVersion());
	}


	/**
	 *  Sets the Analyzer to use for the specified search field, overridding the previous one if it existed.
	 *
	 * @param  fieldName                   field name requiring a non-default analyzer.
	 * @param  analyzerClassName           Name of Analyzer class to use for the field
	 * @exception  ClassNotFoundException  If error
	 * @exception  InstantiationException  If error
	 * @exception  IllegalAccessException  If error
	 */
	public void setAnalyzer(String fieldName, String analyzerClassName)
		 throws ClassNotFoundException, InstantiationException, IllegalAccessException {

		// Instantiate only one of any given Analyzer:
		Object analyzer = null;
		// Check first to see if we already have an instance of the given Analyzer
		Object[] currentAnalyzers = analyzerMap.values().toArray();
		for (int i = 0; i < currentAnalyzers.length; i++) {
			if (currentAnalyzers[i].getClass().getName().equals(analyzerClassName)) {
				analyzer = currentAnalyzers[i];
				break;
			}
		}

		// Instantiate the Analyzer if it does not already exist:
		if (analyzer == null) {
			if(analyzerClassName.equals(standardAnalyzer.getClass().getName()))
				analyzer = standardAnalyzer;
			else {
				Class analyzerClass = Class.forName(analyzerClassName);
				analyzer = analyzerClass.newInstance();
			}
		}

		analyzerMap.put(fieldName, analyzer);
	}

	public String toString() {
		return analyzerMap.toString();	
	}
	
	
	/**
	 *  Adds the Analyzers to use for given fields, using the field=className pairs provided in the
	 *  ResourceBundle, overrridding any previous ones if they existed. The ResourceBundle should contain
	 *  className=field1,field2,... pairs, where the field names are a comma-separated list, for example:<p>
	 *
	 *  <pre>
	 *org.dlese.dpc.index.analysis.SnowballAnalyzer=stems,titlestems
	 *</pre>
	 *
	 * @param  fieldAnalyzerBundle         A resource bundle containing className=field1,field2,etc. pairs
	 * @exception  ClassNotFoundException  If error
	 * @exception  InstantiationException  If error
	 * @exception  IllegalAccessException  If error
	 */
	public void addAnalyzersInBundle(ResourceBundle fieldAnalyzerBundle)
		 throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		if (fieldAnalyzerBundle != null) {
			Enumeration analyzerClassNames = fieldAnalyzerBundle.getKeys();
			while (analyzerClassNames.hasMoreElements()) {
				String className = (String) analyzerClassNames.nextElement();

				String[] fieldNames = fieldAnalyzerBundle.getString(className).trim().split(",");
				for (int i = 0; i < fieldNames.length; i++) {
					setAnalyzer(fieldNames[i].trim(), className);
				}
			}
		}
	}


	/**
	 *  Gets the Analyzer configured for the given field, or null if none exists.
	 *
	 * @param  fieldName  The field name
	 * @return            The Analyzer
	 */
	public Analyzer getAnalyzer(String fieldName) {
		if (fieldName == null)
			return null;
		Analyzer zer = (Analyzer) analyzerMap.get(fieldName);
		if(zer != null)
			return zer;
		if (fieldName.indexOf("/text//") != -1)
			return defaultTextFieldAnalyzer;
		if (fieldName.indexOf("/stems//") != -1)
			return defaultStemsFieldAnalyzer;
		if (fieldName.indexOf("/key//") != -1)
			return defaultKeywordFieldAnalyzer;
		if (fieldName.startsWith("assignsRelationshipById"))
			return defaultKeywordFieldAnalyzer;
		if (fieldName.startsWith("assignsRelationshipByUrl"))
			return defaultKeywordFieldAnalyzer;		
		return null;
	}


	/**
	 *  Gets the default Analyzer being used.
	 *
	 * @return    The default Analyzer
	 */
	public Analyzer getDefaultAnalyzer() {
		return defaultAnalyzer;
	}


	/**
	 *  Sets the default Analyzer to use from here forth.
	 *
	 * @param  analyzer  The new default Analyzer
	 */
	public void setDefaultAnalyzer(Analyzer analyzer) {
		if (analyzer != null)
			defaultAnalyzer = analyzer;
	}


	/**
	 *  Determines if an Analyzer is configured for the given field.
	 *
	 * @param  fieldName  The field name
	 * @return            True if an Analyzer is configured for the given field
	 */
	public boolean containsAnalyzer(String fieldName) {
		if (fieldName == null)
			return false;
		if (fieldName.startsWith("/text/") || fieldName.startsWith("/key/"))
			return true;
		return analyzerMap.containsKey(fieldName);
	}


	/**
	 *  Removes the Analyzer that is configured for the given field, if one exists. After removing, the given
	 *  will will use the default Analyzer.
	 *
	 * @param  fieldName  The field name
	 * @return            The Analyzer that was configured, or null
	 */
	public Analyzer removeAnalyzer(String fieldName) {
		if (fieldName == null)
			return null;
		return (Analyzer) analyzerMap.remove(fieldName);
	}


	/**
	 *  Generates a token stream for the given field.
	 *
	 * @param  fieldName  The field name
	 * @param  reader     The Reader
	 * @return            The TokenStream appropriate for this field
	 */
	public TokenStream tokenStream(String fieldName, Reader reader) {
		Analyzer zer = getAnalyzer(fieldName);
		if (zer == null)
			zer = defaultAnalyzer;

		prtln("Using: " + zer.getClass().getName() + " for field: " + fieldName);

		return zer.tokenStream(fieldName, reader);
	}


	private final static void prtln(String s) {
		//System.out.println("PerFieldAnalyzer: " + s);
	}


	private final static void prtlnErr(String s) {
		System.err.println("PerFieldAnalyzer Error: " + s);
	}
}

