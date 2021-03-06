package com.ashwin.fri.stocks.forecast;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The statistic class computes and saves statistics about a data set.
 * It computes important summary statistics: such as mean, standard
 * deviation, skewness, kurtosis, and margin of error.
 * 
 * @author ashwin
 *
 */
public class Statistic implements Serializable {
	
	private static final long serialVersionUID = -995301427142018566L;
	
	private double _mean, _stdev, _skewness, _kurtosis, _marginOfError;
	private int _N;

	public Statistic(List<Double> values) {
		List<Double> sorted = new ArrayList<Double>(values);
		Collections.sort(sorted);
		double q1  = sorted.get((int) (1 * sorted.size() / 4.0));
		double q3  = sorted.get((int) (3 * sorted.size() / 4.0));
		double iqr = q3 - q1;
				
		// Remove all the outliers from the list of values. Outliers are
		// defined as elements whose values exceed q3 + 1.5 * iqr or
		// values less than q1 - 1.5 * iqr. Once these values have been
		// removed from the sample, statistics can be calculated normally.
		List<Double> sample = new ArrayList<Double>();
		for(Double value : values)
			if(value >= q1 - 1.8 * iqr && value  <= q3 + 1.8 * iqr)
				sample.add(value);
				
		_mean = getCentralMoment(sample, 0, 1);
		_N = sample.size();
		
		// Some statistics require computations on sample statistics.
		// To make a population statistic into a sample statistic, multiply
		// by the bias quantity.
		double bias = _N / (_N - 1);
		
		double variance = getCentralMoment(sample, _mean, 2);
		_stdev = Math.sqrt(variance * bias);
		if(_stdev == 0)
			_stdev = Math.pow(10, -9);
		
		_skewness = getCentralMoment(sample, _mean, 3) * bias / Math.pow(_stdev, 3);
		_kurtosis = getCentralMoment(sample, _mean, 4) / Math.pow(_stdev, 4);
		double standardError = _stdev / Math.sqrt(_N);
		_marginOfError = 1.96 * standardError;
	}
	
	public double getMean() {
		return _mean;
	}
	
	public double getStandardDeviation() {
		return _stdev;
	}
	
	public double getSkewness() {
		return _skewness;
	}
	
	public double getKurtosis() {
		return _kurtosis;
	}
	
	public double getMarginOfError() {
		return _marginOfError;
	}
	
	/**
	 * Normalizes the given value using the calculated mean and
	 * standard deviation.
	 * 
	 * @param value
	 * @return
	 */
	public double normalize(double value) {
		return (value - _mean) / _stdev;
	}
	
	/**
	 * Calculates the raw value from a normalized value using the
	 * calculated mean and standard deviation.
	 * 
	 * @param norm
	 * @return
	 */
	public double raw(double norm) {
		return norm * _stdev + _mean;
	}
	
	private static double getCentralMoment(List<Double> values, double mean, int moment) {
		double sum = 0.0;
		for(int i = 0; i < values.size(); i++)
			sum += Math.pow(values.get(i) - mean, moment);
		return sum / values.size();
	}
}
