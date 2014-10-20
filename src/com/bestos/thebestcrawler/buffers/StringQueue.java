// StringQueue.java
// StringQueue
// 
// Author: Michael Morris

package com.bestos.thebestcrawler.buffers;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.bestos.thebestcrawler.UserPrefs;

/**
 * The StringQueue is a thread-safe storage queue for
 * pending urls and pages to be parsed, with callbacks.
 * @author Michael Morris
 * @version 4/15/2013
 *
 */
public class StringQueue {
	
	/**
	 * Internal enumeration used to depict which type of callback should
	 * be used.
	 * @author Michael Morris
	 * @version 4/13/2013
	 *
	 */
	private static enum SQCallbackType {
		RETRIEVAL,
		ADDITION,
		PEEK
	}
	
	/**
	 * The queue used for pending strings.
	 */
	private Queue<String> strings_pending = null;
	
	/**
	 * The list of listeners which will be notified of changes and method calls.
	 */
	private List<StringQueueListener> listeners = null;
	
	/**
	 * Construct a StringQueue.
	 */
	public StringQueue() {	
		strings_pending = new ConcurrentLinkedQueue<String>();
		listeners = new ArrayList<StringQueueListener>();
	}
	
	/**
	 * (Thread-Safe) method of checking whether there are pending strings to retrieve. NOTE: This method
	 * blocks if there is ANY method currently being executed on the internal queue.
	 * @return True if StringQueue has pending strings, false otherwise.
	 */
	public boolean hasPending() {
		boolean rtn = false;
		String str = null;
		synchronized (strings_pending) {
			str = strings_pending.peek();
			rtn = str != null;
		}
		executeCallback(SQCallbackType.PEEK, str, rtn);
		return rtn;
	}
	
	/**
	 * (Thread-Safe) method of retrieving a pending string. NOTE: This method
	 * blocks if there is ANY method currently being executed on the internal queue.
	 * @return The next pending {@link String} object, or null if none pending.
	 */
	public String getNextPending() {
		String rtn = null;
		synchronized (strings_pending) {
			rtn = strings_pending.poll();
		}
		executeCallback(SQCallbackType.RETRIEVAL, rtn, (rtn!=null) );
		return rtn;
	}
	
	/**
	 * (Thread-Safe) method for adding a {@link String} object to the end of the queue. NOTE: This method
	 * blocks if there is ANY method currently being executed on the internal queue.
	 * @param pending_str The String to be added. Note: null or empty values will
	 * not be added.
	 */
	public void addPending(final String pending_str) {
		boolean success = false;
		if (pending_str != null && !pending_str.isEmpty() && !strings_pending.contains(pending_str)) {
			synchronized (strings_pending) {
				strings_pending.add(pending_str);
			}
			success = true;
		}
		executeCallback(SQCallbackType.ADDITION, pending_str, success);
	}
	
	/**
	 * (Thread-Safe) Add the specified StringQueueListener to this StringQueue,
	 * which will be notified of method calls.
	 * @param listener The listener to add.
	 */
	public void addListener(final StringQueueListener listener) {
		if (!listeners.contains(listener)) {
			synchronized (listeners) {
				listeners.add(listener);
			}
		}
	}
	
	/**
	 * (Thread-Safe) Remove the specified StringQueueListener from this StringQueue.
	 * @param listener The listener to remove.
	 */
	public void removeListener(final StringQueueListener listener) {
		if (listener != null) {
			synchronized (listeners) {
				listeners.remove(listener);
			}
		}
	}
	
	/**
	 * (Thread-Safe) Remove ALL registered listeners from this StringQueue.
	 */
	public void removeAllListeners() {
		synchronized (listeners) {
			listeners.clear();
		}
	}
	
	/**
	 * (Thread-Safe) Returns the number of pending strings within this queue.<BR>
	 * <strong><em>NOTE: StringQueue uses an internal queue that requires a sequential
	 * run through the entire linked list to calculate the size, this method WILL block for all
	 * but the accessor, however this method may be VERY expensive for long queues. Use with
	 * caution.</em></strong>
	 * @return The number of pending strings within the queue.
	 */
	public int numPending() {
		synchronized (strings_pending) {
			return strings_pending.size();
		}
	}
	
	/**
	 * Execute the desired callback with the given values, on a separate thread.
	 * @param type The type of callback to initiate.
	 * @param str A string to send with the callback.
	 * @param success A success value to send with the callback.
	 */
	private void executeCallback(final SQCallbackType type, final String str, final boolean success) {
		
		// Don't bother if there is no one listening.
		if (listeners.isEmpty())
			return;
		
		switch (type) {
			case RETRIEVAL:
				
				// execute the onStringRetrieved method of all the callbacks.
				for (StringQueueListener sql : listeners)
					sql.onStringRetrieved(str, success);
						
				break;
			case ADDITION:
				
				// execute the onStringAddition method of all the callbacks.
				for (StringQueueListener sql : listeners)
					sql.onStringAddition(str, success);
				
				break;
			case PEEK:
	
				// execute the onStringPeek method of all the callbacks.
				for (StringQueueListener sql : listeners)
					sql.onStringPeek(success, str);
						
				break;
			default:
				// TODO: remove this
				throw new IllegalArgumentException("StringQueue attempted to execute an invalid callback type[" + type + "]");
		}
		
	}
	
	/**
	 * The StringQueueListener is a very simple listener object which will be called
	 * for each of the thread-safe methods within the StringQueue.<BR><BR>
	 * Main purpose is for debugging and gui component updates.
	 * 
	 * @author Michael Morris
	 * @version 4/15/2013
	 *
	 */
	public static class StringQueueListener {
		
		/**
		 * Called when an attempt is made to retrieve a pending string from the queue.
		 * @param str A clone of the string that would have been retrieved.
		 * @param success A boolean true if the method executed without error. NOTE: An
		 * exception and/or empty queue constitutes a non-successful attempt (false 
		 * would be returned).
		 */
		public void onStringRetrieved(final String str, final boolean success) { }
		
		/**
		 * Called when an attempt is made to add a pending string to the queue.
		 * @param str A clone of the string that would have been added.
		 * @param success A boolean true if the method executed without error. NOTE: An
		 * exception and/or null/emtpy url constitutes a non-successful attempt (false
		 * would be returned).
		 */
		public void onStringAddition(final String str, final boolean success) { }
		
		/**
		 * Called when a call to hasPending(..) method is executed.
		 * @param result The value that was invariably returned by the method.
		 * @param str The string that was "peeked", or null if the queue was emtpy.
		 */
		public void onStringPeek(final boolean result, final String str) { }
		
	}
	
	// *********************** TEST *****************************//
	
	public static void main(final String... args) {
		
		UserPrefs.getUserPrefs().setDebugMode(true);
		
		// Setup buffer and listener.
		final StringQueue ret_buffer = new StringQueue();
		final StringQueueListener ret_buf_listener = new StringQueueListener() { 
			@Override
			public void onStringAddition(final String str, final boolean success) {
				UserPrefs.debugTxt("URLBuffListener", "added url[" + str + "] success=" + success);
			}
			
			@Override
			public void onStringPeek(final boolean result, final String str) {
				UserPrefs.debugTxt("URLBuffListener", "hasPending called with result[" + result + "] nextURL[" + str + "]");
			}
			
			@Override
			public void onStringRetrieved(final String str, final boolean success) {
				UserPrefs.debugTxt("URLBuffListener", "retrieved url[" + str + "]: success=" + success);
			}
		};
		ret_buffer.addListener(ret_buf_listener);
		
		// The urls that will be slowly/quickly added
		final String[] slow_additions = new String[] { "http://www.google.com/", "http://www.yahoo.com/", "http://to.somewhere.com", "http://to.somewhere.com" };
		final String[] fast_additions = new String[] { "http://www.facebook.com/", "http://www.twitter.com/", "http://www.myspace.com/", "http://www.gmail.com" };
		
		// Runnble for the slow thread.
		final Runnable slow_adder = new Runnable() {
			@Override
			public void run() {
				
				for (String str : slow_additions) {
					try {
						Thread.sleep(1000);
					} catch (Exception ex) { }
					ret_buffer.addPending(str);
					UserPrefs.debugTxt("Slow Adder", "added url", "url = " + str, "num in buffer = " + ret_buffer.numPending());
				}
				while (ret_buffer.hasPending()) {
					UserPrefs.debugTxt("Slow Adder", "retrieved url", "url = " + ret_buffer.getNextPending(), "num in buffer = " + ret_buffer.numPending());
					try {
						Thread.sleep(1000);
					} catch (Exception ex) { }
				}
				
			}
		};
		
		// Runnable for the quick thread
		final Runnable fast_adder = new Runnable() {
			@Override
			public void run() {
				
				for (String str : fast_additions) {
					try {
						Thread.sleep(900);
					} catch (Exception ex) { }
					ret_buffer.addPending(str);
					UserPrefs.debugTxt("Fast Adder", "added url", "url = " + str, "num in buffer = " + ret_buffer.numPending());
				}
				while (ret_buffer.hasPending()) {
					UserPrefs.debugTxt("Fast Adder", "retrieved url", "url = " + ret_buffer.getNextPending(), "num in buffer = " + ret_buffer.numPending());					
					try {
						Thread.sleep(900);
					} catch (Exception ex) { }
				}
			}
		};
		
		// Start the threads.
		Thread slow_thread = new Thread(slow_adder);
		Thread fast_thread = new Thread(fast_adder);
		slow_thread.start();
		fast_thread.start();
		
	}
	
}
