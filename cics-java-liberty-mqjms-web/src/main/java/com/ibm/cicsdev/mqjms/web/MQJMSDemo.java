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
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.JMSRuntimeException;
import javax.jms.JMSConsumer;
import javax.jms.ConnectionFactory;
import javax.jms.Queue;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.cics.server.CicsConditionException;
import com.ibm.cics.server.ItemErrorException;
import com.ibm.cics.server.ItemHolder;
import com.ibm.cics.server.TSQ;

/**
 * Servlet implementation of JMS MQ demo
 * 
 */

@WebServlet("/MQJMSDemo") @SuppressWarnings("serial")
public class MQJMSDemo extends HttpServlet {

	/** CICS local ccsid */
	private static final String CCSID = System.getProperty("com.ibm.cics.jvmserver.local.ccsid");

	/** loop count for read of queue */
	private static final int QM_MAX_DEPTH_COUNT = 10;

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

	/** JMS queue  for MDB test */
	private static Queue mdbq;

	/** Time format */
	private static SimpleDateFormat dfTime = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

	/** CICS TSQ that will be written to */
	private static final String TSQNAME = "RJMSTSQ";

	/** Maximum TSQ read depth */
	private static final int TS_DEPTH_COUNT = 100;

	/**
	 * Servlet initialisation method called only on initialisation of web app
	 *
	 * @param config
	 *            - servlet configuration
	 * @throws ServletException
	 *             - if an error occurs
	 */
	public void init(ServletConfig config) throws ServletException {

		String errmsg;

		// JNDI lookups for all the JNDI strings in this test
		try {
			InitialContext ctx = new InitialContext();
			qcf = (ConnectionFactory) ctx.lookup(JMS_CF1);
			simpleq = (Queue) ctx.lookup(JMS_SIMPLEQ);
			mdbq = (Queue) ctx.lookup(JMS_MDBQ);

		} catch (NamingException ne) {
			errmsg = " ERROR: On JNDI lookup in servlet initialisation ";
			throw new ServletException(errmsg, ne);
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
	 * @throws ServletException
	 *             - if an error occurs.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {

		// Initialise variables;
		PrintWriter pw = response.getWriter();

		// Get test param from HTTP request to determine which test to run
		String test = request.getParameter("test");
		if (test != null) {
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

		} else {
			printWeb(pw, "No test param specified: ");
		}
	}

	/**
	 * Read a JMS queue and construct an HTTP response
	 *
	 * @param request
	 *            - HTTP request
	 * @param response
	 *            - HTTP response
	 * @throws IOException
	 * @throws ServletException
	 *             - if an error occurs.
	 */
	public void readQ(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		// Initialise variables;
		PrintWriter pw = response.getWriter();
		String webmsg;

		// Connect to the queue mgr by creating the JMS context
		// context will be autoclosed due to usage in try/with
		try (JMSContext context = qcf.createContext()) {

			// Create the consumer from the context specifying the queue
			JMSConsumer consumer = context.createConsumer(simpleq);

			// Read messages from the queue until it is empty or we hit READ_COUNT
			webmsg = "Messages read from " + simpleq.getQueueName() + " are as follows:";
			printWeb(pw, webmsg);

			String txtmsg;
			for (int i = 0; i < QM_MAX_DEPTH_COUNT; i++) {
				txtmsg = consumer.receiveBodyNoWait(String.class);
				if (txtmsg != null) {
					webmsg = "Message[" + i + "] " + txtmsg;
					printWeb(pw, webmsg);
				} else {
					break;
				}
			}
		} catch (JMSRuntimeException | JMSException jre) {
			webmsg = "ERROR on JMS receive " + jre.getMessage();
			throw new ServletException(webmsg, jre);
		}
	}

	/**
	 * Write to a JMS queue and construct an HTTP response
	 *
	 * @param request
	 *            - HTTP request
	 * @param response
	 *            - HTTP response
	 * @throws IOException
	 * @throws ServletException
	 *             - if an error occurs.
	 */
	public void putQ(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		// Initialise objects;
		PrintWriter pw = response.getWriter();
		String cicsmsg = formatTime() + " Simple JMS message for CICS";
		String webmsg;

		// Connect to the queue mgr by creating the JMS context
		// context will be autoclosed due to usage in try/with
		try (JMSContext context = qcf.createContext()) {

			// Producer allows message delivery options and headers to be set
			JMSProducer producer = context.createProducer();
			
			// write message to the queue
			producer.send(simpleq, cicsmsg);

			// Log message back to browser
			String title = "Message has been written to " + simpleq.getQueueName();
			printWeb(pw, title);
			
		} catch (JMSException | JMSRuntimeException jre) {
			webmsg = "ERROR on JMS send " + jre.getMessage();
			throw new ServletException(webmsg, jre);
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
	 * @throws ServletException
	 *             - if an error occurs.
	 */
	public void putmdbQ(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

		// Initialise variables
		PrintWriter pw = response.getWriter();
		String webmsg;
		String cicsmsg = formatTime() + " Simple MDB message for CICS";

		// Connect to the queue mgr by creating the JMS context
		// context will be autoclosed due to usage in try/with
		try (JMSContext context = qcf.createContext();)
		{
			// Create producer and set the property to define the CICS TSQ
			JMSProducer producer = context.createProducer();
			producer.setProperty("TSQNAME", TSQNAME);

			// Send the message to the MDB queue
			producer.send(mdbq, cicsmsg);
			

		} catch (JMSRuntimeException jre) {

			webmsg = "ERROR: " + jre.getMessage() + " on put to MDB " + "\n";
			throw new ServletException(webmsg, jre);
		}

		webmsg = "Record has been written to MDB queue " + "\n";
		printWeb(pw, webmsg);

	}

	/**
	 * Read the TSQ written to by the MDB and construct an HTTP response
	 *
	 * @param request
	 *            - HTTP request
	 * @param response
	 *            - HTTP response
	 * @throws IOException
	 * @throws ServletException
	 *             - if an error occurs.
	 */
	public void readTSQ(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		// Initialise variables;
		PrintWriter pw = response.getWriter();
		String webmsg;

		try {
			// Construct the TSQ object and set the name
			TSQ tsqQ = new TSQ();
			tsqQ.setName(TSQNAME);

			// holder object to receive the data from CICS
			ItemHolder holder = new ItemHolder();

			webmsg = "Records read from TSQ (" + TSQNAME + ") are as follows:";
			printWeb(pw, webmsg);

			// Read through the TSQ records until get an ItemError
			for (int i = 1; i < TS_DEPTH_COUNT; i++) {

				tsqQ.readItem(i, holder);
				byte[] data = holder.getValue();
				String strData = new String(data, CCSID);

				webmsg = "Record[" + i + "] " + strData;
				printWeb(pw, webmsg);

			}

		} catch (ItemErrorException e) {
			// Normal condition indicating end of records so ignore this

		} catch (CicsConditionException e) {
			webmsg = "ERROR reading from TSQ (" + TSQNAME + ")";
			throw new ServletException(webmsg, e);

		} catch (UnsupportedEncodingException e) {
			webmsg = "ERROR reading string data with encoding (" + CCSID + ")";
			throw new ServletException(webmsg, e);
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