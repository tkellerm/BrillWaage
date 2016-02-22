package de.abasgmbh.brill.waage;

import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.io.*;

import javax.management.BadAttributeValueExpException;

import org.apache.log4j.Logger;

import de.abas.ceks.jedp.CantBeginEditException;
import de.abas.ceks.jedp.CantBeginSessionException;
import de.abas.ceks.jedp.CantChangeFieldValException;
import de.abas.ceks.jedp.CantSaveException;
import de.abas.ceks.jedp.EDPException;
import de.abas.ceks.jedp.InvalidRowOperationException;
import de.abasgmbh.brill.waage.config.Waage;
import de.abasgmbh.brill.waage.config.WaageConfiguration;

/**
 *
 * @author Tobias Kellermann
 */
public class SocketClient extends Thread {
    
	private static Logger log = Logger.getLogger(SocketClient.class.getName());
	private final String NEW_LINE = "\r\n";
	private final String ANFANGS_ZEICHEN =  Character.toString((char)2);
	private final String ENDE_ZEICHEN =  Character.toString((char)3);
	
    private static SocketClient socketClient=null;
    private Socket socket=null;
    private BufferedInputStream in;
    private boolean desonnected=false;
    private PrintWriter out = null;
    private Integer leuchtZeit;
    private Integer textZeit;
    private WaageConfiguration waageConfiguration;
    private AbasRueckmeldung abasrueckmeldung;
	private String waageName;
	private String waageIP;
	private SocketAddress socketadress;
	private int waagePort;
    
    public synchronized void setDesonnected(boolean cr) {
        desonnected=cr;
    }
    
    private SocketClient( Socket s) {
        super("SocketClient");
        socket=s;
        setDesonnected(false);
        start();
    }
    
    public SocketClient(WaageConfiguration waageConfiguration, Integer waagenindex, AbasRueckmeldung abasrueck) throws UnknownHostException{
    	super("SocketClient");
    	Waage waage = null;
		try {
			this.waageConfiguration = waageConfiguration;
			this.abasrueckmeldung = abasrueck;
			
			waage = this.waageConfiguration.getWaage(waagenindex);
		
	    	this.leuchtZeit = waage.getLeuchtdauer();
	    	this.textZeit = waage.getTextdauer();
	    	this.waageName = waage.getName();
	    	this.waageIP = waage.getIpadress();
	    	this.waagePort = waage.getPort();
	    	
	    	while (this.socket == null) {
				if (Util.checkHost(waage.getIpadress())) {
					try {
						this.socket = new Socket(waage.getIpadress(),
								waage.getPort());
						this.socketadress = socket.getRemoteSocketAddress();
						log.info("Die Waage" + waage.getName()
								+ " mit der IP-Adresse " + waage.getIpadress()
								+ " wird nun überwacht");
					} catch (Exception e) {
						log.error("Die Socketverbindung zu Waage " + waageName + " mit IP " + waageIP + " konte nicht hergestellt werden", e);
					}
					
				} else {
					log.error("Die Waage" + waage.getName()
							+ " mit der IP-Adresse " + waage.getIpadress()
							+ " ist nicht erreichbar");
				}
			}
			setDesonnected(false);
	    	start();
		} catch (BadAttributeValueExpException e) {
			log.error(e);	
		}
    }
    
    
    public static synchronized SocketClient handle(Socket s) {
        if(socketClient==null)
            socketClient=new SocketClient(s);
        else {
            if(socketClient.socket!=null) {
                try	{
                    socketClient.socket.close();
                } catch (Exception e)	{
                    
                }
            }
            socketClient.socket=null;
            socketClient=new SocketClient(s);
        }
        return socketClient;
    }
    
    public void run() {
        InputStream is = null;
        try {
            is = socket.getInputStream();
            in = new BufferedInputStream(is);
        } catch(IOException e) {
            try {
                socket.close();
            } catch(IOException e2) {
                System.err.println("Socket not closed :"+e2);
            }
            
            return;
        }
        String rueckString = "";
        Boolean rueckMeldungActive = false;
        while(!desonnected) {
        	if (!socket.isConnected()) {
				log.error("Socket Verbindung zu Waage " + this.waageName + " mit IP " + this.waageIP + " wurde unterbrochen!");
				
				try {
					this.socket.connect(this.socketadress);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					log.error("Fehhler beim Wiederverbinden zu Waage " + this.waageName + " mit IP " + this.waageIP ,e);
				}
			};
        	Rueckmeldung rueckMeldung=null;
        	 ArrayList<String> rueckschlange = new ArrayList<String>();
        	try {
                String inputString = readInputStream(in); //in.readLine();
                if(inputString== null) {
                    //parent.error("Connection closed by client");
                    log.error("Connection closed for waage " + waageName + " mit IP " + waageIP );
                    boolean isCon = socket.isConnected();
                    boolean isbound = socket.isBound();
                    boolean isinputshutdown = socket.isInputShutdown();
                    boolean isoutputshutdown = socket.isOutputShutdown();
                    socket.close();
                    Boolean newSocket = false;
                    while (!newSocket) {
						try {
							log.trace("Vor neuer Socketverbindung");							
							this.socket = new Socket(waageIP , waagePort );
							log.trace("Nach neuer Socketverbindung");
							is = socket.getInputStream();
				            in = new BufferedInputStream(is);
				            newSocket = true;
						} catch (IOException e) {
							log.error("Connection not opend for waage " + waageName + " mit IP " + waageIP , e );
						}	
					}
                    log.info("Die Waage" + this.waageName
							+ " mit der IP-Adresse " + waageIP
							+ " wird wieder überwacht");    				
                }
                if (rueckString != null) {
				int length = rueckString.length();				
                if (rueckMeldungActive) {
                	if (inputString.contains(ENDE_ZEICHEN)) {
    					String teilString[] = inputString.split(ENDE_ZEICHEN);
    					if (teilString.length>0) {
							rueckString = rueckString + teilString[0];
							rueckschlange.add(rueckString);
							rueckMeldungActive = false;
						}
						for (int i = 1; i < teilString.length; i++) {
							if ((teilString[i].contains(ENDE_ZEICHEN)) & (teilString[i].contains(ANFANGS_ZEICHEN))) {
								rueckschlange.add(teilString[i]);
								rueckMeldungActive = false;
							}else if (teilString[i].contains(ANFANGS_ZEICHEN)) {
									rueckString = teilString[i];
									rueckMeldungActive = true;
								}
							}
						}else if (inputString.contains(ANFANGS_ZEICHEN)) {
							String teilString[] = inputString.split(ANFANGS_ZEICHEN);
							if (teilString.length > 0) {
								rueckString = teilString[0];
								rueckMeldungActive = true;
							}
					
						}
                }else {
//					Falls kein Anfangszeichen enthalten ist, wird auch keine Rückmeldung erzeugt.
                	if (inputString.contains(ANFANGS_ZEICHEN)) {
						String teilString[] = inputString.split(ANFANGS_ZEICHEN);
						if (teilString.length > 1) {
//							Der Start beginnt ja erst nach dem Startzeichen
							for (int i = 1; i < teilString.length; i++) {
								if (teilString[i].contains(ENDE_ZEICHEN)) {
									rueckschlange.add(teilString[1]);
									rueckMeldungActive = false;
								}else {
									rueckString = teilString[i];
									rueckMeldungActive = true;
								}	
							}							
						}
					}

				}
                }
                
                for (String rueckMeldungString : rueckschlange) {

					rueckMeldung = new Rueckmeldung(rueckMeldungString);
				
                if (rueckMeldung.isRueckmeldung()) {
				
                	Integer led = this.abasrueckmeldung.meldung(rueckMeldung);
                    switch (led) {
    				case 1:
//    					grüne Lampe anschalten
    					lampeAnschalten(LEDS.GREEN);
    					break;
    				case 2:
//    					gelbe Lampe anschalten
    					lampeAnschalten(LEDS.YELLOW);
    					break;
    				case 3:
//    					rote Lampe anschalten
    					lampeAnschalten(LEDS.RED);
    					break;
    				case 4:
//    					leiser Piepser anschalten
    					lampeAnschalten(LEDS.PIEPSLEISE);
    					break;
    				case 5:
//    					lauter Piepser anschalten
    					lampeAnschalten(LEDS.PIEPSLAUT);
    					break;	
    				default:
    					break;
    				}
                	
				}
                }
                rueckschlange.clear();
                
            } catch(IOException e) {
                if(!desonnected) {
//                    parent.error(e.getMessage(),"Connection lost");
//                    parent.disconnect();
//                	socket schließen
                }
                break;
            } catch (CantChangeFieldValException e) {
            	log.error("Rueckmeldung anlegen schiefgelaufen!", e);
				fehlerAnWaage("Rueckmeldung nicht erfolgreich");
			} catch (CantBeginSessionException e) {
				log.error("Rueckmeldung anlegen schiefgelaufen!", e);
				fehlerAnWaage("Rueckmeldung nicht erfolgreich");
			} catch (CantBeginEditException e) {
				log.error("Rueckmeldung anlegen schiefgelaufen!", e);
				fehlerAnWaage("Rueckmeldung nicht erfolgreich");
			} catch (InvalidRowOperationException e) {
				log.error("Rueckmeldung anlegen schiefgelaufen!", e);
				fehlerAnWaage("Rueckmeldung nicht erfolgreich");
			} catch (CantSaveException e) {
				log.error("Rueckmeldung anlegen schiefgelaufen!", e);
				fehlerAnWaage("Rueckmeldung nicht erfolgreich");
			} catch (EDPException e) {
				log.error("Rueckmeldung anlegen schiefgelaufen!", e);
				fehlerAnWaage("Rueckmeldung nicht erfolgreich");
			}
        }//end of while
        try	{
            is.close();
            in.close();
            //socket.close();
        } catch (Exception err) {}
        socket=null;
    }//end of run
    
    private void fehlerAnWaage(String string) {
		lampeAnschalten(LEDS.PIEPSLAUT);
		sendText(string);
		
	}

	private void sendText(String string) {
		String befehl = "<CA" + string + "><NO" + textZeit.toString() + "><CC>";
    	sendMessage(befehl);
    	System.out.println("<CA" + " " + string);
    	log.info("<CA" + " " + string);
		
	}

	private void lampeAnschalten(LEDS led) {
		// TODO Auto-generated method stub
    	String befehl = led.getAnschaltCmdString() + "<NO" + leuchtZeit.toString() + ">" + led.getAusschaltCmdString();
    	sendMessage(befehl);
    	log.info("Lampen Befehl : " + led.name() + " " + befehl);
	}

	private static String readInputStream(BufferedInputStream _in) throws IOException {
        String data = "";
        int s = _in.read();
        if(s==-1)
            return null;
        data += ""+(char)s;
        int len = _in.available();
//        System.out.println("Len got : "+len);
        if(len > 0) {
            byte[] byteData = new byte[len];
            _in.read(byteData);
            data += new String(byteData);
            log.trace(data);
//            System.out.println(data);
        }
        return data;
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
            
            try {
				socket.close();
			} catch (IOException e1) { 
//				nichtsmachen			
				}
        }
    }
}
