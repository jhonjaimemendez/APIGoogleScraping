package com.legalCredit;

import static com.legalCredit.componentes.Utilidades.getNumeroEntero;
import static com.legalCredit.componentes.Utilidades.getNumeroReal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
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
import com.legalCredit.componentes.Utilidades.DataOCRFile;
import com.legalCredit.modelo.Collection;
import com.legalCredit.modelo.CuentaReporte;
import com.legalCredit.modelo.HistorialPago;
import com.legalCredit.modelo.Inquiry;
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
	private String idiomaDocumentoPDFOCR = "en";


	public String scrapingPDF(String Json) {

		String result = "";

		try {

			boolean fileOCR = false;

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

			DataOCRFile dataOCRFile = null;
			
			if (textoArchivo.isBlank() ||  textoArchivo.length() < 80  || 
					textoArchivo.startsWith("Scanned by") || textoArchivo.startsWith("Scanned with") || 
					textoArchivo.toLowerCase().startsWith("generated")) { //Se verfica que el archivo no se encuentre escaneado

				dataOCRFile = getProcesarArchivoOCR(document,name,tmpFile);

				if (dataOCRFile.getFile() != null) {

					document = PDDocument.load(dataOCRFile.getFile());

					pdfStripper = new PDFTextStripper();
					textoArchivo = pdfStripper.getText(document).toLowerCase().replaceAll("trans union", "transunion").
							replaceAll("expenan","experian");

					idiomaDocumentoPDFOCR = dataOCRFile.getIdioma();

				}


				fileOCR = true;

			}
			

			String patron[] = getPatron(textoArchivo);	

			if (patron[0].equals("PatronEquifax")) {

				if (patron[1].equals("PatronUno"))

					result = scrapearPatronGenerico(textoArchivo,patronEquifax.getCra());

				else if (patron[1].equalsIgnoreCase("PatronDos")) 

					result = scrapearEquifaxPatronDos(textoArchivo,getTextoConFormato(document));

				else if (patron[1].equalsIgnoreCase("PatronTres")) 

					scrapearEquifaxPatronTres(dataOCRFile.getTexto());

			} else if (patron[0].equals("PatronTransunion")) {

				if (patron[1].equals("PatronUno"))

					result = scrapearTransunionPatronUno(textoArchivo,getTextoConFormato(document),document,true);

				else if (patron[1].equals("PatronDos"))

					result = scrapearPatronGenerico(textoArchivo,patronTransunion.getCra()); 

				else if (patron[1].equals("PatronTres"))

					scrapearTransunionPatronTres(textoArchivo,fileOCR,idiomaDocumentoPDFOCR); 

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
		List<Inquiry> cuentasInquieries = new ArrayList<Inquiry>();
		List<PublicAccount> publicAccounts = new ArrayList<PublicAccount>();

		int posicionInicialCuenta = 0;
		int posicionFinalCuenta = 0;

		Reporte reporte = new Reporte();
		reporte.setSsn(getSSN(textoMinuscula));
		reporte.setCra(cra);
		reporte.setEmployeer(new String[]{utilidades.eliminarRetornosCarro(utilidades.getDatoVertical(textoMinuscula,patronEquifax.getTagEmployee()).replaceAll("employer(s)", ""))});
		reporte.setName(utilidades.getDatoVertical(textoMinuscula,patronEquifax.getTagName()));
		reporte.setOpencreditcards(utilidades .getDatoHorizontal(textoMinuscula,elementosReporte[0]));
		reporte.setOpenretailcards(utilidades.getDatoHorizontal(textoMinuscula,elementosReporte[1]));
		reporte.setOpenrealrstateloans(utilidades.getDatoHorizontal(textoMinuscula,elementosReporte[2]));
		reporte.setOpeninstallmentloans(utilidades.getDatoHorizontal(textoMinuscula,elementosReporte[3]));
		reporte.setTotalopenaccounts(utilidades.getDatoHorizontal(textoMinuscula,elementosReporte[4]));
		reporte.setAccountseverlate(utilidades.getDatoHorizontal(textoMinuscula,elementosReporte[5]));
		reporte.setCollectionsaccounts(utilidades.getDatoHorizontal(textoMinuscula,elementosReporte[6]));
		reporte.setAverageaccountage(utilidades.getDatoHorizontal(textoMinuscula,elementosReporte[7]));
		reporte.setOldestaccount(utilidades.getDatoHorizontal(textoMinuscula,elementosReporte[8]));
		reporte.setNewestaccount(utilidades.getDatoHorizontal(textoMinuscula,elementosReporte[9]));
		reporte.setCreditdebt(utilidades.getDatoVertical(textoMinuscula,elementosReporte[10]));
		reporte.setTotalcredit(utilidades.getDatoVertical(textoMinuscula,elementosReporte[11]));
		reporte.setCreditandretailcarddebt(utilidades.getDatoHorizontal(textoMinuscula,elementosReporte[12]));
		reporte.setRealestatedebt(utilidades.getDatoHorizontal(textoMinuscula,elementosReporte[13]));
		reporte.setInstallmentloansdebt(utilidades.getDatoHorizontal(textoMinuscula,elementosReporte[14]));
		reporte.setCollectionsdebt(utilidades.getDatoHorizontal(textoMinuscula,elementosReporte[15]));
		reporte.setTotaldebt(utilidades.getDatoHorizontal(textoMinuscula,elementosReporte[16]));
		reporte.setMyhardcreditinquiries(utilidades.getLineaAnterior(textoMinuscula,elementosReporte[17]));
		reporte.setMypublicrecords(utilidades.getLineaAnterior(textoMinuscula,elementosReporte[18]));
		reporte.setCreditscore(getCreditScorePatronGenerico(textoMinuscula,elementosReporte[19]));
		reporte.setDateofreport(utilidades.getFechaFormatoMesDiaAño(utilidades.getDatoHorizontal(textoMinuscula,elementosReporte[20])));
		reporte.setAddress(getAddressPatronGenerico(textoMinuscula,patronEquifax.getTagAddress(),reporte.getName()));
		reporte.setBirthyear(utilidades.getDatoVertical(textoMinuscula,elementosReporte[21]));
		reporte.setOverallcreditusage(utilidades.getDatoVertical(textoMinuscula,elementosReporte[22]));
		reporte.setPatron("PatronGenerico");



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

				CuentaReporte cuentaReporte = new CuentaReporte();
				cuentaReporte.setAccountName(utilidades.getDatoHorizontal(textoCuenta, elementos[0]));
				cuentaReporte.setAccountNumber(utilidades.getDatoHorizontal(textoCuenta, elementos[1]));
				cuentaReporte.setAccountType(utilidades.getDatoHorizontal(textoCuenta, elementos[2]));
				cuentaReporte.setAccountStatus(utilidades.getDatoHorizontal(textoCuenta, elementos[4]));
				cuentaReporte.setPaymentStatus(utilidades.getDatoHorizontal(textoCuenta, elementos[5]));
				cuentaReporte.setStatusUpdated(utilidades.getFechaFormatoMesDiaAño(utilidades.getDatoHorizontal(textoCuenta, elementos[6])));
				cuentaReporte.setBalance(utilidades.getDatoHorizontal(textoCuenta, elementos[7]));
				cuentaReporte.setBalanceUpdated(utilidades.getFechaFormatoMesDiaAño(utilidades.getDatoHorizontal(textoCuenta, elementos[8])));
				cuentaReporte.setLimit(utilidades.getDatoHorizontal(textoCuenta, elementos[9]));
				cuentaReporte.setMonthlyPayment(utilidades.getDatoHorizontal(textoCuenta, elementos[10]));
				cuentaReporte.setPastDueAmount(utilidades.getDatoHorizontal(textoCuenta, elementos[11]));
				cuentaReporte.setHighestBalance(utilidades.getDatoHorizontal(textoCuenta, elementos[12]));
				cuentaReporte.setTerms(utilidades.getDatoHorizontal(textoCuenta, elementos[13]));
				cuentaReporte.setResponsibility(utilidades.getDatoHorizontal(textoCuenta, elementos[14]));
				cuentaReporte.setYourStatement(utilidades.getDatoHorizontal(textoCuenta, elementos[15]));
				cuentaReporte.setComments(utilidades.getDatoHorizontal(textoCuenta, elementos[16]));
				cuentaReporte.setDateOpened(utilidades.getFechaFormatoMesDiaAño(utilidades.getDatoHorizontal(textoCuenta, elementos[3])));
				cuentaReporte.setAccounttypeone(getAccounttypeonePatronGenerico(textoMinuscula, textCuentaSinFormato, cuentaReporte.getAccountName()));
				cuentaReporte.setCreditusage(getCreditUsagePatronGenerico(textoCuenta, "payment history"));
				cuentaReporte.setCreditusagedescription(getCreditUsageDescriptionPatronGenerico(textoCuenta, elementos[18]));
				cuentaReporte.setOriginalCreditor(utilidades.getDatoHorizontal(textoCuenta, elementos[17]));


				if (!cuentaReporte.getAccounttypeone().isEmpty())

					accountTypeBefore =	cuentaReporte.getAccounttypeone();

				else

					cuentaReporte.setAccounttypeone(accountTypeBefore);



				/*
				 * ****************** Se obtienen el historial de pago **************************************
				 */
				String historialPago = textoMinuscula.substring(posicionInicioHistorialCuenta,
						posicionFinHistorialPago > 0 ? posicionFinHistorialPago : textoMinuscula.length()).replaceAll(reporte.getDateofreport(), "");
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

				cuentaReporte.setMesesCanceladosPorAno(mesesCanceladosPorAno);

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
						String[] inquiryDates = inquiryDate.split(" ");
						String removalDate = utilidades.getFechaFormatoMesDiaAño(utilidades.getDatoHorizontal(textoCuenta, elementosInquires[1]));
						String businessType = utilidades.getDatoHorizontal(textoCuenta, elementosInquires[2]);
						String contactInformation = textoCuenta.substring(textoCuenta.indexOf(contactinformation),textoCuenta.indexOf(nombreCuenta)).
								replaceAll("\n", " ").replaceAll("\r", " ").replaceAll(contactinformation, " ").
								trim();
						String inquieresType = "inquiry";

						for (String fechaInquiry : inquiryDates) 

							cuentasInquieries.add(new Inquiry(nombreCuenta, utilidades.getFechaFormatoMesDiaAño(fechaInquiry), removalDate,
									businessType, contactInformation, inquieresType,"","",""));
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

						PublicAccount publicAccount = new PublicAccount();
						publicAccount.setAccountName(getUltimaLineaPatronGenerico(textoCuenta,cra));
						publicAccount.setFilingDate(utilidades.getFechaFormatoMesDiaAño(utilidades.getDatoHorizontal(textoCuenta, elementosPublic[0])));
						publicAccount.setClaim_amount(utilidades.getDatoHorizontal(textoCuenta, elementosPublic[1]));
						publicAccount.setReferenceNumber(utilidades.getDatoHorizontal(textoCuenta, elementosPublic[2]));
						publicAccount.setCourt(utilidades.getDatoHorizontal(textoCuenta, elementosPublic[3]));
						publicAccount.setPlaintiff(utilidades.getDatoHorizontal(textoCuenta, elementosPublic[4]));

						publicAccounts.add(publicAccount);

					}

				}while (posicionInicialInquires > 0);
			}

		}

		return generarJSON(reporte,cuentasReporte,cuentasInquieries,publicAccounts,null);
	}

	/**
	 * Scraper equifax patron dos
	 * @param archivo
	 * @param textoMinuscula
	 * @throws IOException
	 */
	private String scrapearEquifaxPatronDos(String textoMinuscula,String textoConFormato) throws IOException {

		textoConFormato = textoConFormato.replaceAll("account number", "accountnumber").
				          replaceAll("public\\s+records", "public records").
				          replaceAll("hard\\s+inquiries", "hard inquiries").
				          replaceAll("soft\\s+inquiries", "soft inquiries");

		String[] elementos = patronEquifax.getElementosAContenerTextoRepetidamentePatronDos();
		String[] tiposCuentaDos = {"revolving accounts","mortgage accounts",
				"installment accounts","other accounts",
		"consumer statements"};



		List<CuentaReporte> cuentasReporte = new ArrayList<CuentaReporte>();
		List<Inquiry> cuentasInquieries = new ArrayList<Inquiry>();
		List<PublicAccount> cuentasPublicas = new ArrayList<PublicAccount>();
		List<Collection> cuentasCollections = new ArrayList<Collection>();
		Map<String,List<HistorialPago>> informacionCuentas = null;

		Reporte reporte = new Reporte();
		reporte.setSsn(utilidades.getDatoHorizontal(textoMinuscula, patronEquifax.getTagSSN()));
		reporte.setCra(patronEquifax.getCra());
		reporte.setEmployeer(getNombreEmpleadosEquifaxPatronDos(textoMinuscula,patronEquifax.getTagEmployeePatronDos()));
		reporte.setName(utilidades.eliminarMasDeDosEspaciosEnTexto(utilidades.getDatoHorizontal(textoMinuscula,patronEquifax.getTagAccountNamePatronDos())));
		reporte.setTotalopenaccounts(utilidades.getDatoHorizontal(textoMinuscula, "total"));
		reporte.setCollectionsaccounts(utilidades.getDatoHorizontal(textoMinuscula,"collections"));
		reporte.setAverageaccountage(utilidades.getDatoHorizontal(textoMinuscula, "average account age"));
		reporte.setOldestaccount(utilidades.getDatoHorizontal(textoMinuscula, "oldest account"));
		reporte.setNewestaccount(utilidades.getDatoHorizontal(textoMinuscula, "most recent account"));
		reporte.setCreditandretailcarddebt(utilidades.getDatoHorizontal(textoMinuscula,"accounts with negative information"));
		reporte.setMypublicrecords(utilidades.getDatoHorizontal(textoMinuscula,"public records"));
		reporte.setDateofreport(utilidades.getFechaFormatoMesDiaAño(utilidades.getDatoHorizontal(textoMinuscula, patronEquifax.getTagAccountReportDate())));
		reporte.setAddress(getAddressEquifaxPatronDos(textoMinuscula,patronTransunion.getTagAddress()));
		reporte.setBirthyear(utilidades.getFechaFormatoMesDiaAño(utilidades.getDatoHorizontal(textoMinuscula,patronEquifax.getTagBirthReportDate())));
		reporte.setPatron("PatronDos");



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

				CuentaReporte cuentaReporte = new CuentaReporte();
				cuentaReporte.setAccountName(getAccountNameEquifaxDos(textoMinuscula,textoCuenta) );
				cuentaReporte.setAccountNumber(utilidades.getCadenaRecortada(utilidades.getDatoHorizontal(textoCuenta, elementos[0]),"reported") );
				cuentaReporte.setAccountType(utilidades.getDatoHorizontal(textoCuenta, elementos[1]));
				cuentaReporte.setAccountStatus(utilidades.getCadenaRecortada(utilidades.getDatoHorizontal(textoCuenta, elementos[3]).replaceAll("-", ""),"debttocredit ratio"));
				cuentaReporte.setBalance( utilidades.getDatoHorizontal(textoCuenta, elementos[7]));
				cuentaReporte.setLimit(utilidades.getCadenaRecortada(utilidades.getDatoHorizontal(textoCuenta, elementos[5],"account details"),"account type") );
				cuentaReporte.setMonthlyPayment(utilidades.getDatoHorizontal(textoCuenta, elementos[15]) );
				cuentaReporte.setPastDueAmount(utilidades.getCadenaRecortada(utilidades.getDatoHorizontal(textoCuenta, elementos[8],"account details"),"date reported") );
				cuentaReporte.setHighestBalance(utilidades.getDatoHorizontal(textoCuenta, elementos[4]) );
				cuentaReporte.setTerms(utilidades.getCadenaRecortada(utilidades.getDatoHorizontal(textoCuenta, elementos[6]),"term") );
				cuentaReporte.setComments(utilidades.getDatoVertical(textoCuenta, elementos[11],"account details") );
				cuentaReporte.setDateOpened(utilidades.getFechaFormatoMesDiaAño(utilidades.getDatoHorizontal(textoCuenta, elementos[2])));
				cuentaReporte.setAccounttypeone(getTypeAccountNamePatronDos(cuentaReporte.getAccountName()));
				cuentaReporte.setAccounttypetwo(getTypeAccount(textosTiposCuentas, textoCuenta, tagFinales,tiposCuentaDos));
				cuentaReporte.setLoanType(utilidades.getCadenaRecortada(utilidades.getDatoHorizontal(textoCuenta, elementos[10]),"date closed"));
				cuentaReporte.setDateClosed(utilidades.getFechaFormatoMesDiaAño( utilidades.getDatoHorizontal(textoCuenta, elementos[12])));
				cuentaReporte.setPaymentReceived(utilidades.getCadenaRecortada(utilidades.getDatoHorizontal(textoCuenta, elementos[14]),"date of last"));
				cuentaReporte.setOriginalCreditor(utilidades.getDatoHorizontal(textoCuenta, elementos[9]));
				cuentaReporte.setLastActivity(utilidades.getCadenaRecortada(utilidades.getDatoHorizontal(textoCuenta, elementos[13]),"scheduled"));
				cuentaReporte.setAccountName(cuentaReporte.getAccountName().replaceAll(cuentaReporte.getAccounttypeone()+"|\\)|\\(", "" ));
				cuentaReporte.setCreditFileStatus(getCreditFileStatus(textoCuenta));

				//****** Se obtiene los datos de las cuentas que se encuentran en las tablas *******

				Matcher matcherFechas = utilidades.getPatronFormatoTablaPatronDos().matcher(textoCuentaFormato);

				int posicionInicialLinea = 0;


				informacionCuentas = new HashMap<String,List<HistorialPago>>();


				while (matcherFechas.find()) { //Se recorren las diferentes tabla que tiene la cuenta

					String lineaMeses = matcherFechas.group().trim();

					posicionInicialLinea = textoCuentaFormato.indexOf(lineaMeses,posicionInicialLinea + 10);
					String tipoCuenta = utilidades.getLineaAnterior(textoCuentaFormato, lineaMeses,posicionInicialLinea);

					if (tipoCuenta.contains("."))

						tipoCuenta = "paymenthistory";

					int posicionFinalLinea = 0;

					boolean sw = true;


					do { 

						posicionInicialLinea = textoCuentaFormato.indexOf("\n",posicionInicialLinea) + 2;
						posicionFinalLinea = textoCuentaFormato.indexOf('\n',posicionInicialLinea+2);


						if (posicionInicialLinea < posicionFinalLinea) {

							String linea = textoCuentaFormato.substring(posicionInicialLinea,posicionFinalLinea).trim();
							List<HistorialPago> datosCuentaMes = new ArrayList<HistorialPago>();

							//Se verifica si contiene el año de la tabla
							Matcher matcherAño = utilidades.getPatronAno().matcher(linea.substring(0,4));

							if (matcherAño.find()) {

								String año = matcherAño.group();
								linea = linea.substring(linea.indexOf(año)).replace(año, "").replace("( ){12}", "").
										replaceAll("( ){8}", "@").replaceAll("( ){7}", "@").replaceAll("( ){6}", "@").
										replaceAll("( ){5}", "@");

								String[] elementosLinea = linea.split("@");

								if (elementosLinea.length == 12) { //Se procede a contar para ver si los valores estan completos

									for (int i = 0; i < elementosLinea.length; i++) 
										datosCuentaMes.add(new HistorialPago(utilidades.getMeses()[i],año + "-" + utilidades.getMeses()[i],
												tipoCuenta,elementosLinea[i].trim()));


									informacionCuentas.put(año, datosCuentaMes); //Se agrega los valores a la coleccion


								} else if (elementosLinea.length > 0 && !linea.isBlank()) {

									for (int i = 0; i < utilidades.getMeses().length; i++)

										datosCuentaMes.add(new HistorialPago(utilidades.getMeses()[i],año + "-" + utilidades.getMeses()[i],
												tipoCuenta,i <elementosLinea.length  ? elementosLinea[i].trim() : "no data disponible"));

									informacionCuentas.put(año, datosCuentaMes); //Se agrega los valores a la coleccion

								} else {

									for (String mes : utilidades.getMeses()) {

										datosCuentaMes.add(new HistorialPago(mes,año + "-" + utilidades.getNumeroMes(mes),
												tipoCuenta,"no data available"));

									}

									informacionCuentas.put(año, datosCuentaMes); //Se agrega los valores a la coleccion


								}

							} else 

								sw = false;

						} else 

							sw = false;

					} while (sw);


				}

				cuentaReporte.setMesesCanceladosPorAno(informacionCuentas);

				cuentasReporte.add(cuentaReporte);

			}

		} while (posicionInicialCuenta>0);

		//**************************** Se obtienen las cuentas inquiries ***************************************
		int posicionInicialInquiries = textoConFormato.indexOf("inquiries",posicionInicialConsumerStatements);
		int posicionFinalInquiries = textoConFormato.indexOf("public records",posicionInicialInquiries);

		String textoInquiries = textoConFormato.substring(posicionInicialInquiries, posicionFinalInquiries);


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

			String inquiryDate = fechas.get(i).replaceAll(",", " ").trim();
			
			inquiryDate = inquiryDate.substring(0, 3) + " " + inquiryDate.substring(3); 
			
			int posicionInicialFecha = textoInquiries.indexOf(fechas.get(i),posicionFinalFecha);
			posicionFinalFecha = ((i+1) == fechas.size()) ? textoInquiries.length() : textoInquiries.indexOf(fechas.get(i+1),posicionInicialFecha + inquiryDate.length()); 

			String textCuenta =textoInquiries.substring(posicionInicialFecha,posicionFinalFecha).replace(fechas.get(i), "").trim();

			if (textCuenta.contains("soft inquiries"))
				textCuenta = textCuenta.substring(0,textCuenta.indexOf("soft inquiries"));


			if (!textCuenta.contains("page")) {

				int posicionRetornoCarro = textCuenta.lastIndexOf("\n") > 0 ? textCuenta.lastIndexOf("\n") : textCuenta.length();
				String[] elementosLinea = textCuenta.substring(0,posicionRetornoCarro).replaceAll("( ){4,}", "@").split("@");

					String accountName = utilidades.eliminarRetornosCarro(elementosLinea[0]);
					String comments = utilidades.eliminarRetornosCarro(utilidades.getValor(elementosLinea, 1));
					String inquieresType = textosInquieres[0].contains(textCuenta) ? "hard inquiries" :
						textosInquieres[1].contains(textCuenta) ? "soft inquiries" :
							"";	  

					if (comments.contains(reporte.getName()))

						comments = comments.substring(0,comments.indexOf(reporte.getName()));
					
					cuentasInquieries.add(new Inquiry(accountName, utilidades.getFechaFormatoMesDiaAño(inquiryDate), "", "", 
							"", inquieresType,comments,"",""));
			}


		}


		reporte.setMyhardcreditinquiries("" + cuentasInquieries.size());

		//Se obtiene las cuentas publicas
		String[] tags = new String[]{"bankruptcies","judgments","liens","collections"};
		int posicionFinalTagPublic = 0;

		for (int i = 0; i < 2; i++) {

			int posicionInicialTagPublic = textoMinuscula.indexOf(tags[i]);
			posicionFinalTagPublic = textoMinuscula.indexOf(tags[i+1],posicionInicialTagPublic);

			String textoCuentaPublic = textoMinuscula.substring( posicionInicialTagPublic,posicionFinalTagPublic);

			int posicionReferenceInicial = 0;

			do {

				posicionReferenceInicial = textoCuentaPublic.indexOf("reference",posicionReferenceInicial);

				if (posicionReferenceInicial >= 0) {

					int posicionReferenceFinal = textoCuentaPublic.indexOf("reference",posicionReferenceInicial+10);

					if (posicionReferenceFinal < 0)

						posicionReferenceFinal = textoCuentaPublic.length();


					String publicRecord = textoCuentaPublic.substring(posicionReferenceInicial,posicionReferenceFinal);

					PublicAccount publicAccount = new PublicAccount();
					publicAccount.setAccountName("NO DEFINED");
					publicAccount.setReferenceNumber("reference number " + utilidades.getDatoHorizontal(publicRecord, "number"));
					publicAccount.setStatus(utilidades.getDatoVertical(publicRecord, "status"));
					publicAccount.setFilingDate(utilidades.getFechaFormatoMesDiaAño(utilidades.getCadenaRecortada(utilidades.getDatoHorizontal(publicRecord, "filed"),"type")));
					publicAccount.setLiability_amount(utilidades.getCadenaRecortada(utilidades.getDatoHorizontal(publicRecord, "liability"),"court"));
					publicAccount.setCourt(utilidades.getDatoHorizontal(publicRecord, "court"));
					publicAccount.setComments(utilidades.getDatoVertical(publicRecord, "comments"));

					cuentasPublicas.add(publicAccount);

					posicionReferenceInicial = posicionReferenceFinal + 10;

				}


			} while (posicionReferenceInicial > 0); 


		}

		//Se obtienen las cuentas de tipo collections
		int posicionCollectionsInicial = textoMinuscula.indexOf("collections",posicionFinalTagPublic);
		int posicionCollectionsFinal = textoMinuscula.indexOf("dispute file",posicionCollectionsInicial);

		String textoCuentaCollections = textoMinuscula.substring(posicionCollectionsInicial,posicionCollectionsFinal);

		posicionCollectionsInicial = 0;
		do {

			posicionCollectionsInicial = textoCuentaCollections.indexOf("date reported",posicionCollectionsInicial);

			if (posicionCollectionsInicial >= 0) {

				posicionCollectionsFinal = textoCuentaCollections.indexOf("date reported",posicionCollectionsInicial + 10);

				if (posicionCollectionsFinal < 0) 

					posicionCollectionsFinal = textoCuentaCollections.length();

				String textoCuenta = textoCuentaCollections.substring(posicionCollectionsInicial, posicionCollectionsFinal).
						replace("original amount", "").
						replace("status date", "status_date");


				Collection collection = new Collection();
				collection.setAccountName(utilidades.getCadenaRecortada(utilidades.getDatoHorizontal(textoCuenta, "agency"),"balance"));
				collection.setOriginalCreditorName(utilidades.getCadenaRecortada(utilidades.getDatoHorizontal(textoCuenta, "name"),"account"));
				collection.setDateAssigned(utilidades.getFechaFormatoMesDiaAño(utilidades.getCadenaRecortada(utilidades.getDatoHorizontal(textoCuenta, "assigned"),"account")));
				collection.setOriginalAmountOwned(utilidades.getCadenaRecortada(utilidades.getDatoHorizontal(textoCuenta, "owed"),"creditor"));
				collection.setAmount(utilidades.getCadenaRecortada(utilidades.getDatoHorizontal(textoCuenta, "amount"),"last"));
				collection.setStatusDate(utilidades.getFechaFormatoMesDiaAño(utilidades.getCadenaRecortada(utilidades.getDatoHorizontal(textoCuenta, "status_date"),"date")));
				collection.setStatus(utilidades.getDatoHorizontal(textoCuenta, "status"));
				collection.setBalanceDate(utilidades.getFechaFormatoMesDiaAño(utilidades.getDatoHorizontal(textoCuenta, "balance date")));
				collection.setAccountDesignerCode(utilidades.getDatoHorizontal(textoCuenta, "code"));
				collection.setAccountNumber(utilidades.getDatoHorizontal(textoCuenta, "number"));
				collection.setCreditorClassification(utilidades.getDatoHorizontal(textoCuenta, "classification"));
				collection.setLastPaymentDate(utilidades.getFechaFormatoMesDiaAño(utilidades.getDatoHorizontal(textoCuenta, "payment date")));
				collection.setDateOfFirstDelinquency(utilidades.getFechaFormatoMesDiaAño(utilidades.getDatoHorizontal(textoCuenta, "delinquency")));
				collection.setComments(utilidades.getDatoHorizontal(textoCuenta, "comments"));

				cuentasCollections.add(collection);

				posicionCollectionsInicial = posicionCollectionsFinal + 15;

			}  

		} while (posicionCollectionsInicial >= 0);


		return generarJSON(reporte,cuentasReporte,cuentasInquieries,cuentasPublicas,cuentasCollections);		

	}


	/**
	 * Scraper equifax patron tres
	 * @param archivo
	 * @param textoMinuscula
	 * @throws IOException
	 */
	private void scrapearEquifaxPatronTres(String textoMinuscula) throws IOException {

		textoMinuscula = utilidades.eliminarFilasVacias(utilidades.getFormatearTexto(textoMinuscula,utilidades.getPalabrasCorregirIngles())) ;

		List<CuentaReporte> cuentasReporte = new ArrayList<CuentaReporte>();
		List<Inquiry> cuentasInquieries = new ArrayList<Inquiry>();

		System.out.println(textoMinuscula);

		Reporte reporte = new Reporte();
		reporte.setSsn(getSSN(textoMinuscula));
		reporte.setCra(patronEquifax.getCra());
		reporte.setEmployeer(new String[]{utilidades.getDatoHorizontal(textoMinuscula, "formerly")});
		reporte.setName(getNombreEmpleadoEquifaxPatronTres(textoMinuscula));
		reporte.setDateofreport(utilidades.getDatoHorizontal(textoMinuscula, "credit file"));
		reporte.setAddress(getAddressEquifaxPatronTres(textoMinuscula));
		reporte.setBirthyear(utilidades.getDatoHorizontal(textoMinuscula,"date of birth"));
		reporte.setPatron("PatronTres");
		
		int posicionFinal = 0;
		int posicionInicial = textoMinuscula.indexOf("date of finalballoon payment");

		if (posicionInicial < 0)

			posicionInicial = textoMinuscula.indexOf("accounts of finalballoon payment");

		String tag = "account number";

		do {

			posicionInicial = textoMinuscula.indexOf(tag,posicionInicial);
			posicionFinal = textoMinuscula.indexOf(tag,posicionInicial + tag.length());

			if (posicionFinal < 0)

				posicionFinal = textoMinuscula.length();


			if (posicionInicial > 0) {


				String textoCuenta = textoMinuscula.substring(posicionInicial,posicionFinal);

				if (textoCuenta.length() > 100) {

					String accountName = utilidades.getCadenaRecortada(utilidades.getLineasAnterior(textoMinuscula, textoCuenta,1),"|");

					//Se obtienen los datos para el historico de balance

					String regexhistorico ="\\d{2}/\\d{2}\\s+\\|\\s+[a-z|$|\\|\\s|0-9|,/]*";
					ArrayList<String> historicoBalance = new ArrayList<String>(); 
					Pattern patron = Pattern.compile(regexhistorico,Pattern.CASE_INSENSITIVE);
					Matcher matcher = patron.matcher(textoCuenta);

					while (matcher.find()) {

						String historico = matcher.group();
						historico = historico.replaceAll("\r|\n|\\|",  " ").replaceAll("auto ","auto\n").replaceAll("s1 ","\\$1,").replaceAll("( )+", " ");
						String lineas[] = historico.split("\n");

						for (String linea : lineas) {

							historicoBalance.add(linea.trim());

						}


					}


					if (accountName.contains("terms"))
						accountName =  utilidades.getCadenaRecortada(utilidades.getLineasAnterior(textoMinuscula, textoCuenta,2),"|");;

						//Se verifica si existe terms duration
						Pattern patronTerm = Pattern.compile("\\d{1,3}@mon[a-z]+",Pattern.CASE_INSENSITIVE);
						matcher = patronTerm.matcher(textoCuenta);
						if (matcher.find()) {

							String term = matcher.group();
							textoCuenta = textoCuenta.replaceAll(term, term.replaceAll("@", " "));

						}

						String linea = utilidades.getLineas(textoCuenta, 1);
						textoCuenta = textoCuenta.replace(linea, "");

						linea = utilidades.getLineas(textoCuenta, 1).replaceAll("( ){15,}","@@").replaceAll("( ){1,}","@");
						textoCuenta = textoCuenta.replace(linea, "");
						String reemplazar =  linea.substring(0,20).replaceAll("@@", "@");
						linea =  (reemplazar + linea.substring(21)).replaceAll("\"", "*");
						String primeraColumnas[] = linea.split("@");

						linea = utilidades.getLineas(textoCuenta, 1);
						textoCuenta = textoCuenta.replace(linea, "");

						linea = utilidades.getLineas(textoCuenta, 1);
						textoCuenta = textoCuenta.replace(linea, "");
						linea = utilidades.getLineas(textoCuenta, 1);
						textoCuenta = textoCuenta.replace(linea, "");
						linea = utilidades.getLineas(textoCuenta, 1).replaceAll("( ){10,}","@@").replaceAll("( ){1,}","@");
						textoCuenta = textoCuenta.replace(linea, "");
						String segundaColumna[] = linea.split("@");

						CuentaReporte cuentaReporte = new CuentaReporte();
						cuentaReporte.setAccountName(accountName);
						cuentaReporte.setAccountNumber(utilidades.getValor(primeraColumnas,0));
						cuentaReporte.setAccountType(utilidades.getCadenaRecortada(utilidades.getDatoHorizontal(textoCuenta.replaceAll("( ){2,}", " "), "type of account"),"type").replaceAll("-", "").trim());
						cuentaReporte.setAccountStatus(utilidades.getCadenaRecortada(utilidades.getDatoHorizontal(textoCuenta, "status"),"type").replaceAll("-", "").trim());
						cuentaReporte.setBalance(utilidades.getValor(segundaColumna,1));
						cuentaReporte.setLimit(utilidades.getValor(primeraColumnas,3));
						cuentaReporte.setMonthlyPayment(utilidades.getValor(segundaColumna,5));
						cuentaReporte.setPastDueAmount(utilidades.getValor(segundaColumna,2));
						cuentaReporte.setHighestBalance(utilidades.getValor(primeraColumnas,2));
						cuentaReporte.setTerms(utilidades.getValor(primeraColumnas,4).isEmpty()  ? utilidades.getValor(primeraColumnas,5) : utilidades.getValor(primeraColumnas,4));
						cuentaReporte.setComments(utilidades.getCadenaRecortada(utilidades.getDatoHorizontal(textoCuenta, "additional information"),"type").replaceAll("-", "").trim());
						cuentaReporte.setDateOpened(utilidades.getValor(primeraColumnas,1));
						cuentaReporte.setLoanType(utilidades.getCadenaRecortada(utilidades.getDatoHorizontal(textoCuenta, "loan"),"whose").replaceAll("-", "").trim());
						cuentaReporte.setDateClosed(utilidades.getValor(segundaColumna,13));
						cuentaReporte.setPaymentReceived(utilidades.getValor(segundaColumna,4));
						cuentaReporte.setLastActivity(utilidades.getValor(segundaColumna,7));
						cuentaReporte.setAccounttypeone("credit item");
						
						cuentasReporte.add(cuentaReporte);

				}

				posicionInicial  = posicionFinal;

			}


		} while (posicionInicial > 0);


	}


	/*******************************************************************************************************
	 *************************************** EXPERIAN ******************************************************
	 *******************************************************************************************************/
	private String scrapearExperianPatronUno(String textoMinuscula, PDDocument document) throws IOException {

		String[] elementos = patronExperian.getElementosAContenerTextoRepetidamentePatronUno();
		String[] tiposCuenta = {"adverse","satisfactory","inquiries","promocionalinquiries","reviewinquiries"};

		List<CuentaReporte> cuentasReporte = new ArrayList<CuentaReporte>();
		List<Inquiry> cuentasInquieries = new ArrayList<Inquiry>();

		Reporte reporte = new Reporte();
		reporte.setSsn(getSSN(textoMinuscula));
		reporte.setCra(patronExperian.getCra());
		reporte.setEmployeer(null);
		reporte.setName(utilidades.getDatoVertical(textoMinuscula,patronExperian.getTagNamePatronUno()).replaceAll("[0-9]+", ""));
		reporte.setAddress(getAddressExperianPatronUno(utilidades.getColumnaPDFPrimeraHoja(document,0,320),patronTransunion.getTagAddress()));
		reporte.setBirthyear(utilidades.getLineaAnterior(textoMinuscula,"year of birth"));
		reporte.setPatron("PatronUno");

		String primeraLinea = utilidades.getLineas(textoMinuscula,1);
		reporte.setDateofreport(utilidades.getFechaFormatoMesDiaAño(primeraLinea.substring(0,primeraLinea.indexOf(" ")),"/",true));



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

				CuentaReporte cuentaReporte = new CuentaReporte();
				cuentaReporte.setAccountName( utilidades.getDatoVertical(textoCuentaColumnaUno,elementos[0],2));

				//Se obtiene los datos de la columna dos
				String textoCuentaColumnaDos = columnaDos.substring(posicionInicialCuentaColumnaDos,posicionFinalCuentaDos);
				cuentaReporte.setAccountNumber(utilidades.getDatoVertical(textoCuentaColumnaDos,elementos[1]));
				cuentaReporte.setAccountType(utilidades.getDatoVertical(textoCuentaColumnaDos,elementos[6]));
				cuentaReporte.setTerms(utilidades.getDatoVertical(textoCuentaColumnaDos,elementos[10]));

				//Se obtiene los datos de la columna tres
				String textoCuentaColumnaTres = columnaTres.substring(posicionInicialCuentaColumnaTres,posicionFinalCuentaTres);
				cuentaReporte.setPaymentReceived(utilidades.getDatoVertical(textoCuentaColumnaTres,elementos[14]));
				cuentaReporte.setBalance(utilidades.getDatoVertical(textoCuentaColumnaTres,elementos[2],2));
				cuentaReporte.setLimit(utilidades.getDatoVertical(textoCuentaColumnaTres,elementos[7],3));
				cuentaReporte.setMonthlyPayment(utilidades.getDatoVertical(textoCuentaColumnaTres,elementos[12]) );
				cuentaReporte.setHighestBalance(utilidades.getDatoVertical(textoCuentaColumnaTres,elementos[9]) );



				//Se obtiene los datos de la columna tres
				String textoCuentaColumnaCuatro = columnaCuatro.substring(posicionInicialCuentaColumnaCuatro,posicionFinalCuentaCuatro);
				cuentaReporte.setStatusUpdated(utilidades.getFechaFormatoMesDiaAño(utilidades.getDatoVertical(textoCuentaColumnaCuatro,elementos[8]),"/",true));
				cuentaReporte.setResponsibility(utilidades.getDatoVertical(textoCuentaColumnaCuatro,elementos[10]));
				cuentaReporte.setDateOpened(utilidades.getFechaFormatoMesDiaAño(utilidades.getDatoVertical(textoCuentaColumnaCuatro,elementos[3]),"/",true));


				//Se obtiene los datos de la columna tres
				String textoCuentaColumnaCinco = columnaCinco.substring(posicionInicialCuentaColumnaCinco,posicionFinalCuentaCinco);
				cuentaReporte.setAccountStatus(utilidades.getDatoVertical(textoCuentaColumnaCinco,elementos[4]));
				cuentaReporte.setComments(utilidades.getDatoVertical(textoCuentaColumnaCinco,elementos[11]) );
				cuentaReporte.setAccounttypetwo(getTypeAccount(textosTiposCuentas, textoCuenta, tagFinales,tiposCuenta));


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

				cuentaReporte.setMesesCanceladosPorAno(mesesCanceladosPorAno);
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

					String accountName = utilidades.getDatoVertical(accountInquiriesColumnaUno, patronExperian.getTagAccountName()).replaceAll(reporte.getName(), "");

					int posicion = accountInquiriesColumnaUno.indexOf(accountName);
					String contactInformation = "";

					if (posicion>0)
						contactInformation = accountInquiriesColumnaUno.substring(posicion).replace(accountName, "").replaceAll("\r\n", "");

					String requestedon = utilidades.getDatoVertical(accountInquiriesColumnaDos, patronExperian.getTagAccountDateReport()).replaceAll("date|of|request\\(s\\)", "").replaceAll(",", " ").trim();
					String[] fechas = requestedon.split(" ");

					String comments = utilidades.getDatoVertical(accountInquiriesColumnaDos, "comments");
					String inquieresType = "inquiry";

					for (String fecha : fechas) {

						cuentasInquieries.add(new Inquiry(accountName, fecha, "", "", 
								contactInformation, inquieresType,comments,"",""));

					}

				}

				posicionInicialInquiriesColumnaUna = posicionFinalInquiriesColumnaUna +10 ;
				posicionInicialInquiriesColumnaDos = posicionFinalInquiriesColumnaDos +10 ;

			} while (posicionFinalInquiriesColumnaUna < columnaUno.length());


		}

		reporte.setMyhardcreditinquiries("" + cuentasInquieries.size());

		return generarJSON(reporte,cuentasReporte,cuentasInquieries,null,null);	
	}


	/*
	 *  Scraper experian patron dos
	 * @param archivo
	 * @param textoMinuscula
	 * @throws IOException
	 */
	private String scrapearExperianPatronDos(String textoMinuscula, PDDocument document,
			boolean poseeCuentas) throws IOException {

		String[] elementos = patronExperian.getElementosAContenerTextoRepetidamentePatronUno();
		String[] tiposCuenta = {"adverse","satisfactory","inquiries","promocionalinquiries","reviewinquiries"};
		String textoPublic = textoMinuscula;

		System.out.println(textoMinuscula);
		List<CuentaReporte> cuentasReporte = new ArrayList<CuentaReporte>();
		List<Inquiry> cuentasInquieries = new ArrayList<Inquiry>();
		List<PublicAccount> publicAccounts = new ArrayList<PublicAccount>();

		Reporte reporte = new Reporte();
		reporte.setSsn(getSSN(textoMinuscula));
		reporte.setCra(patronExperian.getCra());

		reporte.setName(utilidades.getDatoVertical(textoMinuscula, "names:"));
		reporte.setDateofreport(utilidades.getFechaFormatoMesDiaAño(utilidades.getDatoVertical(textoMinuscula, "report date").
				replaceAll("your report number is", "").trim()));
		reporte.setAddress(getAddressExperianPatronDos(textoMinuscula,"address:"));
		reporte.setBirthyear(utilidades.getDatoVertical(textoMinuscula,"year of birth"));

		reporte.setPatron("PatronDos");


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

					String textoCuenta = utilidades.eliminarFilasVacias(textoMinuscula.substring(posicionInicialCuenta,posicionFinalCuenta));

					if (textoCuenta.contains("account number") && textoCuenta.contains("type")) {

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

						CuentaReporte cuentaReporte = new CuentaReporte();
						cuentaReporte.setAccountName(accountName);
						cuentaReporte.setAccountNumber(accountNumber);
						cuentaReporte.setLoanType(utilidades.getDatoVertical(textoCuenta,"last reported"));
						cuentaReporte.setResponsibility(utilidades.getDatoVertical(textoCuenta,elementos[10]));
						cuentaReporte.setStatusDetail(utilidades.getDatoVertical(textoCuenta, "status details"));
						cuentaReporte.setAccountType(utilidades.getDatoVertical(textoCuenta,elementos[6]));
						cuentaReporte.setAccountStatus(utilidades.getDatoVertical(textoCuenta, "status").replaceAll("date|opened|:", ""));
						cuentaReporte.setPaymentStatus( utilidades.getDatoVertical(textoCuenta,elementos[14]));
						cuentaReporte.setStatusUpdated(utilidades.getFechaFormatoMesDiaAño(utilidades.getDatoVertical(textoCuenta,elementos[8]),"/",true));
						cuentaReporte.setBalance(utilidades.getDatoVertical(textoCuenta,elementos[2],2));
						cuentaReporte.setLimit(utilidades.getDatoVertical(textoCuenta,"credit limit/original amount"));
						cuentaReporte.setMonthlyPayment(utilidades.getDatoVertical(textoCuenta,"monthly payment"));
						cuentaReporte.setHighestBalance(utilidades.getDatoVertical(textoCuenta,elementos[9]));
						cuentaReporte.setTerms( utilidades.getDatoVertical(textoCuenta,elementos[10]));
						cuentaReporte.setYourStatement(textoCuenta.contains("payment history")  &&  textoCuenta.contains("statement") ?
								utilidades.eliminarRetornosCarro(textoCuenta.substring(textoCuenta.indexOf("statement")+9,
										textoCuenta.indexOf("payment history"))) :
											utilidades.getDatoVertical(textoCuenta,"your statement") );
						cuentaReporte.setComments(utilidades.getDatoHorizontal(textoCuenta,"comment") );
						cuentaReporte.setDateOpened(utilidades.getFechaFormatoMesDiaAño(utilidades.getDatoVertical(textoCuenta,elementos[3]),"/",true));
						cuentaReporte.setAccounttypetwo(getTypeAccount(textosTiposCuentas,textoCuenta,tagFinales,tiposCuenta));


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

						cuentaReporte.setMesesCanceladosPorAno(mesesCanceladosPorAno);
						cuentasReporte.add(cuentaReporte);

					}

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

					while (accountName.contains("page") || accountName.contains("report") || accountName.contains("https")) {

						posFinal = columnaUno.lastIndexOf("\n",posFinal-2);
						posInicial = columnaUno.lastIndexOf("\n",posFinal -2);
						accountName = columnaUno.substring(posInicial,posFinal).replaceAll("\r", "").replaceAll("\n", "");

					}

					String contactInformation = accountInquiriesColumnaUno.replaceAll("address:", "").replaceAll("\r", "").replaceAll("\n", "").trim();
					String requestedon = utilidades.getDatoVertical(accountInquiriesColumnaDos, patronExperian.getTagAccountDateReportPatronDos()).replaceAll(",", " ").trim();
					String[] fechas = requestedon.split(" ");
					String comments = utilidades.getDatoVertical(accountInquiriesColumnaDos, "comments");
					String inquieresType = "inquiry";

					for (String fecha : fechas) 

						cuentasInquieries.add(new Inquiry(accountName, fecha, "", "", 
								contactInformation, inquieresType,comments,"",""));

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
							PublicAccount publicAccount = new PublicAccount();
							publicAccount.setAccountName(getNombreCuentaPublicPatronDos(textoPublic,adddres));
							publicAccount.setReferenceNumber("identicationNumber " + utilidades.getDatoVertical(textoCuenta, "number:"));
							publicAccount.setStatus(utilidades.getCadenaRecortada(utilidades.getDatoHorizontal(textoCuenta,"status"),"details"));
							publicAccount.setStatus_detail(getStatusDetailsPatronDos(textoCuenta,"status details"));
							publicAccount.setDateResolved(utilidades.getDatoVertical(textoCuenta,"date resolved"));
							publicAccount.setResponsibility(utilidades.getDatoVertical(textoCuenta,"responsibility"));
							publicAccount.setLiability_amount(utilidades.getDatoVertical(textoCuenta,"liability amount"));
							publicAccount.setFilingDate(utilidades.getLineaAnterior(textoCuenta,"date resolved"));

							publicAccounts.add(publicAccount);

							posicionInicialCuenta = posicionFinalCuenta + 8;

						}


					} while (posicionInicialCuenta > 0);


				}

			}

		}

		//Se obtienen las cuentas publicas



		reporte.setMyhardcreditinquiries( "" + cuentasInquieries.size());


		return generarJSON(reporte, cuentasReporte, cuentasInquieries, publicAccounts,null);

	}



	/**
	 * Scraper experian patron cinco 
	 * @param archivo
	 * @param textoMinuscula
	 */
	private String scrapearExperianPatronCinco(String textoMinuscula, String textoConFormatoMinuscula,PDDocument document,
			boolean poseeCuentas) {


		String[] elementos = patronExperian.getElementosAContenerTextoRepetidamentePatronCinco();

		List<CuentaReporte> cuentasReporte = new ArrayList<CuentaReporte>();
		List<Inquiry> cuentasInquieries = new ArrayList<Inquiry>();

		Reporte reporte = new Reporte();
		reporte.setSsn(getSSN(textoMinuscula));
		reporte.setCra(patronExperian.getCra());
		reporte.setName(utilidades.getDatoVertical(textoMinuscula,"prepared for"));
		reporte.setDateofreport(utilidades.getFechaFormatoMesDiaAño(utilidades.getDatoHorizontal(textoMinuscula, "date generated")));
		reporte.setAddress(getAddressExperianPatronCinco(textoMinuscula,patronExperian.getTagAddressPatronCinco()));
		reporte.setBirthyear(utilidades.getDatoVertical(textoConFormatoMinuscula.replaceAll("year\\s+of\\s+birth", "year of birth"),"year of birth"));
		reporte.setPatron("PatronCinco");


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

					CuentaReporte cuentaReporte = new CuentaReporte();
					cuentaReporte.setAccountName(getAccountNamePatronCinco(textoCuenta,elementos[0]));
					cuentaReporte.setAccountNumber(utilidades.getDatoHorizontal(textoCuenta,elementos[1]));
					cuentaReporte.setAccountType(utilidades.getDatoHorizontal(textoCuenta,elementos[2]));
					cuentaReporte.setAccountStatus(utilidades.getDatoHorizontal(textoCuenta,elementos[4]) );
					cuentaReporte.setPaymentReceived(utilidades.getDatoHorizontal(textoCuenta,elementos[11]));
					cuentaReporte.setAccounttypetwo(textoCuenta.contains("negative") ? "Potentially negative" : "Potentially positive");
					cuentaReporte.setStatusUpdated(utilidades.getFechaFormatoMesDiaAño(utilidades.getDatoHorizontal(textoCuenta,elementos[7]),"/",true));
					cuentaReporte.setBalance(utilidades.getDatoHorizontal(textoCuenta,elementos[5]) );
					cuentaReporte.setBalanceUpdated( utilidades.getFechaFormatoMesDiaAño(utilidades.getDatoHorizontal(textoCuenta,elementos[12]),"/",true));
					cuentaReporte.setLimit( utilidades.getDatoHorizontal(textoCuenta,elementos[6]));
					cuentaReporte.setMonthlyPayment(utilidades.getDatoHorizontal(textoCuenta,elementos[13]) );
					cuentaReporte.setHighestBalance(utilidades.getDatoHorizontal(textoCuenta,elementos[8]) );
					cuentaReporte.setTerms(utilidades.getDatoHorizontal(textoCuenta,elementos[9]) );
					cuentaReporte.setDateOpened(utilidades.getFechaFormatoMesDiaAño(utilidades.getDatoHorizontal(textoCuenta,elementos[3]),"/",true));
					cuentaReporte.setResponsibility(utilidades.getDatoHorizontal(textoCuenta,elementos[10]));


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


					cuentasReporte.add(cuentaReporte);
				}

			} while (posicionInicialCuenta>0);

		}

		//*************** Se insertan las cuentas inquiries ********************************** 
		String hardInquiries = "hard inquiries";
		String softInquiries = "soft inquiries";

		//Se obtienen las etiquetas
		Matcher hardInquiriesMatcher = utilidades.getPatronHardInquiries().matcher(textoConFormatoMinuscula.substring(1000));
		Matcher softInquiriesMatcher = utilidades.getPatronSoftInquiries().matcher(textoConFormatoMinuscula.substring(1000));

		if (hardInquiriesMatcher.find())
			hardInquiries = hardInquiriesMatcher.group();

		if (softInquiriesMatcher.find())
			softInquiries = softInquiriesMatcher.group();


		String[] tagFinales = {hardInquiries,softInquiries};
		int posicionInicialHardAccount = textoConFormatoMinuscula.lastIndexOf(tagFinales[0]);
		int posicionInicialSoftAccount = textoConFormatoMinuscula.lastIndexOf(tagFinales[1]);

		int[] posicionesInicialesTiposCuentas = new int[] {posicionInicialHardAccount,posicionInicialSoftAccount};

		String[] textosTiposCuentas = new String[2];
		textosTiposCuentas[0] = posicionInicialHardAccount < 0 ? "" : getFormatearTexto(textoConFormatoMinuscula.substring(posicionInicialHardAccount,utilidades.getPrimerValorPositiva(posicionesInicialesTiposCuentas,0,textoConFormatoMinuscula.length())));
		textosTiposCuentas[1] = posicionInicialSoftAccount < 0 ? "" : getFormatearTexto(textoConFormatoMinuscula.substring(posicionInicialSoftAccount,utilidades.getPrimerValorPositiva(posicionesInicialesTiposCuentas,1,textoConFormatoMinuscula.length())));

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

				String requestedOn = buffer.toString().replaceFirst(",", " ").trim();
				String[] fechas = requestedOn.split(" ");
				contactInformation = contactInformation.trim();

				for (String fecha : fechas) 

					cuentasInquieries.add(new Inquiry(accountName, fecha, "", "", 
							contactInformation, inquieresType,"","",""));

			}


			if (posicionInicialInquiries > 0 ) {
				posicionInicialInquiries = posicionFinalInquiries ;

			}

		} while (posicionFinalInquiries < textoCuentaInquiries.length() && posicionInicialInquiries > 0);

		return generarJSON(reporte,cuentasReporte,cuentasInquieries,null,null);	
	}

	/*****************************************************************************************************************************
	 *************************************** Transunion **************************************************************************
	 *****************************************************************************************************************************/

	/**
	 * Scraper transunion tipo Uno
	 * @param archivo
	 * @param textoMinuscula
	 * @throws IOException 
	 */
	private String scrapearTransunionPatronUno(String textoMinuscula, String textoMinusculaConFormato,PDDocument document,
			boolean poseeCuentas) throws IOException {


		String[] elementos = patronTransunion.getElementosAContenerTextoRepetidamente();
		String[] elementosReporte = patronTransunion.getElementosAdicionalesPatronGenerico();
		String[] tiposCuenta = {"adverse","satisfactory","inquiries","promocionalinquiries","reviewinquiries"};

		List<CuentaReporte> cuentasReporte = new ArrayList<CuentaReporte>();
		List<Inquiry> cuentasInquieries = new ArrayList<Inquiry>();


		//****************** Se definen los datos del reporte ****************************************************

		Reporte reporte = new Reporte();
		reporte.setSsn(getSSN(textoMinuscula));
		reporte.setCra(patronTransunion.getCra());
		reporte.setEmployeer(getNombreEmpleadorTransunion(textoMinuscula,"employer"));
		reporte.setName(utilidades.getDatoHorizontal(textoMinuscula,patronTransunion.getTagName()));
		reporte.setDateofreport(utilidades.getFechaFormatoMesDiaAño(utilidades.getDatoHorizontal(textoMinuscula,elementosReporte[1]),"/",true));
		reporte.setAddress(getAddressTransunionPatronUno(textoMinuscula,patronTransunion.getTagAddress()));
		reporte.setBirthyear(utilidades.getDatoHorizontal(textoMinuscula,elementosReporte[0]));
		reporte.setPatron("PatronUno");



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


					CuentaReporte cuentaReporte = new CuentaReporte();
					cuentaReporte.setAccountName(getAccountNameTransunion(textoMinuscula, numeroCuentas.get(i)) );
					cuentaReporte.setAccountType(utilidades.getDatoHorizontal(textoCuenta,elementos[2]) );
					cuentaReporte.setPaymentStatus(utilidades.getDatoHorizontal(textoCuenta,elementos[12]) );
					cuentaReporte.setStatusUpdated(utilidades.getDatoHorizontal(textoCuenta,elementos[5]));
					cuentaReporte.setBalance(getBalanceTransunion(textoCuenta) ?  utilidades.getDatoHorizontal(textoCuenta,elementos[4]).replaceAll(":|$", "").trim() : "");
					cuentaReporte.setPaymentReceived(utilidades.getDatoHorizontal(textoCuenta,elementos[6]));
					cuentaReporte.setLimit( utilidades.getDatoHorizontal(textoCuenta,elementos[10]));
					cuentaReporte.setPastDueAmount(utilidades.getDatoHorizontal(textoCuenta,elementos[11]) );
					cuentaReporte.setHighestBalance(utilidades.getDatoHorizontal(textoCuenta,elementos[8]) );
					cuentaReporte.setTerms(utilidades.getDatoHorizontal(textoCuenta,elementos[13]) );
					cuentaReporte.setResponsibility(utilidades.getDatoHorizontal(textoCuenta,elementos[1]) );
					cuentaReporte.setDateOpened(utilidades.getDatoHorizontal(textoCuenta,elementos[0]));
					cuentaReporte.setDateClosed(utilidades.getDatoHorizontal(textoCuenta,elementos[14]));
					cuentaReporte.setOriginalCreditor(utilidades.getDatoHorizontal(textoCuenta,elementos[9]));
					cuentaReporte.setAccounttypetwo(getTypeAccount(textosTiposCuentas,textoCuenta,tagFinales,tiposCuenta));
					cuentaReporte.setLoanType(utilidades.getDatoHorizontal(textoCuenta,elementos[3]));


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


					cuentasReporte.add(cuentaReporte);

				}

				posicionInicial=posicionFinal;
				posicionInicialConFormato = posicionFinalConFormato;



			}

		}
		//*************** Se obtienen los datos del Inquiries datos ***********************************
		getAccountsInquieriesTransunionPatronUno(textosTiposCuentas[2],"inquiry type","regular inquiries",cuentasInquieries);
		getAccountsInquieriesTransunionPatronUno(textosTiposCuentas[3],"requested on","promotional inquiries",cuentasInquieries);
		getAccountsInquieriesTransunionPatronUno(textosTiposCuentas[4],"requested on","account review inquiries",cuentasInquieries);

		reporte.setMyhardcreditinquiries("" + cuentasInquieries.size());

		return generarJSON(reporte, cuentasReporte, cuentasInquieries, null,null);

	}

	/*
	 * Scraper transunion tipo tres
	 * @param archivo
	 * @param textoMinuscula
	 * @throws IOException 
	 */
	private void scrapearTransunionPatronTres(String textoMinuscula, boolean fileOCR, String idioma) throws IOException {

		if (fileOCR) 

			textoMinuscula = idioma.equals("es") ? utilidades.getFormatearTexto(textoMinuscula,utilidades.getPalabrasCorregirEspañol()) :
				utilidades.getFormatearTexto(textoMinuscula,utilidades.getPalabrasCorregirIngles())    ;



			//*************************** Se definen los datos del reporte ****************************************************
			String[] tiposCuentaCuantro = {"adversas","satisfactorias"};

			Reporte reporte = new Reporte();
			reporte.setSsn(getSSN(textoMinuscula));
			reporte.setCra(patronTransunion.getCra());
			reporte.setEmployeer(getNombreEmpleadorTransunionPatronTres(textoMinuscula,patronTransunion.getTagNamePatronTres()));
			reporte.setName(utilidades.getDatoHorizontal(textoMinuscula,patronTransunion.getTagNamePatronTres()));
			reporte.setDateofreport(utilidades.getDatoHorizontal(textoMinuscula,"fecha de emision"));
			reporte.setAddress(getAddressTransunionPatronTres(textoMinuscula,patronTransunion.getTagAddressPatronTres()));
			reporte.setBirthyear(utilidades.getDatoHorizontal(textoMinuscula,"fecha de nacimiento"));
			reporte.setPatron("PatronTres");


			String[] tagFinales = {"adversa","satisfactoria"};

			// Se divide el documento para poder clasificar los tipos de cuentas
			int posicionInicialAdverseAccount = textoMinuscula.indexOf(tagFinales[0]);
			int posicionInicialSatisfactoryAccount = textoMinuscula.indexOf(tagFinales[1]);

			if (posicionInicialAdverseAccount > posicionInicialSatisfactoryAccount) 

				posicionInicialAdverseAccount = -1;


			int[] posicionesInicialesTiposCuentas = new int[] {posicionInicialAdverseAccount,posicionInicialSatisfactoryAccount};

			String[] textosTiposCuentas = new String[2];
			textosTiposCuentas[0] = posicionInicialAdverseAccount < 0 ? "" : getFormatearTexto(textoMinuscula.
					substring(posicionInicialAdverseAccount,
							utilidades.getPrimerValorPositiva(posicionesInicialesTiposCuentas,0,textoMinuscula.length())));
			textosTiposCuentas[1] = posicionInicialSatisfactoryAccount < 0 ? "" : getFormatearTexto(textoMinuscula.substring(posicionInicialSatisfactoryAccount,utilidades.getPrimerValorPositiva(posicionesInicialesTiposCuentas,1,textoMinuscula.length())));


			int posicionInicioBusquedaTag = 0;
			int posicionInicialCuenta = 0;
			int posicionFinalCuenta = 0;

			do {

				posicionInicialCuenta = textoMinuscula.indexOf("apertura",posicionInicioBusquedaTag);

				if (posicionInicialCuenta > 0) {

					posicionFinalCuenta = textoMinuscula.indexOf("apertura",posicionInicialCuenta+10);

					if (posicionFinalCuenta < 0) // En caso que ya se termina y no halla mas cuenta

						posicionFinalCuenta = textoMinuscula.length();

					String textoCuenta = textoMinuscula.substring(posicionInicialCuenta,posicionFinalCuenta);
					String accountName = utilidades.getLineasAnterior(textoMinuscula, textoCuenta, 1);
					String accountNumber = "";

					Matcher matcher = utilidades.getPatronNumeroCuenta().matcher(accountName);

					if (matcher.find()) {

						accountNumber = matcher.group();
						accountName = accountName.substring(0, accountName.indexOf(accountNumber));
					}

					CuentaReporte cuentaReporte = new CuentaReporte();
					cuentaReporte.setAccountName(accountName );
					cuentaReporte.setAccountNumber(accountNumber );
					cuentaReporte.setAccountType(utilidades.getCadenaRecortada(utilidades.getCadenaRecortada(utilidades.getCadenaRecortada(utilidades.getDatoHorizontal(textoCuenta,"de cuenta"),"pagado"),"ultimo"),"pago"));
					cuentaReporte.setPaymentStatus(utilidades.getDatoHorizontal(textoCuenta,"estado de pago") );
					cuentaReporte.setStatusUpdated(utilidades.getCadenaRecortada(utilidades.getCadenaRecortada(utilidades.getDatoHorizontal(textoCuenta,"actualizacion")," "),"estado"));
					cuentaReporte.setBalance(utilidades.getCadenaRecortada(utilidades.getDatoHorizontal(textoCuenta,"saldo"),"estado") );
					cuentaReporte.setLimit( utilidades.getCadenaRecortada(utilidades.getDatoHorizontal(textoCuenta,"credito"),"ultimo"));
					cuentaReporte.setPastDueAmount(utilidades.getCadenaRecortada(utilidades.getDatoHorizontal(textoCuenta,"realizado")," ") );
					cuentaReporte.setHighestBalance( utilidades.getDatoHorizontal(textoCuenta,"alto"));
					cuentaReporte.setTerms( utilidades.getDatoHorizontal(textoCuenta,"terminos"));
					cuentaReporte.setComments(utilidades.getDatoHorizontal(textoCuenta,"observaciones") );
					cuentaReporte.setDateOpened(utilidades.getCadenaRecortada(utilidades.getDatoHorizontal(textoCuenta,"apertura")," "));
					cuentaReporte.setAccounttypeone(utilidades.getDatoHorizontal(utilidades.getDatoHorizontal(utilidades.getDatoHorizontal(textoCuenta,"prestamo"),"ultimo"),"saldo"));
					cuentaReporte.setAccounttypetwo(getTypeAccount(textosTiposCuentas,textoCuenta,tagFinales,tiposCuentaCuantro));
					cuentaReporte.setPaymentReceived(utilidades.getCadenaRecortada(utilidades.getDatoHorizontal(textoCuenta,"recibido")," "));
					cuentaReporte.setDateClosed(utilidades.getDatoHorizontal(textoCuenta,"cierre"));
					cuentaReporte.setResponsibility(utilidades.getCadenaRecortada(utilidades.getCadenaRecortada(utilidades.getCadenaRecortada(utilidades.getDatoHorizontal(textoCuenta,"responsabilidad"),"pagado"),"fecha"),"ultimo"));


				}

				posicionInicioBusquedaTag = posicionFinalCuenta+5;

			}while (posicionInicialCuenta>0);


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

	private String getCreditFileStatus(String textoMinuscula) {

		String resultado = "";

		int posicionInicial = textoMinuscula.indexOf("credit file status");
		int posicionFinal = textoMinuscula.indexOf("alert contacts");
		posicionFinal = posicionFinal < 0 ? textoMinuscula.indexOf("average account") : posicionFinal;

		if (posicionInicial > 0) {

			if (posicionFinal > 0) 

				resultado = textoMinuscula.substring(posicionInicial,posicionFinal).replaceAll("credit file status", "").trim();

			else

				resultado = utilidades.getDatoHorizontal(textoMinuscula, "credit file status");



		}

		return resultado;
	}

	/*****************************************************************************************************
	 **************************************** Metodos Patron EquiFax Tres *********************************
	 *****************************************************************************************************/
	private String getNombreEmpleadoEquifaxPatronTres(String textoMinuscula) {


		String resultado = textoMinuscula.indexOf("dear") > 0 ? utilidades.getDatoHorizontal(textoMinuscula, "dear") :
			utilidades.getDatoHorizontal(textoMinuscula, "on file");	

		return resultado;

	}


	private String[] getAddressEquifaxPatronTres(String textoMinuscula) {

		ArrayList<Object> address = new ArrayList<Object>();

		int posicionPrevious = textoMinuscula.indexOf("previous address");
		int posicionFinal = posicionPrevious > 0 ? textoMinuscula.indexOf("formerly") : 0;

		if (posicionPrevious > 0) {

			String[] addressPrevius = textoMinuscula.substring(posicionPrevious, posicionFinal).replaceAll("previous|address", "").split("\n");
			address.addAll(Arrays.asList(addressPrevius));

		}

		String currentAddres = utilidades.getDatoHorizontal(textoMinuscula, "current address");

		if (!currentAddres.isBlank()) 

			address.add(currentAddres);

		return address.toArray(new String[address.size()]);
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

				String direccion = utilidades.eliminarRetornosCarro(textoMinuscula.substring(posicionInicial,posicionFinal).replaceAll("address:|�", "")).trim();
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

	/*************************************************************************************************************************
	 **************************************** Metodos Patron TransUnion Uno **************************************************
	 *************************************************************************************************************************/

	/**
	 * Nombre del empleador para transunion
	 * @return
	 * @throws IOException 
	 */
	private String[] getNombreEmpleadorTransunion(String textoMinuscula,String tag) throws IOException {

		String address[] = null;

		int posicionInicialAddress = textoMinuscula.indexOf(tag);
		int posicionFinalAddress =  textoMinuscula.indexOf("typically");

		if (posicionFinalAddress < 0) 

			posicionFinalAddress =  textoMinuscula.indexOf("rating");

		if (posicionInicialAddress > 0 && posicionInicialAddress < posicionFinalAddress) {

			String addressText = textoMinuscula.substring(posicionInicialAddress,posicionFinalAddress).
					replaceAll("employer|name|date|location|position|verified","").trim();

			address = addressText.split("\n");

		}

		return address;

	}


	private String[] getAddressTransunionPatronUno(String textoMinuscula,String tag) {

		String address[] = null;

		int posicionInicialAddress = textoMinuscula.indexOf(tag);
		int posicionFinalAddress =  textoMinuscula.indexOf("telephone numbers reported");

		if (posicionFinalAddress < 0) 

			posicionFinalAddress =  textoMinuscula.indexOf("employment data reported");

		if (posicionInicialAddress > 0 && posicionInicialAddress < posicionFinalAddress) {

			String addressText = textoMinuscula.substring(posicionInicialAddress,posicionFinalAddress).
					replaceAll("addresses|address|date|reported|:|\r","").trim();

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
	private void getAccountsInquieriesTransunionPatronUno(String datosInquieres, String tag,String type,List<Inquiry> cuentasInquieries) {

		int posicionInicialTag = 0;


		boolean sw = true;

		do {

			int posicionFinalTag = datosInquieres.indexOf(tag,posicionInicialTag);
			String lineaFecha = "";

			if (posicionFinalTag > 0) {

				posicionFinalTag = datosInquieres.indexOf("\n",posicionFinalTag);

				//Se revisa si se tienen mas datos en la cuenta de Inquieres
				int posicionFinalTagMasDatos = datosInquieres.indexOf("\n",posicionFinalTag+2);

				if (posicionFinalTagMasDatos > 0) {

					String linea = datosInquieres.substring(posicionFinalTag,posicionFinalTagMasDatos);
					Matcher matcher = utilidades.getPatronFechaMMDDYYYYY().matcher(linea);

					if (matcher.find()) {
						posicionFinalTag = posicionFinalTagMasDatos;

						lineaFecha = utilidades.eliminarRetornosCarro(linea);
					}

				}


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

				String requestedOn = (utilidades.getDatoHorizontal(textoCuentaInquiries, "requested on") + lineaFecha).replaceAll(",", " ").trim();
				String[] fechasRequestedOn = requestedOn.split(" ");
				String inquieresType = utilidades.getDatoHorizontal(textoCuentaInquiries, "inquiry type");
				String permissible = utilidades.getDatoHorizontal(textoCuentaInquiries, "permissible purpose");
				String contactInformation = textoCuentaInquiries.substring(textoCuentaInquiries.indexOf(account),
						textoCuentaInquiries.indexOf("requested on")).replace(account, "").replaceAll("\r\n", " ");

				for (String fechaRequestOn : fechasRequestedOn) 

					cuentasInquieries.add(new Inquiry(utilidades.eliminarRetornosCarro(account), fechaRequestOn, "",
							"", contactInformation, type,"",inquieresType,permissible));




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

	/*************************************************************************************************************************
	 **************************************** Metodos Patron TransUnion Tres **************************************************
	 *************************************************************************************************************************/

	private String[] getNombreEmpleadorTransunionPatronTres(String textoMinuscula,String tag) {

		String employer[] = null;

		int posicionInicialNombre = textoMinuscula.indexOf(tag);
		int posicionFinalNombre = textoMinuscula.indexOf("fecha de verificacion",posicionInicialNombre);

		if (posicionInicialNombre > 0 && posicionInicialNombre < posicionFinalNombre)

			employer = textoMinuscula.substring(posicionInicialNombre,posicionFinalNombre).split("\n");

		return employer;
	}

	private String[] getAddressTransunionPatronTres(String textoMinuscula,String tag) {

		String[] result = null;
		int posicionInicialAddress = textoMinuscula.indexOf(tag);
		int posicionFinalAddress =  textoMinuscula.indexOf("numero",posicionInicialAddress);

		if (posicionInicialAddress> 0 && posicionInicialAddress < posicionFinalAddress) {
			List<String> address = new ArrayList<String>();

			String texto = textoMinuscula.substring(posicionInicialAddress,posicionFinalAddress).replaceAll("\r","");
			String[] direcciones = texto.split("\n");

			for (String direccion : direcciones) {

				if (!direccion.isEmpty() && !direccion.contains("domicilio") && !direccion.replaceAll(" ", "").isEmpty())
					address.add(direccion);
			}

			result = address.toArray(new String[address.size()]);
		}

		return result;

	}


	/*******************************************************************************************************************************************
	 **************************************** Metodos Patron Experian Cinco ********************************************************************
	 *******************************************************************************************************************************************/

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
		jsonReporte.put("credit_file_date",credit_file_date);
		jsonReporte.put("pattern","none");

		return jsonReporte.toString();
	}

	private String generarJSON(Reporte reporte,List<CuentaReporte> cuentasReporte, 
			List<Inquiry> cuentaInquieries, List<PublicAccount> publicAccounts,
			List<Collection> collectionsAccounts) {


		String previous_address = "";
		String current_address = "";
		String last_reported_employment = "";
		String previous_reported_employment = "";


		int length;

		if (reporte.getAddress() !=null  &&  (length = reporte.getAddress().length) > 0) {

			length--;
			current_address = reporte.getAddress()[length];

			length--;
			if (length >= 0)
				previous_address = reporte.getAddress()[length];

		}

		if (reporte.getEmployeer()!=null && (length = reporte.getEmployeer().length) > 0) {

			length--;
			last_reported_employment = reporte.getEmployeer()[length];

			length--;
			if (length >= 0)
				previous_reported_employment = reporte.getEmployeer()[length];
		}

		/*************************************************************************************************************
		 **************************************** Datos Encabezado del Reporte ***************************************
		 *************************************************************************************************************/

		JSONObject jsonReporte = utilidades.getJSONObjectOrdenNatural();


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


			JSONObject jsonCuentaReporte = utilidades.getJSONObjectOrdenNatural();

			jsonCuentaReporte.put("account_name",cuentaReporte.getAccountName());
			jsonCuentaReporte.put("item_type","Credit");
			jsonCuentaReporte.put("date",cuentaReporte.getDateOpened());
			jsonCuentaReporte.put("date_closed_at",cuentaReporte.getDateClosed());
			jsonCuentaReporte.put("account_number",cuentaReporte.getAccountNumber().isBlank() ? "NO DEFINED" : cuentaReporte.getAccountNumber() );
			jsonCuentaReporte.put("account_type",cuentaReporte.getAccountType());
			jsonCuentaReporte.put("status",cuentaReporte.getAccountStatus());
			jsonCuentaReporte.put("status_updated_at",cuentaReporte.getStatusUpdated());
			jsonCuentaReporte.put("status_detail",cuentaReporte.getStatusDetail());
			jsonCuentaReporte.put("balance",cuentaReporte.getBalance().isBlank() ? "" : getNumeroReal(cuentaReporte.getBalance()));
			jsonCuentaReporte.put("balance_updated_at",cuentaReporte.getBalanceUpdated());
			jsonCuentaReporte.put("limit",cuentaReporte.getLimit().isBlank() ? "" : getNumeroReal(cuentaReporte.getLimit()));
			jsonCuentaReporte.put("monthly_payment",cuentaReporte.getMonthlyPayment().isBlank() ? "" : getNumeroReal(cuentaReporte.getMonthlyPayment()));
			jsonCuentaReporte.put("past_due_amount",cuentaReporte.getPastDueAmount().isBlank() ? "" : getNumeroReal(cuentaReporte.getPastDueAmount()));
			jsonCuentaReporte.put("highest_balance",cuentaReporte.getHighestBalance().isBlank() ? "" : getNumeroReal(cuentaReporte.getHighestBalance()));
			jsonCuentaReporte.put("terms",cuentaReporte.getTerms());
			jsonCuentaReporte.put("responsibility",cuentaReporte.getResponsibility());
			jsonCuentaReporte.put("statement",cuentaReporte.getYourStatement());
			jsonCuentaReporte.put("comments",cuentaReporte.getComments());
			jsonCuentaReporte.put("loan_type",cuentaReporte.getLoanType());
			jsonCuentaReporte.put("total_of_payment_received",cuentaReporte.getPaymentReceived().isBlank() ? "" : getNumeroReal(cuentaReporte.getPaymentReceived()));
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






		/*************************************************************************************************************
		 ******************************** Datos Cuentas de Inquirires del Reporte ***************************************
		 *************************************************************************************************************/

		if (cuentaInquieries != null) {

			for (Inquiry inquiery : cuentaInquieries) {

				JSONObject jsonCuentaInquiries = utilidades.getJSONObjectOrdenNatural();

				jsonCuentaInquiries.put("account_name",inquiery.getAccountName()); 
				jsonCuentaInquiries.put("item_type","Inquiry");
				jsonCuentaInquiries.put("date",inquiery.getInquiryDate()); 
				jsonCuentaInquiries.put("date_closed_at",inquiery.getRemovalDate());
				jsonCuentaInquiries.put("comments",inquiery.getComments());
				jsonCuentaInquiries.put("business_type",inquiery.getBusinessType());
				jsonCuentaInquiries.put("contact_information",inquiery.getContactInformation()); 
				jsonCuentaInquiries.put("account_class_one",inquiery.getInquieresType());
				jsonCuentaInquiries.put("account_class_two",inquiery.getInquieresTypeTwo());
				jsonCuentaInquiries.put("permissible_purpose",inquiery.getPermissible_purpose());

				cuentasReporteArray.put(jsonCuentaInquiries);

			}

		}



		/*************************************************************************************************************
		 ******************************** Datos Cuentas de Publicas del Reporte ***************************************
		 *************************************************************************************************************/


		if (publicAccounts != null) {

			for (PublicAccount publicAccount : publicAccounts) {

				JSONObject publicAccountReporte = utilidades.getJSONObjectOrdenNatural();

				publicAccountReporte.put("account_name",publicAccount.getAccountName());
				publicAccountReporte.put("item_type","Public");
				publicAccountReporte.put("date",publicAccount.getFilingDate());
				publicAccountReporte.put("date_closed_at",publicAccount.getDateResolved());
				publicAccountReporte.put("claim_amount",publicAccount.getClaim_amount());
				publicAccountReporte.put("liability_amount",publicAccount.getLiability_amount());
				publicAccountReporte.put("judgment_in_favor",publicAccount.getJudgment_in_favor());
				publicAccountReporte.put("referencenumber",publicAccount.getReferenceNumber());
				publicAccountReporte.put("court",publicAccount.getCourt());
				publicAccountReporte.put("plain_tiff",utilidades.eliminarMasDeDosEspaciosEnTexto(publicAccount.getPlaintiff()));
				publicAccountReporte.put("status",publicAccount.getStatus());
				publicAccountReporte.put("status_detail",publicAccount.getStatus_detail());
				publicAccountReporte.put("responsibility",publicAccount.getResponsibility());
				cuentasReporteArray.put(publicAccountReporte);

			}

		}
		
		/*************************************************************************************************************
		 ******************************** Datos Cuentas Collections ***************************************
		 *************************************************************************************************************/

		if (collectionsAccounts != null) {
			
			for (Collection collection : collectionsAccounts) {
				
				JSONObject collectionAccountReporte = utilidades.getJSONObjectOrdenNatural();

				collectionAccountReporte.put("account_name",collection.getAccountName());
				collectionAccountReporte.put("item_type","Collection");
				collectionAccountReporte.put("date",collection.getDateAssigned());
				collectionAccountReporte.put("account_number",collection.getAccountNumber().isBlank() ? "NO DEFINED" : collection.getAccountNumber() );
				collectionAccountReporte.put("status",collection.getStatus());
				collectionAccountReporte.put("status_updated_at",collection.getStatusDate());
				collectionAccountReporte.put("balance",collection.getAmount().isBlank() ? "" : getNumeroReal(collection.getAmount()));
				collectionAccountReporte.put("balance_updated_at",collection.getBalanceDate());
				collectionAccountReporte.put("comments",collection.getComments());
				collectionAccountReporte.put("last_activity",collection.getLastPaymentDate());
				collectionAccountReporte.put("original_creditor",collection.getOriginalCreditorName());
				collectionAccountReporte.put("account_class_one",collection.getAccountDesignerCode());
				collectionAccountReporte.put("account_class_two",collection.getCreditorClassification());
				collectionAccountReporte.put("claim_amount",collection.getOriginalAmountOwned());
				collectionAccountReporte.put("date_reported",collection.getDateReported());
				collectionAccountReporte.put("date_of_first_delinquency",collection.getDateOfFirstDelinquency());
				collectionAccountReporte.put("contact_information",collection.getContactInformation());
				cuentasReporteArray.put(collectionAccountReporte);
				
				
			}
			
			
		}
		
		
		jsonReporte.put("credit_report_items",cuentasReporteArray);


		return jsonReporte.toString();
	}

	private String getTextoConFormato(PDDocument document) throws IOException {

		LayoutTextStripper stripper = new LayoutTextStripper();
		stripper.setSortByPosition(true);

		PosicionTextoPDF posicionTextoPDF = new PosicionTextoPDF(document, 1, 1);
		stripper.fixedCharWidth = posicionTextoPDF.getTamañoLetra() > 20 ? 20 : 5;
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

	private String getFormatearTexto(String texto) {

		texto = texto.replaceAll("consumer initiated\r\ntransaction","consumer initiated transaction");
		texto = texto.replaceFirst("\n", "");
		texto = texto.substring(texto.indexOf("\n")+1);

		return texto;

	}


	//**************************************************************************************************************************************



	//******************************************* Metodos OCR ******************************************************************************

	private DataOCRFile getProcesarArchivoOCR(PDDocument document,String nombreArchivo, File tmpFile) {

		DataOCRFile dataOCRFile = utilidades.extraerTextoOCR(document,nombreArchivo,tmpFile);

		return dataOCRFile;

	}

	//*************************************************************************************************

}
