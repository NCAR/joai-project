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
package org.dlese.dpc.index.writer.xml;

/**
 *  Struct used to hold geospatial bounding box lat lon coordinates.
 *
 * @author    John Weatherley
 */
public class BoundingBox {
	private double n, w, e, s;


	/**
	 *  Constructor for the BoundingBox object
	 *
	 * @param  north  North lat
	 * @param  south  South lat
	 * @param  east   East lon
	 * @param  west   West lon
	 */
	public BoundingBox(
	                   double north,
	                   double south,
	                   double east,
	                   double west) throws IllegalArgumentException {
		
		// Do validity checks for values in range:
		if(!testLat(north))
			throw new IllegalArgumentException("North latitute must be in the range of -90 to 90. Invalid value: " + north);
		else if(!testLat(south))
			throw new IllegalArgumentException("South latitute must be in the range of -90 to 90. Invalid value: " + south);
		else if(!(north >= south))
			throw new IllegalArgumentException("North latitue must be greater than or equal to south. North: " + north + " South: " + south);
		else if(!testLon(east))
			throw new IllegalArgumentException("East longitude must be in the range of -180 to 180. Invalid value: " + east);
		else if(!testLon(west))
			throw new IllegalArgumentException("West longitude must be in the range of -180 to 180. Invalid value: " + west);	
		else if(west > east) { 
			// If bounding box extent crosses 180/-180, cull the search bounding box to largest of one side or the other to be compatible with the search algorithm:
			if(distance(west,180d) > distance(-180d,east))
				east = 179.999999d;
			else
				west = -179.999999d;
			//throw new IllegalArgumentException("West longitude may not be greater than east. West: " + west + " East: " + east);
		}
		
		// Trim the values to be just under the max extent
		n = trimLat(north);
		s = trimLat(south);
		e = trimLon(east);
		w = trimLon(west);		
	}

	private boolean testLat(double lat) {
		return (lat >= -90d && lat <= 90d);	
	}

	private boolean testLon(double lon) {
		return (lon >= -180d && lon <= 180d);	
	}
	
	// Return the absolute distance in degrees between two points in a plane.
	private double distance(double westOrNorth, double eastOrSouth) {
		return Math.abs(westOrNorth - eastOrSouth);	
	}
	
	// Trim the max extent of latitues
	private double trimLat(double lat) {
		if(lat > 89.999999d)
			return 89.999999d;
		if(lat < -89.999999d)
			return -89.999999d;
		return lat;		
	}
	
	// Trim the max extent of longitudes
	private double trimLon(double lon) {
		if(lon > 179.999999d)
			return 179.999999d;
		if(lon < -179.999999d)
			return -179.999999d;
		return lon;
	}
	
	/**
	 *  Gets the northCoord attribute of the BoundingBox object
	 *
	 * @return    The northCoord value
	 */
	public double getNorthCoord() {
		return n;
	}


	/**
	 *  Gets the southCoord attribute of the BoundingBox object
	 *
	 * @return    The southCoord value
	 */
	public double getSouthCoord() {
		return s;
	}


	/**
	 *  Gets the eastCoord attribute of the BoundingBox object
	 *
	 * @return    The eastCoord value
	 */
	public double getEastCoord() {
		return e;
	}


	/**
	 *  Gets the westCoord attribute of the BoundingBox object
	 *
	 * @return    The westCoord value
	 */
	public double getWestCoord() {
		return w;
	}


	/**
	 *  String representation
	 *
	 * @return    String for BoundinBox
	 */
	public String toString() {
		String str = "";
		try {
			str = "North: " + n + " South: " + s + " East: " + e + " West: " + w;
		} catch (Throwable t) {}
		return str;
	}
}


