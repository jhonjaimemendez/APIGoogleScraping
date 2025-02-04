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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import org.apache.tika.langdetect.OptimaizeLangDetector;
import org.apache.tika.language.detect.LanguageDetector;
import org.apache.tika.language.detect.LanguageResult;
import org.json.JSONObject;

import net.sourceforge.tess4j.ITesseract.RenderedFormat;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.tess4j.util.PdfUtilities;




/**
 * 
 * Se definen funciones comunes que pueden ser utilizadas en todo el proyecto
 * 
 */

public class Utilidades {

	private static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm");
	private static Pattern patronNumeroEntero;
	private static Pattern patronNumeroReal;
	private SimpleDateFormat formato;

	private String[] meses = {"jan","feb","mar","apr","may","jun","jul","aug","sep","oct","nov","dec"};
	private String rutaTemporal = "recursos/archivos/temp/";
	private String regexFecha = "\\d{2}/\\d{2}/\\d{4}";
	

	private int numeroMaximaRotaciones;
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
			replaceAll(tag + "|\r|:|$|;|\\.|:|!"," ").trim();


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

	public String getLineasAnterior(String textoMinuscula,String tag,int numeroLineas) {


		String resultado = "";

		int posicionFinalComodin = textoMinuscula.indexOf(tag);
		int posicionInicial = 0;
		int posicionFinal = 0;

		int indice = 0;

		while (indice < numeroLineas) {

			posicionFinal = posicionFinalComodin;

			posicionInicial = textoMinuscula.lastIndexOf("\n",posicionFinalComodin-4) - 2;
			posicionInicial = textoMinuscula.lastIndexOf("\n",posicionInicial);


			posicionFinalComodin = posicionInicial;

			indice++;
		}


		if  (posicionInicial >=0 && posicionInicial < posicionFinal) 

			resultado = textoMinuscula.substring(posicionInicial, posicionFinal)
			.replaceAll("\r"," ").replaceAll(":", " ").
			replaceAll("\n","").trim();

		return resultado;

	}

	public String getLineasAnteriorCompletas(String textoMinuscula,String tag,int numeroLineas) {


		String resultado = "";

		int posicionFinalComodin = textoMinuscula.indexOf(tag);
		int posicionInicial = 0;
		int posicionFinal = posicionFinalComodin;

		int indice = 0;

		while (indice < numeroLineas) {

			posicionInicial = textoMinuscula.lastIndexOf("\n",posicionFinal-4) - 2;
			posicionInicial = textoMinuscula.lastIndexOf("\n",posicionInicial);

			posicionFinal = posicionInicial;

			indice++;
		}


		if  (posicionInicial >=0) 

			resultado = textoMinuscula.substring(posicionInicial,  textoMinuscula.indexOf(tag))
			.replaceAll("\r"," ").replaceAll(":", " ").
			replaceAll("\n","").trim();

		return resultado;

	}

	public String getTextoEntreTag(String texto,String tag1, String tag2) {

		String resultado = "";

		int posicionInicial = texto.indexOf(tag1);
		int posicionFinal = texto.indexOf(tag2,posicionInicial);


		if (posicionInicial > 0 && posicionInicial < posicionFinal) 
			resultado = eliminarFilasVacias(texto.substring(posicionInicial, posicionFinal).replaceAll(tag1, ""));

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

	public String eliminarMasDeDosEspaciosEnTexto(String texto) {

		return texto.replaceAll("( ){2,}", " ");
	}

	public String getFechaFormatoMesDiaAño(String texto) {

		return getFechaFormatoMesDiaAño(texto, " ",false);

	}

	public String getFechaFormatoMesDiaAño(String texto,String caracterSeparacion, boolean numeroMesPresente) {

		String resultado = "";
		String numeroMes = "";
		String año =  "";
		String dia = "";

		String[] textoFecha = texto.replaceAll("( )+", " ").replaceAll(",", "").split(caracterSeparacion);

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

	/**
	 * Permite convertir un String en fecha (Date).
	 * @param fecha Cadena de fecha dd/MM/yyyy
	 * @return Objeto Date
	 */
	public Date convertirFecha(String fecha) {

		if (formato == null)
			formato = new SimpleDateFormat("MM/dd/yyyy");

		Date fechaDate = null;

		try {
			fechaDate = formato.parse(fecha);
		} catch (ParseException ex) {
			ex.printStackTrace();
		}

		return fechaDate;
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

	public String eliminarFilasVacias(String texto) {

		String[] filas = texto.replace("\r","").split("\n");
		StringBuffer filasSalidas = new StringBuffer();

		for (String fila : filas) 

			if (!fila.replaceAll("\r|\n|( )", "").replaceAll(" ","").isBlank())
				filasSalidas.append(fila).append("\n");

		return filasSalidas.toString();

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

		String regexFormatoTablaPatronDos = "\\s*year\\s+jan\\s+feb\\s+mar\\s+apr";

		Pattern patronFormatoTablaPatronDos = Pattern.compile(regexFormatoTablaPatronDos,Pattern.CASE_INSENSITIVE);

		return patronFormatoTablaPatronDos;
	}

	public Pattern getPatronValorTabla() {

		String regexValorTabla ="\\${0,1}\\d+,*\\d+";

		Pattern patronValorTabla = Pattern.compile(regexValorTabla);

		return patronValorTabla;
	}

	public Pattern getPatronDateInquiresPatrosDos() {

		String regexDateInquiresPatrosDos = "[a-z]{3}\\s*\\d{1,2}\\s*,\\s*\\d{4}";

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

	public Pattern getPatronNumeroTelefono() {

		String regexNumeroTelefono = "\\d{3} \\d{3} \\d{4}";

		Pattern patronNumeroTelefono = Pattern.compile(regexNumeroTelefono,Pattern.CASE_INSENSITIVE);

		return patronNumeroTelefono;
	}

	public Pattern getPatronDateInquiresOn() {

		String regexDateInquiresOn ="\\d{1,2}/\\d{1,2}/\\d{2,4}";

		Pattern patronDateInquiresOn = Pattern.compile(regexDateInquiresOn,Pattern.CASE_INSENSITIVE);

		return patronDateInquiresOn;
	}
	
	public Pattern getPatronPhones() {

		String regexPhone = "\\(\\d{3}\\)\\s*\\d{3}-\\d{4}";

		Pattern patronPhones = Pattern.compile(regexPhone,Pattern.CASE_INSENSITIVE);

		return patronPhones;
	}


	public Pattern getPatronNumeroCuentaReporte() {

		String regexNumeroCuentaReporte = "#[a-z]*(\\d)+[a-z]*(\\d)+(\\*)+|#{0,1}(\\d)+[a-z]*(\\*)+|(\\d){3,}[.]{3,}";

		Pattern patronNumeroCuentaReporte = Pattern.compile(regexNumeroCuentaReporte,Pattern.CASE_INSENSITIVE);

		return patronNumeroCuentaReporte;
	}

	public Pattern getPatronFechaFormatoReporte() {

		String regexFechaFormatoReporte = "(\\d{2}/\\d{4}\\s*){1,}";

		Pattern patronFechaFormatoReporte = Pattern.compile(regexFechaFormatoReporte);

		return patronFechaFormatoReporte;
	}

	public Pattern getPatronFechaMMDDYYYYY() {

		String regexPatronFechaMMDDYYYYY = "\\d{2}/\\d{2}/\\d{4}";

		Pattern patronFechaMMDDYYYYY = Pattern.compile(regexPatronFechaMMDDYYYYY);

		return patronFechaMMDDYYYYY;
	}

	public Pattern getPatronNumeroCuenta() {

		String regexNumeroCuenta = "([a-z,A-z]*\\d{3,}[a-z,A-z]+\\d*)|(\\d{2,}-\\d{2,})|\\d{1,}|[x]{4,}";

		Pattern patronNumeroCuenta = Pattern.compile(regexNumeroCuenta);

		return patronNumeroCuenta;
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

		Matcher matcher = patronNumeroReal.matcher(texto.replaceAll(",|>|<", "").replace(" ","."));
		double numero = matcher.find() ? Double.parseDouble(matcher.group()) : 0;


		return numero;
	}

	public String[] getPalabrasCorregirEspañol() {

		String[] palabrasCorregir = {"responsabilidad","cuenta","actualizacion","tipo","informacion","fecha",
				"pagado","prestamo","alto","pago","estado","apertura","nombres","transunion",
				"adversas","satisfactorias","numero","domicilios","domicilio","recibido",
				"cierre","observaciones","saldo","excedente","credito","limite","conjunta"};

		return palabrasCorregir;
	}

	public String[] getPalabrasCorregirIngles() {

		String[] palabrasCorregir = {"address","date","inquiry","number","partial","recentbalance",
				"status","individual","account","identification","responsibility","limit",
				"opened","open","dept","payment","account","monthly","high","balance","year",
				"payment","information","first","toyota","equifax","transunion","experian","security",
				"credit","phone","original","reported","comment","terms","dispute","items",
				"history","identification","original","before","after","address id","address",
				"current","previous","dear","file","formerly","finalballoon","bankruptcies",
				"judgments","liens","inquiry","company","social","security","recent",
				"responsibility","status","amount"};

		return palabrasCorregir;

	}

	public String getValor(String[] array,int posicion) {

		return posicion < array.length ? array[posicion].replaceAll("\r", "").replaceAll("\n", " ") : "";		

	}

	public List<String> getTextoPorTag(String texto, String tag, int posicionInicialBusqueda) {

		int posicionInicial = posicionInicialBusqueda < 0 ? 0 : posicionInicialBusqueda;
		List<String> textos = new ArrayList<String>();

		do {

			posicionInicial = texto.indexOf(tag,posicionInicial);

			if (posicionInicial > 0) {

				int posicionFinal = texto.indexOf(tag,posicionInicial+10);

				if (posicionFinal < 0)

					posicionFinal = texto.length();

				textos.add(texto.substring(posicionInicial, posicionFinal));

				posicionInicial = posicionFinal;

			}

		} while (posicionInicial > 0);

		return textos;
	}

	public String eliminarRetornosCarro(String texto) {

		return texto.replaceAll("\n|\r", " ").trim();

	}

	public String eliminarURLsTexto(String texto) {

		String regex = "((http|https)://)(www.)?"
				+ "[a-zA-Z0-9@:%._\\+~#?&//=]"
				+ "{2,256}\\.[a-z]"
				+ "{2,6}\\b([-a-zA-Z0-9@:%"
				+ "._\\+~#?&//=]*)";

		Set<String> urls = new HashSet<String>();
		Matcher matcher = Pattern.compile(regex).matcher(texto);

		while (matcher.find()) 
			
			urls.add(matcher.group());
			
		for (String url : urls) 
			texto = texto.replaceAll(url.replaceAll("\\#", "\\\\\\#").
					                     replaceAll("\\?", "\\\\\\?").
					                     replaceAll("\\&", "\\\\\\&"), "");
		
		
		return texto;

	}

	
	
	//******************************************* Utilidades para el OCR ***********************************
	public String getFormatearTexto(String texto, String[] palabrasCorregir) {

		//Se remplanzan las tildes en caso de los texto en español
		texto = texto.toLowerCase().replaceAll("á", "a").replaceAll("é", "e").replaceAll("í", "i").
				replaceAll("ó", "o").replaceAll("ú", "u").replaceAll(":","").replaceAll("\\(","").replaceAll("\\)","").
				replaceAll("\\*","_").replaceAll("\\{","").replaceAll("\\[", "").
				replaceAll("__", "  ").replaceAll("\\\\", "").replaceAll("[|]", "").
				replaceAll("trans union", "transunion").
				replaceAll("expenan","experian");


		//Se corrigen las palabras que se leen mal por el OCR
		StringTokenizer tokenizer = new StringTokenizer(texto);

		while (tokenizer.hasMoreTokens()) {

			String palabra = tokenizer.nextToken().trim();

			if (palabra.length() > 2 && !palabra.contains(",")) {

				for (String palabraCorrecion : palabrasCorregir) {

					int valor = computeLevenshteinDistance(palabra, palabraCorrecion); 

					if (palabraCorrecion.length() <= 4 && !palabra.equals("data")) {

						if ( valor == 1 ) 

							texto = texto.replaceAll("\\b" + palabra + "\\b", " " + palabraCorrecion+" ");

					} else 

						if ( valor >= 1 && valor <= 2) {

							texto = texto.replaceAll("\\b" +palabra + "\\b", " " + palabraCorrecion + " ");

						}
				}

			}
		}	

		return texto;
	}

	private Integer[] getDatosResolucionTotacionImagen(String ruta) {

		Integer[] datosImagen = new Integer[]{300,0}; 

		try {


			Process process = Runtime.getRuntime().exec("cmd /c  tesseract  --psm 0 " +ruta + " stdout");

			BufferedReader bufferedReaderInputStream = new BufferedReader(new InputStreamReader(process.getInputStream()));

			BufferedReader bufferedReadeErrorStream = new BufferedReader(new InputStreamReader(process.getErrorStream()));

			String linea = null;
			StringBuffer buffer = new StringBuffer();


			while ((linea = bufferedReaderInputStream.readLine()) != null)  
				buffer.append(linea).append("\n");

			while ((linea = bufferedReadeErrorStream.readLine()) != null)  
				buffer.append(linea).append("\n");


			process.waitFor();

			bufferedReadeErrorStream.close();
			bufferedReaderInputStream.close();

			String resultado = buffer.toString().toLowerCase();

			if (!resultado.isBlank()) {

				String datoResolucion = getDatoHorizontal(resultado, "resolution as");

				String rotateImagen = getDatoHorizontal(resultado, "rotate");

				String degrees = getDatoHorizontal(resultado, "degrees");

				if (!rotateImagen.isBlank()) { //Si no esta en blanco estuvo bien la lectura de la imagen

					datosImagen[0] = datoResolucion.isBlank() ? 0 : Integer.parseInt(datoResolucion);
					datosImagen[1] = degrees.equals("270") && rotateImagen.equals("90") ? 270 :Integer.parseInt(rotateImagen);

				} else { //Si esta en blanco se pudo debe a un problema de la posicion de la imagen

					if (numeroMaximaRotaciones <= 2) { //Se rotan maximo dos veces para encontrar la rotacion

						rotarArchivo(new File(ruta),90,true);
						numeroMaximaRotaciones++;
						return getDatosResolucionTotacionImagen(ruta); //Se realiza una llamada recursiva

					}


				}

			}

		} catch (Exception e) {

			e.printStackTrace();

		}


		return datosImagen;


	}

	public String getIdiomaTexto(String texto) {

		String resultado = "en"; 

		try {

			LanguageDetector detector = new OptimaizeLangDetector().loadModels();
			LanguageResult result = detector.detect(texto);
			resultado = result.getLanguage();

		} catch (Exception e) {}


		return resultado;

	}

	public DataOCRFile extraerTextoOCR(PDDocument document, String nombreArchivo, File tmpFile) {

		DataOCRFile dataOCRFile = null;
		String idioma = "";

		try {

			PDFRenderer pdfRenderer = new PDFRenderer(document);
			Tesseract tesseract = new Tesseract();
			tesseract.setPageSegMode(1);
			tesseract.setDatapath("recursos/tessdata");
			tesseract.setTessVariable("preserve_interword_spaces", "1");
			tesseract.setLanguage("eng"); //Se escoge inicialmente lenguaje ingles pero se revisara si este es

			idioma  = getIdiomaTexto(getTextoImagen(pdfRenderer,tesseract,nombreArchivo,0));

			if (idioma.equalsIgnoreCase("es")) 

				tesseract.setLanguage("spa"); 

			else 
				tesseract.setLanguage("eng");

			dataOCRFile = getPDFAndTextImages(pdfRenderer,tesseract,document.getNumberOfPages(),idioma,nombreArchivo);
			dataOCRFile.setIdioma(idioma);


		} catch (Exception e) {

			e.printStackTrace();
		} 


		return dataOCRFile;

	}

	private String getTextoImagen(PDFRenderer pdfRenderer,Tesseract tesseract,String nombreArchivo,
			int numeroPagina) 
					throws IOException, TesseractException {


		BufferedImage bim = pdfRenderer.renderImageWithDPI(numeroPagina, 300, ImageType.RGB);

		String nombreImagen = nombreArchivo.replaceAll(".pdf", "") + numeroPagina;

		File temp = File.createTempFile(nombreImagen, ".tif"); 
		ImageIO.write(bim, "tif", temp);

		String result = tesseract.doOCR(temp);

		return result;

	}

	private DataOCRFile getPDFAndTextImages(PDFRenderer pdfRenderer,Tesseract tesseract,
			int totalPaginas,String idioma, String nombreArchivo) throws Exception {

		List<RenderedFormat> list = new ArrayList<RenderedFormat>();
		list.add(RenderedFormat.PDF);
		
		StringBuffer textoArchivo = new StringBuffer();

		//Se crea una carpeta temporal
		File[] files = new File[totalPaginas];

		int resolucion = 300;

		for (int page = 0; page < totalPaginas; page++) {

			//Se obtiene la resolucion de la imagen
			String nombreImagen = nombreArchivo  + page;
			File tempImagenGenerada = procesarImagenes(pdfRenderer,300,page,nombreImagen);

			limpiarImagen(tempImagenGenerada.getAbsolutePath(),rutaTemporal);

			String nombreArchivoGenerado = rutaTemporal + tempImagenGenerada.getName();

			File tempLimpiando = new File(nombreArchivoGenerado);

			//********************** se obtiene la resolución de la imagen *****************************************

			Integer[] datosImagen = getDatosResolucionTotacionImagen(nombreArchivoGenerado);

			int resolucionImagenActual =  datosImagen[0];
			int rotacionImagenActual  =  datosImagen[1];

			if (page == 0) {

				if (resolucionImagenActual >= 200 && resolucionImagenActual <=300)

					resolucion = 440;

			} else {

				if (resolucion == 300 && (resolucionImagenActual >= 200 && resolucionImagenActual <=300))

					resolucion = 440;

				else if (resolucion == 440 && (resolucionImagenActual >= 300 && resolucionImagenActual <=400))

					resolucion = 300;

				else if (resolucion < 200 && resolucionImagenActual < 200)

					resolucion = resolucionImagenActual;


			}



			//************************************ Se convierte si es necesario ***********************************************
			/*if (resolucion != 300) {

				nombreArchivoGenerado = procesarImagenes(pdfRenderer,300,page,nombreImagen);
				tempLimpiando = new File(nombreArchivoGenerado);


			}*/


			if (rotacionImagenActual != 0) 

				rotarArchivo(tempLimpiando,rotacionImagenActual,false);


			//Se obtiene la imagen limpiada
			String pathImageOutput =  rutaTemporal + nombreImagen;
			tesseract.createDocuments(tempLimpiando.getAbsolutePath(), pathImageOutput , list);
			textoArchivo.append(tesseract.doOCR(tempImagenGenerada));

			File file = new File(pathImageOutput + ".pdf");
			files[page] = file;

			numeroMaximaRotaciones = 1;


		}

		File fileOutputPDF = new File(nombreArchivo + ".pdf");
		fileOutputPDF.deleteOnExit();
		PdfUtilities.mergePdf(files, fileOutputPDF);

		FileUtils.cleanDirectory(new File(rutaTemporal)); 

		return new DataOCRFile(fileOutputPDF,textoArchivo.toString());

	}

	private File procesarImagenes(PDFRenderer pdfRenderer, int resolucion, int page,String nombreImagen) throws IOException {

		BufferedImage bim = pdfRenderer.renderImageWithDPI(page, resolucion, ImageType.RGB);

		File temp = new File(nombreImagen + ".tif");
		ImageIO.write(bim, "tif", temp);

		return temp;
	}

	public void limpiarImagen(String ruta,String rutaDestino) {

		try {

			Process process = Runtime.getRuntime().exec("cmd /c scantailor-cli.exe --start-filter=1 --end_filter=6 --dpi=300 --color-mode=black_and_white " + ruta + " "+  rutaDestino );
			process.waitFor();

		} catch (Exception e) {

			e.printStackTrace();

		}

	}

	public static BufferedImage rotateImagen(BufferedImage img, double angle) {

		double rads = Math.toRadians(angle);
		double sin = Math.abs(Math.sin(rads)), cos = Math.abs(Math.cos(rads));
		int w = img.getWidth();
		int h = img.getHeight();
		int newWidth = (int) Math.floor(w * cos + h * sin);
		int newHeight = (int) Math.floor(h * cos + w * sin);

		BufferedImage rotated = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = rotated.createGraphics();
		AffineTransform at = new AffineTransform();
		at.translate((newWidth - w) / 2, (newHeight - h) / 2);

		int x = w / 2;
		int y = h / 2;

		at.rotate(rads, x, y);
		g2d.setTransform(at);
		g2d.drawImage(img, 0, 0,null);
		g2d.setColor(Color.RED);
		g2d.drawRect(0, 0, newWidth - 1, newHeight - 1);
		g2d.dispose();

		return rotated;
	}

	public void rotarArchivo(File imagenArchivo, int rotacion, boolean sinCambiarTamaño) throws Exception {

		if (sinCambiarTamaño) {

			Process process = Runtime.getRuntime().exec("cmd /c  scantailor-cli  -rotate=" +rotacion + " " + imagenArchivo.getAbsolutePath());
			process.waitFor();

		} else {	

			System.out.println("Imagen archivo rota" + imagenArchivo.getAbsolutePath());
			BufferedImage bufferedImage = ImageIO.read(imagenArchivo);

			BufferedImage bufferedImageRotada = rotateImagen(bufferedImage, rotacion);
			ImageIO.write(bufferedImageRotada, "tif", imagenArchivo);
		}


	}

	public float getTamañoLetraDocumento(PDDocument document) throws IOException {

		PosicionTextoPDF posicionTextoPDF = new PosicionTextoPDF(document, 1, 1);

		return posicionTextoPDF.getTamañoLetra();
	}

	public String getTextoRecortadoFormateada(PDDocument document,int posicionInicial, int ancho,String idioma) throws IOException {

		String texto = getColumnaPDF(document, posicionInicial, ancho);

		texto = eliminarMasDeDosEspaciosEnTexto(
				getFormatearTexto(texto, idioma.equals("en") ? getPalabrasCorregirIngles() : getPalabrasCorregirEspañol()));

		return texto;
	}

	//******************** Este codigo fue tomado de internet *********************************************

	private static int computeLevenshteinDistance(String str1, String str2) {

		return computeLevenshteinDistance(str1.toCharArray(),
				str2.toCharArray());
	}

	private static int minimum(int a, int b, int c) {
		return Math.min(a, Math.min(b, c));
	}

	private static int computeLevenshteinDistance(char [] str1, char [] str2) {
		int [][]distance = new int[str1.length+1][str2.length+1];

		for(int i=0;i<=str1.length;i++){
			distance[i][0]=i;
		}
		for(int j=0;j<=str2.length;j++){
			distance[0][j]=j;
		}
		for(int i=1;i<=str1.length;i++){
			for(int j=1;j<=str2.length;j++){ 
				distance[i][j]= minimum(distance[i-1][j]+1,
						distance[i][j-1]+1,
						distance[i-1][j-1]+
						((str1[i-1]==str2[j-1])?0:1));
			}
		}
		return distance[str1.length][str2.length];

	}

	public boolean deleteDirectory(File directoryToBeDeleted) {

		File[] allContents = directoryToBeDeleted.listFiles();
		if (allContents != null) {
			for (File file : allContents) {
				deleteDirectory(file);
			}
		}
		return directoryToBeDeleted.delete();
	}

	//******************************************** Metodos JSON ************************************************
	public JSONObject getJSONObjectOrdenNatural() {

		JSONObject json = new JSONObject();

		try {

			Field changeMap = json.getClass().getDeclaredField("map");
			changeMap.setAccessible(true);
			changeMap.set(json, new LinkedHashMap<>());
			changeMap.setAccessible(false);

		} catch (Exception e) {}

		return json;

	}

	public void createFile(String nombreArchivo,String JSON) {

		FileWriter file;
		try {

			file = new FileWriter(rutaTemporal+nombreArchivo);
			file.write(JSON);
			file.flush();
			file.close();

		} catch (IOException e) {

			e.printStackTrace();
		} 


	}

	public String getRegexFecha() {
		return regexFecha;
	}


	//**************************************************** class DataOCRFile ***************************************************************

	public class DataOCRFile {

		private File file;
		private String idioma;
		private String texto;


		public DataOCRFile(File file,String texto) {

			this.file = file;
			this.texto = texto;

		}

		public void setTexto(String texto) {
			this.texto = texto;
		}

		public void setIdioma(String idioma) {
			this.idioma = idioma;
		}

		public File getFile() {
			return file;
		}

		public String getTexto() {
			return texto;
		}

		public String getIdioma() {
			return idioma;
		}

	}
}
