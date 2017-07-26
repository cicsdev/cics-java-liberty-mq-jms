/* Licensed Materials - Property of IBM                                   */
/*                                                                        */
/* SAMPLE                                                                 */
/*                                                                        */
/* (c) Copyright IBM Corp. 2017 All Rights Reserved                       */
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
import javax.jms.JMSProducer;
import javax.jms.JMSRuntimeException;
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

import com.ibm.cics.server.ItemHolder;
import com.ibm.cics.server.TSQ;

/**
 * Servlet implementation of JMS MQ demo
 * 
 */

@WebServlet("/MQJMSDemo")
public class MQJMSDemo extends HttpServlet {

	/** CICS local ccsid */
	private static final String CCSID = System.getProperty("com.ibm.cics.jvmserver.local.ccsid");

	/** timeout for read of queue in ms */
	private static final long TIMEOUT = 100;

	/** loop count for read of queue */
	private static final int READ_COUNT = 10;

	/** name of the JMS connection factory */
	private static final String JMS_CF1 = "jms/qcf1";

	/** name of the standard JMS queue for puts & gets */
	private static final String JMS_SIMPLEQ = "jms/simpleq";

	/** name of the JMS MDB queue */
	private static final String JMS_MDBQ = "jms/mdbq";

	/** JMS connection factory */
	private static ConnectionFactory qcf;

	/** JMS queue for CF tests */
	private static Queue simpleq;

	/** JMS Queue object for MDB test */
	private static Queue mdbq;

	/** Time format */
	private static SimpleDateFormat dfTime = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

	/** CICS TSQ that will be written to */
	private static final String TSQNAME = "RJMSTSQ";

	/** Maximum TSQ read depth */
	private static final int DEPTH_COUNT = 100;

	/**
	 * Servlet initialisation method called only on initialisation of web app
	 *
	 * @param config
	 *            - servlet configuration
	 * @throws ServletException
	 *             - if an error occurs
	 */
	public void init(ServletConfig config) throws ServletException {

		// JNDI lookups for all the JNDI strings in this test
		try {
			InitialContext ctx = new InitialContext();
			qcf = (ConnectionFactory) ctx.lookup(JMS_CF1);
			simpleq = (Queue) ctx.lookup(JMS_SIMPLEQ);
			mdbq = (Queue) ctx.lookup(JMS_MDBQ);

		} catch (NamingException ne) {
			System.out.println(
					formatTime() + " ERROR: " + ne.getMessage() + " on JNDI lookup in servlet initialisation ");
			ne.printStackTrace();
		}
	}

	/**
	 * HTTP GET - to analyse HTTP request and invoke relevant test method
	 *
	 * @param request
	 *            - HTTP request
	 * @param response
	 *            - HTTP response
	 * @throws IOException
	 *             - if an error occurs
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

		// Initialise variables;
		PrintWriter pw = response.getWriter();

		// Get test param from HTTP request to determine with test to run
		String test = request.getParameter("test");
		if (test != null) {

			try {
				if (test.equalsIgnoreCase("readq")) {
					readQ(request, response);
				} else if (test.equalsIgnoreCase("putq")) {
					putQ(request, response);
				} else if (test.equalsIgnoreCase("putmdbq")) {
					putmdbQ(request, response);
				} else if (test.equalsIgnoreCase("readtsq")) {
					readTSQ(request, response);
				} else if (test.isEmpty()) {
					printWeb(pw, "Empty test param specified");
				} else {
					printWeb(pw, "Invalid test param specified: " + test);
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			printWeb(pw, "No test param specified: ");

		}

	}

	/**
	 * Read a JMS queue and construct a HTTP response
	 *
	 * @param request
	 *            - HTTP request
	 * @param response
	 *            - HTTP response
	 * @throws Exception
	 *             - if an error occurs.
	 */
	public void readQ(HttpServletRequest request, HttpServletResponse response) throws Exception {

		// Initialise variables;
		PrintWriter pw = response.getWriter();
		String webmsg;
		JMSConsumer consumer;
		JMSContext context = null;

		// Connect to the QM by creating the JMS context
		try {

			context = qcf.createContext();

		} catch (JMSRuntimeException jre) {
			webmsg = " ERROR: " + jre.getMessage() + " on connection to QM ";
			printWeb(pw, webmsg);
			jre.printStackTrace();
		}

		// Read contents of queue and construct a response
		try {

			if (context != null) {
				consumer = context.createConsumer(simpleq);

				// Read first batch of messages from the queue
				webmsg = "First " + READ_COUNT + " records read from " + simpleq.getQueueName() + " are as follows:";
				printWeb(pw, webmsg);

				TextMessage txtmsg;
				for (int i = 0; i < READ_COUNT; i++) {
					txtmsg = (TextMessage) consumer.receive(TIMEOUT);
					if (txtmsg != null) {
						printWeb(pw, txtmsg.getText());
					}
				}
			}

		} catch (JMSRuntimeException jre) {
			webmsg = "ERROR on JMS receive from " + simpleq.getQueueName() + " jre.getMessage()";
			printWeb(pw, webmsg);
			jre.printStackTrace();
		}
	}

	/**
	 * Write to a JMS queue and construct an HTTP response
	 *
	 * @param request
	 *            - HTTP request
	 * @param response
	 *            - HTTP response
	 * @throws Exception
	 *             - if an error occurs.
	 */
	public void putQ(HttpServletRequest request, HttpServletResponse response) throws Exception {

		// Initialise objects;
		PrintWriter pw = response.getWriter();
		String cicsmsg = formatTime() + " Simple JMS message for CICS";
		String msgStr;

		JMSContext context = null;
		JMSProducer producer;

		// Connect to the QM by creating the JMS context
		try {

			context = qcf.createContext();

		} catch (JMSRuntimeException jre) {
			msgStr = " ERROR: " + jre.getMessage() + " on connection to QM ";
			printWeb(pw, msgStr);
			jre.printStackTrace();

		}

		try {

			// check we have a connection to MQ
			if (context != null) {

				// Create message to be written to the producer then send it
				producer = context.createProducer();
				producer.send(simpleq, cicsmsg);

				// Log message back to browser
				String title = "Message has been written to " + simpleq.getQueueName();
				printWeb(pw, title);
			}

		} catch (JMSRuntimeException jre) {

			msgStr = "ERROR on JMS send to " + simpleq.getQueueName() + " jre.getMessage()";
			printWeb(pw, msgStr);
			jre.printStackTrace();

		}

	}

	/**
	 * Write to a JMS queue used for MDB testing
	 *
	 * @param request
	 *            - HTTP request
	 * @param response
	 *            - HTTP response
	 * @throws IOException
	 */
	public void putmdbQ(HttpServletRequest request, HttpServletResponse response) throws IOException {

		// Initialise variables
		PrintWriter pw = response.getWriter();
		String webmsg;
		String cicsmsg = formatTime() + " Simple MDB message for CICS";
		JMSContext context = null;
		JMSProducer producer;

		// Connect to the QM by creating the JMS context
		try {

			context = qcf.createContext();

		} catch (JMSRuntimeException jre) {
			System.out.println(formatTime() + " ERROR: " + jre.getMessage() + " on connection to QM ");
			jre.printStackTrace();
		}

		try {

			// check we have a connection to MQ
			if (context != null) {

				// Create producer and set the property to define the CICS TSQ
				producer = context.createProducer();
				producer.setProperty("TSQNAME", TSQNAME);

				// Send the message to the MDB queue
				producer.send(mdbq, cicsmsg);
			}

		} catch (JMSRuntimeException jre) {

			webmsg = "ERROR: " + jre.getMessage() + " on put to MDB " + "\n";
			printWeb(pw, webmsg);
			jre.printStackTrace();
			throw (jre);

		}

		webmsg = "Record has been written to MDB queue " + "\n";
		printWeb(pw, webmsg);

	}

	/**
	 * Read the TSQ written to by the MDB and construct a HTTP response
	 *
	 * @param request
	 *            - HTTP request
	 * @param response
	 *            - HTTP response
	 * @throws Exception
	 *             - if an error occurs.
	 */
	public void readTSQ(HttpServletRequest request, HttpServletResponse response) throws Exception {

		// Initialise variables;
		PrintWriter pw = response.getWriter();
		String webmsg;

		// Construct the TSQ object and set the name
		TSQ tsqQ = new TSQ();
		tsqQ.setName(TSQNAME);

		// holder object to receive the data from CICS
		ItemHolder holder = new ItemHolder();

		webmsg = "Records read from TSQ (" + TSQNAME + ") are as follows:";
		printWeb(pw, webmsg);

		for (int i = 1; i <= DEPTH_COUNT; i++) {

			tsqQ.readItem(i, holder);
			byte[] data = holder.getValue();
			String strData = new String(data, CCSID);

			webmsg = "Record[" + i + "] " + strData;
			printWeb(pw, webmsg);

		}

	}

	/**
	 * Write a formatted message with a time stamp insert to the web
	 *
	 * @param pw
	 *            - Print writer
	 * @param msg
	 *            - Input message string
	 */
	public void printWeb(PrintWriter pw, String msg) {

		pw.print(formatTime() + " " + msg + "\n");

	}

	/**
	 * Write a formatted message with a time stamp insert to the web
	 *
	 * @return String - formatted time stamp
	 */
	public String formatTime() {

		String time = dfTime.format(new Date());
		return time;
	}
}