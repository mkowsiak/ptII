/* Language independent code generator for Ptolemy Ports.

 Copyright (c) 2008 The Regents of the University of California.
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

package ptolemy.codegen.kernel;

import ptolemy.actor.Director;
import ptolemy.kernel.util.IllegalActionException;

/**
 * @author Man-kit Leungoe
 * @version $Id$
 * @since Ptolemy II 7.1
 * @Pt.ProposedRating Red (cxh)
 * @Pt.AcceptedRating Red (cxh)
 */
public interface PortCodeGenerator extends ComponentCodeGenerator {
    /**Generate the expression that represents the offset in the generated
     * code.
     * @param offsetThe specified offset from the user.
     * @param channel The referenced port channel.
     * @param isWrite Whether to generate the write or read offset.
     * @return The expression that represents the offset in the generated code.
     * @exception IllegalActionException If there is problems getting the port
     *  buffer size or the offset in the channel and offset map.
     */
    public String generateOffset(String offset, int channel, boolean isWrite, 
            Director directorHelper) throws IllegalActionException;

    /** Generate the get code.
     *  @param channel The channel for which the get code is generated.
     *  @return The code that gets data from the channel.
     */ 
    public String generateCodeForGet(String channel) throws IllegalActionException;

    /** Generate the send code.
     *  @param channel The channel for which the send code is generated.
     *  @param dataToken The token to be sent
     *  @return The code that sends the dataToken on the channel.
     */ 
    public String generateCodeForSend(String channel, String dataToken)     
	throws IllegalActionException;

    /** Update the read offset.
     *  @param rate  The rate of the channels.
     *  @param directorHelper The Director helper
     *  @return The offset.
     */
    public String updateOffset(int rate, Director directorHelper) throws IllegalActionException;

    /** Update the write offset of the [multiple] connected ports.
     */
    public Object updateConnectedPortsOffset(int rate, Director director) throws IllegalActionException;


    /** Initialize the offsets. 
     *  @return The code to initialize the offsets.
     */
    public String initializeOffsets() throws IllegalActionException;
}
