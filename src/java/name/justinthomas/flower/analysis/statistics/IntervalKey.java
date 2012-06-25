/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package name.justinthomas.flower.analysis.statistics;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 *
 * @author justin
 */
@Embeddable
public class IntervalKey implements Serializable {

    @Column(name = "STATISTICALINTERVAL", nullable = false)
    private Long statisticalInterval;
    @Column(name = "RESOLUTION", nullable = false)
    private Long resolution;

    protected IntervalKey() {
    }

    public IntervalKey(Long statisticalInterval, Long resolution) {
        this.statisticalInterval = statisticalInterval;
        this.resolution = resolution;
    }

    public Long getStatisticalInterval() {
        return statisticalInterval;
    }

    public void setStatisticalInterval(Long statisticalInterval) {
        this.statisticalInterval = statisticalInterval;
    }

    public Long getResolution() {
        return resolution;
    }

    public void setResolution(Long resolution) {
        this.resolution = resolution;
    }
    
     @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final IntervalKey other = (IntervalKey) obj;
        if (this.statisticalInterval != other.statisticalInterval && (this.statisticalInterval == null || !this.statisticalInterval.equals(other.statisticalInterval))) {
            return false;
        }
        if (this.resolution != other.resolution && (this.resolution == null || !this.resolution.equals(other.resolution))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 43 * hash + (this.statisticalInterval != null ? this.statisticalInterval.hashCode() : 0);
        hash = 43 * hash + (this.resolution != null ? this.resolution.hashCode() : 0);
        return hash;
    }
}
