// PageRetriever.java
// PageRetriever
// 
// Author: Michael Morris

package com.bestos.thebestcrawler.workers;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import com.bestos.thebestcrawler.UserPrefs;
import com.bestos.thebestcrawler.buffers.DoubleStringQueue;
import com.bestos.thebestcrawler.buffers.StringQueue;
import com.bestos.thebestcrawler.utils.RobotTxtUtil;
import com.bestos.thebestcrawler.utils.RobotTxtUtil.RobotInstruction;

public class PageRetriever extends Worker  {

	private static final String[] EXCLUDES = new String[] { "questioneverything.typepad.com" };
	
	/** The name used for debug and error text, of this class. **/
	private static final String TAG = "PageRetriever";
	
	private static Integer pages_retrieved = 0;
	
	/** 
	 * The number of milliseconds to "wait" before trying to
	 * check again for work to do.
	 */
	private static final int MILLIS_TO_YIELD_CPU = 20;
	
	/** 
	 * An array of acceptable content types, per the assignment. This array is
	 * checked against the incoming content types before retrieval of content. 
	 */
	private static final String[] ACCEPTABLE_CONTENT_TYPES = new String[] { "TEXT/PLAIN", "TEXT/HTML" };
	
	/** The maximum number of PageRetriever workers that have been created. **/
	private static int MAX_ID = 0;
	
	/** The flag which denotes if this worker has EVER started working yet. **/
	private boolean started = false;
	
	/** The flag which denotes if this worker is doing real work. **/
	private boolean idle = true;
	
	/** The control flag which controls the running state of the PageRetriever. **/
	private boolean running = true;
	
	/** The id of this PageRetriever. **/
	private final int id;
	
	/** The StringQueue to use for a page buffer. **/
	private final DoubleStringQueue page_buffer;
	
	/** The StringQueue to use for a url buffer. **/
	private final StringQueue url_buffer;
	
	/**
	 * The PRCallbackType is an internally used enumeration
	 * to depict which type of callback to execute.
	 * @author Michael Morris
	 * @version 4/15/2013
	 *
	 */
	private static enum PRCallbackType {
		START, 		// First run of thread.
		SUCCESS,	// Ever successful page retrieval (without error).
		FAIL,		// Upon the FIRST error of every failed attempt of page retrieval.
		IDLE,		// Upon going from a WORKING status to an IDLE status. (not from idle to idle)
		WORKING,    // Upon goingn from an IDLE status to a WORKING status. (not from working to working)
		SHUTDOWN    // Upon shutdown of the thread.
	}
	
	/**
	 * Construct a new PageRetriever.
	 */
	public PageRetriever(final DoubleStringQueue page_buffer, final StringQueue url_buffer) {	
		if (page_buffer == null || url_buffer == null)
			throw new IllegalArgumentException("must specify both page and url buffers; page[" + page_buffer +"], url[" + url_buffer + "]");
		
		this.page_buffer = page_buffer;
		this.url_buffer = url_buffer;
		
		// Set a unique id for this page retriever by simply using the count of all PageRetrievers.
		id = ++MAX_ID;
	}
	
	private boolean isURLBlackListed(final String url) {
		
		boolean rtn = false;
		
		for (String exclude_str : EXCLUDES)
			if (url.toUpperCase().contains(exclude_str.toUpperCase()))				
				rtn = true;
		
		return rtn;
		
	}
	
	private static boolean underMaxPages() {
		boolean rtn = true;
		synchronized (pages_retrieved) {
			if (pages_retrieved >= UserPrefs.getUserPrefs().getMaxPages()) {
				rtn = false;

			}
		}
		return rtn;
		
	}
	
	// TODO: Modulize the run method of PageRetriever!
	@Override
	public void run() {
		
		executeCallback(PRCallbackType.START, null, null);
		UserPrefs.debugTxt(TAG+id, "started up!");
		
		started = true;
		
		while (running) {
			
			// Sanity check to make sure that we have not lost a reference to our url_buffer
			if (url_buffer == null) {
				running = false;
				UserPrefs.debugTxt(TAG+id, "lost reference to url_buffer; cannot continue");
				continue;
			}
			
			// Check to see if there is work to be done
			final String path = url_buffer.getNextPending();
			if (path != null && underMaxPages()) {	
				
				// Check against manually excluded content first!
				if (isURLBlackListed(path)) {
					UserPrefs.errorTxt(TAG+id, "found url from black list [" + path + "]; skip processing...");
					continue;
				}
				
				if (idle = true)
					executeCallback(PRCallbackType.WORKING, null, null);
				
				idle = false;
				boolean error = false;
				
				UserPrefs.debugTxt(TAG+id, "found a url to retrieve.", "url = " + path);
				
				// Open a url and input stream
				URL url = null;
				InputStream in = null;
				
				// Try to create the url reference.
				try {
					url = new URL(path);
				} catch (MalformedURLException ex) {
					executeCallback(PRCallbackType.FAIL, path, ex);
					//UserPrefs.errorTxt(TAG+id, "MalformedURLException thrown while attempting to form URL reference.", "url = " + path, ex.getMessage());
					error = true;
				}
				
				RobotInstruction instruction = null;
				if (!error) {
					// Check the robot instructions for host.
					try {
						instruction = RobotTxtUtil.GetRobotInstructions(url);
					} catch (Exception ex) {
						executeCallback(PRCallbackType.FAIL, path, ex);
						//UserPrefs.errorTxt(TAG+id, "Exception thrown while attempting to find robot instruction", "url = " + path, ex.getMessage());
					}
					
					if (instruction == null || !instruction.canCrawl()) {
						UserPrefs.debugTxt(TAG+id, "robot.txt not found or reported to skip the crawling of this page; skipping...");
						error = true;
					}
					
				}
				
				// Try to open the connection and get an input stream and check content type.
				if (!error) {
					try {
						
						final HttpURLConnection con = (HttpURLConnection)url.openConnection();
						con.setReadTimeout(2000);
						if (con == null || con.getResponseCode() != HttpURLConnection.HTTP_OK) {
							/*UserPrefs.errorTxt(TAG+id,
											   "could not open connection to path[" + path + "]",
											   "(con==null) =" + (con==null),
											   "response =" + con.getResponseCode());*/
							error = true;
						} else if (!isValidContentType(con)) {
							UserPrefs.debugTxt(TAG+id, "invalid content type[" + con.getContentType() + "]");
							error = true;
						} else 
							in = con.getInputStream();
						
					} catch (Exception ioe) {
						executeCallback(PRCallbackType.FAIL, path, ioe);
						//UserPrefs.errorTxt(TAG+id, "Exception thrown while attempting to open an input stream.", "url = " + path, "Message = " + ioe.toString(), "Caused = " + ioe.getCause());
						error = true;
					}
				}
				
				// Try to read all data from the input stream in to a string builder.
				int read_byte;
				final StringBuilder content_builder = new StringBuilder();
				if (!error) {
					try {
						while (!error && (read_byte = in.read()) != -1)
							content_builder.append((char)read_byte);
					} catch (IOException ioe) {
						executeCallback(PRCallbackType.FAIL, path, ioe);
						//UserPrefs.errorTxt(TAG+id, "IOException thrown while attempting read from stream.", "url = " + path, ioe.getMessage());
						error = true;
					} catch (Exception e) {
						executeCallback(PRCallbackType.FAIL, path, e);
						UserPrefs.errorTxt(TAG+id, "General Exception thrown while attempting to read from stream.", "url = " + path, e.getMessage());
					}
				}
				
				// Attempt to close the input stream.
				if (in != null) {
					try {
						in.close();
					} catch (Exception ex) { /* Do nothing because really there is nothing we can do. */ }
				}
				
				// If there were no errors then save the content to the page buffer.
				final String the_content = content_builder.toString().trim();
				if (!error && the_content != null && !the_content.isEmpty()) {
					
					// Sanity check to make sure we have not lost the reference to our page_buffer.
					if (page_buffer == null) {
						running = false;
						error = true;
						// Execute a fail callback without a custom null pointer exception.
						executeCallback(PRCallbackType.FAIL, path, new NullPointerException("page_buffer was unexpectedly null"));
						UserPrefs.errorTxt(TAG+id, "page_buffer was unexpectedly null", "url = " + path);
					} else {
						page_buffer.addPending(path, the_content);
						synchronized (pages_retrieved) {
							pages_retrieved++;
						}
						executeCallback(PRCallbackType.SUCCESS, path, null);
					}
					
				} else if (!error) {
					// Execute a fail callback without an exception.
					executeCallback(PRCallbackType.FAIL, path, null);
				}
				
				UserPrefs.debugTxt(TAG+id, "finished retrieving a url.", "url = " + path, "errors = " + error);
				
			} else {
				
				if (!idle) {
					executeCallback(PRCallbackType.IDLE, null, null);
					UserPrefs.debugTxt(TAG+id, "is idle...");
					if (!underMaxPages())
						UserPrefs.debugTxt(TAG+id, "***************** HIT MAX PAGES TO RETRIEVE ********************");
				}
				
				idle = true;
				
				// If there is NOT, wait
				try {
					sleep(MILLIS_TO_YIELD_CPU);
				} catch (Exception ex) { /* Do nothing since this means that this thread has no way to "wait". */ }
			}
			
		}
		
		executeCallback(PRCallbackType.SHUTDOWN, null, null);
		UserPrefs.debugTxt(TAG+id, "shutting down!");
		
	}
	
	private void executeCallback(final PRCallbackType type, final String url, final Exception e) {
		
		// If there are no listeners to call then why bother.
		if (listeners.isEmpty())
			return;
		
		// execute the correct type of callback.
		switch (type) {
			case START:
				super.executeCallback(WorkerCallbackType.START);
				break;
			case SUCCESS:
				
				// Call all the listeners onSuccess methods.
				for (PageRetrieverListener prl : listeners)
					prl.onSuccess(id, url);
				
				break;
			case FAIL:
				
				// Call all the listeners onFail methods.
				for (PageRetrieverListener prl : listeners)
					prl.onFail(id, url, e);
				
				break;
			case IDLE:
				super.executeCallback(WorkerCallbackType.IDLE);
				break;
			case WORKING:
				super.executeCallback(WorkerCallbackType.WORKING);
				break;
			case SHUTDOWN:
				super.executeCallback(WorkerCallbackType.SHUTDOWN);
				break;
			default:
				// TODO: remove this
				throw new IllegalArgumentException("PageRetriever attempted to execute an invalid callback type[" + type + "]");
		}
		
	}
	
	/**
	 * Check the URLConnection's content type header against the entries within the
	 * ACCEPTABLE_CONTENT_TYPES array. This method should be used to check if the 
	 * url should be retrieved. (Per assignment)
	 * @param con The URLConnection from the URL object.
	 * @return True if the incomming type matches one of the acceptable types.
	 */
	private boolean isValidContentType(final URLConnection con) {
		
		// Generally the content type will contain something like "text/html; charset=UTF-8" but can contain anything really.
		// so split by semi-colon, loop through each (trim) and check content type.
		final String incomming_str = con.getContentType().trim();
		final String[] split = incomming_str.split(";");
		boolean rtn = false;
		
		if (split != null) {
			for (int i = 0; i < split.length && !rtn; i++) {
				split[i] = split[i].trim();
				for (int j = 0; j < ACCEPTABLE_CONTENT_TYPES.length && !rtn; j++)
					if (ACCEPTABLE_CONTENT_TYPES[j].equalsIgnoreCase(split[i]))
						rtn = true;
			}
		}
		
		return rtn;
	}
	
	@Override
	public String getUniqueId() {
		return TAG+id;
	}
	
	/**
	 * Convenience method which goes along with isIdle. Used to check if work as ever been
	 * started. NOTE: A PageRetriever can be alive, not started, and show idle = true, however
	 * when the worker is started the idle flag will most definitely become false.
	 * @return
	 */
	public final boolean hasStarted() {
		return started;
	}
	
	/**
	 * Convenience method for checking if this thread is currently doing useful work. Mainly
	 * used for debugging purposes, but could be used by a custom thread-pool.
	 * @return True if this thread is not currently retrieving a page, false if it is.
	 */
	public final boolean isIdle() {
		return idle;
	}
	
	public final boolean isRunning() {
		return running;
	}
	
	/**
	 * Attempt to gracefully shut down this thread. If, by setting the run flag, it does
	 * not then attempt to interrupt the thread and close down.
	 */
	public void shutdown() {
		if (running && isAlive()) {
			running = false;
			try {
				join(2000);
			} catch (Exception ex) { /* Do nothing since this means that this thread has no way to "wait" */ } 
			if (isAlive()) {
				interrupt();
			}
		}
	}
	
	/**
	 * The PageRetrieverListener class and methods may be subclassed to
	 * recieve notifications of the status changes and method calls of
	 * the page retriever.
	 * @author Michael Morris
	 * @version 4/15/2013
	 *
	 */
	public static class PageRetrieverListener extends WorkerListener {
		
		/**
		 * The onSuccess method is called upon a completed url content retrieval. This
		 * means that a url has been retrieved from the specified url buffer, that a
		 * connection was established, and page contents retrieved (not null/empty) to
		 * be placed in the specified page buffer.
		 * @param id The unique identifier of the page retriever.
		 * @param url The {@link string} url that was retrieved.
		 */
		public void onSuccess(final int id, final String url) { };
		
		/**
		 * The onFail method is called upon most often by a failure to either open a connection to
		 * a url, or read data from the url. This method will also be called if a retrieved url's
		 * content does not conform to the one of the content types specified, or if a general
		 * exception is thrown.
		 * @param id The unique identifier of the page retriever.
		 * @param url The {@link string} url that the attempt was made on.
		 * @param e (Optional) an exception which may have been thrown.
		 */
		public void onFail(final int id, final String url, final Exception e) { };

	}
	

	// ******************** TEST ********************* //
	public static void main(final String... args) {
		
		UserPrefs.getUserPrefs().setDebugMode(true);
		
		final StringQueue url_buffer = new StringQueue();
		final DoubleStringQueue page_buffer = new DoubleStringQueue();
		
		url_buffer.addPending("http://www.w3schools.com/xml/note.xml");
		url_buffer.addPending("https://weblogin.washington.edu/robots.txt");
		url_buffer.addPending("http://www.gljdlkjsdf.com/");
		
		PageRetriever pr = new PageRetriever(page_buffer, url_buffer);
		pr.start();
		
		while (!pr.hasStarted() || !pr.isIdle()) {
			try {
				Thread.sleep(20);
			} catch (Exception ex) { }
		}
		
		pr.shutdown();
		
		while (page_buffer.hasPending()) {
			System.out.println(page_buffer.getNextPending() + "\n\n");
		}
		
	}
	
}
