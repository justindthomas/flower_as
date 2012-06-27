package name.justinthomas.flower.analysis.services;

import java.util.concurrent.Callable;
import name.justinthomas.flower.analysis.authentication.UserAction;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import javax.xml.ws.WebServiceContext;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.MessageContext;
import name.justinthomas.flower.analysis.authentication.AuthenticationToken;
import name.justinthomas.flower.analysis.persistence.FlowManager;
import name.justinthomas.flower.analysis.services.xmlobjects.XMLNetwork;
import name.justinthomas.flower.analysis.services.xmlobjects.XMLNetworkList;
import name.justinthomas.flower.analysis.persistence.Constraints;
import name.justinthomas.flower.analysis.persistence.ManagedNetwork;
import name.justinthomas.flower.analysis.persistence.ManagedNetworkManager;
import name.justinthomas.flower.analysis.persistence.PersistentFlow;
import name.justinthomas.flower.analysis.persistence.SessionManager;
import name.justinthomas.flower.analysis.persistence.ThreadManager;
import name.justinthomas.flower.analysis.persistence.TimedThread;
import name.justinthomas.flower.analysis.services.xmlobjects.XMLDataVolumeList;
import name.justinthomas.flower.analysis.services.xmlobjects.XMLNode;
import name.justinthomas.flower.analysis.statistics.StatisticalInterval;
import name.justinthomas.flower.analysis.statistics.StatisticsManager;
import name.justinthomas.flower.manager.services.CustomerAdministration.Customer;
import name.justinthomas.flower.manager.services.CustomerAdministration.CustomerAdministration;
import name.justinthomas.flower.manager.services.CustomerAdministration.CustomerAdministrationService;
import org.bouncycastle.util.encoders.Base64;

/**
 *
 * @author justin
 */
@WebService()
public class ChartData {

    @EJB
    ThreadManager threadManager;
    @Resource
    private WebServiceContext serviceContext;

    /**
     * Web service operation
     */
    private void createTraceFile(XMLNetworkList xmlNetworkList) {
        try {
            FileWriter writer = new FileWriter("/traces/xmlnetworklist.txt", false);
            writer.append("Date: " + new Date() + "\n");
            for (XMLNetwork network : xmlNetworkList.networks) {
                writer.append("Monitored Network: " + network.address.toString() + " has " + network.nodes.size() + " entries.\n");
                for (XMLNode xnode : network.nodes) {
                    writer.append("n:" + xnode.address + " name: " + xnode.resolvedAddress + "\n");
                }
            }
            writer.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /**
     * Web service operation
     */
    @WebMethod(operationName = "getFlows")
    public FlowSet getFlows(
            @WebParam(name = "customer") String customerID,
            @WebParam(name = "user") String user,
            @WebParam(name = "password") String password,
            @WebParam(name = "constraints") String constraints,
            @WebParam(name = "tracker") String tracker) {

        UserAction userAction = new UserAction();

        if (!userAction.authenticate(customerID, user, password).authorized) {
            return (null);
        }

        MessageContext messageContext = serviceContext.getMessageContext();
        HttpSession session = ((HttpServletRequest) messageContext.get(MessageContext.SERVLET_REQUEST)).getSession();

        if (session == null) {
            throw new WebServiceException("No session in WebServiceContext");
        }

        Customer customer = Utility.getCustomer(customerID);

        if (customer != null) {
            if (tracker == null || tracker.isEmpty()) {
                Integer r = new Random().nextInt();
                byte[] b = new byte[1];
                b[0] = r.byteValue();

                try {
                    MessageDigest md = MessageDigest.getInstance("MD5");
                    md.update(b);
                    tracker = new String(Base64.encode(md.digest()));
                    System.out.println("New Tracker: " + tracker);
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }

                BuildFlowList task = new BuildFlowList(customer, session, constraints, tracker);
                threadManager.start(user, new TimedThread(task));
            } else {
                if(!SessionManager.getFlowMap(session).containsKey(tracker)) {
                    return null;
                }
            }

            FlowSet xmlPacketList = new FlowSet();

            Integer resultCount = 0;
            final Integer MAX_REQUEST_RESULTS = 1000;
            try {
                while (!SessionManager.getFlows(session, tracker).isEmpty() && (resultCount++ < MAX_REQUEST_RESULTS)) {
                    xmlPacketList.flows.add(SessionManager.getFlows(session, tracker).remove(0).toHashTableFlow());
                }

                if (SessionManager.getFlows(session, tracker).isEmpty() && SessionManager.isProcessingPacketsComplete(session, tracker)) {
                    xmlPacketList.finished = true;
                    SessionManager.isProcessingPacketsComplete(session, tracker, null);
                    SessionManager.getFlowMap(session).remove(tracker);
                }
            } catch (NullPointerException e) {
                System.err.println("Could not access flows; this is probably due to a non-existent tracker reference.");
            }

            xmlPacketList.tracker = tracker;
            return xmlPacketList;
        } else {
            System.err.println("Could not identify customer.");
            return null;
        }
    }

    @WebMethod(operationName = "getIntervals")
    public List<StatisticalInterval> getIntervals(
            @WebParam(name = "customer") String customerID,
            @WebParam(name = "user") String user,
            @WebParam(name = "password") String password,
            @WebParam(name = "constraints") String constraints,
            @WebParam(name = "resolution") Integer resolution) {

        UserAction userAction = new UserAction();

        if (!userAction.authenticate(customerID, user, password).authorized) {
            return (null);
        }

        Customer customer = null;

        try {
            CustomerAdministrationService admin = new CustomerAdministrationService(new URL(Utility.getGlobalConfigurationManager().getManager() + "/CustomerAdministrationService?wsdl"));

            CustomerAdministration port = admin.getCustomerAdministrationPort();
            customer = port.getCustomer(null, null, customerID);
        } catch (MalformedURLException e) {
            System.err.println("Could not access Customer Administration service at: " + Utility.getGlobalConfigurationManager().getManager());
            return null;
        }

        StatisticsManager statisticsManager = new StatisticsManager(customer);
        return statisticsManager.getStatisticalIntervals(new Constraints(constraints), resolution);
    }

    /**
     * Web service operation
     */
    @WebMethod(operationName = "getDataVolume")
    public XMLDataVolumeList getDataVolume(
            @WebParam(name = "customer") String customerID,
            @WebParam(name = "user") String user,
            @WebParam(name = "password") String password,
            @WebParam(name = "constraints") String constraints,
            @WebParam(name = "bins") Integer bins,
            @WebParam(name = "tracker") String tracker,
            @WebParam(name = "wait") Boolean wait) {

        UserAction userAction = new UserAction();

        if (!userAction.authenticate(customerID, user, password).authorized) {
            return (null);
        }

        MessageContext messageContext = serviceContext.getMessageContext();
        HttpSession session = ((HttpServletRequest) messageContext.get(MessageContext.SERVLET_REQUEST)).getSession();

        if (session == null) {
            throw new WebServiceException("No session in WebServiceContext");
        }

        Customer customer = Utility.getCustomer(customerID);

        if (customer != null) {
            if (tracker == null) {
                tracker = generateNonce();
                System.out.println("Starting volume build thread: " + constraints + ", " + bins);
                BuildDataVolumeList task = new BuildDataVolumeList(customer, session, constraints, bins, tracker);
                
                try {
                    SessionManager.addVolumeTask(session, task);
                } catch (Exception e) {
                    System.err.println(e.getMessage());
                }

                TimedThread thread = new TimedThread(task);
                threadManager.start(user, thread);

                if (wait) {
                    try {
                        thread.join();
                    } catch (InterruptedException e) {
                        System.err.println("Thread: " + thread.getName() + " was interrupted.");
                    }
                }
            }
            
            BuildDataVolumeList task = SessionManager.getVolumeTask(session, tracker);
            if(task == null) {
                return null;
            }
            
            if(task.call().ready) {
                SessionManager.getVolumeTaskMap(session).remove(tracker);
            }
            
            return task.call();  // The "ready" Boolean in XMLDVL is false by default
        } else {
            System.err.println("Could not identify customer.");
            return null;
        }
    }

    @WebMethod(operationName = "getManagedNetworks")
    public List<XMLNetwork> getManagedNetworks(
            @WebParam(name = "customer") String customerID,
            @WebParam(name = "user") String user,
            @WebParam(name = "password") String password) {

        List<XMLNetwork> xnetworks = new ArrayList<XMLNetwork>();

        UserAction userAction = new UserAction();

        AuthenticationToken token = userAction.authenticate(customerID, user, password);
        if (token.authenticated && token.authorized) {
            for (ManagedNetwork network : new ManagedNetworkManager(Utility.getCustomer(customerID)).getManagedNetworks()) {
                XMLNetwork xnetwork = new XMLNetwork();
                xnetwork.name = network.getDescription();
                String cidr = network.getAddress();
                String[] cidrParts = cidr.split("/");
                xnetwork.address = cidrParts[0];
                xnetwork.mask = Integer.parseInt(cidrParts[1].trim());

                xnetworks.add(xnetwork);
            }
        }
        return xnetworks;
    }

    private String generateNonce() {
        StringBuilder builder = new StringBuilder();
        Random r = new Random();
        for (int i = 0; i < 24; i++) {
            int value = 0;

            while (value < 65) {
                value = r.nextInt(91);
            }

            builder.append((char) value);
        }
        return builder.toString();
    }

    private class BuildFlowList implements Runnable {

        private HttpSession session;
        private String constraints;
        private String tracker;
        private Customer customer;

        private BuildFlowList(Customer customer, HttpSession session, String constraints, String tracker) {
            this.session = session;
            this.constraints = constraints;
            this.tracker = tracker;
            this.customer = customer;
        }

        @Override
        public void run() {
            FlowManager flowManager = new FlowManager(customer);
            flowManager.getFlows(session, constraints, tracker);

            //System.out.println("FlowManager getPackets() appears to have completed...");
            SessionManager.isProcessingPacketsComplete(session, tracker, true);
        }
    }

    public static class BuildDataVolumeList implements Runnable, Callable {

        private HttpSession session;
        private String constraints;
        private Integer bins;
        private Customer customer;
        public String tracker;
        private XMLDataVolumeList results = new XMLDataVolumeList();

        private BuildDataVolumeList(Customer customer, HttpSession session, String constraints, Integer bins, String tracker) {
            this.session = session;
            this.constraints = constraints;
            this.bins = bins;
            this.customer = customer;
            this.tracker = tracker;
        }

        @Override
        public void run() {
            FlowManager flowManager = new FlowManager(customer);
            results = flowManager.getXMLDataVolumes(session, constraints, bins);
        }

        @Override
        public XMLDataVolumeList call() {
            results.tracker = this.tracker;
            return results;
        }
    }

    @XmlType
    public static class FlowSet {

        @XmlElement
        public List<PersistentFlow> flows = new ArrayList();
        @XmlElement
        public String tracker;
        @XmlElement
        public Boolean finished = false;
    }
}
