#!/usr/bin/env python

# To freeze on Debian stable, use:	python /usr/share/doc/python2.6/examples/Tools/freeze/freeze.py netflow.py -m encodings.ascii encodings.utf_8 encodings.idna

import optparse
import logging
import logging.handlers
import netflow
import snort
from daemon import Daemon

class Collector(Daemon):
	def run(self):
		startup()

def stop_nicely():
	logger.info("Instructing threads to stop...")
	snort.stop()
	netflow_processor.stop()

	try:
		logger.info("Joining snort thread...")
		snort.join()
		logger.info("Joining netflow thread...")
		netflow_processor.join()
	except RuntimeError:
		logger.debug("RuntimeError encountered when joining threads.")

	logger.info("Netflow collector shutdown completed")

def startup():
	snort.start()
	netflow_processor.start()
	
	raw_input("press enter to end")
	stop_nicely()

if __name__ == "__main__":
	parser = optparse.OptionParser(description="Netflow to Flower connector", usage="usage: %prog [options] server")
	parser.add_option("-n", "--no-ssl", help="don't use HTTPS to connect to the server", action="store_false", dest="ssl", default=True)
	parser.add_option("-r", "--remote", help="specify the remote TCP server port (default: 8080)", dest="remote", default="8080")
	parser.add_option("-l", "--local", help="specify the local UDP listen port to receive netflows (default: 9995)", dest="local", default="9995")
	parser.add_option("-d", "--debug", help="enable more verbose logging", action="store_true", dest="debug", default=False)
	parser.add_option("-i", "--interactive", help="don't disconnect console (useful for debugging)", action="store_true", dest="interactive", default=False)
	parser.add_option("-s", "--stop", help="end a non-interactive process", action="store_true", dest="stop", default=False)

	(options, args) = parser.parse_args()

	daemon = Collector('/tmp/flower.pid')

	LOG_FILENAME = "/var/log/netflower"
	logger = logging.getLogger('netflower')

	if(not options.debug):
		logger.setLevel(logging.INFO)
	else:
		logger.setLevel(logging.DEBUG)

	handler = logging.handlers.RotatingFileHandler(LOG_FILENAME, maxBytes=16384, backupCount=5)
	formatter = logging.Formatter("%(asctime)s - %(name)s - %(levelname)s - %(message)s")
	handler.setFormatter(formatter)

	logger.addHandler(handler)

	snort = snort.SnortProcessor(logger, args, options)
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
		logger.info("Listening on UDP port: " + options.local)
		if(not options.ssl):
			logger.info("SSL Disabled")

		if(options.interactive):
			startup()
		else:
			daemon = Collector('/tmp/flower.pid')
			daemon.start()
			sys.exit(0)