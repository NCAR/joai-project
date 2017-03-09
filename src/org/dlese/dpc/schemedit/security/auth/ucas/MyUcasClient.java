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
package org.dlese.dpc.schemedit.security.auth.ucas;

import org.dlese.dpc.schemedit.SchemEditUtils;

import edu.ucar.cisl.authenticator.client.*;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.Properties;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import edu.ucar.cisl.authenticator.domain.Authentication;

/**
 *  Extend the UCAS AuthClient class to implement the properties file as a
 *  static variable so its location can be determined and set by LoginModule.<p>
 *
 *  NOTE: this code based almost entirely on edu.ucar.cisl.authenticator.client.AuthClient;
 *
 * @author    Jonathan Ostwald
 */
public class MyUcasClient {
	private static boolean debug = false;
	private static String propsPath = null;
	protected static boolean initialized = false;
	protected static String host = "";
	/**  NOT YET DOCUMENTED */
	public final static String PASSWORD = "password";
	/**  NOT YET DOCUMENTED */
	public final static String TOKEN = "token";
	protected static String serverLogin = "";
	protected static String serverPassword = "";


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  authType                 NOT YET DOCUMENTED
	 * @param  username                 NOT YET DOCUMENTED
	 * @param  password                 NOT YET DOCUMENTED
	 * @return                          NOT YET DOCUMENTED
	 * @exception  AuthClientException  NOT YET DOCUMENTED
	 */
	public static boolean authenticate(String authType, String username, String password) throws AuthClientException {
		Authentication auth = getAuthentication(authType, username, password);
		return auth.isValid();
	}


	/**
	 *  Gets the authentication attribute of the AuthClient class
	 *
	 * @param  authType                 NOT YET DOCUMENTED
	 * @param  username                 NOT YET DOCUMENTED
	 * @param  password                 NOT YET DOCUMENTED
	 * @return                          The authentication value
	 * @exception  AuthClientException  NOT YET DOCUMENTED
	 */
	public static Authentication getAuthentication(String authType, String username, String password) throws AuthClientException {
		setup();
		String url = "https://" + host + "/authenticator/rrh/authentication";
		if (authType == null || !(authType.equalsIgnoreCase(AuthClient.PASSWORD)
			 || authType.equalsIgnoreCase(AuthClient.TOKEN)))
			throw new AuthClientException("Invalid authType. It must be either " + AuthClient.PASSWORD +
				" or " + AuthClient.TOKEN);
		try {
			URL u = new URL(url);
			prtln("\n trying to establish connection");
			HttpsURLConnection uc = (HttpsURLConnection) u.openConnection();
			prtln("  ... OK");
			JSONObject jo = new JSONObject();
			jo.put("authType", authType);
			jo.put("username", username);
			jo.put("password", password);
			prtln("\n JSON: " + jo.toString());
			uc.setRequestMethod("PUT");
			uc.setRequestProperty("Content-Type", "application/json");
			uc.setDoOutput(true);
			prtln("getting output stream");
			OutputStream out = uc.getOutputStream();
			DataOutputStream od = new DataOutputStream(out);
			prtln("\n writing bytes to output stream");
			od.writeBytes(jo.toString());
			out.close();

			prtln("\n sent PUT");

			int status = uc.getResponseCode();
			prtln ("The status code is " + status);
			if (status == 401)
				throw new AuthClientException("Invalid login/password to connect to auth service server. Login=" + serverLogin + " password=" + serverPassword);
			if (status == 500)
				throw new AuthClientException("Auth web service server error");
			InputStream in = uc.getInputStream();
			BufferedReader d = new BufferedReader(new InputStreamReader(in));
			String buffer = d.readLine();
			if (status == 399)
				throw new AuthClientException(buffer);
			jo = new JSONObject(buffer);
			Authentication auth = new Authentication();
			auth.setValid(jo.getBoolean("valid"));
			auth.setUsername(jo.getString("username"));
			auth.setAuthType(jo.getString("authType"));
			if (auth.isValid()) {
				auth.setFirstName(jo.getString("firstName"));
				auth.setLastName(jo.getString("lastName"));
			}
			return auth;
		} catch (MalformedURLException me) {
			//logger.error("Invalid URI. ", me);
			throw new AuthClientException("Invalid URI. ", me);
		} catch (JSONException je) {
			//logger.error("Error in JSON.", je);
			throw new AuthClientException("Error in JSON.", je);
		} catch (IOException ioe) {
			//logger.error("IOException.", ioe);
			throw new AuthClientException("IOException.", ioe);
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  args  NOT YET DOCUMENTED
	 */
	public static void main(String args[]) {
		try {
			boolean result = false;
			Authentication auth = MyUcasClient.getAuthentication("password", "bsun_", "bsun");
			//    result=AuthClient.validate("password", "bsun");
			System.out.println("result is " + result);
			//result=client.authenticate("token", "bsun", "261-3394");
			//result=client.validate("password", "bsun");
			//System.out.println("result is " + result);
		} catch (AuthClientException ex) {
			System.out.println(ex.getMessage());
			Exception e = ex.getException();
			if (e != null)
				e.printStackTrace();
		}
	}


	/**
	 *  Sets the propsPath attribute of the MyUcasClient class
	 *
	 * @param  path  The new ppropsPath value
	 */
	public static void setPropsPath(String path) {
		prtln("set props path to: " + path);
		propsPath = path;
	}


	/**
	 *  Set up from propsfile - most of this method is copied from UCAS AuthClient.
	 *
	 * @exception  AuthClientException  NOT YET DOCUMENTED
	 */
	public static void setup() throws AuthClientException {
		prtln("setup");
		if (initialized == true)
			return;

		if (propsPath == null)
			throw new AuthClientException("propFile not initialized");

		File propertiesFile = new File(propsPath);

		Properties props = new Properties();
		try {
			props.load(new FileInputStream(propertiesFile));
		} catch (FileNotFoundException fnfe) {
			throw new AuthClientException("Can't found properties file", fnfe);
		} catch (IOException fio) {
			throw new AuthClientException("Can't read properties file", fio);
		}

		prtln("properties loaded");

		host = props.getProperty("host");
		if (host == null || host.trim().length() == 0)
			throw new AuthClientException("host property is not provided in ucarAuth.properties file");
		serverLogin = props.getProperty("serverLogin");
		if (serverLogin == null || serverLogin.trim().length() == 0)
			throw new AuthClientException("serverLogin property is not provided in ucarAuth.properties file");

		serverPassword = props.getProperty("serverPassword");
		if (serverPassword == null || serverPassword.trim().length() == 0)
			throw new AuthClientException("serverPassword property is not provided in ucarAuth.properties file");
		host = host.trim();
		serverLogin = serverLogin.trim();
		serverPassword = serverPassword.trim();
		prtln("\t host: \"" + host + "\"");
		prtln("\t serverLogin: \"" + serverLogin + "\"");
		prtln("\t serverPassword: \"" + serverPassword + "\"");
		TrustManager[] trustAllCerts = new TrustManager[]{
			new X509TrustManager() {
				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
					return null;
				}
				public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
				}
				public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
				}
			}};

		prtln("trustmanager instantiated");

		// Install the all-trusting trust manager
		try {
			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		} catch (Exception e) {
			//logger.error("Error in init SSL Context.", e);
			throw new AuthClientException("Error in init SSL Context.", e);
		}

		HostnameVerifier hv =
			new HostnameVerifier() {
				public boolean verify(String urlHostName, SSLSession session) {
					return true;
				}
			};

		prtln("hostnameverifier instantiated");

		HttpsURLConnection.setDefaultHostnameVerifier(hv);
		Authenticator.setDefault(
			new Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(serverLogin, serverPassword.toCharArray());
				}
			});
		initialized = true;
		prtln("returning from setup");
	}


	static void prtln(String s) {
		if (debug)
			SchemEditUtils.prtln(s, "MyUcasClient");
	}

}

