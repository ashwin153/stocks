package com.ashwin.fri.stocks.forecast;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import com.ashwin.fri.stocks.hibernate.HibernateConfig;
import com.ashwin.fri.stocks.hibernate.Number;
import com.ashwin.fri.stocks.hibernate.Submission;
import com.ashwin.fri.stocks.hibernate.Submission.FiscalPeriod;
import com.ashwin.fri.stocks.hibernate.Tag;
import com.ashwin.fri.stocks.neural.NeuralNetwork;

public class Forecast implements Serializable {
	
	private static final long serialVersionUID = 6924154459214792834L;
	
	private static final double MAX_DEVIATIONS = 4.0;
	
	private NeuralNetwork _net;
	
	private List<Tag> _inputs;
	private List<Tag> _outputs;
	
	private List<Statistic> _sin;
	private List<Statistic> _sout;
	
	private int _sic;
	
	/**
	 * Creates a new forecast for a particular sic code, using the "inputs" most
	 * common tags as inputs and the outputs tags as outputs.
	 * 
	 * @param sic
	 * @param inputs
	 * @param outputs
	 */
	@SuppressWarnings("unchecked")
	public Forecast(int sic, int inputs, List<Tag> outputs) {
		_net = new NeuralNetwork(inputs+1, 10, outputs.size());
		_outputs = outputs;
		_sic = sic;
		
		// Finds the inputs most common tags in all the submissions in this
		// particular industry, grouped by tag name.
		Session session = HibernateConfig.FACTORY.openSession();
		Transaction tx  = session.beginTransaction();
		
		List<Object[]> result = session.createCriteria(Tag.class)
				.createAlias("numbers", "number")
				.createAlias("number.submission", "submission")
				.createAlias("submission.registrant", "registrant")
				.add(Restrictions.eq("custom", false))
				.add(Restrictions.eq("abstract", false))
				.add(Restrictions.isNotNull("number.value"))
				.add(Restrictions.eq("registrant.sic", sic))
				.add(Restrictions.eq("submission.detailed", true))
				.add(Restrictions.or(
						Restrictions.eq("submission.form", "10-K"),
						Restrictions.eq("submission.form", "10-K/A"),
						Restrictions.eq("submission.form", "10-Q"),
						Restrictions.eq("submission.form", "10-Q/A")))
				.setProjection(Projections.projectionList()
						.add(Projections.groupProperty("name"))
						.add(Projections.property("datatype"))
						.add(Projections.property("iord"))
						.add(Projections.property("crdr"))
						.add(Projections.property("label"))
						.add(Projections.property("foc"))
						.add(Projections.countDistinct("submission.adsh").as("count")))
				.addOrder(Order.desc("count"))
				.setMaxResults(inputs).list();

		_inputs = new ArrayList<Tag>();
		for (Object[] arr : result)
			_inputs.add(new Tag((String) arr[0], null, false, false, (String) arr[1],
					(String) arr[2], (String) arr[3], (String) arr[4], (String) arr[5], null));
	
		tx.rollback();
		session.close();
	}
	
	/**
	 * Predicts outputs for the given submission. Outputs are the growth rates of the
	 * corresponding output tags.
	 * 
	 * @param submission
	 * @return
	 */
	public List<Double> predict(Submission submission) {
		Double[] growth = getGrowthVector(submission, _inputs);
		List<Double> nin = getNeuralInputVector(submission, growth);

		// Adjust the outputs of the neural network back onto the proper interval
		// and perform the inverse of the normalization procedure to recover actuals.
		List<Double> out = _net.execute(nin);
		for(int i = 0; i < out.size(); i++)
			out.set(i, _sout.get(i).raw(MAX_DEVIATIONS * (out.get(i) - 0.5))); 
		return out;
	}
	
	/**
	 * Trains the forecast on data from the specified fiscal year and fiscal period.
	 * The method returns all submissions in the specified time period that the forecast
	 * was trained over. It is important to note that forecasts should be trained
	 * chronologically. This is because the saved statistics that are used to normalize
	 * inputs and outputs are overwritten every time a new data set is trained. The idea
	 * is that you can train the new data set every quarter without have to train all
	 * the old data all over again.
	 * 
	 * @param fiscalYear
	 * @param fp
	 * @param learningRate
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public List<Submission> train(int fiscalYear, FiscalPeriod fp, double learningRate) throws Exception {
		// Step 1: Load all the submissions that we will use to train the neural network.
		Session session = HibernateConfig.FACTORY.openSession();
		Transaction tx  = session.beginTransaction();
		
		DateFormat df = new SimpleDateFormat("MM/dd/yyyy");
		Date start = df.parse("01/01/" + fiscalYear);
		Date end   = df.parse("12/31/" + fiscalYear);
		
		List<Submission> submissions = session.createCriteria(Submission.class)
				.createAlias("registrant", "registrant")
				.add(Restrictions.eq("registrant.sic", _sic))
				.add(Restrictions.eq("detailed", true))
				.add(Restrictions.eq("fiscalPeriod", fp))
				.add(Restrictions.ge("fiscalYearEndDate", start))
				.add(Restrictions.le("fiscalYearEndDate", end))
				.add(Restrictions.or(
						Restrictions.eq("form", "10-K"),
						Restrictions.eq("form", "10-K/A"),
						Restrictions.eq("form", "10-Q"),
						Restrictions.eq("form", "10-Q/A")))
				.list();
		
		tx.rollback();
		session.close();
				
		// Step 2: Load all the numbers that correspond to the input and output tags
		// for each of the submissions in the fiscal period. r -> raw.
		Double[][] rin  = new Double[submissions.size()][_inputs.size()];
		Double[][] rout = new Double[submissions.size()][_outputs.size()];
		for(int i = 0; i < submissions.size(); i++) {
			rin[i]  = getGrowthVector(submissions.get(i), _inputs);
			rout[i] = getGrowthVector(submissions.get(i), _outputs);
		}
		
		// Step 3: Calculate the column statistics for the columns of the input and
		// output matrixes. We don't want to include null valued columns in the
		// statistic calculation. s -> stat
		_sin = getColumnStatistics(rin);
		_sout = getColumnStatistics(rout);
		
		// Step 4: Run each submission through the neural network. First, compute
		// what the input vector to the neural network should be. If a value is null
		// in the raw data, then interpolate its value from the average number of
		// deviations from the mean and the column statistics for the particular tag.
		List<Submission> training = new ArrayList<Submission>();
		submission: for(int i = 0; i < rin.length; i++) {
			// If the submission contains output values that are null, then it is
			// not a suitable candidate for training. If either the output data or the
			// input data contains data more than MAX_DEVIATIONS from the mean, then the
			// data is too unreliable to use.
			List<Double> nout = new ArrayList<Double>();
			for(int j = 0; j < _sout.size(); j++) {
				if(rout[i][j] == null || Math.abs(_sout.get(j).normalize(rout[i][j])) > MAX_DEVIATIONS)
					continue submission;
				else
					nout.add(_sout.get(j).normalize(rout[i][j]) / MAX_DEVIATIONS + 0.5);
			}
			
			List<Double> nin = getNeuralInputVector(submissions.get(i), rin[i]);
			for(int j = 0; j < _sin.size(); j++)
				if(_sin.get(j).normalize(nin.get(j)) > MAX_DEVIATIONS)
					continue submission;
			
			_net.backpropagate(nin, nout, learningRate);
			training.add(submissions.get(i));
		}
		
		return training;
	}
	
	private List<Statistic> getColumnStatistics(Double[][] matrix) {
		// We ignore growth rates greater than a factor of 50, because these
		// growth rates are erroneous and will likely cause problems when
		// performing statistical calculations.
		List<Statistic> statistics = new ArrayList<Statistic>();
		for(int j = 0; j < matrix[0].length; j++) {
			List<Double> sample = new ArrayList<Double>();
			for(int i = 0; i < matrix.length; i++)
				if(matrix[i][j] != null)
					sample.add(matrix[i][j]);
			statistics.add(new Statistic(sample));
		}
		return statistics;
	}
	
	/**
	 * Returns a vector containing the quarter-over-quarter growth of each tag for the
	 * particular submission. If a particular tag is not present, then the corresponding
	 * value in the growth vector will be null.
	 * 
	 * @param submission
	 * @param tags
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private Double[] getGrowthVector(Submission submission, List<Tag> tags) {
		Session session = HibernateConfig.FACTORY.openSession();
		Transaction tx  = session.beginTransaction();
		
		List<String> names = new ArrayList<String>();
		for(Tag tag : tags)
			names.add(tag.getName());
		
		List<Number> numbers = session.createCriteria(Number.class)
				.createAlias("submission", "submission")
				.createAlias("tag", "tag")
				.add(Restrictions.eq("submission", submission))
				.add(Restrictions.in("tag.name", names))
				.add(Restrictions.isNotNull("value"))
				.addOrder(Order.desc("tag.name"))
				.addOrder(Order.asc("duration"))
				.addOrder(Order.desc("endDate"))
				.list();
				
		Double[] vector = new Double[tags.size()];
		for(int i = 0; i < numbers.size() - 1; i++) {
			String t1 = numbers.get(i+0).getTag().getName();
			String t2 = numbers.get(i+1).getTag().getName();
						
			if(!names.contains(t1) || !t1.equals(t2))
				continue;
			
			// Calculate the quarter-over-quarter growth rate of the number
			// and place the growth rate into the correct location in the
			// input and output matrixes.
			double vn = numbers.get(i+0).getValue().doubleValue();
			double vo = numbers.get(i+1).getValue().doubleValue();
			long off  = numbers.get(i+0).getEndDate().getTime() - 
					    numbers.get(i+1).getEndDate().getTime();
			int quarters = (int) Math.round(off / 31556900000.0 * 4);
			
			double growth = 1 + (vn - vo) / Math.abs((vo == 0) ? 1 : vo);
			double norm   = (quarters <= 1) ? growth : 
							Math.signum(growth) * Math.pow(Math.abs(growth), 1.0 / quarters);
			
			if(names.contains(t1) && vector[names.indexOf(t1)] == null)
				vector[names.indexOf(t1)] = norm;
		}
		
		tx.rollback();
		session.close();
		return vector;
	}
		
	/**
	 * Utilizes the column statistics to interpolate null values in the growth vector. First,
	 * it calculates the average number of deviations from the mean. Then, it uses the column
	 * statistics to determine what the raw growth should have been.
	 * 
	 * @param vector
	 * @param stats
	 * @return
	 */
	private List<Double> getNeuralInputVector(Submission submission, Double[] vector) {
		// Calculate the average number of deviations from the mean. This will 
		// enable us to interpolate null values based on the column statistics
		// for the tag.
		double avg = 0.0;
		for(int i = 0; i < vector.length; i++) {
			double value = (vector[i] == null) ? _sin.get(i).getMean() : vector[i];
			avg += _sin.get(i).normalize(value);
		}
		avg /= _sin.size();
		
		List<Double> neural = new ArrayList<Double>();
		for(int i = 0; i < _sin.size(); i++) {
			if(vector[i] == null)
				neural.add(_sin.get(i).raw(avg));
			else
				neural.add(vector[i]);
		}
		
		neural.add(Double.valueOf(submission.getFilerStatus().ordinal()));
		return neural;
	}

}
