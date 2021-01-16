/**
 * @Clase: Reporte.java
 * 
 * @version  1.0
 * 
 * @since 7/07/2020
 * 
 * @autor Ing. Jhon Mendez
 *
 * @Copyrigth: Legal Credit Solution
 */

package com.legalCredit.modelo;

import java.util.List;
import java.util.Map;

/**
 *  Esta clase modela las cuentas que poseen los reportes.
 *  
 */

public class CuentaReporte {
	
	private String accountName = "";
	private String accountNumber = "";
	private String accountType = "";
	private String accountStatus = "";
	private String paymentStatus = "";
	private String statusUpdated = "";
	private String balance = "";
	private String balanceUpdated = "";
	private String limit = "";
	private String monthlyPayment = "";
	private String pastDueAmount = "";
	private String highestBalance = "";
	private String terms = "";
	private String responsibility = "";
	private String yourStatement = "";
	private String comments = "";
	private String dateOpened = "";
	private String loanType = "";
	private String dateClosed = "";
	private String statusDetail = "";
	private String paymentReceived = "";
	private String originalCreditor = "";
	private String lastActivity = "";
	private String accounttypeone = "";
	private String accounttypetwo = "";
	private String creditusage = "";
	private String creditusagedescription = "";
	private List<List<String>> datosCuentaHistorico;
	private Map<String, List<HistorialPago>> mesesCanceladosPorAno;
	
	public CuentaReporte(){}
	
	
	public String getAccountName() {
		return accountName;
	}

	public String getAccountNumber() {
		return accountNumber;
	}

	public String getAccountType() {
		return accountType;
	}

	public String getAccountStatus() {
		return accountStatus;
	}

	public String getPaymentStatus() {
		return paymentStatus  ;
	}

	public String getStatusUpdated() {
		return statusUpdated;
	}

	public String getBalance() {
		return balance;
	}

	public String getBalanceUpdated() {
		return balanceUpdated;
	}

	public String getMonthlyPayment() {
		return monthlyPayment;
	}

	public String getPastDueAmount() {
		return pastDueAmount;
	}

	public String getHighestBalance() {
		return highestBalance;
	}

	public String getTerms() {
		return terms;
	}

	public String getResponsibility() {
		return responsibility;
	}

	public String getYourStatement() {
		return yourStatement;
	}

	public String getComments() {
		return comments;
	}
	
	public String getLimit() {
		return limit;
	}
	
	public String getDateOpened() {
		return dateOpened;
	}
	
	public String getDateClosed() {
		return dateClosed;
	}
	
	public String getLoanType() {
		return loanType;
	}
	
	public String getStatusDetail() {
		return statusDetail;
	}
	
	public String getPaymentReceived() {
		return paymentReceived;
	}
	
	public String getOriginalCreditor() {
		return originalCreditor;
	}
	
	public String getLastActivity() {
		return lastActivity;
	}
	
	public String getAccounttypeone() {
		return accounttypeone;
	}
	
	public String getAccounttypetwo() {
		return accounttypetwo;
	}
	
	public String getCreditusage() {
		return creditusage;
	}
	
	public String getCreditusagedescription() {
		return creditusagedescription;
	}
	
	
	
	public void setAccountName(String accountName) {
		this.accountName = accountName;
	}

	public void setAccountNumber(String accountNumber) {
		this.accountNumber = accountNumber;
	}

	public void setAccountType(String accountType) {
		this.accountType = accountType;
	}

	public void setAccountStatus(String accountStatus) {
		this.accountStatus = accountStatus;
	}

	public void setPaymentStatus(String paymentStatus) {
		this.paymentStatus = paymentStatus;
	}

	public void setStatusUpdated(String statusUpdated) {
		this.statusUpdated = statusUpdated;
	}

	public void setBalance(String balance) {
		this.balance = balance;
	}

	public void setBalanceUpdated(String balanceUpdated) {
		this.balanceUpdated = balanceUpdated;
	}

	public void setLimit(String limit) {
		this.limit = limit;
	}

	public void setMonthlyPayment(String monthlyPayment) {
		this.monthlyPayment = monthlyPayment;
	}

	public void setPastDueAmount(String pastDueAmount) {
		this.pastDueAmount = pastDueAmount;
	}

	public void setHighestBalance(String highestBalance) {
		this.highestBalance = highestBalance;
	}

	public void setTerms(String terms) {
		this.terms = terms;
	}

	public void setResponsibility(String responsibility) {
		this.responsibility = responsibility;
	}

	public void setYourStatement(String yourStatement) {
		this.yourStatement = yourStatement;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}

	public void setDateOpened(String dateOpened) {
		this.dateOpened = dateOpened;
	}

	public void setLoanType(String loanType) {
		this.loanType = loanType;
	}

	public void setDateClosed(String dateClosed) {
		this.dateClosed = dateClosed;
	}

	public void setStatusDetail(String statusDetail) {
		this.statusDetail = statusDetail;
	}

	public void setPaymentReceived(String paymentReceived) {
		this.paymentReceived = paymentReceived;
	}

	public void setOriginalCreditor(String originalCreditor) {
		this.originalCreditor = originalCreditor;
	}

	public void setLastActivity(String lastActivity) {
		this.lastActivity = lastActivity;
	}

	public void setAccounttypeone(String accounttypeone) {
		this.accounttypeone = accounttypeone;
	}

	public void setAccounttypetwo(String accounttypetwo) {
		this.accounttypetwo = accounttypetwo;
	}

	public void setCreditusage(String creditusage) {
		this.creditusage = creditusage;
	}

	public void setCreditusagedescription(String creditusagedescription) {
		this.creditusagedescription = creditusagedescription;
	}

	public Map<String, List<HistorialPago>> getMesesCanceladosPorAno() {
		return mesesCanceladosPorAno;
	}

	public void setMesesCanceladosPorAno(Map<String, List<HistorialPago>> mesesCanceladosPorAno) {
		this.mesesCanceladosPorAno = mesesCanceladosPorAno;
	}
	
	public List<List<String>> getDatosCuentaHistorico() {
		return datosCuentaHistorico;
	}
	
	public void setDatosCuentaHistorico(List<List<String>> datosCuentaHistorico) {
		this.datosCuentaHistorico = datosCuentaHistorico;
	}
	
	
}
