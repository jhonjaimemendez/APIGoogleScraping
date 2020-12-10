/**
 * @Clase: PublicAccount.java
 * 
 * @version  1.0
 * 
 * @since 09/12/2020
 * 
 * @autor Ing. Jhon Mendez
 *
 * @Copyrigth: Legal Credit Solution
 */

package com.legalCredit.modelo;

public class PublicAccount {

	private String accountName;
	private String filingDate;
	private String amount;
	private String referenceNumber;
	private String court;
	private String plaintiff;
	
	
	public PublicAccount(String accountName, String filingDate, String amount, String referenceNumber, String court,
			String plaintiff) {
		
		this.accountName = accountName;
		this.filingDate = filingDate;
		this.amount = amount;
		this.referenceNumber = referenceNumber;
		this.court = court;
		this.plaintiff = plaintiff;
		
	}


	public String getAccountName() {
		return accountName;
	}


	public String getFilingDate() {
		return filingDate;
	}


	public String getAmount() {
		return amount;
	}


	public String getReferenceNumber() {
		return referenceNumber;
	}


	public String getCourt() {
		return court;
	}


	public String getPlaintiff() {
		return plaintiff;
	}
	
}
