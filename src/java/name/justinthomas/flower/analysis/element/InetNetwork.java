package name.justinthomas.flower.analysis.element;

import java.io.Serializable;
import java.net.InetAddress;

public class InetNetwork implements Serializable {

    static final long serialVersionUID = 1;
    private InetAddress networkAddress;
    private Integer networkMask;
    private String name = null;

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public InetNetwork() { }

    public InetNetwork(InetAddress address, Integer mask) {
        networkAddress = address;
        networkMask = mask;
    }

    public InetNetwork(InetAddress address, Integer mask, String name) {
        networkAddress = address;
        networkMask = mask;
        this.name = name;
    }

    public void setAddress(InetAddress address) {
        if (address != null) {
            networkAddress = address;
        }
    }

    public InetAddress getAddress() {
        return networkAddress;
    }

    @Override
    public String toString() {
        return networkAddress.getHostAddress() + "/" + networkMask;
    }

    public void setMask(Integer mask) {
        if (mask != null) {
            networkMask = mask;
        }
    }

    public Integer getMask() {
        return networkMask;
    }

    public Boolean isDefault() {
        if (networkAddress.getHostAddress().toString().equals("0.0.0.0")) {
            return true;
        }
        return false;
    }
}
