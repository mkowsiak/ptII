/* Ptolemy II Version identifiers

 Copyright (c) 2001 The Regents of the University of California.
 All rights reserved.
 Permission is hereby granted, without written agreement and without
 license or royalty fees, to use, copy, modify, and distribute this
 software and its documentation for any purpose, provided that the above
 copyright notice and the following two paragraphs appear in all copies
 of this software.

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

@ProposedRating Red (cxh@eecs.berkeley.edu)
@AcceptedRating Red (cxh@eecs.berkeley.edu)
*/

package ptolemy.kernel.util;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

//////////////////////////////////////////////////////////////////////////
//// VersionAttribute
/** 
An attribute that identifies the version of an object.  
The value of the attribute contains a String version-id that represents
the version.
A version-id is a string with substrings separated by one of '.', '-' or '_'.
The substrings may consist of any characters except space.  Version-ids can
be compared against each other other with the compareTo() method.

<p>The JNLP specification at
<a href="http://jcp.org/jsr/detail/056.jsp"><code>http://jcp.org/jsr/detail/056.jsp</code></a>
gives the following syntax for version-ids:
<pre>
version-id ::= string ( separator string ) *
string ::= char ( char ) *
char ::= Any ASCII character except a space, a separator or a
modifier
separator ::= "." | "-" | "_"
</pre>
Valid version-id include "1.3", "1.3.1", "1.3-beta_01".

<p>The JNLP specification includes version-strings, which are used for
matching one or more version-ids in a fashion similar to wildcard
matches within a regular expression.  At this time, this class does
not implement version-strings.

<p>
@author Christopher Hylands
@version $Id$ */
public class VersionAttribute
        extends StringAttribute implements Settable, Comparable {
    
    /** Construct an object in the default workspace with the empty string
     *  as its name. The object is added to the list of objects in the
     *  workspace. Increment the version number of the workspace.
     *  @param expression The initial value of this parameter, set
     *   using setExpression().
     *  @exception IllegalActionException If the value is of the
     *   incorrect format.
     *  @see #setExpression(string)
     */
    public VersionAttribute(String expression) throws IllegalActionException {
	super();
	setExpression(expression);
    }

    /** Construct an attribute with the given name contained by the
     *  specified container. The container argument must not be null, or a
     *  NullPointerException will be thrown.  This attribute will use the
     *  workspace of the container for synchronization and version counts.
     *  If the name argument is null, then the name is set to the empty
     *  string. Increment the version of the workspace.
     *  @param container The container.
     *  @param name The name of this attribute.
     *  @exception IllegalActionException If the attribute is not of an
     *   acceptable class for the container, or if the name contains a period.
     *  @exception NameDuplicationException If the name coincides with
     *   an attribute already in the container.
     */
    public VersionAttribute(NamedObj container, String name)
            throws IllegalActionException, NameDuplicationException {
        super(container, name);
        _tupleList = new LinkedList();
    }

    ///////////////////////////////////////////////////////////////
    ////                     public methods                    ////

    /** Compare the value of this VersionAttribute against the argument
     *  according to the VersionAttribute syntax and padding rules.  For
     *  example:
     *  <p> "1.2.2-005" is greater than "1.2.2.4", 
     *  <br> "1.3.1" is an greater than "1.3"
     *  <br> "1.3-beta" is an greater than "1.3-alpha"
     *  <b>
     *  Version-id contain one or more elements. When two version-id's
     *  are compared, they are normalized by padding the shortest
     *  version-id with additional elements containing "0".
     *  During comparison, if both elements can be parsed as Java
     *  <code>int</code>s, then they are compared as integers.  If the
     *  elements cannot be parsed as integers, they are compared as Strings.
     *
     *  @param object The VersionAttribute to compare against. 
     *  @return 0 if the argument is an exact match according to the
     *   version syntax and padding rules, a number less than 0 if the
     *   argument is less than this version, a number greater than 0 if
     *   the argument is greater than this version.
     */
    public int compareTo(Object object) {
	VersionAttribute version = (VersionAttribute) object;
        Iterator versionTuples = version.iterator();
        Iterator tuples = _tupleList.iterator();
        while (versionTuples.hasNext() || tuples.hasNext()){
            String versionTuple, tuple;

            // FIXME: deal with * and + in the JNLP Version String spec.

            // Normalize the shortest tuple by padding with 0
            if (versionTuples.hasNext()) {
                versionTuple = (String)versionTuples.next();
            } else {
                versionTuple = "0";
            }
            if (tuples.hasNext()) {
                tuple = (String)tuples.next();
            } else {
                tuple = "0";
            }

	    // If both elements can be parsed as Java ints, then
	    // compare them as ints.  If not, then compare them
	    // as Strings.

	    try {
		// Try parsing as ints.
		int tupleInt = Integer.parseInt(tuple);
		int versionInt = Integer.parseInt(versionTuple);
		if (tupleInt < versionInt) {
		    return -1;
		} else if (tupleInt > versionInt) {
		    return 1;
		}
	    } catch (NumberFormatException ex) {
		// Compare as Strings.
		int compare = tuple.compareTo(versionTuple);
		if (compare < 0) {
		    return -1;
		} else if (compare > 0) {
		    return 1;
		}
	    }
        }
	return 0;
    }
    
    /** Return an iterator over the elements of the version,
     *  each of which is a String.
     *  @return An iterator over the elements of the version.
     */
    public Iterator iterator() {
	return _tupleList.iterator();
    }

    /** Set the value of the string attribute and notify the container
     *  of the value of this attribute by calling attributeChanged().
     *  Notify any value listeners of this attribute.
     *  @param expression The version string, consisting of 
     *   version ID tuples separated by '.', '-' or '_'. For example:
     *   "1.2", "1.2_beta-4".
     *  @exception IllegalActionException If the argument contains a
     *   space, which violates the JNLP Version format specification
     */
    public void setExpression(String expression)
	throws IllegalActionException {
	super.setExpression(expression);
        if (expression.indexOf(' ') != -1 ) {
            throw new IllegalActionException(this,
                    "Versions cannot contain spaces: '"
                    + expression + "'");
        }
	_tupleList = new LinkedList();
        StringTokenizer tokenizer = new StringTokenizer(expression, ".-_");
        while (tokenizer.hasMoreTokens()) {
            _tupleList.add(tokenizer.nextToken());
        }
        Iterator tuples = _tupleList.iterator();
    }

    ///////////////////////////////////////////////////////////////
    ////                     public variables                  ////


    /** The VersionAttribute that contains the version of the Ptolemy II
     *  release that is currently running.  This variable may be read
     *  to change the Ptolemy II functionality depending on the
     *  version number:
     *  <pre>
     *  if (VersionAttribute.CURRENT_VERSION.compareTo("2.0") >= 0 ) {
     *      // Perform some operation if the current version is
     *      // Ptolemy II 2.0 or later.
     *  }
     *  </pre>
     */ 
    public static final VersionAttribute CURRENT_VERSION;

    static {
	try {
	    CURRENT_VERSION = new VersionAttribute("2.0-devel");

	} catch (Exception ex) {
	    throw new ExceptionInInitializerError("Failed to create "
						  + "CURRENT_VERSION: "
						  + KernelException
						  .stackTraceToString(ex));
	}
    }

    ///////////////////////////////////////////////////////////////
    ////                     private variables                  ////

    // A List representation of the version.
    private List _tupleList;
}
