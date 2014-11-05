__author__ = 'David Turanski'

import os
from springxd.stream import Sink

def receive(data):
    print data

sink = Sink()
sink.start(receive)


