#!/usr/bin/env python

import SocketServer
from Queue import Queue
import socket
import sched
import time
from struct import unpack
from threading import Thread

class QueueProcessor(Thread):
	scheduler = sched.scheduler(time.time, time.sleep)
	stop_flag = False
	templates = {}
	
	def run(self):
		self.scheduler.enter(5, 1, self.process, ('process',))
		self.scheduler.run()
		
	def process(self,name):
		print "Processing packet queue..."

		while(not queue.empty()):
			sender, data = queue.get_nowait()
			version = unpack("!H", data[:2])[0]
			count = unpack("!H", data[2:4])[0]
			uptime = unpack("!I", data[4:8])[0]
			epoch = unpack("!I", data[8:12])[0]
			print ( version, count, uptime, epoch )
			
			if(version == 9):
				self.v9(sender, data[12:])
				
		if(not self.stop_flag):
			self.scheduler.enter(15, 1, self.process, ('process',))

	def v9(self, sender, data):
		sequence = unpack("!I", data[:4])[0]
		source = unpack("!I", data[4:8])[0]
		print ( sequence, source )

		flows = data[8:]
		while(len(flows) > 0):
			id = unpack("!H", flows[:2])[0]
			length = unpack("!H", flows[2:4])[0]
			print ( id, length )
			
			if(id == 0):
				self.template(sender, flows[4:length])
				
			flows = flows[length:]

	def template(self, sender, data):
		id = unpack("!H", data[:2])[0]
		field_count = unpack("!H", data[2:4])[0]
		print "Template: " + str((sender, id, field_count))

		fields = []
		field_data = data[4:]
		
		while(len(field_data) > 0):
			type = unpack("!H", field_data[:2])[0]
			length = unpack("!H", field_data[2:4])[0]
			fields.append((type, length))
			field_data = field_data[4:]

		print fields
		
	def stop(self):
		self.stop_flag = True
		
class NetflowCollector(SocketServer.DatagramRequestHandler):
	def handle(self):
		data = self.rfile.read(4096)
		client = self.client_address[0]
		queue.put((client, data))
		print("Queue Size: " + str(queue.qsize()) + " " + client)

	def finish(self):
		pass

class IPv6Server(SocketServer.UDPServer):
	address_family = socket.AF_INET6
	
if __name__ == "__main__":
	queue = Queue()
	thread = QueueProcessor()
	thread.start()
	HOST, PORT = "::", 9995
	server = IPv6Server((HOST, PORT), NetflowCollector)
	try:
		server.serve_forever()
	except KeyboardInterrupt:
		print "\nInstructing queue processor to shutdown..."
		thread.stop()
