// RobotTxtUtil.java
// RobotTxtUtil
// 
// Author: Michael Morris

package com.bestos.thebestcrawler.utils;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bestos.thebestcrawler.UserPrefs;

/**
 * The robot text class. 
 * 
 * @author Michael Morris
 * @version 5/1/2013
 */
public class RobotTxtUtil {

	private static final String TAG = "RobotTxtUtil";
	
	private static final String PATH_TO_ROBOT_TXT = "/robots.txt";
	
	private static final String TAG_USER_AGENT = "User-agent:";
	private static final String TAG_CRAWL_DELAY = "Crawl-delay:";
	private static final String TAG_ALLOW = "Allow:";
	private static final String TAG_DISALLOW = "Disallow:";
	
	private static final char SPECIAL_TAG_COMMENT = '#';
	private static final String SPECIAL_TAG_ALL_BOTS = "*";
	
	public static final class RobotInstruction {
		private final boolean mCanCrawl;
		private final int mCrawlDelay;
		
		public RobotInstruction(final boolean canCrawl, final int crawlDelay) {
			mCanCrawl = canCrawl;
			mCrawlDelay = crawlDelay;
		}
		
		public final boolean canCrawl() {
			return mCanCrawl;
		}
		
		public final int getDelay() {
			return mCrawlDelay;
		}
	}
	
	private static final class RobotRule {
		public int mCrawlDelay;
		public List<String> mDisallowed;
		public List<String> mAllowed;
		
		public RobotRule() {
			mDisallowed = new ArrayList<String>();
			mAllowed = new ArrayList<String>();
			mCrawlDelay = 0;
		}
		
	}
	
	private static List<String> in_progress;
	private static Map<String, RobotRule> rules;
	
	private static void checkSetup() {
		
		if (in_progress == null)
			in_progress = new ArrayList<String>();
		if (rules == null)
			rules = new HashMap<String, RobotRule>();
		
	}
	
	private static final String getContentFromIn(final InputStream in) throws Exception {
		
		// Try to read all data from the input stream in to a string builder.
		int read_byte;
		final StringBuilder content_builder = new StringBuilder();
		if (in != null)
			while ((read_byte = in.read()) != -1)
				content_builder.append((char)read_byte);
		
		return content_builder.toString().trim();
		
	}
	
	@SuppressWarnings("unused")
	public static final RobotInstruction GetRobotInstructions(final URL url) {
		
		// Make sure our lists exist.
		checkSetup();
		
		RobotInstruction response = null;
		
		// Build the key for the hashmap of rules
		final String url_key = url.getProtocol() + "://" + url.getHost();
		// Build the path for robots.txt
		final String url_to_robots_txt = url_key + PATH_TO_ROBOT_TXT;
		
		// Pause the thread if the url_key is in progress.
		if (in_progress.contains(url_key)) {
			
			UserPrefs.debugTxt(TAG, "pausing thread because robot_txt[" + url_to_robots_txt + "] is currently being parsed by another thread.");
			
			while (in_progress.contains(url_key)) {
				try {
					Thread.sleep(20);
				} catch (InterruptedException ie) { }
			}
			
		}
		
		RobotRule current_rule = null;
		
		// Check to see if a rule already exists
		if (!rules.containsKey(url_key)) {
			
			in_progress.add(url_key);
			
			//UserPrefs.debugTxt(TAG, "no current rule for key[" + url_key + "]; processing...");
			
			
			String content = null;
			try {
				// if rule does not already exist attempt to open a connection.
				final HttpURLConnection con = (HttpURLConnection)(new URL(url_to_robots_txt)).openConnection();
				con.setReadTimeout(2000);
				final InputStream in = con.getInputStream();
				// get content from connection
				content = getContentFromIn(in);			
				in.close();
			} catch (Exception ioe) {
				//UserPrefs.errorTxt(TAG, "exception thrown while attempting to get robot text connection" + ioe);
				content = null;
			}
			
			if (content != null) {
				// split by lines
				final String[] lines = content.split("\\r?\\n");
				
				// Variables for use.
				String current_user_agent = null;
				current_rule = new RobotRule();
				
				for (String str : lines) {
					
					str = str.trim();
					final String[] line_tokens = str.split("\\s+");
					
					// Skip comment-only and blank lines.
					if (str != null && 
						!str.isEmpty() && 
						line_tokens.length >= 2 && 
						line_tokens[0].charAt(0) != SPECIAL_TAG_COMMENT) {
						
						// Check for a "User-Agent:" tag.
						if (line_tokens[0] != null && line_tokens[0].equals(TAG_USER_AGENT)) {
							
							// Check for a wild card (meaning it pertains to us)
							if (line_tokens[1] != null && line_tokens[1].equals(SPECIAL_TAG_ALL_BOTS))
								current_user_agent = SPECIAL_TAG_ALL_BOTS;
							else {
								//UserPrefs.debugTxt(TAG, "found a user-agent[" + line_tokens[1] + "] that does NOT pertain to us.");
								current_user_agent = null;
							}
							
						// Check for a "User-Allow:" tag
						} else if (current_user_agent != null && line_tokens[0] != null && line_tokens[0].equals(TAG_ALLOW)) {
							
							// add the second token to the list of allowed urls.
							if (line_tokens[1] != null)
								current_rule.mAllowed.add(line_tokens[1].trim());
							else {
								//UserPrefs.debugTxt(TAG, "found a null allowed token! error?");
							}
							
						// Check for a "User-Disallow:" tag
						} else if (current_user_agent != null && line_tokens[0] != null && line_tokens[0].equals(TAG_DISALLOW)) {
						
							// add the second token to the list of disallowed urls.
							if (line_tokens[1] != null)
								current_rule.mDisallowed.add(line_tokens[1].trim());
							else {
								//UserPrefs.debugTxt(TAG, "found a null disallowed token! error?");
							}
							
						// Check for a "Crawl-Delay:" tag
						} else if (current_user_agent != null && line_tokens[0] != null && line_tokens[0].equals(TAG_CRAWL_DELAY)) {
							
							// Check to make sure we can parse the second token in to an integer.
							int delay = -1;
							try {
								delay = Integer.parseInt(line_tokens[1]);
							} catch (NumberFormatException nfe) {
								UserPrefs.debugTxt(TAG, "found crawl delay, but second parameter[" + line_tokens[1] + "] could not be parsed to integer");
							}
							
							// Make sure we parsed something
							if (delay >= 0) {
								current_rule.mCrawlDelay = delay;
							} else {
								//UserPrefs.debugTxt(TAG, "crawl delay[" + line_tokens[1] + "] could not be parsed to a valid crawl delay!");
							}
							
						}
						
						// Implied else to skip
						
					}
				}
				
				// Check to make sure something was actually parsed, before adding to rule list.
				if (!current_rule.mAllowed.isEmpty() || !current_rule.mDisallowed.isEmpty()) {
					rules.put(url_key, current_rule);
					//UserPrefs.debugTxt(TAG, "robot rule created and added to rule list.");
				} else {
					//UserPrefs.debugTxt(TAG, "parsed robot.txt did not contain any allows/disallows!");
					// Add a blank allow so that the rule will be saved in the "rules" map.
					if (current_rule == null)
						current_rule = new RobotRule();
					current_rule.mAllowed.add("");
				}
			}
			
			in_progress.remove(url_key);
			
		} else {
			UserPrefs.debugTxt(TAG, "found a rule for key[" + url_key + "]; building robot response...");
			current_rule = rules.get(url_key);
		}
		
		// Build a robot response.
		if (current_rule == null) {
			//UserPrefs.debugTxt(TAG, "returning standard robot response because there was no valid parsed robot.txt");
			response = new RobotInstruction(true, 0);
		} else {
			/*UserPrefs.debugTxt(TAG, "Current Rule: " + url_key);
			UserPrefs.debugTxt(TAG, "Allowed:");
			UserPrefs.debugTxt(TAG, current_rule.mAllowed.toArray(new String[0]));
			UserPrefs.debugTxt(TAG, "Disllowed:");
			UserPrefs.debugTxt(TAG, current_rule.mDisallowed.toArray(new String[0]));
			UserPrefs.debugTxt(TAG, "Crawl-Delay: " + current_rule.mCrawlDelay);
			*/
			response = buildResponse(url.toString(), url_key, current_rule);
		}
		
		return response;
		
	}
	
	private static RobotInstruction buildResponse(final String url, final String key, final RobotRule current_rule) {
		
		boolean match_for_rule = false;
		boolean can_pass = false;
		
		// Check allows first
		for (int i = 0; i < current_rule.mAllowed.size() && !match_for_rule; i++) {
			// Build the current rule string
			final String rule_str = current_rule.mAllowed.get(i);
			
			/*UserPrefs.debugTxt(TAG,
							   "checking allowed rule:",
							   "Allowed='" + rule_str + "'",
							   "    URL='" + url + "'");
			*/
			match_for_rule = isRuleMatch(url, rule_str);
			if (match_for_rule)
				can_pass = true;
			
		}
		
		// Check disallows last
		for (int i = 0; i < current_rule.mDisallowed.size() && !match_for_rule; i++) {
			// Build the current rule string
			final String rule_str = current_rule.mDisallowed.get(i);
			
			/*UserPrefs.debugTxt(TAG,
							   "checking disallowed rule:",
							   "disallowed='" + rule_str + "'",
							   "       URL='" + url + "'");
			*/
			match_for_rule = isRuleMatch(url, rule_str);
			if (match_for_rule)
				can_pass = false;
			
		}
		
		return new RobotInstruction(match_for_rule ? can_pass : true, current_rule.mCrawlDelay);
		
	}
	
	private static boolean isRuleMatch(final String url, String rule) {
		
		rule = rule.trim();
		
		boolean rtn = false;
		
		if (rule != null && rule.isEmpty()) {
			rtn = false;
		
		// Check to see if there is a star at the beginning
		} else if (rule != null && (rule.equals("*") || rule.equals("/*"))) {
			rtn = true;
		} else if (rule != null && rule.startsWith("*") && rule.length() > 1) {
			
			String ending = rule.substring(1);
			if (ending.endsWith("$"))
				ending = ending.substring(0, ending.length() - 2);
			
			if (url.endsWith(ending))
				rtn = true;
		
		} else if (rule != null && rule.endsWith("*")) {
			
			String has_in = rule.substring(0, rule.length() - 2);
			
			if (url.contains(has_in))
				rtn = true;
			
		} else if (rule != null) {
					
			if (url.contains(rule))
				rtn = true;
			
		} else {
			/*UserPrefs.debugTxt(TAG,
							   "could not fully verify rule vs url:",
							   " url = '" + url + "'",
							   "rule = '" + rule + "'");
			*/
		}
		
		return rtn;
		
	}

}
