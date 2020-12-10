/**
 * @Clase: PatronTransunion.java
 * 
 * @version  1.0
 * 
 * @since 9/07/2020
 * 
 * @autor Ing. Jhon Mendez
 *
 * @Copyrigth: Legal Credit Solution
 */

package com.legalCredit.scraper.patrones;

/**
 * Esta clase leera los reportes de tipo transunion 
 *  
 *  Para que perteneca a este patron debe contene la palabra transunion mas los componentes 
 *  que se describen en la variable elementosAContenerTextoRepetidamente
 *  
 */

public class PatronTransunion {

	private String[] elementosAContenerTextoRepetidamentePatronUno = {"date opened","responsibility","account type","loan type",
			"balance","date updated", "payment received", "last payment made", 
			"high balance", "original creditor", "credit limit", "past due",
			"pay status",  "terms", "date closed"};

	private String[] elementosAContenerTextoRepetidamentePatronDos = {"account name","account #","account type","date opened",
			"account status","payment status","status updated","balance",
			"balance updated","credit limit","monthly payment","past due amount",
			"highest balance","terms","responsibility","your statement",
			"comments","original creditor"};

	private String[] elementosAContenerTextoRepetidamentePatronTres = {"saldo","responsabilidad","cuenta","tipo",
			                                                           "prestamo"};

	private String[] elementosAdicionalesPatronGenerico = {"date of birth","report created on"};

	private String[] elementosAContenerTextoRepetidamentePatronUnoSinAccount = {"requested on","inquiry type","permissible purpose"};

	private String tagEmployee = "employer name";

	private String tagReferenciaObtenerEmployee = "position";

	private String tagPrimerTag = "date opened";

	private String tagAddress = "addresses reported";

	private String tagAddressPatronTres = "domicilio";

	private String tagName = "names reported";

	private String tagNamePatronTres = "nombres informados";

	private String cra = "transunion";

	private String texto;

	private String textoPrimerasCincoLineas;

	private int porcentajePresicion = 70; //Esta variable define el porcentaje de elementos que deben estar presentes para
	//ser de tipo transunion

	public boolean isPatronTransunionAccount(String texto, String numeroPatron) {

		int contador = 0;

		boolean resultado = false;

		if (numeroPatron.equalsIgnoreCase("PatronUno")) {

			for (int i = 0; i < elementosAContenerTextoRepetidamentePatronUno.length; i++) {

				if (texto.contains(elementosAContenerTextoRepetidamentePatronUno[i]))  

					contador++;

			}

			resultado = (contador >= (elementosAContenerTextoRepetidamentePatronUno.length * porcentajePresicion/100));

		} else if (numeroPatron.equalsIgnoreCase("PatronDos")) {

			for (int i = 0; i < elementosAContenerTextoRepetidamentePatronDos.length; i++) {

				if (texto.contains(elementosAContenerTextoRepetidamentePatronDos[i]))  

					contador++;

			}

			resultado = (contador >= (elementosAContenerTextoRepetidamentePatronDos.length * porcentajePresicion/100));

		} else if (numeroPatron.equalsIgnoreCase("PatronTres")) {

			for (int i = 0; i < elementosAContenerTextoRepetidamentePatronTres.length; i++) {

				if (texto.contains(elementosAContenerTextoRepetidamentePatronTres[i]))  

					contador++;

			}

			resultado = (contador >= (elementosAContenerTextoRepetidamentePatronTres.length * porcentajePresicion/100));

		}




		return resultado;

	}


	public boolean isPatronTransunionSinAccount(String texto, String numeroPatron) {

		int contador = 0;

		boolean resultado= false ;

		if (numeroPatron.equalsIgnoreCase("PatronUno")) {

			for (int i = 0; i < elementosAContenerTextoRepetidamentePatronUnoSinAccount.length; i++) {

				if (texto.contains(elementosAContenerTextoRepetidamentePatronUnoSinAccount[i]))  

					contador++;

			}

			resultado = (contador >= (elementosAContenerTextoRepetidamentePatronUnoSinAccount.length * 0.5));

		}

		return resultado;

	}

	public String getPatronTransunion(String texto) {

		String resultado;

		this.texto = texto;

		textoPrimerasCincoLineas = texto;

		resultado = isPatronUno()  ? "PatronUno" :
			isPatronDos()  ? "PatronDos"  :
				isPatronTres() ? "PatronTres" :  "Ninguno";


		return resultado;

	}


	public String[] getElementosAContenerTextoRepetidamente() {
		return elementosAContenerTextoRepetidamentePatronUno;
	}



	public boolean isPatronUno() {

		String lineaUnoDebeEstarPresente = "your ssn has been masked for your protection";
		String lineaDosDebeEstarPresente = "date of birth";

		boolean resultado = textoPrimerasCincoLineas.contains(lineaUnoDebeEstarPresente.toLowerCase()) &&
				textoPrimerasCincoLineas.contains(lineaDosDebeEstarPresente.toLowerCase());

		if (!resultado) {
			
			lineaUnoDebeEstarPresente = "you have been on our files";
			
			resultado = textoPrimerasCincoLineas.contains(lineaUnoDebeEstarPresente.toLowerCase()) &&
					textoPrimerasCincoLineas.contains(lineaDosDebeEstarPresente.toLowerCase());
			
		}

		return resultado;
	}


	public boolean isPatronDos() {

		String lineaUnoDebeEstarPresente = "Credit Report Prepared For";
		String lineaDosDebeEstarPresente = "transunion Report As Of";

		boolean resultado = textoPrimerasCincoLineas.contains(lineaUnoDebeEstarPresente.toLowerCase()) &&
				textoPrimerasCincoLineas.contains(lineaDosDebeEstarPresente.toLowerCase());

		return resultado;

	}

	public boolean isPatronTres() {

		String lineaUnoDebeEstarPresente = "saldo";
		String lineaDosDebeEstarPresente = "fecha";
		String lineaTresDebeEstarPresente = "prestamo";
		String lineaCuatroDebeEstarPresente = "cuenta";
		String lineaCincoDebeEstarPresente = "responsabilidad";

		boolean resultado = textoPrimerasCincoLineas.contains(lineaUnoDebeEstarPresente.toLowerCase()) &&
				textoPrimerasCincoLineas.contains(lineaDosDebeEstarPresente.toLowerCase()) &&
				textoPrimerasCincoLineas.contains(lineaTresDebeEstarPresente.toLowerCase()) &&
				textoPrimerasCincoLineas.contains(lineaCuatroDebeEstarPresente.toLowerCase()) &&
				textoPrimerasCincoLineas.contains(lineaCincoDebeEstarPresente.toLowerCase());

		return resultado;

	}

	public String getTagEmployee() {
		return tagEmployee;
	}

	public String getTagName() {
		return tagName;
	}

	public String getCra() {
		return cra;
	}

	public String getTagReferenciaObtenerEmployee() {
		return tagReferenciaObtenerEmployee;
	}

	public String getTagPrimerTag() {
		return tagPrimerTag;
	}

	public String getTagAddress() {
		return tagAddress;
	}

	public void setTexto(String texto) {
		this.texto = texto;
	}

	private String getLineas(int numeroLineas) {

		StringBuffer lineas = new StringBuffer();
		int posicionFila = 0;
		int posicionAnteriorFila = 0;

		for (int i = 0; i < numeroLineas; i++) {

			posicionFila = texto.indexOf("\n", posicionFila) + 1;
			lineas.append(texto.substring(posicionAnteriorFila,posicionFila));
			posicionAnteriorFila = posicionFila;

		}

		return lineas.toString();
	}

	public String getTagNamePatronTres() {
		return tagNamePatronTres;
	}


	public String[] getElementosAdicionalesPatronGenerico() {
		return elementosAdicionalesPatronGenerico;
	}

	public String getTagAddressPatronTres() {
		return tagAddressPatronTres;
	}


}
