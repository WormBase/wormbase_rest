package pseudoace;

import java.util.*;
import java.io.*;
import java.net.*;
import java.security.*;

class AceSocketHandler implements Runnable, Closeable {
    private final AceSocketServer server;
    private final Socket socket;
    private final DataInputStream dis;
    private final DataOutputStream dos;
    private boolean encore;
    private boolean flip;
    private AceSessionHandler handler;

    public AceSocketHandler(AceSocketServer server, Socket socket)
        throws Exception
    {
        this.server = server;
        this.socket = socket;
        this.dis = new DataInputStream(socket.getInputStream());
        this.dos = new DataOutputStream(socket.getOutputStream());
    }

    public void run() {
        try {
            String msg = readMessage();
            if (!msg.equals("bonjour"))
                throw new Exception("Bad handshake");
            System.err.println(msg);
            writeMessage(AceConstants.MSGOK, "padpadpad");
            String auth = readMessage();
            System.err.println(auth);
            writeMessage(AceConstants.MSGOK, "et bonjour a vous");
            System.out.println("Creating session");
	    handler = server.handlerFactory.createSession();
	    
	    while (true) {
		String cmd = readMessage();
                try {
                    String resp = handler.transact(this, cmd);
                    writeMessage(AceConstants.MSGOK, resp);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    writeMessage(AceConstants.MSGOK, "// Bad Stuff happened");
                }
		
	    }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void close() {
        // FIXME quick'n'dirty, should send shutdown handshake.
        try {
            socket.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void writeMessage(String type, String s) throws IOException {
	dos.writeInt(swizzle(flip, AceConstants.OK_MAGIC));
	dos.writeInt(swizzle(flip, s.length() + 1));
	dos.writeInt(swizzle(flip, 1)); // ???Server version???
	dos.writeInt(swizzle(flip, 1)); // clientId
	dos.writeInt(swizzle(flip, 1<<16)); // maxBytes

	dos.writeBytes(type);

	byte[] padding = new byte[30 - type.length()];
	dos.write(padding, 0, padding.length);

	dos.writeBytes(s);
	dos.write(0);
	dos.flush();
    }

    private String readMessage() throws IOException {
	StringBuilder sb = new StringBuilder();
	while (readMessagePart(sb)) {
	    writeEncore();
	}
	return sb.toString();
    }

    void writeEncore()
	throws IOException
    {
	if (!encore)
	    throw new IllegalStateException();
	writeMessage(AceConstants.MSGENCORE, "encore");
    }
    
    private boolean readMessagePart(StringBuilder sb)
	throws IOException
    { 	
	sb.append(new String(read()));
	return this.encore;
    }

    private int swizzle(boolean flip, int x) {
        if (flip)
            return ((x&0xff)<<24) | ((x&0xff00)<<8) | ((x&0xff0000)>>8) | (x&0xff000000)>>24;
        else
            return x;
    }
    
    byte[] read()
	throws IOException
    {
	int magic = dis.readInt();
        boolean flip;
        if (magic == AceConstants.OK_MAGIC) {
            flip = false;
        } else if (swizzle(true, magic) == AceConstants.OK_MAGIC) {
            flip = true;
        } else {
            throw new IOException("Bad magic " + magic);
        }
        System.err.printf("magic=%x\n", magic);
	int length = swizzle(flip, dis.readInt());

	int rServerVersion = swizzle(flip, dis.readInt()); 
	int rClientId = swizzle(flip, dis.readInt());
	int rMaxBytes = swizzle(flip, dis.readInt());
	byte[] typeb = new byte[30];
	dis.readFully(typeb);
	String type = new String(typeb);

        this.flip = flip;

        /*
	if (pendingConfig) {
	    serverVersion = rServerVersion;
	    clientId = rClientId;
	    maxBytes = rMaxBytes;
	    pendingConfig = false;
            } */

	byte[] message = new byte[length-1];
	dis.readFully(message);
	dis.skipBytes(1);
        
	this.encore = type.startsWith(AceConstants.MSGENCORE);
	return message;
    }
}
    
