# Installing jOAI

Installation instructions for the jOAI web application software.

If you are upgrading from a previous version, see notes on 
upgrading below.

To install and run the jOAI software you must have the following:
  
1. oai.war - the jOAI software.
2. Apache Tomcat
3. Java Standard Edition (SE)


Step 1: Download and install the Tomcat servlet container on your server.
Note that on some linux and unix distributions, Tomcat comes pre-installed but does not 
include all the required Java libraries. Download and install
a full copy of Tomcat directly from http://tomcat.apache.org/

Step 2: Download jOAI - the [latest release is available on GitHub here](https://github.com/NCAR/joai-project/releases/latest).
Unzip to obtain the 'oai.war' file.

Step 3: Place the file 'oai.war' into the 'webapps' directory found in    
your Tomcat installation. 'webapps' is the default location where Tomcat 
expects to find web applications.

Step 4: Start or restart Tomcat. Upon startup the first time, Tomcat will 
automatically unpack the oai.war archive, creating a directory and 
application context named 'oai'.
  
Step 5: The jOAI software should now be running. Launch a
web browser and type in the URL to the oai servlet context, which will
have the following form:
- The address will use the context path created in step 3 ('oai'). 
If the domain name to your server is http://www.myserver.edu and Tomcat 
was been installed using the default port (8080), the URL to the OAI
software will be http://www.myserver.edu:8080/oai/
- Alternatively you may access the software using the localhost domain
shortcut: http://localhost:8080/oai/.

Step 6: To begin using the data provider or harvester, go to the administrative
pages listed under the 'Data Provider' and 'Harvester' menus in your browser.

Step 7 (optional): For additional software configuration options such as
enabling access control, see the page 'Configuring jOAI' in your browser, 
e.g.: http://localhost:8080/oai/docs/configuring_joai.jsp

#### Troubleshooting:
- If there are errors accessing the software try repeating or verifying steps 1 and 2.
- Tip: Check the Tomcat logs for error messages located in the 'logs'
  directory of your Tomcat installation if there are problems. Note: jOAI
  will output error messages to standard out, which are written to
  the catalina.out log in Tomcat


### Building from Source Code

(Optional) Read the [Build instructions](BUILD_INSTRUCTIONS.md) for information about obtaining
the source code and building jOAI using ant.


### Upgrading from a previous version

If you are upgrading from v3.0.2 or earlier versions of the software,
please note the following:

- Saved indexes, settings and configuration files from previous versions of the 
software are not compatible with the new version.

- Remove any previous installations from your tomcat webapps directory 
before installing the new version. You can save the previous installation
by moving it's directory (e.g. the 'oai' directory) out of webapps and 
reinstall it later by placing it back into webapps if needed.

- Only one instance of the software should be installed and running in a single
Tomcat (or other application server) at any given time.


### Tips and notes

1. Only one instance of the software should be installed and running in a single
Tomcat (or other application server) at any given time.

2. Tomcat on Linux

	The following tips taken from the Tomcat 5 release notes has been used to 
	fix stability problems reported with the OAI software on Linux:

	GLIBC 2.2 / Linux 2.4 users should define an environment variable:
	export LD_ASSUME_KERNEL=2.2.5
	
	Redhat Linux 9.0 users should use the following setting to avoid
	stability problems:
	export LD_ASSUME_KERNEL=2.4.1

3. If Tomcat does not unpack the oai.war archive automatically, you may do so manually. 
	
	On Linux: 
	1. Create an 'oai' directory in the 'webapps' directory.
	2. cd into 'oai.' 
	3. Enter the command 'jar xvf ../oai.war.'
	

