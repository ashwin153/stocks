package com.ashwin.fri.stocks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.ashwin.fri.stocks.hibernate.HibernateDataManager;
import com.ashwin.fri.stocks.model.Submission;
import com.ashwin.fri.stocks.neural.NeuralNetwork;

public class StockManager {
	
	private static final double LEARNING_RATE = 1.2;
	private static final int NUMBER_OF_INPUTS = 20;
	private static final int SIC_CODE   	  = 1311;
	
	public static void main(String[] args) throws IOException {
		HibernateDataManager hm = new HibernateDataManager(SIC_CODE, NUMBER_OF_INPUTS);
		List<String> inputs  = hm.getInputTags();
		List<String> outputs = hm.getOutputTags();
		List<Submission> submissions = hm.getSubmissions();
		
		List<Double[]> sin  = hm.getAggregateGrowthStatistics(inputs);
		List<Double[]> sout = hm.getAggregateGrowthStatistics(outputs);
		
		NeuralNetwork net = new NeuralNetwork(inputs.size(), 20, 12, 8, outputs.size());
		for(Submission submission : submissions) {
			// Skip all submissions that do not have a full set of actuals to use for output.
			// This is because the normalized growth vectors contain interpolated results.
			List<Double> aout = hm.getActualGrowthVector(submission, outputs);
			List<Double> nout = hm.getNormGrowthVector(aout, sout);
			if(aout.contains(null) || nout.get(0) > 4.0) continue;
			
			List<Double> ain  = hm.getActualGrowthVector(submission, inputs);
			List<Double> nin  = hm.getNormGrowthVector(ain, sin);
			
			// We need to move the normalized output vector onto the interval [0.0 - 1.0].
			// The normalized output vector will lie on the interval [-inf, inf]; however, these
			// values will be concentrated on the interval [-4, 4].
			List<Double> tout = new ArrayList<Double>();
			for(Double value : nout)
				tout.add(value / 8.0 + 0.5);
			net.backpropagate(nin, tout, LEARNING_RATE);
		}
		
		Submission submission = new Submission();
		submission.setAdsh("0000311471-14-000006");
		
		List<Double> ain = hm.getActualGrowthVector(submission, inputs);
		List<Double> nin = hm.getNormGrowthVector(ain, sin);
		List<Double> out = net.execute(nin);
		
		for(int i = 0; i < out.size(); i++) {
			double adj = 8 * (out.get(i) - 0.5);
			double val = adj * sout.get(i)[1] + sout.get(i)[0];
			System.out.println("Expected Growth Rate for " + outputs.get(i) + "\t\t" + val);
		}
	}
}
