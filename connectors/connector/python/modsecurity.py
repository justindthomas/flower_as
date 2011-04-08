# -*- coding: utf-8 -*-
import BaseHTTPServer
import httplib
from threading import Thread
from sched import scheduler
from suds.client import Client
from Queue import Queue
import re
import datetime
import time

pattern = re.compile("^--[a-f0-9]{8}-[ABCDEFGHIJKZ]--$")

modsecurity_raw_queue = Queue()
modsecurity_normalized_queue = Queue()

class ModSecurityTransferThread(Thread):
	def __init__(self, logger, args, options):
		Thread.__init__(self)
		self.logger = logger
		self.args = args
		self.options = options
		self.tasks = scheduler(time.time, time.sleep)
		self.stop_flag = False
	
	def run(self):
		self.tasks.enter(5, 1, self.process, ('process', ))
		self.tasks.run()
	
	def process(self, name):
		self.logger.debug("Processing mod_security transfer queue...")
		
		alerts = []
		
		while(not modsecurity_normalized_queue.empty()):
			sender, alert = modsecurity_normalized_queue.get_nowait()
			alerts.append(alert)
			
		if (len(alerts) != 0):
			try:
				protocol = "http://"
				if(self.options.ssl):
					protocol = "https://"
				ws = protocol + self.args[0] + ":" + self.options.remote + "/flower/analysis/AlertsService?wsdl"
				
				self.logger.debug("Preparing to send mod_security alerts to: " + ws)
				client = Client(ws)
				
				self.logger.debug("Sending mod_security alerts to server...")
				if(client.service.addModSecurityAlerts(alerts) != len(alerts)):
					self.logger.error("Unexpected response from Analysis Server while attempting to add new mod_security alerts")
				else:
					self.logger.debug("Server confirmed successful mod_security alerts transmission.")
					
			except URLError:
				self.logger.error("Unable to connect to Analysis Server from mod_security transfer thread")
			except WebFault:
				self.logger.error("Analysis Server responded with an error from mod_security transfer thread")
			except:
				self.logger.error(sys.exc_info())
				sys.exc_clear()
		
		if(not self.stop_flag):
			self.tasks.enter(15, 1, self.process, ('process', ))
				
	def stop(self):
		self.stop_flag = True

class ModSecurityQueueProcessor(Thread):
	def __init__(self, logger, args, options):
		Thread.__init__(self)
		self.logger = logger
		self.args = args
		self.options = options
		self.tasks = scheduler(time.time, time.sleep)
		self.stop_flag = False
	
	def run(self):
		self.tasks.enter(5, 1, self.process, ('process', ))
		self.tasks.run()
	
	def process(self, name):
		self.logger.debug("Processing mod_security raw alert queue...")
		
		while(not modsecurity_raw_queue.empty()):
			sender, data = modsecurity_raw_queue.get_nowait()
			id = ""
			record = []
			
			for line in data:
				if(pattern.match(line)):
					if(len(record) != 0):
						event[id] = record
						
					record = []
					id = line[11:12]
					continue
				
				if(id != ""):
					record.append(line)
			
			tokens = "".join(event['A']).split()
			dt = tokens[0] + " " + tokens[1]
			#tz = zoneinfo.gettz(dt[21:26])
			months = { 'Jan': 1, 'Feb': 2, 'Mar': 3, 'Apr': 4, 'May': 5, 'Jun': 6, 'Jul': 7, 'Aug': 8, 'Sep': 9, 'Oct': 10, 'Nov': 11, 'Dec': 12 }
			timestamp = datetime.datetime(int(dt[8:12]), int(months[dt[4:7]]), int(dt[1:3]), int(dt[13:15]), int(dt[16:18]), int(dt[19:21]), 0)
			epoch = int(time.mktime(timestamp.timetuple()))
			
			try:
				protocol = "http://"
				if(self.options.ssl):
					protocol = "https://"
				ws = protocol + self.args[0] + ":" + self.options.remote + "/flower/analysis/AlertsService?wsdl"
				
				client = Client(ws)
				palert = client.factory.create("modSecurityAlert")
				
				palert.date = epoch
				palert.sourceAddress = tokens[3]
				palert.sourcePort = tokens[4]
				palert.destinationAddress = tokens[5]
				palert.destinationPort = tokens[6]
				palert.auditLogHeader = "".join(event['A']).rstrip()
				
				if 'B' in event:
					palert.requestHeaders = "".join(event['B']).rstrip()
				if 'C' in event:
					palert.requestBody = "".join(event['C']).rstrip()
				if 'E' in event:
					palert.intermediateResponseBody = "".join(event['E']).rstrip()
				if 'F' in event:
					palert.finalResponseHeaders = "".join(event['F']).rstrip()
					
				palert.auditLogTrailer = "".join(event['H']).rstrip()
				
				modsecurity_normalized_queue.put((sender, palert))
				
			except URLError:
				self.logger.error("Unable to connect to Analysis Server to retrieve Alert WSDL")
			except WebFault:
				self.logger.error("Analysis Server responded with an error while attempting to retrieve Alert WSDL")
			except:
				self.logger.error(sys.exc_info())
				sys.exc_clear()
				
		if(not self.stop_flag):
			self.tasks.enter(15, 1, self.process, ('process', ))
				
	def stop(self):
		self.stop_flag = True
		
class ModSecurityHandler(BaseHTTPServer.BaseHTTPRequestHandler):
	def do_PUT(self):
		#print self.__dict__
		event = {}
		content_length = int(self.headers['Content-Length'])
		data = self.rfile.readlines(content_length)
		modsecurity_raw_queue.put((client_address[0], data))
		
class ModSecurityReceiver(Thread):
	def __init__(self, logger, args, options):
		Thread.__init__(self)
		self.logger = logger
		self.args = args
		self.options = options
		self.tasks = scheduler(time.time, time.sleep)
		self.stop_flag = False
		
		self.normalizer = ModSecurityQueueProcessor(self.logger, self.args, self.options)
		self.transfer = ModSecurityTransferThread(self.logger, self.args, self.options)
		
	def run(self):
		self.logger.info("Starting mod_security normalizer and transfer threads")
		self.normalizer.start()
		self.transfer.start()
		
		self.server = BaseHTTPServer.HTTPServer(('', int(self.options.modsecurity_port)), ModSecurityHandler)
		self.server.serve_forever()
	
	def stop(self):
		self.logger.info("Stopping mod_security processor...")
		
		if(self.server != None):
			self.server.shutdown()
			
		self.normalizer.stop()
		self.transfer.stop()
		self.normalizer.join()
		self.transfer.join()
		self.logger.info("mod_security processor thread completed."
		
#if __name__ == "__main__":
#	server = BaseHTTPServer.HTTPServer
#	server(('', PORT_NUMBER), ModSecurityHandler).serve_forever()