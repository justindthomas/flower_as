package name.justinthomas.flower.analysis.services;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.servlet.http.HttpServletRequest;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;
import name.justinthomas.flower.analysis.element.Flow;
import name.justinthomas.flower.analysis.persistence.FlowReceiver;
import name.justinthomas.flower.analysis.persistence.PersistentFlow;
import name.justinthomas.flower.analysis.statistics.StatisticsManager;
import name.justinthomas.flower.global.GlobalConfigurationManager;
import name.justinthomas.flower.manager.services.Customer;
import name.justinthomas.flower.manager.services.Customer.Collectors;
import name.justinthomas.flower.manager.services.CustomerAdministration;
import name.justinthomas.flower.manager.services.CustomerAdministrationService;

/**
 *
 * @author justin
 */
@WebService()
public class FlowInsert {

    @Resource
    WebServiceContext context;
    @EJB
    GlobalConfigurationManager globalConfigurationManager;

    @WebMethod(operationName = "addFlows")
    public Integer addFlows(
            @WebParam(name = "customerID") String customerID,
            @WebParam(name = "flows") List<PersistentFlow> flowSet) {

        MessageContext messageContext = context.getMessageContext();
        HttpServletRequest request = (HttpServletRequest) messageContext.get(MessageContext.SERVLET_REQUEST);

        String address = request.getRemoteAddr();
        System.out.println("Request received from: " + address);

        Customer customer = null;
        
        try {
            CustomerAdministrationService admin = new CustomerAdministrationService(new URL(globalConfigurationManager.getManager() + "/CustomerAdministrationService?wsdl"));

            CustomerAdministration port = admin.getCustomerAdministrationPort();
            customer = port.getCustomer(null, null, customerID);
        } catch (MalformedURLException e) {
            System.err.println("Could not access Customer Administration service.");
            return 1;
        }

        Boolean found = false;
        for (Collectors.Entry entry : customer.getCollectors().getEntry()) {
            if (entry.getKey().equals(address)) {
                found = true;
                break;
            }
        }

        if (found && customer != null) {
            try {
                InetAddress collector = InetAddress.getByName(address);
                Thread thread = new Thread(new InsertThread(flowSet, collector));
                thread.start();
            } catch (UnknownHostException e) {
                System.err.println("Could not resolve remote address.");
            }
        } else {
            return 1;
        }

        return 0;
    }

    class InsertThread implements Runnable {

        InetAddress collector;
        List<PersistentFlow> flowSet;
        HashMap<Long, Flow> flows = new HashMap();

        public InsertThread(List<PersistentFlow> flowSet, InetAddress collector) {
            this.flowSet = flowSet;
            this.collector = collector;
        }

        @Override
        public void run() {
            System.out.println("Beginning to store " + flowSet.size() + " flows...");
            LinkedList<Flow> converted = new LinkedList();

            for (PersistentFlow xflow : flowSet) {
                try {
                    if (xflow.size.longValue() < 0) {
                        throw new Exception("Negative bytesSent value (" + xflow.size.longValue() + ") in XMLFlow received.");
                    }
                    converted.add(new Flow(xflow));
                    //Flow flow = new Flow(xflow);

                    ;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            FlowReceiver receiver = new FlowReceiver();
            LinkedList<Long> flowIDs = receiver.addFlows(converted, null);

            Iterator<Long> idIterator = flowIDs.iterator();
            Iterator<Flow> flowIterator = converted.iterator();

            while (idIterator.hasNext()) {
                flows.put(idIterator.next(), flowIterator.next());
            }

            System.out.println("Completed storing and beginning to process statistics for " + flows.size() + " flows.");

            for (Entry<Long, Flow> flow : flows.entrySet()) {
                StatisticsManager statisticsManager = new StatisticsManager();
                statisticsManager.addStatisticalSeconds(flow.getValue(), flow.getKey(), collector);
            }

            //System.out.println("Completed processing statistics for " + flows.size() + " flows.");
        }
    }
}
