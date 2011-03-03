/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package name.justinthomas.flower.collector;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;

/**
 *
 * @author justin
 */
//@Singleton
//@Startup
public class Collector {
    private static Listener listener;

    //@PostConstruct
    public void init() {
        listener = new Listener();
        listener.listen();
    }
}
