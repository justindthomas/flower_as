package name.justinthomas.flower.analysis.persistence;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.transaction.*;
import name.justinthomas.flower.analysis.element.Flow;
import name.justinthomas.flower.analysis.services.xmlobjects.XMLDataVolume;
import name.justinthomas.flower.analysis.services.xmlobjects.XMLDataVolumeList;
import name.justinthomas.flower.analysis.statistics.StatisticalInterval;
import name.justinthomas.flower.analysis.statistics.StatisticsManager;
import name.justinthomas.flower.global.GlobalConfigurationManager;
import name.justinthomas.flower.manager.services.CustomerAdministration.Customer;

@PersistenceContext(name = "persistence/Analysis", unitName = "AnalysisPU")
public class FlowManager {

    private Customer customer;
    private GlobalConfigurationManager globalConfigurationManager;
    private EntityManager em;

    public FlowManager(Customer customer) {
        this.customer = customer;

        try {
            this.globalConfigurationManager = InitialContext.doLookup("java:global/Analysis/GlobalConfigurationManager");
        } catch (NamingException e) {
            e.printStackTrace();
        }

        try {
            this.em = (EntityManager) InitialContext.doLookup("java:comp/env/persistence/Analysis");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    public List<PersistentFlow> getFlows(Set<Long> ids) {
        List<PersistentFlow> flows = new ArrayList();

        for (Long id : ids) {
            flows.add((PersistentFlow) em.find(PersistentFlow.class, id));
        }

        return flows;
    }

    public void getFlows(HttpSession session, String constraintsString, String tracker) {
        System.out.println("getFlows called.");
        Constraints constraints = new Constraints(constraintsString);
        SessionManager.getFlows(session, tracker).clear();

        StatisticsManager statisticsManager = new StatisticsManager(customer);
        List<StatisticalInterval> intervals = statisticsManager.getStatisticalIntervals(constraints, null);

        HashSet<Long> flowIDs = new HashSet();

        for (StatisticalInterval interval : intervals) {
            for (Long id : interval.getFlowIDs()) {
                flowIDs.add(id);
            }
        }

        List<PersistentFlow> flows = this.getFlows(flowIDs);


        System.out.println("Total flagged IDs: " + flowIDs.size());

        try {
            for (PersistentFlow pflow : flows) {
                Boolean select = false;
                if (constraints.sourceAddressList.isEmpty() || constraints.sourceAddressList.contains(InetAddress.getByName(pflow.getSource()))) {
                    select = true;
                } else if (constraints.destinationAddressList.isEmpty() || constraints.destinationAddressList.contains(InetAddress.getByName(pflow.getDestination()))) {
                    select = true;
                }

                if (select) {
                    SessionManager.getFlows(session, tracker).add(new Flow(customer, pflow));
                }

                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException();
                }
            }
        } catch (InterruptedException ie) {
            System.err.println("FlowManager interrupted during getFlows: " + ie.getMessage());
        } catch (UnknownHostException uhe) {
            System.err.println("FlowManager interrupted during getFlows: " + uhe.getMessage());
        }
    }

    public Long rebuildStatistics(String collector, Long start) {

        /*
         * StatisticsManager statisticsManager = new
         * StatisticsManager(customer);
         *
         * Environment environment; EntityStore readOnlyEntityStore = new
         * EntityStore(environment = setupEnvironment(), "Flow",
         * this.getStoreConfig(true)); FlowAccessor dataAccessor = new
         * FlowAccessor(readOnlyEntityStore);
         *
         * Long id = null; ForwardCursor<PersistentFlow> flowCursor = null;
         *
         * try { System.out.println("beginning to build statistics from flows,
         * starting at: " + start + "...");
         *
         *
         * int flowsProcessed = 0; PersistentFlow flow = null;
         *
         * if (start == 0l) { flowCursor = dataAccessor.flowById.entities(); }
         * else { flowCursor = dataAccessor.flowById.entities(start, false,
         * null, false); }
         *
         * while ((flow = flowCursor.next()) != null) {
         * statisticsManager.addStatisticalSeconds(new Flow(customer, flow),
         * flow.getId(), InetAddress.getByName(collector));
         *
         * id = flow.getId(); if (++flowsProcessed % 20000 == 0) {
         * System.out.println("processed " + flowsProcessed + " flows to
         * statistics - pausing at: " + id); break; } }
         *
         * if (flow == null) { System.out.println("completed building statistics
         * from flows."); id = null; } } catch (UnknownHostException uhe) {
         * System.err.println("Error in rebuildStatistics: " +
         * uhe.getMessage()); } finally { if (flowCursor != null) {
         * flowCursor.close(); } closeStore(readOnlyEntityStore);
         * closeEnvironment(environment); }
         *
         * return id;
         *
         */
        return null;
    }

    public XMLDataVolumeList getXMLDataVolumes(HttpSession session, String constr, Integer nmb_bins) {
        XMLDataVolumeList volumeList = new XMLDataVolumeList();

        Boolean cancelVolume = false;
        StatisticsManager statisticsManager = new StatisticsManager(customer);

        Constraints constraints = new Constraints(constr);
        Long intervalDuration = (constraints.endTime.getTime() - constraints.startTime.getTime()) / nmb_bins;

        try {
            LinkedHashMap<Date, HashMap<String, HashMap<Integer, Long>>> bins = statisticsManager.getVolumeByTime(constraints, nmb_bins);

            if (bins == null) {
                volumeList.errors.add(XMLDataVolumeList.Error.INSUFFICIENT_DATA);
                volumeList.ready = true;
                return volumeList;
            }

            for (Date date : bins.keySet()) {
                XMLDataVolume volume = new XMLDataVolume();

                volume.date = date;
                volume.duration = intervalDuration;
                volume.total = 0l;
                volume.tcp = 0l;
                volume.udp = 0l;
                volume.icmp = 0l;
                volume.icmpv6 = 0l;
                volume.ipv4 = 0l;
                volume.ipv6 = 0l;
                volume.ipsec = 0l;
                volume.sixinfour = 0l;

                for (Integer version : bins.get(date).get("versions").keySet()) {
                    volume.total += bins.get(date).get("versions").get(version);
                }

                if (bins.get(date).get("types").containsKey(6)) {
                    volume.tcp = bins.get(date).get("types").get(6);
                }

                if (bins.get(date).get("types").containsKey(17)) {
                    volume.udp = bins.get(date).get("types").get(17);
                }

                if (bins.get(date).get("types").containsKey(1)) {
                    volume.icmp = bins.get(date).get("types").get(1);
                }

                if (bins.get(date).get("types").containsKey(58)) {
                    volume.icmpv6 = bins.get(date).get("types").get(58);
                }

                if (bins.get(date).get("types").containsKey(41)) {
                    volume.sixinfour = bins.get(date).get("types").get(41);
                }

                if (bins.get(date).get("types").containsKey(50)) {
                    volume.ipsec += bins.get(date).get("types").get(50);
                }

                if (bins.get(date).get("types").containsKey(51)) {
                    volume.ipsec += bins.get(date).get("types").get(51);
                }

                if (bins.get(date).get("versions").containsKey(4)) {
                    volume.ipv4 += bins.get(date).get("versions").get(4);
                }

                if (bins.get(date).get("versions").containsKey(6)) {
                    volume.ipv6 += bins.get(date).get("versions").get(6);
                }

                volumeList.bins.add(volume);

                if (cancelVolume) {
                    break;
                }
            }
        } catch (ClassNotFoundException cnfe) {
            System.err.println("ClassNotFoundException caught: " + cnfe.getMessage());
        }

        if (!cancelVolume) {
            volumeList.ready = true;
        } else {
            System.out.println("Canceled volume build");
        }

        return volumeList;
    }

    public void cleanFlows(ArrayList<Long> flowIDs) {
        /*
         * System.out.println("Deleting expired flows...");
         *
         * Environment environment; EntityStore entityStore = new
         * EntityStore(environment = setupEnvironment(), "Flow",
         * this.getStoreConfig(false)); FlowAccessor dataAccessor = new
         * FlowAccessor(entityStore);
         *
         * int deletedCount = 0; for (Long flowID : flowIDs) { if ((flowID !=
         * null) && dataAccessor.flowById.contains(flowID)) {
         * dataAccessor.flowById.delete(flowID);
         *
         * if (++deletedCount % 1000 == 0) { System.out.println("Flows deleted:
         * " + deletedCount); } } }
         *
         * System.out.println("Total flows deleted: " + deletedCount);
         *
         * closeStore(entityStore); //recordEnvironmentStatistics(environment);
         * cleanLog(environment); checkpoint(environment);
         * closeEnvironment(environment);
         *
         */
    }

    public LinkedList<Long> addFlows(String sender, LinkedList<Flow> flows) {
        return this.addFlows(sender, flows, null);
    }

    private void addFlow(PersistentFlow flow) {
        try {
            Context context = new InitialContext();
            UserTransaction utx = (UserTransaction) context.lookup("java:comp/UserTransaction");

            utx.begin();
            em.persist(flow);
            utx.commit();
        } catch (NullPointerException npe) {
            npe.printStackTrace();
        } catch (NotSupportedException nse) {
            nse.printStackTrace();
        } catch (RollbackException rbe) {
            rbe.printStackTrace();
        } catch (HeuristicMixedException hme) {
            hme.printStackTrace();
        } catch (HeuristicRollbackException hrbe) {
            hrbe.printStackTrace();
        } catch (SystemException se) {
            se.printStackTrace();
        } catch (NamingException ne) {
            ne.printStackTrace();
        }
    }

    public LinkedList<Long> addFlows(String sender, LinkedList<Flow> flows, HttpServletRequest request) {
        LinkedList<Long> ids = new LinkedList();

        try {
            for (Flow flow : flows) {
                if ((flow.protocol == 6) || (flow.protocol == 17)) {
                    globalConfigurationManager.addFrequency(customer, flow.protocol, flow.ports);
                }

                PersistentFlow pflow = flow.toHashTableFlow();
                this.addFlow(pflow);

                ids.add(pflow.getId());
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

        return ids;
    }
}
