package westbank.ws.business.loansettlement._2009._11;

import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceFeature;
import javax.xml.ws.Service;

/**
 * This class was generated by Apache CXF 3.2.4
 * 2018-06-16T23:03:40.732+10:00
 * Generated source version: 3.2.4
 *
 */
@WebServiceClient(name = "LoanSettlement",
                  targetNamespace = "urn:westbank:ws:business:LoanSettlement:2009:11")
public class LoanSettlement_Service extends Service {

    public final static URL WSDL_LOCATION;

    public final static QName SERVICE = new QName("urn:westbank:ws:business:LoanSettlement:2009:11", "LoanSettlement");
    public final static QName LoanSettlementPort = new QName("urn:westbank:ws:business:LoanSettlement:2009:11", "LoanSettlementPort");
    static {
        WSDL_LOCATION = null;
    }

    public LoanSettlement_Service(URL wsdlLocation) {
        super(wsdlLocation, SERVICE);
    }

    public LoanSettlement_Service(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public LoanSettlement_Service() {
        super(WSDL_LOCATION, SERVICE);
    }

    public LoanSettlement_Service(WebServiceFeature ... features) {
        super(WSDL_LOCATION, SERVICE, features);
    }

    public LoanSettlement_Service(URL wsdlLocation, WebServiceFeature ... features) {
        super(wsdlLocation, SERVICE, features);
    }

    public LoanSettlement_Service(URL wsdlLocation, QName serviceName, WebServiceFeature ... features) {
        super(wsdlLocation, serviceName, features);
    }




    /**
     *
     * @return
     *     returns LoanSettlement
     */
    @WebEndpoint(name = "LoanSettlementPort")
    public LoanSettlement getLoanSettlementPort() {
        return super.getPort(LoanSettlementPort, LoanSettlement.class);
    }

    /**
     *
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns LoanSettlement
     */
    @WebEndpoint(name = "LoanSettlementPort")
    public LoanSettlement getLoanSettlementPort(WebServiceFeature... features) {
        return super.getPort(LoanSettlementPort, LoanSettlement.class, features);
    }

}
