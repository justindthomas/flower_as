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
import name.justinthomas.flower.analysis.persistence.FrequencyManager;
import name.justinthomas.flower.analysis.persistence.ManagedNetwork;
import name.justinthomas.flower.analysis.persistence.ManagedNetworkManager;
import name.justinthomas.flower.analysis.authentication.UserManager;
import name.justinthomas.flower.analysis.authentication.User;
import name.justinthomas.flower.analysis.services.xmlobjects.XMLDirectoryDomain;
import name.justinthomas.flower.analysis.services.xmlobjects.XMLDirectoryGroup;
import name.justinthomas.flower.analysis.services.xmlobjects.XMLNetwork;
import name.justinthomas.flower.analysis.statistics.CachedStatistics;
import name.justinthomas.flower.global.GlobalConfigurationManager;

/**
 *
 * @author justin
 */
@WebService()
public class Administration {

    @EJB GlobalConfigurationManager globalConfigurationManager;
    private ManagedNetworkManager managedNetworkManager = new ManagedNetworkManager();
    private DirectoryDomainManager directoryDomainManager = new DirectoryDomainManager();

    @WebMethod(operationName = "addManagedNetwork")
    public Boolean addManagedNetwork(
            @WebParam(name = "user") String user,
            @WebParam(name = "password") String password,
            @WebParam(name = "name") String name,
            @WebParam(name = "address") String address) {

        System.out.println("Attempting to add: " + user + ", " + name + ", " + address);
        UserAction userAction = new UserAction();
        AuthenticationToken token = userAction.authenticate(user, password);
        if (token.authenticated && token.authorized && token.administrator) {
            try {
                return managedNetworkManager.addManagedNetwork(new ManagedNetwork(address, name));
            } catch (UnknownHostException e) {
                System.err.println("Managed network address appears to be malformed.");
                return false;
            }
        }

        return false;
    }

    @WebMethod(operationName = "addDomainGroup")
    public Boolean addDomainGroup(
            @WebParam(name = "user") String user,
            @WebParam(name = "password") String password,
            @WebParam(name = "domain") String domain,
            @WebParam(name = "group") String group,
            @WebParam(name = "privileged") Boolean privileged) {

        UserAction userAction = new UserAction();
        AuthenticationToken token = userAction.authenticate(user, password);
        if (token.authenticated && token.authorized && token.administrator) {
            return directoryDomainManager.addDirectoryDomain(domain, group, privileged);
        }
        
        return false;
    }

    @WebMethod(operationName = "removeManagedNetwork")
    public Boolean removeManagedNetwork(
            @WebParam(name = "user") String user,
            @WebParam(name = "password") String password,
            @WebParam(name = "address") String address) {

        UserAction userAction = new UserAction();
        AuthenticationToken token = userAction.authenticate(user, password);
        if (token.authenticated && token.authorized && token.administrator) {
            return managedNetworkManager.deleteManagedNetwork(address);
        }

        return false;
    }

    @WebMethod(operationName = "removeDomainGroup")
    public Boolean removeDomainGroup(
            @WebParam(name = "user") String user,
            @WebParam(name = "password") String password,
            @WebParam(name = "domain") String domain,
            @WebParam(name = "group") String group) {

        UserAction userAction = new UserAction();
        AuthenticationToken token = userAction.authenticate(user, password);
        if (token.authenticated && token.authorized && token.administrator) {
            directoryDomainManager.removeGroup(domain, group);
            return true;
        }

        return false;
    }

    @WebMethod(operationName = "addUser")
    public Boolean addUser(
            @WebParam(name = "user") String user,
            @WebParam(name = "password") String password,
            @WebParam(name = "updatedUser") String updatedUser,
            @WebParam(name = "updatedPassword") String updatedPassword,
            @WebParam(name = "fullName") String fullName,
            @WebParam(name = "administrator") Boolean administrator,
            @WebParam(name = "timeZone") String timeZone) {

        UserAction userAction = new UserAction();
        AuthenticationToken token = userAction.authenticate(user, password);
        UserManager userManager = new UserManager();

        if (token.authenticated && token.authorized && token.administrator) {
            if (userManager.getUser(updatedUser) == null) {
                if (!userManager.updateUser(updatedUser, updatedPassword, fullName, administrator, timeZone)) {
                    return false;
                }
            } else {
                return false;
            }
        }

        return true;
    }

    @WebMethod(operationName = "privilegedUpdateUser")
    public Boolean privilegedUpdateUser(
            @WebParam(name = "user") String user,
            @WebParam(name = "password") String password,
            @WebParam(name = "updatedUser") String updatedUser,
            @WebParam(name = "updatedPassword") String updatedPassword,
            @WebParam(name = "fullName") String fullName,
            @WebParam(name = "administrator") Boolean administrator,
            @WebParam(name = "timeZone") String timeZone) {

        UserAction userAction = new UserAction();
        AuthenticationToken token = userAction.authenticate(user, password);

        UserManager userManager = new UserManager();
        if (token.authenticated && token.authorized && token.administrator && (userManager.getUser(updatedUser) != null)) {
            if (!userManager.updateUser(updatedUser, updatedPassword, fullName, administrator, timeZone)) {
                return false;
            }
        } else {
            return false;
        }

        return true;

    }

    @WebMethod(operationName = "deleteUser")
    public Boolean deleteUser(
            @WebParam(name = "user") String user,
            @WebParam(name = "password") String password,
            @WebParam(name = "deletedUser") String deletedUser) {

        UserAction userAction = new UserAction();
        AuthenticationToken token = userAction.authenticate(user, password);

        UserManager userManager = new UserManager();
        if (token.authenticated && token.authorized && token.administrator) {
            return userManager.deleteUser(deletedUser);
        }

        return false;
    }

    @WebMethod(operationName = "getUsers")
    public List<User> getUsers(
            @WebParam(name = "user") String user,
            @WebParam(name = "password") String password) {

        List<User> xusers = new ArrayList();
        UserAction userAction = new UserAction();
        AuthenticationToken token = userAction.authenticate(user, password);

        UserManager userManager = new UserManager();
        if (token.authenticated && token.authorized && token.administrator) {
            for (User puser : userManager.getUsers()) {
                xusers.add(puser.sanitize());
            }
        }

        return xusers;
    }

    @WebMethod(operationName = "getDirectoryDomains")
    public List<XMLDirectoryDomain> getDirectoryDomains(
            @WebParam(name = "user") String user,
            @WebParam(name = "password") String password) {

        List<XMLDirectoryDomain> xdomains = new ArrayList();
        UserAction userAction = new UserAction();
        AuthenticationToken token = userAction.authenticate(user, password);
        if (token.authenticated && token.authorized && token.administrator) {
            for (DirectoryDomain directoryDomain : directoryDomainManager.getDirectoryDomains()) {
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

    @WebMethod(operationName = "getManagedNetworks")
    public List<XMLNetwork> getManagedNetworks(
            @WebParam(name = "user") String user,
            @WebParam(name = "password") String password) {

        List<XMLNetwork> xnetworks = new ArrayList<XMLNetwork>();

        UserAction userAction = new UserAction();
        AuthenticationToken token = userAction.authenticate(user, password);
        if (token.authenticated && token.authorized && token.administrator) {
            for (ManagedNetwork network : managedNetworkManager.getManagedNetworks()) {
                XMLNetwork xnetwork = new XMLNetwork();
                xnetwork.name = network.description;
                String cidr = network.address;
                String[] cidrParts = cidr.split("/");
                xnetwork.address = cidrParts[0];
                xnetwork.mask = Integer.parseInt(cidrParts[1].trim());

                xnetworks.add(xnetwork);
            }
        }
        return xnetworks;
    }

    @WebMethod(operationName = "getOperatingStatistics")
    public OperatingStatistics getStatistics(
            @WebParam(name = "user") String user,
            @WebParam(name = "password") String password,
            @WebParam(name = "customer") String customerID) {

        OperatingStatistics stats = new OperatingStatistics();

        UserAction userAction = new UserAction();
        AuthenticationToken token = userAction.authenticate(user, password);
        if (token.authenticated && token.authorized && token.administrator) {
            stats.insertionCacheSize = globalConfigurationManager.getCachedStatistics(customerID).getCacheSize();
            stats.frequencyMapSize = FrequencyManager.getFrequencyManager().getMapSize();
            stats.largestFrequency = FrequencyManager.getFrequencyManager().getLargestFrequency();
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
