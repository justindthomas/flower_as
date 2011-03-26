import socket,os,struct,sys,time,base64
import SocketServer
from dpkt.ethernet import Ethernet
from socket import inet_ntoa as ntoa
from threading import Thread
from suds.client import Client

class SnortProcessor(Thread):
	def __init__(self, logger, args, options):
		Thread.__init__(self)
		self.logger = logger
		self.args = args
		self.options = options
		
	def run(self):
		self.logger.info("Starting Snort handler...")
		os.setuid(105)
		
		try:
			os.remove("/var/log/snort/snort_alert")
		except OSError:
			pass
		
		self.server = SnortAlertServer(self.logger, "/var/log/snort/snort_alert", SnortAlertHandler)
		self.server.serve_forever()
	
	def stop(self):
		self.server.shutdown()

class SnortAlertServer(SocketServer.UnixDatagramServer):
	def __init__(self, logger, socket, handler):
		SocketServer.UnixDatagramServer.__init__(self, socket, handler)
		self.logger = logger

class SnortAlertHandler(SocketServer.DatagramRequestHandler):
	def handle(self):
		try:
			ALERTMSG_LENGTH=256
			SNAPLEN=1500

			#s = socket.socket(socket.AF_UNIX, socket.SOCK_DGRAM)

			fmt = "%ds9I%ds" % (ALERTMSG_LENGTH, SNAPLEN)
			fmt_size = struct.calcsize(fmt)
			
			#(datain, addr) = s.recvfrom(4096)
			#(msg, ts_sec, ts_usec, caplen, pktlen, dlthdr, nethdr, transhdr, data, val, pkt) = struct.unpack(fmt, datain[:fmt_size])
			(msg, ts_sec, ts_usec, caplen, pktlen, dlthdr, nethdr, transhdr, data, val, pkt) = struct.unpack(fmt, self.request[0][:fmt_size])
			ethernet = Ethernet(pkt)
			ip = ethernet.data
			
			client = Client('http://localhost:8080/flower/analysis/AlertsService?wsdl')
			
			alert = msg.rstrip("\0")
			
			sport = None
			dport = None
			if(ip.p == 6 or ip.p == 17):
				sport = ip.data.sport
				dport = ip.data.dport
			
			packet = base64.b64encode(pkt)
			response = client.service.addAlert(ts_sec, ts_usec, ntoa(ip.src), ntoa(ip.dst), sport, dport, alert, packet)
			
		except struct.error, e:
			print "bad message? (msglen=%d): %s" % (len(datain), e.message)
