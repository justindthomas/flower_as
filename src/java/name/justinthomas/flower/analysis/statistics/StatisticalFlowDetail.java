package name.justinthomas.flower.analysis.statistics;

import com.sleepycat.persist.model.Persistent;

/**
 *
 * @author justin
 */
@Persistent
public class StatisticalFlowDetail {
    public static enum Count {
        PACKET, BYTE;
    }

    public static enum Version {
        IPV6, IPV4;
    }

    protected Count type;
    protected Version version;
    protected Integer protocol;
    protected Integer source;
    protected Integer destination;

    protected StatisticalFlowDetail() { }
    public StatisticalFlowDetail(Count type, Version version, Integer protocol, Integer source, Integer destination) {
        this.type = type;
        this.version = version;
        this.protocol = protocol;
        this.source = source;
        this.destination = destination;
    }
    
    @Override
    public boolean equals(Object obj) {
        if(obj instanceof StatisticalFlowDetail) {
            StatisticalFlowDetail tuple = (StatisticalFlowDetail)obj;
            if(tuple.type.equals(this.type) && tuple.protocol.equals(this.protocol) && tuple.source.equals(this.source) && tuple.destination.equals(this.destination)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 70 * hash + (this.type != null ? this.type.hashCode() : 0);
        hash = 72 * hash + (this.version != null ? this.version.hashCode() : 0);
        hash = 74 * hash + (this.protocol != null ? this.protocol.hashCode() : 0);
        hash = 79 * hash + (this.source != null ? this.source.hashCode() : 0);
        hash = 83 * hash + (this.destination != null ? this.destination.hashCode() : 0);
        return hash;
    }

    public Integer getDestination() {
        return destination;
    }

    public void setDestination(Integer destination) {
        this.destination = destination;
    }

    public Integer getSource() {
        return source;
    }

    public void setSource(Integer source) {
        this.source = source;
    }

    public Integer getProtocol() {
        return protocol;
    }

    public void setProtocol(Integer protocol) {
        this.protocol = protocol;
    }

    public Count getType() {
        return type;
    }

    public void setType(Count type) {
        this.type = type;
    }

    public Version getVersion() {
        return version;
    }

    public void setVersion(Version version) {
        this.version = version;
    }
}
