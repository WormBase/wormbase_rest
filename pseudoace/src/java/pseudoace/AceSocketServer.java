package pseudoace;

import java.io.*;
import java.net.*;

public class AceSocketServer {
    private final ServerSocket server;
    final AceSessionHandlerFactory handlerFactory;
    final ThreadGroup tg;
    
    public AceSocketServer(int port, AceSessionHandlerFactory handlerFactory)
        throws Exception
    {
        this.server = new ServerSocket(port);
	this.handlerFactory = handlerFactory;
        this.tg = new ThreadGroup("AceServer-" + port);
        new Thread(/* tg, */ new Listener()).start();
    }

    class Listener implements Runnable {
        public void run() {
            try {
                while (true) {
                    Socket s = server.accept();
                    new Thread(/* tg, */ new AceSocketHandler(AceSocketServer.this, s)).start();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void shutdown()
        throws Exception
    {
        server.close();
        //tg.stop();
    }

    public static void main(String[] args)
        throws Exception
    {
        new AceSocketServer(23002, new AceSessionHandlerFactory() {
		public AceSessionHandler createSession() {
		    return new AceSessionHandler() {
			public String transact(Closeable session, String cmd) {
			    return "executing " + cmd;
			}
		    };
		}
	    });
    }
}
