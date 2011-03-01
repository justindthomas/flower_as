#!/usr/bin/env python

import socket,os,struct,sys,time,base64
from dpkt.ethernet import Ethernet
from socket import inet_ntoa as ntoa
from daemon import Daemon
from suds.client import Client

class SnortSocket(Daemon):
	def run(self):
		os.setuid(105)

		ALERTMSG_LENGTH=256
		SNAPLEN=1500

		s = socket.socket(socket.AF_UNIX, socket.SOCK_DGRAM)

		fmt = "%ds9I%ds" % (ALERTMSG_LENGTH, SNAPLEN)
		fmt_size = struct.calcsize(fmt)

		try:
			os.remove("/var/log/snort/snort_alert")
		except OSError:
			pass

		s.bind("/var/log/snort/snort_alert")

		while True:
			try:
				(datain, addr) = s.recvfrom(4096)
				(msg, ts_sec, ts_usec, caplen, pktlen, dlthdr, nethdr, transhdr, data, val, pkt) = struct.unpack(fmt, datain[:fmt_size])

				ethernet = Ethernet(pkt)
				ip = ethernet.data

				client = Client(self.server + '/flower/analysis/AlertsService?wsdl')

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

if __name__ == "__main__":
	daemon = SnortSocket('/tmp/snortsocket.pid')
	if len(sys.argv) == 2:
		if 'stop' == sys.argv[1]:
			daemon.stop();
		elif 'restart' == sys.argv[1]:
			daemon.restart();
		else:
			print "Unknown command"
			sys.exit(2)
		sys.exit(0);
	elif len(sys.argv) == 3:
		if 'start' == sys.argv[2]:
			daemon.server = sys.argv[1]
			daemon.start()
		else:
			print "Unknown command"
			sys.exit(2)
		sys.exit(0)
	else:
		print "usage: %s url start|stop|restart" % sys.argv[0]
		sys.exit(2)
