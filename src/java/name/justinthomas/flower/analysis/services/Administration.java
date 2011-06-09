package name.justinthomas.flower.analysis.services;

import name.justinthomas.flower.analysis.authentication.UserAction;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import javax.ejb.EJB;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import name.justinthomas.flower.analysis.authentication.AuthenticationToken;
import name.justinthomas.flower.analysis.authentication.DirectoryDomain;
import name.justinthomas.flower.analysis.authentication.DirectoryDomainManager;
import name.justinthomas.flower.analysis.persistence.ManagedNetwork;
import name.justinthomas.flower.analysis.persistence.ManagedNetworkManager;
import name.justinthomas.flower.analysis.authentication.UserManager;
import name.justinthomas.flower.analysis.authentication.User;
import name.justinthomas.flower.analysis.services.xmlobjects.XMLDirectoryDomain;
import name.justinthomas.flower.analysis.services.xmlobjects.XMLDirectoryGroup;
import name.justinthomas.flower.global.GlobalConfigurationManager;
import name.justinthomas.flower.manager.services.CustomerAdministration.Customer;

/**
 *
 * @author justin
 */
@WebService()
public class Administration {

    @EJB
    GlobalConfigurationManager globalConfigurationManager;
    
    @WebMethod(operationName = "addManagedNetwork")
    public Boolean addManagedNetwork(
            @WebParam(name = "customer") String customerID,
            @WebParam(name = "user") String user,
            @WebParam(name = "password") String password,
            @WebParam(name = "name") String name,
            @WebParam(name = "address") String address) {

        System.out.println("Attempting to add: " + user + ", " + name + ", " + address);
        UserAction userAction = new UserAction();
        AuthenticationToken token = userAction.authenticate(customerID, user, password);
        if (token.authenticated && token.authorized && token.administrator) {
            try {
                return new ManagedNetworkManager(Utility.getCustomer(customerID)).addManagedNetwork(new ManagedNetwork(address, name));
            } catch (UnknownHostException e) {
                System.err.println("Managed network address appears to be malformed.");
                return false;
            }
        }

        return false;
    }

    @WebMethod(operationName = "addDomainGroup")
    public Boolean addDomainGroup(
            @WebParam(name = "customer") String customerID,
            @WebParam(name = "user") String user,
            @WebParam(name = "password") String password,
            @WebParam(name = "domain") String domain,
            @WebParam(name = "group") String group,
            @WebParam(name = "privileged") Boolean privileged) {

        UserAction userAction = new UserAction();
        AuthenticationToken token = userAction.authenticate(customerID, user, password);
        if (token.authenticated && token.authorized && token.administrator) {
            return new DirectoryDomainManager(Utility.getCustomer(customerID)).addDirectoryDomain(domain, group, privileged);
        }

        return false;
    }

    @WebMethod(operationName = "removeManagedNetwork")
    public Boolean removeManagedNetwork(
            @WebParam(name = "customer") String customerID,
            @WebParam(name = "user") String user,
            @WebParam(name = "password") String password,
            @WebParam(name = "address") String address) {

        UserAction userAction = new UserAction();
        AuthenticationToken token = userAction.authenticate(customerID, user, password);
        if (token.authenticated && token.authorized && token.administrator) {
            return new ManagedNetworkManager(Utility.getCustomer(customerID)).deleteManagedNetwork(address);
        }

        return false;
    }

    @WebMethod(operationName = "removeDomainGroup")
    public Boolean removeDomainGroup(
            @WebParam(name = "customer") String customerID,
            @WebParam(name = "user") String user,
            @WebParam(name = "password") String password,
            @WebParam(name = "domain") String domain,
            @WebParam(name = "group") String group) {

        UserAction userAction = new UserAction();
        AuthenticationToken token = userAction.authenticate(customerID, user, password);
        if (token.authenticated && token.authorized && token.administrator) {
            new DirectoryDomainManager(Utility.getCustomer(customerID)).removeGroup(domain, group);
            return true;
        }

        return false;
    }

    @WebMethod(operationName = "addUser")
    public Boolean addUser(
            @WebParam(name = "customer") String customerID,
            @WebParam(name = "user") String user,
            @WebParam(name = "password") String password,
            @WebParam(name = "updatedUser") String updatedUser,
            @WebParam(name = "updatedPassword") String updatedPassword,
            @WebParam(name = "fullName") String fullName,
            @WebParam(name = "administrator") Boolean administrator,
            @WebParam(name = "timeZone") String timeZone) {

        UserAction userAction = new UserAction();
        AuthenticationToken token = userAction.authenticate(customerID, user, password);

        Customer customer = Utility.getCustomer(customerID);

        if (customer != null) {
            UserManager userManager = new UserManager(customer);

            if (token.authenticated && token.authorized && token.administrator) {
                if (userManager.getUser(updatedUser) == null) {
                    if (!userManager.updateUser(updatedUser, updatedPassword, fullName, administrator, timeZone)) {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        } else {
            return false;
        }

        return true;
    }

    @WebMethod(operationName = "privilegedUpdateUser")
    public Boolean privilegedUpdateUser(
            @WebParam(name = "customer") String customerID,
            @WebParam(name = "user") String user,
            @WebParam(name = "password") String password,
            @WebParam(name = "updatedUser") String updatedUser,
            @WebParam(name = "updatedPassword") String updatedPassword,
            @WebParam(name = "fullName") String fullName,
            @WebParam(name = "administrator") Boolean administrator,
            @WebParam(name = "timeZone") String timeZone) {

        UserAction userAction = new UserAction();
        AuthenticationToken token = userAction.authenticate(customerID, user, password);

        Customer customer = Utility.getCustomer(customerID);

        if (customer != null) {
            UserManager userManager = new UserManager(customer);
            if (token.authenticated && token.authorized && token.administrator && (userManager.getUser(updatedUser) != null)) {
                if (!userManager.updateUser(updatedUser, updatedPassword, fullName, administrator, timeZone)) {
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

    @WebMethod(operationName = "deleteUser")
    public Boolean deleteUser(
            @WebParam(name = "customer") String customerID,
            @WebParam(name = "user") String user,
            @WebParam(name = "password") String password,
            @WebParam(name = "deletedUser") String deletedUser) {

        UserAction userAction = new UserAction();
        AuthenticationToken token = userAction.authenticate(customerID, user, password);

        Customer customer = Utility.getCustomer(customerID);

        if (customer != null) {
            UserManager userManager = new UserManager(customer);
            if (token.authenticated && token.authorized && token.administrator) {
                return userManager.deleteUser(deletedUser);
            }
        }

        return false;
    }

    @WebMethod(operationName = "getUsers")
    public List<User> getUsers(
            @WebParam(name = "customer") String customerID,
            @WebParam(name = "user") String user,
            @WebParam(name = "password") String password) {

        List<User> xusers = new ArrayList();
        UserAction userAction = new UserAction();
        AuthenticationToken token = userAction.authenticate(customerID, user, password);

        Customer customer = Utility.getCustomer(customerID);

        if (customer != null) {
            UserManager userManager = new UserManager(customer);
            if (token.authenticated && token.authorized && token.administrator) {
                for (User puser : userManager.getUsers()) {
                    xusers.add(puser.sanitize());
                }
            }
        }

        return xusers;
    }

    @WebMethod(operationName = "getDirectoryDomains")
    public List<XMLDirectoryDomain> getDirectoryDomains(
            @WebParam(name = "customer") String customerID,
            @WebParam(name = "user") String user,
            @WebParam(name = "password") String password) {

        List<XMLDirectoryDomain> xdomains = new ArrayList();
        UserAction userAction = new UserAction();
        AuthenticationToken token = userAction.authenticate(customerID, user, password);
        if (token.authenticated && token.authorized && token.administrator) {
            for (DirectoryDomain directoryDomain : new DirectoryDomainManager(Utility.getCustomer(customerID)).getDirectoryDomains()) {
                XMLDirectoryDomain xdomain = new XMLDirectoryDomain();
                xdomain.domain = directoryDomain.domain;
                for (Entry<String, Boolean> group : directoryDomain.groups.entrySet()) {
                    XMLDirectoryGroup xgroup = new XMLDirectoryGroup();
                    xgroup.name = group.getKey();
                    xgroup.privileged = group.getValue();
                    xdomain.groups.add(xgroup);
                }
                xdomains.add(xdomain);
            }
        }
        return xdomains;
    }
    
    @WebMethod(operationName = "getOperatingStatistics")
    public OperatingStatistics getStatistics(
            @WebParam(name = "customer") String customerID,
            @WebParam(name = "user") String user,
            @WebParam(name = "password") String password) {

        OperatingStatistics stats = new OperatingStatistics();

        Customer customer = Utility.getCustomer(customerID);

        UserAction userAction = new UserAction();
        AuthenticationToken token = userAction.authenticate(customerID, user, password);
        if (token.authenticated && token.authorized && token.administrator) {
            stats.insertionCacheSize = globalConfigurationManager.getCachedStatistics(customerID).getCacheSize();
            stats.frequencyMapSize = globalConfigurationManager.getFrequencyManager(customer).getMapSize();
            stats.largestFrequency = globalConfigurationManager.getFrequencyManager(customer).getLargestFrequency();
            stats.representationMap = globalConfigurationManager.getCachedStatistics(customerID).getRepresentationMapSize();
        }

        return stats;

    }

    @XmlType
    public static class OperatingStatistics {

        @XmlElement
        public Integer insertionCacheSize;
        @XmlElement
        public Integer frequencyMapSize;
        @XmlElement
        public Integer largestFrequency;
        @XmlElement
        public Integer representationMap;
    }
}
