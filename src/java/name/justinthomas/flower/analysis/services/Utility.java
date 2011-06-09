/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package name.justinthomas.flower.analysis.services;

import java.net.MalformedURLException;
import java.net.URL;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import name.justinthomas.flower.global.GlobalConfigurationManager;
import name.justinthomas.flower.manager.services.CustomerAdministration.Customer;
import name.justinthomas.flower.manager.services.CustomerAdministration.CustomerAdministration;
import name.justinthomas.flower.manager.services.CustomerAdministration.CustomerAdministrationService;

/**
 *
 * @author justin
 */
public class Utility {

    private static GlobalConfigurationManager globalConfigurationManager;

    public static Customer getCustomer(String customerID) {
        Customer customer = null;

        try {
            CustomerAdministrationService admin = new CustomerAdministrationService(new URL(Utility.getGlobalConfigurationManager().getManager() + "/CustomerAdministrationService?wsdl"));

            CustomerAdministration port = admin.getCustomerAdministrationPort();
            customer = port.getCustomer(null, null, customerID);
        } catch (MalformedURLException e) {
            System.err.println("Could not access Customer Administration service at: " + Utility.getGlobalConfigurationManager().getManager());
        }

        return customer;
    }

    public static GlobalConfigurationManager getGlobalConfigurationManager() {
        if (globalConfigurationManager == null) {
            try {
                globalConfigurationManager = (GlobalConfigurationManager) InitialContext.doLookup("java:global/Analysis/GlobalConfigurationManager");
            } catch (NamingException e) {
                e.printStackTrace();
            }
        }

        return globalConfigurationManager;
    }
}
