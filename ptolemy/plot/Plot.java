/* A signal plotter.

@Copyright (c) 1997 The Regents of the University of California.
All rights reserved.

Permission is hereby granted, without written agreement and without
license or royalty fees, to use, copy, modify, and distribute this
software and its documentation for any purpose, provided that the
above copyright notice and the following two paragraphs appear in all
copies of this software.

IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY
FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
ENHANCEMENTS, OR MODIFICATIONS.

						PT_COPYRIGHT_VERSION_2
						COPYRIGHTENDKEY
*/
package plot;

// FIXME: To do
//   - support for oscilloscope-like plots (where x axis wraps around).
//   - steps between points rather than connected lines.
//   - cubic spline interpolation
//   - support error bars (ydelta, or ylow yhigh)
//     (adjust yrange to fit error bars)
//   - log scale axes

// NOTE: The XOR drawing mode is needed in order to be able to erase
// plotted points and restore the grid line, tick marks, and boundary
// rectangle.  Another alternative would be to put the tick marks
// outside the rectangle, disallow grid marks, and adjust drawing so
// it never overlaps the boundary rectangle.  Then erasing could be
// done by redrawing in white. This would be better.

// NOTE: There are quite a few subjective spacing parameters, all
// given, unfortunately, in pixels.  This means that as resolutions
// get better, this program may need to be adjusted.

import java.awt.*;
import java.io.*;
import java.util.*;
import java.applet.Applet;

//////////////////////////////////////////////////////////////////////////
//// Plot
/** 
 * A flexible signal plotter.  The plot can be configured and data can be
 * provided either through a file with commands or
 * through direct invocation of the public methods of the class.
 * If a file is used, the file can be given as a URL through the
 * applet parameter called "dataurl". The file contains any number
 * commands, one per line.  Unrecognized commands and commands with
 * syntax errors are ignored.  Comments are denoted by a line starting
 * with a pound sign "#".  The recognized commands include those
 * supported by the base class, plus a few more.
 * The following command defines the number of data sets to be plotted.
 * <pre>
 * NumSets: <i>positiveInteger</i>
 * </pre>
 * If data is provided for more data sets than this number, those
 * data are ignored.  Each dataset can be optionally identified with
 * color (see the base class) or with unique marks.  The style of
 * marks used to denote a data point is defined by one of the following
 * commands:
 * <pre>
 * Marks: none
 * Marks: points
 * Marks: dots
 * Marks: various
 * </pre>
 * Here, "points" are small dots, while "dots" are larger.  If "various"
 * is specified, then unique marks are used for the first ten data sets,
 * and then recycled.
 * Using no marks is useful when lines connect the points in a plot,
 * which is done by default.  To disable connecting lines, use:
 * <pre>
 * Lines: off
 * </pre>
 * To reenable them, use
 * <pre>
 * Lines: on
 * </pre>
 * You can also specify "impulses," which are lines drawn from a plotted point
 * down to the x axis.  These are off by default, but can be turned on with the
 * command:
 * <pre>
 * Impulses: on
 * </pre>
 * or back off with the command
 * <pre>
 * Impulses: off
 * </pre>
 * To create a bar graph, turn off lines and use any of the following commands:
 * <pre>
 * Bars: on
 * Bars: <i>width</i>
 * Bars: <i>width, offset</i>
 * </pre>
 * The <i>width</i> is a real number specifying the width of the bars
 * in the units of the x axis.  The <i>offset</i> is a real number
 * specifying how much the bar of the <i>i</i><sup>th</sup> data set
 * is offset from the previous one.  This allows bars to "peek out"
 * from behind the ones in front.  Note that the frontmost data set
 * will be the last one.  To turn off bars, use
 * <pre>
 * Bars: off
 * </pre>
 * To specify data to be plotted, start a data set with the following command:
 * <pre>
 * DataSet: <i>string</i>
 * </pre>
 * Here, <i>string</i> is a label that will appear in the legend.
 * It is not necessary to enclose the string in quotation marks.
 * The data itself is given by a sequence of commands with one of the
 * following forms:
 * <pre>
 * <i>x</i>,<i>y</i>
 * draw: <i>x</i>,<i>y</i>
 * move: <i>x</i>,<i>y</i>
 * </pre>
 * The "draw" command is optional, so the first two forms are equivalent.
 * The "move" command causes a break in connected points, if lines are
 * being drawn between points. The numbers <i>x</i> and <i>y</i> are
 * arbitrary numbers as supported by the Double parser in Java.
 *
 * @author: Edward A. Lee, Christopher Hylands
 * @version: $Id$
 */
public class Plot extends PlotBox {

    //////////////////////////////////////////////////////////////////////////
    ////                         public methods                           ////
   
    /**
     * In the specified data set, add the specified x,y point to the
     * plot.  Data set indices begin with zero.  If the dataset
     * argument is out of range, ignore.  The number of data sets is
     * given by calling *setNumSets()*.  The fourth argument indicates
     * whether the point should be connected by a line to the previous
     * point.
     */
    public synchronized void addPoint(int dataset, double x, double y,
				      boolean connected) {
        if (dataset >= _numsets || dataset < 0) return;
        
        // For auto-ranging, keep track of min and max.
        if (x < _xBottom) _xBottom = x;
        if (x > _xTop) _xTop = x;
        if (y < _yBottom) _yBottom = y;
        if (y > _yTop) _yTop = y;

        // FIXME: Ignoring sweeps for now.
        PlotPoint pt = new PlotPoint();
        pt.x = x;
        pt.y = y;
        pt.connected = connected;
        Vector pts = _points[dataset];
        pts.addElement(pt);
        if (__pointsPersistence > 0) {
            if (pts.size() > __pointsPersistence) erasePoint(dataset,0);
        }
        __drawPlotPoint(dataset, pt);
    }
    
    /**
     * Draw the axes and then plot all points.  This is synchronized
     * to prevent multiple threads from drawing the plot at the same
     * time.  It calls <code>notify()</code> at the end so that a
     * thread can use <code>wait()</code> to prevent it plotting
     * points before the axes have been first drawn.  If the argument
     * is true, clear the display first.
     */
	public synchronized void drawPlot(boolean clearfirst) {
    	// Draw the axes
    	super.drawPlot(clearfirst);
    	// Plot the points
    	for (int dataset = 0; dataset < _numsets; dataset++) {
    	    Vector data = _points[dataset];
    	    for (int pointnum = 0; pointnum < data.size(); pointnum++) {
    	        __drawPlotPoint(dataset, (PlotPoint)data.elementAt(pointnum));
    	    }
    	}
    	notify();
    }
    
    /** 
     * Erase the point at the given index in the given dataset.  If
     * lines are being drawn, also erase the line to the next points
     * (note: not to the previous point).  The point is not checked to
     * see whether it is in range, so care must be taken by the caller
     * to ensure that it is.
     */
    public synchronized void erasePoint(int dataset, int index) {
        Vector pts = _points[dataset];
        // Erase the line to the next point rather than the previous point.
        int saveprevx = __prevx[dataset];
        int saveprevy = __prevy[dataset];
        PlotPoint pp = (PlotPoint)pts.elementAt(index);
        if (index < pts.size() - 1) {
            PlotPoint nextp = (PlotPoint)pts.elementAt(index+1);
            pp.connected = nextp.connected;
            __prevx[dataset] = _ulx + (int) ((nextp.x - _xMin) * _xscale);
            __prevy[dataset] = _lry - (int) ((nextp.y - _yMin) * _yscale);
        }
        __drawPlotPoint(dataset, pp);
        __prevx[dataset] = saveprevx;
        __prevy[dataset] = saveprevy;
        pts.removeElementAt(index);
        pp = (PlotPoint)pts.elementAt(index);
        pp.connected = false;
    }

    /**
     * Return a string describing this applet.
     */
    public String getAppletInfo() {
        return "Plot 1.0: A flexible data plotter.\n" +
	    "By: Edward A. Lee, eal@eecs.berkeley.edu and\n " +
	    "Christopher Hylands, cxh@eecs.berkeley.edu\n" +
	    "($Id$)";
    }

    /**
     * Return the maximum number of datasets
     */
    public int getMaxDataSets() {
        return __MAX_DATASETS;
    }

    /**
     * Initialize the applet.  If a dataurl parameter has been specified,
     * read the file given by the URL and parse the commands in it.
     * Also, make a button for auto-scaling the plot.
     */
    public synchronized void init() {
        setNumSets(_numsets);
        _currentdataset = -1;
        super.init();
    }

	/**
	 * Draw the axes and the accumulated points.
	 */
	public void paint(Graphics g) {
	    drawPlot(true);
    }

    /**
     * Parse a line that gives plotting information. Return true if
     * the line is recognized.  Lines with syntax errors are ignored.
     */
    public boolean parseLine (String line) {
        boolean connected = false;
        if (__connected) connected = true;
        // parse only if the super class does not recognize the line.
        if (super.parseLine(line)) {
	    return true;
	} else {
            int start = 0;
            if (line.startsWith("Marks:")) {
                String style = (line.substring(6)).trim();
                setMarksStyle(style);
                return true;
            } else if (line.startsWith("NumSets:")) {
                String num = (line.substring(8)).trim();
                try {
                    setNumSets(Integer.parseInt(num));
                }
                catch (NumberFormatException e) {
                    // ignore bogons
                }
                return true;
            } else if (line.startsWith("DataSet:")) {
                // new data set
                __firstinset = true;
                _currentdataset += 1;
                if (_currentdataset >= 10) _currentdataset = 0;
                String legend = (line.substring(8)).trim();
                addLegend(_currentdataset, legend);
                return true;
            } else if (line.startsWith("Lines:")) {
                if (line.indexOf("off",6) >= 0) {
                    setConnected(false);
                } else {
                    setConnected(true);
                }
                return true;
            } else if (line.startsWith("Impulses:")) {
                if (line.indexOf("off",9) >= 0) {
                    setImpulses(false);
                } else {
                    setImpulses(true);
                }
                return true;
            } else if (line.startsWith("Bars:")) {
                if (line.indexOf("off",5) >= 0) {
                    setBars(false);
                } else {
                    setBars(true);
         	        int comma = line.indexOf(",", 5);
         	        String barwidth;
         	        String baroffset = null;
        	        if (comma > 0) {
                        barwidth = (line.substring(5, comma)).trim();
                        baroffset = (line.substring(comma+1)).trim();
                    } else {
                        barwidth = (line.substring(5)).trim();
                    }
        	        try {
        	            Double bwidth = new Double(barwidth);
        	            double boffset = __baroffset;
        	            if (baroffset != null) {
        	                boffset = (new Double(baroffset)).doubleValue();
        	            }
        	            setBars(bwidth.doubleValue(), boffset);
        	        } catch (NumberFormatException e) {
        	            // ignore if format is bogus.
        	        }
                }
                return true;
            } else if (line.startsWith("move:")) {
                // a disconnected point
                connected = false;
                start = 5;
            } else if (line.startsWith("draw:")) {
                // a connected point, if connect is enabled.
                start = 5;
            }
            // See if an x,y point is given
         	int comma = line.indexOf(",", start);
        	if (comma > 0) {
                String x = (line.substring(start, comma)).trim();
                String y = (line.substring(comma+1)).trim();
        	    try {
        	        Double xpt = new Double(x);
        	        Double ypt = new Double(y);
        	        if (__firstinset) {
        	            connected = false;
        	            __firstinset = false;
        	        }
        	        addPoint(_currentdataset, xpt.doubleValue(),
				 ypt.doubleValue(), connected);
        	        return true;
        	    } catch (NumberFormatException e) {
        	        // ignore if format is bogus.
        	    }
        	}
        }
        return false;
    }

    /**
     * Turn bars on or off.
     */
    public void setBars (boolean on) {
        if (on) {
            __bars = true;
        } else {
            __bars = false;
        }
    }

    /** 
     * Turn bars on and set the width and offset.  Both are specified
     * in units of the x axis.  The offset is the amount by which the
     * i<sup>th</sup> data set is shifted to the right, so that it
     * peeks out from behind the earlier data sets.
     */
    public void setBars (double width, double offset) {
        __barwidth = width;
        __baroffset = offset;
        __bars = true;
    }
    
    /** 
     * If the argument is true, then the default is to connect
     * subsequent points with a line.  If the argument is false, then
     * points are not connected.  When points are by default
     * connected, individual points can be not connected by giving the
     * appropriate argument to <code>addPoint()</code>.
     */
    public void setConnected (boolean on) {
        if (on) {
            __connected = true;
        } else {
            __connected = false;
        }
    }
    
    /** 
     * If the argument is true, then a line will be drawn from any
     * plotted point down to the x axis.  Otherwise, this feature is
     * disabled.
     */
    public void setImpulses (boolean on) {
        if (on) {
            __impulses = true;
        } else {
            __impulses = false;
        }
    }
    
    /**
     * Set the marks style to "none", "points", "dots", or "various".
     * In the last case, unique marks are used for the first ten data
     * sets, then recycled.
     */
    public void setMarksStyle (String style) {
        if (style.equalsIgnoreCase("none")) {
            _marks = 0;
        } else if (style.equalsIgnoreCase("points")) {
            _marks = 1;
        } else if (style.equalsIgnoreCase("dots")) {
            _marks = 2;
        } else if (style.equalsIgnoreCase("various")) {
            _marks = 3;
        }
    }

    /** 
     * Specify the number of data sets to be plotted together.
     * Allocate a Vector to store each data set.  Note that calling
     * this causes any previously plotted points to be forgotten.
     * This method should be called before
     * <code>setPointsPersistence</code>.
     */
    public void setNumSets (int numsets) {
        this._numsets = numsets;
        _points = new Vector[numsets];
        __prevx = new int[numsets];
        __prevy = new int[numsets];
        for (int i=0; i<numsets; i++) {
            _points[i] = new Vector();
        }
    }
    
    /** 
     * Calling this method with a positive argument sets the
     * persistence of the plot to the given number of points.  Calling
     * with a zero argument turns off this feature, reverting to
     * infinite memory (unless sweeps persistence is set).  If both
     * sweeps and points persistence are set then sweeps take
     * precedence.  This method should be called after
     * <code>setNumSets()</code>.  
     * FIXME: No file format yet.
     */
    public void setPointsPersistence (int persistence) {
        __pointsPersistence = persistence;
        if (persistence > 0) {
            for (int i = 0; i < _numsets; i++) {
                _points[i].setSize(persistence);
            }
        }
    }
    
    /** 
     * A sweep is a sequence of points where the value of X is
     * increasing.  A point that is added with a smaller x than the
     * previous point increments the sweep count.  Calling this method
     * with a non-zero argument sets the persistence of the plot to
     * the given number of sweeps.  Calling with a zero argument turns
     * off this feature.  If both sweeps and points persistence are
     * set then sweeps take precedence.
     * FIXME: No file format yet.
     * FIXME: Not implemented yet.
     */
    public void setSweepsPersistence (int persistence) {
        __sweepsPersistence = persistence;
    }

    //////////////////////////////////////////////////////////////////////////
    ////                          protected methods                       ////
    
    /**
     * Read in a pxgraph format binary file.
     * @exception PlotDataException if there is a serious data format problem.
     * @exception java.io.IOException if an I/O error occurs.
     */	
    protected void _convertBinaryStream(DataInputStream in) 
	throws PlotDataException,  IOException
	{
	int c;
	boolean printedDataSet = false;

	// FIXME: The 'final' in the line below causes problems with jdk1.0.2
	// which we compile under for compatibility with Netscape3.x
	/*final*/ int MAX_DATASETS = 63; // Maximum number of datasets	

	String datasets[] = new String[MAX_DATASETS];
	_currentdataset = 0;
	try {
	    setConnected(false);
	    while (true) {
		// Here, we read pxgraph binary format data.
		// For speed reasons, the Ptolemy group extended 
		// pxgraph to read binary format data.
		// The format consists of a command character,
		// followed by optional arguments
		// d <4byte float> <4byte float> - Draw a X,Y point
		// e                             - End of a data set
		// n <chars> \n                  - New set name, ends in \n
		// m                             - Move to a point
		c = in.readByte();
		switch (c) {
		case 'd':
		    {
			// Data point.
			float x = in.readFloat();
			float y = in.readFloat();
			//if (debug) System.out.print(outputline);
			if (!printedDataSet) {
			    String datasetstring;
			    printedDataSet = true;
			    if (datasets[0] == null) {
				//setNumSets(2);
				addLegend(_currentdataset, "Set " +
					  _currentdataset++); 
			    } else {
				addLegend(_currentdataset++, datasets[0]);
			    }
			    addPoint(_currentdataset, x, y, false);
			} else {
			    addPoint(_currentdataset, x, y, true);
			}
		    }
		    break;
		case 'e':
		    // End of set name.
		    setConnected(false);
		    break;
		case 'n':
		    {
			StringBuffer datasetname = new StringBuffer();
			// New set name, ends in \n.
			while (c != '\n')
			    datasetname.append(in.readChar());
			addLegend(_currentdataset++, datasetname.toString());
			setConnected(true);
		    }
		    break;
		case 'm':
		    setConnected(false);
		    break;
		default:
		    throw new PlotDataException("Don't understand `" + c + 
						"'character in binary data");
		}
	    } 
	} catch (EOFException e) {}	    
	setConnected(false);
    }

    /**
     * Put a mark corresponding to the specified dataset at the
     * specified x and y position. If the fourth argument is true,
     * attempt to connect this point with the previous point by
     * drawing a line.  If the fifth argument is true, then check the
     * range and plot only points and that portion of the connecting
     * line that is in range.  If both the new point and the previous
     * point are out of range, no line is drawn.  Return true if the
     * point is actually drawn.  It is not drawn if either it is out
     * of range or there is no mark requested.
     */
    protected boolean _drawPoint(int dataset, int xpos, int ypos,
				boolean connected, boolean clip) {
                                    
        // Points are only distinguished up to 10 data sets.
        dataset %= 10;
        if (__pointsPersistence > 0) {
            // To allow erasing to work by just redrawing the points.
            graphics.setXORMode(Color.white);
        }
        if (_usecolor) {
            graphics.setColor(_colors[dataset]);
        } else {
            graphics.setColor(Color.black);
        }
        // If the point is not out of range, draw it.
        boolean pointinside = ypos <= _lry && ypos >= _uly &&
	    xpos <= _lrx && xpos >= _ulx;
        boolean showsomething = pointinside || !clip;
        if (showsomething) {
            switch (_marks) {
            case 1:
                // points -- use 3-pixel ovals.
                graphics.fillOval(xpos-1, ypos-1, 3, 3);
                break;
            case 2:
                // dots
                graphics.fillOval(xpos-__radius, ypos-__radius,
				   __diameter, __diameter); 
                break;
            case 3:
                // marks
                int xpoints[], ypoints[];
                switch (dataset) {
                    case 0:
                        // filled circle
                        graphics.fillOval(xpos-__radius, ypos-__radius,
					  __diameter, __diameter); 
                        break;
                    case 1:
                        // cross
                        graphics.drawLine(xpos-__radius, ypos-__radius,
					  xpos+__radius, ypos+__radius); 
                        graphics.drawLine(xpos+__radius, ypos-__radius,
					  xpos-__radius, ypos+__radius); 
                        break;
                    case 2:
                        // square
                        graphics.drawRect(xpos-__radius, ypos-__radius,
					  __diameter, __diameter); 
                        break;
                    case 3:
                        // filled triangle
                        xpoints = new int[4];
                        ypoints = new int[4];
                        xpoints[0] = xpos; ypoints[0] = ypos-__radius;
                        xpoints[1] = xpos+__radius; ypoints[1] = ypos+__radius;
                        xpoints[2] = xpos-__radius; ypoints[2] = ypos+__radius;
                        xpoints[3] = xpos; ypoints[3] = ypos-__radius;
                        graphics.fillPolygon(xpoints, ypoints, 4);
                        break;
                    case 4:
                        // diamond
                        xpoints = new int[5];
                        ypoints = new int[5];
                        xpoints[0] = xpos; ypoints[0] = ypos-__radius;
                        xpoints[1] = xpos+__radius; ypoints[1] = ypos;
                        xpoints[2] = xpos; ypoints[2] = ypos+__radius;
                        xpoints[3] = xpos-__radius; ypoints[3] = ypos;
                        xpoints[4] = xpos; ypoints[4] = ypos-__radius;
                        graphics.drawPolygon(xpoints, ypoints, 5);
                        break;
                    case 5:
                        // circle
                        graphics.drawOval(xpos-__radius, ypos-__radius,
					  __diameter, __diameter); 
                        break;
                    case 6:
                        // plus sign
                        graphics.drawLine(xpos, ypos-__radius, xpos,
					  ypos+__radius); 
                        graphics.drawLine(xpos-__radius, ypos, xpos+__radius,
					  ypos); 
                        break;
                    case 7:
                        // filled square
                        graphics.fillRect(xpos-__radius, ypos-__radius,
					  __diameter, __diameter); 
                        break;
                    case 8:
                        // triangle
                        xpoints = new int[4];
                        ypoints = new int[4];
                        xpoints[0] = xpos; ypoints[0] = ypos-__radius;
                        xpoints[1] = xpos+__radius; ypoints[1] = ypos+__radius;
                        xpoints[2] = xpos-__radius; ypoints[2] = ypos+__radius;
                        xpoints[3] = xpos; ypoints[3] = ypos-__radius;
                        graphics.drawPolygon(xpoints, ypoints, 4);
                        break;
                    case 9:
                        // filled diamond
                        xpoints = new int[5];
                        ypoints = new int[5];
                        xpoints[0] = xpos; ypoints[0] = ypos-__radius;
                        xpoints[1] = xpos+__radius; ypoints[1] = ypos;
                        xpoints[2] = xpos; ypoints[2] = ypos+__radius;
                        xpoints[3] = xpos-__radius; ypoints[3] = ypos;
                        xpoints[4] = xpos; ypoints[4] = ypos-__radius;
                        graphics.fillPolygon(xpoints, ypoints, 5);
                        break;
                }
                break;
            default:
                // none
            }
        }
        
        // Draw the impulse line to the x axis, if appropriate.
        // Do not draw them if the data is out of range along the x axis
        // or is below the y axis.
        if (__impulses && ypos <= _lry && xpos <= _lrx && xpos >= _ulx) {
            int topofline = ypos;
            if (ypos < _uly) {
                topofline = _uly;
            }
            graphics.drawLine(xpos, topofline, xpos, _lry);
        }

        // Draw the bars to the x axis, if appropriate.
        // Do not draw them if the data is out of range along the x axis
        // or is below the y axis.
        if (__bars && ypos <= _lry && xpos <= _lrx && xpos >= _ulx) {
            int topofbar = ypos;
            if (ypos < _uly) {
                topofbar = _uly;
            }
            // left x position of bar.
            int barlx = (int)(xpos - __barwidth * _xscale/2 +
                     (_numsets - dataset - 1) * __baroffset * _xscale);
            // right x position of bar
            int barrx = (int)(barlx + __barwidth * _xscale);
            if (barlx < _ulx) barlx = _ulx;
            if (barrx > _lrx) barrx = _lrx;
            graphics.fillRect(barlx, topofbar, barrx - barlx, _lry - topofbar);
        }

        int xstart = xpos;
        int ystart = ypos;
        int prevx = __prevx[dataset];
        int prevy = __prevy[dataset];

        // Draw a line to the previous point, if appropriate.
        if (connected) {
            if (clip) {
                 // Is the previous point in range?
                 boolean previnside = prevx >= _ulx && prevx <= _lrx &&
                         prevy >= _uly && prevy <= _lry;
                 // If the previous point is out of x range, adjust
                 // prev point to boundary.
                 int tmp;
                 if (pointinside) {
                     if (prevx < _ulx) {
                         prevy = prevy + ((ypos - prevy) *
					  (_ulx - prevx))/(xpos - prevx);
                         prevx = _ulx;
                     } else if (prevx > _lrx) {
                         prevy = prevy + ((ypos - prevy) *
					  (_lrx - prevx))/(xpos - prevx);
                         prevx = _lrx;
                     }

                     // If prev point is out of y range, adjust to boundary.
                     // Note that y increases downward
                     if (prevy < _uly) {
                         prevx = prevx + ((xpos - prevx) *
					  (_uly - prevy))/(ypos - prevy);
                         prevy = _uly;
                     } else if (prevy > _lry) {
                         prevx = prevx + ((xpos - prevx) *
					  (_lry - prevy))/(ypos - prevy);
                         prevy = _lry;
                     }
                 }

                 // If we are in line drawing mode, and the previous
                 // point was in range, but the current point is out
                 // of range, recompute the current point position to
                 // lie on the boundary of the plot
                 if (!pointinside && previnside) {
                      showsomething = true;
                      // Adjust current point to lie on the boundary.
                      if (xpos < _ulx) {
	                      ypos = ypos + ((prevy - ypos) *
					     (_ulx - xpos))/(prevx - xpos);
	                      xpos = _ulx;
                      } else if (xpos > _lrx) {
	                      ypos = ypos + ((prevy - ypos) *
					     (_lrx - xpos))/(prevx - xpos);
	                      xpos = _lrx;
                      }
                      if (ypos < _uly) {
	                      xpos = xpos + ((prevx - xpos) *
					     (_uly - ypos))/(prevy - ypos);
	                      ypos = _uly;
                      } else if (ypos > _lry) {
	                      xpos = xpos + ((prevx - xpos) *
					     (_lry - ypos))/(prevy - ypos);
	                      ypos = _lry;
                      }
                  }
             }

             if (showsomething) {
                 graphics.drawLine(xpos, ypos, prevx, prevy);
             }
        }

        __prevx[dataset] = xstart;
        __prevy[dataset] = ystart;
        graphics.setColor(Color.black);
        if (__pointsPersistence > 0) {
            // Restore paint mode in case axes get redrawn.
            graphics.setPaintMode();
        }
        return ((pointinside || !clip) && (_marks != 0));
    }
    
    //////////////////////////////////////////////////////////////////////////
    ////                       protected variables                        ////
    
    // The current dataset
    protected int _currentdataset = -1;
    
    // A vector of datasets.
    protected Vector[] _points;
    
    // An indicator of the marks style.  See parseLine method for
    // interpretation.
    protected int _marks;
    
    protected int _numsets = __MAX_DATASETS;

    //////////////////////////////////////////////////////////////////////////
    ////                       private methods                            ////
    
    /**
     * Draw the specified point. If the point is out of range, do nothing.
     */
    private void __drawPlotPoint(int dataset, PlotPoint pt) {
        int ypos = _lry - (int) ((pt.y - _yMin) * _yscale);
        int xpos = _ulx + (int) ((pt.x - _xMin) * _xscale);
        _drawPoint(dataset, xpos, ypos, pt.connected, true);
    }
    
    //////////////////////////////////////////////////////////////////////////
    ////                       private variables                          ////
    
    private int __pointsPersistence = 0;
    private int __sweepsPersistence = 0;
    private boolean __connected = true;
    private boolean __impulses = false;
    private boolean __firstinset = true;
    private boolean __bars = false;
    private double __barwidth = 0.5;
    private double __baroffset = 0.05;
    
    // Give both radius and diameter of a point for efficiency.
    private int __radius = 3;
    private int __diameter = 6;
    
    // Information about the previously plotted point.
    private int __prevx[], __prevy[];
    // Maximum number of _datasets.
    private static final int __MAX_DATASETS = 63;
}
