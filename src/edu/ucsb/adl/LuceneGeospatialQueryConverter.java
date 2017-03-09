///////////////////////////////////////////////////////////////////////////////
//                                                                           //
//                        ALEXANDRIA DIGITAL LIBRARY                         //
//                 University of California at Santa Barbara                 //
//                                                                           //
// ------------------------------------------------------------------------- //
//                                                                           //
//     Copyright (c) 2004 by the Regents of the University of California     //
//                            All rights reserved                            //
//                                                                           //
// Redistribution and use in source and binary forms, with or without        //
// modification, are permitted provided that the following conditions are    //
// met:                                                                      //
//                                                                           //
//     1. Redistributions of source code must retain the above copyright     //
//        notice, this list of conditions, and the following disclaimer.     //
//                                                                           //
//     2. Redistributions in binary form must reproduce the above copyright  //
//        notice, this list of conditions, and the following disclaimer in   //
//        the documentation and/or other materials provided with the         //
//        distribution.                                                      //
//                                                                           //
//     3. All advertising materials mentioning features or use of this       //
//        software must display the following acknowledgement: This product  //
//        includes software developed by the Alexandria Digital Library,     //
//        University of California at Santa Barbara, and its contributors.   //
//                                                                           //
//     4. Neither the name of the University nor the names of its            //
//        contributors may be used to endorse or promote products derived    //
//        from this software without specific prior written permission.      //
//                                                                           //
// THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS "AS IS" AND ANY //
// EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED //
// WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, ARE   //
// DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR  //
// ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL    //
// DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS   //
// OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)     //
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,       //
// STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN  //
// ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE           //
// POSSIBILITY OF SUCH DAMAGE.                                               //
//                                                                           //
///////////////////////////////////////////////////////////////////////////////

package edu.ucsb.adl;

import java.text.DecimalFormat;

import org.apache.lucene.index.Term;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.search.TermQuery;

/**
 *  Converts a geospatial query to a <a target="_top" href="http://jakarta.apache.org/lucene/">Lucene</a>
 *  query. <p>
 *
 *  The conversion assumes that each Lucene document optionally has associated with it a <em>geographic
 *  footprint</em> (a geographic region representing the document's area of relevance) in the form of a <em>
 *  box</em> (defined below). A <em>geospatial query</em> takes a query region (also in the form of a box) and
 *  a spatial predicate (e.g., "<code>overlaps</code>") and returns all documents that 1) have a geographic
 *  footprint that 2) has the predicate relationship to the query region. <p>
 *
 *  Formally, a <em>box</em> is a geographic region defined by north and south bounding coordinates (latitudes
 *  expressed in degrees north of the equator and in the range [-90,90]) and east and west bounding
 *  coordinates (longitudes expressed in degrees east of the Greenwich meridian and in the range [-180,180]).
 *  The north bounding coordinate must be greater than or equal to the south. The west bounding coordinate may
 *  be less than, equal to, or greater than the east; in the latter case, a box that crosses the
 *  &plusmn;180&deg; meridian is described. As a special case, the set of all longitudes is described by a
 *  west bounding coordinate of -180 and an east bounding coordinate of 180. <p>
 *
 *  Boxes must be represented in Lucene as four indexed, <strong>non-tokenized</strong> fields holding the
 *  four bounding coordinates. In addition, in order to emulate numeric comparisons using lexicographic
 *  comparisons, before insertion into Lucene <strong>bounding coordinates must be encoded using the {@link
 *  #encodeLatitude(double) encodeLatitude} and {@link #encodeLongitude(double) encodeLongitude} functions
 *  </strong> supplied by this class. <p>
 *
 *  Two limitations: first, this converter assumes that each Lucene document has at most one associated
 *  geographic footprint. Second, this converter assumes that <em>no</em> document footprint crosses the
 *  &plusmn;180&deg; meridian. However, query regions that cross the &plusmn;180&deg; meridian are allowed and
 *  are handled correctly, as are all discontinuities involving that meridian and the poles.
 *
 * @author     <a href="mailto:gjanee@alexandria.ucsb.edu">Greg Jan&eacute;e</a> <br>
 *      <a href="http://www.alexandria.ucsb.edu/">Alexandria Digital Library Project</a>
 * @version    $Header: /cvsroot/dlsciences/dlese-tools-project/src/edu/ucsb/adl/LuceneGeospatialQueryConverter.java,v
 *      1.2 2007/07/05 22:29:34 jweather Exp $ <p>
 *
 *      $Log: LuceneGeospatialQueryConverter.java,v $
 *      Revision 1.5  2010/07/14 00:18:48  jweather
 *      -All Lucene-related classes have been upgraded to Lucene v3.0.2. Major changes include:
 *      --Search results are now returned in a new ResultDocList Object instead of a ResultDoc[]
 *        array. This provides for more efficient searching (does not require an additional
 *        loop over results as before) and expandability (methods can be added to the ResultDocList
 *        to support future fuctnionality) and better utilizes the built-in Lucene classes
 *        for search than before (TopDocs, Sort, etc.)
 *      --Uses Lucene Sort class for sorting at search time. Replaces logic that sorted
 *        results after the search (deprecated but still supported for backward-compatibility)
 *
 *      - Final previous version was tagged in CVS with 'lucene_2_4_final_version'
 *
 *      Revision 1.4  2009/01/08 00:40:36  jweather
 *      -A geospatial search query can now be conducted in the DDSWS service by supplying the following arguments:
 *      geoPredicate, geoBBNorth, geoBBWest, geoBBEast, geoBBSouth, geoClause. Previously, geospatial queries
 *      needed to be separately encoded and then supplied as part of the query (q) argument.
 *
 *      -Added a 'code' attribute to the error response element in DDSWS 1.1 that indicates the type of error. This makes it possible to determine
 *      the reason why a request failed and respond appropriately to users. Error codes include noRecordsMatch, badArgument, badVerb,
 *      badQuery, notAuthorized, internalServerError. Note that, while this should not effect consumers of XML response from the service,
 *      the structure of the JSON output has changed slightly and my effect clients that use it.
 *
 *      -Updated the JSON example client to handle the new error code data format.
 *
 *      -If geospatial bounding box footprint extent crosses longitude 180/-180, the indexer now culls the search bounding box to the
 *      largest of one side or the other to be compatible with the search algorithm.
 *
 *      -More robust error checking is now conducted in the indexer to ensure geospatial bounding box coordinates error free.
 *      If a bounding box is non-conformant, it is gracefully dropped from the given record.
 *
 *      -In the DDSWS search service, if the query crosses the 180/-180 longitude it is split into two query regions,
 *      one on each side, joined by boolean clause.
 *
 *      Revision 1.3  2008/05/24 00:44:35  jweather
 *      -fixed problem with geospatial tokens for search, introduced with lucene 2.0 upgrade
 *      -created EL functions for generating geospatial queries
 * Revision 1.2 2007/07/05 22:29:34 jweather -Upgradded
 *      from Lucene v1.4.3 to Lucene v2.2.0 (code merged from lucene-2-upgrade-branch to head) Revision
 *      1.1.6.1 2007/07/05 21:12:24 jweather -Updated Field BooleanQuery method calls to use Lucene 2.0 syntax
 *      Revision 1.1 2004/09/07 18:58:16 jweather The Lucene bounding box query converter/encoder, contributed
 *      by Greg Jenee at ADL Revision 1.1 2004/08/31 18:16:26 gjanee Initial revision <p>
 *
 *
 */
public class LuceneGeospatialQueryConverter {

	private LuceneGeospatialQueryConverter() { }



	/**
	 *  Converts a geospatial query to a Lucene query. Assumes the fields containing the bounds are named
	 *  northCoord, southCoord, eastCoord, and westCoord.
	 *
	 * @param  predicate                  The spatial predicate, which must be "<code>contains</code>", "<code>overlaps</code>
	 *      ", or "<code>within</code>".
	 * @param  north                      The north bounding coordinate of the query region.
	 * @param  south                      The south bounding coordinate of the query region.
	 * @param  east                       The east bounding coordinate of the query region.
	 * @param  west                       The west bounding coordinate of the query region.
	 * @return                            Lucene Query
	 * @exception  NumberFormatException  If unable to convert bounding coordinate String to double
	 * @throws  IllegalArgumentException  If <code>predicate</code> is not one of the three supported spatial
	 *      predicates; if <code>north</code> or <code>south</code> are outside the range [-90,90]; if <code>north</code>
	 *      is less than <code>south</code>; or if <code>east</code> or <code>west</code> are outside the range
	 *      [-180,180].
	 */
	public static Query convertQuery(String predicate, String north, String south,
	                                 String east, String west) throws IllegalArgumentException, NumberFormatException {
		double northVal = 0d;
		double southVal = 0d;
		double eastVal = 0d;
		double westVal = 0d;
		
		try {
			northVal = Double.parseDouble(north);
		} catch (Exception e) {
			throw new NumberFormatException("Illegal north coordinate: " + e.getMessage());	
		}
		
		try {
			southVal = Double.parseDouble(south);
		} catch (Exception e) {
			throw new NumberFormatException("Illegal south coordinate: " + e.getMessage());	
		}
		
		try {
			eastVal = Double.parseDouble(east);
		} catch (Exception e) {
			throw new NumberFormatException("Illegal east coordinate: " + e.getMessage());	
		}
		
		try {
			westVal = Double.parseDouble(west);
		} catch (Exception e) {
			throw new NumberFormatException("Illegal west coordinate: " + e.getMessage());	
		}
										 
		return convertQuery("northCoord",
			"southCoord",
			"eastCoord",
			"westCoord",
			predicate,
			northVal,
			southVal,
			eastVal,
			westVal);
	}


	/**
	 *  Converts a geospatial query to a Lucene query.
	 *
	 * @param  northField                 The Lucene field holding north bounding coordinates.
	 * @param  southField                 The Lucene field holding south bounding coordinates.
	 * @param  eastField                  The Lucene field holding east bounding coordinates.
	 * @param  westField                  The Lucene field holding west bounding coordinates.
	 * @param  predicate                  The spatial predicate, which must be "<code>contains</code>", "<code>overlaps</code>
	 *      ", or "<code>within</code>".
	 * @param  north                      The north bounding coordinate of the query region.
	 * @param  south                      The south bounding coordinate of the query region.
	 * @param  east                       The east bounding coordinate of the query region.
	 * @param  west                       The west bounding coordinate of the query region.
	 * @return                            Lucene Query
	 * @throws  IllegalArgumentException  If <code>predicate</code> is not one of the three supported spatial
	 *      predicates; if <code>north</code> or <code>south</code> are outside the range [-90,90]; if <code>north</code>
	 *      is less than <code>south</code>; or if <code>east</code> or <code>west</code> are outside the range
	 *      [-180,180].
	 */
	public static Query convertQuery(
	                                 String northField, String southField, String eastField,
	                                 String westField, String predicate, double north, double south,
	                                 double east, double west) throws IllegalArgumentException {
		if (predicate == null || (!predicate.equals("within") && !predicate.equals("overlaps") &&
			!predicate.equals("contains"))) {
			throw new IllegalArgumentException("Illegal or missing spatial predicate. Must be one of [within|contains|overlaps].");
		}
		if (north < -90.0 || north > 90.0) {
			throw new IllegalArgumentException("North out of range. Must be a value from -90 to 90.");
		}
		if (south < -90.0 || south > 90.0) {
			throw new IllegalArgumentException("South out of range. Must be a value from -90 to 90.");
		}
		if (north < south) {
			throw new IllegalArgumentException("North cannot be less than south");
		}
		if (east < -180.0 || east > 180.0) {
			throw new IllegalArgumentException("East out of range. Must be a value from -180 to 180.");
		}
		if (west < -180.0 || west > 180.0) {
			throw new IllegalArgumentException("Wast out of range. Must be a value from -180 to 180.");
		}
		if (north == -90.0) {
			if (predicate.equals("contains") || predicate.equals("overlaps")) {
				return EQ_lat(southField, -90.0);
			}
			else { // within
				return EQ_lat(northField, -90.0);
			}
		}
		else if (south == 90.0) {
			if (predicate.equals("contains") || predicate.equals("overlaps")) {
				return EQ_lat(northField, 90.0);
			}
			else { // within
				return EQ_lat(southField, 90.0);
			}
		}
		else {
			if (west > east) {
				if (west == 180.0) {
					west = -180.0;
				}
				else if (east == -180.0) {
					east = 180.0;
				}
			}
			if (west <= east) {
				if (predicate.equals("contains")) {
					Query q = AND(GE_lat(northField, north),
						LE_lat(southField, south));
					if (west == east && (west == 180.0 || west == -180.0)) {
						q = AND(q, OR(EQ_lon(eastField, 180.0),
							EQ_lon(westField, -180.0)));
					}
					else {
						q = AND(q, AND(GE_lon(eastField, east),
							LE_lon(westField, west)));
					}
					return q;
				}
				else if (predicate.equals("within")) {
					Query ns = null;
					Query outerNs = null;
					if (north == 90.0) {
						outerNs = EQ_lat(southField, 90.0);
					}
					else {
						ns = LE_lat(northField, north);
					}
					if (south == -90.0) {
						outerNs = OR(outerNs, EQ_lat(northField, -90.0));
					}
					else {
						ns = AND(ns, GE_lat(southField, south));
					}
					Query ew = null;
					Query outerEw = null;
					if (west != -180.0 || east != 180.0) {
						if (east == 180.0 || east == -180.0) {
							outerEw = EQ_lon(eastField, -180.0);
						}
						else {
							ew = LE_lon(eastField, east);
						}
						if (west == 180.0 || west == -180.0) {
							outerEw = OR(outerEw, EQ_lon(westField, 180.0));
						}
						else {
							ew = AND(ew, GE_lon(westField, west));
						}
						ew = OR(ew, outerEw);
					}
					Query q;
					if (ew != null) {
						q = OR(AND(ns, ew), outerNs);
					}
					else {
						if (ns != null) {
							q = ns;
						}
						else {
							// constant true; all documents match
							q = GE_lat(northField, -90.0);
						}
					}
					return q;
				}
				else { // overlaps
					Query ns = null;
					Query outerNs = null;
					if (north == 90.0) {
						outerNs = EQ_lat(southField, 90.0);
					}
					else {
						ns = LE_lat(southField, north);
					}
					if (south == -90.0) {
						outerNs = OR(outerNs, EQ_lat(northField, -90.0));
					}
					else {
						ns = AND(ns, GE_lat(northField, south));
					}
					Query ew = null;
					Query outerEw = null;
					if (west != -180.0 || east != 180.0) {
						if (east == 180.0 || east == -180.0) {
							outerEw = EQ_lon(westField, -180.0);
						}
						else {
							ew = LE_lon(westField, east);
						}
						if (west == 180.0 || west == -180.0) {
							outerEw = OR(outerEw, EQ_lon(eastField, 180.0));
						}
						else {
							ew = AND(ew, GE_lon(eastField, west));
						}
						ew = OR(ew, outerEw);
					}
					Query q;
					if (ew != null) {
						q = OR(AND(ns, ew), outerNs);
					}
					else {
						if (ns != null) {
							q = ns;
						}
						else {
							// constant true; all documents match
							q = GE_lat(northField, -90.0);
						}
					}
					return q;
				}
			}
			else {
				if (predicate.equals("contains")) {
					return AND(GE_lat(northField, north),
						AND(LE_lat(southField, south),
						AND(EQ_lon(eastField, 180.0),
						EQ_lon(westField, -180.0))));
				}
				else if (predicate.equals("within")) {
					Query ns = null;
					Query outerNs = null;
					if (north == 90.0) {
						outerNs = EQ_lat(southField, 90.0);
					}
					else {
						ns = LE_lat(northField, north);
					}
					if (south == -90.0) {
						outerNs = OR(outerNs, EQ_lat(northField, -90.0));
					}
					else {
						ns = AND(ns, GE_lat(southField, south));
					}
					return OR(AND(ns,
						OR(LE_lon(eastField, east),
						GE_lon(westField, west))),
						outerNs);
				}
				else { // overlaps
					Query ns = null;
					Query outerNs = null;
					if (north == 90.0) {
						outerNs = EQ_lat(southField, 90.0);
					}
					else {
						ns = LE_lat(southField, north);
					}
					if (south == -90.0) {
						outerNs = OR(outerNs, EQ_lat(northField, -90.0));
					}
					else {
						ns = AND(ns, GE_lat(northField, south));
					}
					return OR(AND(ns,
						OR(LE_lon(westField, east),
						GE_lon(eastField, west))),
						outerNs);
				}
			}
		}
	}


	private static Query EQ_lat(String field, double value) {
		return new TermQuery(new Term(field, encodeLatitude(value)));
	}


	private static Query GE_lat(String field, double value) {
		if (value == 90.0) {
			return EQ_lat(field, value);
		}
		else {
			return new TermRangeQuery (field, encodeLatitude(value),encodeLatitude(90.0),true,true);
		}
	}


	private static Query LE_lat(String field, double value) {
		if (value == -90.0) {
			return EQ_lat(field, value);
		}
		else {
			return new TermRangeQuery(field, encodeLatitude(-90.0),encodeLatitude(value),true,true);
		}
	}


	private static Query EQ_lon(String field, double value) {
		return new TermQuery(new Term(field, encodeLongitude(value)));
	}


	private static Query GE_lon(String field, double value) {
		if (value == 180.0) {
			return EQ_lon(field, value);
		}
		else {
			return new TermRangeQuery (field, encodeLongitude(value), encodeLongitude(180.0),true,true);
		}
	}


	private static Query LE_lon(String field, double value) {
		if (value == -180.0) {
			return EQ_lon(field, value);
		}
		else {
			return new TermRangeQuery (field,encodeLongitude(-180.0),encodeLongitude(value),true,true);
		}
	}


	private static Query AND(Query q1, Query q2) {
		if (q1 == null)
			return q2;
		if (q2 == null)
			return q1;
		if (q1 instanceof BooleanQuery &&
			((BooleanQuery) q1).getClauses()[0].isRequired()) {
			if (q2 instanceof BooleanQuery &&
				((BooleanQuery) q2).getClauses()[0].isRequired()) {
				BooleanClause[] clauses = ((BooleanQuery) q2).getClauses();
				for (int i = 0; i < clauses.length; ++i) {
					((BooleanQuery) q1).add(clauses[i]);
				}
			}
			else {
				((BooleanQuery) q1).add(q2, BooleanClause.Occur.MUST);
			}
			return q1;
		}
		else {
			if (q2 instanceof BooleanQuery &&
				((BooleanQuery) q2).getClauses()[0].isRequired()) {
				((BooleanQuery) q2).add(q1, BooleanClause.Occur.MUST);
				return q2;
			}
			else {
				BooleanQuery q = new BooleanQuery();
				q.add(q1, BooleanClause.Occur.MUST);
				q.add(q2, BooleanClause.Occur.MUST);
				return q;
			}
		}
	}


	private static Query OR(Query q1, Query q2) {
		if (q1 == null)
			return q2;
		if (q2 == null)
			return q1;
		if (q1 instanceof BooleanQuery &&
			!((BooleanQuery) q1).getClauses()[0].isRequired()) {
			if (q2 instanceof BooleanQuery &&
				!((BooleanQuery) q2).getClauses()[0].isRequired()) {
				BooleanClause[] clauses = ((BooleanQuery) q2).getClauses();
				for (int i = 0; i < clauses.length; ++i) {
					((BooleanQuery) q1).add(clauses[i]);
				}
			}
			else {
				((BooleanQuery) q1).add(q2, BooleanClause.Occur.SHOULD);
			}
			return q1;
		}
		else {
			if (q2 instanceof BooleanQuery &&
				!((BooleanQuery) q2).getClauses()[0].isRequired()) {
				((BooleanQuery) q2).add(q1, BooleanClause.Occur.SHOULD);
				return q2;
			}
			else {
				BooleanQuery q = new BooleanQuery();
				q.add(q1, BooleanClause.Occur.SHOULD);
				q.add(q2, BooleanClause.Occur.SHOULD);
				return q;
			}
		}
	}


	private static DecimalFormat c_latitudeFormatter = new DecimalFormat("y000.0###");


	/**
	 *  Encodes a latitude to a textual form that supports numeric ordering.
	 *
	 * @param  latitude                   The latitude.
	 * @return                            The encoded latitude.
	 * @throws  IllegalArgumentException  If <code>latitude</code> is outside the range [-90,90].
	 */
	public static String encodeLatitude(double latitude)
		 throws IllegalArgumentException {
		if (latitude < -90.0 || latitude > 90.0) {
			throw new IllegalArgumentException("latitude out of range");
		}
		synchronized (c_latitudeFormatter) {
			return c_latitudeFormatter.format(latitude + 90.0).replace('.', 'd');
		}
	}


	/**
	 *  Encodes a latitude to a textual form that supports numeric ordering.
	 *
	 * @param  latitude                   The latitude.
	 * @return                            The encoded latitude.
	 * @exception  NumberFormatException  If unable to convert latitude String to double
	 * @throws  IllegalArgumentException  If <code>latitude</code> is outside the range [-90,90].
	 */
	public static String encodeLatitude(String latitude)
		 throws IllegalArgumentException, NumberFormatException {
		return encodeLatitude(Double.parseDouble(latitude));
	}


	private static DecimalFormat c_longitudeFormatter = new DecimalFormat("x000.0###");


	/**
	 *  Encodes a longitude to a textual form that supports numeric ordering.
	 *
	 * @param  longitude                  The longitude.
	 * @return                            The encoded longitude.
	 * @throws  IllegalArgumentException  If <code>longitude</code> is outside the range [-180,180].
	 */
	public static String encodeLongitude(double longitude)
		 throws IllegalArgumentException {
		if (longitude < -180.0 || longitude > 180.0) {
			throw new IllegalArgumentException("longitude out of range");
		}
		synchronized (c_longitudeFormatter) {
			return c_longitudeFormatter.format(longitude + 180.0).replace('.', 'd');
		}
	}


	/**
	 *  Encodes a longitude to a textual form that supports numeric ordering.
	 *
	 * @param  longitude                  The longitude.
	 * @return                            The encoded longitude.
	 * @exception  NumberFormatException  If unable to convert longitute String to double
	 * @throws  IllegalArgumentException  If <code>longitude</code> is outside the range [-180,180].
	 */
	public static String encodeLongitude(String longitude)
		 throws IllegalArgumentException, NumberFormatException {
		return encodeLongitude(Double.parseDouble(longitude));
	}


	/**
	 *  Test driver.
	 *
	 * @param  args  The command line arguments
	 */
	public static void main(String[] args) {
		if (args.length != 5) {
			System.err.println(
				"usage: java edu.ucsb.adl.LuceneGeospatialQueryConverter " +
				"predicate north south east west");
			System.exit(1);
		}
		Query q = convertQuery("northCoord", "southCoord", "eastCoord", "westCoord", args[0],
			Double.valueOf(args[1]).doubleValue(),
			Double.valueOf(args[2]).doubleValue(),
			Double.valueOf(args[3]).doubleValue(),
			Double.valueOf(args[4]).doubleValue());
		System.out.println(q.toString());
	}

}

