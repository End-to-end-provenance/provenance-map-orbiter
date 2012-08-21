/*
 * A Collection of Miscellaneous Utilities
 *
 * Copyright 2011
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

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.border.*;

import edu.harvard.util.Utils;


/**
 * A wizard
 * 
 * @author Peter Macko
 */
@SuppressWarnings("serial")
public class Wizard extends JDialog {

	private JPanel cardPanel;
	private CardLayout cardLayout;

	private JButton backButton;
	private JButton nextButton;
	private JButton cancelButton;
	
	protected HashMap<Object, WizardPanel> panelMap;
	protected Vector<WizardPanel> panels;
	protected WizardPanel firstPanel;
	protected WizardPanel currentPanel;

	private EventHandler handler;
	private List<WizardListener> listeners;

	private int returnCode;
	private JFrame parent;


	/**
	 * Constructor of class Wizard
	 * 
	 * @param parent the parent frame
	 * @param name the title of the dialog
	 * @param modal whether the dialog should be modal
	 */
	public Wizard(JFrame parent, String name, boolean modal) {
		super(parent, name, modal);

		// Code inspired by http://java.sun.com/developer/technicalArticles/GUI/swing/wizard/

		listeners = new LinkedList<WizardListener>();
		returnCode = -1;
		this.parent = parent;
		
		panelMap = new HashMap<Object, WizardPanel>();
		panels = new Vector<WizardPanel>();
		firstPanel = null;
		currentPanel = null;

		handler = new EventHandler();


		// Create the main panel

		JPanel buttonPanel = new JPanel();
		Box buttonBox = new Box(BoxLayout.X_AXIS);

		cardPanel = new JPanel();
		cardPanel.setBorder(new EmptyBorder(new Insets(5, 10, 5, 10))); 

		cardLayout = new CardLayout(); 
		cardPanel.setLayout(cardLayout);
		backButton = new JButton("Back");
		nextButton = new JButton("Next");
		cancelButton = new JButton("Cancel");

		backButton.addActionListener(handler);
		nextButton.addActionListener(handler);
		cancelButton.addActionListener(handler);

		backButton.setEnabled(false);

		buttonPanel.setLayout(new BorderLayout());
		buttonPanel.add(new JSeparator(), BorderLayout.NORTH);

		buttonBox.setBorder(new EmptyBorder(new Insets(5, 10, 5, 10))); 
		buttonBox.add(backButton);
		buttonBox.add(Box.createHorizontalStrut(10));
		buttonBox.add(nextButton);
		buttonBox.add(Box.createHorizontalStrut(30));
		buttonBox.add(cancelButton);
		buttonPanel.add(buttonBox, java.awt.BorderLayout.EAST);
		getContentPane().add(buttonPanel, java.awt.BorderLayout.SOUTH);
		getContentPane().add(cardPanel, java.awt.BorderLayout.CENTER);
		
		pack();
	}


	/**
	 * Display the dialog
	 * 
	 * @return the return code
	 */
	public int run() {
		
		if (this.parent == null) Utils.centerWindow(this);
		
		pack();
		setVisible(true);

		return returnCode;
	}


	/**
	 * Close the dialog
	 * 
	 * @param code the return code
	 */
	public void closeWizard(int code) {
		returnCode = code;
		setVisible(false);
	}


	/**
	 * Register a wizard panel
	 *  
	 * @param id the panel ID
	 * @param panel the panel
	 */
	protected void registerWizardPanel(Object id, WizardPanel panel) {
		
		panel.key = id;
		panel.wizard = this;
		
		panels.add(panel);
		panelMap.put(id, panel);
		
		cardPanel.add(panel.getPanel(), panel.toString()); 

		if (firstPanel == null) {
			firstPanel = panel;
			currentPanel = panel;
			setCurrentPanel(panel);
			pack();
		}
	}


	/**
	 * Register a wizard panel
	 *  
	 * @param panel the panel
	 */
	protected void registerWizardPanel(WizardPanel panel) {
		registerWizardPanel(panel, panel);
	}


	/**
	 * Set the current panel
	 * 
	 * @param panel the panel
	 */
	protected void setCurrentPanel(WizardPanel panel) {
		
		currentPanel = panel;
		currentPanel.prepare();
		
		cardLayout.show(cardPanel, currentPanel.toString());
		
		backButton.setEnabled(currentPanel != firstPanel);
		cancelButton.setText("Cancel");
		nextButton.setText("Next");
		cancelButton.setEnabled(true);
	}


	/**
	 * Set the current panel by panel ID
	 * 
	 * @param id the panel ID
	 */
	protected void setCurrentPanelByID(Object id) {
		setCurrentPanel(panelMap.get(id));
	}


	/**
	 * Make the current panel the final panel
	 * 
	 * @param cancelEnabled whether the cancel button should be enabled
	 */
	public void finalWizardPanel(boolean cancelEnabled) {
		nextButton.setText("Finish");
		cancelButton.setEnabled(cancelEnabled);
	}


	/**
	 * Add a wizard listener
	 * 
	 * @param listener the new listener
	 */
	public void addWizardListener(WizardListener listener) {
		this.listeners.add(listener);
	}


	/**
	 * Remove a wizard listener
	 * 
	 * @param listener the listener
	 */
	public void removeWizardListener(WizardListener listener) {
		this.listeners.remove(listener);
	}
	
	
	/**
	 * Determine whether the wizard has panels
	 * 
	 * @return true if the wizard has one or more panels
	 */
	public boolean hasPanels() {
		return !panels.isEmpty();
	}
	
	
	/**
	 * Go to the previous panel
	 */
	public void back() {
		if (!backButton.isEnabled()) return;
		currentPanel.wizardBack();
		for (WizardListener listener : listeners) listener.wizardBack(currentPanel);
	}
	
	
	/**
	 * Go to the next panel, or finish if this is the last panel
	 */
	public void next() {
		if (!nextButton.isEnabled()) return;
		currentPanel.canAdvance = true;
		currentPanel.wizardNext();
		if (!currentPanel.canAdvance) return;
		for (WizardListener listener : listeners) listener.wizardNext(currentPanel);
	}
	
	
	/**
	 * Cancel the dialog
	 */
	public void cancel() {
		if (!cancelButton.isEnabled()) return;
		currentPanel.wizardCancel();
		for (WizardListener listener : listeners) returnCode = listener.wizardCancel(currentPanel);
		setVisible(false);
	}


	/**
	 * The event handler
	 * 
	 * @author Peter Macko
	 */
	private class EventHandler implements ActionListener {

		public EventHandler() {
		}

		public void actionPerformed(ActionEvent event) {

			if (event.getSource() == backButton) back();
			if (event.getSource() == nextButton) next();
			if (event.getSource() == cancelButton) cancel();
		}
	}
}
