
     
/// FORMAT MENU  ///
menuStyle = "3d"                                  // Menu Style (flat, 3d)
cellPadding = "3"                                   // Cell Padding
cellBorder = 1                                      // Border width (for no border, enter 0)  THIS VALUE APPLIES TO ALL MENUS
verticalOffset = "0"                                // Vertical offset of Sub Menu. 
horizontalOffset = "0"                              // Horizontal offset of Sub Menu. 
subMenuDelay = 350                                    // Time sub menu stays visible for (in milliseconds). THIS VALUE APPLIES TO ALL MENUS
subIndicate = 1                                     // Show if a sub menu is present (use 0 for "no")  THIS VALUE APPLIES TO ALL MENUS

// Main Menu Items
menuWidth = "100"                  // Width of menu item.  Use 0 for default
borderColor = "#666666"            // Border Colour (flat mode only)
borderHighlight = "#666666"      // Border Highlight Colour (3d mode only)
borderShadow = "#666666"         // Border Shadow Colour (3d mode only)
menuBackground = "#A8B955"       // Cell Background Colour
menuHoverBackground = "#E7ECCF"    // Cell Background Colour on mouse rollover
fontFace = "arial"               // Font Face
fontColour = "#3f4c02"           // Font Colour
fontHoverColour = "#3f4c02"      // Font Colour on mouse rollover
fontSize = "10pt"                 // Font Size
fontDecoration = "none"          // Style of the link text (none, underline, overline, line-through)
fontWeight = "normal"            // Font Weight (normal, bold)

// Sub Menu Items
smenuWidth = "0"                 // Width of sub menu item.  Use 0 for default
sborderColor = "#666666"           // Border Colour (flat mode only)
sborderHighlight = "#666666"     // Border Highlight Colour (3d mode only)
sborderShadow = "#666666"        // Border Shadow Colour (3d mode only)
smenuBackground = "#BDCA80"        // Cell Background Colour
smenuHoverBackground = "#E7ECCF" // Cell Background Colour on mouse rolloverr
sfontFace = "arial"              // Font Face
sfontColour = "#3f4c02"         // Font Colour
sfontHoverColour = "#3f4c02"     // Font Colour on mouse rollover
sfontSize = "10pt"                // Font Size
sfontDecoration = "none"         // Style of the link text (none, underline, overline, line-through)
sfontWeight = "normal"           // Font Weight (normal, bold)

quantity = 1
/// END FORMAT MENU  ////

/// DO NOT EDIT BELOW THIS LINE  ///
// Browser Sniffer (uses browser.js library)
var isIE6down = browser.isIE6down;
var isIE = (document.getElementById && document.all)?true:false; 
var isNS4 = (document.layers)?true:false;
var isNS6 = (document.getElementById && !document.all)?true:false;
var timer;
var obj = (isIE)?"document.all":"document.getElementById"
// Menu Styles
function createStyles(quant){
styleBorder=(menuStyle.split(",")[quant-1].toLowerCase() == "flat")?cellBorder:0 
  document.writeln ('<style>');
  document.writeln ('.rcMenuStatic'+quant+' {font-family:'+fontFace.split(",")[quant-1]+';font-size:'+fontSize.split(",")[quant-1]+';color:'+fontColour.split(",")[quant-1]+';font-weight:'+fontWeight.split(",")[quant-1]+';background-color:'+menuBackground.split(",")[quant-1]+'; cursor:pointer; text-decoration:'+fontDecoration.split(",")[quant-1]+'}');
  document.writeln ('.rcMenuHover'+quant+'  {font-family:'+fontFace.split(",")[quant-1]+';font-size:'+fontSize.split(",")[quant-1]+';color:'+fontHoverColour.split(",")[quant-1]+';font-weight:'+fontWeight.split(",")[quant-1]+';background-color:'+menuHoverBackground.split(",")[quant-1]+'; cursor:pointer; text-decoration:'+fontDecoration.split(",")[quant-1]+'}');
  document.writeln ('.rcSubMenuStatic'+quant+' {font-family:'+sfontFace.split(",")[quant-1]+';font-size:'+sfontSize.split(",")[quant-1]+';color:'+sfontColour.split(",")[quant-1]+';font-weight:'+sfontWeight.split(",")[quant-1]+';text-decoration:'+sfontDecoration.split(",")[quant-1]+';background-color:'+smenuBackground.split(",")[quant-1]+'; cursor:pointer}');
  document.writeln ('.rcSubMenuHover'+quant+'  {font-family:'+sfontFace.split(",")[quant-1]+';font-size:'+sfontSize.split(",")[quant-1]+';color:'+sfontHoverColour.split(",")[quant-1]+';font-weight:'+sfontWeight.split(",")[quant-1]+';text-decoration:'+sfontDecoration.split(",")[quant-1]+';background-color:'+smenuHoverBackground.split(",")[quant-1]+'; cursor:pointer}');
  document.writeln ('</style>');
}
// Build and show the main menu items
function showMenus(quant,definedOrientation)
{
  createStyles(quant);
  if(definedOrientation!=""){orientation=definedOrientation} 
  document.writeln('<div onmouseenter="hideSelectLists(true);" onmouseleave="hideSelectLists(false); " >');
  if (orientation.toLowerCase() == "vertical"){document.writeln ('<table border="0" cellpadding="0" cellspacing="'+styleBorder+'" bgColor="'+borderColor.split(",")[quant-1]+'">')}
  else{document.writeln ('<table border="0" cellpadding="0" cellspacing="'+styleBorder+'" bgColor="'+borderColor.split(",")[quant-1]+'"><tr>')}  
  for (x=0; x<eval("Menu"+quant).length; x++)
  {
    if (orientation.toLowerCase()=="vertical") document.writeln('<tr>')
    document.writeln ('<td width="'+menuWidth+'" onclick="tdMouseClick(\''+quant+'mainLink'+x+'\')" onMouseOver="hoverMenu(); popDown(\''+quant+'\','+x+', \''+quant+'button'+x+'\',\''+orientation+'\'); " onMouseOut="clearMenu('+quant+','+x+'); " ')
    if (menuStyle.split(",")[quant-1].toLowerCase() == "3d"){document.writeln ('style="border-left:'+cellBorder+'px solid '+borderHighlight.split(",")[quant-1]+';border-top:'+cellBorder+'px solid '+borderHighlight.split(",")[quant-1]+';border-right:'+cellBorder+'px solid '+borderShadow.split(",")[quant-1]+';border-bottom:'+cellBorder+'px solid '+borderShadow.split(",")[quant-1]+';"');}        
    document.writeln ('><div id="'+quant+'button'+x+'"><table border="0" cellpadding="'+cellPadding.split(",")[quant-1]+'" cellspacing="0" width="100%"><tr><td class="rcMenuStatic'+quant+'" id="'+quant+'cell'+x+'" nowrap>');
    document.writeln ('<a style="text-decoration:none" id="'+quant+'mainLink'+x+'" href="'+eval("Menu"+quant)[x][1]+'" target="'+eval("Menu"+quant)[x][2]+'" class="rcMenuStatic'+quant+'">'+eval("Menu"+quant)[x][0]+'</a></td>');
    if (subIndicate == 1&&eval("subMenu"+quant)[x].length>=1){
      document.writeln('<td class="rcMenuStatic'+quant+'" id="'+quant+'cell'+x+'a" align="right">');
      document.writeln ('<a style="text-decoration:none" id="'+quant+'mainLink'+x+'a" href="'+eval("Menu"+quant)[x][1]+'" target="'+eval("Menu"+quant)[x][2]+'" class="rcMenuStatic'+quant+'">'+indicator+'</a></td>');}
    document.writeln ('</tr></table></div></td>');    
    if (orientation.toLowerCase()=="vertical") document.writeln('</tr>')
  }
  if (orientation.toLowerCase() == "vertical"){document.writeln ('</table>');}
  else{document.writeln ('</tr></table>');}   
// Build the sub menu items
  for (x=0; x<eval("Menu"+quant).length; x++)
  { 
    if (eval("subMenu"+quant)[x].length > 0)
    {     
      document.writeln ('<div id="'+quant+'MENU'+x+'" style="visibility:hidden; position:absolute; z-index:2" >');
      document.writeln ('<table width="'+smenuWidth.split(",")[quant-1]+'" border="0" cellpadding="'+cellPadding.split(",")[quant-1]+'" cellspacing="'+styleBorder+'" bgColor="'+sborderColor.split(",")[quant-1]+'">');
      for (y=0; y<eval("subMenu"+quant)[x].length; y++)
      {
        document.writeln ('<tr>');
        if (eval("subMenu"+quant)[x][y][1].indexOf("#") != -1)
        {
          document.writeln ('<td bgColor="'+eval("subMenu"+quant)[x][y][2]+'" id="'+quant+'subMenu'+x+y+'" onMouseOver="hoverMenu(); highlightMenu(\'sub\','+x+','+y+',\'\','+quant+');  " nowrap')
          if (menuStyle.split(",")[quant-1].toLowerCase() == "3d"){document.writeln ('style="border-left:'+cellBorder+'px solid '+sborderHighlight.split(",")[quant-1]+';border-top:'+cellBorder+'px solid '+sborderHighlight.split(",")[quant-1]+';border-right:'+cellBorder+'px solid '+sborderShadow.split(",")[quant-1]+';border-bottom:'+cellBorder+'px solid '+sborderShadow.split(",")[quant-1]+';"');}
          document.writeln ('><p style="font-family:'+sfontFace.split(",")[quant-1]+'; font-size:'+sfontSize.split(",")[quant-1]+'; color:'+eval("subMenu"+quant)[x][y][1]+'"id="'+quant+'subLink'+x+y+'">'+eval("subMenu"+quant)[x][y][0]+'</p></td></tr>');
        }
        else
        { // this is the main menu items 
          document.writeln ('<td id="'+quant+'subMenu'+x+y+'" class="rcSubMenuStatic'+quant+'" onMouseOver="hoverMenu(); highlightMenu(\'sub\','+x+','+y+',\'\','+quant+')" onMouseOut="clearMenu('+quant+','+x+'); " onclick="tdMouseClick(\''+quant+'subLink'+x+y+'\');  " nowrap')
          if (menuStyle.split(",")[quant-1].toLowerCase() == "3d"){document.writeln ('style="border-left:'+cellBorder+'px solid '+sborderHighlight.split(",")[quant-1]+';border-top:'+cellBorder+'px solid '+sborderHighlight.split(",")[quant-1]+';border-right:'+cellBorder+'px solid '+sborderShadow.split(",")[quant-1]+';border-bottom:'+cellBorder+'px solid '+sborderShadow.split(",")[quant-1]+';"');}
          document.writeln ('><a style="text-decoration:none" id="'+quant+'subLink'+x+y+'" href="'+eval("subMenu"+quant)[x][y][1]+'" target="'+eval("subMenu"+quant)[x][y][2]+'" class="rcSubMenuStatic'+quant+'">'+eval("subMenu"+quant)[x][y][0]+'</a></td></tr>');
        }
      }
      document.writeln ('</table></div>');
    }
  }
    document.writeln('</div>');

} 

function blah()
{
 //setTimeout("hideSelectLists(false)",subMenuDelay-200);
 hideSelectLists(false); 
}
// Change colour or menu and submenu items when the mouse hovers over.  
function highlightMenu(element,mainMenu,dropMenu,state,quant)
{
  hoverMenu();
  state=(state == "hover")?"rcMenuHover"+quant:"rcMenuStatic"+quant
  if (element == "sub")
  {
    for (x=0; x < eval("subMenu"+quant)[mainMenu].length; x++)
    {
      if (eval("subMenu"+quant)[mainMenu][x][1].indexOf("#") == -1){
        eval(obj+'("'+quant+'subMenu'+mainMenu+x+'").className = "rcSubMenuStatic'+quant+'"')
        eval(obj+'("'+quant+'subLink'+mainMenu+x+'").className = "rcSubMenuStatic'+quant+'"')
      }
    } 
    if (eval("subMenu"+quant)[mainMenu][dropMenu][1].indexOf("#") == -1)  {
      eval(obj+'("'+quant+'subMenu'+mainMenu+dropMenu+'").className="rcSubMenuHover'+quant+'"')
      eval(obj+'("'+quant+'subLink'+mainMenu+dropMenu+'").className="rcSubMenuHover'+quant+'"')
    }
  }
  else
  {
    eval(obj+'("'+quant+'cell'+mainMenu+'").className = "'+state+'"')
    eval(obj+'("'+quant+'mainLink'+mainMenu+'").className = "'+state+'"')
    if (subIndicate == 1&&eval("subMenu"+quant)[mainMenu].length>=1)
    {
      eval(obj+'("'+quant+'cell'+mainMenu+'a").className = "'+state+'"')
      eval(obj+'("'+quant+'mainLink'+mainMenu+'a").className = "'+state+'"')
    }
  }
}
// Find positioning for sub menus
function getOffset(obj, dim) 
{
  if(dim=="left") 
  {     
    oLeft = obj.offsetLeft;    
    while(obj.offsetParent!=null) 
    {    
      oParent = obj.offsetParent     
      oLeft += oParent.offsetLeft 
      obj = oParent 	
    }
    return oLeft
  }
  else if(dim=="top")
  {
    oTop = obj.offsetTop;
    while(obj.offsetParent!=null) 
    {
      oParent = obj.offsetParent
      oTop += oParent.offsetTop
      obj = oParent 	
    }
    return oTop
  }
  else if(dim=="width")
  {
    oWidth = obj.offsetWidth
    return oWidth
  }  
  else if(dim=="height")
  {
    oHeight = obj.offsetHeight
    return oHeight
  }    
  else
  {
    alert("Error: invalid offset dimension '" + dim + "' in getOffset()")
    return false;
  }
}
// Show sub menus
function popDown(quant, param, id, orientation)
{
	  

  var cellBorderOffset = (isNS6)?cellBorder:eval(cellBorder*2)
  var browserAdjustment = (isNS6)?cellBorder:0
  var menu;
  var button;
  

  if (id)
  {    
    getOffset(eval(obj+'(id)'),'left');
    getOffset(eval(obj+'(id)'),'top');    
    getOffset(eval(obj+'(id)'),'width');  
    getOffset(eval(obj+'(id)'),'height');
    
    if (eval("Menu"+quant+"["+param+"][3]")=="right" && eval("subMenu"+quant+"["+param+"].length")>0) 
    { 
      oLeft=oLeft  
      oLeft=oLeft+oWidth; 
      getOffset(eval(obj+'("'+quant+'MENU'+param+'")'),'width');
      oLeft=oLeft-oWidth ;
      alignAdjustment = cellBorder*2 + 1
    }
    else 
    {
      alignAdjustment = 0
      oLeft=oLeft
    }    
  }  

  
  n = 0;    
  while (n < eval("Menu"+quant).length)
  {          
    menu = quant+"MENU"+n
    if (param == n)
    {
      theObj = eval(obj+'(menu)');
      if (theObj)
      {
         theObj.style.visibility = "visible"
          if (orientation.toLowerCase()=="vertical"){
            theObj.style.left=(menuStyle.split(",")[quant-1].toLowerCase()=="flat")?oLeft+oWidth+cellBorder+parseInt(horizontalOffset.split(",")[quant-1]):oLeft+oWidth+cellBorderOffset+parseInt(horizontalOffset.split(",")[quant-1]);
            theObj.style.top=(menuStyle.split(",")[quant-1].toLowerCase()=="flat")?oTop-cellBorder+parseInt(verticalOffset.split(",")[quant-1]):oTop+parseInt(verticalOffset.split(",")[quant-1])-browserAdjustment}
          else{
            theObj.style.left=(menuStyle.split(",")[quant-1].toLowerCase()=="flat")?oLeft-cellBorder+parseInt(horizontalOffset.split(",")[quant-1])+alignAdjustment:oLeft+parseInt(horizontalOffset.split(",")[quant-1])-browserAdjustment+alignAdjustment;
            theObj.style.top=(menuStyle.split(",")[quant-1].toLowerCase()=="flat")?oTop+oHeight+cellBorder+parseInt(verticalOffset.split(",")[quant-1]):oTop+oHeight+cellBorderOffset+parseInt(verticalOffset.split(",")[quant-1]);}
        }
      
       highlightMenu('main',n,'','hover',quant)
       if (eval("subMenu"+quant)[param].length > 0)
       {
         for (x=0; x<eval("subMenu"+quant)[param].length; x++)
         {
           if(eval("subMenu"+quant)[param][x][1].indexOf("#") == -1){
             eval (obj+'("'+quant+'subMenu'+param+x+'").className = "rcSubMenuStatic'+quant+'"')
             eval (obj+'("'+quant+'subLink'+param+x+'").className = "rcSubMenuStatic'+quant+'"')  
           }       
         } 
       }
    }
    else 
    {  
      for (x=1; x<quantity+1; x++)
      {       
        menu = x+"MENU"+n   
        //alert(menu)     
        if (eval(obj+'(menu)'))
        {
          eval(obj+'(menu).style.visibility = "hidden"');
        }
	else
	{
	//hideSelectLists(false); 
	}
        highlightMenu ('main',n,'','static',quant)
      }
    }

    n++
  }

    
}
// Re-set timer for sub menus
function hoverMenu()
{	
  if(timer){
  	clearTimeout(timer);
  }
}
// Set timer for sub menus
function clearMenu(quant,menu)
{
  setDelay = subMenuDelay;
  delay = (eval("subMenu"+quant)[menu].length > 0)?setDelay:1;
  timer = setTimeout("popDown("+quant+","+(eval("Menu"+quant).length + 1)+")",delay);
}
// when you click the box, perform the same function as if the user had clicked the hyperlink
function tdMouseClick(theElement)
{
  eval(obj+'(theElement).click()')
}

// IE6 and earlier select lists don't support z-index or zIndex, so we hide all of them:
function hideSelectLists( hide ) {
   if ( isIE6down ) {
       for ( var i = 0; i < document.forms.length; i++ ) {
           for ( var j = 0; j < document.forms[i].elements.length; j++ ) {
               if ( document.forms[i].elements[j].type &&
                   ( document.forms[i].elements[j].type.indexOf( "select" ) == 0 ) ) {
                   if ( hide )
                       document.forms[i].elements[j].style.visibility = "hidden";
                   else
                       document.forms[i].elements[j].style.visibility = "visible";
               }
	       }
	       }
	       
	      if ((! (document.getElementById('setsConfigPage'))) && (!(document.getElementById('addEditHPage'))) && (!(document.getElementById('odlDocsPage'))) )
	   //  var sS = document.title;
	     //if ((sS.indexOf("Edit Set") == -1) && (sS.indexOf("Add New Set") == -1))
		{
	        for ( var i = document.forms.length-1; i > -1 ; i-- ) {
		for ( var j = document.forms[i].elements.length -1; j > -1; j-- ) {
	        if ( document.forms[i].elements[j].type &&
                   ( document.forms[i].elements[j].type.indexOf( "text" ) == 0 ) &&
			document.forms[i].elements[j].style.visibility != 'hidden'){
		    if ( hide )
                       document.forms[i].elements[j].blur(); 
                   else
		   {
			

                       document.forms[i].elements[j].focus();
		   }
		   }
	       
           }
       }
		}
   }
}

// Trim white space around a string...
function trim(s) 
{
	if(!s)
		return "";
	while (s.substring(0,1) == ' ')
		s = s.substring(1, s.length);
	while (s.substring(s.length-1, s.length) == ' ')
		s = s.substring(0,s.length-1);
	return s;
}
