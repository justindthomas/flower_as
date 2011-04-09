#!/usr/bin/env python
# -*- coding: utf-8 -*-

# To freeze on Debian stable, use:	python /usr/share/doc/python2.6/examples/Tools/freeze/freeze.py netflow.py -m encodings.ascii encodings.utf_8 encodings.idna

import optparse
import logging
import logging.handlers
import netflow
import snort
import modsecurity
from daemon import Daemon

class Collector(Daemon):
	def run(self):
		startup()

def stop_nicely():
	logger.info("Instructing threads to stop...")
	if (not options.nosnort):
		snort.stop()
	
	if (not options.nomodsecurity):
		modsecurity.stop()
		
	netflow_processor.stop()

	try:
		if (not options.nosnort):
			logger.info("Joining snort thread...")
			snort.join()
		
		if (not options.nomodsecurity):
			logger.info("Joining modsecurity thread...")
			modsecurity.join()
			
		logger.info("Joining netflow thread...")
		netflow_processor.join()
	except RuntimeError:
		logger.debug("RuntimeError encountered when joining threads.")

	logger.info("Collector shutdown completed")

def startup():
	if (not options.nosnort):
		try:
			snort.start()
		except:
			self.logger.error(sys.exc_info())
			sys.exc_clear()
	
	if (not options.nomodsecurity):
		modsecurity.start()
		
	netflow_processor.start()
	
	raw_input("press enter to end")
	stop_nicely()

if __name__ == "__main__":
	parser = optparse.OptionParser(description="Netflow to Flower connector", usage="usage: %prog [options] server")
	parser.add_option("-n", "--no-ssl", help="don't use HTTPS to connect to the server", action="store_false", dest="ssl", default=True)
	parser.add_option("-r", "--remote", help="specify the remote TCP server port (default: 8080)", dest="remote", default="8080")
	parser.add_option("-f", "--netflow-port", help="specify the local UDP listen port to receive netflows (default: 9995)", dest="netflow_port", default="9995")
	parser.add_option("-d", "--debug", help="enable more verbose logging", action="store_true", dest="debug", default=False)
	parser.add_option("-i", "--interactive", help="don't disconnect console (useful for debugging)", action="store_true", dest="interactive", default=False)
	parser.add_option("-s", "--stop", help="end a non-interactive process", action="store_true", dest="stop", default=False)
	
	parser.add_option("-S", "--disable-snort", help="disable Snort alert collector", action="store_true", dest="nosnort", default=False)
	parser.add_option("-k", "--socket", help="specify UNIX socket for Snort alerts", dest="unsock", default="/var/log/snort/snort_alert")
	parser.add_option("-u", "--uid", help="specify the numeric user ID of the Snort alert socket owner", dest="uid", default="105")
	
	parser.add_option("-M", "--disable-modsecurity", help="disable ModSecurity alert collector", action="store_true", dest="nomodsecurity", default=False)
	parser.add_option("-w", "--modsecurity-port", help="specify the local TCP listen port to receive ModSecurity alerts (default: 9000)", dest="modsecurity_port", default="9000")
	
	(options, args) = parser.parse_args()

	daemon = Collector('/tmp/flower.pid')

	LOG_FILENAME = "/var/log/flower/connector.log"
	logger = logging.getLogger('flower')

	if(not options.debug):
		logger.setLevel(logging.INFO)
	else:
		logger.setLevel(logging.DEBUG)

	handler = logging.handlers.RotatingFileHandler(LOG_FILENAME, maxBytes=2097152, backupCount=10)
	formatter = logging.Formatter("%(asctime)s - %(name)s - %(levelname)s - %(message)s")
	handler.setFormatter(formatter)

	logger.addHandler(handler)

	if (not options.nomodsecurity):
		modsecurity = modsecurity.ModSecurityReceiver(logger, args, options)
	else:
		logger.info("ModSecurity alert processor disabled")
		
	if (not options.nosnort):
		snort = snort.SnortProcessor(logger, args, options)
	else:
		logger.info("Snort alert processor disabled")
		
	netflow_processor = netflow.NetflowProcessor(logger, args, options)

	if(options.stop):
		stop_nicely()
		daemon.stop()
	else:
		if len(args) != 1:
			parser.error("Please specify the name or address of a Flower Analysis Server")

		if(options.debug):
			logger.debug("Debugging enabled")

		logger.info("Starting Netflow collector...")
		logger.info("Flower Analysis Server: " + args[0] + ":" + options.remote)
		logger.info("Listening on UDP port: " + options.netflow_port)
		if(not options.ssl):
			logger.info("SSL Disabled")

		if(options.interactive):
			startup()
		else:
			daemon = Collector('/tmp/flower.pid')
			daemon.start()
			sys.exit(0)
