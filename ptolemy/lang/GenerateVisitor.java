/* Generate a visitor

 Copyright (c) 2000 The Regents of the University of California.
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

package ptolemy.lang;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.StringTokenizer;

//////////////////////////////////////////////////////////////////////////
//// GenerateVisitor
/**  Generate visitor classes.
@author Jeff Tsay
@version $Id$
 */
public class GenerateVisitor {

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println("Usage : GenerateVisitor TypeNameListFile " +
                    "[VisitorClassName] [BaseNodeName] [node path]");
            return;
        }

        GenerateVisitor genVisitor = new GenerateVisitor(args);

        genVisitor.generate();
    }

    public GenerateVisitor(String[] args) throws IOException {

        File fsrc  = new File(args[0]);

        String visitorOutFile;
        if (args.length < 2) {
            _visitorClassName = "Visitor";
            visitorOutFile = "Visitor.java";
        } else {
            _visitorClassName = args[1];
            visitorOutFile = _visitorClassName + ".java";
        }

        if (args.length < 3) {
            _baseNodeName = "TreeNode";
        } else {
            _baseNodeName = args[2];
        }

        if (args.length < 4) {
            _nodePath = ".";
        } else {
            _nodePath = args[3];
        }

        if (_nodePath.charAt(_nodePath.length() - 1) != File.separatorChar) {
            _nodePath = _nodePath + File.separatorChar;
        }

        File fdest = new File(visitorOutFile);

        if (!fdest.createNewFile()) {
            fdest.delete();
            fdest = new File(visitorOutFile);
            fdest.createNewFile();
        }

        try {
            _ofs = new FileWriter(fdest);
        } catch (FileNotFoundException e) {
            System.err.println("Couldn't open destination file.");
            return;
        }

        try {
            _ifs = new LineNumberReader(new FileReader(fsrc));
        } catch (FileNotFoundException e) {
            System.err.println("Couldn't open input file.");
            return;
        }

        // initialize _lastLine
        _lastLine = _ifs.readLine();

        // get common header for nodes and visitor
        String commonHeader = _readBlock("cheader");

        // get header for visitor
        _visitorHeader = commonHeader + _readBlock("vheader");

        // get header for nodes
        _nodeHeader = commonHeader + _readBlock("nheader");

        // get header for classID's
        _classIDHeader = commonHeader + _readBlock("iheader");
    }

    /** Generate the visitor class and the node class files */
    public void generate() throws IOException {

        _ofs.write(_visitorHeader);
        _ofs.write("public class "+ _visitorClassName +
                " implements IVisitor {\n" +
                "    public " + _visitorClassName + "() {\n" +
                "        this(TM_CHILDREN_FIRST);\n" +
                "    }\n\n" +
                "    public " + _visitorClassName +
                "(int traversalMethod) {\n" +
                "        if (traversalMethod > TM_CUSTOM) {\n" +
                "           throw new RuntimeException(\"" +
                "Illegal traversal method\");\n" +
                "        }\n" +
                "        _traversalMethod = traversalMethod;\n" +
                "    }\n\n");

        _readPlacement();

        _readClassInfo();

        _ifs.close();

        Iterator itr = _typeList.iterator();
        Iterator parItr = _parentTypeList.iterator();
        Iterator concreteItr = _isConcreteList.iterator();
        Iterator singletonItr = _isSingletonList.iterator();
        Iterator inTreeItr = _isInTreeList.iterator();
        Iterator interfaceItr = _isInterfaceList.listIterator();
        Iterator methodListItr = _methodListList.iterator();
        Iterator implListItr = _implListList.iterator();

        while (itr.hasNext()) {
            String typeName = (String) itr.next();
            String parentTypeName = (String) parItr.next();
            boolean isConcrete = ((Boolean) concreteItr.next()).booleanValue();
            boolean isSingleton =
                ((Boolean) singletonItr.next()).booleanValue();
            boolean isInterface =
                ((Boolean) interfaceItr.next()).booleanValue();
            boolean isInTree =
                ((Boolean) inTreeItr.next()).booleanValue();

            LinkedList methodList = (LinkedList) methodListItr.next();
            LinkedList implList = (LinkedList) implListItr.next();

            if (isConcrete) {
                if (isInTree) {
                    _ofs.write(
                            "\n" +
                            "    public Object visit" + typeName + "(" +
                            typeName +
                            " node, LinkedList args) {\n" +
                            "        return _defaultVisit(node, args);\n" +
                            "    }\n");
                }

                _idStringList.add(typeName.toUpperCase() + "_ID");
            }

            _generateNodeFile(typeName, parentTypeName, isConcrete, isSingleton,
                    isInterface, isInTree, methodList, implList);

            _generateClassIDFile();
        }

        _ofs.write(
                "\n" +
                "    /** Specify the order in visiting the nodes. */\n" +
                "    public final int traversalMethod() { " +
                "return _traversalMethod; }\n" +
                "\n" +
                "    /** The default visit method. */\n" +
                "    protected Object _defaultVisit(" + _baseNodeName +
                " node, LinkedList args) {\n" +
                "        return null;\n" +
                "    }\n" +
                "\n" +
                "    protected final int _traversalMethod;\n" +
                "}\n");

        _ofs.close();
    }

    protected String _readBlock(String marker) throws IOException {
        if (_lastLine != null) {
            _lastLine = _lastLine.trim();
        }

        while ((_lastLine != null) &&
                (_lastLine.equals("") || _lastLine.startsWith("//"))) {
            _lastLine = _ifs.readLine();

            if (_lastLine != null) {
                _lastLine = _lastLine.trim();
            }
        }

        String beginTag = "<" + marker + ">";
        String endTag   = "</" + marker + ">";

        if ((_lastLine == null) || !_lastLine.equals(beginTag)) {
            return "";
        }

        StringBuffer sb = new StringBuffer();

        boolean endHeader = false;
        do {
            _lastLine = _ifs.readLine();

            endHeader = ((_lastLine == null) || _lastLine.equals(endTag));

            if (!endHeader) {
                sb.append(_lastLine + "\n");
            }
        } while (!endHeader);

        _lastLine = _ifs.readLine();

        sb.append('\n');

        return sb.toString();
    }

    protected void _generateNodeFile(String typeName, String parentTypeName,
            boolean isConcrete, boolean isSingleton, boolean isInterface,
            boolean isInTree, LinkedList methodList, LinkedList implList)
            throws IOException {
        File fdest = new File(_nodePath + typeName + ".java");

        if (!fdest.createNewFile()) {
            fdest.delete();
            fdest = new File(_nodePath + typeName + ".java");
            fdest.createNewFile();
        }

        FileWriter fw = new FileWriter(fdest);

        StringBuffer sb = new StringBuffer();

        sb.append(_nodeHeader);

        sb.append("public ");

        boolean concreteClass = isConcrete && !isInterface;

        if (!isConcrete && !isInterface) {
            sb.append("abstract ");
        } else if (isSingleton) {
            sb.append("final ");
        }

        sb.append(isInterface ? "interface " : "class ");
        sb.append(typeName);

        if (!parentTypeName.equals("<none>")) {
            sb.append(" extends ");
            sb.append(parentTypeName);
        }

        Iterator implItr = implList.listIterator();

        if (implItr.hasNext()) {
            sb.append(" implements ");

            do {
                String interfaceName = (String) implItr.next();

                sb.append(interfaceName);

                if (implItr.hasNext()) {
                    sb.append(", ");
                }
            } while (implItr.hasNext());
        }

        sb.append(" {\n");

        if (concreteClass) {
            String idString = typeName.toUpperCase() + "_ID";

            // add a method to return the class id
            methodList.add(new MethodSignature("public final", "int",
                    "classID", new LinkedList(), new LinkedList(),
                    "return NodeClassID." + idString + ";"));

            if (isInTree) {
                // add a method that accepts a visitor
                LinkedList acceptMethodArgTypes = new LinkedList();
                acceptMethodArgTypes.addLast("IVisitor");
                acceptMethodArgTypes.addLast("LinkedList");

                LinkedList acceptMethodArgNames = new LinkedList();
                acceptMethodArgNames.addLast("visitor");
                acceptMethodArgNames.addLast("args");

                methodList.add(new MethodSignature("protected final",
                        "Object", "_acceptHere",
                        acceptMethodArgTypes, acceptMethodArgNames,
                        "return ((" + _visitorClassName +
                        ") visitor).visit" + typeName + "(this, args);"));

            }
        }

        if (isSingleton) {

            // add the instance of the singleton
            ClassField cf  = new ClassField(typeName, "instance",
                    "public static final", "new " + typeName + "()");
            methodList.addLast(cf);

            // add the constructor of the singleton
            MethodSignature ms = new MethodSignature(typeName);
            methodList.addLast(ms);

            // add a isSingleton() method
            methodList.add(new MethodSignature("public final", "boolean",
                    "isSingleton", new LinkedList(), new LinkedList(),
                    "return true;"));
        }

        // do methods first
        Iterator methodItr = methodList.listIterator();

        while (methodItr.hasNext()) {
            Object o = methodItr.next();

            if (o instanceof MethodSignature) {
                sb.append(o.toString() + "\n");
            }
        }

        // now do fields
        methodItr = methodList.listIterator();

        while (methodItr.hasNext()) {
            Object o = methodItr.next();

            if (o instanceof ClassField) {
                sb.append(o.toString() + "\n");
            }
        }

        sb.append("}\n");

        fw.write(sb.toString());
        fw.close();
    }

    protected void _generateClassIDFile() throws IOException {
        File fdest = new File(_nodePath + "NodeClassID.java");

        if (!fdest.createNewFile()) {
            fdest.delete();
            fdest = new File(_nodePath + "NodeClassID.java");
            fdest.createNewFile();
        }

        FileWriter fw = new FileWriter(fdest);

        StringBuffer sb = new StringBuffer();

        sb.append(_classIDHeader);

        sb.append("public interface NodeClassID {\n");

        int count = 0;

        Iterator stringItr = _idStringList.iterator();

        while (stringItr.hasNext()) {
            String idString = (String) stringItr.next();
            ClassField field = new ClassField("int", idString, "public",
                    Integer.toString(count));
            sb.append(field.toString() + '\n');
            count++;
        }
        sb.append("}\n");

        fw.write(sb.toString());
        fw.close();
    }

    protected void _readPlacement() throws IOException {
        _lastLine = _ifs.readLine();

        if (_lastLine.startsWith("mplace")) {
            _defaultPlacement = _lastLine.charAt(7);
            _lastLine = _ifs.readLine();
        }
    }

    protected void _readClassInfo() throws IOException {
        StringTokenizer strTokenizer;
        String className;
        String marker;

        do {
            if ((_lastLine != null) && (_lastLine.length() > 4) &&
                    !(_lastLine.startsWith("//"))) {

                strTokenizer = new StringTokenizer(_lastLine);

                className = strTokenizer.nextToken();

                ApplicationUtility.trace("Reading class info for : "
                        + className);

                try {
                    _typeList.addLast(className);
                } catch (NullPointerException e) {
                    System.err.println("Not enough parameters in line : " +
                            _lastLine);
                    return;
                }

                String nextToken = strTokenizer.nextToken();

                boolean isSingleton;
                boolean isInterface;
                boolean isConcrete;
                try {
                    isSingleton = nextToken.startsWith("S");
                    _isSingletonList.addLast(new Boolean(isSingleton));

                    isConcrete = isSingleton || nextToken.startsWith("C");
                    _isConcreteList.addLast(new Boolean(isConcrete));

                    isInterface = nextToken.startsWith("I");
                    _isInterfaceList.addLast(new Boolean(isInterface));

                    _isInTreeList.addLast(
                            new Boolean(!nextToken.endsWith("N")));

                } catch (NullPointerException e) {
                    System.err.println("Not enough parameters in line : " +
                            _lastLine);
                    return;
                }

                try {
                    nextToken = strTokenizer.nextToken();
                    _parentTypeList.addLast(nextToken);
                } catch (NullPointerException e) {
                    System.err.println("Not enough parameters in line : " +
                            _lastLine);
                    return;
                }

                LinkedList methodList = new LinkedList();
                LinkedList implList   = new LinkedList();

                while (strTokenizer.hasMoreTokens()) {
                    marker = strTokenizer.nextToken();
                    char markChar = marker.charAt(0);
                    switch (markChar) {

                    case 'c':
                    case 'm':
                        {
                            MethodSignature ms =
                                new MethodSignature(markChar, strTokenizer,
                                        className, _defaultPlacement,
                                        isInterface, isConcrete);
                            methodList.addLast(ms);
                        }
                        break;

                    case 'k':
                        {
                            MethodSignature ms =
                                new MethodSignature(markChar, strTokenizer,
                                        className, _defaultPlacement,
                                        isInterface, isConcrete);

                            methodList.addLast(ms);
                            methodList.addAll(ms.accessors());
                        }
                        break;

                    case 'i': // add a class name to the implements list
                        {
                            boolean isName;
                            do {
                                nextToken = strTokenizer.nextToken();

                                isName = ((nextToken != null) &&
                                        !nextToken.equals("i"));
                                if (isName) {
                                    implList.addLast(nextToken);
                                }
                            } while (isName);
                        }
                        break;

                    default:
                        throw new RuntimeException("Unrecognized marker : " +
                                marker);
                    }
                }

                _methodListList.addLast(methodList);
                _implListList.addLast(implList);
            }
            _lastLine = _ifs.readLine();
        } while (_lastLine != null);
    }

    private static final String _IDENT = "    ";


    ///////////////////////////////////////////////////////////////////
    ////                         inner class                       ////

    public static class MethodSignature {
        public MethodSignature() {}

        public MethodSignature(String modifiers, String returnType,
                String name,
                LinkedList paramTypes, LinkedList paramNames,
                String methodBody) {
            _modifiers = modifiers;
            _returnType = returnType;
            _name       = name;
            _paramTypes = paramTypes;
            _paramNames = paramNames;
            _methodBody = methodBody;
        }

        /** a singleton constructor */
        public MethodSignature(String className) {
            _modifiers = "private";
            _name = className;
            _construct = true;
            _isConcrete = true;
        }

        /** a constructor or method */
        public MethodSignature(char sigType, StringTokenizer strToken,
                String className, char defaultPlacement, boolean isInterface,
                boolean isConcrete)
                throws IOException {

            _isInterface = isInterface;
            _isConcrete = isConcrete;

            _defConstruct = (sigType == 'k');
            _construct = (sigType == 'c') || _defConstruct;

            _modifiers = "public";

            if (_construct) { // constructor
                _name = className;
                _returnType = "";

                _superParams = Integer.parseInt(strToken.nextToken());

            } else if (sigType == 'm') { // method
                _superParams = 0;

                if (!isInterface) _modifiers += " final";

                _returnType = strToken.nextToken() + " ";

                _name = strToken.nextToken();
            } else {
                throw new RuntimeException("Invalid token for " +
                        "MethodSignature : " + sigType);
            }

            String s = strToken.nextToken();

            while (!(s.equals("c") || s.equals("m") || s.equals("k"))) {
                if (s.charAt(0) == '{') {
                    // explicit placement, don't check
                    _varPlacements.addLast(new Character(s.charAt(1)));
                    s = s.substring(3);
                }  else {

                    if ((defaultPlacement == 'l') && isJavaType(s)) {
                        // make it a member if it's a Java type and we
                        // default to put it in a list
                        _varPlacements.addLast(new Character('m'));
                    } else {
                        if (defaultPlacement == 'l') {
                            _childListSize++;
                        }

                        _varPlacements.addLast(
                                new Character(defaultPlacement));
                    }
                }

                if (s.equals("[")) { // super constructor argument
                    StringBuffer sb = new StringBuffer();
                    boolean isInit;

                    s = strToken.nextToken();

                    sb.append(s);

                    do {
                        s = strToken.nextToken();

                        isInit = !s.equals("]");

                        if (isInit) {
                            sb.append(' ');
                            sb.append(s);
                        }
                    } while (isInit);

                    _superArgs.addLast(sb.toString());

                    _paramTypes.addLast("omitted");
                    _paramNames.addLast("omitted");

                } else {

                    _paramTypes.addLast(s);

                    String paramName = strToken.nextToken();

                    _paramNames.addLast(paramName);
                    _superArgs.addLast(paramName);
                }

                s = strToken.nextToken();
            }
        }

        /** A getter or a setter method for a child in the list.
         *  The childIndex parameter is necessary to differentiate
         *  this constructor from the following constructor.
         */
        public MethodSignature(String returnType, String name,
                int childIndex, boolean setter) {
            _modifiers = "public final";

            Character firstLetter =
                new Character(Character.toUpperCase(name.charAt(0)));

            String partName = firstLetter.toString() + name.substring(1);

            if (setter) {
                _returnType = "void";
                _name = "set" + partName;

                _paramTypes.addLast(returnType);
                _paramNames.addLast(name);

                _methodBody = "_childList.set(CHILD_INDEX_" +
                    name.toUpperCase() + ", " + name + ");";

            } else {
                _returnType = returnType;
                _name = "get" + partName;


                _methodBody = "return (" + _returnType +
                    ") _childList.get(CHILD_INDEX_" +
                    name.toUpperCase() + ");";
            }
        }

        /** A getter or a setter method for data not in the list. */
        public MethodSignature(String returnType, String name,
                boolean setter) {
            _modifiers = "public final";

            Character firstLetter =
                new Character(Character.toUpperCase(name.charAt(0)));

            String partName = firstLetter.toString() + name.substring(1);

            if (setter) {
                _returnType = "void";
                _name = "set" + partName;

                _paramTypes.addLast(returnType);
                _paramNames.addLast(name);

                _methodBody = "_" + name + " = " + name + ";";
            } else {
                _returnType = returnType;
                _name = "get" + partName;

                _methodBody = "return _" + name + ";";
            }
        }

        /** A hasX() method that returns true. */
        public MethodSignature(String name, int dummy) {
            _modifiers = "public final";

            Character firstLetter =
                new Character(Character.toUpperCase(name.charAt(0)));

            String partName = firstLetter.toString() + name.substring(1);

            _name = "has" + partName;
            _returnType = "boolean";
            _methodBody = "return true;";
        }

        public String methodBody() {

            if (_methodBody != null) {
                return _IDENT + _IDENT + _methodBody;
            }

            if (_construct) {

                StringBuffer sb = new StringBuffer();

                if (_superParams > 0) {

                    Iterator argsItr = _superArgs.listIterator();

                    sb.append(_IDENT + _IDENT);
                    sb.append("super(");

                    for (int i = 0; i < _superParams; i++) {
                        sb.append((String) argsItr.next());

                        if (i < (_superParams - 1)) {
                            sb.append(", ");
                        }
                    }

                    sb.append(");\n");
                }

                if (_defConstruct) {

                    Iterator typeItr = _paramTypes.listIterator();
                    Iterator nameItr = _paramNames.listIterator();
                    Iterator varPlaceItr = _varPlacements.listIterator();

                    int varCount = 0;

                    do {
                        String typeStr = (String) typeItr.next();
                        String nameStr = (String) nameItr.next();
                        char placement =
                            ((Character) varPlaceItr.next()).charValue();

                        if (varCount >= _superParams) {

                            sb.append(_IDENT);
                            sb.append(_IDENT);

                            switch (placement) {

                            case 'l':
                                sb.append("_childList.add(" + nameStr + ");");
                                break;

                            case 'm':
                            case 'h':
                                sb.append("_" + nameStr + " = " +
                                        nameStr + ";");
                                break;

                            case 'p':
                                sb.append("setProperty(" + nameStr + ", " +
                                        _wrapPrimitive(typeStr, nameStr) +
                                        ");");
                                break;

                            case 'n':
                                // do nothing
                                break;

                            default:
                                throw new RuntimeException(
                                        "unknown variable placment");
                            }

                            if (typeItr.hasNext()) {
                                sb.append('\n');
                            }
                        }

                        varCount++;
                    } while (typeItr.hasNext());
                }

                if (_isConcrete) {
                    sb.append("\n" + _IDENT + _IDENT +
                            "_childList.trimToSize();");
                }

                return sb.toString();
            } // if _construct

            // this is method

            if (_returnType.equals("void") || _returnType.equals("")) {
                return "";
            }

            if (_returnType.equals("long")) {
                return _IDENT + _IDENT + "return 0L;";
            }

            if (_returnType.equals("int") || _returnType.equals("short") ||
                    _returnType.equals("byte")) {
                return _IDENT + _IDENT + "return 0;";
            }

            if (_returnType.equals("char")) {
                return _IDENT + _IDENT + "return '\\0';";
            }

            if (_returnType.equals("float")) {
                return _IDENT + _IDENT + "return 0.0f;";
            }

            if (_returnType.equals("double")) {
                return _IDENT + _IDENT + "return 0.0d";
            }

            if (_returnType.equals("boolean")) {
                return _IDENT + _IDENT + "return false";
            }

            return _IDENT + _IDENT + "return null;";
        }

        public String toString() {
            StringBuffer sb = new StringBuffer(_IDENT);

            if (!_modifiers.equals("")) {
                sb.append(_modifiers + " ");
            }

            if (!_returnType.equals("")) {
                sb.append(_returnType + " ");
            }

            sb.append(_name + "(");

            Iterator typeItr = _paramTypes.listIterator();
            Iterator nameItr = _paramNames.listIterator();

            int paramCount = 0;
            while (typeItr.hasNext()) {

                String typeName = (String) typeItr.next();
                String paramName = (String) nameItr.next();

                if (!typeName.equals("omitted")) {
                    if (paramCount > 0) {
                        sb.append(", ");
                    }
                    paramCount++;

                    sb.append(typeName);
                    sb.append(' ');
                    sb.append(paramName);
                }
            }

            sb.append(')');

            if (_isInterface) {
                sb.append(";");
            } else {
                sb.append(" {\n" + methodBody() + "\n" + _IDENT + "}\n");
            }

            return sb.toString();
        }

        public LinkedList accessors() {
            LinkedList retval = new LinkedList();

            int varCount = 0;
            int childIndex = 0;

            Iterator typeItr = _paramTypes.listIterator();
            Iterator nameItr = _paramNames.listIterator();
            Iterator varPlaceItr = _varPlacements.listIterator();

            while (typeItr.hasNext()) {

                String typeStr = (String) typeItr.next();
                String nameStr = (String) nameItr.next();
                char placement = ((Character) varPlaceItr.next()).charValue();

                if (varCount >= _superParams) {

                    switch (placement) {

                    case 'l':
                        // getter
                        retval.addLast(new MethodSignature(typeStr, nameStr,
                                childIndex, false));

                        //setter
                        retval.addLast(new MethodSignature(typeStr, nameStr,
                                childIndex, true));

                        // getter index
                        retval.addLast(new ClassField("int",
                                "CHILD_INDEX_" + nameStr.toUpperCase(),
                                "public static final",
                                Integer.toString(childIndex)));
                        break;

                    case 'h': // member with hasX()
                        retval.addLast(new MethodSignature(nameStr, -1));
                        // no break;

                    case 'm': // member
                        retval.addLast(new ClassField(typeStr, nameStr));

                        // getter
                        retval.addLast(new MethodSignature(typeStr, nameStr,
                                false));

                        // setter
                        retval.addLast(new MethodSignature(typeStr, nameStr,
                                true));
                        break;

                    case 'n':
                        // do nothing
                        break;

                    case 'p': // property
                        // do nothing : data is accessed through
                        // PropertyMap methods
                        break;

                    default:
                        throw new RuntimeException(
                                "unknown variable placement");

                    }
                }

                if (placement == 'l') {
                    childIndex++;
                }

                varCount++;
            }

            return retval;
        }

        protected String _wrapPrimitive(String typeStr, String nameStr) {
            String wrapper = null;

            if (typeStr.startsWith("int")) {
                wrapper = "Integer";
            } else if (typeStr.startsWith("char")) {
                wrapper = "Character";
            } else if (typeStr.startsWith("long")) {
                wrapper = "Long";
            } else if (typeStr.startsWith("byte")) {
                wrapper = "Byte";
            } else if (typeStr.startsWith("float")) {
                wrapper = "Float";
            } else if (typeStr.startsWith("double")) {
                wrapper = "Double";
            } else if (typeStr.startsWith("boolean")) {
                wrapper = "Boolean";
            }

            if (wrapper != null) {
                return "new " + wrapper + "(" + nameStr + ")";
            } else {
                return nameStr;
            }
        }

        protected String _modifiers = "public ";
        protected String _returnType = "";
        protected String _name;

        protected LinkedList _paramTypes = new LinkedList();
        protected LinkedList _paramNames = new LinkedList();
        protected LinkedList _varPlacements = new LinkedList();
        protected LinkedList _superArgs = new LinkedList();

        protected int _childListSize = 0; // not currently used

        protected int _superParams = 0;

        protected String _methodBody = null;

        protected boolean _construct = false;
        protected boolean _isInterface = false;
        protected boolean _defConstruct = false;

        /** True if this is a member of a concrete class. */
        protected boolean _isConcrete = false;
    }

    public static class ClassField {
        public ClassField(String type, String name, String modifiers,
                String init) {
            _type = type;
            _name = name;
            _modifiers = modifiers;
            _init = init;
        }

        public ClassField(String type, String name) {
            this(type, name, "protected", null);
        }

        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append(_IDENT);
            sb.append(_modifiers);
            sb.append(' ');
            sb.append(_type);
            sb.append(' ');

            if (!_modifiers.startsWith("public")) {
                sb.append(" _");
            }

            sb.append(_name);

            if (_init != null) {
                sb.append(" = " + _init);
            }

            sb.append(';');
            return sb.toString();
        }

        String _type;
        String _name;
        String _modifiers;
        String _init;
    }

    public static final boolean isPrimitiveType(String s) {
        return (s.startsWith("int") || s.startsWith("char") ||
                s.startsWith("long") || s.startsWith("byte") ||
                s.startsWith("float") || s.startsWith("double") ||
                s.startsWith("boolean"));
    }

    public static final boolean isJavaType(String s) {
        return (isPrimitiveType(s) || s.startsWith("String"));
    }

    protected char _defaultPlacement = 'l';

    protected FileWriter _ofs;
    protected LineNumberReader _ifs;

    protected LinkedList _typeList = new LinkedList();
    protected LinkedList _parentTypeList = new LinkedList();
    protected LinkedList _isConcreteList = new LinkedList();
    protected LinkedList _isSingletonList = new LinkedList();
    protected LinkedList _isInterfaceList = new LinkedList();
    protected LinkedList _isInTreeList = new LinkedList();
    protected LinkedList _methodListList = new LinkedList();
    protected LinkedList _implListList = new LinkedList();

    protected LinkedList _idStringList = new LinkedList();

    protected String _visitorClassName;
    protected String _baseNodeName;
    protected String _nodeHeader;
    protected String _visitorHeader;
    protected String _classIDHeader;
    protected String _nodePath;

    protected String _lastLine;
}
