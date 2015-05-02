package com.ashwin.fri.stocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import com.ashwin.fri.stocks.forecast.Forecast;
import com.ashwin.fri.stocks.hibernate.HibernateConfig;
import com.ashwin.fri.stocks.hibernate.Submission;
import com.ashwin.fri.stocks.hibernate.Submission.FiscalPeriod;
import com.ashwin.fri.stocks.hibernate.Tag;

public class ForecastTester {
	
	public static void main(String[] args) throws Exception {
		List<Tag> outputs = new ArrayList<Tag>(Arrays.asList(
				getTagByName("Revenues"),
				getTagByName("OperatingExpenses"),
				getTagByName("OperatingIncomeLoss")
		));
		
		Forecast forecast = new Forecast(1311, 15, outputs);
		List<Submission> training = new ArrayList<Submission>();
		for(int year = 2014; year < 2015; year++) {
				training.addAll(forecast.train(year, FiscalPeriod.Q1, 1.0));
				training.addAll(forecast.train(year, FiscalPeriod.Q2, 1.0));
				training.addAll(forecast.train(year, FiscalPeriod.Q3, 1.0));
				training.addAll(forecast.train(year, FiscalPeriod.FY, 1.0));
		}
				
		System.out.println("Successfully trained on " + training.size() + " submissions");
	}
	
	public static Tag getTagByName(String name) {
		Session session = HibernateConfig.FACTORY.openSession();
		Transaction tx  = session.beginTransaction();
		
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
		
		tx.rollback();
		session.close();
		
		return new Tag((String) arr[0], null, false, false, (String) arr[1],
				(String) arr[2], (String) arr[3], (String) arr[4], (String) arr[5], null);
	}
}
