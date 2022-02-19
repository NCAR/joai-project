
## Release Notes and Changes

Release notes and version documentation for the 
jOAI web application software version @VERSION@

v3.3 (Released to GitHub February 19, 2022)

- Added Docker support
- Updated to run under Tomcat 9
- Removed web links to DLESE (the dlese.org website and repository have been retired)
- Renamed all .txt README files to .md


v3.2 (Released to GitHub March 21, 2017)

- Updated to run under Tomcat 6, 7, or 8, and Java 8
- Released under Apache 2.0 License (previous versions were GPL license)
- Fixed issue where links on the search page were not displayed for URLs beginning with https
- Updated support email found in various files
- Moved junit test package/classes in with primary src directory
- Moved project code to GitHub https://github.com/NCAR/joai-project (previous versions hosted at SourceForge)


v3.1.1.4 (Released to SourceForge June 4, 2015)

- Fixed NullPointerException that was occurring with some harvests 
(issue when null HarvestMessageHandler was passed into Harvester)

- Added a new OAIChangeListener interface for the Harvester API that enables a process to recieve event messages 
during a harvest that notify when OAI record files are created, modified, exist but not modified, or deleted.

- In Harvester
    - Fixed null pointer exception that occurred when harvested records were marked as deleted
    - Fixed null pointer exception that occurred when outdir was passed in as null

- Placed unit tests in the dlese-tools-project tree


v3.1.1.3 (Released to SourceForge November 19, 2012)

- Fixed issue with nsdl_dc to oai_dc transform that resulted in non-valid records returned.
Transform now properly handles the new LAR fields.

- Added lar to oai_dc transform (requires xslt 2.0)

- Added saxon XSLT processor with XSLT 2.0 support

- Changed the boilerplate localize XSLT to a different stylesheet that does not 
throw the warning "The child axis starting at an attribute node will never select anything"
from the saxon XSLT processor

- Harvester now writes the XML schema instance namespace on the root element of each harvested file
if it wasn't included by the data provider (e.g. xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance")


v3.1.1.2 (Released to SourceForge October 6, 2012) 

- Added ability to select whether or not to zip harvested files after harvesting. This can be set for each
configured harvest in the Add/Edit harvest dialog box (defaults to off).

- Harvester now uses the java.net.URLEncoder.encode() method to encode the resumptionToken, set, and 
metadataPrefix arguments. Fixes an issue where some characters such as the linefeed were not being encoded 
and therefore causing harvests to fail.

- Harvester now preserves CDATA. Previously, CDATA nodes were converted to text.

- Fixed problem with the links to the harvested zip archives that appear in the 
'Harvester Setup and Status' page (occured on Windows installations only).

- Added lar format schema/namespace info (and others from trunk) for OAI ListMetadataFormats response



v3.1.1.1 (Released to SourceForge June 13, 2012 - branch tag joai_v3_1_1_branch) 

- Patched the following:
  - Fixed OutOfMemoryError errors that were occuring with large repositories (> 300k records)
  - Added ability to configure new indexing handlers (instances of XmlFileIndexingWriter) via the servlet config
  - Fixed issues with threads not sutting down properly when the webapp is stopped/reloaded
  - Patch was applied to release tag oai_v3_1_1 with new branch tag joai_v3_1_1_branch
  - Patch submitted by Timo Proescholdt <timo@proescholdt.de>

- Harvester now allows harvesting of baseURLs that begin with https://, not just http://

- Added smile_item to nsdl_dc format converter

- Added lar to nsdl_dc format converter


v3.1.1 (Released to SourceForge September 9, 2010)

- Fixed bug in sort order for the 'Harvest History and Progress' display in the UI 
(bug was introduced in v3.1.0). Display is now properly sorted showing the newest
harvest first.

- Removed the maxlength limit in the form fields for entering the 
setSpec and setName of an OAI set in the administrative UI 
(there is no reason to limit these).


v3.1.0 (Released to SourceForge August 30, 2010)

- Upgraded to Lucene 3.0.2. No functional changes were made to jOAI as a result,
but the underlying repository code was modified significantly to use the 
new Lucene classes.

- Entity references in the <identifier> element are now propely escaped in the XML responses for 
GetRecord, ListRecords, ListIdentifiers (matters only if entities such as '&' exist in your
identifiers).

- Added documentation in the 'Data Provider Documentation' page under 'Preparing files for serving' 
about escaping reserved characters in file names for the OAI records on disk


v3.0.19

- Added more info to the install instructions


v3.0.18 (Released to SourceForge May 25, 2010)

- badVerb error code is now returned instead of badArgument if no verb argument exists in 
the request (conforms to the protocol).

- Added additional protocol validation checks if arguments are repeated (badArgument) or 
if identifier does not exist for ListMetadataFormats (idDoesNotExist)

- Verified that jOAI passes all validation checks at the OAI Data Provider registration site 
(http://www.openarchives.org/Register/ValidateSite)

- Changed 'Provider' to 'Data Provider' throughout the UI and documentation

- Updated documentation


v3.0.17 (Released to SourceForge May 8, 2010)

- Fixed incompatibility in ODL search when slashes (/) appear in the search query. Updated
ODL specification to require that all slashes (/) in [search query] and [set] be escaped 
with %2F. This enables XPath search fields to be used in ODL search queries.

- Created a jOAI User Forum at SourceForge. Updated links and documentation to
point users to the forum for support:
http://sourceforge.net/projects/dlsciences/forums/forum/1138932

- Provided additional details about available search fields and query syntax in the
documentation... points to DDS Search documentation:
http://www.dlese.org/dds/services/ddsws1-1/service_specification.jsp#availableSearchFields

- Added ncs_collect to dlese_collect format converter

- Clarified build instructions


v3.0.16 (Released to SourceForge February 11, 2010)

- Fixed OAI protocol compliance for badArgument error condition. OAI response no longer displays
any attributes on the <request> element, as required for badArgument or badVerb error conditions.

- Fixed issue where files that have a byte-order mark (BOM) character (\uFEFF) at the beginning could not be indexed. 
Fixes problem for users of the Windows Notepad editor, which writes a BOM in files that are saved as UTF-8 encoding. 

- Added a config option to import and insert a page at the bottom of each HTML page in the interface.
Used for inserting tracking code such as Google Analytics or other snippets at the bottom of each page.

- Improved css styles for buttons in the interface.
			

v3.0.15 (Released to SourceForge December 2, 2009)

- Converted all the action links (Add, Edit, Delete, etc.) in the software settings pages from hrefs to form buttons.
This protects them from inadvertently being activated by a web crawler. Changes were made
in the Metadata Files Configuration, Sets Configuration, and Harvester Setup and Status 
pages.

- Fixed issue with empty collection ID value that caused indexing to fail for null value

- Added alternate nsdl_dc to oai_dc transform that is compatible with nsdl_dc v1.00
(default configuration is to use nsdl_dc to oai_dc transform for v1.02)

- Added more robust badArgument checking of OAI requests. Returns badArgument if a requested
argument is not valid in OAI-PMH.

- Updated install and system requirements, FAQ and other documentation for the software

- Default Java XSL transform engine is now used, to be compatible when running in Tomcat with Fedora 
and other XSL processors running in the same JVM that require the default engine.
Removed the following explicit System.setProperty call: 
System.setProperty("javax.xml.transform.TransformerFactory", "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl");

- Updated dlese_anno to nsdl_dc transform to be compatible with the above change (fixed typo)

- Placed the contentType="text/xml; charset=UTF-8" page directive at the top of each OAI 
request JSP page to ensure proper contentType is suppled in all, not just ListRecords.

- Tested and added build support for Tomcat 6, Java 6

- To do: Add ability to configre which days of the week, not just time of day, to run the indexing cron for 
the RepositoryManager data sources - **need to add to jOAI config and test!**


v3.0.14 (Released to SourceForge February 2, 2009)

- Fixed issue in ListRecords response where UTF-8 encoding was occasionally not applied
properly in the response.

- Fixed issue in the public search page related to the above where UTF-8 characters
would occasionally not display properly.

- Fixed issue in the admin search page to ensure UTF-8 characters are properly displayed

- Added documentation on how to configure Tomcat to use UTF-8 encoding when reading URIs 
(e.g. forms in Web pages, http parameters, etc.).

- Added the harvest, validate, and transform scripts to build and deploy with the project,
and provided instructions on how to install them in the jOAI documentation.


v3.0.13 (Released to SourceForge December 17, 2008)

- Fixed an issue found on Windows systems whereby the file processor was 
improperly updating the OAI datestamps for all records each time the indexer 
ran regardless of whether the file had changed, which made it impossible 
to effectively perform incremental harvests.

Changes inherited from repository code base:

- Added default search fields for all xml formats derived from the xPath to each 
element and attribute in the XML instance document. These are encoded as text, 
stemmed text and keywords for each field. Can be used in admin search (not supported in ODL search yet). 
  - Example search fields for oai_dc records:
    - /text//dc/title:(my search) - Search for terms 'my search' in the title field
    - /stems//dc/title:(my search) - Search for terms 'my search' in the title field using word stemming
    - /key//dc/title:"my search" - Search for terms 'my search' in the title field as an exact keyword (whitespace and other non-word characters included in search)

- Configured indexer to use SnowballAnalyzer for stems fields, which provides improvements over PorterStemmerAnalyzer

- Updated to Lucene v2.4.0


v3.0.12 (Released to SourceForge June 20, 2008)

- Added configurations for dif and ncs_collect XML formats 

- Added xsl stylesheets for XML format conversion 
  - dif to oai_dc
  - dif to fgdc
  - ncs_collect to nsdl_dc

- Updated the indexer for Lucene v2.2.0. After updating to this 
version of jOAI, existing indexes will need to be deleted and rebuilt to accommodate 
the new Lucene format for Date fields. Do the following after re-starting Tomcat:
From the 'Metadata Files Configuration' page, choose 'Show advanced index operations'
then click the 'Reset the index' button. This upgrade will reset the OAI date stamps
and remove the history of deleted records from the data provider. The history
of harvests will also be lost.

- Added nsdl_dc transforms for dlese_anno, dlese_collect and news_opps formats

- Added link to WHOIS lookup via NetworkSolutions in the reports area

- Encoding for resumption tokens now uses just the reserved
characters listed in the OAI 2.0 spec instead of using java.net.URLEncoder

- Fixed display of number of deleted and error records in a given 
set/collection on Win file systems. Was problem in query parser with colon in file path


v3.0.12-rc1

- Added documentation in the FAQ for configuring Tomcat for use with
the mod_proxy Apache module

- Added the ability to configure a fixed server URL (scheme, hostname and port,
e.g. http://www.example.org) used to display the baseUrl in OAI 
responses and elsewhere in the application

- Created better error reporting in the harvest when harvests fail due to write permissions
when creating file or zip archive directories


v3.0.11 (Released June 21, 2007)

- Added more documentation in the FAQ

- Updated copyright notice for Digital Learning Sciences (DLS)

- OAI project copied from the dlese SourceForge area to its new home in the
dlsciences SourceForge area and the CVS module has been renamed from 
oai-project to joai-project.



v3.0.10 (Released March 20, 2007)

- Files are now explicitly read into the index using UTF-8 encoding instead of the 
native system encoding type (rt ticket 6937). LC_CTYPE no longer needs to be set to UTF-8 in unix
environments and encoding should now work properly on Windows. Index Writers expect
that the files reside in UTF-8 encoding.

- Select lists in the UI are now disabled only for IE 6 and lower. IE 7 
properly supports z-index layers with select lists.

- Added more documentation in the FAQ


v3.0.9 (Released Feb 23, 2007)

- Added nsdl_dc to oai_dc xsl crosswalk

- Fixed issue where corrupt XML data was being seen in the output from XSL 
format converters, which can propagate as data corruption in the OAI 
ListRecords and GetRecord responses. Fixed by applying synchronization 
in the XMLConversionService to address this concurrency-related issue.


v3.0.8 (Released Jan 19, 2007)

- Fixed bug in the data provider where sometimes empty content was returned in 
ListRecords or GetRecord responses. Crosswalk transformer no longer writes the cached 
file if the transformed content is empty. This ensures that the transform will be 
attemped again the next time the given record/format is requested by a harvester.

- Made the new adn to nsdl_dc crosswalk with ASN identifiers the default configuration


v3.0.7

- Improved performance of the Harvester. Fixed bug that caused the harvester
to slow increasingly as the number of harvested records grew

- Added more documentation in the FAQ, ODL search client (odl_search_client.jsp),
and elsewhere

- Included a version of the adn to nsdl_dc XSL crosswalk that inserts ASN
identifiers for content standards

- Added ability to configure in web.xml how long the harvester waits for a response
before timing out

- Distribution now built using Java 1.5 (specifically Sun jdk1.5.0_09), which has seen 
improved stability over previous versions of Java


v3.0.6 (Released Oct 3, 2006)

- Links in the UI to external pages now open in the same window


v3.0.5 Changes

- Summary: This release provides updates primarily only to the 
documentation pages. No other significant changes have been made
since version 3.0.4

- The ODL 'Search' page now displays parse errors from malformed ODL
service responses as HTML comments rather than to the user

-Added additional FAQ and configuration documentation

- Fixed the link in the 'Indexing Errors' page that brings up the file
for an ID used in another file


v3.0.4 Changes (Released Sept 12, 2006)

- Added display of the harvest from date and duration of the harvest in 
the harvest history

- File directories are now sorted alphabetically in the edit sets 
definitions page

- Added display-name to web.xml (used for display in the Tomcat manager)

- Fixed 'No write permissions' error that occured when harvesting all 
records for the first time

- Fixed problem that made it impossible to delete a configured harvest 


v3.0.3 Changes

- Implemented deletions in the data provider, used to support
incremental or selective harvesting

- Automatic harvests now check if the data provider supports deletions.
If deletions are supported, an incremental harvest is performed
by requesting and synchronizing only those records that have been added,
modified or deleted since the previous harvest. If deletions are not 
supported, a full harvest is performed by deleting all previously 
harvested records and harvesting all records from scratch

- Automatic harvests that occur daily now run at a given
time of day, which is entered in the UI when setting up the harvest

- Added additional user instructions and software documentation

- Sets are now sorted by set name in the sets configuration page

- Zipped harvests are now saved to a single directory for each
provider/set, and the directory is now in the admin area so access 
can be controlled

- Added ability to configure when to log the requests 
that are made to the data provider and ability to disable 
logging altogether. This eliminates or reduces writes that are 
made to the log index to save disc space.

- The OAI datestamps for items in the data provider are now 
set to three minutes ahead of the time the change was recorded 
in the index to provide for overlap in indexing time

- A dded more harvest status messages during harvests

- Addressed some inconsistencies with the index mirroring the 
metadata files

- Fixed issue that was causing the server to shut down 
when a harvest attempted to save to a directory 
that did not have write permissions

- Fixed zip exception thrown when zero files were harvested


v3.0.2 Changes (Released June 7, 2006)

- Added statement about how to provide attribution when
using, modifying or redistributing the software

- Fixed problem where namespace and schema information
was not being saved when a new metadata format was entered
in the data provider files configuration page


v3.0.1 Changes

- Automatic harvests now perform a clean harvest of all records
from the data provider instead of only those that have changed
since the previous harvest

- Moved the data provider Explorer page to the top level menu
in the UI

- Fixed problem on Windows where the paths to download files from the
harvester were broken


v3.0.0 Changes (Released April 2006)

- The software has been renamed from 'DLESE OAI Software' to 'jOAI'

- A completely redesigned User Interface

- Ability to define arbitraty OAI sets in the data provider. 
Sets are now configured separately from the file directories via the UI

- Ability to define the data provider namespaces and schemas that are 
exposed in the ListMetadataFormats response via the UI

- The harvester now packages each set of harvested records into
zip files, which are made available for download from the
harvester UI

- Improved/added XSL transforms in the data provider to oai_dc 
from these formats: adn, dlese_anno, dlese_collect, news_opps

- Documentation and help pages updated for better clarity

- Deletions no longer supported in the data provider

- If the same ID is found in more than one metadata file, this is now detected
and an error is displayed in the data provider's administration UI

- Numerous updates and bug fixes for the underlying repository, indexing and other
core libraries and routines

- Updated to use Struts v1.2.7 and Lucene v1.4.3

- Compatible in Tomcat 5.x and Java 1.4 or higher


v2.0.12 Changes

- Added a search page as the main index.jsp. This search page uses the ODL interface
to search the repository and display results to users.

- Harvester now ignores it if a given repository returns a record that has an empty
metadata element. Previously the presence of an empty metadata element within the
ListRecords response would result in a harvest error and halting of the harvest.

- Harvester now properly reports HTTP error (type 500, 403, etc.). Previously
these were being reported as connection time-outs (since the time-out functionality
was added).

- The Provider search and discovery page can now display dlese_ims records. Previously 
an exception was thrown.

- The dlese_ims indexer can now index files with the % character in their path. 

- Harvester now indents the XML it outputs with nice formatting. Previously the XML was 
written to file as a single line. Output is now also explicitly set to UTF-8.

- Provider JSPs now explicitly set the charset response header to UTF-8.

- Gzip filter now outputs using the given charset, which is UTF-8.


v2.0.11 Changes

- Harvester now times-out if a connection to the remote provider can not be established within 
180 seconds. Previously, the harvester would hang indefinately waiting for the harvest to 
complete.

v2.0.10 Changes

- Provider search and discovery now defaults to search on everything
when nothing is typed in the search box. Previously the user had to
type * to search on everything.

- Rearranged the reindexing button in the provider admin UI to place it
in a more logical spot.

- Changed parameter settings to address Mac OSX limitation on the number of 
files that can be open at any given time. For OSX compatibility, the Max 
number of files to index per block is set to 25 and the max number of 
files to harvest per resumption token is set to 50.


v2.0.9.3 Changes (Released Oct 2005)

- Made compatible for running in Tomcat 5.5.x


v2.0.9 Changes (Released Sept 2003)

- Added user feedback about the indexing process in the provider administration page.

- Updated the ADN to NSDL-DC and ADN to OAI-DC format converters.

- Improved display of ADN records in the provider Search and Discovery interface.

- Users can now only configure a directory of metadata files once.
This fixes some rare bugs that occur if a given directory is configured
for more than one set.

- Harvester now encodes resumptionTokens properly. Previously, resumptionTokens
were being encoded twice, which resulted in inproper requests to data providers.

- A bit more documentation and user instructions added.

- Binary and source distributions now automaticlly built and zipped by Ant. 

- New button in the provider admin interface allows administrators to rebuild the
metadata index from scratch, resetting all OAI datestamps and deleted records

Known issues:

- Files that have been created or harvested on Windows platforms sometimes
cannot be indexed properly in the data provider.

- Some UTF character encoding problems when writing metadata files.

- Problems on Windows reading some XML files that have had their file names encoded.

v2.0.8 Changes

- Added more documentation.

v2.0.7 Changes

- Fixed bug in displaying the Provider explorer ODL search pages in Netscape 4.

- Fixed IndexOutOfBounds error when selecting multiple formats in Provider search box.

v2.0.6 Changes

- Additions to documentation.

- XSL format converters in the data provider should now work properly on Windows systems.

v2.0.5 Changes

- Encoded colons and other chars in file names are now properly unencoded
to form a proper item ID.

- Now able to view XML files in the provider data search and reports interfaces that contain file 
name encoding, such as escaped colons. 


v2.0.4 Changes

- Added a bit more documentation.

- Performance enhancements made in the ODL search and the admin keyword search 
engines. 

- Fixed a bug in the creation of file directory path names to escape
characters that are illegal in Windows and other platforms file names.

- Harvests that were terminated by a server shut-down or crash are now
logged appropriately. Previously the harvest log would show these as 
"in progress."

- Removed the 'b' from the version number. 

v2.0.3b Changes (Released June 2003)

- Added a harvester. Features include:
  - Supports protocol versions 2.0 and 1.1. 
  - Automatic updating and synchronizing of the local data files with the remote data provider. 
  - Supports gzip compression. 
  - Ability to monitor harvest activity through the web interface. 
  - Saves files to disk in folders by baseURL path, set and format. 
  
- Added new metadata converters to convert from ADN to oai_dc, ADN to nsdl_dc, ADN to the briefmata 
format, DLESE IMS to ADN and DLESE IMS to ADN with cataloger information suppressed.



v2.0.2b Changes

- Added parsers for adn and dlese_anno metadata types, allowing search and display by field.

- Updated the dlese_ims to adn converters.

- Added a demo Java based XML converter.

- Improved the navigational menus in the UI. 

- Added a bit more documentation.


v2.0.1b features

- Keyword search over items in the repository. 

- UI to explore the repository using OAI-PMH requests. 

- Implements an ODL search extension of the OAI-PMH protocol that supports content-based queries. 

- Serves metadata directly from XML files cached on disc and requires no database for operation. 

- Allows users to validate and view each OAI response that is being returned by the provider. 

- Allows users to validate and view individual XML metadata records within the repository. 

- Supports configuration of arbitrary metadata sets. 

- Supports indexing and serving of any schema-based XML metadata format, plus DLESE-IMS (contains a DTD). 

- Provides extensible, plug-and-play configuration of XML metadata format converters, with support for XSL or Java. 

- Includes an XSL format converter to convert from DLESE IMS to ADN. 

- Allows a number of administrative options to control the behavior of the provider and the data that is returned. 

- Provides searchable reports that detail validation or critical errors found in metadata files. 

- Provides searchable reports of harvesters that have accessed the data provider. 

- Supports data transmission in compressed gzip format. 

- Supports deletions. 

- Supports modification date granularity down to seconds. 


v1.01 (Released to SourceForge March 2002)
