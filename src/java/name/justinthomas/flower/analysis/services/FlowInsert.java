package name.justinthomas.flower.analysis.services;

import java.util.HashMap;
import java.util.Map.Entry;
import javax.annotation.Resource;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.servlet.http.HttpServletRequest;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;
import name.justinthomas.flower.analysis.element.Flow;
import name.justinthomas.flower.analysis.persistence.FlowReceiver;
import name.justinthomas.flower.analysis.services.xmlobjects.XMLFlow;
import name.justinthomas.flower.analysis.services.xmlobjects.XMLFlowSet;
import name.justinthomas.flower.analysis.statistics.StatisticsManager;

/**
 *
 * @author justin
 */
@WebService()
public class FlowInsert {
    @Resource
    WebServiceContext context;

    @WebMethod(operationName = "addFlows")
    public Integer addFlows(
            @WebParam(name = "flows") XMLFlowSet flowSet) {

        MessageContext messageContext = context.getMessageContext();
        HttpServletRequest request = (HttpServletRequest) messageContext.get(MessageContext.SERVLET_REQUEST);

        Thread thread = new Thread(new InsertThread(flowSet));
        thread.start();

        return 0;
    }

    class InsertThread implements Runnable {

        XMLFlowSet flowSet;
        HashMap<Long, Flow> flows = new HashMap();

        public InsertThread(XMLFlowSet flowSet) {
            this.flowSet = flowSet;
        }

        @Override
        public void run() {
            System.out.println("Beginning to store " + flowSet.flows.size() + " flows...");
            for (XMLFlow xflow : flowSet.flows) {
                try {
                    if (xflow.bytesSent.longValue() < 0) {
                        throw new Exception("Negative bytesSent value (" + xflow.bytesSent.longValue() + ") in XMLFlow received.");
                    }

                    Flow flow = new Flow(xflow);

                    FlowReceiver receiver = new FlowReceiver();
                    Long flowID = receiver.addFlow(flow);

                    flows.put(flowID, flow);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            System.out.println("Completed storing and beginning to process statistics for " + flows.size() + " flows.");

            for (Entry<Long, Flow> flow : flows.entrySet()) {
                StatisticsManager statisticsManager = new StatisticsManager();
                statisticsManager.addStatisticalSeconds(flow.getValue(), flow.getKey());
            }

            //System.out.println("Completed processing statistics for " + flows.size() + " flows.");
        }
    }
}
