package com.westbank.mvc.customer.controller;

import com.westbank.db.dao.DataAccess;
import com.westbank.db.entity.Address;
import com.westbank.db.entity.Customer;
import com.westbank.mvc.Constants;
import com.westbank.mvc.customer.model.ApplicationForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.CustomBooleanEditor;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A controller for populating a customer's profile.
 * 
 */
@Controller
@RequestMapping("/profile.html")
public class ProfileController {

	static Logger log = LoggerFactory.getLogger(ProfileController.class);

	static final String THIS_VIEW = "customer/profile";
	static final String CUSTOMER_LOGIN_VIEW = "redirect:/login.html";

	@Autowired
	protected DataAccess dataAccessObject;

	@Autowired
	ProfileValidator validator;

	@ModelAttribute("applicationForm")
	public ApplicationForm setupApplicationForm(HttpSession session) {
		final Object sessionId = SessionValidator.validateSession(session, Constants.SESSION_CUSTOMER_ID);
		if (sessionId != null) {
			try {
				long customerId = (Long) sessionId;
				log.info("Session ID is valid. Initialize customer data");
				return loadCustomerProfile(customerId);
			} catch (NumberFormatException e) {
				log.error("Session ID is invalid. Abort!");
			}
		} else {
			log.info("Session ID is invalid. Abort!");
		}
		return new ApplicationForm();
	}

	@RequestMapping(method = RequestMethod.GET)
	public String prepare(HttpSession session) {
		final Object sessionId = SessionValidator.validateSession(session, Constants.SESSION_CUSTOMER_ID);
		if (sessionId != null) {
			log.info("Return the profile view");
			session.setAttribute(Constants.SESSION_NAV, Constants.NAV_PROFILE);
			session.removeAttribute(Constants.SESSION_PROCESS_STATUS);
			session.removeAttribute(Constants.SESSION_PROCESS_STATUS_KEY);
			return THIS_VIEW;
		} else {
			log.info("Session ID is invalid. Customer must log-in first");
			session.setAttribute(Constants.SESSION_NAV, Constants.NAV_LOGIN);
			return CUSTOMER_LOGIN_VIEW;
		}
	}

	@InitBinder
	public void initBinder(HttpServletRequest request, ServletRequestDataBinder binder) throws Exception {
		binder.registerCustomEditor(Boolean.class, new CustomBooleanEditor(false));
		final DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		final CustomDateEditor editor = new CustomDateEditor(df, false);
		binder.registerCustomEditor(Date.class, editor);
	}

	@RequestMapping(method = RequestMethod.POST)
	public String processSubmission(ApplicationForm form, BindingResult result, HttpSession session) {

		boolean updatedOK = false;
		final Object sessionId = SessionValidator.validateSession(session, Constants.SESSION_CUSTOMER_ID);
		if (sessionId == null) {
			return CUSTOMER_LOGIN_VIEW;
		} else {
			session.setAttribute(Constants.SESSION_NAV, Constants.NAV_PROFILE);
			session.removeAttribute(Constants.SESSION_PROCESS_STATUS);
			session.removeAttribute(Constants.SESSION_PROCESS_STATUS_KEY);

			if (validator != null) {
				log.info("Start validating customer profile");
				validator.validate(form, result);
				if (!result.hasFieldErrors()) {
					if (dataAccessObject != null) {
						log.info("Update customer's profile");
						Customer customer = dataAccessObject.updateCustomerProfile((Long) sessionId, form);
						if (customer != null) {
							updatedOK = true;
						} else {
							updatedOK = false;
						}
					} else {
						updatedOK = false;
					}
					if (updatedOK) {
						session.setAttribute(Constants.SESSION_PROCESS_STATUS, Constants.PROCESS_STATUS_OK);
						session.setAttribute(Constants.SESSION_PROCESS_STATUS_KEY, Constants.MSG_PROFILE_UPDATE_OK);
					} else {
						session.setAttribute(Constants.SESSION_PROCESS_STATUS, Constants.PROCESS_STATUS_ERROR);
						session.setAttribute(Constants.SESSION_PROCESS_STATUS_KEY, Constants.MSG_DAO_ERR);
					}
				} else { /* some error */
					log.info("Customer profile validation failed");
				}
			} else {
				log.error("Cannot get the autowired profile validator bean");
			}
		}
		return THIS_VIEW;
	}

	protected ApplicationForm loadCustomerProfile(long customerId) {
		final ApplicationForm form = new ApplicationForm();
		if (dataAccessObject != null) {
			Customer customer = dataAccessObject.getCustomerById(customerId);
			if (customer != null) {
				form.setBorrowerTitle(customer.getTitle());
				form.setBorrowerFirstName(customer.getFirstName());
				form.setBorrowerLastName(customer.getLastName());
				form.setBorrowerMaritalStatus(customer.getMaritalStatus());
				form.setBorrowerDateOfBirth(customer.getDateOfBirth());
				form.setBorrowerOccupation(customer.getOccupation());
				form.setBorrowerLengthOfService(customer.getLengthOfService());
				form.setBorrowerIncome(customer.getIncome());
				Address address = customer.getAddress();
				if (address != null) {
					form.setBorrowerStreet(address.getStreet());
					form.setBorrowerCity(address.getCity());
					form.setBorrowerZipcode(address.getZipcode());
					form.setBorrowerState(address.getState());
					form.setBorrowerCountry(address.getCountry());
				}
				form.setBorrowerPhone(customer.getPhone());
				form.setBorrowerMobilePhone(customer.getMobilePhone());
				form.setBorrowerEmail(customer.getEmail());
				form.setBorrowerNumberOfChildren(customer.getNumberOfChildren());
				form.setBorrowerMaritalStatus(customer.getMaritalStatus());
			} else {
				log.error("Cannot retrieve a valid customer object");
			}
		} else {
			log.error("Cannot get the autowired DAO bean");
		}
		return form;
	}
}
