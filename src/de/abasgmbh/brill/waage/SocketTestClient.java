package de.abasgmbh.brill.waage;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.management.BadAttributeValueExpException;
import javax.swing.ImageIcon;

import org.apache.log4j.Logger;

import com.sun.xml.internal.ws.resources.SenderMessages;

import de.abasgmbh.brill.waage.config.Waage;
import de.abasgmbh.brill.waage.config.WaageConfiguration;


public class SocketTestClient {
	private static Logger log = Logger.getLogger(SocketTestClient.class.getName());
    private final String NEW_LINE = "\r\n";
    
    private Socket socket = null;
    private PrintWriter out;
    private SocketClient socketClient;
    private static WaageConfiguration waageConfiguration ;
    
    

	public static void main(String[] args) {
    		AbasRueckmeldung abasrueck;
			Thread threadAbas = new Thread(abasrueck = new AbasRueckmeldung(waageConfiguration));
    		threadAbas.start();
    		try {
    			for (int i = 0; i < waageConfiguration.getAnzahlWaagen() ; i++) {
    				new SocketClient(waageConfiguration , i , abasrueck);
				}
				
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    		
    	
    	
    		
    		
    		
    		
		}
    	
    	
    	
	

    
    
    /////////////////
    //action methods
    //////////////////
    private  boolean connect(String ipadr, int port) {
        if(socket!=null) {
            disconnect();    
        }
                try {
					socket = new Socket(ipadr,port);
					socketClient=SocketClient.handle(socket);
                } catch (UnknownHostException e) {
					
					e.printStackTrace();
				} catch (IOException e) {
					
					e.printStackTrace();
				}finally{
					if (socket.isConnected()) {
						try {
							socket.close();
						} catch (IOException e) {
//							do nothing
						}
					}
				}
				return false;
        
      
        
    }
    
    public synchronized void disconnect() {
        try {
            socketClient.setDesonnected(true);
            socket.close();
        } catch (Exception e) {
            System.err.println("Error closing client : "+e);
        }
        socket=null;
        out=null;
    
        
    }
  
    
   
    
    
    
    public void sendMessage(String s) {
        
        try	{
            if(out==null) {
                out = new PrintWriter(new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream())), true);
            }
        
            out.print(s+NEW_LINE);
            out.flush();
        
            
        } catch (Exception e) {
            
            disconnect();
        }
    }
    
    
    
}
