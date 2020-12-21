package com.legalCredit;


import static com.legalCredit.componentes.Utilidades.getNumeroEntero;
import static com.legalCredit.componentes.Utilidades.getNumeroReal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.json.JSONArray;
import org.json.JSONObject;

import com.legalCredit.componentes.LayoutTextStripper;
import com.legalCredit.componentes.PosicionTexto;
import com.legalCredit.componentes.PosicionTextoPDF;
import com.legalCredit.componentes.Utilidades;
import com.legalCredit.modelo.CuentaReporte;
import com.legalCredit.modelo.HistorialPago;
import com.legalCredit.modelo.Inquiery;
import com.legalCredit.modelo.PublicAccount;
import com.legalCredit.modelo.Reporte;
import com.legalCredit.scraper.patrones.PatronEquifax;
import com.legalCredit.scraper.patrones.PatronExperian;
import com.legalCredit.scraper.patrones.PatronTransunion;

public class ScrapingPDF {

	private String[] cra = {"equifax","experian","transunion"};

	private Utilidades utilidades = new Utilidades();

	private PatronEquifax patronEquifax = new PatronEquifax();
	private PatronTransunion patronTransunion = new PatronTransunion();
	private PatronExperian patronExperian = new PatronExperian();

	private String bureau_id;
	private String credit_file_date;

	public String scrapingPDF(String Json) {

		String result = "";

		try {

			JSONObject rootJSON = new JSONObject(Json);
			bureau_id =  rootJSON.getString("bureau_id");
			credit_file_date =  rootJSON.getString("credit_file_date");
			byte[] decoded = Base64.getDecoder().decode(rootJSON.getString("file"));

			SimpleDateFormat simpleDateFormat = new SimpleDateFormat("ddMMyy-hhmmss");
			String name = String.format("%s%s.pdf", simpleDateFormat.format( new Date() ),Math.random());
			File tmpFile = File.createTempFile(name, ".pdf");

			FileOutputStream writer = new FileOutputStream(tmpFile);
			writer.write(decoded);
			writer.close();

			//Se cargan los datos del PDF con la libreria PDFBox
			PDDocument document = PDDocument.load(tmpFile);

			PDFTextStripper pdfStripper = new PDFTextStripper();
			String textoArchivo = pdfStripper.getText(document).toLowerCase().replaceAll("trans union", "transunion").
					replaceAll("expenan","experian");

			String patron[] = getPatron(textoArchivo);	

			if (patron[0].equals("PatronEquifax")) {

				if (patron[1].equals("PatronUno"))

					result = scrapearPatronGenerico(textoArchivo,patronEquifax.getCra());

				else if (patron[1].equalsIgnoreCase("PatronDos")) 

					result = scrapearEquifaxPatronDos(textoArchivo,getTextoConFormato(document));

			} else if (patron[0].equals("PatronTransunion")) {

				if (patron[1].equals("PatronUno"))

					result = scrapearTransunionPatronUno(textoArchivo,getTextoConFormato(document),document,true);

				else if (patron[1].equals("PatronDos"))

					result = scrapearPatronGenerico(textoArchivo,patronTransunion.getCra()); 

			} else if (patron[0].equals("PatronExperian")) {

				if (patron[1].equalsIgnoreCase("PatronUno")) 

					result = scrapearExperianPatronUno(textoArchivo,document);

				else if (patron[1].equalsIgnoreCase("PatronDos")) 

					result = scrapearExperianPatronDos(textoArchivo,document,true);

				else if (patron[1].equalsIgnoreCase("PatronCuatro")) 

					result = scrapearPatronGenerico(textoArchivo,patronExperian.getCra());

				else if (patron[1].equalsIgnoreCase("PatronCinco")) 

					result = scrapearExperianPatronCinco(textoArchivo,getTextoConFormato(document),document,true);

			} else if (patron[0].equals("PatronExperianSinCuentas")) {

				if (patron[1].equalsIgnoreCase("PatronDos")) 

					result = scrapearExperianPatronDos(textoArchivo,document,false);

				else if (patron[1].equalsIgnoreCase("PatronCinco")) 

					result = scrapearExperianPatronCinco(textoArchivo,getTextoConFormato(document),document,false);

			} else {

				result = generarJSONNingunPatron();

			}

			if (document != null)  
				document.close();


		} catch (Exception e) {

			result = e.getMessage();

		}


		return result;
	}


	/*******************************************************************************************************
	 *************************************** EQUIFAX *******************************************************
	 *******************************************************************************************************/

	/**
	 * Scraper equifax de tipo patron uno 
	 * @param archivo
	 * @param textoMinuscula
	 */
	private String scrapearPatronGenerico(String textoMinuscula, String cra) {

		String[] elementos = patronEquifax.getElementosAContenerTextoRepetidamentePatronUno();
		String[] elementosReporte = patronEquifax.getElementosAdicionalesPatronGenerico();

		List<CuentaReporte> cuentasReporte = new ArrayList<CuentaReporte>();
		List<Inquiery> cuentasInquieries = new ArrayList<Inquiery>();
		List<PublicAccount> publicAccounts = new ArrayList<PublicAccount>();

		int posicionInicialCuenta = 0;
		int posicionFinalCuenta = 0;

		String ssn = getSSN(textoMinuscula);
		String employeer[] = new String[]{utilidades.eliminarRetornosCarro(utilidades.getDatoVertical(textoMinuscula,patronEquifax.getTagEmployee()).replaceAll("employer(s)", ""))};
		String name = utilidades.getDatoVertical(textoMinuscula,patronEquifax.getTagName());
		String opencreditcards = utilidades .getDatoHorizontal(textoMinuscula,elementosReporte[0]);
		String openretailcards = utilidades.getDatoHorizontal(textoMinuscula,elementosReporte[1]);
		String openrealrstateloans = utilidades.getDatoHorizontal(textoMinuscula,elementosReporte[2]);
		String openinstallmentloans = utilidades.getDatoHorizontal(textoMinuscula,elementosReporte[3]);
		String totalopenaccounts = utilidades.getDatoHorizontal(textoMinuscula,elementosReporte[4]);
		String accountseverlate = utilidades.getDatoHorizontal(textoMinuscula,elementosReporte[5]);
		String collectionsaccounts = utilidades.getDatoHorizontal(textoMinuscula,elementosReporte[6]);
		String averageaccountage = utilidades.getDatoHorizontal(textoMinuscula,elementosReporte[7]);
		String oldestaccount = utilidades.getDatoHorizontal(textoMinuscula,elementosReporte[8]);
		String newestaccount = utilidades.getDatoHorizontal(textoMinuscula,elementosReporte[9]);
		String creditdebt = utilidades.getDatoVertical(textoMinuscula,elementosReporte[10]);
		String totalcredit = utilidades.getDatoVertical(textoMinuscula,elementosReporte[11]);
		String creditandretailcarddebt = utilidades.getDatoHorizontal(textoMinuscula,elementosReporte[12]);
		String realestatedebt = utilidades.getDatoHorizontal(textoMinuscula,elementosReporte[13]);
		String installmentloansdebt = utilidades.getDatoHorizontal(textoMinuscula,elementosReporte[14]);
		String collectionsdebt = utilidades.getDatoHorizontal(textoMinuscula,elementosReporte[15]);
		String totaldebt = utilidades.getDatoHorizontal(textoMinuscula,elementosReporte[16]);
		String myhardcreditinquiries = utilidades.getLineaAnterior(textoMinuscula,elementosReporte[17]);
		String mypublicrecords = utilidades.getLineaAnterior(textoMinuscula,elementosReporte[18]);
		String creditscore = getCreditScorePatronGenerico(textoMinuscula,elementosReporte[19]);
		String dateofreport = utilidades.getFechaFormatoMesDiaAño(utilidades.getDatoHorizontal(textoMinuscula,elementosReporte[20]).trim());


		String[] address = getAddressPatronGenerico(textoMinuscula,patronEquifax.getTagAddress(),name);
		String birthyear = utilidades.getDatoVertical(textoMinuscula,elementosReporte[21]);
		String overallcreditusage = utilidades.getDatoVertical(textoMinuscula,elementosReporte[22]);

		Reporte reporte = new Reporte(ssn, name, patronTransunion.getCra(), employeer, opencreditcards, openretailcards, 
				openrealrstateloans, openinstallmentloans, totalopenaccounts, accountseverlate, 
				collectionsaccounts, averageaccountage, oldestaccount, newestaccount,
				myhardcreditinquiries, creditdebt, totalcredit, creditandretailcarddebt, 
				realestatedebt, installmentloansdebt, collectionsdebt, totaldebt, mypublicrecords, 
				dateofreport, creditscore, address, birthyear, overallcreditusage,"PatronGenerico");


		//************************* Se obtienen los datos de la cuentas del reporte ********************

		int posicionInicioBusquedaTag = 0;

		String accountTypeBefore = null;
		String linea = "";

		do {

			posicionInicialCuenta = textoMinuscula.indexOf(patronEquifax.getTagPrimerTag(),
					posicionInicioBusquedaTag);

			if (posicionInicialCuenta > 0) {

				posicionFinalCuenta = textoMinuscula.indexOf(patronEquifax.getTagUltimoTag(),
						posicionInicialCuenta);

				if (posicionFinalCuenta < 0) // En caso que ya se termina y no halla mas cuenta

					posicionFinalCuenta = textoMinuscula.length();


				int posicionInicioHistorialCuenta = textoMinuscula.indexOf("payment history", posicionInicioBusquedaTag);
				int posicionFinHistorialPago = textoMinuscula.indexOf(patronEquifax.getTagPrimerTag(),
						posicionFinalCuenta);

				if (posicionFinHistorialPago < 0) {

					posicionFinHistorialPago = textoMinuscula.indexOf("date of report",posicionInicialCuenta);
					posicionFinalCuenta = posicionFinHistorialPago;

				}

				posicionInicioBusquedaTag = posicionFinalCuenta;

				String textoCuenta = textoMinuscula.substring(posicionInicialCuenta, posicionFinalCuenta);
				String textCuentaSinFormato = textoCuenta;


				if (linea.length() == 0 && textoCuenta.contains("date of report")) {

					int posicionInicial = textoCuenta.indexOf("date of report");
					int posicionFinal =  textoCuenta.indexOf("\n",posicionInicial);
					linea = textoCuenta.substring(posicionInicial,posicionFinal);

				}

				textoCuenta = textoCuenta.replace(linea, "");

				String accountName = utilidades.getDatoHorizontal(textoCuenta, elementos[0]);
				String accountNumber = utilidades.getDatoHorizontal(textoCuenta, elementos[1]);
				String accountType = utilidades.getDatoHorizontal(textoCuenta, elementos[2]);
				String accountStatus = utilidades.getDatoHorizontal(textoCuenta, elementos[4]);
				String paymentStatus = utilidades.getDatoHorizontal(textoCuenta, elementos[5]);
				String statusUpdated = utilidades.getFechaFormatoMesDiaAño(utilidades.getDatoHorizontal(textoCuenta, elementos[6]));
				String balance = utilidades.getDatoHorizontal(textoCuenta, elementos[7]);
				String balanceUpdated = utilidades.getFechaFormatoMesDiaAño(utilidades.getDatoHorizontal(textoCuenta, elementos[8]));
				String limit = utilidades.getDatoHorizontal(textoCuenta, elementos[9]);
				String monthlyPayment = utilidades.getDatoHorizontal(textoCuenta, elementos[10]);
				String pastDueAmount = utilidades.getDatoHorizontal(textoCuenta, elementos[11]);
				String highestBalance = utilidades.getDatoHorizontal(textoCuenta, elementos[12]);
				String terms = utilidades.getDatoHorizontal(textoCuenta, elementos[13]);
				String responsibility = utilidades.getDatoHorizontal(textoCuenta, elementos[14]);
				String yourStatement = utilidades.getDatoHorizontal(textoCuenta, elementos[15]);
				String comments = utilidades.getDatoHorizontal(textoCuenta, elementos[16]);
				String dateOpened =  utilidades.getFechaFormatoMesDiaAño(utilidades.getDatoHorizontal(textoCuenta, elementos[3]));
				String accounttypeone = getAccounttypeonePatronGenerico(textoMinuscula, textCuentaSinFormato, accountName);
				String accounttypetwo = "";
				String creditusage = getCreditUsagePatronGenerico(textoCuenta, "payment history");
				String creditusagedescription = getCreditUsageDescriptionPatronGenerico(textoCuenta, elementos[18]);
				String loanType = "";
				String dateClosed = "";
				String statusDetail = "";
				String paymentReceived = "";
				String originalCreditor = utilidades.getDatoHorizontal(textoCuenta, elementos[17]);
				String lastActivity = "";

				if (!accounttypeone.isEmpty())

					accountTypeBefore =	accounttypeone;

				else

					accounttypeone = accountTypeBefore;



				/*
				 * ****************** Se obtienen el historial de pago **************************************
				 */
				String historialPago = textoMinuscula.substring(posicionInicioHistorialCuenta,
						posicionFinHistorialPago > 0 ? posicionFinHistorialPago : textoMinuscula.length()).replaceAll(dateofreport, "");
				historialPago = historialPago.replace(linea, "");

				ArrayList<String> anosHistoriaPago = new ArrayList<String>();
				Matcher matcher = utilidades.getPatronAno().matcher(historialPago);

				while (matcher.find()) {

					int numeroAno = Integer.parseInt(matcher.group());

					if ((numeroAno > 1950 && numeroAno < 2020) && !anosHistoriaPago.contains(matcher.group()))
						anosHistoriaPago.add(matcher.group());

				}

				Map<String, List<HistorialPago>> mesesCanceladosPorAno = new HashMap<String, List<HistorialPago>>();

				for (int i = 0; i < anosHistoriaPago.size(); i++) {

					List<HistorialPago> meses = new ArrayList<HistorialPago>();
					String texto = historialPago.substring(historialPago.indexOf(anosHistoriaPago.get(i)),
							(i + 1) == anosHistoriaPago.size() ? historialPago.length()
									: historialPago.indexOf(anosHistoriaPago.get(i + 1)));

					//Se obtienen los meses
					matcher = utilidades.getPatronMes().matcher(texto);

					while (matcher.find()) {

						String mes = matcher.group();

						meses.add(new HistorialPago(mes, anosHistoriaPago.get(i)  +"-" + utilidades.getNumeroMes(mes), 
								"payment historical", ""));

					}	
					mesesCanceladosPorAno.put(anosHistoriaPago.get(i),meses );
				}

				CuentaReporte cuentaReporte = new CuentaReporte(accountName, accountNumber,
						accountType, accountStatus, paymentStatus, statusUpdated, balance, balanceUpdated, limit,
						monthlyPayment, pastDueAmount, highestBalance, terms, responsibility, yourStatement,
						comments, dateOpened, loanType, dateClosed, statusDetail, paymentReceived, originalCreditor,
						lastActivity, accounttypeone, accounttypetwo, creditusage, creditusagedescription,
						mesesCanceladosPorAno);

				cuentasReporte.add(cuentaReporte);

			}

		} while (posicionInicialCuenta > 0);



		//*************** Se obtienen los datos del Inquiries datos ***********************************

		int posicionInicialInquires = textoMinuscula.indexOf(patronEquifax.getTagInquiries(),posicionFinalCuenta);

		//Se busca el final del texto donde se buscaran los datos del inquieries
		if (posicionInicialInquires> 0) {

			int posicionFinalInquires = textoMinuscula.indexOf(patronEquifax.getTagPublicRecords(),posicionFinalCuenta) 
					+ patronEquifax.getTagPublicRecords().length();

			if (posicionFinalInquires < 13)

				posicionFinalInquires = textoMinuscula.indexOf(patronEquifax.getTagCreditScore(),posicionFinalCuenta);

			//Se verifica que existan cuentas 
			if (posicionFinalInquires > posicionInicialInquires) {

				String[] elementosInquires = patronEquifax.getElementosInquiryAccount();
				String textoCuentaInquires = textoMinuscula.substring(posicionInicialInquires,posicionFinalInquires);
				String contactinformation = "contact information";

				posicionFinalInquires = 0;
				do {

					posicionInicialInquires = textoCuentaInquires.indexOf(elementosInquires[0],posicionFinalInquires);
					posicionFinalInquires = textoCuentaInquires.indexOf(elementosInquires[0],posicionInicialInquires+6);


					if (posicionFinalInquires < 0)

						posicionFinalInquires = textoCuentaInquires.length();


					if (posicionInicialInquires > 0) {

						String textoCuenta = textoCuentaInquires.substring(posicionInicialInquires,posicionFinalInquires);

						String nombreCuenta = getUltimaLineaPatronGenerico(textoCuenta,cra);
						String inquiryDate = utilidades.getDatoHorizontal(textoCuenta, elementosInquires[0]);
						String removalDate = utilidades.getDatoHorizontal(textoCuenta, elementosInquires[1]);
						String businessType = utilidades.getDatoHorizontal(textoCuenta, elementosInquires[2]);
						String contactInformation = textoCuenta.substring(textoCuenta.indexOf(contactinformation),textoCuenta.indexOf(nombreCuenta)).
								replaceAll("\n", " ").replaceAll("\r", " ").replaceAll(contactinformation, " ").
								trim();
						String inquieresType = "inquiries";

						cuentasInquieries.add(new Inquiery(nombreCuenta, inquiryDate, removalDate,
								businessType, contactInformation, inquieresType,""));
					}

				}while (posicionInicialInquires > 0);


			}



		}

		//*************** Se obtienen los datos de los public account ****************************************

		int posicionInicialPublic = textoMinuscula.indexOf(patronEquifax.getTagPublicRecords(),posicionFinalCuenta);


		//Se busca el final del texto donde se buscaran los datos del inquieries
		if (posicionInicialPublic> 0) {

			int posicionFinalPublic = textoMinuscula.indexOf(patronEquifax.getTagCreditScore(),posicionInicialPublic) 
					+ patronEquifax.getTagCreditScore().length();

			if (posicionFinalPublic < 13)

				posicionFinalPublic = textoMinuscula.length();

			//Se verifica que existan cuentas 
			if (posicionFinalPublic > posicionInicialPublic) {

				String[] elementosPublic = patronEquifax.getElementosPublicRecorsAccount();
				String textoCuentaPublic = textoMinuscula.substring(posicionInicialPublic,posicionFinalPublic);


				posicionFinalPublic = 0;
				do {

					posicionInicialPublic = textoCuentaPublic.indexOf(elementosPublic[0],posicionFinalPublic);
					posicionFinalPublic = textoCuentaPublic.indexOf(elementosPublic[0],posicionInicialPublic+6);


					if (posicionFinalPublic < 0)

						posicionFinalPublic = textoCuentaPublic.length();


					if (posicionInicialPublic > 0) {

						String textoCuenta = textoCuentaPublic.substring(posicionInicialPublic,posicionFinalPublic);

						String accountName = getUltimaLineaPatronGenerico(textoCuenta,cra);
						String filingDate = utilidades.getDatoHorizontal(textoCuenta, elementosPublic[0]);
						String amount = utilidades.getDatoHorizontal(textoCuenta, elementosPublic[1]);
						String referenceNumber = utilidades.getDatoHorizontal(textoCuenta, elementosPublic[2]);
						String court = utilidades.getDatoHorizontal(textoCuenta, elementosPublic[3]);
						String plaintiff = utilidades.getDatoHorizontal(textoCuenta, elementosPublic[4]);

						publicAccounts.add(new PublicAccount(accountName, filingDate, amount, 
								referenceNumber, court, plaintiff));

					}

				}while (posicionInicialInquires > 0);
			}

		}

		return generarJSON(reporte,cuentasReporte,cuentasInquieries,publicAccounts);
	}

	/**
	 * Scraper equifax patron dos
	 * @param archivo
	 * @param textoMinuscula
	 * @throws IOException
	 */
	private String scrapearEquifaxPatronDos(String textoMinuscula,String textoConFormato) throws IOException {

		String[] elementos = patronEquifax.getElementosAContenerTextoRepetidamentePatronDos();
		String[] tiposCuentaDos = {"revolving accounts","mortgage accounts",
				"installment accounts","other accounts",
		"consumer statements"};


		List<CuentaReporte> cuentasReporte = new ArrayList<CuentaReporte>();
		List<Inquiery> cuentasInquieries = new ArrayList<Inquiery>();
		Map<String,List<HistorialPago>> informacionCuentas = null;

		String ssn = utilidades.getDatoHorizontal(textoMinuscula, patronEquifax.getTagSSN());
		String name = utilidades.getDatoHorizontal(textoMinuscula,patronEquifax.getTagAccountNamePatronDos()).replaceAll(" +", " ");
		String employeer[] = getNombreEmpleadosEquifaxPatronDos(textoMinuscula,patronEquifax.getTagEmployeePatronDos());
		String opencreditcards = "";
		String openretailcards = "";
		String openrealrstateloans = "";
		String openinstallmentloans = "";
		String totalopenaccounts = utilidades.getDatoHorizontal(textoMinuscula, "total");
		String accountseverlate = "";
		String collectionsaccounts = utilidades.getDatoHorizontal(textoMinuscula,"collections");
		String averageaccountage = utilidades.getDatoHorizontal(textoMinuscula, "average account age");
		String oldestaccount = utilidades.getDatoHorizontal(textoMinuscula, "oldest account");
		String newestaccount = utilidades.getDatoHorizontal(textoMinuscula, "most recent account");
		String creditdebt = "";
		String totalcredit = "";
		String creditandretailcarddebt = utilidades.getDatoHorizontal(textoMinuscula,"accounts with negative information");
		String realestatedebt = "";
		String installmentloansdebt = "";
		String collectionsdebt = "";
		String totaldebt = "";
		String myhardcreditinquiries = "";
		String mypublicrecords = utilidades.getDatoHorizontal(textoMinuscula,"public records");
		String creditscore = "";

		String dateofreport = utilidades.getFechaFormatoMesDiaAño(utilidades.getDatoHorizontal(textoMinuscula, patronEquifax.getTagAccountReportDate()));

		String address[] = getAddressEquifaxPatronDos(textoMinuscula,patronTransunion.getTagAddress());
		String birthyear =utilidades.getFechaFormatoMesDiaAño(utilidades.getDatoHorizontal(textoMinuscula,patronEquifax.getTagBirthReportDate()));
		String overallcreditusage = "";




		//****************** Se definen los datos de la cuenta ****************************************************

		String[] tagFinales = {"revolving accounts","mortgage accounts",
				"installment accounts","other accounts",
		"consumer statements"};

		// Se divide el documento para poder clasificar los tipos de cuentas
		int posicionInicialRevolvingAccounts = textoMinuscula.indexOf(tagFinales[0]);
		int posicionInicialMortgageAccounts = textoMinuscula.indexOf(tagFinales[1],posicionInicialRevolvingAccounts);
		int posicionInicialInstallmentAccounts = textoMinuscula.indexOf(tagFinales[2],posicionInicialMortgageAccounts);
		int posicionInicialOtherAccounts = textoMinuscula.indexOf(tagFinales[3],posicionInicialInstallmentAccounts);
		int posicionInicialConsumerStatements = textoMinuscula.indexOf(tagFinales[4],posicionInicialOtherAccounts);

		int[] posicionesInicialesTiposCuentas = new int[] {posicionInicialRevolvingAccounts,posicionInicialMortgageAccounts,posicionInicialInstallmentAccounts,
				posicionInicialOtherAccounts,posicionInicialConsumerStatements};

		String[] textosTiposCuentas = new String[5];

		//Se obtienen los textos
		for (int i = 0; i < posicionesInicialesTiposCuentas.length; i++) {

			int posicionInicialTexto = posicionesInicialesTiposCuentas[i];
			int posicionFinalTexto = ((i+1) == (posicionesInicialesTiposCuentas.length)) ? textoMinuscula.length() :
				(posicionesInicialesTiposCuentas[i+1] + 100);

			String texto = "";

			if (posicionInicialTexto > - 1 && posicionInicialTexto<posicionFinalTexto) {

				texto = textoMinuscula.substring(posicionInicialTexto, posicionFinalTexto);
				texto = texto.replaceFirst("\n", "");
				texto = texto.substring(texto.indexOf("\n")+1);

			}
			textosTiposCuentas[i] = texto;

		}


		int posicionInicialCuenta = 0;
		int posicionFinalCuenta = 0;
		int posicionInicialConFormatoCuenta = 0;
		int posicionFinalConFormatoCuenta = 0;
		int posicionInicialCuentaTag = 0;
		int posicionInicialConFormatoCuentaTag = 0;

		do {

			posicionInicialCuenta = textoMinuscula.indexOf("account number",posicionInicialCuentaTag);
			posicionFinalCuenta = textoMinuscula.indexOf("account number",
					(posicionInicialCuenta + 10));

			posicionFinalCuenta = posicionFinalCuenta > 0 ? posicionFinalCuenta : textoMinuscula.length();
			posicionInicialCuentaTag = posicionFinalCuenta;

			posicionInicialConFormatoCuenta = textoConFormato.indexOf("accountnumber",posicionInicialConFormatoCuentaTag);
			posicionFinalConFormatoCuenta = textoConFormato.indexOf("accountnumber",
					(posicionInicialConFormatoCuenta + 10));

			posicionFinalConFormatoCuenta = posicionFinalConFormatoCuenta > 0 ? posicionFinalConFormatoCuenta : textoMinuscula.length();
			posicionInicialConFormatoCuentaTag = posicionFinalConFormatoCuenta;

			if (posicionInicialCuenta > 0 && posicionFinalConFormatoCuenta>posicionInicialConFormatoCuenta) {

				String textoCuenta = textoMinuscula.substring(posicionInicialCuenta,posicionFinalCuenta);
				String textoCuentaFormato = textoConFormato.substring(posicionInicialConFormatoCuenta,posicionFinalConFormatoCuenta);
				String accountName = getAccountNameEquifaxDos(textoMinuscula,textoCuenta);
				String accountNumber = utilidades.getCadenaRecortada(utilidades.getDatoHorizontal(textoCuenta, elementos[0]),"reported");;
				String accountType = utilidades.getDatoHorizontal(textoCuenta, elementos[1]);
				String accountStatus = utilidades.getCadenaRecortada(utilidades.getDatoHorizontal(textoCuenta, elementos[3]),"debt-to-credit ratio");
				String paymentStatus = "";
				String statusUpdated = "";
				String balance = utilidades.getDatoHorizontal(textoCuenta, elementos[7]);
				String balanceUpdated = ""; 
				String limit = utilidades.getCadenaRecortada(utilidades.getDatoHorizontal(textoCuenta, elementos[5],"account details"),"account type");;
				String monthlyPayment = utilidades.getDatoHorizontal(textoCuenta, elementos[15]);
				String pastDueAmount = utilidades.getCadenaRecortada(utilidades.getDatoHorizontal(textoCuenta, elementos[8],"account details"),"date reported");
				String highestBalance = utilidades.getDatoHorizontal(textoCuenta, elementos[4]);
				String terms = utilidades.getDatoHorizontal(textoCuenta, elementos[6]);
				String responsibility = "";
				String yourStatement = "";
				String comments = utilidades.getDatoVertical(textoCuenta, elementos[11],"account details");

				String dateOpened = utilidades.getFechaFormatoMesDiaAño(utilidades.getDatoHorizontal(textoCuenta, elementos[2]));
				String accounttypeone = getTypeAccountNamePatronDos(accountName);
				String accounttypetwo = getTypeAccount(textosTiposCuentas, textoCuenta, tagFinales,tiposCuentaDos);
				String creditusage = "";
				String creditusagedescription = ""; 
				String loanType = utilidades.getCadenaRecortada(utilidades.getDatoHorizontal(textoCuenta, elementos[10]),"date closed");
				String dateClosed =utilidades.getFechaFormatoMesDiaAño( utilidades.getDatoHorizontal(textoCuenta, elementos[12]));
				String statusDetail = "";
				String paymentReceived = utilidades.getCadenaRecortada(utilidades.getDatoHorizontal(textoCuenta, elementos[14]),"date of last");
				String originalCreditor = utilidades.getDatoHorizontal(textoCuenta, elementos[9]);
				String lastActivity = utilidades.getCadenaRecortada(utilidades.getDatoHorizontal(textoCuenta, elementos[13]),"scheduled");
				accountName = accountName.replace(accounttypeone, "").replace("(", "").replace(")", "");


				//****** Se obtiene los datos de las cuentas que se encuentran en las tablas *******

				Matcher matcherFechas = utilidades.getPatronFormatoTablaPatronDos().matcher(textoCuentaFormato);

				int posicionInicialLinea = 0;


				if (accountName.equalsIgnoreCase("pennymac loan services llc")) {

					informacionCuentas = new HashMap<String,List<HistorialPago>>();


					while (matcherFechas.find()) { //Se recorren las diferentes tabla que tiene la cuenta

						String lineaMeses = matcherFechas.group().trim();

						posicionInicialLinea = textoCuentaFormato.indexOf(lineaMeses,posicionInicialLinea + 10);
						String tipoCuenta = utilidades.getLineaAnterior(textoCuentaFormato, lineaMeses,posicionInicialLinea);

						if (tipoCuenta.contains("."))

							tipoCuenta = "paymenthistory";

						int posicionFinalLinea = 0;

						boolean sw = true;

						do { //Se procede a obtener las filas de los años

							posicionInicialLinea = textoCuentaFormato.indexOf("\n",posicionInicialLinea) + 2;
							posicionFinalLinea = textoCuentaFormato.indexOf('\n',posicionInicialLinea+2);


							if (posicionInicialLinea < posicionFinalLinea) {

								String linea = textoCuentaFormato.substring(posicionInicialLinea,posicionFinalLinea).trim();
								List<HistorialPago> datosCuentaMes = new ArrayList<HistorialPago>();

								//Se verifica si contiene el año de la tabla
								Matcher matcherAño = utilidades.getPatronAno().matcher(linea.substring(0,4));

								if (matcherAño.find()) {

									String año = matcherAño.group();
									linea = linea.substring(linea.indexOf(año)).replace(año, "    ");

									//Se procede a contar para ver si los valores estan completos
									Matcher matcherTabla = utilidades.getPatronValorTabla().matcher(linea);
									int numeroElmentos = 0;


									//Se verifica para el caso que todos los meses estan completo
									while (matcherTabla.find()) 

										numeroElmentos++;

									if (numeroElmentos == 12) {

										int indice = 0;
										matcherTabla = utilidades.getPatronValorTabla().matcher(linea);
										while (matcherTabla.find()) { //Se obtienen los valores

											datosCuentaMes.add(new HistorialPago(utilidades.getMeses()[indice],año + "-" + utilidades.getMeses()[indice],
													tipoCuenta,matcherTabla.group()));
											indice++;																						
										}

										informacionCuentas.put(año, datosCuentaMes); //Se agrega los valores a la coleccion


									} else if (numeroElmentos > 0) {

										//Se cuenta el numero de caracateres
										matcherTabla = utilidades.getPatronValorTabla().matcher(linea);
										matcherTabla.find();
										int numeroCaracteres = matcherTabla.group().length() <= 7 ? 7 : matcherTabla.group().length();

										for (String mes : utilidades.getMeses()) {

											int posicionMes = lineaMeses.indexOf(mes);
											String valor = "";

											if (posicionMes < linea.length()) {

												int posicionInicial = posicionMes - 2;
												int posicionFinal = posicionInicial + numeroCaracteres;

												if (posicionFinal > linea.length())

													posicionFinal = linea.length();

												valor = linea.substring(posicionInicial,posicionFinal).trim();

											} 

											datosCuentaMes.add(new HistorialPago(mes,año + "-" + utilidades.getNumeroMes(mes),
													tipoCuenta,valor));

										}

										informacionCuentas.put(año, datosCuentaMes); //Se agrega los valores a la coleccion

									}

								} else 

									sw = false;

							} else 

								sw = false;

						} while (sw);


					}

				}

				CuentaReporte cuentaReporte = new CuentaReporte(accountName, accountNumber,
						accountType, accountStatus, paymentStatus, statusUpdated, balance, balanceUpdated, limit,
						monthlyPayment, pastDueAmount, highestBalance, terms, responsibility, yourStatement,
						comments, dateOpened, loanType, dateClosed, statusDetail, paymentReceived, originalCreditor,
						lastActivity, accounttypeone, accounttypetwo, creditusage, creditusagedescription,
						informacionCuentas);

				cuentasReporte.add(cuentaReporte);

			}



		} while (posicionInicialCuenta>0);

		//**************************** Se obtienen las cuentas inquiries ***************************************
		int posicionInicialInquiries = textoMinuscula.indexOf("inquiries",posicionInicialConsumerStatements);
		int posicionFinalInquiries = textoMinuscula.indexOf("public records",posicionInicialInquiries);

		String textoInquiries = textoMinuscula.substring(posicionInicialInquiries, posicionFinalInquiries);


		int posicionHardInquieries =  textoInquiries.indexOf("hard inquiries");
		int posicionSoftInquieries =  textoInquiries.indexOf("soft inquiries");

		String[] textosInquieres = new String[2];
		textosInquieres[0] = posicionHardInquieries < 0 ? "No" :
			posicionSoftInquieries > 0 ? textoInquiries.substring(posicionHardInquieries,posicionSoftInquieries) : 
				textoInquiries.substring(posicionHardInquieries,textoInquiries.length()) ;

			textosInquieres[1] = posicionSoftInquieries > 0 ? 
					textoInquiries.substring(posicionSoftInquieries,textoInquiries.length()) :
						"No";


					Matcher m = utilidades.getPatronDateInquiresPatrosDos().matcher(textoInquiries);
					List<String> fechas = new ArrayList<String>();
					int posicionFinalFecha = 0;

					while (m.find()) 

						fechas.add(m.group().replaceAll("\n", ""));

					for (int i = 0; i < fechas.size(); i++) {

						String inquiryDate = fechas.get(i);

						int posicionInicialFecha = textoInquiries.indexOf(fechas.get(i),posicionFinalFecha);
						posicionFinalFecha = ((i+1) == fechas.size()) ? textoInquiries.length() : textoInquiries.indexOf(fechas.get(i+1),posicionInicialFecha + inquiryDate.length()); 

						String textCuenta =textoInquiries.substring(posicionInicialFecha,posicionFinalFecha).replace(inquiryDate, "").trim();

						if (textCuenta.contains("soft inquiries"))
							textCuenta = textCuenta.substring(0,textCuenta.indexOf("soft inquiries"));


						if (!textCuenta.contains("page")) {

							int posicionRetornoCarro = textCuenta.indexOf("\n") > 0 ? textCuenta.indexOf("\n") : 
								textCuenta.indexOf("\r") > 0 ? textCuenta.indexOf("\r") : textCuenta.length();

								String accountName = utilidades.eliminarRetornosCarro(textCuenta.substring(0,posicionRetornoCarro));
								String contactInformation = utilidades.eliminarRetornosCarro(textCuenta.replace(accountName,"")).replaceAll(" +", " ");
								String inquieresType = textosInquieres[0].contains(textCuenta) ? "hard" :
									textosInquieres[1].contains(textCuenta) ? "soft" :
										"";	  

								if (contactInformation.contains(name))

									contactInformation = contactInformation.substring(0,contactInformation.indexOf(name));

								cuentasInquieries.add(new Inquiery(accountName, inquiryDate, "", "", 
										contactInformation, inquieresType,""));
						}


					}

					//Se crea el reporte que se va ha insertar
					Reporte reporte = new Reporte(ssn, name, patronEquifax.getCra(), employeer, opencreditcards, openretailcards, 
							openrealrstateloans, openinstallmentloans, totalopenaccounts, accountseverlate, 
							collectionsaccounts, averageaccountage, oldestaccount, newestaccount,
							myhardcreditinquiries, creditdebt, totalcredit, creditandretailcarddebt, 
							realestatedebt, installmentloansdebt, collectionsdebt, totaldebt, mypublicrecords, 
							dateofreport, creditscore, address, birthyear, overallcreditusage,"PatronDos");

					return generarJSON(reporte,cuentasReporte,cuentasInquieries,null);		

	}

	/*******************************************************************************************************
	 *************************************** EXPERIAN ******************************************************
	 *******************************************************************************************************/
	private String scrapearExperianPatronUno(String textoMinuscula, PDDocument document) throws IOException {

		String[] elementos = patronExperian.getElementosAContenerTextoRepetidamentePatronUno();
		String[] tiposCuenta = {"adverse","satisfactory","inquiries","promocionalinquiries","reviewinquiries"};

		List<CuentaReporte> cuentasReporte = new ArrayList<CuentaReporte>();
		List<Inquiery> cuentasInquieries = new ArrayList<Inquiery>();

		String ssn = getSSN(textoMinuscula);
		String name = utilidades.getDatoVertical(textoMinuscula,patronExperian.getTagNamePatronUno()).
				replaceAll("[0-9]+", "");
		String employeer[] = new String[]{""};
		String opencreditcards = "";
		String openretailcards = "";
		String openrealrstateloans = "";
		String openinstallmentloans = "";
		String totalopenaccounts = "";
		String accountseverlate = "";
		String collectionsaccounts = "";
		String averageaccountage = "";
		String oldestaccount = "";
		String newestaccount = "";
		String creditdebt = "";
		String totalcredit = "";
		String creditandretailcarddebt = "";
		String realestatedebt = "";
		String installmentloansdebt = "";
		String collectionsdebt = "";
		String totaldebt = "";
		String myhardcreditinquiries = "";
		String mypublicrecords = "";
		String creditscore = "";

		String primeraLinea = utilidades.getLineas(textoMinuscula,1);
		String dateofreport = utilidades.getFechaFormatoMesDiaAño(primeraLinea.substring(0,primeraLinea.indexOf(" ")),"/",true);

		String address[] = getAddressExperianPatronUno(utilidades.getColumnaPDFPrimeraHoja(document,0,320),patronTransunion.getTagAddress());
		String birthyear = utilidades.getLineaAnterior(textoMinuscula,"year of birth");
		String overallcreditusage = "";

		//Se crea el reporte que se va ha insertar
		Reporte reporte = new Reporte(ssn, name, patronExperian.getCra(), employeer, opencreditcards, openretailcards, 
				openrealrstateloans, openinstallmentloans, totalopenaccounts, accountseverlate, 
				collectionsaccounts, averageaccountage, oldestaccount, newestaccount,
				myhardcreditinquiries, creditdebt, totalcredit, creditandretailcarddebt, 
				realestatedebt, installmentloansdebt, collectionsdebt, totaldebt, mypublicrecords, 
				dateofreport, creditscore, address, birthyear, overallcreditusage,"PatronUno");


		String[] tagFinales = {"potentially negative items","accounts in good standing"};
		int posicionInicialAdverseAccount = textoMinuscula.indexOf(tagFinales[0]);
		int posicionInicialSatisfactoryAccount = textoMinuscula.indexOf(tagFinales[1],posicionInicialAdverseAccount);

		int[] posicionesInicialesTiposCuentas = new int[] {posicionInicialAdverseAccount,posicionInicialSatisfactoryAccount};

		String[] textosTiposCuentas = new String[2];
		textosTiposCuentas[0] = posicionInicialAdverseAccount < 0 ? "" : getFormatearTexto(textoMinuscula.substring(posicionInicialAdverseAccount,utilidades.getPrimerValorPositiva(posicionesInicialesTiposCuentas,0,textoMinuscula.length())));
		textosTiposCuentas[1] = posicionInicialSatisfactoryAccount < 0 ? "" : getFormatearTexto(textoMinuscula.substring(posicionInicialSatisfactoryAccount,utilidades.getPrimerValorPositiva(posicionesInicialesTiposCuentas,1,textoMinuscula.length())));


		//Se obtiene las columnas del pdf
		PosicionTextoPDF posicionTextoPDF = new PosicionTextoPDF(document, 1, 2);
		PosicionTexto posicionTexto = posicionTextoPDF.buscarPosicionPalabra("Account number",1);
		Double anchoColumnaUno = posicionTexto.getPosXInicial();


		String columnaUno = utilidades.getColumnaPDF(document,0,anchoColumnaUno.intValue()).toLowerCase();
		int posicionFinalArchivo = columnaUno.indexOf("no inquiries shared with others");
		columnaUno = columnaUno.substring(0,posicionFinalArchivo>0 ? posicionFinalArchivo : columnaUno.length());

		PosicionTexto posicionTextoRecent = posicionTextoPDF.buscarPosicionPalabra("Recent balance",1);
		Double anchoColumnaDos = posicionTextoRecent.getPosXInicial() - anchoColumnaUno;

		String columnaDos = utilidades.getColumnaPDF(document,anchoColumnaUno.intValue(),anchoColumnaDos.intValue()).toLowerCase();

		PosicionTexto posicionTextoDateOpened = posicionTextoPDF.buscarPosicionPalabra("Date opened",1);
		Double anchoColumnaTres = posicionTextoDateOpened.getPosXInicial() - posicionTextoRecent.getPosXInicial();

		String columnaTres = utilidades.getColumnaPDF(document,(int)posicionTextoRecent.getPosXInicial(),anchoColumnaTres.intValue()).toLowerCase();

		PosicionTexto posicionTextoStatus = posicionTextoPDF.buscarPosicionPalabra("Status",1);
		Double anchoColumnaCuatro = posicionTextoStatus.getPosXInicial() - posicionTextoDateOpened.getPosXInicial();

		String columnaCuatro = utilidades.getColumnaPDF(document,(int)posicionTextoDateOpened.getPosXInicial(),anchoColumnaCuatro.intValue()).toLowerCase();

		String columnaCinco = utilidades.getColumnaPDF(document,(int)posicionTextoStatus.getPosXInicial(),anchoColumnaCuatro.intValue()).toLowerCase();

		int posicionInicioBusquedaTagColumnaUno = 0;
		int posicionInicialCuentaColumnaUno = 0;
		int posicionFinalCuentaUno = 0;

		int posicionInicioBusquedaTagColumnaDos = 0;
		int posicionInicialCuentaColumnaDos = 0;
		int posicionFinalCuentaDos = 0;

		int posicionInicioBusquedaTagColumnaTres = 0;
		int posicionInicialCuentaColumnaTres = 0;
		int posicionFinalCuentaTres = 0;

		int posicionInicioBusquedaTagColumnaCuatro = 0;
		int posicionInicialCuentaColumnaCuatro = 0;
		int posicionFinalCuentaCuatro = 0;

		int posicionInicioBusquedaTagColumnaCinco = 0;
		int posicionInicialCuentaColumnaCinco = 0;
		int posicionFinalCuentaCinco = 0;

		int posicionInicioBusquedaTagTextoCuenta = 0;
		int posicionInicialTextoCuenta = 0;
		int posicionFinalTextoCuenta = 0;


		do {

			//******************** Se inserta el balance historico *******************************************
			posicionInicialTextoCuenta = textoMinuscula.indexOf(patronExperian.getTagPrimerTagPatronUno(),posicionInicioBusquedaTagTextoCuenta);
			posicionFinalTextoCuenta = textoMinuscula.indexOf(patronExperian.getTagPrimerTagPatronUno(),
					(posicionInicialTextoCuenta + patronExperian.getTagPrimerTagPatronUno().length()));

			posicionFinalTextoCuenta = posicionFinalTextoCuenta > 0 ? posicionFinalTextoCuenta : textoMinuscula.length();

			//Este permite otener los de la primera columna
			posicionInicialCuentaColumnaUno = columnaUno.indexOf(patronExperian.getTagPrimerTagPatronUno(),posicionInicioBusquedaTagColumnaUno);
			posicionFinalCuentaUno = columnaUno.indexOf(patronExperian.getTagPrimerTagPatronUno(),
					(posicionInicialCuentaColumnaUno + patronExperian.getTagPrimerTagPatronUno().length()));

			posicionFinalCuentaUno = posicionFinalCuentaUno > 0 ? posicionFinalCuentaUno : columnaUno.length();


			//Este permite otener los de la segunda columna
			posicionInicialCuentaColumnaDos = columnaDos.indexOf(elementos[1],posicionInicioBusquedaTagColumnaDos);
			posicionFinalCuentaDos = columnaDos.indexOf(elementos[1],
					(posicionInicialCuentaColumnaDos + elementos[1].length()));

			posicionFinalCuentaDos = posicionFinalCuentaDos > 0 ? posicionFinalCuentaDos : columnaDos.length();

			//Este permite otener los de la tercera columna
			posicionInicialCuentaColumnaTres = columnaTres.indexOf(elementos[2],posicionInicioBusquedaTagColumnaTres);
			posicionFinalCuentaTres = columnaTres.indexOf(elementos[2],
					(posicionInicialCuentaColumnaTres + elementos[2].length()));

			posicionFinalCuentaTres = posicionFinalCuentaTres > 0 ? posicionFinalCuentaTres : columnaTres.length();


			//Este permite otener los de la cuarta columna
			posicionInicialCuentaColumnaCuatro = columnaCuatro.indexOf(elementos[3],posicionInicioBusquedaTagColumnaCuatro);
			posicionFinalCuentaCuatro = columnaCuatro.indexOf(elementos[3],
					(posicionInicialCuentaColumnaCuatro + elementos[3].length()));

			posicionFinalCuentaCuatro = posicionFinalCuentaCuatro > 0 ? posicionFinalCuentaCuatro : columnaCuatro.length();

			//Este permite otener los de la quinta columna
			posicionInicialCuentaColumnaCinco = columnaCinco.indexOf(elementos[4],posicionInicioBusquedaTagColumnaCinco);
			posicionFinalCuentaCinco = columnaCinco.indexOf(elementos[4],
					(posicionInicialCuentaColumnaCinco + elementos[4].length()));

			posicionFinalCuentaCinco = posicionFinalCuentaCinco > 0 ? posicionFinalCuentaCinco : columnaCinco.length();


			if (posicionInicialCuentaColumnaUno > 0) {

				String textoCuenta = textoMinuscula.substring(posicionInicialTextoCuenta,posicionFinalTextoCuenta);


				//Se obtiene los datos de la columna una
				String textoCuentaColumnaUno = columnaUno.substring(posicionInicialCuentaColumnaUno,posicionFinalCuentaUno);
				String accountName = utilidades.getDatoVertical(textoCuentaColumnaUno,elementos[0],2);

				//Se obtiene los datos de la columna dos
				String textoCuentaColumnaDos = columnaDos.substring(posicionInicialCuentaColumnaDos,posicionFinalCuentaDos);
				String accountNumber = utilidades.getDatoVertical(textoCuentaColumnaDos,elementos[1]);
				String accountType = utilidades.getDatoVertical(textoCuentaColumnaDos,elementos[6]);
				String terms = utilidades.getDatoVertical(textoCuentaColumnaDos,elementos[10]);

				//Se obtiene los datos de la columna tres
				String textoCuentaColumnaTres = columnaTres.substring(posicionInicialCuentaColumnaTres,posicionFinalCuentaTres);
				String balance = utilidades.getDatoVertical(textoCuentaColumnaTres,elementos[2],2);
				String limit = utilidades.getDatoVertical(textoCuentaColumnaTres,elementos[7],3);
				String highestBalance = utilidades.getDatoVertical(textoCuentaColumnaTres,elementos[9]);
				String monthlyPayment = utilidades.getDatoVertical(textoCuentaColumnaTres,elementos[12]);
				String paymentReceived = utilidades.getDatoVertical(textoCuentaColumnaTres,elementos[14]);

				//Se obtiene los datos de la columna tres
				String textoCuentaColumnaCuatro = columnaCuatro.substring(posicionInicialCuentaColumnaCuatro,posicionFinalCuentaCuatro);
				String dateOpened = utilidades.getFechaFormatoMesDiaAño(utilidades.getDatoVertical(textoCuentaColumnaCuatro,elementos[3]),"/",true);
				String statusUpdated = utilidades.getFechaFormatoMesDiaAño(utilidades.getDatoVertical(textoCuentaColumnaCuatro,elementos[8]),"/",true);
				String responsibility = utilidades.getDatoVertical(textoCuentaColumnaCuatro,elementos[10]);


				//Se obtiene los datos de la columna tres
				String textoCuentaColumnaCinco = columnaCinco.substring(posicionInicialCuentaColumnaCinco,posicionFinalCuentaCinco);
				String accountStatus = utilidades.getDatoVertical(textoCuentaColumnaCinco,elementos[4]);
				String comments = utilidades.getDatoVertical(textoCuentaColumnaCinco,elementos[11]);

				String paymentStatus = "";
				String balanceUpdated = "";
				String pastDueAmount = "";
				String yourStatement = "";
				String loanType = "";
				String dateClosed = "";
				String statusDetail = "";
				String originalCreditor = "";
				String lastActivity = "";
				String accounttypeone = "";
				String accounttypetwo = getTypeAccount(textosTiposCuentas, textoCuenta, tagFinales,tiposCuenta);
				String creditusage = "";
				String creditusagedescription = "";


				//******************************* Historico de balance **************************************

				Map<String, List<HistorialPago>> mesesCanceladosPorAno = new HashMap<String, List<HistorialPago>>();
				int posicionInicioBalanceHistorico = textoCuenta.indexOf("date: account balance");
				int posicionFinalBalanceHistorico = textoCuenta.indexOf("the original amount");

				if (posicionFinalBalanceHistorico < 0)
					posicionFinalBalanceHistorico = textoCuenta.indexOf("account history",posicionInicioBalanceHistorico);

				if (posicionFinalBalanceHistorico < 0)
					posicionFinalBalanceHistorico = textoCuenta.indexOf("between",posicionInicioBalanceHistorico);

				if (posicionFinalBalanceHistorico < 0)
					posicionFinalBalanceHistorico = textoCuenta.length();

				if (posicionInicioBalanceHistorico > 0 && posicionInicioBalanceHistorico < posicionFinalBalanceHistorico) {

					String balanceHistorico = textoCuenta.substring(posicionInicioBalanceHistorico,
							posicionFinalBalanceHistorico);
					balanceHistorico = balanceHistorico.replace(utilidades.getLineas(balanceHistorico, 1), "");

					String[] filasBalance = balanceHistorico.split("\n");

					for (String filas : filasBalance) {

						int posicionPunto = filas.indexOf(":");

						if (posicionPunto > 0) {

							String date = filas.substring(0,posicionPunto);
							String columnas[] = filas.substring(posicionPunto+1).split("/");
							String mes = date.substring(0,3);
							String año = date.substring(3);
							mesesCanceladosPorAno.put(año, new ArrayList<HistorialPago>());

							List<HistorialPago> meses = mesesCanceladosPorAno.get(año);
							meses.add(new HistorialPago(mes, año+"-"+utilidades.getNumeroMes(mes), "balance", columnas[0].trim()));
							meses.add(new HistorialPago(mes, año+"-"+utilidades.getNumeroMes(mes), "payment received", columnas[1].trim()));
							meses.add(new HistorialPago(mes, año+"-"+utilidades.getNumeroMes(mes), "scheduled payment amount", columnas[2].trim()));
							meses.add(new HistorialPago(mes, año+"-"+utilidades.getNumeroMes(mes), "actual amount paid", columnas[3].trim()));

						}
					}
				}


				posicionInicioBusquedaTagColumnaUno = posicionFinalCuentaUno;
				posicionInicioBusquedaTagColumnaDos = posicionFinalCuentaDos;
				posicionInicioBusquedaTagColumnaTres = posicionFinalCuentaTres;
				posicionInicioBusquedaTagColumnaCuatro = posicionFinalCuentaCuatro;
				posicionInicioBusquedaTagColumnaCinco = posicionFinalCuentaCinco;
				posicionInicioBusquedaTagTextoCuenta = posicionFinalTextoCuenta;

				CuentaReporte cuentaReporte = new CuentaReporte(accountName, accountNumber,
						accountType, accountStatus, paymentStatus, statusUpdated, balance, balanceUpdated, limit,
						monthlyPayment, pastDueAmount, highestBalance, terms, responsibility, yourStatement,
						comments, dateOpened, loanType, dateClosed, statusDetail, paymentReceived, originalCreditor,
						lastActivity, accounttypeone, accounttypetwo, creditusage, creditusagedescription,
						mesesCanceladosPorAno);

				cuentasReporte.add(cuentaReporte);

			}

		} while (posicionInicialCuentaColumnaUno > 0);


		//*************** Se insertan las cuentas inquiries ********************************** 
		columnaUno = utilidades.getColumnaPDF(document,0,250).toLowerCase();
		columnaDos = utilidades.getColumnaPDF(document,250,500).toLowerCase();

		int posicionInicialInquiriesColumnaUna = columnaUno.indexOf("credit history");
		int posicionInicialInquiriesColumnaDos = columnaDos.indexOf("credit inquiries");
		int posicionFinalInquiriesColumnaUna = 0;
		int posicionFinalInquiriesColumnaDos = 0;


		if (posicionInicialInquiriesColumnaUna > 0  && posicionInicialInquiriesColumnaDos> 0) {

			columnaUno = columnaUno.substring(posicionInicialInquiriesColumnaUna);
			columnaDos = columnaDos.substring(posicionInicialInquiriesColumnaDos);

			posicionInicialInquiriesColumnaUna = 0;
			posicionInicialInquiriesColumnaDos = 0;

			do {

				posicionInicialInquiriesColumnaUna = columnaUno.indexOf(patronExperian.getTagAccountName(),posicionInicialInquiriesColumnaUna);
				posicionFinalInquiriesColumnaUna = columnaUno.indexOf(patronExperian.getTagAccountName(),posicionInicialInquiriesColumnaUna+10);

				posicionInicialInquiriesColumnaDos = columnaDos.indexOf(patronExperian.getTagAccountDateReport(),posicionInicialInquiriesColumnaDos);
				posicionFinalInquiriesColumnaDos = columnaDos.indexOf(patronExperian.getTagAccountDateReport(),posicionInicialInquiriesColumnaDos+10);

				if (posicionFinalInquiriesColumnaUna < 0)

					posicionFinalInquiriesColumnaUna = columnaUno.length();

				if (posicionFinalInquiriesColumnaDos < 0)

					posicionFinalInquiriesColumnaDos = columnaDos.length();

				if (posicionInicialInquiriesColumnaUna > 0 && posicionInicialInquiriesColumnaUna < posicionFinalInquiriesColumnaUna &&
						posicionInicialInquiriesColumnaDos > 0 && posicionInicialInquiriesColumnaDos < posicionFinalInquiriesColumnaDos) {


					String accountInquiriesColumnaUno = columnaUno.substring(posicionInicialInquiriesColumnaUna,posicionFinalInquiriesColumnaUna);
					String accountInquiriesColumnaDos = columnaDos.substring(posicionInicialInquiriesColumnaDos,posicionFinalInquiriesColumnaDos);

					String accountName = utilidades.getDatoVertical(accountInquiriesColumnaUno, patronExperian.getTagAccountName()).replaceAll(name, "");

					int posicion = accountInquiriesColumnaUno.indexOf(accountName);
					String contactInformation = "";

					if (posicion>0)
						contactInformation = accountInquiriesColumnaUno.substring(posicion).replace(accountName, "").replaceAll("\r\n", "");

					String requestedon = utilidades.getDatoVertical(accountInquiriesColumnaDos, patronExperian.getTagAccountDateReport());
					String comments = utilidades.getDatoVertical(accountInquiriesColumnaDos, "comments");
					String inquieresType = "inquiries";

					cuentasInquieries.add(new Inquiery(accountName, requestedon, "", "", 
							contactInformation, inquieresType,comments));


				}

				posicionInicialInquiriesColumnaUna = posicionFinalInquiriesColumnaUna +10 ;
				posicionInicialInquiriesColumnaDos = posicionFinalInquiriesColumnaDos +10 ;

			} while (posicionFinalInquiriesColumnaUna < columnaUno.length());


		}


		return generarJSON(reporte,cuentasReporte,cuentasInquieries,null);	
	}


	/*
	 *  Scraper experian patron dos
	 * @param archivo
	 * @param textoMinuscula
	 * @throws IOException
	 */
	private String scrapearExperianPatronDos(String textoMinuscula, PDDocument document,boolean poseeCuentas) throws IOException {

		String[] elementos = patronExperian.getElementosAContenerTextoRepetidamentePatronUno();
		String[] tiposCuenta = {"adverse","satisfactory","inquiries","promocionalinquiries","reviewinquiries"};
		String textoPublic = textoMinuscula;


		List<CuentaReporte> cuentasReporte = new ArrayList<CuentaReporte>();
		List<Inquiery> cuentasInquieries = new ArrayList<Inquiery>();
		List<PublicAccount> publicAccounts = new ArrayList<PublicAccount>();

		String ssn = getSSN(textoMinuscula);
		String name = utilidades.getDatoVertical(textoMinuscula, "names:");
		String employeer[] = new String[]{""};
		String opencreditcards = "";
		String openretailcards = "";
		String openrealrstateloans = "";
		String openinstallmentloans = "";
		String totalopenaccounts = "";
		String accountseverlate = "";
		String collectionsaccounts = "";
		String averageaccountage = "";
		String oldestaccount = "";
		String newestaccount = "";
		String creditdebt = "";
		String totalcredit = "";
		String creditandretailcarddebt = "";
		String realestatedebt = "";
		String installmentloansdebt = "";
		String collectionsdebt = "";
		String totaldebt = "";
		String myhardcreditinquiries = "";
		String mypublicrecords = "";
		String creditscore = "";

		String dateofreport = utilidades.getFechaFormatoMesDiaAño(utilidades.getDatoVertical(textoMinuscula, "report date").
				                                           replaceAll("your report number is", "").trim());
		
		String address[] = getAddressExperianPatronDos(textoMinuscula,"address:");
		String birthyear = utilidades.getDatoVertical(textoMinuscula,"year of birth");
		String overallcreditusage = "";

		//Se crea el reporte que se va ha insertar
		Reporte reporte = new Reporte( ssn, name, patronExperian.getCra(), employeer, opencreditcards, openretailcards, 
				openrealrstateloans, openinstallmentloans, totalopenaccounts, accountseverlate, 
				collectionsaccounts, averageaccountage, oldestaccount, newestaccount,
				myhardcreditinquiries, creditdebt, totalcredit, creditandretailcarddebt, 
				realestatedebt, installmentloansdebt, collectionsdebt, totaldebt, mypublicrecords, 
				dateofreport, creditscore, address, birthyear, overallcreditusage,"PatronDos");



		if (poseeCuentas) {

			//************************************** Se buscan los datos de la cuenta *****************************************************
			String[] tagFinales = {"credit items","accounts in good standing"};
			int posicionInicialAdverseAccount = textoMinuscula.indexOf(tagFinales[0]);
			int posicionInicialSatisfactoryAccount = textoMinuscula.indexOf(tagFinales[1],posicionInicialAdverseAccount);

			int[] posicionesInicialesTiposCuentas = new int[] {posicionInicialAdverseAccount,posicionInicialSatisfactoryAccount};

			String[] textosTiposCuentas = new String[2];
			textosTiposCuentas[0] = posicionInicialAdverseAccount < 0 ? "" : getFormatearTexto(textoMinuscula.substring(posicionInicialAdverseAccount,utilidades.getPrimerValorPositiva(posicionesInicialesTiposCuentas,0,textoMinuscula.length())));
			textosTiposCuentas[1] = posicionInicialSatisfactoryAccount < 0 ? "" : getFormatearTexto(textoMinuscula.substring(posicionInicialSatisfactoryAccount,utilidades.getPrimerValorPositiva(posicionesInicialesTiposCuentas,1,textoMinuscula.length())));

			int posicionInicialCuenta = 0;
			int posicionFinalCuenta = 0;
			int posicionInicialCuentaTag = posicionInicialAdverseAccount;

			//Se define el limite del documento
			int posicionFinalDocumento =  textoMinuscula.lastIndexOf("personal information");

			if (posicionFinalDocumento > 600)

				textoMinuscula = textoMinuscula.substring(0,posicionFinalDocumento);

			do {
				String accountNumber = "";

				posicionInicialCuenta = textoMinuscula.indexOf("address identification number:",posicionInicialCuentaTag);
				posicionFinalCuenta = textoMinuscula.indexOf("address identification number:",
						(posicionInicialCuenta + 10)) - 20;
				posicionInicialCuenta = textoMinuscula.lastIndexOf("address:",posicionInicialCuenta);
				posicionFinalCuenta = textoMinuscula.lastIndexOf("address:",posicionFinalCuenta);


				posicionFinalCuenta = posicionFinalCuenta > 0 ? posicionFinalCuenta : textoMinuscula.length();
				posicionInicialCuentaTag = posicionFinalCuenta;

				if (posicionInicialCuenta > 0) {

					String textoCuenta = textoMinuscula.substring(posicionInicialCuenta,posicionFinalCuenta);

					Matcher matcher = utilidades.getPatronNumeroCuentaReporteExperian().matcher(textoCuenta);

					if (matcher.find()) 
						accountNumber = matcher.group();


					if (accountNumber.isEmpty()) 

						accountNumber = utilidades.getDatoVertical(textoCuenta, "number:");


					int posicionFinalAccountName = textoMinuscula.lastIndexOf('\n',textoMinuscula.indexOf(accountNumber));
					posicionFinalAccountName = textoMinuscula.lastIndexOf("address",posicionFinalAccountName);
					int posicionInicialAccountName = textoMinuscula.lastIndexOf('\n',posicionFinalAccountName-2);


					String accountName = "";

					if (posicionInicialAccountName>=0 && posicionFinalAccountName > posicionInicialAccountName) 
						accountName = utilidades.eliminarRetornosCarro(textoMinuscula.substring(posicionInicialAccountName,posicionFinalAccountName));


					String accountStatus = utilidades.getDatoVertical(textoCuenta, "status");
					String statusDetail = utilidades.getDatoVertical(textoCuenta, "status details");
					String dateOpened = utilidades.getFechaFormatoMesDiaAño(utilidades.getDatoVertical(textoCuenta,elementos[3]),"/",true);
					String accountType = utilidades.getDatoVertical(textoCuenta,elementos[6]);
					String statusUpdated = utilidades.getFechaFormatoMesDiaAño(utilidades.getDatoVertical(textoCuenta,elementos[8]),"/",true);
					String paymentReceived = utilidades.getDatoVertical(textoCuenta,elementos[14]);

					String balance = utilidades.getDatoVertical(textoCuenta,elementos[2],2);

					String limit = utilidades.getDatoVertical(textoCuenta,"credit limit/original amount");

					String highestBalance = utilidades.getDatoVertical(textoCuenta,elementos[9]);

					String monthlyPayment = utilidades.getDatoVertical(textoCuenta,"monthly payment");
					String responsibility = utilidades.getDatoVertical(textoCuenta,elementos[10]);
					String terms = utilidades.getDatoVertical(textoCuenta,elementos[10]);

					String paymentStatus = "";
					String balanceUpdated = "";
					String pastDueAmount = "";
					String yourStatement = "";
					String loanType = "";
					String dateClosed = "";
					String originalCreditor = "";
					String comments = "";
					String lastActivity = "";
					String accounttypeone = "";
					String accounttypetwo = getTypeAccount(textosTiposCuentas,textoCuenta,tagFinales,tiposCuenta);
					String creditusage = "";
					String creditusagedescription = "";



					//******************************* Historico de balance **************************************
					Map<String, List<HistorialPago>> mesesCanceladosPorAno = new HashMap<String, List<HistorialPago>>();
					int posicionInicioBalanceHistorico = textoCuenta.indexOf("account balance");
					int posicionFinalBalanceHistorico = textoCuenta.indexOf("the original amount");

					if (posicionFinalBalanceHistorico < 0)
						posicionFinalBalanceHistorico = textoCuenta.indexOf("account history",posicionInicioBalanceHistorico);

					if (posicionFinalBalanceHistorico < 0)
						posicionFinalBalanceHistorico = textoCuenta.indexOf("between",posicionInicioBalanceHistorico);

					if (posicionFinalBalanceHistorico < 0)
						posicionFinalBalanceHistorico = textoCuenta.length();

					if (posicionInicioBalanceHistorico > 0 && posicionInicioBalanceHistorico < posicionFinalBalanceHistorico) {
						String balanceHistorico = textoCuenta.substring(posicionInicioBalanceHistorico,
								posicionFinalBalanceHistorico);
						balanceHistorico = balanceHistorico.replace(utilidades.getLineas(balanceHistorico, 1), "");


						String[] filasBalance = balanceHistorico.split("\n");

						for (String filas : filasBalance) {

							int posicionPunto = filas.indexOf(":");

							if (posicionPunto > 0) {

								String date = filas.substring(0,posicionPunto);
								String columnas[] = filas.substring(posicionPunto+1).split("/");
								String mes = date.substring(0,3);
								String año = date.substring(3);

								if (columnas.length == 4) {

									mesesCanceladosPorAno.put(año, new ArrayList<HistorialPago>());

									List<HistorialPago> meses = mesesCanceladosPorAno.get(año);
									meses.add(new HistorialPago(mes, año+"-"+utilidades.getNumeroMes(mes), "balance", columnas[0].trim()));
									meses.add(new HistorialPago(mes, año+"-"+utilidades.getNumeroMes(mes), "payment received", columnas[1].trim()));
									meses.add(new HistorialPago(mes, año+"-"+utilidades.getNumeroMes(mes), "scheduled payment amount", columnas[2].trim()));
									meses.add(new HistorialPago(mes, año+"-"+utilidades.getNumeroMes(mes), "actual amount paid", columnas[3].trim()));

								}

							}
						}
					}

					CuentaReporte cuentaReporte = new CuentaReporte(accountName, accountNumber,
							accountType, accountStatus, paymentStatus, statusUpdated, balance, balanceUpdated, limit,
							monthlyPayment, pastDueAmount, highestBalance, terms, responsibility, yourStatement,
							comments, dateOpened, loanType, dateClosed, statusDetail, paymentReceived, originalCreditor,
							lastActivity, accounttypeone, accounttypetwo, creditusage, creditusagedescription,
							mesesCanceladosPorAno);

					cuentasReporte.add(cuentaReporte);

				}

			} while (posicionInicialCuenta>0);


		}
		//************************************************************************************************************************



		//*************** Se insertan las cuentas inquiries ********************************** 
		String columnaUno = utilidades.getColumnaPDF(document,0,250).toLowerCase();
		String columnaDos = utilidades.getColumnaPDF(document,250,500).toLowerCase();

		int posicionInicialInquiriesColumnaUna = columnaUno.indexOf("inquiries");
		int posicionInicialInquiriesColumnaDos = columnaDos.indexOf("date of request");
		int posicionFinalInquiriesColumnaUna = 0;
		int posicionFinalInquiriesColumnaDos = 0;

		if (posicionInicialInquiriesColumnaUna > 0  && posicionInicialInquiriesColumnaDos> 0) {

			int posicion = columnaUno.indexOf("personal",posicionInicialInquiriesColumnaUna);

			if (posicion < 0)

				posicion = columnaUno.indexOf("the following information is reported",posicionInicialInquiriesColumnaUna);


			columnaUno = columnaUno.substring(posicionInicialInquiriesColumnaUna,posicion);
			columnaDos = columnaDos.substring(posicionInicialInquiriesColumnaDos);

			posicionInicialInquiriesColumnaUna = 0;
			posicionInicialInquiriesColumnaDos = 0;

			do {

				posicionInicialInquiriesColumnaUna = columnaUno.indexOf(patronExperian.getTagAddress(),posicionInicialInquiriesColumnaUna);
				posicionFinalInquiriesColumnaUna = columnaUno.indexOf(patronExperian.getTagAddress(),posicionInicialInquiriesColumnaUna+10);

				posicionInicialInquiriesColumnaDos = columnaDos.indexOf(patronExperian.getTagAccountDateReportPatronDos(),posicionInicialInquiriesColumnaDos);
				posicionFinalInquiriesColumnaDos = columnaDos.indexOf(patronExperian.getTagAccountDateReportPatronDos(),posicionInicialInquiriesColumnaDos+10);

				if (posicionFinalInquiriesColumnaUna < 0)

					posicionFinalInquiriesColumnaUna = columnaUno.length();

				if (posicionFinalInquiriesColumnaDos < 0)

					posicionFinalInquiriesColumnaDos = columnaDos.length();

				if (posicionInicialInquiriesColumnaUna >= 0 && posicionInicialInquiriesColumnaUna < posicionFinalInquiriesColumnaUna &&
						posicionInicialInquiriesColumnaDos >= 0 && posicionInicialInquiriesColumnaDos < posicionFinalInquiriesColumnaDos) {

					String accountInquiriesColumnaUno = columnaUno.substring(posicionInicialInquiriesColumnaUna,posicionFinalInquiriesColumnaUna);
					String accountInquiriesColumnaDos = columnaDos.substring(posicionInicialInquiriesColumnaDos,posicionFinalInquiriesColumnaDos);


					int posFinal = columnaUno.lastIndexOf("\n",columnaUno.indexOf(accountInquiriesColumnaUno));
					int posInicial = columnaUno.lastIndexOf("\n",posFinal -1);

					String accountName = columnaUno.substring(posInicial,posFinal).replaceAll("\r", "").replaceAll("\n", "");
					String contactInformation = accountInquiriesColumnaUno.replaceAll("address:", "").replaceAll("\r", "").replaceAll("\n", "").trim();
					String requestedon = utilidades.getDatoVertical(accountInquiriesColumnaDos, patronExperian.getTagAccountDateReportPatronDos());
					String comments = utilidades.getDatoVertical(accountInquiriesColumnaDos, "comments");
					String inquieresType = "inquiries";

					cuentasInquieries.add(new Inquiery(accountName, requestedon, "", "", 
							contactInformation, inquieresType,comments));

				}


				if (posicionInicialInquiriesColumnaUna > 0 && posicionInicialInquiriesColumnaDos > 1) {
					posicionInicialInquiriesColumnaUna = posicionFinalInquiriesColumnaUna;
					posicionInicialInquiriesColumnaDos = posicionFinalInquiriesColumnaDos;
				}

			} while (posicionFinalInquiriesColumnaUna < columnaUno.length() && posicionInicialInquiriesColumnaDos > 0  && posicionInicialInquiriesColumnaUna > 0);


			//******************************** Se buscan las cuentas public ******************************************************
			int posicionInicialPublicRecords = textoPublic.indexOf("credit grantors may");


			if (posicionInicialPublicRecords > 0) {

				int posicionFinalPublicRecords = textoPublic.indexOf("credit items");

				if (posicionFinalPublicRecords < 0)

					posicionFinalPublicRecords = textoPublic.indexOf("inquiries share");

				if (posicionFinalPublicRecords < 0)

					posicionFinalPublicRecords = textoPublic.indexOf("the following information is reported to");


				if (posicionInicialPublicRecords < posicionFinalPublicRecords) {

					textoPublic = textoPublic.substring(posicionInicialPublicRecords,posicionFinalPublicRecords);

					int posicionInicialCuenta = 0;
					int posicionFinalCuenta = 0;

					do {

						posicionInicialCuenta = textoPublic.indexOf("address:",posicionInicialCuenta);
						posicionFinalCuenta = textoPublic.indexOf("address:",posicionInicialCuenta + 8);

						if (posicionInicialCuenta > 0 ) {

							if (posicionFinalCuenta < 0)

								posicionFinalCuenta = textoPublic.length();

							String textoCuenta = textoPublic.substring(posicionInicialCuenta,posicionFinalCuenta);

							int posicionFinal = textoCuenta.indexOf("identification\r\number:");

							if (posicionFinal < 0)

								posicionFinal = textoCuenta.indexOf("identification number:");


							String adddres = textoCuenta.substring(0,posicionFinal);
							String accountName = getNombreCuentaPublicPatronDos(textoPublic,adddres);
							String identicationNumber = utilidades.getDatoVertical(textoCuenta, "number:");
							String status = utilidades.getCadenaRecortada(utilidades.getDatoHorizontal(textoCuenta,"status"),"details");
							String statusDetails = getStatusDetailsPatronDos(textoCuenta,"status details") ;
							String dateResolved = utilidades.getDatoVertical(textoCuenta,"date resolved");
							String responsibility = utilidades.getDatoVertical(textoCuenta,"responsibility");
							String liabilityAmount = utilidades.getDatoVertical(textoCuenta,"liability amount");
							String dateFiled = utilidades.getLineaAnterior(textoCuenta,"date resolved");

							publicAccounts.add(new PublicAccount(accountName, "", "", 
									identicationNumber, "", ""));


							posicionInicialCuenta = posicionFinalCuenta + 8;

						}


					} while (posicionInicialCuenta > 0);


				}

			}

		}

		return generarJSON(reporte, cuentasReporte, cuentasInquieries, publicAccounts);

	}



	/**
	 * Scraper experian patron cinco 
	 * @param archivo
	 * @param textoMinuscula
	 */
	private String scrapearExperianPatronCinco(String textoMinuscula, String textoConFormatoMiniscula,PDDocument document,boolean poseeCuentas) {

		String[] elementos = patronExperian.getElementosAContenerTextoRepetidamentePatronCinco();

		List<CuentaReporte> cuentasReporte = new ArrayList<CuentaReporte>();
		List<Inquiery> cuentasInquieries = new ArrayList<Inquiery>();


		String ssn = getSSN(textoMinuscula);
		String name = utilidades.getDatoVertical(textoMinuscula,"prepared for");
		String employeer[] = new String[]{""};
		String opencreditcards = "";
		String openretailcards = "";
		String openrealrstateloans = "";
		String openinstallmentloans = "";
		String totalopenaccounts = "";
		String accountseverlate = "";
		String collectionsaccounts = "";
		String averageaccountage = "";
		String oldestaccount = "";
		String newestaccount = "";
		String creditdebt = "";
		String totalcredit = "";
		String creditandretailcarddebt = "";
		String realestatedebt = "";
		String installmentloansdebt = "";
		String collectionsdebt = "";
		String totaldebt = "";
		String myhardcreditinquiries = "";
		String mypublicrecords = "";
		String creditscore = "";

		String dateofreport = utilidades.getFechaFormatoMesDiaAño(utilidades.getDatoHorizontal(textoMinuscula, "date generated"));
		String address[] = getAddressExperianPatronCinco(textoMinuscula,patronExperian.getTagAddressPatronCinco());
		String birthyear = utilidades.getLineaAnterior(textoMinuscula,"year of birth");
		String overallcreditusage = "";

		//Se crea el reporte que se va ha insertar
		Reporte reporte = new Reporte(ssn, name, patronExperian.getCra(), employeer, opencreditcards, openretailcards, 
				openrealrstateloans, openinstallmentloans, totalopenaccounts, accountseverlate, 
				collectionsaccounts, averageaccountage, oldestaccount, newestaccount,
				myhardcreditinquiries, creditdebt, totalcredit, creditandretailcarddebt, 
				realestatedebt, installmentloansdebt, collectionsdebt, totaldebt, mypublicrecords, 
				dateofreport, creditscore, address, birthyear, overallcreditusage,"PatronCinco");



		if (poseeCuentas) {

			int posicionInicialCuenta = 0;
			int posicionFinalCuenta = 0;
			int posicionInicialCuentaTag = 0;


			do {

				posicionInicialCuenta = textoMinuscula.indexOf(elementos[0],posicionInicialCuentaTag);
				posicionFinalCuenta = textoMinuscula.indexOf(elementos[0],
						(posicionInicialCuenta + 10));

				posicionFinalCuenta = posicionFinalCuenta > 0 ? posicionFinalCuenta : textoMinuscula.length();
				posicionInicialCuentaTag = posicionFinalCuenta;

				if (posicionInicialCuenta > 0) {

					String textoCuenta = textoMinuscula.substring(posicionInicialCuenta,posicionFinalCuenta);

					String accountName = getAccountNamePatronCinco(textoCuenta,elementos[0]);
					String accountNumber = utilidades.getDatoHorizontal(textoCuenta,elementos[1]);
					String accountType =  utilidades.getDatoHorizontal(textoCuenta,elementos[2]);
					String accountStatus =  utilidades.getDatoHorizontal(textoCuenta,elementos[4]);

					String paymentStatus = "";
					String statusUpdated =  utilidades.getFechaFormatoMesDiaAño(utilidades.getDatoHorizontal(textoCuenta,elementos[7]),"/",true);
					String balance =    utilidades.getDatoHorizontal(textoCuenta,elementos[5]);
					String balanceUpdated =  utilidades.getFechaFormatoMesDiaAño(utilidades.getDatoHorizontal(textoCuenta,elementos[12]),"/",true);
					String limit =  utilidades.getDatoHorizontal(textoCuenta,elementos[6]);
					String monthlyPayment =  utilidades.getDatoHorizontal(textoCuenta,elementos[13]);
					String pastDueAmount = "";
					String highestBalance =  utilidades.getDatoHorizontal(textoCuenta,elementos[8]);

					String terms =  utilidades.getDatoHorizontal(textoCuenta,elementos[9]);
					String responsibility =  utilidades.getDatoHorizontal(textoCuenta,elementos[10]);
					String yourStatement = "";
					String comments = "";
					String dateOpened =  utilidades.getFechaFormatoMesDiaAño(utilidades.getDatoHorizontal(textoCuenta,elementos[3]),"/",true);
					String loanType = "";
					String dateClosed = "";
					String statusDetail = "";
					String paymentReceived =  utilidades.getDatoHorizontal(textoCuenta,elementos[11]);
					String originalCreditor = "";
					String lastActivity = "";
					String accounttypeone = "";
					String accounttypetwo = "";
					String creditusage = "";
					String creditusagedescription = "";



					//************************* Se obtiene el balance historico ***************************************************
					int posicionInicial = textoCuenta.lastIndexOf("balance history");
					int posicionFinal = textoCuenta.indexOf("paid on");


					Map<String, List<HistorialPago>> mesesCanceladosPorAno = new HashMap<String, List<HistorialPago>>();

					do {

						if (posicionInicial > 0 && posicionInicial < posicionFinal) {

							String balanceHistory = textoCuenta.substring(posicionInicial,posicionFinal);

							if (!balanceHistory.contains("account name") && !balanceHistory.contains("http")) {

								balanceHistory = balanceHistory.replace(utilidades.getLineas(balanceHistory, 1), "");
								String datePaidOn = textoCuenta.substring(posicionFinal,textoCuenta.indexOf('\n',posicionFinal));
								balanceHistory += datePaidOn;

								int posicionEspacioBlanco = balanceHistory.indexOf(' ');
								posicionEspacioBlanco = balanceHistory.indexOf(' ',posicionEspacioBlanco+1);


								String date = balanceHistory.substring(0,posicionEspacioBlanco+1);
								String balanceValue = balanceHistory.substring(posicionEspacioBlanco+1,balanceHistory.indexOf(' ',posicionEspacioBlanco+1));
								String scheduledPayment = utilidades.getLineaAnterior(balanceHistory, "scheduled payment");
								String paidOn = utilidades.getLineaAnterior(balanceHistory, "paid on");
								String mes = date.substring(0,3);
								String año = date.substring(3);
								mesesCanceladosPorAno.put(año, new ArrayList<HistorialPago>());


								List<HistorialPago> meses = mesesCanceladosPorAno.get(año);
								meses.add(new HistorialPago(mes, año+"-"+utilidades.getNumeroMes(mes), "scheduled payment", scheduledPayment));
								meses.add(new HistorialPago(mes, año+"-"+utilidades.getNumeroMes(mes), "balance", balanceValue));
								meses.add(new HistorialPago(mes, año+"-"+utilidades.getNumeroMes(mes), "paid on", paidOn));


							}

						}

						posicionInicial = posicionFinal;
						posicionFinal = textoCuenta.indexOf("paid on",posicionInicial+4);

					} while (posicionFinal > 0);

					CuentaReporte cuentaReporte = new CuentaReporte( accountName, accountNumber,
							accountType, accountStatus, paymentStatus, statusUpdated, balance, balanceUpdated, limit,
							monthlyPayment, pastDueAmount, highestBalance, terms, responsibility, yourStatement,
							comments, dateOpened, loanType, dateClosed, statusDetail, paymentReceived, originalCreditor,
							lastActivity, accounttypeone, accounttypetwo, creditusage, creditusagedescription,mesesCanceladosPorAno);

					cuentasReporte.add(cuentaReporte);
				}

			} while (posicionInicialCuenta>0);

		}

		//*************** Se insertan las cuentas inquiries ********************************** 
		String hardInquiries = "hard inquiries";
		String softInquiries = "soft inquiries";

		//Se obtienen las etiquetas
		Matcher hardInquiriesMatcher = utilidades.getPatronHardInquiries().matcher(textoConFormatoMiniscula.substring(1000));
		Matcher softInquiriesMatcher = utilidades.getPatronSoftInquiries().matcher(textoConFormatoMiniscula.substring(1000));

		if (hardInquiriesMatcher.find())
			hardInquiries = hardInquiriesMatcher.group();

		if (softInquiriesMatcher.find())
			softInquiries = softInquiriesMatcher.group();


		String[] tagFinales = {hardInquiries,softInquiries};
		int posicionInicialHardAccount = textoConFormatoMiniscula.lastIndexOf(tagFinales[0]);
		int posicionInicialSoftAccount = textoConFormatoMiniscula.lastIndexOf(tagFinales[1]);

		int[] posicionesInicialesTiposCuentas = new int[] {posicionInicialHardAccount,posicionInicialSoftAccount};

		String[] textosTiposCuentas = new String[2];
		textosTiposCuentas[0] = posicionInicialHardAccount < 0 ? "" : getFormatearTexto(textoConFormatoMiniscula.substring(posicionInicialHardAccount,utilidades.getPrimerValorPositiva(posicionesInicialesTiposCuentas,0,textoConFormatoMiniscula.length())));
		textosTiposCuentas[1] = posicionInicialSoftAccount < 0 ? "" : getFormatearTexto(textoConFormatoMiniscula.substring(posicionInicialSoftAccount,utilidades.getPrimerValorPositiva(posicionesInicialesTiposCuentas,1,textoConFormatoMiniscula.length())));

		textosTiposCuentas[0] = textosTiposCuentas[0].replaceAll("( ){2,}", " ");
		textosTiposCuentas[1] = textosTiposCuentas[1].replaceAll("( ){2,}", " ");


		int posicionInicialInquiries = textoMinuscula.lastIndexOf("account name");

		if (posicionInicialInquiries < 0 )

			posicionInicialInquiries = textoMinuscula.lastIndexOf("hard inquiries");

		if (posicionInicialInquiries < 0 )

			posicionInicialInquiries = textoMinuscula.lastIndexOf("soft inquiries");


		String textoCuentaInquiries = textoMinuscula.substring(posicionInicialInquiries);

		posicionInicialInquiries = 0;
		int posicionFinalInquiries = 0;


		posicionInicialInquiries = 0;

		do {

			posicionInicialInquiries = textoCuentaInquiries.indexOf(patronExperian.getTagInquiredOn(),posicionInicialInquiries);
			posicionFinalInquiries = textoCuentaInquiries.indexOf(patronExperian.getTagInquiredOn(),posicionInicialInquiries+10);

			if (posicionFinalInquiries < 0)

				posicionFinalInquiries = textoCuentaInquiries.length();

			if (posicionInicialInquiries >= 0 && posicionInicialInquiries < posicionFinalInquiries ) {

				String accountInquiriesColumnaUno = textoCuentaInquiries.substring(posicionInicialInquiries,posicionFinalInquiries);


				int posFinal = textoCuentaInquiries.lastIndexOf("\n",textoCuentaInquiries.indexOf(accountInquiriesColumnaUno));
				int posInicial = textoCuentaInquiries.lastIndexOf("\n",posFinal -1);

				String accountName = textoCuentaInquiries.substring(posInicial,posFinal).replaceAll("\r", "").replaceAll("\n", "").trim();
				String contactInformation = accountInquiriesColumnaUno.replaceAll("address:", "").replaceAll("\r", " ").replaceAll("\n", " ").
						replaceAll("inquired on", "").replaceAll(",", "").replaceFirst("and","");

				String inquieresType = textosTiposCuentas[0].contains(accountName) ? "hard inquiries" :
					textosTiposCuentas[1].contains(accountName) ? "soft inquiries" : "";	

				Matcher m = utilidades.getPatronDateInquiresOn().matcher(contactInformation);
				StringBuffer buffer = new StringBuffer();

				while (m.find()) { 

					String fecha = m.group();
					buffer.append(fecha).append(" ");
					contactInformation = contactInformation.replace(fecha, "");

				}

				String requestedOn = buffer.toString().trim();
				contactInformation = contactInformation.trim();


				cuentasInquieries.add(new Inquiery(accountName, requestedOn, "", "", 
						contactInformation, inquieresType,""));

			}


			if (posicionInicialInquiries > 0 ) {
				posicionInicialInquiries = posicionFinalInquiries ;

			}

		} while (posicionFinalInquiries < textoCuentaInquiries.length() && posicionInicialInquiries > 0);

		return generarJSON(reporte,cuentasReporte,cuentasInquieries,null);	
	}

	/*******************************************************************************************************
	 *************************************** Transunion ******************************************************
	 *******************************************************************************************************/

	/**
	 * Scraper transunion tipo Uno
	 * @param archivo
	 * @param textoMinuscula
	 * @throws IOException 
	 */
	private String scrapearTransunionPatronUno(String textoMinuscula, String textoMinusculaConFormato,PDDocument document, boolean poseeCuentas) throws IOException {

		PosicionTextoPDF posicionTextoPDF = new PosicionTextoPDF(document, 0, 1);


		String[] elementos = patronTransunion.getElementosAContenerTextoRepetidamente();
		String[] elementosReporte = patronTransunion.getElementosAdicionalesPatronGenerico();
		String[] tiposCuenta = {"adverse","satisfactory","inquiries","promocionalinquiries","reviewinquiries"};

		List<CuentaReporte> cuentasReporte = new ArrayList<CuentaReporte>();
		List<Inquiery> cuentasInquieries = new ArrayList<Inquiery>();


		//****************** Se definen los datos del reporte ****************************************************

		String ssn = getSSN(textoMinuscula);
		String name = utilidades.getDatoHorizontal(textoMinuscula,patronTransunion.getTagName());
		String employeer[] = new String[]{getNombreEmpleadorTransunion(posicionTextoPDF,document)};
		String opencreditcards = "";
		String openretailcards = "";
		String openrealrstateloans = "";
		String openinstallmentloans = "";
		String totalopenaccounts = "";
		String accountseverlate = "";
		String collectionsaccounts = "";
		String averageaccountage = "";
		String oldestaccount = "";
		String newestaccount = "";
		String creditdebt = "";
		String totalcredit = "";
		String creditandretailcarddebt = "";
		String realestatedebt = "";
		String installmentloansdebt = "";
		String collectionsdebt = "";
		String totaldebt = "";
		String myhardcreditinquiries = "";
		String mypublicrecords = "";
		String creditscore = "";

		String dateofreport = utilidades.getFechaFormatoMesDiaAño(utilidades.getDatoHorizontal(textoMinuscula,elementosReporte[1]),"/",true);
		String address[] = getAddressTransunionPatronUno(textoMinuscula,patronTransunion.getTagAddress());
		String birthyear = utilidades.getDatoHorizontal(textoMinuscula,elementosReporte[0]);
		String overallcreditusage = "";

		//Se crea el reporte que se va ha insertar
		Reporte reporte = new Reporte(ssn, name, patronTransunion.getCra(), employeer, opencreditcards, openretailcards, 
				openrealrstateloans, openinstallmentloans, totalopenaccounts, accountseverlate, 
				collectionsaccounts, averageaccountage, oldestaccount, newestaccount,
				myhardcreditinquiries, creditdebt, totalcredit, creditandretailcarddebt, 
				realestatedebt, installmentloansdebt, collectionsdebt, totaldebt, mypublicrecords, 
				dateofreport, creditscore, address, birthyear, overallcreditusage,"PatronUno");


		//****************** Se definen los datos de la cuenta ****************************************************

		String[] tagFinales = {"adverse information","the following accounts are reported with no adverse information",
				"regular inquiries","the companies listed below received your name, address and other limited information about",
		"the listing of a company's inquiry in this section means"};

		// Se divide el documento para poder clasificar los tipos de cuentas
		int posicionInicialAdverseAccount = textoMinuscula.indexOf(tagFinales[0]);
		int posicionInicialSatisfactoryAccount = textoMinuscula.indexOf(tagFinales[1]);
		int posicionInicialRegularInquiries = textoMinuscula.indexOf(tagFinales[2]);
		int posicionInicialPromotionalInquiries = textoMinuscula.indexOf(tagFinales[3]);
		int posicionInicialAccountReviewInquiries = textoMinuscula.indexOf(tagFinales[4]);

		if (posicionInicialAdverseAccount > posicionInicialSatisfactoryAccount) 

			posicionInicialAdverseAccount = -1;


		int[] posicionesInicialesTiposCuentas = new int[] {posicionInicialAdverseAccount,posicionInicialSatisfactoryAccount,posicionInicialRegularInquiries,
				posicionInicialPromotionalInquiries,posicionInicialAccountReviewInquiries};

		String[] textosTiposCuentas = new String[5];
		textosTiposCuentas[0] = posicionInicialAdverseAccount < 0 ? "" : getFormatearTexto(textoMinuscula.substring(posicionInicialAdverseAccount,utilidades.getPrimerValorPositiva(posicionesInicialesTiposCuentas,0,textoMinuscula.length())));
		textosTiposCuentas[1] = posicionInicialSatisfactoryAccount < 0 ? "" : getFormatearTexto(textoMinuscula.substring(posicionInicialSatisfactoryAccount,utilidades.getPrimerValorPositiva(posicionesInicialesTiposCuentas,1,textoMinuscula.length())));
		textosTiposCuentas[2] = posicionInicialRegularInquiries < 0 ? "" : getFormatearTexto(textoMinuscula.substring(posicionInicialRegularInquiries,utilidades.getPrimerValorPositiva(posicionesInicialesTiposCuentas,2,textoMinuscula.length())));
		textosTiposCuentas[3] = posicionInicialPromotionalInquiries < 0 ? "" : getFormatearTexto(textoMinuscula.substring(posicionInicialPromotionalInquiries,utilidades.getPrimerValorPositiva(posicionesInicialesTiposCuentas,3,textoMinuscula.length())));
		textosTiposCuentas[4] = posicionInicialAccountReviewInquiries < 0 ? "" : getFormatearTexto(textoMinuscula.substring(posicionInicialAccountReviewInquiries,utilidades.getPrimerValorPositiva(posicionesInicialesTiposCuentas,4,textoMinuscula.length()))).replace("certain collection companies may have access to other collection company inquiries, and users of a report for employment purposes may have access to other employment inquiries,\r\n" + 
				"where permitted by law).", "");


		if (poseeCuentas) {

			List<String> numeroCuentas = new ArrayList<String>();

			Matcher matcher = utilidades.getPatronNumeroCuentaReporte().matcher(textoMinuscula);

			//Se obtiene todos los codigos que esten presentes en el formato
			while (matcher.find()) {

				try {
					String accountNumber = matcher.group();
					numeroCuentas.add(accountNumber);
				} catch (Exception e) {}

			}

			int posicionInicial = 0;
			int posicionInicialConFormato = 0;

			//Se obtienen los datos de cada cuenta
			for (int i = 0; i < numeroCuentas.size(); i++) {


				posicionInicial = textoMinuscula.indexOf(numeroCuentas.get(i),posicionInicial);
				int posicionFinal = ((i+1)==numeroCuentas.size()) ? textoMinuscula.length() : 
					textoMinuscula.indexOf(numeroCuentas.get(i+1),posicionInicial + numeroCuentas.get(i).length() );

				posicionInicialConFormato  = textoMinusculaConFormato.indexOf(numeroCuentas.get(i),posicionInicialConFormato);
				int posicionFinalConFormato = ((i+1)==numeroCuentas.size()) ? textoMinusculaConFormato.length() : textoMinusculaConFormato.indexOf(numeroCuentas.get(i+1),posicionInicialConFormato + numeroCuentas.get(i).length());

				if (posicionInicialConFormato > 0 && posicionInicialConFormato < posicionFinalConFormato) {

					String textoCuenta = textoMinuscula.substring(posicionInicial,posicionFinal);
					String textoCuentaConFormato = textoMinusculaConFormato.substring(posicionInicialConFormato,posicionFinalConFormato);

					String accountName = getAccountNameTransunion(textoMinuscula, numeroCuentas.get(i));
					String accountNumber = numeroCuentas.get(i);

					String accountType = utilidades.getDatoHorizontal(textoCuenta,elementos[2]);
					String accountStatus = "";
					String paymentStatus = utilidades.getDatoHorizontal(textoCuenta,elementos[12]);
					String statusUpdated = utilidades.getDatoHorizontal(textoCuenta,elementos[5]);
					String balance = getBalanceTransunion(textoCuenta) ?  utilidades.getDatoHorizontal(textoCuenta,elementos[4]).replaceAll(":|$", "").trim() : "";
					String balanceUpdated = "";

					String limit = utilidades.getDatoHorizontal(textoCuenta,elementos[10]);
					String monthlyPayment = "";
					String pastDueAmount = utilidades.getDatoHorizontal(textoCuenta,elementos[11]);
					String highestBalance = utilidades.getDatoHorizontal(textoCuenta,elementos[8]);
					String terms = utilidades.getDatoHorizontal(textoCuenta,elementos[13]);
					String responsibility = utilidades.getDatoHorizontal(textoCuenta,elementos[1]);
					String yourStatement = "";

					String comments = "";
					String dateOpened = utilidades.getDatoHorizontal(textoCuenta,elementos[0]);
					String loanType = utilidades.getDatoHorizontal(textoCuenta,elementos[3]);
					String dateClosed = utilidades.getDatoHorizontal(textoCuenta,elementos[14]);
					String statusDetail = "";
					String paymentReceived = utilidades.getDatoHorizontal(textoCuenta,elementos[6]);
					String originalCreditor = utilidades.getDatoHorizontal(textoCuenta,elementos[9]);
					String lastActivity = "";
					String accounttypeone = "";
					String accounttypetwo = getTypeAccount(textosTiposCuentas,textoCuenta,tagFinales,tiposCuenta);
					String creditusage = "";
					String creditusagedescription = "";


					//************** Se obtiene los datos de la tabla de cada cuenta ************************************ 
					boolean sw = false; 
					String[] filas = textoCuentaConFormato.split("\n");

					List<String> filasTabla = new ArrayList<String>(); 
					List<List<String>> datosTabla = new ArrayList<List<String>>();

					for (String fila : filas) {

						matcher = utilidades.getPatronFechaFormatoReporte().matcher(fila);

						if (matcher.find()) 

							sw = true;

						if (sw) {

							if (!fila.trim().equalsIgnoreCase("payment") && !fila.trim().equalsIgnoreCase("remarks")) 
								filasTabla.add(fila);

							if (fila.contains("rating")) {

								sw = false; 
								datosTabla.add(filasTabla); 
								filasTabla = new ArrayList<String>();
							} 

						}


					}

					CuentaReporte cuentaReporte = new CuentaReporte(accountName, accountNumber,
							accountType, accountStatus, paymentStatus, statusUpdated, balance, balanceUpdated, limit,
							monthlyPayment, pastDueAmount, highestBalance, terms, responsibility, yourStatement,
							comments, dateOpened, loanType, dateClosed, statusDetail, paymentReceived, originalCreditor,
							lastActivity, accounttypeone, accounttypetwo, creditusage, creditusagedescription,null);

					cuentasReporte.add(cuentaReporte);

				}

				posicionInicial=posicionFinal;
				posicionInicialConFormato = posicionFinalConFormato;



			}

		}
		//*************** Se obtienen los datos del Inquiries datos ***********************************
		insertarInquieriesTransusionPatronUno(textosTiposCuentas[2],"inquiry type","regular inquiries",cuentasInquieries);
		insertarInquieriesTransusionPatronUno(textosTiposCuentas[3],"requested on","promotional inquiries",cuentasInquieries);
		insertarInquieriesTransusionPatronUno(textosTiposCuentas[4],"requested on","account review inquiries",cuentasInquieries);

		return generarJSON(reporte, cuentasReporte, cuentasInquieries, null);

	}


	//****************************************************************************************************
	//**************************************** Patron Generico ******************************************
	//****************************************************************************************************

	/**
	 * Se obtiene el patron en funcion a los datos del archivo.
	 * @param textoMinuscula
	 * @return Patron
	 */
	private String[] getPatron(String textoMinuscula) {

		String tipoPatron = "Ninguno";
		String numeroPatron = "Ninguno";

		if (textoMinuscula.length() > 20) {

			if ((textoMinuscula.contains(cra[0]) && utilidades.getNumeroOcurrenciasPalabra(textoMinuscula,cra[0]) > 3) &&
					(textoMinuscula.contains(cra[1]) && utilidades.getNumeroOcurrenciasPalabra(textoMinuscula,cra[1]) > 3) &&	
					(textoMinuscula.contains(cra[2]) && utilidades.getNumeroOcurrenciasPalabra(textoMinuscula,cra[2]) > 3)) {


				tipoPatron = "Todos";


			} else if ((textoMinuscula.contains(cra[0]) && utilidades.getNumeroOcurrenciasPalabra(textoMinuscula,cra[0]) > 3) &&
					(textoMinuscula.contains(cra[1]) && utilidades.getNumeroOcurrenciasPalabra(textoMinuscula,cra[1]) > 3)) { 

				tipoPatron = "PatronEquifaxExperian";


			} else if ((textoMinuscula.contains(cra[0]) && utilidades.getNumeroOcurrenciasPalabra(textoMinuscula,cra[0]) > 3) &&
					((textoMinuscula.contains(cra[2]) && utilidades.getNumeroOcurrenciasPalabra(textoMinuscula,cra[2]) > 3)) ) {

				tipoPatron = "PatronEquifaxTransunion";



			} else if ((textoMinuscula.contains(cra[1]) && utilidades.getNumeroOcurrenciasPalabra(textoMinuscula,cra[1]) > 3)   && 
					(textoMinuscula.contains(cra[2]) && utilidades.getNumeroOcurrenciasPalabra(textoMinuscula,cra[2]) > 3)) {

				tipoPatron = "PatronExperianTransunion";


			} else if (textoMinuscula.contains(cra[0]) && utilidades.getNumeroOcurrenciasPalabra(textoMinuscula,cra[0]) > 3) {

				numeroPatron = patronEquifax.getPatronEquifax(textoMinuscula);

				//Se verifica si contiene los datos para ser Equifax
				if (patronEquifax.isPatronEquifax(textoMinuscula,numeroPatron)) {

					tipoPatron = "PatronEquifax";

				} 

			} else if (textoMinuscula.contains(cra[1]) && utilidades.getNumeroOcurrenciasPalabra(textoMinuscula,cra[1]) > 3) {

				numeroPatron = patronExperian.getPatronExperian(textoMinuscula);

				//Se verifica si contiene los datos para ser Experian
				if (patronExperian.isPatronExperianAccount(textoMinuscula,numeroPatron)) 
					tipoPatron = "PatronExperian";

				else if (patronExperian.isPatronExperianSinAccount(textoMinuscula,numeroPatron))
					tipoPatron = "PatronExperianSinCuentas";


			} else if (textoMinuscula.contains(cra[2]) && utilidades.getNumeroOcurrenciasPalabra(textoMinuscula,cra[2]) >= 3) {

				numeroPatron = patronTransunion.getPatronTransunion(textoMinuscula);

				//Se verifica si contiene los datos para ser Transunion
				if (patronTransunion.isPatronTransunionAccount(textoMinuscula,numeroPatron)) 
					tipoPatron = "PatronTransunion";

				else if (patronTransunion.isPatronTransunionSinAccount(textoMinuscula,numeroPatron)) 
					tipoPatron = "PatronTransunionSinCuenta";

			}

		} else {

			tipoPatron = "No tiene datos";


		}
		/*
		 * Se verifica si el CRA detectado es un archivo tipo reporte
		 */


		return new String[]{tipoPatron,numeroPatron};

	}

	/**
	 * Devuelve el SSN que posee el reporte
	 * @param texto
	 * @return
	 */
	private String getSSN(String texto) {

		String SSN = "";

		try {

			Matcher matcher = utilidades.getPatronSSNReporte().matcher(texto); matcher.find();

			SSN = matcher.group();

		} catch (Exception e) {}

		return SSN;
	}

	/**
	 * Obtiene el tipo de cuenta 2
	 * @param textoCompleto
	 * @param textoCuenta
	 * @param tag
	 * @return
	 */
	private String getAccounttypeonePatronGenerico(String textoCompleto, String textoCuenta,String tag) {

		String resultado = "";

		int posicionFinalTexto = textoCompleto.indexOf(textoCuenta);
		posicionFinalTexto = textoCompleto.lastIndexOf(tag,posicionFinalTexto);

		int posicionInicialTexto = textoCompleto.lastIndexOf("\n",posicionFinalTexto-2);

		if (posicionInicialTexto >=0 && posicionInicialTexto < posicionFinalTexto) {

			resultado = textoCompleto.substring(posicionInicialTexto,posicionFinalTexto).replaceAll("\n", "").trim();

			if (resultado.contains("date") || resultado.contains("report"))

				resultado = "";

		}


		return resultado.replaceAll("( ){2,}", " ").replaceAll("\r\n", "");

	}

	/**
	 * Metodo que recupera el dato que se encuentra vertical
	 * @param textoMinuscula
	 * @param tag
	 * @return
	 */
	private String getCreditUsagePatronGenerico(String textoMinuscula,String tag) {

		String resultado = "";

		if (textoMinuscula.contains("credit usage")) {

			int posicionInicial = textoMinuscula.indexOf(tag);
			int posicionFinal = textoMinuscula.indexOf("\n",posicionInicial);
			posicionFinal  = textoMinuscula.indexOf("\n", posicionFinal+2);

			if (posicionInicial>= 0 && posicionFinal > posicionInicial)
				resultado = textoMinuscula.substring(posicionInicial,posicionFinal).replaceAll(tag+ "|\r|:", " ").
				replaceAll("\n","").trim();

		}


		return resultado;

	}

	/**
	 * Obiene el credit usage del formato generico
	 * @param textoMinuscula
	 * @param tag
	 * @return
	 */
	private String getCreditUsageDescriptionPatronGenerico(String textoMinuscula,String tag) {

		String resultado = "";

		int posicionInicialCreditUsage = textoMinuscula.indexOf(tag);
		int posicionFinalCreditUsage = textoMinuscula.indexOf("contact information");

		if (posicionFinalCreditUsage < 0) 

			posicionFinalCreditUsage = textoMinuscula.indexOf("payment history");

		if (posicionInicialCreditUsage > 0 && posicionInicialCreditUsage < posicionFinalCreditUsage) 

			resultado = textoMinuscula.substring(posicionInicialCreditUsage,posicionFinalCreditUsage);

		return resultado.replaceAll(tag+ "|\r|:", " ").
				replaceAll("\n","").trim();
	}

	/**
	 * Se obtiene el credit score para el patron generico
	 * @param textoMinuscula
	 * @param tag
	 * @return
	 */
	private String getCreditScorePatronGenerico(String textoMinuscula,String tag) {

		String resultado = "";
		String tag2 = "exceptional";
		int posicionInicial = textoMinuscula.indexOf(tag);
		posicionInicial = textoMinuscula.indexOf(tag2,posicionInicial);

		int posicionFinal = textoMinuscula.indexOf("\n",posicionInicial);
		posicionFinal  = textoMinuscula.indexOf("\n", posicionFinal+2);

		if (posicionInicial>= 0 && posicionFinal > posicionInicial)
			resultado = textoMinuscula.substring(posicionInicial,posicionFinal).
			replace(tag2, "").replaceAll("\r"," ").replaceAll(":", " ").
			replaceAll("\n","").trim();

		return resultado;

	}

	/**
	 * Se obtienen los datos de la dirección del patron generico
	 * @param textoMinuscula
	 * @param tag
	 * @param name
	 * @return
	 */
	private String[] getAddressPatronGenerico(String textoMinuscula,String tag, String name) {

		String resultado[] = null;

		int posicionInicialAddress =  textoMinuscula.indexOf(tag);
		int posicionFinalAddress = textoMinuscula.indexOf("employer(s)");

		if (posicionFinalAddress < 0) 

			posicionFinalAddress = textoMinuscula.indexOf(name);


		if (posicionInicialAddress > 0 && posicionInicialAddress < posicionFinalAddress) {

			String texto = textoMinuscula.substring(posicionInicialAddress,posicionFinalAddress).replaceAll(tag,"");


			String[] direccionesArray = texto.split("\n");
			StringBuffer direcciones = new StringBuffer();

			//las direccione tienen dos lineas
			for (int i = 0; i < direccionesArray.length; i++) {

				String linea = direccionesArray[i].replaceAll("\r", " ");

				if (linea.length() > 5) {

					direcciones.append(utilidades.eliminarRetornosCarro(linea));

					if (i%2 == 0)

						direcciones.append("\n");

				}

			}


			resultado = direcciones.toString().split("\n");

		}

		return resultado;

	}

	/**
	 * 
	 * @param textoMinuscula
	 * @param cra
	 * @return
	 */
	private String getUltimaLineaPatronGenerico(String textoMinuscula,String cra) {

		String resultado = "";

		textoMinuscula = textoMinuscula.replaceAll("\nno public records","").trim();

		int posicionInicialUltimaLinea = textoMinuscula.lastIndexOf("\n",textoMinuscula.length()-2);

		if (posicionInicialUltimaLinea >  0)

			resultado = textoMinuscula.substring(posicionInicialUltimaLinea).replaceAll("\n", "").trim();


		if (resultado.equals("public records") || resultado.equals("credit score")) {

			int posicionFinalUltimaLinea = textoMinuscula.indexOf(cra.toLowerCase());
			posicionFinalUltimaLinea = textoMinuscula.lastIndexOf("\n",posicionFinalUltimaLinea);
			posicionInicialUltimaLinea = textoMinuscula.lastIndexOf("\n",posicionFinalUltimaLinea-1);
			resultado = textoMinuscula.substring(posicionInicialUltimaLinea,posicionFinalUltimaLinea).replaceAll("\n", "").trim();;


		}

		return resultado;




	}


	/*****************************************************************************************************
	 **************************************** Metodos Patron EquiFax Dos *********************************
	 *****************************************************************************************************/

	private String[] getNombreEmpleadosEquifaxPatronDos(String textoMinuscula,String tag) {

		String[] resultado = null;
		int posicionInicialAddress = textoMinuscula.indexOf(tag);
		posicionInicialAddress = textoMinuscula.indexOf("company",posicionInicialAddress);

		if (posicionInicialAddress > 0) {
			int posicionFinalEmployer = textoMinuscula.indexOf("inquiries",posicionInicialAddress);

			if (posicionFinalEmployer > 0) {
				String textoAddress = textoMinuscula.substring(posicionInicialAddress,posicionFinalEmployer).replace("address status date reported", "");
				String tag2 = "current";

				posicionInicialAddress = 0;

				List<String> address = new ArrayList<String>();

				do {

					posicionFinalEmployer = textoAddress.indexOf(tag2,posicionInicialAddress);

					if (posicionFinalEmployer < 0) {

						tag2 = "former";
						posicionFinalEmployer = textoAddress.indexOf(tag2,posicionInicialAddress);

					}

					if (posicionInicialAddress >= 0 && posicionInicialAddress < posicionFinalEmployer) {

						posicionFinalEmployer += tag2.length();
						String employer = utilidades.eliminarRetornosCarro(textoAddress.substring(posicionInicialAddress,
								posicionFinalEmployer)).replace("company occupation start date status address", "").
								trim();

						textoAddress = textoAddress.substring(posicionFinalEmployer);
						address.add(utilidades.eliminarRetornosCarro(employer.replaceAll("employer(s)", "")));
						posicionInicialAddress = posicionFinalEmployer;

					} 


				} while (posicionFinalEmployer > 0);

				resultado = address.toArray(new String[address.size()]);
			}
		}

		return resultado;


	}

	private String[] getAddressEquifaxPatronDos(String textoMinuscula,String tag) {

		int posicionInicialAddress = textoMinuscula.indexOf("personal information");
		posicionInicialAddress = textoMinuscula.indexOf("address status",posicionInicialAddress);
		int posicionFinalAddress = textoMinuscula.indexOf("employment history");

		List<String> address = new ArrayList<String>();

		if (posicionInicialAddress>1 && posicionFinalAddress>posicionInicialAddress) {

			String textoAddress = textoMinuscula.substring(posicionInicialAddress,posicionFinalAddress).replace("address status date reported", "");


			posicionInicialAddress = 0;

			boolean sw = true;

			do {

				Matcher matcher = utilidades.getPatronDateReportPatronTres().matcher(textoAddress);
				sw = matcher.find();

				if (sw) {
					String fecha = matcher.group();
					posicionFinalAddress = textoAddress.indexOf(fecha,posicionInicialAddress);

					if (posicionInicialAddress >= 0 && posicionInicialAddress < posicionFinalAddress) {

						posicionFinalAddress +=  fecha.length();
						String direccion = utilidades.eliminarRetornosCarro(textoAddress.substring(posicionInicialAddress,posicionFinalAddress));
						textoAddress = textoAddress.substring(posicionFinalAddress);
						address.add(direccion);

					} else

						sw = false;

				}
			} while (sw);

		}

		return address.size() > 0 ? address.toArray(new String[address.size()]) : null;

	}

	private String getAccountNameEquifaxDos(String textoMinuscula,String textoCuenta) {

		String resultado = "";

		int posicionFinal = textoMinuscula.indexOf(textoCuenta);

		String texto = textoMinuscula.substring(0,posicionFinal);
		String numeral = "";

		Matcher matcher = utilidades.getPatronItemNumeral().matcher(texto);

		while (matcher.find())
			numeral = matcher.group();

		int posicionInicial = textoMinuscula.indexOf(numeral);

		if (posicionInicial > 0) {

			resultado = textoMinuscula.substring(posicionInicial);

			posicionFinal = resultado.indexOf("\n");

			resultado = resultado.substring(0, posicionFinal).replace(numeral, "").trim();
		}			

		return resultado;
	}

	private String getTypeAccountNamePatronDos(String accountName) {

		String resultado = "";

		Matcher matcher = utilidades.getPatronContenidoParentesis().matcher(accountName);

		if (matcher.find())

			resultado = matcher.group().replaceAll("\\(|\\)", "");

		return resultado;

	}

	/*****************************************************************************************************
	 **************************************** Metodos Patron Experian Uno *********************************
	 *****************************************************************************************************/

	private String[] getAddressExperianPatronUno(String textoMinuscula,String tag) {

		ArrayList<String> address = new ArrayList<String>();

		Pattern patronAddressIdentifactionNumber = Pattern.compile("\\d{6,}");

		int posicionInicialAddress = textoMinuscula.indexOf(tag);

		if (posicionInicialAddress < 0) {

			posicionInicialAddress = textoMinuscula.indexOf("address") + 11;
		}


		int posicionFinalAddress =  textoMinuscula.indexOf("year of birth");

		if (posicionFinalAddress < 0) 

			posicionFinalAddress =  textoMinuscula.indexOf("spouse");

		if (posicionInicialAddress > 0 && posicionInicialAddress < posicionFinalAddress) {

			String addressText = textoMinuscula.substring(posicionInicialAddress,posicionFinalAddress).
					replaceAll("address|name|identification|:|\r","").
					trim();

			String addressString[] = addressText.split("\n");

			StringBuffer direccion = null ;
			String linea = null;
			

			for (int i = 0; i < addressString.length; i++) {
				
				linea = addressString[i];
				
				if (!linea.isBlank() && !linea.contains("associated") && !linea.contains("number")) {

					Matcher matcher = patronAddressIdentifactionNumber.matcher(linea);

					if (matcher.find()) {

						if (direccion!=null && direccion.length() > 0) 

							address.add(utilidades.eliminarRetornosCarro(direccion.toString()));

						direccion = new StringBuffer();
						direccion.append(linea.replaceAll(matcher.group(), ""));


					} else 

						direccion.append(linea);

				}

			}
			
			address.add(utilidades.eliminarRetornosCarro(direccion.toString()));


		}

		return address.toArray(new String[address.size()]);


	}

	private String getFormatearTexto(String texto) {

		texto = texto.replaceAll("consumer initiated\r\ntransaction","consumer initiated transaction");
		texto = texto.replaceFirst("\n", "");
		texto = texto.substring(texto.indexOf("\n")+1);

		return texto;

	}

	private String[] getAddressTransunionPatronUno(String textoMinuscula,String tag) {

		String address[] = null;

		int posicionInicialAddress = textoMinuscula.indexOf(tag);
		int posicionFinalAddress =  textoMinuscula.indexOf("telephone numbers reported");

		if (posicionFinalAddress < 0) 

			posicionFinalAddress =  textoMinuscula.indexOf("employment data reported");

		if (posicionInicialAddress > 0 && posicionInicialAddress < posicionFinalAddress) {

			String addressText = utilidades.eliminarRetornosCarro(textoMinuscula.substring(posicionInicialAddress,posicionFinalAddress).
					replaceAll("addresses|address|date|reported|:|\r","").trim());

			address = addressText.split("\n");

		}

		return address;

	}

	/**
	 * Inserta los diferentes inquiries del patron uno de transunion
	 * @param datosInquieres
	 * @param tag
	 * @param type
	 * @param codigoReporte
	 */
	private void insertarInquieriesTransusionPatronUno(String datosInquieres, String tag,String type,List<Inquiery> cuentasInquieries) {

		int posicionInicialTag = 0;


		boolean sw = true;

		do {

			int posicionFinalTag = datosInquieres.indexOf(tag,posicionInicialTag);


			if (posicionFinalTag > 0) {

				posicionFinalTag = datosInquieres.indexOf("\n",posicionFinalTag);

				int posFinal = datosInquieres.indexOf("\n",posicionFinalTag+1) > 0 ? datosInquieres.indexOf("\n",posicionFinalTag+1) : datosInquieres.length();
				String permissiblePurpose = datosInquieres.substring(posicionFinalTag+1, posFinal).replaceAll("\n", "");
				String textoCuentaInquiries = datosInquieres.substring(posicionInicialTag,posicionFinalTag);



				String primeraLinea = utilidades.getLineas(textoCuentaInquiries, 1).trim().replaceAll("\r\n", "");

				if (primeraLinea.contains("permissible purpose")) 

					textoCuentaInquiries = textoCuentaInquiries.replaceAll(primeraLinea, "");

				textoCuentaInquiries += (permissiblePurpose.contains("permissible purpose") ? permissiblePurpose : "");
				textoCuentaInquiries = textoCuentaInquiries.replaceAll("(?m)^\\s*$[\n\r]{1,}", "");


				//Se obtienen los datos para insertas las cuentas inquieres
				String account = utilidades.getLineas(textoCuentaInquiries, 1);

				if (account.contains("page")) {

					account = utilidades.getLineas(textoCuentaInquiries, 2);
					account = account.substring(account.indexOf("\n")+1);
				}


				String requestedOn = utilidades.getDatoHorizontal(textoCuentaInquiries, "requested on");
				String inquieresType = utilidades.getDatoHorizontal(textoCuentaInquiries, "inquiry type");
				String permissible = utilidades.getDatoHorizontal(textoCuentaInquiries, "permissible purpose");
				String contactInformation = textoCuentaInquiries.substring(textoCuentaInquiries.indexOf(account),
						textoCuentaInquiries.indexOf("requested on")).replace(account, "").replaceAll("\r\n", " ");

				cuentasInquieries.add(new Inquiery(account, requestedOn, "",
						"", contactInformation, inquieresType,permissible));


			}

			posicionInicialTag = posicionFinalTag;


			if (posicionFinalTag < 0 && sw) {

				posicionFinalTag = datosInquieres.length();
				sw = false;

			}

		} while (posicionInicialTag >-1);
	}


	/**
	 * Nombre de cuenta para Transunion
	 * @param textoMinuscula
	 * @param numeroCuenta
	 * @return
	 */
	private String getAccountNameTransunion(String textoMinuscula,String numeroCuenta) {

		String accounName = "";

		int posicionInicialNumeroCuenta = textoMinuscula.indexOf(numeroCuenta);
		int posicionInicialNombreCuenta = textoMinuscula.lastIndexOf("\n",posicionInicialNumeroCuenta);

		if (posicionInicialNumeroCuenta >= 0 &&  posicionInicialNombreCuenta  >= 0 &&  
				posicionInicialNombreCuenta < posicionInicialNumeroCuenta)

			accounName = textoMinuscula.substring(posicionInicialNombreCuenta,posicionInicialNumeroCuenta);

		return accounName.replaceAll("\n", "").trim();


	}

	/**
	 * BUscar el balance en el formato transunion
	 * @param source
	 * @return
	 */
	private boolean getBalanceTransunion(String source){

		boolean resultado = false;

		int posicionBalance = source.indexOf("balance:");

		if (posicionBalance>=0) {

			int primeraPosicionBalance = source.lastIndexOf("\n",posicionBalance);
			String balance = source.substring(primeraPosicionBalance, posicionBalance).replaceAll("\n","").trim();
			resultado = !balance.equalsIgnoreCase("high");

		}
		return  resultado;

	}

	/*****************************************************************************************************
	 **************************************** Metodos Patron Experian Dos *********************************
	 *****************************************************************************************************/
	private String[] getAddressExperianPatronDos(String textoMinuscula,String tag) {

		int posicionInicial = textoMinuscula.lastIndexOf("personal information");
		int posicionFinal  = 0;

		if (posicionInicial < 0)

			posicionInicial = textoMinuscula.lastIndexOf("the following information is reported");

		List<String> address = new ArrayList<String>();

		do {

			posicionInicial = textoMinuscula.indexOf(tag,posicionInicial);

			if (posicionInicial > 0 ) {

				posicionFinal = textoMinuscula.indexOf('\n',posicionInicial) + 1;
				posicionFinal = textoMinuscula.indexOf('\n',posicionFinal);

				String direccion = utilidades.eliminarRetornosCarro(textoMinuscula.substring(posicionInicial,posicionFinal).replaceAll("address:", "")).trim();
				address.add(direccion);

				posicionInicial = posicionFinal+6;

			}


		} while (posicionInicial > 0);


		return address.toArray(new String[address.size()]);

	}

	private String getNombreCuentaPublicPatronDos(String textoPublic,String adddres) {

		String resultado = "";

		String texto = textoPublic.substring(0,textoPublic.indexOf(adddres));

		int posicionInicial = texto.lastIndexOf("\n",texto.length() - 2);


		if (posicionInicial>= 0)
			resultado = texto.substring(posicionInicial).
			replace("address", "").replaceAll("\r"," ").replaceAll(":", " ").
			replaceAll("\n","").trim();


		return resultado;

	}

	private String getStatusDetailsPatronDos(String textoMinuscula,String tag) {

		String  resultado = "";

		int posicionInicialStatusDetails = textoMinuscula.indexOf(tag);
		int posicionFinalStatusDetails = textoMinuscula.indexOf("date filed");

		if (posicionInicialStatusDetails >= 0 && posicionInicialStatusDetails < posicionFinalStatusDetails) {

			resultado =  utilidades.eliminarRetornosCarro(textoMinuscula.substring(posicionInicialStatusDetails,posicionFinalStatusDetails)).
					replaceAll(tag+"|:", "").trim();
		}

		return resultado;
	}

	/*****************************************************************************************************
	 **************************************** Metodos Patron TransUnion Uno *********************************
	 *****************************************************************************************************/
	/**
	 * Nombre del empleador para transunion
	 * @return
	 * @throws IOException 
	 */
	private String getNombreEmpleadorTransunion(PosicionTextoPDF posicionTextoPDF,PDDocument document) throws IOException {

		String employer = "";

		PosicionTexto posicionTexto = posicionTextoPDF.buscarPosicionPalabra("Position",1);

		if (posicionTexto != null) {

			Double posicionY = posicionTexto.getPosYInicial() - 20;
			Double ancho = posicionTexto.getPosXInicial();

			String textoArea = utilidades.getTextoArea(document,0, 0, posicionY.intValue(),ancho.intValue() , 40);
			employer = utilidades.eliminarRetornosCarro(utilidades.getDatoVertical(textoArea.replace("NULL", "").toLowerCase(),patronTransunion.getTagEmployee()).replaceAll("employer(s)", ""));
		}

		return employer;
	}

	private String[] getAddressExperianPatronCinco(String textoMinuscula,String tag) {

		int posicionInicial = textoMinuscula.lastIndexOf("name id");
		int posicionFinal = textoMinuscula.indexOf("address id");

		List<String> address = new ArrayList<String>();

		if (posicionInicial < 0)

			posicionInicial = textoMinuscula.indexOf(tag);

		do {

			if (posicionInicial > 0 && posicionInicial < posicionFinal) {

				String direccion = textoMinuscula.substring(posicionInicial,posicionFinal);
				direccion = direccion.replace(utilidades.getLineas(direccion, 1), "");
				String numeroDireccion = textoMinuscula.substring(posicionFinal,textoMinuscula.indexOf('\n',posicionFinal));
				direccion += numeroDireccion;
				address.add(utilidades.eliminarRetornosCarro(direccion));

			}

			posicionInicial = posicionFinal;
			posicionFinal = textoMinuscula.indexOf("address id",posicionInicial+8);

		} while (posicionFinal > 0);


		return address.toArray(new String[address.size()]);
	}

	/*****************************************************************************************************
	 **************************************** Metodos Patron Experian Cinco *********************************
	 *****************************************************************************************************/


	/**
	 * 
	 * @param textoMinuscula
	 * @return
	 */
	private String getAccountNamePatronCinco(String textoMinuscula, String tag) {

		String dato = "";
		int posicionInicial = textoMinuscula.indexOf(tag);
		int posicionFinal  = textoMinuscula.indexOf("\n",posicionInicial);

		//Se sacan las dos lineas
		posicionFinal  = textoMinuscula.indexOf("\n",posicionFinal + 1);

		if (posicionInicial >= 0 && posicionFinal > posicionInicial)
			dato = textoMinuscula.substring(posicionInicial,posicionFinal).
			replaceAll(tag+"|:|$", "").replaceAll("\n|\r"," ").trim();

		int posicionAccountNumber = dato.indexOf("account");

		if (posicionAccountNumber > 0 )

			dato = dato.substring(0, posicionAccountNumber);


		return dato;


	}

	/*****************************************************************************************************
	 **************************************** Metodos Generales *********************************
	 *****************************************************************************************************/

	private String generarJSONNingunPatron() {

		JSONObject jsonReporte = new JSONObject();
		jsonReporte.put("bureau_id",bureau_id);
		jsonReporte.put("pattern","none");

		return jsonReporte.toString();
	}


	private String generarJSON(Reporte reporte,List<CuentaReporte> cuentasReporte, 
			List<Inquiery> cuentaInquieries, List<PublicAccount> publicAccounts) {

		String previous_address = "";
		String current_address = "";
		String last_reported_employment = "";
		String previous_reported_employment = "";


		int length;

		if (reporte.getAddress() !=null  &&  (length = reporte.getAddress().length) > 0) {

			length--;
			current_address = reporte.getAddress()[length];

			length--;
			if (length > 0)
				previous_address = reporte.getAddress()[length];

		}

		if (reporte.getEmployeer()!=null && (length = reporte.getEmployeer().length) > 0) {

			length--;
			last_reported_employment = reporte.getEmployeer()[length];

			length--;
			if (length > 0)
				previous_reported_employment = reporte.getEmployeer()[length];
		}

		/*************************************************************************************************************
		 **************************************** Datos Encabezado del Reporte ***************************************
		 *************************************************************************************************************/

		JSONObject jsonReporte = new JSONObject();
		try {
			Field changeMap = jsonReporte.getClass().getDeclaredField("map");
			changeMap.setAccessible(true);
			changeMap.set(jsonReporte, new LinkedHashMap<>());
			changeMap.setAccessible(false);
		} catch (IllegalAccessException | NoSuchFieldException e) {}


		jsonReporte.put("bureau_id",bureau_id) ;
		jsonReporte.put("credit_file_date",credit_file_date);
		jsonReporte.put("bureau_name", reporte.getCra());
		jsonReporte.put("ssn", reporte.getSsn());
		jsonReporte.put("dob",reporte.getBirthyear());
		jsonReporte.put("current_address",current_address);
		jsonReporte.put("previous_address",previous_address);
		jsonReporte.put("last_reported_employment",last_reported_employment);
		jsonReporte.put("previous_reported_employment",previous_reported_employment);
		jsonReporte.put("count_of_credit_cards",reporte.getOpencreditcards().isEmpty() ? "" : getNumeroEntero(reporte.getOpencreditcards()));
		jsonReporte.put("count_of_retail_cards",reporte.getOpenretailcards().isEmpty() ? "" :  getNumeroEntero(reporte.getOpenretailcards()));
		jsonReporte.put("count_of_real_state_loans",reporte.getOpenrealrstateloans().isEmpty() ? "" : getNumeroEntero(reporte.getOpenrealrstateloans()));
		jsonReporte.put("count_of_installment_loans",reporte.getOpeninstallmentloans().isEmpty() ? "" :getNumeroEntero(reporte.getOpeninstallmentloans()));
		jsonReporte.put("total_of_open_accounts",reporte.getTotalopenaccounts().isEmpty() ? "" :getNumeroEntero(reporte.getTotalopenaccounts()));
		jsonReporte.put("count_of_sever_late",reporte.getAccountseverlate().isEmpty() ? "" :getNumeroEntero(reporte.getAccountseverlate()));
		jsonReporte.put("collections_accounts",reporte.getCollectionsaccounts().isEmpty() ? "" :getNumeroEntero(reporte.getCollectionsaccounts()));
		jsonReporte.put("average_account_age",reporte.getAverageaccountage());
		jsonReporte.put("oldest_account",reporte.getOldestaccount());
		jsonReporte.put("newest_account",reporte.getNewestaccount());
		jsonReporte.put("my_hard_credit_inquiries",reporte.getMyhardcreditinquiries().isEmpty() ? "" : getNumeroEntero(reporte.getMyhardcreditinquiries()));
		jsonReporte.put("overall_credit_usage",reporte.getOverallcreditusage());
		jsonReporte.put("credit_debt",reporte.getCreditdebt().isEmpty() ? "" : getNumeroReal(reporte.getCreditdebt()));
		jsonReporte.put("total_credit",reporte.getTotalcredit().isEmpty() ? "" : getNumeroReal(reporte.getTotalcredit()));
		jsonReporte.put("credit_and_retail_card_debt",reporte.getCreditandretailcarddebt().isEmpty() ? "" : getNumeroReal(reporte.getCreditandretailcarddebt()));
		jsonReporte.put("real_estate_debt",reporte.getRealestatedebt().isEmpty() ? "" : getNumeroReal(reporte.getRealestatedebt()));
		jsonReporte.put("installment_loans_debt",reporte.getInstallmentloansdebt().isEmpty() ? "" : getNumeroReal(reporte.getInstallmentloansdebt()));
		jsonReporte.put("collections_debt",reporte.getCollectionsdebt().isEmpty() ? "" : getNumeroReal(reporte.getCollectionsdebt()));
		jsonReporte.put("total_debt",reporte.getTotaldebt().isEmpty() ? "" : getNumeroReal(reporte.getTotaldebt()));
		jsonReporte.put("my_public_records",reporte.getMypublicrecords().isEmpty() ? "" : getNumeroEntero(reporte.getMypublicrecords()));
		jsonReporte.put("credit_score",reporte.getCreditscore().isEmpty() ? "" : getNumeroReal(reporte.getCreditscore()));
		jsonReporte.put("name", reporte.getName());
		jsonReporte.put("pattern",reporte.getPatron());
		jsonReporte.put("date_report",reporte.getDateofreport());


		/*************************************************************************************************************
		 ******************************** Datos Cuentas de Credito del Reporte ***************************************
		 *************************************************************************************************************/
		JSONArray cuentasReporteArray = new JSONArray();

		for (CuentaReporte cuentaReporte : cuentasReporte) {

			JSONArray pagosCuentasHistorico = new JSONArray();


			if (cuentaReporte.getMesesCanceladosPorAno()!=null) {

				for (Map.Entry<String, List<HistorialPago>> entry : cuentaReporte.getMesesCanceladosPorAno().entrySet()) {

					String año = entry.getKey();
					JSONArray pagosCuentasPorMes = new JSONArray();

					List<HistorialPago> historialPagos = entry.getValue();

					for (HistorialPago historialPago : historialPagos) {

						JSONObject pagosMeses = new JSONObject();

						pagosMeses.put("month", historialPago.getMes());
						pagosMeses.put("period",historialPago.getPeriodo());
						pagosMeses.put("type",historialPago.getTipo());
						pagosMeses.put("value",historialPago.getValor());
						pagosCuentasPorMes.put(pagosMeses);

					}

					JSONObject pagosHistoricoAño = new JSONObject();
					pagosHistoricoAño.put(año,pagosCuentasPorMes);

					pagosCuentasHistorico.put(pagosHistoricoAño);

				}

			}


			JSONObject jsonCuentaReporte = new JSONObject();

			try {
				Field changeMap = jsonCuentaReporte.getClass().getDeclaredField("map");
				changeMap.setAccessible(true);
				changeMap.set(jsonCuentaReporte, new LinkedHashMap<>());
				changeMap.setAccessible(false);
			} catch (IllegalAccessException | NoSuchFieldException e) {}


			jsonCuentaReporte.put("account_name",cuentaReporte.getAccountName());
			jsonCuentaReporte.put("account_number",cuentaReporte.getAccountNumber().isBlank() ? "NO DEFINED" : cuentaReporte.getAccountNumber() );
			jsonCuentaReporte.put("account_type",cuentaReporte.getAccountType());
			jsonCuentaReporte.put("account_status",cuentaReporte.getAccountStatus());
			jsonCuentaReporte.put("status_updated_at",cuentaReporte.getStatusUpdated());
			jsonCuentaReporte.put("status_detail",cuentaReporte.getStatusDetail());
			jsonCuentaReporte.put("balance",cuentaReporte.getBalance().isEmpty() ? "" : getNumeroReal(cuentaReporte.getBalance()));
			jsonCuentaReporte.put("balance_updated_at",cuentaReporte.getBalanceUpdated());
			jsonCuentaReporte.put("limit",cuentaReporte.getLimit().isEmpty() ? "" : getNumeroReal(cuentaReporte.getLimit()));
			jsonCuentaReporte.put("monthly_payment",cuentaReporte.getMonthlyPayment().isEmpty() ? "" : getNumeroReal(cuentaReporte.getMonthlyPayment()));
			jsonCuentaReporte.put("past_due_amount",cuentaReporte.getPastDueAmount().isEmpty() ? "" : getNumeroReal(cuentaReporte.getPastDueAmount()));
			jsonCuentaReporte.put("highest_balance",cuentaReporte.getHighestBalance().isEmpty() ? "" : getNumeroReal(cuentaReporte.getHighestBalance()));
			jsonCuentaReporte.put("terms",cuentaReporte.getTerms());
			jsonCuentaReporte.put("responsibility",cuentaReporte.getResponsibility());
			jsonCuentaReporte.put("statement",cuentaReporte.getYourStatement());
			jsonCuentaReporte.put("comments",cuentaReporte.getComments());
			jsonCuentaReporte.put("date_opened_at",cuentaReporte.getDateOpened());
			jsonCuentaReporte.put("date_closed_at",cuentaReporte.getDateClosed());
			jsonCuentaReporte.put("loan_type",cuentaReporte.getLoanType());
			jsonCuentaReporte.put("total_of_payment_received",cuentaReporte.getPaymentReceived().isEmpty() ? "" : getNumeroReal(cuentaReporte.getPaymentReceived()));
			jsonCuentaReporte.put("payment_status",cuentaReporte.getPaymentStatus());
			jsonCuentaReporte.put("last_activity",cuentaReporte.getLastActivity());
			jsonCuentaReporte.put("original_creditor",cuentaReporte.getOriginalCreditor());
			jsonCuentaReporte.put("account_class_one",cuentaReporte.getAccounttypeone());
			jsonCuentaReporte.put("account_class_two",cuentaReporte.getAccounttypetwo());
			jsonCuentaReporte.put("credit_usage",cuentaReporte.getCreditusage());
			jsonCuentaReporte.put("credit_usage_description",cuentaReporte.getCreditusagedescription());
			jsonCuentaReporte.put("history",pagosCuentasHistorico);

			cuentasReporteArray.put(jsonCuentaReporte);


		}


		jsonReporte.put("credit_report_items",cuentasReporteArray);



		/*************************************************************************************************************
		 ******************************** Datos Cuentas de Inquirires del Reporte ***************************************
		 *************************************************************************************************************/
		JSONArray cuentasReporteInquiriesArrays = new JSONArray();

		if (cuentaInquieries != null) {

			for (Inquiery inquiery : cuentaInquieries) {

				JSONObject jsonCuentaInquiries = new JSONObject();

				jsonCuentaInquiries.put("account_name",inquiery.getAccountName()); 
				jsonCuentaInquiries.put("inquiry_date",inquiery.getInquiryDate()); 
				jsonCuentaInquiries.put("removal_date",inquiery.getRemovalDate());
				jsonCuentaInquiries.put("business_type",inquiery.getBusinessType());
				jsonCuentaInquiries.put("contact_information",inquiery.getContactInformation()); 

				jsonCuentaInquiries.put("inquieres_type",inquiery.getInquieresType());
				jsonCuentaInquiries.put("comments",inquiery.getComments());

				cuentasReporteInquiriesArrays.put(jsonCuentaInquiries);

			}

		}

		jsonReporte.put("accounts_inquiries",cuentasReporteInquiriesArrays);

		/*************************************************************************************************************
		 ******************************** Datos Cuentas de Publicas del Reporte ***************************************
		 *************************************************************************************************************/


		JSONArray publicAccountsReporteArrays = new JSONArray();

		if (publicAccounts != null) {

			for (PublicAccount publicAccount : publicAccounts) {

				JSONObject publicAccountReporte = new JSONObject();

				publicAccountReporte.put("account_name",publicAccount.getAccountName());
				publicAccountReporte.put("filing_date",publicAccount.getFilingDate());
				publicAccountReporte.put("amount",publicAccount.getAmount());
				publicAccountReporte.put("reference_number",publicAccount.getReferenceNumber());
				publicAccountReporte.put("court",publicAccount.getCourt());
				publicAccountReporte.put("plain_tiff",publicAccount.getPlaintiff().replaceAll("( ){2,}", " "));
				publicAccountsReporteArrays.put(publicAccountReporte);

			}

		}

		jsonReporte.put("accounts_publics",publicAccountsReporteArrays);

		return jsonReporte.toString();
	}

	private String getTextoConFormato(PDDocument document) throws IOException {

		LayoutTextStripper stripper = new LayoutTextStripper();
		stripper.setSortByPosition(true);
		stripper.fixedCharWidth = 5;
		String textoConFormato = stripper.getText(document).toLowerCase().
				replaceAll("trans union", "transunion").
				replaceAll("expenan","experian");
		return textoConFormato;
	}

	private String getTypeAccount(String[] textos,String textoCuenta,String[] tagFinales,String[] tiposCuenta) {

		boolean sw = true;
		String resultado = "";

		for (int i = 0; i < textos.length && sw; i++) {

			textoCuenta = ((i+1) != textos.length && textoCuenta.contains(tagFinales[i+1])) ? 
					textoCuenta.substring(0,textoCuenta.indexOf(tagFinales[i+1])) : textoCuenta;

					if (textos[i].contains(utilidades.getCadenaRecortada(textoCuenta,"the listing of a company's inquiry in"))) { 

						sw = false;
						resultado = tiposCuenta[i];	
					}


		}


		if (resultado.isEmpty() && textos[0].length() < 120)

			resultado = tiposCuenta[1];

		return resultado;

	}

	//*************************************************************************************************


}
