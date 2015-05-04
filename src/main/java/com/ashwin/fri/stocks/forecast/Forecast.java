package com.ashwin.fri.stocks.forecast;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import com.ashwin.fri.stocks.hibernate.HibernateConfig;
import com.ashwin.fri.stocks.hibernate.Number;
import com.ashwin.fri.stocks.hibernate.Submission;
import com.ashwin.fri.stocks.hibernate.Tag;
import com.ashwin.fri.stocks.neural.NeuralNetwork;

public class Forecast implements Serializable {
	
	private static final long serialVersionUID = 6924154459214792834L;
	
	private static final double MAX_DEVIATIONS = 2.2;
	
	private List<NeuralNetwork> _networks;
	
	private List<Tag> _inputs;
	private List<Tag> _outputs;
	
	private List<Statistic> _sin;
	private List<Statistic> _sout;
	
	private int _sic;
	
	public Forecast(int sic, int inputs, String... tagNames) {
		this(sic, inputs, getTagsByNames(tagNames));
	}
	
	/**
	 * Creates a new forecast for a particular sic code, using the "inputs" most
	 * common tags as inputs and the output tag as outputs.
	 * 
	 * @param sic
	 * @param inputs
	 * @param outputs
	 */
	@SuppressWarnings("unchecked")
	public Forecast(int sic, int inputs, List<Tag> outputs) {
		_networks = new ArrayList<NeuralNetwork>();
		for(int i = 0; i < outputs.size(); i++)
			_networks.add(new NeuralNetwork(inputs+2, 10, 3, 1));
		
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
	
	public List<Tag> getInputTags() {
		return _inputs;
	}
	
	public List<Tag> getOutputTags() {
		return _outputs;
	}
	
	public int getSic() {
		return _sic;
	}
	
	public List<NeuralNetwork> getNeuralNetwork() {
		return _networks;
	}
	
	/**
	 * Predicts outputs for the given submission. Outputs are the growth rates of the
	 * corresponding output tags.
	 * 
	 * @param submission
	 * @return
	 */
	public List<Double> predict(Submission submission) {
		List<Double> gi = getGrowthVector(submission, _inputs);
		List<Double> ii = getInterpolatedVector(gi, _sin);
		
		// Adds additional data to the interpolated vector. Note these additions
		// must be performed in the exact same order as the additions in the train
		// method. If this requirement is not satisfied, then results are unpredictable.
		ii.add(Double.valueOf(submission.getFilerStatus().ordinal()+1));
		
		// Adjust the outputs of the neural network back onto the proper interval
		// and perform the inverse of the normalization procedure to recover actuals.
		List<Double> out = new ArrayList<Double>();
		for(int i = 0; i < _networks.size(); i++)
			out.add(_sout.get(i).raw(MAX_DEVIATIONS * (_networks.get(i).execute(ii).get(0) - 0.5)));
		
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
	public Set<Submission> train(Date start, Date end, double confidence, double learningRate) throws Exception {
		// Step 1: Generate the Training Data
		// The training data includes all submissions from the start date to the end date
		// that have a maximum of (1 - confidence) * _inputs.size() interpolated values.
		Session session = HibernateConfig.FACTORY.openSession();
		Transaction tx  = session.beginTransaction();
		
		List<Submission> submissions = session.createCriteria(Submission.class)
				.createAlias("registrant", "registrant")
				.add(Restrictions.eq("registrant.sic", _sic))
				.add(Restrictions.eq("detailed", true))
				.add(Restrictions.or(
						Restrictions.eq("form", "10-K"),
						Restrictions.eq("form", "10-K/A"),
						Restrictions.eq("form", "10-Q"),
						Restrictions.eq("form", "10-Q/A")))
				.add(Restrictions.ge("filingDate", start))
				.add(Restrictions.le("filingDate", end))
				.addOrder(Order.desc("registrant.name"))
				.addOrder(Order.asc("filingDate"))
				.list();
		
		tx.rollback();
		session.close();
		
		// Step 2: Throw out invalid training submission candidates. These submissions
		// include those whose values are abnormally large or abnormally small and those
		// that contain excessive null inputs or any null output values. 
		List<List<Double>> ri  = new ArrayList<List<Double>>();
		List<List<Double>> ro = new ArrayList<List<Double>>();
		for(int i = 0; i < submissions.size() - 1; i++) {
			// The output values of a submission are the current values of the next filed
			// submission. If the data for the next filed submission is unknown, then we
			// can't use this submission as traning data.
			if(!submissions.get(i+0).getRegistrant()
					.equals(submissions.get(i+1).getRegistrant()))
				continue;
			
			List<Double> gi = getGrowthVector(submissions.get(i+0), _inputs);
			List<Double> go = getGrowthVector(submissions.get(i+1), _outputs);

			double totalNullValues = 0;
			for(Double val : gi)
				if(val == null)
					totalNullValues++;
			
			// If the input vector doesn't contain too may null values and the output
			// vector doesn't contain any input values, we add the two vectors to the
			// training raw data set.
			if(totalNullValues / gi.size() <= 1 - confidence) {
				ri.add(gi);
				ro.add(go);
			}
		}
		
		// Step 3: Calculate the column statistics for the columns of the input and
		// output matrixes. We don't want to include null valued columns in the
		// statistic calculation. s -> stat
		_sin = getColumnStatistics(ri);
		_sout = getColumnStatistics(ro);
		
		// Step 4: Run each submission through the neural network. First, compute
		// what the input vector to the neural network should be. If a value is null
		// in the raw data, then interpolate its value from the average number of
		// deviations from the mean and the column statistics for the particular tag.
		Set<Submission> training = new HashSet<Submission>();
		for(int i = 0; i < ri.size() && i < ro.size(); i++) {
			List<Double> ii = getInterpolatedVector(ri.get(i), _sin);
			List<Double> oi = getInterpolatedVector(ro.get(i), _sout);
			
			if(getAbsoluteMaximum(ii) > MAX_DEVIATIONS || getAbsoluteMaximum(oi) > MAX_DEVIATIONS)
				continue;
			
			for(int j = 0; j < oi.size(); j++) {
				List<Double> to = new ArrayList<Double>();
				to.add(oi.get(j) / MAX_DEVIATIONS + 0.5);
				
			}
			
			// Add additional data to the interpolated input vector. Note that this has
			// to be performed in the exact same order as it is in the predict method
			// or the input vector components will differ and results will be unpredictable.
			ii.add(Double.valueOf(submissions.get(i).getFilerStatus().ordinal()+1));
			
			// Transform the interpolated output values onto the proper interval [0.0 - 1.0]
			for(int j = 1; j < oi.size(); j++) {
				if(ro.get(i).get(j-1) == null)
					continue;
				
				List<Double> to = new ArrayList<Double>();
				to.add(oi.get(j) / MAX_DEVIATIONS + 0.5);
				_networks.get(j-1).backpropagate(ii, to, learningRate);
				training.add(submissions.get(i));
			}
		}
		
		return training;
	}
		
//	public List<Submission> train(int fiscalYear, FiscalPeriod fp, double learningRate) throws Exception {
//		// Step 1: Load all the submissions that we will use to train the neural network.
//		Session session = HibernateConfig.FACTORY.openSession();
//		Transaction tx  = session.beginTransaction();
//		
//		DateFormat df = new SimpleDateFormat("MM/dd/yyyy");
//		Date start = df.parse("01/01/" + fiscalYear);
//		Date end   = df.parse("12/31/" + fiscalYear);
//		
//		List<Submission> submissions = session.createCriteria(Submission.class)
//				.createAlias("registrant", "registrant")
//				.add(Restrictions.eq("registrant.sic", _sic))
//				.add(Restrictions.eq("detailed", true))
//				.add(Restrictions.eq("fiscalPeriod", fp))
//				.add(Restrictions.ge("fiscalYearEndDate", start))
//				.add(Restrictions.le("fiscalYearEndDate", end))
//				.add(Restrictions.or(
//						Restrictions.eq("form", "10-K"),
//						Restrictions.eq("form", "10-K/A"),
//						Restrictions.eq("form", "10-Q"),
//						Restrictions.eq("form", "10-Q/A")))
//				.addOrder(Order.desc("registrant.name"))
//				.addOrder(Order.asc("filingDate"))
//				.list();
//		
//		tx.rollback();
//		session.close();
//				
//		// Step 2: Load all the numbers that correspond to the input and output tags
//		// for each of the submissions in the fiscal period. r -> raw.
//		Double[][] rin  = new Double[submissions.size()][_inputs.size()];
//		Double[][] rout = new Double[submissions.size()][_outputs.size()];
//		for(int i = 0; i < submissions.size(); i++) {
//			rin[i]  = getGrowthVector(submissions.get(i), _inputs);
//			rout[i] = getGrowthVector(submissions.get(i), _outputs);
//		}
//		
//		// Step 3: Calculate the column statistics for the columns of the input and
//		// output matrixes. We don't want to include null valued columns in the
//		// statistic calculation. s -> stat
//		_sin = getColumnStatistics(rin);
//		_sout = getColumnStatistics(rout);
//		
//		// Step 4: Run each submission through the neural network. First, compute
//		// what the input vector to the neural network should be. If a value is null
//		// in the raw data, then interpolate its value from the average number of
//		// deviations from the mean and the column statistics for the particular tag.
//		List<Submission> training = new ArrayList<Submission>();
//		submission: for(int i = 0; i < rin.length - 1; i++) {
//			// We use the next submission to determine output data. If the next submission
//			// doesn't have the same registrant, then ignore this submission.
//			Submission cur = submissions.get(i+0);
//			Submission nxt = submissions.get(i+1);
//			
//			if(!cur.getRegistrant().equals(nxt.getRegistrant()))
//				continue;
//			
//			// If the submission contains output values that are null, then it is
//			// not a suitable candidate for training. If either the output data or the
//			// input data contains data more than MAX_DEVIATIONS from the mean, then the
//			// data is too unreliable to use.
//			List<Double> nout = new ArrayList<Double>();
//			for(int j = 0; j < _sout.size(); j++) {
//				Double norm = (rout[i+1][j] == null) ? null : _sout.get(j).normalize(rout[i+1][j]); 
//				if(norm == null || Math.abs(norm) > MAX_DEVIATIONS)
//					continue submission;
//				else
//					nout.add(norm / MAX_DEVIATIONS + 0.5);
//			}
//			
//			List<Double> nin = getInterpolatedVector(rin[i], _sin);
//			nin.add(Double.valueOf(submissions.get(i).getFilerStatus().ordinal()+1));
//			for(int j = 0; j < _sin.size(); j++)
//				if(_sin.get(j).normalize(nin.get(j)) > MAX_DEVIATIONS)
//					continue submission;
//			
//			_net.backpropagate(nin, nout, learningRate);
//			training.add(submissions.get(i));
//		}
//		
//		return training;
//	}
	
	/**
	 * Returns summary statistics (mean, std) of the columns of the matrix. This method
	 * ignores all values that are null.
	 * 
	 * @param matrix
	 * @return
	 */
	private List<Statistic> getColumnStatistics(List<List<Double>> matrix) {
		// We ignore growth rates greater than a factor of 50, because these
		// growth rates are erroneous and will likely cause problems when
		// performing statistical calculations.
		List<Statistic> statistics = new ArrayList<Statistic>();
		for(int j = 0; j < matrix.get(0).size(); j++) {
			List<Double> sample = new ArrayList<Double>();
			for(int i = 0; i < matrix.size(); i++)
				if(matrix.get(i).get(j) != null)
					sample.add(matrix.get(i).get(j));
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
	private List<Double> getGrowthVector(Submission submission, List<Tag> tags) {
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
		return Arrays.asList(vector);
	}
	
	private double getAbsoluteMaximum(List<Double> values) {
		double max = 0.0;
		for(Double value : values)
			if(value != null && Math.abs(value) > max)
				max = value;
		return max;
	}
	
	/**
	 * Utilizes the column statistics to interpolate null values in the growth vector. First,
	 * it calculates the average number of deviations from the mean. Then, it uses the column
	 * statistics to determine what the raw growth should have been. The average number of
	 * deviations is included as the first component of the returned vector.
	 * 
	 * @param vector
	 * @param stats
	 * @return
	 */
	private List<Double> getInterpolatedVector(List<Double> vector, List<Statistic> stats) {
		// Calculate the average number of deviations from the mean. This will 
		// enable us to interpolate null values based on the column statistics
		// for the tag.
		Double avg = 0.0;
		for(int i = 0; i < vector.size(); i++) {
			Double value = (vector.get(i) == null) ? stats.get(i).getMean() : vector.get(i);
			Double norm  = stats.get(i).normalize(value);
			avg += (norm.isNaN()) ? 0.0 : norm;
		}
		avg /= stats.size();
		
		// Add the average number of deviations from the mean for the submission
		// to the neural input vector. This signifies how well the company did relative
		// to other companies in the industry for this particular fiscal period. We also
		// add the filer status to the input vector. Larger companies have lower filer periods.
		// The size of a company has a huge effect on growth (smaller usually is faster).
		List<Double> interpolated = new ArrayList<Double>();
		interpolated.add(avg);
		
		for(int i = 0; i < stats.size(); i++) {
			if(vector.get(i) == null)
				interpolated.add(stats.get(i).raw(avg));
			else
				interpolated.add(vector.get(i));
		}
		
		return interpolated;
	}

	/**
	 * Fetches tags from the database that have the given name. The returned tags
	 * will have null versions, but will have all other fields populated.
	 * 
	 * @param name
	 * @return tag
	 */
	public static List<Tag> getTagsByNames(String... names) {
		Session session = HibernateConfig.FACTORY.openSession();
		Transaction tx  = session.beginTransaction();
		List<Tag> tags  = new ArrayList<Tag>();
		
		for(String name : names) {
			Object[] arr = (Object[]) session.createCriteria(Tag.class)
					.add(Restrictions.eq("name", name))
					.add(Restrictions.eq("custom", false))
					.add(Restrictions.eq("abstract", false))
					.setProjection(Projections.projectionList()
							.add(Projections.groupProperty("name"))
							.add(Projections.property("datatype"))
							.add(Projections.property("iord"))
							.add(Projections.property("crdr"))
							.add(Projections.property("label"))
							.add(Projections.property("foc")))
					.uniqueResult();
			
			tags.add(new Tag((String) arr[0], null, false, false, (String) arr[1],
					(String) arr[2], (String) arr[3], (String) arr[4], (String) arr[5], null));
		}
		
		tx.rollback();
		session.close();
		
		return tags;
	}
}
