package name.justinthomas.flower.analysis.element;

import java.net.InetAddress;
import java.util.Collection;
import java.util.HashMap;

import name.justinthomas.flower.analysis.services.xmlobjects.XMLNetwork;
import name.justinthomas.flower.analysis.services.xmlobjects.XMLNode;

public class Network {

    static final long serialVersionUID = 1;
    HashMap<String, Node> nodes = new HashMap<String, Node>();
    InetNetwork network = null;

    public Network(InetAddress address, Integer mask, String name) {
        network = new InetNetwork(address, mask, name);
    }

    public Network(InetNetwork network) {
        this.network = network;
    }
    
    public XMLNetwork toXMLNetwork() {
        XMLNetwork xnetwork = new XMLNetwork();
        xnetwork.address = getNetwork().getAddress().getHostAddress();
        xnetwork.mask = getNetwork().getMask();
        xnetwork.name = getNetwork().getName();

        for (Node node : getAllNodes()) {
            XMLNode xnode = new XMLNode();
            xnode.address = node.getAddress().getHostAddress();
            xnode.resolvedAddress = node.getResolvedAddress();

            for (Flow flow : node.getFlowsOriginated()) {
                xnode.flowsOriginated.add(flow.toHashTableFlow());
            }

            for (Flow flow : node.getFlowsReceived()) {
                xnode.flowsReceived.add(flow.toHashTableFlow());
            }

            xnetwork.nodes.add(xnode);
        }

        return xnetwork;
    }

    public Integer flowCount() {
        Integer count = 0;

        for(Node node : nodes.values()) {
            count += node.flowCount();
        }

        return count;
    }

    public Integer size() {
        return nodes.size();
    }

    public InetNetwork getNetwork() {
        return network;
    }

    public void addNode(Node node) {
        if (!nodes.containsKey(node.getAddress().getHostAddress())) {
            nodes.put(node.getAddress().getHostAddress(), node);
        } else {
            Node storedNode = nodes.get(node.getAddress().getHostAddress());
//System.out.println("Node Address: " + node.getAddress().getHostAddress());
            for(Flow flow : node.getFlowsOriginated()) {
                storedNode.addFlow(flow);
            }

            for(Flow flow : node.getFlowsReceived()) {
                storedNode.addFlow(flow);
            }
        }
    }

    public Collection<Node> getAllNodes() {
        Collection<Node> list = nodes.values();

        return list;
    }

    public Node getNodeByAddress(String address) {
        Node storedNode = nodes.get(address);
        return storedNode;
    }
}
