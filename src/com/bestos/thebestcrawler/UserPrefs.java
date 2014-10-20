// UserPrefs.java
// UserPrefs
// 
// Author: Michael Morris

package com.bestos.thebestcrawler;

import java.io.PrintStream;

/**
 * The user preference class.
 * 
 * @author Michael Morris
 * @version 4/13/2013
 */
public class UserPrefs {

	private static UserPrefs _instance = null;
	
	public static UserPrefs getUserPrefs() {
		if (_instance == null)
			_instance = new UserPrefs();
		return _instance;
	}
	
	private static final int UPPER_LIMIT_PAGES = 10000; // Capped to 10000 pages to retrieve and parse
	
	private boolean debug_mode;
	private int max_pages;
	
	private UserPrefs() {
		max_pages = UPPER_LIMIT_PAGES;
		debug_mode = false;
	}

	/**
	 * Check whether debug mode is enabled.
	 * @return True if debug mode enabled, false otherwise.
	 */
	public final boolean isDebugMode() {
		return debug_mode;
	}
	
	/**
	 * Set the preferred debug mode. True to enable debug mode.
	 * @param flag True to enable debug mode, false otherwise.
	 */
	public final void setDebugMode(final boolean flag) {
		debug_mode = flag;
	}
	
	/**
	 * Get the maximum number of pages to be parsed.
	 * @return An int containing the number of pages to be parsed max.
	 */
	public final int getMaxPages() {
		return max_pages;
	}
	
	/**
	 * Set the maximum pages to be parsed. There is an internal upper limit of 10000 pages.
	 * @param new_max The new maximum.
	 */
	public final void setMaxPages(final int new_max) {
		max_pages = new_max <= UPPER_LIMIT_PAGES ? new_max : UPPER_LIMIT_PAGES;
	}
	
	/**
	 * Print, using System.out, the message formatted to use the <code>tag</code>
	 * and <code>msg</code> array. <BR><BR>
	 * <em><strong>NOTE: This method will have no effect unless debug mode is enabled.</strong></em>
	 * @param tag The name of the object who owns the text (e.g. System, Info, Listener, etc.).
	 * @param msg One or more Strings containing messages (can be sent as comma-separated list of messages or an array of Strings).
	 * @see #setDebugMode(Boolean)
	 * @see #isDebugMode()
	 * @see #errorTxt(String, String...)
	 * @see #txtOut(PrintStream, String, String...)
	 */
	public static final void debugTxt(final String tag, final String... msg) {
		if (getUserPrefs().debug_mode)
			txtOut(System.out, tag, msg);
	}
	
	/**
	 * Print, using System.err, the message formatted to use the <code>tag</code>
	 * and <code>msg</code> array. 
	 * @param tag The name of the object who owns the text (e.g. System, Info, Listener, etc.).
	 * @param msg One or more Strings containing messages (can be sent as comma-separated list of messages or an array of Strings).
	 * @see #debugTxt(String, String...)
	 * @see #txtOut(PrintStream, String, String...)
	 */
	public static final void errorTxt(final String tag, final String... msg) {
		txtOut(System.err, tag, msg);
	}
	
	/**
	 * Print a message to the specified {@link PrintStream}. The message will be formatted as:<BR>
	 * <BR><p>
	 * [<em>tag</em>] - <em>msg[0]</em><BR>
	 * <em>each additional msg (msg[1..(n-1)]) indented on a separate line.</em>
	 * 
	 * 
	 * @param out The PrintStream for output.
	 * @param tag The name of the object who owns the text (e.g. System, Info, Listener, etc.).
	 * @param msg One or more Strings containing messages (can be sent as comma-separated list of messages or an array of Strings).
	 */
	public static final void txtOut(final PrintStream out, final String tag, final String... msg) {
		if (tag != null && msg != null && !tag.isEmpty() && msg.length > 0) {
			
			final StringBuilder sb = new StringBuilder();
			
			final String tagline_prefix = "[" + tag + "] - ";
			final String indent = genWhiteSpace(tagline_prefix.length());
			sb.append(tagline_prefix);
			for (int i = 0; i < msg.length; i++) {
				if (i > 0)
					sb.append(indent);
				sb.append(msg[i]);
				if (i < (msg.length - 1))
					sb.append('\n');
			}
			
			out.println(sb.toString());
		}
	}
	
	/**
	 * Internal method for generating an indent string.
	 * @param num_spaces The number of spaces to the indent.
	 * @return A String containing the number of spaces specified.
	 */
	private static final String genWhiteSpace(final int num_spaces) {
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < num_spaces; i++)
			sb.append(' ');
		return sb.toString();
	}
	
}
