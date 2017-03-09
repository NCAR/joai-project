<?xml version="1.0" encoding="ISO-8859-1" standalone="yes"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:str="http://xsltsl.org/string" xmlns:xalan="http://xml.apache.org/xalan" xmlns:dif="http://gcmd.gsfc.nasa.gov/Aboutus/xml/dif/">	

<!-- Stylesheet created by Fedele Stella (GeoConnections Discovery Portal) -->

<!-- Stylesheet modified by Scott Ritz (SSAI,Inc.) in April 2005
     Validates against the FGDC DTD.
     Contact: ritz@gcmd.nasa.gov  -->

<!-- Stylesheet Last modified by Melanie F. Meaux (RSIS,Inc.) in February 2007
     Validates against the FGDC DTD.
     Contact: mmeaux@gcmd.nasa.gov  -->
	 
<!-- Stylesheet added to jOAI software June 2008
	Contact: John Weatherley jweather@ucar.edu -->
	 
<!-- Additional information:
 http://www.w3schools.com/xsl/xsl_transformation.asp
 http://www.fgdc.gov/metadata/csdgm/
 http://biology.usgs.gov/fgdc.metadata/version2/

 Command line:

 java org.apache.xalan.xslt.Process -IN DIF_*.xml -XSL dif_to_fgdc3.xsl -OUT FGDC_*.xml

 /home/mmeaux/Application/bin/tidy -config /home/mmeaux/Application/bin/config.txt FGDC_*.xml > FGDC_*_clean.xml

 mp.lnx FGDC_*_clean.xml -e FGDC_*_clean.err

 for multiple files, run
 cd in directory where files are (US_GLOBEC_NEP_0001 ...etc)
 (source .bashrc)
 csh
 dif_fgdc_mload
 dif_fgdc_clean

 DIF xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="http://gcmd.gsfc.nasa.gov/Aboutus/xml/dif/dif.xsd">
 -->

<xsl:import href="string-modified.xsl"/>

<xsl:output method="xml" encoding="ISO-8859-1" omit-xml-declaration="no" standalone="yes" indent="yes" xalan:indent-amount="3"/>

<xsl:preserve-space elements="*"/>

<xsl:template match="/dif:DIF" xml:space="preserve">
<metadata>
 <idinfo>

<!-- begin Citation -->
      <xsl:choose>
      <xsl:when test="dif:Data_Set_Citation">
        <xsl:apply-templates select="dif:Data_Set_Citation"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:apply-templates select="dif:Originating_Metadata_Node" />
      </xsl:otherwise>
      </xsl:choose>
 <!-- end Citation -->

 <!-- begin Description -->
      <descript>
      <abstract>
      <xsl:apply-templates select="dif:Summary"/>
      </abstract>
      <purpose>Not Available</purpose>
      <xsl:choose> <!-- place reference field in supplemental -->
       <xsl:when test="dif:Reference">
       <xsl:apply-templates select="dif:Reference"/>
       </xsl:when>
       <xsl:otherwise>
       <supplinf>Not Available</supplinf>
       </xsl:otherwise>
      </xsl:choose>
      </descript>
<!-- end Description -->

 <!-- begin Time Period of Content -->
       <xsl:choose>
       <xsl:when test="dif:Temporal_Coverage">
       <xsl:apply-templates select="dif:Temporal_Coverage"/>
       </xsl:when>
       <xsl:otherwise>
       <begdate>Unknown</begdate>
       <enddate>Unknown</enddate>
       </xsl:otherwise>
       </xsl:choose>
 <!-- end Time Period of Content -->

 <!-- begin Status -->
      <xsl:choose>
       <xsl:when test="dif:Data_Set_Progress">
        <xsl:apply-templates select="dif:Data_Set_Progress"/>
       </xsl:when>
       <xsl:otherwise>
       	 <status>
       	<progress>Complete</progress> <!-- status and Update are mandatory fields in FGDC -->
	<update>As needed</update>
	 </status>
       </xsl:otherwise>
      </xsl:choose>
 <!-- end Status -->

 <!-- begin Spatial Domain -->
      <xsl:choose>
       <xsl:when test="dif:Spatial_Coverage">
        <xsl:apply-templates select="dif:Spatial_Coverage"/>
       </xsl:when>
       <xsl:otherwise>
       <spdom>
       <bounding>
        <westbc>Not Available</westbc>
        <eastbc>Not Available</eastbc>
        <northbc>Not Available</northbc>
        <southbc>Not Available</southbc>
       </bounding>
       </spdom>
       </xsl:otherwise>
      </xsl:choose>
 <!-- end Spatial Domain -->

 <!-- begin Keywords -->
      <keywords>
         <theme>
	    <!-- <themekt>GCMD ENTRY ID</themekt>
	    <xsl:apply-templates select="dif:Entry_ID"/> -->
            <themekt>GCMD SCIENCE PARAMETERS</themekt>
            <xsl:apply-templates select="dif:Parameters"/>
	    <xsl:choose>
	    <xsl:when test="dif:Source_Name">
	      <themekt>GCMD PLATFORM</themekt>  
	      <xsl:apply-templates select="dif:Source_Name"/>   
	    </xsl:when>
            </xsl:choose> 
	    <xsl:choose>
	    <xsl:when test="dif:Sensor_Name">
	      <themekt>GCMD INSTRUMENT</themekt>  
	      <xsl:apply-templates select="dif:Sensor_Name"/>    
	    </xsl:when>
            </xsl:choose> 
	    <xsl:choose>
	    <xsl:when test="dif:Project">
	      <themekt>PROJECT</themekt>
              <xsl:apply-templates select="dif:Project"/>  
	    </xsl:when>
            </xsl:choose> 
	    <xsl:choose>
	    <xsl:when test="dif:Keyword">
	     <themekt>ANCILLARY KEYWORDS</themekt>
             <xsl:apply-templates select="dif:Keyword"/> 
	    </xsl:when>
            </xsl:choose> 
	    <xsl:choose>
	    <xsl:when test="dif:ISO_Topic_Category">
	     <themekt>ISO TOPIC CATEGORY</themekt>
             <xsl:apply-templates select="dif:ISO_Topic_Category"/> 
	    </xsl:when>
            </xsl:choose> 
	    <xsl:choose>
	    <xsl:when test="dif:Data_Set_Language">
	     <themekt>DATA SET LANGUAGE</themekt>
             <xsl:apply-templates select="dif:Data_Set_Language"/> 
	    </xsl:when>
            </xsl:choose> 
         </theme>
	 <xsl:choose>
	 <xsl:when test="dif:Location">
          <place>
            <placekt>GCMD</placekt>
            <xsl:apply-templates select="dif:Location"/>
          </place>
	 </xsl:when>
         </xsl:choose>
	 <xsl:choose>
	 <xsl:when test="/dif:DIF/dif:Data_Resolution/dif:Temporal_Resolution">
	  <temporal>
            <tempkt>GCMD</tempkt>
            <xsl:apply-templates select="/dif:DIF/dif:Data_Resolution/dif:Temporal_Resolution"/>
	    <xsl:apply-templates select="/dif:DIF/dif:Data_Resolution/dif:Temporal_Resolution_Range"/>
         </temporal>
         </xsl:when>
         </xsl:choose>
      </keywords>
 <!-- end Keywords -->

 <!-- begin Access & Use Constraints -->
      <xsl:choose>
      <xsl:when test="dif:Access_Constraints">
        <xsl:apply-templates select="dif:Access_Constraints"/>
      </xsl:when>
      <xsl:otherwise>
        <accconst>Not Available</accconst>
      </xsl:otherwise>
      </xsl:choose>

      <xsl:choose>
      <xsl:when test="dif:Use_Constraints">
        <xsl:apply-templates select="dif:Use_Constraints"/>
      </xsl:when>
      <xsl:otherwise>
        <useconst>Not Available</useconst>
      </xsl:otherwise>
      </xsl:choose>
 <!-- end Access & Use Constraints -->

 <!-- begin Point of Contact; can only be repeated once so if both technical contact and investigator info present in DIF
 just transfer personnel info for Technical Contact -->
      <xsl:choose>
      <xsl:when test="dif:Personnel/dif:Role/text() = 'TECHNICAL CONTACT'">
        <ptcontac>
          <cntinfo>
          <xsl:apply-templates select="dif:Personnel[dif:Role='TECHNICAL CONTACT']"/>
          </cntinfo>
      </ptcontac>
      </xsl:when>
      <xsl:when test="dif:Personnel/dif:Role/text() = 'INVESTIGATOR'">
        <ptcontac>
          <cntinfo>
          <xsl:apply-templates select="dif:Personnel[dif:Role='INVESTIGATOR']"/>
          </cntinfo>
      </ptcontac>
      </xsl:when>
      <xsl:otherwise>
      </xsl:otherwise>
      </xsl:choose>
 <!-- end Point of Contact -->

 <!-- begin Browse Graphic-->
      <xsl:apply-templates select="dif:Multimedia_Sample"/>
 <!-- end Browse Graphic -->


 <!-- begin Cross Reference -->
 <!--      <xsl:apply-templates select="Reference"/> -->
 <!-- end Cross Reference -->

 </idinfo>

<!-- begin Data Quality information -->
 <dataqual>
       <xsl:choose>
        <xsl:when test="dif:Quality">
          <xsl:apply-templates select="dif:Quality"/>
	</xsl:when>
        <xsl:otherwise>
	  <attracc>
             <attraccr>Not Available</attraccr>
          </attracc>
          <logic>Not Available</logic> <!-- Mandatory in data quality -->
          <complete>Not Available</complete> <!-- Mandatory in data quality -->
          <lineage>
    	     <procstep>
		<procdesc>Not Available</procdesc>
	        <procdate>Unknown</procdate>
             </procstep>
         </lineage> <!-- Mandatory in data quality -->
        </xsl:otherwise>
       </xsl:choose>
 </dataqual>
 <!-- end Data Quality information -->

 <!-- begin Spatial Reference information -->
 <spref>
     <xsl:choose>
      <xsl:when test="/dif:DIF/dif:Data_Resolution">
        <xsl:apply-templates select="/dif:DIF/dif:Data_Resolution"/>
      </xsl:when>
      <!--
      <xsl:otherwise>
        <horizsys>
	<geograph>
	<latres>Not Available</latres>
	<longres>Not Available</longres>
	<geogunit>Decimal degrees</geogunit>
	</geograph>
	</horizsys>
	<vertdef>
	  <altsys>
            <altdatum>Not Available</altdatum>
            <altres>Not Available</altres>
            <altunits>Not Available</altunits>
            <altenc>Implicit coordinate</altenc>
          </altsys>
          <depthsys>
            <depthdn>Not Available</depthdn>
            <depthres>Not Available</depthres>
            <depthdu>Not Available</depthdu>
            <depthem>Implicit coordinate</depthem>
          </depthsys>
	</vertdef>
      </xsl:otherwise>
      -->
      </xsl:choose>
 </spref>
 <!-- end Spatial Reference information -->

 <!-- begin Distribution Info -->
 <distinfo>
       <xsl:apply-templates select="dif:Data_Center"/> <!-- distributor -->

       <xsl:choose>
       <xsl:when test="dif:Data_Center/dif:Data_Set_ID">      
        <xsl:apply-templates select="dif:Data_Center/dif:Data_Set_ID"/> <!-- resource description -->
       </xsl:when>
       <xsl:otherwise>
        <xsl:apply-templates select="/dif:DIF/dif:Entry_ID"/> <!-- resource description -->
       </xsl:otherwise>     
       </xsl:choose>
       
       <distliab>Not Available</distliab>

       <stdorder>
       <digform>
       <xsl:choose>
       <xsl:when test="dif:Distribution">
          <digtinfo>
            <xsl:apply-templates select="dif:Distribution"/>
	  </digtinfo>
       </xsl:when>
       <xsl:otherwise>
          <digtinfo>
	  <formname>Not Available</formname> <!-- Mandatory field -->
	  </digtinfo>
       </xsl:otherwise>
       </xsl:choose>

       <xsl:choose>
       <xsl:when test="/dif:DIF/dif:Data_Center/dif:Data_Center_URL">
          <digtopt>
            <xsl:apply-templates select="/dif:DIF/dif:Data_Center/dif:Data_Center_URL"/>
          </digtopt>
       </xsl:when>
       <xsl:otherwise>
          <digtopt/>
       </xsl:otherwise>
       </xsl:choose>

       <xsl:choose>
       <xsl:when test="dif:Related_URL">
            <xsl:apply-templates select="dif:Related_URL"/>
       </xsl:when>
       <xsl:otherwise>
          <digtopt/>
       </xsl:otherwise>
       </xsl:choose>

       </digform>

       <xsl:choose>
       <xsl:when test="/dif:DIF/dif:Distribution/dif:Fees">
            <xsl:apply-templates select="/dif:DIF/dif:Distribution/dif:Fees"/>
       </xsl:when>
       <xsl:otherwise>
          <fees>Not Available</fees>
       </xsl:otherwise>
       </xsl:choose>

       </stdorder>
 </distinfo>
 <!-- end Distribution Info -->

 <!-- begin Metadata Info -->
 <metainfo>
       <xsl:choose>
       <xsl:when test="dif:DIF_Creation_Date">
        <xsl:apply-templates select="dif:DIF_Creation_Date"/>
       </xsl:when>
       <xsl:otherwise>
         <metd/>
       </xsl:otherwise>
       </xsl:choose>

       <xsl:apply-templates select="dif:Last_DIF_Revision_Date"/>
       <xsl:apply-templates select="dif:Future_DIF_Review_Date"/>

       <metc>
         <xsl:choose>
            <xsl:when test="dif:Personnel[dif:Role='DIF AUTHOR']">
	     <cntinfo>
                 <xsl:apply-templates select="dif:Personnel[dif:Role='DIF AUTHOR']"/>
	     </cntinfo>
            </xsl:when>
            <xsl:otherwise>
            <cntinfo>
            <cntperp>
            <cntper>GCMD User Support Office</cntper>
            <cntorg>NASA Global Change Master Directory</cntorg>
            </cntperp>
            <cntaddr>
            <addrtype>Mailing and Physical Address</addrtype>
            <address>Not Available</address>
            <city>Lanham</city>
            <state>MD</state>
            <postal>20706</postal>
            <country>USA</country>
            </cntaddr>
            <cntvoice>Not Available</cntvoice>
            <cntemail>gcmduso@gcmd.gsfc.nasa.gov</cntemail>
            </cntinfo>
            </xsl:otherwise>
          </xsl:choose>
       </metc>
       <metstdn>FGDC Content Standards for Digital Geospatial Metadata</metstdn>
       <metstdv>FGDC-STD-001-1998</metstdv>
       <mettc>local time</mettc>
 </metainfo>
<!-- end Metadata Info -->

</metadata>
</xsl:template>

<!-- <xsl:apply-templates select="Data_Set_Citation"/> -->
  <xsl:template match="dif:Data_Set_Citation">
    <citation>
     <citeinfo>
       <xsl:choose>
         <xsl:when test="dif:Dataset_Creator">
          <xsl:apply-templates select="dif:Dataset_Creator"/>
         </xsl:when>
         <xsl:otherwise>
         <origin>Unknown</origin>
         </xsl:otherwise>
       </xsl:choose>
       <xsl:choose>
         <xsl:when test="dif:Dataset_Release_Date">
          <xsl:apply-templates select="dif:Dataset_Release_Date"/>
         </xsl:when>
         <xsl:otherwise>
          <pubdate>Unknown</pubdate>
         </xsl:otherwise>
       </xsl:choose>
       <xsl:choose>
         <xsl:when test="dif:Dataset_Title">
          <xsl:apply-templates select="dif:Dataset_Title"/>
         </xsl:when>
         <xsl:otherwise>
          <xsl:apply-templates select="dif:Entry_Title"/>
         </xsl:otherwise>
       </xsl:choose>
       <edition><xsl:apply-templates select="dif:Version"/></edition>
       <geoform><xsl:apply-templates select="dif:Data_Presentation_Form"/></geoform>
       <serinfo>
       <sername><xsl:apply-templates select="dif:Dataset_Series_Name"/></sername>
       <issue><xsl:apply-templates select="dif:Issue_Identification"/></issue>
       </serinfo>
       <pubinfo>
       <pubplace><xsl:apply-templates select="dif:Dataset_Release_Place"/></pubplace>
       <publish><xsl:apply-templates select="dif:Dataset_Publisher"/></publish>
       </pubinfo>
       <othercit><xsl:apply-templates select="dif:Other_Citation_Details"/></othercit>
       <onlink><xsl:apply-templates select="dif:Online_Resource"/></onlink>
     </citeinfo>
    </citation>
  </xsl:template>

<!-- <xsl:apply-templates select="Originating_Metadata_Node" /> -->
<xsl:template match="dif:Originating_Metadata_Node">
     <citation>
     <citeinfo>
     <origin>Unknown</origin>
     <pubdate>Unknown</pubdate>
     <xsl:apply-templates select="/dif:DIF/dif:Entry_Title"/>
     </citeinfo>
     </citation>
</xsl:template>

<!-- <xsl:apply-templates select="Dataset_Creator"/> etc-->
<xsl:template match="dif:Dataset_Creator">
     <origin><xsl:value-of select="."/></origin>
</xsl:template>
<xsl:template match="dif:Dataset_Release_Date">
     <pubdate><xsl:value-of select="translate(.,'-','')"/></pubdate>
</xsl:template>
<xsl:template match="dif:Dataset_Title">
     <title><xsl:value-of select="."/></title>
</xsl:template>
<xsl:template match="dif:Entry_Title">
     <title><xsl:value-of select="."/></title>
</xsl:template>
<xsl:template match="dif:Version">
     <xsl:value-of select="."/>
</xsl:template>
<xsl:template match="dif:Data_Presentation_Form">
     <xsl:value-of select="."/>
</xsl:template>
<xsl:template match="dif:Dataset_Series_Name">
     <xsl:value-of select="."/>
</xsl:template>
<xsl:template match="dif:Issue_Identification">
     <xsl:value-of select="."/>
</xsl:template>
<xsl:template match="dif:Dataset_Release_Place">
     <xsl:value-of select="."/>
</xsl:template>
<xsl:template match="dif:Dataset_Publisher">
     <xsl:value-of select="."/>
</xsl:template>
<xsl:template match="dif:Dataset_Series_Name">
     <xsl:value-of select="."/>
</xsl:template>
<xsl:template match="dif:Other_Citation_Details">
     <xsl:value-of select="."/>
</xsl:template>
<xsl:template match="dif:Online_Resource">
     <xsl:value-of select="."/>
</xsl:template>

<!-- <xsl:apply-templates select="Summary"/>  -->
<xsl:template match="dif:Summary">
     <xsl:value-of select="."/>
</xsl:template>

<!-- <xsl:apply-templates select="Reference"/>  -->
<xsl:template match="dif:Reference">
     <supplinf><xsl:text> REFERENCE: </xsl:text><xsl:value-of select="."/></supplinf>
</xsl:template>

<!-- <xsl:apply-templates select="Temporal_Coverage"/> -->
<xsl:template match="dif:Temporal_Coverage">
<timeperd>
<timeinfo>
<rngdates>
   <xsl:choose>
       <xsl:when test="dif:Start_Date">
        <xsl:apply-templates select="dif:Start_Date"/>
       </xsl:when>
       <xsl:otherwise>
        <begdate>Unknown</begdate>
       </xsl:otherwise>
   </xsl:choose>
   <xsl:choose>
       <xsl:when test="dif:Stop_Date">
        <xsl:apply-templates select="dif:Stop_Date"/>
       </xsl:when>
       <xsl:otherwise>
         <enddate>Unknown</enddate>
       </xsl:otherwise>
   </xsl:choose>
</rngdates>
</timeinfo>
<current>Unknown</current>
</timeperd>
</xsl:template>

<xsl:template match="dif:Start_Date">
     <begdate><xsl:value-of select="translate(.,'-','')"/></begdate>
</xsl:template>
<xsl:template match="dif:Stop_Date">
     <enddate><xsl:value-of select="translate(.,'-','')"/></enddate>
</xsl:template>

<!-- <xsl:apply-templates select="Data_Set_Progress"/> -->
<xsl:template match="dif:Data_Set_Progress">
    <status>
    <progress>
	<xsl:call-template name="str:capitalise">
        <xsl:with-param name="text" select="."/>
        <xsl:with-param name="all" select="true()"/>
        </xsl:call-template></progress>
     <update>As needed</update>
     </status>
  </xsl:template>

<!-- <xsl:apply-templates select="Spatial_Coverage"/> -->
<xsl:template match="dif:Spatial_Coverage">
   <spdom>
   <bounding>
    <xsl:apply-templates select="dif:Westernmost_Longitude"/>
    <xsl:apply-templates select="dif:Easternmost_Longitude"/>
    <xsl:apply-templates select="dif:Northernmost_Latitude"/>
    <xsl:apply-templates select="dif:Southernmost_Latitude"/>
    </bounding>
   </spdom>
</xsl:template>
<xsl:template match="dif:Westernmost_Longitude">
     <westbc><xsl:value-of select="."/></westbc>
</xsl:template>
<xsl:template match="dif:Easternmost_Longitude">
     <eastbc><xsl:value-of select="."/></eastbc>
</xsl:template>
<xsl:template match="dif:Southernmost_Latitude">
     <southbc><xsl:value-of select="."/></southbc>
</xsl:template>
<xsl:template match="dif:Northernmost_Latitude">
     <northbc><xsl:value-of select="."/></northbc>
</xsl:template>

<!-- <xsl:apply-templates select="Parameters"/> -->
<!--
<xsl:template match="Entry_ID">
     <themekey><xsl:value-of select="."/></themekey>
</xsl:template>
-->
<xsl:template match="dif:Parameters">
   <themekey>
    <xsl:apply-templates select="dif:Category"/>
    <xsl:apply-templates select="dif:Topic"/>
    <xsl:apply-templates select="dif:Term"/>
    <xsl:apply-templates select="dif:Variable_Level_1"/>
    <xsl:apply-templates select="dif:Variable_Level_2"/>
    <xsl:apply-templates select="dif:Variable_Level_3"/>
    <xsl:apply-templates select="dif:Detailed_Variable"/>
   </themekey>
</xsl:template>
<xsl:template match="dif:Category">
        <xsl:call-template name="str:to-upper">
        <xsl:with-param name="text" select="."/>
        <xsl:with-param name="all" select="true()"/>
        </xsl:call-template>
</xsl:template>
<xsl:template match="dif:Topic"> &gt; <xsl:call-template name="str:to-upper">
        <xsl:with-param name="text" select="."/>
        <xsl:with-param name="all" select="true()"/>
        </xsl:call-template>
</xsl:template>
<xsl:template match="dif:Term"> &gt; <xsl:call-template name="str:to-upper">
        <xsl:with-param name="text" select="."/>
        <xsl:with-param name="all" select="true()"/>
        </xsl:call-template>
</xsl:template>
<xsl:template match="dif:Variable_Level_1"> &gt; <xsl:call-template name="str:to-upper">
        <xsl:with-param name="text" select="."/>
        <xsl:with-param name="all" select="true()"/>
        </xsl:call-template>
</xsl:template>
<xsl:template match="dif:Variable_Level_2"> &gt; <xsl:call-template name="str:to-upper">
        <xsl:with-param name="text" select="."/>
        <xsl:with-param name="all" select="true()"/>
        </xsl:call-template>
</xsl:template>
<xsl:template match="dif:Variable_Level_3"> &gt; <xsl:call-template name="str:to-upper">
        <xsl:with-param name="text" select="."/>
        <xsl:with-param name="all" select="true()"/>
        </xsl:call-template>
</xsl:template>
<xsl:template match="dif:Detailed_Variable"> &gt; <xsl:call-template name="str:to-upper">
        <xsl:with-param name="text" select="."/>
        <xsl:with-param name="all" select="true()"/>
     </xsl:call-template>
</xsl:template>

<xsl:template match="dif:Source_Name">
   <themekey>
    <xsl:apply-templates select="dif:Short_Name"/>
    <xsl:apply-templates select="dif:Long_Name"/>
   </themekey>
</xsl:template>
<xsl:template match="dif:Sensor_Name">
   <themekey>
    <xsl:apply-templates select="dif:Short_Name"/>
    <xsl:apply-templates select="dif:Long_Name"/>
   </themekey>
</xsl:template>
<xsl:template match="dif:Project">
   <themekey>
    <xsl:apply-templates select="dif:Short_Name"/>
    <xsl:apply-templates select="dif:Long_Name"/>
   </themekey>
</xsl:template>
<xsl:template match="dif:Short_Name">
    <xsl:call-template name="str:to-upper">
    <xsl:with-param name="text" select="."/>
    <xsl:with-param name="all" select="true()"/>
    </xsl:call-template>
</xsl:template>
<xsl:template match="dif:Long_Name"> &gt; <xsl:call-template name="str:to-upper">
     <xsl:with-param name="text" select="."/>
     <xsl:with-param name="all" select="true()"/>
     </xsl:call-template>
</xsl:template>

<xsl:template match="dif:Keyword">
   <themekey>
    <xsl:call-template name="str:capitalise">
    <xsl:with-param name="text" select="."/>
    <xsl:with-param name="all" select="true()"/>
    </xsl:call-template>
  </themekey>
</xsl:template>


<xsl:template match="dif:Location">
   <placekey>
    <xsl:apply-templates select="dif:Location_Category"/>
    <xsl:apply-templates select="dif:Location_Type"/>
    <xsl:apply-templates select="dif:Location_Subregion1"/>
    <xsl:apply-templates select="dif:Location_Subregion2"/>
    <xsl:apply-templates select="dif:Location_Subregion3"/>
    <xsl:apply-templates select="dif:Detailed_Location"/>
   </placekey>
</xsl:template>
<xsl:template match="dif:Location_Category">
        <xsl:call-template name="str:to-upper">
        <xsl:with-param name="text" select="."/>
        <xsl:with-param name="all" select="true()"/>
        </xsl:call-template>
</xsl:template>
<xsl:template match="dif:Location_Type"> &gt; <xsl:call-template name="str:to-upper">
        <xsl:with-param name="text" select="."/>
        <xsl:with-param name="all" select="true()"/>
        </xsl:call-template>
</xsl:template>
<xsl:template match="dif:Location_Subregion1"> &gt; <xsl:call-template name="str:to-upper">
        <xsl:with-param name="text" select="."/>
        <xsl:with-param name="all" select="true()"/>
        </xsl:call-template>
</xsl:template>
<xsl:template match="dif:Location_Subregion2"> &gt; <xsl:call-template name="str:to-upper">
        <xsl:with-param name="text" select="."/>
        <xsl:with-param name="all" select="true()"/>
        </xsl:call-template>
</xsl:template>
<xsl:template match="dif:Location_Subregion3"> &gt; <xsl:call-template name="str:to-upper">
        <xsl:with-param name="text" select="."/>
        <xsl:with-param name="all" select="true()"/>
     </xsl:call-template>
</xsl:template>
<xsl:template match="dif:Detailed_Location"> &gt; <xsl:call-template name="str:to-upper">
   <xsl:with-param name="text" select="normalize-space(.)"/>
   <xsl:with-param name="all" select="true()"/>
   </xsl:call-template>
</xsl:template>

<xsl:template match="/dif:DIF/dif:Data_Resolution/dif:Temporal_Resolution">
  <tempkey><xsl:call-template name="str:capitalise">
    <xsl:with-param name="text" select="."/>
    <xsl:with-param name="all" select="true()"/>
    </xsl:call-template>
  </tempkey>
</xsl:template>

<xsl:template match="/dif:DIF/dif:Data_Resolution/dif:Temporal_Resolution_Range">
  <tempkey><xsl:call-template name="str:capitalise">
    <xsl:with-param name="text" select="."/>
    <xsl:with-param name="all" select="true()"/>
    </xsl:call-template>
  </tempkey>
</xsl:template>

<xsl:template match="dif:ISO_Topic_Category">
  <themekey><xsl:call-template name="str:to-upper">
    <xsl:with-param name="text" select="."/>
    <xsl:with-param name="all" select="true()"/>
    </xsl:call-template>
  </themekey>
</xsl:template>

<xsl:template match="dif:Data_Set_Language">
  <themekey><xsl:call-template name="str:to-upper">
    <xsl:with-param name="text" select="."/>
    <xsl:with-param name="all" select="true()"/>
    </xsl:call-template>
  </themekey>
</xsl:template>

<!-- <xsl:apply-templates select="Access and Use Constraints"/> -->
<xsl:template match="dif:Access_Constraints">
  <accconst><xsl:value-of select="."/></accconst>
</xsl:template>
<xsl:template match="dif:Use_Constraints">
  <useconst><xsl:value-of select="."/></useconst>
</xsl:template>


<!-- <xsl:apply-templates select="Multimedia_Sample"/> -->
<xsl:template match="dif:Multimedia_Sample">
   <browse>
   <xsl:choose>
    <xsl:when test="dif:URL">
     <browsen><xsl:apply-templates select="dif:URL"/></browsen>
    </xsl:when>
    <xsl:otherwise>
         <browsen><xsl:apply-templates select="dif:File"/></browsen>
    </xsl:otherwise>
   </xsl:choose>
   <xsl:choose>
    <xsl:when test="dif:Description">
         <browsed><xsl:apply-templates select="dif:Description"/></browsed>
    </xsl:when>
    <xsl:otherwise>
         <browsed><xsl:apply-templates select="dif:Caption"/></browsed>
    </xsl:otherwise>
   </xsl:choose>
         <browset><xsl:apply-templates select="dif:Format"/></browset>
   </browse>
  </xsl:template>

<xsl:template match="dif:URL">
     <xsl:value-of select="."/>
</xsl:template>
<xsl:template match="dif:File">
     <xsl:value-of select="."/>
</xsl:template>
<xsl:template match="dif:Description">
     <xsl:value-of select="."/>
</xsl:template>
<xsl:template match="dif:Caption">
     <xsl:value-of select="."/>
</xsl:template>
<xsl:template match="dif:Format">
     <xsl:value-of select="."/>
</xsl:template>

<!-- <xsl:apply-templates select="Reference"/> -->
<!-- NO MATCH BECAUSE FGDC uses citation fields, GCMD one "Reference" fields -->

<!-- <xsl:apply-templates select="/DIF/Data_Resolution"/>  -->
<xsl:template match="/dif:DIF/dif:Data_Resolution">
    <xsl:if test="/dif:DIF/dif:Data_Resolution/dif:Latitude_Resolution/text() != '' or
                 /dif:DIF/dif:Data_Resolution/dif:Longitude_Resolution/text() != ''">
      <horizsys>
       <geograph>
        <xsl:apply-templates select="/dif:DIF/dif:Data_Resolution/dif:Latitude_Resolution"/>
        <xsl:apply-templates select="/dif:DIF/dif:Data_Resolution/dif:Longitude_Resolution"/>
	<geogunit>Decimal degrees</geogunit>
       </geograph>
      </horizsys>
    </xsl:if>
    <xsl:if test="/dif:DIF/dif:Data_Resolution/dif:Vertical_Resolution/text() != '' ">
     <vertdef>
       <xsl:apply-templates select="/dif:DIF/dif:Data_Resolution/dif:Vertical_Resolution"/>
     </vertdef>
    </xsl:if>
</xsl:template>

<xsl:template match="/dif:DIF/dif:Data_Resolution/dif:Latitude_Resolution">
     <latres><xsl:value-of select="."/></latres>
</xsl:template>
<xsl:template match="/dif:DIF/dif:Data_Resolution/dif:Longitude_Resolution">
     <longres><xsl:value-of select="."/></longres>
</xsl:template>
<!-- Vertical Resolution is match to both altitude and depth resolution in FGDC -->
<xsl:template match="/dif:DIF/dif:Data_Resolution/dif:Vertical_Resolution">
     <altsys>
      <altdatum>Not Available</altdatum> <!-- Mandatory field -->
      <altres><xsl:value-of select="."/></altres> <!-- Mandatory field -->
      <altunits>Not Available</altunits> <!-- Mandatory field -->
      <altenc>Implicit coordinate</altenc> <!-- Mandatory field -->
     </altsys>
     <depthsys>
      <depthdn>Not Available</depthdn> <!-- Mandatory field -->
      <depthres><xsl:value-of select="."/></depthres> <!-- Mandatory field -->
      <depthdu>Not Available</depthdu> <!-- Mandatory field -->
      <depthem>Implicit coordinate</depthem> <!-- Mandatory field -->
     </depthsys>
</xsl:template>

<!-- <xsl:apply-templates select="Quality"/> -->
<xsl:template match="dif:Quality">
    <attracc>
    <attraccr><xsl:value-of select="."/></attraccr>
    </attracc>
    <logic>Not Available</logic> <!-- Mandatory in data quality -->
    <complete>Not Available</complete> <!-- Mandatory in data quality -->
    <lineage>
    	<procstep>
		<procdesc>Not Available</procdesc>
	        <procdate>Unknown</procdate>
        </procstep>
    </lineage> <!-- Mandatory in data quality -->
</xsl:template>

<!-- <xsl:apply-templates select="Data_Center and Distribution information"/> -->
<xsl:template match="dif:Data_Center">
 <distrib>
   <cntinfo>
     <cntorgp>
      <cntorg>
        <xsl:apply-templates select="dif:Data_Center_Name/dif:Short_Name"/>
        <xsl:apply-templates select="dif:Data_Center_Name/dif:Long_Name"/>
      </cntorg>
      <cntper>
       <xsl:apply-templates select="dif:Personnel/dif:First_Name"/>
       <xsl:apply-templates select="dif:Personnel/dif:Middle_Name"/>
       <xsl:apply-templates select="dif:Personnel/dif:Last_Name"/>
      </cntper>
     </cntorgp>
     <!-- <xsl:apply-templates select="Personnel"/>
     Contact_Information permits only one of Contact_Person_Primary or
     Contact_Organization_Primary  -->
     <!-- <xsl:apply-templates select="/DIF/Data_Center/Personnel/Role"/> -->
     <cntpos>DATA CENTER CONTACT</cntpos>
     <xsl:apply-templates select="dif:Personnel/dif:Contact_Address" />
     <xsl:apply-templates select="dif:Personnel/dif:Phone" />
     <xsl:apply-templates select="dif:Personnel/dif:Fax" />
     <xsl:apply-templates select="dif:Personnel/dif:Email" />
   </cntinfo>
  </distrib>
</xsl:template>

<xsl:template match="dif:Personnel/dif:Contact_Address">
   <cntaddr>
     <addrtype>Mailing and Physical Address</addrtype>  
     <xsl:apply-templates select="dif:Address"/>           
     <xsl:apply-templates select="dif:City"/>         
     <xsl:apply-templates select="dif:Province_or_State"/>          
     <xsl:apply-templates select="dif:Postal_Code"/>       
     <xsl:apply-templates select="dif:Country"/>        
   </cntaddr>
  </xsl:template>

<xsl:template match="dif:Data_Center_Name/dif:Short_Name"><xsl:value-of select="."/></xsl:template>
<xsl:template match="dif:Data_Center_Name/dif:Long_Name"> &gt; <xsl:value-of select="."/></xsl:template>
<xsl:template match="dif:Personnel/First_Name"><xsl:value-of select="."/><xsl:text> </xsl:text></xsl:template>
<xsl:template match="dif:Personnel/Middle_Name"><xsl:value-of select="."/><xsl:text> </xsl:text></xsl:template>
<xsl:template match="dif:Personnel/Last_Name"><xsl:value-of select="."/></xsl:template>
<xsl:template match="dif:Personnel/Phone"><cntvoice><xsl:value-of select="."/></cntvoice></xsl:template>
<xsl:template match="dif:Personnel/Fax"><cntfax><xsl:value-of select="."/></cntfax></xsl:template>
<xsl:template match="dif:Personnel/Email"><cntemail><xsl:value-of select="."/></cntemail></xsl:template>

<xsl:template match="dif:Data_Center/dif:Data_Set_ID"><resdesc><xsl:value-of select="."/></resdesc></xsl:template>
<xsl:template match="/dif:DIF/dif:Entry_ID"><resdesc><xsl:value-of select="."/></resdesc></xsl:template>

<!-- Distribution information -->
<xsl:template match="Distribution">
      <xsl:choose>
       <xsl:when test="dif:Distribution_Format">
        <xsl:apply-templates select="dif:Distribution_Format"/>
       </xsl:when>
       <xsl:otherwise>
         <formname>Not Available</formname>
       </xsl:otherwise>
      </xsl:choose>
      <xsl:choose>
       <xsl:when test="dif:Distribution_Size">
        <xsl:apply-templates select="dif:Distribution_Size"/>
       </xsl:when>
       <xsl:otherwise>
       </xsl:otherwise>
      </xsl:choose>
</xsl:template>


<xsl:template match="dif:Distribution_Format"><formname><xsl:value-of select="."/></formname></xsl:template>
<xsl:template match="dif:Distribution_Size"><transize><xsl:value-of select="."/></transize></xsl:template>

<xsl:template match="/dif:DIF/dif:Data_Center/dif:Data_Center_URL">
     <onlinopt>
     <computer>
     <networka>
     <networkr><xsl:value-of select="."/></networkr>
     </networka>
     </computer>
     <accinstr>DATA CENTER URL</accinstr>
     </onlinopt>
</xsl:template>

<xsl:template match="dif:Related_URL">
      <xsl:choose>
       <xsl:when test="dif:URL">
       <digtopt>
        <onlinopt>
         <computer>
          <networka>
           <networkr><xsl:apply-templates select="dif:URL"/></networkr>
          </networka>
         </computer>
	 <accinstr><xsl:apply-templates select="dif:Description"/></accinstr>
        </onlinopt>
       </digtopt>
       </xsl:when>
       <xsl:otherwise>
           <digtopt><onlinopt><computer><networka><networkr>Not Available</networkr></networka></computer></onlinopt></digtopt>
       </xsl:otherwise>
      </xsl:choose>
</xsl:template>
<xsl:template match="dif:URL"><xsl:value-of select="."/></xsl:template>
<xsl:template match="dif:Description"><xsl:value-of select="."/></xsl:template>

<xsl:template match="/dif:DIF/dif:Distribution/dif:Fees">
     <xsl:choose>
       <xsl:when test="/dif:DIF/dif:Distribution/dif:Fees">
        <xsl:apply-templates select="/dif:DIF/dif:Distribution/dif:Fees"/>
       </xsl:when>
       <xsl:otherwise>
         <fees>Not Available</fees>
       </xsl:otherwise>
      </xsl:choose>
</xsl:template>

<xsl:template match="/dif:DIF/dif:Distribution/dif:Fees">
    <fees><xsl:call-template name="str:capitalise">
    <xsl:with-param name="text" select="."/>
    <xsl:with-param name="all" select="true()"/>
    </xsl:call-template></fees>
</xsl:template>

<!--
<xsl:template match="/DIF/Distribution/Fees"><fees><xsl:value-of select="."/></fees></xsl:template>
-->

<!-- Personnel/Contact Information -->
<xsl:template match="dif:Personnel">
     <cntperp>
      <cntper>
       <xsl:apply-templates select="dif:First_Name"/>
       <xsl:apply-templates select="dif:Middle_Name"/>
       <xsl:apply-templates select="dif:Last_Name"/>
      </cntper>
     </cntperp>
    <xsl:apply-templates select="dif:Role"/>
     <xsl:choose>
        <xsl:when test="dif:Contact_Address">
        <xsl:apply-templates select="dif:Contact_Address"/>
        </xsl:when>
        <xsl:otherwise>
          <cntaddr>
          <addrtype>Mailing and Physical Address</addrtype>
          <address>Not Available</address>
          <city>Not Available</city>
          <state>Not Available</state>
          <postal>Not Available</postal>
          </cntaddr>
        </xsl:otherwise>
     </xsl:choose>
    <xsl:apply-templates select="dif:Phone" />
    <xsl:apply-templates select="dif:Fax" />
    <xsl:apply-templates select="dif:Email" />
</xsl:template>

<xsl:template match="dif:Contact_Address">
   <cntaddr>
     <addrtype>Mailing and Physical Address</addrtype>
      <xsl:choose>
        <xsl:when test="dif:Address">
         <xsl:apply-templates select="dif:Address"/>
        </xsl:when>
        <xsl:otherwise>
         <address>Not Available</address>
        </xsl:otherwise>
      </xsl:choose>
      <xsl:choose>
        <xsl:when test="dif:City">
         <xsl:apply-templates select="dif:City"/>
        </xsl:when>
        <xsl:otherwise>
         <city>Not Available</city>
        </xsl:otherwise>
      </xsl:choose>
      <xsl:choose>
        <xsl:when test="dif:Province_or_State">
         <xsl:apply-templates select="dif:Province_or_State"/>
        </xsl:when>
        <xsl:otherwise>
         <state>Not Available</state>
       </xsl:otherwise>
      </xsl:choose>
      <xsl:choose>
        <xsl:when test="dif:Postal_Code">
         <xsl:apply-templates select="dif:Postal_Code"/>
        </xsl:when>
        <xsl:otherwise>
         <postal>Not Available</postal>
        </xsl:otherwise>
      </xsl:choose>
      <xsl:choose>
        <xsl:when test="dif:Country">
         <xsl:apply-templates select="dif:Country"/>
        </xsl:when>
        <xsl:otherwise>
         <postal>Not Available</postal>
        </xsl:otherwise>
      </xsl:choose>
   </cntaddr>
  </xsl:template>

<xsl:template match="dif:First_Name"><xsl:value-of select="."/><xsl:text> </xsl:text></xsl:template>
<xsl:template match="dif:Middle_Name"><xsl:value-of select="."/><xsl:text> </xsl:text></xsl:template>
<xsl:template match="dif:Last_Name"><xsl:value-of select="."/><xsl:text> </xsl:text></xsl:template>
<xsl:template match="dif:Role"><cntpos><xsl:value-of select="."/></cntpos></xsl:template>
<xsl:template match="dif:Phone"><cntvoice><xsl:value-of select="."/></cntvoice></xsl:template>
<xsl:template match="dif:Fax"><cntfax><xsl:value-of select="."/></cntfax></xsl:template>
<xsl:template match="dif:Email"><cntemail><xsl:value-of select="."/></cntemail></xsl:template>
<xsl:template match="dif:Address"><address><xsl:value-of select="."/><xsl:text> </xsl:text></address></xsl:template>
<xsl:template match="dif:City"><city><xsl:value-of select="."/></city></xsl:template>
<xsl:template match="dif:Province_or_State"><state><xsl:value-of select="."/></state></xsl:template>
<xsl:template match="dif:Postal_Code"><postal><xsl:value-of select="."/></postal></xsl:template>
<xsl:template match="dif:Country"><country><xsl:value-of select="."/></country></xsl:template>

<!-- <xsl:apply-templates select="DIF_Creation_Date"/> -->
<xsl:template match="dif:DIF_Creation_Date"><metd><xsl:value-of select="translate(.,'-','')"/></metd></xsl:template>
<xsl:template match="dif:Last_DIF_Revision_Date"><metrd><xsl:value-of select="translate(.,'-','')"/></metrd></xsl:template>
<xsl:template match="dif:Future_DIF_Review_Date"><metfrd><xsl:value-of select="translate(.,'-','')"/></metfrd></xsl:template>

</xsl:stylesheet>
