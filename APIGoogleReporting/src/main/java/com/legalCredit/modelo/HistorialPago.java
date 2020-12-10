package com.legalCredit.modelo;

public class HistorialPago {
	
	private String mes;
	private String periodo;
	private String tipo;
	private String valor;
	
	public HistorialPago(String mes, String periodo, String tipo, String valor) {
		
		this.mes = mes;
		this.periodo = periodo;
		this.tipo = tipo;
		this.valor = valor;
	}

	public String getMes() {
		return mes;
	}

	public String getPeriodo() {
		return periodo;
	}

	public String getValor() {
		return valor;
	}
	
	public String getTipo() {
		return tipo;
	}

}
