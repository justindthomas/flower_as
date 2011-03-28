import socket,os,struct,sys,time,base64
import SocketServer
from dpkt.ethernet import Ethernet
from socket import inet_ntoa as ntoa
from socket import inet_ntop as ntop
from socket import AF_INET, AF_INET6
from threading import Thread
from suds.client import Client
from sched import scheduler
from threading import Thread
from Queue import Queue
from urllib2 import URLError

alert_queue = Queue()
ALERTMSG_LENGTH=256
SNAPLEN=1500

fmt = "%ds9I%ds" % (ALERTMSG_LENGTH, SNAPLEN)
fmt_size = struct.calcsize(fmt)

class SnortProcessor(Thread):
	def __init__(self, logger, args, options):
		Thread.__init__(self)
		self.logger = logger
		self.args = args
		self.options = options
		self.server = None
		self.queue_processor = SnortQueueProcessor(self.logger, self.args, self.options)
	
	def run(self):
		self.logger.info("Starting Snort alert processor main thread...")
		
		try:
			os.setuid(int(self.options.uid))
		except OSError:
			self.logger.error("Could not change user for Snort alert processor")
			return
		
		try:
			os.remove(self.options.unsock)
		except OSError:
			pass
		
		self.queue_processor.start()
		
		self.logger.debug("Binding UNIX socket for Snort alerts at " + self.options.unsock)
		self.server = SnortAlertServer(self.logger, self.options.unsock, SnortAlertHandler)
		self.server.serve_forever()
	
	def stop(self):
		if(self.server != None):
			self.server.shutdown()
			
		self.queue_processor.stop()

class SnortAlertServer(SocketServer.UnixDatagramServer):
	def __init__(self, logger, socket, handler):
		SocketServer.UnixDatagramServer.__init__(self, socket, handler)
		self.logger = logger

class SnortAlertHandler(SocketServer.DatagramRequestHandler):
	def handle(self):
		try:
			alert_queue.put(self.request[0][:fmt_size])
				
		except struct.error, e:
			print "bad message? (msglen=%d): %s" % (len(datain), e.message)
	
	def finish(self):
		pass

class SnortQueueProcessor(Thread):
	def __init__(self, logger, args, options):
		Thread.__init__(self)
		self.logger = logger
		self.args = args
		self.options = options
		self.tasks = scheduler(time.time, time.sleep)
		self.stop_flag = False
		
	def run(self):
		self.logger.info("Starting Snort alert queue processor...")
		self.tasks.enter(5, 1, self.process, ('process', ))
		self.tasks.run()
		
	def process(self, name):
		palerts = []
		
		ws = 'http://' + self.args[0] + ':' + self.options.remote + '/flower/analysis/AlertsService?wsdl'
		self.logger.debug('Connecting to web service: ' + ws)
		
		try:
			client = Client(ws)
		
			while(not alert_queue.empty()):	
				(msg, ts_sec, ts_usec, caplen, pktlen, dlthdr, nethdr, transhdr, data, val, pkt) = struct.unpack(fmt, alert_queue.get_nowait())
				ethernet = Ethernet(pkt)
				ip = ethernet.data
			
				alert = msg.rstrip("\0")
			
				sport = None
				dport = None
				if(ip.p == 6 or ip.p == 17):
					sport = ip.data.sport
					dport = ip.data.dport
			
				packet = base64.b64encode(pkt)
			
				sourceType = client.factory.create("sourceType")
			
				palert = client.factory.create("persistentAlert")
				palert.type = sourceType.SNORT
				palert.date = ts_sec
				palert.usec = ts_usec
				palert.sourceAddress = self.parse_address(ip.src)
				palert.destinationAddress = self.parse_address(ip.dst)
				palert.sourcePort = sport
				palert.destinationPort = dport
				palert.alert = alert
				palert.packet = packet
			
				palerts.append(palert)
			
			response = client.service.addAlerts(palerts)
		
		except URLError:
			self.logger.error('Failed to connect to Snort alert web service')
		
		if(not self.stop_flag):
			self.tasks.enter(15, 1, self.process, ('process', ))
			
	def parse_address(self, data):
		if(len(data) == 16):
			return str(ntop(AF_INET6, data))
		elif(len(data) == 4):
			return str(ntop(AF_INET, data))
		else:
			return ""
	
	def stop(self):
		self.stop_flag = True
			