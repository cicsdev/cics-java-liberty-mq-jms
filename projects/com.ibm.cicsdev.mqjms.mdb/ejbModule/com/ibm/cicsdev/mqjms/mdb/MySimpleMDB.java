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
import javax.jms.TextMessage;

import com.ibm.cics.server.CicsConditionException;
import com.ibm.cics.server.TSQ;
import com.ibm.cics.server.Task;

/**
 * Message-Driven Bean implementation class for MySimpleMDB
 * 
 * 
 * The <jmsActivationSpec id> attribute must match the format of application_name/module_name/bean_name when this app is deployed  
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
	 * Set TransactionAttributeType.REQUIRED to make container managed JTA transaction to control CICS UOW
	 *      
	 * @param jmsmsg - The incoming JMS message
	 */ 
	@TransactionAttribute(value = TransactionAttributeType.REQUIRED)
	public void onMessage(Message  jmsmsg) { 
		
		// Get CICS task number
		int cicsTask = Task.getTask().getTaskNumber();			

		try { 

			// Get destination to allow us to check the queue name
			Destination jmsDestination = jmsmsg.getJMSDestination();			
			String q = ((Queue) jmsDestination).getQueueName();
			
			System.out.println(formatTime() + " Task:" + cicsTask + " Message received from MDB queue " + q ) ;		

			// Cast input message to a text message to read the message body
			String strMsg;
			try {
				strMsg = ((TextMessage) jmsmsg).getText();
			} catch (ClassCastException e) {
				System.out.println(formatTime() + " Task:" + cicsTask + " ERROR: invalid message received " + q ) ;		
				e.printStackTrace();
				throw (e);
			}					

			// Construct the TSQ object and set the name 
			String TSQname =  jmsmsg.getStringProperty("TSQNAME");
			TSQ tsqQ = new TSQ();
			tsqQ.setName(TSQname);

			// Write the message contents back to the CICS TSQ
			tsqQ.writeString(strMsg);
			System.out.println(formatTime() + " Task:" + cicsTask + " Message written to TSQ: " + TSQname);

		} catch (CicsConditionException e) {
			System.out.println(formatTime() + " ERROR: Task:" + cicsTask + " Exception: " + e.getLocalizedMessage() ) ;
		} catch (JMSRuntimeException|JMSException e) {
			System.out.println(formatTime() + " ERROR: JMS error " + cicsTask + " Exception: " + e.getLocalizedMessage() ) ;
			e.printStackTrace();		
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
