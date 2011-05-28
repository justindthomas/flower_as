package name.justinthomas.flower.analysis.services;

import name.justinthomas.flower.analysis.authentication.UserAction;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Resource;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.ws.WebServiceContext;
import name.justinthomas.flower.analysis.persistence.AlertManager;
import name.justinthomas.flower.analysis.persistence.Constraints;
import name.justinthomas.flower.analysis.persistence.ModSecurityAlert;
import name.justinthomas.flower.analysis.persistence.SnortAlert;

/**
 *
 * @author justin
 */
@WebService()
public class Alerts {

    @Resource
    private WebServiceContext serviceContext;

    @WebMethod(operationName = "addSnortAlerts")
    public Integer addSnortAlerts(
            @WebParam(name = "alerts") List<SnortAlert> alerts) {
        
        System.out.println("Received " + alerts.size() + " alerts.");
        
        AlertManager alertManager = new AlertManager();
        for(SnortAlert alert : alerts) {
            alertManager.addAlert(alert);
        }
        
        return alerts.size();
    }

    @WebMethod(operationName = "addModSecurityAlerts")
    public Integer addModSecurityAlerts(
            @WebParam(name = "alerts") List<ModSecurityAlert> alerts) {

        System.out.println("Received " + alerts.size() + " alerts.");

        AlertManager alertManager = new AlertManager();
        for(ModSecurityAlert alert : alerts) {
            alertManager.addAlert(alert);
        }

        return alerts.size();
    }

    @WebMethod(operationName = "deleteSnortAlert")
    public Boolean deleteSnortAlert(
            @WebParam(name = "user") String user,
            @WebParam(name = "password") String password,
            @WebParam(name = "record") Long record) {
        UserAction userAction = new UserAction();
        if(userAction.authenticate(user, password).authorized) {
            System.out.println("Deleting alert...");
            AlertManager alertManager = new AlertManager();
            return alertManager.deleteSnortAlert(record);
        }
        return false;
    }

    @WebMethod(operationName = "deleteModSecurityAlert")
    public Boolean deleteModSecurityAlert(
            @WebParam(name = "user") String user,
            @WebParam(name = "password") String password,
            @WebParam(name = "record") Long record) {
        UserAction userAction = new UserAction();
        if(userAction.authenticate(user, password).authorized) {
            System.out.println("Deleting alert...");
            AlertManager alertManager = new AlertManager();
            return alertManager.deleteModSecurityAlert(record);
        }
        return false;
    }

    @WebMethod(operationName = "getSnortAlerts")
    public List<SnortAlert> getSnortAlerts(
            @WebParam(name = "user") String user,
            @WebParam(name = "password") String password,
            @WebParam(name = "constraints") String constraintsString) {
        UserAction userAction = new UserAction();

        ArrayList<SnortAlert> alerts = new ArrayList();

        if(userAction.authenticate(user, password).authorized) {
            System.out.println("Retrieving alerts...");
            AlertManager alertManager = new AlertManager();
            Constraints constraints = new Constraints(constraintsString);
            alerts.addAll(alertManager.getSnortAlerts(constraints));
        }

        return alerts;
    }

    @WebMethod(operationName = "getModSecurityAlerts")
    public List<ModSecurityAlert> getModSecurityAlerts(
            @WebParam(name = "user") String user,
            @WebParam(name = "password") String password,
            @WebParam(name = "constraints") String constraintsString) {
        UserAction userAction = new UserAction();

        ArrayList<ModSecurityAlert> alerts = new ArrayList();

        if(userAction.authenticate(user, password).authorized) {
            System.out.println("Retrieving alerts...");
            AlertManager alertManager = new AlertManager();
            Constraints constraints = new Constraints(constraintsString);
            alerts.addAll(alertManager.getModSecurityAlerts(constraints));
        }

        return alerts;
    }

}
