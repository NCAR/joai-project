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
import java.net.URLEncoder;
import org.dlese.dpc.util.*;
import org.dlese.dpc.xml.*;

import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.index.Term;

import org.dlese.dpc.webapps.tools.OutputTools;

/**
 *  Data structure used in the {@link RepositoryManager} to hold information about a set of metadata files.
 *  Note that as of jOAI v3.x (Feb 2006), this class is no longer used to define OAI sets but is now only used
 *  to define the files that are configured in the {@link RepositoryManager}. OAI sets are now defined
 *  separately.
 *
 * @author     John Weatherley
 * @see        DirInfo
 */
public class SetInfo implements Serializable, Comparable {
	private String name = "";
	private String setSpec = "";
	private String description = "";
	private String enabled = "true";
	private String id = "";
	private String accessionStatus = "";
	private ArrayList dirInfos = new ArrayList();
	private HashMap attributes = new HashMap();
	private long uniqueID = 0;
	int numIndexed = -1, numDiscoverable = -1, numErrors = -1, numFiles = -1, numDeleted = -1;


	/**  Constructor for the SetInfo object */
	public SetInfo() {
		numIndexed = -1;
		numDiscoverable = -1;
		numErrors = -1;
		numFiles = -1;
		numDeleted = -1;
	}


	/**
	 *  Constructor for the SetInfo object
	 *
	 * @param  name         The human-readable name of the set
	 * @param  setSpec      The oai setSpec that will be used to reference this set
	 * @param  dir          A directory where files for this set are located
	 * @param  format       The native metadata format for the files in the above directory
	 * @param  description  A description for this set, in XML form.
	 * @param  enabled      [true | false]
	 * @param  id           DESCRIPTION
	 */
	public SetInfo(String name,
	               String setSpec,
	               String description,
	               String enabled,
	               String dir,
	               String format,
	               String id) {
		this.name = name.trim();
		this.setSpec = setSpec.trim();
		this.description = description.trim();
		this.enabled = enabled.trim().toLowerCase();
		this.uniqueID = Utils.getUniqueID();
		this.id = id;

		if (dir != null && format != null) {
			if (dirInfos == null)
				dirInfos = new ArrayList();

			dirInfos.add(new DirInfo(dir, format));
		}
	}


	/**
	 *  Gets the name attribute of the SetInfo object
	 *
	 * @return    The name value
	 */
	public String getName() {
		return name;
	}


	/**
	 *  Gets the record ID for this SetInfo.
	 *
	 * @return    The record ID.
	 */
	public String getId() {
		if (id == null)
			return "";
		return id;
	}


	/**
	 *  Sets the record ID for this SetInfo.
	 *
	 * @param  id  The record ID.
	 */
	public void setId(String id) {
		this.id = id;
	}


	/**
	 *  Gets the unique ID for this SetInfo.
	 *
	 * @return    The unique ID.
	 */
	public String getUniqueID() {
		return Long.toString(uniqueID);
	}


	/**
	 *  Gets the unique ID for this SetInfo.
	 *
	 * @return    The unique ID.
	 */
	public long getUniqueIDLong() {
		return uniqueID;
	}


	/**
	 *  Gets the name attribute of the SetInfo object, encoded for use in a URL.
	 *
	 * @return    The name encoded with URL encoing.
	 */
	public String getNameEncoded() {
		try {
			return URLEncoder.encode(name, "utf-8");
		} catch (Throwable e) {
			return name;
		}
	}


	/**
	 *  Sets the name attribute of the SetInfo object
	 *
	 * @param  val  The new name value
	 */
	public void setName(String val) {
		name = val.trim();
	}


	/**
	 *  Gets the setSpec attribute of the SetInfo object
	 *
	 * @return    The setSpec value
	 */
	public String getSetSpec() {
		return setSpec;
	}


	/**
	 *  Sets the setSpec attribute of the SetInfo object
	 *
	 * @param  val  The new setSpec value
	 */
	public void setSetSpec(String val) {
		setSpec = val.trim();
	}



	/**
	 *  Gets the XML description.
	 *
	 * @return    The description value
	 */
	public String getDescription() {
		return description;
	}


	/**
	 *  Gets the metadata format of the files in the primary directory configured for this set, or an empty
	 *  String if none is configured. The primary directory is the first one configured.
	 *
	 * @return    The metadta format of the files.
	 */
	public String getFormat() {
		DirInfo primaryDir = getDirInfo(0);
		if (primaryDir == null)
			return "";
		else
			return primaryDir.getFormat();
	}


	/**
	 *  Gets the primary directory path configured for this set, or an empty String if none is configured. The
	 *  primary directory is the first one configured.
	 *
	 * @return    The directory path.
	 */
	public String getDirectory() {
		DirInfo primaryDir = getDirInfo(0);
		if (primaryDir == null)
			return "";
		else
			return new File(primaryDir.getDirectory()).getAbsolutePath();
	}


	/**
	 *  Gets the descriptionHtml attribute of the SetInfo object
	 *
	 * @return    The descriptionHtml value
	 */
	public String getDescriptionHtml() {
		return OutputTools.xmlToHtml(description);
	}


	/**
	 *  Sets the description, which must be in XML form.
	 *
	 * @param  val  The new description value
	 */
	public void setDescription(String val) {
		description = val.trim();
	}



	/**
	 *  Gets the enabled status String [true | false].
	 *
	 * @return    The enabled status.
	 */
	public String getEnabled() {
		return enabled;
	}


	/**
	 *  Gets the enabled status boolean [true | false].
	 *
	 * @return    The enabled status.
	 */
	public boolean isEnabled() {
		return enabled.equals("true");
	}


	/**
	 *  Sets the enabled status [true | false].
	 *
	 * @param  val  The String 'true' or 'false'.
	 */
	public void setEnabled(String val) {
		enabled = val.trim().toLowerCase();
	}



	/**
	 *  Gets the dirInfos attribute of the SetInfo object
	 *
	 * @return    The dirInfos value
	 */
	public ArrayList getDirInfos() {
		return dirInfos;
	}


	/**
	 *  Gets the dirInfo attribute of the SetInfo object
	 *
	 * @param  i  DESCRIPTION
	 * @return    The dirInfo value
	 */
	public DirInfo getDirInfo(int i) {
		return (DirInfo) dirInfos.get(i);
	}


	/**
	 *  Sets the dirInfo attribute of the SetInfo object
	 *
	 * @param  i        The new dirInfo value
	 * @param  dirInfo  The new dirInfo value
	 */
	public void setDirInfo(int i, DirInfo dirInfo) {
		dirInfos.set(i, dirInfo);
	}


	/**
	 *  Sets the dirInfo attribute of the SetInfo object
	 *
	 * @param  i       The new dirInfo value
	 * @param  dir     The new dirInfo value
	 * @param  format  The new dirInfo value
	 */
	public void setDirInfo(int i, String dir, String format) {
		DirInfo tmp = new DirInfo(dir, format);
		dirInfos.set(i, tmp);
	}


	/**
	 *  Determines whether a {@link DirInfo} that matches the attributes of the given {@link DirInfo} already
	 *  exists in this SetInfo.
	 *
	 * @param  dirInfo  A {@link DirInfo} to check for existance.
	 * @return          True if the {@link DirInfo} exists in this SetInfo.
	 */
	public boolean containsDirInfo(DirInfo dirInfo) {
		for (int i = 0; i < dirInfos.size(); i++)
			if (dirInfo.equals((DirInfo) dirInfos.get(i)))
				return true;
		return false;
	}


	/**
	 *  Gets the number of files for this collection.
	 *
	 * @return    The numFiles value
	 */
	public String getNumFiles() {
		return Integer.toString(numFiles);
	}


	/**
	 *  Gets the numFiles attribute of the SetInfo object
	 *
	 * @return    The numFiles value
	 */
	public int getNumFilesInt() {
		return numFiles;
	}


	/**
	 *  Sets data in the SetInfo for display in the UI. This includes things like the number of items indexed for
	 *  this collection, the number if items that are discoverable for this collection, the number of files for
	 *  this collection, and the number of index errors that were detected for this collection. Also the
	 *  collection accession status.
	 *
	 * @param  rm  The RepositoryManager whoes index contains the collections configured by this SetSpec.
	 */
	public void setSetInfoData(RepositoryManager rm) {
		try {
			//numDiscoverable = rm.getIndex().numDocs(rm.getDiscoverableItemsQuery() + " AND collection:0" + getSetSpec());
			numIndexed = rm.getIndex().getNumDocs("collection:0" + getSetSpec() + " AND deleted:false");
			
			BooleanQuery deletedQ = new BooleanQuery();
			deletedQ.add(new TermQuery(new Term("deleted", "true")), BooleanClause.Occur.MUST);
			deletedQ.add(new TermQuery(new Term("docdir", new File(getDirectory()).getAbsolutePath())), BooleanClause.Occur.MUST);			
			numDeleted = rm.getIndex().getNumDocs(deletedQ);
			
			BooleanQuery errQ = new BooleanQuery();
			errQ.add(new TermQuery(new Term("error", "true")), BooleanClause.Occur.MUST);
			errQ.add(new TermQuery(new Term("docdir", new File(getDirectory()).getAbsolutePath())), BooleanClause.Occur.MUST);
			numErrors = rm.getIndex().getNumDocs(errQ);
			
			ArrayList dirInfos = getDirInfos();
			if (dirInfos != null) {
				numFiles = 0;
				for (int i = 0; i < dirInfos.size(); i++) {
					DirInfo dirInfo = (DirInfo) dirInfos.get(i);
					String dir = dirInfo.getDirectory();
					File[] files = null;
					if (dir != null)
						files = new File(dir).listFiles(new XMLFileFilter());
					if (files != null)
						numFiles += files.length;
				}
			}
		} catch (Throwable e) {}
	}


	/**
	 *  Gets the accession status of this Set.
	 *
	 * @return    The accession status
	 */
	public String getAccessionStatus() {
		return accessionStatus;
	}


	/**
	 *  Sets the accession status for this Set.
	 *
	 * @param  val  The accession status.
	 */
	public void setAccessionStatus(String val) {
		accessionStatus = val.trim();
	}



	/**
	 *  Gets the number of items indexed for this collection.
	 *
	 * @return    The numIndexed value
	 */
	public String getNumIndexed() {
		if (numIndexed == -1)
			return "Not available";
		return Integer.toString(numIndexed);
	}


	/**
	 *  Gets the number of items indexed for this collection.
	 *
	 * @return    The numIndexed value
	 */
	public int getNumIndexedInt() {
		return numIndexed;
	}

	/**
	 *  Gets the number of items deleted for this collection.
	 *
	 * @return    The numDeleted value
	 */
	public String getNumDeleted() {
		if (numDeleted == -1)
			return "Not available";
		return Integer.toString(numDeleted);
	}

	/**
	 *  Gets the number of items deleted for this collection.
	 *
	 * @return    The numDeleted value
	 */
	public int getNumDeletedInt() {
		return numDeleted;
	}	
	

	/**
	 *  Gets the number of items that are discoverable for this collection.
	 *
	 * @return    The numDiscoverable value
	 */
	/* public String getNumDiscoverable() {
		if (numDiscoverable == -1)
			return "Not available";
		return Integer.toString(numDiscoverable);
	} */
	/**
	 *  Gets the number of indexing errors that were found for this collection.
	 *
	 * @return    The numIndexingErrors value
	 */
	public String getNumIndexingErrors() {
		if (this.numErrors == -1)
			return "Not available";
		return Integer.toString(numErrors);
	}


	/**
	 *  Gets the number of indexing errors that were found for this collection.
	 *
	 * @return    The numIndexingErrors value
	 */
	public int getNumIndexingErrorsInt() {
		return numErrors;
	}


	/**
	 *  Determines whether a directory is configured in this SetInfo.
	 *
	 * @param  directory  A file directory.
	 * @return            True if the directory is configured in this SetInfo.
	 */
	public boolean containsDirectory(File directory) {
		for (int i = 0; i < dirInfos.size(); i++) {
			DirInfo dirInfo = (DirInfo) dirInfos.get(i);
			File f = new File(dirInfo.getDirectory());
			if (f.equals(directory))
				return true;
		}
		return false;
	}


	/**
	 *  DESCRIPTION
	 *
	 * @param  i  DESCRIPTION
	 */
	public void removeDirInfo(int i) {
		dirInfos.remove(i);
	}


	/**
	 *  Adds a feature to the DirInfo attribute of the SetInfo object
	 *
	 * @param  format  The feature to be added to the DirInfo attribute
	 * @param  dir     The feature to be added to the DirInfo attribute
	 */
	public void addDirInfo(String format, String dir) {
		if (format == null || dir == null)
			return;

		if (dirInfos == null)
			dirInfos = new ArrayList();
		DirInfo di = new DirInfo(dir, format);
		if (!dirInfos.contains(di))
			dirInfos.add(new DirInfo(dir, format));
	}


	/**
	 *  Sets an attribute Object that will be available for access using the given key. The object MUST be
	 *  serializable.
	 *
	 * @param  key        The key used to reference the attribute.
	 * @param  attribute  Any Java Object that is Serializable.
	 */
	public void setAttribute(String key, Object attribute) {
		attributes.put(key, attribute);
	}


	/**
	 *  Gets an attribute Object from this SetInfo.
	 *
	 * @param  key  The key used to reference the attribute.
	 * @return      The Java Object that was stored under the given key or null if none exists.
	 */
	public Object getAttribute(String key) {
		return attributes.get(key);
	}


	/**
	 *  Provides a String representataion for this SetInfo. This method may be used for debugging to see what is
	 *  in the SetInfo. This method is also used it the {@link #equals(Object)} method.
	 *
	 * @return    String describing all data in the SetInfo.
	 */
	public String toString() {
		StringBuffer ret =
			new StringBuffer("\n Set name:\t\t" + name +
			"\n setSpec:\t\t" + setSpec +
			"\n id:\t\t\t" + id +
			"\n enabled:\t\t" + enabled +
			"\n status:\t\t" + accessionStatus);
		for (int i = 0; i < dirInfos.size(); i++) {
			ret.append("\n format:\t\t");
			ret.append(((DirInfo) dirInfos.get(i)).getFormat());
			ret.append("\n directory:\t\t");
			ret.append(((DirInfo) dirInfos.get(i)).getDirectory());
		}
		Object[] attributeKeys = attributes.keySet().toArray();
		for (int i = 0; i < attributeKeys.length; i++)
			ret.append("\n attribute:\t" + attributeKeys[i].toString());
		return ret.toString();
	}


	/**
	 *  Checks equality of two SetInfo objects.
	 *
	 * @param  o  The SetInfo to compare to this
	 * @return    True iff the compared object is equal
	 */
	public boolean equals(Object o) {
		if (o == null || !(o instanceof SetInfo))
			return false;
		try {
			return this.toString().equals(o.toString());
			/*
			 *  return ( ((((SetInfo) o).name == null && this.name == null) || ((SetInfo) o).name.equals(this.name)) &&
			 *  ((((SetInfo) o).setSpec == null && this.setSpec == null) || ((SetInfo) o).setSpec.equals(this.setSpec)) &&
			 *  ((((SetInfo) o).dirInfos == null && this.dirInfos == null) || ((SetInfo) o).dirInfos.equals(this.dirInfos)) );
			 */
		} catch (Throwable e) {
			// Catch null pointer...
			return false;
		}
	}


	/**
	 *  Compares two SetInfos by the Set Name. Collections.sort() or Arrays.sort() can thus be used to sort a
	 *  list of SetInfos by Name.
	 *
	 * @param  o                       The SetInfo to compare
	 * @return                         Returns a negative integer, zero, or a positive integer as this object is
	 *      less than, equal to, or greater than the specified object.
	 * @exception  ClassCastException  If the object passed in is not a SetInfo.
	 */
	public int compareTo(Object o)
		 throws ClassCastException {
		SetInfo other = (SetInfo) o;
		return this.getName().toLowerCase().compareTo(other.getName().toLowerCase());
	}



	private static long current = System.currentTimeMillis();


	/**
	 *  Gets the comparator attribute of the SetInfo class
	 *
	 * @param  type  The comparator type, for example 'key'
	 * @return       The comparator value
	 */
	public static Comparator getComparator(String type) {
		if (type.equals("setSpec") || type.equals("key"))
			return new SetSpecComparator();
		else if (type.equals("status"))
			return new StatusComparator();
		else if (type.equals("format"))
			return new FormatComparator();
		else if (type.equals("numFiles"))
			return new NumFilesComparator();
		else if (type.equals("numIndexed"))
			return new NumIndexedComparator();
		else if (type.equals("numDeleted"))
			return new NumDeletedComparator();		
		else if (type.equals("numIndexingErrors"))
			return new NumIndexingErrorsComparator();
		else
			return new NameComparator();
	}


	/**
	 *  Implements Comparator to enable sorting by numFiles.
	 *
	 * @author     John Weatherley
	 */
	public static class NumFilesComparator implements Comparator {
		/**
		 *  Campares the numFiles field.
		 *
		 * @param  O1                      A SetInfo Object
		 * @param  O2                      A SetInfo Object
		 * @return                         A negative integer, zero, or a positive integer as the first argument is
		 *      less than, equal to, or greater than the second.
		 * @exception  ClassCastException  If Object is not SetInfo
		 */
		public int compare(Object O1, Object O2)
			 throws ClassCastException {
			int one = ((SetInfo) O1).getNumFilesInt();
			int two = ((SetInfo) O2).getNumFilesInt();
			if (one == two)
				return 0;
			if (one > two)
				return -1;
			else
				return 1;
		}
	}


	/**
	 *  Implements Comparator to enable sorting by numIndexed.
	 *
	 * @author     John Weatherley
	 */
	public static class NumIndexedComparator implements Comparator {
		/**
		 *  Campares the numIndexed field.
		 *
		 * @param  O1                      A SetInfo Object
		 * @param  O2                      A SetInfo Object
		 * @return                         A negative integer, zero, or a positive integer as the first argument is
		 *      less than, equal to, or greater than the second.
		 * @exception  ClassCastException  If Object is not SetInfo
		 */
		public int compare(Object O1, Object O2)
			 throws ClassCastException {
			int one = ((SetInfo) O1).getNumIndexedInt();
			int two = ((SetInfo) O2).getNumIndexedInt();
			if (one == two)
				return 0;
			if (one > two)
				return -1;
			else
				return 1;
		}
	}

	/**
	 *  Implements Comparator to enable sorting by numDeleted.
	 *
	 * @author     John Weatherley
	 */
	public static class NumDeletedComparator implements Comparator {
		/**
		 *  Campares the numDeleted field.
		 *
		 * @param  O1                      A SetInfo Object
		 * @param  O2                      A SetInfo Object
		 * @return                         A negative integer, zero, or a positive integer as the first argument is
		 *      less than, equal to, or greater than the second.
		 * @exception  ClassCastException  If Object is not SetInfo
		 */
		public int compare(Object O1, Object O2)
			 throws ClassCastException {
			int one = ((SetInfo) O1).getNumDeletedInt();
			int two = ((SetInfo) O2).getNumDeletedInt();
			if (one == two)
				return 0;
			if (one > two)
				return -1;
			else
				return 1;
		}
	}	

	/**
	 *  Implements Comparator to enable sorting by numIndexingErrors.
	 *
	 * @author     John Weatherley
	 */
	public static class NumIndexingErrorsComparator implements Comparator {
		/**
		 *  Campares the numIndexingErrors field.
		 *
		 * @param  O1                      A SetInfo Object
		 * @param  O2                      A SetInfo Object
		 * @return                         A negative integer, zero, or a positive integer as the first argument is
		 *      less than, equal to, or greater than the second.
		 * @exception  ClassCastException  If Object is not SetInfo
		 */
		public int compare(Object O1, Object O2)
			 throws ClassCastException {
			int one = ((SetInfo) O1).getNumIndexingErrorsInt();
			int two = ((SetInfo) O2).getNumIndexingErrorsInt();
			if (one == two)
				return 0;
			if (one > two)
				return -1;
			else
				return 1;
		}
	}


	/**
	 *  Implements Comparator to enable sorting by setSpec.
	 *
	 * @author     John Weatherley
	 */
	public static class SetSpecComparator implements Comparator {
		/**
		 *  Campares the setSpec field.
		 *
		 * @param  O1                      A SetInfo Object
		 * @param  O2                      A SetInfo Object
		 * @return                         A negative integer, zero, or a positive integer as the first argument is
		 *      less than, equal to, or greater than the second.
		 * @exception  ClassCastException  If Object is not SetInfo
		 */
		public int compare(Object O1, Object O2)
			 throws ClassCastException {
			return ((SetInfo) O1).getSetSpec().compareTo(((SetInfo) O2).getSetSpec());
		}
	}


	/**
	 *  Implements Comparator to enable sorting by status.
	 *
	 * @author     John Weatherley
	 */
	public static class StatusComparator implements Comparator {
		/**
		 *  Campares the status field.
		 *
		 * @param  O1                      A SetInfo Object
		 * @param  O2                      A SetInfo Object
		 * @return                         A negative integer, zero, or a positive integer as the first argument is
		 *      less than, equal to, or greater than the second.
		 * @exception  ClassCastException  If Object is not SetInfo
		 */
		public int compare(Object O1, Object O2)
			 throws ClassCastException {
			return ((SetInfo) O2).getEnabled().compareTo(((SetInfo) O1).getEnabled());
		}
	}


	/**
	 *  Implements Comparator to enable sorting by format.
	 *
	 * @author     John Weatherley
	 */
	public static class FormatComparator implements Comparator {
		/**
		 *  Campares the format field.
		 *
		 * @param  O1                      A SetInfo Object
		 * @param  O2                      A SetInfo Object
		 * @return                         A negative integer, zero, or a positive integer as the first argument is
		 *      less than, equal to, or greater than the second.
		 * @exception  ClassCastException  If Object is not SetInfo
		 */
		public int compare(Object O1, Object O2)
			 throws ClassCastException {
			return ((SetInfo) O1).getFormat().compareTo(((SetInfo) O2).getFormat());
		}
	}


	/**
	 *  Implements Comparator to enable sorting by name.
	 *
	 * @author     John Weatherley
	 */
	public static class NameComparator implements Comparator {
		/**
		 *  Campares the name field.
		 *
		 * @param  O1                      A SetInfo Object
		 * @param  O2                      A SetInfo Object
		 * @return                         A negative integer, zero, or a positive integer as the first argument is
		 *      less than, equal to, or greater than the second.
		 * @exception  ClassCastException  If Object is not SetInfo
		 */
		public int compare(Object O1, Object O2)
			 throws ClassCastException {
			return ((SetInfo) O1).getName().toLowerCase().compareTo(((SetInfo) O2).getName().toLowerCase());
		}
	}

}

