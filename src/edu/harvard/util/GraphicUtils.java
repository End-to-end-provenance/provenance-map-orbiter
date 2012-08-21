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

import java.awt.geom.*;
import java.awt.image.*;
import java.awt.*;
import java.io.*;
import javax.imageio.ImageIO;
import javax.swing.*;


/**
 * Miscellaneous utilities for manipulating graphics
 * 
 * @author Peter Macko
 */
public class GraphicUtils {
	
	private static final String[] SUPPORTED_IMAGE_FORMATS = { "png", "jpg", "jpeg", "gif" };
	private static final String[] SUPPORTED_IMAGE_LOSSLESS_FORMATS = { "png" };
	private static FileExtensionFilter filterImage;
	private static FileExtensionFilter filterImageLossless;
    private static File lastChosenImageFile = null;

	private static int yCor(int len, double dir) {return (int)(len * Math.cos(dir));}
	private static int xCor(int len, double dir) {return (int)(len * Math.sin(dir));}


	/**
	 * Initialize
	 */
	static {
		filterImage = new FileExtensionFilter("Supported Image Formats (*.png, *.jpg, *.jpeg, *.gif)", SUPPORTED_IMAGE_FORMATS);
		filterImageLossless = new FileExtensionFilter("Lossless Image Formats (*.png)", SUPPORTED_IMAGE_LOSSLESS_FORMATS);
	}
	
	
	/**
	 * The text justification
	 * 
	 * @author Peter Macko
	 */
	public static enum TextJustify {
		LEFT, RIGHT, CENTER
	}

	
	/**
	 * Draw an arrow head
	 *
	 * @param g the graphics object
	 * @param x1 the source X coordinate
	 * @param y1 the source Y coordinate
	 * @param x2 the target X coordinate
	 * @param y2 the target Y coordinate
	 * @param size the head size
	 * @param filled whether to fill the arrowhead
	 */
	public static void drawArrowHead(Graphics g, int x1, int y1, int x2, int y2, double size, boolean filled) {
		
		// Original source code: http://forums.sun.com/thread.jspa?threadID=378460
		
		Polygon p = new Polygon();
		double aDir = Math.atan2(x1 - x2, y1 - y2);
		int i1 = (int) size;
		//int i2 =  6 + (int) stroke;
		
		p.addPoint(x2, y2);
		p.addPoint(x2 + xCor(i1, aDir+.5), y2 + yCor(i1, aDir+.5));
		//p.addPoint(x2 + xCor(i2, aDir   ), y2 + yCor(i2, aDir   ));
		p.addPoint(x2 + xCor(i1, aDir-.5), y2 + yCor(i1, aDir-.5));
		p.addPoint(x2, y2);
		
		if (filled) g.fillPolygon(p); else g.drawPolygon(p);
	}

	
	/**
	 * Draw an arrow
	 *
	 * @param g the graphics object
	 * @param x1 the source X coordinate
	 * @param y1 the source Y coordinate
	 * @param x2 the target X coordinate
	 * @param y2 the target Y coordinate
	 * @param stroke the stroke size
	 * @param filled whether to fill the arrowhead
	 */
	public static void drawArrow(Graphics g, int x1, int y1, int x2, int y2, double stroke, boolean filled) {
		g.drawLine(x2, y2, x1, y1);
		drawArrowHead(g, x1, y1, x2, y2, stroke, filled);
	}
	
	
	/**
	 * Choose an image to open or save
	 */
	public static File chooseImageFile(Component parent, String title, boolean open, boolean losslessOnly) {
		JFileChooser fc = new JFileChooser();
		fc.setDialogTitle(title);
		fc.setDialogType(open ? JFileChooser.OPEN_DIALOG : JFileChooser.SAVE_DIALOG);
		if (lastChosenImageFile != null) fc.setSelectedFile(lastChosenImageFile);
		
		fc.setAcceptAllFileFilterUsed(false);
		fc.addChoosableFileFilter(losslessOnly ? filterImageLossless : filterImage);
		
		int r = 0;
		if (open) {
			r = fc.showOpenDialog(parent);
		}
		else {
			r = fc.showSaveDialog(parent);
		}
		if (r != JFileChooser.APPROVE_OPTION) return null;
        
        lastChosenImageFile = fc.getSelectedFile();
        
        return lastChosenImageFile;
    }
	
	
	/**
	 * Choose an image to open or save
	 */
	public static File chooseImageFile(Component parent, String title, boolean open) {
        return chooseImageFile(parent, title, open, false);
    }
	
	
	/**
	 * Save an image
	 * 
	 * @param file the file
	 * @param image the image to save
	 */
	public static void saveImage(File file, BufferedImage image) throws IOException {
		String extension = Utils.getExtension(file);
		if (extension == null) extension = "png";
		ImageIO.write(image, extension.toLowerCase(), file);
	}
	
	
	/**
	 * Save an image
	 * 
	 * @param fileName the file name
	 * @param image the image to save
	 */
	public static void saveImage(String fileName, BufferedImage image) throws IOException {
		String extension = Utils.getExtension(new File(fileName));
		if (extension == null) extension = "png";
		File f = new File(fileName);
		ImageIO.write(image, extension.toLowerCase(), f);
	}
	
	
	/**
	 * Grabs a RGB values of the given image icon
	 *
	 * @param icon the image icon
	 * @return an array of RGB values
	 */
	public static int[] grabRGB(ImageIcon icon) {
	
		int width = icon.getIconWidth();
		int height = icon.getIconHeight();
		
		int[] rgb = new int[width * height];
		PixelGrabber pg = new PixelGrabber(icon.getImage(), 0, 0, width, height, rgb, 0, width);
		
		try {
			pg.grabPixels();
		} catch (InterruptedException e) {
			throw new RuntimeException("Interrupted waiting for pixels!");
		}
		
		if ((pg.getStatus() & ImageObserver.ABORT) != 0) {
			throw new RuntimeException("Image fetch aborted or errored");
		}
		
		for (int i = 0; i < rgb.length; i++) rgb[i] &= 0xffffff;
		
		return rgb;
	}
	
	
	/**
	 * Create a buffered image from an RGB array
	 *
	 * @param rgb the RGB array
	 * @param width the image width
	 * @param height the image height
	 * @return the buffered image
	 */
	public static BufferedImage plotRGB(int rgb[], int width, int height) {
		
		BufferedImage b = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		
		for (int y = 0; y < height; y++) {
			int base = y * width;
			for (int x = 0; x < width; x++) {
				b.setRGB(x, y, rgb[base + x]);
			}
		}
		
		return b;
	}
	
	
	/**
	 * Determine whether the given image is grayscale
	 *
	 * @param rgb the RGB data
	 * @return true if the image is grayscale
	 */
	public static boolean isGrayscale(int[] rgb) {
		
		for (int i = 0; i < rgb.length; i++) {
			int pixel = rgb[i];
			int red   = (pixel >> 16) & 0xff;
			int green = (pixel >>  8) & 0xff;
			int blue  = (pixel      ) & 0xff;
			if ((red != blue) || (red != green) || (blue != green)) return false;
		}
		
		return true;
	}
	
	
	/**
	 * Determine whether the given image icon is grayscale
	 *
	 * @param icon the image icon
	 * @return true if the image is grayscale
	 */
	public static boolean isGrayscale(ImageIcon icon) {
		return isGrayscale(grabRGB(icon));
	}
	
	
	/**
	 * Compute a color in between the two given colors
	 * 
	 * @param color1 the first color
	 * @param color2 the second color
	 * @param weight where in between (0 = color1, 1 = color2)
	 * @return the color in between color1 and color2
	 */
	public static Color getColorInBetween(Color color1, Color color2, double weight) {
		
		int r1 = color1.getRed();
		int g1 = color1.getGreen();
		int b1 = color1.getBlue();
		
		int r2 = color2.getRed();
		int g2 = color2.getGreen();
		int b2 = color2.getBlue();
		
		int r3 = (int) Math.round(r1 + weight * (r2 - r1));
		int g3 = (int) Math.round(g1 + weight * (g2 - g1));
		int b3 = (int) Math.round(b1 + weight * (b2 - b1));
		
		return new Color(r3, g3, b3);
	}

	
	/**
	 * Draw a multi-line text
	 * 
	 * @param g the graphics context
	 * @param s the string
	 * @param x the X coordinate
	 * @param y the Y coordinate (middle)
	 * @param justify the justification
	 */
	public static void drawText(Graphics g, String s, int x, int y, TextJustify justify) {
		
		FontMetrics fm = g.getFontMetrics();
		int fh = g.getFont().getSize();
		
		int cur_y = y + (int)(fh / 2 - fm.getDescent()) + 1; 
		int lh = (int) fh + 1;
		
		if (s.indexOf('\n') >= 0) {
			String[] a = s.split("\n");
			cur_y -= ((a.length - 1) * lh) / 2;
			
			if (justify == TextJustify.LEFT) {
				for (int ai = 0; ai < a.length; ai++) {
					g.drawString(a[ai], x, cur_y + ai * lh);
				}
			}
			else if (justify == TextJustify.CENTER) {
				for (int ai = 0; ai < a.length; ai++) {
					Rectangle2D ar = fm.getStringBounds(a[ai], g);
					g.drawString(a[ai], x - (int)(ar.getWidth() / 2), cur_y + ai * lh);
				}
			}
			else if (justify == TextJustify.RIGHT) {
				for (int ai = 0; ai < a.length; ai++) {
					Rectangle2D ar = fm.getStringBounds(a[ai], g);
					g.drawString(a[ai], x - (int)ar.getWidth(), cur_y + ai * lh);
				}
			}
		}
		else {
			if (justify == TextJustify.LEFT) {
				g.drawString(s, x, cur_y);
			}
			else if (justify == TextJustify.CENTER) {
				Rectangle2D r = fm.getStringBounds(s, g);
				g.drawString(s, x - (int)(r.getWidth() / 2), cur_y);
			}
			else if (justify == TextJustify.RIGHT) {
				Rectangle2D r = fm.getStringBounds(s, g);
				g.drawString(s, x - (int)r.getWidth(), cur_y);
			}
		}
	}
	
	
	/**
	 * Get bounds of a multi-line text
	 * 
	 * @param fm the font metrics
	 * @param s the string
	 * @return the bounds
	 */
	public static Rectangle2D getTextBounds(FontMetrics fm, String s) {
		
		if (s.indexOf('\n') >= 0) {
			String[] a = s.split("\n");
			
			int fh = fm.getFont().getSize();
			int lh = (int) fh + 1;
			double h = lh * a.length;
			
			double w = 0;
			for (int ai = 0; ai < a.length; ai++) {
				int tw = SwingUtilities.computeStringWidth(fm, a[ai]);
				if (tw > w) w = tw;
			}
			
			return new Rectangle2D.Double(0, 0, w, h);
		}
		else {
			
			int h = fm.getFont().getSize();
			int w = SwingUtilities.computeStringWidth(fm, s);
			
			return new Rectangle2D.Double(0, 0, w, h);
		}
	}
	
	
	/**
	 * Get bounds of a multi-line text
	 * 
	 * @param g the graphics context
	 * @param s the string
	 * @return the bounds
	 */
	public static Rectangle2D getTextBounds(Graphics g, String s) {
		
		FontMetrics fm = g.getFontMetrics();
		return getTextBounds(fm, s);
	}
	
	
	/**
	 * Clip the specified line to the given rectangle
	 * 
	 * @param line the line (<code>null</code> not permitted).
	 * @param rect the clipping rectangle (<code>null</code> not permitted).
	 * 
	 * @return <code>true</code> if the clipped line is visible, and
	 *         <code>false</code> otherwise.
	 */
	public static boolean clipLine(Line2D line, Rectangle2D rect) {
		
		// From: JFreeChart, (C) Copyright 2000-2008, by Object Refinery Limited and Contributors
		//       Distributed under the GNU Lesser General Public License
		//       http://www.java2s.com/Code/Java/2D-Graphics-GUI/Clipsthespecifiedlinetothegivenrectangle.htm

		double x1 = line.getX1();
		double y1 = line.getY1();
		double x2 = line.getX2();
		double y2 = line.getY2();

		double minX = rect.getMinX();
		double maxX = rect.getMaxX();
		double minY = rect.getMinY();
		double maxY = rect.getMaxY();

		int f1 = rect.outcode(x1, y1);
		int f2 = rect.outcode(x2, y2);

		while ((f1 | f2) != 0) {
			if ((f1 & f2) != 0) {
				return false;
			}
			double dx = (x2 - x1);
			double dy = (y2 - y1);
			// update (x1, y1), (x2, y2) and f1 and f2 using intersections
			// then recheck
			if (f1 != 0) {
				// first point is outside, so we update it against one of the
				// four sides then continue
				if ((f1 & Rectangle2D.OUT_LEFT) == Rectangle2D.OUT_LEFT
						&& dx != 0.0) {
					y1 = y1 + (minX - x1) * dy / dx;
					x1 = minX;
				} else if ((f1 & Rectangle2D.OUT_RIGHT) == Rectangle2D.OUT_RIGHT
						&& dx != 0.0) {
					y1 = y1 + (maxX - x1) * dy / dx;
					x1 = maxX;
				} else if ((f1 & Rectangle2D.OUT_BOTTOM) == Rectangle2D.OUT_BOTTOM
						&& dy != 0.0) {
					x1 = x1 + (maxY - y1) * dx / dy;
					y1 = maxY;
				} else if ((f1 & Rectangle2D.OUT_TOP) == Rectangle2D.OUT_TOP
						&& dy != 0.0) {
					x1 = x1 + (minY - y1) * dx / dy;
					y1 = minY;
				}
				f1 = rect.outcode(x1, y1);
			} else if (f2 != 0) {
				// second point is outside, so we update it against one of the
				// four sides then continue
				if ((f2 & Rectangle2D.OUT_LEFT) == Rectangle2D.OUT_LEFT
						&& dx != 0.0) {
					y2 = y2 + (minX - x2) * dy / dx;
					x2 = minX;
				} else if ((f2 & Rectangle2D.OUT_RIGHT) == Rectangle2D.OUT_RIGHT
						&& dx != 0.0) {
					y2 = y2 + (maxX - x2) * dy / dx;
					x2 = maxX;
				} else if ((f2 & Rectangle2D.OUT_BOTTOM) == Rectangle2D.OUT_BOTTOM
						&& dy != 0.0) {
					x2 = x2 + (maxY - y2) * dx / dy;
					y2 = maxY;
				} else if ((f2 & Rectangle2D.OUT_TOP) == Rectangle2D.OUT_TOP
						&& dy != 0.0) {
					x2 = x2 + (minY - y2) * dx / dy;
					y2 = minY;
				}
				f2 = rect.outcode(x2, y2);
			}
		}

		line.setLine(x1, y1, x2, y2);
		return true; // the line is visible - if it wasn't, we'd have
					 // returned false from within the while loop above
	}
	
	
	/**
	 * Clip the specified line to the given ellipse
	 * 
	 * @param line the line (<code>null</code> not permitted).
	 * @param ellipse the ellipse (<code>null</code> not permitted).
	 * 
	 * @return <code>true</code> if the clipped line is visible, and
	 *         <code>false</code> otherwise.
	 */
	public static boolean clipLine(Line2D line, Ellipse2D ellipse) {
		
		// Parts of the code come from:
		//   Original author: Sean James McKenzie
		//   http://www.baconandgames.com/2010/02/22/intersection-of-an-ellipse-and-a-line-in-as3/
		
		
		boolean in1 = ellipse.contains(line.getP1());
		boolean in2 = ellipse.contains(line.getP2());
		
		if (in1 && in2) return true;
		
		
		// Normalize the points relative to the center of the ellipse
		
		double x1 =   line.getX1() - ellipse.getCenterX();
		double y1 = - line.getY1() + ellipse.getCenterY();
		double x2 =   line.getX2() - ellipse.getCenterX();
		double y2 = - line.getY2() + ellipse.getCenterY();
		
		
		// Check if we need to flip the coordinate system
		
		boolean flip = false;
		
		double a = ellipse.getWidth() / 2;
		double b = ellipse.getHeight() / 2;
		
		double dx = x2 - x1;
		double dy = y2 - y1;
		
		if (dx == 0 && dy == 0) return false;
		if (dx == 0) {
			double t;
			t = x1; x1 = y1; y1 = t;
			t = x2; x2 = y2; y2 = t;
			t = dx; dx = dy; dy = t;
			t = a ; a  = b ; b  = t;
			flip = true;
		}
		
		
		// Get the slope of the line and the slope intercept
		
		double m  = dy / dx;
		double si = y2 - m * x2;
		
		
		// Get the coefficients
		
		double A = b*b + a*a*m*m;
		double B = 2*a*a*si*m;
		double C = a*a*si*si - a*a*b*b;
		
		
		// Variables for the intercepts
		
		double x3, y3, x4, y4;
		
		
		// Use the quadratic equation to find x
		
		double radicand = B*B - 4*A*C;
		
		if (radicand >= 0) {
			
			// We have two intercepts
			// Solve for x values using the quadratic equation
			
			x3 = (-B - Math.sqrt(radicand)) / (2*A);
			x4 = (-B + Math.sqrt(radicand)) / (2*A);
			
			
			// Calculate y
			
			y3 = m*x3 + si;
			y4 = m*x4 + si;
			
			
			// Revert to the original coordinate system
			
			if (flip) {
				double t;
				t = x3; x3 = y3; y3 = t;
				t = x4; x4 = y4; y4 = t;
			}
			
			x1  = line.getX1();
			y1  = line.getY1();
			x2  = line.getX2();
			y2  = line.getY2();
			
			x3 += ellipse.getCenterX();
			y3  = ellipse.getCenterY() - y3;
			x4 += ellipse.getCenterX();
			y4  = ellipse.getCenterY() - y4;
			
			
			// Set the line - only point 2 is in the ellipse
			
			if (!in1 &&  in2) {
				
				// Make sure point 1 is closer to point 3
				
				double d13 = (x1-x3)*(x1-x3) + (y1-y3)*(y1-y3);
				double d14 = (x1-x4)*(x1-x4) + (y1-y4)*(y1-y4);
				if (d13 > d14) {
					double t;
					t = x3; x3 = x4; x4 = t;
					t = y3; y3 = y4; y4 = t;
				}
				
				line.setLine(x3, y3, x2, y2);
				return true;
			}
			
			
			// Set the line - only point 1 is in the ellipse
			
			if ( in1 && !in2) {
				
				// Make sure point 2 is closer to point 4
				
				double d23 = (x2-x3)*(x2-x3) + (y2-y3)*(y2-y3);
				double d24 = (x2-x4)*(x2-x4) + (y2-y4)*(y2-y4);
				if (d23 < d24) {
					double t;
					t = x3; x3 = x4; x4 = t;
					t = y3; y3 = y4; y4 = t;
				}

				line.setLine(x1, y1, x4, y4);
				return true;
			}
			
			
			// Both points are outside of the ellipse;
			// check that the points are on the line
			
			boolean bx = (x1 >= x3 && x3 <= x2) || (x2 >= x3 && x3 <= x1);
			boolean by = (y1 >= y3 && y3 <= y2) || (y2 >= y3 && y3 <= y1);
			
			if (!(bx && by)) return false;
			
			
			// Make sure point 1 is closer to point 3
			
			double d13 = (x1-x3)*(x1-x3) + (y1-y3)*(y1-y3);
			double d14 = (x1-x4)*(x1-x4) + (y1-y4)*(y1-y4);
			if (d13 > d14) {
				double t;
				t = x3; x3 = x4; x4 = t;
				t = y3; y3 = y4; y4 = t;
			}
			
			
			// Set the line
			
			line.setLine(x3, y3, x4, y4);
			return true;
		}
		else if (radicand == 0){
			
			// We have only one intercept
			
			x3 = -B / (2*A);	
			y3 = m*x3 + si;				
			
			
			// Revert to the original coordinate system
			
			if (flip) {
				double t;
				t = x3; x3 = y3; y3 = t;
			}
			
			x3 += ellipse.getCenterX();
			y3  = ellipse.getCenterY() - y3;
			
			
			// Check whether point 3 lies on the line
			
			boolean bx = (x1 >= x3 && x3 <= x2) || (x2 >= x3 && x3 <= x1);
			boolean by = (y1 >= y3 && y3 <= y2) || (y2 >= y3 && y3 <= y1);
			
			if (!(bx && by)) return false;
			
			
			// Set the line				
			
			line.setLine(x3, y3, x3, y3);
			
			return true;
		}
		else {
			
			// No intercept
			
			return false;
		}
	}
}
