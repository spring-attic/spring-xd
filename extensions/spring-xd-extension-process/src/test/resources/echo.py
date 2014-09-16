import sys

#=====================
# Write data to stdout
#=====================
def send(data):
	sys.stdout.write(data)
	sys.stdout.flush()

#===============================================================================
# Terminate a message, in this case to be consumed using ByteArrayCrlfSerializer
#===============================================================================
def eod():
	send("\r\n")

#===========================
# Main - Echo the input
#===========================
if len(sys.argv) > 1 :
	send("running " + sys.argv[0])
	eod()

while True:
	try:
   		data = raw_input()
   		if not data:
   			break
   		send(data)
   		eod()
   	except EOFError:
   		eod()
   		break