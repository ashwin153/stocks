package com.ashwin.fri.stocks.hibernate;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.ashwin.fri.stocks.hibernate.Number.NumberPK;

@Entity
@Table(name="numbers")
@IdClass(NumberPK.class)
public class Number implements Serializable {

	private static final long serialVersionUID = 1179390802275094269L;
	
	private Submission _submission;
	private Tag _tag;
	private String _units, _footnote, _coreg;
	private Date _endDate;
	private Integer _duration;
	private BigDecimal _value;
	
	public Number() {}
	
	public Number(Submission submission, Tag tag, Date endDate, Integer duration,
			String units, String coreg, BigDecimal value, String footnote) {
		
		setSubmission(submission);
		setTag(tag);
		setEndDate(endDate);
		setDuration(duration);
		setUnits(units);
		setCoregistrant(coreg);
		setValue(value);
		setFootnote(footnote);
	}
	
	/**
	 * Returns the submission associated with this number.
	 * 
	 * @return submission
	 */
	@Id
	@ManyToOne
	@JoinColumn(name="adsh")
	public Submission getSubmission() {
		return _submission;
	}
	
	public void setSubmission(Submission submission) {
		_submission = submission;
	}
	
	/**
	 * Returns the tag associated with this number. The tag is determined by
	 * the name and version attributes.
	 * 
	 * @return tag
	 */
	@Id
	@ManyToOne
	@JoinColumns({
		@JoinColumn(name="name"),
		@JoinColumn(name="version")
	})
	public Tag getTag() {
		return _tag;
	}
	
	public void setTag(Tag tag) {
		_tag = tag;
	}
	
	/**
	 * The end date of the data value, rounded to the nearest month end.
	 * 
	 * @return end date
	 */
	@Id
	@Column(name="ddate", nullable=false)
	public Date getEndDate() {
		return _endDate;
	}

	public void setEndDate(Date endDate) {
		_endDate = endDate;
	}
	
	/**
	 * Duration represented by the data value in number of quarters. Zero indicates
	 * that the data value is a point-in-time value.
	 * 
	 * @param endDate
	 */
	@Id
	@Column(name="duration", length=8, nullable=false)
	public Integer getDuration() {
		return _duration;
	}
	
	public void setDuration(Integer duration) {
		_duration = duration;
	}
	
	/**
	 * The units of measure for the data value.
	 * 
	 * @return units of measure (uom)
	 */
	@Id
	@Column(name="units", length=20, nullable=false)
	public String getUnits() {
		return _units;
	}
	
	public void setUnits(String units) {
		_units = units;
	}
	
	/**
	 * If specified, this field indicates a specific coregistrant, the parent
	 * company, or other entity. NULL indicates the consolidated entity
	 * 
	 * @return coreg
	 */
	@Id
	@Column(name="coreg", length=255, nullable=true)
	public String getCoregistrant() {
		return _coreg;
	}
	
	public void setCoregistrant(String coreg) {
		_coreg = coreg;
	}
	
	/**
	 * The value associated with this number. 
	 * 
	 * @return data value
	 */
	@Column(name="value", precision=28, scale=4, nullable=true)
	public BigDecimal getValue() {
		return _value;
	}
	
	public void setValue(BigDecimal value) {
		_value = value;
	}
	
	/**
	 * The text of any superscripted footnotes on the value, as shown on the 
	 * statement page, truncated to 512 characters, or if there is no footnote, 
	 * then this field will be blank.
	 * 
	 * @return footnote
	 */
	@Column(name="footnote", length=512, nullable=true)
	public String getFootnote() {
		return _footnote;
	}
	
	public void setFootnote(String footnote) {
		_footnote = footnote;
	}
	
	@Override
	public int hashCode() {
		return new HashCodeBuilder()
			.append(_submission.getAdsh())
			.append(_tag.getName())
			.append(_tag.getVersion())
			.append(_endDate)
			.append(_duration)
			.append(_units)
			.append(_coreg)
			.build();
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj == null || !(obj instanceof Number))
			return false;
		
		Number oth = (Number) obj;
		return new EqualsBuilder()
				.append(_submission, oth.getSubmission())
				.append(_tag, oth.getTag())
				.append(_endDate, oth.getEndDate())
				.append(_duration, oth.getDuration())
				.append(_units, oth.getUnits())
				.append(_coreg, oth.getCoregistrant())
				.build();
	}
	
	public static class NumberPK implements Serializable {
		
		private static final long serialVersionUID = 2602552004310428677L;
		
		private Submission _submission;
		private Tag _tag;
		private String _units, _coreg;
		private Date _endDate;
		private Integer _duration;
		
		public NumberPK() {}
		
		public NumberPK(Submission submission, Tag tag, Date endDate, Integer duration, String units, String coreg) {
			setSubmission(submission);
			setTag(tag);
			setEndDate(endDate);
			setDuration(duration);
			setUnits(units);
			setCoregistrant(coreg);
		}
		
		public Submission getSubmission() {
			return _submission;
		}
		
		public void setSubmission(Submission submission) {
			_submission = submission;
		}
		
		public Tag getTag() {
			return _tag;
		}
		
		public void setTag(Tag tag) {
			_tag = tag;
		}
		
		public String getUnits() {
			return _units;
		}
		
		public void setUnits(String units) {
			_units = units;
		}
		
		public Date getEndDate() {
			return _endDate;
		}
		
		public void setEndDate(Date endDate) {
			_endDate = endDate;
		}
		
		public Integer getDuration() {
			return _duration;
		}
		
		public void setDuration(Integer duration) {
			_duration = duration;
		}
		
		public String getCoregistrant() {
			return _coreg;
		}
		
		public void setCoregistrant(String coreg) {
			_coreg = coreg;
		}
		
		@Override
		public int hashCode() {
			return new HashCodeBuilder()
				.append(_submission)
				.append(_tag)
				.append(_endDate)
				.append(_duration)
				.append(_units)
				.append(_coreg)
				.build();
		}
		
		@Override
		public boolean equals(Object obj) {
			if(obj == null || !(obj instanceof NumberPK))
				return false;
			
			NumberPK oth = (NumberPK) obj;
			return new EqualsBuilder()
					.append(_submission, oth.getSubmission())
					.append(_tag, oth.getTag())
					.append(_endDate, oth.getEndDate())
					.append(_duration, oth.getDuration())
					.append(_units, oth.getUnits())
					.append(_coreg, oth.getCoregistrant())
					.build();
		}
	}
}