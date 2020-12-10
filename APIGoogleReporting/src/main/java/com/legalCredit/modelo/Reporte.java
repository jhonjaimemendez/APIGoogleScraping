/**
 * @Clase: Reporte.java
 * 
 * @version  1.0
 * 
 * @since 23/07/2020
 * 
 * @autor Ing. Jhon Mendez
 *
 * @Copyrigth: Legal Credit Solution
 */

package com.legalCredit.modelo;

/**
 *  Esta clase modela los datos de un reporte
 *  
 */

public class Reporte {

	private String ssn;
	private String  name;
	private String cra;
	private String[] employeer;
	private String opencreditcards;
	private String openretailcards;
	private String openrealrstateloans;
	private String openinstallmentloans;
	private String totalopenaccounts;
	private String accountseverlate;
	private String collectionsaccounts;
	private String averageaccountage;
	private String oldestaccount;
	private String newestaccount;
	private String myhardcreditinquiries;
	private String creditdebt;
	private String totalcredit;
	private String creditandretailcarddebt;
	private String realestatedebt;
	private String installmentloansdebt;
	private String collectionsdebt;
	private String totaldebt;
	private String mypublicrecords;
	private String dateofreport;
	private String creditscore;
	private String[] address;
	private String birthyear;
	private String overallcreditusage;
	private String patron;


	public Reporte(String ssn, String name, String cra, String[] employeer, String opencreditcards,
			String openretailcards, String openrealrstateloans, String openinstallmentloans, String totalopenaccounts,
			String accountseverlate, String collectionsaccounts, String averageaccountage, String oldestaccount,
			String newestaccount, String myhardcreditinquiries, String creditdebt, String totalcredit,
			String creditandretailcarddebt, String realestatedebt, String installmentloansdebt, String collectionsdebt,
			String totaldebt, String mypublicrecords, String dateofreport, String creditscore, String[] address,
			String birthyear,String overallcreditusage,String patron) {

		this.ssn = ssn;
		this.name = name;
		this.cra = cra;
		this.employeer = employeer;
		this.opencreditcards = opencreditcards;
		this.openretailcards = openretailcards;
		this.openrealrstateloans = openrealrstateloans;
		this.openinstallmentloans = openinstallmentloans;
		this.totalopenaccounts = totalopenaccounts;
		this.accountseverlate = accountseverlate;
		this.collectionsaccounts = collectionsaccounts;
		this.averageaccountage = averageaccountage;
		this.oldestaccount = oldestaccount;
		this.newestaccount = newestaccount;
		this.myhardcreditinquiries = myhardcreditinquiries;
		this.creditdebt = creditdebt;
		this.totalcredit = totalcredit;
		this.creditandretailcarddebt = creditandretailcarddebt;
		this.realestatedebt = realestatedebt;
		this.installmentloansdebt = installmentloansdebt;
		this.collectionsdebt = collectionsdebt;
		this.totaldebt = totaldebt;
		this.mypublicrecords = mypublicrecords;
		this.dateofreport = dateofreport;
		this.creditscore = creditscore;
		this.address = address;
		this.birthyear = birthyear;
		this.overallcreditusage = overallcreditusage;
		this.patron = patron;
	}


	public String getSsn() {
		return ssn;
	}


	public String getName() {
		return name;
	}


	public String getCra() {
		return cra;
	}


	public String[] getEmployeer() {
		return employeer;
	}


	public String getOpencreditcards() {
		return opencreditcards;
	}


	public String getOpenretailcards() {
		return openretailcards;
	}


	public String getOpenrealrstateloans() {
		return openrealrstateloans;
	}


	public String getOpeninstallmentloans() {
		return openinstallmentloans;
	}


	public String getTotalopenaccounts() {
		return totalopenaccounts;
	}


	public String getAccountseverlate() {
		return accountseverlate;
	}


	public String getCollectionsaccounts() {
		return collectionsaccounts;
	}


	public String getAverageaccountage() {
		return averageaccountage;
	}


	public String getOldestaccount() {
		return oldestaccount;
	}


	public String getNewestaccount() {
		return newestaccount;
	}


	public String getMyhardcreditinquiries() {
		return myhardcreditinquiries;
	}


	public String getCreditdebt() {
		return creditdebt;
	}


	public String getTotalcredit() {
		return totalcredit;
	}


	public String getCreditandretailcarddebt() {
		return creditandretailcarddebt;
	}


	public String getRealestatedebt() {
		return realestatedebt;
	}


	public String getInstallmentloansdebt() {
		return installmentloansdebt;
	}


	public String getCollectionsdebt() {
		return collectionsdebt;
	}


	public String getTotaldebt() {
		return totaldebt;
	}


	public String getMypublicrecords() {
		return mypublicrecords;
	}


	public String getDateofreport() {
		return dateofreport;
	}


	public String getCreditscore() {
		return creditscore;
	}


	public String[] getAddress() {
		return address;
	}


	public String getBirthyear() {
		return birthyear;
	}
	
	
	public String getOverallcreditusage() {
		return overallcreditusage;
	}


	public String getPatron() {
		return patron;
	}
}
