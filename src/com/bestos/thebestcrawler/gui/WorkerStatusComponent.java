// WorkerStatusComponent.java
// WorkerStatusComponent
// 
// Author: Michael Morris

package com.bestos.thebestcrawler.gui;

import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.JPanel;

import com.bestos.thebestcrawler.workers.PageRetriever.PageRetrieverListener;
import com.bestos.thebestcrawler.workers.Worker;
import com.bestos.thebestcrawler.workers.Worker.WorkerStatus;

/**
 * The JPanel for each thread to be displayed on the main panel.
 * 
 * @author Michael Morris
 * @version 5/1/2013
 */
public class WorkerStatusComponent extends JPanel {

	/** Generated serial version UID. **/
	private static final long serialVersionUID = -4144634227836797022L;
	
	private WorkerStatus my_status;
	
	private JLabel id_lbl;
	
	public WorkerStatusComponent(final Worker worker) {
		
		if (worker == null)
			throw new IllegalArgumentException("cannot create WorkerSTatusComponent with null worker");
		
		// Initialize the status to unknown, until we receive a callback.
		my_status = WorkerStatus.UNKNOWN;
		
		worker.addListener(new WorkerStatusListener());
		
		id_lbl = new JLabel(String.valueOf(worker.getUniqueId()));
		add(id_lbl);
		
		this.setPreferredSize(new Dimension(50, 50));
		this.setMinimumSize(this.getPreferredSize());
		this.setMaximumSize(this.getPreferredSize());
		
		redrawMe();
	
		
	}

	private void redrawMe() {
		setBackground(my_status.getColor());
		repaint();
	}
	
	private class WorkerStatusListener extends PageRetrieverListener {

		@Override
		public void onStart(final String id) {
			my_status = WorkerStatus.STARTED;
			redrawMe();
		};
		
		@Override
		public void onIdle(final String id) {
			my_status = WorkerStatus.IDLE;
			redrawMe();
		};
		
		@Override
		public void onWorking(final String id) {
			my_status = WorkerStatus.WORKING;
			redrawMe();
		};
		
		@Override
		public void onShutdown(final String id) {
			my_status = WorkerStatus.SHUTDOWN;
			redrawMe();
		};
		
	}
	
}