/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package name.justinthomas.flower.analysis.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.ws.WebServiceContext;
import name.justinthomas.flower.analysis.authentication.AuthenticationToken;
import name.justinthomas.flower.analysis.persistence.ConfigurationManager;
import name.justinthomas.flower.analysis.persistence.UserManager;
import name.justinthomas.flower.analysis.persistence.PersistentUser;
import name.justinthomas.flower.analysis.services.xmlobjects.XMLDirectoryDomain;
import name.justinthomas.flower.analysis.services.xmlobjects.XMLDirectoryGroup;
import name.justinthomas.flower.analysis.services.xmlobjects.XMLNetwork;
import name.justinthomas.flower.analysis.services.xmlobjects.XMLUser;

/**
 *
 * @author justin
 */
@WebService()
public class Administration {

    @Resource
    private WebServiceContext serviceContext;
    @EJB
    ConfigurationManager configurationManager;
    @EJB
    UserManager userManager;

    @WebMethod(operationName = "addNetwork")
    public Boolean addNetwork(
            @WebParam(name = "authUser") String authUser,
            @WebParam(name = "authPassword") String authPassword,
            @WebParam(name = "name") String name,
            @WebParam(name = "address") String address) {

        UserAction userAction = new UserAction();
        AuthenticationToken token = userAction.authenticate(authUser, authPassword);
        if (token.authenticated && token.authorized && token.administrator) {
            if (!configurationManager.getManagedNetworks().containsKey(address)) {
                configurationManager.getManagedNetworks().put(address, name);
                configurationManager.getConfiguration().managedNetworks = configurationManager.getManagedNetworks();
                configurationManager.updateConfiguration(configurationManager.getConfiguration(), false);
                return true;
            }
        }

        return false;
    }

    @WebMethod(operationName = "addGroup")
    public Boolean addGroup(
            @WebParam(name = "username") String username,
            @WebParam(name = "password") String password,
            @WebParam(name = "domain") String domain,
            @WebParam(name = "group") String group,
            @WebParam(name = "privileged") String privileged) {

        UserAction userAction = new UserAction();
        AuthenticationToken token = userAction.authenticate(username, password);
        if(token.authenticated && token.authorized && token.administrator) {
            if(!configurationManager.getDirectoryDomains().containsKey(domain)) {
                HashMap<String, Boolean> groups = new HashMap();
                groups.put(group, Boolean.parseBoolean(privileged));
                configurationManager.getDirectoryDomains().put(domain, groups);
                configurationManager.updateConfiguration(configurationManager.getConfiguration(), false);
                return true;
            } else {
                HashMap<String, Boolean> groups = configurationManager.getDirectoryDomains().get(domain);
                groups.put(group, Boolean.parseBoolean(privileged));
                configurationManager.getDirectoryDomains().put(domain, groups);
                configurationManager.updateConfiguration(configurationManager.getConfiguration(), false);
                return true;
            }
        }
        return false;
    }

    @WebMethod(operationName = "removeNetwork")
    public Boolean removeNetwork(
            @WebParam(name = "authUser") String authUser,
            @WebParam(name = "authPassword") String authPassword,
            @WebParam(name = "address") String address) {

        UserAction userAction = new UserAction();
        AuthenticationToken token = userAction.authenticate(authUser, authPassword);
        if (token.authenticated && token.authorized && token.administrator) {
            if (configurationManager.getManagedNetworks().containsKey(address)) {
                configurationManager.getManagedNetworks().remove(address);
                configurationManager.getConfiguration().managedNetworks = configurationManager.getManagedNetworks();
                configurationManager.updateConfiguration(configurationManager.getConfiguration(), false);
                return true;
            }
        }

        return false;
    }

    @WebMethod(operationName = "removeGroup")
    public Boolean removeGroup(
            @WebParam(name = "username") String username,
            @WebParam(name = "password") String password,
            @WebParam(name = "domain") String domain,
            @WebParam(name = "group") String group) {

        UserAction userAction = new UserAction();
        AuthenticationToken token = userAction.authenticate(username, password);
        if (token.authenticated && token.authorized && token.administrator) {
            if (configurationManager.getDirectoryDomains().containsKey(domain)) {
                if(configurationManager.getDirectoryDomains().get(domain).containsKey(group)) {
                    configurationManager.getDirectoryDomains().get(domain).remove(group);
                }

                if(configurationManager.getDirectoryDomains().get(domain).isEmpty()) {
                    configurationManager.getDirectoryDomains().remove(domain);
                }

                configurationManager.getConfiguration().directoryDomains = configurationManager.getDirectoryDomains();
                configurationManager.updateConfiguration(configurationManager.getConfiguration(), false);
                return true;
            }
        }

        return false;
    }

    /**
     * Web service operation
     */
    @WebMethod(operationName = "addUser")
    public Boolean addUser(
            @WebParam(name = "authUser") String authUser,
            @WebParam(name = "authPassword") String authPassword,
            @WebParam(name = "updatedUser") String updatedUser,
            @WebParam(name = "updatedPassword") String updatedPassword,
            @WebParam(name = "fullName") String fullName,
            @WebParam(name = "administrator") Boolean administrator) {

        UserAction userAction = new UserAction();
        AuthenticationToken token = userAction.authenticate(authUser, authPassword);
        if (token.authenticated && token.authorized && token.administrator) {
            if (userManager.getUser(updatedUser) == null) {
                if (!userManager.updateUser(updatedUser, updatedPassword, fullName, administrator)) {
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
            @WebParam(name = "authUser") String authUser,
            @WebParam(name = "authPassword") String authPassword,
            @WebParam(name = "updatedUser") String updatedUser,
            @WebParam(name = "updatedPassword") String updatedPassword,
            @WebParam(name = "fullName") String fullName,
            @WebParam(name = "administrator") Boolean administrator) {

        UserAction userAction = new UserAction();
        AuthenticationToken token = userAction.authenticate(authUser, authPassword);
        if (token.authenticated && token.authorized && token.administrator && (userManager.getUser(updatedUser) != null)) {
            if (!userManager.updateUser(updatedUser, updatedPassword, fullName, administrator)) {
                return false;
            }
        } else {
            return false;
        }

        return true;

    }

    @WebMethod(operationName = "deleteUser")
    public Boolean deleteUser(
            @WebParam(name = "authUser") String authUser,
            @WebParam(name = "authPassword") String authPassword,
            @WebParam(name = "deletedUser") String deletedUser) {

        UserAction userAction = new UserAction();
        AuthenticationToken token = userAction.authenticate(authUser, authPassword);
        if (token.authenticated && token.authorized && token.administrator) {
            return userManager.deleteUser(deletedUser);
        }

        return false;
    }

    @WebMethod(operationName = "getUserList")
    public List<XMLUser> getUserList(
            @WebParam(name = "authUser") String authUser,
            @WebParam(name = "authPassword") String authPassword) {

        List<XMLUser> xusers = new ArrayList<XMLUser>();
        UserAction userAction = new UserAction();
        AuthenticationToken token = userAction.authenticate(authUser, authPassword);
        if (token.authenticated && token.authorized && token.administrator) {
            for (PersistentUser user : userManager.getUsers()) {
                xusers.add(user.toXmlUser(false));
            }
        }

        return xusers;
    }

    @WebMethod(operationName = "getDirectoryGroups")
    public List<XMLDirectoryDomain> getDirectoryGroups(
            @WebParam(name = "username") String username,
            @WebParam(name = "password") String password) {

        List<XMLDirectoryDomain> xdomains = new ArrayList();
        UserAction userAction = new UserAction();
        AuthenticationToken token = userAction.authenticate(username, password);
        if(token.authenticated && token.authorized && token.administrator) {
            for (Entry<String, HashMap<String, Boolean>> entry : configurationManager.getDirectoryDomains().entrySet()) {
                XMLDirectoryDomain xdomain = new XMLDirectoryDomain();
                xdomain.domain = entry.getKey();
                for(Entry<String, Boolean> group : entry.getValue().entrySet()) {
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

    @WebMethod(operationName = "getNetworkList")
    public List<XMLNetwork> getNetworkList(
            @WebParam(name = "authUser") String authUser,
            @WebParam(name = "authPassword") String authPassword) {

        List<XMLNetwork> xnetworks = new ArrayList<XMLNetwork>();

        UserAction userAction = new UserAction();
        AuthenticationToken token = userAction.authenticate(authUser, authPassword);
        if (token.authenticated && token.authorized && token.administrator) {
            for (Entry<String, String> entry : configurationManager.getManagedNetworks().entrySet()) {
                XMLNetwork xnetwork = new XMLNetwork();
                xnetwork.name = entry.getValue();
                String cidr = entry.getKey();
                String[] cidrParts = cidr.split("/");
                xnetwork.address = cidrParts[0];
                xnetwork.mask = Integer.parseInt(cidrParts[1].trim());

                xnetworks.add(xnetwork);
            }
        }
        return xnetworks;
    }
}
