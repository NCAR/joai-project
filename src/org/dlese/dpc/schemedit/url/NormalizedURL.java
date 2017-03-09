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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.HashMap;

/**
 * @author Edwin Shin
 */
public class NormalizedURL implements NormalizedURI {
    private URI uri;
    
    /**
     * see http://www.iana.org/assignments/uri-schemes
     * see http://www.iana.org/assignments/port-numbers
     */
    private static final HashMap<String, Integer> defaultPorts = new HashMap<String, Integer>();

    static {
        defaultPorts.put("acap", new Integer(2628));
        defaultPorts.put("afs", new Integer(1483));
        defaultPorts.put("dict", new Integer(674));
        defaultPorts.put("ftp", new Integer(21));
        defaultPorts.put("go", new Integer(1096));
        defaultPorts.put("gopher", new Integer(70));
        defaultPorts.put("http", new Integer(80));
        defaultPorts.put("https", new Integer(443));
        defaultPorts.put("imap", new Integer(143));
        defaultPorts.put("ipp", new Integer(631));
        defaultPorts.put("iris.beep", new Integer(702));
        defaultPorts.put("ldap", new Integer(389));
        defaultPorts.put("telnet", new Integer(23));
        defaultPorts.put("mtqp", new Integer(1038));
        defaultPorts.put("mupdate", new Integer(3905));
        defaultPorts.put("nfs", new Integer(2049));
        defaultPorts.put("nntp", new Integer(119));
        defaultPorts.put("pop", new Integer(110));
        defaultPorts.put("prospero", new Integer(1525));
        defaultPorts.put("rtsp", new Integer(554));
        defaultPorts.put("smtp", new Integer(25));
        defaultPorts.put("sip", new Integer(5060));
        defaultPorts.put("sips", new Integer(5061));
        defaultPorts.put("snmp", new Integer(161));
        defaultPorts.put("soap.beep", new Integer(605));
        defaultPorts.put("soap.beeps", new Integer(605));
        defaultPorts.put("telnet", new Integer(23));
        defaultPorts.put("tftp", new Integer(69));
        defaultPorts.put("vemmi", new Integer(575));
        defaultPorts.put("wais", new Integer(210));
        defaultPorts.put("xmlrpc.beep", new Integer(602));
        defaultPorts.put("xmlrpc.beeps", new Integer(602));
        defaultPorts.put("z39.50r", new Integer(210));
        defaultPorts.put("z39.50s", new Integer(210));
    }

    public NormalizedURL(String url) throws URISyntaxException {
        uri = new URI(url);
    }
    
    /* (non-Javadoc)
     * @see org.nsdl.util.uri.NormalizedURI#normalize()
     */
    public void normalize() {
        normalizeSyntax();
        normalizeByScheme();
        normalizeByProtocol();
    }

    /* (non-Javadoc)
     * @see org.nsdl.util.uri.NormalizedURI#normalizeSyntax()
     */
    public void normalizeSyntax() {
        normalizeCase();
        normalizePercentEncoding();
        normalizePathSegment();
    }

    /* (non-Javadoc)
     * @see org.nsdl.util.uri.NormalizedURI#normalizeCase()
     */
    public void normalizeCase() {
        try {
            // Scheme and host should be lowercase
            String scheme = uri.getScheme();
            String host = uri.getHost();
            String rURI = toString().replaceFirst(scheme, scheme.toLowerCase());
            if (host != null) {
                rURI = rURI.replaceFirst(host, host.toLowerCase());
            }
            
            // Percent-encoded characters should be uppercase
            if (rURI.indexOf('%') != -1) {
                Pattern p = Pattern.compile("%([a-z0-9]{2})");
                Matcher m = p.matcher(rURI);
                
                StringBuffer sb = new StringBuffer();
                int lastEnd = 0;
                while(m.find()) {
                    sb.append(rURI.substring(lastEnd, m.start()));
                    sb.append(m.group().toUpperCase());
                    lastEnd = m.end();
                }
                sb.append(rURI.substring(lastEnd, rURI.length()));
                rURI = sb.toString();
            }
            
            uri = new URI(rURI);
        } catch (URISyntaxException e) {
            // This should never be reached
            e.printStackTrace();
        }
    }

    /* (non-Javadoc)
     * @see org.nsdl.util.uri.NormalizedURI#normalizePercentEncoding()
     */
    public void normalizePercentEncoding() {
        try {
            uri = new URI(uri.getScheme(), 
                          uri.getSchemeSpecificPart(), 
                          uri.getFragment());
        } catch (URISyntaxException e) {
            // This should never be reached
            e.printStackTrace();
        }
    }

    /* (non-Javadoc)
     * @see org.nsdl.util.uri.NormalizedURI#normalizePathSegment()
     */
    public void normalizePathSegment() {
        uri = uri.normalize();
    }

    /* (non-Javadoc)
     * @see org.nsdl.util.uri.NormalizedURI#normalizeByScheme()
     */
    public void normalizeByScheme() {
        String rURI = toString();
        String scheme = uri.getScheme();
        String authority = uri.getAuthority();
        String host = uri.getHost();
        int port = uri.getPort();
        String path = uri.getPath();
        
        if (port == defaultPort(scheme)) {
            rURI = rURI.replaceFirst(":" + port, "");
            try {
                uri = new URI(rURI);
            } catch (URISyntaxException e) {
                // This should never be reached
                e.printStackTrace();
            }
        }
        
        if (port == -1 && authority != null && authority.endsWith(":")) {
            rURI = rURI.replaceFirst(authority, authority.substring(0, authority.length() -1));
            try {
                uri = new URI(rURI);
            } catch (URISyntaxException e) {
                // This should never be reached
                e.printStackTrace();
            }
        }
        
        if (path == null || path.length() == 0) {
            if (host != null) {
                rURI = rURI.replaceFirst(host, host + '/');
            } else {
                rURI = rURI.replaceFirst(authority, authority + '/');
            }
            try {
                uri = new URI(rURI);
            } catch (URISyntaxException e) {
                // This should never be reached
                e.printStackTrace();
            }
        }
    }

    /* (non-Javadoc)
     * @see org.nsdl.util.uri.NormalizedURI#normalizeByProtocol()
     */
    public void normalizeByProtocol() {
        // TODO Auto-generated method stub
        
    }
    
    public String toString() {
        return uri.toASCIIString();
    }
    
    /**
     * Return the default port used by a given scheme.
     * 
     * @param the scheme, e.g. http
     * @return the port number, or 0 if unknown
     */
    private final static int defaultPort(String scheme) {
        Integer port = defaultPorts.get(scheme.trim().toLowerCase());
        return (port != null) ? port.intValue() : 0;
    }
}
