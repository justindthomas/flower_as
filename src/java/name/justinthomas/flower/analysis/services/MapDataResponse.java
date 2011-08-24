/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package name.justinthomas.flower.analysis.services;

import java.util.HashSet;
import java.util.Set;
import javax.xml.bind.annotation.XmlType;

/**
 *
 * @author justin
 */
@XmlType
public class MapDataResponse {
    public Boolean ready = false;
    public String untracked;
    public Set<Network> networks;
    public Set<Flow> flows;
    
    public MapDataResponse() {
        
    }
    
    public MapDataResponse(String untracked) {
        this.untracked = untracked;
        this.networks = new HashSet();
        this.flows = new HashSet();
    }
    
    public void addFlow(Flow flow) {
        if(flows.contains(flow)) {
            for(Flow storedFlow : flows) {
                if(storedFlow.equals(flow)) {
                    storedFlow.bytesReceived = String.valueOf(Long.parseLong(storedFlow.bytesReceived) + Long.parseLong(flow.bytesReceived));
                    storedFlow.bytesSent = String.valueOf(Long.parseLong(storedFlow.bytesSent) + Long.parseLong(flow.bytesSent));
                }
            }
        } else {
            flows.add(flow);
        }
    }
    
    public static class Node {
        public String address;
    
        public Node() {
            
        }
        
        public Node(String address) {
            this.address = address;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Node other = (Node) obj;
            if ((this.address == null) ? (other.address != null) : !this.address.equals(other.address)) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 43 * hash + (this.address != null ? this.address.hashCode() : 0);
            return hash;
        }
    }
    
    public static class Flow {
        public String sourceAddress;
        public String destinationAddress;
        public String protocol;
        public String port;
        public String bytesSent;
        public String bytesReceived;
        
        public Flow() {
            
        }
        
        public Flow(String sourceAddress, String destinationAddress, String protocol, String port, String bytesSent, String bytesReceived) {
            this.sourceAddress = sourceAddress;
            this.destinationAddress = destinationAddress;
            this.protocol = protocol;
            this.port = port;
            this.bytesReceived = bytesReceived;
            this.bytesSent = bytesSent;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Flow other = (Flow) obj;
            if ((this.sourceAddress == null) ? (other.sourceAddress != null) : !this.sourceAddress.equals(other.sourceAddress)) {
                return false;
            }
            if ((this.destinationAddress == null) ? (other.destinationAddress != null) : !this.destinationAddress.equals(other.destinationAddress)) {
                return false;
            }
            if ((this.protocol == null) ? (other.protocol != null) : !this.protocol.equals(other.protocol)) {
                return false;
            }
            if ((this.port == null) ? (other.port != null) : !this.port.equals(other.port)) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 29 * hash + (this.sourceAddress != null ? this.sourceAddress.hashCode() : 0);
            hash = 29 * hash + (this.destinationAddress != null ? this.destinationAddress.hashCode() : 0);
            hash = 29 * hash + (this.protocol != null ? this.protocol.hashCode() : 0);
            hash = 29 * hash + (this.port != null ? this.port.hashCode() : 0);
            return hash;
        }
    }
    
    public static class Network {
        public String name;
        public String version;
        public String address;
        public Set<Node> nodes;
        
        public Network() {
            
        }
        
        public Network(String name, String version, String address) {
            this.name = name;
            this.version = version;
            this.address = address;
            this.nodes = new HashSet();
        }
        
    }
}
