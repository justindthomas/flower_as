package name.justinthomas.flower.analysis.element;

public class DefaultNode extends Node {

    public DefaultNode() {
        setAddress("0.0.0.0");
    }
/*
    @Override
    public void addFlow(StatisticalFlow statisticalFlow) {
        try {
            if (!statisticalFlow.getTcpBytes().isEmpty()) {
                for (Entry<StatisticalPortTuple, Long> entry : statisticalFlow.getTcpBytes().entrySet()) {
                    this.addFlow(new Flow(6, entry, statisticalFlow));
                }
            }

            if (!statisticalFlow.getUdpBytes().isEmpty()) {
                for (Entry<StatisticalPortTuple, Long> entry : statisticalFlow.getUdpBytes().entrySet()) {
                    this.addFlow(new Flow(17, entry, statisticalFlow));
                }
            }

            if (!statisticalFlow.getIpBytes().isEmpty()) {
                for (Entry<Integer, Long> entry : statisticalFlow.getIpBytes().entrySet()) {
                    if ((entry.getKey() != 6) && (entry.getKey() != 17)) {
                        this.addFlow(new Flow(entry, statisticalFlow));
                    }
                }
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addFlow(Flow flow) {
        Boolean found = false;

        for(Flow storedFlow : flowsOriginated.values()) {
            if (storedFlow.addresses[0].getHostAddress().equals(flow.addresses[0].getHostAddress())
                    && (storedFlow.protocol.equals(flow.protocol)) ) {
                if ((storedFlow.protocol.intValue() == 6) || (storedFlow.protocol.intValue() == 17)) {
                    if (storedFlow.ports[1].equals(flow.ports[1])) {
                        storedFlow.ports[0] = 0;
                    }
                }
                storedFlow.setBytesSent(storedFlow.getBytesSent().add(flow.getBytesSent()));
                storedFlow.setBytesReceived(storedFlow.getBytesReceived().add(flow.getBytesReceived()));
                storedFlow.setPacketsSent(storedFlow.getPacketsSent() + flow.getPacketsSent());
                storedFlow.setPacketsReceived(storedFlow.getPacketsReceived() + flow.getPacketsReceived());
                found = true;
            }
        }

        if(!found) {
            try {
                flowsOriginated.put(new IdentificationTuple(flow), flow);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        found = false;

        for(Flow storedFlow : flowsReceived.values()) {
            if (storedFlow.addresses[1].getHostAddress().equals(flow.addresses[1].getHostAddress())
                    && (storedFlow.protocol.equals(flow.protocol)) ) {
                if ((storedFlow.protocol.intValue() == 6) || (storedFlow.protocol.intValue() == 17)) {
                    if (storedFlow.ports[1].equals(flow.ports[1])) {
                        storedFlow.ports[1] = 0;
                    }
                }
                storedFlow.setBytesSent(storedFlow.getBytesSent().add(flow.getBytesSent()));
                storedFlow.setBytesReceived(storedFlow.getBytesReceived().add(flow.getBytesReceived()));
                storedFlow.setPacketsSent(storedFlow.getPacketsSent() + flow.getPacketsSent());
                storedFlow.setPacketsReceived(storedFlow.getPacketsReceived() + flow.getPacketsReceived());
                found = true;
            }
        }

        if(!found) {
            try {
                flowsOriginated.put(new IdentificationTuple(flow), flow);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    */
}
