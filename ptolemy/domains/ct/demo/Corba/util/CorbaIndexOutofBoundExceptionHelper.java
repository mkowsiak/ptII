/*
 * File: ../../../../../PTOLEMY/DOMAINS/CT/DEMO/CORBA/UTIL/CORBAINDEXOUTOFBOUNDEXCEPTIONHELPER.JAVA
 * From: CORBAACTOR.IDL
 * Date: Thu Jul 29 14:22:20 1999
 *   By: idltojava Java IDL 1.2 Aug 18 1998 16:25:34
 */

package ptolemy.domains.ct.demo.Corba.util;
public class CorbaIndexOutofBoundExceptionHelper {
    // It is useless to have instances of this class
    private CorbaIndexOutofBoundExceptionHelper() { }

    public static void write(org.omg.CORBA.portable.OutputStream out, ptolemy.domains.ct.demo.Corba.util.CorbaIndexOutofBoundException that) {
        out.write_string(id());

	out.write_short(that.index);
    }
    public static ptolemy.domains.ct.demo.Corba.util.CorbaIndexOutofBoundException read(org.omg.CORBA.portable.InputStream in) {
        ptolemy.domains.ct.demo.Corba.util.CorbaIndexOutofBoundException that = new ptolemy.domains.ct.demo.Corba.util.CorbaIndexOutofBoundException();
        // read and discard the repository id
        in.read_string();

	that.index = in.read_short();
        return that;
    }
    public static ptolemy.domains.ct.demo.Corba.util.CorbaIndexOutofBoundException extract(org.omg.CORBA.Any a) {
        org.omg.CORBA.portable.InputStream in = a.create_input_stream();
        return read(in);
    }
    public static void insert(org.omg.CORBA.Any a, ptolemy.domains.ct.demo.Corba.util.CorbaIndexOutofBoundException that) {
        org.omg.CORBA.portable.OutputStream out = a.create_output_stream();
        write(out, that);
        a.read_value(out.create_input_stream(), type());
    }
    private static org.omg.CORBA.TypeCode _tc;
    synchronized public static org.omg.CORBA.TypeCode type() {
        int _memberCount = 1;
        org.omg.CORBA.StructMember[] _members = null;
        if (_tc == null) {
            _members= new org.omg.CORBA.StructMember[1];
            _members[0] = new org.omg.CORBA.StructMember(
                    "index",
                    org.omg.CORBA.ORB.init().get_primitive_tc(org.omg.CORBA.TCKind.tk_short),
                    null);
            _tc = org.omg.CORBA.ORB.init().create_exception_tc(id(), "CorbaIndexOutofBoundException", _members);
        }
        return _tc;
    }
    public static String id() {
        return "IDL:util/CorbaIndexOutofBoundException:1.0";
    }
}
