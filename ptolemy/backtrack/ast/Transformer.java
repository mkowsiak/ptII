/* Transform Java source programs to support backtracking.

Copyright (c) 2005 The Regents of the University of California.
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

package ptolemy.backtrack.ast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.CompilationUnit;

import ptolemy.backtrack.ast.transform.AssignmentRule;
import ptolemy.backtrack.ast.transform.PackageRule;
import ptolemy.backtrack.ast.transform.TransformRule;
import ptolemy.backtrack.util.PathFinder;
import ptolemy.backtrack.util.SourceOutputStream;
import ptolemy.backtrack.xmlparser.ConfigParser;
import ptolemy.backtrack.xmlparser.XmlOutput;

//////////////////////////////////////////////////////////////////////////
//// Transform
/**
   A tool to transform Java source programs to support backtracking. This
   transformation is done by first analyzing the Java programs, and then
   refactoring the program based on the information collected in the
   analysis phase.
  
   @author Thomas Feng
   @version $Id$
   @since Ptolemy II 5.1
   @Pt.ProposedRating Red (tfeng)
   @Pt.AcceptedRating Red (tfeng)
*/
public class Transformer {
    
    /** Transform a set of files into backtracking-enabled ones. The set of
     *  files is given by an array of strings as their names, or the names of
     *  the directories that contain them. If a string in the array is a file
     *  name, it is refactored and the output is printed out; if it is the name
     *  of a directory, all Java files in the directory and its sub-directories
     *  are looked up and refactored in the same way.
     * 
     *  @param args The array of file names or names of directories.
     *  @exception Exception If any exception occues.
     */
    public static void main(String[] args) throws Exception {
        boolean outputResult = true;
        
        if (args.length == 0)
            System.err.println("USAGE: java ptolemy.backtrack.ast.Transform " +
                    "[.java files... | directories...]");
        else {
            String[] paths = PathFinder.getPtClassPaths();
            Writer standardWriter = 
                outputResult ? new OutputStreamWriter(System.out) : null;
            for (int i = 0; i < args.length;) {
                int newPosition = _parseArguments(args, i);
                if (newPosition != i) {
                    i = newPosition;
                    continue;
                }
                
                String pathOrFile = args[i];
                File[] files;
                if (pathOrFile.startsWith("@")) {
                    // A file list.
                    // Each line in the file contains a single file name.
                    String listName = pathOrFile.substring(1);
                    BufferedReader reader = 
                        new BufferedReader(
                                new InputStreamReader(
                                        new FileInputStream(listName)));
                    List strings = new LinkedList();
                    String line = reader.readLine();
                    while (line != null) {
                        strings.add(line);
                        line = reader.readLine();
                    }
                    files = new File[strings.size()];
                    Iterator stringsIter = strings.iterator();
                    for (int j = 0; stringsIter.hasNext(); j++)
                        files[j] = new File((String)stringsIter.next());
                } else
                    files = PathFinder.getJavaFiles(pathOrFile, true);
                for (int j = 0; j < files.length; j++) {
                    String fileName = files[j].getPath();
                    System.err.print("Transforming \"" + fileName + "\"...");
                    
                    if (fileName.endsWith(".java")) {
                        String classFileName = 
                            fileName.substring(0, fileName.length() - 5) +
                            ".class";
                        if (new File(classFileName).exists())
                            System.err.println();
                        else {
                            System.err.println(" SKIP");
                            continue;
                        }
                    } else
                        continue;
                    
                    System.err.flush();
                    
                    transform(files[j].getPath(), standardWriter, paths);
                    
                    if (outputResult)
                        standardWriter.flush();
                }
                i++;
            }
            _outputConfig();
            if (outputResult)
                standardWriter.close();
        }
    }
    
    /** Transform the Java source with no explicit class path, and
     *  output the result to the writer. This is the same as calling
     *  <tt>transform(source, writer, null)</tt>.
     * 
     *  @param source The Java source.
     *  @param writer The writer where output is written.
     *  @exception IOException If IO exception occurs when writing to
     *   the output.
     *  @exception ASTMalformedException If the Java source is illegal.
     *  @see #transform(char[], Writer, String[])
     */
    public static void transform(char[] source, Writer writer)
            throws IOException, ASTMalformedException {
        transform(source, writer, null);
    }
    
    /** Transform the Java source with given classpaths, and output the
     *  result to the writer.
     * 
     *  @param source The Java source.
     *  @param writer The writer where output is written.
     *  @param classPaths The class paths.
     *  @exception IOException If IO exception occurs when writing to the
     *   output.
     *  @exception ASTMalformedException If the Java source is illegal.
     *  @see #transform(char[], Writer)
     */
    public static void transform(char[] source, Writer writer, 
            String[] classPaths)
            throws IOException, ASTMalformedException {
        Transformer transform = new Transformer(source, classPaths);
        transform._startTransform();
    }

    /** Transform the Java source in the file given by its name with no
     *  explicit class path, and output the result to the writer. This
     *  is the same as calling <tt>transform(fileName, writer, null)</tt>.
     * 
     *  @param fileName The Java file name.
     *  @param writer The writer where output is written.
     *  @exception IOException If IO exception occurs when reading from
     *   the Java file or riting to the output.
     *  @exception ASTMalformedException If the Java source is illegal.
     *  @see #transform(String, Writer, String[])
     */
    public static void transform(String fileName, Writer writer)
            throws IOException, ASTMalformedException {
        transform(fileName, writer, null);
    }
    
    /** Transform the Java source in the file given by its name with
     *  given class paths, and output the result to the writer.
     *  <p>
     *  If a output directory is set with the <tt>-output</tt>
     *  command-line argument, the output is written to a Java source
     *  file with that directory as the root directory. The given
     *  writer is not used in that case.
     * 
     *  @param fileName The Java file name.
     *  @param writer The writer where output is written.
     *  @param classPaths The class paths.
     *  @exception IOException If IO exception occurs when reading from
     *   the Java file or riting to the output.
     *  @exception ASTMalformedException If the Java source is illegal.
     *  @see #transform(String, Writer)
     */
    public static void transform(String fileName, Writer writer, 
            String[] classPaths)
            throws IOException, ASTMalformedException {
        boolean needClose = false;
        
        Transformer transform = new Transformer(fileName, classPaths);
        transform._startTransform();
        
        if (_rootPath != null) {
            String packageName = 
                transform._ast.getPackage().getName().toString();
            File file = new File(fileName);
            String outputFileName = file.getName();
            SourceOutputStream outputStream = 
                SourceOutputStream.getStream(_rootPath, packageName, 
                        outputFileName, _overwrite);
            writer = new OutputStreamWriter(outputStream);
            needClose = true;
        }
        
        transform._outputSource(writer);
        
        if (needClose)
            writer.close();
        
        // Record the class name.
        if (_configName != null) {
            File file = new File(fileName);
            String simpleName = file.getName();
            if (simpleName.toUpperCase().endsWith(".JAVA")) {
                String baseName = 
                    simpleName.substring(0, simpleName.length() - 5);
                CompilationUnit root = 
                    (CompilationUnit)transform._ast.getRoot();
                String className;
                if (root.getPackage() != null)
                    className =
                        root.getPackage().getName().toString() + "." +
                        baseName;
                else
                    className = baseName;
                
                if (_prefix != null && _prefix.length() > 0)
                    className = className.substring(_prefix.length() + 1);
                
                _classes.add(className);
            }
        }
    }

    /** The refactoring rules to be sequentially applied to the source code.
     */
    public static TransformRule[] RULES = new TransformRule[] {
        new AssignmentRule(),
        new PackageRule()
    };
    
    /** Call the <tt>afterTraverse</tt> of all the refactoring rules. For
     *  each rule, the <tt>afterTraverse</tt> is called after the AST is
     *  traversed. The rule may clean up states or finalize the
     *  transformation.
     */
    protected void _afterTraverse() {
        for (int i = 0; i < RULES.length; i++)
            RULES[i].afterTraverse(_visitor, _ast);
    }
    
    /** Call the <tt>beforeTraverse</tt> of all the refactoring rules. For
     *  each rule, the <tt>beforeTraverse</tt> is called before the AST is
     *  traversed. The rule may set up handlers for special events in the
     *  traversal.
     */
    protected void _beforeTraverse() {
        for (int i = 0; i < RULES.length; i++)
            RULES[i].beforeTraverse(_visitor, _ast);
    }
    
    protected static void _outputConfig() throws Exception {
        if (_configName != null) {
            SourceOutputStream stream = 
                SourceOutputStream.getStream(_configName, _overwrite);
            Set classSet = new HashSet();
            classSet.addAll(_classes);

            ConfigParser parser = new ConfigParser();
            parser.parseConfigFile(ConfigParser.DEFAULT_SYSTEM_ID, classSet);
            if (_prefix != null && _prefix.length() > 0)
                parser.addPackagePrefix(_prefix, classSet);
            
            OutputStreamWriter writer = new OutputStreamWriter(stream);
            XmlOutput.outputXmlTree(parser.getTree(), writer);
            writer.close();
        }
    }
    
    /** Output the Java source from the current AST with {@link ASTFormatter}.
     * 
     *  @param writer The writer where the output is written to.
     */
    protected void _outputSource(Writer writer) {
        ASTFormatter formatter = new ASTFormatter(writer);
        _ast.accept(formatter);
    }
    
    /** Parse the Java source and retrieve the AST. If a source file name is
     *  given, the file is read and parsed; if the source is given in the form
     *  of <tt>char[]</tt>, it is directly passed to the parser.
     *  
     *  @exception IOException If a file name is given, but exception
     *   occurs when reading from the file.
     *  @exception ASTMalformedException If the source is illegal.
     */
    protected void _parse()
            throws IOException, ASTMalformedException {
        if (_fileName != null)
            _ast = ASTBuilder.parse(_fileName);
        else
            _ast = ASTBuilder.parse(_source);
    }
    
    /** Parse the command-line arguments starting from the given position.
     *  If one or more argument corresponds to an option, proper actions are
     *  performed to record that option. The position is adjusted to the next
     *  file name or option and returned.
     * 
     *  @param args The command-line arguments.
     *  @param position The starting position.
     *  @return The new position.
     */
    protected static int _parseArguments(String[] args, int position) {
        String arg = args[position];
        if (arg.equals("-prefix") || arg.equals("-p")) {
            position++;
            _prefix = args[position];
            for (int i = 0; i < RULES.length; i++)
                if (RULES[i] instanceof PackageRule) {
                    ((PackageRule)RULES[i]).setPrefix(_prefix);
                    break;
                }
            position++;
        } else if (arg.equals("-output") || arg.equals("-o")) {
            position++;
            _rootPath = args[position];
            if (_rootPath.length() == 0)
                _rootPath = ".";
            position++;
        } else if (arg.equals("-overwrite") || arg.equals("-w")) {
            position++;
            _overwrite = true;
        } else if (arg.equals("-nooverwrite") || arg.equals("-nw")) {
            position++;
            _overwrite = false;
        } else if (arg.equals("-config") || arg.equals("-c")) {
            position++;
            _configName = args[position];
            position++;
        }
        return position;
    }
    
    /** Start the transformation by first parsing the source with
     *  {@link #_parse()}, then call {@link #_beforeTraverse()},
     *  then traverse the AST with {@link TypeAnalyzer} visitor,
     *  and finally call {@link #_afterTraverse()}.
     *   
     *  @exception IOException If a file name is given, but exception
     *   occurs when reading from the file.
     *  @exception ASTMalformedException If the source is illegal.
     *  @see #_afterTraverse()
     *  @see #_beforeTraverse()
     *  @see #_parse()
     */
    protected void _startTransform()
            throws IOException, ASTMalformedException {
        _parse();
        _beforeTraverse();
        _ast.accept(_visitor);
        _afterTraverse();
    }

    /** Construct a transformer. This constructor should not be called from
     *  outside of this class.
     * 
     *  @param source The Java source.
     *  @param writer The writer where the output is written.
     *  @param classPaths An array of explicit class paths, or <tt>null</tt>
     *   if none.
     */
    private Transformer(char[] source, String[] classPaths) {
        _source = source;
        _visitor = new TypeAnalyzer(classPaths);
    }
    
    /** Construct a transformer. This constructor should not be called from
     *  outside of this class.
     * 
     *  @param fileName The Java source file name.
     *  @param writer The writer where the output is written.
     *  @param classPaths An array of explicit class paths, or <tt>null</tt>
     *   if none.
     */
    private Transformer(String fileName, String[] classPaths) {
        _fileName = fileName;
        _visitor = new TypeAnalyzer(classPaths);
    }
    
    /** The Java source file name, if not <tt>null</tt>.
     */
    private String _fileName;
    
    /** The Java source, if not <tt>null</tt>
     */
    private char[] _source;
    
    /** The AST. Not <tt>null</tt> after {@link #_parse()} is successfully
     *  called.
     */
    private CompilationUnit _ast;
    
    /** The visitor used to traverse the AST.
     */
    private TypeAnalyzer _visitor;
    
    /** The prefix to be added to the package name of the source.
     */
    private static String _prefix;
    
    /** The root directory of the Java source output.
     */
    private static String _rootPath;
    
    /** Whether to overwrite existing file(s).
     */
    private static boolean _overwrite = false;
    
    /** The name of the output XML configuration.
     */
    private static String _configName;
    
    /** Class names of all the source parsed.
     */
    private static List _classes = new LinkedList();
}
