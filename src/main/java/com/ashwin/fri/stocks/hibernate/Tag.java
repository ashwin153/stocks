package com.ashwin.fri.stocks.hibernate;

import java.io.Serializable;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.ashwin.fri.stocks.hibernate.Tag.TagPK;

@Entity
@Table(name="tags")
@IdClass(TagPK.class)
public class Tag implements Serializable {
	
	private static final long serialVersionUID = -4327937397901300576L;
	
	private String _name, _version, _datatype, _label, _foc, _iord, _crdr;
	private Boolean _isCustom, _isAbstract;
	private List<Number> _numbers;
	
	public Tag() {}
	
	public Tag(String name, String version, Boolean isCustom, Boolean isAbstract,
			String datatype, String iord, String crdr, String label, String foc, 
			List<Number> numbers) {
		
		setName(name);
		setVersion(version);
		setCustom(isCustom);
		setAbstract(isAbstract);
		setDatatype(datatype);
		setIord(iord);
		setCrdr(crdr);
		setLabel(label);
		setFoc(foc);
		setNumbers(numbers);
	}
	
	/**
	 * @return the unique identifier (name) for a tag in a specific taxonomy release.
	 */
	@Id
	@Column(name="name", length=255, nullable=false)
	public String getName() {
		return _name;
	}
	
	public void setName(String name) {
		_name = name;
	}
	
	/**
	 * @return for a standard tag, an identifier for the taxonomy; otherwise the 
	 * accession number where the tag was defined.
	 */
	@Id
	@Column(name="version", length=20, nullable=false)
	public String getVersion() {
		return _version;
	}
	
	public void setVersion(String version) {
		_version = version;
	}
	
	/**
	 * Returns all the numbers associated with the submission.
	 * 
	 * @return numbers
	 */
	@OneToMany(cascade=CascadeType.ALL, mappedBy="tag", fetch=FetchType.LAZY)
	public List<Number> getNumbers() {
		return _numbers;
	}
	
	public void setNumbers(List<Number> numbers) {
		_numbers = numbers;
	}
	
	/**
	 * @return 1 if tag is custom (version=adsh), 0 if it is standard. Note: This 
	 * flag is technically redundant with the  version and adsh columns.
	 */
	@Column(name="custom", nullable=false)
	public Boolean isCustom() {
		return _isCustom;
	}
	
	public void setCustom(Boolean isCustom) {
		_isCustom = isCustom;
	}
	
	/**
	 * @return 1 if the tag is not used to represent a numeric fact.
	 */
	@Column(name="abstract", nullable=false)
	public Boolean isAbstract() {
		return _isAbstract;
	}
	
	public void setAbstract(Boolean isAbstract) {
		_isAbstract = isAbstract;
	}
	
	/**
	 * @return if abstract=1, then NULL, otherwise the data type (e.g., monetary) 
	 * for the tag.
	 */
	@Column(name="datatype", length=20, nullable=true)
	public String getDatatype() {
		return _datatype;
	}
	
	public void setDatatype(String datatype) {
		_datatype = datatype;
	}
	
	/**
	 * @return if abstract=1, then NULL; otherwise, “I” if the value is a point-in 
	 * time, or “D” if the value is a duration.
	 */
	@Column(name="iord", length=1, nullable=true)
	public String getIord() {
		return _iord;
	}
	
	public void setIord(String iord) {
		_iord = iord;
	}
	
	/**
	 * @return if datatype = monetary, then the tag’s natural accounting balance 
	 * (debit or credit); if not defined, then NULL.
	 */
	@Column(name="crdr", length=1, nullable=true)
	public String getCrdr() {
		return _crdr;
	}
	
	public void setCrdr(String crdr) {
		_crdr = crdr;
	}
	
	/**
	 * @return if a standard tag, then the label text provided by the taxonomy, 
	 * otherwise the text provided by the filer.  A tag which had neither would 
	 * have a NULL value here.
	 */
	@Column(name="label", length=512, nullable=true)
	public String getLabel() {
		return _label;
	}
	
	public void setLabel(String label) {
		_label = label;
	}
	
	/**
	 * @return the detailed definition for the tag (truncated to 2048 characters). 
	 * If a standard tag, then the text provided by the taxonomy, otherwise the 
	 * text assigned by the filer.  Some tags have neither, and this field is NULL.
	 */
	@Column(name="foc", length=2048, nullable=true)
	public String getFoc() {
		return _foc;
	}
	
	public void setFoc(String foc) {
		_foc = foc;
	}
	
	@Override
	public int hashCode() {
		return new HashCodeBuilder()
			.append(_name)
			.append(_version)
			.build();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof Tag))
			return false;

		Tag oth = (Tag) obj;
		return new EqualsBuilder().append(_name, oth.getName())
				.append(_version, oth.getVersion()).build();
	}
	
	public static class TagPK implements Serializable {
		
		private static final long serialVersionUID = 4925940222144168575L;
		
		private String _name, _version;
		
		public TagPK() {}
		
		public TagPK(String name, String version) {
			_name = name;
			_version = version;
		}
		
		public String getName() {
			return _name;
		}
		
		public void setName(String name) {
			_name = name;
		}
		
		public String getVersion() {
			return _version;
		}
		
		public void setVersion(String version) {
			_version = version;
		}
		
		@Override
		public int hashCode() {
			return new HashCodeBuilder()
				.append(_name)
				.append(_version)
				.build();
		}
		
		@Override
		public boolean equals(Object obj) {
			if (obj == null || !(obj instanceof TagPK))
				return false;

			TagPK oth = (TagPK) obj;
			return new EqualsBuilder()
					.append(_name, oth.getName())
					.append(_version, oth.getVersion()).build();
		}
	}
}
