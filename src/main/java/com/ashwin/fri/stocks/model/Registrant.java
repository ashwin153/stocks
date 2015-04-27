package com.ashwin.fri.stocks.model;

import java.io.Serializable;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

@Entity
@Table(name="registrants")
public class Registrant implements Serializable {

	private static final long serialVersionUID = -6617466116667079824L;
	
	private Integer _cik, _sic, _ein;
	private String _name;
	private List<Submission> _submissions;

	public Registrant() {}

	public Registrant(Integer cik, String name, Integer sic, Integer ein, List<Submission> submissions) {
		setCik(cik);
		setName(name);
		setSic(sic);
		setEin(ein);
		setSubmissions(submissions);
	}

	/**
	 * The central index key (cik) is a ten digit number assigned by the SEC 
	 * to each registrant that submits filings.
	 * 
	 * @return central index key
	 */
	@Id
	@Column(name="cik", length=10, unique=true, nullable=false)
	public Integer getCik() {
		return _cik;
	}
	
	public void setCik(Integer cik) {
		_cik = cik;
	}

	/**
	 * The legal name of the entity as recorded in EDGAR as of the filing date.
	 * This name should represent the most recent name of the registrant.
	 * 
	 * @return legal name
	 */
	@Column(name="name", length=150, unique=false, nullable=false)
	public String getName() {
		return _name;
	}
	
	public void setName(String name) {
		_name = name;
	}

	/**
	 * The standard industrial classification (sic) of the registrant. The standard
	 * industrial classification is a four digit code assigned by the SEC that
	 * indicates the registrant's type of business.
	 * 
	 * @return standard industrial classification
	 */
	@Column(name="sic", length=4, unique=false, nullable=true)
	public Integer getSic() {
		return _sic;
	}	
	
	public void setSic(Integer sic) {
		_sic = sic;
	}

	/**
	 * The employer identification number (ein) is a 9 digit identification number
	 * assigned by the IRS to business entities operating within the US.
	 * 
	 * @return employer identification number
	 */
	@Column(name="ein", unique=false, nullable=true)
	public Integer getEin() {
		return _ein;
	}
	
	public void setEin(Integer ein) {
		_ein = ein;
	}

	/**
	 * Returns all the submissions associated with this registrant.
	 * 
	 * @return submissions
	 */
	@OneToMany(cascade=CascadeType.ALL, mappedBy="registrant", fetch=FetchType.LAZY)
	public List<Submission> getSubmissions() {
		return _submissions;
	}
	
	public void setSubmissions(List<Submission> submissions) {
		_submissions = submissions;
	}
	
	@Override
	public int hashCode() {
		return new HashCodeBuilder()
			.append(_cik)
			.build();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof Registrant))
			return false;

		Registrant oth = (Registrant) obj;
		return new EqualsBuilder().append(_cik, oth.getCik()).build();
	}
}
