/**
 * @Clase: PatronEquifax.java
 * 
 * @version  1.0
 * 
 * @since 6/07/2020
 * 
 * @autor Ing. Jhon Mendez
 *
 * @Copyrigth: Legal Credit Solution
 */

package com.legalCredit.scraper.patrones;

/**
 *  Esta clase leera los reportes de tipo equifax. 
 *  
 *  Para que perteneca a este patron debe contene la palabra equifax mas los componentes 
 *  que se describen en la variable elementosAContenerTextoRepetidamente
 */

public class PatronEquifax {

	private String[] elementosAContenerTextoRepetidamentePatronUno = {"account name","account #","account type","date opened",
			"account status","payment status","status updated","balance",
			"balance updated","credit limit","monthly payment","past due amount",
			"highest balance","terms","responsibility","your statement",
			"comments","original creditor","credit usage"};


	private String[] elementosAContenerTextoRepetidamentePatronDos = {"account number","account type","date opened","account status",
																	 "high balance","credit limit","terms frequency","balance",
																	 "amount past due",	"creditor classification","loan type","comments",
																	 "date closed","date of last activity","actual payment amount","scheduled payment amount"};

	private String[] elementosAContenerTextoRepetidamentePatronTres = {"account number","high credit","date opened","items as of","credit limit","terms duration"};

	private String[] elementosAdicionalesPatronGenerico = {"open credit cards","open retail cards","open real estate loans","open installment loans",
			"total open accounts","accounts ever late","collections accounts","average account age",
			"oldest account","newest account","credit debt","total credit",
			"credit and retail card debt","real estate debt","installment loans debt","collections debt",
			"total debt","my hard credit inquiries","my public records","credit score",
			"date of report","birth year","overall credit usage"};
	
	private String[] elementosInquiryAccount = {"inquiry date","removal date","business type","contact information"};

	private String[] elementosPublicRecorsAccount = {"filing date","amount","reference number","court","plaintiff"};

	private String tagEmployee = "employer(s)";

	private String tagName = "name";

	private String tagPrimerTag = "account name";
	
	private String tagSSN = "social security number";

	private String tagUltimoTag = "account details";

	private String tagAddress = "addresses";

	private String tagInquiries = "inquiries";

	private String tagPublicRecords = "public records";

	private String tagCollections = "collections";

	private String tagCreditScore = "credit score";

	private String tagEmployeePatronDos = "employment history";

	private String cra = "equifax";

	private String tagAccountReportDate = "report date";

	private String tagBirthReportDate = "age or date of birth";

	private String tagAccountNamePatronDos = "name";
	
	private String tagAccountNamePatronTres = "name on file";

	private String tagAcountEmployerPatronTres = "previous employment";
	
	private String tagAddressPatronTres = "current address";
	
	private String texto;
	
	private String textoConFormatoEspacios;

	private String textoPrimerasLineas;

	private int porcentajePresicion = 60; //Esta variable define el porcentaje de elementos que deben estar presentes para
	//ser de tipo Equifax


	public boolean isPatronEquifax(String texto, String numeroPatron) {

		int contador = 0;
		
		boolean resultado = false;

		if (numeroPatron.equalsIgnoreCase("PatronUno"))  {

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

	public String getPatronEquifax(String texto) {

		String resultado;

		this.texto = texto;

		if (texto.split("\n").length >=250)
		
			textoPrimerasLineas = getLineas(250);
		
		else
			
			textoPrimerasLineas = texto;

		resultado = isPatronUno() ?  "PatronUno"  : 
			        isPatronDos() ?  "PatronDos" :
				    isPatronTres() ? "PatronTres" :
				    "Ninguno";


		return resultado;

	}


	public boolean isPatronUno() {

		String lineaUnoDebeEstarPresente = "Credit Report Prepared For";
		String lineaDosDebeEstarPresente = "equifax Report As Of";

		boolean resultado = textoPrimerasLineas.contains(lineaUnoDebeEstarPresente.toLowerCase()) &&
				textoPrimerasLineas.contains(lineaDosDebeEstarPresente.toLowerCase());


		return resultado;

	}

	public boolean isPatronDos() {

		String lineaUnoDebeEstarPresente = "CREDIT REPORT";
		String lineaDosDebeEstarPresente = "Report Confirmation";
		String lineaTresDebeEstarPresente = "Summary";

		boolean resultado = textoPrimerasLineas.contains(lineaUnoDebeEstarPresente.toLowerCase()) &&
				textoPrimerasLineas.contains(lineaDosDebeEstarPresente.toLowerCase()) &&
				textoPrimerasLineas.contains(lineaTresDebeEstarPresente.toLowerCase());


		return resultado;

	}
	
	public boolean isPatronTres() {
		
		//Teniendo en cuenta por OCR, se realiza por porcentaje de exactitud
		String[] lineaUnoDebeEstarPresente = {"security",
											"credit account",
											"credit file"};
		
		textoConFormatoEspacios = texto.replaceAll("( ){2,}", " ");
		
		//Se verifica cuantas filas Se contienen
		
		int contador = 0;
		
		for (String linea : lineaUnoDebeEstarPresente) 
		
			if (textoConFormatoEspacios.contains(linea))
				contador++;
		
		return (contador >= (lineaUnoDebeEstarPresente.length * porcentajePresicion/100));
		
	}


	public String getCra() {
		return cra;
	}

	public String getTagEmployee() {
		return tagEmployee;
	}


	public String getTagName() {
		return tagName;
	}


	public String getTagPrimerTag() {
		return tagPrimerTag;
	}


	public String getTagUltimoTag() {
		return tagUltimoTag;
	}

	public String[] getElementosAdicionalesPatronGenerico() {
		return elementosAdicionalesPatronGenerico;
	}

	public String[] getElementosAContenerTextoRepetidamentePatronUno() {
		return elementosAContenerTextoRepetidamentePatronUno;
	}

	public String getTagAddress() {
		return tagAddress;
	}

	public String getTagInquiries() {
		return tagInquiries;
	}

	public String[] getElementosInquiryAccount() {
		return elementosInquiryAccount;
	}

	public String[] getElementosPublicRecorsAccount() {
		return elementosPublicRecorsAccount;
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

	public String getTagPublicRecords() {
		return tagPublicRecords;
	}

	public String getTagCollections() {
		return tagCollections;
	}

	public String getTagCreditScore() {
		return tagCreditScore;
	}

	public String getTagAccountReportDate() {
		return tagAccountReportDate;
	}

	public String getTagAccountNamePatronDos() {
		return tagAccountNamePatronDos;
	}

	public String getTagBirthReportDate() {
		return tagBirthReportDate;
	}
	
	public String[] getElementosAContenerTextoRepetidamentePatronDos() {
		return elementosAContenerTextoRepetidamentePatronDos;
	}

	public String getTagEmployeePatronDos() {
		return tagEmployeePatronDos;
	}
	
	public String getTagSSN() {
		return tagSSN;
	}

    public String getTagAccountNamePatronTres() {
		return tagAccountNamePatronTres;
	}

    public String getTagAcountEmployerPatronTres() {
		return tagAcountEmployerPatronTres;
	}
    
    public String getTagAddressPatronTres() {
		return tagAddressPatronTres;
	}

}
