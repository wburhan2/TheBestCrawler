package com.bestos.thebestcrawler.workers;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Data Gatherer class where all parse threads send information gathered and
 * this class maintains the informational integrity from several threads sending
 * information at once.
 * 
 * @author Michael Carr
 *
 */
public class DataGatherer extends Thread {
	
	/**
	 * The queue that stores data as it is passed in from all parsing threads.
	 */
	Queue<PageData> allData;
	
	/**
	 * The reporter object that does all data calculation, stores totals, and sends
	 * information to the GUI.
	 */
	Reporter theReporter;
	
	/**
	 * Boolean that keeps the thread running as long as it is set to false.
	 */
	boolean stopRunning = false;
	
	/** {@inheritDoc}
	 */
	@Override
	public void run() {
		PageData nextData = null;
		long runningTime;
		while(!stopRunning) {
					
			synchronized (allData) {
				nextData = allData.poll();
			}
			
			if (nextData != null) {
				runningTime = nextData.getEndTime() - nextData.getStartTime();
				theReporter.getDataGathererInfo(nextData.getPagesRetrieved(), nextData.getTotalWords(), nextData.getCurrentURL(), 
											    nextData.getKeywordMap(), runningTime);
			} else {
				
				try {
					sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			
			}
		}
	}
	
	/**
	 * Constructs the data gatherer.
	 */
	public DataGatherer() {
		allData = new LinkedList<PageData>();
		theReporter = new Reporter();
	}
	
	/**
	 * Adds the PageData to the queue in a synchronized manner so that only one
	 * parser can store a PageData object at a time.
	 * 
	 * @param theData All the data gathered by the parser.
	 */
	public void addPageData(PageData theData) {
		synchronized (allData) {
			allData.add(theData);
		}
	}
	
	/**
	 * Sets the stopRunning boolean to true and in turn shuts down the thread.
	 */
	public void shutDown() {
		stopRunning = true;
	}
}
