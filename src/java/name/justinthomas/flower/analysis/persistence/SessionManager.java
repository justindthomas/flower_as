/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package name.justinthomas.flower.analysis.persistence;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.http.HttpSession;
import name.justinthomas.flower.analysis.element.Flow;
import name.justinthomas.flower.analysis.services.ChartData;
import name.justinthomas.flower.analysis.services.MapDataResponse;
import name.justinthomas.flower.analysis.services.xmlobjects.XMLDataVolumeList;
import name.justinthomas.flower.analysis.services.xmlobjects.XMLNetworkList;

/**
 *
 * @author JustinThomas
 */
public abstract class SessionManager {

    @SuppressWarnings("unchecked")
    public static List<Flow> getFlows(HttpSession session, String tracker) {
        Map<String, List<Flow>> map = getFlowMap(session);

        if (!map.containsKey(tracker)) {
            map.put(tracker, Collections.synchronizedList(new LinkedList<Flow>()));
        }

        List<Flow> flows = ((Map<String, List<Flow>>) session.getAttribute("flows")).get(tracker);
        return flows;
    }
    
    public static Map<String, List<Flow>> getFlowMap(HttpSession session) {
        Map<String, List<Flow>> map = null;
        
        if (session.getAttribute("flows") == null) {
            //map = Collections.synchronizedMap(new HashMap<String, List<Flow>>());
            map = new ConcurrentHashMap();
            session.setAttribute("flows", map);
        } else {
            map = (Map) session.getAttribute("flows");
        }
        
        return map;
    }

    public static void isProcessingPacketsComplete(HttpSession session, String tracker, Boolean status) {
        if (session.getAttribute("processing_packets_complete") == null) {
            session.setAttribute("processing_packets_complete", Collections.synchronizedMap(new HashMap<String, Boolean>()));
        }

        if (status != null) {
            ((Map<String, Boolean>) session.getAttribute("processing_packets_complete")).put(tracker, status);
        } else {
            ((Map<String, Boolean>) session.getAttribute("processing_packets_complete")).remove(tracker);
        }
    }

    public static Boolean isProcessingPacketsComplete(HttpSession session, String tracker) {
        if (session.getAttribute("processing_packets_complete") != null) {
            if (((Map<String, Boolean>) session.getAttribute("processing_packets_complete")).containsKey(tracker)) {
                return ((Map<String, Boolean>) session.getAttribute("processing_packets_complete")).get(tracker);
            }
        }
        return false;
    }

    public static void errorStatus(HttpSession session, Boolean error) {
        session.setAttribute("error", error);
    }

    public static Boolean errorStatus(HttpSession session) {
        if (session.getAttribute("error") != null) {
            return (Boolean) session.getAttribute("error");
        } else {
            return false;
        }
    }

    public static String getPath(HttpSession session) {
        if (session.getAttribute("path") != null) {
            return (String) session.getAttribute("path");
        } else {
            return "";
        }
    }

    public static String getBasePath(HttpSession session) {
        if (session.getAttribute("basePath") != null) {
            return (String) session.getAttribute("basePath");
        } else {
            return "";
        }
    }
    
    public static ChartData.BuildDataVolumeList getVolumeTask(HttpSession session, String nonce) {
        return getVolumeTaskMap(session).get(nonce);
    }
    
    public static void addVolumeTask(HttpSession session, ChartData.BuildDataVolumeList task) throws Exception {
        HashMap<String, ChartData.BuildDataVolumeList> volumeTaskMap = getVolumeTaskMap(session);
        
        if(volumeTaskMap.containsKey(task.tracker)) {
            throw new Exception("Task: " + task.tracker + " is already registered.");
        } else {
            volumeTaskMap.put(task.tracker, task);
        }
    }
    
    public static HashMap<String, ChartData.BuildDataVolumeList> getVolumeTaskMap(HttpSession session) {
        if (session.getAttribute("volumetaskmap") == null) {
            session.setAttribute("volumetaskmap", new HashMap<String, ChartData.BuildDataVolumeList>()); 
        }
        
        return (HashMap<String, ChartData.BuildDataVolumeList>)session.getAttribute("volumetaskmap");
    }
    
    public static void setMapDataThread(HttpSession session, TimedThread thread) {
        if (thread != null) {
            session.setAttribute("mapdatathread", thread);
        } else {
            session.removeAttribute("mapdatathread");
        }
    }
    
    public static TimedThread getMapDataThread(HttpSession session) {
        if (session.getAttribute("mapdatathread") != null) {
            return (TimedThread) session.getAttribute("mapdatathread");
        } else {
            return null;
        }
    }
    
    public static void setMapDataResponse(HttpSession session, MapDataResponse response) {
        if (response != null) {
            session.setAttribute("mapdataresponse", response);
        } else {
            session.removeAttribute("mapdataresponse");
        }
    }
    
    public static MapDataResponse getMapDataResponse(HttpSession session) {
        if (session.getAttribute("mapdataresponse") != null) {
            return (MapDataResponse) session.getAttribute("mapdataresponse");
        } else {
            return null;
        }
    }

    public static void setNetworks(HttpSession session, XMLNetworkList networks) {
        if (networks != null) {
            session.setAttribute("networks", networks);
        } else {
            session.removeAttribute("networks");
        }
    }

    public static XMLNetworkList getNetworks(HttpSession session) {

        if (session.getAttribute("networks") != null) {
            return (XMLNetworkList) session.getAttribute("networks");
        } else {
            return null;
        }
    }

    public static void setFlows(HttpSession session, LinkedHashMap<String, Flow> flows) {

        if (flows != null) {
            session.setAttribute("flows", flows);
        }
    }

    public static void setUsername(HttpSession session, String username) {
        if (username != null) {
            session.setAttribute("username", username);
        }
    }

    public static String getUsername(HttpSession session) {
        if (session.getAttribute("username") != null) {
            return (String) session.getAttribute("username");
        } else {
            return null;
        }
    }

    public static void setPassword(HttpSession session, String password) {
        if (password != null) {
            session.setAttribute("password", password);
        }
    }

    public static String getPassword(HttpSession session) {
        if (session.getAttribute("password") != null) {
            return (String) session.getAttribute("password");
        } else {
            return null;
        }
    }

    public static void isAuthenticated(HttpSession session, Boolean auth) {
        if (auth) {
            session.setAttribute("authenticated", auth);
        } else {
            session.removeAttribute("authenticated");
        }
    }

    public static Boolean isAuthenticated(HttpSession session) {
        if (session.getAttribute("authenticated") != null) {
            return (Boolean) session.getAttribute("authenticated");
        } else {
            return false;
        }
    }
}
