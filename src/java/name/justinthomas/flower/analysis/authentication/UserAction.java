package name.justinthomas.flower.analysis.authentication;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import javax.ejb.EJB;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import name.justinthomas.flower.global.GlobalConfigurationManager;
import name.justinthomas.flower.manager.services.CustomerAdministration.Customer;
import name.justinthomas.flower.manager.services.CustomerAdministration.CustomerAdministration;
import name.justinthomas.flower.manager.services.CustomerAdministration.CustomerAdministrationService;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 *
 * @author justin
 */
@WebService()
public class UserAction {
    private static final Logger log = Logger.getLogger(UserAction.class.getName());
    private static FileAppender fileAppender;
    private static GlobalConfigurationManager globalConfigurationManager;
    
    public UserAction() {
        if (fileAppender == null) {
            try {
                String pattern = "%d{dd MMM yyyy HH:mm:ss.SSS} - %p - %m %n";
                PatternLayout layout = new PatternLayout(pattern);
                fileAppender = new FileAppender(layout, this.getGlobalConfigurationManager().getBaseDirectory() + "/authentication.log");
                log.addAppender(fileAppender);
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }
    }

    /**
     * Web service operation
     */
    @WebMethod(operationName = "authenticate")
    public AuthenticationToken authenticate(
            @WebParam(name = "customer") String customerID,
            @WebParam(name = "username") String username,
            @WebParam(name = "password") String password) {

        AuthenticationToken token = internal(customerID, username, password);
        if (token.authorized) {
            return token;
        }

        return external(getCustomer(customerID), username, password);
    }

    private AuthenticationToken external(Customer customer, String username, String password) {
        AuthenticationToken token = new AuthenticationToken();
        if ((username != null) && (password != null)) {
            ActiveDirectory activeDirectory = new ActiveDirectory(customer, username, password);
            token = activeDirectory.authenticate();
        }

        if (token.authenticated) {
            log.debug("Successfully authenticated against Active Directory");
        }

        if (token.authorized) {
            log.debug("Successfully authorized against Active Directory");
        }

        return token;
    }

    private AuthenticationToken internal(String customerID, String username, String password) {
        AuthenticationToken token = new AuthenticationToken();
        token.internal = true;

        Customer customer = null;

        try {
            CustomerAdministrationService admin = new CustomerAdministrationService(new URL(this.getGlobalConfigurationManager().getManager() + "/CustomerAdministrationService?wsdl"));

            CustomerAdministration port = admin.getCustomerAdministrationPort();
            customer = port.getCustomer(null, null, customerID);
        } catch (MalformedURLException e) {
            log.error("Could not access Customer Administration service at: " + this.getGlobalConfigurationManager().getManager());
            return token;
        }

        if (customer != null) {
            UserManager userManager = new UserManager(customer);

            if ((username != null) && (password != null) && (userManager != null)) {
                FlowerUser user = userManager.getUser(username);
                if (user == null) {
                    log.debug("Invalid User: " + username);
                    return token;
                }

                Security.addProvider(new BouncyCastleProvider());
                MessageDigest hash = null;

                try {
                    hash = MessageDigest.getInstance("SHA-256", "BC");
                    hash.update(password.getBytes());
                } catch (NoSuchAlgorithmException nsae) {
                    nsae.printStackTrace();
                } catch (NoSuchProviderException nspe) {
                    nspe.printStackTrace();
                }


                if (user.getHashedPassword().equals(new String(hash.digest()))) {
                    token.authenticated = true;
                    token.authorized = true;
                    if (user.getAdministrator()) {
                        token.administrator = true;
                    }
                    token.fullName = user.getFullName();
                    token.timeZone = user.getTimeZone();
                    log.debug("Authenticated successfully against internal database: " + username);
                    return token;
                }
            }
        }
        log.debug("Failed Authentication: " + username);
        return token;
    }

    @WebMethod(operationName = "administrator")
    public Boolean administrator(
            @WebParam(name = "customer") String customerID,
            @WebParam(name = "username") String username,
            @WebParam(name = "password") String password) {

        AuthenticationToken token = authenticate(customerID, username, password);
        if (token.authorized) {
            return token.administrator;
        }

        return false;
    }

    @WebMethod(operationName = "getFullName")
    public String getFullName(
            @WebParam(name = "customer") String customerID,
            @WebParam(name = "username") String username,
            @WebParam(name = "password") String password) {

        AuthenticationToken token = authenticate(customerID, username, password);
        if (token.authorized) {
            return token.fullName;
        }

        return null;
    }

    @WebMethod(operationName = "getTimeZone")
    public String getTimeZone(
            @WebParam(name = "customer") String customerID,
            @WebParam(name = "username") String username,
            @WebParam(name = "password") String password) {

        AuthenticationToken token = authenticate(customerID, username, password);
        if (token.authorized) {
            return token.timeZone;
        }

        return null;
    }

    @WebMethod(operationName = "updateUser")
    public Boolean updateUser(
            @WebParam(name = "customer") String customerID,
            @WebParam(name = "authUser") String authUser,
            @WebParam(name = "authPassword") String authPassword,
            @WebParam(name = "updatedPassword") String updatedPassword,
            @WebParam(name = "fullName") String fullName,
            @WebParam(name = "timeZone") String timeZone) {

        AuthenticationToken token = authenticate(customerID, authUser, authPassword);

        Customer customer = null;

        try {
            CustomerAdministrationService admin = new CustomerAdministrationService(new URL(this.getGlobalConfigurationManager().getManager() + "/CustomerAdministrationService?wsdl"));

            CustomerAdministration port = admin.getCustomerAdministrationPort();
            customer = port.getCustomer(null, null, customerID);
        } catch (MalformedURLException e) {
            log.error("Could not access Customer Administration service at: " + this.getGlobalConfigurationManager().getManager());
            return false;
        }

        if (customer != null) {
            UserManager userManager = new UserManager(customer);
            FlowerUser user = userManager.getUser(authUser);
            if (token.authorized && token.internal && (userManager.getUser(authUser) != null)) {
                if (!userManager.updateUser(authUser, updatedPassword, fullName, user.getAdministrator(), timeZone)) {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }

        return true;
    }
    
    private Customer getCustomer(String customerID) {
        Customer customer = null;

        try {
            CustomerAdministrationService admin = new CustomerAdministrationService(new URL(this.getGlobalConfigurationManager().getManager() + "/CustomerAdministrationService?wsdl"));

            CustomerAdministration port = admin.getCustomerAdministrationPort();
            customer = port.getCustomer(null, null, customerID);
        } catch (MalformedURLException e) {
            log.error("Could not access Customer Administration service at: " + this.getGlobalConfigurationManager().getManager());
            return null;
        }

        return customer;
    }
    
    private GlobalConfigurationManager getGlobalConfigurationManager() {
        if(globalConfigurationManager == null) {
            try {
                globalConfigurationManager = (GlobalConfigurationManager) InitialContext.doLookup("java:global/Analysis/GlobalConfigurationManager");
            } catch (NamingException e) {
                log.error("Could not locate GlobalConfigurationManager: " + e.getExplanation());
            }
        }
        
        return globalConfigurationManager;
    }
}
