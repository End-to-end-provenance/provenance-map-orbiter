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

package edu.harvard.pass.orbiter.gui;

import edu.harvard.pass.*;
import edu.harvard.util.gui.*;
import edu.harvard.util.*;


/**
 * A timeline decorator
 *
 * @author Peter Macko
 */
public class PASSTimelineDecorator extends DefaultTimelineDecorator<PObject> {

	/**
	 * Create an instance of class PASSTimelineDecorator
	 */
	public PASSTimelineDecorator() {
	}

	
	/**
	 * Determine the text to display on the timeline
	 * 
	 * @param event the event
	 * @return the text
	 */
	public String getText(TimelineEvent<PObject> event) {
		
		PObject o = event.getValue();
		if (o == null) return "<null>";
		
		String name = o.getName();
		if (name == null) name = "";
		int rslash = name.lastIndexOf('/');
		if (rslash >= 0) name = name.substring(rslash + 1);
		
		return name;
	}
	
	
	/**
	 * Determine the text to display as a supplemental information next to the main text
	 * 
	 * @param event the event
	 * @return the text
	 */
	public String getSupplementalText(TimelineEvent<PObject> event) {
		
		PObject o = event.getValue();
		if (o == null) return "";
		
		String s = o.getAttribute("ARGV");
		if (s == null) return "";
		
		int k = s.indexOf("[", 3);
		if (k  < 1) return "";
		return s.substring(k);
	}
}
