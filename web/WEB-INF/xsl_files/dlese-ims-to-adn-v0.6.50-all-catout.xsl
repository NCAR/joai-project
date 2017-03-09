<?xml version="1.0"?>
<xsl:stylesheet 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xsi ="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:d="http://www.dlese.org/"
    exclude-result-prefixes="xsi d" 
    version="1.0">
    
<!--ORGANIZATION OF THIS FILE-->
<!-- **************************************-->
<!--The top half of this file, before the break with 5 lines of blank comments, is the apply-templates logic. This section is organized in document order of an ADN  item-level metadata record. The bottom half, after the break with 5 lines of blank comments, are the templates. The templates are organized in alphabetical order.-->

<!--ASSUMPTIONS-->
<!-- **************************************-->
<!--1. The transform is run over only DLESE-IMS records a collection considers to be accessioned; This assumes that the DLESE-IMS metametadata.catalogentry.accession tag has content, otherwise an error is given upon validation.-->
<!--2. For external collections, the ADN fields of dateInfo.created and dateInfo.accessioned will not be assigned but will use the collection builder dates. As such, it is assumed that at least 1 DLESE-IMS metametadata.contributor tag has been completed with at least a role=Creator and a date, this needs to be true for those collection builder who suppress cataloger information, otherwise an error is given upon validation-->
<!--3. For the DLESE Community Collection, the ADN fields of dateInfo.created and dateInfo.accessioned will not be assigned; the DLESE-IMS dates will be used.-->
<!--4. Under DLESE-IMS educational.learning resource type, it is assumed that the collection builder has used the single langstring tag to indicate resource type rather than the extension.category.langstring tags.-->
<!--5. Assumes all resources cataloged in DLESE-IMS will be online resources in ADN-->
<!--6. Assumes the 1st DLESE-IMS technical.location tag is the ADN technical.online.primaryURL tag content. If there is more than one DLESE-IMS technical.location tag, then a ADN technical.online.mirrorURLs tag is generated.-->
<!--7. The first DLESE-IMS metametadata.contribute.role.langstring='Creator' will be the date the record is created for the ADN metadata record.-->
<!--8. Assumes DLESE-IMS general.topic field has content; no test for the presence of content because this is required for ADN-->
<!--9. Assumes lifecycle has content; this is okay because ADN will enforce a contributor-->
<!--10. Accounts for the fact that DLESE-IMS keywords may be blank-->
<!--11. Does not transform the contents of DLESE-IMS fields general.coverage.extension.begtime.description or general.coverage.extension.endtime.description but does do DLESE-IMS fields of general.coverage.extension.begtime.datecoverage or time and general.coverage.extension.endtime.datecoverage or time; this means no date/time information older than 0000AD or relative time is transformed. The transform checks for content in the date/time description fields so that an error will generate on ADN validation. Then a manual check can be performed on the record.--> 
<!--12. Skips transforming DLESE-IMS educational.semanticdensity. There is no equivalent in ADN-->
<!--13. Skips transforming DLESE-IMS general.aggregationlevel. There is no equivalent in ADN-->
<!--14. Skips transforming DLESE-IMS general.structure. There is no equivalent in ADN-->
<!--15. Skips transforming DLESE-IMS education.typicallearningtime.datetime because in ADN learning time is associated with an individual gradeRange. Since ADN gradeRange can occur multiple times there is no way to correctly transform the single-occurrence DLESE-IMS value-->
<!--16. Skips transforming DLESE-IMS education.typicalagerange because in ADN age range is associated with an individual gradeRange. Since ADN gradeRange can occur multiple times there is no way to correctly transform the single-occurrence DLESE-IMS value -->
<!--17. Skips transforming DLESE-IMS educational.intendedenduserrole.langstring because in ADN this concept, tooFor, is associated with an individual gradeRange. Since ADN gradeRange can occur multiple times there is no way to correctly transform the single-occurrence DLESE-IMS value-->
<!--18. Skips transforming the entire DLESE-IMS annotation tag set. There is no equivalent in ADN; use the DLESE annotation metadata framework-->
<!--19. Skips transforming the entire DLESE-IMS collection tag set. There is no equivalent in ADN-->
<!--20. Skips transforming DLESE-IMS rights.copyrightand other restrictions. There is no equivalent in ADN-->
<!--21. A value of 9999 in DLESE-IMS general.coverage.begtime.datecoverage or general.coverage.endtime.datecoverage is assumed to represent the 'Present' in ADN temporalCoverages.timeAndPeriod.timeAD.begin.date and end.date-->

	<xsl:output method="xml" indent="yes" encoding="UTF-8"/>

<!--VARIABLES used throughout the transform-->
<!-- **********************************************************-->
<!--variable for adding a line return-->
	<xsl:variable name="newline">
		<xsl:text>
		</xsl:text>
	</xsl:variable>

	<xsl:template match="*|/">
		<xsl:text disable-output-escaping="yes">&lt;itemRecord xmlns=&quot;http://adn.dlese.org&quot; xmlns:xsi=&quot;http://www.w3.org/2001/XMLSchema-instance&quot; xsi:schemaLocation=&quot;http://adn.dlese.org http://www.dlese.org/Metadata/adn-item/0.6.50/record.xsd&quot;&gt;</xsl:text>
		<xsl:value-of select="$newline"/>
         <xsl:apply-templates/>
	</xsl:template>

<!--begin TEMPLATES TO APPLY. Referenced by the ADN field name-->
<!-- *******************************************************************************-->

	<xsl:template match="d:record">

<!--**********************************************************************************************-->
<!--GENERAL category-->
		<xsl:element name="general">
			<xsl:value-of select="$newline"/>

<!-- title-->
			<xsl:element name="title">
				<xsl:apply-templates select="d:general/d:title"/> <!--see LANGSTRING template-->
			</xsl:element>			
			<xsl:value-of select="$newline"/>

<!--general description-->
			<xsl:apply-templates select="d:general/d:description"/> <!--see DESCRIPTION template-->
			<xsl:value-of select="$newline"/>

<!--general language-->
			<xsl:if test="string-length(d:general/d:language)>0">
				<xsl:apply-templates select="d:general/d:language"/> <!--see LANUAGE template-->
				<xsl:value-of select="$newline"/>
			</xsl:if>

<!--begin subjects-->
<!--no test to determine presence of content, like in kewords; assuming there is content in the DLESE-IMS topic field-->
			<xsl:element name="subjects">
				<xsl:value-of select="$newline"/>
				<xsl:apply-templates select="d:general/d:extension/d:topic"/> <!--see TOPIC template-->
			</xsl:element>
			<xsl:value-of select="$newline"/>
<!--end subjects-->

<!--begin keywords below-->
<!--determine if any DLESE-IMS general.keywords tags have any content; do this by defining a variables to grab the entire node (rather than doing a string function test that only grabs the 1st tag); this variable (rather than the 1st tag) is then tested for content-->

<!--	variable for DLESE-IMS general.keywords-->
			<xsl:variable name="allkeywords">
				<xsl:for-each select="d:general/d:keywords/d:langstring">
					<xsl:value-of select="."/>
				</xsl:for-each>
			</xsl:variable>
	
<!--test for the presence of content in DLESE-IMS general.keywords; do this to prevent the ADN:general.keywords tag from appearing if it doesn't need to-->
			<xsl:if test="string-length($allkeywords)>0">
				<xsl:element name="keywords">
					<xsl:value-of select="$newline"/>
					<xsl:apply-templates select="d:general/d:keywords"/> <!-- see KEYWORDS template-->
				</xsl:element>
				<xsl:value-of select="$newline"/>
			</xsl:if>
<!--end keywords above-->

<!--begin general catalogEntries-->
<!--determine if any DLESE-IMS general.catalogentry tags have any content; do this by defining a variables to grab the entire node (rather than doing a string function test that only grabs the 1st tag); this variable (rather than the 1st tag) is then tested for content-->

<!--	variable for DLESE-IMS general.catalogentry-->
			<xsl:variable name="allcatentry">
				<xsl:for-each select="d:general/d:catalogentry/d:entry/d:langstring">
					<xsl:value-of select="."/>
				</xsl:for-each>
			</xsl:variable>

<!--test for the presence of content in the DLESE-IMS general.catalogentry tags; do this to prevent the ADN:general.catalogEntries tag from appearing if it doesn't need to-->
			<xsl:if test="string-length($allcatentry)>0">
				<xsl:element name="catalogEntries">
					<xsl:value-of select="$newline"/>
					<xsl:apply-templates select="d:general/d:catalogentry"/> <!--see CATALOGENTRY template-->
				</xsl:element>
				<xsl:value-of select="$newline"/>
			</xsl:if>
<!--end general catalogEntries above-->

		</xsl:element>
		<xsl:value-of select="$newline"/>
<!--end GENERAL category-->

<!--**********************************************************************************************-->
<!--LIFECYCLE category-->
		<xsl:element name="lifecycle">
			<xsl:value-of select="$newline"/>
	
	<!-- version-->		
			<xsl:if test="string-length(d:lifecycle/d:version/d:langstring)>0">
				<xsl:element name="version">
					<xsl:apply-templates select="d:lifecycle/d:version"/> <!--see LANGSTRING template-->
				</xsl:element>
				<xsl:value-of select="$newline"/>
			</xsl:if>

<!--begin lifecycle contributors below-->
<!--determine if any DLESE-IMS contribute.role tags have any content; do this by defining a variables to grab the entire node (rather than doing a string function test that only grabs the 1st tag); this variable (rather than the 1st tag) is then tested for content-->

<!--	variable for DLESE-IMS lifecycle.contribute.role-->
			<xsl:variable name="allroles">
				<xsl:for-each select="d:lifecycle/d:contribute/d:role/d:langstring">
					<xsl:value-of select="."/>
				</xsl:for-each>
			</xsl:variable>
	
<!--DLESE-IMS records can have a contribute.role.langstring value of Creator and date and no other information; this code deals with that--> 
<!--test for the presence of content in the DLESE-IMS lifecycle.contribute.role tags; do this to prevent the ADN:lifecycle.contributors tag from appearing if it doesn't need to-->
			<xsl:if test="string-length($allroles)>0">
				<xsl:element name="contributors">
					<xsl:value-of select="$newline"/>
					<xsl:apply-templates select="d:lifecycle/d:contribute"/><!--see CONTRIBUTE template-->
				</xsl:element>
				<xsl:value-of select="$newline"/>
			</xsl:if>
<!--end lifecycle contributors above-->
		</xsl:element>
		<xsl:value-of select="$newline"/>
<!--end LIFECYCLE category-->


<!--**********************************************************************************************-->
<!--METAMETADATA category-->
		<xsl:element name="metaMetadata">
			<xsl:value-of select="$newline"/>

<!--begin metaMetadata catalogEntries-->
<!--determine if any DLESE-IMS metametadata.catalogentry tags have any content; do this by defining a variables to grab the entire node (rather than doing a string function test that only grabs the 1st tag); this variable (rather than the 1st tag) is then tested for content-->

<!--	variable for DLESE-IMS metametadata.catalogentry-->
			<xsl:variable name="allmcatentry">
				<xsl:for-each select="d:metametadata/d:catalogentry/d:entry/d:langstring">
					<xsl:value-of select="."/>
				</xsl:for-each>
			</xsl:variable>
		
<!--test for the presence of content in the DLESE-IMS metametadata.catalogentry tags; do this to prevent the ADN:metaMetadata.catalogEntries tag from appearing if it doesn't need to-->
			<xsl:if test="string-length($allmcatentry)>0">
				<xsl:element name="catalogEntries">
					<xsl:value-of select="$newline"/>
					<xsl:apply-templates select="d:metametadata/d:catalogentry"/><!--see CATALOGENTRY template-->
				</xsl:element>
				<xsl:value-of select="$newline"/>
			</xsl:if>
<!--end metaMetadata catalogEntries above-->

<!--dateinfo-->
<!--To generate the ADN dateInfo.created attribute, use the date of the 1st DLESE-IMS metametadata.contributor.role=Creator tag. Even if the DLESE-IMS creator name is not complete, the creator role and date will be present to grab.-->
<!--This code  grabs the 1st DLESE-IMS metametadata.contributor.role=Creator tag and ignores all others-->
<!--Assumes that at least 1 DLESE-IMS metametadata.contributor tag has been completed with at least a role=Creator and a date, this need to be true for those collection builder who suppress cataloger information-->
			<xsl:element name="dateInfo">
				<xsl:attribute name="created">
					<xsl:choose>
						<xsl:when test="d:metametadata/d:contribute/d:role/d:langstring[contains(.,'Creator')]">
							<xsl:value-of select="d:metametadata/d:contribute/d:date/d:datetime"/>
						</xsl:when>
						<xsl:otherwise>
							<xsl:text>Need a record creation date</xsl:text>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:attribute>
				<xsl:attribute name="accessioned">
					<xsl:choose>
						<xsl:when test="string-length(d:metametadata/d:catalogentry/d:accession)>0">
							<xsl:value-of select="d:metametadata/d:catalogentry/d:accession"/>
						</xsl:when>
						<xsl:otherwise>
							<xsl:text>ERROR:Need a record accession date</xsl:text>
						</xsl:otherwise> 
					</xsl:choose>
				</xsl:attribute>
			</xsl:element>
			<xsl:value-of select="$newline"/>
	
<!--statusOf-->
			<xsl:element name="statusOf">
				<xsl:attribute name="status">
					<xsl:choose>
						<xsl:when test="string-length(d:metametadata/d:catalogentry/d:accession)>0">
							<xsl:value-of select="'Accessioned'"/>
						</xsl:when>
						<xsl:otherwise>
							<xsl:text>Need a record status</xsl:text>					
						</xsl:otherwise>
					</xsl:choose>
				</xsl:attribute>
			</xsl:element>
			<xsl:value-of select="$newline"/>
		
<!--metaMetadata language-->
			<xsl:apply-templates select="d:metametadata/d:language"/> <!--see LANGUAGE template-->
			<xsl:value-of select="$newline"/>
	
<!--scheme-->
			<xsl:element name="scheme">
				<xsl:value-of select="'ADN (ADEPT/DLESE/NASA Alexandria Digital Earth Prototype/Digital Library for Earth System Education/National Aeronautics and Space Administration)'"/>
			</xsl:element>
			<xsl:value-of select="$newline"/>

<!--metaMetadata copyright-->
			<xsl:element name="copyright">
				<xsl:value-of select="d:metametadata/d:metadataCopyright"/>
			</xsl:element>
			<xsl:value-of select="$newline"/>
		
<!--metaMetadata termsOfUse-->
			<xsl:element name="termsOfUse">
				<!--test to see if ADN:metaMetadata.termsOfUse.URI is needed-->
				<xsl:if test="string-length(d:metametadata/d:metadataTermsOfUse/@URI)>0">
					<xsl:attribute name="URI">
						<xsl:value-of select="d:metametadata/d:metadataTermsOfUse/@URI"/>
					</xsl:attribute>
				</xsl:if>
				<xsl:value-of select="d:metametadata/d:metadataTermsOfUse"/>
			</xsl:element>
			<xsl:value-of select="$newline"/>

<!--begin metaMetadata contributors-->
<!--determine if any DLESE-IMS contribute.role tags have any content; do this by defining a variables to grab the entire node (rather than doing a string function test that only grabs the 1st tag); this variable (rather than the 1st tag) is then tested for content-->

<!--	variable for DLESE-IMS metametadata.contribute.role-->
<!--			<xsl:variable name="allmroles">
				<xsl:for-each select="d:metametadata/d:contribute/d:role/d:langstring">
					<xsl:value-of select="."/>
				</xsl:for-each>
			</xsl:variable>-->
<!--many DLESE-IMS records have a contribute.role.langstring value of Creator and date and then no other information; this code deals with that--> 
<!--test for the presence of content in the DLESE-IMS metametadata.contribute.role tags; do this to prevent the ADN:metadata.contributors tag from appearing if it doesn't need to-->
<!--			<xsl:if test="string-length($allmroles)>0">
				<xsl:element name="contributors">
					<xsl:value-of select="$newline"/>-->
<!--					<xsl:apply-templates select="d:metametadata/d:contribute"/>--> <!--see CONTRIBUTE template-->
<!--				</xsl:element>
				<xsl:value-of select="$newline"/>
			</xsl:if>-->
<!--end metaMetadata contributors above-->

		</xsl:element>
		<xsl:value-of select="$newline"/>
<!--end METAMETADATA category-->


<!--**********************************************************************************************-->
<!--TECHNICAL category-->
		<xsl:element name="technical">
			<xsl:value-of select="$newline"/>

<!--begin online below-->
			<xsl:element name="online">
				<xsl:value-of select="$newline"/>

<!--primaryURL-->
<!--assumes the 1st DLESE-IMS technical.location tag is the ADN technical.online.primaryURL content-->
<!--use an XPATH expression to grab only the first URL and put it in a variable-->
<!--a variable is used to then check for spaces in the URL and remove them in order to comply with the W3C URI type-->
<!--this testing assumes a maximum of 2 blank spaces in the URL; if there are more this code won't find it-->
				<xsl:element name="primaryURL">
					<xsl:choose>
						<xsl:when test="contains(d:technical/d:location[position()=1], ' ')">
						<!--fix the first blank space occurrence-->
						<xsl:variable name="buildurl">
							<xsl:value-of select="concat(substring-before(d:technical/d:location[position()=1], ' '), '%20', substring-after(d:technical/d:location[position()=1], ' '))"/>
						</xsl:variable>
						<!--test and fix for a 2nd blank space occurrence-->
						<xsl:choose>
							<xsl:when test="contains($buildurl, ' ')">
								<!--this is the write if there are 2 blank spaces-->
								<xsl:value-of select="concat(substring-before($buildurl, ' '), '%20', substring-after($buildurl, ' '))"/>
							</xsl:when>
						<xsl:otherwise>
								<!--this is the write if there is only 1 blank space-->
								<xsl:value-of select="$buildurl"/>
							</xsl:otherwise>
						</xsl:choose>
			
<!--next line works on all blank spaces at once but only replaces with a % rather thana %20-->
<!--W3C note: If the third argument string is longer than the second argument string, then excess characters are ignored-->
<!--							<xsl:value-of select="translate(d:technical/d:online/d:primaryURL, ' ', '%20')"/>		-->
						</xsl:when>
						<xsl:otherwise>
							<!--this is the write if there are no blank spaces-->
							<xsl:value-of select="d:technical/d:location[position()=1]"/>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:element>
				<xsl:value-of select="$newline"/>

<!--begin mirrorURLs below: read assumptions above-->
<!--determine if any DLESE-IMS technical.location tags past the 1st tag have content; do this by defining a variables to grab the entire node (rather than doing a string function test that only grabs the 1st tag) past the 1st tag; this variable is then tested for content-->

<!--	variable for DLESE-IMS technical.location-->
				<xsl:variable name="tlocation">
					<xsl:for-each select="d:technical/d:location[position()>1]">
						<xsl:value-of select="."/>
					</xsl:for-each>
				</xsl:variable>
	
<!--test for the presence of content in the DLESE-IMS technical.location (past the 1st tag); do this to prevent the ADN:technical.online.mirrorURLs tag from appearing if it doesn't need to-->
				<xsl:if test="string-length($tlocation)>0"> 
					<xsl:element name="mirrorURLs">
						<xsl:value-of select="$newline"/>
						<!--use an XPATH expression to grab URLs after the first one. These are the mirror URLs-->
						<xsl:apply-templates select="d:technical/d:location[position()>1]"/> <!--MIRROR-URL template-->
						<xsl:value-of select="$newline"/>
					</xsl:element>
					<xsl:value-of select="$newline"/>
				</xsl:if> 
<!--end mirrorULRs above-->

<!--begin mediums below-->
<!--determine if any DLESE-IMS technical.format tags have any content; do this by defining a variables to grab the entire node (rather than doing a string function test that only grabs the 1st tag); this variable (rather than the 1st tag) is then tested for content-->

<!--	variable for DLESE-IMS technical.format-->
				<xsl:variable name="allformat">
					<xsl:for-each select="d:technical/d:format/d:langstring">
						<xsl:value-of select="."/>
					</xsl:for-each>
				</xsl:variable>
		
<!--test for the presence of content in the DLESE-IMS technical.format tags; do this to prevent the ADN:technical.online.mediums tag from appearing if it doesn't need to-->
				<xsl:if test="string-length($allformat)>0">
					<xsl:element name="mediums">
						<xsl:value-of select="$newline"/>
						<xsl:apply-templates select="d:technical/d:format"/> <!--see FORMAT template-->
					</xsl:element>			
					<xsl:value-of select="$newline"/>
				</xsl:if>
<!--end mediums above-->

<!-- size-->		
				<xsl:if test="string-length(d:technical/d:size)>0">
					<xsl:element name="size">
						<xsl:value-of select="d:technical/d:size"/>
					</xsl:element>
					<xsl:value-of select="$newline"/>
				</xsl:if>


<!--duration-->
				<xsl:if test="string-length(d:technical/d:duration/d:datetime)>0">
					<xsl:element name="duration">
						<xsl:value-of select="d:technical/d:duration/d:datetime"/>
					</xsl:element>
					<xsl:value-of select="$newline"/>
				</xsl:if>

<!--begin requirements below-->
<!--no if tests here like in kewords because this crosswalk is assuming there is content in the DLESE-IMS requirements.typename field-->
				<xsl:element name="requirements">
					<xsl:value-of select="$newline"/>
					<xsl:apply-templates select="d:technical/d:requirements"/><!--see REQUIREMENTS template-->
				</xsl:element>
				<xsl:value-of select="$newline"/>
<!--end requirements above-->

<!--begin otherRequirements below-->
<!--this if test prevents processing DLESE-IMS technical.otherrequirementsinfo if it is null-->
<!--must do this if test to prevent the ADN:technical.otherRequirements tag from appearing if it doesn't need to-->
<!--since DLESE-IMS technical.otherrequirementsinfo can only appear once, don't need to set up a variable to grab an entire node set.-->
				<xsl:if test="string-length(d:technical/d:otherrequirementsinfo)>0">
					<xsl:element name="otherRequirements">
						<xsl:value-of select="$newline"/>
						<xsl:element name="otherRequirement">
							<xsl:value-of select="$newline"/>
							<xsl:element name="otherType">
								<xsl:value-of select="d:technical/d:otherrequirementsinfo"/>
							</xsl:element>
							<xsl:value-of select="$newline"/>
						</xsl:element>
						<xsl:value-of select="$newline"/>
					</xsl:element>
					<xsl:value-of select="$newline"/>
				</xsl:if>
<!--end otherRequirements above-->
			</xsl:element>
			<xsl:value-of select="$newline"/>
<!--end online above-->
		</xsl:element>
		<xsl:value-of select="$newline"/>
<!--end technical above-->


<!--**********************************************************************************************-->
<!--EDUCATIONAL category-->
		<xsl:element name="educational">
			<xsl:value-of select="$newline"/>

<!--audiences below-->
<!--no if tests here like in kewords because this crosswalk is assuming there is content in at least 1 DLESE-IMS learningcontext.langstring field-->
			<xsl:element name="audiences">
				<xsl:value-of select="$newline"/>
				<xsl:apply-templates select="d:educational/d:learningcontext"/> <!--see LEARNINGCONTEXT temp.-->
			</xsl:element>			
			<xsl:value-of select="$newline"/>
<!--end audiences above-->

<!--resourceTypes below-->
<!--no if tests here like in kewords because this crosswalk is assuming there is content in at least 1 DLESE-IMS learningresourccetype.langstring field-->
			<xsl:element name="resourceTypes">
				<xsl:value-of select="$newline"/>
				<xsl:apply-templates select="d:educational/d:learningresourcetype"/> <!--see RESOURCETYPE temp.-->
			</xsl:element>
			<xsl:value-of select="$newline"/>
<!--end resourceTypes above-->

<!--begin contentStandards below-->
<!--determine if any DLESE-IMS educational.scistd or educational.geogstd tags have any content; do this by defining a variables to grab the entire node (rather than doing a string function test that only grabs the 1st tag); this variable (rather than the 1st tag) is then tested for content-->

<!--	variable for DLESE-IMS educational.scistd-->
			<xsl:variable name="allscistd">
				<xsl:for-each select="d:educational/d:scistd">
					<xsl:value-of select="."/>
				</xsl:for-each>
			</xsl:variable>
	
<!--	variable for DLESE-IMS educational.geogstd-->
			<xsl:variable name="allgeogstd">
				<xsl:for-each select="d:educational/d:geogstd">
					<xsl:value-of select="."/>
				</xsl:for-each>
			</xsl:variable>
	
<!--test for the presence of content in either of the DLESE-IMS educational.scistd or geostd tags; do this to prevent the ADN:educational.contentStandards tag from appearing if it doesn't need to-->
			<xsl:if test="string-length($allscistd)>0 or string-length($allgeogstd)>0">
				<xsl:element name="contentStandards">
					<xsl:value-of select="$newline"/> 
				
<!--contentStandard from the DLESE-IMS field of scistd-->
					<xsl:if test="string-length($allscistd)>0">
						<xsl:apply-templates select="d:educational/d:scistd"/> <!--see SCIENCE STD template-->
					</xsl:if>
			
<!--contentStandard from the DLESE-IMS field of geogstd-->
					<xsl:if test="string-length($allgeogstd)>0"> 
						<xsl:apply-templates select="d:educational/d:geogstd"/> <!-- see GEOGRAPHY STD template-->
					</xsl:if> 
	
				</xsl:element>
				<xsl:value-of select="$newline"/>
			</xsl:if> 
<!--end contentStandards above-->

<!--begin interactivityLevel below-->		
<!--can only appear once in DLESE-IMS; so test for content-->
<!--in DLESE-IMS the vocab terms are 0,1,2,3,4 while in ADN they are as shown below with the LOM vocab-->
			<xsl:if test="string-length(d:educational/d:interactivitylevel)>0">
				<xsl:element name="interactivityLevel">
					<xsl:choose>
						<xsl:when test="string(d:educational/d:interactivitylevel)=0">
							<xsl:text>LOM:Very low</xsl:text>
						</xsl:when>
						<xsl:when test="string(d:educational/d:interactivitylevel)=1">
							<xsl:text>LOM:Low</xsl:text>
						</xsl:when>
						<xsl:when test="string(d:educational/d:interactivitylevel)=2">
							<xsl:text>LOM:Medium</xsl:text>
						</xsl:when>
						<xsl:when test="string(d:educational/d:interactivitylevel)=3">
							<xsl:text>LOM:High</xsl:text>
						</xsl:when>
						<xsl:when test="string(d:educational/d:interactivitylevel)=4">
							<xsl:text>LOM:Very high</xsl:text>
						</xsl:when>
					</xsl:choose>
				</xsl:element>
				<xsl:value-of select="$newline"/>
			</xsl:if>
			<xsl:value-of select="$newline"/>
<!--end interactivityLevel above-->

<!--interactivityType-->		
<!--can only appear once in DLESE-IMS; so test for content-->
			<xsl:if test="string-length(d:educational/d:interactivitytype/d:langstring)>0">
				<xsl:element name="interactivityType">
					<xsl:value-of select="concat('LOM:',d:educational/d:interactivitytype/d:langstring)"/>
				</xsl:element>
				<xsl:value-of select="$newline"/>
			</xsl:if>

<!--educational.description-->		
<!--this if test prevents processing DLESE-IMS educational.description if it is null-->
<!--must do this if test to prevent the ADN:educational.description tag from appearing if it doesn't need to-->
			<xsl:if test="string-length(d:educational/d:description/d:langstring)>0">
				<xsl:apply-templates select="d:educational/d:description"/> <!--see DESCRIPTION template-->
				<xsl:value-of select="$newline"/>
			</xsl:if>
			</xsl:element>
			<xsl:value-of select="$newline"/>
<!--end EDUCATIONAL category-->


<!--**********************************************************************************************-->
<!--RIGHTS category-->
			<xsl:element name="rights">
				<xsl:value-of select="$newline"/>

<!--cost-->
				<xsl:element name="cost">
					<xsl:value-of select="concat('DLESE:',d:rights/d:cost/d:langstring)"/>
				</xsl:element>
				<xsl:value-of select="$newline"/>

<!--description-->
				<xsl:apply-templates select="d:rights/d:description"/> <!--see DESCRIPTION template-->
				<xsl:value-of select="$newline"/>

			</xsl:element>
			<xsl:value-of select="$newline"/>
<!--end RIGHTS category-->


<!--**********************************************************************************************-->
<!--RELATION category-->
<!--determine if any DLESE-IMS relation.kind tags have any content; do this by defining a variables to grab the entire node (rather than doing a string function test that only grabs the 1st tag); this variable (rather than the 1st tag) is then tested for content-->

<!--	variable for DLESE-IMS relation.kind-->
			<xsl:variable name="allkind">
				<xsl:for-each select="d:relation/d:kind/d:langstring">
					<xsl:value-of select="."/>
				</xsl:for-each>
			</xsl:variable>
<!--test for the presence of content in the DLESE-IMS relation.kind tags; do this to prevent the ADN:relations tag from 	appearing if it doesn't need to-->
			<xsl:if test="string-length($allkind)>0">
				<xsl:element name="relations">
					<xsl:value-of select="$newline"/>
					<xsl:apply-templates select="d:relation/d:kind"/> <!--see KIND template-->
				</xsl:element>			
				<xsl:value-of select="$newline"/>
			</xsl:if>
<!--end RELATION category-->


<!--**********************************************************************************************-->
<!--GEOSPATIALCOVERAGES category-->
<!--	variable for DLESE-IMS general.place-events-->
			<xsl:variable name="allplace-events">
				<xsl:for-each select="d:general/d:coverage/d:extension/d:place_event_name/d:langstring">
					<xsl:value-of select="."/>
				</xsl:for-each>
			</xsl:variable>

<!--	variable for DLESE-IMS general.boxes-->
			<xsl:variable name="allboxes">
				<xsl:for-each select="d:general/d:coverage/d:extension/d:locations/d:box/d:north">
					<xsl:value-of select="."/>
				</xsl:for-each>
			</xsl:variable>

<!--test for the presence of content in DLESE-IMS general.coverage.boxes and general.place-events; do this to prevent the ADN:general.geospatialCoverages tag from appearing if it doesn't need to-->
			<xsl:if test="string-length($allboxes)>0 or string-length($allplace-events)>0">
				<xsl:element name="geospatialCoverages">
					<xsl:value-of select="$newline"/>
					<xsl:element name="geospatialCoverage">
						<xsl:value-of select="$newline"/>
<!--body -->						
						<xsl:element name="body">
							<xsl:value-of select="$newline"/>
<!--planet -->						
							<xsl:element name="planet">
								<xsl:text>Earth</xsl:text>							
							</xsl:element>
							<xsl:value-of select="$newline"/>
						</xsl:element>
						<xsl:value-of select="$newline"/>

<!--geodeticGlobal or Horizontal -->						
						<xsl:element name="geodeticDatumGlobalOrHorz">
							<xsl:text>DLESE:Unknown</xsl:text>							
						</xsl:element>
						<xsl:value-of select="$newline"/>

<!--projection -->						
						<xsl:element name="projection">
							<xsl:attribute name="type">
								<xsl:text>DLESE:Unknown</xsl:text>							
							</xsl:attribute>
						</xsl:element>
						<xsl:value-of select="$newline"/>
						
<!--coordinateSystem -->						
						<xsl:element name="coordinateSystem">
							<xsl:attribute name="type">
								<xsl:text>DLESE:Geographic latitude and longitude</xsl:text>												</xsl:attribute>
						</xsl:element>
						<xsl:value-of select="$newline"/>

<!--boundBox -->						
						<xsl:element name="boundBox">
							<xsl:value-of select="$newline"/>
							<xsl:apply-templates select="d:general/d:coverage" mode="boundBox"/>
							<!--see COVERAGE template mode=BOUNDBOX-->
						</xsl:element>
						<xsl:value-of select="$newline"/>

<!--detGeos -->						
						<xsl:element name="detGeos">
							<xsl:value-of select="$newline"/>
							<xsl:apply-templates select="d:general/d:coverage" mode="detGeos"/>
							<!--see COVERAGE template mode=DETGEOS-->
						</xsl:element>
						<xsl:value-of select="$newline"/>
						
					</xsl:element>
					<xsl:value-of select="$newline"/>
				</xsl:element>
				<xsl:value-of select="$newline"/>
			</xsl:if>
			
<!--end GEOSPATIALCOVERAGES category-->


<!--**********************************************************************************************-->
<!--TEMPORALCOVERAGES category-->
<!--Since ADN has temporal coverages that encompass, timeAD, timeBC and timeRelative, each must be done separately-->

<!--begin ADN temporalCoverages.timeAD-->
<!--	variable for DLESE-IMS general.coverage.begtime.datecoverage-->
			<xsl:variable name="allbegdates">
				<xsl:for-each select="d:general/d:coverage/d:extension/d:begtime/d:datecoverage">
					<xsl:value-of select="."/>
				</xsl:for-each>
			</xsl:variable>
<!--	variable for DLESE-IMS general.coverage.endtime.datecoverage-->
			<xsl:variable name="allenddates">
				<xsl:for-each select="d:general/d:coverage/d:extension/d:endtime/d:datecoverage">
					<xsl:value-of select="."/>
				</xsl:for-each>
			</xsl:variable>
<!--Since this transform can not deal with the important free text information in the time description fields, grab the data as a variable and throw an error-->
<!--	variable for DLESE-IMS general.coverage.endtime.description.langstring-->
			<xsl:variable name="allbegdescr">
				<xsl:for-each select="d:general/d:coverage/d:extension/d:endtime/d:description/d:langstring">
					<xsl:value-of select="."/>
				</xsl:for-each>
			</xsl:variable>
<!--	variable for DLESE-IMS general.coverage.endtime.description.langstring-->
			<xsl:variable name="allenddescr">
				<xsl:for-each select="d:general/d:coverage/d:extension/d:endtime/d:description/d:langstring">
					<xsl:value-of select="."/>
				</xsl:for-each>
			</xsl:variable>

<!--test for the presence of content in DLESE-IMS general.coverage.begtime (and endtime).datecoverage; do this to prevent the ADN: temporalCoverages tag from appearing if it doesn't need to-->
			<xsl:if test="string-length($allbegdates)>0 or string-length($allenddates)>0">
				<xsl:element name="temporalCoverages">
					<xsl:value-of select="$newline"/>
					<xsl:apply-templates select="d:general/d:coverage/d:extension/d:begtime/d:datecoverage"/>
					<!--see DATECOVERAGE template-->
				</xsl:element>
				<xsl:value-of select="$newline"/>
			</xsl:if>		
<!--end ADN temporalCoverages that does only timeAD-->
<!--beging check on time description information-->
<!--test for the presence of content in DLESE-IMS general.coverage.begtime (and endtime).datecoverage; do this to prevent the ADN: temporalCoverages tag from appearing if it doesn't need to-->
			<xsl:if test="string-length($allbegdescr)>0 or string-length($allenddescr)>0">
				<xsl:element name="TIME">
					<xsl:text>Begin and endtime description information has been found. It has not been transformed. Please check original record for vital data.</xsl:text>
				</xsl:element>
			</xsl:if>		
<!--end check on time description information-->
<!--end TEMPORALCOVERAGES category-->


<!--end itemRecord-->
		<xsl:text disable-output-escaping="yes">&lt;/itemRecord&gt;</xsl:text>
	</xsl:template>


<!--**********************************************************************************************-->
<!--**********************************************************************************************-->
<!--**********************************************************************************************-->
<!--**********************************************************************************************-->
<!--**********************************************************************************************-->
<!--**********************************************************************************************-->

<!--end TEMPLATES TO APPLY. Reference in alpha order-->

<!--CATALOGENTRY template-->
<!--DLESE-IMS records can have null catalogentry.catalogue and catalogentry.entry values; this code deals with that--> 
<!--process only DLESE-IMS catalogentry tags that have both an entry and catalogue tag completed-->
	<xsl:template match="d:catalogentry">
		<xsl:if test="string-length(d:entry/d:langstring)>0">
			<xsl:if test="string-length(d:catalogue)>0">
				<!--complete the ADN catalog tag with attribute entry and element content-->
				<xsl:element name="catalog">
					<xsl:attribute name="entry">
						<xsl:value-of select="d:entry/d:langstring"/>
					</xsl:attribute>
					<xsl:value-of select="d:catalogue"/>
				</xsl:element>
				<xsl:value-of select="$newline"/>
			</xsl:if>
		</xsl:if>
	</xsl:template>

<!--**********************************************************************************************-->
<!--CONTRIBUTE template-->
<!--many DLESE-IMS have contribute.role.langstring=Creator and a date but no name or organization information--> 
<!--if tests here prevent processing of DLESE-IMS contribute tag sets that have only a role and date completed-->
<!--if tests split the DLESE-IMS contribute tags into persons and organizations-->
	<xsl:template match="d:contribute">
		<xsl:if test="string-length(d:role/d:langstring)>0">
			<xsl:choose>
				<xsl:when test="string-length(d:centity/d:extension/d:lastname)>0">
					<!--complete the ADN contributor tag-->
					<xsl:apply-templates select="d:centity"/> <!--see CENTITY template-->
				</xsl:when>
				<xsl:when test="string-length(d:centity/d:extension/d:org)>0">
					<!--complete the ADN contributor tag-->
					<xsl:apply-templates select="d:centity"/><!--see CENTITY template-->
				</xsl:when>
				<xsl:otherwise/>
			</xsl:choose>
		</xsl:if>
	</xsl:template>

<!--**********************************************************************************************-->
<!--COVERAGE template mode=BOUNDBOX-->
	<xsl:template match="d:general/d:coverage" mode="boundBox">
		<xsl:choose>
			<xsl:when test="d:extension/d:place_event_name/d:langstring[contains(.,'BB:')]">

<!--ADN boundBox.westCoord-->
				<xsl:element name="westCoord">
					<xsl:value-of select="d:extension/d:locations/d:box/d:west"/>
				</xsl:element>
				<xsl:value-of select="$newline"/>
<!--ADN boundBox.eastCoord-->
				<xsl:element name="eastCoord">
					<xsl:value-of select="d:extension/d:locations/d:box/d:east"/>
				</xsl:element>
				<xsl:value-of select="$newline"/>
<!--ADN boundBox.northCoord-->
				<xsl:element name="northCoord">
					<xsl:value-of select="d:extension/d:locations/d:box/d:north"/>
				</xsl:element>
				<xsl:value-of select="$newline"/>
<!--ADN boundBox.southCoord-->
				<xsl:element name="southCoord">
					<xsl:value-of select="d:extension/d:locations/d:box/d:south"/>
				</xsl:element>
				<xsl:value-of select="$newline"/>
<!--ADN bbSrcName-->
				<xsl:element name="bbSrcName">
					<xsl:text>Cataloger supplied</xsl:text>
				</xsl:element>
				<xsl:value-of select="$newline"/>
<!--begin ADN bbVert below-->
				<xsl:if test="string-length(d:extension/d:locations/d:box/d:min_vertical)>0">
					<xsl:element name="bbVert">
						<xsl:value-of select="$newline"/>
						<xsl:apply-templates select="d:extension/d:locations/d:box/d:min_vertical"/>
						<!--see VERTICAL template-->
					</xsl:element>
					<xsl:value-of select="$newline"/>
				</xsl:if>
<!--end ADN bbVert above-->

<!--begin ADN bbPlaces and bbEvents below-->
<!--	variable for DLESE-IMS the current coverage's places-->
<!--	variable for DLESE-IMS the current coverage's events-->
				<xsl:variable name="cov-places">
					<xsl:for-each select="d:extension/d:place_event_name">
						<xsl:value-of select="d:langstring[contains(.,'PLACE')]"/>
					</xsl:for-each>
				</xsl:variable>
				<xsl:variable name="cov-events">
					<xsl:for-each select="d:extension/d:place_event_name">
						<xsl:value-of select="d:langstring[contains(.,'EVENT:')]"/>
					</xsl:for-each>
				</xsl:variable>
<!--bbPlaces-->
				<xsl:if test="string-length($cov-places)>0">
					<xsl:element name="bbPlaces">
						<xsl:value-of select="$newline"/>
						<xsl:apply-templates select="d:extension/d:place_event_name" mode="places"/>
						<!--see PLACES-EVENTS template mode=PLACES-->
					</xsl:element>
					<xsl:value-of select="$newline"/>
				</xsl:if>
<!--bbEvents-->
				<xsl:if test="string-length($cov-events)>0">
					<xsl:element name="bbEvents">
						<xsl:value-of select="$newline"/>
						<xsl:apply-templates select="d:extension/d:place_event_name" mode="events"/>
						<!--see PLACES-EVENTS template mode=EVENTS-->
					</xsl:element>
					<xsl:value-of select="$newline"/>
				</xsl:if>
<!--bbPlaces or Events error for those place-event names without the proper indicator labels-->
				<xsl:if test="string-length($cov-events)>0">
					<xsl:apply-templates select="d:extension/d:place_event_name" mode="other"/>
						<!--see PLACES-EVENTS template mode=OTHER-->
					<xsl:value-of select="$newline"/>
				</xsl:if>


<!--end ADN bbPlaces and bbEvents above-->

			</xsl:when>
		</xsl:choose>
<!--end boundBox above-->
	</xsl:template>

<!--**********************************************************************************************-->
<!--COVERAGE template mode=DETGEOS-->
	<xsl:template match="d:general/d:coverage" mode="detGeos">
<!--in order to find those coverage tags that have just a place and event name and no bounding box; need to use a varibable to get the contents of the DLESE-IMS place_event_names tag and use with north in a comparison-->
<!--	variable for DLESE-IMS the current coverage's places and events-->
				<xsl:variable name="cov-places-events">
					<xsl:for-each select="d:extension/d:place_event_name/d:langstring">
						<xsl:value-of select="."/>
					</xsl:for-each>
				</xsl:variable>
<!--process those coverage tags that have content in DLESE-IMS north or content in DLESE-IMS place_name_event-->
		<xsl:if test="string-length(d:extension/d:locations/d:box/d:north)>0 or string-length($cov-places-events)>0">
			<xsl:element name="detGeo">
				<xsl:choose>
<!-- a point -->
					<xsl:when test="d:extension/d:place_event_name/d:langstring[contains(.,'PT:')]">
						<xsl:element name="typeDetGeo">
							<xsl:text>Point</xsl:text>
						</xsl:element>
						<xsl:element name="geoNumPts">
							<xsl:text>1</xsl:text>
						</xsl:element>
						<xsl:element name="geoPtOrder">
							<xsl:text>None because the detailed geometry is a point or polyline or bounding box</xsl:text>
						</xsl:element>
						<xsl:element name="longLats">
							<xsl:element name="longLat">
								<xsl:attribute name="longitude">
									<xsl:value-of select="d:extension/d:locations/d:box/d:west"/>
								</xsl:attribute>
								<xsl:attribute name="latitude">
									<xsl:value-of select="d:extension/d:locations/d:box/d:north"/>
								</xsl:attribute>
							</xsl:element>
						</xsl:element>
					</xsl:when>

<!-- an east-west line -->
					<xsl:when test="d:extension/d:place_event_name/d:langstring[contains(.,'LNEW:')]">
						<xsl:element name="typeDetGeo">
							<xsl:text>Polyline</xsl:text>
						</xsl:element>
						<xsl:element name="geoNumPts">
							<xsl:text>2</xsl:text>
						</xsl:element>
						<xsl:element name="geoPtOrder">
							<xsl:text>None because the detailed geometry is a point or polyline or bounding box</xsl:text>
						</xsl:element>
						<xsl:element name="longLats">
							<xsl:element name="longLat">
								<xsl:attribute name="longitude">
									<xsl:value-of select="d:extension/d:locations/d:box/d:west"/>
								</xsl:attribute>
								<xsl:attribute name="latitude">
									<xsl:value-of select="d:extension/d:locations/d:box/d:north"/>
								</xsl:attribute>
							</xsl:element>
							<xsl:element name="longLat">
								<xsl:attribute name="longitude">
									<xsl:value-of select="d:extension/d:locations/d:box/d:east"/>
								</xsl:attribute>
								<xsl:attribute name="latitude">
									<xsl:value-of select="d:extension/d:locations/d:box/d:north"/>
								</xsl:attribute>
							</xsl:element>
						</xsl:element>
					</xsl:when>

<!-- a north-south line -->
					<xsl:when test="d:extension/d:place_event_name/d:langstring[contains(.,'LNNS:')]">
						<xsl:element name="typeDetGeo">
							<xsl:text>Polyline</xsl:text>
						</xsl:element>
						<xsl:element name="geoNumPts">
							<xsl:text>2</xsl:text>
						</xsl:element>
						<xsl:element name="geoPtOrder">
							<xsl:text>None because the detailed geometry is a point or polyline or bounding box</xsl:text>
						</xsl:element>
						<xsl:element name="longLats">
							<xsl:element name="longLat">
								<xsl:attribute name="longitude">
									<xsl:value-of select="d:extension/d:locations/d:box/d:west"/>
								</xsl:attribute>
								<xsl:attribute name="latitude">
									<xsl:value-of select="d:extension/d:locations/d:box/d:north"/>
								</xsl:attribute>
							</xsl:element>
							<xsl:element name="longLat">
								<xsl:attribute name="longitude">
									<xsl:value-of select="d:extension/d:locations/d:box/d:west"/>
								</xsl:attribute>
								<xsl:attribute name="latitude">
									<xsl:value-of select="d:extension/d:locations/d:box/d:south"/>
								</xsl:attribute>
							</xsl:element>
						</xsl:element>
					</xsl:when>

<!-- a bounding box -->
					<xsl:otherwise>
						<xsl:element name="typeDetGeo">
							<xsl:text>Bounding box</xsl:text>
						</xsl:element>
						<xsl:element name="geoNumPts">
							<xsl:text>4</xsl:text>
						</xsl:element>
						<xsl:element name="geoPtOrder">
							<xsl:text>None because the detailed geometry is a point or polyline or bounding box</xsl:text>
						</xsl:element>
						<xsl:element name="longLats">
							<xsl:element name="longLat">
								<xsl:attribute name="longitude">
									<xsl:value-of select="d:extension/d:locations/d:box/d:east"/>
								</xsl:attribute>
								<xsl:attribute name="latitude">
									<xsl:value-of select="d:extension/d:locations/d:box/d:north"/>
								</xsl:attribute>
							</xsl:element>
							<xsl:element name="longLat">
								<xsl:attribute name="longitude">
									<xsl:value-of select="d:extension/d:locations/d:box/d:east"/>
								</xsl:attribute>
								<xsl:attribute name="latitude">
									<xsl:value-of select="d:extension/d:locations/d:box/d:south"/>
								</xsl:attribute>
							</xsl:element>
							<xsl:element name="longLat">
								<xsl:attribute name="longitude">
									<xsl:value-of select="d:extension/d:locations/d:box/d:west"/>
								</xsl:attribute>
								<xsl:attribute name="latitude">
									<xsl:value-of select="d:extension/d:locations/d:box/d:south"/>
								</xsl:attribute>
							</xsl:element>
							<xsl:element name="longLat">
								<xsl:attribute name="longitude">
									<xsl:value-of select="d:extension/d:locations/d:box/d:west"/>
								</xsl:attribute>
								<xsl:attribute name="latitude">
									<xsl:value-of select="d:extension/d:locations/d:box/d:north"/>
								</xsl:attribute>
							</xsl:element>
						</xsl:element>
					</xsl:otherwise>
				</xsl:choose>
	
<!--ADN detSrcName-->
				<xsl:element name="detSrcName">
					<xsl:text>Cataloger supplied</xsl:text>
				</xsl:element>
				<xsl:value-of select="$newline"/>

<!--begin ADN detVert below-->
				<xsl:if test="string-length(d:extension/d:locations/d:box/d:min_vertical)>0">
					<xsl:element name="detVert">
						<xsl:value-of select="$newline"/>
						<xsl:apply-templates select="d:extension/d:locations/d:box/d:min_vertical"/>
						<!--see VERTICAL template-->
					</xsl:element>
					<xsl:value-of select="$newline"/>
				</xsl:if>
<!--end ADN detVert above-->

<!--begin ADN detPlaces and detEvents below-->
<!--	variable for DLESE-IMS the current coverage's places-->
<!--	variable for DLESE-IMS the current coverage's events-->
				<xsl:variable name="cov-places">
					<xsl:for-each select="d:extension/d:place_event_name">
						<xsl:value-of select="d:langstring[contains(.,'PLACE')]"/>
					</xsl:for-each>
				</xsl:variable>
				<xsl:variable name="cov-events">
					<xsl:for-each select="d:extension/d:place_event_name">
						<xsl:value-of select="d:langstring[contains(.,'EVENT:')]"/>
					</xsl:for-each>
				</xsl:variable>
				<xsl:variable name="cov-all">
					<xsl:for-each select="d:extension/d:place_event_name">
						<xsl:value-of select="d:langstring"/>
					</xsl:for-each>
				</xsl:variable>
<!--detPlaces-->
				<xsl:if test="string-length($cov-places)>0">
					<xsl:element name="detPlaces">
						<xsl:value-of select="$newline"/>
						<xsl:apply-templates select="d:extension/d:place_event_name" mode="places"/>
						<!--see PLACES-EVENTS template mode=PLACES-->
					</xsl:element>
					<xsl:value-of select="$newline"/>
				</xsl:if>
<!--detEvents-->
				<xsl:if test="string-length($cov-events)>0">
					<xsl:element name="detEvents">
						<xsl:value-of select="$newline"/>
						<xsl:apply-templates select="d:extension/d:place_event_name" mode="events"/>
						<!--see PLACES-EVENTS template mode=EVENTS-->
					</xsl:element>
					<xsl:value-of select="$newline"/>
				</xsl:if>
<!--general detEvents and detPlaces errors if no geospatial indicator labels are used-->
				<xsl:if test="string-length($cov-all)>0">
					<xsl:apply-templates select="d:extension/d:place_event_name" mode="other"/>
						<!--see PLACES-EVENTS template mode=OTHER-->
					<xsl:value-of select="$newline"/>
				</xsl:if>
<!--end ADN detPlaces and detEvents above-->

<!-- -->
			</xsl:element>
		</xsl:if>
<!--end detGeo above-->
	</xsl:template>

<!--**********************************************************************************************-->
<!--CENTITY template: additional processing template for CONTRIBUTE template-->
	<xsl:template match="d:centity">
		<!--ADN:contributor with the attributes of role and date-->
		<xsl:element name="contributor">
			<xsl:attribute name="role">
				<xsl:value-of select="../d:role/d:langstring"/>
			</xsl:attribute>
			<xsl:if test="string-length(../d:date/d:datetime)>0">
				<xsl:attribute name="date">
					<xsl:value-of select="../d:date/d:datetime"/>
				</xsl:attribute>
			</xsl:if>
			<xsl:value-of select="$newline"/>
			<!--choose either person or organization as the appropriate child tags to ADN:contributor-->
			<!--this is for a person-->
			<xsl:choose>
				<xsl:when test="string-length(d:extension/d:lastname)>0">
					<xsl:element name="person">
					<xsl:value-of select="$newline"/>
						<!--ADN:nameTitle-->
						<xsl:if test="string-length(d:extension/d:nametitle)>0">
							<xsl:element name="nameTitle">
								<xsl:value-of select="d:extension/d:nametitle"/>
							</xsl:element>
							<xsl:value-of select="$newline"/>
						</xsl:if>
						<!--ADN:nameFirst-->
						<xsl:if test="string-length(d:extension/d:firstname)>0">
							<xsl:element name="nameFirst">
								<xsl:value-of select="d:extension/d:firstname"/>
							</xsl:element>
							<xsl:value-of select="$newline"/>
						</xsl:if>
						<!--ADN:nameMiddle-->
						<xsl:if test="string-length(d:extension/d:mi)>0">
							<xsl:element name="nameMiddle">
								<xsl:value-of select="d:extension/d:mi"/>
							</xsl:element>
							<xsl:value-of select="$newline"/>
						</xsl:if>
						<!--ADN:nameLast-->
						<xsl:if test="string-length(d:extension/d:lastname)>0">
							<xsl:element name="nameLast">
								<xsl:value-of select="d:extension/d:lastname"/>
							</xsl:element>
							<xsl:value-of select="$newline"/>
						</xsl:if>
						<!--ADN:instName-->
						<xsl:element name="instName">
							<xsl:choose>
								<xsl:when test="string-length(d:extension/d:org)>0">
									<xsl:value-of select="d:extension/d:org"/>
								</xsl:when>
								<xsl:otherwise>
									<xsl:text>No institutional affiliation is known</xsl:text>
								</xsl:otherwise>
							</xsl:choose>
						</xsl:element>
						<xsl:value-of select="$newline"/>
						<!--ADN:emailPrimary-->
						<xsl:element name="emailPrimary">
							<xsl:choose>
								<xsl:when test="string-length(d:extension/d:email)>0">
									<xsl:value-of select="d:extension/d:email"/>
								</xsl:when>
								<xsl:otherwise>
									<xsl:text>Unknown</xsl:text>
								</xsl:otherwise>									
							</xsl:choose>
						</xsl:element>
						<xsl:value-of select="$newline"/>
					</xsl:element>
					<xsl:value-of select="$newline"/>
				</xsl:when>
				<!--if not a person, it is assumed the tags represent an organization; no specific check is done to see if this assumption is true-->
				<!--this is for an organization-->
				<xsl:otherwise>
					<xsl:element name="organization">
						<xsl:value-of select="$newline"/>
						<!--ADN:instName-->
						<xsl:if test="string-length(d:extension/d:org)>0">
							<xsl:element name="instName">
								<xsl:value-of select="d:extension/d:org"/>
							</xsl:element>
							<xsl:value-of select="$newline"/>
						</xsl:if>
						<!--ADN:instEmail-->
						<xsl:if test="string-length(d:extension/d:email)>0">
							<xsl:element name="instEmail">
								<xsl:value-of select="d:extension/d:email"/>
							</xsl:element>
							<xsl:value-of select="$newline"/>
						</xsl:if>
						<!--ADN:instUrl-->
						<xsl:if test="string-length(d:extension/d:url)>0">
							<xsl:element name="instUrl">
								<xsl:value-of select="d:extension/d:url"/>
							</xsl:element>
							<xsl:value-of select="$newline"/>
						</xsl:if>
					</xsl:element>
					<xsl:value-of select="$newline"/>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:element>
		<xsl:value-of select="$newline"/>
	</xsl:template>

<!--**********************************************************************************************-->
<!--DATECOVERAGE template for temporal information: ADN contents of timeAndPeriod below-->
	<xsl:template match="d:datecoverage">
		<xsl:if test="string-length (.)>0 or string-length(../d:time)>0">
			<xsl:element name="timeAndPeriod">
				<xsl:value-of select="$newline"/>
				<xsl:element name="timeInfo">
					<xsl:value-of select="$newline"/>
					<xsl:element name="timeAD">
						<xsl:value-of select="$newline"/>
<!--begin ADN begin date-->
						<xsl:element name="begin">
<!--determine if the value 9999 is present and convert it to the word 'Present'-->
							<xsl:attribute name="date">
								<xsl:choose>
									<xsl:when test=".=9999">
										<xsl:text>Present</xsl:text>
									</xsl:when>
									<xsl:otherwise>
										<xsl:value-of select="."/>
									</xsl:otherwise>
								</xsl:choose>
							</xsl:attribute>
							<xsl:if test="string-length(../d:time)>0">
								<xsl:attribute name="time">
									<xsl:value-of select="../d:time"/>
								</xsl:attribute>
							</xsl:if>
						</xsl:element>
						<xsl:value-of select="$newline"/>
<!--end ADN begin date-->
<!--begin ADN end date-->						
						<xsl:element name="end">
<!--determine if the value 9999 is present and convert it to the word 'Present'-->
							<xsl:attribute name="date">
								<xsl:choose>
									<xsl:when test="../../d:endtime/d:datecoverage=9999">
										<xsl:text>Present</xsl:text>
									</xsl:when>
									<xsl:otherwise>
										<xsl:value-of select="../../d:endtime/d:datecoverage"/>
									</xsl:otherwise>
								</xsl:choose>
							</xsl:attribute>
							<xsl:if test="string-length(../../d:endtime/d:time)>0">
								<xsl:attribute name="time">
									<xsl:value-of select="../../d:endtime/d:time"/>
								</xsl:attribute>
							</xsl:if>
						</xsl:element>
					<xsl:value-of select="$newline"/>
<!--end ADN end date-->
					</xsl:element> <!--end timeAD-->
					<xsl:value-of select="$newline"/>
				</xsl:element> <!--end timeInfo-->
				<xsl:value-of select="$newline"/>
			</xsl:element> <!--end timeAndPeriod-->
			<xsl:value-of select="$newline"/>
		</xsl:if>
	</xsl:template>

<!--**********************************************************************************************-->
<!--DESCRIPTION template (for general and rights)-->	
	<xsl:template match="d:description">
		<xsl:element name="description">
			<xsl:value-of select="d:langstring"/>
		</xsl:element>
	</xsl:template>

<!--**********************************************************************************************-->
<!--FORMAT template-->
<!--do an if test to rid null tags-->
	<xsl:template match="d:format">
		<xsl:if test="string-length(d:langstring)>0">
			<xsl:element name="medium">
				<xsl:value-of select="d:langstring"/>
			</xsl:element>
			<xsl:value-of select="$newline"/>
		</xsl:if>
	</xsl:template>

<!--**********************************************************************************************-->
<!--GEOGRAPHY STD template-->
<!--do an if test to rid null tags-->
	<xsl:template match="d:geogstd">
		<xsl:if test="string-length(.)>0">
			<xsl:element name="contentStandard">
<!--between DLESE-IMS and ADN there is a typo error in one of the geography standards; The choose/when tests cause the typo to be corrected.-->
				<xsl:choose>
					<xsl:when test="contains(.,'mosaic')">
						<xsl:value-of select="concat('NCGE:', substring-before(.,'mosaic'), 'mosaics')"/>
					</xsl:when>
					<xsl:otherwise>
						<xsl:value-of select="concat('NCGE:',.)"/>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:element>
			<xsl:value-of select="$newline"/>
		</xsl:if>
	</xsl:template>

<!--**********************************************************************************************-->
<!--KEYWORDS template-->
<!--do an if test to rid null tags-->
	<xsl:template match="d:keywords">
		<xsl:if test="string-length(d:langstring)>0">
			<xsl:element name="keyword">
				<xsl:value-of select="d:langstring"/>
			</xsl:element>
			<xsl:value-of select="$newline"/>
		</xsl:if> 
	</xsl:template>

<!--**********************************************************************************************-->
<!--KIND template-->
	<xsl:template match="d:kind">
		<xsl:if test="string-length(d:langstring)>0">
			<xsl:element name="relation">
				<xsl:value-of select="$newline"/>
				<xsl:choose>
					<xsl:when test="string-length(../d:resource/d:extension/d:location)>0">
						<xsl:element name="urlEntry">
							<xsl:attribute name="kind">
								<xsl:apply-templates select="d:langstring" mode="kind"/><!--see LANGSTRING/KIND-->
							</xsl:attribute>
							<xsl:attribute name="url">
								<xsl:value-of select="string(../d:resource/d:extension/d:location)"/>
							</xsl:attribute>
						</xsl:element>
						<xsl:value-of select="$newline"/>
					</xsl:when>
					<xsl:when test="string-length(../d:resource/d:extension/d:catalogentry/d:entry/d:langstring)>0">
						<xsl:element name="idEntry">
							<xsl:attribute name="kind">
								<xsl:apply-templates select="d:langstring" mode="kind"/><!--see LANGSTRING/KIND-->
							</xsl:attribute>
							<xsl:attribute name="entry">
								<xsl:value-of select="string(../d:resource/d:extension/d:catalogentry/d:entry/d:langstring)"/>
							</xsl:attribute>
						</xsl:element>
						<xsl:value-of select="$newline"/>
					</xsl:when>
				</xsl:choose>
			</xsl:element>
		</xsl:if>
		<xsl:value-of select="$newline"/>
	</xsl:template>

<!--**********************************************************************************************-->
<!--LANGSTRING template-->
	<xsl:template match="d:title|d:version">
			<xsl:value-of select="d:langstring"/>
	</xsl:template> 

<!--**********************************************************************************************-->
<!--LANGSTRING template mode=KIND-->
	<xsl:template match="d:langstring" mode="kind">
		<xsl:choose>
			<xsl:when test="string(.)='IsPartOf'">
				<xsl:text>DC:Is part of</xsl:text>
			</xsl:when>
			<xsl:when test="string(.)='HasPart'">
				<xsl:text>DC:Has part</xsl:text>
			</xsl:when>
			<xsl:when test="string(.)='IsVersionOf'">
				<xsl:text>DC:Is version of</xsl:text>
			</xsl:when>
			<xsl:when test="string(.)='HasVersion'">
				<xsl:text>DC:Has version</xsl:text>
			</xsl:when>
			<xsl:when test="string(.)='IsFormatOf'">
				<xsl:text>DC:Is format of</xsl:text>
			</xsl:when>
			<xsl:when test="string(.)='HasFormat'">
				<xsl:text>DC:Has format</xsl:text>
			</xsl:when>
			<xsl:when test="string(.)='References'">
				<xsl:text>DC:References</xsl:text>
			</xsl:when>
			<xsl:when test="string(.)='IsReferencedBy'">
				<xsl:text>DC:Is referenced by</xsl:text>
			</xsl:when>
			<xsl:when test="string(.)='Requires'">
				<xsl:text>DC:Requires</xsl:text>
			</xsl:when>
			<xsl:when test="string(.)='IsRequiredBy'">
				<xsl:text>DC:Is required by</xsl:text>
			</xsl:when>
			<xsl:when test="string(.)='IsReplacedBy'">
				<xsl:text>DC:Is replaced by</xsl:text>
			</xsl:when>
			<xsl:when test="string(.)='Replaces'">
				<xsl:text>DC:Replaces</xsl:text>
			</xsl:when>
			<xsl:when test="string(.)='HasThumbnail'">
				<xsl:text>DLESE:Has thumbnail</xsl:text>
			</xsl:when>
			<xsl:when test="string(.)='IsBasedOn'">
				<xsl:text>DLESE:Is based on</xsl:text>
			</xsl:when>
			<xsl:when test="string(.)='IsBasisFor'">
				<xsl:text>DLESE:Is basis for</xsl:text>
			</xsl:when>
		</xsl:choose> 
	</xsl:template>

<!--**********************************************************************************************-->
<!--LANGUAGE template (for general and metaMetadata)-->
	<xsl:template match="d:language">
		<xsl:element name="language">
			<xsl:value-of select="."/>
		</xsl:element>
	</xsl:template>

<!--**********************************************************************************************-->
<!--LEARNINGCONTEXT template-->
<!--do an if test to rid null tags-->
	<xsl:template match="d:learningcontext">
		<xsl:if test="string-length(d:langstring)>0">
			<xsl:element name="audience">
			<xsl:value-of select="$newline"/>
				<xsl:element name="gradeRange">
					<xsl:value-of select="concat('DLESE:',d:langstring)"/>
				</xsl:element>
				<xsl:value-of select="$newline"/>
			</xsl:element>
			<xsl:value-of select="$newline"/>
		</xsl:if>
	</xsl:template>

<!--**********************************************************************************************-->
<!--MIRROR-URL template-->
	<xsl:template match="d:location[position()>1]">
		<xsl:if test="string-length(.)>0">
			<xsl:element name="mirrorURL">
				<xsl:value-of select="."/>
			</xsl:element>
			<xsl:value-of select="$newline"/>
		</xsl:if> 
	</xsl:template>

<!--**********************************************************************************************-->
<!--PLACE-EVENT template mode=EVENTS for ADN contents of bbEvents and detEvents below-->
	<xsl:template match="d:extension/d:place_event_name" mode="events">
		<xsl:if test="d:langstring[contains(.,'EVENT:')]">
			<xsl:element name="event">
				<xsl:value-of select="$newline"/>
				<xsl:element name="name">
					<xsl:value-of select="substring-after(d:langstring,'EVENT:')"/>
				</xsl:element>
				<xsl:value-of select="$newline"/>
				<xsl:element name="source">
					<xsl:text>Unknown</xsl:text>
				</xsl:element>
				<xsl:value-of select="$newline"/>
			</xsl:element>
		</xsl:if>
	</xsl:template>

<!--**********************************************************************************************-->
<!--PLACE-EVENT template mode=OTHER for place-event error information below-->
	<xsl:template match="d:extension/d:place_event_name" mode="other">
<!--weed out blank entries-->
		<xsl:if test="string-length(d:langstring)>0">
			<xsl:choose>
				<xsl:when test="d:langstring[contains(.,'EVENT:')]">
				</xsl:when>
				<xsl:when test="d:langstring[contains(.,'PLACE:')]">
				</xsl:when>
				<xsl:when test="d:langstring[contains(.,'BB:')]">
				</xsl:when>
				<xsl:when test="d:langstring[contains(.,'PT:')]">
				</xsl:when>
				<xsl:when test="d:langstring[contains(.,'LNEW:')]">
				</xsl:when>
				<xsl:when test="d:langstring[contains(.,'LNNS:')]">
				</xsl:when>
				<xsl:otherwise>
					<xsl:element name="ERROR">
						<xsl:text>Transform found place-event data without PLACE or EVENT labels</xsl:text>
					</xsl:element>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:if>
	</xsl:template>

<!--**********************************************************************************************-->
<!--PLACE-EVENT template mode=PLACES for ADN contents of bbPlaces and detPlaces below-->
	<xsl:template match="d:extension/d:place_event_name" mode="places">
		<xsl:if test="d:langstring[contains(.,'PLACE:')]">
			<xsl:element name="place">
				<xsl:value-of select="$newline"/>
				<xsl:element name="name">
					<xsl:value-of select="substring-after(d:langstring,'PLACE:')"/>
				</xsl:element>
				<xsl:value-of select="$newline"/>
				<xsl:element name="source">
					<xsl:text>Unknown</xsl:text>
				</xsl:element>
				<xsl:value-of select="$newline"/>
			</xsl:element>
		</xsl:if>
	</xsl:template>

<!--**********************************************************************************************-->
<!--REQUIREMENTS template-->
<!--do an if test to rid null tags-->
	<xsl:template match="d:requirements">
		<xsl:if test="string-length(d:typename)>0">
			<xsl:element name="requirement">
				<xsl:value-of select="$newline"/>
				<xsl:element name="reqType">
<!--between DLESE-IMS and ADN the word 'plug in'	was changed to 'plug-in'; this choose/when test causes the change to occur-->
					<xsl:choose>
						<xsl:when test="d:typename[contains(.,'plug in')]">
							<xsl:value-of select="concat('DLESE:Software or plug-in:',substring-after(d:typename,'plug in:'))"/>
						</xsl:when>
						<xsl:otherwise>
							<xsl:value-of select="concat('DLESE:',d:typename)"/>					
						</xsl:otherwise>
					</xsl:choose>
				</xsl:element>
				<xsl:value-of select="$newline"/>
				
<!--minimum version-->
<!--do an if test to rid null tags-->
				<xsl:if test="string-length(d:minimumversion)>0">
					<xsl:element name="minimumVersion">
						<xsl:value-of select="d:minimumversion"/>
					</xsl:element>
				</xsl:if>
				
<!--maximum version-->
<!--do an if test to rid null tags-->
				<xsl:if test="string-length(d:maximumversion)>0">
					<xsl:element name="maximumVersion">
						<xsl:value-of select="d:maximumversion"/>
					</xsl:element>
				</xsl:if>
			</xsl:element>
			<xsl:value-of select="$newline"/>
		</xsl:if>
	</xsl:template>

<!--**********************************************************************************************-->
<!--RESOURCETYPE template-->
<!--for simplicity, assumes collection builders use a DCS or have completed the learningresourcetype.langstring field-->
<!--do an if test to rid null tags-->
	<xsl:template match="d:learningresourcetype">
		<xsl:if test="string-length(d:extension/d:category//d:langstring)>0">
			<xsl:element name="resourceType">
<!--between DLESE-IMS and ADN the phrase 'Learning Materials' was changed to 'Learning materials'; this choose/when test causes this change to occur-->
<!--assumes learningresourcetype.langstring rather than learningresourctype.extension...langstring has the node content-->
<!--the resource type of Text:Annotation is in DLESE-IMS but not in ADN; this test accounts for that-->
				<xsl:choose>
					<xsl:when test="d:langstring[contains(.,'Materials')]">
						<xsl:value-of select="concat('DLESE:Learning materials:',substring-after(d:langstring,'Materials:'))"/>
					</xsl:when>
					<xsl:when test="d:langstring[contains(.,'Annotation')]">
						<xsl:text>Annotation as a resource type is not allowed</xsl:text>
					</xsl:when>
					<xsl:otherwise>
						<xsl:value-of select="concat('DLESE:',d:langstring)"/>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:element>
			<xsl:value-of select="$newline"/>
		</xsl:if>
	</xsl:template>

<!--**********************************************************************************************-->
<!--SCIENCE STD template-->
<!--do an if test to rid null tags-->
	<xsl:template match="d:scistd">
		<xsl:if test="string-length(.)>0">
			<xsl:element name="contentStandard">
<!--between DLESE-IMS and ADN all the phrases of the NSES vocab were changed to add back the dash between the grade range numbers. This means K 4 becomes K-4; 5 8 becomes 5-8 and 9 12 becomes 9-12. this choose/when test causes these changes to occur-->
<!--between DLESE-IMS and ADN there are 4 typos in the NSES vocab. The choose/when tests within a grade range test causes the typos to be corrected.-->
				<xsl:choose>
					<xsl:when test="contains(.,'K 4:')">
						<xsl:value-of select="concat('NSES:K-4:',substring-after(.,'K 4:'))"/>
					</xsl:when>
					<xsl:when test="contains(.,'5 8:')">
						<xsl:choose>
<!--correct Understanding to Understandings in std E; be sure to choose a unique substring in order to grad the correct standard-->	
							<xsl:when test="contains(.,'E Science and Technology Standards:Understanding')">
								<xsl:value-of select="concat('NSES:5-8:Content Standard E Science and Technology Standards:Understandings',substring-after(.,'Understanding'))"/>
							</xsl:when>
<!--correct Risk to Risks in std F; be sure to choose a unique substring in order to grad the correct standard-->
							<xsl:when test="contains(.,'F Science in Personal and Social Perspectives Standards:Risk')">
								<xsl:value-of select="concat('NSES:5-8:Content Standard F Science in Personal and Social Perspectives Standards:Risks',substring-after(.,'Risk'))"/>
							</xsl:when>
							<xsl:otherwise>
								<xsl:value-of select="concat('NSES:5-8:',substring-after(.,'5 8:'))"/>
							</xsl:otherwise>
						</xsl:choose>
					</xsl:when>
					<xsl:when test="contains(.,'9 12:')">
						<xsl:choose>
<!--correct Understanding to Understandings in std A; be sure to choose a unique substring in order to grad the correct standard-->	
							<xsl:when test="contains(.,'A Science as Inquiry Standards:Understanding' )">
								<xsl:value-of select="concat('NSES:9-12:Content Standard A Science as Inquiry Standards:Understandings',substring-after(.,'Understanding'))"/>
							</xsl:when>
<!--correct Understanding to Understandings in std E; be sure to choose a unique substring in order to grad the correct standard-->
							<xsl:when test="contains(.,'E Science and Technology Standards:Understanding')">
								<xsl:value-of select="concat('NSES:9-12:Content Standard E Science and Technology Standards:Understandings',substring-after(.,'Understanding'))"/>
							</xsl:when>
							<xsl:otherwise>
								<xsl:value-of select="concat('NSES:9-12:',substring-after(.,'9 12:'))"/>
							</xsl:otherwise>
						</xsl:choose>
					</xsl:when>
				</xsl:choose>
			</xsl:element>
			<xsl:value-of select="$newline"/>
		</xsl:if>
	</xsl:template>

<!--**********************************************************************************************-->
<!--TOPIC template-->
<!--do an if test to rid null tags-->
	<xsl:template match="d:topic">
		<xsl:if test="string-length(d:langstring)>0">
			<xsl:element name="subject">
				<xsl:choose>
					<xsl:when test="contains(.,'None')">
						<xsl:text>DLESE:Other</xsl:text>
					</xsl:when>
					<xsl:otherwise>
						<xsl:value-of select="concat('DLESE:',d:langstring)"/>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:element>
			<xsl:value-of select="$newline"/>
		</xsl:if>
	</xsl:template> 

<!--**********************************************************************************************-->
<!--VERTICAL template for bbVert and detVert below-->
	<xsl:template match="d:min_vertical">
		<xsl:element name="geodeticDatumGlobalOrVert">
			<xsl:text>DLESE:Sea level</xsl:text>
		</xsl:element>
		<xsl:value-of select="$newline"/>
<!--ADN vertBase-->
		<xsl:element name="vertBase">
			<xsl:text>Ground level</xsl:text>
		</xsl:element>
		<xsl:value-of select="$newline"/>
<!--ADN vertMin-->
		<xsl:element name="vertMin">
			<xsl:attribute name="units">
				<xsl:text>meters (m)</xsl:text>
			</xsl:attribute>
			<xsl:value-of select="."/>
		</xsl:element>
		<xsl:value-of select="$newline"/>
<!--ADN vertMax-->
		<xsl:element name="vertMax">
			<xsl:attribute name="units">
				<xsl:text>meters (m)</xsl:text>
			</xsl:attribute>
			<xsl:value-of select="../d:max_vertical"/>
		</xsl:element>
	</xsl:template>

</xsl:stylesheet>
