# This is to test directly call rasa nlu after training, rather than run rasa_nlu server.
# XKL modified on 08.02.2017
#
#
# 05.05.2017, modified to query the installed Rasa stable version which is using
# pipeline in config and with new functionalities like ner_crf
#
# 05.06.2017, modified to auto-read modelDir, testSetDir and configFile which are generated
# by Java preprocess. 
#
# 31.08.2017 Ver2: allow multiple query and evaluate by keeping info in the allGeneFileNames.txt
#
# 10.11.2017 extened to cover new items 'project' and 'fixed_model_name' in training config file.
# 24.03.2018 modified to query for Cross Validation -- CV
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

    # ./preprocessResults/out4RasaReal/rasa_json_2018_03_22-13_01_25_169_80Train/
    #   CrossValidation/KFold_1/testset/text
    parser = argparse.ArgumentParser(description='query RASA trained model.')
    parser.add_argument('--timestamp', dest='timestamp', action='store', metavar='TIMESTAMP', required=True,
                        help='The timestamp of generated data.')
    parser.add_argument('--kfold', dest='kfold', action='store', metavar='KFOLD', required=True,
                        help='order number of the kfold, e.g. 1,2,...')

    args = parser.parse_args()
    timestamp = ""
    if args.timestamp is not None:
        timestamp = args.timestamp
    else:
        raise RuntimeError,'Usage: python progName --timestamp tm --kfold Num'
   
    kfold = ""
    if args.kfold is not None:
        kfold = args.kfold
    else:
        raise RuntimeError,'Usage: python progName --timestamp tm --kfold Num'

    rootDirTop = "../.."
    
    textTestsetDir = rootDirTop + "/preprocessResults/autoGeneFromRealAnno/autoGene_"+timestamp + "/CrossValidation/KFold_" + kfold + "/testset/text"
    srcFiles = getFiles(textTestsetDir)
    rasaKfoldBaseDir = rootDirTop + "/preprocessResults/out4RasaReal/rasa_json_" + timestamp + "_80Train/CrossValidation/KFold_" + kfold
    confUri = rasaKfoldBaseDir + "/rasaTrainConfig/configLatest_KFold_" + kfold +"_spacy_sklearn_"+ timestamp + ".json"
    
    # ./trained_models/FinalNormMainV1/model_KFold_2_spacy_sklearn_2018_03_22-13_01_25_169
    modelDir = rootDirTop + "/trained_models/FinalNormMainV1/model_KFold_" + kfold + "_spacy_sklearn_" + timestamp
            
    metadata = Metadata.load(modelDir) # need to modify metadata.json for correct path
    
    print "Finish loading the model. Prepare Interpreter...."
    
    interpreter = Interpreter.load(metadata, RasaNLUConfig(confUri))
        
    print "Got the interpreter. Parsing ...."
   
    crntIdx = 1
    serviceName = "RASA"
    trainset = "KFold"
    trainTimeStamp = timestamp
    pipeline = "spacy_sklearn"
    baseDestDir = rootDirTop + "/testResults/" + serviceName + "/" + serviceName + "_TestResults_" 
    #baseDestDir = baseDestDir + trainset + "_" + pipeline + "_" + trainTimeStamp + "-" + str(crntIdx)
    baseDestDir = baseDestDir + trainset + "_" + trainTimeStamp + "-" + str(crntIdx)
    baseDestDir = baseDestDir +  "/KFold_" + kfold
    if(not os.path.exists(baseDestDir)):
        os.mkdir(baseDestDir)
    
    # new generated test input text files are contained in one directory
    # not have further subdirectory for each domain as before.
    # srcFiles = getFiles(srcBaseDir) === got from above.
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
    

