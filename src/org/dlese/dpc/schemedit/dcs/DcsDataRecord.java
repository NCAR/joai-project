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
package org.dlese.dpc.schemedit.dcs;

import org.dlese.dpc.schemedit.*;
import org.dlese.dpc.schemedit.config.*;
import org.dlese.dpc.schemedit.repository.RepositoryService;
import java.io.*;
import java.util.*;
import java.text.*;

import org.dlese.dpc.xml.*;
import org.dlese.dpc.util.*;
import org.dlese.dpc.repository.*;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Attribute;
import org.dom4j.Node;

/**
 *  Used to manage dcs_data that is associated with an indexed item-level
 *  record. A DcsDataRecord reads from and writes to a file on disk that is
 *  named the same as it's "source" item-level record, but in a different
 *  directory.<P>
 *
 *  NOTE: the Document attribute should be implemented as a {@link
 *  org.dlese.dpc.schemedit.DocMap}!!
 *
 * @author    ostwald
 */
public class DcsDataRecord implements Serializable {

	static boolean debug = true;

	private Document doc = null;
	private File source = null;
	private MetaDataFramework framework = null;
	private List entryList = null;
	private DcsDataManager dcsDataManager = null;
	private CollectionConfig collectionConfig;


	/**
	 *  Constructor for DcsDataRecord.
	 *
	 * @param  dcsDataFile       file on disk containing xml source
	 * @param  framework         this record's framework
	 * @param  collectionConfig  this record's collection
	 * @param  dcsDataManager    manager for all dcsDataRecords
	 */
	public DcsDataRecord(File dcsDataFile, MetaDataFramework framework, CollectionConfig collectionConfig, DcsDataManager dcsDataManager) {
		this.dcsDataManager = dcsDataManager;
		this.collectionConfig = collectionConfig;
		this.source = dcsDataFile;
		this.framework = framework;
		try {
			this.doc = getDocument();
		} catch (Throwable e) {
			prtlnErr("failed to create document: " + e.getMessage());
		}
		if (!source.exists()) { // this is a new DcsDataRecord
			setValidationReport(Constants.UNKNOWN_VALIDITY);
			// an empty or null value means the record is valid
			String dateString = RepositoryService.getDateString();
			setLastTouchDate(dateString);

			// clear the currentStatusEntry - it is initialized when the record is indexed
			// (in RepositoryService.saveNewRecord)
			Element currentEntry = (Element) getNode("/dcsDataRecord/statusEntries");
			currentEntry.clearContent();
		}
		else {
			normalizeStatus();
		}
	}


	/**
	 *  Gets the collection attribute of the DcsDataRecord object
	 *
	 * @return    The collection value
	 */
	public String getCollection() {
		if (collectionConfig != null) {
			return collectionConfig.getId();
		}
		else {
			prtlnErr("WARNING: dcsDataRecord.collectionConfig not found for " + getId());
			return "";
		}
	}

	public void setCollectionConfig (CollectionConfig collectionConfig) {
		this.collectionConfig = collectionConfig;
	}
	
	/**
	 *  Returns true if this record has "Final" status. A record has final status if the value of the
	 status element in the current status entry has the form: "_|-final-{collection-key}-|_", which is
	 referred to as the final status VALUE. The final status LABEL is a human-readable string that is
	 configured in the collection configuration and stored internally in the collectionConfig.<p>
	 This method returns true if the status value has the pattern "_|-final-{*}-|_" and as a side effect
	 calls "normalize" to ensure that the collection key part of the status value refers to the correct
	 collection.
	 *
	 * @return    true if this record has a "final" status.
	 */
	public boolean isFinalStatus() {
		return isFinalStatus (getStatus());
	}

	public boolean isFinalStatus (String value) {
		return value.toLowerCase().equals(collectionConfig.getFinalStatusValue().toLowerCase());
	}

	/**
	 *  Gets the readable representation of this record's status.
	 *
	 * @return    status label as String
	 */
	public String getStatusLabel() {
		return getStatusLabel (this.getStatus());
	}

	public String getStatusLabel (String value) {
		if (isFinalStatus(value))
			return collectionConfig.getFinalStatusLabel();
		else
			return value;
	}
	
	/**
	 *  Ensure that 1) records having a status LABEL equal to the configured final
	 *  status label also have the correct status VALUE, and 2) records having a final
	 status refer to the correct collection.
	 */
	protected void normalizeStatus() {
		if (collectionConfig == null) {
			// prtln ("WARNING: unable to normalize status for " + getId() + " because no collectionConfig is available");
			return;
		}
		
		/* make sure that this record's label and value are correct
		- is it a final status?
		- are both the value and label correct?
		*/
		
		String myStatus = this.getStatus();
		boolean update = false;
		String finalStatusLabel = collectionConfig.getFinalStatusLabel();
		String finalStatusValue = collectionConfig.getFinalStatusValue();		
		
		if (StatusFlags.isFinalStatusValue(myStatus) && 
			!myStatus.equals (finalStatusValue)) {
			// the status is A final status but not the final status for this collection!
			update = true;
		}
		else if (myStatus.equals (finalStatusLabel)) {
			// the status matches the final status LABEL (when it should match the final
			// status VALUE)
			update = true;
		}
			
		if (update) {
			setStatus(finalStatusValue);
			try {
				flushToDisk();
			} catch (Exception e) {
				prtlnErr(" ... flushToDisk failed for " + getId());
			}
		}
	}


	/**
	 *  Change the current status of this record.
	 *
	 * @param  status         new status for this record
	 * @param  statusNote     new status note
	 * @param  editor         user performing status update
	 * @exception  Exception  if status cannot be updated
	 */
	public synchronized void updateStatus(String status, String statusNote, String editor)
		 throws Exception {
		StatusEntry newEntry = new StatusEntry(status, statusNote, editor, "");
		updateStatus(newEntry);
	}


	/**
	 *  Adds a StatusEntry to the the DcsDataRecord object
	 *
	 * @param  statusEntry  The feature to be added to the StatusEntry attribute
	 */
	private synchronized void addStatusEntry(StatusEntry statusEntry) {
		Element entries = doc.getRootElement().element("statusEntries");
		Element entryElement = (Element) statusEntry.getElement();
		entries.add(entryElement);
		// force entry list to be recalcuated
		entryList = null;
	}


	/**
	 *  Update this record's status with the provided StatusEntry object.
	 *
	 * @param  statusEntry    new statusEntry
	 * @exception  Exception  if unable to update
	 */
	public synchronized void updateStatus(StatusEntry statusEntry) throws Exception {
		updateStatus(statusEntry, false);
	}


	/**
	 *  Update this record's status with the provided StatusEntry object and notify
	 *  event Listeners of change.
	 *
	 * @param  statusEntry       new statusEntry
	 * @param  retainChangeDate  if true, change date is not altered
	 * @exception  Exception     if unable to update
	 */
	public synchronized void updateStatus(StatusEntry statusEntry, boolean retainChangeDate)
		 throws Exception {
		if (!retainChangeDate) {
			String changeDate = SchemEditUtils.fullDateString(new Date());
			try {
				statusEntry.setChangeDate(changeDate);
			} catch (Exception e) {
				String errorMsg = "updateStatus error: " + e.getMessage();
				// prtln(errorMsg);
				throw new Exception(errorMsg);
			}
		}
		setLastEditor(statusEntry.getEditor());
		addStatusEntry(statusEntry);
		dcsDataManager.notifyListeners(this);
	}


	/**
	 *  Gets the current StatusEntry attribute of the DcsDataRecord object
	 *
	 * @return    The currentEntry value
	 */
	public StatusEntry getCurrentEntry() {
		List entryList = getEntryList();
		if (entryList.size() == 0) {
			String now = SchemEditUtils.fullDateString(new Date());
			StatusEntry defaultEntry = new StatusEntry(StatusFlags.UNKNOWN_STATUS, "", Constants.UNKNOWN_EDITOR, now);
			try {
				addStatusEntry(defaultEntry);
			} catch (Exception e) {
				prtlnErr("getCurrentEntry error for " + getId() + ": " + e.getMessage());
			}
			return defaultEntry;
		}
		return (StatusEntry) entryList.get(0);
	}


	/**
	 *  Returns List of managed suggestions records. Record entry list is sorted
	 *  and cashed. When new entryList are added, entryList is set to null so it
	 *  will be regenerated
	 *
	 * @return    The entryList value
	 */
	public List getEntryList() {
		if (entryList == null) {
			entryList = new ArrayList();
			List entries = doc.selectNodes("/dcsDataRecord/statusEntries/statusEntry");
			for (Iterator i = entries.iterator(); i.hasNext(); ) {
				Element entryElement = (Element) i.next();
				entryList.add(new StatusEntry(entryElement));
			}
			Collections.sort(entryList, new SortStatusEntries());
		}
		return entryList;
	}


	/**
	 *  Gets the historyElement attribute of the DcsDataRecord object
	 *
	 * @param  changeDate  NOT YET DOCUMENTED
	 * @return             The historyElement value
	 */
	private Element getHistoryElement(String changeDate) {
		List dateElements = doc.selectNodes("//dcsDataRecord/statusEntries/statusEntry/changeDate");
		if (dateElements != null && dateElements.size() > 0)
			for (Iterator i = dateElements.iterator(); i.hasNext(); ) {
				Element dateElement = (Element) i.next();
				if (dateElement.getText().equals(changeDate))
					return dateElement.getParent();
			}
		return null;
	}


	/**
	 *  Gets the statusEntry corresponding to provided changeDate
	 *
	 * @param  changeDate  key used to locate existing status entry
	 * @return             The statusEntry value
	 */
	public StatusEntry getStatusEntry(String changeDate) {
		Element historyElement = getHistoryElement(changeDate);
		if (historyElement != null)
			return new StatusEntry(historyElement);
		else
			return null;
	}


	/**
	 *  Removes statusEntry corresponding to provided changeDate, and then adds new statusEntry
	 *
	 * @param  changeDate     key used to locate existing status entry
	 * @param  statusEntry    new status entry to be added to record
	 * @exception  Exception  if unable to add status entry (no exception thrown if entry to delete does not exist)
	 */
	public synchronized void replaceStatusEntry(String changeDate, StatusEntry statusEntry) throws Exception {
		statusEntry.setChangeDate(changeDate);
		deleteStatusEntry(changeDate);
		addStatusEntry(statusEntry);
	}


	/**
	 *  Remove specified status entry from the status entry list
	 *
	 * @param  changeDate     key used to specify existing status entry
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public synchronized void deleteStatusEntry(String changeDate) throws Exception {
		Element historyElement = getHistoryElement(changeDate);

		if (historyElement == null) {
			throw new Exception("historyElement not found for " + changeDate);
		}
		historyElement.detach();
		entryList = null;
	}


	/**
	 *  Gets the metadata framework of this DcsDataRecord
	 *
	 * @return    The framework value
	 */
	public MetaDataFramework getFramework() {
		if (framework == null) {
			prtlnErr("framework is null!");
		}
		return framework;
	}


	/**
	 *  Gets Editable (localized and ampersand-expanded) Document for this record.
	 *  <p>
	 *
	 *  First tries to read from cache, then tries to read from disk, then creates
	 *  new record.
	 *
	 * @return                The document value
	 * @exception  Exception  Description of the Exception
	 */
	public Document getDocument()
		 throws Exception {
		if (doc != null) {
			return doc;
		}

		if (source.exists()) {
			doc = SchemEditUtils.getLocalizedXmlDocument(source.toURI());
			return doc;
		}
		else {
			// prtln("source file does not exist, fetching an instance document ...");
			doc = (Document) framework.getSchemaHelper().getInstanceDocument().clone();
		}
		return doc;
	}


	/**
	 *  Gets the source attribute of the DcsDataRecord object
	 *
	 * @return    The source value
	 */
	public File getSource() {
		return source;
	}


	/**
	 *  Sets the source attribute of the DcsDataRecord object
	 *
	 * @param  source  The new source value
	 */
	public void setSource(File source) {
		this.source = source;
	}


	/**
	 *  Gets the isValid attribute of the DcsDataRecord object
	 *
	 * @return    The isValid value
	 */
	public String getIsValid() {
		String vRpt = null;
		try {
			vRpt = getValidationReport();
		} catch (Throwable t) {
			prtlnErr ("Unable to get validationReport: " + t.getMessage());
		}
		if (vRpt == null || vRpt.trim().length() == 0) {
			return "true";
		}
		else {
			return "false";
		}
	}


	/**
	 *  Gets the valid attribute of the DcsDataRecord object
	 *
	 * @return    true if the metadata record for this dcsDataRecord is valid
	 */
	public boolean isValid() {
		return "true".equals(getIsValid());
	}


	/**
	 *  Returns true 
	 *
	 * @return    true if the validitity of the metadata record for this dcsDataRecord is unknown
	 */
	public boolean getIsValidityUnknown() {
		return (getValidationReport().equals(Constants.UNKNOWN_VALIDITY));
	}


	/**
	 *  Gets the validationReportElement attribute of the DcsDataRecord object
	 *
	 * @return    The validationReportElement value
	 */
	public Element getValidationReportElement() {
		Element validationReport = null;
		Node rptNode = getNode("/dcsDataRecord/validationReport");
		if (rptNode == null) {
			return doc.getRootElement().addElement("validationReport");
		}
		else {
			return (Element) rptNode;
		}
	}


	/**
	 *  Sets the validationReport attribute of the DcsDataRecord object
	 *
	 * @param  report  The new validationReport value
	 */
	public void setValidationReport(String report) {
		if (report == null) {
			report = "";
		}
		getValidationReportElement().setText(report);
	}


	/**
	 *  Gets the validationReport attribute of the DcsDataRecord object
	 *
	 * @return    The validationReport value
	 */
	public String getValidationReport() {
		return getValidationReportElement().getText();
	}


	/**
	 *  Gets the lastTouchDate attribute of the DcsDataRecord object, reflecting
	 *  the last time the metadata was changed.
	 *
	 * @return    The lastTouchDate value
	 */
	public String getLastTouchDate() {
		return getNodeText("/dcsDataRecord/lastTouchDate");
	}


	/**
	 *  Sets the lastTouchDate attribute of the DcsDataRecord object
	 *
	 * @param  dateString  The new lastTouchDate value
	 */
	public void setLastTouchDate(String dateString) {
		try {
			getNode("/dcsDataRecord/lastTouchDate").setText(dateString);
		} catch (Throwable e) {
			prtlnErr("setLastTouchDate failed: " + e.getMessage());
		}
	}


	/**
	 *  Gets the lastTouchDateDate attribute of the DcsDataRecord object
	 *
	 * @return    The lastTouchDateDate value
	 */
	public Date getLastTouchDateDate() {
		try {
			return SchemEditUtils.fullDateFormat.parse(getLastTouchDate());
		} catch (Throwable t) {
			prtlnErr("getLastTouchDateDate() error: " + t.getMessage());
		}
		return new Date();
	}


	/**
	 *  Gets the id attribute of the DcsDataRecord object
	 *
	 * @return    The id value
	 */
	public String getId() {
		return getNodeText("/dcsDataRecord/recordID");
	}


	/**
	 *  Sets the id attribute of the DcsDataRecord object
	 *
	 * @param  id  The new id value
	 */
	public void setId(String id) {
		try {
			getNode("/dcsDataRecord/recordID").setText(id);
		} catch (Throwable e) {
			prtlnErr("setId failed: " + e.getMessage());
		}
	}

	/**
	 *  Gets the lastEditor attribute of the DcsDataRecord object, reflecting
	 *  the last time the metadata was changed.
	 *
	 * @return    The lastEditor value
	 */
	public String getLastEditor() {
		String editor = getNodeText("/dcsDataRecord/lastEditor");
		if (editor.trim().length() == 0) {
			StatusEntry entry = this.getCurrentEntry();
			try {
				editor = entry.getEditor();
			} catch (Exception e) {
				editor = Constants.UNKNOWN_EDITOR;
			}
			this.setLastEditor (editor);
		}
		return editor;
	}


	/**
	 *  Sets the lastEditor attribute of the DcsDataRecord object
	 *
	 * @param  dateString  The new lastEditor value
	 */
	public void setLastEditor(String userName) {
		try {
			Node node = getNode("/dcsDataRecord/lastEditor");
			if (node == null)
				node = this.doc.getRootElement().addElement("lastEditor");
			((Element)node).setText(userName);
		} catch (Throwable e) {
			prtlnErr("setLastEditor failed: " + e.getMessage());
		}
	}
	
	
	// NDR Info Accessors ---------------------------

	String ndrInfoPath = "/dcsDataRecord/ndrInfo";


	/**  Clear the ndrInfo element  */
	public void clearNdrInfo() {
		Element ndrInfo = getNdrInfo();
		if (ndrInfo != null) {
			ndrInfo.clearContent();
		}
	}


	/**
	 *  Gets the ndrInfo attribute of the DcsDataRecord object
	 *
	 * @return    The ndrInfo value
	 */
	public Element getNdrInfo() {
		return (Element) getNode(ndrInfoPath);
	}

	public String getNdrNormalizedStatus() {
		return (this.isFinalStatus() ? org.dlese.dpc.ndr.apiproxy.NDRConstants.NCS_FINAL_STATUS : this.getStatus());
	}
	

	/**
	 *  Sets the ndrInfoValue attribute of the DcsDataRecord object
	 *
	 * @param  childName  The new ndrInfoValue value
	 * @param  value      The new ndrInfoValue value
	 */
	private void setNdrInfoValue(String childName, String value) {
		Element ndrInfo = DocumentHelper.makeElement(this.doc, ndrInfoPath);
		Element child = DocumentHelper.makeElement(this.doc, ndrInfoPath + "/" + childName);
		child.setText(value);
	}


	String ndrHandlePath = "/dcsDataRecord/ndrInfo/ndrHandle";


	/**
	 *  Gets the ndrHandle attribute of the DcsDataRecord object
	 *
	 * @return    The ndrHandle value
	 */

	public String getNdrHandle() {
		String ret = getNodeText(ndrHandlePath);
		return ret;
	}


	/**
	 *  Sets the ndrHandle attribute of the DcsDataRecord object
	 *
	 * @param  ndrHandle  The new ndrHandle value
	 */
	public void setNdrHandle(String ndrHandle) {
		try {
			setNdrInfoValue("ndrHandle", ndrHandle);
		} catch (Throwable e) {
			prtlnErr("setNdrHandle failed: " + e.getMessage());
		}
	}


	String metadataProviderHandlePath = "/dcsDataRecord/ndrInfo/metadataProviderHandle";


	/**
	 *  Gets the ndrHandle attribute of the DcsDataRecord object
	 *
	 * @return    The ndrHandle value
	 */

	public String getMetadataProviderHandle() {
		String ret = getNodeText(metadataProviderHandlePath);
		return ret;
	}


	/**
	 *  Sets the ndrHandle attribute of the DcsDataRecord object
	 *
	 * @param  mdpHandle  The new metadataProviderHandle value
	 */
	public void setMetadataProviderHandle(String mdpHandle) {
		try {
			setNdrInfoValue("metadataProviderHandle", mdpHandle);
		} catch (Throwable e) {
			prtlnErr("setMetadataProviderHandle failed: " + e.getMessage());
		}
	}


	String nsdlItemIdPath = "/dcsDataRecord/ndrInfo/nsdlItemId";


	/**
	 *  Gets the nsdlItemId attribute of the DcsDataRecord object
	 *
	 * @return    The nsdlItemId value
	 */

	public String getNsdlItemId() {
		String ret = getNodeText(nsdlItemIdPath);
		return ret;
	}


	/**
	 *  Sets the nsdlItemId attribute of the DcsDataRecord object
	 *
	 * @param  itemId  The new nsdlItemId value
	 */
	public void setNsdlItemId(String itemId) {
		try {
			setNdrInfoValue("nsdlItemId", itemId);
		} catch (Throwable e) {
			prtlnErr("setNsdlItemId failed: " + e.getMessage());
		}
	}


	String lastSyncDatePath = "/dcsDataRecord/ndrInfo/lastSyncDate";


	/**
	 *  Sets the lastSyncDate attribute of the DcsDataRecord object
	 *
	 * @param  dateString  The new lastSyncDate value
	 */
	public void setLastSyncDate(String dateString) {
		try {
			setNdrInfoValue("lastSyncDate", dateString);
		} catch (Throwable e) {
			prtlnErr("setLastSyncDate failed: " + e.getMessage());
		}
	}


	/**
	 *  Sets the lastSyncDate attribute of the DcsDataRecord object
	 *
	 * @param  date  The new lastSyncDate value
	 */
	public void setLastSyncDate(Date date) {
		setLastSyncDate(SchemEditUtils.fullDateString(date));
	}


	/**
	 *  Gets the lastSyncDate attribute of the DcsDataRecord object
	 *
	 * @return    The lastSyncDate value
	 */
	public String getLastSyncDate() {
		return getNodeText(lastSyncDatePath);
	}


	/**
	 *  Gets the lastSyncDateDate attribute of the DcsDataRecord object
	 *
	 * @return    The lastSyncDateDate value
	 */
	public Date getLastSyncDateDate() {
		try {
			return SchemEditUtils.fullDateFormat.parse(getLastSyncDate());
		} catch (Throwable t) {
			prtlnErr("getLastSyncDateDate() error: " + t.getMessage());
		}
		return new Date(0);
	}


	String ndrSyncErrorPath = "/dcsDataRecord/ndrInfo/syncError";


	/**
	 *  Gets the ndrHandle attribute of the DcsDataRecord object
	 *
	 * @return    The ndrHandle value
	 */
	public String getNdrSyncError() {
		String ret = getNodeText(ndrSyncErrorPath);
		return ret;
	}


	/**
	 *  Returns true if this DcsDataRecord has a sync error
	 *
	 * @return    NOT YET DOCUMENTED
	 */
	public boolean hasSyncError() {
		return (getNdrSyncError() != null && getNdrSyncError().trim().length() > 0);
	}


	/**
	 *  Returns "true" if there are any sync errors (errors occuring when writing to
	 *  an external repository, such as the NDR).
	 *
	 * @return    The hasSyncError value
	 */
	public String getHasSyncError() {
		return (hasSyncError() ? "true" : "false");
	}


	/**
	 *  Sets the ndrSyncError attribute of the DcsDataRecord object
	 *
	 * @param  ndrSyncError  The new ndrSyncError value
	 */
	public void setNdrSyncError(String ndrSyncError) {
		try {
			setNdrInfoValue("syncError", ndrSyncError);
		} catch (Throwable e) {
			prtlnErr("setNdrSyncError failed: " + e.getMessage());
		}
	}


	/**
	 *  Clear any errors associated with External repositories, such as the NDR.
	 *  This method should be called before each write.
	 */
	public void clearSyncErrors() {
		setNdrSyncError("");
	}

	String setSpecPath = "/dcsDataRecord/ndrInfo/setSpec";


	/**
	 *  Gets the setSpec attribute of the DcsDataRecord object (used to cache the 
	 setSpec of collections written to the NDR.
	 *
	 * @return    The setSpec value
	 */

	public String getSetSpec() {
		String ret = getNodeText(setSpecPath);
		return ret;
	}


	/**
	 *  Sets the setSpec attribute of the DcsDataRecord object
	 *
	 * @param  setSpec  The new setSpec value
	 */
	public void setSetSpec(String setSpec) {
		try {
			setNdrInfoValue("setSpec", setSpec);
		} catch (Throwable e) {
			prtlnErr("setSetSpec failed: " + e.getMessage());
		}
	}


	/**
	 *  Gets the status attribute of the most recent StatusEntry.
	 *
	 * @return    The status value
	 */
	public String getStatus() {
		try {
			return getCurrentEntry().getStatus();
		} catch (Exception e) {
			prtlnErr("getStatus failed: " + e.getMessage());
			return "";
		}
	}


	/**
	 *  Gets the priorStatus attribute of the DcsDataRecord object
	 *
	 * @return    The priorStatus value
	 */
	public String getPriorStatus() {
		String priorStatus = "";
		List entryList = getEntryList();
		if (entryList.size() > 1) {
			StatusEntry priorEntry = (StatusEntry) entryList.get(1);
			try {
				priorStatus = priorEntry.getStatus();
			} catch (Exception e) {
				prtlnErr("getPriorStatus error: " + e.getMessage());
			}
		}
		return priorStatus;
	}


	/**
	 *  Sets the status of most recent StatusEntry.
	 *
	 * @param  status  The new status value
	 */
	private void setStatus(String status) {
		try {
			getCurrentEntry().setStatus(status);
		} catch (Throwable e) {
			prtlnErr("setStatus failed: " + e.getMessage());
		}
	}


	/**
	 *  Gets the statusNote attribute of the most recent StatusEntry.
	 *
	 * @return    The statusNote value
	 */
	public String getStatusNote() {
		try {
			return getCurrentEntry().getStatusNote();
		} catch (Exception e) {
			prtlnErr("getStatusNote failed: " + e.getMessage());
			return "";
		}
	}


	/**
	 *  Sets the statusNote of most recent StatusEntry.
	 *
	 * @param  statusNote  The new statusNote value
	 */
	private void setStatusNote(String statusNote) {
		try {
			getCurrentEntry().setStatusNote(statusNote);
		} catch (Throwable e) {
			prtlnErr("setStatusNote failed: " + e.getMessage());
		}
	}


	/**
	 *  Gets the changeDate attribute of the most recent StatusEntry. This reflects
	 *  the last time a status was changed.
	 *
	 * @return    The changeDate value
	 */
	public String getChangeDate() {
		try {
			return getCurrentEntry().getChangeDate();
		} catch (Exception e) {
			prtlnErr("getChangeDate failed: " + e.getMessage());
			return "";
		}
	}


	/**
	 *  Sets the changeDate of most recent StatusEntry.
	 *
	 * @param  changeDate  The new changeDate value
	 */
	private void setChangeDate(String changeDate) {
		try {
			getCurrentEntry().setChangeDate(changeDate);
		} catch (Throwable e) {
			prtlnErr("setChangeDate failed: " + e.getMessage());
		}
	}


	/**
	 *  Gets the lastChangeDate (last status update) as a Date.
	 *
	 * @return    The lastTouchDateDate value
	 */
	public Date getChangeDateDate() {
		try {
			return SchemEditUtils.fullDateFormat.parse(getChangeDate());
		} catch (Throwable t) {
			prtlnErr("getChangeDateDate() error: " + t.getMessage());
		}
		return new Date();
	}


	/**
	 *  Write record to disk and then remove it from cashe so any xml processing
	 *  (such as contracting ampersands) is picked up next time record is needed.
	 *
	 * @exception  Exception  If record cannot be written to disk.
	 */
	public void flushToDisk()
		 throws Exception {

		framework.writeEditableDocument(getDocument(), source);
	}


	/**
	 *  Delete this record by removing it from the cache, destroying the contents,
	 *  and deleting the source file from disk.
	 *
	 * @return                Description of the Return Value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public boolean delete() throws Exception {
		String id = "unknown";
		try {
			id = getId();
			File source = getSource();
			dcsDataManager.removeFromCache(getId());
			destroy();
			return source.delete();
		} catch (Throwable t) {
			throw new Exception("Unable to delete dcsDataRecord (id: " + id + ")");
		}
	}


	/**
	 *  Gets the docType attribute of the DcsDataRecord, which is 'dcs_data'
	 *
	 * @return    The docType, which is 'dlese_collect.'
	 */
	public String getDocType() {
		return framework.getXmlFormat();
	}


	// DOM utilities
	/**
	 *  get all Nodes satisfying the given xpath
	 *
	 * @param  xpath  an XPath
	 * @return        a List of all modes satisfying given XPath, or null
	 */
	private List getNodes(String xpath) {
		try {
			return doc.selectNodes(xpath);
		} catch (Throwable e) {
			// prtlnErr("getNodes() failed with " + xpath + ": " + e);
		}
		return null;
	}


	/**
	 *  gets a single Node satisfying give XPath. if more than one Node is found,
	 *  the first is returned (and a msg is printed)
	 *
	 * @param  xpath  an XPath
	 * @return        a dom4j Node
	 */

	private Node getNode(String xpath) {
		List list = getNodes(xpath);
		if ((list == null) || (list.size() == 0)) {
			// prtln("getNode() did not find node for " + xpath);
			return null;
		}
		if (list.size() > 1) {
			prtln("getNode() found mulitple modes for " + xpath + " (returning first)");
		}
		return (Node) list.get(0);
	}


	/**
	 *  return the Text of a Node satisfying the given XPath.
	 *
	 * @param  xpath  an XPath\
	 * @return        Text of Node or empty String if no Node is found
	 */
	private String getNodeText(String xpath) {
		Node node = getNode(xpath);
		String val = "";
		try {
			return node.getText();
		} catch (Throwable t) {
			// prtlnErr("getNodeText() failed with " + xpath + ": " + t.getMessage());
		}
		return "";
	}


	/**
	 *  This method is called at the conclusion of processing and may be used for
	 *  tear-down.
	 */
	protected void destroy() {
		doc = null;
		dcsDataManager = null;
		collectionConfig = null;
		framework = null;
		source = null;
	}


	/**
	 *  Sets the debug attribute of the DcsDataRecord class
	 *
	 * @param  bol  The new debug value
	 */
	public static void setDebug(boolean bol) {
		debug = bol;
	}


	/**
	 *  Print a line to standard out.
	 *
	 * @param  s  The String to print.
	 */
	static void prtln(String s) {
		if (debug) {
			System.out.println("DcsDataRecord: " + s);
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	static void prtlnErr(String s) {
		if (debug) {
			System.out.println("DcsDataRecord: " + s);
		}
	}

}

