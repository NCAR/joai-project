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
package org.dlese.dpc.services.mmd;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

import org.dlese.dpc.util.DpcErrors;

/**
 *  Provides access to the meta-metadata database. <p>
 *
 *  Sample test program: <pre>
 * ###
 * </pre>
 *
 * @author    Sonal Bhushan
 */

public class Query {

	/**
	 *  The queryType parameter for the findDups method: only return results from the
	 *  collection collKey.
	 */
	public final static int QUERY_SAME = 1;

	/**
	 *  The queryType parameter for the findDups method: only return results <b> not </b>
	 *  from the collection collKey.
	 */
	public final static int QUERY_OTHER = 2;

	/**  The queryType parameter for the findDups method: return all results. */
	public final static int QUERY_BOTH = 3;

	int bugs;
	DbConn dbconn = null;



	/**
	 *  Constructor: Makes the connection to the database.
	 *
	 * @param  bugs              DESCRIPTION
	 * @param  dbUrl             DESCRIPTION
	 * @exception  MmdException  DESCRIPTION
	 */
	public Query(int bugs, String dbUrl)
		 throws MmdException {
		this.bugs = bugs;
		dbconn = new DbConn(bugs, dbUrl);
	}



	/**
	 *  Closes the database connection. After close, this Query object is useless.
	 *
	 * @exception  MmdException  DESCRIPTION
	 */
	public void closeDb()
		 throws MmdException {
		if (dbconn != null) {
			dbconn.closeDb();
			dbconn = null;
		}
	}



	/**
	 *  Returns the collection name associated with a collection key, or null if none found.
	 *
	 * @param  collKey           DESCRIPTION
	 * @return                   The collectionName value
	 * @exception  MmdException  DESCRIPTION
	 */

	public String getCollectionName(String collKey)
		 throws MmdException {
		String res = dbconn.getDbString(
			"SELECT collName FROM idmapCollection"
			 + " WHERE collKey = " + dbconn.dbstring(collKey));
		return res;
	}



	/**
	 *  Returns the directory containing the XML records of the given collection key, or null
	 *  if none found.
	 *
	 * @param  collKey           DESCRIPTION
	 * @return                   The directory value
	 * @exception  MmdException  DESCRIPTION
	 */

	public String getDirectory(String collKey)
		 throws MmdException {
		String res = dbconn.getDbString(
			"SELECT dirPath FROM idmapCollection"
			 + " WHERE collKey = " + dbconn.dbstring(collKey));
		return res;
	}



////**
/// * OBSOLETE: SEE getMmdRec.
/// * Returns the MmdRecord specified, or null if none found.
/// *
/// * @param changeType  The changeType to be set; one of the CHANGE_ values
/// *		specified in {@link MmdRecord MmdRecord}.
/// * @param collKey   The collection key.
/// * @param id        The id of the record
/// */
///
///public MmdRecord getMmdRecord(
///	int changeType,
///	String collKey,
///	String id)
///throws MmdException
///{
///	MmdRecord mrec = getMmdRecord( collKey, id);
///	mrec.setChangeType( changeType);
///	return mrec;
///}



////**
/// * OBSOLETE: SEE getMmdRec.
/// * Returns the MmdRecord specified, or null if none found.
/// *
/// * @param collname  The collection key.
/// * @param id        The id of the record
/// */
///
///public MmdRecord getMmdRecord(
///	String collKey,
///	String id)
///throws MmdException
///{
///	Object[][] dbmat;
///	int ii;
///	MmdRecord mmdRec = null;		// result
///
///	dbmat = dbconn.getDbTable(
///		"SELECT fileName, status, firstAccessionDate, lastMetaModDate,"
///		+ " recCheckDate"
///		+ " FROM idmapMmd"
///		+ " WHERE collKey = " + dbconn.dbstring( collKey)
///		+ " AND id = " + dbconn.dbstring( id),
///		new String[] { "string", "string", "date", "date", "date"},	// types
///		true);					// allow nulls in fields
///	if (dbmat.length > 1)
///		mkerror("collection key \"" + collKey + "\" multiply defined.");
///	if (dbmat.length == 1) {
///		String fileName        = (String) dbmat[0][0];
///		String status          = (String) dbmat[0][1];
///		long firstAccessionDate = ((Long) dbmat[0][2]).longValue();
///		long lastMetaModDate    = ((Long) dbmat[0][3]).longValue();
///		long recCheckDate       = ((Long) dbmat[0][4]).longValue();
///
///		String metastyle = dbconn.getDbString(
///			"SELECT metastyle FROM idmapCollection"
///			+ " WHERE collKey = " + dbconn.dbstring( collKey));
///
///		// Get error messages from most recent check.
///		dbmat = dbconn.getDbTable(
///			"SELECT msgType, fileName, xpath, urllabel, url,"
///			+ " msg, auxinfo"
///			+ " FROM idmapMessages"
///			+ " WHERE collKey = " + dbconn.dbstring( collKey)
///			+ " AND id = " + dbconn.dbstring( id)
///			+ " AND recCheckDate = "
///			+ dbconn.dbstring( new Timestamp( recCheckDate)),
///			new String[] { "string", "string", "string", "string",
///				"string", "string", "string"},
///			true);					// allow nulls in fields
///
///		ErrorDesc[] errs = null;
///		if (dbmat.length > 0) {
///			errs = new ErrorDesc[ dbmat.length];
///			for (ii = 0; ii < dbmat.length; ii++) {
///				String msgtypestg   = (String) dbmat[ii][0];
///				String errfilename  = (String) dbmat[ii][1];
///				String xpath        = (String) dbmat[ii][2];
///				String urllabel     = (String) dbmat[ii][3];
///				String url          = (String) dbmat[ii][4];
///				String msg          = (String) dbmat[ii][5];
///				String auxinfo      = (String) dbmat[ii][6];
///
///				String tmpmsg = "";
///				if (errfilename != null)
///					tmpmsg += "  file: \"" + errfilename + "\"";
///				if (xpath != null) tmpmsg += "  xpath: \"" + xpath + "\"";
///				if (urllabel != null) tmpmsg += "  urllabel: \""
///					+ urllabel + "\"";
///				if (url != null) tmpmsg += "  url: \"" + url + "\"";
///				if (msg != null) tmpmsg += "  msg: \"" + msg + "\"";
///				if (auxinfo != null) tmpmsg += "  auxinfo: \""
///					+ auxinfo + "\"";
///
///				int msgType = DpcErrors.getType( msgtypestg);
///				errs[ii] = new ErrorDesc( msgType, tmpmsg);
///			}
///		}
///
///		mmdRec = new MmdRecord( MmdRecord.CHANGE_NONE,
///			collKey, id, fileName, status, metastyle,
///			firstAccessionDate, lastMetaModDate, recCheckDate, errs);
///	} // if dbmat.length == 1
///	else mmdRec = null;
///
///	return mmdRec;
///} // end getMmdRecord






	/**
	 *  Returns the MmdRec specified, or null if none found.
	 *
	 * @param  id                The id of the record
	 * @param  collKey           DESCRIPTION
	 * @return                   The mmdRec value
	 * @exception  MmdException  DESCRIPTION
	 */

	public MmdRec getMmdRec(
	                        String collKey,
	                        String id)
		 throws MmdException {
		Object[][] dbmat = null;
		int ii;
		MmdRec mmdRec = null;// result
		String primaryContent = null;
		String primarycontentType = null;

		try {
			dbmat = dbconn.getDbTable(
				"SELECT fileName, primaryUrl, status, firstAccessionDate,"
				 + " lastMetaModDate, recCheckDate, primaryContent, primarycontentType"
				 + " FROM idmapMmd"
				 + " WHERE collKey = " + dbconn.dbstring(collKey)
				 + " AND id = " + dbconn.dbstring(id),
				new String[]{"string", "string", "string",
				"date", "date", "date", "string", "string"}, // types
			true);// allow nulls in fields
			primaryContent = (String) dbmat[0][6];
			primarycontentType = (String) dbmat[0][7];
		} catch (Throwable t) {

		}

		if (dbmat == null) {
			dbmat = dbconn.getDbTable(
				"SELECT fileName, primaryUrl, status, firstAccessionDate,"
				 + " lastMetaModDate, recCheckDate"
				 + " FROM idmapMmd"
				 + " WHERE collKey = " + dbconn.dbstring(collKey)
				 + " AND id = " + dbconn.dbstring(id),
				new String[]{"string", "string", "string",
				"date", "date", "date"}, // types
			true);// allow nulls in fields
		}

		if (dbmat.length > 1)
			mkerror("collection key \"" + collKey + "\" multiply defined.");
		if (dbmat.length == 1) {
			String fileName = (String) dbmat[0][0];
			String primaryUrl = (String) dbmat[0][1];
			String status = (String) dbmat[0][2];
			long firstAccessionDate = ((Long) dbmat[0][3]).longValue();
			long lastMetaModDate = ((Long) dbmat[0][4]).longValue();
			long recCheckDate = ((Long) dbmat[0][5]).longValue();

			String metastyle = dbconn.getDbString(
				"SELECT metastyle FROM idmapCollection"
				 + " WHERE collKey = " + dbconn.dbstring(collKey));

			// Get error messages from most recent check.
			dbmat = dbconn.getDbTable(
				"SELECT msgType, fileName, xpath, urllabel, url,"
				 + " msg, auxinfo"
				 + " FROM idmapMessages"
				 + " WHERE collKey = " + dbconn.dbstring(collKey)
				 + " AND id = " + dbconn.dbstring(id)
				 + " AND recCheckDate = "
				 + dbconn.dbstring(new Timestamp(recCheckDate)),
				new String[]{"string", "string", "string", "string",
				"string", "string", "string"},
				true);// allow nulls in fields

			MmdWarning[] warnings = null;
			if (dbmat.length > 0) {
				warnings = new MmdWarning[dbmat.length];
				for (ii = 0; ii < dbmat.length; ii++) {
					String msgtypestg = (String) dbmat[ii][0];
					String errfilename = (String) dbmat[ii][1];
					String xpath = (String) dbmat[ii][2];
					String urllabel = (String) dbmat[ii][3];
					String url = (String) dbmat[ii][4];
					String msg = (String) dbmat[ii][5];
					String auxinfo = (String) dbmat[ii][6];

					int msgType = DpcErrors.getType(msgtypestg);
					warnings[ii] = new MmdWarning(msgType, errfilename,
						xpath, urllabel, url, msg, auxinfo);
				}
			}

			mmdRec = new MmdRec(collKey, id, fileName, status, metastyle,
				firstAccessionDate, lastMetaModDate, recCheckDate, primaryContent, primarycontentType, warnings);
		}// if dbmat.length == 1
		else
			mmdRec = null;

		return mmdRec;
	}// end getMmdRec



	/**
	 *  Updates the status of a single record. The new status must be one of the legal values
	 *  defined in {@link MmdRecord MmdRecord}.
	 *
	 * @param  collKey           The collection key.
	 * @param  id                The id of the record
	 * @param  newStatus         The new status value
	 * @exception  MmdException  DESCRIPTION
	 */
	public void setStatus(
	                      String collKey,
	                      String id,
	                      String newStatus)
		 throws MmdException {
		MmdRec.checkStatusString(newStatus);
		dbconn.updateDb("UPDATE idmapMmd SET status = "
			 + dbconn.dbstring(newStatus)
			 + " WHERE collKey = " + dbconn.dbstring(collKey)
			 + " AND id = " + dbconn.dbstring(id));
	}



///
////**
/// * Returns an array of ids that have changed either meta-metadata
/// * or associations (duplicates) since the specified date.
/// * <p>
/// * Such ids may or may not have
/// * changed their meta-metadata content.  For example,
/// * if id A used to appear identical to id B,
/// * but recently changed content so it now appears identical to id C,
/// * all three A, B, and C will be returned.
/// * This method is conservative: it also
/// * may return a few ids that have not changed associations.
/// *
/// * @param specdate  The specified date
/// *
/// *
/// * @param specdateparm  The specified date
/// * @param assocFlag If false we return only the mmd records that have changed
/// * in the DB; if true we return those plus any whose associations
/// * may have changed.
/// */
///
/////### test performance: lots of calls to getMmdRecord.
///
///public MmdRecord[] findAssociationChanges(
///	Date specdateparm,
///	boolean assocFlag)
///throws MmdException
///{
///	Object[][] dbmat;
///	Object[][] submat;
///	int ii, kk, icoll;
///	long specdate = specdateparm.getTime();
///
///	// Make hashmap of String collKey -> Long checkdate
///	// where checkdate is after specdate
///	dbmat = dbconn.getDbTable(
///		"SELECT collKey, collCheckDate FROM idmapCollection"
///			+ " WHERE collCheckDate >= "
///			+ dbconn.dbstring( new Timestamp( specdate))
///			+ " ORDER BY collCheckDate",
///		new String[] { "string", "date"},		// types
///		false);					// allow nulls in fields
///	HashMap datemap = new HashMap();
///	for (ii = 0; ii < dbmat.length; ii++) {
///		String collKey  = (String) dbmat[ii][0];
///		Long chkdatelong =  (Long) dbmat[ii][1];
///		datemap.put( collKey, chkdatelong);	// overwrite earlier date
///	}
///
///	// collKeys are those collections scanned after specdate.
///	String[] collKeys = (String[]) (datemap.keySet().toArray( new String[0]));
///
///	// Make HashSet of MmdRecords representing changed ids
///	HashSet changeSet = new HashSet();
///	for (icoll = 0; icoll < collKeys.length; icoll++) {
///		String collKey = collKeys[icoll];
///
///		// newdate = most recent check date for collKey,
///		// which is after specdate since that's how we searched above.
///		long newdate = ((Long) datemap.get( collKey)).longValue();
///
///		// Find olddate = last check before specdate
///		submat = dbconn.getDbTable(
///			"SELECT MAX( collCheckDate) FROM idmapCheckDate "
///				+ " WHERE collKey = " + dbconn.dbstring( collKey)
///				+ " AND collCheckDate < "
///				+ dbconn.dbstring( new Timestamp( specdate)),
///			new String[] { "string" },	// types: silly mysql returns MAX as
///										// a string: "2003-04-23 14:32:55"
///			true);					// allow nulls in fields
///		long olddate = 0;
///		if (submat.length > 0 && submat[0][0] != null)
///			olddate = Timestamp.valueOf( (String) submat[0][0]).getTime();
///		if (bugs >= 1) {
///			prtln("findAssocChanges: olddate: " + new Timestamp( olddate));
///			prtln("findAssocChanges: newdate: " + new Timestamp( newdate));
///		}
///
///		if (olddate == 0) {
///			submat = dbconn.getDbTable(
///				"SELECT id FROM idmapMmd"
///					+ " WHERE collKey = " + dbconn.dbstring( collKey)
///					+ " ORDER BY id",
///				new String[] { "string" }, // types
///				false);					// allow nulls in fields
///			for (ii = 0; ii < submat.length; ii++) {
///				String id = (String) submat[ii][0];
///				changeSet.add( getMmdRecord(
///					MmdRecord.CHANGE_ADDED, collKey, id));
///			}
///		}
///		else {
///			// olddate != 0, so we have a prior checkdate.
///			// We must find which ids changed between oldate and newdate.
///
///			// Create list of records interleaving old, new results:
///			//     id_1, OLD_checkdate, checksum, status
///			//     id_1, NEW_checkdate, checksum, status
///			//     id_2, OLD_checkdate, checksum, status
///			//     id_2, NEW_checkdate, checksum, status
///			//     id_3, OLD_checkdate, checksum, status
///			//     id_3, NEW_checkdate, checksum, status
///			//     ...
///			//
///			// Ids that were added or deleted will appear as singletons.
///
///			submat = dbconn.getDbTable(
///				"SELECT id, recCheckDate, metaChecksum,"
///					+ " primaryChecksum, status"
///					+ " FROM idmapHistory"
///					+ " WHERE collKey = " + dbconn.dbstring( collKey)
///					+ " AND (recCheckDate = "
///					+ dbconn.dbstring( new Timestamp( olddate))
///					+ " OR recCheckDate = "
///					+ dbconn.dbstring( new Timestamp( newdate))
///					+ ")"
///					+ " ORDER BY id, recCheckDate",
///				new String[] { "string", "date", "long", "long", "string" },
///				false);					// allow nulls in fields
///
///			// Run down the list, adding to changeSet as needed.
///			ii = 0;
///			while (ii < submat.length) {
///				// Extract *a = [ii], *b = [ii+1]
///				String ida        = (String) submat[ii][0];
///				long datea         = ((Long) submat[ii][1]).longValue();
///				long metachecksuma = ((Long) submat[ii][2]).longValue();
///				long primchecksuma = ((Long) submat[ii][3]).longValue();
///				String statusa    = (String) submat[ii][4];
///
///				String idb = null;
///				long dateb = 0;
///				long metachecksumb = 0;
///				long primchecksumb = 0;
///				String statusb = null;
///				if (ii < submat.length - 1) {
///					idb        = (String) submat[ii+1][0];
///					dateb       = ((Long) submat[ii+1][1]).longValue();
///					metachecksumb = ((Long) submat[ii][2]).longValue();
///					primchecksumb = ((Long) submat[ii][3]).longValue();
///					statusb    = (String) submat[ii+1][4];
///				}
///
///				// If this line is a singleton ...
///				if (ii == submat.length - 1
///					|| (! ida.equals( idb))
///					|| (! statusa.equals( statusb)))
///				{
///					if (datea == olddate) {			// if rec deleted
///						// Actually this never happens since we never
///						// remove a record.  We just change it's status.
///						if (bugs >= 1) prtln("findAssocChanges: deleted: "
///							+ collKey + "  " + ida);
///
///						if (assocFlag)
///							addhistdups( datea, primchecksuma, changeSet);
///						else changeSet.add( getMmdRecord(
///							MmdRecord.CHANGE_DELETED, collKey, ida));
///					}
///					if (datea == newdate) {			// if rec added
///						if (bugs >= 1) prtln("findAssocChanges: added: "
///							+ collKey + "  " + ida);
///						if (assocFlag)
///							addhistdups( datea, primchecksuma, changeSet);
///						else changeSet.add( getMmdRecord(
///							MmdRecord.CHANGE_ADDED, collKey, idb));
///					}
///					else mkerror("datea mismatch");
///					ii++;							// advance over singleton
///				}
///
///				// else if record changed ...
///				else if (metachecksuma != metachecksumb
///					|| (assocFlag && (primchecksuma != primchecksumb)))
///				{
///					if (bugs >= 1) prtln("findAssocChanges: changed: "
///						+ collKey + "  " + idb);
///					if (assocFlag) {
///						addhistdups( datea, primchecksuma, changeSet);
///						addhistdups( dateb, primchecksumb, changeSet);
///					}
///					else changeSet.add( getMmdRecord(
///						MmdRecord.CHANGE_MMD, collKey, idb));
///					ii += 2;						// advance over pair
///				}
///				else {								// no change
///					ii += 2;						// advance over pair
///				}
///			} // while ii < submat.length
///		} // if olddate not 0
///	} // for icoll
///
///	MmdRecord[] mrecs = (MmdRecord[]) changeSet.toArray( new MmdRecord[0]);
///	Arrays.sort( mrecs);
///	return mrecs;
///}
///
///
///
///
///
///private void addhistdups(
///	long specdate,
///	long primchecksum,
///	HashSet changeSet)
///throws MmdException
///{
///	int ii;
///	Object[][] dbmat;
///
///	if (primchecksum != 0) {
///		dbmat = dbconn.getDbTable(
///			"SELECT collKey, id FROM idmapHistory"
///				+ " WHERE recCheckDate = "
///				+ dbconn.dbstring( new Timestamp( specdate))
///				+ " AND primaryChecksum = " + dbconn.dbstring( primchecksum),
///			new String[] { "string", "string"},		// types
///			false);					// allow nulls in fields
///		LinkedList reslist = new LinkedList();
///		for (ii = 0; ii < dbmat.length; ii++) {
///			String collKey = (String) dbmat[ii][0];
///			String id       = (String) dbmat[ii][1];
///			changeSet.add( getMmdRecord(
///				MmdRecord.CHANGE_ASSOC, collKey, id));
///		}
///	}
///}









////**
/// * OBSOLETE: See findDups
/// * Returns an array of MmdRecord representing ids in
/// * other collections that appear identical to the specified
/// * (collKey, id).
/// *
/// * @param collKey  The collection key.
/// * @param id  The id to search for
/// *
/// * @return An array of MmdRecod, each of which represents a resource
/// * from a different collection having possibly identical content.
/// */
///
/////### test performance: lots of calls to getMmdRecord.
///
///public MmdRecord[] findDuplicates(
///	String collKey,
///	String id)
///throws MmdException
///{
///	Object[][] dbmat;
///	int ii, kk;
///
///	MmdRecord[] resmat = null;
///
///	dbmat = dbconn.getDbTable(
///		"SELECT primaryChecksum FROM idmapMmd"
///			+ " WHERE collKey = " + dbconn.dbstring( collKey)
///			+ " AND id = " + dbconn.dbstring( id),
///		new String[] { "long"},		// types
///		false);					// allow nulls in fields
///	if (dbmat.length == 0) {}
///		// call it ok if not found
///		///mkerror("findDuplicates: id \"" + id
///		///	+ "\"  not found in collection \"" + collKey + "\"");
///
///	else if (dbmat.length > 1)
///		mkerror("findDuplicates: > 1 db rows match id");
///
///	else {
///		long checksum = ((Long) dbmat[0][0]).longValue();
///		// If checksum is 0, no test is possible.
///		if (checksum != 0) {
///			dbmat = dbconn.getDbTable(
///				"SELECT collKey, id FROM idmapMmd"
///					+ " WHERE primaryChecksum = " + dbconn.dbstring( checksum)
///					+ " AND collKey != " + dbconn.dbstring( collKey),
///				new String[] { "string", "string"},		// types
///				false);					// allow nulls in fields
///
///			// Normally the SELECT would return our original
///			// id  since it has the original checksum.
///			// And we would have to use ...
///
///			///		if (dbmat.length > 1) {
///			///			// Result matrix len is one less, since we get our
///			///			// original id  is returned as well.
///			///			resmat = new MmdRecord[ dbmat.length - 1];
///			///			kk = 0;
///			///			for (ii = 0; ii < dbmat.length; ii++) {
///			///				String dbcollKey = (String) dbmat[ii][0];
///			///				String dbid       = (String) dbmat[ii][1];
///			///				if ( ! (dbcollKey.equals(collKey)
///			///					&& dbid.equals(id)))
///			///				{
///			///					resmat[kk++] = getMmdRecord(dbcollKey,id);
///			///				}
///			///			}
///			///		}
///
///			// But since the SELECT specifies "collKey != ..."
///			// every returned row is meaningful ...
///			if (dbmat.length > 0) {
///				resmat = new MmdRecord[ dbmat.length];
///				for (ii = 0; ii < dbmat.length; ii++) {
///					String dbcollKey = (String) dbmat[ii][0];
///					String dbid       = (String) dbmat[ii][1];
///					resmat[ii] = getMmdRecord( dbcollKey, dbid);
///				}
///			}
///		}
///	}
///
///	return resmat;
///} // findDuplicates






	public static void reloadIdExclusionDocument(){
		CatchDup.reloadIdExclusionDocument();
	}




	/**
	 *  Returns an array of MmdRec representing ids in other collections that appear
	 *  identical to the specified (collKey, id).
	 *
	 * @param  queryType         One of:
	 *      <ul>
	 *        <li> QUERY_SAME: only return results from collection collKey
	 *        <li> QUERY_OTHER: only return results <b> not </b> from collection collKey
	 *        <li> QUERY_BOTH: return all results.
	 *      </ul>
	 *
	 * @param  collKey           The collection key.
	 * @param  id                The id to search for
	 * @return                   An array of MmdRecod, each of which represents a resource
	 *      from a different collection having possibly identical content.
	 * @exception  MmdException  DESCRIPTION
	 */

	public MmdRec[] findDups(
	                         int queryType,
	                         String collKey,
	                         String id, String exc)
		 throws MmdException {
		Object[][] dbmat;
		int ii;
		int kk;

		MmdRec[] resmat = null;
		//String exc  = new String("http://www.dlese.org/Metadata/documents/xml/nondups.xml");


		dbmat = dbconn.getDbTable(
			"SELECT primaryUrl, primaryChecksum FROM idmapMmd"
			 + " WHERE collKey = " + dbconn.dbstring(collKey)
			 + " AND id = " + dbconn.dbstring(id),
			new String[]{"string", "long"}, // types
		true);// allow nulls in fields

		if (dbmat.length == 0) {
		}
		// call it ok if not found
		///mkerror("findDups: id \"" + id
		///	+ "\"  not found in collection \"" + collKey + "\"");

		else if (dbmat.length > 1)
			mkerror("findDups: > 1 db rows match id");

		else {
			String primaryUrl = null;
			if (dbmat[0][0] != null)
				primaryUrl = (String) dbmat[0][0];
			long checksum = 0;
			if (dbmat[0][1] != null)
				checksum = ((Long) dbmat[0][1]).longValue();
			if (checksum != 0) {
				String qtypestg = null;
				if (queryType == QUERY_SAME)
					qtypestg = " AND collKey = " + dbconn.dbstring(collKey);
				else if (queryType == QUERY_OTHER)
					qtypestg = " AND collKey != " + dbconn.dbstring(collKey);
				else if (queryType == QUERY_BOTH)
					qtypestg = "";
				else
					mkerror("findDups: invalid queryType: " + queryType);

				qtypestg += " AND hasFile!=0";
				dbmat = dbconn.getDbTable(
					"SELECT collKey, id, primaryUrl, primaryChecksum FROM idmapMmd"
					 + " WHERE ( primaryChecksum = "
					 + dbconn.dbstring(checksum)
					 + " OR primaryUrl = " + dbconn.dbstring(primaryUrl)
					 + " ) " + qtypestg,
					new String[]{"string", "string", "string", "long"}, // types
				false);// allow nulls in fields

				if (dbmat.length > 0) {
					boolean realdup = true;
					if (dbmat.length == 1) {
						String dbcollKey = (String) dbmat[0][0];
						String dbid = (String) dbmat[0][1];
						if ((dbcollKey.equals(collKey)) && (dbid.equals(id)))
							realdup = false;
						else
							realdup = true;
					}
					if (realdup == true) {
						int length = 0;
						for (ii = 0; ii < dbmat.length; ii++) {
							String dbcollKey = (String) dbmat[ii][0];
							String dbid = (String) dbmat[ii][1];
							String dbUrl = (String) dbmat[ii][2];
							long dbchecksum = ((Long) dbmat[ii][3]).longValue();

							CatchDup c = new CatchDup(checksum, primaryUrl, id, dbchecksum, dbUrl, dbid, exc);
							boolean isdup = c.isDup();
							if (isdup == true)
								length++;
						}
						if (length > 0) {
							int count = 0;
							resmat = new MmdRec[length];
							for (ii = 0; ii < dbmat.length; ii++) {
								String dbcollKey = (String) dbmat[ii][0];
								String dbid = (String) dbmat[ii][1];
								String dbUrl = (String) dbmat[ii][2];
								long dbchecksum = ((Long) dbmat[ii][3]).longValue();

								CatchDup c = new CatchDup(checksum, primaryUrl, id, dbchecksum, dbUrl, dbid, exc);
								boolean isdup = c.isDup();
								if (isdup == true) {
									resmat[count] = getMmdRec(dbcollKey, dbid);
									count++;
								}
							}
						}
					}
				}
			}
			else {

				String qtypestg = null;
				if (queryType == QUERY_SAME)
					qtypestg = " AND collKey = " + dbconn.dbstring(collKey);
				else if (queryType == QUERY_OTHER)
					qtypestg = " AND collKey != " + dbconn.dbstring(collKey);
				else if (queryType == QUERY_BOTH)
					qtypestg = "";
				else
					mkerror("findDups: invalid queryType: " + queryType);

				qtypestg += " AND hasFile!=0";
				dbmat = dbconn.getDbTable(
					"SELECT collKey, id, primaryUrl, primaryChecksum FROM idmapMmd"
					 + " WHERE primaryUrl = " + dbconn.dbstring(primaryUrl)
					 + qtypestg,
					new String[]{"string", "string", "string", "long"}, // types
				false);// allow nulls in fields

				if (dbmat.length > 0) {
					boolean realdup = true;
					if (dbmat.length == 1) {
						String dbcollKey = (String) dbmat[0][0];
						String dbid = (String) dbmat[0][1];
						if ((dbcollKey.equals(collKey)) && (dbid.equals(id)))
							realdup = false;
						else
							realdup = true;
					}
					if (realdup == true) {
						int length = 0;
						for (ii = 0; ii < dbmat.length; ii++) {
							String dbcollKey = (String) dbmat[ii][0];
							String dbid = (String) dbmat[ii][1];
							String dbUrl = (String) dbmat[ii][2];
							long dbchecksum = ((Long) dbmat[ii][3]).longValue();

							CatchDup c = new CatchDup(0, primaryUrl, id, dbchecksum, dbUrl, dbid, exc);
							boolean isdup = c.isDup();
							if (isdup == true)
								length++;

						}
						if (length > 0) {
							int count = 0;
							resmat = new MmdRec[length];
							for (ii = 0; ii < dbmat.length; ii++) {
								String dbcollKey = (String) dbmat[ii][0];
								String dbid = (String) dbmat[ii][1];
								String dbUrl = (String) dbmat[ii][2];
								long dbchecksum = ((Long) dbmat[ii][3]).longValue();

								CatchDup c = new CatchDup(0, primaryUrl, id,  dbchecksum, dbUrl, dbid, exc);
								boolean isdup = c.isDup();
								if (isdup == true) {
									resmat[count] = getMmdRec(dbcollKey, dbid);
									count++;
								}
							}
						}
					}
				}
			}
		}

		return resmat;
	}// findDups



	/**
	 *  Simply throws an MmdException.
	 *
	 * @param  msg               DESCRIPTION
	 * @exception  MmdException  DESCRIPTION
	 */

	static void mkerror(String msg)
		 throws MmdException {
		throw new MmdException(msg);
	}



	/**
	 *  Prints a String without a trailing newline.
	 *
	 * @param  msg  DESCRIPTION
	 */

	static void prtstg(String msg) {
		System.out.print(msg);
	}



	/**
	 *  Prints a String with a trailing newline.
	 *
	 * @param  msg  DESCRIPTION
	 */

	static void prtln(String msg) {
		System.out.println(msg);
	}

}// end class Query

