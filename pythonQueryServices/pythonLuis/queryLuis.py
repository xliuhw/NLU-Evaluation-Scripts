'''
Copyright (c) Microsoft. All rights reserved.
Licensed under the MIT license.

Microsoft Cognitive Services (formerly Project Oxford): https://www.microsoft.com/cognitive-services

Microsoft Cognitive Services (formerly Project Oxford) GitHub:
https://github.com/Microsoft/ProjectOxford-ClientSDK

Copyright (c) Microsoft Corporation
All rights reserved.

MIT License:
Permission is hereby granted, free of charge, to any person obtaining
a copy of this software and associated documentation files (the
"Software"), to deal in the Software without restriction, including
without limitation the rights to use, copy, modify, merge, publish,
distribute, sublicense, and/or sell copies of the Software, and to
permit persons to whom the Software is furnished to do so, subject to
the following conditions:

The above copyright notice and this permission notice shall be
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED ""AS IS"", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
'''
# XL modified to test uploaded app. 13-03-2017

# 12.06.2017, modified to auto-read testSetDir info etc which are generated
# by Java preprocess. 
#
# 31.08.2017 Ver2: allow multiple query and evaluate by keeping info in the allGeneFileNames.txt
#

from luis_sdk import LUISClient

import os.path
import sys, argparse
import datetime
import json
from pprint import pprint
import time

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

def readFile2List(infile):
    lines = []
    with open(infile) as file:
        for line in file:
            line = line.strip() #or strip the '\n'
            lines.append(line)
    return lines

def write2fileAppend(outfile, itemlist):
    with open(outfile, 'a') as f:
        f.write("\n".join(itemlist))

def saveJson2File(dirNameInCrntDir, domainName, jdata):
    path = os.getcwd()
    path = os.path.join(path, dirNameInCrntDir)
    dirname = os.path.join(path, domainName)
    if(not os.path.exists(dirname)):
        os.mkdir(dirname)

    tstamp = datetime.datetime.utcnow().strftime('%Y%m%d-%H%M%S-%f')[:-3]        
    fileName = "Res_" + domainName + "_" + tstamp + ".json"
    with open(os.path.join(dirname, fileName), 'w') as f:
        f.write(json.dumps(jdata, indent=4, sort_keys=True))


def process_res(res):
    '''
    A function that processes the luis_response object and prints info from it.
    :param res: A LUISResponse object containing the response data.
    :return: None
    '''
    print res
    print(u'---------------------------------------------')
    print(u'LUIS Response: ')
    print(u'Query: ' + res.get_query())
    print(u'Top Scoring Intent: ' + res.get_top_intent().get_name())
    if res.get_dialog() is not None:
        if res.get_dialog().get_prompt() is None:
            print(u'Dialog Prompt: None')
        else:
            print(u'Dialog Prompt: ' + res.get_dialog().get_prompt())
        if res.get_dialog().get_parameter_name() is None:
            print(u'Dialog Parameter: None')
        else:
            print('Dialog Parameter Name: ' + res.get_dialog().get_parameter_name())
        print(u'Dialog Status: ' + res.get_dialog().get_status())
    print(u'Entities:')
    for entity in res.get_entities():
        print(u'"%s":' % entity.get_name())
        print(u'Type: %s, Score: %s' % (entity.get_type(), entity.get_score()))

def process_res_json(res):
    '''
     XL: process Luis query results and return a json object
    '''
    text = res.get_query() # the query utterance
    intent = res.get_top_intent().get_name()
    confidence = res.get_top_intent().get_score() # confidence of intent
    entities = []
    for entity in res.get_entities():
        print(u'"%s":' % entity.get_name())
        print(u'Type: %s, Score: %s' % (entity.get_type(), entity.get_score()))
        e_type = entity.get_type()
        print e_type
        e_value = entity.get_name()
        e_score = entity.get_score()
        e_start = entity.get_start_idx()
        e_end = entity.get_end_idx()
        entityJO = {"entity": e_type, "value": e_value, "score": e_score, "start": e_start, "end": e_end}
        entities.append(entityJO)
    
    
    resObj = {"text": text, "confidence": confidence, "intent": intent, "entities": entities}
    return resObj
    
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
        # only add the first one in reversed order, not overwrite it by new one which is previously saved.
        if key not in dictkv:
            dictkv[key]=val
    print " ======================= "        
    print "dict size = " + str(len(dictkv))

    return dictkv
    
def main():
    # 12.06.2017, modified to auto-read saved preprocessed info.
    #
    # 31.08.2017 Ver2: allow multiple query and evaluate by keeping info in the allGeneFileNames.txt
    #
    
    parser = argparse.ArgumentParser(description='query LUIS web agent.')
    parser.add_argument('--testset', dest='testset', action='store', metavar='TESTSET', required=True,
                        help='The testset dir to be used, value can be: default or specified. If specified specifyTestSetInfo.txt will be read.')

    parser.add_argument('--appid', dest='appid', action='store', metavar='APPID', required=True,
                        help='The APPID from LUIS agent which you get when you create the agent.')
    parser.add_argument('--appkey', dest='appkey', action='store', metavar='APPKEY', required=True,
                        help='The APPKEY from LUIS agent which you get when you create the agent.')
                        
    args = parser.parse_args()
    appID = ""
    appKey = ""    
    testset = ""
    if args.testset is not None:
        testset = args.testset
    else:
        raise RuntimeError,'Usage: python progName --testset default/specified  --appid id --appkey key'
    
    if args.appid is not None:
        appID = args.appid
    else:
        raise RuntimeError,'Usage: python progName --testset default/specified  --appid id --appkey key'
           
    if args.appkey is not None:
        appKey = args.appkey
    else:
        raise RuntimeError,'Usage: python progName --testset default/specified  --appid id --appkey key'

    testset = testset.lower()
    rootDirTop = "../.."
    infoFile = rootDirTop + '/allGeneFileNames/allGeneFileNames.txt'
    dictkv = getDictInfoFromFile(infoFile)
    serviceName = dictkv["serviceName_LUIS"]
    trainset = dictkv["luisActuallyUsedTrainset"]
    
    resultsIdx = dictkv["luisLatestTestResultsIdx"] # used for label testResults dir to allow multiple tests
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
    saveinfo.append("\n# ---- written by queryLuisAuto2.py:")
    saveinfo.append("crntTimeStamp=" + crntTimeStamp)
    saveinfo.append("luisLatestTestResultsIdx=" + str(crntIdx)) #TODO rasa would use serviceName
    saveinfo.append("luisLatestTestResultsDir=" + baseDestDir)  # evaluater read this       
    saveinfo.append("luisTestSetAnnotated=" + annoTestsetDir)      # evaluator read this 
    saveinfo.append("luisQueriedAppID="+ appID)
    saveinfo.append("luisQueriedAppKey="+ appKey)    
    saveinfo.append("luisQueriedTestset="+ testset)        
    
    write2fileAppend(infoFile, saveinfo)
    
    CLIENT = LUISClient(appID, appKey, True)
    
    srcBaseDir = textTestsetDir

    numUtt = 500 # maximum 
    totalUttProcessed = 0
    start_time=time.time()    
    for i in range(len(srcFiles)):    
        results = {'results': []}
        domainFile = srcFiles[i]
        print "srcFiles:" + domainFile

        fileUri = os.path.join(srcBaseDir, domainFile)
        print "fileUri:" + fileUri
        utterances = readFile2List(fileUri)
        num = numUtt
        if(num > len(utterances) ):
            num = len(utterances)
            print num
            
        for j in range(num):
            utt = utterances[j]
            utt = utt.lower()
            print "query:" + str(j) + " = " + utt
            res = CLIENT.predict(utt)
            res_j = process_res_json(res)
            results['results'].append(res_j)
            totalUttProcessed = totalUttProcessed +1
            print "totalUttProcessed:" + str(totalUttProcessed)
            
            # wait a bit, then query again for another utt.
            # LUIS limit: 5 calls Per seconds, 10K calls per month for Free Account
            #time.sleep(0.3) # Time in seconds.
            # time.sleep(0.6) # Time in seconds. For Pay-As-You-Go account, 50 per second
            time.sleep(0.2) # 0.2 should be enough, as query itself takes time
            
        domainName = domainFile[0:domainFile.rfind(".")]
        print "domainName:" + domainName
        
        saveJson2File(baseDestDir, domainName, results)
        
    elapsed_time=time.time()-start_time
    print "==== LUIS query finished ==== Elapsed Time: " + str(elapsed_time)

        
if __name__ == '__main__':
    main()


