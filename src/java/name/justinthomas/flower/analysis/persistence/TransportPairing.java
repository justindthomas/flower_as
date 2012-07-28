package name.justinthomas.flower.analysis.persistence;

import java.io.Serializable;

/**
 *
 * @author justin
 */
public class TransportPairing implements Serializable {
    public Integer protocol;
    public Integer port;

    public TransportPairing(Integer protocol, Integer port) {
        this.protocol = protocol;
        this.port = port;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TransportPairing other = (TransportPairing) obj;
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
        hash = 11 * hash + (this.protocol != null ? this.protocol.hashCode() : 0);
        hash = 11 * hash + (this.port != null ? this.port.hashCode() : 0);
        return hash;
    }
}
