/**
 * @Clase: Utilidades.java
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.awt.Rectangle;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripperByArea;


/**
 * 
 * Se definen funciones comunes que pueden ser utilizadas en todo el proyecto
 * 
 */

public class Utilidades {

	private static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm");
	private static Pattern patronNumeroEntero;
	private static Pattern patronNumeroReal;
	private String[] meses = {"jan","feb","mar","apr","may","jun","jul","aug","sep","oct","nov","dec"};


	/**
	 * Cuenta el numero de ocurrencias de una palabra
	 * @param texto
	 * @param palabra
	 * @return
	 */
	public int getNumeroOcurrenciasPalabra(String texto,String palabra) {

		int contador = 0;

		while (texto.indexOf(palabra) > -1) {

			texto = texto.substring(texto.indexOf(
					palabra)+palabra.length(),
					texto.length());

			contador++; 
		}

		return contador;

	}

	public String getDatoVertical(String textoMinuscula,String tag) {

		String resultado = "";

		int posicionInicial = textoMinuscula.indexOf(tag);
		int posicionFinal = textoMinuscula.indexOf("\n",posicionInicial);
		posicionFinal  = textoMinuscula.indexOf("\n", posicionFinal+2);

		if (posicionInicial>= 0 && posicionFinal > posicionInicial)
			resultado = textoMinuscula.substring(posicionInicial,posicionFinal).
			replaceAll(tag + "|\r|:"," ").replaceAll("\n","").trim();


		return resultado;

	}

	/**
	 * Metodo que obtiene el tag que se envia por parametro
	 * @param texto
	 * @param textoMinuscula
	 * @param tag
	 * @return
	 */
	public String getDatoHorizontal(String textoMinuscula,String tag) {

		String dato = "";
		int posicionInicial = textoMinuscula.indexOf(tag);
		int posicionFinal  = textoMinuscula.indexOf("\n",posicionInicial);

		if (posicionFinal < 0)
			posicionFinal  = textoMinuscula.indexOf("\r",posicionInicial);

		if (posicionInicial >= 0 && posicionFinal > posicionInicial)
			dato = textoMinuscula.substring(posicionInicial,posicionFinal).replaceFirst("!", "").replaceFirst("-", "").replaceFirst(",", "").
			replaceAll(tag + "|\r|:|$|\\.|;"," ").trim();


		return dato;

	}

	public String getDatoHorizontal(String textoMinuscula,String tag,String tag2) {

		String dato = "";
		int posicionInicial = textoMinuscula.indexOf(tag,textoMinuscula.indexOf(tag2));
		int posicionFinal  = textoMinuscula.indexOf("\n",posicionInicial);

		if (posicionFinal < 0)
			posicionFinal  = textoMinuscula.indexOf("\r",posicionInicial);

		if (posicionInicial >= 0 && posicionFinal > posicionInicial)
			dato = textoMinuscula.substring(posicionInicial,posicionFinal).
			replaceAll(tag, "").replaceFirst(",", "").replaceAll(":", "").
			replaceAll("$","").trim();


		return dato;

	}

	public String getLineaAnterior(String textoMinuscula,String tag) {

		String resultado = "";

		int posicionFinalCreditInquieres = textoMinuscula.indexOf(tag);
		int posicionInicialCreditInquieres = textoMinuscula.lastIndexOf("\n",posicionFinalCreditInquieres-4);
		posicionInicialCreditInquieres = textoMinuscula.lastIndexOf("\n",posicionInicialCreditInquieres);

		if  (posicionInicialCreditInquieres >=0 && posicionInicialCreditInquieres < posicionFinalCreditInquieres) 

			resultado = textoMinuscula.substring(posicionInicialCreditInquieres, posicionFinalCreditInquieres).
			replaceAll(tag, "").replaceAll("\r|:|\n"," ").trim();

		return resultado;

	}

	public String getColumnaPDF(PDDocument document, int posicionInicial, int posicionFinal) throws IOException {

		StringBuffer columna = new StringBuffer();

		for (int i = 0; i < document.getNumberOfPages(); i++) {

			columna.append(getTextoArea(document,i, posicionInicial, 0, posicionFinal , 8200));
		}


		return columna.toString();

	}

	public String getColumnaPDFPrimeraHoja(PDDocument document, int posicionInicial, int posicionFinal) throws IOException {

		StringBuffer columna = new StringBuffer();

		columna.append(getTextoArea(document,0, posicionInicial, 0, posicionFinal , 8200));

		return columna.toString().toLowerCase();

	}


	public String getFechaFormatoMesDiaAño(String texto) {

		return getFechaFormatoMesDiaAño(texto, " ",false);

	}

	public String getFechaFormatoMesDiaAño(String texto,String caracterSeparacion, boolean numeroMesPresente) {

		String resultado = "";
		String numeroMes = "";
		String año =  "";
		String dia = "";

		String[] textoFecha = texto.replaceAll(",", "").split(caracterSeparacion);

		if (textoFecha.length == 1) {

			resultado = textoFecha[0];

		} else if (textoFecha.length == 2) {

			dia = "01";
			numeroMes = numeroMesPresente ? (textoFecha[0].length() == 1 ? "0" + textoFecha[0] : textoFecha[0]) : getNumeroMes(textoFecha[0].substring(0,3));
			año = textoFecha[1];

			resultado = numeroMes + "/" + (dia.length() == 1 ? "0" + dia : dia) + "/" + año;

		} else if (textoFecha.length == 3) {

			dia = textoFecha[1];
			numeroMes = numeroMesPresente ?  (textoFecha[0].length() == 1 ? "0" + textoFecha[0] : textoFecha[0]) : getNumeroMes(textoFecha[0].substring(0,3));
			año = textoFecha[2];
			resultado = numeroMes + "/" + (dia.length() == 1 ? "0" + dia : dia) + "/" + año;

		}



		return resultado;


	}

	/**
	 * Recupera el texto de un area de un pdf
	 * @param document
	 * @param numeroPagina
	 * @param x
	 * @param y
	 * @param ancho
	 * @param largo
	 * @return
	 * @throws IOException 
	 */
	public String getTextoArea(PDDocument document,int numeroPagina, int x, int y, int ancho, int largo) throws IOException {

		String textoArea = "";

		PDFTextStripperByArea stripper = new PDFTextStripperByArea();
		stripper.setSortByPosition( true );
		Rectangle rect = new Rectangle(x,y,ancho,largo);
		stripper.addRegion( "rectangulo", rect );
		PDPage pagina = document.getPage(numeroPagina);

		stripper.extractRegions( pagina );

		textoArea = stripper.getTextForRegion( "rectangulo" ) ;



		return textoArea;

	}

	/**
	 * Se obtiene la fecha actual del sistema
	 * 
	 * @return Fecha
	 */
	public static String getFechaActual() {

		LocalDateTime now = LocalDateTime.now();
		return dtf.format(now);

	}

	public String eliminarRetornosCarro(String texto) {

		return texto.replaceAll("\n|\r", " ").trim();

	}

	public String getCadenaRecortada(String textoMinuscula,String tag) {

		if (textoMinuscula.contains(tag))

			textoMinuscula = textoMinuscula.substring(0,textoMinuscula.indexOf(tag));

		return textoMinuscula;
	}

	public String getDatoVertical(String textoMinuscula,String tag, int numeroFilas) {

		StringBuffer lineas = new StringBuffer();
		int posicionInicial = textoMinuscula.indexOf(tag);
		int posicionFinal = 0;

		for (int i = 0; i < numeroFilas; i++) {

			posicionFinal = textoMinuscula.indexOf("\n", posicionInicial) + 1;

			if (posicionInicial >=0 && posicionInicial < posicionFinal)
				lineas.append(textoMinuscula.substring(posicionInicial,posicionFinal));

			posicionInicial = posicionFinal;

		}

		return lineas.toString().replaceAll(tag, "").replaceAll(":", "").replaceAll("\r\n", " ").trim();

	}

	public String getDatoVertical(String textoMinuscula,String tag,String tag2) {

		String resultado = "";

		int posicionInicial = textoMinuscula.indexOf(tag,textoMinuscula.indexOf(tag2));
		int posicionFinal = textoMinuscula.indexOf("\n",posicionInicial);
		posicionFinal  = textoMinuscula.indexOf("\n", posicionFinal+2);

		if (posicionInicial>= 0 && posicionFinal > posicionInicial)
			resultado = textoMinuscula.substring(posicionInicial,posicionFinal).
			replace(tag, "").replaceAll("\r|:|\n"," ").trim();


		return resultado;

	}

	public String getLineaAnterior(String textoMinuscula,String tag,int posicion) {

		String resultado = "";

		int posicionFinal = textoMinuscula.indexOf(tag,posicion);

		int posicionInicial = textoMinuscula.lastIndexOf("\n",posicionFinal-4) - 2;
		posicionInicial = textoMinuscula.lastIndexOf("\n",posicionInicial);

		if  (posicionInicial >=0 && posicionInicial < posicionFinal) 

			resultado = textoMinuscula.substring(posicionInicial, posicionFinal).
			replace(tag, "").replaceAll("\r"," ").replaceAll(":", " ").
			replaceAll("\n","").trim();

		return resultado;

	}

	public int getPrimerValorPositiva(int array[], int posicion, int posicionFinalText) {

		int resultado = posicionFinalText;
		int i = posicion + 1;

		boolean sw = false;

		while  (i < array.length && !sw) {

			if (array[i] >  0) {

				resultado = array[i];
				sw = true;

			}

			i++;

		}

		return resultado;

	}

	public String[] getMeses() {



		return meses;
	}

	public String getNumeroMes(String nombreMes) {

		int numeroMes = Arrays.asList(meses).indexOf(nombreMes) + 1;

		return numeroMes < 10 ? "0"+numeroMes : ""+numeroMes;
	}


	/**
	 * Obtiene las lineas de un texto
	 * @param texto
	 * @param numeroLineas
	 * @return
	 */
	public String getLineas(String texto,int numeroLineas) {

		StringBuffer lineas = new StringBuffer();
		int posicionInicial = 0;
		int posicionFinal = 0;

		for (int i = 0; i < numeroLineas; i++) {

			posicionFinal = texto.indexOf("\n",posicionInicial+1) + 1;

			if (posicionInicial >= 0 && posicionInicial<posicionFinal)
				lineas.append(texto.substring(posicionInicial, posicionFinal));

			posicionInicial = posicionFinal;

		}

		return lineas.toString();

	}

	public Pattern getPatronAno() {

		String regexAno ="\\d{4}";
		Pattern patronAno = Pattern.compile(regexAno);
		return patronAno;

	}

	public Pattern getPatronSSNReporte() {

		String regexSSNReporte = "(\\d|x){3}-(\\d|x){2}-(\\d|x){4}";

		Pattern patronSSNReporte = Pattern.compile(regexSSNReporte,Pattern.CASE_INSENSITIVE);

		return patronSSNReporte;
	}

	public Pattern getPatronMes() {

		String regexMes ="jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec";

		Pattern patronMes = Pattern.compile(regexMes,Pattern.CASE_INSENSITIVE);

		return patronMes;
	}

	public Pattern getPatronDateReportPatronTres() {

		String regexDateReportPatronTres = "[a-zA-z]{3}\\s+\\d{2},\\s+\\d{4}";

		Pattern patronDateReportPatronTres = Pattern.compile(regexDateReportPatronTres,Pattern.CASE_INSENSITIVE);

		return patronDateReportPatronTres;
	}

	public Pattern getPatronItemNumeral() {

		String regexItemNumeral ="\\d{1}[.]{1}\\d{1,2}";

		Pattern patronItemNumeral = Pattern.compile(regexItemNumeral);

		return patronItemNumeral;
	}

	public Pattern getPatronContenidoParentesis() {

		String regexContenidoParentesis = "\\([a-zA-z]+\\)";

		Pattern patronContenidoParentesis = Pattern.compile(regexContenidoParentesis);

		return patronContenidoParentesis;
	}

	public Pattern getPatronFormatoTablaPatronDos() {

		String regexFormatoTablaPatronDos = "\\s*year\\s+jan\\s+feb\\s+mar\\s+apr\\s+may\\s+jun\\s+jul\\s+aug\\s+sep\\s+oct\\s+nov\\s+dec";

		Pattern patronFormatoTablaPatronDos = Pattern.compile(regexFormatoTablaPatronDos,Pattern.CASE_INSENSITIVE);

		return patronFormatoTablaPatronDos;
	}

	public Pattern getPatronValorTabla() {

		String regexValorTabla ="\\${0,1}\\d+,*\\d+";

		Pattern patronValorTabla = Pattern.compile(regexValorTabla);

		return patronValorTabla;
	}

	public Pattern getPatronDateInquiresPatrosDos() {

		String regexDateInquiresPatrosDos = "[a-z]{3}\\s+\\d{1,2}\\s*,\\s*\\d{4}";

		Pattern patronDateInquiresPatrosDos = Pattern.compile(regexDateInquiresPatrosDos);

		return patronDateInquiresPatrosDos;
	}

	public Pattern getPatronNumeroCuentaReporteExperian() {

		String regexNumeroCuentaReporteExperian = "#{0,1}(\\d)+(\\*)+|(\\d){3,}[.]{3,}";

		Pattern patronNumeroCuentaReporteExperian = Pattern.compile(regexNumeroCuentaReporteExperian,Pattern.CASE_INSENSITIVE);

		return patronNumeroCuentaReporteExperian;
	}

	public Pattern getPatronHardInquiries() {

		String regexHardInquiries = "hard\\s+inquiries";

		Pattern patronHardInquiries = Pattern.compile(regexHardInquiries,Pattern.CASE_INSENSITIVE);

		return patronHardInquiries;
	}

	public Pattern getPatronSoftInquiries() {

		String regexSoftInquiries = "soft\\s+inquiries";

		Pattern patronSoftInquiries = Pattern.compile(regexSoftInquiries,Pattern.CASE_INSENSITIVE);

		return patronSoftInquiries;
	}

	public Pattern getPatronDateInquiresOn() {

		String regexDateInquiresOn ="\\d{1,2}/\\d{1,2}/\\d{2,4}";

		Pattern patronDateInquiresOn = Pattern.compile(regexDateInquiresOn,Pattern.CASE_INSENSITIVE);

		return patronDateInquiresOn;
	}

	public Pattern getPatronNumeroCuentaReporte() {

		String regexNumeroCuentaReporte = "#[a-z]*(\\d)+[a-z]*(\\d)+(\\*)+|#{0,1}(\\d)+[a-z]*(\\*)+|(\\d){3,}[.]{3,}";

		Pattern patronNumeroCuentaReporte = Pattern.compile(regexNumeroCuentaReporte,Pattern.CASE_INSENSITIVE);

		return patronNumeroCuentaReporte;
	}

	public Pattern getPatronFechaFormatoReporte() {

		String regexFechaFormatoReporte = "(\\d{2}/\\d{4}\\s+){2,}";

		Pattern patronFechaFormatoReporte = Pattern.compile(regexFechaFormatoReporte);

		return patronFechaFormatoReporte;
	}


	public static int getNumeroEntero(String texto) {

		if (patronNumeroEntero == null) {

			String regexPatronNumeroEntero = "\\d+";

			patronNumeroEntero = Pattern.compile(regexPatronNumeroEntero);

		}



		Matcher matcher = patronNumeroEntero.matcher(texto.replaceAll(",|<|>", ""));
		int numero = matcher.find() ? Integer.parseInt(matcher.group()) : 0;

		return numero;
	}

	public static double getNumeroReal(String texto) {

		if (patronNumeroReal == null) {

			String regexPatronNumeroReal = "\\d+.{0,1}\\d*";

			patronNumeroReal = Pattern.compile(regexPatronNumeroReal);

		}

		Matcher matcher = patronNumeroReal.matcher(texto.replaceAll(",|>|<", ""));
		double numero = matcher.find() ? Double.parseDouble(matcher.group()) : 0;


		return numero;
	}



}
