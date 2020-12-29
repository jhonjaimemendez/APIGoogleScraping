/**
 * @Clase: ScraperCreditReport.java
 * 
 * @version  1.0
 * 
 * @since 12/12/2020
 * 
 * @autor Ing. Jhon Mendez
 *
 * @Copyrigth: Legal Credit Solution
 */


package com.legalCredit;

/**
 * Clase publicada como funtion en Google Cloud
 *
 */

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.util.Base64;

import org.json.JSONObject;

import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;



public class ScraperCreditReport implements HttpFunction {


	@Override
	public void service(HttpRequest request, HttpResponse response) throws Exception {

		String result = null;

		String contentType = request.getContentType().orElse("");

		switch (contentType) {


		case "text/plain":

			StringBuffer contenido = new StringBuffer();
			request.getReader().lines().forEach(p -> contenido.append(p));


			result = new ScrapingPDF().scrapingPDF(contenido.toString());
			break;


		default:

			response.setStatusCode(HttpURLConnection.HTTP_BAD_REQUEST);
			return;
		}

		BufferedWriter writer = response.getWriter();
		writer.write(result);
		
	}
	
	
	public void scraper(String textoPDFBase64) throws Exception {

		JSONObject jsonReporte = new JSONObject();
		jsonReporte.put("bureau_id","1") ;
		jsonReporte.put("credit_file_date","13") ;
		jsonReporte.put("file", textoPDFBase64);
		
		String result = new ScrapingPDF().scrapingPDF(jsonReporte.toString());

		
	}
	
	/**
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		File file = new File("reportes/reportesScrapeados/equifax/creditReport_1485542551006.pdf");
		FileInputStream fileInputStream = new FileInputStream(file);
		byte[] bytes = new byte[(int)file.length()];
		fileInputStream.read(bytes);
		fileInputStream.close();

		String encoded = Base64.getEncoder().encodeToString(bytes);
		new ScraperCreditReport().scraper(encoded);

	}

	
}
