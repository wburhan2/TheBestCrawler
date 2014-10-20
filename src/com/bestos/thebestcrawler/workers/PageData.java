package com.bestos.thebestcrawler.workers;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;


/**
 * The wrapper class for all parsed data.
 * 
 * @author Michael Carr
 *
 */
public class PageData {
	
	/**
	 * The amount of pages retrieved from the current URL.
	 */
	int pagesRetrieved;
	
	/**
	 * The keyword map that keeps track of the amount of hits of a certain keyword.
	 */
	Map<String, Integer> keywordMap;
	
	/**
	 * The start time, end time, and total amount of words, stored as a long.
	 */
	long startTime, endTime, totalWords;
	
	/**
	 * The URL that was parsed for the desired information.
	 */
	String currentURL;
	
	/**
	 * Creates a page data object and initializes the keyword map with all keywords
	 * initialized to the value 0.
	 * 
	 * @param keywordList The keywords to add to the keyword map.
	 */
	public PageData(ArrayList<String> keywordList) {
		keywordMap = new TreeMap<String, Integer>();
		for(String keyword : keywordList) {
			keywordMap.put(keyword, 0);
		}
	}
	
	/**
	 * Increments the desired keyword by one.
	 * 
	 * @param theKeyword The keyword that will be incremented.
	 */
	public void incrementKeyword(String theKeyword) {
		int value = keywordMap.get(theKeyword);
		keywordMap.put(theKeyword, value+1);
	}
	
	/**
	 * Increments the amount of pages retrieved on the current URL.
	 */
	public void incrementPagesRetrieved() {
		pagesRetrieved++;
	}
	
	/**
	 * Sets the desired time variable.
	 * 
	 * @param theTime The time, in milliseconds.
	 * @param test True if for start time, False if for end time.
	 */
	public void setTime(long theTime, boolean test) {
		if(test) startTime = theTime;
		else endTime = theTime;
	}
	
	/**
	 * Sets the total word amount.
	 * 
	 * @param theTotal The amount of words parsed on the current URL.
	 */
	public void setTotalWords(long theTotal) {
		totalWords = theTotal;
	}
	
	/**
	 * Sets the current URL string variable.
	 * 
	 * @param theURL The current URL that was just parsed.
	 */
	public void setDataURL(String theURL) {
		currentURL = theURL;		
	}
	
	/**
	 * Returns the amount of pages retrieved.
	 * 
	 * @return The pages Retrieved.
	 */
	public int getPagesRetrieved() {
		return pagesRetrieved;
	}
	
	/**
	 * Returns the keyword map.
	 * 
	 * @return The keyword map.
	 */
	public Map<String, Integer> getKeywordMap() {
		return keywordMap;
	}
	
	/**
	 * Returns the start time as a long.
	 * 
	 * @return The starting time.
	 */
	public Long getStartTime() {
		return startTime;
	}
	
	/**
	 * Returns the end time as a long.
	 * 
	 * @return The ending time.
	 */
	public Long getEndTime() {
		return endTime;
	}
	
	/**
	 * Returns the total amount of words parsed on the current URL.
	 * 
	 * @return The total amount of words.
	 */
	public Long getTotalWords() {
		return totalWords;
	}
	
	/**
	 * Returns the current URL for all the data in this data object.
	 * 
	 * @return The current URL.
	 */
	public String getCurrentURL() {
		return currentURL;
	}
}
