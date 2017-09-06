cics-java-liberty-mq-jms
================
Sample JMS Java EE web application demonstrating how to use a message-driven bean (MDB) and a JMS Connection Factory to connect to a remote IBM MQ queue manager using an MQ client mode connection.

## Repository structure

* [projects/](projects) - Eclipse projects suitable for importing into a CICS Explorer environment
* [etc/](etc) - Liberty server configuration file

## Samples overview


* `com.ibm.cicsdev.mqjms.cf.cicsbundle` - CICS bundle project that references the web project for the connection factory sample
* `com.ibm.cicsdev.mqjms.cf.web` - Web project containing the MQJMSDemo servlet that uses a JMS connection factory 
* `com.ibm.cicsdev.mqjms.mdb.cicsbundle` - CICS bundle project that references the EAR project for the MDB sample
* `com.ibm.cicsdev.mqjms.mdb` - EJB project containing the MySimpleMDB that recives a message put to the MDB queue
* `com.ibm.cicsdev.mqjms.mdb.ear` - EAR project referring to the MDB EJB project


## Requirements

* IBM CICS TS V5.3 with APAR [PI58375](http://www.ibm.com/support/docview.wss?uid=swg1PI58375) and [PI67640](http://www.ibm.com/support/docview.wss?uid=swg1PI67640), or CICS TS V5.4
* IBM MQ V8.0 or later on z/OS
* IBM MQ Resource Adapter version 9.0.1. Download from [Fix Central](http://www-01.ibm.com/support/docview.wss?uid=swg21633761) 
* IBM CICS Explorer V5.4 with the IBM CICS SDK for Java EE and Liberty feature installed. Download from [IBM Mainframe DEV](https://developer.ibm.com/mainframe/products/downloads)


## Configuration

The two samples projects can be deployed into a CICS Liberty JVM server. The MySimpleMDB will receive a JMS message written to an MQ queue and then write this message to a CICS TSQ queue.
The MQJMSDemo servlet can be used to get and put messages to the defined queues using an MQ client mode connection to a remote queue manager. 

### Import the samples into Eclipse

1. Import the projects into CICS Explorer using **File > Import > General > Existing projects into workspace**.
1. Resolve the build path errors on the Web and EJB projects using the following menu from each project: **Build Path > Configure Build Path > Libraries > Add Library > CICS with Java EE and Liberty** and select the version of CICS TS for deployment.



### To deploy the MDB sample 


Setup the following resources in the IBM MQ queue manager:

1. MQ channel named `WAS.JMS.SVRCONN`
1. MQ queue named `DEMO.MDBQUEUE`

Note the MDBQUEUE must be defined as shareable to allow usage in the multi-threaded environment in Liberty. In addition it is advisable to set the 
[BackoutThreshold](https://www.ibm.com/support/knowledgecenter/en/SSFKSJ_9.0.0/com.ibm.mq.dev.doc/q032280_.htm) attribute on the MDBQUEUE to a finite value, 
to prevent the MDB being constantly redriven if the MDB fails during the processing of the message 

Setup the following resources in the CICS region

1. Create a Liberty JVM server as described in [Starting a CICS Liberty JVM server in 4 easy steps](https://developer.ibm.com/cics/2015/06/04/starting-a-cics-liberty-jvm-server-in-4-easy-steps/).

1. Download the MQ V9 RAR and then define the RAR location in the Liberty server.xml configuration file. Replace `<path>`.

    ```xml
    <variable name="wmqJmsClient.rar.location" value="<path>wmq.jmsra.rar" />
    ```

1. Ensure the following Liberty features are present in server.xml.

    ```xml
    <feature>cicsts:core-1.0</feature>
    <feature>mdb-3.2</feature>
    <feature>wmqJmsClient-2.0</feature>
    ```

1. Add a definition to the server.xml for the queue required by the sample.

    ```xml    
    <jmsQueue id="jms/mdbq" jndiName="jms/mdbq">
        <properties.wmqJms baseQueueName="DEMO.MDBQUEUE" />
    </jmsQueue>
    ```

1. Add a JMS activation spec to the server.xml for the MDB test. This defines that the MySimpleMDB is invoked when the MDBQUEUE is written to. Replace `<port>` and `<queue_manager>`.

    ```xml
	<jmsActivationSpec id="com.ibm.cicsdev.mqjms.mdb.ear/com.ibm.cicsdev.mqjms.mdb/MySimpleMDB">
		<properties.wmqJms destinationRef="jms/mdbq" destinationType="javax.jms.Queue"
                    channel="WAS.JMS.SVRCONN"
                    hostName="localhost"
                    port="<port>"
                    queueManager="<queue_manager>"			 
                    transportType="CLIENT" />
	</jmsActivationSpec>
    ```

    The jmsActivationSpec id attribute must be in the format of application name/module name/bean name, and is output in the Liberty messages.log in the following CNTR0180I message:
    
    ```
    [8/16/17 15:42:47:611 BST] 0000004b com.ibm.ws.ejbcontainer.runtime.AbstractEJBRuntime           I CNTR0180I: The MySimpleMDB message-driven bean in the com.ibm.cicsdev.mqjms.mdb.jar module of the com.ibm.cicsdev.mqjms.mdb.ear application is bound to the com.ibm.cicsdev.mqjms.mdb.ear/com.ibm.cicsdev.mqjms.mdb/MySimpleMDB activation specification.

    ```

1. Optinally, define and install a CICS TSMODEL resource named `RJMSTSQ` with the attribute `RECOVERY(YES)` if you want to make the MDB test transactional.

1. Optionally, change the name of the JVMSERVER in the .warbundle file in the com.ibm.cicsdev.mqjms.cf.cicsbundle project from DFHWLP to the name of the JVMSERVER resource defined in CICS.

1. Using the CICS Explorer, export the **com.ibm.cicsdev.mqjms.cf.cicsbundle** project to a zFS directory.

1. Define and install a CICS BUNDLE resource definition referring to the deployed bundle directory on zFS in step 8, and ensure all resources are enabled.


Running the sample


* To write records to the MDB queue you can use the IBM MQ client sample program `amqsputc`. To use this sample set the MQSERVER variable to the name of the channel, and TCP/IP hostname and port
  
```> set MQSERVER=WAS.JMS.SVRCONN/TCP/<hostname(port)>```

* Then connect to the MDB queue and write some test data using the amqsputc from the workstation command line, for instance: 

```
> amqsputc DEMO.MDBQUEUE
  Sample AMQSPUT0 start
  target queue is DEMO.MDBQUEUE
  hello from CICS
```

* To verify that the MDB has been triggered, you can read the contents of the TSQ RJMSTSQ using the command `CEBR RJMSTSQ` to view the contents with the CICS
TSQ browsing transaction. The test data should then be visible as follows:

```
  CEBR  TSQ RJMSTSQ          SYSID Z32E REC     1 OF     1    COL     1 OF    47
  ENTER COMMAND ===>                                                            
       **************************  TOP OF QUEUE  *******************************
 00001 29/08/2017 14:18:15 hello from CICS                          
       *************************  BOTTOM OF QUEUE  *****************************
                                                                                
                                                                                
                                                                                
                                                                                
                                                                                
                                                                                
                                                                                
                                                                                
 PF1 : HELP                PF2 : SWITCH HEX/CHAR     PF3 : TERMINATE BROWSE     
 PF4 : VIEW TOP            PF5 : VIEW BOTTOM         PF6 : REPEAT LAST FIND     
 PF7 : SCROLL BACK HALF    PF8 : SCROLL FORWARD HALF PF9 : UNDEFINED            
 PF10: SCROLL BACK FULL    PF11: SCROLL FORWARD FULL PF12: UNDEFINED            
```




### To deploy the Connection Factory sample 

Add the following additional resources:

1. Define an MQ queue named `DEMO.SIMPLEQ`. This should also be defined as shareable to allow useage from multiple servlet threads. 


1. Ensure the following additional Liberty features are present in server.xml.

    ```xml
    <feature>jndi-1.0</feature>
    ```

1. Add a JMS connection factory definition to the server.xml. Replace `<port>` and `<queue_manager>` based on the queue manager configuration.

    ```xml
    <jmsQueueConnectionFactory connectionManagerRef="ConMgrJms" jndiname="jms/qcf1">
        <properties.wmqJms channel="WAS.JMS.SVRCONN" 
                           hostName="localhost" 
                           port="<port>"
                           queueManager="<queue_manager>" 
                           transportType="CLIENT"/>
    </jmsQueueConnectionFactory>
    
    <connectionManager id="ConMgr" maxPoolSize="10"/>
    ```

1. Add a definition to the server.xml for the queue required by the Connection Factory sample.

    ```xml
    <jmsQueue id="jms/simpleq" jndiName="jms/simpleq">
        <properties.wmqJms baseQueueName="DEMO.SIMPLEQ" />
    </jmsQueue>    
    ```


Running the sample


* The Web application is configured with a context root of *jmsweb* so to invoke the servlet to write records to the DEMO.SIMPLEQ specify the test=putq parameter after the context root for example: 
 [http://host:port/jmsweb?test=putq](http://host:port/jmsweb?test=putq)

 If the test is successful, you will see the following response written to the browser: 

    22/06/2017 16:11:20 Message has been written to queue:///DEMO.SIMPLEQ

* To read the records back specify the *readQ* parameter: 
 [http://host:port/jmsweb?test=readq](http://host:port/jmsweb?test=readq)

* You can also use the *readtsq* test parameter on the servlet to read the contents of the CICS TSQ for the MDB test:
  [http://host:port/jmsweb?test=readtsq](http://host:port/jmsweb?test=readtsq)

* You can further use the *putmdbq* test parameter on the servlet to write records to the MDB queue rather than using amqsputc:
 [http://host:port/jmsweb?test=putmdbq](http://host:port/jmsweb?test=putmdbq) 






## References

*  [Liberty and the IBM MQ resource adapter](https://www.ibm.com/support/knowledgecenter/en/SSFKSJ_9.0.0/com.ibm.mq.dev.doc/q120040_.htm) in the IBM MQ Knowledge Center
*  [Deploying message-driven beans to connect to IBM MQ](https://www.ibm.com/support/knowledgecenter/en/SS7K4U_liberty/com.ibm.websphere.wlp.zseries.doc/ae/twlp_dep_msg_mdbwmq.html) in the Liberty Knowledge Center
*  [Defining MDB queues as shareable](http://www.ibm.com/support/docview.wss?uid=swg21232930) for details on the `2042 MQRC_OBJECT_IN_USE when an MDB tries to get a message`
*  [Getting to grips with JCICS](https://developer.ibm.com/cics/2017/02/27/jcics-the-java-api-for-cics/) in the CICS Developer Center

## License

This project is licensed under [Apache License Version 2.0](LICENSE).
