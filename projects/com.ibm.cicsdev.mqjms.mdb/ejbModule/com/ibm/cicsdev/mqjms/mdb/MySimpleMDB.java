/* Licensed Materials - Property of IBM                                   */
/*                                                                        */
/* SAMPLE                                                                 */
/*                                                                        */
/* (c) Copyright IBM Corp. 2017 All Rights Reserved                       */
/*                                                                        */
/* US Government Users Restricted Rights - Use, duplication or disclosure */
/* restricted by GSA ADP Schedule Contract with IBM Corp                  */
/*                                                                        */

package com.ibm.cicsdev.mqjms.mdb;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.JMSRuntimeException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Queue;

import com.ibm.cics.server.CicsConditionException;
import com.ibm.cics.server.TSQ;

/**
 * Message-Driven Bean implementation class for MySimpleMDB
 * 
 * 
 * The <jmsActivationSpec id> attribute must match the format of
 * application_name/module_name/bean_name when this app is deployed
 */

@MessageDriven(activationConfig = {
		@ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue") })
public class MySimpleMDB implements MessageListener {

	/** Time format */
	static SimpleDateFormat dfTime = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		
	/** Default CICS TSQ that will be written to */
	private static final String TSQNAME = "RJMSTSQ";

	/**
	 * Default constructor
	 */
	public MySimpleMDB() {
	}

	/**
	 * The onMessage() method is invoked by the EJB container when the queue receives a msg 
	 * Set TransactionAttributeType.REQUIRED to make updates via the CICS UOW controlled
	 * by the EJB container managed transaction with JTA.
	 * 
	 * @param jmsmsg
	 *            - The incoming JMS message
	 *            	 
	 * @throws RuntimeException
	 *             - if an error occurs.
	 */
	@TransactionAttribute(value = TransactionAttributeType.REQUIRED)
	public void onMessage(Message jmsmsg) {

		// Initialise variables
		String msgErr;
		String TSQname;
		String msgBody;
		String q;		

		try {

			// Get JMS destination header 			
			Destination jmsDestination = jmsmsg.getJMSDestination();

			// Get incoming queue name from destination header
			if (jmsDestination != null) {
				q = ((Queue) jmsDestination).getQueueName();
				System.out.println(formatTime() + " JMS message received from MDB " + q);
			} else {
				System.out.println(formatTime() + " MQ message received from MDB ");
			}
		} catch (JMSRuntimeException | JMSException e) {
			msgErr = " ERROR: JMS error getting destination: " + e.getLocalizedMessage();
			throw new RuntimeException(msgErr, e);
		}

		try {
			// Get message property header and use this to set TSQ name if available
			TSQname = jmsmsg.getStringProperty("TSQNAME");
			if (TSQname == null) {
				TSQname = TSQNAME;
			}

		} catch (JMSRuntimeException | JMSException e) {
			msgErr = " ERROR: JMS error getting  msg property: " + e.getLocalizedMessage();
			throw new RuntimeException(msgErr, e);
		}

		// Get msg body from the JMS message object
		try {
			msgBody = jmsmsg.getBody(String.class);
		} catch (JMSRuntimeException | JMSException e) {
			msgErr = " ERROR: JMS error reading mesage body";
			throw new RuntimeException(msgErr, e);
		}

		try {
			// Construct the TSQ object based on the value from the message property
			TSQ tsqQ = new TSQ();
			tsqQ.setName(TSQname);

			// Write the JMS message contents back to the CICS TSQ
			tsqQ.writeString(msgBody);
			System.out.println(formatTime() + " Message written to CICS TSQ: " + TSQname);

		} catch (CicsConditionException e) {
			msgErr = " ERROR: Failure during CICS TSQ I/O ";
			throw new RuntimeException(msgErr);
		}
	}

	/**
	 * @return String formatted time stamp
	 */
	public String formatTime() {

		String time = dfTime.format(new Date());
		return time;
	}

}
