/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package name.justinthomas.flower.analysis.statistics;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import name.justinthomas.flower.analysis.persistence.FrequencyManager;

/**
 *
 * @author justin
 */
public class StatisticalProfile {

    //public static final Long SAMPLE_INTERVAL = 5000l;
    //public static final Long HISTORICAL_INTERVAL = 14 * 24 * 60 * 60 * 1000l;
    private ProfileType type;
    private HashMap<String, ServiceList> nodes = new HashMap<String, ServiceList>();
    FrequencyManager frequencyManager;// = FrequencyManager.getFrequencyManager();

    public void addStatisticalSecond(StatisticalInterval second) {
        for (StatisticalFlow flow : second.flows.values()) {
            ServiceList sourceServices =
                    nodes.containsKey(flow.source) ? nodes.get(flow.source) : new ServiceList();

            ServiceList destinationServices =
                    nodes.containsKey(flow.destination) ? nodes.get(flow.destination) : new ServiceList();

            for (StatisticalFlowDetail detail : flow.getCount().keySet()) {
                Service sourceService = new Service(flow.destination);
                sourceService.relationshipToNode =
                        frequencyManager.getFrequency(detail.protocol, detail.source) > frequencyManager.getFrequency(detail.protocol, detail.destination)
                        ? Relationship.CONSUMER : Relationship.PROVIDER;
                sourceService.protocol = detail.protocol;

                if (sourceService.relationshipToNode == Relationship.CONSUMER) {
                    sourceService.port = detail.source;
                } else {
                    sourceService.port = detail.destination;
                }

                Attributes sourceAttributes = sourceServices.services.containsKey(sourceService)
                        ? sourceServices.services.get(sourceService) : new Attributes();

                Long count = flow.getCount().get(detail);
                if (detail.getType().equals(StatisticalFlowDetail.Count.BYTE)) {
                    sourceAttributes.maxBytesSent = sourceAttributes.maxBytesSent > count
                            ? sourceAttributes.maxBytesSent : count;
                    sourceAttributes.minBytesSent = sourceAttributes.minBytesSent < count
                            ? sourceAttributes.minBytesSent : count;
                } else if (detail.getType().equals(StatisticalFlowDetail.Count.PACKET)) {
                    sourceAttributes.maxPacketsSent = sourceAttributes.maxPacketsSent > count
                            ? sourceAttributes.maxPacketsSent : count;
                    sourceAttributes.minPacketsSent = sourceAttributes.minPacketsSent < count
                            ? sourceAttributes.minPacketsSent : count;
                }

                Service destinationService = new Service(flow.source);
                destinationService.relationshipToNode =
                        frequencyManager.getFrequency(detail.protocol, detail.source) > frequencyManager.getFrequency(detail.protocol, detail.destination)
                        ? Relationship.PROVIDER : Relationship.CONSUMER;
                destinationService.protocol = detail.protocol;

                if (destinationService.relationshipToNode == Relationship.CONSUMER) {
                    destinationService.port = detail.source;
                } else {
                    destinationService.port = detail.destination;
                }
            }
        }
    }

    public static enum ProfileType {

        WEEKDAY,
        WEEKEND
    }

    public static enum Relationship {

        PROVIDER,
        CONSUMER
    }

    public static class ServiceList {

        HashMap<Service, Attributes> services = new HashMap<Service, Attributes>();
    }

    public static class Service {

        Relationship relationshipToNode;
        InetAddress address;
        Integer protocol;
        Integer port;

        public Service(String address) {
            try {
                this.address = InetAddress.getByName(address);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Service other = (Service) obj;
            if (this.address != other.address && (this.address == null || !this.address.equals(other.address))) {
                return false;
            }
            if (this.protocol != other.protocol && (this.protocol == null || !this.protocol.equals(other.protocol))) {
                return false;
            }
            if (this.port != other.port && (this.port == null || !this.port.equals(other.port))) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 53 * hash + (this.address != null ? this.address.hashCode() : 0);
            hash = 53 * hash + (this.protocol != null ? this.protocol.hashCode() : 0);
            hash = 53 * hash + (this.port != null ? this.port.hashCode() : 0);
            return hash;
        }
    }

    public static class Attributes {

        public Long maxBytesReceived = 0l;
        public Long minBytesReceived = 0l;
        public Long avgBytesReceived = 0l;
        public Long maxBytesSent = 0l;
        public Long minBytesSent = 0l;
        public Long avgBytesSent = 0l;
        public Long maxPacketsReceived = 0l;
        public Long minPacketsReceived = 0l;
        public Long avgPacketsReceived = 0l;
        public Long maxPacketsSent = 0l;
        public Long minPacketsSent = 0l;
        public Long avgPacketsSent = 0l;
    }
}
