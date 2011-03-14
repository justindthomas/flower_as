package name.justinthomas.flower.analysis.element;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashMap;

import org.bouncycastle.util.encoders.Base64Encoder;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map.Entry;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import name.justinthomas.flower.analysis.persistence.FrequencyManager;
import name.justinthomas.flower.analysis.persistence.PersistentFlow;
import name.justinthomas.flower.analysis.statistics.StatisticalFlow;
import name.justinthomas.flower.analysis.statistics.StatisticalFlowDetail;
import name.justinthomas.flower.analysis.statistics.StatisticalFlowDetail.Count;
import name.justinthomas.flower.analysis.services.xmlobjects.XMLFlow;

public class Flow implements Serializable {

    private static final Integer DEBUG = 1;
    public Date startTimeStamp;
    public Date lastTimeStamp;
    public String ethernetType;
    public String reportedBy;
    public InetAddress[] addresses = new InetAddress[2];
    public Integer[] ports = {null, null};
    public Integer sourceOrdinal;
    public Integer protocol;
    public Integer packetsSent;
    public Integer packetsReceived;
    public BigDecimal bytesSent = new BigDecimal(0);
    public BigDecimal bytesReceived = new BigDecimal(0);
    public Integer flags = 0;
    HashMap<Integer, String> ipTypeResolution = new HashMap<Integer, String>();
    public static final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss.SSS");
    public static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    public static final SimpleDateFormat timeDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    FrequencyManager frequencyManager;

    private void init() {
        try {
            Context context = new InitialContext();
            frequencyManager = (FrequencyManager) context.lookup("java:global/Analysis/FrequencyManager");
        } catch (NamingException ne) {
            ne.printStackTrace();
        }

        populateIpTypes();
    }

    public Flow() {
        init();
    }

    public Flow(Entry<StatisticalFlowDetail, Long> entry, StatisticalFlow statisticalFlow) throws UnknownHostException {
        this();

        Integer sourceMapping = 0;
        if (entry.getKey().getProtocol().equals(6) || entry.getKey().getProtocol().equals(17)) {
            if (frequencyManager.getFrequency(entry.getKey().getProtocol(), entry.getKey().getSource()) > frequencyManager.getFrequency(entry.getKey().getProtocol(), entry.getKey().getDestination())) {
                sourceMapping = 1;
            }

            this.ports[sourceMapping] = entry.getKey().getSource();
            this.ports[sourceMapping ^ 1] = entry.getKey().getDestination();
        }

        this.sourceOrdinal = 0;
        this.addresses[sourceMapping] = InetAddress.getByName(statisticalFlow.getSource());
        this.addresses[sourceMapping ^ 1] = InetAddress.getByName(statisticalFlow.getDestination());
        this.protocol = entry.getKey().getProtocol();

        if (sourceMapping == 0) {
            if (entry.getKey().getType().equals(Count.BYTE)) {
                this.bytesSent = new BigDecimal(entry.getValue());
            } else if (entry.getKey().getType().equals(Count.PACKET)) {
                this.packetsSent = entry.getValue().intValue();
            }
        } else if (sourceMapping == 1) {
            if (entry.getKey().getType().equals(Count.BYTE)) {
                this.bytesReceived = new BigDecimal(entry.getValue());
            } else if (entry.getKey().getType().equals(Count.PACKET)) {
                this.packetsReceived = entry.getValue().intValue();
            }
        }
    }

    public Flow(XMLFlow xflow) throws UnknownHostException {
        this();

        this.bytesSent = new BigDecimal(xflow.bytesSent);
        this.bytesReceived = new BigDecimal(xflow.bytesReceived);
        this.addresses[0] = InetAddress.getByName(xflow.sourceAddress);
        this.addresses[1] = InetAddress.getByName(xflow.destinationAddress);
        this.ports[0] = xflow.sourcePort;
        this.ports[1] = xflow.destinationPort;

        this.ethernetType = xflow.ethernetType;

        this.startTimeStamp = new Date(xflow.startTimeStamp);
        this.lastTimeStamp = new Date(xflow.lastTimeStamp);
        this.packetsSent = xflow.packetsSent;
        this.packetsReceived = xflow.packetsReceived;
        this.protocol = xflow.protocol;
        this.flags = xflow.flags;
        if (this.flags < 0) {
            System.err.println("Negative flags value in Flow(XMLFlow): " + this.flags);
        }

        this.reportedBy = xflow.reportedBy;
    }

    public Flow(PersistentFlow sflow) throws UnknownHostException {
        this();

        this.bytesSent = new BigDecimal(sflow.size);

        this.addresses[0] = InetAddress.getByName(sflow.source);
        this.addresses[1] = InetAddress.getByName(sflow.destination);
        this.ports[0] = sflow.sourcePort;
        this.ports[1] = sflow.destinationPort;
        this.sourceOrdinal = 0;

        this.ethernetType = sflow.ethernetType;

        this.lastTimeStamp = sflow.lastTimeStamp;
        this.startTimeStamp = sflow.startTimeStamp;

        this.packetsSent = sflow.packetCount;
        this.protocol = sflow.protocol;

        this.flags = sflow.flags;

        this.reportedBy = sflow.reportedBy;
    }

    public PersistentFlow toHashTableFlow() {
        PersistentFlow sflow = new PersistentFlow(this);

        return sflow;
    }

    public XMLFlow toXMLFlow() {
        XMLFlow xflow = new XMLFlow();

        if (startTimeStamp != null) {
            xflow.startTimeStamp = startTimeStamp.getTime();
        }

        if (lastTimeStamp != null) {
            xflow.lastTimeStamp = lastTimeStamp.getTime();
        }

        xflow.ethernetType = ethernetType;

        try {
            xflow.sourceAddress = getUnfixedSourceAddress().getHostAddress();
            xflow.destinationAddress = getUnfixedDestinationAddress().getHostAddress();
            xflow.sourcePort = getUnfixedSourcePort();
            xflow.destinationPort = getUnfixedDestinationPort();
        } catch (Exception e) {
            System.err.println("Exception in Flow: " + e.getMessage());
        }

        if (protocol != null) {
            xflow.protocol = protocol;
        }

        if (bytesSent != null) {
            xflow.bytesSent = bytesSent.longValue();
        }

        if (bytesReceived != null) {
            xflow.bytesReceived = bytesReceived.longValue();
        }

        if (packetsSent != null) {
            xflow.packetsSent = packetsSent;
        }

        if (packetsReceived != null) {
            xflow.packetsReceived = packetsReceived;
        }

        xflow.flags = this.flags;

        xflow.reportedBy = this.reportedBy;

        return xflow;
    }

    private Boolean determineDirectionality() {
        if ((protocol != 6) && (protocol != 17)) {
            sourceOrdinal = 0;
        }

        if ((sourceOrdinal == null) && (protocol == 6)) {
            // TODO: Insert code when flows include TCP flag information
        }

        // Expression below returns true if the above block didn't succeed (i.e., if this
        // is a non-TCP flow or if there are no SYN or SYN-ACK packets
        // to use to determine directionality)
        if (sourceOrdinal == null) {

            // If the ports in ports[0] and ports[1] are the same, this block won't work properly
            // This statement checks for the possibility
            if ((ports[0] != null) && (ports[1] != null) && (ports[0].intValue() != ports[1].intValue())) {

                //Retrieve the frequencies for each port and populate a new Integer array with that data
                Integer[] frequency = new Integer[2];

                for (int i = 0; i < 2; i++) {
                    frequency[i] = frequencyManager.getFrequency(protocol, ports[i]);
                }

                // If the frequency of ports[0] == the frequency of ports[1], ports[0] will be used.
                int maximumFrequency = 0;
                Integer destinationOrdinal = null;
                for (int i = 0; i < 2; i++) {
                    // Iterate over the retrieved frequencies and populate 'f' with the greater
                    // frequency (this indicates the destination of the flow); desinationOrdinal is
                    // populated with the ordinal associated with the larger frequency
                    if (frequency[i] > maximumFrequency) {
                        maximumFrequency = frequency[i];
                        destinationOrdinal = i;
                    }
                }

                if (destinationOrdinal != null) {
                    sourceOrdinal = (destinationOrdinal ^ 1);
                }
            } else {
                // Source and Destination ports match; directionality is indeterminate
                // This is common for broadcast traffic; in that case all flows are unidirectional
                // so we just accept ordinal "0" as the source
                sourceOrdinal = 0;
            }
        }

        if (sourceOrdinal == null) {
            // If sourceOrdinal is still null, there is probably an error in the code above
            return false;
        }

        return true;
    }

    public InetAddress getUnfixedSourceAddress() {
        return addresses[0];
    }

    public InetAddress getUnfixedDestinationAddress() {
        return addresses[1];
    }

    public InetAddress getSourceAddress() throws Exception {
        if (sourceOrdinal == null) {
            //System.out.println("Determining source address.");
            if (!determineDirectionality()) {
                throw new Exception("SourceAddress can not be determined.");
            }
        }

        return addresses[sourceOrdinal];
    }

    public InetAddress getDestinationAddress() throws Exception {
        if (sourceOrdinal == null) {
            //System.out.println("Determining destination address.");
            if (!determineDirectionality()) {
                throw new Exception("DestinationAdddress has not been determined.");
            }
        }

        return addresses[sourceOrdinal ^ 1];    //TODO: Verify that this works as intended
    }

    public Integer getUnfixedSourcePort() {
        return ports[0];
    }

    public Integer getSourcePort() throws Exception {
        if (sourceOrdinal == null) {
            //System.out.println("Determining source port.");
            if (!determineDirectionality()) {
                throw new Exception("SourcePort has not been determined.");
            }
        }

        return ports[sourceOrdinal];
    }

    public Integer getUnfixedDestinationPort() {
        return ports[1];
    }

    public Integer getDestinationPort() throws Exception {
        if (sourceOrdinal == null) {
            //System.out.println("Determining destination port.");
            if (!determineDirectionality()) {
                throw new Exception("DestinationPort has not been determined.");
            }
        }

        return ports[sourceOrdinal ^ 1];        //TODO: Verify that this works as intended
    }

    public HashMap<String, Boolean> getFlagMap(Integer flags) {


        //#define CWR flag[0]
        //#define ECE flag[1]
        //#define URG flag[2]
        //#define ACK flag[3]
        //#define PSH flag[4]
        //#define RST flag[5]
        //#define SYN flag[6]
        //#define FIN flag[7]

        HashMap<String, Boolean> bools = new HashMap<String, Boolean>();

        String flagNames[] = {"cwr", "ece", "urg", "ack", "psh", "rst", "syn", "fin"};
        Boolean flag[] = {false, false, false, false, false, false, false, false};

        if (flags != null) {
            int a, b, f = flags;

            for (a = 128, b = 0; a > 0; b++) {
                if (f >= a) {
                    flag[b] = true;
                    f -= a;
                }
                if (a == 1) {
                    a -= 1;
                } else {
                    a -= (a / 2);
                }
            }
        }

        for (int i = 0; i <= 7; i++) {
            bools.put(flagNames[i], flag[i]);
        }

        return bools;
    }

    private void populateIpTypes() {
        ipTypeResolution.put(6, "tcp");
        ipTypeResolution.put(17, "udp");
        ipTypeResolution.put(50, "esp");
        ipTypeResolution.put(51, "ah");
        ipTypeResolution.put(1, "icmp");
        ipTypeResolution.put(41, "ipv6-in-ipv4");
        ipTypeResolution.put(58, "icmpv6");
    }

    public String summarizedId() {
        Base64Encoder encoder = new Base64Encoder();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        StringBuffer id = new StringBuffer();
        if (addresses[0] != null) {
            id.append(addresses[0].getHostAddress());
            if (addresses[1] != null) {
                id.append("/" + addresses[1].getHostAddress());
            }
            if (ports[1] != null) {
                id.append(":" + ports[1]);
            }
            if (protocol != null) {
                id.append("#" + protocol);
            }
        } else {
            System.err.println("This flow seems to be malformed: " + toString());
        }

        try {
            encoder.encode(id.toString().getBytes(), 0, id.length(), baos);
        } catch (IOException ioe) {
            System.err.println(ioe.toString());
        }

        return baos.toString();
    }

    public String id() {
        Base64Encoder encoder = new Base64Encoder();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        StringBuffer id = new StringBuffer();
        if (addresses[0] != null) {
            id.append(addresses[0].getHostAddress());
            if (ports[0] != null) {
                id.append(":" + ports[0]);
            }
            if (addresses[1] != null) {
                id.append("/" + addresses[1].getHostAddress());
            }
            if (ports[1] != null) {
                id.append(":" + ports[1]);
            }
            if (protocol != null) {
                id.append("#" + protocol);
            }
        } else {
            System.err.println("This flow seems to be malformed: " + toString());
        }

        try {
            encoder.encode(id.toString().getBytes(), 0, id.length(), baos);
        } catch (IOException ioe) {
            System.err.println(ioe.toString());
        }

        return baos.toString();
    }

    @Override
    public String toString() {

        StringBuffer flowString = new StringBuffer();
        if (startTimeStamp != null) {
            flowString.append(timeFormat.format(startTimeStamp) + " ");
        }

        try {
            if (getSourceAddress() != null) {
                if (getSourceAddress().getHostAddress().equals("0.0.0.0")) {
                    flowString.append(" DEFAULT");
                } else {
                    flowString.append(" " + getSourceAddress().getHostAddress());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (getSourcePort() != null) {
                if (!getSourcePort().equals(-1)) {
                    flowString.append(":" + getSourcePort());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        flowString.append(" to ");

        try {
            if (getDestinationAddress() != null) {
                if (getDestinationAddress().getHostAddress().equals("0.0.0.0")) {
                    flowString.append("DEFAULT");
                } else {
                    flowString.append(getDestinationAddress().getHostAddress());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (getDestinationPort() != null) {
                if (!getDestinationPort().equals(-1)) {
                    flowString.append(":" + getDestinationPort());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (ipTypeResolution.get(protocol) == null) {
            flowString.append(" " + ethernetType);
        } else {
            flowString.append("/" + ipTypeResolution.get(protocol));
        }

        flowString.append(" bytesSent: " + bytesSent);
        flowString.append(" bytesReceived: " + bytesReceived);

        return flowString.toString();
    }

    public String getResolvedProtocol() {
        return ipTypeResolution.get(protocol);
    }

    public String getStartDate() {
        return dateFormat.format(startTimeStamp);
    }

    public String getStartTime() {
        return timeFormat.format(startTimeStamp);
    }

    public String getLastDate() {
        return dateFormat.format(lastTimeStamp);
    }

    public String getLastTime() {
        return timeFormat.format(lastTimeStamp);
    }

    public BigDecimal getBytesReceived() {
        if (bytesReceived == null) {
            return new BigDecimal(0);
        }

        return bytesReceived;
    }

    public void setBytesReceived(BigDecimal bytesReceived) {
        this.bytesReceived = bytesReceived;
    }

    public BigDecimal getBytesSent() {
        if (bytesSent == null) {
            return new BigDecimal(0);
        }

        return bytesSent;
    }

    public void setBytesSent(BigDecimal bytesSent) {
        this.bytesSent = bytesSent;
    }

    public Integer getPacketsReceived() {
        if (packetsReceived == null) {
            return 0;
        }

        return packetsReceived;
    }

    public void setPacketsReceived(Integer packetsReceived) {
        this.packetsReceived = packetsReceived;
    }

    public Integer getPacketsSent() {
        if (packetsSent == null) {
            return 0;
        }

        return packetsSent;
    }

    public void setPacketsSent(Integer packetsSent) {
        this.packetsSent = packetsSent;
    }
}

