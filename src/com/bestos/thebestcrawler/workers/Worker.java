// Worker.java
// Worker
// 
// Author: Michael Morris

package com.bestos.thebestcrawler.workers;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import com.bestos.thebestcrawler.workers.PageRetriever.PageRetrieverListener;

public abstract class Worker extends Thread {
	
	/** The listeners registered to this PageRetriever. **/
	protected List<PageRetrieverListener> listeners;
	
	protected Worker() {
		listeners = new ArrayList<PageRetrieverListener>();
	}
	
	public abstract String getUniqueId();
	
	protected static enum WorkerCallbackType {
		START, 		// First run of thread.
		IDLE,		// Upon going from a WORKING status to an IDLE status. (not from idle to idle)
		WORKING,    // Upon goingn from an IDLE status to a WORKING status. (not from working to working)
		SHUTDOWN    // Upon shutdown of the thread.
	}
	
	public static enum WorkerStatus {
		STARTED(Color.blue),
		SHUTDOWN(Color.red),
		IDLE(Color.yellow),
		WORKING(Color.green),
		UNKNOWN(Color.gray);
		
		private final Color my_color;
		
		private WorkerStatus(final Color c) {
			my_color = c;
		}
		
		public final Color getColor() {
			return my_color;
		}
		
	}
	
	/**
	 * (Thread-Safe) Add the specified PageRetrieverListener to this PageRetriever,
	 * which will be notified of method calls.
	 * @param listener The listener to add.
	 */
	public void addListener(final PageRetrieverListener listener) {
		if (!listeners.contains(listener)) {
			synchronized (listeners) {
				listeners.add(listener);
			}
		}
	}
	
	/**
	 * (Thread-Safe) Remove the specified PageRetrieverListener from this PageRetriever.
	 * @param listener The listener to remove.
	 */
	public void removeListener(final PageRetrieverListener listener) {
		if (listener != null) {
			synchronized (listeners) {
				listeners.remove(listener);
			}
		}
	}
	
	/**
	 * (Thread-Safe) Remove ALL registered listeners from this PageRetriever.
	 */
	public void removeAllListeners() {
		synchronized (listeners) {
			listeners.clear();
		}
	}
	
	protected void executeCallback(final WorkerCallbackType type) {
		
		// If there are no listeners to call then why bother.
		if (listeners.isEmpty())
			return;
		
		// execute the correct type of callback.
		switch (type) {
			case START:
				
				// Call all the listeners onStart methods.
				for (PageRetrieverListener prl : listeners)
					prl.onStart(getUniqueId());
				break;
				
			case IDLE:
				
				// Call all the listeners onIdle methods.
				for (PageRetrieverListener prl : listeners)
					prl.onIdle(getUniqueId());
				
				break;
			case WORKING:
				
				// Call all the listeners onWorking methods.
				for (PageRetrieverListener prl : listeners)
					prl.onWorking(getUniqueId());
				break;
				
			case SHUTDOWN:
				
				// Call all the listeners onShutdown methods.
				for (PageRetrieverListener prl : listeners)
					prl.onShutdown(getUniqueId());
				break;
				
		}
		
	}
	
	public static class WorkerListener {
			
		/**
		 * The onStart method is called upon a worker thread's start(..)
		 * method being called. NOTE: Calling a threads run(..) method manually
		 * does NOT execute it on a new thread, but on the existing one. Make sure
		 * you use thread.start().
		 * @param id The unique identifier of the worker that started.
		 */
		public void onStart(final String id) { };
		
		/**
		 * The onIdle method is called upon a worker moving from a Working status to an Idle status
		 * NOTE: this method will NOT be called for consecutive idle statuses (i.e. idle -> idle -> idle = only 1 call to
		 * this method).
		 * @param id The unique identifier of the worker.
		 */
		public void onIdle(final String id) { };
		
		/**
		 * The onWorking method is called upon a worker moving from an Idle status to a Working status
		 * NOTE: This method will NOT be called for consecutive working statuses (i.e. working -> working -> working = only 1 call to
		 * this method).
		 * @param id The unique identifier of the worker.
		 */
		public void onWorking(final String id) { };
		
		/**
		 * The onShutdown method is called directly before a worker thread is going to die.
		 * @param id The unique identifier of the worker.
		 */
		public void onShutdown(final String id) { };

	}
	
}
