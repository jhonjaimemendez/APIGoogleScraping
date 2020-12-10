/**
 * @Clase: Inquiery.java
 * 
 * @version  1.0
 * 
 * @since 02/12/2020
 * 
 * @autor Ing. Jhon Mendez
 *
 * @Copyrigth: Legal Credit Solution
 */

package com.legalCredit.modelo;

public class Inquiery {

	private String accountName; 
	private String inquiryDate; 
	private String removalDate;
	private String businessType;
	private String contactInformation; 
	private String inquieresType;
	private String comments;
	
	
	public Inquiery(String accountName, String inquiryDate, String removalDate, String businessType,
			        String contactInformation, String inquieresType,String comments) {
		
		this.accountName = accountName;
		this.inquiryDate = inquiryDate;
		this.removalDate = removalDate;
		this.businessType = businessType;
		this.contactInformation = contactInformation;
		this.inquieresType = inquieresType;
		this.comments = comments;
	}


	public String getAccountName() {
		return accountName;
	}


	public String getInquiryDate() {
		return inquiryDate;
	}


	public String getRemovalDate() {
		return removalDate;
	}


	public String getBusinessType() {
		return businessType;
	}


	public String getContactInformation() {
		return contactInformation;
	}


	public String getInquieresType() {
		return inquieresType;
	}
	
	public String getComments() {
		return comments;
	}
}
