package de.abasgmbh.brill.waage;

public enum LEDS {
	GREEN ("<FK1060>","<FK10699999>"), YELLOW ("<FK1050>","<FK105999999>"), RED("<FK1070>","<FK107999999>"), PIEPSLEISE("<FK1080>","<FK108999999>") , PIEPSLAUT ("<FK1080>","<FK108999999>") ;
private String[] eintrag;
	
	private LEDS(String text1 , String text2 ) {
		this.eintrag =	new String[2] ;
		this.eintrag[0] = text1;
		this.eintrag[1] = text2;
	}
	
	public String getAusschaltCmdString(){
//		return eintrag[0];
//		Demo Gerät ist anderes Programmiert gewesen daher der Tausch
		return eintrag[1];
	}
	public String getAnschaltCmdString(){
//		return eintrag[1];
//		Demo Gerät ist anderes Programmiert gewesen daher der Tausch
		return eintrag[0];
	}
}
