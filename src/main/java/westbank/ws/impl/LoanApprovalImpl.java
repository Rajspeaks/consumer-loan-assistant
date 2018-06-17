package westbank.ws.impl;

import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import westbank.db.dao.DataAccess;
import westbank.db.entity.Address;
import westbank.db.entity.Customer;
import westbank.db.entity.LoanFileStatus;
import westbank.db.entity.Role;
import westbank.util.DateUtil;
import westbank.ws.business.bankinformation._2009._11.BankInformation;
import westbank.ws.business.bankinformation._2009._11.BankInformationRequest;
import westbank.ws.business.bankinformation._2009._11.BankInformationResponse;
import westbank.ws.business.bankprivilege._2009._11.BankPrivilege;
import westbank.ws.business.bankprivilege._2009._11.BankPrivilegeRequest;
import westbank.ws.business.bankprivilege._2009._11.BankPrivilegeResponse;
import westbank.ws.business.creditworthiness._2009._11.CreditWorthiness;
import westbank.ws.business.creditworthiness._2009._11.CreditWorthinessRequest;
import westbank.ws.business.creditworthiness._2009._11.CreditWorthinessResponse;
import westbank.ws.business.loanapprovalclosing._2009._11.LoanApprovalClosing;
import westbank.ws.business.loanapprovalclosing._2009._11.LoanApprovalClosingRequest;
import westbank.ws.business.loanapprovalclosing._2009._11.LoanApprovalClosingResponse;
import westbank.ws.business.loancontract._2009._11.LoanContract;
import westbank.ws.business.loancontract._2009._11.LoanContractRequest;
import westbank.ws.business.loancontract._2009._11.LoanContractResponse;
import westbank.ws.business.loanfile._2009._11.LoanFile;
import westbank.ws.business.loanfile._2009._11.LoanFileRequest;
import westbank.ws.business.loanfile._2009._11.LoanFileResponse;
import westbank.ws.business.loanrisk._2009._11.LoanRisk;
import westbank.ws.business.loanrisk._2009._11.LoanRiskRequest;
import westbank.ws.business.loanrisk._2009._11.LoanRiskResponse;
import westbank.ws.business.loansettlement._2009._11.LoanSettlement;
import westbank.ws.business.loansettlement._2009._11.LoanSettlementRequest;
import westbank.ws.business.loansettlement._2009._11.LoanSettlementResponse;
import westbank.ws.business.taskdispatch._2009._11.TaskDispatch;
import westbank.ws.business.taskdispatch._2009._11.TaskDispatchRequest;
import westbank.ws.business.taskdispatch._2009._11.TaskDispatchResponse;
import westbank.ws.client.callbackloanapproval.CallbackLoanApproval;
import westbank.ws.client.callbackloanapproval.CallbackLoanApprovalRequest;
import westbank.ws.client.callbackloancontract.CallbackLoanContract;
import westbank.ws.client.callbackloancontract.CallbackLoanContractRequest;
import westbank.ws.process.loanapproval._2009._11.AddressType;
import westbank.ws.process.loanapproval._2009._11.CustomerDecision;
import westbank.ws.process.loanapproval._2009._11.LoanApproval;
import westbank.ws.process.loanapproval._2009._11.LoanApprovalRequest;
import westbank.ws.process.loanapproval._2009._11.ManagerDecision;
import westbank.ws.process.loanapproval._2009._11.ManagerSignature;
import westbank.ws.process.loanapproval._2009._11.StaffIdentity;

@javax.jws.WebService(serviceName = "LoanApproval", portName = "LoanApprovalPort", targetNamespace = "urn:westbank:ws:process:LoanApproval:2009:11", endpointInterface = "westbank.ws.process.loanapproval._2009._11.LoanApproval")
public class LoanApprovalImpl implements LoanApproval {

	static final Logger log = LoggerFactory.getLogger(LoanApprovalImpl.class);

	static final String WSDL = "?wsdl";
	static final String BankInformation = "BankInformation";
	static final String BankPrivilege = "BankPrivilege";
	static final String CreditWorthiness = "CreditWorthiness";
	static final String LoanApprovalClosing = "LoanApprovalClosing";
	static final String LoanFile = "LoanFile";
	static final String LoanContract = "LoanContract";
	static final String LoanRisk = "LoanRisk";
	static final String LoanSettlement = "LoanSettlement";
	static final String TaskDispatch = "TaskDispatch";
	static final String CallbackLoanApproval = "CallbackLoanApproval";
	static final String CallbackLoanContract = "CallbackLoanContract";

	String endpointBase;

	BankInformation bankInformation;
	BankPrivilege bankPrivilege;
	CreditWorthiness creditWorthiness;
	LoanApprovalClosing loanApprovalClosing;
	LoanFile loanFile;
	LoanContract loanContract;
	LoanRisk loanRisk;
	LoanSettlement loanSettlement;
	TaskDispatch taskDispatch;
	CallbackLoanApproval callbackLoanApproval;
	CallbackLoanContract callbackLoanContract;

	DataAccess dataAccessObject;

	@Override
	public void start(LoanApprovalRequest request) {
		log.info("Received a loan request: " + request);
		try {
			createLoanFile(request);
		} catch (final Exception ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public void processedByStaff(StaffIdentity request) {
		log.info("Received a staff identity:" + request);
		if (request != null) {

			String staffId = request.getStaffId();
			String staffRole = request.getStaffRole();
			String loanFileId = request.getLoanFileId();

			if (Role.CREDIT_BROKER.equals(staffRole)) {
				try {
					invokeBankPrivilege(loanFileId, staffId, staffRole);

					requestBankInformation(loanFileId, staffId, staffRole);

					dispatchTask(loanFileId, staffId, staffRole);

				} catch (final Exception e) {
					e.printStackTrace();
				}

			} else if (Role.POST_PROCESSING_CLERK.equals(staffRole) || Role.SUPERVISOR.equals(staffRole)) {
				try {

					checkCreditWorthiness(loanFileId, staffId, staffRole);
					LoanRiskResponse risk = evaluateLoanRisk(loanFileId, staffId, staffRole);
					if (risk != null && !risk.isHighRisk()) {
						createLoanContract(loanFileId, staffId, staffRole);
					}
				} catch (final Exception e) {
					e.printStackTrace();
				}
			} else {
				log.info("Role '" + staffRole + "' has not been supported at this point");
			}
		}
	}

	@Override
	public void decidedByManager(ManagerDecision request) {
		log.info("Received the manager's decision:" + request);
		if (request != null) {
			String loanFileId = request.getLoanFileId();
			String staffId = request.getStaffId();
			String staffRole = Role.MANAGER;
			// the high-risk loan is granted ...
			if (request.isGranted()) {
				try {
					createLoanContract(loanFileId, staffId, staffRole);
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else { /* ... or rejected */
				try {
					notifyCustomer(loanFileId, null);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void informedByCustomer(CustomerDecision request) {
		log.info("Received the customer's signature:" + request);
		try {
			String contractId = request.getContractId();
			if (contractId != null) {
				if (dataAccessObject != null) {
					westbank.db.entity.Contract contract = dataAccessObject.getContractById(contractId);
					String loanFileId = contract.getLoanFile().getLoanFileId();
					performLoanSettlement(contractId);
					closeLoanApproval(loanFileId, contractId);
					notifyCustomer(loanFileId, contractId);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();

		}
	}

	@Override
	public void signedByManager(ManagerSignature request) {
		log.info("Received the manager's signature:" + request);
		try {
			String contractId = request.getContractId();
			String staffId = request.getStaffId();
			String staffRole = Role.MANAGER;
			if (contractId != null) {
				if (dataAccessObject != null) {
					sendLoanContract(contractId, staffId, staffRole);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();

		}
	}

	// CreateLoanFile
	protected LoanFileResponse createLoanFile(LoanApprovalRequest loanRequest) throws Exception {

		final LoanFileRequest loanFileRequest = new LoanFileRequest();

		Long borrowerCustomerId = loanRequest.getBorrowerCustomerId();
		Customer borrower = null;
		if (borrowerCustomerId != null) {
			borrower = dataAccessObject.getCustomerById(borrowerCustomerId);
		}
		if (borrower != null) {
			loanFileRequest.setBorrowerCustomerId(borrowerCustomerId);
			loanFileRequest.setBorrowerTitle(borrower.getTitle());
			loanFileRequest.setBorrowerFirstName(borrower.getFirstName());
			loanFileRequest.setBorrowerLastName(borrower.getLastName());
			loanFileRequest.setBorrowerPersonalId(borrower.getPersonalId());
			loanFileRequest.setBorrowerDateOfBirth(DateUtil.convert(borrower.getDateOfBirth()));
			final Address address = borrower.getAddress();
			if (address != null) {
				loanFileRequest.setBorrowerStreet(address.getStreet());
				loanFileRequest.setBorrowerCity(address.getCity());
				loanFileRequest.setBorrowerZipcode(address.getZipcode());
				loanFileRequest.setBorrowerState(address.getState());
				loanFileRequest.setBorrowerCountry(address.getCountry());
			}
			loanFileRequest.setBorrowerPhone(borrower.getPhone());
			loanFileRequest.setBorrowerMobilePhone(borrower.getMobilePhone());
			loanFileRequest.setBorrowerEmail(borrower.getEmail());
			loanFileRequest.setBorrowerOccupation(borrower.getOccupation());
			loanFileRequest.setBorrowerLengthOfService(borrower.getLengthOfService());
			loanFileRequest.setBorrowerIncome(borrower.getIncome());
			if (borrower.getMaritalStatus() != null)
				loanFileRequest.setBorrowerMaritalStatus(borrower.getMaritalStatus().name());
			loanFileRequest.setBorrowerNumberOfChildren(borrower.getNumberOfChildren());

		} else {
			loanFileRequest.setBorrowerTitle(loanRequest.getBorrowerTitle());
			loanFileRequest.setBorrowerFirstName(loanRequest.getBorrowerFirstName());
			loanFileRequest.setBorrowerLastName(loanRequest.getBorrowerLastName());
			loanFileRequest.setBorrowerPersonalId(loanRequest.getBorrowerPersonalId());
			loanFileRequest.setBorrowerDateOfBirth(loanRequest.getBorrowerDateOfBirth());
			final AddressType address = loanRequest.getBorrowerAddress();
			if (address != null) {
				loanFileRequest.setBorrowerStreet(address.getStreet());
				loanFileRequest.setBorrowerCity(address.getCity());
				loanFileRequest.setBorrowerZipcode(address.getZipcode());
				loanFileRequest.setBorrowerState(address.getState());
				loanFileRequest.setBorrowerCountry(address.getCountry());
			}
			loanFileRequest.setBorrowerPhone(loanRequest.getBorrowerPhone());
			loanFileRequest.setBorrowerMobilePhone(loanRequest.getBorrowerMobilePhone());
			loanFileRequest.setBorrowerEmail(loanRequest.getBorrowerEmail());
			loanFileRequest.setBorrowerOccupation(loanRequest.getBorrowerOccupation());
			loanFileRequest.setBorrowerLengthOfService(loanRequest.getBorrowerLengthOfService());
			loanFileRequest.setBorrowerIncome(loanRequest.getBorrowerIncome());
			loanFileRequest.setBorrowerMaritalStatus(loanRequest.getBorrowerMaritalStatus());
			loanFileRequest.setBorrowerNumberOfChildren(loanRequest.getBorrowerNumberOfChildren());

			// loan information
			loanFileRequest.setResidenceType(loanRequest.getResidenceType());
			loanFileRequest.setEstateType(loanRequest.getEstateType());
			loanFileRequest.setEstateLocation(loanRequest.getEstateLocation());
		}
		// is there any co-borrower?
		loanFileRequest.setCoBorrower(loanRequest.isCoBorrower());
		if (loanRequest.isCoBorrower()) {

			Long coborrowerCustomerId = loanRequest.getCoBorrowerCustomerId();
			Customer coborrower = null;
			if (coborrowerCustomerId != null) {
				coborrower = dataAccessObject.getCustomerById(coborrowerCustomerId);
				log.info("Co-borrower existed!");
			}
			if (coborrower != null) {
				loanFileRequest.setCoBorrowerCustomerId(coborrowerCustomerId);
				loanFileRequest.setCoBorrowerTitle(coborrower.getTitle());
				loanFileRequest.setCoBorrowerFirstName(coborrower.getFirstName());
				loanFileRequest.setCoBorrowerLastName(coborrower.getLastName());
				loanFileRequest.setCoBorrowerDateOfBirth(DateUtil.convert(coborrower.getDateOfBirth()));
				loanFileRequest.setCoBorrowerOccupation(coborrower.getOccupation());
				loanFileRequest.setCoBorrowerLengthOfService(coborrower.getLengthOfService());
				loanFileRequest.setCoBorrowerIncome(coborrower.getIncome());
				loanFileRequest.setCoBorrowerEmail(coborrower.getEmail());
			} else {
				loanFileRequest.setCoBorrowerTitle(loanRequest.getCoBorrowerTitle());
				loanFileRequest.setCoBorrowerFirstName(loanRequest.getCoBorrowerFirstName());
				loanFileRequest.setCoBorrowerLastName(loanRequest.getCoBorrowerLastName());
				loanFileRequest.setCoBorrowerDateOfBirth(loanRequest.getCoBorrowerDateOfBirth());
				loanFileRequest.setCoBorrowerOccupation(loanRequest.getCoBorrowerOccupation());
				loanFileRequest.setCoBorrowerLengthOfService(loanRequest.getCoBorrowerLengthOfService());
				loanFileRequest.setCoBorrowerIncome(loanRequest.getCoBorrowerIncome());
				loanFileRequest.setCoBorrowerEmail(loanRequest.getCoBorrowerEmail());
			}
		}
		// loan info
		loanFileRequest.setResidenceType(loanRequest.getResidenceType());
		loanFileRequest.setEstateType(loanRequest.getEstateType());
		loanFileRequest.setEstateLocation(loanRequest.getEstateLocation());
		loanFileRequest.setLoanReason(loanRequest.getLoanReason());
		loanFileRequest.setLoanAmount(loanRequest.getLoanAmount());
		loanFileRequest.setLoanTerm(loanRequest.getLoanTerm());
		loanFileRequest.setInterestRate(loanRequest.getInterestRate());
		loanFileRequest.setTotalPurchasePrice(loanRequest.getTotalPurchasePrice());
		loanFileRequest.setPersonalCapitalContribution(loanRequest.getPersonalCapitalContribution());
		loanFileRequest.setSettlementDate(loanRequest.getSettlementDate());

		loanFileRequest.setAccessSensitiveData(loanRequest.isAccessSensitiveData());
		log.info("Access Sensitive Data Authorized? : " + loanRequest.isAccessSensitiveData());
		LoanFileResponse response = null;
		try {
			response = loanFile.update(loanFileRequest);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return response;
	}

	protected BankPrivilegeResponse invokeBankPrivilege(String loanFileId, String staffId, String staffRole)
			throws Exception {
		BankPrivilegeResponse response = null;

		final BankPrivilegeRequest request = new BankPrivilegeRequest();

		if (dataAccessObject != null) {
			westbank.db.entity.LoanFile loanFile = dataAccessObject.getLoanFileById(loanFileId);
			if (loanFile != null) {
				request.setBorrowerCustomerId(loanFile.getBorrower().getCustomerId());
				request.setBorrowerFirstName(loanFile.getBorrower().getFirstName());
				request.setBorrowerLastName(loanFile.getBorrower().getLastName());
				request.setBorrowerDateOfBirth(DateUtil.convert(loanFile.getBorrower().getDateOfBirth()));
				request.setStaffId(staffId);
				request.setStaffRole(staffRole);
				response = bankPrivilege.check(request);
			}
		}
		return response;
	}

	protected BankInformationResponse requestBankInformation(String loanFileId, String staffId, String staffRole)
			throws Exception {

		final BankInformationRequest request = new BankInformationRequest();

		BankInformationResponse response = null;

		if (dataAccessObject != null) {
			westbank.db.entity.LoanFile loanFile = dataAccessObject.getLoanFileById(loanFileId);
			if (loanFile != null) {
				request.setLoanAmount(loanFile.getLoanAmount());
				request.setLoanTerm(loanFile.getLoanTerm());
				request.setInterestRate(loanFile.getInterestRate());
				request.setStaffId(staffId);
				request.setStaffRole(staffRole);

				response = bankInformation.retrieve(request);
			}
		}
		return response;
	}

	protected TaskDispatchResponse dispatchTask(String loanFileId, String staffId, String staffRole) throws Exception {

		final TaskDispatchRequest request = new TaskDispatchRequest();

		TaskDispatchResponse response = null;

		if (dataAccessObject != null) {
			westbank.db.entity.LoanFile loanFile = dataAccessObject.getLoanFileById(loanFileId);
			if (loanFile != null) {
				request.setLoanAmount(loanFile.getLoanAmount());
				request.setStaffId(staffId);
				request.setStaffRole(staffRole);
				response = taskDispatch.dispatch(request);
			}
		}
		return response;
	}

	protected CreditWorthinessResponse checkCreditWorthiness(String loanFileId, String staffId, String staffRole)
			throws Exception {

		final CreditWorthinessRequest request = new CreditWorthinessRequest();

		CreditWorthinessResponse response = null;

		if (loanFileId != null) {
			request.setLoanFileId(loanFileId);
			request.setNumberOfIncidents(new Random().nextInt());
			request.setNumberOfBanks(new Random().nextInt());
			request.setStaffId(staffId);
			request.setStaffRole(staffRole);
			response = creditWorthiness.check(request);
		}
		return response;
	}

	protected LoanRiskResponse evaluateLoanRisk(String loanFileId, String staffId, String staffRole) throws Exception {

		final LoanRiskRequest request = new LoanRiskRequest();
		if (loanFileId != null) {
			request.setLoanFileId(loanFileId);
			request.setStaffId(staffId);
			request.setStaffRole(staffRole);
		}
		return loanRisk.evaluate(request);
	}

	protected LoanContractResponse createLoanContract(String loanFileId, String staffId, String staffRole)
			throws Exception {

		final LoanContractRequest request = new LoanContractRequest();
		if (loanFileId != null) {
			request.setLoanFileId(loanFileId);
			request.setStaffId(staffId);
			request.setStaffRole(staffRole);
		}
		return loanContract.create(request);
	}

	protected LoanApprovalClosingResponse closeLoanApproval(String loanFileId, String contractId) throws Exception {

		final LoanApprovalClosingRequest request = new LoanApprovalClosingRequest();
		if (loanFileId != null) {
			request.setLoanFileId(loanFileId);
		}
		if (contractId != null) {
			request.setLoanContractId(contractId);
		}
		return loanApprovalClosing.close(request);
	}

	protected LoanSettlementResponse performLoanSettlement(String contractId) throws Exception {

		final LoanSettlementRequest request = new LoanSettlementRequest();
		if (contractId != null) {
			request.setLoanContractId(contractId);
		}
		return loanSettlement.start(request);
	}

	protected void sendLoanContract(String contractId, String staffId, String staffRole) throws Exception {

		final CallbackLoanContractRequest request = new CallbackLoanContractRequest();
		if (contractId != null) {
			request.setLoanContractId(contractId);
		}
		callbackLoanContract.send(request);
	}

	protected void notifyCustomer(String loanFileId, String contractId) throws Exception {

		final CallbackLoanApprovalRequest request = new CallbackLoanApprovalRequest();
		if (contractId != null) {
			request.setContractId(contractId);
		}
		if (dataAccessObject != null) {
			westbank.db.entity.LoanFile loanFile = dataAccessObject.getLoanFileById(loanFileId);
			if (loanFile != null) {
				request.setBorrowerCustomerId(loanFile.getBorrower().getCustomerId());
				request.setStatus(LoanFileStatus.APPROVED.name());
				request.setDescription(loanFile.getStatus().name());
			}
		}
		callbackLoanApproval.notify(request);
	}

	public void setDataAccessObject(DataAccess dataAccessObject) {
		this.dataAccessObject = dataAccessObject;
	}

	public void setBankInformation(BankInformation bankInformation) {
		this.bankInformation = bankInformation;
	}

	public void setBankPrivilege(BankPrivilege bankPrivilege) {
		this.bankPrivilege = bankPrivilege;
	}

	public void setCreditWorthiness(CreditWorthiness creditWorthiness) {
		this.creditWorthiness = creditWorthiness;
	}

	public void setLoanApprovalClosing(LoanApprovalClosing loanApprovalClosing) {
		this.loanApprovalClosing = loanApprovalClosing;
	}

	public void setLoanFile(LoanFile loanFile) {
		this.loanFile = loanFile;
	}

	public void setLoanContract(LoanContract loanContract) {
		this.loanContract = loanContract;
	}

	public void setLoanRisk(LoanRisk loanRisk) {
		this.loanRisk = loanRisk;
	}

	public void setLoanSettlement(LoanSettlement loanSettlement) {
		this.loanSettlement = loanSettlement;
	}

	public void setTaskDispatch(TaskDispatch taskDispatch) {
		this.taskDispatch = taskDispatch;
	}

	public void setCallbackLoanApproval(CallbackLoanApproval callbackLoanApproval) {
		this.callbackLoanApproval = callbackLoanApproval;
	}

	public void setCallbackLoanContract(CallbackLoanContract callbackLoanContract) {
		this.callbackLoanContract = callbackLoanContract;
	}

	public void setEndpointBase(String endpointBase) {
		this.endpointBase = endpointBase;
	}

}
