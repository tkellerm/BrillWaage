package de.abasgmbh.brill.waage;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Date;

import org.apache.log4j.Logger;

public class TestSocket extends Thread {
	
	private static Logger log = Logger.getLogger(SocketClient.class.getName());
	private Socket socket;
	private String waageName;
	private PrintWriter out = null;
	private final String NEW_LINE = "\r\n";
	
	public TestSocket(Socket socket, String waageName) {
		this.socket = socket;
		this.waageName = waageName;
		log.info(this.waageName + " Constructor TestSocketClient");
		start();
	}
	
	
	@Override
	public void run() {
		try {
			Date date = new Date();
			log.trace(this.waageName + " TestSocketClient for Sschleife" + new Date().toString() );
			for (int i = 0; i < 20; i++) {
				Thread.sleep(2000);
				log.trace(this.waageName + " TestSocketClient for PIEPSLEISE" + new Date().toString());
				lampeAnschalten(LEDS.PIEPSLEISE);	
			}
			
		} catch (InterruptedException e) {
			log.error(this.waageName , e);
		}
	}
	
	void lampeAnschalten(LEDS led) {
		// TODO Auto-generated method stub
    	String befehl = led.getAnschaltCmdString() + "<NO" + 1000 + ">" + led.getAusschaltCmdString();
    	sendMessage(befehl);
  
		log.info(this.waageName + " Lampen Befehl : " + led.name() + " " + befehl);
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
        	log.error(this.waageName, e);
            try {
				socket.close();
			} catch (IOException e1) { 
//				nichtsmachen			
				}
        }
    }
	
	
}
