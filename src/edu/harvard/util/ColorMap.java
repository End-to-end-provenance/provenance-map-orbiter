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

import java.awt.Color;


/**
 * A color map
 *
 * @author Peter Macko
 */
public class ColorMap {

	private Color[] colorMap;
	private double min;
	private double max;
	private double power;


	/**
	 * Constructor for objects of type ColorMap
	 *
	 * @param colorMin the color associated with the minimum value
	 * @param colorMax the color associated with the maximum value
	 * @param min the minimum expected value
	 * @param max the maximum expected value
	 */
	public ColorMap(Color colorMin, Color colorMax, double min, double max) {
		
		int r1 = colorMin.getRed();
		int g1 = colorMin.getGreen();
		int b1 = colorMin.getBlue();
		int r2 = colorMax.getRed();
		int g2 = colorMax.getGreen();
		int b2 = colorMax.getBlue();

		colorMap = new Color[256];
		for (int i = 0; i < colorMap.length; i++) {
			float f = i / (float) colorMap.length;
			
			int r = Math.round(r1 + f * (r2 - r1));
			int g = Math.round(g1 + f * (g2 - g1));
			int b = Math.round(b1 + f * (b2 - b1));
			
			colorMap[i] = new Color(r, g, b);
		}

		this.min = min;
		this.max = max;
		this.power = 1;
	}


	/**
	 * Get color
	 *
	 * @param value the value between min and max
	 * @return the color
	 */
	public Color getColor(double value) {
		double l = (value - min) / (max - min);
		int i = (int)((colorMap.length - 1) * Math.pow(l, power));
		if (i < 0) i = 0;
		if (i >= colorMap.length) i = colorMap.length - 1;
		return colorMap[i];
	}


	/**
	 * Get color by specifying a value between 0 and 1
	 *
	 * @param value the value between 0 and 1
	 * @return the color
	 */
	public Color getColorByFraction(double value) {
		double l = value;
		int i = (int)((colorMap.length - 1) * Math.pow(l, power));
		if (i < 0) i = 0;
		if (i >= colorMap.length) i = colorMap.length - 1;
		return colorMap[i];
	}
	
	
	/**
	 * Get the minimum value
	 * 
	 * @return the minimum value
	 */
	public double getMin() {
		return min;
	}
	
	
	/**
	 * Get the maximum value
	 * 
	 * @return the maximum value
	 */
	public double getMax() {
		return max;
	}
}
