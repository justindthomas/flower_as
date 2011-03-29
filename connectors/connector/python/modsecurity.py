import BaseHTTPServer
import httplib
from threading import Thread
from sched import scheduler
from suds.client import Client
import re
import datetime
import time

HOST_NAME = 'localhost'
PORT_NUMBER = 9000
pattern = re.compile("^--[a-f0-9]{8}-[ABCDEFGHIJKZ]--$")

class ModSecurityHandler(BaseHTTPServer.BaseHTTPRequestHandler):
	def do_PUT(self):
		#print self.__dict__
		event = {}
		content_length = int(self.headers['Content-Length'])
		data = self.rfile.readlines(content_length)
		
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
		print (timestamp, epoch)
		ws = 'http://dev:8080/flower/analysis/AlertsService?wsdl'
		#self.logger.debug('Connecting to web service: ' + ws)
		client = Client(ws)
		sourceType = client.factory.create("sourceType")
		palert = client.factory.create("persistentAlert")
		
		palert.type = sourceType.MODSECURITY
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
		
		print palert
		
class ModSecurityReceiver(Thread):
	def __init__(self, logger, args, options):
		Thread.__init__(self)
		self.logger = logger
		self.args = args
		self.options = options
		self.tasks = scheduler(time.time, time.sleep)
		self.stop_flag = False
		
	def run(self):
		server = BaseHTTPServer.HTTPServer
		server(('', PORT_NUMBER), ModSecurityHandler).serve_forever()
		
if __name__ == "__main__":
	server = BaseHTTPServer.HTTPServer
	server(('', PORT_NUMBER), ModSecurityHandler).serve_forever()