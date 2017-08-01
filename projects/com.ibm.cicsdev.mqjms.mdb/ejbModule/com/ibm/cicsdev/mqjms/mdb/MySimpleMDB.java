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

import java.io.IOException;
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
import javax.jms.TextMessage;
import javax.servlet.ServletException;

import com.ibm.cics.server.CicsConditionException;
import com.ibm.cics.server.IsCICS;
import com.ibm.cics.server.TSQ;
import com.ibm.cics.server.Task;

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
		int cicsTask;
		String msgErr;
		String q;
		String TSQname;
		String msgBody;

		// Get CICS task number
		if (IsCICS.getApiStatus() == IsCICS.CICS_REGION_AND_API_ALLOWED) {
			cicsTask = Task.getTask().getTaskNumber();
		} else {
			msgErr = "ERROR: No CICS environment";
			throw new RuntimeException(msgErr);
		}

		try {

			// Get destination to allow us to check the queue name
			q = ((Queue) jmsmsg.getJMSDestination()).getQueueName();
			System.out.println(formatTime() + " Task:" + cicsTask + " Message received from MDB queue " + q);

			// Get message property
			TSQname = jmsmsg.getStringProperty("TSQNAME");

		} catch (JMSRuntimeException | JMSException e) {
			msgErr = " ERROR: JMS error " + cicsTask + " Exception: " + e.getLocalizedMessage();
			throw new RuntimeException(msgErr, e);
		}

		// Cast input message to a text message to read the message body
		try {
			msgBody = ((TextMessage) jmsmsg).getText();
		} catch (ClassCastException | JMSException e) {
			msgErr = " Task:" + cicsTask + " ERROR: invalid JMS message received on queue: " + q;
			throw new RuntimeException(msgErr, e);
		}

		try {
			// Construct the TSQ object based on the name from the message
			// property
			TSQ tsqQ = new TSQ();
			tsqQ.setName(TSQname);

			// Write the message contents back to the CICS TSQ
			tsqQ.writeString(msgBody);
			System.out.println(formatTime() + " Task:" + cicsTask + " Message written to TSQ: " + TSQname);

		} catch (CicsConditionException e) {
			msgErr = " ERROR: CICS Task:" + cicsTask + " failed during TSQ I/O ";
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
