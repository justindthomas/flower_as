package name.justinthomas.flower.analysis.element;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import name.justinthomas.flower.analysis.persistence.ConfigurationManager;
import name.justinthomas.flower.utility.AddressAnalysis;

public class ManagedNetworks {

    Boolean DEBUG = true;
    LinkedHashMap<String, InetNetwork> subnets = new LinkedHashMap<String, InetNetwork>();
    ConfigurationManager configurationManager;

    public ManagedNetworks() {
        init();
    }

    public void init() {
        try {
            Context context = new InitialContext();
            configurationManager = (ConfigurationManager) context.lookup("java:global/Analysis/ConfigurationManager");
        } catch (NamingException ne) {
            ne.printStackTrace();
        }
    }
    public void addNetwork(String id, InetNetwork network) {
        subnets.put(id, network);
    }

    public LinkedHashMap<String,InetNetwork> getNetworks() {
        if (subnets.size() == 0) {
            populate();
        }
        return subnets;
    }

    public Boolean isManaged(InetAddress address) {
        if (subnets.size() == 0) {
            populate();
        }

        for(InetNetwork network : subnets.values()) {
            if (AddressAnalysis.isMember(address, network)) {
                return true;
            }
        }
        return false;
    }

    private void populate() {
        for(Entry<String, String> entry : configurationManager.getManagedNetworks().entrySet()) {
            //System.out.println("Network ID: " + e.getValue());

            String[] parts = entry.getKey().split("/");
            try {
                addNetwork(entry.getValue(), new InetNetwork(InetAddress.getByName(parts[0].trim()), Integer.valueOf(parts[1].trim()), entry.getValue()));
            } catch (UnknownHostException uhe) {
                System.err.println("Unable to parse address: " + uhe.getMessage());
            }
        }
    }
}
