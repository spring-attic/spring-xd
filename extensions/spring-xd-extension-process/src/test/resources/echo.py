__author__ = 'David Turanski'
import sys
import os

sys.path.append(os.path.abspath('../../spring-xd-python/src/springxd'))
from stream import Processor

def echo(data):
    return data

processor =  Processor()
processor.start(echo)
