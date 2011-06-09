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
import name.justinthomas.flower.analysis.services.xmlobjects.XMLDataVolumeList;
import name.justinthomas.flower.analysis.services.xmlobjects.XMLNetworkList;

/**
 *
 * @author JustinThomas
 */
public abstract class SessionManager {

    public static void clearMap(HttpSession session) {
        session.removeAttribute("map");
    }

    public static void clearHistogram(HttpSession session) {
        session.removeAttribute("histogram");
    }

    public static void clearPattern(HttpSession session) {
        session.removeAttribute("pattern");
    }

    public static Boolean isMapReady(HttpSession session) {
        if (getMap(session) != null) {
            return true;
        }
        return false;
    }

    public static Boolean isHistogramReady(HttpSession session) {
        if (getHistogram(session) != null) {
            return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public static List<Flow> getFlows(HttpSession session, String tracker) {
        Map<String, List<Flow>> map;
        if (session.getAttribute("flows") == null) {
            //map = Collections.synchronizedMap(new HashMap<String, List<Flow>>());
            map = new ConcurrentHashMap();
            session.setAttribute("flows", map);
        } else {
            map = (Map) session.getAttribute("flows");
        }

        if (!map.containsKey(tracker)) {
            map.put(tracker, Collections.synchronizedList(new LinkedList<Flow>()));
        }

        List<Flow> flows = ((Map<String, List<Flow>>) session.getAttribute("flows")).get(tracker);
        return flows;
    }

    public static void clearPackets(HttpSession session) {
        session.removeAttribute("packets");
    }

    public static void setMap(HttpSession session, String content) {
        session.setAttribute("map", content);
    }

    public static String getMap(HttpSession session) {
        if (session.getAttribute("map") != null) {
            return (String) session.getAttribute("map");
        } else {
            return null;
        }
    }

    public static void setMapBuildStage(HttpSession session, Integer stage) {
        if (stage != null) {
            session.setAttribute("map_build_stage", stage);
        } else {
            session.removeAttribute("map_build_stage");
        }
    }

    public static int getMapBuildStage(HttpSession session) {
        if (session.getAttribute("map_build_stage") != null) {
            return (Integer) session.getAttribute("map_build_stage");
        } else {
            return 0;
        }
    }

    public static void setHistogram(HttpSession session, String content) {
        session.setAttribute("histogram", content);
    }

    public static String getHistogram(HttpSession session) {
        if (session.getAttribute("histogram") != null) {
            return (String) session.getAttribute("histogram");
        } else {
            return null;
        }
    }

    public static void setHistogramBuildStage(HttpSession session, Integer stage) {
        if (stage != null) {
            session.setAttribute("histogram_build_stage", stage);
        } else {
            session.removeAttribute("histogram_build_stage");
        }
    }

    public static int getHistogramBuildStage(HttpSession session) {
        if (session.getAttribute("histogram_build_stage") != null) {
            return (Integer) session.getAttribute("map_build_stage");
        } else {
            return 0;
        }
    }

    public static void isMapBuilding(HttpSession session, Boolean status) {
        if (status != null) {
            session.setAttribute("map_building", status);
        } else {
            session.removeAttribute("map_building");
        }
    }

    public static Boolean isMapBuilding(HttpSession session) {
        if (session.getAttribute("map_building") != null) {
            return (Boolean) session.getAttribute("map_building");
        } else {
            return false;
        }
    }

    public static void isProcessingPackets(HttpSession session, Boolean status) {
        if (status != null) {
            session.setAttribute("processing_packets", status);
        } else {
            session.removeAttribute("processing_packets");
        }
    }

    public static Boolean isProcessingPackets(HttpSession session) {
        if (session.getAttribute("processing_packets") != null) {
            return (Boolean) session.getAttribute("processing_packets");
        } else {
            return false;
        }
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

    public static void isHistogramBuilding(HttpSession session, Boolean status) {
        session.setAttribute("histogram_building", status);
    }

    public static Boolean isHistogramBuilding(HttpSession session) {
        if (session.getAttribute("histogram_building") != null) {
            return (Boolean) session.getAttribute("histogram_building");
        } else {
            return false;
        }
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

    public static void setVolumes(HttpSession session, XMLDataVolumeList volumes) {
        if (volumes != null) {
            session.setAttribute("volumes", volumes);
        } else {
            session.removeAttribute("volumes");
        }
    }

    public static XMLDataVolumeList getVolumes(HttpSession session) {
        if (session.getAttribute("volumes") != null) {
            return (XMLDataVolumeList) session.getAttribute("volumes");
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
