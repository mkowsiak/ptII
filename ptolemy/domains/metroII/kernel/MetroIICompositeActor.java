/*
Below is the copyright agreement for the Ptolemy II system.

Copyright (c) 1995-2013 The Regents of the University of California.
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

Ptolemy II includes the work of others, to see those copyrights, follow
the copyright link on the splash page or see copyright.htm.
*/
package ptolemy.domains.metroII.kernel;

import java.util.Iterator;

import net.jimblackler.Utils.CollectionAbortedException;
import net.jimblackler.Utils.Collector;
import net.jimblackler.Utils.ResultHandler;
import net.jimblackler.Utils.ThreadedYieldAdapter;
import net.jimblackler.Utils.YieldAdapterIterable;
import ptolemy.actor.CompositeActor;
import ptolemy.actor.Director;
import ptolemy.actor.IOPort;
import ptolemy.actor.TypedCompositeActor;
import ptolemy.actor.parameters.ParameterPort;
import ptolemy.domains.metroII.kernel.util.ProtoBuf.metroIIcomm.Event;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Workspace;

public class MetroIICompositeActor extends TypedCompositeActor implements
        MetroIIEventHandler {

    public MetroIICompositeActor() {
        // TODO Auto-generated constructor stub
    }

    public MetroIICompositeActor(Workspace workspace) {
        super(workspace);
        // TODO Auto-generated constructor stub
    }

    public MetroIICompositeActor(CompositeEntity container, String name)
            throws IllegalActionException, NameDuplicationException {
        super(container, name);
        // TODO Auto-generated constructor stub
    }

    public YieldAdapterIterable<Iterable<Event.Builder>> adapter() {
        return new ThreadedYieldAdapter<Iterable<Event.Builder>>()
                .adapt(new Collector<Iterable<Event.Builder>>() {
                    public void collect(
                            ResultHandler<Iterable<Event.Builder>> resultHandler)
                            throws CollectionAbortedException {
                        getfire(resultHandler);
                    }
                });
    }

    public void getfire(ResultHandler<Iterable<Event.Builder>> resultHandler)
            throws CollectionAbortedException {
        try {

            if (_debugging) {
                _debug("Calling fire()");
            }

            try {
                _workspace.getReadAccess();

                // First invoke piggybacked methods.
                //                    if (_piggybacks != null) {
                //                        // Invoke the fire() method of each piggyback.
                //                        for (Executable piggyback : _piggybacks) {
                //                            piggyback.fire();
                //                        }
                //                    }
                //                    if (_derivedPiggybacks != null) {
                //                        // Invoke the fire() method of each piggyback.
                //                        for (Executable piggyback : _derivedPiggybacks) {
                //                            piggyback.fire();
                //                        }
                //                    }

                if (!isOpaque()) {
                    throw new IllegalActionException(this,
                            "Cannot fire a non-opaque actor.");
                }

                _transferPortParameterInputs();
                
                Director _director = getDirector();
                // Use the local director to transfer inputs from
                // everything that is not a port parameter.
                // The director will also update the schedule in
                // the process, if necessary.
                for (Iterator<?> inputPorts = inputPortList().iterator(); inputPorts
                        .hasNext() && !_stopRequested;) {
                    IOPort p = (IOPort) inputPorts.next();

                    if (!(p instanceof ParameterPort)) {
                        _director.transferInputs(p);
                    }
                }

                if (_stopRequested) {
                    return;
                }

                // _director.fire();
                if (_director instanceof MetroIIEventHandler) {
                    ((MetroIIEventHandler) _director).getfire(resultHandler);
                } else {
                    _director.fire();
                }

                if (_stopRequested) {
                    return;
                }

                // Use the local director to transfer outputs.
                _director.transferOutputs();
            } finally {
                _workspace.doneReading();
            }

            if (_debugging) {
                _debug("Called fire()");
            }
        } catch (IllegalActionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

}
