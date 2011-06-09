package name.justinthomas.flower.analysis.element;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import name.justinthomas.flower.analysis.statistics.StatisticalFlow;
import name.justinthomas.flower.analysis.statistics.StatisticalFlowDetail;
import name.justinthomas.flower.manager.services.Customer;
import org.bouncycastle.util.encoders.UrlBase64Encoder;

public class Node {

    static final long serialVersionUID = 1;
    Long bytesReceived = 0l;
    Long bytesSent = 0l;
    protected InetAddress address;
    private String resolvedAddress;
    protected HashMap<IdentificationTuple, Flow> flowsOriginated = new HashMap<IdentificationTuple, Flow>();
    protected HashMap<IdentificationTuple, Flow> flowsReceived = new HashMap<IdentificationTuple, Flow>();

    public Node() {
    }

    public Node(String address) {
        setAddress(address);
        //setResolvedAddress(getAddress().getHostName());
    }

    protected class IdentificationTuple {

        protected String sourceAddress, destinationAddress;
        protected Integer sourcePort, destinationPort;
        protected Integer protocol;

        public IdentificationTuple(Flow flow) {
            try {
                this.sourceAddress = flow.getSourceAddress().getHostAddress();
                this.destinationAddress = flow.getDestinationAddress().getHostAddress();
                this.destinationPort = flow.getDestinationPort();
                this.protocol = flow.protocol;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof IdentificationTuple) {
                IdentificationTuple tuple = (IdentificationTuple) obj;
                if (tuple.sourceAddress.equals(this.sourceAddress)
                        && tuple.destinationAddress.equals(this.destinationAddress)
                        && tuple.protocol.equals(this.protocol)) {
                    if (tuple.protocol.equals(6) || tuple.protocol.equals(17)) {
                        if (tuple.destinationPort.equals(this.destinationPort)) {
                            return true;
                        }
                    } else {
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 97 * hash + (this.sourceAddress != null ? this.sourceAddress.hashCode() : 0);
            hash = 97 * hash + (this.destinationAddress != null ? this.destinationAddress.hashCode() : 0);
            hash = 97 * hash + (this.destinationPort != null ? this.destinationPort.hashCode() : 0);
            hash = 97 * hash + (this.protocol != null ? this.protocol.hashCode() : 0);
            return hash;
        }

        @Override
        public String toString() {
            return (sourceAddress + destinationAddress + destinationPort + protocol);
        }
    }

    public Integer flowCount() {
        return flowsOriginated.size();
    }

    public Long getTotalBytes() {
        return (getBytesSent() + getBytesReceived());
    }

    public Long getBytesSent() {
        return bytesSent;
    }

    public Long getBytesReceived() {
        return bytesReceived;
    }

    public void addFlow(Customer customer, StatisticalFlow statisticalFlow) {
        try {
            if (!statisticalFlow.getCount().isEmpty()) {
                for (Entry<StatisticalFlowDetail, Long> entry : statisticalFlow.getCount().entrySet()) {
                    //System.out.println("Adding TCP statFlow: " + statisticalFlow.getSource() + ":" + entry.getKey().getSource() + " -> " +
                    //        statisticalFlow.getDestination() + ":" + entry.getKey().getDestination() + " " + entry.getValue() + " bytes");
                    addFlow(new Flow(customer, entry, statisticalFlow));
                }
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public void addFlow(Flow flow) {
        try {
            if (flowsOriginated.containsKey(new IdentificationTuple(flow))) {
                Flow storedFlow = flowsOriginated.get(new IdentificationTuple(flow));
                storedFlow.setBytesSent(storedFlow.getBytesSent().add(flow.getBytesSent()));
                storedFlow.setBytesReceived(storedFlow.getBytesReceived().add(flow.getBytesReceived()));
                storedFlow.setPacketsSent(storedFlow.getPacketsSent() + flow.getPacketsSent());
                storedFlow.setPacketsReceived(storedFlow.getPacketsReceived() + flow.getPacketsReceived());
                flowsOriginated.put(new IdentificationTuple(storedFlow), storedFlow);
            } else if (this.getAddress().equals(flow.getSourceAddress())) {
                flowsOriginated.put(new IdentificationTuple(flow), flow);
            }

            if (flowsReceived.containsKey(new IdentificationTuple(flow))) {
                Flow storedFlow = flowsReceived.get(new IdentificationTuple(flow));
                storedFlow.setBytesSent(storedFlow.getBytesSent().add(flow.getBytesSent()));
                storedFlow.setBytesReceived(storedFlow.getBytesReceived().add(flow.getBytesReceived()));
                storedFlow.setPacketsSent(storedFlow.getPacketsSent() + flow.getPacketsSent());
                storedFlow.setPacketsReceived(storedFlow.getPacketsReceived() + flow.getPacketsReceived());
                flowsReceived.put(new IdentificationTuple(storedFlow), storedFlow);
            } else if (this.getAddress().equals(flow.getDestinationAddress())) {
                flowsReceived.put(new IdentificationTuple(flow), flow);
            }

            bytesSent += flow.getBytesSent().longValue();
            bytesReceived += flow.getBytesReceived().longValue();
        } catch (Exception e) {
            System.err.println("Exception caught in Node: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public ArrayList<Flow> getFlowsOriginated() {
        ArrayList<Flow> list = new ArrayList<Flow>();
        list.addAll(flowsOriginated.values());
        return list;
    }

    public ArrayList<Flow> getFlowsReceived() {
        ArrayList<Flow> list = new ArrayList<Flow>();
        list.addAll(flowsReceived.values());
        return list;
    }

    public void setAddress(String ipAddress) {
        try {
            this.address = InetAddress.getByName(ipAddress);
        } catch (UnknownHostException e) {
        }
    }

    public InetAddress getAddress() {
        return address;
    }

    public Boolean isDefault() {
        if (address.getHostAddress().equals("0.0.0.0")) {
            return true;
        }
        return false;
    }

    private Long getReceivedBytes() {
        Long receivedBytes = 0l;
        for (Flow flow : flowsOriginated.values()) {
            receivedBytes += flow.getBytesReceived().longValue();
        }

        for (Flow flow : flowsReceived.values()) {
            receivedBytes += flow.getBytesSent().longValue();
        }

        return receivedBytes;
    }

    private Long getSentBytes() {
        Long sentBytes = 0l;
        for (Flow flow : flowsOriginated.values()) {
            sentBytes += flow.getBytesSent().longValue();
        }

        for (Flow flow : flowsReceived.values()) {
            sentBytes += flow.getBytesReceived().longValue();
        }

        return sentBytes;
    }

    @Override
    public String toString() {
        return address.getHostAddress().toString();
    }

    public String id() {
        UrlBase64Encoder encoder = new UrlBase64Encoder();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        StringBuffer id = new StringBuffer();
        if (address != null) {
            id.append(address);
        } else {
            System.err.println("This node seems to be malformed: " + toString());
        }

        try {
            encoder.encode(id.toString().getBytes(), 0, id.length(), baos);
        } catch (IOException ioe) {
            System.err.println(ioe.toString());
        }

        return baos.toString();
    }

    public String getResolvedAddress() {
        if (resolvedAddress == null) {
            resolvedAddress = address.getHostName();
        }
        return resolvedAddress;
    }
}
