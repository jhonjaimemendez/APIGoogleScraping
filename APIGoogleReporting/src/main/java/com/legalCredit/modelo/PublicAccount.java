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
	private String dateResolved;
	private String claim_amount;
	private String liability_amount;
	private String judgment_in_favor;
	private String referenceNumber;
	private String court;
	private String plaintiff;
	private String status;
	private String status_detail;
	private String responsibility;
	private String comments;

	
	
	public PublicAccount(String accountName, String filingDate, String dateResolved, String claim_amount,
	 	 	             String liability_amount, String judgment_in_favor, String referenceNumber, String court, 
	 	 	             String plaintiff) {
	
		this.accountName = accountName;
		this.filingDate = filingDate;
		this.dateResolved = dateResolved;
		this.claim_amount = claim_amount;
		this.liability_amount = liability_amount;
		this.judgment_in_favor = judgment_in_favor;
		this.referenceNumber = referenceNumber;
		this.court = court;
		this.plaintiff = plaintiff;
		
	}

	
	

	public PublicAccount(String accountName, String filingDate, String dateResolved, String claim_amount,
			String liability_amount, String judgment_in_favor, String referenceNumber, String court, 
			String plaintiff,String status, String status_detail, String responsibility) {
		
		this.accountName = accountName;
		this.filingDate = filingDate;
		this.dateResolved = dateResolved;
		this.claim_amount = claim_amount;
		this.liability_amount = liability_amount;
		this.judgment_in_favor = judgment_in_favor;
		this.referenceNumber = referenceNumber;
		this.court = court;
		this.plaintiff = plaintiff;
		this.status = status;
		this.status_detail = status_detail;
		this.responsibility = responsibility;
		
		
	}




	public String getStatus() {
		return status;
	}




	public String getStatus_detail() {
		return status_detail;
	}




	public String getResponsibility() {
		return responsibility;
	}




	public String getAccountName() {
		return accountName;
	}


	public String getFilingDate() {
		return filingDate;
	}


	public String getDateResolved() {
		return dateResolved;
	}


	public String getClaim_amount() {
		return claim_amount;
	}


	public String getLiability_amount() {
		return liability_amount;
	}


	public String getJudgment_in_favor() {
		return judgment_in_favor;
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
	
	public String getComments() {
		return comments;
	}
	
	
}