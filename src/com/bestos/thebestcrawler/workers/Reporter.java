package com.bestos.thebestcrawler.workers;

import java.util.Map;
import java.util.TreeMap;

import com.bestos.thebestcrawler.UserPrefs;
import com.bestos.thebestcrawler.gui.BestOsGUI;

/**
 * 
 * @author Edward Bassan and OS team members
 * 
 */
public class Reporter {
	/**
	 * my pages retrieved
	 */
	private int my_pages_retrieved;
	/**
	 * my_pages average urls
	 */
	private int my_avg_urls;
	/**
	 * my_url
	 */
	private String my_url;

	/**
	 * my average words per page
	 */
	private int my_avg_words_per_page;
	/**
	 * my key word
	 */
	private Map<String, Integer> my_keyword_map;
	/**
	 * key
	 */
	private Map<String, BestOsGUI.keyMap> key;
	/**
	 * my average hit per page
	 */
	private double my_avg_hit_per_page;
	/**
	 * my total hits
	 */
	private double my_total_words;
	/**
	 * my page limit
	 */
	private int my_page_limit;
	/**
	 * my average parse time per page
	 */
	private double my_avg_parse_time_page;
	/**
	 * my total running time
	 */
	private double my_total_running_time;
	/**
     * my running time
     */
	private long my_running_time;

	/**
	 * my total hit
	 */
	private int my_total_hit;
	/**
	 * my_total_pages
	 */
	private int my_total_pages;

    /**
     * Reporter Constructor
     */
	public Reporter() {
		my_keyword_map = new TreeMap<String, Integer>();
		key = new TreeMap<String, BestOsGUI.keyMap>();
	}
	/**
	 * Gets all the information from the DataGatherer
	 * 
	 * @param pgRtd The pages retrieved
	 * @param totalWords The total words
	 * @param currentUrl The current url
	 * @param keywordMap The keywordMap
	 * @param runningTime The runnung time
	 */
	public void getDataGathererInfo(int pgRtd, double totalWords, String currentUrl,
			Map<String, Integer> keywordMap, long runningTime) {
		//long avgHitPerPg, long totalHits, long avgParsePerPg
		
		if (my_total_pages+1 > UserPrefs.getUserPrefs().getMaxPages())
			return;
		
		my_pages_retrieved += pgRtd;
		my_total_words += totalWords;
		my_keyword_map = keywordMap;
		my_url = currentUrl;
		my_running_time += runningTime;
		my_total_pages += 1;
		
		this.setAvgParseTimePage();
		this.setAvgHitPage();
		this.setAvgWordsPage();
	    this.setTotalHits();
	    this.setAvgWordsPage();
	    this.setPageLimit(5000);
	    this.setTotalHits();
	    this.setAvgHitPage();
	    setAvgUrl();
	    setKey(keywordMap);
		BestOsGUI.addQueue(my_url, key, my_avg_words_per_page, my_total_pages, my_avg_urls, my_page_limit, 
				my_avg_parse_time_page, my_running_time, my_total_hit, my_avg_hit_per_page);

	}
   /**
    * Sets key word
    * 
    * @param keywordMap The key word
    */
	public void setKey(Map<String, Integer> keywordMap)
	{
		for(String name : keywordMap.keySet()) {
			BestOsGUI.keyMap oldkeymap = key.get(name);
			BestOsGUI.keyMap map = new BestOsGUI.keyMap();
			map.total_hit =  (oldkeymap != null ? oldkeymap.total_hit : 0) + keywordMap.get(name);
			map.avg_hit = map.total_hit / my_total_pages ;// getAvgHitPage();
			key.put(name, map); 
		}
		
	}
	/**
	 * Sets total pages
	 * 
	 * @param total_page The total Page
	 */
	public void setTotalPage(int total_page){
		my_total_pages = total_page;
	}
	/**
	 * Return total pages
	 * 
	 * @return The total pages
	 */
	public int getTotalPage(){
		return my_total_pages;
	}
	/**
	 * Set pages retrieved
	 * 
	 * @param pages The pages retrieved
	 */
	public void setPagesRetrieved(int pagesRetrieved){
		my_pages_retrieved = pagesRetrieved;
	}
	/**
	 * Set total words
	 * 
	 * @param total_words The total words
	 */
	public void setTotalWords(int total_words){
		my_total_words = total_words;
	}
	/**
	 * Set keyWord
	 * 
	 * @param keyWord The keyword
	 */
	public void setKeyWord(Map<String, Integer> keyWord){
		
		my_keyword_map = keyWord;
	}
	/**
	 * Sets url
	 * 
	 * @param url The url
	 */
	public void setUrl(String url){
		my_url = url;
	}
	public void setRunningTime(long runningTime){
		my_running_time = runningTime;
	}
	
	/**
	 * Sending all the required fields to GUI
	 * 
	 * @param url The URL
	 * @param avg_word Th averagr word
	 * @param pages_retrieved The pages retrieved
	 * @param avg_url The URL
	 * @param page_limit The page limit
	 * @param avg_parse_time The average parse time
	 * @param running_time The running time
	 * @param total_hit The total hit
	 * @param avg_hit The average hit
	 */
	public  void addQueue(String url, int avg_word, int pages_retrieved, 
			int avg_url, int page_limit, double avg_parse_time, long running_time, int total_hit,
			int avg_hit)
	{
		my_avg_urls = avg_url;
		my_page_limit = page_limit;
		my_avg_parse_time_page = avg_parse_time;
		my_running_time = running_time;
		my_total_hit = total_hit;
		my_avg_hit_per_page = avg_hit;

	}
	/**
	 * Return my total hit
	 * 
	 * @return The total hit
	 */
	public double getTotalHit()
	{
		return my_total_hit;
	}

	/**
	 * Set average parse time per page
	 */
	public void setAvgParseTimePage() {
    	my_avg_parse_time_page = (my_running_time / (double)my_total_pages)/1000.00;
	}

	/**
	 * Return average parese per time
	 * 
	 * @return Average parse per time
	 */
	public double getAvgParseTimePage() {

		return my_avg_parse_time_page;
	}

	/**
	 * Set total running time
	 */
	public void setTotalRunningTime(double total_running_time) {
		my_total_running_time = total_running_time;
	}

	/**
	 * Return the total running time
	 * 
	 * @return The total running time
	 */
	public double getTotalRunningTime() {
		return my_total_running_time;
	}

	/**
	 * Set page limit
	 */
	public void setPageLimit(int limit) {
		my_page_limit = limit;
	}

	/**
	 * Return the page limit
	 * 
	 * @return The page limit
	 */
	public long getPageLimit() {
		return my_page_limit;
	}

	/**
	 * Return pages retrieved
	 * 
	 * @return My pages retrieved
	 */
	public long getPagesRetrived() {
		return my_pages_retrieved;
	}

	/**
	 * Set average URL
	 */
	public void setAvgUrl() {
		/*int count = 0;
		for(int i = 0; i < my_keyword_map.size(); i++)
		{
			if(my_keyword_map.containsKey(i))
			{
				count =+ count + 1;
			}
		}
		my_avg_urls = count / my_total_pages;*/
		my_avg_urls = my_pages_retrieved / my_total_pages;
	}

	/**
	 * Return the average URL
	 * 
	 * @return The average URL
	 */
	public long getAvgUrl() {
		return my_avg_urls;

	}

	/**
	 * Set average words per page
	 */
	public void setAvgWordsPage() {
		 my_avg_words_per_page = (int)(my_total_words / my_total_pages);

	}

	/**
	 * Return average words per page
	 * 
	 * @return The average words per page
	 */
	public long getAvgWordsPage() {
		return my_avg_words_per_page;
	}

	/**
	 * Set average hit per page
	 */
	public void setAvgHitPage(){
		
		int count = 0;
		for(int i = 0; i < my_keyword_map.size(); i++)
		{
			if(my_keyword_map.containsValue(i))
			{
				count =+ count + 1;
			}
		}
		my_avg_hit_per_page = count / my_total_pages;

	}
	/**
	 * Return the average hit per page
	 * @return The average hit per page
	 */
	public double getAvgHitPage() {
		return my_avg_hit_per_page;
	}

	/**
	 * Sets the total hits
	 */
	public void setTotalHits() {
		int count = 0;
		for(String s : my_keyword_map.keySet()) {
			count += my_keyword_map.get(s);
			
		}
		my_total_hit += count;
	}
     /**
      * Return total hit
      * 
      * @return The total hit
      */
	public double getTotalHits() {
		return my_total_hit;
	}

	/**
	 * Return keyword and hit
	 * 
	 * @return The keyword
	 */
	public Map<String, Integer> getKeywordMap() {
		return my_keyword_map;
	}

    /**
     * 
     * @param args The arg
     */
	public static void main(final String... args) {
	  
	}
}
