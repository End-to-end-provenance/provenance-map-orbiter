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

import edu.harvard.util.Utils;


/**
 * A simple wizard with a linear flow of panels
 * 
 * @author Peter Macko
 */
@SuppressWarnings("serial")
public class SimpleWizard extends Wizard {
	
	public static final int OK = 0;
	public static final int CANCEL = -1;
	
	private EventHandler handler;


	/**
	 * Constructor of class SimpleWizard
	 * 
	 * @param parent the parent frame
	 * @param name the title of the dialog
	 * @param modal whether the dialog should be modal
	 */
	public SimpleWizard(JFrame parent, String name, boolean modal) {
		super(parent, name, modal);

		handler = new EventHandler();
		addWizardListener(handler);
	}


	/**
	 * Add a wizard panel
	 *  
	 * @param panel the panel
	 */
	public void addWizardPanel(WizardPanel panel) {
		registerWizardPanel(panels.size(), panel);
	}


	/**
	 * Display the dialog
	 * 
	 * @return the return code
	 */
	public int run() {
		
		if (panels.size() == 1) {
			finalWizardPanel(true);
		}
		
		return super.run();
	}


	/**
	 * The event handler
	 * 
	 * @author Peter Macko
	 */
	private class EventHandler implements WizardListener {

		
		/**
		 * The back button has been pressed
		 * 
		 * @param panel the wizard panel
		 */
		public void wizardBack(WizardPanel panel) {
			
			if (panel == panels.get(0)) {
				return;
			}
			
			Integer i = Utils.cast(panel.key);
			setCurrentPanelByID(i.intValue() - 1);
			
			if (currentPanel == panels.get(panels.size() - 1)) {
				finalWizardPanel(true);
			}
		}
		

		/**
		 * The next button has been pressed
		 * 
		 * @param panel the wizard panel
		 */
		public void wizardNext(WizardPanel panel) {
			
			if (panel == panels.get(panels.size() - 1)) {
				closeWizard(OK);
				return;
			}
			
			Integer i = Utils.cast(panel.key);
			setCurrentPanelByID(i.intValue() + 1);
			
			if (currentPanel == panels.get(panels.size() - 1)) {
				finalWizardPanel(true);
			}
		}

		
		/**
		 * The close button has been pressed
		 * 
		 * @param panel the wizard panel
		 * @return the return code
		 */
		public int wizardCancel(WizardPanel panel) {
			return -1;
		}
	}
}
