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
	
	private String accountName;
	private String accountNumber;
	private String accountType;
	private String accountStatus;
	private String paymentStatus;
	private String statusUpdated;
	private String balance;
	private String balanceUpdated;
	private String limit;
	private String monthlyPayment;
	private String pastDueAmount;
	private String highestBalance;
	private String terms;
	private String responsibility;
	private String yourStatement;
	private String comments;
	private String dateOpened;
	private String loanType;
	private String dateClosed;
	private String statusDetail;
	private String paymentReceived;
	private String originalCreditor;
	private String lastActivity;
	private String accounttypeone;
	private String accounttypetwo;
	private String creditusage;
	private String creditusagedescription;
	private Map<String, List<HistorialPago>> mesesCanceladosPorAno;
	

	public CuentaReporte(String accountName, String accountNumber, String accountType,
			String accountStatus, String paymentStatus, String statusUpdated, String balance, String balanceUpdated,
			String limit, String monthlyPayment, String pastDueAmount, String highestBalance, String terms,
			String responsibility, String yourStatement, String comments, String dateOpened, String loanType,
			String dateClosed, String statusDetail, String paymentReceived,String originalCreditor,
			String lastActivity, String accounttypeone,String accounttypetwo,String creditusage,
			String creditusagedescription,Map<String, List<HistorialPago>> mesesCanceladosPorAno) {
		
		
		this.accountName = accountName;
		this.accountNumber = accountNumber;
		this.accountType = accountType;
		this.accountStatus = accountStatus;
		this.paymentStatus = paymentStatus;
		this.statusUpdated = statusUpdated;
		this.balance = balance;
		this.balanceUpdated = balanceUpdated;
		this.limit = limit;
		this.monthlyPayment = monthlyPayment;
		this.pastDueAmount = pastDueAmount;
		this.highestBalance = highestBalance;
		this.terms = terms;
		this.responsibility = responsibility;
		this.yourStatement = yourStatement;
		this.comments = comments;
		this.dateOpened = dateOpened;
		this.loanType = loanType;
		this.dateClosed = dateClosed;
		this.statusDetail = statusDetail;
		this.paymentReceived = paymentReceived;
		this.originalCreditor = originalCreditor;
		this.lastActivity = lastActivity;
		this.accounttypeone = accounttypeone;
		this.accounttypetwo = accounttypetwo;
		this.creditusage = creditusage;
		this.creditusagedescription = creditusagedescription;
		this.mesesCanceladosPorAno = mesesCanceladosPorAno;
	}

	
	

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
		return paymentStatus == null ? "" : paymentStatus;
	}

	public String getStatusUpdated() {
		return statusUpdated == null ? "" : statusUpdated;
	}

	public String getBalance() {
		return balance == null ? "" : balance;
	}

	public String getBalanceUpdated() {
		return balanceUpdated == null ? "" : balanceUpdated;
	}

	public String getMonthlyPayment() {
		return monthlyPayment == null ? "" : monthlyPayment;
	}

	public String getPastDueAmount() {
		return pastDueAmount == null ? "" : pastDueAmount;
	}

	public String getHighestBalance() {
		return highestBalance == null ? "" : highestBalance;
	}

	public String getTerms() {
		return terms == null ? "" : terms;
	}

	public String getResponsibility() {
		return responsibility == null ? "" : responsibility;
	}

	public String getYourStatement() {
		return yourStatement == null ? "" : yourStatement;
	}

	public String getComments() {
		return comments == null ? "" : comments;
	}
	
	public String getLimit() {
		return limit == null ? "" : limit;
	}
	
	public String getDateOpened() {
		return dateOpened == null ? "" : dateOpened;
	}
	
	public String getDateClosed() {
		return dateClosed == null ? "" : dateClosed;
	}
	
	public String getLoanType() {
		return loanType == null ? "" : loanType;
	}
	
	public String getStatusDetail() {
		return statusDetail == null ? "" : statusDetail;
	}
	
	public String getPaymentReceived() {
		return paymentReceived == null ? "" : paymentReceived;
	}
	
	public String getOriginalCreditor() {
		return originalCreditor == null ? "" : originalCreditor;
	}
	
	public String getLastActivity() {
		return lastActivity == null ? "" : lastActivity;
	}
	
	public String getAccounttypeone() {
		return accounttypeone == null ? "" : accounttypeone;
	}
	
	public String getAccounttypetwo() {
		return accounttypetwo == null ? "" : accounttypetwo;
	}
	
	public String getCreditusage() {
		return creditusage == null ? "" : creditusage;
	}
	
	public String getCreditusagedescription() {
		return creditusagedescription == null ? "" : creditusagedescription;
	}
	
	public Map<String, List<HistorialPago>> getMesesCanceladosPorAno() {
		return mesesCanceladosPorAno;
	}
	
	
}
