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
import org.dlese.dpc.gui.Files;

/**
 *  <p>
 *
 *  Assigns encoded IDs to new vocabularies in their fields files, and checks
 *  the integrity of the existing IDs against a serialized "database" (HashMap)
 *  of encoded ID values as mapped against field/vocab pairs.</p>
 *
 * @author    Ryan Deardorff
 */
public class MetadataVocabAssignIDs {

	static String metadataGroupsLoaderFile;
	private ArrayList fieldsList = new ArrayList();                // list of fields loader files
	private ArrayList fieldsFiles = new ArrayList();               // all fields files
	private StringBuffer currentChars = new StringBuffer();
	private String currentDir = "";

	private static HashMap vocabIDs;                               // serialized "database" of encoded IDs per vocab
	private final static String SERIALIZED_IDS = "state/id_encodings.dat";// serialized encodings filename

	private HashMap highestIDs;                                    // serialized "database" of the highest encoded ID per vocab
	private final static String SERIALIZED_IDS_HIGHEST = "state/highest_id_encodings.dat";// serialized encodings filename

	private HashMap errors = new HashMap();

	/**
	 *  Constructor for the MetadataServlet object
	 *
	 * @param  metadataGroupsLoaderFile
	 */
	public MetadataVocabAssignIDs( String metadataGroupsLoaderFile ) {
		this.metadataGroupsLoaderFile = metadataGroupsLoaderFile;
		int splitUrl = metadataGroupsLoaderFile.lastIndexOf( "/" );
		if ( splitUrl > -1 ) {
			currentDir = metadataGroupsLoaderFile.substring( 0, splitUrl + 1 );
		}
	}

	/**
	 *  Assign IDs and check the integrity of the existing ones against a
	 *  "database" (serialized HashMaps) of IDs.
	 */
	public synchronized void assignIDs() {
		if ( !( new File( SERIALIZED_IDS_HIGHEST ).exists() ) ) {
			System.out.println( "ERROR: Cannot find file " + SERIALIZED_IDS_HIGHEST );
			// The following was used to initialize the MUI 1.0 "database" of highest ID values:
			//highestIDs = new HashMap();
			//highestIDs.put( "gr", "0a" );
			//highestIDs.put( "re", "00r" );
			//highestIDs.put( "cs", "01w" );
			//highestIDs.put( "su", "0x" );
			//highestIDs.put( "ky", "01p" );
			//Files.putSerializedObject( SERIALIZED_IDS_HIGHEST, highestIDs );
		}
		else if ( !( new File( SERIALIZED_IDS ).exists() ) ) {
			System.out.println( "ERROR: Cannot find file " + SERIALIZED_IDS );
			vocabIDs = new HashMap();
			Files.putSerializedObject( SERIALIZED_IDS, vocabIDs );
		}
		else {
			highestIDs = (HashMap)Files.getSerializedObject( SERIALIZED_IDS_HIGHEST );
			vocabIDs = (HashMap)Files.getSerializedObject( SERIALIZED_IDS );
			try {
				String baseFilesDirectory = metadataGroupsLoaderFile.replaceFirst( "(.*\\/)[^\\/]+", "$1" );
				String loaderFileText = Files.getFileContents( metadataGroupsLoaderFile );
				while ( loaderFileText.matches( "[\\s\\S]*<fields>[^<]+</fields>[\\s\\S]*" ) ) {
					String fieldsBuildFile = loaderFileText.replaceFirst( "[\\s\\S]*<fields>([^<]+)</fields>[\\s\\S]*", "$1" );
					loaderFileText = loaderFileText.replaceFirst( "([\\s\\S]*)<fields>[^<]+</fields>([\\s\\S]*)", "$1$2" );
					fieldsList.add( baseFilesDirectory + fieldsBuildFile );
				}
				for ( int i = 0; i < fieldsList.size(); i++ ) {
					System.out.println( "Loading build list: " + (String)fieldsList.get( i ) );
					loaderFileText = Files.getFileContents( (String)fieldsList.get( i ) );
					while ( loaderFileText.matches( "[\\s\\S]*<file>[^<]+</file>[\\s\\S]*" ) ) {
						String fieldsFile = loaderFileText.replaceFirst( "[\\s\\S]*<file>([^<]+)</file>[\\s\\S]*", "$1" );
						loaderFileText = loaderFileText.replaceFirst( "([\\s\\S]*)<file>[^<]+</file>([\\s\\S]*)", "$1$2" );
						fieldsFiles.add( baseFilesDirectory + fieldsFile );
					}
				}
				for ( int i = 0; i < fieldsFiles.size(); i++ ) {
					processFile( (String)fieldsFiles.get( i ) );
				}
				Set s = errors.keySet();
				if ( s.size() == 0 ) {
					System.out.println( "No errors found." );
				}
				else {
					Iterator i = s.iterator();
					while ( i.hasNext() ) {
						String key = (String)i.next();
						String errs = (String)errors.get( key );
						if ( errs.length() > 0 ) {
							System.out.println( "\n\n" + key + " had the following error(s):" );
							System.out.println( errs );
						}
					}
				}
			}
			catch ( Exception e ) {
				e.printStackTrace();
			}
		}
	}

	/**
	 *  See if the given XML file needs IDs assigned, and assign them if so.
	 *
	 * @param  file
	 */
	private void processFile( String file ) {
		try {
			String fileErrors = (String)errors.get( file );
			if ( fileErrors == null ) {
				fileErrors = "";
			}
			StringBuffer xmlOut = new StringBuffer();
			String xml = Files.getFileContents( file );
			boolean addedID = false;
			if ( xml.matches( "[\\s\\S]*<field [^>]*id\\s*=\\s*\"[^\"]+\"[\\s\\S]*" ) ) {
				String fieldID = xml.replaceFirst( "[\\s\\S]*<field [^>]*id\\s*=\\s*\"([^\"]+)\"[\\s\\S]*", "$1" );
				int ind = xml.indexOf( "<termAndDeftn " );
				int lastInd = 0;
				while ( ind > -1 ) {
					xmlOut.append( xml.substring( lastInd, ind ) );
					int ind2 = xml.indexOf( ">", ind + 1 );
					String tag = xml.substring( ind, ind2 );
					String tagVocab = tag.replaceFirst( "<termAndDeftn[\\s\\S]+vocab\\s*=\\s*\"([^\"]+)\"[\\s\\S]*", "$1" );
					if ( !tag.matches( "[\\s\\S]+id\\s*=\\s*\"\\S+\"[\\s\\S]*" ) ) {
						String databaseID = (String)vocabIDs.get( fieldID + "/" + tagVocab );
						if ( databaseID != null ) {
							fileErrors += "\n\tVocab '" + tagVocab + "' has no ID--should be '" + databaseID + "'!  Please add this attribute back to the XML and re-run this ID assignment script.";
							errors.put( file, fileErrors );
						}
						else {
							// No ID, so we assign one
							addedID = true;
							String newID = getNextID( fieldID );
							xmlOut.append( tag + " id=\"" + newID + "\">" );
							//System.out.println( "Registering new ID: " + fieldID + "/" + tagVocab + " = " + newID );
							vocabIDs.put( fieldID + "/" + tagVocab, newID );
						}
					}
					else {
						// Check the integrity of the existing ID
						String existingID = tag.replaceFirst( "[\\s\\S]*id\\s*=\\s*\"([^\"]+)\"[\\s\\S]*", "$1" );
						xmlOut.append( tag + ">" );
						String databaseID = (String)vocabIDs.get( fieldID + "/" + tagVocab );
						if ( databaseID == null ) {
							fileErrors += "\n\tVocab '" + tagVocab + "' has ID '" + existingID + "' when none exists in the database!  Please remove the 'id' attribute from the XML and re-run this ID assignment script.";
							errors.put( file, fileErrors );
						}
						else if ( !databaseID.equals( existingID ) ) {
							fileErrors += "\n\tVocab '" + tagVocab + "' has ID '" + existingID + "' that differs from database value '" + databaseID + "'!  Please fix this in the XML and re-run this ID assignment script.";
							errors.put( file, fileErrors );
						}
						// The following was used for "database" initialization from MUI 1.0 set of existing IDs:
						//System.out.println( "Init registering new ID: " + fieldID + "/" + tagVocab + " = " + existingID );
						//vocabIDs.put( fieldID + "/" + tagVocab, existingID );
					}
					lastInd = ind2 + 1;
					ind = xml.indexOf( "<termAndDeftn ", lastInd );
				}
				xmlOut.append( xml.substring( lastInd, xml.length() ) );
				if ( addedID ) {
					System.out.println( "Assigned new ID(s) for '" + fieldID + "' in " + file );
					try {
						Files.writeFile( file, xmlOut.toString() );
					}
					catch ( Exception e ) {
						e.printStackTrace();
					}
				}
				Files.putSerializedObject( SERIALIZED_IDS, vocabIDs );
			}
		}
		catch ( Exception e ) {
			e.printStackTrace();
		}
	}

	/**
	 *  Assign a new vocab encoded ID using Base-36 incrementation (with first
	 *  character always '0')
	 *
	 * @param  fieldID
	 * @return          The nextID value
	 */
	private String getNextID( String fieldID ) {
		StringBuffer highestID = new StringBuffer( (String)highestIDs.get( fieldID ) );
		if ( highestID == null ) {
			// No Id existed yet, so "00" is registered
			System.out.println( "NEW FIELD " + fieldID + ": assigning first ID '00'" );
			registerNewID( fieldID, "00" );
			return "00";
		}
		for ( int i = highestID.length() - 1; i >= 1; i-- ) {
			int charInt = Character.getNumericValue( highestID.charAt( i ) );
			if ( charInt < 35 ) {
				char newChar = Character.forDigit( charInt + 1, 36 );
				highestID.setCharAt( i, newChar );
				registerNewID( fieldID, highestID.toString() );
				return highestID.toString();
			}
			else {
				highestID.setCharAt( i, '0' );
			}
		}
		highestID.append( '0' );
		registerNewID( fieldID, highestID.toString() );
		return highestID.toString();
	}

	/**
	 *  Place the new ID into the map and serialize it to disk.
	 *
	 * @param  fieldID
	 * @param  newID
	 */
	private void registerNewID( String fieldID, String newID ) {
		highestIDs.put( fieldID, newID );
		Files.putSerializedObject( SERIALIZED_IDS_HIGHEST, highestIDs );
	}

	/**
	 * @param  args
	 */
	public static void main( String[] args ) {
		if ( ( args.length < 1 ) || ( args[0] == null ) ) {
			failFormat();
		}
		else {
			MetadataVocabAssignIDs checker = new MetadataVocabAssignIDs( args[0] );
			checker.assignIDs();
		}
	}

	/**
	 */
	private static void failFormat() {
		System.out.println( "Format:\n\t\tjava org.dlese.dpc.MetadataVocabAssignIDs [loader XML filename]" );
	}
}

