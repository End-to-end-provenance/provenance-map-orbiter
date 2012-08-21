/*
 * Provenance Map Orbiter: A visualization tool for large provenance graphs
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

package edu.harvard.pass.orbiter;

import java.util.Vector;

import edu.harvard.pass.orbiter.gui.*;

import javax.swing.*;


/**
 * The main class of the application
 * 
 * @author Peter Macko
 */
public class Main {
	
	
	/**
	 * Print the program usage information
	 */
	public static void usage() {
		
		System.err.println("Usage: java -jar Orbiter.jar [INPUT_FILE]");
	}

	
	/**
	 * The entry point to the application
	 * 
	 * @param args the command-line arguments
	 */
    public static void main (String args[]) {
		
		
		// Set-up platform-specific properties
		
		try {
			System.setProperty("apple.laf.useScreenMenuBar", "true");
			System.setProperty("com.apple.mrj.application.growbox.intrudes", "false");
			System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Provenance Map Orbiter");
			System.setProperty("com.apple.macos.smallTabs", "true");
			System.setProperty("com.apple.mrj.application.live-resize", "true");
			
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e) {
			// do a silent failover
		}
		
		
		// Parse command-line arguments
		
		String file = null;
		
		if (args.length > 0) {
		
			// Separate arguments into flags and the actual arguments
			
			Vector<String> flags = new Vector<String>();
			Vector<String> optarg = new Vector<String>();
			
			for (int i = 0; i < args.length; i++) {
				
				if (args[i].startsWith("--")) {
					flags.add(args[i]);
				}
				
				else if (args[i].startsWith("-")) {
					for (int j = 1; j < args[i].length(); j++) {
						flags.add("-" + args[i].charAt(j));
					}
				}
				
				else {
					optarg.add(args[i]);
				}
			}
			
			
			// Check flags
			
			for (String s : flags) {
				
				if ("-h".equals(s) || "--help".equals(s)) {
					usage();
					return;
				}
				
				else {
					System.err.println("Invalid argument: " + s);
					System.exit(1);
				}
			}
			
			
			// Check argument count
			
			if (optarg.size() > 1) {
				System.err.println("Too many input files");
				System.exit(1);
			}
			
			file = optarg.isEmpty() ? null : optarg.get(0);
		}
		
		
		// Create the main window and run the application
		
		MainFrame m = new MainFrame();
        m.run(file);
    }

}
