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

	private String accountName= "";
	private String filingDate= "";
	private String dateResolved= "";
	private String claim_amount= "";
	private String liability_amount= "";
	private String judgment_in_favor= "";
	private String referenceNumber= "";
	private String court= "";
	private String plaintiff= "";
	private String status= "";
	private String status_detail= "";
	private String responsibility= "";
	private String comments= "";

	

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




	public void setAccountName(String accountName) {
		this.accountName = accountName;
	}




	public void setFilingDate(String filingDate) {
		this.filingDate = filingDate;
	}




	public void setDateResolved(String dateResolved) {
		this.dateResolved = dateResolved;
	}




	public void setClaim_amount(String claim_amount) {
		this.claim_amount = claim_amount;
	}




	public void setLiability_amount(String liability_amount) {
		this.liability_amount = liability_amount;
	}




	public void setJudgment_in_favor(String judgment_in_favor) {
		this.judgment_in_favor = judgment_in_favor;
	}




	public void setReferenceNumber(String referenceNumber) {
		this.referenceNumber = referenceNumber;
	}




	public void setCourt(String court) {
		this.court = court;
	}




	public void setPlaintiff(String plaintiff) {
		this.plaintiff = plaintiff;
	}




	public void setStatus(String status) {
		this.status = status;
	}




	public void setStatus_detail(String status_detail) {
		this.status_detail = status_detail;
	}




	public void setResponsibility(String responsibility) {
		this.responsibility = responsibility;
	}




	public void setComments(String comments) {
		this.comments = comments;
	}
	
	
	
}