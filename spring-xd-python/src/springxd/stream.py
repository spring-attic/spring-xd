__author__ = 'David Turanski'
import sys
'''
Python 2 or 3 agnostic input function
'''
if sys.version_info >= (3,0):
	receive_input = input
else:
	receive_input = raw_input

class Encoders:
    CRLF, LF = range(2)

'''
Wraps a function and handles streaming I/O for request/reply communications to use the function as
a Spring XD processor.
NOTE: This implementation only works with LF, or CRLF encoding since reading single chars from stdin is not
standard nor portable.
'''
class Processor:

    def __init__(self, encoder = Encoders.CRLF):
        self.encoder = encoder

    '''
    Write data to stdout
    '''
    def send(self, data):
        sys.stdout.write(self.encode(data))
        sys.stdout.flush()

    '''
    encode data
    '''
    def encode(self,data):
        if self.encoder == Encoders.CRLF:
            data = data + "\r\n"
        elif self.encoder == Encoders.LF:
            data = data + "\n"
        return data
    '''
    decode data
    '''
    def decode(self,data):
        if self.encoder == Encoders.CRLF:
            data.rstrip("\r\n")
        elif self.encoder == Encoders.LF:
            data.rstrip("\n")
        return data

    '''
    Run the I/O loop with a user-defined function
    '''
    def start(self, func):
        while True:
            try:
                input = receive_input()
                if input:
                    data = self.decode(input)
                    self.send(func(data))
            except EOFError:
                break
            except KeyboardInterrupt:
                break
            

class Sink(Processor):
    '''
    Run the I/O loop with a user-defined function
    '''
    def start(self, func):
        while True:
            try:
                input = receive_input()
                if input:
                    data = self.decode(input)
                    func(data)
            except EOFError:
                break
            except EOFError:
                break
            except KeyboardInterrupt:
                break
