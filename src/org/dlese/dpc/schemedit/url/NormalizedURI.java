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
package org.dlese.dpc.schemedit.url;

/**
 * @author Edwin Shin
 */
public interface NormalizedURI {
    
    public void normalize();
    
    /**
     * Performs the following:
     *  Case Normalization
     *  Percent-Encoding Normalization
     *  Path Segment Normalization
     *
     */
    public void normalizeSyntax();
    
    /**
     * Case Normalization (see RFC3986 6.2.2.1)
     *
     */
    public void normalizeCase();
    
    /**
     * Percent-Encoding Normalization (see RFC3986 6.2.2.2)
     *
     */
    public void normalizePercentEncoding();
    
    /**
     * Path Segment Normalization (see RFC3986 6.2.2.3)
     *
     */
    public void normalizePathSegment();
    
    /**
     * Scheme-Based Normalization (see RFC3986 6.2.3)
     *
     */
    public void normalizeByScheme();
    
    /**
     * Protocol-Based Normalization (see RFC3986 6.2.4)
     *
     */
    public void normalizeByProtocol();

}
