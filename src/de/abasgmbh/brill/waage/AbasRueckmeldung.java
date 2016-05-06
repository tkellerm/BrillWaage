package de.abasgmbh.brill.waage;

import org.apache.log4j.Logger;

import sun.security.acl.OwnerImpl;
import de.abas.ceks.jedp.CantBeginSessionException;
import de.abas.ceks.jedp.CantChangeFieldValException;
import de.abas.ceks.jedp.CantReadSettingException;
import de.abas.ceks.jedp.CantSaveException;
import de.abas.ceks.jedp.ConnectionLostException;
import de.abas.ceks.jedp.EDPEditAction;
import de.abas.ceks.jedp.EDPEditRefType;
import de.abas.ceks.jedp.EDPEditor;
import de.abas.ceks.jedp.EDPException;
import de.abas.ceks.jedp.EDPFactory;
import de.abas.ceks.jedp.EDPQuery;
import de.abas.ceks.jedp.EDPSession;
import de.abas.ceks.jedp.EDPVariableLanguage;
import de.abas.ceks.jedp.InvalidQueryException;
import de.abas.ceks.jedp.InvalidRowOperationException;
import de.abas.ceks.jedp.StandardEDPSelection;
import de.abas.ceks.jedp.StandardEDPSelectionCriteria;
import de.abasgmbh.brill.waage.config.WaageConfiguration;

public class AbasRueckmeldung implements Runnable {

	private static final int TIMEOUT =  86400000; //Timeout ein Tag
	private static Logger log = Logger.getLogger(AbasRueckmeldung.class.getName());
	final String arbeitschein = "" ;
	final Double menge=0.0;
	
	private String server;
	private Integer port;
	private String mandant;
	private String passwort;
	
	private String Fehler;
	private EDPSession edpSession;
	
	private Double rotGrenze;
	private Double gelbGrenze;
	
	private Boolean isConnected;
	private WaageConfiguration waageConfiguration;
	
	
	@Override
	public void run() {
		
//		edp prozess session aufbauen
		this.edpSession = EDPFactory.createEDPSession (); 
		Boolean errorFlag = false;
		while (!errorFlag ) {
			if (!waageConfiguration.pidFileexists()) {
				log.info("pid-File existiert nicht mehr");
				errorFlag = true;
			}
			Boolean connetedt = edpSession.isConnected();
			if (connetedt) {
				try {
					
					String version = edpSession.getABASVersion();
//					log.trace("Schleife AbasRueckmeldung " + version);
					Thread.sleep(1000);
				} catch (CantReadSettingException e1) {
					// TODO Auto-generated catch block
					log.error(e1);
					edpSession.endSession();
					this.isConnected = false;
				}catch (ConnectionLostException e) {
					log.error(e);
					edpSession.endSession();
					this.isConnected = false;
				} catch (InterruptedException e) {
					log.error(e);
				}
			}
			if (!edpSession.isConnected()) {
				try {
					sessionAufbauen(server, port, mandant, passwort);
				} catch (CantBeginSessionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					log.error(e);
					errorFlag = true;
				}
			}
		}
		
	}
	
	public synchronized Rueckmeldung meldung(Rueckmeldung rueckMeldung) throws EDPException {
		Integer wert = 0;
		if (!this.edpSession.isConnected()) {
			sessionAufbauen(this.server, this.port, this.mandant, this.passwort);
		}
				
		EDPEditor editor = this.edpSession.createEditor();
		erzeugeRueckmeldung(rueckMeldung , editor);
		EDPQuery query = this.edpSession.createQuery();
		Double prozent = selectOffeneMengeBa(rueckMeldung, query); 
		if (prozent > this.gelbGrenze) {
//			GrünLeuchtet
			wert = 1;
		}else if (prozent <= this.gelbGrenze && prozent > this.rotGrenze ) {
//			Gelb leuchtet
			wert = 2; 
		}else {
//			Rot leuchtet
			wert = 3;
		}
		rueckMeldung.setLed(wert);
		log.trace("Rückgabe Meldung : Prozent :" + prozent + " rotGrenze : " + this.rotGrenze.toString() + "gelbGrenze : " + this.gelbGrenze.toString() +"  Rückgabe Wert :" + wert);
		return rueckMeldung ;
	}
	
	private Double selectOffeneMengeBa(Rueckmeldung rueckMeldung, EDPQuery query) throws EDPException {
		
		String betriebauftrag = rueckMeldung.getBetriebsauftrag();
		String krit = "idno=="  + betriebauftrag +  ";@englvar=true;@language=en;" ;		
		StandardEDPSelectionCriteria criteria = new StandardEDPSelectionCriteria(krit);
		StandardEDPSelection edpcriteria = new StandardEDPSelection("9:1", criteria); 
		edpcriteria.setDatabase("9");
		edpcriteria.setGroup("1");
		String[] fieldNames = {"id" , "unitQty" , "confYieldSUTotal" };
		
//			
			try {
				query.startQuery(edpcriteria, fieldNames);
				query.getLastRecord();
				int anzahlTreffer = query.getRecordCount();
				if (anzahlTreffer == 1) {
//					String[] fields = query.getFields();
//					int fieldCount = query.getFieldCount();
					String offeneMengeStr = query.getField("unitQty");
					String rueckgemeldeteMengeStr = query.getField("confYieldSUTotal");
					Double offeneMenge = new Double(offeneMengeStr);
					Double rueckgemeldeteMenge = new Double(rueckgemeldeteMengeStr);
					Double gesamtmenge = offeneMenge + rueckgemeldeteMenge;
					Double prozent = (offeneMenge /gesamtmenge ) * 100;
					rueckMeldung.setOfMenge(offeneMenge);
					log.trace("Query Ergebnis : BA : " + betriebauftrag + " offene Menge : " + offeneMengeStr + " Gebuchte Menge : " + rueckgemeldeteMengeStr +   " prozent " + prozent.toString());
					return prozent;
					
				}
			
			} catch (InvalidQueryException e) {
				log.error(e);
				throw new EDPException("Fehler in Query" , e);
			}finally{
				query.breakQuery();
			}
			throw new EDPException("Kein Treffer in der Selektion");
		
	}

	private void erzeugeRueckmeldung(Rueckmeldung rueckMeldung, EDPEditor editor) throws EDPException {
		
		
	        String databaseName = "9";
            String groupName = "1";
            
            String asnum = rueckMeldung.getBetriebsauftrag();
	        
	        log.trace("Begin EDP-EDITOR für Rückmeldung " + asnum );
		editor.beginEdit(EDPEditAction.DONE, databaseName , groupName,EDPEditRefType.NUMSW , asnum);
		
//		Bemerkung füllen 
		try {
			editor.setFieldVal("comments" , "Nr: " + rueckMeldung.getLaufendeNummer() + " W: " + rueckMeldung.getWaagenNummer() + " NG: " + rueckMeldung.getNettoGewicht().toString() + " refG: " + rueckMeldung.getRefgewicht().toString() );
			editor.setFieldVal("printImmediately", "1");
			if (editor.getRowCount() == 0) {
				editor.insertRow(1);		
			}
			editor.setFieldVal(1, "confYieldSU" , rueckMeldung.getStueck().toString());
			editor.endEditSave();
		} catch (CantChangeFieldValException e) {
			log.error(e);
			throw new EDPException("Es konnte ein Feld nicht beschrieben werden!", e);
		} catch (InvalidRowOperationException e) {
			log.error(e);
			throw new EDPException("Zeilenoperation fehlgeschlagen!",e);
		} catch (CantSaveException e) {
			log.error(e);
			throw new EDPException("Es konnte nicht gespeichert werden", e);
		}finally{
			if (editor.isActive()) {
				editor.endEditCancel();
			}
		}
		log.trace("Ende EDP-EDITOR für Rückmeldung " + asnum );
	}

	public AbasRueckmeldung(WaageConfiguration configuration) {
		if (configuration != null) {
			this.server = configuration.getEdpServer();
			this.mandant = configuration.getMandant();
			this.port =  configuration.getPort();
			this.passwort = configuration.getPassword();
			this.gelbGrenze = configuration.getGelbGrenze();
			this.rotGrenze = configuration.getRotGrenze();
			this.waageConfiguration = configuration;
		}else {
			String error = "Die Konfiguration hat leider den Wert null!";
			log.error(error);
			throw new NullPointerException(error);
		}
	}

	
	

	private void sessionAufbauen(String server, int port, String mandant, String passwort) throws CantBeginSessionException   
    { 
			log.trace("Session wird versucht aufzubauen Server: " + server + " Port : " + port + " Mandant : " + mandant + " Passwort : " + passwort);
    	  		try {
    	  			this.edpSession.setConnectTimeout(TIMEOUT);
    	  			this.edpSession.beginSession(server , port, mandant, passwort, "Waage");
    	  			this.isConnected = true;
    	  		} catch (CantBeginSessionException ex) 
    	  			{
    	  			this.isConnected = false;
    	  			log.error(ex);
    	  			throw new CantBeginSessionException("FEHLER\n EDP Session kann nicht gestartet werden\n" , ex);
    	  			}
                 log.trace("Session wurde aufgebaut");
              this.edpSession.setVariableLanguage(EDPVariableLanguage.ENGLISH);
        
	        
    }

	public Boolean isConnected() {
	
		if (this.edpSession != null) {
			if (this.edpSession.isConnected()) {
				//			check Version
				try {
					String version = edpSession.getABASVersion();
					if (version != null) {
						return true;
					} else
						return false;
				} catch (CantReadSettingException e1) {
					// TODO Auto-generated catch block
					log.error("Prüfung isConnected fehlgeschlagen ", e1);
					return false;

				} catch (ConnectionLostException e) {
					return false;
				}
			} else
				return false;
		}else 
			return false;
		
	}
	
}
