cics-java-liberty-mq-jms
================

[![Build](https://github.com/cicsdev/cics-java-liberty-mq-jms/actions/workflows/main.yml/badge.svg)](https://github.com/cicsdev/cics-java-liberty-mq-jms/actions/workflows/main.yml)


This project contains two samples which can be used  in a CICS Liberty JVM server to test demonstrate JMS integration with IBM MQ.
The first sample is an EJB application demonstrating how to use a message-driven bean (MDB).
The second sample is a Java EE web application that uses JMS to connect to a remote IBM MQ queue manager using an MQ 
client mode connection. This sample can also be used to write a JMS message to drive
the MDB application. For further details on how to develop these samples refer to the accompanying IBM developer tutorial [Develop a JMS application for CICS Liberty](https://developer.ibm.com/tutorials/develop-jms-applications-for-ibm-cics-liberty)

## Repository structure

* [cics-java-liberty-mqjms-cicsbundle](cics-java-liberty-mqjms-cicsbundle) - CICS bundle plug-in based project. Use with Gradle and Maven build.
* [cics-java-liberty-mqjms-ear](cics-java-liberty-mqjms-ear) - EAR project referring to the EJB project and Web projects.
* [cics-java-liberty-mqjms-mdb](cics-java-liberty-mqjms-mdb) - EJB project containg an MDB.
* [cics-java-liberty-mqjms-web](cics-java-liberty-mqjms-web) - Dynamic web project containing a servlet.
* [etc/server.xml](etc/server.xml) - Example Liberty server configuration file
* [etc/eclipse_projects/com.ibm.cicsdev.mqjms.cicsbundle](etc/eclipse_projects/com.ibm.cicsdev.mqjms.cicsbundle) - CICS Explorer based CICS bundle project. For use with CICS Explorer.

## Java Code
* com.ibm.cicsdev.mqjms.mdb
  * [MySimpleMDB.java](cics-java-liberty-mqjms-mdb/src/main/java/com/ibm/cicsdev/mqjms/mdb/MySimpleMDB.java) - An MDB which receives JMS
  messages written to the MQ queue `DEMO.MDBQUEUE` and writes these messages to the CICS TSQ `RJMSTSQ`.
* com.ibm.cicsdev.mqjms.web
  * [MQJMSDemo.java](cics-java-liberty-mqjms-web/src/main/java/com/ibm/cicsdev/mqjms/cf/web/MQJMSDemo.java) - A servlet that can be used to get and put messages to the MQ  queue `DEMO.SIMPLEQ` using an MQ client mode connection to a remote queue manager.

## Requirements
* IBM CICS TS V5.5 or later
* IBM MQ V9.0 or later on z/OS
* Java SE 1.8 or later on the workstation
* Eclipse with the IBM CICS SDK for Java EE, Jakarta EE and Liberty, downloaded from [here](https://ibm.github.io/mainframe-downloads/downloads.html) or any IDE that supports usage of the Maven Central artifact [com.ibm.cics:com.ibm.cics.server.](https://search.maven.org/artifact/com.ibm.cics/com.ibm.cics.server)


## Configuring IBM MQ
Setup the following resources in the IBM MQ queue manager:

1. MQ channel named `WAS.JMS.SVRCONN`
2. MQ queue named `DEMO.MDBQUEUE`. This must be defined as shareable
3. MQ queue named `DEMO.SIMPLEQ`. This should be defined as shareable

**Note:** `DEMO.MDBQUEUE` *must* be defined as shareable to allow usage in the 
multi-threaded environment in Liberty. In addition it is advisable to set the 
[BackoutThreshold](https://www.ibm.com/support/knowledgecenter/en/SSFKSJ_9.0.0/com.ibm.mq.dev.doc/q032280_.htm) 
attribute on the MDBQUEUE to a finite value, to prevent the MDB being 
constantly redriven if the MDB fails during the processing of the message.

`DEMO.SIMPLEQ` *should* be defined as shareable as Liberty servlets can be accessed concurrently.


## Importing the samples into Eclipse
1. Using an Eclipse development environment with the CICS Explorer SDK installed create 
Import the projects into CICS Explorer using File → Import → General → Existing projects into workspace, 
Ensure the following Eclipse projects are imported:
* [com.ibm.cicsdev.mqjms.ear](cics-java-liberty-mqjms-ear) - EAR project referring to the EJB project and Web projects.
* [com.ibm.cicsdev.mqjms.mdb](cics-java-liberty-mqjms-mdb) - EJB project containg an MDB.
* [com.ibm.cicsdev.mqjms.web](cics-java-liberty-mqjms-web) - Dynamic web project containing a servlet.
* [com.ibm.cicsdev.mqjms.cicsbundle](etc/eclipse_projects/com.ibm.cicsdev.mqjms.cicsbundle) - CICS Explorer based CICS bundle project that includes an EAR bundle part for the EAR project.

1. Add the *CICS with Java EE 6 & 7 Liberty Library* to the build path of your web and EJB projects. 
1. Ensure the projects are targeted to compile at a level that is compatible with the Java level being used on CICS. This can be achieved by editing the Java Project Facet in the project properties.
1. Update the name of the CICS JVM server DFHWLP specified in the CICS bundle project to match the value used in the target CICS system. This is specified in the `com.ibm.cicsdev.mqjms.ear.earbundle` XML file.
1. Export the CICS bundle project to zFS using the "Export Bundle Project to z/OS UNIX File System" menu.

## Building 
You can build the sample using an IDE of your choice, or you can build it from the command line. For both approaches, using Gradle or Maven is the recommended way to get a consistent version of build tooling. 
The output of the build will produce an EAR file containing the EJB JAR and WAR files, and can optionally create a CICS bundle archive 
for installing the EAR file within a CICS bundle into a CICS region.

### Building with Gradle
An EAR file is created inside the `cics-java-liberty-mqjms-ear/build/libs` directory and a CICS bundle ZIP file inside the `cics-java-liberty-mqjms-cicsbundle/build/distributions` directory.

If building a CICS bundle ZIP the CICS JVM server name for the EAR bundle part should be set using the `cics.jvmserver` property, defined in the [`cics-java-liberty-mqjms-cicsbundle/build.gradle`](cics-java-liberty-mqjms-cicsbundle/build.gradle) file, or alternatively can be set on the command line. 
See the following commands for example Gradle build commands.

| Tool | Command |
| ----------- | ----------- |
| Gradle Wrapper (Linux/Mac) | ```./gradlew clean build``` |
| Gradle Wrapper (Windows) | ```gradle.bat clean build``` |
| Gradle (command-line) | ```gradle clean build``` |
| Gradle (command-line & setting jvmserver) | ```gradle clean build -Pcics.jvmserver=MYJVM``` |

### Building with Apache Maven
An EAR file is created inside the `cics-java-liberty-mqjms-ear/target` directory and a CICS bundle ZIP file inside the `cics-java-liberty-mqjms-cicsbundle/target` directory.

If building a CICS bundle ZIP the CICS JVM server name for the EAR bundle part should be modified in the 
 `cics.jvmserver` property, defined in [`cics-java-liberty-mqjms-cicsbundle/pom.xml`](cics-java-liberty-mqjms-cicsbundle/pom.xml) file under the `defaultjvmserver` configuration property, or alternatively can be set on the command line.
 See the following commands for example Maven build commands.

| Tool | Command |
| ----------- | ----------- |
| Maven Wrapper (Linux/Mac) | ```./mvnw clean verify``` |
| Maven Wrapper (Windows) | ```mvnw.cmd clean verify``` |
| Maven (command-line) | ```mvn clean verify``` |
| Maven (command-line & setting jvmserver) | ```mvn clean verify -Dcics.jvmserver=MYJVM``` |


## Configuring the IBM MQ JMS Adapter
1. Download the required version of the MQ RAR from 
   [Fix Central](http://www-01.ibm.com/support/docview.wss?uid=swg21633761).
2. **Important:** Ensure you follow the installation instructions. The downloaded .jar needs additional processing to produce the JCA resource adapter: `wmq.jmsra.rar`. 

## Deploying the sample
1. Export the CICS bundle to zFS
1. Define and install a Liberty JVMSERVER named `DFHWLP` 
1. Specify the location of the IBM MQ Resource Adapter by adding the following 
   entry to the server.xml file:

   ```xml
   <variable name="wmqJmsClient.rar.location" value="/path/to/wmq/rar/wmq.jmsra.rar"/>
   ```

   where the `value` attribute specifies the absolute path to the IBM MQ 
   Resource Adapter file, `wmq.jmsra.rar`.

1. Add the features `mdb-3.2` and `wmqJmsClient-2.0` and `jndi-1.0` to the `featureManager` 
   element in the Liberty JVM server's server.xml configuration file.

1. Add a definition to the server.xml for the queues required by the sample:

   ```xml    
   <jmsQueue id="jms/mdbq" jndiName="jms/mdbq">
       <properties.wmqJms baseQueueName="DEMO.MDBQUEUE" />
   </jmsQueue>     
   <jmsQueue id="jms/simpleq" jndiName="jms/simpleq">
       <properties.wmqJms baseQueueName="DEMO.SIMPLEQ" />
   </jmsQueue>    
   ```

1. Add a JMS connection factory definition to the server.xml:

   ```xml
   <jmsQueueConnectionFactory connectionManagerRef="ConMgrJms" jndiname="jms/qcf1">
       <properties.wmqJms channel="WAS.JMS.SVRCONN" 
                          hostName="localhost" 
                          port="1414"
                          queueManager="QM1" 
                          transportType="CLIENT"/>
   </jmsQueueConnectionFactory>
   
   <connectionManager id="ConMgr" maxPoolSize="10"/>
   ```

1. Add a JMS activation spec to the server.xml for the MDB test. This defines 
   that the MDB `MySimpleMDB` is invoked when the MDBQUEUE is written to. 

   ```xml
   <jmsActivationSpec id="cics-java-liberty-mqjms-ear-1.0.0/cics-java-liberty-mqjms-mdb/MySimpleMDB">
       <properties.wmqJms destinationRef="jms/mdbq" destinationType="javax.jms.Queue"
                          channel="WAS.JMS.SVRCONN"
                          hostName="localhost"
                          port="1414"
                          queueManager="QM1"			 
                          transportType="CLIENT" />
   </jmsActivationSpec>
   ```

   where the `port` attribute specifies the port the IBM MQ queue manager is 
   running on and the `queueManager` attribute specifies the name of the IBM MQ
   queue manager. These values will be specific to your MQ installation.

1. Define and install a CICS BUNDLE resource definition referring to the 
   deployed bundle directory on zFS and ensure all resources are 
   enabled.

*Optinally*, you can define and install a CICS TSMODEL resource named `RJMSTSQ`
with the attribute `RECOVERY(YES)` if you want to make the MDB test 
transactional.

**Note:** The jmsActivationSpec `id` attribute must match the JNDI name of the 
MDB in the `java:global` namespace. In Liberty, EJBs are registered to the 
`java:global`  namespace using the following scheme: `application/module/bean`.

This name is displayed in the Liberty servers messages.log file when the MDB is
registered into the EJB container:
    
```
[8/16/17 15:42:47:611 BST] 0000004b com.ibm.ws.ejbcontainer.runtime.AbstractEJBRuntime           I CNTR0180I: The MySimpleMDB message-driven bean in the com.ibm.cicsdev.mqjms.mdb.jar module of the com.ibm.cicsdev.mqjms.mdb.ear application is bound to the cics-java-liberty-mqjms-ear-1.0.0/cics-java-liberty-mqjms-mdb/MySimpleMDB activation specification.
```


## Running the MDB sample
1. Write a record to the IBM MQ queue `DEMO.MDBQUEUE`.
2. Browse the contents of the TSQ `RJMSTSQ`, the record written in 
   `DEMO.MDBQUEUE` will have been read by the MDB and written to this CICS TSQ.

Records can be written using the IBM MQ client sample program `amqsputc`, or by
running the JMS servlet provided with the JMS Connection Factory part of this sample. The
IBM MQ client samples are provided with a distributed (workstation) installation of MQ.

To use the `amqsputc` sample from the workstation command line, set the MQSERVER variable to the name of the channel:
  
```sh
set MQSERVER=WAS.JMS.SVRCONN/TCP/localhost(1414)
```

where `localhost` is the hostname of the system the IBM MQ queue manager is 
running on and `1414` is the TCP port the IBM MQ queue manager is listening on.

Then connect to the MDB queue and write some test data as follows using amqsputc from 
the workstation command line:

```sh
amqsputc DEMO.MDBQUEUE
Sample AMQSPUT0 start
target queue is DEMO.MDBQUEUE
hello from CICS
```

The next section describes how to use the JMS Connection Factory Servlet to send a message to the MDB.


## Running the Connection Factory sample
1. Using a web browser, access the web application using the following URL:
   
   http://mvs.example.ibm.com:9080/com.ibm.cicsdev.mqjms.web?test=putq

   where `mvs.example.ibm.com` is the hostname of the LPAR and `9080` is the 
   HTTP port Liberty is configured to run on.
   
   This will write a record to the IBM MQ queue `DEMO.SIMPLEQ` and return the
   following HTTP response:

   ```
   22/06/2017 16:11:20 Message has been written to queue:///DEMO.SIMPLEQ
   ```
2. Using a web browser, access the web application using the following URL:

   http://mvs.example.com:9080/com.ibm.cicsdev.mqjms.web?test=readq

   This will read a record from the IBM MQ queue `DEMO.SIMPLEQ` and return it
   in the HTTP response.



## Running Both Samples
Both the MDB sample and the ConnectionFactory sample can be run together. The
ConnectionFactory servlet can be used to write messages onto the queue the MDB
is bound to.

The same servlet can then be used to read records from the TSQ the MDB stores
the messages to.

1. Using a web browser, access the web application using the following URL:

   http://mvs.example.ibm.com:9080/com.ibm.cicsdev.mqjms.web?test=putmdbq

   where `mvs.example.ibm.com` is the hostname of the LPAR and `9080` is the 
   HTTP port Liberty is configured to run on.
1. The MDB should automatically read this record of the queue
1. Using a web browser, access the web application using the following URL:

   http://mvs.example.ibm.com:9080/com.ibm.cicsdev.mqjms.web?test=readtsq

   This should return the TSQ record writen by the MDB from the IBM MQ queue.


## References
For further details on using the JMS APIs in CICS Liberty refer to this 
[tutorial](https://developer.ibm.com/tutorials/develop-jms-applications-for-ibm-cics-liberty)

For further information on JMS and IBM MQ refer to the following:

*  [Liberty and the IBM MQ resource adapter](https://www.ibm.com/support/knowledgecenter/en/SSFKSJ_9.0.0/com.ibm.mq.dev.doc/q120040_.htm) 
   in the IBM MQ Knowledge Center
*  [Deploying message-driven beans to connect to IBM MQ](https://www.ibm.com/support/knowledgecenter/en/SS7K4U_liberty/com.ibm.websphere.wlp.zseries.doc/ae/twlp_dep_msg_mdbwmq.html) in the Liberty Knowledge Center

## License
This project is licensed under [Apache License Version 2.0](LICENSE).
