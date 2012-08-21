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

import javax.swing.*;


/**
 * A wizard panel
 * 
 * @author Peter Macko
 */
public class WizardPanel {

	protected JPanel panel;
	protected String title;
	boolean canAdvance;
	Object key;
	Wizard wizard;


	/**
	 * Constructor of class WizardPanel
	 * 
	 * @param panel the wizard panel
	 * @param title the panel title
	 */
	public WizardPanel(JPanel panel, String title) {
		
		this.panel = panel;
		this.title = title;
		this.canAdvance = true;
		this.key = null;
		this.wizard = null;
	}


	/**
	 * Constructor of class WizardPanel
	 * 
	 * @param title the panel title
	 */
	protected WizardPanel(String title) {
		
		this.panel = new JPanel();
		this.title = title;
		this.canAdvance = true;
		this.key = null;
		this.wizard = null;
	}
	
	
	/**
	 * Get the GUI panel
	 * 
	 * @return the GUI panel
	 */
	public JPanel getPanel() {
		return panel;
	}
	
	
	/**
	 * Get the title
	 * 
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}
	
	
	/**
	 * Get the wizard
	 * 
	 * @return the wizard this panel is a part of, or null if none
	 */
	public Wizard getWizard() {
		return wizard;
	}
	
	
	/**
	 * The callback to be used by the handler of wizardNext() to stop
	 * the advance to the next panel in the case of an error
	 */
	protected void cancelNext() {
		canAdvance = false;
	}
	
	
	/**
	 * Prepare the panel to be displayed (this is a callback
	 * for just before the wizard panel is displayed)
	 */
	protected void prepare() {}
	
	
	/**
	 * Callback for when the next button was clicked
	 */
	protected void wizardNext() {}
	
	
	/**
	 * Callback for when the back button was clicked
	 */
	protected void wizardBack() {}
	
	
	/**
	 * Callback for when the cancel button was clicked
	 */
	protected void wizardCancel() {}
}
