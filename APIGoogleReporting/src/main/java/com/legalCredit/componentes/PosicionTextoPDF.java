/**
 * @Clase: PosicionTextoPDF.java
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
 * Clase que obtiene las palabras que contiene un PDF
 */

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;


public class PosicionTextoPDF extends PDFTextStripper {

	private List<PosicionTexto> palabras;
	int e = 1;
	
	public PosicionTextoPDF(PDDocument documento, int paginaInicial, int paginaFinal) throws IOException {

		palabras = new ArrayList<PosicionTexto>();
		
		setSortByPosition( true );
		setStartPage(paginaInicial);
		setEndPage(paginaFinal);

		Writer dummy = new OutputStreamWriter(new ByteArrayOutputStream());
		writeText(documento, dummy);
	}

	/**
	 * Override the default functionality of PDFTextStripper.
	 */
	@Override
	protected void writeString(String string, List<TextPosition> posicionTextos) throws IOException {

		StringBuffer buffer = new StringBuffer();

		double posXInicial = 0;
		double posYInicial = 0;
		
		boolean sw = true;
		
		TextPosition textoUltimaPosicion = null;
		
		for (TextPosition texto : posicionTextos) {
			
			if (sw) {

				posXInicial = texto.getXDirAdj();
				posYInicial = texto.getYDirAdj();
				sw = false;
			} 

			buffer.append(texto.getUnicode());
			
			textoUltimaPosicion = texto;

		}
		

		PosicionTexto posicionTextoPDF = new PosicionTexto(buffer.toString(), posXInicial, 
				posYInicial, textoUltimaPosicion.getXDirAdj(), textoUltimaPosicion.getYDirAdj());
		palabras.add(posicionTextoPDF);
		
		

	}
	
	public PosicionTexto buscarPosicionPalabra(String palabra, int posicion) {
		
		PosicionTexto posicionTexto = null;
		
		
		int i = 0;
		int posicionPalabraArrayList = 0;
		boolean sw = false;
		
		while (i < palabras.size() && !sw) {
			
			if (palabras.get(i).getTexto().equalsIgnoreCase(palabra)) {
				
				if (++posicionPalabraArrayList == posicion) {
					
					posicionTexto = palabras.get(i);
					sw = true;
					
				}
				
			}
			
			i++;
			
		}
		
		
		return posicionTexto;
		
		
		
	}
	
	public List<PosicionTexto> getPalabras() {
		return palabras;
	}

}
