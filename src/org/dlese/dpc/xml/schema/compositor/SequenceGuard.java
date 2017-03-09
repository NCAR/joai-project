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
package org.dlese.dpc.xml.schema.compositor;

import java.util.*;
import org.dlese.dpc.xml.schema.SchemaUtils;

import org.dlese.dpc.xml.Dom4jUtils;

import org.dom4j.Element;
import org.dom4j.Node;

/**
 *  Implementation of {@link org.dlese.dpc.xml.schema.compositor.CompositorGuard} for Sequence Compositors
 *
 * @author    ostwald
 */
public class SequenceGuard extends CompositorGuard {

	private static boolean debug = false;


	/**
	 *  Constructor for the SequenceGuard object
	 *
	 * @param  compositor       NOT YET DOCUMENTED
	 * @param  instanceElement  NOT YET DOCUMENTED
	 */
	public SequenceGuard(Compositor compositor, Element instanceElement) {
		this(compositor, instanceElement.elements());
	}


	/**
	 *  Constructor for the SequenceGuard object
	 *
	 * @param  compositor    NOT YET DOCUMENTED
	 * @param  instanceList  NOT YET DOCUMENTED
	 */
	public SequenceGuard(Compositor compositor, List instanceList) {
		super(compositor, instanceList);
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @return    NOT YET DOCUMENTED
	 */
	public boolean acceptsNewMember() {
		// element must be valid to accept a new member
		prtln("acceptsNewMember");

		boolean requireValidCompositor = false;
		if (requireValidCompositor) {
			boolean isValid;
			try {
				isValid = checkValid();
			} catch (Exception e) {
				prtln(" .. compositor element is NOT Valid: " + e.getMessage());
				return false;
			}

			if (parsingError != null) {
				prtln(" .. inValid compositor: " + parsingError);
				return false;
			}
		}

		// for sequence compositors with a single member, we accept a new member if the last occurrance has
		// any room ...

		int oCount = getOccurrencesCount();
		if (compositor.getMembers().size() == 1 && oCount == compositor.getMaxOccurs()) {
			Occurrence lastOccurrence = (Occurrence) occurrences.get(oCount - 1);
			int memberMaxOccurs = compositor.getMemberAt(0).maxOccurs;

			// prtln(" .. singleton: elements: " + lastOccurrence.count + ", maxOccurs: " + memberMaxOccurs);
			// printOccurrences();

			return (lastOccurrence.count < memberMaxOccurs);
		}
		else {
			// prtln(" .. occurrences: " + oCount + ", compositor.maxOccurs: " + compositor.getMaxOccurs());

			return (oCount < compositor.getMaxOccurs());
		}
	}


	/**
	 *  Gets the occurrences attribute of the SequenceGuard object
	 *
	 * @return                The occurrences value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	protected List getOccurrences() throws Exception {

		List occurrences = new ArrayList();

		List buckets = getCMBuckets();

		// walk down the buckets
		int lastCMindex = -1;
		int maxCMindex = compositor.getMembers().size() - 1;

		// traverse the buckets - looking for Occurrences
		// prtln("Walking buckets");
		for (Iterator i = buckets.iterator(); i.hasNext(); ) {

			CMBucket bucket = (CMBucket) i.next();
			// prtln("\t bucket: " + bucket.label);

			occurrences.addAll(normalizeBucket(bucket));

		}
		return occurrences;
	}


	/**
	 *  Returns a list of element CMBuckets (representing contiguous elements
	 *  belonging to the same top-level (the children of the compositor element)
	 *  member.<p>
	 *
	 *  E.g., if the top-level member is a sequence compositor, then as long as
	 *  instanceElements belong to that compositor (i.e., they are in the set of
	 *  leafMembersNames) we collect them in the same bucket.<p>
	 *
	 *  Each bucket instance keeps track of how many elements it contains. Later,
	 *  the buckets list is split into Occurrences.
	 *
	 * @return                CMBuckets found in the instance document element to
	 *      be tested
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	private List getCMBuckets() throws Exception {

		// first pass - aggregate instanceElement children into buckets

		List buckets = new ArrayList();
		CompositorMember lastCM = null;
		CMBucket currentBucket = null;
		CompositorMember currentMember = null;

		for (Iterator i = instanceMembers.iterator(); i.hasNext(); ) {
			Element child = (Element) i.next();
			String name = child.getQualifiedName();
			// prtln ("compositorMemberName: " + name);
			// sanity check - must find a member for this child
			if (!compositor.getLeafMemberNames().contains(name)) {
				// prtln (" .. not found but maybe it's an any type");
				if (!compositor.hasAnyTypeMember())
					throw new Exception("Unexpected child element: \"" + name + "\"");
			}

			CompositorMember cm = resolveLeafNameToCM(name);

			if (cm == null) {
				throw new Exception("CompositorMember (cm) not found for \"" + name + "\"");
			}

			// create a new bucket if the cm has changed AND
			// the cmType has also changed.
			// -- new buckets NOT added for adjacent ELEMENT compositors
			if (cm != lastCM) {
				// we're looking at a different CompositorMember. Create a
				// new bucket UNLESS ...
				if (lastCM != null &&
					!lastCM.hasSubCompositor() &&
					!cm.hasSubCompositor()) {

					// do not create a new bucket for adjacent ELEMENTs

				}
				else {

					// prtln ("\t ** adding new bucket");
					currentBucket = new CMBucket(cm);
					buckets.add(currentBucket);
				}
				lastCM = cm;
			}
			currentBucket.add(child);
		}

		// prtln(buckets.size() + " buckets after FIRST pass ....");
		// printCMBuckets(buckets);
		return buckets;
	}


	/*
	 *  Return occurrences build from a CompositorMember bucket.<p>
	 *  Bucket elements are used in order to populate occurrences.
	 *  The instanceElements are consummed greedily (maximum number of
	 *  of members available and allowed are used). When a member does not
	 *  legally follow it's predecessor, start a new occurrence.<p>
	 *  throw Exceptions for:
	 *  - an instanceElement that can't be matched against a CM
	 *  - there are too many occurrences
	 */
	/**
	 *  Description of the Method
	 *
	 * @param  bucket         Description of the Parameter
	 * @return                Description of the Return Value
	 * @exception  Exception  Description of the Exception
	 */
	private List normalizeBucket(CMBucket bucket) throws Exception {

		List occurrences = new ArrayList();
		List instanceElements = bucket.elements;
		int lastCMindex = -1;
		int currentCMindex = -1;
		int cmCount = 0;
		Occurrence currentOcc = null;

		// Walk the bucketElements
		Iterator bucketElements = bucket.elements.iterator();
		while (bucketElements.hasNext()) {
			Element instanceElement = (Element) bucketElements.next();

			// get the cm (CompositorMember) for this instanceElement

			String instanceName = instanceElement.getQualifiedName();
			CompositorMember currentCM = compositor.getMember(instanceName);

			currentCMindex = compositor.getIndexOfMember(currentCM);
			if (currentCMindex == -1) {
				if (this.compositor.hasAnyTypeMember()) {
					currentCM = this.compositor.getAnyTypeMember();
					currentCMindex = this.compositor.getIndexOfMember(currentCM);
				}
				else
					throw new Exception("currentCM not found in compositor");
			}

			// is the first element we've looked at?
			if (lastCMindex == -1) {
				//Create new currentOcc
				cmCount = 1;
				currentOcc = new Occurrence(compositor);
				occurrences.add(currentOcc);
			}
			// same CM - create new occurrence if maxOccurs contraint dictates
			else if (currentCMindex == lastCMindex) {
				// same instanceName add to current if possible
				cmCount++;

				if (cmCount > currentCM.maxOccurs) {
					currentOcc = new Occurrence(compositor);
					occurrences.add(currentOcc);
				}
			}
			// currentCM comes BEFORE last - create a new occurrence
			// SAME action as for first element.
			else if (currentCMindex < lastCMindex) {

				// we jumped to an earlier cmIndex, so start new occurrence
				cmCount = 1;
				currentOcc = new Occurrence(compositor);
				occurrences.add(currentOcc);
			}
			//	we are working on the sameOccurrence but with a new CM
			else if (currentCMindex > lastCMindex) {
				cmCount = 1;
			}

			/*
			 *  if currentCM is complex we make recursive call
			 *  - getSubOccurrences with this instanceElement
			 *  we then check subOoccurrenceCount against the currentCM.maxOccurs
			 */
			if (currentCM.hasSubCompositor()) {

				int subOccurrenceCount = getSubOccurrenceCount(currentCM, instanceElement);
				if (subOccurrenceCount > currentCM.maxOccurs) {
					throw new Exception(" .. instanceElement cannot be normalized");
				}
			}

			currentOcc.add(instanceElement);
			lastCMindex = currentCMindex;
		}
		return occurrences;
	}


	/**
	 *  debugging
	 *
	 * @param  buckets  Description of the Parameter
	 */
	public void printCMBuckets(List buckets) {
		prtln(buckets.size() + " CMBuckets");
		for (int i = 0; i < buckets.size(); i++) {
			CMBucket bucket = (CMBucket) buckets.get(i);
			prtln("\t" + i + " - " + bucket.label + " " + bucket.count + " items");
			for (Iterator j = bucket.elements.iterator(); j.hasNext(); ) {
				prtln("\t\t" + ((Element) j.next()).asXML());
			}
		}
	}


	/**
	 *  Description of the Method
	 *
	 * @param  bucket  Description of the Parameter
	 */
	public void printBucketElements(CMBucket bucket) {
		prtln("CMBucket elements for " + bucket.label);
		for (Iterator i = bucket.elements.iterator(); i.hasNext(); ) {
			prtln("\t" + ((Element) i.next()).asXML());
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  node  NOT YET DOCUMENTED
	 * @return       NOT YET DOCUMENTED
	 */
	static String pp(Node node) {
		try {
			return (Dom4jUtils.prettyPrint(node));
		} catch (Exception e) {
			return (e.getMessage());
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	static void prtln(String s) {
		if (debug)
			// System.out.println("SequenceGuard: " + s);
			SchemaUtils.prtln(s, "SequenceGuard");
	}

}

