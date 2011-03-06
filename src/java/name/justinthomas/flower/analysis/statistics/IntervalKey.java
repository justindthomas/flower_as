/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package name.justinthomas.flower.analysis.statistics;

import com.sleepycat.persist.model.KeyField;
import com.sleepycat.persist.model.Persistent;

/**
 *
 * @author justin
 */
@Persistent
public class IntervalKey {

    @KeyField(2)
    public Long interval;
    @KeyField(1)
    public Long resolution;

    protected IntervalKey() {
    }

    public IntervalKey(Long interval, Long resolution) {
        this.interval = interval;
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
        if (this.interval != other.interval && (this.interval == null || !this.interval.equals(other.interval))) {
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
        hash = 43 * hash + (this.interval != null ? this.interval.hashCode() : 0);
        hash = 43 * hash + (this.resolution != null ? this.resolution.hashCode() : 0);
        return hash;
    }

    
}
