package com.ashwin.fri.stocks;

import com.ashwin.fri.stocks.hibernate.HibernateConfig;
import com.ashwin.fri.stocks.hibernate.Submission;

import org.hibernate.Session;

public class ForecastTester {
	
	public static void main(String[] args) throws Exception {
//		Forecast forecast = new Forecast(1311, 15, "Revenues");
//		Date start = new Date(0);
//		Date end   = new Date(System.currentTimeMillis());
//		Set<Submission> training = forecast.train(start, end, 0.90, 1.0);
//		System.out.println("Successfully trained on " + training.size() + " submissions");

		Session session = HibernateConfig.FACTORY.openSession();
		Submission submission = new Submission();
		submission.setAdsh("0000311471-14-000006");
		submission.setFilerStatus(Submission.FilerStatus.ACCELERATED);
//		System.out.println(forecast.predict(submission));
//		submission.setFilerStatus(Submission.FilerStatus.LARGE_ACCELERATED);
//		System.out.println(forecast.predict(submission));
		
//		Registrant apco = new Registrant();
//		apco.setCik(311471);
//		apco.setSic(1311);
//
//		DCFAnalysis dcf = new DCFAnalysis(1311);
//		System.out.println(dcf.value(apco, 38800000, 32480000, 3370000, 5817000, 72040000, 38020000));
	}
}
