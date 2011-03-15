/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package name.justinthomas.flower.collector;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import name.justinthomas.flower.analysis.services.FlowInsert.FlowInsert;
import name.justinthomas.flower.analysis.services.FlowInsert.FlowInsertService;
import name.justinthomas.flower.analysis.services.FlowInsert.XmlFlow;
import name.justinthomas.flower.analysis.services.FlowInsert.XmlFlowSet;

/**
 *
 * @author justin
 */
public class QueueManager implements Runnable {
    protected static List<DatagramPacket> queue = Collections.synchronizedList(new LinkedList<DatagramPacket>());
    XmlFlowSet flowSet;

    public void run() {
        flowSet = new XmlFlowSet();
        processQueue();
    }

    private void processQueue() {
        System.out.println("Beginning to process queue...");

        ArrayList<DatagramPacket> retry = new ArrayList();
        while (!QueueManager.queue.isEmpty()) {
            DatagramPacket packet = null;

            try {
                packet = QueueManager.queue.remove(0);
            } catch (IndexOutOfBoundsException e) {
                System.err.println("Reached end of queue.");
                continue;
            }

            if (packet != null) {
                InetAddress sender = packet.getAddress();

                ByteArrayInputStream bais = new ByteArrayInputStream(packet.getData());
                DataInput input = new DataInputStream(bais);

                int version = 0, count = 0, uptime = 0, secs = 0;
                byte[] remaining = null;

                try {
                    version = input.readShort();    // 2 bytes
                    count = input.readShort();      // 2 bytes
                    uptime = input.readInt();       // 4 bytes
                    secs = input.readInt();        // 4 bytes

                    int bytesRemaining = packet.getLength() - 12;   // Subtract pre-processed length above
                    remaining = new byte[bytesRemaining];
                    input.readFully(remaining);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (version == 9) {
                    if (!v9(sender, remaining, count, uptime, secs)) {
                        retry.add(packet);
                    }
                } else if (version == 5) {
                    v5(remaining, count);
                }
            }
        }
        
        QueueManager.queue.addAll(retry);

        System.out.println("Completed processing queue...");

        if (!flowSet.getFlows().isEmpty()) {
            try {
                System.out.println("Sending " + flowSet.getFlows().size() + " flows to server...");
                FlowInsertService service = new FlowInsertService(new URL("http://localhost:8080/flower/analysis/FlowInsertService?wsdl"));
                FlowInsert port = service.getFlowInsertPort();
                port.addFlows(flowSet);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
    }

    private Boolean v9(InetAddress sender, byte[] remaining, Integer count, Integer sysUpTime, long epoch) {
        ByteArrayInputStream bais = new ByteArrayInputStream(remaining);
        DataInput input = new DataInputStream(bais);

        try {
            int sequence = input.readInt();
            //System.out.println("Sequence: " + sequence);

            long source = input.readInt();
            //System.out.println("Source: " + source);

            Long hash = new Long(sender.hashCode()) & 0xffffffffL;
            //System.out.println("Sender Hash: " + hash);
            Long streamId = (hash << 4) | source;
            //System.out.println("StreamID: " + streamId);

            for (int n = 0; n < count; n++) {
                //System.out.println("Iteration: " + n + "/" + count);
                int flowSetId = input.readUnsignedShort();
                int length = input.readUnsignedShort() - 4;

                //System.out.println("Received FlowSet ID: " + streamId.toString() + ":" + flowSetId + ", Length: " + length + ", From: " + sender.getHostAddress());
                if (flowSetId == 0) {
                    byte templateBytes[] = new byte[length];
                    input.readFully(templateBytes);

                    parseTemplates(streamId, templateBytes);
                } else if (flowSetId <= 255) {
                    // Reserved FlowSet; e.g., Options
                    input.skipBytes(length);
                } else {
                    // Data FlowSet
                    if (Listener.templates.containsKey(streamId.toString() + ":" + flowSetId)) {
                        // We have a template for this FlowSet
                        byte dataBytes[] = new byte[length];
                        input.readFully(dataBytes);

                        n += parseData(dataBytes, Listener.templates.get(streamId.toString() + ":" + flowSetId), sysUpTime, epoch);
                        return true;
                    } else {
                        //System.out.println("Template for " + streamId.toString() + ":" + flowSetId + " not yet received, discarding flow set");
                        input.skipBytes(length);
                        n = count; // Stop iterating
                        return false;
                    }
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return false;
    }

    private void v5(byte[] remaining, Integer count) {
        ByteArrayInputStream bais = new ByteArrayInputStream(remaining);
        DataInput input = new DataInputStream(bais);

        try {
            System.out.println("Nanos: " + input.readInt());
            System.out.println("Flows Seen: " + input.readInt());
            System.out.println("Engine Type: " + input.readUnsignedByte());
            System.out.println("Engine ID: " + input.readUnsignedByte());
            System.out.println("Sampling: " + input.readUnsignedShort());

            for (int n = 0; n < count; n++) {
                byte source[] = new byte[4];
                for (int i = 0; i < 4; i++) {
                    source[i] = input.readByte();
                }
                InetAddress address = InetAddress.getByAddress(source);
                System.out.print(address.getHostAddress() + " > ");

                byte destination[] = new byte[4];
                for (int i = 0; i < 4; i++) {
                    destination[i] = input.readByte();
                }
                InetAddress destinationAddress = InetAddress.getByAddress(destination);
                System.out.println(destinationAddress.getHostAddress());

                byte next[] = new byte[4];
                for (int i = 0; i < 4; i++) {
                    next[i] = input.readByte();
                }
                InetAddress nextHop = InetAddress.getByAddress(next);
                System.out.println(nextHop.getHostAddress());

                System.out.println("Inbound IF: " + input.readUnsignedShort());
                System.out.println("Outbound IF: " + input.readUnsignedShort());
                System.out.println("Packets: " + input.readInt());
                System.out.println("Size: " + input.readInt());
                System.out.println("Start Time: " + input.readInt());
                System.out.println("End Time: " + input.readInt());
                System.out.println("Source Port: " + input.readUnsignedShort());
                System.out.println("Destination Port: " + input.readUnsignedShort());

                input.skipBytes(1); // Padding

                int flags = input.readUnsignedByte();
                System.out.println("Flags: " + flags);
                Boolean ece, cwr, urg, ack, psh, rst, syn, fin;
                ece = cwr = urg = ack = psh = rst = syn = fin = false;
                if (flags >= 32) {
                    urg = true;
                    System.out.print("URG ");
                    flags -= 32;
                }
                if (flags >= 16) {
                    ack = true;
                    System.out.print("ACK ");
                    flags -= 16;
                }
                if (flags >= 8) {
                    psh = true;
                    System.out.print("PSH ");
                    flags -= 8;
                }
                if (flags >= 4) {
                    rst = true;
                    System.out.print("RST ");
                    flags -= 4;
                }
                if (flags >= 2) {
                    syn = true;
                    System.out.print("SYN ");
                    flags -= 2;
                }
                if (flags >= 1) {
                    fin = true;
                    System.out.print("FIN");
                    flags -= 1;
                }

                System.out.print("\n");
                System.out.println("L4 Protocol: " + input.readUnsignedByte());
                System.out.println("ToS: " + input.readUnsignedByte());
                System.out.println("Source AS: " + input.readUnsignedShort());
                System.out.println("Destination AS: " + input.readUnsignedShort());
                System.out.println("Source Mask Bits: " + input.readUnsignedByte());
                System.out.println("Destination Mask Bits: " + input.readUnsignedByte());

                input.skipBytes(2); // Padding
            }

        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private void parseTemplates(Long stream, byte[] templateBytes) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(templateBytes);
        DataInput input = new DataInputStream(bais);

        //System.out.println("Template Length: " + templateBytes.length);

        Boolean complete = false;
        while (!complete) {
            int id = input.readUnsignedShort();
            //System.out.println("Received Template ID: " + id);

            int count = input.readUnsignedShort();
            //System.out.println("Template Fields: " + count);
            Template template = new Template();

            int remainingBytes = templateBytes.length - 8;
            for (int n = 0; n < count; n++) {
                int type = input.readUnsignedShort();
                int length = input.readUnsignedShort();
                //System.out.println("type: " + type + ", length: " + length);
                template.fields.put(type, length);
                remainingBytes -= 4;
            }

            Listener.templates.put(stream.toString() + ":" + id, template);
            //System.out.println("Added Template: " + stream.toString() + ":" + id);
            if (remainingBytes <= 0) {
                complete = true;
            }
        }
    }

    private InetAddress parseAddress(DataInput input, Integer size) throws IOException, UnknownHostException {
        byte[] bytes = new byte[size];
        input.readFully(bytes);
        InetAddress address = InetAddress.getByAddress(bytes);
        return address;
    }

    private long parseNumber(DataInput input, Integer size) throws IOException {
        switch (size) {
            case 4:
                return input.readInt() & 0xffffffffL;
            case 2:
                return input.readUnsignedShort();
            case 1:
                return input.readUnsignedByte();
        }
        return 0;
    }

    private Long parseDate(DataInput input, Integer sysUpTime, long epoch) throws IOException, DatatypeConfigurationException {
        long ms = input.readInt();
        long time = ((epoch * 1000) - sysUpTime + ms);

        return time;
    }

    private int parseData(byte[] dataBytes, Template template, Integer sysUpTime, long epoch) throws IOException {
        //System.out.println("epoch: " + epoch);
        //System.out.println("sysUpTime: " + sysUpTime);
        ByteArrayInputStream bais = new ByteArrayInputStream(dataBytes);
        DataInput input = new DataInputStream(bais);

        Integer iterations = dataBytes.length / template.length();

        int flows = 0;
        for (int n = 0; n < iterations; n++) {
            flows++;
            Long size = 0l;
            Long packets = 0l;

            XmlFlow xnetflow = new XmlFlow();
            for (Entry<Integer, Integer> entry : template.fields.entrySet()) {
                try {
                    switch (entry.getKey()) {
                        case TemplateTypes.IPV4_SRC_ADDR:
                            xnetflow.setSourceAddress(parseAddress(input, entry.getValue()).getHostAddress());
                            break;

                        case TemplateTypes.IPV4_DST_ADDR:
                            xnetflow.setDestinationAddress(parseAddress(input, entry.getValue()).getHostAddress());
                            break;

                        case TemplateTypes.IPV6_SRC_ADDR:
                            xnetflow.setSourceAddress(parseAddress(input, entry.getValue()).getHostAddress());
                            break;

                        case TemplateTypes.IPV6_DST_ADDR:
                            xnetflow.setDestinationAddress(parseAddress(input, entry.getValue()).getHostAddress());
                            break;

                        case TemplateTypes.PROTOCOL:
                            xnetflow.setProtocol(new Integer(input.readUnsignedByte()));
                            break;

                        case TemplateTypes.IP_PROTOCOL_VERSION:
                            xnetflow.setEthernetType("ipv" + input.readUnsignedByte());
                            break;

                        case TemplateTypes.TCP_FLAGS:
                            xnetflow.setFlags(new Integer(input.readUnsignedByte()));
                            break;

                        case TemplateTypes.L4_SRC_PORT:
                            xnetflow.setSourcePort(new Integer(input.readUnsignedShort()));
                            break;

                        case TemplateTypes.L4_DST_PORT:
                            xnetflow.setDestinationPort(new Integer(input.readUnsignedShort()));
                            break;

                        case TemplateTypes.IN_BYTES:
                            long j = parseNumber(input, entry.getValue());
                            //System.out.println("IN_BYTES: " + j);
                            size += j;
                            break;

                        case TemplateTypes.OUT_BYTES:
                            long i = parseNumber(input, entry.getValue());
                            //System.out.println("OUT_BYTES: " + i + "\n");
                            size += i;
                            break;

                        case TemplateTypes.IN_PKTS:
                            packets += parseNumber(input, entry.getValue());
                            break;

                        case TemplateTypes.OUT_PKTS:
                            packets += parseNumber(input, entry.getValue());
                            break;

                        case TemplateTypes.FIRST_SWITCHED:
                            xnetflow.setStartTimeStamp(parseDate(input, sysUpTime, epoch));
                            break;

                        case TemplateTypes.LAST_SWITCHED:
                            xnetflow.setLastTimeStamp(parseDate(input, sysUpTime, epoch));
                            break;

                        default:
                            input.skipBytes(entry.getValue());
                    }
                } catch (DatatypeConfigurationException dte) {
                    dte.printStackTrace();
                }
            }
            xnetflow.setBytesSent(size);
            xnetflow.setPacketsSent(packets.intValue());

            // softflowd 0.9.8 mixes up the time stamps
            if (xnetflow.getStartTimeStamp() > xnetflow.getLastTimeStamp()) {
                Long temp = xnetflow.getStartTimeStamp();
                xnetflow.setStartTimeStamp(xnetflow.getLastTimeStamp());
                xnetflow.setLastTimeStamp(temp);
            }


            flowSet.getFlows().add(xnetflow);
        }

        return flows;
    }
}
