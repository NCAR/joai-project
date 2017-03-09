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
package org.dlese.dpc.vocab;

import java.io.*;
import java.util.*;
import java.text.*;
import org.dlese.dpc.gui.OPMLTree;
import org.dlese.dpc.gui.Files;

/**
 *  <p>
 *
 *  Loads a MetadataVocab instance from the given loader XML, and checks the
 *  integrity of all loaded non-default groups OPML, reporting on any missing
 *  vocabularies. </p>
 *
 * @author    Ryan Deardorff
 */
public class MetadataVocabIntegrityChecker {

	static String metadataGroupsLoaderFile;
	static MetadataVocabOPML vocab;
	private static HashMap errors = new HashMap();

	/**
	 *  Constructor for the MetadataServlet object
	 *
	 * @param  metadataGroupsLoaderFile
	 */
	public MetadataVocabIntegrityChecker( String metadataGroupsLoaderFile ) {
		this.metadataGroupsLoaderFile = metadataGroupsLoaderFile;
		System.out.println( "Loading OPML from base loader at " + metadataGroupsLoaderFile );
		vocab = (MetadataVocabOPML)( new LoadMetadataOPML( true ) ).getMetadataVocabInstance( metadataGroupsLoaderFile );
	}

	/**
	 * @param  args
	 */
	public static void main( String[] args ) {
		if ( ( args.length < 2 ) || ( args[0] == null ) ) {
			failFormat();
		}
		else {
			MetadataVocabIntegrityChecker checker = new MetadataVocabIntegrityChecker( args[0] );
			checker.checkMissing();
		}
	}

	/**
	 *  Report any vocabs that are missing from any of the non-default groups UIs
	 */
	private static void checkMissing() {
		Set s = vocab.getVocabSystemInterfaces();
		Iterator iter = s.iterator();
		while ( iter.hasNext() ) {
			String key = (String)iter.next();
			if ( !key.matches( ".*/default/.*" ) ) {
				System.out.println( "Check interface: " + key );
				String defaultKey = key.replaceFirst( "(.+/.+)/.+/(.+)", "$1/default/$2" );
				//System.out.println( "Check defaultKey: " + defaultKey );
				ArrayList fieldPaths = vocab.getVocabFieldPaths();
				for ( int j = 0; j < fieldPaths.size(); j++ ) {
					String checkPath = key + "/" + fieldPaths.get( j );
					ArrayList defaultVocabs = new ArrayList();
					HashMap nonDefaultVocabs = new HashMap();
					vocab.setCurrentTree( defaultKey + "/" + fieldPaths.get( j ) );
					iterateTreeStoreVocabs( vocab.getCurrentTree().getTopMenu(), defaultVocabs );
					vocab.setCurrentTree( checkPath );
					iterateTreeStoreVocabs( vocab.getCurrentTree().getTopMenu(), nonDefaultVocabs );
					for ( int i = 0; i < defaultVocabs.size(); i++ ) {
						String checkVocab = (String)defaultVocabs.get( i );
						if ( nonDefaultVocabs.get( checkVocab ) == null ) {
							String addErr = (String)errors.get( failFilename() );
							if ( addErr == null ) {
								addErr = "";
							}
							addErr += "\n\t" + checkVocab;
							errors.put( failFilename(), addErr );
						}
					}
				}
			}
		}
		s = errors.keySet();
		if ( s.size() == 0 ) {
			System.out.println( "No errors found." );
		}
		else {
			Iterator i = s.iterator();
			while ( i.hasNext() ) {
				String key = (String)i.next();
				System.out.println( "\n\n" + key + " is missing the following vocabs:" );
				System.out.println( errors.get( key ) );
			}
		}
	}

	/**
	 *  Iterate a tree instance and add outline "vocab" attributes to the given
	 *  list/map
	 *
	 * @param  menu
	 * @param  addTo
	 */
	private static void iterateTreeStoreVocabs( OPMLTree.TreeNode menu, Object addTo ) {
		for ( int i = 0; i < menu.treeNodes.size(); i++ ) {
			OPMLTree.TreeNode thisNode = (OPMLTree.TreeNode)menu.treeNodes.get( i );
			String vocab = (String)thisNode.getAttribute( "vocab" );
			if ( vocab != null ) {
				if ( addTo instanceof ArrayList ) {
					( (ArrayList)addTo ).add( vocab );
				}
				else if ( addTo instanceof HashMap ) {
					( (HashMap)addTo ).put( vocab, new Boolean( true ) );
				}
			}
			if ( thisNode.treeNodes.size() > 0 ) {
				iterateTreeStoreVocabs( thisNode, addTo );
			}
		}
	}

	/**
	 *  Description of the Method
	 *
	 * @return
	 */
	private static String failFilename() {
		OPMLTree.TreeNode menu = vocab.getCurrentTree().getTopMenu();
		return vocab.getFilenameOfFieldPath( menu.getAttribute( "metaFormat" ),
			menu.getAttribute( "metaVersion" ), menu.getAttribute( "audience" ), menu.getAttribute( "language" ),
			menu.getAttribute( "path" ) );
	}

	/**
	 */
	private static void failFormat() {
		System.out.println( "Format:\n\t\tjava org.dlese.dpc.MetadataVocabIntegrityChecker [loader XML filename]" );
	}
}

