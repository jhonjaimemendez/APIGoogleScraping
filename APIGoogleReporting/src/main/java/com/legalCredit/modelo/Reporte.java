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

	private String ssn = "";
	private String  name = "";
	private String cra = "";
	private String[] employeer = null;
	private String opencreditcards = "";
	private String openretailcards = "";
	private String openrealrstateloans = "";
	private String openinstallmentloans = "";
	private String totalopenaccounts = "";
	private String accountseverlate = "";
	private String collectionsaccounts = "";
	private String averageaccountage = "";
	private String oldestaccount = "";
	private String newestaccount = "";
	private String myhardcreditinquiries = "";
	private String creditdebt = "";
	private String totalcredit = "";
	private String creditandretailcarddebt = "";
	private String realestatedebt = "";
	private String installmentloansdebt = "";
	private String collectionsdebt = "";
	private String totaldebt = "";
	private String mypublicrecords = "";
	private String dateofreport = "";
	private String creditscore = "";
	private String[] address = null;
	private String birthyear = "";
	private String overallcreditusage = "";
	private String patron = "";

	public Reporte() {}


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

	public void setSsn(String ssn) {
		this.ssn = ssn;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setCra(String cra) {
		this.cra = cra;
	}

	public void setEmployeer(String[] employeer) {
		this.employeer = employeer;
	}

	public void setOpencreditcards(String opencreditcards) {
		this.opencreditcards = opencreditcards;
	}

	public void setOpenretailcards(String openretailcards) {
		this.openretailcards = openretailcards;
	}

	public void setOpenrealrstateloans(String openrealrstateloans) {
		this.openrealrstateloans = openrealrstateloans;
	}

	public void setOpeninstallmentloans(String openinstallmentloans) {
		this.openinstallmentloans = openinstallmentloans;
	}

	public void setTotalopenaccounts(String totalopenaccounts) {
		this.totalopenaccounts = totalopenaccounts;
	}

	public void setAccountseverlate(String accountseverlate) {
		this.accountseverlate = accountseverlate;
	}

	public void setCollectionsaccounts(String collectionsaccounts) {
		this.collectionsaccounts = collectionsaccounts;
	}

	public void setAverageaccountage(String averageaccountage) {
		this.averageaccountage = averageaccountage;
	}

	public void setOldestaccount(String oldestaccount) {
		this.oldestaccount = oldestaccount;
	}

	public void setNewestaccount(String newestaccount) {
		this.newestaccount = newestaccount;
	}

	public void setMyhardcreditinquiries(String myhardcreditinquiries) {
		this.myhardcreditinquiries = myhardcreditinquiries;
	}

	public void setCreditdebt(String creditdebt) {
		this.creditdebt = creditdebt;
	}

	public void setTotalcredit(String totalcredit) {
		this.totalcredit = totalcredit;
	}

	public void setCreditandretailcarddebt(String creditandretailcarddebt) {
		this.creditandretailcarddebt = creditandretailcarddebt;
	}

	public void setRealestatedebt(String realestatedebt) {
		this.realestatedebt = realestatedebt;
	}

	public void setInstallmentloansdebt(String installmentloansdebt) {
		this.installmentloansdebt = installmentloansdebt;
	}

	public void setCollectionsdebt(String collectionsdebt) {
		this.collectionsdebt = collectionsdebt;
	}

	public void setTotaldebt(String totaldebt) {
		this.totaldebt = totaldebt;
	}

	public void setMypublicrecords(String mypublicrecords) {
		this.mypublicrecords = mypublicrecords;
	}

	public void setDateofreport(String dateofreport) {
		this.dateofreport = dateofreport;
	}

	public void setCreditscore(String creditscore) {
		this.creditscore = creditscore;
	}

	public void setAddress(String[] address) {
		this.address = address;
	}

	public void setBirthyear(String birthyear) {
		this.birthyear = birthyear;
	}

	public void setOverallcreditusage(String overallcreditusage) {
		this.overallcreditusage = overallcreditusage;
	}

	public void setPatron(String patron) {
		this.patron = patron;
	}
	
}
