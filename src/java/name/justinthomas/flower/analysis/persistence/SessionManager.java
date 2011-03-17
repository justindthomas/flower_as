/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package name.justinthomas.flower.analysis.persistence;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpSession;
import name.justinthomas.flower.analysis.element.Flow;
import name.justinthomas.flower.analysis.services.xmlobjects.XMLDataVolumeList;
import name.justinthomas.flower.analysis.services.xmlobjects.XMLNetworkList;
import org.bouncycastle.util.encoders.Base64;

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
        if (session.getAttribute("flows") == null) {
            Map<String, List<Flow>> map = Collections.synchronizedMap(new HashMap<String, List<Flow>>());

            List<Flow> flows = Collections.synchronizedList(new LinkedList<Flow>());
            map.put(tracker, flows);

            session.setAttribute("flows", map);
        }

        List<Flow> flows = ((Map<String, List<Flow>>)session.getAttribute("flows")).get(tracker);
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
        if(session.getAttribute("processing_packets_complete") != null) {
            if (((Map<String, Boolean>) session.getAttribute("processing_packets_complete")).containsKey(tracker)) {
                return ((Map<String, Boolean>) session.getAttribute("processing_packets_complete")).get(tracker);
            }
        }
        return false;
    }

    public static void flowsProcessed(HttpSession session, Integer flowsProcessed) {
        if (flowsProcessed != null) {
            session.setAttribute("flows_processed", flowsProcessed);
        } else {
            session.removeAttribute("flows_processed");
        }
    }

    public static int flowsProcessed(HttpSession session) {
        if (session.getAttribute("flows_processed") != null) {
            return (Integer) session.getAttribute("flows_processed");
        } else {
            return 0;
        }
    }

    public static void packetsProcessed(HttpSession session, Integer packetsProcessed) {
        if (packetsProcessed != null) {
            session.setAttribute("packets_processed", packetsProcessed);
        } else {
            session.removeAttribute("packets_processed");
        }
    }

    public static int packetsProcessed(HttpSession session) {
        if (session.getAttribute("packets_processed") != null) {
            return (Integer) session.getAttribute("packets_processed");
        } else {
            return 0;
        }
    }

    public static void nodesProcessed(HttpSession session, Integer nodesProcessed) {
        if (nodesProcessed != null) {
            session.setAttribute("nodes_processed", nodesProcessed);
        } else {
            session.removeAttribute("nodes_processed");
        }
    }

    public static Integer nodesProcessed(HttpSession session) {
        if (session.getAttribute("nodes_processed") != null) {
            return (Integer) session.getAttribute("nodes_processed");
        } else {
            return 0;
        }
    }

    public static void networksProcessed(HttpSession session, Integer networksProcessed) {
        if (networksProcessed != null) {
            session.setAttribute("networks_processed", networksProcessed);
        } else {
            session.removeAttribute("networks_processed");
        }
    }

    public static Integer networksProcessed(HttpSession session) {
        if (session.getAttribute("networks_processed") != null) {
            return (Integer) session.getAttribute("networks_processed");
        } else {
            return 0;
        }
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
    /*
    @SuppressWarnings("unchecked")
    public static LinkedHashMap<String, Flow> getFlows(HttpSession session) {

        if (session.getAttribute("flows") != null) {
            return (LinkedHashMap<String, Flow>) session.getAttribute("flows");
        } else {
            return null;
        }
    }

    /*
    public static void setPattern(String pattern, HttpSession session) {
    if(pattern != null)
    session.setAttribute("pattern", pattern);
    }
     */
    public static Boolean setPattern(HttpSession session, String pattern) {

        // Check to see if a constraint pattern has been requested.
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        Boolean modifiedPatternFound = false;

        if (pattern != null) {

            // Some kind of pattern has been requested
            if (!pattern.equals("")) {

                // The pattern is something other than a blank string
                try {
                    Base64.decode(pattern, baos);
                } catch (IOException e) {
                }

                pattern = baos.toString();
            }

            // If we've already built (or are in the process of building) a map for this constraint,
            // a session attribute will exist and will match the requested constraint.  We check here
            // for a negative match to see if we have a new constraint to build for.
            if ((getPattern(session) == null) // No pattern was previously requested
                    || !getPattern(session).equals(pattern)) {  // This pattern is already built

                //System.out.println("Requested Pattern: " + pattern);

                // Store this constraint in the session to prevent infinite looping
                session.setAttribute("pattern", pattern);

                // Delete the session-stored SVG if it exists (forcing a recreation)
                if (isMapReady(session)) {
                    clearMap(session);
                }

                if (isHistogramReady(session)) {
                    clearHistogram(session);
                }

                modifiedPatternFound = true;
            }
        } else {
            // No pattern was specified in the request 'pattern' parameter (null)
            if (getPattern(session) != null) {
                clearPattern(session);
            }
        }

        return modifiedPatternFound;
    }

    public static String getPattern(HttpSession session) {
        if (session.getAttribute("pattern") != null) {
            return (String) session.getAttribute("pattern");
        } else {
            return null;
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
