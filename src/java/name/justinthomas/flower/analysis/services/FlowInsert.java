package name.justinthomas.flower.analysis.services;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import javax.annotation.Resource;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.UserTransaction;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;
import name.justinthomas.flower.analysis.authentication.UserAction;
import name.justinthomas.flower.analysis.element.Flow;
import name.justinthomas.flower.analysis.persistence.FlowManager;
import name.justinthomas.flower.analysis.persistence.PersistentFlow;
import name.justinthomas.flower.analysis.statistics.StatisticsManager;
import name.justinthomas.flower.manager.services.CustomerAdministration.Customer;
import name.justinthomas.flower.manager.services.CustomerAdministration.Customer.Collectors;

/**
 *
 * @author justin
 */
@WebService()
public class FlowInsert {

    @PersistenceContext
    EntityManager em;
    @Resource
    private UserTransaction utx;
    @Resource
    WebServiceContext context;

    @WebMethod(operationName = "rebuildStatistics")
    public Boolean rebuildStatistics(
            @WebParam(name = "customerID") String customerID,
            @WebParam(name = "username") String username,
            @WebParam(name = "password") String password) {

        UserAction userAction = new UserAction();

        if (!userAction.authenticate(customerID, username, password).administrator) {
            return (null);
        }

        MessageContext messageContext = context.getMessageContext();
        HttpServletRequest request = (HttpServletRequest) messageContext.get(MessageContext.SERVLET_REQUEST);

        String address = request.getRemoteAddr();
        System.out.println("Rebuild request received from: " + address);

        Customer customer = Utility.getCustomer(customerID);

        Thread thread = new Thread(new RebuildThread(customer, address));
        thread.start();

        return true;
    }

    @WebMethod(operationName = "addFlows")
    public Integer addFlows(
            @WebParam(name = "customerID") String customerID,
            @WebParam(name = "flows") List<PersistentFlow> flowSet) {

        MessageContext messageContext = context.getMessageContext();
        HttpServletRequest request = (HttpServletRequest) messageContext.get(MessageContext.SERVLET_REQUEST);

        String address = request.getRemoteAddr();
        System.out.println("Request received from: " + address);

        Customer customer = Utility.getCustomer(customerID);

        if (customer != null) {
            Boolean found = false;
            for (Collectors.Entry entry : customer.getCollectors().getEntry()) {
                if (entry.getKey().equals(address)) {
                    found = true;
                    break;
                }
            }

            if (found) {
                try {
                    InetAddress collector = InetAddress.getByName(address);
                    Thread thread = new Thread(new InsertThread(customer, flowSet, collector));
                    thread.start();
                } catch (UnknownHostException e) {
                    System.err.println("Could not resolve remote address: " + address);
                    return 1;
                }
            } else {
                return 1;
            }
        } else {
            System.err.println("Could not locate customer for request from: " + address);
            return 1;
        }

        return 0;
    }

    class RebuildThread implements Runnable {

        Customer customer;
        String collector;

        public RebuildThread(Customer customer, String collector) {
            this.customer = customer;
            this.collector = collector;
        }

        @Override
        public void run() {
            FlowManager flowManager = new FlowManager(customer);
            Long end = 0l;

            while (end != null) {
                end = flowManager.rebuildStatistics(collector, end);

                try {
                    flowManager = new FlowManager(customer);
                    System.gc();
                    Thread.sleep(60000);
                } catch (InterruptedException ie) {
                    System.err.println("RebuildThread interrupted.");
                    return;
                }
            }
        }
    }

    class InsertThread implements Runnable {

        Customer customer;
        InetAddress collector;
        List<PersistentFlow> flowSet;
        HashMap<Long, Flow> flows = new HashMap();

        public InsertThread(Customer customer, List<PersistentFlow> flowSet, InetAddress collector) {
            System.out.println("Creating InsertThread...");
            this.flowSet = flowSet;
            this.collector = collector;
            this.customer = customer;
        }

        @Override
        public void run() {
            System.out.println("Beginning to store " + flowSet.size() + " flows...");
            LinkedList<Flow> converted = new LinkedList();

            for (PersistentFlow xflow : flowSet) {
                try {
                    if (xflow.getByteSize().longValue() < 0) {
                        throw new Exception("Negative bytesSent value (" + xflow.getByteSize().longValue() + ") in XMLFlow received.");
                    }

                    xflow.setReportedBy(this.collector.getHostAddress());

                    converted.add(new Flow(customer, xflow));
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                    return;
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
            }

            FlowManager receiver = new FlowManager(customer);
            LinkedList<Long> flowIDs = receiver.addFlows(collector.getHostAddress(), converted, null);

            Iterator<Long> idIterator = flowIDs.iterator();
            Iterator<Flow> flowIterator = converted.iterator();

            while (idIterator.hasNext()) {
                flows.put(idIterator.next(), flowIterator.next());
            }

            System.out.println("Completed storing and beginning to process statistics for " + flows.size() + " flows.");

            for (Entry<Long, Flow> flow : flows.entrySet()) {
                StatisticsManager statisticsManager = new StatisticsManager(customer);
                statisticsManager.addStatisticalSeconds(flow.getValue(), flow.getKey(), collector);
            }

            System.out.println("Completed processing statistics for " + flows.size() + " flows.");
        }
    }
}
