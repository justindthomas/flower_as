package name.justinthomas.flower.analysis.services;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.MessageContext;
import name.justinthomas.flower.analysis.authentication.UserAction;
import name.justinthomas.flower.analysis.persistence.Constraints;
import name.justinthomas.flower.analysis.persistence.SessionManager;
import name.justinthomas.flower.analysis.persistence.ThreadManager;
import name.justinthomas.flower.analysis.persistence.TimedThread;
import name.justinthomas.flower.analysis.statistics.StatisticsManager;
import name.justinthomas.flower.manager.services.CustomerAdministration.Customer;

/**
 *
 * @author justin
 */
@WebService(serviceName = "MapData")
public class MapData {

    @Resource
    private WebServiceContext serviceContext;
    @EJB
    ThreadManager threadManager;

    @WebMethod(operationName = "getMapData")
    public MapDataResponse getMapData(
            @WebParam(name = "customer") String customerID,
            @WebParam(name = "user") String user,
            @WebParam(name = "password") String password,
            @WebParam(name = "constraints") String constraints,
            @WebParam(name = "wait") Boolean wait) {

        UserAction userAction = new UserAction();

        if (!userAction.authenticate(customerID, user, password).authorized) {
            return (null);
        }

        MessageContext messageContext = serviceContext.getMessageContext();
        HttpSession session = ((HttpServletRequest) messageContext.get(MessageContext.SERVLET_REQUEST)).getSession();

        if (session == null) {
            throw new WebServiceException("No session in WebServiceContext");
        }

        Customer customer = Utility.getCustomer(customerID);

        if (customer != null) {
            System.out.println("Checking for map data...");
            MapDataResponse response = SessionManager.getMapDataResponse(session);

            if (response == null && SessionManager.getMapDataThread(session) == null) {
                System.out.println("Beginning to build map data...");
                BuildMapData task = new BuildMapData(customer, session, constraints);
                TimedThread thread = new TimedThread(task);
                SessionManager.setMapDataThread(session, thread);
                threadManager.start(user, thread);

                if (wait) {
                    System.out.println("Waiting for map data to complete...");
                    try {
                        thread.join();
                        response = SessionManager.getMapDataResponse(session);
                        SessionManager.setMapDataResponse(session, null);
                        SessionManager.setMapDataThread(session, null);
                    } catch (InterruptedException e) {
                        System.err.println("Thread: " + thread.getName() + " was interrupted.");
                    }
                    return response;
                }
                
                response = new MapDataResponse("untracked");
            } else if (response == null && SessionManager.getMapDataThread(session).isExpired()) {
                System.err.println("MapData build thread expired.");
                SessionManager.getMapDataThread(session).interrupt();
                response = new MapDataResponse("untracked");
                response.ready = true;
                return response;
            } else if (response == null) {
                response = new MapDataResponse("untracked");
            } else {
                SessionManager.setMapDataResponse(session, null);
                SessionManager.setMapDataThread(session, null);
            }

            return response;
        }


        return new MapDataResponse("untracked");
    }

    private class BuildMapData implements Runnable {

        private HttpSession session;
        private String constraints;
        private Customer customer;

        private BuildMapData(Customer customer, HttpSession session, String constraints) {
            this.session = session;
            this.constraints = constraints;
            this.customer = customer;
        }

        @Override
        public void run() {
            System.out.println("Calling StatisticsManager to build map data...");
            StatisticsManager statisticsManager = new StatisticsManager(customer);
            try {
                SessionManager.setMapDataResponse(session, statisticsManager.getMapData(session, new Constraints(constraints)));
                System.out.println("StatisticsManager appears to be complete.");
            } catch (InterruptedException e) {
                System.err.println("MapData build thread was interrupted.");
            }
        }
    }
}
