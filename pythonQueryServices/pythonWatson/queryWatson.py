#!/usr/bin/env python
# -*- coding: utf-8 -*-

# XKL modified queryApiai/py for Watson query.
# see Watson Python SDK examples conversation_v1.py

import os.path
import sys, argparse
import datetime
import json
from pprint import pprint
import time
from watson_developer_cloud import ConversationV1


def write2fileAppend(outfile, itemlist):
    with open(outfile, 'a') as f:
        f.write("\n".join(itemlist))

def saveJson2File(basePath, domainName, jdata):
    tstamp = datetime.datetime.utcnow().strftime('%Y%m%d-%H%M%S-%f')[:-3]
    dirname = os.path.join(basePath, domainName)
    if(not os.path.exists(dirname)):
        os.mkdir(dirname)
    fileName = "Res_" + domainName + "_" + tstamp + ".json"
    with open(os.path.join(dirname, fileName), 'w') as f:
        f.write(json.dumps(jdata, indent=4, sort_keys=True))
    
def readFile2List(infile):
    lines = []
    with open(infile) as file:
        for line in file:
            line = line.strip() #or strip the '\n'
            lines.append(line)
    return lines

def readJsonFileAsStr(infile):
    with open(infile) as json_data:
        d = json.load(json_data)

    return d
    
def getDirectories(indir):
    dirs = []
    for dirName, subdirList, fileList in os.walk(indir):
        if(dirName != indir):
            dirs.append(dirName)
    return dirs

def getFilesFromStr(filenamestr):
    '''
	To get file names array from string separated by comma
	first split str, then convert from list to array
    '''
    testfiles = []
    # from list to array
    for item in filenamestr.split(','): # comma, or other
        testfiles.append(item)
    return testfiles
    
def getFiles(indir):
    '''
	To get file names list in current directory
    '''
    crntDirFiles = []
    for dirName, subdirList, fileList in os.walk(indir):
        if(dirName == indir):
            crntDirFiles = fileList
            break

    return crntDirFiles

def queryWatson(utt):
    
    conversation = ConversationV1(
        # change to your own username and password
        username='uuuu',
        password='pppp',
        version='2017-04-21')

    # replace with your own workspace_id
    workspace_id = 'w-w-w-w'

    # 12.03.2018 the python sdk api changed. See  https://github.com/watson-developer-cloud/python-sdk
    # message_input --> input
    # response = conversation.message(workspace_id=workspace_id, message_input={'text': utt})
    #print(json.dumps(response, indent=2))
    response = conversation.message(workspace_id=workspace_id, input={'text': utt})
    
    return response

def getDictInfoFromFile(filename):
    #filename = '../allGeneFileNames/allGeneFileNames.txt'
    lines = readFile2List(filename)
    dictkv = {}
    for line in reversed(lines):
        if(line.startswith('#')):
            continue
        line = line.strip()
        if(line == ""):
            continue
        if(line == "=A_TASK_START="):
            break
            
        idx = line.find("=")
        key = line[:idx]
        val = line[idx+1:]
        print  key + " = " + val
        # only add the first one in reversed order, not overwrite it by new one which is previously saved.
        if key not in dictkv:
            dictkv[key]=val
    print " ======================= "        
    print "dict size = " + str(len(dictkv))

    return dictkv

def main():

    parser = argparse.ArgumentParser(description='query Watson web agent.')
    parser.add_argument('--testset', dest='testset', action='store', metavar='TESTSET', required=True,
                        help='The testset dir to be used, value can be: default or specified. If specified specifyTestSetInfo.txt will be read.')
    #parser.add_argument('--token', dest='token', action='store', metavar='TOKEN', required=True,
    #                    help='The APIAI client access token which you get when you create the agent.')
    args = parser.parse_args()
           
    testset = ""
    if args.testset is not None:
        testset = args.testset
    else:
        raise RuntimeError,'Usage: python progName --testset default/specified'
           
    testset = testset.lower()
    rootDirTop = "../.."    
    infoFile = rootDirTop + '/allGeneFileNames/allGeneFileNames.txt'
    dictkv = getDictInfoFromFile(infoFile)
    serviceName = dictkv["serviceName_WATSON"]
    trainset = dictkv["watsonActuallyUsedTrainset"]
    
    resultsIdx = dictkv["watsonLatestTestResultsIdx"] # used for label testResults dir to allow multiple tests
    crntIdx = int(resultsIdx) +1;
    srcFiles = []
    trainTimeStamp = dictkv["timeStamp"]
    textTestsetDir = ""
    annoTestsetDir = ""   # for saveInfo, used by evaluator
    if testset == "default":
        annoTestsetDir = dictkv["testSetAnnotated"]  # testSetAnnotated was added when first generating data
        textTestsetDir = dictkv["testSetPureTextDir"]
        srcFiles = getFiles(textTestsetDir)
    else:
        testInfoFile = rootDirTop + '/allGeneFileNames/specifyTestSetInfo.txt'
        testsetkv = getDictInfoFromFile(testInfoFile)
        testTimeStamp = testsetkv["testTimeStamp"]
        testFileStr = testsetkv["testIntentFiles"]
        testDirPref = rootDirTop + "/preprocessResults/autoGeneFromRealAnno/autoGene_"
        testDirSuff = "/DomainIntentNoDupShuffleSubset_Test/text"
        textTestsetDir = testDirPref + testTimeStamp + testDirSuff
        annoTestsetDir = testDirPref + testTimeStamp + "/DomainIntentNoDupShuffleSubset_Test/annotated"
        if testFileStr == "full":
            srcFiles = getFiles(textTestsetDir)
        else:
            srcFiles = getFilesFromStr(testFileStr)
    
    srcBaseDir = textTestsetDir
    baseDestDir = rootDirTop + "/testResults/" + serviceName
    if(not os.path.exists(baseDestDir)):
        os.mkdir(baseDestDir)
    
    baseDestDir = baseDestDir + "/" + serviceName + "_TestResults_" + trainset + "_" + trainTimeStamp + "-" + str(crntIdx)
    if(not os.path.exists(baseDestDir)):
        os.mkdir(baseDestDir)
        
    # save saveinfo -- append to file: allGeneFileNames/allGeneFileNames.txt
    crntTimeStamp = datetime.datetime.now().strftime('%Y%m%d-%H%M%S-%f')[:-3]
    saveinfo =[]
    saveinfo.append("\n# ---- written by queryWatsonAuto2.py:")
    saveinfo.append("crntTimeStamp=" + crntTimeStamp)
    saveinfo.append("watsonLatestTestResultsIdx=" + str(crntIdx)) 
    saveinfo.append("watsonLatestTestResultsDir=" + baseDestDir)  # evaluater read this       
    saveinfo.append("watsonTestSetAnnotated=" + annoTestsetDir)   # evaluator read this 
    saveinfo.append("watsonQueriedTestset="+ testset)        
    
    write2fileAppend(infoFile, saveinfo)
    
    numUtt = 500 # max num of utt to test
    totalUttProcessed = 0
    
    # new generated test input text files are contained in one directory
    # not have further subdirectory for each domain as before.
    # srcFiles = getFiles(srcBaseDir)  === got from above.
    start_time=time.time()    
    for i in range(len(srcFiles)):
        results = {'results': []}
        domainFile = srcFiles[i]
        print "srcFiles:" + domainFile

        fileUri = os.path.join(srcBaseDir, domainFile)
        print "fileUri:" + fileUri
        utterances = readFile2List(fileUri)
        num = numUtt  # numUtt is the max num of utt to test
        if(num > len(utterances) ):
            num = len(utterances)
            print "num of utt to test:" + str(num)
            
        for j in range(num):
            utt = utterances[j]
            utt = utt.lower()
            print "query:" + str(j) + " = " + utt
            res = queryWatson(utt)
            
            results['results'].append(res)
            totalUttProcessed = totalUttProcessed +1
            print "totalUttProcessed:" + str(totalUttProcessed)
            
            # wait a bit, then query again for another utt.
            #time.sleep(1.2) # Time in seconds.
            #time.sleep(0.3) # Time in seconds.
            time.sleep(0.2) # Time in seconds.  0.2 should be enough, as query itself takes time
            
        domainName = domainFile[0:domainFile.rfind(".")]
        print "domainName:" + domainName
        saveJson2File(baseDestDir, domainName, results)

    elapsed_time=time.time()-start_time
    print "==== Watson query finished ==== Elapsed Time: " + str(elapsed_time)
    
if __name__ == '__main__':
    main()

    
