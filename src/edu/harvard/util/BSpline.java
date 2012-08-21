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

package edu.harvard.util;

import java.awt.*;
import java.util.*;


/**
 * B-spline
 * 
 * @author Miriah Meyer et al. (MizBee project, http://www.mizbee.org)
 * @author Peter Macko (Java port)
 */
public class BSpline {

	int _degree, _detail;
	double _tightness, _dt;
	ArrayList<Double> _cp_x, _cp_y;
	double[] _knots;
	double _mindist;

	
	/**
	 * Create an instance of class BSpline with the default settings
	 */
	public BSpline() {   
		_degree = 3;
		_tightness = 1.0f;
		_detail = 10;
		_mindist = 3;

		_dt = 1.0f / ((double)( _detail-1 ) + 0.000001f);

		_cp_x = new ArrayList<Double>();
		_cp_y = new ArrayList<Double>();
	}

	
	/**
	 * Add a control point
	 * 
	 * @param x the X coordinate
	 * @param y the Y coordinate
	 */
	public void addCP( double x, double y ) {
		_cp_x.add( x );
		_cp_y.add( y );
	}

	
	/**
	 * Set the degree of the spline
	 *  
	 * @param d the new degree
	 */
	public void setDegree( int d ) {
		_degree = d;
	}

	
	/**
	 * Return the degree of the spline
	 *  
	 * @return the degree
	 */
	public int getDegree() {
		return _degree;
	}

	
	/**
	 * Set the tightness of the spline
	 * 
	 * @param t the new tightness
	 */
	public void setTightness( double t ) {
		_tightness = t;
	}

	
	/**
	 * Set the detail of the spline
	 * 
	 * @param d the new value of the detail
	 */
	public void setDetail( int d ) {
		_detail = Math.max( d+1, 1 );
		_dt = 1.0f / ((double)( _detail-1 ) + 0.000001f);
	}

	
	/**
	 * Clear all control points
	 */
	public void clearCP() {    
		_cp_x.clear();
		_cp_y.clear();
	}
	
	
	/**
	 * Get the number of control points
	 * 
	 * @return the number of control points
	 */
	public int sizeCP() {
		return _cp_x.size();
	}

	
	/**
	 * Render the spline in the given graphics context
	 * 
	 * @param g the graphics context
	 */
	public void render(Graphics g) {
		if ( _cp_x.size() <= _degree ) {
			throw new RuntimeException( "BSpline Error: " + _cp_x.size() + " is not enough control points for a degree " + _degree + " bspline curve" );
		}

		createKnotVector();
		straightenControlPoints();

		int sx = 0, sy = 0, dx, dy;
		boolean first = true;

		double[] pos = new double[2];
		int num_intervals = (_knots.length-1) - 2*_degree;
		int interval;
		for ( interval = 0; interval < num_intervals; interval++ ) {
			for ( double t = _knots[_degree+interval]; t < _knots[_degree+interval+1]; t += _dt ) {
				pos = evaluateCurve( t, _degree+interval );
				
				if (first) {
					sx = (int) Math.round(pos[0]);
					sy = (int) Math.round(pos[1]);
					first = false;
				}
				else {
					dx = (int) Math.round(pos[0]);
					dy = (int) Math.round(pos[1]);
					
					if ((dx - sx) * (dx - sx) + (dy - sy) * (dy - sy) >= _mindist * _mindist) {
						g.drawLine(sx, sy, dx, dy);
						sx = dx;
						sy = dy;
					}
				}
			}
		}

		pos[0] = (Double)_cp_x.get(_cp_x.size()-1);
		pos[1] = (Double)_cp_y.get(_cp_y.size()-1);
		
		if (first) {
			sx = (int) Math.round(pos[0]);
			sy = (int) Math.round(pos[1]);
			first = false;
		}
		else {
			dx = (int) Math.round(pos[0]);
			dy = (int) Math.round(pos[1]);
			g.drawLine(sx, sy, dx, dy);
			sx = dx;
			sy = dy;
		}
	}

	
	/**
	 * Create the knot vector
	 */
	private void createKnotVector() {
		int num_middle = _cp_x.size() - (_degree+1);
		_knots = new double[_degree + _cp_x.size() + 1 ];

		for ( int i = 0; i <= _degree; i++ ) {
			_knots[i] = 0.0f;
		}

		for ( int i = (_degree+1); i < (_degree+1+num_middle); i++ ) {
			_knots[i] = _knots[i-1]+1;
		}

		for ( int i = (_degree+1+num_middle); i <= (2*_degree+1+num_middle); i++ ) {
			_knots[i] = _knots[_degree+num_middle]+1;
		}
	}

	
	/**
	 * Compute the coordinates of a point on a curve
	 * 
	 * @param t the value from the knots vector
	 * @param i the value of degree+interval
	 * @return the coordinates of the point
	 */
	private double[] evaluateCurve( double t, int i )
	{
		double px, py, alpha_x=0.0f, alpha_y=0.0f;
		double beta;
		double[] pos = new double[2];
		for ( int m = i-_degree; m <= i; m++ )
		{
			px = (Double)_cp_x.get(m);
			py = (Double)_cp_y.get(m);

			beta = computeBeta( t, m, _degree );

			alpha_x += px * beta;
			alpha_y += py * beta;
		} 

		pos[0] = alpha_x;
		pos[1] = alpha_y;

		return pos;
	}

	
	/**
	 * Straighten the control points
	 */
	private void straightenControlPoints() {
		if ( _tightness == 1.0 ) {
			return;
		}

		double p0x = (Double)_cp_x.get(0);
		double pNx = (Double)_cp_x.get(_cp_x.size()-1);
		double p0y = (Double)_cp_y.get(0);
		double pNy = (Double)_cp_y.get(_cp_y.size()-1);
		for ( int i = 1; i < _cp_x.size()-1; i++ ) {
			_cp_x.set(i, _tightness*(Double)_cp_x.get(i) + (1.0f - _tightness)*(p0x + ((double) i)/((double)(_cp_x.size()-1)) * (pNx - p0x)));
			_cp_y.set(i, _tightness*(Double)_cp_y.get(i) + (1.0f - _tightness)*(p0y + ((double) i)/((double)(_cp_y.size()-1)) * (pNy - p0y)));
		}
	}

	
	/**
	 * Compute beta
	 * 
	 * @param t the value from the knots vector
	 * @param i the value of degree+interval (???)
	 * @param k the degree of the spline
	 * @return beta
	 */
	private double computeBeta( double t, int i, int k ) {
		if ( k == 0 ) {
			if ( (t >= _knots[i]) && (t < _knots[i+1]) ) {
				return 1.0f;
			}
			return 0.0f;
		}

		if ( t >= _knots[i+1+k] ) {
			return 0.0f;
		}

		double left, rite;
		double beta = computeBeta( t, i, k-1 );
		if ( beta != 0.0 )
			left = ((t-_knots[i])/(_knots[i+k]-_knots[i])) * beta;
		else
			left = 0.0f;

		beta = computeBeta( t, i+1, k-1 );
		if ( beta != 0.0 )
			rite = ((_knots[i+1+k]-t)/(_knots[i+1+k]-_knots[i+1])) * 
			beta;
		else
			rite = 0.0f;

		return (left + rite);
	}
}
