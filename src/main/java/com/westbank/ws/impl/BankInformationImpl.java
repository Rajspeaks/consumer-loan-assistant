package com.westbank.ws.impl;

import com.westbank.db.dao.DataAccess;
import com.westbank.ws.business.bankinformation._2018._06.BankInformation;
import com.westbank.ws.business.bankinformation._2018._06.BankInformationRequest;
import com.westbank.ws.business.bankinformation._2018._06.BankInformationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

@javax.jws.WebService(
		serviceName = "BankInformation",
		portName = "BankInformationPort",
		targetNamespace = "urn:com:westbank:ws:business:BankInformation:2018:06",
		endpointInterface = "com.westbank.ws.business.bankinformation._2018._06.BankInformation")
public class BankInformationImpl implements BankInformation {

	static final Logger log = LoggerFactory.getLogger(BankInformationImpl.class);

	@Autowired
	protected DataAccess dataAccessObject;

	public void setDataAccessObject(DataAccess dataAccessObject) {
		this.dataAccessObject = dataAccessObject;
	}

	/**
	 * 
	 * Calculates the monthly payment according to the amortization formulas at
	 * 
	 * http://www.vertex42.com/ExcelArticles/amortization-calculation.html
	 * 
	 * monthlyPayment = loanAmount * r * (1 + r) ^ n / ( ( 1 + r )^n - 1) =
	 * loanAmount * r / (1 - 1/ (1 + r)^n )
	 * 
	 * where: monthlyPayment = payment amount per month r = interest rate per period
	 * n = loan terms (by years convert to the number of months)
	 * 
	 * 
	 */
	public BankInformationResponse retrieve(BankInformationRequest request) {
		log.info("Executing operation retrieve:" + request);
		try {

			// loan term = loanTerm (years) * 12;
			int loanTerm = request.getLoanTerm() * 12;

			// monthlyInterestRate = annualInterestRate / 12
			double monthlyInterestRate = request.getInterestRate() / 12.0;

			// total loan amount
			double loanAmount = request.getLoanAmount();

			final BankInformationResponse response = new BankInformationResponse();

			double monthlyPayment = loanAmount * monthlyInterestRate / (1 - 1.0 / Math.pow(monthlyInterestRate + 1, loanTerm));

			response.setMonthlyPayment(monthlyPayment);

			log.info(" Response: " + response);

			return response;

		} catch (final Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
	}
}
