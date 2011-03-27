package name.justinthomas.flower.analysis.services;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.ws.WebServiceContext;
import name.justinthomas.flower.analysis.persistence.AlertManager;
import name.justinthomas.flower.analysis.persistence.ConfigurationManager;
import name.justinthomas.flower.analysis.persistence.Constraints;
import name.justinthomas.flower.analysis.persistence.PersistentAlert;

/**
 *
 * @author justin
 */
@WebService()
public class Alerts {

    @Resource
    private WebServiceContext serviceContext;
    @EJB
    ConfigurationManager configurationManager;

    @WebMethod(operationName = "addAlerts")
    public Integer addAlerts(
            @WebParam(name = "alerts") List<PersistentAlert> alerts) {
        
        System.out.println("Received " + alerts.size() + " alerts.");
        
        AlertManager alertManager = new AlertManager();
        for(PersistentAlert alert : alerts) {
            alertManager.addAlert(alert);
        }
        
        return alerts.size();
    }

    @WebMethod(operationName = "deleteAlert")
    public Boolean deleteAlert(
            @WebParam(name = "user") String user,
            @WebParam(name = "password") String password,
            @WebParam(name = "record") Long record) {
        UserAction userAction = new UserAction();
        if(userAction.authenticate(user, password).authorized) {
            System.out.println("Deleting alert...");
            AlertManager alertManager = new AlertManager();
            return alertManager.deleteAlert(record);
        }
        return false;
    }

    @WebMethod(operationName = "getAlerts")
    public List<PersistentAlert> getAlerts(
            @WebParam(name = "user") String user,
            @WebParam(name = "password") String password,
            @WebParam(name = "constraints") String constraintsString) {
        UserAction userAction = new UserAction();

        ArrayList<PersistentAlert> alerts = new ArrayList();

        if(userAction.authenticate(user, password).authorized) {
            System.out.println("Retrieving alerts...");
            AlertManager alertManager = new AlertManager();
            Constraints constraints = new Constraints(constraintsString);
            alerts.addAll(alertManager.getAlerts(constraints));
        }

        return alerts;
    }

}
