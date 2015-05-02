package com.ashwin.fri.stocks.hibernate;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * "The submissions data set contains summary information about an entire 
 * EDGAR submission. Some fields were sourced directly from EDGAR submission 
 * information, while other columns of data were sourced from the Interactive 
 * Data attachments of the submission. Note: EDGAR derived fields represent 
 * the most recent EDGAR assignment as of a given filing’s submission date 
 * and do not necessarily represent the most current assignments." (SEC Readme)
 * 
 * @author ashwin
 *
 */
@Entity
@Table(name="submissions")
public class Submission implements Serializable {

	private static final long serialVersionUID = 6139114596188200635L;
	
	private Registrant _registrant;
	private List<Number> _numbers;
	private String _adsh, _form;
	private Date _fiscalYearEndDate, _balanceSheetDate, _filingDate, _acceptedDate;
	private Boolean _isWksi, _isDetailed;
	private FilerStatus _filerStatus;
	private FiscalPeriod _fiscalPeriod;
	
	public enum FilerStatus {
		LARGE_ACCELERATED("1-LAF"), ACCELERATED("2-ACC"), SMALLER_REPORTING_ACCELERATED("3-SRA"),
		NON_ACCELERATED("4-NON"), SMALLER_REPORTING_FILER("5-SML");
		
		private String _str;
		
		private FilerStatus(String str) {
			_str = str;
		}
		
		public static FilerStatus fromString(String str) {
			for(FilerStatus status : FilerStatus.values())
				if(status._str.equals(str))
					return status;
			return null;
		}
	}
	
	public enum FiscalPeriod {
		FY, Q1, Q2, Q3, Q4, H1, H2, M9, T1, T2, T3, M8, CY;
	}
	
	public Submission() {}
	
	public Submission(String adsh, Registrant registrant, FilerStatus filerStatus, Boolean isWksi,
			Date fiscalYearEndDate, String form, Date balanceSheetDate, FiscalPeriod fiscalPeriod, 
			Date filingDate, Date acceptedDate, Boolean isDetailed, List<Number> numbers) {
		
		setAdsh(adsh);
		setRegistrant(registrant);
		setFilerStatus(filerStatus);
		setWksi(isWksi);
		setFiscalYearEndDate(fiscalYearEndDate);
		setForm(form);
		setBalanceSheetDate(balanceSheetDate);
		setFiscalPeriod(fiscalPeriod);
		setFilingDate(filingDate);
		setAcceptedDate(acceptedDate);
		setDetailed(isDetailed);
		setNumbers(numbers);
	}
	/**
	 * The Accession Number (adsh) is a 20 character string formed from the 18-digit number
	 * assigned by the SEC to each EDGAR submission.
	 * 
	 * @return adsh
	 */
	@Id
	@Column(name="adsh", length=20, unique=true, nullable=false)
	public String getAdsh() {
		return _adsh;
	}
	
	public void setAdsh(String adsh) {
		_adsh = adsh;
	}
	
	/**
	 * Returns the associated registrant to the submission. Although the SEC permits multiple
	 * entities to file together (multiple ciks per submission). We only care about the main
	 * entity that submitted the filing.
	 * 
	 * @return registrant
	 */
	@ManyToOne(cascade=CascadeType.ALL)
	@JoinColumn(name="cik")
	public Registrant getRegistrant() {
		return _registrant;
	}
	
	public void setRegistrant(Registrant registrant) {
		_registrant = registrant;
	}
	
	/**
	 * Returns all the numbers associated with the submission.
	 * 
	 * @return numbers
	 */
	@OneToMany(cascade=CascadeType.ALL, mappedBy="submission", fetch=FetchType.LAZY)
	public List<Number> getNumbers() {
		return _numbers;
	}
	
	public void setNumbers(List<Number> numbers) {
		_numbers = numbers;
	}
	
	/**
	 * Returns the filer status of the registrant with the SEC at the time of submission.
	 * Filer status can vary from submission to submission.
	 * 
	 * @return filer status
	 */
	@Column(name="afs", nullable=true)
	@Enumerated(EnumType.ORDINAL)
	public FilerStatus getFilerStatus() {
		return _filerStatus;
	}
	
	public void setFilerStatus(FilerStatus filerStatus) {
		_filerStatus = filerStatus;
	}
	
	/**
	 * Returns whether or not the registrant is a well known seasoned issuer. A wksi is
	 * an issuer that meets specific SEC requirements at some point during a 60-day period
	 * prior to the date the issuer satisfies its obligation to update its shelf registration statement.
	 * 
	 * @return true if well known seasoned issuer, false otherwise
	 */
	@Column(name="wksi", nullable=false)
	public Boolean isWksi() {
		return _isWksi;
	}
	
	public void setWksi(Boolean isWksi) {
		_isWksi = isWksi;
	}
	
	/**
	 * The form is the submission type of the registrant's filing.
	 * 
	 * @return submission type
	 */
	@Column(name="form", length=10, nullable=false)
	public String getForm() {
		return _form;
	}
	
	public void setForm(String form) {
		_form = form;
	}
	
	/**
	 * @return the period or balance sheet date.
	 */
	@Column(name="period", nullable=false)
	public Date getBalanceSheetDate() {
		return _balanceSheetDate;
	}
	
	public void setBalanceSheetDate(Date balanceSheetDate) {
		_balanceSheetDate = balanceSheetDate;
	}
	
	/**
	 * @return the end date of the fiscal year.
	 */
	@Column(name="fye", nullable=true)
	public Date getFiscalYearEndDate() {
		return _fiscalYearEndDate;
	}
	
	public void setFiscalYearEndDate(Date fiscalYearEndDate) {
		_fiscalYearEndDate = fiscalYearEndDate;
	}
	
	/**
	 * The fiscal period focus (as defined in EFM CH. 6) within the fiscal year.
	 * The fiscal period focus is defined in the enumeration above.
	 * 
	 * @return fiscal period focus
	 */
	@Column(name="fp", nullable=true)
	@Enumerated(EnumType.STRING)
	public FiscalPeriod getFiscalPeriod() {
		return _fiscalPeriod;
	}
	
	public void setFiscalPeriod(FiscalPeriod fiscalPeriod) {
		_fiscalPeriod = fiscalPeriod;
	}
	
	/**
	 * @return the date that the registrant filed with the SEC.
	 */
	@Column(name="filed", nullable=false)
	public Date getFilingDate() {
		return _filingDate;
	}
	
	public void setFilingDate(Date filingDate) {
		_filingDate = filingDate;
	}
	
	/**
	 * The acceptance date/time of the registrant's filing with the SEC.
	 * Filings accepted after 5:30pm EST are considered filed the following day.
	 * 
	 * @return acceptance date/time
	 */
	@Column(name="accepted", nullable=false)
	public Date getAcceptedDate() {
		return _acceptedDate;
	}
	
	public void setAcceptedDate(Date acceptedDate) {
		_acceptedDate = acceptedDate;
	}
	
	/**
	 * True indicates that the XBRL submission contains quantitative disclosures 
	 * within the footnotes and schedules at the required detail level.
	 * 
	 * @return true if detailed, false otherwise
	 */
	@Column(name="detail", nullable=false)
	public Boolean isDetailed() {
		return _isDetailed;
	}
	
	public void setDetailed(Boolean isDetailed) {
		_isDetailed = isDetailed;
	}
	
	@Override
	public int hashCode() {
		return new HashCodeBuilder()
			.append(_adsh)
			.build();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof Submission))
			return false;

		Submission oth = (Submission) obj;
		return new EqualsBuilder().append(_adsh, oth.getAdsh()).build();
	}
}
