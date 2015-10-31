/* Support for the websocket accessor.

@Copyright (c) 2015 The Regents of the University of California.
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
package ptolemy.actor.lib.jjs.modules.socket;

import io.vertx.core.http.ClientAuth;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.NetSocket;

import java.util.Map;

import javax.imageio.ImageIO;

import jdk.nashorn.api.scripting.ScriptObjectMirror;
import ptolemy.actor.lib.jjs.VertxHelperBase;

///////////////////////////////////////////////////////////////////
//// SocketHelper

/**
   A helper class for the socket module in JavaScript.
   You should use {@link #getOrCreateHelper(Object)} to create
   exactly one instance of this helper per actor. Pass the actor
   as an argument.
   
   A confusing aspect of this design is the socket client will
   have exactly one socket associated with it, whereas a socket
   server can have any number of sockets associated with it.
   In any case, there should be only one instance of this class
   associated with any actor. This ensures that all the socket
   actions and callbacks managed by this instance execute in
   a single verticle.

   @author Edward A. Lee
   @version $Id$
   @see SocketServerHelper
   @since Ptolemy II 11.0
   @Pt.ProposedRating Yellow (eal)
   @Pt.AcceptedRating Red (eal)
 */
public class SocketHelper extends VertxHelperBase {

    /** Constructor for SocketHelper for the specified actor.
     *  @param actor The actor that this will help.
     */
    public SocketHelper(Object actor) {
    	super(actor);
    }
    
    ///////////////////////////////////////////////////////////////////
    ////                         public methods                    ////
    
    /** Create a client-side socket on behalf of the specified
     *  JavaScript SocketClient object. After this is called,
     *  the specified socketClient will emit the following events:
     *  * open: Emitted when the connection has been established
     *    with the server. This will not be passed any arguments.
     *  * data: Emitted when data is received on the socket.
     *    The received data will be an argument to the event.
     *  * close: Emitted when a socket is closed.
     *    This will not be passed any arguments.
     *  * error: Emitted when an error occurs. This will be passed
     *    an error message.
     * 
     *  @param socketClient The JavaScript SocketClient instance.
     *  @param port The remote port to connect to.
     *  @param host The remote host to connect to.
     *  @param options The options (see the socket.js JavaScript module).
     */
    public void openClientSocket(
    		final ScriptObjectMirror socketClient,
    		final int port,
    		final String host,
    	    Map<String,Object> options) {
    	
    	// NOTE: The following assumes all the options are defined.
    	// This is handled in the associated JavaScript socket.js module.
    	final NetClientOptions clientOptions = new NetClientOptions()
    			.setConnectTimeout((Integer)options.get("connectTimeout"))
    			.setIdleTimeout((Integer)options.get("idleTimeout"))
    			.setReceiveBufferSize((Integer)options.get("receiveBufferSize"))
    			.setReconnectAttempts((Integer)options.get("reconnectAttempts"))
    			.setReconnectInterval((Integer)options.get("reconnectInterval"))
    			.setSendBufferSize((Integer)options.get("sendBufferSize"))
    			.setSsl((Boolean)options.get("sslTls"))
    			.setTcpKeepAlive((Boolean)options.get("keepAlive"));

    	/* FIXME: Not used options:
        'receiveType': 'application/json',
        'sendType': 'application/json',
        'discardMessagesBeforeOpen': false,
		*/

    	// Create the socket in the associated verticle.
    	submit(() -> {
	    	NetClient client = _vertx.createNetClient(clientOptions);
	    	// NOTE: In principle, this client can handle multiple connections.
	    	// But here we use exactly one client per connection. Is this OK?
	    	client.connect(port, host, response -> {
	    		if (response.succeeded()) {
	    			// Socket has been opened.
	        	    NetSocket socket = response.result();
	        	    
	    			_issueResponse(() -> {
	    				// This should be called in the director thread because it
	    				// emits an event that may be handled by the user.
	    				socketClient.callMember("_opened", socket, client);
	    			});
	    		} else {
	        	    _error(socketClient, "Failed to connect: " + response.cause().getMessage());
	    		}
	    	});
    	});
    }
    
    /** Get or create a helper for the specified actor.
     *  If one has been created before and has not been garbage collected, return
     *  that one. Otherwise, create a new one.
	 *  @param actor Either a JavaScript actor or a RestrictedJavaScriptInterface.
     */
    public static SocketHelper getOrCreateHelper(Object actor) {
    	VertxHelperBase helper = VertxHelperBase.getHelper(actor);
    	if (helper instanceof SocketHelper) {
    		return (SocketHelper) helper;
    	}
    	return new SocketHelper(actor);
    }

    /** Create a server that can accept socket connection requests
     *  on behalf of the specified JavaScript SocketServer object.
     *  After this is called, the specified JavaScript
     *  SocketServer object will emit the following events:
     *  <ul>
     *  <li> listening: Emitted when the server is listening.
     *    This will be passed the port number that the server is
     *    listening on (this is useful if the port is specified to be 0).
     *  <li> connection: Emitted when a new connection is established
     *    after a request from (possibly remote) client.
     *    This will be passed an instance of the JavaScript Socket
     *    class that is defined in the socket.js module.
     *    That instance has a send() and close()
     *    function that can be used to send data or to close the socket.
     *    It is also an event emitter that emits 'close', 'data',
     *    and 'error' events.
     *  <li> error: If this server fails to start listening.
     *    An error message will be passed to any event handler.
     *  </ul>
     *  @param socketServer The JavaScript SocketServer instance.
     *  @param options The options (see the socket.js JavaScript module).
     */
    public void openServer(
    		final ScriptObjectMirror socketServer,
    		final Map<String,Object> options) {
    	
    	// Translate clientAuth option to an enum.
    	ClientAuth auth = ClientAuth.NONE;
    	String authSpec = (String)options.get("clientAuth");
    	if (authSpec.toLowerCase().trim().equals("request")) {
    		auth = ClientAuth.REQUEST;
    	} else if (authSpec.toLowerCase().trim().equals("required")) {
    		auth = ClientAuth.REQUIRED;
    	}
    	
    	// NOTE: The following assumes all the options are defined.
    	// This is handled in the associated JavaScript socket.js module.
    	final NetServerOptions serverOptions = new NetServerOptions()
    			.setClientAuth(auth)
    			.setHost((String)options.get("hostInterface"))
    			.setIdleTimeout((Integer)options.get("idleTimeout"))
    			.setTcpKeepAlive((Boolean)options.get("keepAlive"))
    			.setPort((Integer)options.get("port"))
    			.setReceiveBufferSize((Integer)options.get("receiveBufferSize"))
    			.setSendBufferSize((Integer)options.get("sendBufferSize"))
    			.setSsl((Boolean)options.get("sslTls"));
    	
    	/* FIXME: Not used:
        'receiveType': 'string',
        'sendType': 'string',
        */

    	// Create the server in the associated verticle.
    	submit(() -> {
	    	final NetServer server = _vertx.createNetServer(serverOptions);
	    	
	    	// Notify the JavaScript SocketServer object of the server.
	    	socketServer.callMember("_serverCreated", server);
	    	
	    	server.connectHandler(socket -> {
	    		// Connection is established with a client.
    			_issueResponse(() -> {
    				socketServer.callMember("_socketCreated", socket);
    			});
	    	});
	    	
	    	server.listen(result -> {
				_issueResponse(() -> {
					if (result.succeeded()) {
						socketServer.callMember("emit", "listening", server.actualPort());
					} else {
						_error("Failed to start server listening.");
					}
				});
	    	});
    	});
    }

    /** Return an array of the types supported by the current host for
     *  receiveType arguments.
     */
    public static String[] supportedReceiveTypes() {
        String[] imageTypes = ImageIO.getReaderFormatNames();
        String[] result = new String[imageTypes.length + 2];
        result[0] = "application/json";
        result[1] = "text/plain";
        System.arraycopy(imageTypes, 0, result, 2, imageTypes.length);
        return result;
    }

    /** Return an array of the types supported by the current host for
     *  sendType arguments.
     */
    public static String[] supportedSendTypes() {
        String[] imageTypes = ImageIO.getWriterFormatNames();
        String[] result = new String[imageTypes.length + 2];
        result[0] = "application/json";
        result[1] = "text/plain";
        System.arraycopy(imageTypes, 0, result, 2, imageTypes.length);
        return result;
    }

    ///////////////////////////////////////////////////////////////////
    ////                     private fields                        ////

    /** True to discard messages before the socket is open. False to discard them. */
    private boolean _discardMessagesBeforeOpen;
    
    /** The MIME type to assume for received messages. */
    private String _receiveType;
    
    /** The MIME type to assume for sent messages. */
    private String _sendType;

    ///////////////////////////////////////////////////////////////////
    ////                     public classes                        ////

    /** Wrapper for connected sockets.
     *  An instance of this class handles socket events from the
     *  Vert.x NetSocket object and translates them into JavaScript
     *  events emitted by the eventEmitter specified in the constructor.
     *  The events emitted are:
     *  <ul>
     *  <li> close: Emitted when the socket closes. It has no arguments.
     *  <li> error: Emitted when an error occurs. It is passed an error message.
     *  <li> data: Emitted when the socket received data. It is passed the data.
     *  </ul>
     */
    public class SocketWrapper {
    	
    	/** Construct a handler for connections established.
    	 *  @param eventEmitter The JavaScript object that will emit socket
    	 *   events.
    	 *  @param socket The Vertx socket object.
    	 */
    	public SocketWrapper(ScriptObjectMirror eventEmitter, Object socket) {
    		_eventEmitter = eventEmitter;
    		_socket = (NetSocket)socket;
    		
    	    // Set up handlers for data, errors, etc.
    	    _socket.closeHandler((Void) -> {
    			_issueResponse(() -> {
    				_eventEmitter.callMember("emit", "close");
    			});
    	    });
    	    _socket.drainHandler((Void) -> {
    			// FIXME: This should unblock send(),
    			// which should block itself when the buffer gets full.
    	    });
    	    _socket.endHandler((Void) -> {
    	    	// End event on the socket triggers a close of the socket.
    	    	// This gets called when the remote side sends a FIN packet.
    	    	// FIXME: This isn't right. Need FIN from both ends to close the socket!
    	    	// _client.close();
    	    });
    	    _socket.exceptionHandler(throwable -> {
    			_error(_eventEmitter, throwable.toString());
    	    });
    	    _socket.handler(buffer -> {
    			_issueResponse(() -> {
    				// FIXME: handle the buffer data more intelligently here.
    				// If the received type is 'double', 'byte', etc., then do multiple emits.
    				// See defaultOptions in the JS module.
    				_eventEmitter.callMember("emit", "data", buffer.toString());
    			});
    	    });
    	}
    	/** Close the socket.
    	 */
		public void close() {
			submit(() -> {
				_socket.close();
			});
		}
		/** Send data over the socket.
		 *  @param data The data to send.
		 */
		public void send(final Object data) {
			// FIXME: Should block if the send buffer is full.
			
			submit(() -> {
				// FIXME: Need to handle data types here.
				if (data instanceof String) {
					// FIXME: A second argument could take an encoding.
					// Defaults to UTF-8. Option?
					_socket.write((String)data);
				} else {
					_error(_eventEmitter, "Unsupported type for socket: "
							+ data.getClass().getName());
				}
			});
		}
		private NetSocket _socket;
		private ScriptObjectMirror _eventEmitter;
    }
}
