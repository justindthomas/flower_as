/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package name.justinthomas.flower.analysis.services;

import javax.annotation.Resource;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.servlet.http.HttpServletRequest;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;
import name.justinthomas.flower.analysis.element.Flow;
import name.justinthomas.flower.analysis.persistence.FlowReceiver;
import name.justinthomas.flower.analysis.statistics.StatisticsManager;
import name.justinthomas.flower.analysis.services.xmlobjects.XMLFlow;
import name.justinthomas.flower.analysis.services.xmlobjects.XMLFlowSet;

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

        //System.out.println("Analysis server received: " + flowSet.flows.size() + " flow records");
        for(XMLFlow xflow : flowSet.flows) {
            try {
                if (xflow.bytesSent.longValue() < 0) {
                    throw new Exception("Negative bytesSent value (" + xflow.bytesSent.longValue() + ") in XMLFlow received from: " + request.getRemoteAddr());
                }

                //if (xflow.bytesReceived.longValue() < 0) {
                //    throw new Exception("Negative bytesReceived value (" + xflow.bytesSent.longValue() + ") in XMLFlow received from: " + request.getRemoteAddr());
                //}

                Flow flow = new Flow(xflow);

                FlowReceiver receiver = new FlowReceiver();
                Long flowID = receiver.addFlow(flow, request);

                StatisticsManager statisticsManager = new StatisticsManager();
                statisticsManager.addStatisticalSeconds(flow, flowID);
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return 0;
    }
}
