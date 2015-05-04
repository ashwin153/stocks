package com.ashwin.fri.stocks;

import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import com.ashwin.fri.stocks.forecast.Forecast;
import com.ashwin.fri.stocks.hibernate.HibernateConfig;
import com.ashwin.fri.stocks.hibernate.Registrant;
import com.ashwin.fri.stocks.hibernate.Submission;

/**
 * This class combines various forecasting tools to perform discounted
 * cash flow (DCF) analysis on a submission. This analysis produces the
 * intrinsic "fair value" of the company, which can be further processed
 * to determine the optimal share price for a company.
 * 
 * @author ashwin
 *
 */
public class DCFAnalysis {
	
	private Forecast _fcf;
	
	public DCFAnalysis(int sic) throws Exception {
		_fcf = new Forecast(sic, 15, "Revenues", "CostsAndExpenses", 
				"TaxesOther", "InvestmentIncomeNonOperating",
				"AssetsCurrent", "LiabilitiesCurrent");
		
		Date start = new Date(0);
		Date end   = new Date(System.currentTimeMillis());
		_fcf.train(start, end, 0.70, 1.2);
	}
	
	/**
	 * Performs a valuation of the specified submission. The function attempts
	 * to determine the present value by using the forecasting engines to predict
	 * growth of the components of a DCF.
	 * 
	 * @param submission
	 * @return
	 */
	public double value(Registrant registrant, double revenue, double costs, double taxes,
			double netInvestments, double assets, double liabilities) {
		
		Session session = HibernateConfig.FACTORY.openSession();
		Transaction tx  = session.beginTransaction();
		
		Submission submission = (Submission) session.createCriteria(Submission.class)
				.add(Restrictions.eq("registrant", registrant))
				.addOrder(Order.desc("filingDate"))
				.setMaxResults(1)
				.uniqueResult();
		
		tx.rollback();
		session.close();
		
		// Step 1: Determine Free Cash Flow (FCF).
		// FCF = Revenue - Operating Costs - Taxes - Net Investments - Net Change in Working Capital
		List<Double> growths = _fcf.predict(submission);
		double cRevenue 	   = growths.get(0) * revenue;
		double cCosts 		   = growths.get(1) * costs;
		double cTaxes 		   = growths.get(2) * taxes;
		double cNetInvestments = growths.get(3) * netInvestments;
		double cAssets 		   = growths.get(4) * assets;
		double cLiabilities    = growths.get(5) * liabilities;
		
		System.out.println("Predicted Revenue: " + cRevenue);
		System.out.println("Predicted Costs: " + cCosts);
		System.out.println("Predicted Taxes: " + cTaxes);
		System.out.println("Predicted Investments: " + cNetInvestments);
		System.out.println("Predicted Assets: " + cAssets);
		System.out.println("Predicted Liabilities: " + cLiabilities);
		
		double fcf = cRevenue - cCosts - cTaxes - cNetInvestments - 
				((cAssets - cLiabilities) - (assets - liabilities));
		
		return fcf;
		
		// Step 2: Determine the Weighted Average Cost of Capital (WACC)
		// The WACC is the weighted average of the cost of equity and the cost of debt
		// Cost of Equity = CAPM = Risk Free Rate + Beta * Equity Market Risk Premium
		// Cost of Debt   = Rate that the company is paying on its debt after tax
		// WACC = Cost of Equity * Total Equity / Total Value + Cost of Debt * Debt / Total Value,
		// where total value = total equity + total debt.
		
		// Step 3: Calculate the Terminal Value
		// Gordon Growth Model: Terminal Value = Final Projected Year Cash Flow * Long Term Cash Flow Growth Rate
	}
}
