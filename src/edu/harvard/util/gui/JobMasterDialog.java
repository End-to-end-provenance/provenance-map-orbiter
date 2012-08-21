/*
 * A Collection of Miscellaneous Utilities
 *
 * Copyright 2010
 *      The President and Fellows of Harvard College.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the University nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE UNIVERSITY AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE UNIVERSITY OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */

package edu.harvard.util.gui;

import edu.harvard.util.job.*;
import edu.harvard.util.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;


/**
 * The GUI-driven job master
 * 
 * @author Peter Macko
 */
@SuppressWarnings("serial")
public class JobMasterDialog extends JDialog implements JobMaster, JobObserver {
	
	private boolean closeOnError;

	private JPanel panel;
	private JLabel tasksLabel;
	
	private JPanel progressPanel;
	private JProgressBar progress;
	private JPanel buttonPanel;
	private JButton cancelButton;
	
	private EventHandler handler;
	
	private LinkedList<Job> jobs;
	private boolean okay;
	private boolean running;
	private JobException error;
	private Job current;

	
	/**
	 * Constructor of class JobMasterDialog
	 *
	 * @param parent the parent frame
	 * @param title the dialog title
	 */
	public JobMasterDialog(JFrame parent, String title) {
		super(parent, title);

		closeOnError = true;
		
		jobs = new LinkedList<Job>();
		running = false;
		okay = true;
		error = null;
		current = null;
	
		handler = new EventHandler();
		
		setResizable(false);
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		
		
		// Create the main panel
		
		panel = new JPanel();
		panel.setLayout(new BorderLayout());
		getContentPane().add(panel);
		panel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
		
		
		// Create the tasks panel
		
		tasksLabel = new JLabel();
		panel.add(tasksLabel, BorderLayout.CENTER);
		
		
		// Create the progress-bar panel
		
		progressPanel = new JPanel();
		progressPanel.setLayout(new BoxLayout(progressPanel, BoxLayout.PAGE_AXIS));
		panel.add(progressPanel, BorderLayout.SOUTH);
		
		progress = new JProgressBar();
		progress.setIndeterminate(true);
		progressPanel.add(progress);
		
		buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
		progressPanel.add(buttonPanel);

		cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(handler);
		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(cancelButton);
	}
	
	
	/**
	 * Set the tasks label
	 */
	private void updateTasksLabel() {
		String label = "<html><body>";
		int i = 0;
		boolean seenActive = false;
		
		for (Job j : jobs) {
			
			boolean hasFont = false;
			String l = "";
			
			if (j != current || !current.isMinor()) {
				i++;
			}
			
			
			// Set font and determine status
			
			if (!seenActive && j != current) {
				hasFont = true;
				l += "<font color=\"green\">";
			}
			if (j == current) {
				if (!okay) {
					l += "<font color=\"red\">";
				}
				else {
					l += "<font color=\"blue\">";
				}
				l += "<b>";
				seenActive = true;
				hasFont = true;
			}
			
			
			// Add label
			
			l += i;
			l += ". ";
			
			l += j.getName();
			
			
			// Finish
			
			if (j == current) {
				if (!okay) l += " - failed";
				l += "</b>";
			}
			
			if (hasFont) l += "</font>";
			l += "<br/>";
			
			
			// Add to the list
			
			if (j != current || !current.isMinor()) {
				label += l;
			}
		}
		
		label += "</body></html>";
		tasksLabel.setText(label);
	}
	
	
	/**
	 * Run the jobs
	 * 
	 * @throws JobException if one of the jobs failed
	 * @throws JobCanceledException if one of the jobs was canceled
	 */
	public void run() throws JobException {

		okay = true;
		running = false;
		error = null;
		current = null;
		if (jobs.size() < 1) return;

		running = true;
		updateTasksLabel();
		cancelButton.setText("Cancel");
		cancelButton.setEnabled(false);
		
		
		// Compute the window dimensions
		
		int height = 160;
		
		for (Job j : jobs) {
			if (j.isMinor()) continue;
			
			height += tasksLabel.getFont().getSize();
		}
		
		setMinimumSize(new Dimension(320, height));
		setPreferredSize(new Dimension(320, height));
		
		
		// Prepare the window
		
		pack();
		Utils.centerWindow(this);
		setModalityType(ModalityType.DOCUMENT_MODAL);

		
		// Start the tasks
		
		Thread t = new Thread(new TaskThread());
		t.start();
		

		// Display the window. This waits until the tasks are finished
		
		setVisible(true);
		
		
		// Finish
		
		if (error != null) throw error;
	}

	
	/**
	 * Add a job
	 *
	 * @param job the job to add
	 */
	public void add(Job job) {
		jobs.add(job);
	}


	/**
	 * Determine whether the jobs are running
	 *
	 * @return true if the jobs are running
	 */
	public boolean isRunning() {
		return running;
	}
	
	
	/**
	 * Set the range of progress values
	 *
	 * @param min the minimum value
	 * @param max the maximum value
	 */
	public void setRange(int min, int max) {
		progress.setMinimum(min);
		progress.setMaximum(max);
		progress.setValue(min);
		progress.setIndeterminate(false);
	}
	
	
	/**
	 * Set the progress value
	 *
	 * @param value the progress value
	 */
	public void setProgress(int value) {
		progress.setIndeterminate(false);
		progress.setValue(value);
	}
	
	
	/**
	 * Set the progress as indeterminate
	 */
	public void makeIndeterminate() {
		progress.setIndeterminate(true);
	}
	
	
	/**
	 * The task thread
	 */
	private class OneTaskThread implements Runnable {
		
		private Job job;
		private JobException e;
		
		
		/**
		 * Constructor for class OneTaskThread
		 * 
		 * @param job the job to run
		 */
		public OneTaskThread(Job job) {
			this.job = job;
			this.e = null;
		}
		
		
		/**
		 * Run the thread
		 */
		public void run() {
			try {
				e = null;
				job.run();
			}
			catch (JobException e) {
				this.e = e;
			}
		}
		
		
		/**
		 * Get the error
		 * 
		 * @return the error, or null if none
		 */
		public JobException getException() {
			return e;
		}
	}
	
	
	/**
	 * The task thread
	 */
	private class TaskThread implements Runnable {
		
		/**
		 * Constructor for class TaskThread
		 */
		public TaskThread() {
		}
		
		
		/**
		 * Performs the tasks
		 */
		public void run() {
			
			// Wait for the dialog window to appear
			
			while (!isVisible()) Thread.yield();
			

			// Start running the jobs
			
			for (Job j : jobs) {
				current = j;
				
				
				// Update the dialog window
				
				updateTasksLabel();
				progress.setIndeterminate(true);
				cancelButton.setEnabled(j.isCancelable());
				
				repaint();
				try {
					Thread.sleep(50);
				}
				catch (InterruptedException e) {
				}

				
				// Run the thread
				
				j.setJobObserver(JobMasterDialog.this);
				OneTaskThread ott = new OneTaskThread(j);
				Thread t = new Thread(ott);
				t.start();
				
				try {
					t.join();
				}
				catch (Exception e) {
					okay = false;
					error = new JobException(e);
					updateTasksLabel();
					break;
				}
				
				
				// Handle errors
				
				if (ott.getException() != null) {
					okay = false;
					error = ott.getException();
					updateTasksLabel();
					break;
				}
			}
			
			
			// Finished
			
			running = false;
			current = null;
			
			cancelButton.setText("Close");
			cancelButton.setEnabled(true);

			progress.setEnabled(false);
			progress.setIndeterminate(false);
			progress.setMinimum(0);
			progress.setMaximum(1);
			progress.setValue(0);
			
			if (okay || closeOnError) {
				updateTasksLabel();
				setVisible(false);
				dispose();
			}
		}
	}
    
    
    /**
     * The event handler
     * 
     * @author Peter Macko
     */
	private class EventHandler implements ActionListener {
		
		public EventHandler() {
		}
		
		@Override
		public void actionPerformed(ActionEvent event) {
			
			if (event.getSource() == cancelButton) {
				
				if (running) {
					if (current.isCancelable()) current.cancel();
				}
				else {
					setVisible(false);
					dispose();
				}
			}
		}
	}
}
