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
package org.dlese.dpc.ndr.reader;

import org.dlese.dpc.schemedit.SchemEditUtils;

import org.dlese.dpc.ndr.NdrUtils;
import org.dlese.dpc.ndr.request.*;
import org.dlese.dpc.ndr.reader.*;
import org.dlese.dpc.ndr.apiproxy.InfoXML;
import org.dlese.dpc.ndr.apiproxy.NDRConstants;
import org.dlese.dpc.ndr.apiproxy.NDRConstants.NDRObjectType;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.index.SimpleLuceneIndex;
import org.dom4j.*;

import java.io.File;
import java.util.*;
import java.net.URL;

import java.lang.reflect.Method;

/**
 *  Provides accesess to NSDL Collections in the NDR - exposing collection management information
 *  collected from each NDR Object that defines a collection, as well as the NDR
 *  Objects.<p>
 *
 *  NOTE: also exposes ncs_collect
 *
 * @author    Jonathan Ostwald
 */
public class NSDLCollectionReader {

	private static boolean debug = true;
	private static boolean verbose = false;

	/**  NOT YET DOCUMENTED */
	public AggregatorReader aggregator = null;
	/**  NOT YET DOCUMENTED */
	public MetadataProviderReader mdp = null;
	/**  NOT YET DOCUMENTED */
	public MetadataReader metadata = null;
	/**  NOT YET DOCUMENTED */
	public NdrObjectReader resource = null;
	private Element nsdl_dc = null;
	private Element nsdl_dc_info = null;
	private Volume volume;


	/**
	 *  Constructor for the NSDLCollectionReader object
	 *
	 * @param  aggHandle      NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public NSDLCollectionReader(String aggHandle) throws Exception {
		volume = new Volume();
		volume.set(verbose);
		try {
			this.aggregator = new AggregatorReader(aggHandle);
			String resourceHandle = aggregator.getRelationship("associatedWith");
			if (resourceHandle == null || resourceHandle.trim().length() == 0)
				throw new Exception("resource not found");
			this.resource = new NdrObjectReader(resourceHandle);
			this.metadata = findCollectionMetadata(aggHandle, resourceHandle);
			this.mdp = findMetadataProvider(aggHandle);
		} catch (Throwable t) {
			// prtln("Unable to instantiate NSDLCollectionReader: " + t);
			t.printStackTrace();
			throw new Exception(t.getMessage());
		}
		// volume.restore();
	}


	/**
	 *  Gets the ncsCollect attribute of the NSDLCollectionReader object
	 *
	 * @return    The ncsCollect value
	 */
	public Element getNcsCollect() {
		return this.metadata.getDataStream("ncs_collect");
	}


	/**
	 *  Gets the nsdlDc attribute of the NSDLCollectionReader object
	 *
	 * @return    The nsdlDc value
	 */
	public Element getNsdlDc() {
		return this.metadata.getDataStream("nsdl_dc");
	}


	private NsdlDcReader nsdlDcReader = null;


	/**
	 *  Gets the nsdlDcReader attribute of the NSDLCollectionReader object
	 *
	 * @return    The nsdlDcReader value
	 */
	public NsdlDcReader getNsdlDcReader() {
		if (this.nsdlDcReader == null) {
			try {
				nsdlDcReader = new NsdlDcReader(getNsdlDc().asXML());
			} catch (Throwable t) {
				// prtln ("couldn't instantate nsdlDcReader: " + t.getMessage());
			}
		}
		return nsdlDcReader;
	}


	/**
	 *  Gets the title attribute of the NSDLCollectionReader object
	 *
	 * @return    The title value
	 */
	public String getTitle() {
		String title = null;
		try {
			title = getNsdlDcReader().getTitle();
		} catch (Throwable t) {}
		return title;
	}


	/**
	 *  Gets the nsdlDcInfo attribute of the NSDLCollectionReader object
	 *
	 * @return    The nsdlDcInfo value
	 */
	public Element getNsdlDcInfo() {
		return this.metadata.getDataStream("nsdl_dc_info");
	}


	/**
	 *  Gets the harvestInfo attribute of the NSDLCollectionReader object
	 *
	 * @return    The harvestInfo value
	 */
	public HarvestInfoReader getHarvestInfo() {
		return this.mdp.getHarvestInfo();
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  aggHandle      NOT YET DOCUMENTED
	 * @return                NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	String findCollectionResource(String aggHandle) throws Exception {
		AggregatorReader reader = null;
		try {
			reader = new AggregatorReader(aggHandle);
		} catch (Exception e) {
			throw new Exception ("could not read aggregator at " + aggHandle + ": " + e.getMessage());
		}
		return reader.getRelationship("associatedWith");
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  aggHandle       NOT YET DOCUMENTED
	 * @param  resourceHandle  NOT YET DOCUMENTED
	 * @return                 NOT YET DOCUMENTED
	 * @exception  Exception   NOT YET DOCUMENTED
	 */
	MetadataReader findCollectionMetadata(String aggHandle, String resourceHandle) throws Exception {
		FindRequest findRequest = new FindRequest(NDRConstants.NDRObjectType.METADATA);
		findRequest.addCommand("relationship", "metadataFor", aggHandle);
		findRequest.addCommand("relationship", "metadataFor", resourceHandle);
		String mdHandle = findRequest.getResultHandle();
		if (mdHandle == null || mdHandle.trim().length() == 0)
			throw new Exception("collection metadata not found for\n\tagg: " + aggHandle + "\n\tres: " + resourceHandle);
		return new MetadataReader(mdHandle, null);
	}


	/**
	 *  Gets the resourceUrl attribute of the NSDLCollectionReader object
	 *
	 * @return    The resourceUrl value
	 */
	public String getResourceUrl() {
		return this.resource.getProperty("hasResourceURL");
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  aggHandle      NOT YET DOCUMENTED
	 * @return                NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	MetadataProviderReader findMetadataProvider(String aggHandle) throws Exception {
		FindRequest findRequest = new FindRequest(NDRConstants.NDRObjectType.METADATAPROVIDER);
		findRequest.addCommand("relationship", "aggregatedBy", aggHandle);
		String mdpHandle = findRequest.getResultHandle();
		if (mdpHandle == null || mdpHandle.trim().length() == 0)
			throw new Exception("collection metadataProvider not found (aggHandle: " + aggHandle + ")");
		return new MetadataProviderReader(mdpHandle);
	}


	/**  NOT YET DOCUMENTED */
	public void report() {
		prtln("\n" + this.mdp.getSetName() + "  (setSpec: " + this.mdp.getSetSpec() + ")");

		prtln("Aggregator: " + this.aggregator.getHandle());
		prtln("Resource: " + this.resource.getHandle() + "  (" + this.getResourceUrl() + ")");
		prtln("Metadata: " + this.metadata.getHandle());
		prtln("MetadataProvider: " + this.mdp.getHandle());
		prtln("setSpec: " + this.mdp.getSetSpec());
		prtln("setSpec: " + this.mdp.getSetName());

		// prtln("harvestInfo");
		// prtln (this.getHarvestInfo().toString());
	}

	/**
	 *  The main program for the NSDLCollectionReader class
	 *
	 * @param  args           The command line arguments
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static void main(String[] args) throws Exception {

		String methodName = null;
		File propFile = null; // propFile must be assigned!
		NdrUtils.setup (propFile);

		// all keyed off aggHandle
		String aggHandle;
		aggHandle = "2200/test.20080317005937427T"; //
		if (args.length > 0)
			aggHandle = args[0];
/* 		prtln ("args.length = " + args.length);
		for (int i=0;i<args.length;i++)
			prtln ("\t" + i + ": " + args[i]); */

		NSDLCollectionReader nsdlColl = new NSDLCollectionReader(aggHandle);
		nsdlColl.report();

	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  node  NOT YET DOCUMENTED
	 */
	private static void pp(Node node) {
		if (node == null)
			prtln("Provided node is NULL");
		else
			prtln(Dom4jUtils.prettyPrint(node));
	}


	/**
	 *  Description of the Method
	 *
	 * @param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		if (debug) {
			SchemEditUtils.prtln(s, "");
		}
	}


	class Volume {
		boolean debug;
		boolean verbose;


		/**  Constructor for the Volume object */
		Volume() {
			this.debug = SimpleNdrRequest.getDebug();
			this.verbose = SimpleNdrRequest.getVerbose();
		}


		/**
		 *  NOT YET DOCUMENTED
		 *
		 * @param  verbose  NOT YET DOCUMENTED
		 */
		void set(boolean verbose) {
			set(verbose, verbose);
		}


		/**
		 *  NOT YET DOCUMENTED
		 *
		 * @param  debug    NOT YET DOCUMENTED
		 * @param  verbose  NOT YET DOCUMENTED
		 */
		void set(boolean debug, boolean verbose) {
			SimpleNdrRequest.setDebug(debug);
			SimpleNdrRequest.setVerbose(verbose);
		}


		/**  NOT YET DOCUMENTED */
		void restore() {
			SimpleNdrRequest.setDebug(this.debug);
			SimpleNdrRequest.setVerbose(this.verbose);
		}
	}
}

