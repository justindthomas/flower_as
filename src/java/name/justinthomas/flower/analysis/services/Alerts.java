package name.justinthomas.flower.analysis.services;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.ws.WebServiceContext;
import name.justinthomas.flower.analysis.authentication.UserAction;
import name.justinthomas.flower.analysis.persistence.AlertManager;
import name.justinthomas.flower.analysis.persistence.Constraints;
import name.justinthomas.flower.analysis.persistence.ModSecurityAlert;
import name.justinthomas.flower.analysis.persistence.SnortAlert;
import name.justinthomas.flower.global.GlobalConfigurationManager;

/**
 *
 * @author justin
 */
@WebService()
public class Alerts {

    @Resource
    private WebServiceContext serviceContext;
    
    @EJB private GlobalConfigurationManager globalConfigurationManager;

    @WebMethod(operationName = "addSnortAlerts")
    public Integer addSnortAlerts(
            @WebParam(name = "customer") String customerID,
            @WebParam(name = "alerts") List<SnortAlert> alerts) {
        
        System.out.println("Received " + alerts.size() + " alerts.");
        
        AlertManager alertManager = new AlertManager(Utility.getCustomer(customerID));
        for(SnortAlert alert : alerts) {
            alertManager.addAlert(alert);
        }
        
        return alerts.size();
    }

    @WebMethod(operationName = "addModSecurityAlerts")
    public Integer addModSecurityAlerts(
            @WebParam(name = "customer") String customerID,
            @WebParam(name = "alerts") List<ModSecurityAlert> alerts) {

        System.out.println("Received " + alerts.size() + " alerts.");

        AlertManager alertManager = new AlertManager(Utility.getCustomer(customerID));
        for(ModSecurityAlert alert : alerts) {
            alertManager.addAlert(alert);
        }

        return alerts.size();
    }

    @WebMethod(operationName = "deleteSnortAlert")
    public Boolean deleteSnortAlert(
            @WebParam(name = "customer") String customerID,
            @WebParam(name = "user") String user,
            @WebParam(name = "password") String password,
            @WebParam(name = "record") Long record) {
        UserAction userAction = new UserAction();
        if(userAction.authenticate(customerID, user, password).authorized) {
            System.out.println("Deleting alert...");
            AlertManager alertManager = new AlertManager(Utility.getCustomer(customerID));
            return alertManager.deleteAlert(SnortAlert.class, record);
        }
        return false;
    }

    @WebMethod(operationName = "deleteModSecurityAlert")
    public Boolean deleteModSecurityAlert(
            @WebParam(name = "customer") String customerID,
            @WebParam(name = "user") String user,
            @WebParam(name = "password") String password,
            @WebParam(name = "record") Long record) {
        UserAction userAction = new UserAction();
        if(userAction.authenticate(customerID, user, password).authorized) {
            System.out.println("Deleting alert...");
            AlertManager alertManager = new AlertManager(Utility.getCustomer(customerID));
            return alertManager.deleteAlert(ModSecurityAlert.class, record);
        }
        return false;
    }

    @WebMethod(operationName = "getSnortAlerts")
    public List<SnortAlert> getSnortAlerts(
            @WebParam(name = "customer") String customerID,
            @WebParam(name = "user") String user,
            @WebParam(name = "password") String password,
            @WebParam(name = "constraints") String constraintsString) {
        UserAction userAction = new UserAction();

        ArrayList<SnortAlert> alerts = new ArrayList();

        if(userAction.authenticate(customerID, user, password).authorized) {
            System.out.println("Retrieving alerts...");
            AlertManager alertManager = new AlertManager(Utility.getCustomer(customerID));
            Constraints constraints = new Constraints(constraintsString);
            alerts.addAll(alertManager.getAlerts(SnortAlert.class, constraints));
        }

        return alerts;
    }

    @WebMethod(operationName = "getModSecurityAlerts")
    public List<ModSecurityAlert> getModSecurityAlerts(
            @WebParam(name = "customer") String customerID,
            @WebParam(name = "user") String user,
            @WebParam(name = "password") String password,
            @WebParam(name = "constraints") String constraintsString) {
        UserAction userAction = new UserAction();

        ArrayList<ModSecurityAlert> alerts = new ArrayList();

        if(userAction.authenticate(customerID, user, password).authorized) {
            System.out.println("Retrieving alerts...");
            AlertManager alertManager = new AlertManager(Utility.getCustomer(customerID));
            Constraints constraints = new Constraints(constraintsString);
            alerts.addAll(alertManager.getAlerts(ModSecurityAlert.class, constraints));
        }

        return alerts;
    }
}
