#
# Calculate micro-average by sum of TP/FP/FN of all classes (all intent names and/or entity names).
#
#  X. Liu @ HWU, 12.07.2018
#  Ver2

import os.path
import sys, getopt
import argparse
import json
#import datetime
from datetime import datetime
import time
#from decimal import Decimal
import csv
from collections import defaultdict
import pandas as pd
from collections import Counter
import codecs
import itertools
from difflib import SequenceMatcher
import re
     

def calc_kfolds_top():
    src_basedir = "./NewEvaluateResults_CV_PartialToken_MO"
    dest_basedir = "."
    dest_subdir = "micro_ave_res"                
    timestamp = "2018_03_22-13_01_25_169"
    
    services = ["RASA", "APIAI", "LUIS", "Watson"]    
    for service in services:
        calcScoresOverAllItemsInKfolds(src_basedir, dest_basedir, dest_subdir, service, timestamp)
    print "Finished -- calc_kfolds_top !"
    
def calc_R_top():
    # crntDir: /CV-Result-Calc-R-ReCalcScores-PartialToken_MO/pythonCalcScores
    src_basedir = "."
    src_subdir = "micro_ave_res"
    # APIAI_kfold_entity_2018_03_22-13_01_25_169.csv
    timestamp = "2018_03_22-13_01_25_169"
    calc_type = ["intent", "entity", "combined"]
    services = ["RASA", "APIAI", "LUIS", "Watson"]
    src_dir = src_basedir + "/" + src_subdir
    prepare_R_data(calc_type, services, timestamp, src_dir)
    
    print "Finished -- calc_R_top !"


def prepare_R_data(calc_type, services, timestamp, src_dir):
    # effectsize-csv/10Folds/intent, entity, combined
    # cv-f1-service-effectsize-r-intent-Luis-Watson.csv
    # cv-f1-service-effectsize-r-entity-Luis-Watson.csv
    # cv-f1-service-effectsize-r-comb-Apiai-Watson.csv
    # cv-all-10folds-intents-avg.csv
    # cv-all-10folds-entities-avg.csv    
    # cv-all-10folds-combined-avg.csv
    # cv-r-data-intents/entities/combined.csv

    # 1. prepare R significance test data   
    dest_dir = src_dir + "/signif-r-csv"
    if(not os.path.exists(dest_dir)):
        os.mkdir(dest_dir)
    
    for ctype in calc_type: # for intent, entity, combined
        f1serv_all = []
        f1serv_all.append("f1,service")        
        for service in services:
            f1serv_file = src_dir+"/" + service+"_f1service_" + ctype + "_" + timestamp + ".csv"
            f1serv = readFile2List(f1serv_file)
            f1serv_all.extend(f1serv)
        dest_file = dest_dir+"/" + "cv-r-data-" + ctype + "_" + timestamp + ".csv"
        saveListStringToFile(f1serv_all, dest_file)
        
    # 2. prepare Effect Size R data        

    for ctype in calc_type:
        #dest_dir = src_dir + "/effectsize-csv/10Folds/" + ctype
        dest_dir = os.path.join(src_dir, "effectsize-csv", "10Folds", ctype)
        if(not os.path.exists(dest_dir)):
            os.makedirs(dest_dir)
        combs = itertools.combinations(services, 2) # get all combinations of 2 elements in the list    
        for comb in combs:
            #s1 = comb[0].lower().capitalize()
            #s2 = comb[1].lower().capitalize()
            s1 = comb[0]
            s2 = comb[1]
            f1serv_2serv = []
            f1serv_2serv.append("f1,service")
            f1serv_file1 = src_dir+"/" + s1+"_f1service_" + ctype + "_" + timestamp + ".csv"
            f1serv = readFile2List(f1serv_file1)
            f1serv_2serv.extend(f1serv)
                        
            f1serv_file2 = src_dir+"/" + s2+"_f1service_" + ctype + "_" + timestamp + ".csv"
            f1serv = readFile2List(f1serv_file2)
            f1serv_2serv.extend(f1serv)
            
            # save to file
            #cv-f1-service-effectsize-r-intent-Luis-Watson.csv
            filename = "cv-f1-service-effectsize-r-" +ctype +"-" + s1 + "-" +s2 + ".csv"
            dest_file = dest_dir + "/" + filename
            print dest_file
            saveListStringToFile(f1serv_2serv,dest_file)

    # 3. prepare overall average data of all service with Pre/Rec/F1, for presentation.
    
    # in macro_ave_res/Watson_kfold_entity_2018_03_22-13_01_25_169.csv
    # kfold	tp	fp	fn	tn	pre	rec	f1
    # sum	7281	13015	9133	521185	3.5847	4.4349	3.9643
    dest_dir = src_dir + "/overall_scores"
    if(not os.path.exists(dest_dir)):
        os.mkdir(dest_dir)

    kfoldsize = 10
    for ctype in calc_type: # for intent, entity, combined 
        ave_all = []
        ave_all.append("," + "Precision,Recall,F1 Score")  
        
        for service in services:
            src_file = src_dir+"/" + service+"_kfold_" + ctype + "_" + timestamp + ".csv"
            allfolds = pd.read_csv(src_file, encoding='UTF-8')

            for inst_no, inst in allfolds.iterrows():
                # header: kfold	tp	fp	fn	tn	pre	rec	f1
                # kfold = 'KFold_X', or 'sum'
                # print inst['f1']
                if inst['kfold'] == "sum":
                    ave_pre = 1.0 * inst['pre']/kfoldsize
                    ave_rec = 1.0 * inst['rec']/kfoldsize
                    ave_f1 = 1.0 * inst['f1']/kfoldsize

                    ave_pre = "%.4f" % ave_pre
                    ave_rec = "%.4f" % ave_rec
                    ave_f1 = "%.4f" % ave_f1
                                                            
                    break
             #           
            ave_str = service + "," + str(ave_pre) + "," + str(ave_rec) + "," + str(ave_f1)
            ave_all.append(ave_str)
        # save to file: cv-all-10folds-intents-avg.csv
        filename = "cv-all-10folds-" +ctype +"-avg" + ".csv"
        dest_file = dest_dir + "/" + filename
        saveListStringToFile(ave_all,dest_file)
             
        
def testCombinations():
    alist = ["rasa", "apiai", "luis", "watson"]
    combs = itertools.combinations(alist, 2) # get all combinations of 2 elements in the list
    max_utt = ""
    max_score = 0.0
    max_utt1 = ""
    max_utt2 = ""
    for comb in combs:
        print comb[0] + "--" + comb[1]

def getSumInAllFolds(res_csv_allfolds):
    tp,fp,fn,tn = 0,0,0,0
    pre,rec,f1=0.0,0.0,0.0
    for row in res_csv_allfolds: # each row for one fold
        tp += int(row[1])
        fp += int(row[2])
        fn += int(row[3])
        tn += int(row[4])
        pre += float(row[5])
        rec += float(row[6])
        f1 += float(row[7])
        
    # keep 4 digits precision: "%.4f" % pre
    pre_s = "%.4f" % pre
    rec_s = "%.4f" % rec
    f1_s = "%.4f" % f1
    sumlist = ["sum",str(tp),str(fp),str(fn),str(tn), pre_s,rec_s,f1_s]
    return sumlist
    
def getF1ServiceInAllFolds(res_csv_allfolds, service):
    f1=0.0
    res_csv_allfolds_f1serv = []
    for row in res_csv_allfolds: # each row for one fold
        if row[0] == "sum":
            continue
        f1 = float(row[7])
        f1serv = "%.4f" % f1
        res_csv_allfolds_f1serv.append(str(f1serv) +"," + service.lower())
        
    return res_csv_allfolds_f1serv

def calcScoresOverAllItemsInKfolds(src_basedir, dest_basedir, dest_subdir, service, timestamp): 
    # TODO separate Intent and Entity from one method
    
    cvDir = src_basedir + "/" + service + "/" +service + "_EvalRes_KFold_" + timestamp + "-1/CV"
    
    kfolds = os.listdir(cvDir)
    kfolds.sort()

    file_int = service + "_IntentConfRes_NumsCsv_" + timestamp + ".csv"    
    file_ent = service + "_EntityConfRes_NumsCsv_" + timestamp + ".csv"
    res_csv_int = []
    res_csv_ent = []
    res_csv_comb = []          
    #res_csv_int.append("kfold,tp,fp,fn,tn,pre,rec,f1")
    #res_csv_ent.append("kfold,tp,fp,fn,tn,pre,rec,f1")
        
    for kfold in kfolds:
        filedir = cvDir + "/" + kfold + "/csv"
                
        fileuri_int = filedir + "/" + file_int
        fileuri_ent = filedir + "/" + file_ent
        score_int = getSumOfConfusionTable(fileuri_int,kfold) # a str for each fold
        score_int = score_int.split(",")
        score_ent = getSumOfConfusionTable(fileuri_ent,kfold)
        score_ent = score_ent.split(",")        
        
        score_comb = getSumOfConfusionTable_Comb(fileuri_int,fileuri_ent,kfold)
        score_comb = score_comb.split(",")
        res_csv_comb.append(score_comb)
        
        res_csv_int.append(score_int)
        res_csv_ent.append(score_ent)
        
    int_sum = getSumInAllFolds(res_csv_int)
    res_csv_int.append(int_sum)
    
    ent_sum = getSumInAllFolds(res_csv_ent)
    res_csv_ent.append(ent_sum)

    comb_sum = getSumInAllFolds(res_csv_comb)
    res_csv_comb.append(comb_sum)
    res_csv_int_f1serv = []
    res_csv_ent_f1serv = []
    res_csv_comb_f1serv = []        
    res_csv_int_f1serv.extend(getF1ServiceInAllFolds(res_csv_int,service))
    res_csv_ent_f1serv.extend(getF1ServiceInAllFolds(res_csv_ent,service))
    res_csv_comb_f1serv.extend(getF1ServiceInAllFolds(res_csv_comb,service)) 
    
    labels = ["kfold","tp","fp","fn","tn","pre","rec","f1"]
    df_int = pd.DataFrame.from_records(res_csv_int, columns=labels)
    df_ent = pd.DataFrame.from_records(res_csv_ent, columns=labels)
    df_comb = pd.DataFrame.from_records(res_csv_comb, columns=labels)      

    #df_int_f1 = pd.DataFrame.from_records(res_csv_int_f1serv)
    #df_ent_f1 = pd.DataFrame.from_records(res_csv_ent_f1serv)
    
    dest_dir = dest_basedir + "/" + dest_subdir
    if(not os.path.exists(dest_dir)):
        os.mkdir(dest_dir)
    
    dest_file = dest_dir+"/" + service+"_kfold_intent_" + timestamp + ".csv"
    df_int.to_csv(dest_file, sep=',', encoding='utf-8', index=False)

    dest_file = dest_dir+"/" + service+"_kfold_entity_" + timestamp + ".csv"
    df_ent.to_csv(dest_file, sep=',', encoding='utf-8', index=False)

    dest_file = dest_dir+"/" + service+"_kfold_combined_" + timestamp + ".csv"
    df_comb.to_csv(dest_file, sep=',', encoding='utf-8', index=False)

    #dest_file = dest_dir+"/" + service+"_f1service_intent_" + timestamp + ".csv"
    #df_int_f1.to_csv(dest_file, sep=',', encoding='utf-8', index=False)

    #dest_file = dest_dir+"/" + service+"_f1service_entity_" + timestamp + ".csv"
    #df_ent_f1.to_csv(dest_file, sep=',', encoding='utf-8', index=False)

    dest_file = dest_dir+"/" + service+"_f1service_intent_" + timestamp + ".csv"
    saveListStringToFile(res_csv_int_f1serv,dest_file)
    
    dest_file = dest_dir+"/" + service+"_f1service_entity_" + timestamp + ".csv"
    saveListStringToFile(res_csv_ent_f1serv,dest_file)

    dest_file = dest_dir+"/" + service+"_f1service_combined_" + timestamp + ".csv"
    saveListStringToFile(res_csv_comb_f1serv,dest_file)

def readFile2List(infile):
    lines = []
    with open(infile) as file:
        for line in file:
            line = line.strip() #or strip the '\n'
            lines.append(line)
    return lines
    
def saveListStringToFile(liststr, fileuri):
    outfile = open(fileuri, 'w')
    outfile.write("\n".join(liststr))
    outfile.write("\n")
    outfile.close()
    
def getSumOfConfusionTable(csvConfTbl, kfold):
    #filedir = "..../NewEvaluateResults_CV/RASA/RASA_EvalRes_KFold_2018_03_22-13_01_25_169-1/CV/KFold_1/csv"
    
    #csvConfTbl = filedir + "/RASA_EntityConfRes_NumsCsv_2018_03_22-13_01_25_169.csv"
    df = pd.read_csv(csvConfTbl, sep=';',encoding='UTF-8')
        
    num = 0
    tp = 0
    fp = 0
    fn = 0
    tn = 0
    for row_index, row in df.iterrows():
        #print "process orig token:" + str(row['token'])
        tp += row['TP']
        fp += row['FP']
        fn += row['FN']
        tn += row['TN']
                                
    #  precision = tp/(tp+fp),  recall = tp / (tp + fn) , f1 = 2*p*r / (p+r)
    pre = 1.0*tp/(tp+fp)
    rec = 1.0*tp / (tp + fn)
    f1 = 2.0*pre*rec / (pre+rec)

    #print "tp -- fp -- fn -- tn -- pre -- rec -- f1"
    sumall = kfold + "," + str(tp) + "," + str(fp) + "," + str(fn) + "," + str(tn) + "," + str(pre) + "," + str(rec) + "," + str(f1)
    print  str(sumall)
    
    return sumall
                 
    # index=False: do not store the preceding indices of each row of the DataFrame object.
    #dest_file = "tokens-FullEval-Valiated2.csv"
    #df.to_csv(dest_file, sep=',', encoding='utf-8', index=False)

def getSumOfConfusionTable_Comb(csvConfTbl_int,csvConfTbl_ent, kfold):
    #filedir = "..../NewEvaluateResults_CV/RASA/RASA_EvalRes_KFold_2018_03_22-13_01_25_169-1/CV/KFold_1/csv"
    
    #csvConfTbl = filedir + "/RASA_EntityConfRes_NumsCsv_2018_03_22-13_01_25_169.csv"
    df_int = pd.read_csv(csvConfTbl_int, sep=';',encoding='UTF-8')
    df_ent = pd.read_csv(csvConfTbl_ent, sep=';',encoding='UTF-8')
        
    num = 0
    tp = 0
    fp = 0
    fn = 0
    tn = 0
    for row_index, row_int in df_int.iterrows():
        #print "process orig token:" + str(row['token'])
        tp += row_int['TP']
        fp += row_int['FP']
        fn += row_int['FN']
        tn += row_int['TN']

    for row_index, row_ent in df_ent.iterrows():
        #print "process orig token:" + str(row['token'])
        tp += row_ent['TP']
        fp += row_ent['FP']
        fn += row_ent['FN']
        tn += row_ent['TN']
                                
    #  precision = tp/(tp+fp),  recall = tp / (tp + fn) , f1 = 2*p*r / (p+r)
    pre = 1.0*tp/(tp+fp)
    rec = 1.0*tp / (tp + fn)
    f1 = 2.0*pre*rec / (pre+rec)

    #print "tp -- fp -- fn -- tn -- pre -- rec -- f1"
    sumall = kfold + "," + str(tp) + "," + str(fp) + "," + str(fn) + "," + str(tn) + "," + str(pre) + "," + str(rec) + "," + str(f1)
    #sumall = str(tp) + "," + str(fp) + "," + str(fn) + "," + str(tn) + "," + str(pre) + "," + str(rec) + "," + str(f1)
    #print  str(sumall)
    
    return sumall
                 
    # index=False: do not store the preceding indices of each row of the DataFrame object.
    #dest_file = "tokens-FullEval-Valiated2.csv"
    #df.to_csv(dest_file, sep=',', encoding='utf-8', index=False)

    
def main(argv):

    # ---------------------------------------------------
    calc_kfolds_top()
    calc_R_top()
    
if __name__ == "__main__":
    main(sys.argv[1:])
    
    
    
    
    
    
    
