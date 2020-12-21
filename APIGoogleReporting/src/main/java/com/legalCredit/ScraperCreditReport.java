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
import java.net.HttpURLConnection;

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

	
}
