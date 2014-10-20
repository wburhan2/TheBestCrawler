// PageParser.java
// PageParser
// 
// Author: Michael Morris

package com.bestos.thebestcrawler.workers;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

import org.ccil.cowan.tagsoup.jaxp.SAXParserImpl;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import com.bestos.thebestcrawler.UserPrefs;
import com.bestos.thebestcrawler.buffers.DoubleStringQueue;
import com.bestos.thebestcrawler.buffers.StringQueue;
import com.bestos.thebestcrawler.workers.PageRetriever.PageRetrieverListener;

public class PageParser extends Worker {

	private static final String TAG = "PageParser";
	private static int ID_COUNTER = 0;
	
	/** The id of this PageParser. **/
	private final int id;
	
	/** The flag which denotes if this worker has EVER started working yet. **/
	private boolean started = false;
	
	/** The flag which denotes if this worker is doing real work. **/
	private boolean idle = true;
	
	/** The control flag which controls the running state of the PageRetriever. **/
	private boolean running = true;
	
	/** 
	 * The number of milliseconds to "wait" before trying to
	 * check again for work to do.
	 */
	private static final int MILLIS_TO_YIELD_CPU = 20;
	
	/** The StringQueue to use for a page buffer. **/
	private final DoubleStringQueue page_buffer;
	
	/** The StringQueue to use for a url buffer. **/
	private final StringQueue url_buffer;
	
	private static Integer pages_parsed = 0;

	private final ArrayList<String> mKeywords;
		
	private final DataGatherer mGatherer;
	
	/**
	 * The PRCallbackType is an internally used enumeration
	 * to depict which type of callback to execute.
	 * @author Michael Morris
	 * @version 4/15/2013
	 *
	 */
	private static enum PPCallbackType {
		START, 		// First run of thread.
		SUCCESS,	// Ever successful page retrieval (without error).
		FAIL,		// Upon the FIRST error of every failed attempt of page retrieval.
		IDLE,		// Upon going from a WORKING status to an IDLE status. (not from idle to idle)
		WORKING,    // Upon goingn from an IDLE status to a WORKING status. (not from working to working)
		SHUTDOWN    // Upon shutdown of the thread.
	}
	
	/**
	 * Construct a new PageParser.
	 */
	public PageParser(final DoubleStringQueue page_buffer, final StringQueue url_buffer, final ArrayList<String> keywords, DataGatherer gatherer) {	
		if (page_buffer == null || url_buffer == null)
			throw new IllegalArgumentException("must specify both page and url buffers; page[" + page_buffer +"], url[" + url_buffer + "]");
		if (gatherer == null)
			throw new IllegalArgumentException("must specify a DataGatherer");
		
		mGatherer = gatherer;
		this.page_buffer = page_buffer;
		this.url_buffer = url_buffer;
		
		mKeywords = keywords;
		
		// Set a unique id for this page retriever by simply using the count of all PageRetrievers.
		id = ++ID_COUNTER;
		
	}
	
	private static boolean underMaxPages() {
		boolean rtn = true;
		synchronized (pages_parsed) {			
			if (pages_parsed >= UserPrefs.getUserPrefs().getMaxPages())
				rtn = false;
		}
		return rtn;
	}
	
	@Override
	public void run() {

		executeCallback(PPCallbackType.START, null, null);
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
			final String[] current = page_buffer.getNextPending();
			if (underMaxPages() && current != null && current[0] != null && current[1] != null) {
				
				final String path = current[0];
				final String content = current[1];
				final PageData pageData = new PageData(mKeywords);
				pageData.setDataURL(path);
				
				if (idle = true)
					executeCallback(PPCallbackType.WORKING, null, null);
				
				idle = false;
				boolean error = false;
				final long start_time = System.currentTimeMillis();
				pageData.setTime(start_time, true);
				
				// Attempt create an input stream from the content itself.
				InputStream in = null;
				try {
					in = new ByteArrayInputStream(content.getBytes("UTF-8"));
				} catch (UnsupportedEncodingException e) {
					UserPrefs.errorTxt(TAG+id, "ByteArrayInputStream thrown while attempting to aquire stream from content; cannot continue", e.getMessage());
					executeCallback(PPCallbackType.FAIL, path, e);
					error = true;
				}
				
				// Try to aquire a parser instance and parse.
				if (!error && in != null) {
					
					try {
						SAXParserImpl.newInstance(null).parse(
						        in,
						        new DefaultHandler() {
						           
						        	String last_parsed_token = null;
						        	
						        	// Method for retrieving the displayable text
						        	@Override
						        	public void characters(char[] chars, int start, int length) throws SAXException {
						        		
						        		final String[] tokens = ((last_parsed_token != null ? last_parsed_token : "") + String.valueOf(Arrays.copyOfRange(chars, start, start + length)).toUpperCase()).split("\\s+");
						        		
						        			if (tokens != null && tokens.length > 0) {
						        				
						        				for (String token : tokens) {
						        					
						        					token = token.trim();
						        					
						        					if (token != null && !token.isEmpty())
						        						pageData.setTotalWords(pageData.getTotalWords() + 1);
						        					
						        					if (mKeywords != null && !mKeywords.isEmpty()) {
								        				//UserPrefs.debugTxt(TAG+id, token);
							        					last_parsed_token = token;
							        					for (int i = 0; i < mKeywords.size(); i++) {
							        						final String current_keyword = mKeywords.get(i);
								        					if (token.contains(current_keyword.toUpperCase())) {
								        						//UserPrefs.errorTxt(TAG+id, "found hit for keyword[" + current_keyword + "] in text[" + token + "]");
								        						pageData.incrementKeyword(current_keyword);
								        						last_parsed_token = null;
								        					}
									        			}
						        					}
						        				
						        				}						        		
						        			}
						        		
						        	}
						        	
						        	// Method for retrieving our links.
						        	@Override
						        	public void startElement(String uri, String localName,
						                                     String name, Attributes a)
						            {
						            	
						            	// An anchor tag has been found.
						                if (name.equalsIgnoreCase("a")) {
						                	//UserPrefs.errorTxt(TAG+id, "found link[" + a.getValue("href") + "]");
						                	
						                	String url = a.getValue("href");
						                	if (url != null) {
							                	// If it is a valid url, excludes ftp and others
							                	if (!url.contains("mailto:") && (url.startsWith("http://") || url.startsWith("https://") || !url.contains("://" ))) {
							                	
							                		final int strip_index = url.indexOf('#');
							                		// Strip off #
							                		if (strip_index >= 0)
							                			url = url.substring(0, strip_index);
							                		
							                		
							                		if (!url.isEmpty()) {
							                			
							                			
							                			URL url_to_page = null;
							                			try {
							                				url_to_page = new URL(path);
							                			} catch (Exception ex) {
							                				UserPrefs.errorTxt(TAG+id, "Exception thrown while attempting to validate link", ex.getMessage());
							                			}
							                			
							                			// add a path to relative links
							                			if (url_to_page != null && !url.contains("http") ) {
							                				String tmp = url_to_page.getProtocol() + "://" + url_to_page.getHost() + url_to_page.getFile();
							                				tmp = tmp.substring(0, tmp.lastIndexOf('/'));
							                				url = tmp + (url.startsWith("/") ? "" : "/") + url;
							                			}
							                			
							                			// Add the url to the url buffer.
							                			pageData.incrementPagesRetrieved();
							                			url_buffer.addPending(url);
							                			
							                		}
							                	} 
						                	}
						                }
						            }
						        	
						        	@Override
						        	public void warning(SAXParseException saxe) throws SAXException {
						        		super.warning(saxe);
						        		UserPrefs.errorTxt(TAG+id, "SAXParseException thrown", saxe.getMessage());
						        	}
						        }
						    );
					} catch (Exception ex) {
						UserPrefs.errorTxt(TAG+id, "Exception thrown while attempting to parse the content", ex.getMessage());
						executeCallback(PPCallbackType.FAIL, path, ex);
						error = true;
					}
					
				}
				
				final long end_time = System.currentTimeMillis();
				pageData.setTime(end_time, false);
				final long elapsed = end_time - start_time;

				// Send the data to DataGatherer
				mGatherer.addPageData(pageData);
				
				// Update pages parsed
				synchronized (pages_parsed) {
					pages_parsed++;
				}
				
				executeCallback(PPCallbackType.SUCCESS, path, null);
				
			} else {
				
				if (!idle) {
					executeCallback(PPCallbackType.IDLE, null, null);
					UserPrefs.debugTxt(TAG+id, "is idle...");
				}
				
				idle = true;
				
				// If there is NOT, wait
				try {
					sleep(MILLIS_TO_YIELD_CPU);
				} catch (Exception ex) { /* Do nothing since this means that this thread has no way to "wait". */ }
			}
			
			
		}
		
		executeCallback(PPCallbackType.SHUTDOWN, null, null);
		UserPrefs.debugTxt(TAG+id, "shutting down!");
		
	}

	@Override
	public String getUniqueId() {
		return TAG+id;
	}
	
	/**
	 * Convenience method which goes along with isIdle. Used to check if work as ever been
	 * started. NOTE: A PageParser can be alive, not started, and show idle = true, however
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
	
	private void executeCallback(final PPCallbackType type, final String url, final Exception e) {
		
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
				throw new IllegalArgumentException("PageParser attempted to execute an invalid callback type[" + type + "]");
		}
		
	}
	
	public static class PageParserListener extends WorkerListener {
		
		
		public void onSuccess(final int id, final String url) { };
		
		
		public void onFail(final int id, final String url, final Exception e) { };

	}
	
	/*
	
	// ******************** TEST ********************* //
		public static void main(final String... args) {
			
			UserPrefs.getUserPrefs().setDebugMode(true);
			
			final StringQueue url_buffer = new StringQueue();
			final DoubleStringQueue page_buffer = new DoubleStringQueue();
			final ArrayList<String> keywords = new ArrayList<String>();
			final DataGatherer the_gatherer = new DataGatherer();
			
			keywords.add("intelligence");
			keywords.add("artificial");
			keywords.add("agent");
			keywords.add("university");
			keywords.add("research");
			keywords.add("science");
			keywords.add("robot");

			PageRetriever[] retrievers = new PageRetriever[4];
			for (int i = 0; i < retrievers.length; i++) {
				retrievers[i]= new PageRetriever(page_buffer, url_buffer);
				retrievers[i].start();
			}
			PageParser[] parsers = new PageParser[4];
			for (int i = 0; i < parsers.length; i++) {
				parsers[i]= new PageParser(page_buffer, url_buffer, keywords, the_gatherer);
				parsers[i].start();
			}

			final long start_time = System.currentTimeMillis();
			url_buffer.addPending("http://faculty.washington.edu/gmobus/");
			
			try {
				Thread.sleep(2000);
			} catch (Exception ex) { }
			
			while (!areRetrieversIdle(retrievers) || !areParsersIdle(parsers)) {
				try {
					Thread.sleep(10);
				} catch (Exception ex) { }
			}
			
			final long elapsed = System.currentTimeMillis() - start_time;
			
			
			for (int i = 0; i < retrievers.length; i++) {
				retrievers[i].shutdown();
			}
			for (int i = 0; i < parsers.length; i++) {
				parsers[i].shutdown();
			}
			
			System.out.println("TOTAL ALL_DATA: " + the_gatherer.getNumPagesAdded());
			System.out.println("TOTAL URLS IN BUFFER STILL: " + url_buffer.numPending());
			System.out.println("TOTAL PARSE TIME: " + elapsed + " ms");
			
		}
	
		private static boolean areRetrieversIdle(PageRetriever[] retrievers) {
			boolean allidle = true;
			for (int i = 0; i < retrievers.length && allidle; i++) {
				if (!retrievers[i].isIdle())
					allidle = false;
			}
			return allidle;
		}
		
		
		private static boolean areParsersIdle(PageParser[] parsers) {
			
			boolean allidle = true;
			
			for (int i = 0; i < parsers.length && allidle; i++)
				if (!parsers[i].isIdle())
					 allidle = false;
			
			return allidle;
			
		}
		*/
		
}
