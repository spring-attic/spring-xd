__author__ = 'David Turanski'

from springxd.stream import Processor


def echo(data):
    return data

process = Processor()
process.start(echo)

