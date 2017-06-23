/* Licensed Materials - Property of IBM                                   */
/*                                                                        */
/* SAMPLE                                                                 */
/*                                                                        */
/* (c) Copyright IBM Corp. 2016 All Rights Reserved                       */
/*                                                                        */
/* US Government Users Restricted Rights - Use, duplication or disclosure */
/* restricted by GSA ADP Schedule Contract with IBM Corp                  */
/*                                                                        */

package com.ibm.cicsdev.mqjms.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.JMSConsumer;
import javax.jms.ConnectionFactory;
import javax.jms.Queue;
import javax.jms.TextMessage;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation of JMSMQ Demo 
 * 
 */

@WebServlet("/MQJMSDemo")
public class MQJMSDemo extends HttpServlet
{

	/**
	 * tiemout for read of queue 
	 */
	private static final long TIMEOUT = 1000;

	/**
	 * Name of the JMS connection factory
	 */
	private static final String JMS_CF1 = "jms/qcf1";

	/**
	 * name of the standard JMS queue for puts/gets
	 */
	private static final String JMS_SIMPLEQ = "jms/simpleq";

	/**
	 * name of the JMS MDB queue
	 */
	private static final String JMS_MDBQ = "jms/mdbq";

	/**
	 * Connection factory object
	 */
	private ConnectionFactory qcf = null;

	/**
	 * JMS Queue object for the JMS tests
	 */
	private Queue simpleq = null;

	/**
	 * JMS Queue object for the MDB test
	 */

	private Queue mdbq = null;	

	/**
	 * Servlet initialisation method called only on start of web app
	 * 
	 * Used to query and cache JNDI references   
	 *
	 */	
	public void init(ServletConfig config) throws ServletException {

		/* JNDI lookups for all the JNDI strings  in this test */		
		try {
			InitialContext ctx = new InitialContext();
			qcf = (ConnectionFactory) ctx.lookup(JMS_CF1);
			simpleq = (Queue) ctx.lookup(JMS_SIMPLEQ);			
			mdbq = (Queue) ctx.lookup(JMS_MDBQ);

		} catch (NamingException e) {
			e.printStackTrace();
		}

	}

	/**
	 * HTTP GET - to analyse HTTP request and invoke relevant test method 
	 *
	 */	
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		/*
		 *  print writer for output stream		 
		 */
		PrintWriter pw = response.getWriter();

		// Reflect on the input test parm from the query string and call the relevant test method
		String test = request.getParameter("test");
		try {

			getClass().getMethod(test, HttpServletRequest.class, HttpServletResponse.class).invoke(this, request,
					response);			

		} catch (NoSuchMethodException e) {
			printWeb(pw, "No such test: " + test);

		} catch (NullPointerException npe) {
			if (test == null ) {
				printWeb(pw, "ERROR: No test parameter supplied");
			}
			else {
				printWeb(pw, "NPE in requested test: " + test);
				printWeb(pw, npe.toString());
			}
		} catch (Exception e){
			printWeb(pw, "ERROR in requested test: " + test);
			printWeb(pw, e.toString());
		}			
	}

	/**
	 * Example reading msgs from a JMS queue 
	 *
	 * @param request
	 *            HTTP request
	 * @param response
	 *            HTTP response
	 * @throws IOException
	 *             if an error occurs.
	 */	
	public void readQ(HttpServletRequest request, HttpServletResponse response) throws IOException {

		/*
		 *  print writer for output stream		 
		 */
		PrintWriter pw = response.getWriter();		


		// Read contents of queue and construct a response
		try {
			JMSContext context = qcf.createContext();
			JMSConsumer consumer = context.createConsumer(simpleq);

			String title = "MQ records read from " + simpleq.getQueueName() + " are as follows:";		
			printWeb (pw, title);

			TextMessage msg;
			do {
				msg = (TextMessage) consumer.receive(TIMEOUT);
				if ( msg != null) {
					printWeb (pw, msg.getText());
				}
			} while (msg != null);


		} catch (Exception e) {
			e.printStackTrace();
		}			
	}

	/**
	 * Example writing msgs to a JMS queue 
	 *
	 * @param request
	 *            HTTP request
	 * @param response
	 *            HTTP response
	 * @throws IOException
	 *             if an error occurs.
	 */	
	public void putQ(HttpServletRequest request, HttpServletResponse response) throws IOException {

		/*
		 *  print writer for output stream		 
		 */
		PrintWriter pw = response.getWriter();

		/*
		 *  initialise strings		 
		 */
		String cicsmsg = formatTime() + " Simple JMS message for CICS";

		/*
		 *  Create JMS context from CF and then put 3 msgs on the queue	 
		 */
		try {
			JMSContext context = qcf.createContext();
			JMSProducer producer = context.createProducer();
			
			// Create message to be written to the producer then send it 
			TextMessage message = context.createTextMessage(cicsmsg);
			producer.send(simpleq, message);
			
			// Log message back to browser
			String title = "Message has been written to " + simpleq.getQueueName() ;			
			printWeb(pw, title);

		} catch (JMSException je) {
			je.printStackTrace();
		}		

	}	
	/**
	 * Example writing msgs to a queue with an associated MDB
	 *
	 * @param request
	 *            HTTP request
	 * @param response
	 *            HTTP response
	 * @throws IOException
	 *             if an error occurs.
	 */	
	public void putmdbQ(HttpServletRequest request, HttpServletResponse response) throws IOException {


		/*
		 *  simple print writer for output stream		 
		 */
		PrintWriter pw = response.getWriter();

		/*
		 * Initialise strings
		 */		
		String webmsg = null;
		String cicsmsg = formatTime() + " Simple MDB message for CICS";

		// Create context and producer from connection factory
		JMSContext context = qcf.createContext();
		JMSProducer producer = context.createProducer();

		// Create message to be written to the producer then send it 
		TextMessage message = context.createTextMessage(cicsmsg);
		producer.setProperty("TSQNAME", "MDBQ");
		producer.send(mdbq, message);

		try {
			webmsg = "Record has been written to MDB " + mdbq.getQueueName() + "\n";
		} catch (JMSException e) {

			e.printStackTrace();
		}
		printWeb(pw, webmsg);		

	}

	/**
	 * @param pw
              Print writer
	 * @param msg 
	 *        Input message string
	 *        
	 *        Send a formatted message with a time stamp insert to the print writer
	 */
	public void printWeb(PrintWriter pw, String msg) {

		pw.print(formatTime() + " " + msg + "\n");		

	}

	/**
	 * @return 
	 *     String formatted time stamp
	 */
	public String formatTime() {
		SimpleDateFormat dfTime = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss"); 
		String time = dfTime.format(new Date());
		return time;
	}
}