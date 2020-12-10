/**
 * @Clase: PosicionTexto.java
 * 
 * @version  1.0
 * 
 * @since 24/06/2020
 * 
 * @autor Ing. Jhon Mendez
 *
 * @Copyrigth: Legal Credit Solution
 */


package com.legalCredit.componentes;

/**
 * 
 * Define un tipo de datos para almacenar la posicion de un texto dentro un pdf 
 *
 */

public class PosicionTexto {
	
	private String texto;
	private double posXInicial;
	private double posYInicial;
	private double posXFinal;
	private double posYFinal;
	
	
	public PosicionTexto(String texto, double posXInicial, double posYInicial, double posXFinal, double posYFinal) {
		
		this.texto = texto;
		this.posXInicial = posXInicial;
		this.posYInicial = posYInicial;
		this.posXFinal = posXFinal;
		this.posYFinal = posYFinal;
	}
	

	public String getTexto() {
		return texto;
	}


	public double getPosXInicial() {
		return posXInicial;
	}


	public double getPosYInicial() {
		return posYInicial;
	}


	public double getPosXFinal() {
		return posXFinal;
	}


	public double getPosYFinal() {
		return posYFinal;
	}

	@Override
	public String toString() {
		// 
		return "Texto: " + texto + "\nposXInicial: "+posXInicial +
				"\nposYInicial: "+posYInicial +
				"\nposXFinal: "+posXFinal +
				"\nposXFinal: "+posYFinal +
				"\n";
		
		
	}
}
