package de.abasgmbh.brill.waage;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;

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
				if (checkHost(waage.getIpadress())) {
					try {
						this.socket = new Socket(waage.getIpadress(),
								waage.getPort());
						this.socket.setKeepAlive(true);
						this.socketadress = socket.getRemoteSocketAddress();
						log.info(this.waageName + " Die Waage" + waage.getName()
								+ " mit der IP-Adresse " + waage.getIpadress()
								+ " wird nun überwacht");
					} catch (Exception e) {
						log.error(this.waageName + " Die Socketverbindung zu Waage " + waageName + " mit IP " + waageIP + " konte nicht hergestellt werden", e);
					}
					
				} else {
					log.error(this.waageName + " Die Waage" + waage.getName()
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
            log.trace(this.waageName + " an Stream horchen" );
        } catch(IOException e) {
            try {
                socket.close();
            } catch(IOException e2) {
                System.err.println(this.waageName +"Socket not closed :"+e2);
            }
            
            return;
        }
        File pidFileSicherung = waageConfiguration.getPidFile();
        String rueckString = "";
        Boolean rueckMeldungActive = false;
        Boolean errorFlag =false;
        Boolean reconnect = false;
        while(!errorFlag) {
        	log.trace(this.waageName + " Schleife Socketclient");
//        	Änderung da der direkte Aufruf von pidFileexists in die Hose ging.
        	if (!waageConfiguration.pidFileexists()) {
				log.info(this.waageName +" pid-File existiert nicht mehr");
				File pidfile = waageConfiguration.getPidFile();
				log.error(this.waageName +" Das PID-File hat den Namen " + pidfile.getAbsolutePath() +" und das PID-Sicherung " + pidFileSicherung.getAbsolutePath());
				if (!pidfile.exists()) {
					log.error(this.waageName +" Das pidfile existiert nicht!");
					if (!pidFileSicherung.exists()) {
						log.error(this.waageName +" Das pidfileSicherung existiert auch nicht!");
						errorFlag = true;	
						
					}else {
						log.error(this.waageName +" Das pidfileSicherung existiert!");
					}
						
				}
				
			}
        	if (!socket.isConnected()) {
				log.error(this.waageName + " Socket Verbindung zu Waage " + this.waageName + " mit IP " + this.waageIP + " wurde unterbrochen!");
				reconnect = true;
			};
        	Rueckmeldung rueckMeldung=null;
        	 ArrayList<String> rueckschlange = new ArrayList<String>();
        	 String inputString = null;
        	try {
                inputString = readInputStream(in, this.waageName); //in.readLine();
                log.info(this.waageName + " : " + inputString);
                if(inputString== null || reconnect) {
                    //parent.error("Connection closed by client");
                    log.error(this.waageName + " Connection closed for waage " + this.waageName + " mit IP " + waageIP );
//                    boolean isCon = socket.isConnected();
//                    boolean isbound = socket.isBound();
//                    boolean isinputshutdown = socket.isInputShutdown();
//                    boolean isoutputshutdown = socket.isOutputShutdown();
                    socket.close();
                    Boolean newSocket = false;
                    while (!newSocket) {
						try {
							log.trace(this.waageName +   " Vor neuer Socketverbindung");							
							this.socket = new Socket(waageIP , waagePort );
							this.socket.setKeepAlive(true);
							log.trace(this.waageName +   " Nach neuer Socketverbindung");
							is = socket.getInputStream();
							log.trace(this.waageName +   " Nach getinputstream");
//							Stream schliessen, bevor wieder geöffnet wird
							in.close();
				            in = new BufferedInputStream(is);
				            log.trace(this.waageName +   " Nach BufferedInputstream");
				            newSocket = true;
						} catch (IOException e) {
							log.error(this.waageName +   " Connection not opend for waage " +  " mit IP " + waageIP , e );
						} catch (Exception e){
							log.error(this.waageName +   " neue Exception "  , e );
						}
						log.trace(this.waageName + " Ende NewSocket Schleife" );
					}
                    
//                  zum Testen Daten wegschicken, da dann eine Antwort erfolgt
                    log.trace(this.waageName + " Vor lampeAnschalten PIEPSLEISE" );
                    lampeAnschalten(LEDS.PIEPSLEISE);
                    log.trace(this.waageName + " Vor readInputStream" );
                    inputString = readInputStream(in, this.waageName);
                    log.info("Die Waage" + this.waageName
							+ " mit der IP-Adresse " + waageIP
							+ " wird wieder überwacht");    				
                }
                
                if (rueckString != null) {				
                if (rueckMeldungActive) {
                	if (inputString.contains(ENDE_ZEICHEN)) {
    					String teilString[] = inputString.split(ENDE_ZEICHEN);
    					if (teilString.length>0) {
							rueckString = rueckString + teilString[0];
							rueckschlange.add(rueckString);
							log.trace(this.waageName + " RückmeldungString : " + rueckString);
							rueckMeldungActive = false;
						}
						for (int i = 1; i < teilString.length; i++) {
							if ((teilString[i].contains(ENDE_ZEICHEN)) & (teilString[i].contains(ANFANGS_ZEICHEN))) {
								rueckschlange.add(teilString[i]);
								log.trace(this.waageName + " RückmeldungString : " + teilString[1]);
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
                	
                	if (inputString != null) {
                		if (inputString.contains(ANFANGS_ZEICHEN)) {
							String teilString[] = inputString.split(ANFANGS_ZEICHEN);
							if (teilString.length > 1) {
	//							Der Start beginnt ja erst nach dem Startzeichen
								for (int i = 1; i < teilString.length; i++) {
									if (teilString[i].contains(ENDE_ZEICHEN)) {
										rueckschlange.add(teilString[1]);
										log.trace(this.waageName + " RückmeldungString : " + teilString[1]);
										rueckMeldungActive = false;
									}else {
										rueckString = teilString[i];
										rueckMeldungActive = true;
									}	
								}							
							}
						}
					}else {
						log.error("Die Variable inputstring ist null.Es erfolgt einreconnect");
						reconnect = true;
					}
	                	

				}
                }
                
                for (String rueckMeldungString : rueckschlange) {

					rueckMeldung = new Rueckmeldung(rueckMeldungString);
					
                if (rueckMeldung.isRueckmeldung()) {
                	rueckMeldung = this.abasrueckmeldung.meldung(rueckMeldung);
                	Integer led = rueckMeldung.getLed(); 
                    switch (led) {
    				case 1:
//    					grüne Lampe anschalten
    					rueckmeldungAnWaageSenden(LEDS.GREEN,rueckMeldung.getOfMenge());
    					break;
    				case 2:
//    					gelbe Lampe anschalten
    					rueckmeldungAnWaageSenden(LEDS.YELLOW,rueckMeldung.getOfMenge());
    					break;
    				case 3:
//    					rote Lampe anschalten
    					rueckmeldungAnWaageSenden(LEDS.RED,rueckMeldung.getOfMenge());
    					break;
    				case 4:
//    					leiser Piepser anschalten
    					rueckmeldungAnWaageSenden(LEDS.PIEPSLEISE,rueckMeldung.getOfMenge());
    					break;
    				case 5:
//    					lauter Piepser anschalten
    					rueckmeldungAnWaageSenden(LEDS.PIEPSLAUT,rueckMeldung.getOfMenge());
    					break;	
    				default:
    					log.info("Break in Case Wert LED : " + led );
    					break;
    				}
                	
				}
                }
                rueckschlange.clear();
        	} catch(SocketException e) {
            	log.error(this.waageName + "Socketverbindung wurde unterbrochen!",e);
            	reconnect = true;                
            } catch(IOException e) {
            	log.error(e);
            	reconnect = true;
            } catch (CantChangeFieldValException e) {
            	log.error(this.waageName + "Rueckmeldung anlegen schiefgelaufen!", e);
				fehlerAnWaage("Rueckmeldung nicht erfolgreich");
			} catch (CantBeginSessionException e) {
				log.error(this.waageName + "Rueckmeldung anlegen schiefgelaufen!", e);
				fehlerAnWaage("Rueckmeldung nicht erfolgreich");
			} catch (CantBeginEditException e) {
				log.error(this.waageName + "Rueckmeldung anlegen schiefgelaufen!", e);
				fehlerAnWaage("Rueckmeldung nicht erfolgreich");
			} catch (InvalidRowOperationException e) {
				log.error(this.waageName + "Rueckmeldung anlegen schiefgelaufen!", e);
				fehlerAnWaage("Rueckmeldung nicht erfolgreich");
			} catch (CantSaveException e) {
				log.error(this.waageName + "Rueckmeldung anlegen schiefgelaufen!", e);
				fehlerAnWaage("Rueckmeldung nicht erfolgreich");
			} catch (EDPException e) {
				log.error(this.waageName + "Rueckmeldung anlegen schiefgelaufen!", e);
				fehlerAnWaage("Rueckmeldung nicht erfolgreich");
			}catch (Exception e){
				log.error(this.waageName + " unbekannte Exception 2", e);
			}
        }//end of while
        log.trace(this.waageName + " PID FILE nicht gefunden - Socketclient wird beendet");
        try	{
            is.close();
            in.close();
            //socket.close();
        } catch (Exception err) {}
        socket=null;
    }//end of run
    
    private void rueckmeldungAnWaageSenden(LEDS led, Double ofMenge) {
    	lampeAnschalten(led);
    	sendText("offene Menge : " + ofMenge.toString());
    	labelDrucken();
    	waageZurücksetzen();
	}

	private void fehlerAnWaage(String string) {
		lampeAnschalten(LEDS.PIEPSLAUT);
		sendText(string);
		waageZurücksetzen();
		
	}
    
	private void sendText(String string) {
		String befehl = "<CA" + string + "><NO" + textZeit.toString() + "><CC>";
    	sendMessage(befehl);
    	log.info(this.waageName + " <CA" + " " + string);
		
	}
	
	private void waageZurücksetzen() {
		String befehl = "<FR>";
//		<VS0020> löscht den Betriebsauftrag aus der Waage 
		befehl = befehl + "<VS0020>";
    	sendMessage(befehl);
    	log.info(this.waageName + " Waage zurücksetzen " + befehl);
	} 
	private void labelDrucken(){
		String befehl = "<FP099>";
    	sendMessage(befehl);
    	log.info(this.waageName  + " Label drucken" + " " + befehl);
		
	}
	private void lampeAnschalten(LEDS led) {
		// TODO Auto-generated method stub
    	String befehl = led.getAnschaltCmdString() + "<NO" + leuchtZeit.toString() + ">" + led.getAusschaltCmdString();
    	sendMessage(befehl);
    	log.info(this.waageName + " Lampen Befehl : " + led.name() + " " + befehl);
	}

	private static String readInputStream(BufferedInputStream _in,String _waageName) throws IOException {
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
            log.trace( _waageName + " " + data);
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

    private  boolean checkHost(String host) {
        try {
            InetAddress adress = InetAddress.getByName(host);
            
            boolean erreichbar = adress.isReachable(5000);
            return erreichbar;
        } catch(UnknownHostException e) {
        	log.error(this.waageName + " ist mit der Adresse " + host + " unbekannt " , e);
        	return(false);
        } catch (IOException e) {
        	log.error(this.waageName + " ist mit der Adresse " + host + " nicht ereichbar " , e);
        	return(false);
		}
    }

}
