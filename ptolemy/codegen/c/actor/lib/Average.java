/*
  @Copyright (c) 2005 The Regents of the University of California.
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
/*
 * Created on Feb 21, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package ptolemy.codegen.c.actor.lib;

import ptolemy.codegen.kernel.CCodeGeneratorHelper;
import ptolemy.kernel.util.IllegalActionException;

/**
 * @author Man-Kit Leung
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class Average extends CCodeGeneratorHelper {

    /**
     * @param component
     */
    public Average(ptolemy.actor.lib.Average actor) {
        super(actor);
    }

    public void  generateFireCode(StringBuffer stream)
            throws IllegalActionException {

        ptolemy.actor.lib.Sequence actor =
            (ptolemy.actor.lib.Sequence)getComponent();

        StringBuffer tmpStream = new StringBuffer();

        tmpStream.append(
                "if ($val(reset)) {\n"
                + "    sum = 0;\n"
                + "    count = 0;\n"
                + "} else {\n"
                + "    sum += $val(input);\n"
                + "    count++;\n"
                + "    $val(output) = sum / count;\n"
                + "}\n");


        _codeBlock = tmpStream.toString();
        stream.append(processCode(_codeBlock));
    }

    public String generateInitializeCode()
            throws IllegalActionException {
        return processCode(_initBlock);
    }


    ///////////////////////////////////////////////////////////////////
    ////                     protected variables                   ////

    protected String _codeBlock;

    protected String _initBlock =
    "int sum = 0;\n"
    + "int count = 0;\n";
}
