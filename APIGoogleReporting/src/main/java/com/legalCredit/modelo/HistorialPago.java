package com.legalCredit.modelo;

public class HistorialPago {
	
	private String año;
	private String mes;
	private String tipo;
	private String valor;
	
	public HistorialPago() {}
	
	public HistorialPago(String año,String mes, String tipo, String valor) {
		
		this.mes = mes;
		this.año = año;
		this.tipo = tipo;
		this.valor = valor;
	}

	
	public String getAño() {
		return año;
	}
	public void setAño(String año) {
		this.año = año;
	}
	public String getMes() {
		return mes;
	}
	public void setMes(String mes) {
		this.mes = mes;
	}
	
	public String getTipo() {
		return tipo;
	}
	public void setTipo(String tipo) {
		this.tipo = tipo;
	}
	public String getValor() {
		return valor;
	}
	public void setValor(String valor) {
		this.valor = valor;
	}
	
}
