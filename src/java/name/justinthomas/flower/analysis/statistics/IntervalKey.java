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
}
