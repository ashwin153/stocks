package com.ashwin.fri.stocks.hibernate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import com.ashwin.fri.stocks.model.Number;
import com.ashwin.fri.stocks.model.Submission;
import com.ashwin.fri.stocks.model.Tag;

public class HibernateDataManager {
	
	private SessionFactory _factory;
	private List<Submission> _submissions;
	private List<String> _in;
	private List<String> _out;
	
	/**
	 * Creates a new data set for the given industry and the given number of inputs.
	 * This data set can be serialized and reused to populate a neural network, etc.
	 * 
	 * @param sic industry to create data set for
	 * @param inputs number of inputs to the network
	 * @return a data set
	 * @throws IOException unable to begin session
	 */
	@SuppressWarnings("unchecked")
	public HibernateDataManager(int sic, int inputs) throws IOException {
		_factory = HibernateConfig.getHibernateSessionFactory();
		Session session = _factory.openSession();
		Transaction tx = session.beginTransaction();
		
		// Step 1: Load all the submissions are 10-Qs and 10-Ks that were filed by registrants
		// in the given sic code between the start and end times.
		_submissions = session.createCriteria(Submission.class)
				.createAlias("registrant", "registrant")
				.add(Restrictions.eq("registrant.sic", sic))
				.add(Restrictions.eq("detailed", true))
				.add(Restrictions.or(
						Restrictions.eq("form", "10-K"),
						Restrictions.eq("form", "10-K/A"),
						Restrictions.eq("form", "10-Q"),
						Restrictions.eq("form", "10-Q/A")))
				.list();
		
		// Step 2: Find the tags that appear most often in submissions in this industry and determine
		// the average value and standard deviations of values of this tag in this industry limited to
		// the number of inputs specified as a parameter to this method.
		List<Object[]> in = session.createCriteria(Tag.class)
				.createAlias("numbers", "number")
				.createAlias("number.submission", "submission")
				.add(Restrictions.eq("custom", false))
				.add(Restrictions.eq("abstract", false))
				.add(Restrictions.isNotNull("number.value"))
				.add(Restrictions.eq("number.duration", 1))
				.add(Restrictions.in("number.submission", _submissions))
				.setProjection(Projections.projectionList()
						.add(Projections.groupProperty("name"))
						.add(Projections.countDistinct("submission.adsh").as("count")))
				.addOrder(Order.desc("count"))
				.setMaxResults(inputs)
				.list();
		
		_in = new ArrayList<String>();
		for(Object[] arr : in)
			_in.add((String) arr[0]);
		_out = Arrays.asList("SalesRevenueNet");
		
		tx.rollback();
		session.close();
	}
	
	public List<Submission> getSubmissions() {
		return _submissions;
	}
	
	public List<String> getInputTags() {
		return _in;
	}
	
	public List<String> getOutputTags() {
		return _out;
	}
	
	/**
	 * Returns a vector representing the growth of each specified tag in the submission.
	 * If a tag is not present in the submission or there is not enough data to compute
	 * the growth, the corresponding component in the growth vector will be null.
	 * 
	 * @param submission
	 * @param tags
	 * @return
	 * @throws IOException unable to establish session
	 */
	@SuppressWarnings("unchecked")
	public List<Double> getActualGrowthVector(Submission submission, List<String> tags) {
		Session session = _factory.openSession();
		Transaction tx = session.beginTransaction();
		
		List<Double> vector = new ArrayList<Double>();
		for(String tag : tags) {
			List<Number> numbers = session.createCriteria(Number.class)
					.add(Restrictions.eq("submission", submission))
					.add(Restrictions.eq("tag.name", tag))
					.add(Restrictions.eq("duration", 1))
					.add(Restrictions.isNotNull("value"))
					.addOrder(Order.desc("endDate"))
					.setMaxResults(2)
					.list();
			
			if(numbers == null || numbers.size() != 2) {
				// If there are not enough numbers for the given tag, then
				// put all null as the input value. This null value will be
				// corrected later based on the industry average/stdev of
				// this particular tag.
				vector.add(null);
			} else {
				double v1 = numbers.get(0).getValue().doubleValue();
				double v2 = numbers.get(1).getValue().doubleValue();	
				long off  = numbers.get(0).getEndDate().getTime() - 
							numbers.get(1).getEndDate().getTime();
				double quarters = Math.round(off / 31556900000.0 * 4);
				
				// We only want terms that are one year apart. Anything longer
				// is too long to extract meaningful results from.
				if(quarters == 0 || v2 == 0)
					vector.add(null);
				else
					vector.add(Math.signum(v1 / v2) * Math.pow(Math.abs(v1 / v2), 1 / quarters));
			}
		}
		
		tx.rollback();
		session.close();
		return vector;
	}
	
	/**
	 * Returns a normalized version of the actuals vector. Null values are replaced with 
	 * the average difference between known values in the actuals vector and their
	 * corresponding means in the tags map.
	 * 
	 * @param actuals
	 * @param tags
	 * @return normalized growth vector
	 */
	public List<Double> getNormGrowthVector(List<Double> actuals, List<Double[]> stats) {
		// Determine the average difference between known values in the actuals and
		// their corresponding industry-wide means. This mean will give us a value that
		// we can use to fill in null values in the actuals vector.	
		double sum = 0.0;
		for(int i = 0; i < stats.size(); i++) {
			double value = (actuals.get(i) == null) ? stats.get(i)[0] : actuals.get(i);
			sum += stats.get(i)[0] - value;
		}
		double mean = sum / stats.size();
			
		// Normalize the values in actuals and use the calculated mean to fill in for
		// null values in the actuals vector.
		List<Double> norm = new ArrayList<Double>();
		for(int i = 0; i < stats.size(); i++) {
			double value = (actuals.get(i) == null) ? mean : actuals.get(i);
			norm.add((value - stats.get(i)[0]) / stats.get(i)[1]);
		}
		
		return norm;
	}

	/**
	 * Returns the mean and standard deviation of the first derivative of the tag (e.g., if the tag
	 * is SalesRevenueNet, this method would return the mean and standard deviation of the rate of 
	 * change in SalesRevenueNet or revenue growth).
	 * 
	 * @param session
	 * @param submissions
	 * @param tag
	 * @return arr[0] = mean, arr[1] = stdev, arr[2] = n
	 * @throws IOException unable to establish session
	 */
	public List<Double[]> getAggregateGrowthStatistics(List<String> tags) {
		List<List<Double>> actuals = new ArrayList<List<Double>>();
		for(Submission submission : _submissions)
			actuals.add(getActualGrowthVector(submission, tags));
		
		List<Double[]> stats = new ArrayList<Double[]>();
		for(int i = 0; i < tags.size(); i++) {
			double sum = 0.0, ssum = 0.0, n = 0;
			for(List<Double> actual : actuals) {
				if(actual.get(i) != null && actual.get(i) < 2.0) {
					sum  += actual.get(i);
					ssum += actual.get(i) * actual.get(i);
					n++;
				}
			}
			
			double mean  = sum / n;
			double stdev = Math.sqrt((ssum - (sum * sum) / n) / (n - 1)); 
			stats.add(new Double[] { mean, stdev, n } );
		}
		
		return stats;
	}
	
}
