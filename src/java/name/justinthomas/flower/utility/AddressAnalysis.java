package name.justinthomas.flower.utility;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import name.justinthomas.flower.analysis.element.InetNetwork;

public class AddressAnalysis {

    private static Boolean DEBUG = false;

    /*
     * TODO Migrate all of this into proper object classes.
     *
     * isMember() should go to InetAddress.isMember(InetNetwork)
     * isBroadcast() should go to InetAddress.isBroadcast(InetNetwork)
     */
    public static Boolean isMember(InetAddress address, InetNetwork network) {

        if (network.isDefault()) {
            if (address.getHostAddress().equals("0.0.0.0")) {
                return true;
            } else {
                return false;
            }
        }
        if(DEBUG) System.out.println("Comparing: " + address.getHostAddress() + " to " + network.getAddress().getHostAddress());

        byte[] addressBytes = address.getAddress();
        byte[] networkBytes = network.getAddress().getAddress();

        Integer a = 0;
        for (a = 0; a < (network.getMask() / 8); a++) {
            if (addressBytes[a] != networkBytes[a]) {
                return false;
            }
        }

        // Check to see if there's a partial byte
        if (network.getMask() % 8 != 0) {
            // Zero out the host portion of the address
            Integer c = addressBytes[a] & networkBytes[a];

            // Compare the address-zeroed out host address to the network address
            if ((c ^ networkBytes[a]) != 0) {
                return false;
            }
        }

        return true;
    }

    public static String getHostPortion(InetAddress address, InetNetwork network) {
        String addressString = address.getHostAddress();
        String networkString = network.getAddress().getHostAddress();

        String separator = null;
        if(address instanceof Inet4Address) {
            separator = "\\.";
        } else if(address instanceof Inet6Address) {
            separator = ":";
        }

        StringBuffer hostPortion = new StringBuffer();
        if(separator != null) {
            String[] addressParts = addressString.split(separator);
            String[] networkParts = networkString.split(separator);
            for(int i = 0; i < addressParts.length; i++) {
                if(!addressParts[i].equals(networkParts[i])) {
                    hostPortion.append(addressParts[i]);
                    if(i < (addressParts.length - 1)) hostPortion.append(".");
                }
            }
        }
        return hostPortion.toString();
    }

    public static Boolean isBroadcast(InetAddress address) {
        return isBroadcast(address, null);
    }

    public static Boolean isBroadcast(InetAddress address, InetNetwork network) {

        if (address instanceof Inet6Address) {
            if(DEBUG) System.out.println("This is a non-multicast IPv6 Address");
            return false;
        }

        if (address.getHostAddress().equals("0.0.0.0")) {
            return false;
        }

        if (address instanceof Inet4Address) {
            final byte[] allOnes = {0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff};
            if(DEBUG) System.out.println("This is an IPv4 Address");
            byte[] bytes = address.getAddress();

            // Determine if this is a global broadcast (255.255.255.255)
            if (address.getHostAddress().equals("255.255.255.255")) {
                return true;
            }

            if (network != null) {
                // Sanity check
                if (!isMember(address, network)) {
                    if(DEBUG) System.out.println("Address is not a member of the network");
                    return false;
                }

                // If the network mask is /32 or /128, we assume the address is meant as a single host
                // and not a broadcast address.  TODO:  Verify that this works as intended.
                if (~network.getMask() == 0) {
                    if(DEBUG) System.out.println("Address mask is all 1s");
                    return false;
                }

                // Convert byte array to Integer
                int addressInteger = 0;
                for (int i = 0; i < bytes.length; i++) {
                    int shift = ((bytes.length - 1 - i) * 8);
                    addressInteger += ((bytes[i] & 0x000000FF) << shift);
                }

                int allOnesInteger = 0;
                for (int i = 0; i < allOnes.length; i++) {
                    int shift = ((allOnes.length - 1 - i) * 8);
                    allOnesInteger += ((allOnes[i] & 0x000000FF) << shift);
                }

                // Convert CIDR mask to Integer value
                int rightShift = 32 - network.getMask();
                int maskInteger = allOnesInteger >> rightShift << rightShift;

                // Inclusive OR addressInteger and the opposite of maskInteger
                // This should result in the network broadcast address
                int broadcastAddress = (addressInteger | ~maskInteger);

                if ((broadcastAddress ^ addressInteger) == 0) {
                    return true;
                }
            }
        }

        return false;
    }
}
