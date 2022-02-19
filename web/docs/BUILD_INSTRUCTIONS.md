
# Building jOAI

Instructions for building the jOAI web application software from source.

Quick instructions
--------------------------------------------

Use Ant to build the software. If you are already using Ant
and Tomcat, building the software is a three-step process:
1. Obtain the code directory joai-project
2. Set the ant property 'catalina.home' to point to your Tomcat installation.
3. From the joai-project directory, execute the 'deploy' target.

This will build the software and place it in a directory named 'oai'
inside your Tomcat webapps directory.


Detailed instructions
--------------------------------------------

Obtain the joai-project code at:
https://github.com/NCAR/joai-project


To build the jOAI software from source you must have the following:

1. joai-project - the jOAI software source code directory
3. Tomcat (available at http://tomcat.apache.org/)
4. Ant (available at http://ant.apache.org/)
5. Java Platform, Standard Edition


### Build instructions

These instructions assume you will be working in a UNIX command-line environment.
The software can also be built on Windows but specific instructions are not
provided here.

1. Obtain the joai-project code directory (see above)

2. Obtain and install all the required tools - Java, Ant, Tomcat (see above)

3. Place the joai-project directory into a build directory
For example:

    > cd ~/my_build_area
    
    > ls joai-project

4. cd into the joai-project directory.
    > cd joai-project

5. Create a file named build.properties or joai.properties in your build directory,
that is the parent directory of the joai-project directory. Alternatively this file
can be placed in your home directory.

6. Edit the build.properties file and set the property catalina.home to
point to your installation of Tomcat, for example:
catalina.home = /home/username/dev/apache-tomcat-8

See the build.xml file located in joai-project for information on the
properties settings.

7. Execute the Ant deploy command.
    > ant deploy
   
You should see a number of messages in your terminal. Once complete
you should see a message like:
    
    BUILD SUCCESSFUL
    Total time: 36 seconds

8. The software will be built into a directory named oai inside your
Tomcat webapps directory. After (re)starting Tomcat you will be able to
access the software at the URL http://localhost:8080/oai/
(substitute localhost with your domain name if appropriate).

### Generate the Javadoc documentation

To generate and view the Java code documentation, execute these Ant 
commands

> ant javadoc

> ant deploy 

Then view the Javadoc in your browser at URL http://localhost:8080/oai/docs/javadoc

### Code History

The jOAI code was previously managed under two separate modules at SourceForge
(joai-project and dlese-tools-project branch joai_v3_1_1_branch at
https://sourceforge.net/projects/dlsciences/).
In March 2017, the jOAI code project was moved to GitHub.
