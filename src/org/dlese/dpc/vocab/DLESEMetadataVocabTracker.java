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

import java.sql.*;
import java.util.*;
import org.dlese.dpc.util.strings.StringUtil;
import java.io.*;

/**
 *  DLESE metadata vocabulary tracker (Id assignment, tracking of changes). This
 *  implementation uses an SQL database.
 *
 * @author    Ryan Deardorff
 */
public class DLESEMetadataVocabTracker implements MetadataVocabTracker {
	private Connection con = null;
	private Statement stmt;
	private ResultSet rs;
	private boolean usingDatabase = false;
	private StringUtil stringUtil = new StringUtil();
	private String vocabTextFile = null;
	MetadataVocab vocab;

	/**
	 *  Is the system using this tracker connected to a database that handles
	 *  ensuring Id consistency and UI label changes?
	 *
	 * @return    The usingDatabase value
	 */
	public boolean isUsingDatabase() {
		return usingDatabase;
	}

	/**
	 *  Constructor for the DLESEMetadataVocabTracker object
	 *
	 * @param  vocab          MetadataVocab instance using this tracker
	 * @param  sqlDriver      driver class for SQL DB
	 * @param  sqlURL         URL of SQL DB
	 * @param  sqlUser        SQL user
	 * @param  sqlPassword    SQL user password
	 * @param  vocabTextFile
	 */
	public DLESEMetadataVocabTracker( MetadataVocab vocab,
	                                  String sqlDriver,
	                                  String sqlURL,
	                                  String sqlUser,
	                                  String sqlPassword,
	                                  String vocabTextFile ) {
		this.vocab = vocab;
		this.vocabTextFile = vocabTextFile;
		if ( ( sqlURL != null ) && !sqlURL.equals( "" ) ) {
			boolean gotToSelect = false;
			usingDatabase = true;
			try {
				System.out.println( "Database in use: URL = " + sqlURL );
				Class.forName( sqlDriver ).newInstance();
				con = DriverManager.getConnection( sqlURL + "?user=" + sqlUser + "&password=" + sqlPassword );
				stmt = con.createStatement();
				gotToSelect = true;
				rs = stmt.executeQuery( "SELECT * FROM vocab_values WHERE id = '1'" );
				stmt.close();
			}
			catch ( Exception e ) {
				if ( gotToSelect ) {
					// Exception must be from SELECT statement, so it is assumed the vocab table doesn't exist yet:
					createVocabValuesTable();
				}
				else {
					vocab.reportError( "SQL error" );
					e.printStackTrace();
				}
			}
			finally {
				if ( stmt != null ) {
					try {
						stmt.close();
					}
					catch ( Exception e ) {}
				}
			}
		}
	}

	/**
	 *  Close the connection to SQL database
	 */
	public void closeConnection() {
		if ( con != null ) {
			try {
				con.close();
			}
			catch ( Exception e ) {
				vocab.reportError( "SQL error" );
				e.printStackTrace();
			}
		}
	}

	/**
	 *  Create the vocab_values tables
	 */
	private void createVocabValuesTable() {
		try {
			System.out.println( "Creating SQL table vocab_values" );
			stmt = con.createStatement();
			stmt.executeQuery( "CREATE TABLE vocab_values (id INT NOT NULL PRIMARY KEY AUTO_INCREMENT, "
				 + "metadataFieldId CHAR(16), metadataValue CHAR(255), systemId CHAR(6), uiLabels TEXT, "
				 + "createdDate DATETIME, retiredDate DATETIME)" );
			stmt.close();
		}
		catch ( Exception e ) {
			vocab.reportError( "SQL error" );
			e.printStackTrace();
		}
	}

	/**
	 *  Assign a unique system Id for a new vocabulary value. ALL Ids will start
	 *  with 0, so that Lucene * queries will be possible.
	 *
	 * @param  metadataFieldId  field encoded Id
	 * @param  metadataValue    metadata value name
	 * @return                  the new Id
	 */
	public String assignNewSystemId( String metadataFieldId, String metadataValue ) {
		if ( usingDatabase ) {
			StringBuffer highestId = new StringBuffer( getHighestId( metadataFieldId ) );
			if ( highestId.toString().equals( "-1" ) ) {                 // No Id existed yet, so "00" was registered
				insertNewIdIntoValuesTable( metadataFieldId, metadataValue, "00" );
				return "00";
			}
			for ( int i = highestId.length() - 1; i >= 1; i-- ) {
				int charInt = Character.getNumericValue( highestId.charAt( i ) );
				if ( charInt < 35 ) {
					char newChar = Character.forDigit( charInt + 1, 36 );
					highestId.setCharAt( i, newChar );
					setHighestId( metadataFieldId, highestId.toString() );
					insertNewIdIntoValuesTable( metadataFieldId, metadataValue, highestId.toString() );
					return highestId.toString();
				}
				else {
					highestId.setCharAt( i, '0' );
				}
			}
			highestId.append( '0' );
			setHighestId( metadataFieldId, highestId.toString() );
			insertNewIdIntoValuesTable( metadataFieldId, metadataValue, highestId.toString() );
			return highestId.toString();
		}
		else {
			vocab.reportError( "No database in use to assign new system Id for metadata field/value pair: "
				 + metadataFieldId + "/" + metadataValue );
			return "-1";
		}
	}

	/**
	 *  Does the current fieldId/value pair already exist in the database? If so,
	 *  return the Id, if not, return "" (empty string).
	 *
	 * @param  metadataFieldId  encoded field Id
	 * @param  metadataValue    metadata value name
	 * @return                  The id value
	 */
	public String getId( String metadataFieldId, String metadataValue ) {
		String ret = "";
		if ( usingDatabase ) {
			try {
				stmt = con.createStatement();
				rs = stmt.executeQuery( "SELECT systemId FROM vocab_values WHERE metadataFieldId = '" + metadataFieldId + "' "
					 + "AND metadataValue = '" + stringUtil.escapeQuotesSQL( metadataValue ) + "'" );
				if ( rs.next() ) {
					ret = rs.getString( "systemId" );
				}
				stmt.close();
			}
			catch ( Exception e ) {
				vocab.reportError( "SQL error" );
				e.printStackTrace();
			}
		}
		return ret;
	}

	/**
	 *  Gets the highestId attribute of the DLESEMetadataVocabTracker object
	 *
	 * @param  metadataFieldSystemId
	 * @return                        The highestId value
	 */
	private String getHighestId( String metadataFieldSystemId ) {
		boolean gotToFirstSelect = false;
		try {
			stmt = con.createStatement();
			gotToFirstSelect = true;
			stmt.executeQuery( "SELECT * FROM vocab_fields;" );
			rs = stmt.executeQuery( "SELECT * FROM vocab_fields WHERE metadataFieldId = '" + metadataFieldSystemId + "'" );
			if ( rs.next() ) {
				stmt.close();
				return rs.getString( "highestValueId" );
			}
			else {
				try {
					stmt.executeQuery( "INSERT INTO vocab_fields (metadataFieldId, highestValueId) "
						 + "VALUES ('" + metadataFieldSystemId + "', '00')" );
					return "-1";
				}
				catch ( Exception ex ) {
					vocab.reportError( "SQL error" );
					ex.printStackTrace();
				}
			}
		}
		catch ( Exception e ) {
			// Table doesn't exist yet:
			if ( gotToFirstSelect ) {
				createVocabFieldsTable( metadataFieldSystemId );
				return "-1";
			}
			else {
				vocab.reportError( "SQL error" );
				e.printStackTrace();
			}
		}
		finally {
			if ( stmt != null ) {
				try {
					stmt.close();
				}
				catch ( Exception e ) {}
			}
		}
		// Shouldn't get here:
		return "NULL";
	}

	/**
	 *  Sets the highestId attribute of the DLESEMetadataVocabTracker object
	 *
	 * @param  metadataFieldSystemId  The new highestId value
	 * @param  newHighest             The new highestId value
	 */
	private void setHighestId( String metadataFieldSystemId, String newHighest ) {
		try {
			stmt = con.createStatement();
			stmt.executeQuery( "UPDATE vocab_fields SET highestValueId = '" + newHighest
				 + "' WHERE metadataFieldId = '" + metadataFieldSystemId + "'" );
		}
		catch ( Exception e ) {
			vocab.reportError( "SQL error" );
			e.printStackTrace();
		}
		finally {
			if ( stmt != null ) {
				try {
					stmt.close();
				}
				catch ( Exception e ) {}
			}
		}
	}

	/**
	 *  Create the vocab_fields table
	 *
	 * @param  metadataFieldSystemId
	 */
	private void createVocabFieldsTable( String metadataFieldSystemId ) {
		try {
			System.out.println( "Creating SQL table vocab_fields" );
			stmt = con.createStatement();
			stmt.executeQuery( "CREATE TABLE vocab_fields (id INT NOT NULL PRIMARY KEY AUTO_INCREMENT, "
				 + "metadataFieldId CHAR(5), highestValueId CHAR(5))" );
			stmt.executeQuery( "INSERT INTO vocab_fields (metadataFieldId, highestValueId) "
				 + "VALUES ('" + metadataFieldSystemId + "', '00')" );
			stmt.close();
		}
		catch ( Exception e ) {
			vocab.reportError( "SQL error" );
			e.printStackTrace();
		}
	}

	/**
	 *  Log the creation of a new metadata field/value pair with its associated new
	 *  system Id
	 *
	 * @param  systemId
	 * @param  metadataField
	 * @param  metadataValue
	 */
	private void insertNewIdIntoValuesTable( String metadataField, String metadataValue, String systemId ) {
		try {
			stmt = con.createStatement();
			stmt.executeQuery( "INSERT INTO vocab_values (metadataFieldId, metadataValue, systemId, createdDate, retiredDate) "
				 + "VALUES ('" + metadataField + "', '" + stringUtil.escapeQuotesSQL( metadataValue ) + "', '" + systemId + "', NOW(), '')" );
			stmt.close();
		}
		catch ( Exception e ) {
			vocab.reportError( "SQL error" );
			e.printStackTrace();
		}
	}

	/**
	 *  Step through all of the current (non-retired) values in the vocab_values
	 *  table and examine the UI labels, comparing what is in the DB with what was
	 *  just loaded from the XML
	 *
	 * @param  uiSystems
	 * @param  uiLabelOfSystemIds
	 */
	public void registerUiLabels( HashMap uiSystems,
	                              HashMap uiLabelOfSystemIds ) {
		if ( usingDatabase ) {
			try {
				stmt = con.createStatement();
				rs = stmt.executeQuery( "SELECT id, metadataFieldId, metadataValue, systemId, uiLabels FROM vocab_values "
					 + "WHERE retiredDate = '';" );
				while ( rs.next() ) {
					String sqlId = rs.getString( "id" );
					String fieldId = rs.getString( "metadataFieldId" );
					String metaValue = rs.getString( "metadataValue" );
					String valueId = rs.getString( "systemId" );
					String dbLabels = rs.getString( "uiLabels" );
					String xmlLabels = concatUiSystemLabels( fieldId, valueId, uiSystems, uiLabelOfSystemIds );
					if ( dbLabels == null ) {
						Statement updateStmt = con.createStatement();
						updateStmt.executeQuery( "UPDATE vocab_values SET uiLabels='" + stringUtil.escapeQuotesSQL( xmlLabels )
							 + "' WHERE id='" + sqlId + "'" );
						updateStmt.close();
					}
					else if ( !vocab.getFieldValueIdPairExists( fieldId, valueId ) ) {
						Statement updateStmt = con.createStatement();
						// Retire value that no longer exists in the XML:
						updateStmt.executeQuery( "UPDATE vocab_values SET retiredDate=NOW() WHERE id='" + sqlId + "'" );
						updateStmt.close();
					}
					else if ( !xmlLabels.equals( dbLabels ) ) {
						Statement updateStmt = con.createStatement();
						// Retire old UI labels:
						updateStmt.executeQuery( "UPDATE vocab_values SET retiredDate=NOW() WHERE id='" + sqlId + "'" );
						// Insert new UI labels:
						updateStmt.executeQuery( "INSERT INTO vocab_values (metadataFieldId, metadataValue, systemId, uiLabels, createdDate, retiredDate) "
							 + "VALUES ('" + fieldId + "', '" + stringUtil.escapeQuotesSQL( metaValue ) + "', '" + valueId + "', '" + stringUtil.escapeQuotesSQL( xmlLabels )
							 + "', NOW(), '')" );
						updateStmt.close();
					}
				}
				stmt.close();
			}
			catch ( Exception e ) {
				vocab.reportError( "SQL error" );
				e.printStackTrace();
			}
		}
	}

	/**
	 *  Concatenate UI labels (system=label, seperated by \n, sorted alphabetically
	 *  by system name)
	 *
	 * @param  fieldId
	 * @param  valueId
	 * @param  uiSystems
	 * @param  uiLabelOfSystemIds
	 * @return
	 */
	private String concatUiSystemLabels( String fieldId,
	                                     String valueId,
	                                     HashMap uiSystems,
	                                     HashMap uiLabelOfSystemIds ) {
		StringBuffer ret = new StringBuffer();
		TreeMap sortedUiSystems = new TreeMap( uiSystems );
		Set s = sortedUiSystems.keySet();
		Iterator i = s.iterator();
		while ( i.hasNext() ) {
			String system = (String)i.next();
			String label = (String)uiLabelOfSystemIds.get( system + fieldId + valueId );
			if ( label != null ) {
				ret.append( system + "=" + label + "\n" );
			}
		}
		return ret.toString();
	}

	/**
	 *  This method writes a text-based version of the entire vocabulary (even
	 *  retired values!) that simply lists each field/value system ID, along with
	 *  the metadataValue. This text file is used by the log analysis scripts to
	 *  generate spreadsheet headers that are human-readable (i.e., instead of
	 *  "gr=01" you get "gradeLevel=DLESE:Graduate or professional"). This was done
	 *  to avoid having to connect the log analysis scripts to the database, and so
	 *  that existing analysis code could still be used (the text format written is
	 *  the same as used by V1 of the DDS).
	 */
	public void writeDataAsTextFile() {
		if ( vocabTextFile != null ) {
			try {
				FileWriter file = new FileWriter( vocabTextFile );
				String outStr = getDataAsText();
				file.write( outStr, 0, outStr.length() );
				file.flush();
				file.close();
			}
			catch ( Exception e ) {
				System.out.println( "Problem writing vocab text file: " + e.toString() );
				e.printStackTrace();
			}
		}
	}

	/**
	 *  Iterate through all rows in the database, and compile a list of field/value
	 *  id -> metadataValue mappings for use in log analysis
	 *
	 * @return    The dataAsText value
	 */
	private String getDataAsText() {
		StringBuffer ret = new StringBuffer();
		HashMap fieldIds = new HashMap();
		HashMap fieldValuePairs = new HashMap();
		if ( usingDatabase ) {
			try {
				stmt = con.createStatement();
				rs = stmt.executeQuery( "SELECT metadataFieldId, metadataValue, systemId FROM vocab_values;" );
				while ( rs.next() ) {
					fieldIds.put( rs.getString( "metadataFieldId" ), new Boolean( true ) );
					fieldValuePairs.put( rs.getString( "metadataFieldId" ) + "=" + rs.getString( "systemId" ),
						rs.getString( "metadataValue" ) );
					//ret.append( rs.getString( "metadataFieldId" ) + "|\n\t" + rs.getString( "systemId" )
					//	 + "\t" + rs.getString( "metadataValue" ) + "\n" );
				}
				Set keys = fieldIds.keySet();
				Iterator i = keys.iterator();
				while ( i.hasNext() ) {
					String thisField = (String)i.next();
					ret.append( thisField + "|\n" );
					Set pairKeys = fieldValuePairs.keySet();
					Iterator j = pairKeys.iterator();
					while ( j.hasNext() ) {
						String test = (String)j.next();
						if ( test.indexOf( thisField + "=" ) == 0 ) {
							ret.append( "\t" + test.substring( thisField.length() + 1, test.length() ) );
							ret.append( "\t" + fieldValuePairs.get( test ) + "\n" );
						}
					}
				}
			}
			catch ( Exception e ) {
				System.out.println( "Problem examining database: " + e.toString() );
				e.printStackTrace();
			}
		}
		return ret.toString();
	}
}

