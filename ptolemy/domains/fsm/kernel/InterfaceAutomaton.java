/* An Interface Automaton.

 Copyright (c) 1999-2001 The Regents of the University of California.
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
@ProposedRating Yellow (liuxj@eecs.berkeley.edu)
@AcceptedRating Yellow (kienhuis@eecs.berkeley.edu)
*/

package ptolemy.domains.fsm.kernel;

import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.ComponentPort;
import ptolemy.kernel.ComponentRelation;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.InternalErrorException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Settable;
import ptolemy.kernel.util.Workspace;
import ptolemy.actor.IOPort;
import ptolemy.data.expr.Parameter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.HashSet;
import java.util.Set;

//////////////////////////////////////////////////////////////////////////
//// InterfaceAutomaton
/**
This class models an Interface Automaton. Interface automata is an automata
model defined by Luca de Alfaro in the paper "Interface Automata". 
An InterfaceAutomaton contains a set of states and
InterfaceAutomatonTransitions. There are three kinds transitions:
input transition, output transition, and internal transitions.
The input and output transitions correspond to input and output ports,
respectively. These ports are added by the user. The internal transition
correspond to a parameter in this InterfaceAutomaton. The parameter is
added automatically when the internal transition is added.
<p>
When an InterfaceAutomaton is fired, the outgoing transitions of the current
state are examined. An IllegalActionException is thrown if there is more than
one enabled transition. If there is exactly one enabled transition then it is
taken.
<p>
An InterfaceAutomaton enters its initial state during initialization. The
name of the initial state is specified by the <i>initialStateName</i> string
attribute.
<p>

@author Yuhong Xiong, Xiaojun Liu and Edward A. Lee
@version $Id$
@see State
@see InterfaceAutomatonTransition
*/

// FIXME: Are interface automata that are fired required to be deterministic?
// or just randomly choose a transition.

public class InterfaceAutomaton extends FSMActor {

    /** Construct an InterfaceAutomaton in the default workspace with an
     *  empty string as its name. Add the actor to the workspace directory.
     *  Increment the version number of the workspace.
     */
    public InterfaceAutomaton() {
        super();
    }

    /** Construct an InterfaceAutomaton in the specified workspace with an
     *  empty string as its name. The name can be changed later with
     *  setName().
     *  If the workspace argument is null, then use the default workspace.
     *  Add the actor to the workspace directory.
     *  Increment the version number of the workspace.
     *  @param workspace The workspace that will list the actor.
     */
    public InterfaceAutomaton(Workspace workspace) {
        super(workspace);
    }

    /** Create an InterfaceAutomaton in the specified container with the
     *  specified name. The name must be unique within the container or an
     *  exception is thrown. The container argument must not be null, or a
     *  NullPointerException will be thrown.
     *  @param container The container.
     *  @param name The name of this automaton within the container.
     *  @exception IllegalActionException If the entity cannot be contained
     *   by the proposed container.
     *  @exception NameDuplicationException If the name coincides with
     *   an entity already in the container.
     */
    public InterfaceAutomaton(CompositeEntity container, String name)
            throws IllegalActionException, NameDuplicationException {
        super(container, name);
    }

    ///////////////////////////////////////////////////////////////////
    ////                         public variables                  ////

    ///////////////////////////////////////////////////////////////////
    ////                         public methods                    ////

    /** Return a new InterfaceAutomaton that is the composition of the
     *  specified InterfaceAutomaton and this one.
     *  @param automaton An InterfaceAutomaton to compose with this one.
     *  @return An InterfaceAutomaton that is the composition.
     *  @exception IllegalActionException If this automaton is not composable
     *   with the argument.
     */
    public InterfaceAutomaton compose(InterfaceAutomaton automaton)
                throws IllegalActionException {

	this._check();
	automaton._check();

	// check composability
	_checkComposability(automaton);

	// computes the input, output, and internal transitions of the
	// composition
	Set[] sets = _computeCompositionTransitions(automaton);
        Set inputs = sets[0];
	Set outputs = sets[1];
	Set internals = sets[2];

// step 1: compute product
/* cases in frontier exploration at state p x q: for a transition from T from p:
(1) T is input for p:
    (1A) T is input of product: add to product
    (1B) T is shared:
        (1Ba) q has T ouput: add to product
	(1Bb) q does not have T output: transition cannot happen in
	                                product. ignore
(2) T is output for p:
    (2A) T is output of product: add to product
    (2B) T is shared:
        (2Ba) q has T input: add to product
	(2Bb) q does not have T input: mark p x q as illegal. stop exploring
	                               from p x q

(3) T is internal for p: add to product

The cases for q is symmetric, but be careful not to add shared transition
twice.
*/

// step2: prune out illegal states.
// need step1 to mark illegal states.

// step3: eliminate states disconnected to initial state.


        // First computes the product automaton, then prunes the illegal
        // states.
        InterfaceAutomaton product = _computeProduct(automaton);


        return null;
    }

    /** Choose the enabled transition among the outgoing transitions of
     *  the current state. Throw an exception if there is more than one
     *  transition enabled.
     *  @exception IllegalActionException If there is more than one
     *   transition enabled.
     */
    public void fire() throws IllegalActionException {
        super.fire();
    }

    /** Return the names of the input ports as a Set.
     *  @return A Set containing all the input port names.
     */
    public Set inputNameSet() {
	Set set = new HashSet();
	Iterator iterator = inputPortList().iterator();
	while (iterator.hasNext()) {
	    IOPort port = (IOPort)iterator.next();
	    set.add(port.getName());
	}
	return set;
    }

    /** Return the names of the internal transitions as a Set.
     *  @return A Set containing all the internal transition names.
     */
    // This method differs from inputNameSet() and outputNameSet() in that
    // those methods return the names of the input or output ports, but this
    // one does not get the names from the parameters corresponding to the
    // internal transitions. As a result, all the returned names have one or
    // more corresponding internal transition instances. The is because
    // (1) Unlike the relation between input/output transitions and ports,
    // where some ports may not have corresponding instances of transition,
    // it does not make sense to have any "internal transition parameter"
    // that does not have transition instances; (2) there is no way to tell
    // which parameter is for internal transition, and which is for other
    // purpose.
    public Set internalTransitionNameSet() {
	Set set = new HashSet();
	Iterator iterator = relationList().iterator();
	while (iterator.hasNext()) {
	    InterfaceAutomatonTransition transition =
	        (InterfaceAutomatonTransition)iterator.next();
	    String label = transition.getLabel();
	    if (label.endsWith(";")) {
	        String name = label.substring(0, label.length()-1);
		set.add(name);
	    }
	}
	return set;
    }

    /** Create a new instance of InterfaceAutomatonTransition with the
     *  specified name in this actor, and return it.
     *  This method is write-synchronized on the workspace.
     *  @param name The name of the new transition.
     *  @return An InterfaceAutomatonTransition with the given name.
     *  @exception IllegalActionException If the name argument is null.
     *  @exception NameDuplicationException If name collides with that
     *   of a transition already in this actor.
     */
    public ComponentRelation newRelation(String name)
            throws IllegalActionException, NameDuplicationException {
        try {
            workspace().getWriteAccess();
            InterfaceAutomatonTransition transition =
                    new InterfaceAutomatonTransition(this, name);
            return transition;
        } finally {
            workspace().doneWriting();
        }
    }

    /** Return the names of the output ports as a Set.
     *  @return A Set containing all the output port names.
     */
    public Set outputNameSet() {
	Set set = new HashSet();
	Iterator iterator = outputPortList().iterator();
	while (iterator.hasNext()) {
	    IOPort port = (IOPort)iterator.next();
	    set.add(port.getName());
	}
	return set;
    }

    ///////////////////////////////////////////////////////////////////
    ////                         protected methods                 ////

    /** Add an InterfaceAutomatonTransition to this InterfaceAutomaton.
     *  This method should not be used directly.  Call the setContainer()
     *  method of the transition instead. This method does not set the
     *  container of the transition to refer to this container. This method
     *  is <i>not</i> synchronized on the workspace, so the caller should be.
     *
     *  @param relation The InterfaceAutomatonTransition to contain.
     *  @exception IllegalActionException If the transition has no name, or
     *   is not an instance of Transition.
     *  @exception NameDuplicationException If the name collides with a name
     *   already on the contained transitions list.
     */
    protected void _addRelation(ComponentRelation relation)
            throws IllegalActionException, NameDuplicationException {
        if (!(relation instanceof InterfaceAutomatonTransition)) {
            throw new IllegalActionException(this, relation,
                    "InterfaceAutomaton can only contain instances of "
                    + "InterfaceAutomatonTransition.");
        }
        super._addRelation(relation);
    }

    ///////////////////////////////////////////////////////////////////
    ////                         protected variables               ////

    ///////////////////////////////////////////////////////////////////
    ////                         private methods                   ////

    // Check if this automaton is consistent. The automaton is consistent
    // if all input transitions have a corresponding input port, all
    // output transitions have a corresponding output port, and all
    // internal transitions have a corresponding parameter.
    // If this automaton is not consistent, an exception is thrown;
    // otherwise, this method just returns.
    //
    private void _check() throws IllegalActionException {
	Iterator iterator = relationList().iterator();
	while (iterator.hasNext()) {
	    InterfaceAutomatonTransition transition =
	        (InterfaceAutomatonTransition)iterator.next();
	    String label = transition.getLabel();
	    String name = label.substring(0, label.length()-1);
	    if (label.endsWith("?")) {
	        IOPort port = (IOPort)getPort(name);
		if (port == null || port.isInput() == false) {
		    throw new IllegalActionException(
		        "InterfaceAutomaton._check: The input transition "
			+ name + " does not have a corresponding input port.");
		}
	    } else if (label.endsWith("!")) {
	        IOPort port = (IOPort)getPort(name);
		if (port == null || port.isOutput() == false) {
		    throw new IllegalActionException(
		        "InterfaceAutomaton._check: The input transition "
			+ name + " does not have a corresponding output port.");
		}
	    } else if (label.endsWith(";")) {
	        Attribute attribute = getAttribute(name);
		if (attribute == null || !(attribute instanceof Parameter)) {
		    throw new IllegalActionException(
		        "InterfaceAutomaton._check: The internal transition "
			+ name + " does not have a corresponding Parameter.");
		}
	    } else {
	        throw new InternalErrorException(
		    "InterfaceAutomaton._check: The label " + label
		        + " does not end with ?, !, or ;.");
	    }
	}
    }

    // Throw an exception if this automaton and the specified one is not
    // composable.
    private void _checkComposability(InterfaceAutomaton automaton)
            throws IllegalActionException {
	String message = "InterfaceAutomaton._checkComposability: "
		+ this.getFullName() + " is not composable with "
		+ automaton.getFullName() + " because ";

	// check the internal transitions of one do not overlap with the
	// transitions of the other
        Set thisInternals = this.internalTransitionNameSet();

        Set thatInputs = automaton.inputNameSet();
        Set thatOutputs = automaton.outputNameSet();
        Set thatInternals = automaton.internalTransitionNameSet();

	thatInputs.retainAll(thisInternals);
	thatOutputs.retainAll(thisInternals);
	thatInternals.retainAll(thisInternals);

	if ( !thatInputs.isEmpty() || 
	     !thatOutputs.isEmpty() ||
	     !thatInternals.isEmpty()) {
	    throw new IllegalActionException(message + "the internal "
	        + "transitions of the former overlaps with the transitions "
		+ "of the latter.");
	}

        thatInternals = automaton.internalTransitionNameSet();

        Set thisInputs = this.inputNameSet();
        Set thisOutputs = this.outputNameSet();
        thisInternals = this.internalTransitionNameSet();

	thisInputs.retainAll(thatInternals);
	thisOutputs.retainAll(thatInternals);
	thisInternals.retainAll(thatInternals);
	if ( !thisInputs.isEmpty() ||
	     !thisOutputs.isEmpty() ||
	     !thisInternals.isEmpty()) {
	    throw new IllegalActionException(message + "the internal "
	        + "transitions of the latter overlaps with the transitions "
		+ "of the former.");
	}

	// check the input transitions do not overlap
        thisInputs = this.inputNameSet();
        thatInputs = automaton.inputNameSet();
	thisInputs.retainAll(thatInputs);
	if ( !thisInputs.isEmpty()) {
	    throw new IllegalActionException(message + "the input "
	        + "transitions of the two overlap.");
	}

	// check the output transitions do not overlap
        thisOutputs = this.outputNameSet();
        thatOutputs = automaton.outputNameSet();
	thisOutputs.retainAll(thatOutputs);
	if ( !thisOutputs.isEmpty()) {
	    throw new IllegalActionException(message + "the output "
	        + "transitions of the two overlap.");
	}
    }

    // Compute the input, output, and internal transitions of the composition.
    // Return the result in an array of sets, in that order.
    private Set[] _computeCompositionTransitions(InterfaceAutomaton automaton) {
        Set[] sets = new Set[3];

	// compute shared transitions
	Set shared = this.inputNameSet();
	Set thatOutputs = automaton.outputNameSet();
	shared.retainAll(thatOutputs);

	Set shared1 = this.outputNameSet();
	Set thatInputs = automaton.inputNameSet();
	shared1.retainAll(thatInputs);

	shared.addAll(shared1);

	// compute input, output, and internal transitions
	sets[0] = this.inputNameSet();
	sets[0].addAll(automaton.inputNameSet());
	sets[0].removeAll(shared);

	sets[1] = this.outputNameSet();
	sets[1].addAll(automaton.outputNameSet());
	sets[1].removeAll(shared);

	sets[2] = this.internalTransitionNameSet();
	sets[2].addAll(automaton.internalTransitionNameSet());
	sets[2].addAll(shared);

	return sets;
    }

    // Compute the product of this autmaton and the argument.
    // Use frontier exploration. The frontier is represented by a HashMap
    // frontier. The key is the name of the state in the product, the value
    // is a Triple: productState, stateInThis, stateInArgument. The keys
    // are used to easily check if a product state is in the frontier.
    //
    // init: set the product to empty
    //       product = frontier = (this.initialState x automaton.initialSate, 
    //                             this.initialState, automaton.initialState)
    // iterate: pick a currentState from frontie
    //          for each transition from the currentState:
    //              (1) check composability;
    //              (2) if destination state not in product, add it to
    //                  both the product and the frontier
    //              (3) add the transition to the product.
    //          remove currentState from frontier
    // end: when frontier is empty
    //
    // The name of the states in the product is formed by the 
    // <nameInThisAutomaton>_&_<nameInArgumentAutomaton>
    //
    private InterfaceAutomaton _computeProduct(InterfaceAutomaton automaton)
            throws IllegalActionException {
        try {
            // init
            InterfaceAutomaton product = new InterfaceAutomaton();
            HashMap frontier = new HashMap();

            State currentStateThis = this.getInitialState();
            State currentStateArgument = automaton.getInitialState();
            String name = currentStateThis.getName() + "_&_"
                              + currentStateArgument.getName();
            // set container to null now so it is not in the product automaton
            State currentState = new State(null, name);
            Triple triple = new Triple(currentState, currentStateThis,
                                       currentStateArgument);
            frontier.put(name, triple);

            // iterate
            while ( !frontier.isEmpty()) {
                // pick a value from frontier. It seems that there isn't an
                // easy way to pick an arbitrary entry from a HashMap, except
                // through Iterator
                Iterator iterator = frontier.keySet().iterator();
                name = (String)iterator.next();
                triple = (Triple)frontier.get(name);

                // extend frontier from state in this automaton
                currentStateThis = triple._stateInThis;
                ComponentPort outPort = currentStateThis.outgoingPort;
                Iterator transitions = outPort.linkedRelationList().iterator();
                while (transitions.hasNext()) {
                    InterfaceAutomatonTransition transition =
                            (InterfaceAutomatonTransition)transitions.next();
		    // process non-shared transitions
                    int transitionType = transition.getTransitionType();
                    if (transitionType ==
                            InterfaceAutomatonTransition.INPUT_TRANSITION) {


                    }
                    else if (transitionType ==
                            InterfaceAutomatonTransition.OUTPUT_TRANSITION) {


                    } else if (transitionType ==
                            InterfaceAutomatonTransition.INTERNAL_TRANSITION) {


                    } else {
                        throw new InternalErrorException(
                            "InterfaceAutomaton._computeProduct: unrecognized "
                            + "transition type.");
                    }
                }
            }

            return null;
        } catch (NameDuplicationException exception) {
            // FIXME: this can actually happen, although extremly unlikely.
            // Eg. this automaton has states "a" and "b_&_c", the argument
            // has "a_&_b" and "c". Do we need to worry about this?
            throw new InternalErrorException(
                "InterfaceAutomaton._computeProduct: name in product "
		+ "automaton clashes: " + exception.getMessage());
	}
    } 

    ///////////////////////////////////////////////////////////////////
    ////                         private variables                 ////
    
    ///////////////////////////////////////////////////////////////////
    ////                            inner class                    ////
    private class Triple {
        private Triple(State productState, State stateInThis,
                                                State stateInArgument) {
            _productState = productState;
            _stateInThis = stateInThis;
            _stateInArgument = stateInArgument;
        }

        private State _productState = null;
        private State _stateInThis = null;
        private State _stateInArgument = null;
    }
}
