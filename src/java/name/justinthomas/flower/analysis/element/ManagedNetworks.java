package name.justinthomas.flower.analysis.element;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedHashMap;
import name.justinthomas.flower.analysis.persistence.ManagedNetwork;
import name.justinthomas.flower.analysis.persistence.ManagedNetworkManager;
import name.justinthomas.flower.manager.services.CustomerAdministration.Customer;
import name.justinthomas.flower.utility.AddressAnalysis;

public class ManagedNetworks {

    private Customer customer;
    Boolean DEBUG = true;
    LinkedHashMap<String, InetNetwork> subnets = new LinkedHashMap();

    public ManagedNetworks(Customer customer) {
        this.customer = customer;
    }
    
    public void addNetwork(String id, InetNetwork network) {
        subnets.put(id, network);
    }

    public LinkedHashMap<String, InetNetwork> getNetworks() {
        if (subnets.size() == 0) {
            populate();
        }
        return subnets;
    }

    public Boolean isManaged(InetAddress address) {
        if (subnets.size() == 0) {
            populate();
        }

        for (InetNetwork network : subnets.values()) {
            if (AddressAnalysis.isMember(address, network)) {
                return true;
            }
        }
        return false;
    }

    private void populate() {
        ManagedNetworkManager mnm = new ManagedNetworkManager(customer);
        for (ManagedNetwork network : mnm.getManagedNetworks()) {
            //System.out.println("Network ID: " + e.getValue());

            String[] parts = network.address.split("/");
            try {
                addNetwork(network.description, new InetNetwork(InetAddress.getByName(parts[0].trim()), Integer.valueOf(parts[1].trim()), network.description));
            } catch (UnknownHostException uhe) {
                System.err.println("Unable to parse address: " + uhe.getMessage());
            }
        }
    }
}
