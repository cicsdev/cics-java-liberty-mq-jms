cics-java-liberty-mq-jms
================
This project contains two samples. One sample is Java EE web application demonstrating 
how to use a message-driven bean (MDB). The second sample is a JMS 
ConnectionFactory sample that connects to a remote IBM MQ queue manager using an MQ 
client mode connection. This sample can also be used to write a JMS message
for the MDB sample to process. For further details on how to develop these samples refer to the accompanying tutorial [Developing an MQ JMS application for CICS Liberty](blog.md)

## Repository structure

* [projects/](projects) - Eclipse projects suitable for importing into a CICS 
                          Explorer environment
* [etc/](etc) - Example Liberty server configuration file

## Overview

### MDB Sample
* `com.ibm.cicsdev.mqjms.mdb` - EJB project containing the MySimpleMDB that 
                                recives a message put to the MDB queue.
* `com.ibm.cicsdev.mqjms.mdb.ear` - EAR project referring to the MDB EJB 
                                    project.
* `com.ibm.cicsdev.mqjms.mdb.cicsbundle` - CICS bundle project that references 
                                           the EAR project for the MDB sample.

### Connection Factory Sample
* `com.ibm.cicsdev.mqjms.cf.web` - Web project containing the MQJMSDemo servlet
                                   that uses a JMS ConnectionFactory.
* `com.ibm.cicsdev.mqjms.cf.ear` - EAR project referring to the the web project
                                   `com.ibm.cicsdev.mqjms.cf.web`.
* `com.ibm.cicsdev.mqjms.cf.cicsbundle` - CICS bundle project that references 
                                          the web project for the 
					  ConnectionFactory sample.

## Java Code
* com.ibm.cicsdev.mqjms.mdb
  * MySimpleMDB.java - An MDB which receives JMS messages written to the MQ 
                       queue `DEMO.MDBQUEUE` and writes these messages to the 
		       CICS TSQ `RJMSTSQ`.
* com.ibm.cicsdev.mqjms.cf.web
  * MQJMSDemo.java - A servlet can be used to get and put messages to the MQ 
                     queue `DEMO.SIMPLEQ` using an MQ client mode connection to
		     a remote queue manager.

## Requirements
* IBM CICS TS V5.3 with APAR 
  [PI58375](http://www.ibm.com/support/docview.wss?uid=swg1PI58375) and 
  [PI67640](http://www.ibm.com/support/docview.wss?uid=swg1PI67640), or 
  CICS TS V5.4
* IBM MQ V8.0 or later on z/OS
* IBM CICS Explorer V5.4 with the IBM CICS SDK for Java EE and Liberty feature 
  installed. Download from [IBM Mainframe DEV](https://developer.ibm.com/mainframe/products/downloads)


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

`DEMO.SIMPLEQ` *should* be defined as shareable as Liberty servlets can be 
accessed concurrently.


## Importing the samples into Eclipse
1. Clone or download the repository
2. Import the projects into CICS Explorer using 
   **File > Import > General > Existing projects into workspace**.
3. Resolve the build path errors on the Web and EJB projects using the 
   following menu from each project: 
   **Build Path > Configure Build Path > Libraries > Add Library > CICS with Java EE and Liberty** 
   and select the version of CICS TS for deployment.

## Configuring the IBM MQ JMS Adapter
1. Download the MQ V9.0.1 RAR from 
   [Fix Central](http://www-01.ibm.com/support/docview.wss?uid=swg21633761).
2. **Important:** Ensure you follow the installation instructions. The downloaded .jar needs additional processing to produce the JCA resource 
   adapter: `wmq.jmsra.rar`. 

## Deploying the MDB sample
To deploy the MDB sample you must have previously imported the projects into CICS 
Explorer.

1. Export the CICS bundle from Eclipse by selecting the project 
   **`com.ibm.cicsdev.mqjms.mdb.cicsbundle` > Export Bundle Project to z/OS UNIX File System**
2. Define and install a Liberty JVMSERVER named `DFHWLP` in the CICS region 
   (see [Starting a CICS Liberty JVM server in 4 easy steps](https://developer.ibm.com/cics/2015/06/04/starting-a-cics-liberty-jvm-server-in-4-easy-steps/)).
3. Specify the location of the IBM MQ Resource Adapter by adding the following 
   entry to the server.xml file:

   ```xml
   <variable name="wmqJmsClient.rar.location" value="/path/to/wmq/rar/wmq.jmsra.rar"/>
   ```

   where the `value` attribute specifies the absolute path to the IBM MQ 
   Resource Adapter file, `wmq.jmsra.rar`.

4. Add the features `mdb-3.2` and `wmqJmsClient-2.0` to the `featureManager` 
   element in the Liberty JVM server's server.xml configuration file.
5. Add a definition to the server.xml for the queue required by the sample:

   ```xml    
   <jmsQueue id="jms/mdbq" jndiName="jms/mdbq">
       <properties.wmqJms baseQueueName="DEMO.MDBQUEUE" />
   </jmsQueue>
   ```

6. Add a JMS activation spec to the server.xml for the MDB test. This defines 
   that the MDB `MySimpleMDB` is invoked when the MDBQUEUE is written to. 

   ```xml
   <jmsActivationSpec id="com.ibm.cicsdev.mqjms.mdb.ear/com.ibm.cicsdev.mqjms.mdb/MySimpleMDB">
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

7. Define and install a CICS BUNDLE resource definition referring to the 
   deployed bundle directory on zFS in step 1, and ensure all resources are 
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
[8/16/17 15:42:47:611 BST] 0000004b com.ibm.ws.ejbcontainer.runtime.AbstractEJBRuntime           I CNTR0180I: The MySimpleMDB message-driven bean in the com.ibm.cicsdev.mqjms.mdb.jar module of the com.ibm.cicsdev.mqjms.mdb.ear application is bound to the com.ibm.cicsdev.mqjms.mdb.ear/com.ibm.cicsdev.mqjms.mdb/MySimpleMDB activation specification.
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


## Deploying the Connection Factory sample

1. Export the CICS bundle from Eclipse by selecting the project 
   **`com.ibm.cicsdev.mqjms.cf.cicsbundle` > Export Bundle Project to z/OS UNIX File System**
2. Define and install a Liberty JVMSERVER named `DFHWLP` in the CICS region.
3. Add the features `mdb-3.2`, `wmqJmsClient-2.0` and `jndi-1.0` to the 
   `featureManager` element in the Liberty JVM server's server.xml 
   configuration file.
4. Add a JMS connection factory definition to the server.xml:

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

   where the `port` attribute specifies the port the IBM MQ queue manager is 
   running on and the `queueManager` attribute specifies the name of the IBM MQ
   queue manager.

5. Add a definition to the server.xml for the queue required by the 
   ConnectionFactory sample.

   ```xml
   <jmsQueue id="jms/simpleq" jndiName="jms/simpleq">
       <properties.wmqJms baseQueueName="DEMO.SIMPLEQ" />
   </jmsQueue>    
   ```


## Running the Connection Factory sample

1. Using a web browser, access the web application using the following URL:
   
   http://mvs.example.ibm.com:9080/jmsweb?test=putq

   where `mvs.example.ibm.com` is the hostname of the LPAR and `9080` is the 
   HTTP port Liberty is configured to run on.
   
   This will write a record to the IBM MQ queue `DEMO.SIMPLEQ` and return the
   following HTTP response:

   ```
   22/06/2017 16:11:20 Message has been written to queue:///DEMO.SIMPLEQ
   ```
2. Using a web browser, access the web application using the following URL:

   http://mvs.example.com:9080/jmsweb?test=readq

   This will read a record from the IBM MQ queue `DEMO.SIMPLEQ` and return it
   in the HTTP response.



## Running Both Samples
Both the MDB sample and the ConnectionFactory sample can be run together. The
ConnectionFactory servlet can be used to write messages onto the queue the MDB
is bound to.

The same servlet can then be used to read records from the TSQ the MDB stores
the messages to.

1. Deploy both the ConnectionFactory and MDB samples and configure the Liberty
   server.xml as defined in the previous sections.
2. Using a web browser, access the web application using the following URL:

   http://mvs.example.ibm.com:9080/jmsweb?test=putmdbq

   where `mvs.example.ibm.com` is the hostname of the LPAR and `9080` is the 
   HTTP port Liberty is configured to run on.
3. The MDB should automatically read this record of the queue
4. Using a web browser, access the web application using the following URL:

   http://mvs.example.ibm.com:9080/jmsweb?test=readtsq

   This should return the TSQ record writen by the MDB from the IBM MQ queue.


## References

For further details on using the JMS APIs in CICS Liberty refer to this 
[tutorial](https://github.com/cicsdev/cics-java-liberty-mq-jms/blob/master/blog.md)

For further information on JMS and IBM MQ refer to the following:

*  [Liberty and the IBM MQ resource adapter](https://www.ibm.com/support/knowledgecenter/en/SSFKSJ_9.0.0/com.ibm.mq.dev.doc/q120040_.htm) 
   in the IBM MQ Knowledge Center
*  [Deploying message-driven beans to connect to IBM MQ](https://www.ibm.com/support/knowledgecenter/en/SS7K4U_liberty/com.ibm.websphere.wlp.zseries.doc/ae/twlp_dep_msg_mdbwmq.html)
   in the Liberty Knowledge Center
*  [Defining MDB queues as shareable](http://www.ibm.com/support/docview.wss?uid=swg21232930) 
   for details on the `2042 MQRC_OBJECT_IN_USE when an MDB tries to get a message`

## License

This project is licensed under [Apache License Version 2.0](LICENSE).
