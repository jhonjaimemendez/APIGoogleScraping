/**
 * @Clase: PatronExperian.java
 * 
 * @version  1.0
 * 
 * @since 14/07/2020
 * 
 * @autor Ing. Jhon Mendez
 *
 * @Copyrigth: Legal Credit Solution
 */

package com.legalCredit.scraper.patrones;

/**
 * Esta clase leera los reportes de tipo Experian 
 *  
 *  Para que perteneca a este patron debe contene la palabra experian mas los componentes 
 *  que se describen en la variable elementosAContenerTextoRepetidamente
 *  
 */

public class PatronExperian {

	private String[] elementosAContenerTextoRepetidamentePatronUno = {"account name","account number","recent balance","date opened",
			"status","balance","type","credit limit or","date of status",
			"high balance","terms","responsibility","comments",
			"monthly payment","recent payment"};
	
	private String[] elementosAContenerTextoRepetidamentePatronTres = {"date opened","type","credit limit","recent balance",
			"responsibility","terms","status","high balance","partial account number","payment","comment","monthly",
			"original creditor"};

	private String[] elementosAContenerTextoRepetidamentePatronCuatro = {"account name","account #","account type","date opened",
			"account status","payment status","status updated","balance",
			"balance updated","credit limit","monthly payment","past due amount",
			"highest balance","terms","responsibility","your statement",
			"comments","original creditor"};

	private String[] elementosAContenerTextoRepetidamentePatronCinco = {"account name","account number","account type","date opened",
			"status","balance","credit limit","status updated",
			"high balance","terms","responsibility","recent payment",
			"balance updated","monthly payment"};
	
	private String[] elementosAContenerTextoRepetidamentePatronSeis = {"date opened","first reported","recent balance","payment history",
							"terms","status","responsibility","credit limit","monthly payment","high balance"};

	private String[] elementosAContenerTextoRepetidamentePatronDosSinAccount = {"inquiries","address","date of request"};
	
	private String[] elementosAContenerTextoRepetidamentePatronCincoSinAccount = {"inquired on","hard inquiries","soft inquiries"};


	private String tagEmployee = "employer name";

	private String tagReferenciaObtenerEmployee = "position";

	private String tagPrimerTagPatronUno = "account name";
	
	private String tagNamePAtronTres = "prepared for";

	private String tagNamePatronUno = "highlighted below.";

	private String cra = "Experian";

	private String tagAccountName = "account name";
	
	private String tagAddress = "address";
	
	private String tagInquiredOn = "inquired on";
	
	private String tagAddressPatronCinco = "addresses";

	private String tagAccountDateReport = "date of request(s)";
	
	private String tagAccountDateReportPatronDos = "date of request";
	
	private String tagAccountDateReportPatronTres = "date";

	private String texto;

	private String textoPrimerasLineas;
	
	private String textoConFormatoEspacios;

	private int porcentajePrecision = 80; //Esta variable define el porcentaje de elementos que deben estar presentes para
	//ser de tipo Equifax

	public boolean isPatronExperianAccount(String texto, String numeroPatron) {

		int contador = 0;
		
		boolean resultado= false ;

		if (numeroPatron.equalsIgnoreCase("PatronUno") || numeroPatron.equalsIgnoreCase("PatronDos")) {

			for (int i = 0; i < elementosAContenerTextoRepetidamentePatronUno.length; i++) {

				if (texto.contains(elementosAContenerTextoRepetidamentePatronUno[i]))  

					contador++;

			}
			
			resultado = (contador >= (elementosAContenerTextoRepetidamentePatronUno.length * porcentajePrecision/100));

		} else if (numeroPatron.equalsIgnoreCase("PatronTres")) {

			for (int i = 0; i < elementosAContenerTextoRepetidamentePatronTres.length; i++) {

				if (texto.contains(elementosAContenerTextoRepetidamentePatronTres[i]))  

					contador++;

			}
			
			resultado = (contador >= (elementosAContenerTextoRepetidamentePatronTres.length * porcentajePrecision/100));
			
	} else if (numeroPatron.equalsIgnoreCase("PatronCuatro")) {

			for (int i = 0; i < elementosAContenerTextoRepetidamentePatronCuatro.length; i++) {

				if (texto.contains(elementosAContenerTextoRepetidamentePatronCuatro[i]))  

					contador++;

			}
			
			resultado = (contador >= (elementosAContenerTextoRepetidamentePatronCuatro.length * porcentajePrecision/100));
			
		} else if (numeroPatron.equalsIgnoreCase("PatronCinco") ) {

			for (int i = 0; i < elementosAContenerTextoRepetidamentePatronCinco.length; i++) {

				if (texto.contains(elementosAContenerTextoRepetidamentePatronCinco[i]))  

					contador++;

			}
			
			resultado = (contador >= (elementosAContenerTextoRepetidamentePatronCinco.length * porcentajePrecision/100));

		} else if (numeroPatron.equalsIgnoreCase("PatronSeis") ) {

			for (int i = 0; i < elementosAContenerTextoRepetidamentePatronSeis.length; i++) {

				if (texto.contains(elementosAContenerTextoRepetidamentePatronSeis[i]))  

					contador++;

			}
			
			resultado = (contador >= (elementosAContenerTextoRepetidamentePatronSeis.length * porcentajePrecision/100));

		}

	

		return resultado;

	}

	public boolean isPatronExperianSinAccount(String texto, String numeroPatron) {
		
		int contador = 0;
		
		boolean resultado= false ;

		if (numeroPatron.equalsIgnoreCase("PatronDos")) {

			for (int i = 0; i < elementosAContenerTextoRepetidamentePatronDosSinAccount.length; i++) {

				if (texto.contains(elementosAContenerTextoRepetidamentePatronDosSinAccount[i]))  

					contador++;

			}
			
			resultado = (contador >= (elementosAContenerTextoRepetidamentePatronDosSinAccount.length * porcentajePrecision/100));

		} else if (numeroPatron.equalsIgnoreCase("PatronCinco")) {

			for (int i = 0; i < elementosAContenerTextoRepetidamentePatronCincoSinAccount.length; i++) {

				if (texto.contains(elementosAContenerTextoRepetidamentePatronCincoSinAccount[i]))  

					contador++;

			}
			
			resultado = (contador >= (elementosAContenerTextoRepetidamentePatronCincoSinAccount.length * porcentajePrecision/100));

		} 
		
		return resultado;
		
	}

	public String getPatronExperian(String texto) {

		String resultado;

		this.texto = texto;

		textoPrimerasLineas = getLineas(200);

		resultado = isPatronUno() ? "PatronUno" :
			        isPatronDos() ? "PatronDos" :
				    isPatronTres() ? "PatronTres" :
					isPatronCuatro() ? "PatronCuatro" :
					isPatronCinco() ? "PatronCinco" : 
				    isPatronSeis() ? "PatronSeis" : "Ninguno";
		
		return resultado;

	}


	public boolean isPatronUno() {

		String lineaUnoDebeEstarPresente = "Experian - Access your credit report";
		String lineaDosDebeEstarPresente = " Any pending disputes will be highlighted below";

		boolean resultado = textoPrimerasLineas.contains(lineaUnoDebeEstarPresente.toLowerCase()) &&
				textoPrimerasLineas.contains(lineaDosDebeEstarPresente.toLowerCase());


		return resultado;
	}


	public boolean isPatronDos() {

		String lineaUnoDebeEstarPresente = "Experian credit report prepared for";
		String lineaDosDebeEstarPresente = "Report date";
		String lineaTresDebeEstarPresente = "Your report number is";

		boolean resultado = textoPrimerasLineas.contains(lineaUnoDebeEstarPresente.toLowerCase()) &&
				textoPrimerasLineas.contains(lineaDosDebeEstarPresente.toLowerCase()) &&
				textoPrimerasLineas.contains(lineaTresDebeEstarPresente.toLowerCase());

		return resultado;

	}


	public boolean isPatronTres() {

		String lineaUnoDebeEstarPresente = "credit report";
		String lineaDosDebeEstarPresente = "dispute results";
		String lineaTresDebeEstarPresente = "credit report";

		boolean resultado = textoPrimerasLineas.contains(lineaUnoDebeEstarPresente.toLowerCase()) &&
				textoPrimerasLineas.contains(lineaDosDebeEstarPresente.toLowerCase()) &&
				textoPrimerasLineas.contains(lineaTresDebeEstarPresente.toLowerCase());


		return resultado;

	}


	public boolean isPatronCuatro() {

		String lineaUnoDebeEstarPresente = "Credit Report Prepared For";
		String lineaDosDebeEstarPresente = "Experian Report As Of";

		boolean resultado = textoPrimerasLineas.contains(lineaUnoDebeEstarPresente.toLowerCase()) &&
				textoPrimerasLineas.contains(lineaDosDebeEstarPresente.toLowerCase());


		return resultado;

	}


	public boolean isPatronCinco() {

		String lineaUnoDebeEstarPresente = "date generated";
		String lineaDosDebeEstarPresente = "report number";
		String lineaTresDebeEstarPresente = "personal information";


		boolean resultado = textoPrimerasLineas.contains(lineaUnoDebeEstarPresente.toLowerCase()) &&
				textoPrimerasLineas.contains(lineaDosDebeEstarPresente.toLowerCase()) && 
				textoPrimerasLineas.contains(lineaTresDebeEstarPresente.toLowerCase());


		return resultado;

	}
	
	public boolean isPatronSeis() {
		
		String[] lineaUnoDebeEstarPresente = {"before dispute","report #","after dispute","dispute results"};
		

		textoConFormatoEspacios = texto.replaceAll("( ){2,}", " ");
		
		//Se verifica cuantas filas Se contienen
		
		int contador = 0;
		
		for (String linea : lineaUnoDebeEstarPresente) 
		
			if (textoConFormatoEspacios.contains(linea))
				contador++;
		
		return (contador >= (lineaUnoDebeEstarPresente.length * porcentajePrecision/100));
		
	}

	public String[] getElementosAContenerTextoRepetidamentePatronUno() {
		return elementosAContenerTextoRepetidamentePatronUno;
	}

	public String[] getElementosAContenerTextoRepetidamentePatronCinco() {
		return elementosAContenerTextoRepetidamentePatronCinco;
	}


	public String getTagEmployee() {
		return tagEmployee;
	}

	public String getTagNamePatronUno() {
		return tagNamePatronUno;
	}

	public String getCra() {
		return cra;
	}

	public String getTagReferenciaObtenerEmployee() {
		return tagReferenciaObtenerEmployee;
	}

	public String getTagPrimerTagPatronUno() {
		return tagPrimerTagPatronUno;
	}

	public void setTexto(String texto) {

		this.texto = texto.replaceAll("( )+", " ");
	}

	public String getLineas(int numeroLineas) {

		StringBuffer lineas = new StringBuffer();
		int posicionInicial = 0;
		int posicionFinal = 0;

		for (int i = 0; i < numeroLineas; i++) {

			posicionFinal = texto.indexOf("\n",posicionInicial) + 1;
			lineas.append(texto.substring(posicionInicial, posicionFinal));
			posicionInicial = posicionFinal;

		}

		return lineas.toString();

	}

	public String getTagAccountName() {
		return tagAccountName;
	}

	public String getTagAccountDateReport() {
		return tagAccountDateReport;
	}
	
	public String getTagAddress() {
		return tagAddress;
	}
	
	public String getTagAccountDateReportPatronDos() {
		return tagAccountDateReportPatronDos;
	}
	
	public String getTagAddressPatronCinco() {
		return tagAddressPatronCinco;
	}
	
	public String getTagInquiredOn() {
		return tagInquiredOn;
	}
	
	public String getTagAccountDateReportPatronTres() {
		return tagAccountDateReportPatronTres;
	}
	
	public String getTagNamePatronTres() {
		return tagNamePAtronTres;
	}
	
	public String[] getElementosAContenerTextoRepetidamentePatronTres() {
		return elementosAContenerTextoRepetidamentePatronTres;
	}

}
