/* An actor for the dynamic dataflow model of computation.

Copyright (c) 1998-2004 The Regents of the University of California.
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

package ptolemy.domains.ddf.kernel;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.data.IntToken;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;


//////////////////////////////////////////////////////////////////////////
///DDFActor
/**
 * @author Gang Zhou
 *
 */
public class DDFActor extends TypedAtomicActor {
	
	/**
	 * @param container
	 * @param name
	 * @throws IllegalActionException
	 * @throws NameDuplicationException
	 */
	public DDFActor(CompositeEntity container, String name)
			throws IllegalActionException, NameDuplicationException {
		super(container, name);
		
		firingsPerIteration = new Parameter(this, "firingsPeriteration");
		firingsPerIteration.setTypeEquals(BaseType.INT);
		firingsPerIteration.setToken(new IntToken(0));
	}
	
    ///////////////////////////////////////////////////////////////////
    ////                     ports and parameters                  ////
    public Parameter firingsPerIteration;
}
