cics-java-liberty-mq-jms
================
Sample JMS Java EE web application demonstrating how to use a JMS Connection Factory and a message-driven bean (MDB) to get and put messages to queues managed using IBM MQ

## Samples overview

* `com.ibm.cicsdev.mqjms.ear` - EAR project referring to the Web and EJB projects
* `com.ibm.cicsdev.mqjms.web` - Dynamic web project containing the MQJMSDemo servlet that uses JMS to get and put messages to the queues
* `com.ibm.cicsdev.mqjms.mdb` - EJB project containing the MQSimpleMDB that recives a message put to the MDB queue
* `com.ibm.cicsdev.mqjms.cicsbundle` - CICS bundle project that references the EAR bundle part for deployment in a CICS bundle


## Repository structure

* [`projects/`](projects) - Complete Eclipse projects suitable for importing into a CICS Explorer environment.



## Pre-requisites

* CICS TS V5.3 with APAR PI67640 and APAR PI58375, or CICS TS V5.4
* Java SE 7 or later on the z/OS system
* Java SE 7 or later on the workstation
* CICS Explorer with the IBM CICS SDK for Java EE and Liberty feature installed [see](https://developer.ibm.com/mainframe/products/downloads)
* IBM MQ V8.0 or later on z/OS
* IBM MQ Version 9.0.1 resource adapter, available from [IBM Fix Central](https://www.ibm.com/support/fixcentral/)
 

## Configuration

The sample code can be deployed as an EAR file into a CICS Liberty JVM server. The MQJMSDemo servlet can then be used to get and put messages to the defined queues and using an MQ client mode connection to a remote queue manager. There is also an MDB provided which will receive a message written to a MQ queue and then write this message to a CICS TSQ queue in the same unit of work




### To import the samples into Eclipse

1. Import the projects into CICS Explorer using **File -> Import -> General -> Existing** projects into workspace
1. Resolve the build path errors on the Web and EAR projects using the following menu from each project: **Build Path -> Configure Build Path -> Libraries -> Add Library -> CICS with Java EE and Liberty** and select the version of CICS TS for deployment (either CICS TS V5.3 or CICS TS V5.4)

### To configure IBM MQ
Setup the following resources in the IBM MQ queue manager

 1. MQ channel - WAS.JMS.SVRCONN
 1. MQ queue - DEMO.SIMPLEQ 
 1. MQ queue - DEMO.MDBQUEUE - This must be defined as shareable

### To configure CICS
1.Create a Liberty JVM server as described in [4 easy steps](https://developer.ibm.com/cics/2015/06/04/starting-a-cics-liberty-jvm-server-in-4-easy-steps/)

2.Download and install the MQ V9 RAR. This is available at [Fix Central](http://www-01.ibm.com/support/docview.wss?uid=swg21633761) and then define the RAR in the Liberty server.xml configuration file as follows:
`<variable name="wmqJmsClient.rar.location" value="/u/cics1/RARs/wmq.jmsra.rar"/>`

3.Ensure the following Liberty features are present in server.xml
 ```xml
<feature>cicsts:core-1.0</feature>
<feature>wmqJmsClient-2.0</feature>
<feature>mdb-3.2</feature>
<feature>jndi-1.0</feature>
```
4.Add a JMS connection factory definition to the server.xml as follows:
```xml
<jmsQueueConnectionFactory connectionManagerRef="ConMgr" jndiname="jms/qcf1">
    <properties.wmqJms channel="WAS.JMS.SVRCONN" 
        hostName="localhost" 
        port="<your port>" queueManager="<your queue manager>" 
        transportType="CLIENT"/>
</jmsQueueConnectionFactory>
    
<connectionManager id="ConMgr" maxPoolSize="10"/>  
```
5.Add a definition for the queues required by the test as follows:
```xml
<jmsQueue id="jms/simpleq" jndiName="jms/simpleq">
    <properties.wmqJms baseQueueName="DEMO.SIMPLEQ" />
</jmsQueue>
<jmsQueue id="jms/mdbq" jndiName="jms/mdbq">
    <properties.wmqJms baseQueueName="DEMO.MDBQUEUE" />
</jmsQueue>  
```
6.Add a JMS activation spec to the server.xml to define our MDB that will be invoked from the MDB queue. 
        
```xml
<jmsActivationSpec id="mySimpleJMSEAR/mySimpleJMSMDB/MySimpleMDB">
    <properties.wmqJms channel="WAS.JMS.SVRCONN"
        destinationRef="jms/mdbq" destinationType="javax.jms.Queue"
        port="<your port>" queueManager="<your queue manager>"
        transportType="CLIENT" />
</jmsActivationSpec>
```

The jmsActivationSpec must be in the format of application name/module name/bean name, and is output in the Liberty messages.log in the following CNTR0180I message:  
`[6/21/17 10:42:46:223 BST] 00000076 com.ibm.ws.ejbcontainer.runtime.AbstractEJBRuntime           I CNTR0180I: The MySimpleMDB message-driven bean in the mySimpleJMSMDB.jar module of the mySimpleJMSEAR application is bound to the com.ibm.cicsdev.mqjms.ear/com.ibm.cicsdev.mqjms.mdb/MySimpleMDB activation specification`    

The `hostName`, `port`, `queueManager` properties in each of these elements should be set based on your local MQ configuration. The `transportType` property must be set to `CLIENT`

7.Define and install a CICS TSMODEL resource named RJMSTSQ with the attribute RECOVERY(YES) if you want to run the MDB test

 

### To deploy the sample into a CICS region 
1. Change the name of the JVMSERVER in the .warbundle file from DFHWLP to the name of the JVMSERVER resource defined in CICS. 
1. Using the CICS Explorer export the com.ibm.cicsdev.mqjms.cicsbundle project to a zFS directory. 
1. Define and install a CICS BUNDLE resource definition referring to the deployed bundle directory on zFS in step 2, and ensure all resources are enabled. 

## Running the sample


* The Web application is configured with a context root of *jmsweb* so to invoke the servlet to write records to the simple JMS queue specify the test=putQ parameter after the context root for example:
[http://host:port/jmsweb?test=putQ](http://host:port/jmsweb?test=putQ)  

If the test is successful, you will see the following response written to the browser:  
`22/06/2017 16:11:20 3 records have been written to queue:///DEMO.SIMPLEQ`

* To read the records back specify the readQ parameter:
[http://host:port/jmsweb?test=readQ](http://host:port/jmsweb?test=readQ)

* To write records to the MDB queue specify the putmdbQ parameter:
[http://host:port/jmsweb?test=putmdbQ](http://host:port/jmsweb?test=putmdbQ)  

To verify that the MDB has been triggered, you can browse the contents of the CICS TSQ RJMSTSQ using the CICS supplied CEBR transaction as the MDB will write the messages from the MDBQUEUE to this CICS TSQ. 

## Reference
*  IBM Knowledge Center [Deploying message-driven beans to connect to IBM MQ](https://www.ibm.com/support/knowledgecenter/en/was_beta_liberty/com.ibm.websphere.wlp.nd.multiplatform.doc/ae/twlp_dep_msg_mdbwmq.html)
*  MQRC_OBJECT_IN_USE if the MDB tries to get a message from a queue that is not defined as shareable [Defining MDB queues as shareable](http://www-01.ibm.com/support/docview.wss?uid=swg21232930)
*  For further details on the JCICS APIs used in this sample refer to this [developer center article](https://developer.ibm.com/cics/2017/02/27/jcics-the-java-api-for-cics/)


## License
This project is licensed under [Apache License Version 2.0](LICENSE).  



