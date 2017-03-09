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
package org.dlese.dpc.ldap;

/**
 * Stores the names and values of the attributes for an LDAP entry.
 * Although the values can be any Serializable Object,
 * the methods used to retrieve values convert them to
 * Strings.  Currently DLESE's use of attributes is entirely
 * Strings; at some future point we may choose to store
 * Objects in attribute values.
 */

public class LdapEntry {

String dn;

/**
 * Stores the names and values of the attributes for an LDAP entry.
 * <p>
 * Each row i represents one attribute and it's values:
 * attrs[i][0] is the String attribute name,
 * and attrs[i][1 ... rowlen-1] are the Object values.
 * <p>
 * Caution: the attrs matrix may not be rectangular, since
 * different attributes may have different numbers of values.
 */

private Object[][] attrs = null;

public LdapEntry( String dn) {
	this.dn = dn;
}

/**
 * Returns a String representation of the entire set of attributes
 * represented by this LdapEntry.  Used for debugging.
 */

public String toString() {
	String res = "dn: \"" + dn + "\"\n";
	if (attrs == null) res += "    (attrs == null)\n";
	else {
		// For each attribute:
		for (int iat = 0; iat < attrs.length; iat++) {
			if (attrs[iat].length <= 1)				// no values
				res += "    " + attrs[iat][0] + ": (no values)\n";
			else if (attrs[iat].length == 2)		// only 1 value
				res += "    " + attrs[iat][0] + ": "
					+ formatValue( attrs[iat][1]) + "\n";
			else {									// multiple values
				res += "    " + attrs[iat][0] + ":\n";
				for (int ival = 1; ival < attrs[iat].length; ival++) {
					res += "        " + formatValue( attrs[iat][ival])
						+ "\n";
				}
			} // else multiple values
		} // for iat
	} // else attrs not null
	return res;
} // end toString





private String formatValue( Object val) {
	String valstg = "";
	if (val == null) valstg = "(null)";
	else {
		// Format byte vectors specially.
		// Used for passwords.
		String clsnm = val.getClass().getName();
		if (clsnm.equals("[B")) {	// "]" what a kluge is java
			byte[] bvec = (byte[]) val;
			valstg = "(" + bvec.length + " bytes): \"";
			for (int ii = 0; ii < bvec.length; ii++) {
				valstg += (char) (bvec[ii] & 0xff);
			}
			valstg += "\"";
		}
		else if ( ! (val instanceof String))
			valstg = "(" + clsnm + ") \"" + val.toString() + "\"";
		else valstg = "\"" + val.toString() + "\"";
	}
	return valstg;
}



/**
 * Allocates the <code>attrs </code> matrix to the specified
 * number of rows.
 */

void allocAttrs( int nrows) {
	attrs = new Object[nrows][0];
}


/**
 * Allocates one row of the <code>attrs </code> matrix to
 * the specified number of columns.
 *
 * @param irow  The row to be allocated.
 * @param ncols  The number of columns (cells) to allocate.
 */

void allocAttrsRow( int irow, int ncols) {
	attrs[irow] = new Object[ncols];
}


/**
 * Sets an entire row of the <code>attrs </code> matrix to
 * the specified values.
 * <p>
 * Each row i represents one attribute and it's values:
 * attrs[i][0] is the String attribute name,
 * and attrs[i][1 ... rowlen-1] are the Object values.
 *
 * @param irow  The row to be set.
 * @param vals  The values to use for the row.
 */

void setAttrsRow( int irow, Object[] vals) {
	attrs[irow] = new Object[ vals.length];
	for (int jj = 0; jj < vals.length; jj++) {
		attrs[irow][jj] = vals[jj];
	}
}



/**
 * Sets a single element of the <code>attrs </code> matrix.
 * <p>
 * Each row i represents one attribute and it's values:
 * attrs[i][0] is the String attribute name,
 * and attrs[i][1 ... rowlen-1] are the Object values.
 *
 * @param irow  The row to be set.
 * @param icol  The column to be set.
 * @param val  The value to use.
 */

void setAttr( int irow, int icol, Object val) {
	attrs[irow][icol] = val;
}


/**
 * Returns the dn (distinguished name) associated with this entry.
 */
public String getDn() {
	return dn;
}


/**
 * Returns the number of rows in the <code>attrs </code> matrix.
 */

public int getAttrsRows() {
	if (attrs == null) return 0;
	else return attrs.length;
}


/**
 * Returns a 1-dimensional array of the attribute names
 * stored in the <code>attrs </code> matrix.
 */

public String[] getAttrNames() {
	String[] names = new String[ attrs.length];
	for (int ii = 0; ii < names.length; ii++) {
		names[ii] = attrs[ii][0].toString();
	}
	return names;
}
	

/**
 * Returns the attribute name
 * stored in the specified row of the <code>attrs </code> matrix.
 */

public String getAttrName( int ii) {
	return attrs[ii][0].toString();
}
	


/**
 * Returns a 1-dimensional array of the values associated
 * with the specified row of the <code>attrs </code> matrix.
 * If no values were associated with the attribute name,
 * returns a length 0 array.
 */

public String[] getAttrStrings( int irow) {
	// First col in each row is the attribute name.  Skip it.
	String[] vals = new String[ attrs[irow].length - 1];
	for (int jj = 0; jj < vals.length; jj++) {
		vals[jj] = attrs[irow][jj + 1].toString();
	}
	return vals;
}
	

/**
 * Returns a 1-dimensional array of the values associated
 * with the specified attribute name.
 * Uses case-insensitive matching on the attribute name.
 * Returns null if attrName not found.
 */

public String[] getAttrStrings( String attrName) {
	String lowname = attrName.toLowerCase();
	int ix = -1;
	for (int ii = 0; ii < attrs.length; ii++) {
		String sname = attrs[ii][0].toString();
		if (sname.toLowerCase().equals( lowname)) {
			ix = ii;
			break;
		}
	}
	String[] resvec = null;
	if (ix >= 0) resvec = getAttrStrings( ix);
	return resvec;
}


} // end class LdapEntry



