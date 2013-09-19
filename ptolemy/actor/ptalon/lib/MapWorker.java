/* A MapWorker actor, as a subsystem of the MapReduce system.

 Copyright (c) 2006-2013 The Regents of the University of California.
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

 */
package ptolemy.actor.ptalon.lib;

import java.util.List;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.StringToken;
import ptolemy.data.expr.StringParameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

///////////////////////////////////////////////////////////////////
////MapWorker

/**
 A MapWorker actor, as a subsystem of the MapReduce system.

 <p> This actor has a parameter <i>classNameForMap</i> which is the
 qualified name for a Java class that extends
 ptolemy.actor.ptalon.lib.MapReduceAlgorithm.  It must also have a no
 argument constructor.  By extending this abstract class, it will
 implement a method named <i>map</i> with type signature:

 <p>
 <code>public List&lt;KeyValuePair&gt; map(String key, String value)</code>

 <p> This method defines the Map algorithm for the MapReduce system.
 At each call, it should return list of KeyValuePairs, which is just a
 pair of strings, one representing a key for the reduce algorithm, and
 one representing a value for the reduce algorithm.  One instance of
 the MapReduce algorithm class will be created when the
 <i>classNameForMap</i> parameter is set.  It will be reused at each
 firing.  This actor inputs a key and value token and outputs key,
 value pairs for each pair in the list generated by the user defined
 map procedure.

 @author Adam Cataldo
 @version $Id$
 @since Ptolemy II 6.1
 @Pt.ProposedRating Red (cxh)
 @Pt.AcceptedRating Red (cxh)
 @see ptolemy.actor.ptalon.lib.KeyValuePair
 @see ptolemy.actor.ptalon.lib.MapReduceAlgorithm
 @author Adam Cataldo
 */

public class MapWorker extends TypedAtomicActor {

    /** Create a new actor in the specified container with the specified
     *  name.  The name must be unique within the container or an exception
     *  is thrown. The container argument must not be null, or a
     *  NullPointerException will be thrown.
     *
     *  @param container The container.
     *  @param name The name of this actor within the container.
     *  @exception IllegalActionException If this actor cannot be contained
     *   by the proposed container (see the setContainer() method).
     *  @exception NameDuplicationException If the name coincides with
     *   an entity already in the container.
     */
    public MapWorker(CompositeEntity container, String name)
            throws IllegalActionException, NameDuplicationException {
        super(container, name);

        classNameForMap = new StringParameter(this, "classNameForMap");
        classNameForMap.setExpression("ptolemy.actor.ptalon.lib.WordCount");

        inputKey = new TypedIOPort(this, "inputKey");
        inputKey.setInput(true);
        inputKey.setTypeEquals(BaseType.STRING);

        inputValue = new TypedIOPort(this, "inputValue");
        inputValue.setInput(true);
        inputValue.setTypeEquals(BaseType.STRING);

        outputKey = new TypedIOPort(this, "outputKey");
        outputKey.setOutput(true);
        outputKey.setTypeEquals(BaseType.STRING);

        outputValue = new TypedIOPort(this, "outputValue");
        outputValue.setOutput(true);
        outputValue.setTypeEquals(BaseType.STRING);

    }

    ///////////////////////////////////////////////////////////////////
    ////                     ports and parameters                  ////

    /** The qualifed class name for a Java class containing a method
     *  with signature:
     *  <p>
     *  <code>public static List&lt;String[]&gt; map(String key, String value)</code>
     *  <p>
     *  Each element of each returned list should be a length two array of
     *  Strings.
     */
    public StringParameter classNameForMap;

    /** A String input key.
     */
    public TypedIOPort inputKey;

    /** A String input value.
     */
    public TypedIOPort inputValue;

    /** A String output key.  For each pair of inputKey, inputValue
     *  tokens read in, a possibly empty list of outputKey, outputValue
     *  pairs will be outputs.
     */
    public TypedIOPort outputKey;

    /** A String output value.  For each pair of inputKey, inputValue
     *  tokens read in, a possibly empty list of outputKey, outputValue
     *  pairs will be outputs.
     */
    public TypedIOPort outputValue;

    ///////////////////////////////////////////////////////////////////
    ////                         public methods                    ////

    /** React to a change in an attribute.  This method is called by
     *  a contained attribute when its value changes.  In this base class,
     *  the method does nothing.  In derived classes, this method may
     *  throw an exception, indicating that the new attribute value
     *  is invalid.  It is up to the caller to restore the attribute
     *  to a valid value if an exception is thrown.  If the attribute
     *  changed is <i>classNameForMap</i>, update this actor accordingly.
     *  @param attribute The attribute that changed.
     *  @exception IllegalActionException If the change is not acceptable
     *   to this container.  If the class set in <i>classNameForMap<i/>
     *   does not exist, or if the class exists but does not contain a map
     *   method with an appropriate signature, this exception will be thrown.
     */
    public void attributeChanged(Attribute attribute)
            throws IllegalActionException {
        if (attribute == classNameForMap) {
            _setMapMethod();
        } else {
            super.attributeChanged(attribute);
        }
    }

    /** Read in a token on the <i>inputKey</i> and <i>inputValue</i>
     *  ports and output pairs of tokens on the <i>ouputKey</i>, <i>outputValue</i>
     *  ports.
     *
     *  @exception IllegalActionException If there is any trouble calling
     *  the map method.
     */
    public void fire() throws IllegalActionException {
        super.fire();
        if (_algorithm == null) {
            throw new IllegalActionException("No map method.");
        }
        String key = ((StringToken) inputKey.get(0)).stringValue();
        String value = ((StringToken) inputValue.get(0)).stringValue();
        List<KeyValuePair> output = null;
        try {
            output = _algorithm.map(key, value);
        } catch (Exception e) {
            throw new IllegalActionException(this, e,
                    "Unable to execute map method.");
        }
        for (KeyValuePair pair : output) {
            outputKey.send(0, new StringToken(pair.getKey()));
            outputValue.send(0, new StringToken(pair.getValue()));
        }
    }

    /** Return true if there is an available key token and value token
     *  on the <i>inputKey</i> and <i>inputValue</i> ports.
     *
     *  @return True if this actor is ready for firing, false otherwise.
     *  @exception IllegalActionException Not thrown in this class.
     */
    public boolean prefire() throws IllegalActionException {
        boolean returnValue = super.prefire();
        if (inputKey.hasToken(0) && inputValue.hasToken(0)) {
            return returnValue;
        }
        return false;
    }

    /** Extract the map method from the <i>classNameForMap</i> parameter.
     *
     *  @exception IllegalActionException If unable to extract an appropriate
     *  map method.
     */
    public void initialize() throws IllegalActionException {
        super.initialize();
        _setMapMethod();
    }

    ///////////////////////////////////////////////////////////////////
    ////                         private methods                   ////

    /**
     * Set the map method using the class specified in <i>classNameForMap<i>.
     * @exception IllegalActionException If the map method does not
     * exist, has the wrong type signature, or has the wrong
     * access modifiers.
     */
    private void _setMapMethod() throws IllegalActionException {
        String className = classNameForMap.stringValue();
        Class<?> mapClass = null;
        Class<?> algorithmClass = null;
        Class<?> objectClass = null;
        try {
            mapClass = Class.forName(className);
            algorithmClass = Class
                    .forName("ptolemy.actor.ptalon.lib.MapReduceAlgorithm");
            objectClass = Class.forName("java.lang.Object");

        } catch (ClassNotFoundException e) {
            throw new IllegalActionException("No class named " + className
                    + " could be found.");
        }
        Class<?> superClass = mapClass;
        while (!superClass.equals(objectClass)) {
            superClass = superClass.getSuperclass();
            if (superClass.equals(algorithmClass)) {
                break;
            }
            if (superClass.equals(objectClass)) {
                throw new IllegalActionException(className
                        + " is not a subclass of "
                        + "ptolemy.actor.ptalon.lib.MapReduceAlgorithm.");
            }
        }
        try {
            _algorithm = (MapReduceAlgorithm) mapClass.newInstance();
        } catch (IllegalAccessException e) {
            throw new IllegalActionException(className
                    + " does not have a no argument constructor");
        } catch (InstantiationException e) {
            throw new IllegalActionException(className + " is abstract.");
        } catch (ClassCastException e) {
            throw new IllegalActionException("Unable to cast instance of "
                    + className
                    + " to ptolemy.actor.ptalon.lib.MapReduceAlgorithm.");
        }
    }

    ///////////////////////////////////////////////////////////////////
    ////                        private members                    ////

    private MapReduceAlgorithm _algorithm;

}
