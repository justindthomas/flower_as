import BaseHTTPServer
import httplib
from threading import Thread
from sched import scheduler

HOST_NAME = 'localhost'
PORT_NUMBER = 9000

class ModSecurityHandler(BaseHTTPServer.BaseHTTPRequestHandler):
	def do_PUT(self):
		print self.__dict__
		content_length = int(self.headers['Content-Length'])
		data = self.rfile.read(content_length)
		print str(data)
		
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