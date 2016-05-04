package de.abasgmbh.brill.waage.config;


import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.management.BadAttributeValueExpException;

import org.apache.log4j.Logger;


public class WaageConfiguration {
	
	private static Logger log = Logger.getLogger(WaageConfiguration.class.getName());
	
	private String edpServer;
	private Integer port;
	private String mandant;
	private String password;
	private double rotGrenze;
	private double gelbGrenze;
	private File pidFile;
	
	private List<Waage> waagenListe = new ArrayList<Waage>();
	
	private WaageConfiguration() {
//		rotGrenze und gelb Grenze vorblegen
		this.rotGrenze = 0;
		this.gelbGrenze = 10;
    }
	
	
	private static class GetConfig {
        private static final WaageConfiguration INSTANCE = new WaageConfiguration();
    }

    public static WaageConfiguration getInstance() {
        return GetConfig.INSTANCE;
    }
	
	
	
	public WaageConfiguration(String edpServer, Integer port, String mandant,
			String password) throws BadAttributeValueExpException {
		
		if (edpServer != null &
				port != null &
				mandant != null &
				password != null 
				) 
		{
			this.edpServer = edpServer;
			this.mandant = mandant;
			this.password = password;
			this.port = port;
			
			
		}else {		
			log.error("Der EDPserver : " + edpServer +  " ,Port : " + port + " ,Mandant : " + mandant + " Password (Länge=" + password.length() +  "dürfen nicht leersein" );
			throw new BadAttributeValueExpException("Der edpServer, Port, mandant und password dürfen nicht leer sein");
		}
		
	}

	public String getEdpServer() {
		return this.edpServer;
	}

	public String getMandant() {
		return this.mandant;
	}

	public Integer getPort() {
		return this.port;
	}

	public String getPassword() {
		return this.password;
	}
	public Integer getAnzahlWaagen(){
		return this.waagenListe.size();
	}
	
	public Waage getWaage(Integer waagennummer) throws BadAttributeValueExpException{
		
		if (waagennummer !=null) {
			if (waagennummer <= waagenListe.size()) {
				return this.waagenListe.get(waagennummer);	
			}else {
				
			    String errorText = "Die Waagennummer " + waagennummer + "ist größer als die Größe der Liste";
				log.error(errorText);
				throw new BadAttributeValueExpException(errorText);
			}
		}else {
			
			String errorText = "Die Waagennummer darf nicht null sein!";
			log.error(errorText);
			throw new BadAttributeValueExpException("Die Waagennummer darf nicht null sein!");
			
		}
		
	}

	public void addWaage(Waage waage) {		
		this.waagenListe.add(waage);		
	}



	public void setEdpServer(String edpServer) {
		this.edpServer = edpServer;
	}



	public void setPort(Integer port) {
		this.port = port;
	}



	public void setMandant(String mandant) {
		this.mandant = mandant;
	}



	public void setPassword(String password) {
		this.password = password;
	}



	public double getRotGrenze() {
		return rotGrenze;
	}



	public void setRotGrenze(double rotGrenze) {
		this.rotGrenze = rotGrenze;
	}



	public double getGelbGrenze() {
		return gelbGrenze;
	}



	public void setGelbGrenze(double gelbGrenze) {
		this.gelbGrenze = gelbGrenze;
	}



	public void setPIDFile(File pidfile) {
		
		this.pidFile = pidfile;
		
	}
	
	public synchronized Boolean pidFileexists(){
//        log.trace("pid-File ist da " + this.pidFile.exists());
		if (this.pidFile.exists()) {
			return true;
		}else {
			log.trace("pid-File " + this.pidFile.getAbsolutePath() + " nicht ist da " + this.pidFile.exists());
			File test = new File(this.pidFile.getAbsolutePath());
			if (test.exists()) {
				return true;
			}else {
				log.trace("pid-File " + test.getAbsolutePath() + " auch bei der 2. Prüfung nicht ist da " + test.exists());
				return false;
			}
		}
		
	}
	


	
}
