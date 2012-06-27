/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package name.justinthomas.flower.analysis.persistence;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.*;
import name.justinthomas.flower.analysis.authentication.UserManager;
import name.justinthomas.flower.global.GlobalConfigurationManager;
import name.justinthomas.flower.manager.services.CustomerAdministration.Customer;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;

/**
 *
 * @author justin
 */
@PersistenceContext(name = "persistence/Analysis", unitName = "AnalysisPU")
public class FrequencyManager {

    private static final Logger log = Logger.getLogger(UserManager.class.getName());
    private static FileAppender fileAppender;
    private Customer customer;
    private static GlobalConfigurationManager globalConfigurationManager;
    private final Map<TransportPairing, Integer> map = new ConcurrentHashMap();
    private EntityManager em;

    public static EntityManager getEntityManager() {
        try {
            return (EntityManager) InitialContext.doLookup("java:comp/env/persistence/Analysis");
        } catch (NamingException e) {
            log.error(e.getMessage());
        }

        return null;
    }

    public FrequencyManager(Customer customer) {
        this.customer = customer;
        setConfiguration();
        em = FrequencyManager.getEntityManager();
        
        if (fileAppender == null) {
            try {
                fileAppender = new FileAppender(new SimpleLayout(), globalConfigurationManager.getBaseDirectory() + "/authentication.log");
                log.addAppender(fileAppender);
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }

        PersistentFrequency stored = loadMap();
        if(stored != null) {
            map.putAll(stored.getFrequencyMap());
        }

        ScheduledThreadPoolExecutor stpe = new ScheduledThreadPoolExecutor(3);
        UpdateDatabase ud = new UpdateDatabase(customer, this);
        stpe.scheduleAtFixedRate(ud, 15, 15, TimeUnit.MINUTES);
    }

    private void setConfiguration() {
        try {
            if (globalConfigurationManager == null) {
                globalConfigurationManager = InitialContext.doLookup("java:global/Analysis/GlobalConfigurationManager");
            }
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    private PersistentFrequency loadMap() {
        System.out.println("Retrieving frequency map from storage.");

        PersistentFrequency pf = null;
        
        List<PersistentFrequency> frequencies = (List<PersistentFrequency>) em.createQuery(
                "SELECT f FROM PersistentFrequency f WHERE f.accountId LIKE :accountid").setParameter("accountid", customer.getAccount()).getResultList();

        if (frequencies.size() > 1) {
            System.err.println("FrequencyManager: too many results");
        } else if (!frequencies.isEmpty()) {
            pf = frequencies.get(0);
        }
        
        return pf;
    }

    public void incrementFrequency(Integer protocol, Integer port) {
        TransportPairing pairing = new TransportPairing(protocol, port);
        if (map.get(pairing) != null) {
            map.put(pairing, map.get(pairing) + 1);
        } else {
            map.put(pairing, 1);
        }
    }

    public Integer getFrequency(Integer protocol, Integer port) {
        TransportPairing pairing = new TransportPairing(protocol, port);

        if (map.get(pairing) == null) {
            return 0;
        }

        return map.get(pairing);
    }

    public void addPort(Integer protocol, Integer port) {
        incrementFrequency(protocol, port);
    }

    public void addPort(Integer protocol, Integer[] port) {
        for (int i = 0; i < port.length; i++) {
            addPort(protocol, port[i]);
        }
    }

    public void pruneMap() {
        for (Entry<TransportPairing, Integer> frequency : map.entrySet()) {
            map.put(frequency.getKey(), new Double(0.1 * frequency.getValue()).intValue());
        }
    }

    public Integer getMapSize() {
        return map.size();
    }

    public Integer getLargestFrequency() {
        Integer i = 0;
        for (Integer frequency : map.values()) {
            if (frequency > i) {
                i = frequency;
            }
        }
        return i;
    }
    
    public class UpdateDatabase implements Runnable {
        Customer customer;
        FrequencyManager frequencyManager;

        public UpdateDatabase(Customer customer, FrequencyManager frequencyManager) {
            this.customer = customer;
            this.frequencyManager = frequencyManager;
        }

        private void saveMap() {
            try {
                Context context = new InitialContext();
                UserTransaction utx = (UserTransaction) context.lookup("java:comp/UserTransaction");

                if (frequencyManager.loadMap() != null) {
                    PersistentFrequency stored = frequencyManager.loadMap();
                    stored.getFrequencyMap().clear();
                    stored.getFrequencyMap().putAll(frequencyManager.map);

                    utx.begin();
                    em.merge(stored);
                    utx.commit();
                } else {
                    PersistentFrequency frequencies = new PersistentFrequency();
                    frequencies.setAccountId(customer.getAccount());
                    frequencies.setFrequencyMap(new HashMap<TransportPairing, Integer>());
                    
                    utx.begin();
                    em.persist(frequencies);
                    utx.commit();
                }
            } catch (NullPointerException npe) {
                npe.printStackTrace();
            } catch (NotSupportedException nse) {
                nse.printStackTrace();
            } catch (RollbackException rbe) {
                rbe.printStackTrace();
            } catch (HeuristicMixedException hme) {
                hme.printStackTrace();
            } catch (HeuristicRollbackException hrbe) {
                hrbe.printStackTrace();
            } catch (SystemException se) {
                se.printStackTrace();
            } catch (NamingException ne) {
                ne.printStackTrace();
            }
        }

        @Override
        public void run() {
            if (frequencyManager.getLargestFrequency() > (0.75 * Integer.MAX_VALUE)) {
                pruneMap();
            }

            saveMap();
        }
    }
}
