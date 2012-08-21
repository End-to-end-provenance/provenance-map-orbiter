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

package edu.harvard.util;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import javax.swing.*;


/**
 * Miscellaneous utilities
 * 
 * @author Peter Macko
 */
public class Utils {
	
	public static final String OS;
	public static final NumberFormat LONG_DECIMAL_FORMAT = new DecimalFormat("0.000000000000000");
	
	static {
		OS = System.getProperty("os.name");
	}


	/**
	 * Perform an unsafe cast without compiler warnings
	 *
	 * @param x the input object
	 * @return the casted object
	 */
	@SuppressWarnings("unchecked")
	public static <T> T cast(Object x) {
		// Source: http://weblogs.java.net/blog/2007/03/30/getting-rid-unchecked-warnings-casts
		return (T) x;
	}
	
	
	/**
	 * Determine whether the OS is MacOS
	 * 
	 * @return true if the application is running on top of MacOS
	 */
	public static boolean isMacOS() {
		return OS.startsWith("Mac");	
	}
	
	
	/**
	 * Determine whether the OS is Linux
	 * 
	 * @return true if the application is running on top of Linux
	 */
	public static boolean isLinux() {
		return OS.startsWith("Linux");	
	}
	
	
	/**
	 * Determine whether the OS is Windows
	 * 
	 * @return true if the application is running on top of Windows
	 */
	public static boolean isWindows() {
		return OS.startsWith("Win");	
	}
	
	
	/**
	 * Print heap utilization
	 */
	public static void printHeapUtilization() {
		long heapSize = Runtime.getRuntime().totalMemory();
		long heapMaxSize = Runtime.getRuntime().maxMemory();
		System.out.println("Heap: " + (heapSize/1024) + " / " + (heapMaxSize/1024) + " KB  (" + Math.round(10000 * heapSize / heapMaxSize)/100 + "%)");
	}


	/**
	 * Center the window
	 * 
	 * @param frame the window frame to center
	 */
	public static void centerWindow(Window frame) {
		Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
		frame.setLocation((d.width - frame.getWidth()) / 2, (d.height - frame.getHeight()) / 2);
	}

	
	/**
	 * Make a JTextArea to look like a JLabel
	 * 
	 * @param textArea the text area to modify
	 * @return the same text area
	 */
	public static JTextArea makeLabelStyle(JTextArea textArea) {
		
		// From: http://www.coderanch.com/t/338648/GUI/java/Multiple-lines-JLabel
		
		if (textArea == null)
			return null;
		
		textArea.setEditable(false);
		textArea.setCursor(null);
		textArea.setOpaque(false);
		textArea.setFocusable(false);
		
		return textArea;
	}

	
	/**
	 * Overwrite confirmation
	 * 
	 * @param parent the parent component
	 * @param file the file to ask about
	 * @return true if the user wishes to overwrite the file
	 */
	public static boolean shouldOverwrite(Component parent, File file) {
		return JOptionPane.showConfirmDialog(parent, "Are you sure you want to overwrite " + file.getName() + "?", "Warning",
											 JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
	}
	
	
	/**
	 * Check whether the file exists, and if so, ask the user for a confirmation
	 * 
	 * @param parent the parent component
	 * @param file the file to check
	 * @return true if the file does not exist, or if the user wishes to overwrite it if it does
	 */
	public static boolean checkOverwrite(Component parent, File file) {
		if (!file.exists()) return true;
		return shouldOverwrite(parent, file);
	}


	/**
	 * Get the extension of a file.
	 * The code is based on http://java.sun.com/docs/books/tutorial/uiswing/components/examples/Utils.java
	 *
	 * @param f the file
	 * @return the file extension
	 */
	public static String getExtension(File f) {
		String ext = null;
		String s = f.getName();
		int i = s.lastIndexOf('.');
		
		if ((i > 0) && (i < s.length() - 1)) {
			ext = s.substring(i + 1);
		}
		return ext;
	}


	/**
	 * Return a key stroke accelerator for a menu item
	 *
	 * @param keyCode the virtual key
	 * @return the accelerator
	 */
	public static KeyStroke getKeyStrokeForMenu(int keyCode) {
		return KeyStroke.getKeyStroke(keyCode, isMacOS() ? ActionEvent.META_MASK : ActionEvent.CTRL_MASK);
	}


	/**
	 * Run a program
	 *
	 * @param cmd the command to run
	 * @return the process object
	 */
	public static Process exec(String cmd) throws IOException {
		if (isWindows()) {
			String[] cmds = {"cmd", "/c", cmd};
			return Runtime.getRuntime().exec(cmds);
		}
		else {
			String[] cmds = {"/bin/sh", "-c", cmd};
			return Runtime.getRuntime().exec(cmds);
		}
	}


	/**
	 * Run a program, capture the output, wait for completion, and throw
	 * an exception if its execution failed
	 *
	 * @param cmd the command to run
	 * @return the program output
	 * @throws IOException if an error occurred
	 */
	public static String execAndWait(String cmd) throws IOException {

		String line;
		StringBuilder error = new StringBuilder();
		StringBuilder output = new StringBuilder();
		int result = -1;

		try {
			Process p = Utils.exec(cmd);

			BufferedReader es = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			BufferedReader os = new BufferedReader(new InputStreamReader(p.getInputStream()));

			while ((line = os.readLine()) != null) output.append(line);
			while ((line = es.readLine()) != null) error.append(line);

			es.close();
			os.close();

			result = p.waitFor();
		}
		catch (Exception e) {
			result = -1;
			error.append(e.getMessage());
		}

		if (result != 0) {
			throw new IOException(error.toString());
		}
		else {
			return output.toString();
		}
	}


	/**
	 * Determine whether the given external program tool is installed
	 *
	 * @param tool the tool to run
	 * @return true if the tool is installed
	 */
	public static boolean isProgramInstalled(String tool) {
		int r = -1;
		try {
			if (isWindows()) {
				r = exec("where " + tool).waitFor();
			}
			else {
				r = exec("which " + tool).waitFor();
			}
		}
		catch (Exception e) {}
		return r == 0;
	}
	
	
	/**
	 * Trim and remove quotes from a string
	 *
	 * @param str the string
	 * @return the string without quotes
	 */
	public static String removeQuotes(String str) {
		str = str.trim();
		if (str.charAt(0) == '"' || str.charAt(0) == '\'') str = str.substring(1, str.length() - 1);
		return str;
	}


	/**
	 * Create an intersection of two arrays, removing null values and duplicates
	 *
	 * @param a the first array
	 * @param b the second array
	 * @return the intersection
	 */
	public static <T> T[] intersect(T[] a, T[] b) {

		ArrayList<T> l = new ArrayList<T>();

		for (int i = 0; i < a.length; i++) {
			if (a[i] == null) continue;
			for (int j = 0; j < b.length; j++) {
				if (b[j] == null) continue;
				if (a[i].equals(b[j])) {
					if (!l.contains(a[i])) l.add(a[i]);
					break;
				}
			}
		}

		T[] t = Arrays.<T>copyOf(a, l.size());
		for (int i = 0; i < t.length; i++) t[i] = l.get(i);

		return t;
	}


	/**
	 * Determine whether the array contains the given element
	 *
	 * @param a the array
	 * @param e the element
	 * @return true if the element is in the array
	 */
	public static <T> boolean contains(T[] a, T e) {
		
		if (a == null) return false;
		
		for (int i = 0; i < a.length; i++) {
			if (a[i] == e) return true;
			if (e != null && e.equals(a[i])) return true;
		}
		
		return false;
	}
	
	
	/**
	 * Determine whether two collections are equal (order matters)
	 * 
	 * @param a the first set
	 * @param b the second set
	 * @return true if they are equal
	 */
	public static <T> boolean areCollectionsEqual(Collection<T> a, Collection<T> b) {
		
		if (a.size() != b.size()) return false;
		
		Iterator<T> i1 = a.iterator();
		Iterator<T> i2 = b.iterator();
		
		while (true) {
			
			boolean b1 = i1.hasNext();
			boolean b2 = i2.hasNext();
			
			if (b1 != b2) return false;
			if (!b1) return true;
			
			T e1 = i1.next();
			T e2 = i2.next();
			
			if (e1 == e2) continue;
			if (e1 == null || e2 == null) return false;
			if (!e1.equals(e2)) return false;
		}
	}
	
	
	/**
	 * Perform a very basic string escape
	 * 
	 * @param str the string to escape
	 * @return the escaped version of the string
	 */
	public static String escapeSimple(String str) {
		
		String s = str;
		
		if (s.indexOf('\\') >= 0) s = s.replaceAll("\\\\", "\\\\");
		if (s.indexOf('\'') >= 0) s = s.replaceAll("\\\'", "\\\'");
		if (s.indexOf('\"') >= 0) s = s.replaceAll("\\\"", "\\\"");
		if (s.indexOf('\n') >= 0) s = s.replaceAll("\n", "\\n");
		if (s.indexOf('\r') >= 0) s = s.replaceAll("\r", "\\r");
		if (s.indexOf('\b') >= 0) s = s.replaceAll("\b", "\\b");
		
		return s;
	}
	
	
	/**
	 * Convert a glob to a regular expression string
	 * 
	 * @param glob the glob
	 * @return the equivalent regex
	 */
	public static String convertGlobToRegEx(String glob) {
		
		// From: http://stackoverflow.com/questions/1247772/is-there-an-equivalent-of-java-util-regex-for-glob-type-patterns
		
		String line = glob.trim();
		int strLen = line.length();
		StringBuilder sb = new StringBuilder(strLen);
		
		// Remove beginning and ending * globs because they're useless
		if (line.startsWith("*")) {
			line = line.substring(1);
			strLen--;
		}
		if (line.endsWith("*")) {
			line = line.substring(0, strLen - 1);
			strLen--;
		}
		
		boolean escaping = false;
		int inCurlies = 0;
		for (char currentChar : line.toCharArray()) {
			switch (currentChar) {
			case '*':
				if (escaping)
					sb.append("\\*");
				else
					sb.append(".*");
				escaping = false;
				break;
			case '?':
				if (escaping)
					sb.append("\\?");
				else
					sb.append('.');
				escaping = false;
				break;
			case '.':
			case '(':
			case ')':
			case '+':
			case '|':
			case '^':
			case '$':
			case '@':
			case '%':
				sb.append('\\');
				sb.append(currentChar);
				escaping = false;
				break;
			case '\\':
				if (escaping) {
					sb.append("\\\\");
					escaping = false;
				} else
					escaping = true;
				break;
			case '{':
				if (escaping) {
					sb.append("\\{");
				} else {
					sb.append('(');
					inCurlies++;
				}
				escaping = false;
				break;
			case '}':
				if (inCurlies > 0 && !escaping) {
					sb.append(')');
					inCurlies--;
				} else if (escaping)
					sb.append("\\}");
				else
					sb.append("}");
				escaping = false;
				break;
			case ',':
				if (inCurlies > 0 && !escaping) {
					sb.append('|');
				} else if (escaping)
					sb.append("\\,");
				else
					sb.append(",");
				break;
			default:
				escaping = false;
				sb.append(currentChar);
			}
		}
		return sb.toString();
	}
}
