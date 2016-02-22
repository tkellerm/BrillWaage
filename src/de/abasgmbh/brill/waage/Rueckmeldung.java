package de.abasgmbh.brill.waage;

public class Rueckmeldung {
	
	private String waagenNummer;
	private String laufendeNummer;
	private String betriebsauftrag;
	private Double bruttoGewicht;
	private Double taraGewicht;
	private Double nettoGewicht;
	private Double stueck;
	private Double refgewicht;
	
	
	public Rueckmeldung(String waagenNummer, String laufendeNummer,
			String betriebsauftrag, Double bruttoGewicht, Double taraGewicht,
			Double nettoGewicht, Double stueck, Double refgewicht) {
		this.waagenNummer = waagenNummer;
		this.laufendeNummer = laufendeNummer;
		this.betriebsauftrag = betriebsauftrag;
		this.bruttoGewicht = bruttoGewicht;
		this.taraGewicht = taraGewicht;
		this.nettoGewicht = nettoGewicht;
		this.stueck = stueck;
		this.refgewicht = refgewicht;
	}
	

	public Rueckmeldung(String nachricht) {
		
//		;001;       198;ABCD12345678;   0.000;   0.000;   0.000;       0; 0.00000;
		String[] dataset = nachricht.split(";");
		int length = dataset.length;
		if (length >= 9  ) {
//			Wir haben von der Anzahl die richtigen Daten
			
			this.waagenNummer = dataset[1].replaceAll(" ", "");
			
			this.laufendeNummer = dataset[2].replaceAll(" ", "");
			this.betriebsauftrag = dataset[3].replaceAll(" ", "");			 
			this.bruttoGewicht = string2Double(dataset[4]); 
			this.taraGewicht = string2Double(dataset[5]);
			this.nettoGewicht = string2Double(dataset[6]);
			this.stueck = string2Double(dataset[7]);
			this.refgewicht = string2Double(dataset[8]);
		}
		
		
//		nur zu testszwecken
//		this.betriebsauftrag = "1050001";
		
		
	}


	private Double string2Double(String string) {
		String ohneLeerrzeichen = string.replaceAll(" ", "");
		String ohnekomma =ohneLeerrzeichen.replaceAll(",",".");
		return new Double(ohnekomma);
	}


	public String getWaagenNummer() {
		return waagenNummer;
	}


	public String getLaufendeNummer() {
		return laufendeNummer;
	}


	public String getBetriebsauftrag() {
		return betriebsauftrag;
	}


	public Double getBruttoGewicht() {
		return bruttoGewicht;
	}


	public Double getTaraGewicht() {
		return taraGewicht;
	}


	public Double getNettoGewicht() {
		return nettoGewicht;
	}


	public Double getStueck() {
		return stueck;
	}


	public Double getRefgewicht() {
		return refgewicht;
	}


	public boolean isRueckmeldung() {
		
		return !betriebsauftrag.isEmpty();
	}
	
	
	

}
