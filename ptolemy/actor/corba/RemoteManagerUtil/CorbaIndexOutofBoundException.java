package ptolemy.actor.corba.RemoteManagerUtil;


/**
* ptolemy/actor/corba/RemoteManagerUtil/CorbaIndexOutofBoundException.java .
* Generated by the IDL-to-Java compiler (portable), version "3.1"
* from RemoteManager.idl
* Thursday, January 16, 2003 3:50:31 PM PST
*/


/* Exception for channel indeces.
	 */
public final class CorbaIndexOutofBoundException extends org.omg.CORBA.UserException
{
  public short index = (short)0;

  public CorbaIndexOutofBoundException ()
  {
    super(CorbaIndexOutofBoundExceptionHelper.id());
  } // ctor

  public CorbaIndexOutofBoundException (short _index)
  {
    super(CorbaIndexOutofBoundExceptionHelper.id());
    index = _index;
  } // ctor


  public CorbaIndexOutofBoundException (String $reason, short _index)
  {
    super(CorbaIndexOutofBoundExceptionHelper.id() + "  " + $reason);
    index = _index;
  } // ctor

} // class CorbaIndexOutofBoundException
