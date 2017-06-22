package com.ibm.cicsdev.mqjms.mdb;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.TextMessage;

import com.ibm.cics.server.CicsConditionException;
import com.ibm.cics.server.TSQ;
import com.ibm.cics.server.Task;

/**
 * Message-Driven Bean implementation class for: MySimpleMDB
 * 
 * 
 * The <jmsActivationSpec id> attribute must match the format of application_name/module_name/bean_name when this app is deployed  
 */


@MessageDriven(activationConfig = {
		@ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue") })
public class MySimpleMDB implements MessageListener {

	/**	
	 * The CICS TSQ that will be written to	
	 */
	private static final String TSQNAME = "RJMSTSQ";
	
    /**
     * Default constructor
     */
    public MySimpleMDB() {
    }
	

    /**
     * The onMessage() method is invoked by the EJB container when the queue receives a msg
     * Set TransactionAttributeType.REQUIRED to make container managed JTA transaction to control CICS UOW
     *      
     * @param message - The incoming message
     */ 
	@TransactionAttribute(value = TransactionAttributeType.REQUIRED)
    public void onMessage(Message  message) {    	
	
    	try { 
    		
    		// First determine we have been driven from an MDB queue
    		Destination jmsDestination = message.getJMSDestination();
    		if (jmsDestination instanceof Queue) {
    			String q = ((Queue) jmsDestination).getQueueName();
    			System.out.println(formatTime() + " Task:" + Task.getTask().getTaskNumber() + " Message received from MDB queue " + q ) ;
            } else {
            	System.out.println(formatTime() + " ERROR:" + Task.getTask().getTaskNumber() + " Message received from invalid queue") ;
            }
            
 			// Cast input message to a text mesage to read the message 
			String txtMsg = ((TextMessage) message).getText(); 		
       		
 			// Construct the TSQ object
 			TSQ tsqQ = new TSQ();
 			tsqQ.setName(TSQNAME);	 			
			
			// Write the message text to the TSQ
			tsqQ.writeString(txtMsg);
			
		} catch (CicsConditionException e) {
			System.out.println(formatTime() + " ERROR: Task:" + Task.getTask().getTaskNumber() + " Exception: " + e.getLocalizedMessage() ) ;
		} catch (JMSException e) {
			System.out.println(formatTime() + " ERROR: JMS error " + Task.getTask().getTaskNumber() + " Exception: " + e.getLocalizedMessage() ) ;
			e.printStackTrace();			
		} finally {
			System.out.println(formatTime() + " Task:" + Task.getTask().getTaskNumber() + " Message written to TSQ: " + TSQNAME ) ;
		}
    }
    
	public String formatTime() {
		SimpleDateFormat dfTime = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss"); 
		String time = dfTime.format(new Date());
		return time;
	}

}
