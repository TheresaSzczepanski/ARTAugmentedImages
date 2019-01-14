# This is a Python Program
# Need to find the file directory that this python file is in
# Read all of the file names, display an Array that Java can use.

import sys
import os

# /Users/RTLocalUser/AndroidStudioProjects/ARTAugmentedImages
fileList = []
fileCounter = 0

for file in os.listdir((sys.path[0] + "/app/src/main/assets")):  
    if file.endswith(".jpg"):
        fileList.append(file)

print ("{")
for file in fileList:
    if ((fileCounter + 1) < len(fileList)):
        print ("\"" + file + "\",")
    else:
        print ("\"" + file + "\"")
    fileCounter = fileCounter + 1
print ("}")
