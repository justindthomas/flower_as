#!/usr/bin/env python

# To freeze on Debian stable, use:  python /usr/share/doc/python2.6/examples/Tools/freeze/freeze.py netflow.py -m encodings.ascii encodings.utf_8 encodings.idna

import SocketServer
import time
import optparse
import logging
import logging.handlers
import sys
from daemon import Daemon

from Queue import Queue
from struct import unpack
from sched import scheduler
from socket import inet_ntop, AF_INET, AF_INET6
from threading import Thread
from suds.client import Client
from suds import WebFault
from urllib2 import URLError

class TransferThread(Thread):
    tasks = scheduler(time.time, time.sleep)
    stop_flag = False

    def run(self):
        self.tasks.enter(5, 1, self.process, ('process', ))
        self.tasks.run()

    def process(self, name):
        logger.debug("Processing transfer queue...")

        try:
            protocol = "http://"

            if(options.ssl):
                protocol = "https://"

            logger.debug("Connecting to: " + protocol + args[0] + ":" + options.port + "/flower/analysis/FlowInsertService?wsdl")
            client = Client(protocol + args[0] + ":" + options.port + "/flower/analysis/FlowInsertService?wsdl")

            xflowset = client.factory.create("xmlFlowSet")
            while(not normalized.empty()):
                flow = normalized.get_nowait()
                xflow = client.factory.create("xmlFlow")
                xflow.sourceAddress = flow["source"]
                xflow.destinationAddress = flow["destination"]
                xflow.sourcePort = flow["sport"]
                xflow.destinationPort = flow["dport"]
                xflow.flags = flow["tcp_flags"]
                xflow.bytesSent = flow["in_bytes"] + flow["out_bytes"]
                xflow.packetsSent = flow["in_pkts"] + flow["out_pkts"]
                xflow.protocol = flow["protocol"]
                xflow.startTimeStamp = flow["first_switched"]
                xflow.lastTimeStamp = flow["last_switched"]
                xflowset.flows.append(xflow)
                #print xflow

            if(len(xflowset.flows) > 0):
                if(client.service.addFlows(xflowset) != 0):
                    logger.error("Unexpected response from Analysis Server while attempting to add new flows")

        except URLError:
            logger.error("Unable to connect to Analysis Server")
        except WebFault:
            logger.error("Unable to complete data transfer to Analysis Server")
            
        if(not self.stop_flag):
            self.tasks.enter(15, 1, self.process, ('process', ))

    def stop(self):
        self.stop_flag = True

class NetflowQueueProcessor(Thread):
    tasks = scheduler(time.time, time.sleep)
    stop_flag = False
    templates = {}

    fields = {"IN_BYTES":1, "IN_PKTS":2, "FLOWS":3, "PROTOCOL":4, "TOS":5, "TCP_FLAGS":6, "L4_SRC_PORT":7,
        "IPV4_SRC_ADDR":8, "SRC_MASK":9, "INPUT_SNMP":10, "L4_DST_PORT":11, "IPV4_DST_ADDR":12, "DST_MASK":13,
        "OUTPUT_SNMP":14, "IPV4_NEXT_HOP":15, "SRC_AS":16, "DST_AS":17, "BGP_IPV4_NEXT_HOP":18, "MUL_DST_PKTS":19,
        "MUL_DST_BYTES":20, "LAST_SWITCHED":21, "FIRST_SWITCHED":22, "OUT_BYTES":23, "OUT_PKTS":24, "IPV6_SRC_ADDR":27,
        "IPV6_DST_ADDR":28, "IPV6_SRC_MASK":29, "IPV6_DST_MASK":30, "IPV6_FLOW_LABEL":31, "ICMP_TYPE":32,
        "MUL_IGMP_TYPE":33, "SAMPLING_INTERVAL":34, "SAMPLING_ALGORITHM":35, "FLOW_ACTIVE_TIMEOUT":36,
        "FLOW_INACTIVE_TIMEOUT":37, "ENGINE_TYPE":38, "ENGINE_ID":39, "TOTAL_BYTES_EXP":40, "TOTAL_PKTS_EXP":41,
        "TOTAL_FLOWS_EXP":42, "MPLS_TOP_LABEL_TYPE":46, "MPLS_TOP_LABEL_IP_ADDR":47, "FLOW_SAMPLER_ID":48,
        "FLOW_SAMPLER_MODE":49, "FLOW_SAMPLER_RANDOM_INTERVAL":50, "DST_TOS":55, "SRC_MAC":56, "DST_MAC":57,
        "SRC_VLAN":58, "DST_VLAN":59, "IP_PROTOCOL_VERSION":60, "DIRECTION":61, "IPV6_NEXT_HOP":62, "BGP_IPV6_NEXT_HOP":63,
        "IPV6_OPTION_HEADERS":64, "MPLS_LABEL_1":70, "MPLS_LABEL_2":71, "MPLS_LABEL_3":72, "MPLS_LABEL_4":73,
        "MPLS_LABEL_5":74, "MPLS_LABEL_6":75, "MPLS_LABEL_7":76, "MPLS_LABEL_8":77, "MPLS_LABEL_9":78, "MPLS_LABEL_10":79}
	
    def run(self):
        self.tasks.enter(5, 1, self.process, ('process', ))
        self.tasks.run()
		
    def process(self, name):
        logger.debug("Processing NetFlow queue...")

        retry = []
        while(not netflows.empty()):
            sender, data = netflows.get_nowait()
            version = self.parse_number(data[:2])
            count = self.parse_number(data[2:4])
            uptime = self.parse_number(data[4:8])
            epoch = self.parse_number(data[8:12])
            #print (version, count, uptime, epoch)
			
            if(version == 9):
                #print("Data size: " + str(len(data)))
                result = self.v9(sender, count, epoch, uptime, data)
                if(result != None):
                    retry.append(result)

        for netflow in retry:
            netflows.put((sender, netflow))
				
        if(not self.stop_flag):
            self.tasks.enter(15, 1, self.process, ('process', ))

    def v9(self, sender, count, epoch, uptime, data):
        sequence = self.parse_number(data[12:16])
        source = self.parse_number(data[16:20])
        #print (sequence, source)
        flowsets = data[20:]

        counter = 0
        while(counter < count):
            id = self.parse_number(flowsets[:2])
            length = self.parse_number(flowsets[2:4])
            #print (id, length)

            flowset = flowsets[:length]
            flowsets = flowsets[length:]

            netflows = flowset[4:]

            if(id == 0):
                counter += self.parse_templates(sender, netflows)
            elif((sender in self.templates.keys()) and (id in self.templates[sender].keys())):
                while(counter < count and len(netflows) > 3):
                    flow_size = 0
                    #print "Flow bytes remaining: " + str(len(netflows))
                    #print "Counter: " + str(counter) + ", count: " + str(count)
                    counter += 1
                    flow = { "source":None, "destination":None, "protocol":None, "sport":None,
                        "dport":None, "flows":None, "in_bytes":0, "out_bytes":0, "in_pkts":0,
                        "out_pkts":0, "last_switched":None, "first_switched":None, "tcp_flags":0,
                        "version":None }
                    for type, location, size in self.templates[sender][id]:
                        if(type == self.fields["IPV4_SRC_ADDR"] or type == self.fields["IPV6_SRC_ADDR"]):
                            flow["source"] = self.parse_address(netflows[location:location + size])
                        if(type == self.fields["IPV4_DST_ADDR"] or type == self.fields["IPV6_DST_ADDR"]):
                            flow["destination"] = self.parse_address(netflows[location:location + size])
                        if(type == self.fields["PROTOCOL"]):
                            flow["protocol"] = self.parse_number(netflows[location:location + size])
                        if(type == self.fields["L4_SRC_PORT"]):
                            flow["sport"] = self.parse_number(netflows[location:location + size])
                        if(type == self.fields["L4_DST_PORT"]):
                            flow["dport"] = self.parse_number(netflows[location:location + size])
                        if(type == self.fields["FLOWS"]):
                            flow["flows"] = self.parse_number(netflows[location:location + size])
                        if(type == self.fields["IN_BYTES"]):
                            flow["in_bytes"] = self.parse_number(netflows[location:location + size])
                        if(type == self.fields["IN_PKTS"]):
                            flow["in_pkts"] = self.parse_number(netflows[location:location + size])
                        if(type == self.fields["OUT_BYTES"]):
                            flow["out_bytes"] = self.parse_number(netflows[location:location + size])
                        if(type == self.fields["OUT_PKTS"]):
                            flow["out_pkts"] = self.parse_number(netflows[location:location + size])
                        if(type == self.fields["LAST_SWITCHED"]):
                            end = ((epoch * 1000) - uptime + self.parse_number(netflows[location:location + size]))
                            flow["last_switched"] = end
                        if(type == self.fields["FIRST_SWITCHED"]):
                            start = ((epoch * 1000) - uptime + self.parse_number(netflows[location:location + size]))
                            flow["first_switched"] = start
                        if(type == self.fields["TCP_FLAGS"]):
                            flow["tcp_flags"] = self.parse_number(netflows[location:location + size])
                        if(type == self.fields["IP_PROTOCOL_VERSION"]):
                            flow["version"] = self.parse_number(netflows[location:location + size])

                        flow_size += size

                    normalized.put(flow)
                    netflows = netflows[flow_size:]
                #print normalized
                logger.debug("Normalized queue contains: " + str(normalized.qsize()) + " entries")
            else:
                counter = count
                return data

        return None

    def parse_address(self, data):
        if(len(data) == 16):
            return str(inet_ntop(AF_INET6, data))
        elif(len(data) == 4):
            return str(inet_ntop(AF_INET, data))
        else:
            return ""

    def parse_number(self, data):
        if(len(data) == 1):
            return unpack("!B", data)[0]
        if(len(data) == 2):
            return unpack("!H", data)[0]
        elif(len(data) == 4):
            return unpack("!I", data)[0]

    def parse_templates(self, sender, data):
        #print "Data: " + str(len(data))
        template_count = 0

        while(len(data) > 0):
            template_count += 1
            id = self.parse_number(data[:2])
            field_count = self.parse_number(data[2:4])
            #print "Template: " + str((sender, id, field_count))

            fields = []
            data = data[4:]

            count = 0
            location = 0
            while(count < field_count):
                count += 1
                type = self.parse_number(data[:2])
                length = self.parse_number(data[2:4])
                fields.append((type, location, length))
                location += length
                data = data[4:]

            if(not sender in self.templates.keys()):
                self.templates[sender] = {}

            self.templates[sender][id] = fields

        logging.debug("Templates Parsed: " + str(self.templates))
        return template_count
		
    def stop(self):
        self.stop_flag = True
		
class NetflowCollector(SocketServer.DatagramRequestHandler):
    def handle(self):
        data = self.rfile.read(4096)
        client = self.client_address[0]
        netflows.put((client, data))
        #print("Queue Size: " + str(netflows.qsize()) + " " + client)
        logger.debug("Netflow queue contains: " + str(netflows.qsize()) + " entries")

    def finish(self):
        pass

class IPv6Server(SocketServer.UDPServer):
    address_family = AF_INET6

class Collector(Daemon):
    def run(self):
        startup()

def stop_nicely():
    logger.info("Instructing normalizer and transfer threads to stop...")
    normalizer.stop()
    transfer.stop()

    try:
        normalizer.join()
        transfer.join()
    except RuntimeError:
        logger.debug("RuntimeError encountered when joining threads.")

    logger.info("Netflow collector shutdown completed")

def startup():
    normalizer.start()
    transfer.start()

    HOST, PORT = "::", 9995
    server = IPv6Server((HOST, PORT), NetflowCollector)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass

    stop_nicely()

if __name__ == "__main__":
    parser = optparse.OptionParser(description="Netflow to Flower connector", usage="usage: %prog [options] server")
    parser.add_option("-n", "--no-ssl", action="store_false", dest="ssl", default=True)
    parser.add_option("-p", "--port", dest="port", default="8080")
    parser.add_option("--debug", action="store_true", dest="debug", default=False)
    parser.add_option("-i", action="store_true", dest="interactive", default=False)
    parser.add_option("--stop", action="store_true", dest="stop", default=False)

    (options, args) = parser.parse_args()

    daemon = Collector('/tmp/netflowd.pid')

    LOG_FILENAME = "/var/log/netflow"
    logger = logging.getLogger('netflowd')

    if(not options.debug):
        logger.setLevel(logging.INFO)
    else:
        logger.setLevel(logging.DEBUG)

    handler = logging.handlers.RotatingFileHandler(LOG_FILENAME, maxBytes=16384, backupCount=5)
    formatter = logging.Formatter("%(asctime)s - %(name)s - %(levelname)s - %(message)s")
    handler.setFormatter(formatter)

    logger.addHandler(handler)

    netflows = Queue()
    normalized = Queue()

    normalizer = NetflowQueueProcessor()
    transfer = TransferThread()

    if(options.stop):
        stop_nicely()
        daemon.stop()
    else:
        if len(args) != 1:
            parser.error("Please specify the name or address of a Flower Analysis Server")

        if(options.debug):
            logger.debug("Debugging enabled")

        logger.info("Starting Netflow collector...")
        logger.info("Flower Analysis Server: " + args[0] + ":" + options.port)
        if(not options.ssl):
            logger.info("SSL Disabled")

        if(options.interactive):
            startup()
        else:
            daemon = Collector('/tmp/netflowd.pid')
            daemon.start()
            sys.exit(0)
