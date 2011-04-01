package name.justinthomas.flower.analysis.services;

import java.io.FileWriter;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
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
import name.justinthomas.flower.analysis.element.InetNetwork;
import name.justinthomas.flower.analysis.persistence.FlowManager;
import name.justinthomas.flower.analysis.services.xmlobjects.XMLNetwork;
import name.justinthomas.flower.analysis.services.xmlobjects.XMLNetworkList;
import name.justinthomas.flower.analysis.element.ManagedNetworks;
import name.justinthomas.flower.analysis.persistence.ConfigurationManager;
import name.justinthomas.flower.analysis.persistence.Constraints;
import name.justinthomas.flower.analysis.persistence.PersistentFlow;
import name.justinthomas.flower.analysis.persistence.SessionManager;
import name.justinthomas.flower.analysis.persistence.ThreadManager;
import name.justinthomas.flower.analysis.persistence.TimedThread;
import name.justinthomas.flower.analysis.services.xmlobjects.XMLDataVolumeList;
import name.justinthomas.flower.analysis.services.xmlobjects.XMLNode;
import name.justinthomas.flower.analysis.statistics.StatisticalInterval;
import name.justinthomas.flower.analysis.statistics.StatisticsManager;
import org.bouncycastle.util.encoders.Base64;

/**
 *
 * @author justin
 */
@WebService()
public class ChartData {

    @EJB
    ThreadManager threadManager;
    //@EJB
    //FlowManager flowManager;
    @EJB
    ConfigurationManager configurationManager;
    @Resource
    private WebServiceContext serviceContext;

    /**
     * Web service operation
     */
    @WebMethod(operationName = "getNetworkMap")
    public XMLNetworkList getNetworkMap(
            @WebParam(name = "user") String user,
            @WebParam(name = "password") String password,
            @WebParam(name = "constraints") String constraints,
            @WebParam(name = "wait") Boolean wait) {

        UserAction userAction = new UserAction();

        if (!userAction.authenticate(user, password).authorized) {
            return (null);
        }

        MessageContext messageContext = serviceContext.getMessageContext();
        HttpSession session = ((HttpServletRequest) messageContext.get(MessageContext.SERVLET_REQUEST)).getSession();

        if (session == null) {
            throw new WebServiceException("No session in WebServiceContext");
        }

        XMLNetworkList networks = SessionManager.getNetworks(session);

        if (networks == null) {
            System.out.println("networks session variable is null.");
            if (!SessionManager.isMapBuilding(session)) {
                SessionManager.isMapBuilding(session, true);

                System.out.println("Starting map build thread.");
                BuildNetworkList task = new BuildNetworkList(session, constraints);
                TimedThread thread = new TimedThread(task);
                threadManager.start(user, thread);

                if (wait) {
                    try {
                        thread.join();
                        networks = SessionManager.getNetworks(session);
                        SessionManager.setNetworks(session, null);
                    } catch (InterruptedException e) {
                        System.err.println("Thread: " + thread.getName() + " was interrupted.");
                    }
                }
            }
        } else {
            networks = SessionManager.getNetworks(session);
            SessionManager.setNetworks(session, null);
        }

        if (networks == null) {
            // networks will be null at this point if wait is false
            // and the map has not been built yet, or if there is an error
            networks = new XMLNetworkList();
        }

        System.out.println("ready flag set to: " + networks.ready);
        return (networks);  // The "ready" Boolean in XMLNL is false by default
    }

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
            @WebParam(name = "user") String user,
            @WebParam(name = "password") String password,
            @WebParam(name = "constraints") String constraints,
            @WebParam(name = "tracker") String tracker) {

        UserAction userAction = new UserAction();

        if (!userAction.authenticate(user, password).authorized) {
            return (null);
        }

        MessageContext messageContext = serviceContext.getMessageContext();
        HttpSession session = ((HttpServletRequest) messageContext.get(MessageContext.SERVLET_REQUEST)).getSession();

        if (session == null) {
            throw new WebServiceException("No session in WebServiceContext");
        }

        System.out.println("constraints: " + constraints + ", tracker: " + tracker);
        if (tracker == null || tracker.isEmpty()) {
            //This is a new request
            //System.out.println("It appears that processing has not begun...");
            SessionManager.isProcessingPackets(session, true);

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

            BuildFlowList task = new BuildFlowList(session, constraints, tracker);
            threadManager.start(user, new TimedThread(task));
        }

        //if (!SessionManager.isProcessingPacketsComplete(session, constraints)) {
        //    //System.out.println("It appears that processing is not complete...");
        //    if (!SessionManager.isProcessingPackets(session)) {
        //        //System.out.println("It appears that processing has not begun...");
        //        SessionManager.isProcessingPackets(session, true);
        //
        //        //System.out.println("Starting packet processing thread.");
        //        BuildFlowList task = new BuildFlowList(session, constraints);
        //        threadManager.start(user, new TimedThread(task));
        //    }
        //}


        FlowSet xmlPacketList = new FlowSet();

        Integer resultCount = 0;
        final Integer MAX_REQUEST_RESULTS = 1000;
        try {
            while (!SessionManager.getFlows(session, tracker).isEmpty() && (resultCount++ < MAX_REQUEST_RESULTS)) {
                xmlPacketList.flows.add(SessionManager.getFlows(session, tracker).remove(0).toHashTableFlow());
            }

            if (SessionManager.getFlows(session, tracker).isEmpty() && SessionManager.isProcessingPacketsComplete(session, tracker)) {
                xmlPacketList.finished = true;
                SessionManager.isProcessingPackets(session, null);
                SessionManager.isProcessingPacketsComplete(session, tracker, null);
                SessionManager.getFlows(session, tracker).clear();
            }
        } catch (NullPointerException e) {
            System.err.println("Could not access flows; this is probably due to a non-existent tracker reference.");
        }

        xmlPacketList.tracker = tracker;
        return xmlPacketList;
    }

    @WebMethod(operationName = "getIntervals")
    public List<StatisticalInterval> getIntervals(
            @WebParam(name = "user") String user,
            @WebParam(name = "password") String password,
            @WebParam(name = "constraints") String constraints,
            @WebParam(name = "resolution") Integer resolution) {

        UserAction userAction = new UserAction();

        if (!userAction.authenticate(user, password).authorized) {
            return (null);
        }

        StatisticsManager statisticsManager = new StatisticsManager();
        return statisticsManager.getStatisticalIntervals(null, new Constraints(constraints), resolution);
    }

    /**
     * Web service operation
     */
    @WebMethod(operationName = "getDataVolume")
    public XMLDataVolumeList getDataVolume(
            @WebParam(name = "user") String user,
            @WebParam(name = "password") String password,
            @WebParam(name = "constraints") String constraints,
            @WebParam(name = "bins") Integer bins,
            @WebParam(name = "wait") Boolean wait) {

        UserAction userAction = new UserAction();

        if (!userAction.authenticate(user, password).authorized) {
            return (null);
        }

        MessageContext messageContext = serviceContext.getMessageContext();
        HttpSession session = ((HttpServletRequest) messageContext.get(MessageContext.SERVLET_REQUEST)).getSession();

        if (session == null) {
            throw new WebServiceException("No session in WebServiceContext");
        }

        XMLDataVolumeList volumes = SessionManager.getVolumes(session);

        if (volumes == null) {
            volumes = new XMLDataVolumeList();
            if (!SessionManager.isHistogramBuilding(session)) {
                SessionManager.isHistogramBuilding(session, true);
                System.out.println("Starting volume build thread: " + constraints + ", " + bins);
                BuildDataVolumeList task = new BuildDataVolumeList(session, constraints, bins);
                TimedThread thread = new TimedThread(task);
                threadManager.start(user, thread);
                if (wait) {
                    try {
                        thread.join();
                        volumes = SessionManager.getVolumes(session);
                        SessionManager.setVolumes(session, null);
                    } catch (InterruptedException e) {
                        System.err.println("Thread: " + thread.getName() + " was interrupted.");
                    }
                }
            }
        } else {
            SessionManager.setVolumes(session, null);
        }

        return (volumes);  // The "ready" Boolean in XMLDVL is false by default
    }

    private class BuildFlowList implements Runnable {

        private HttpSession session;
        private String constraints;
        private String tracker;

        private BuildFlowList(HttpSession session, String constraints, String tracker) {
            this.session = session;
            this.constraints = constraints;
            this.tracker = tracker;
        }

        @Override
        public void run() {
            FlowManager flowManager = new FlowManager();
            flowManager.getFlows(session, constraints, tracker);

            //System.out.println("FlowManager getPackets() appears to have completed...");
            SessionManager.isProcessingPacketsComplete(session, tracker, true);
        }
    }

    private class BuildNetworkList implements Runnable {

        private HttpSession session;
        private String constraints;

        private BuildNetworkList(HttpSession session, String constraints) {
            this.session = session;
            this.constraints = constraints;
        }

        @Override
        public void run() {
            FlowManager flowManager = new FlowManager();
            flowManager.getXMLNetworks(session, constraints);
            System.out.println("Completed BuildNetworkList thread.");
        }
    }

    private class BuildDataVolumeList implements Runnable {

        private HttpSession session;
        private String constraints;
        private Integer bins;

        private BuildDataVolumeList(HttpSession session, String constraints, Integer bins) {
            this.session = session;
            this.constraints = constraints;
            this.bins = bins;
        }

        @Override
        public void run() {
            FlowManager flowManager = new FlowManager();
            flowManager.getXMLDataVolumes(session, constraints, bins);
        }
    }

    /**
     * Web service operation
     */
    @WebMethod(operationName = "getManagedNetworks")
    public XMLNetworkList getManagedNetworks(
            @WebParam(name = "user") String user,
            @WebParam(name = "password") String password) {

        UserAction userAction = new UserAction();

        if (!userAction.authenticate(user, password).authorized) {
            XMLNetworkList networkList = new XMLNetworkList();
            networkList.ready = true;

            return (networkList);
        }

        {
            LinkedHashMap<String, InetNetwork> networks = new ManagedNetworks().getNetworks();
            XMLNetworkList networkList = new XMLNetworkList();

            for (Entry<String, InetNetwork> entry : networks.entrySet()) {
                XMLNetwork xnetwork = new XMLNetwork();
                xnetwork.address = entry.getValue().getAddress().getHostAddress();
                xnetwork.mask = entry.getValue().getMask();
                xnetwork.name = entry.getValue().getName();

                networkList.networks.add(xnetwork);
            }

            return networkList;
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
