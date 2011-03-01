This is the Snort IDS connector.  To use this package, you must have Python 2.6 with
the SUDS (python-suds package in Debian) web services package installed.

Snort must be configured to log to a UNIX socket at /var/log/snort/snort_alert (the
default location for Debian).  Snort will log to a UNIX socket if the "-A unsock" flag
is passed on startup.  The location can be specified using the -l flag (e.g.,
"-l /var/log/snort"). The connector assumes that snort_alert will be found in
/var/log/snort.

Once that is configured, the connector may be started by issuing the following command:

./connector.py http://localhost:8080 start

The URL will differ based on where your Analysis server is - the above assumes that it
is running on the same server as the connector, but that is not necessarily the case.
