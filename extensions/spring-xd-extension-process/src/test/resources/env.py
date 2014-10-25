import os
import sys
import time
print os.environ['FOO'] + os.environ['BAR']
sys.stdout.flush()
time.sleep(10)
