package name.justinthomas.flower.analysis.services;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

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
import name.justinthomas.flower.analysis.persistence.SessionManager;
import name.justinthomas.flower.analysis.persistence.ThreadManager;
import name.justinthomas.flower.analysis.persistence.TimedThread;
import name.justinthomas.flower.analysis.services.xmlobjects.XMLDataVolumeList;
import name.justinthomas.flower.analysis.services.xmlobjects.XMLFlowSet;
import name.justinthomas.flower.analysis.services.xmlobjects.XMLNode;
import name.justinthomas.flower.analysis.statistics.StatisticalInterval;
import name.justinthomas.flower.analysis.statistics.StatisticsManager;

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
            @WebParam(name = "constraints") String constraints,
            @WebParam(name = "user") String user,
            @WebParam(name = "password") String password) {

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
            if (!SessionManager.isMapBuilding(session)) {
                SessionManager.isMapBuilding(session, true);

                //System.out.println("Starting map build thread.");
                BuildNetworkList task = new BuildNetworkList(session, constraints);
                threadManager.start(user, new TimedThread(task));
            }
        } else {
            SessionManager.setNetworks(session, null);
            SessionManager.flowsProcessed(session, null);
            createTraceFile(networks);
            return networks;
        }

        XMLNetworkList xmlNetworkList = new XMLNetworkList();
        xmlNetworkList.flowsProcessed = SessionManager.flowsProcessed(session);

        return (xmlNetworkList);  // The "ready" Boolean in XMLNL is false by default
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
    @WebMethod(operationName = "getPackets")
    public XMLFlowSet getPackets(
            @WebParam(name = "constraints") String constraints,
            @WebParam(name = "user") String user,
            @WebParam(name = "password") String password) {

        UserAction userAction = new UserAction();

        if (!userAction.authenticate(user, password).authorized) {
            return (null);
        }

        MessageContext messageContext = serviceContext.getMessageContext();
        HttpSession session = ((HttpServletRequest) messageContext.get(MessageContext.SERVLET_REQUEST)).getSession();

        if (session == null) {
            throw new WebServiceException("No session in WebServiceContext");
        }

        if (!SessionManager.isProcessingPacketsComplete(session)) {
            //System.out.println("It appears that processing is not complete...");
            if (!SessionManager.isProcessingPackets(session)) {
                //System.out.println("It appears that processing has not begun...");
                SessionManager.isProcessingPackets(session, true);

                //System.out.println("Starting packet processing thread.");
                BuildPacketList task = new BuildPacketList(session, constraints);
                threadManager.start(user, new TimedThread(task));
            }
        }

        XMLFlowSet xmlPacketList = new XMLFlowSet();

        Integer resultCount = 0;
        final Integer MAX_REQUEST_RESULTS = 100;
        while (!SessionManager.getPackets(session).isEmpty() && (resultCount++ < MAX_REQUEST_RESULTS)) {
            xmlPacketList.flows.add(SessionManager.getPackets(session).remove(0).toXMLFlow());
        }

        if (SessionManager.getPackets(session).isEmpty() && SessionManager.isProcessingPacketsComplete(session)) {
            xmlPacketList.finished = true;
            SessionManager.isProcessingPackets(session, null);
            SessionManager.isProcessingPacketsComplete(session, null);
            SessionManager.getPackets(session).clear();
        }

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
                        SessionManager.packetsProcessed(session, null);
                    } catch (InterruptedException e) {
                        System.err.println("Thread: " + thread.getName() + " was interrupted.");
                    }
                }
            }
        } else {
            SessionManager.setVolumes(session, null);
            SessionManager.packetsProcessed(session, null);
        }

        return (volumes);  // The "ready" Boolean in XMLDVL is false by default
    }

    private class BuildPacketList implements Runnable {

        private HttpSession session;
        private String constraints;

        private BuildPacketList(HttpSession session, String constraints) {
            this.session = session;
            this.constraints = constraints;
        }

        @Override
        public void run() {
            FlowManager flowManager = new FlowManager();
            flowManager.getFlows(session, constraints);

            //System.out.println("FlowManager getPackets() appears to have completed...");
            SessionManager.isProcessingPacketsComplete(session, true);
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
}
