<%@ page contentType="text/html; charset=UTF-8" %>

<%@ taglib prefix='dds' uri='http://www.dlese.org/dpc/dds/tags' %>

<%@ taglib prefix="x" uri="http://java.sun.com/jsp/jstl/xml" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%@ page isELIgnored ="false" %>



<html>



<head>

<title>jOAI Harvester Documentation</title>

<c:import url="../head.jsp"/>

</head>



<body>

<a name="top"></a><c:import url="../top.jsp?sec=doc"/>

<!-- content begins-->





<h1>Harvester Documentation</h1>

<table border="0" align="left" cellpadding="0" cellspacing="0">

  <tr>

    <td align="left"><a href="provider.jsp#overview">Overview</a></td>

  </tr>

  <tr>

    <td width="50%" align="left"><a href="harvester.jsp#how">Harvester setup </a></td>

  </tr>

  <tr>

    <td align="left"><a href="harvester.jsp#test">Harvest test files </a></td>

  </tr>

  <tr>

    <td align="left"><a href="harvester.jsp#list">Registered data providers  </a></td>

  </tr>

  <tr>

    <td align="left"><a href="harvester.jsp#program">The Java Harvester API </a></td>

  </tr>
  
   <tr>

    <td align="left"><a href="harvester.jsp#cmd">Harvest, validate and transform from the command line</a></td>

  </tr> 

</table>





<p>&nbsp;</p>

<p>&nbsp;</p>

<p>&nbsp;</p>

<p>&nbsp;</p>

<h3><a name="overview"></a>Overview</h3>

<p>The jOAI harvester is  used to  retrieve metadata records from remote OAI data providers and save them to the local file system, one record per file. In addition, records that have been harvested  may be packaged into zip archives that can be downloaded and opened through the harvester's web-based interface.The harvester can be configured to harvest automatically at regular intervals and effectively maintain a mirror of the remote repository on the local file system. </p>

<p>The jOAI harvester supports OAI protocol versions 1.1 and 2.0, supports data 

  providers that use resumption tokens for <a href="http://www.openarchives.org/OAI/2.0/openarchivesprotocol.htm#FlowControl">flow 

  control</a>, selective harvesting by <a

  href="http://www.openarchives.org/OAI/2.0/openarchivesprotocol.htm#SelectiveHarvestingandDatestamps">date</a> 

  or <a href="http://www.openarchives.org/OAI/2.0/openarchivesprotocol.htm#SelectiveHarvestingandSets">set</a>, 

  gzip <a href="http://www.openarchives.org/OAI/2.0/openarchivesprotocol.htm#ResponseCompression">response 

  compression</a> and other protocol features.</p>

<p>See the <a href="<c:url value='/docs/faq.jsp#harvester'/>">Harvester FAQ</a> 

  for additional information.</p>

<hr>

<p><a name="how"></a></p>

<h3>Harvester setup</h3>

1. Install the jOAI software on a system in a servlet container such as <a href="http://tomcat.apache.org/">Apache Tomcat</a>.

<p>

  See <a href="INSTALL.txt">INSTALL.txt</a> for installation instructions. If reading this page, most likely this step has  been completed.

<p>

  2. Complete <a href="<c:url value='/admin/harvester.do'/>">Harvester Setup</a>.  Add a new harvest and complete:</p>

<ul>

  <li>Enter a repository name (<em>required</em>)</li>

  <li>Provide a repository base URL that starts with  http:// (<em>required</em>)</li>

  <li>Include a setSpec (<em>optional</em>)</li>

  <li>Provide the metadata format being harvested (<em>required</em>) </li>

  <li>Indicate if the harvest should occur at regular  intervals (<em>optional</em>)</li>

  <li>Indicate where metadata files should be saved (<em>required</em>)</li>

  <li>Indicate how metadata files are saved (by set or  not)</li>

</ul>

<p>The <strong>repository name</strong> is a name to describe  the data provider being harvested. The harvester status table is organized as  an alphabetical listing of repository names.</p>

<p>  The <strong>base URL</strong> is the access point of a data  provider. It&rsquo;s a web address that starts with http://</p>

  The  harvested <strong>metadata format</strong> can be any  metadata format as long as it matches a metadata format used by the provider  being harvested. Use the OAI ListMetadataFormats  request to find available metadata formats at the provider. The  ListMetadataFormats requests look like:<p>

  http://some.provider.org/base/url?verb=ListMetadataFormats</p>

  that is, concatenate together the [base URL] + [?verb=ListMetadataFormats]<p>

  The OAI ListMetadataFormats request returns an XML document and the  XML element, metadataPrefix, provides the metadata formats available.

  <p>

  <strong>Harvest automatically at  regular intervals</strong> means a time  interval (days/hours/minutes/seconds) can be specified that tells the jOAI  harvester when and how often to perform an automatic harvest that checks for and updates  new  records.

  <p>

  <strong>Saving files</strong> at the default harvest location means metadata files are saved to the context (directory) within the OAI application  generally of the form &quot;~oai/WEB-INF/harvested_records/&quot;. To view the default  directory path of this location, click on the save files help button (the question mark).

  <p>

  <strong>Saving files</strong> to a non-default harvest location means  metadata files are saved to a user-specified location in which the full directory  path is provided or files are saved to a recently used location.

  <p>

  If a <strong>SetSpec is specified</strong>,  metadata files are saved as a group. If a <strong>SetSpec  is not specified</strong>, metadata files can be saved into one big group (the do  not split by set option) or saved in many groups (split by set option)  depending on how the provider being harvested is organized. The default save  option is do not split by set.</p>

  <p><a href="harvester.jsp#top"><img src="<c:url value='/images/arrowup.gif'/>" alt="top" width="9" height="11" border="0" title="back to top"></a></p>

<hr>

<a name="test"></a>

<h3 align="justify">Harvest test files</h3>

<p>Conduct a test harvest  by completing the harvester setup section above but use the following  information:</p>

<ul type="disc">

  <li>Repository name: DLESE</li>

  <li>Repository base URL: http://dlese.org/oai/provider</li>

  <li>Metadata format: adn</li>

</ul>

<p>Leave all other fields  blank and save the entry.

<p>

  On the <a href="<c:url value='/admin/harvester.do'/>">Harvester Setup and Status</a> click 'View harvest history' page to  see the harvest being performed. Click 'Refresh  page' to see the number of metadata files increase. The entire harvest may  take several minutes to complete.

<p>

  The test harvest is  successful if the metadata files can be viewed by one of these methods. On the <a href="<c:url value='/admin/harvester.do'/>">Harvester Setup and Status</a> page,



<ul >

  <li>Locate and go to the 'Harvested to' directory on the server and view the files.</li>
  
  <li>If zipping of files was enebled, under 'Download zipped harvest', click on 'Most recent<em>'.</em> Save the zip file to       your Desktop, unzip it, and view the harvested records. </li>

</ul>

  <a href="harvester.jsp#top"><img src="<c:url value='/images/arrowup.gif'/>" alt="top" width="9" height="11" border="0" title="back to top"></a>



<hr>

<a name="list"></a>

<h3><strong>Registered data providers </strong></h3>

<p>The Open Archives Initiative maintains a <a href="http://www.openarchives.org/Register/BrowseSites">list  of registered data providers</a> that can be harvested.</p>

<hr>

<a name="program"></a>

<h3><strong>The Java Harvester API </strong></h3>

<p>The jOAI code base includes a Harvester API that may be used in Java programs to harvest from OAI data providers. The API is part of the DLESETools.jar Java library, found in the <code>$tomcat/webapps/oai/WEB-INF/lib/</code> directory of the jOAI installation. See the <a href="javadoc/org/dlese/dpc/oai/harvester/Harvester.html">Harvester  Javadoc</a> for details. Use of the API assumes familiarity with the Java programming language. </p>
<hr>
<a name="cmd"></a>
<h3><strong>Harvest, validate and transform from the command line </strong></h3>
<p>Linux shell scripts are included in the jOAI distribution that allow you to perform OAI harvests, XML validation, and XML transformations from the command line.  </p>
<ul>
  <li><strong>To install:</strong> See the instructions provided in the README and script files located in the jOAI installation at <code>$tomcat/webapps/oai/WEB-INF/bin/</code>. Once installed, the scripts do not require the jOAI Web application in order to be used.</li>
</ul>

<p><strong>harvest</strong> - This script  performs  harvests from OAI data providers and saves the harvested records as individual files on disk. It accepts options to harvest by date range, set, and variations on how the metadata is written to files. It is simply a wrapper to the  Java Harvester API mentioned above.</p>
<p><strong>validate</strong> - This script performs schema validation on a single XML file or batch validation on a directory of files, outputting a summary report of the results.</p>
<p><strong>transform</strong> - This script performs an XSL transformation on a single XML file or batch transformations on a directory of files, outputting the transformed XML files to a directory. </p>

<p>&nbsp;</p>
<p><a href="harvester.jsp#top"><img src="<c:url value='/images/arrowup.gif'/>" alt="top" width="9" height="11" border="0" title="back to top"></a></p>

<p>&nbsp;</p>

<p>&nbsp;</p>

<c:import url="../bottom.jsp"/>



</body>

</html>



