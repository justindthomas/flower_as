package name.justinthomas.flower.analysis.persistence;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 *
 * @author JustinThomas
 */
public class Constraints {

    public Integer[] sourcePortRange = new Integer[]{-1, 65535};
    public ArrayList<Integer> sourcePortList = new ArrayList<Integer>();
    public Integer[] destinationPortRange = new Integer[]{-1, 65535};
    public ArrayList<Integer> destinationPortList = new ArrayList<Integer>();
    public Date startTime = new Date();
    public Date endTime = new Date();
    public Boolean ipv4 = true;
    public Boolean ipv6 = true;
    public ArrayList<InetAddress> sourceAddressList = new ArrayList<InetAddress>();
    public ArrayList<InetAddress> destinationAddressList = new ArrayList<InetAddress>();
    public ArrayList<Integer> protocolList = new ArrayList<Integer>();
    public Integer maxFlowSize = 0;
    public Integer minFlowSize = 0;
    public static final SimpleDateFormat timeDateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

    public Constraints(String pattern) {
        // Set the default constraints to try to avoid querying records that are being written
        //startTime.setTime(startTime.getTime() - 600000);
        //endTime.setTime(endTime.getTime() - 600000);

        if (pattern != null) {
            parsePattern(pattern);
        } else {
            takeThirty();
        }
    }

    public Constraints() {
        takeThirty();
    }

    private void takeThirty() {
        // Grab the last 30 minutes
        startTime.setTime(endTime.getTime() - (1000l * 60 * 30));
    }

    private void parsePattern(String pattern) {
        takeThirty();

        if ((pattern == null) || pattern.equals("")) {
            return;
        }

        Boolean start = false;
        String[] token = pattern.split("\\s");
        for (int x = 0; x < token.length; x++) {
            if (token[x].equals("maxsize")) {
                maxFlowSize = Integer.valueOf(token[++x]);
            } else if (token[x].equals("ipv6")) {
                ipv4 = false;
            } else if (token[x].equals("ipv4")) {
                ipv6 = false;
            } else if (token[x].equals("tcp")) {
                protocolList.add(6);
            } else if (token[x].equals("udp")) {
                protocolList.add(17);
            } else if (token[x].equals("port")) {
                destinationPortList.add(Integer.valueOf(token[++x]));
            } else if (token[x].equals("esp")) {
                protocolList.add(50);
            } else if (token[x].equals("6in4")) {
                protocolList.add(42);
            } else if (token[x].equals("proto")) {
                protocolList.add(Integer.valueOf(token[++x]));
            } else if (token[x].equals("startms")) {
                start = true;
                startTime.setTime(Long.parseLong(token[++x]));
            } else if (token[x].equals("durationms")) {
                if(start) {
                    endTime.setTime(startTime.getTime() + Long.parseLong(token[++x]));
                }
            } else if (token[x].equals("start")) {
                start = true;
                String startDate = token[++x] + " " + token[++x];
                try {
                    startTime = timeDateFormat.parse(startDate);
                } catch (ParseException pe) {
                    System.err.println("Couldn't parse start date/time: " + startDate);
                }
            } else if (token[x].equals("days")) {
                if (start) {
                    endTime.setTime(startTime.getTime() + (1000 * 60 * 60 * 24 * Long.valueOf(token[++x])));
                } else {
                    startTime.setTime(endTime.getTime() - (1000 * 60 * 60 * 24 * Long.valueOf(token[++x])));
                }
            } else if (token[x].equals("minutes")) {
                if (start) {
                    endTime.setTime(startTime.getTime() + (1000 * 60 * Long.valueOf(token[++x])));
                } else {
                    startTime.setTime(endTime.getTime() - (1000 * 60 * Long.valueOf(token[++x])));
                }
            } else if (token[x].equals("hours")) {
                if (start) {
                    endTime.setTime(startTime.getTime() + (1000 * 60 * 60 * Long.valueOf(token[++x])));
                } else {
                    startTime.setTime(endTime.getTime() - (1000 * 60 * 60 * Long.valueOf(token[++x])));
                }
            } else if (token[x].equals("sip")) {
                try {
                    sourceAddressList.add(InetAddress.getByName(token[++x]));
                } catch (UnknownHostException uhe) {
                    System.err.println("Couldn't parse source host parameter: " + uhe.getMessage());
                }
            } else if (token[x].equals("dip")) {
                try {
                    destinationAddressList.add(InetAddress.getByName(token[++x]));
                } catch (UnknownHostException uhe) {
                    System.err.println("Couldn't parse destination host parameter: " + uhe.getMessage());
                }
            }
        }
    }
}
