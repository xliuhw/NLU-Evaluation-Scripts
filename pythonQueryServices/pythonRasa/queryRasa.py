# This is to test directly call rasa nlu after training, rather than run rasa_nlu server.
# XKL modified on 08.02.2017
#
# 05.05.2017, modified to query the installed Rasa stable version which is using
# pipeline in config and with new functionalities like ner_crf
#
# 05.06.2017, modified to auto-read modelDir, testSetDir and configFile which are generated
# by Java preprocess. 
#
# 31.08.2017 Ver2: allow multiple query and evaluate by keeping info in the allGeneFileNames.txt
#
# 10.11.2017 extended to cover new items 'project' and 'fixed_model_name' in training config file.
#

import os.path
import sys, argparse
import json
import datetime
import time

from rasa_nlu.model import Metadata, Interpreter
from rasa_nlu.config import RasaNLUConfig
from rasa_nlu.model import Metadata

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
    To get file names array in current directory

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

def readJsonFileAsStr(infile):
    with open(infile) as json_data:
        d = json.load(json_data)

    return d

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
    

def main(argv):

    parser = argparse.ArgumentParser(description='query RASA trained model.')
    parser.add_argument('--testset', dest='testset', action='store', metavar='TESTSET', required=True,
                        help='The testset dir to be used, value can be: default or specified. If specified specifyTestSetInfo.txt will be read.')
    parser.add_argument('--pipeline', dest='pipeline', action='store', metavar='PIPELINE', required=True,
                        help='The RASA pipeline name used for training the model.')

    args = parser.parse_args()
    pipeline = ""
    if args.pipeline is not None:
        pipeline = args.pipeline
    else:
        raise RuntimeError,'Usage: python progName --testset default/specified --pipeline pipelineName'
   
    testset = ""
    if args.testset is not None:
        testset = args.testset
    else:
        raise RuntimeError,'Usage: python progName --testset default/specified  --pipeline pipelineName'

    pipeline = pipeline.lower()
    testset = testset.lower()
    rootDirTop = "../.."
    
    confFileField = ""
    if pipeline == "mitie" or pipeline == "spacy_sklearn" :
          confFileField = "rasaTrainConfigFile_" + pipeline
    else:
       raise RuntimeError,'Pipeline is not supported:' + pipeline
       
    infoFile = rootDirTop +  '/allGeneFileNames/allGeneFileNames.txt'
    dictkv = getDictInfoFromFile(infoFile)
    serviceName = dictkv["serviceName_RASA"]
    confDir = dictkv["rasaTrainConfigDir"]
    confFile = dictkv[confFileField]
    trainset = dictkv["rasaActuallyUsedTrainset"]
    
    resultsIdx = dictkv["rasaLatestTestResultsIdx"] # used for label testResults dir to allow multiple tests
    crntIdx = int(resultsIdx) +1;
    srcFiles = []
    trainTimeStamp = dictkv["timeStamp"]
    textTestsetDir = ""
    annoTestsetDir = ""   
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
        
    confUri = confDir + "/" + confFile    
    confJson = readJsonFileAsStr(confUri)
    modelName = ""
    if 'fixed_model_name' not in confJson: # Rasa 0.10.4, use fixed_model_name
        modelName = confJson["name"]  # previous version, use 'name'
    else:
        modelName = confJson["fixed_model_name"]
        
    trainedModelPath = confJson["path"]
    
    projName = ""
    if 'project' in confJson:
        projName = confJson["project"]    # Rasa 0.10.4, added 'project'
    
    print "loading the model ...."

    modelDir = trainedModelPath + "/" + modelName;
    if projName != "" :
        modelDir = trainedModelPath + "/" +  projName + "/" + modelName;
        
    metadata = Metadata.load(modelDir) # need to modify metadata.json for correct path
    
    print "Finish loading the model. Prepare Interpreter...."
    
    interpreter = Interpreter.load(metadata, RasaNLUConfig(confUri))
        
    print "Got the interpreter. Parsing ...."
   
    srcBaseDir = textTestsetDir
    #baseDestDir = "../testResults/RASA/RasaResults_" + trainTimeStamp
    baseDestDir = rootDirTop + "/testResults/" + serviceName + "/" + serviceName + "_TestResults_" 
    baseDestDir = baseDestDir + trainset + "_" + pipeline + "_" + trainTimeStamp + "-" + str(crntIdx)

    # save saveinfo -- append to file: allGeneFileNames/allGeneFileNames.txt
    crntTimeStamp = datetime.datetime.now().strftime('%Y%m%d-%H%M%S-%f')[:-3]
    saveinfo =[]
    saveinfo.append("\n# ---- written by queryRasaLatestAuto2.py:")
    saveinfo.append("crntTimeStamp=" + crntTimeStamp)
    saveinfo.append("rasaLatestTestResultsIdx=" + str(crntIdx)) #TODO rasa would use serviceName
    saveinfo.append("rasaLatestTestResultsDir=" + baseDestDir)  # evaluator read this        
    saveinfo.append("rasaQueriedModel_ConfDir=" + confDir)
    saveinfo.append("rasaQueriedModel_ConfFile=" + confFile)  
    saveinfo.append("rasaTestSetAnnotated=" + annoTestsetDir)      # evaluator read this 
    saveinfo.append("rasaQueriedPipeline="+ pipeline)            
    saveinfo.append("rasaQueriedTestset="+ testset)        
         
    write2fileAppend(infoFile, saveinfo)
   
    if(not os.path.exists(baseDestDir)):
        os.mkdir(baseDestDir)
    
    # new generated test input text files are contained in one directory
    # not have further subdirectory for each domain as before.
    # srcFiles = getFiles(srcBaseDir) === got from above.
    numUtt = 500 # maximum
    totalUttProcessed = 0
    start_time=time.time()
    for i in range(len(srcFiles)):

        results = {'results': []}
        #domainDir = srcDomainDirs[i] # take first one for test now
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
            #utt = u"\"" + utt + "\"";
            utt = u"" + utt;
            res = interpreter.parse(utt)
            results['results'].append(res)
            totalUttProcessed = totalUttProcessed +1
            print "totalUttProcessed:" + str(totalUttProcessed)
            # wait a bit, then query again for another utt.
            #time.sleep(0.2) # Time in seconds.
            
        domainName = domainFile[0:domainFile.rfind(".")]
        print "domainName:" + domainName
        saveJson2File(baseDestDir, domainName, results)

    elapsed_time=time.time()-start_time
    print "==== RASA query finished ==== Elapsed Time: " + str(elapsed_time)
           
if __name__ == "__main__":
    main(sys.argv[1:])
    

    
