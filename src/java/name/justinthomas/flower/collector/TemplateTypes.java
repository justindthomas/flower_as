/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package name.justinthomas.flower.collector;

/**
 *
 * @author justin
 */
public abstract class TemplateTypes {

    // Template Field Types                                     Length in Bytes
    public static final int IN_BYTES = 1;                       // Length: N
    public static final int IN_PKTS = 2;                        // Length: N
    public static final int FLOWS = 3;                          // Length: N
    public static final int PROTOCOL = 4;                       // Length: 1
    public static final int TOS = 5;                            // Length: 1
    public static final int TCP_FLAGS = 6;                      // Length: 1
    public static final int L4_SRC_PORT = 7;                    // Length: 2
    public static final int IPV4_SRC_ADDR = 8;                  // Length: 4
    public static final int SRC_MASK = 9;                       // Length: 1
    public static final int INPUT_SNMP = 10;                    // Length: N
    public static final int L4_DST_PORT = 11;                   // Length: 2
    public static final int IPV4_DST_ADDR = 12;                 // Length: 4
    public static final int DST_MASK = 13;                      // Length: 1
    public static final int OUTPUT_SNMP = 14;                   // Length: N
    public static final int IPV4_NEXT_HOP = 15;                 // Length: 4
    public static final int SRC_AS = 16;                        // Length: N
    public static final int DST_AS = 17;                        // Length: N
    public static final int BGP_IPV4_NEXT_HOP = 18;             // Length: 4
    public static final int MUL_DST_PKTS = 19;                  // Length: N
    public static final int MUL_DST_BYTES = 20;                 // Length: N
    public static final int LAST_SWITCHED = 21;                 // Length: 4
    public static final int FIRST_SWITCHED = 22;                // Length: 4
    public static final int OUT_BYTES = 23;                     // Length: N
    public static final int OUT_PKTS = 24;                      // Length: N
    public static final int IPV6_SRC_ADDR = 27;                 // Length: 16
    public static final int IPV6_DST_ADDR = 28;                 // Length: 16
    public static final int IPV6_SRC_MASK = 29;                 // Length: 1
    public static final int IPV6_DST_MASK = 30;                 // Length: 1
    public static final int IPV6_FLOW_LABEL = 31;               // Length: 3
    public static final int ICMP_TYPE = 32;                     // Length: 2
    public static final int MUL_IGMP_TYPE = 33;                 // Length: 1
    public static final int SAMPLING_INTERVAL = 34;             // Length: 4
    public static final int SAMPLING_ALGORITHM = 35;            // Length: 1
    public static final int FLOW_ACTIVE_TIMEOUT = 36;           // Length: 2
    public static final int FLOW_INACTIVE_TIMEOUT = 37;         // Length: 2
    public static final int ENGINE_TYPE = 38;                   // Length: 1
    public static final int ENGINE_ID = 39;                     // Length: 1
    public static final int TOTAL_BYTES_EXP = 40;               // Length: N
    public static final int TOTAL_PKTS_EXP = 41;                // Length: N
    public static final int TOTAL_FLOWS_EXP = 42;               // Length: N
    public static final int MPLS_TOP_LABEL_TYPE = 46;           // Length: 1
    public static final int MPLS_TOP_LABEL_IP_ADDR = 47;        // Length: 4
    public static final int FLOW_SAMPLER_ID = 48;               // Length: 1
    public static final int FLOW_SAMPLER_MODE = 49;             // Length: 1
    public static final int FLOW_SAMPLER_RANDOM_INTERVAL = 50;  // Length: 4
    public static final int DST_TOS = 55;                       // Length: 1
    public static final int SRC_MAC = 56;                       // Length: 6
    public static final int DST_MAC = 57;                       // Length: 6
    public static final int SRC_VLAN = 58;                      // Length: 2
    public static final int DST_VLAN = 59;                      // Length: 2
    public static final int IP_PROTOCOL_VERSION = 60;           // Length: 1
    public static final int DIRECTION = 61;                     // Length: 1
    public static final int IPV6_NEXT_HOP = 62;                 // Length: 16
    public static final int BGP_IPV6_NEXT_HOP = 63;             // Length: 16
    public static final int IPV6_OPTION_HEADERS = 64;           // Length: 4
    public static final int MPLS_LABEL_1 = 70;                  // Length: 3
    public static final int MPLS_LABEL_2 = 71;                  // Length: 3
    public static final int MPLS_LABEL_3 = 72;                  // Length: 3
    public static final int MPLS_LABEL_4 = 73;                  // Length: 3
    public static final int MPLS_LABEL_5 = 74;                  // Length: 3
    public static final int MPLS_LABEL_6 = 75;                  // Length: 3
    public static final int MPLS_LABEL_7 = 76;                  // Length: 3
    public static final int MPLS_LABEL_8 = 77;                  // Length: 3
    public static final int MPLS_LABEL_9 = 78;                  // Length: 3
    public static final int MPLS_LABEL_10 = 79;                 // Length: 3


}
