/**
 * ************************************************************
 * @(#)PrepareRasaCorpora.java
 *
 *
 *              Copyright(C) 2016 iLab MACS
 *              Heriot-Watt University
 *
 *                  All rights reserved
 *
 *   Version: 0.1  Created on 31 Jan 2017
 *
 *   Author: Dr Xingkun Liu, Dr Verena Rieser
 *
 *   Project: Emotech/TheDataLab
 *
 *************************************************************
 */

package uk.ac.hw.macs.ilab.nlu.corpus;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import uk.ac.hw.macs.ilab.nlu.util.GeneralUtils;

/**
 *
 * @author X.Liu, V.Rieser
 */
public class PreprocessRealData {

    static final String CONFIG_FILE = "./resources/config/Config_DataPreprocess.txt"; 
    /*
    final String[] music = {"music_dislikeness.csv","music_likeness.csv","music_play.csv", 
                            "music_query.csv", "music_settings.csv"};
    final String[] audio = {"audio_volume_down.csv","audio_volume_mute.csv",
                            "audio_volume_other.csv","audio_volume_up.csv"};
    final String[] iot = {"iot_hue_changecolor.csv", "iot_hue_lightdim.csv", "iot_hue_lightoff.csv", 
        "iot_hue_lightup.csv", "iot_cleaning.csv", "iot_coffee.csv", "iot_wemo_off.csv", "iot_wemo_on.csv"};
    final String[] news = {"news_query.csv"};
    final String[] weather = {"weather_query.csv"};
    final String[] alarm = {"alarm_query.csv", "alarm_remove.csv","alarm_set.csv"};
    final String[] calendar = {"calendar_query.csv", "calendar_delete.csv","calendar_set.csv"};
    final String[] audiobook = {"audiobook_play.csv"};
    */
    
    List<String> FinalExceptIntents; // from config file
    List<String> OnlyIncludeIntents; // from config file

    // -------------- Params to be overwritten from the config file ----------------
    String destRootDirTop = ".";
    boolean ExcludeSingleWord = false; // true to remove single word utt. in removeDup.. method    
    int trainPercent = 80; // when it is 100, means to test trainset(use all to train), i.e. testset is part of the trainset
    int testPercent = 20;  // set config file notes.
    int maxNumUtt = 500; 
    int minNumUtt = 10;

    String IntentTier = "TIER1"; // see the config file
    String origAnnoCsvUrl = "";
    Properties configProperties; // by loadConfig, and to save to new file for records.
    int maxNumLongUtt = 30; // writen by config
    int numKFold = 0; // 0: do not create data for cross validation. Overwritten by config file
    // serviceName default: RASA. It is not yet used in this class, load from config file
    // in this class to force to specify it as this is the first step of the processes.
    String serviceName = IntEntUtils.ServiceName_RASA;
    // -----------------------------------------
     
     
    public PreprocessRealData() {

        
        loadConfig();
        
        System.out.println("==== PreprocessRealData ==== ");
        System.out.println("destRootDirTop = " + destRootDirTop);
        System.out.println("ExcludeSingleWord = " + ExcludeSingleWord);
        System.out.println("trainPercent = " + trainPercent);
        System.out.println("maxNumUtt = " + maxNumUtt);
        System.out.println("minNumUtt = "+minNumUtt);
        System.out.println("maxNumLongUtt = "+maxNumLongUtt);

        System.out.println("IntentTier = " + IntentTier);
        System.out.println("origAnnoCsvUrl = " + origAnnoCsvUrl);
        System.out.println("====  Starting process ........");
        
        initCreateDirs();    // for the first time deployment
        
        preprocessMain_RealAnno();
        
    }
    
    /**
     * Check if the required directories exist. If no, create them.
     * This is mainly for the first deployment in a new machine.
     * 
     */
    private void initCreateDirs() {
        GeneralUtils.checkAndCreateDirectory(destRootDirTop + "/allGeneFileNames" );
        GeneralUtils.checkAndCreateDirectory(destRootDirTop + "/evaluateResults/RASA" );
        GeneralUtils.checkAndCreateDirectory(destRootDirTop + "/preprocessResults/autoGeneFromRealAnno" );
        GeneralUtils.checkAndCreateDirectory(destRootDirTop + "/trained_models" );
        GeneralUtils.checkAndCreateDirectory(destRootDirTop + "/testResults/RASA" );
    }
    
    private void preprocessMain_RealAnno() {
        
        boolean valid = validateAnnotationLabel_RealAnno(origAnnoCsvUrl);
      
        if(!valid) {
           System.exit(-1);
        }
              
        List<String> procInfo = new ArrayList<>(); // size of each domain, entity names/values etc.
        List<String> procInfo_EV_All = new ArrayList<>();
        
        procInfo.add("Original CSV file=" + origAnnoCsvUrl);
        procInfo.add("\nmaxiNumUtt=" + maxNumUtt);
        procInfo.add("minNumUtt=" + minNumUtt);
        procInfo.add("minNumUtt=" + minNumUtt);
        
        procInfo.add("trainsetPercent=" + trainPercent);
        procInfo.add("testsetPercent=" + testPercent);
        procInfo.add("IntentTier=" +IntentTier);
        procInfo.add("ExcludeSingleWord=" + ExcludeSingleWord);
        procInfo.add("destRootDirTop=" + destRootDirTop);
        procInfo.add("OnlyIncludeIntents=" + OnlyIncludeIntents);
        procInfo.add("FinalExceptIntents=" + FinalExceptIntents);
                  
        // true scenario only, as function as extractUniqueContentOfColumn for scenario above.
        //IntEntUtils.extractScenarioIntent(origAnnoCsvUrl, true, 500);
        List<String> tmpinfo = IntEntUtils.extractScenarioIntent(origAnnoCsvUrl, true, null);
        
        procInfo.add("  ");
        procInfo.addAll(tmpinfo);
        
        List<String> forSaveInNameFile = new ArrayList<>();
        String tline = tmpinfo.get(2);
        forSaveInNameFile.add("TotalCsvFileSize=" + tline.substring(tline.lastIndexOf(":")+1));
        String excl = "[]";
        if(FinalExceptIntents != null && !FinalExceptIntents.isEmpty())
            excl = FinalExceptIntents.toString();
        
        forSaveInNameFile.add("FinalExceptIntents=" +excl );

        String incl = "[]";
        if(OnlyIncludeIntents != null && !OnlyIncludeIntents.isEmpty())
            incl = OnlyIncludeIntents.toString();
        
        forSaveInNameFile.add("OnlyIncludeIntents=" + incl);
        forSaveInNameFile.add("ExcludeSingleWord (1--T, 0--F) =" + ExcludeSingleWord);
        
        tline = tmpinfo.get(3);
        forSaveInNameFile.add("maxSizeUsedForProcessing=" + tline.substring(tline.lastIndexOf(":")+1));
        tline = tmpinfo.get(4);
        forSaveInNameFile.add("numOfDomains=" + tline.substring(tline.lastIndexOf(":")+1));
        forSaveInNameFile.add(" ");
        
        forSaveInNameFile.add("trainsetPercent=" + trainPercent);
        forSaveInNameFile.add("testsetPercent=" + testPercent);
        
        forSaveInNameFile.add("IntentTier=" +IntentTier);
        
        forSaveInNameFile.add("rasaLatestTrainResultsIdx=0");
        forSaveInNameFile.add("rasaLatestTestResultsIdx=0");
        forSaveInNameFile.add("rasaLatestEvalResultsIdx=0");
        
        forSaveInNameFile.add("apiaiLatestTestResultsIdx=0");
        forSaveInNameFile.add("apiaiLatestEvalResultsIdx=0");

        forSaveInNameFile.add("luisLatestTestResultsIdx=0");
        forSaveInNameFile.add("luisLatestEvalResultsIdx=0");
        
        forSaveInNameFile.add("watsonLatestTestResultsIdx=0");
        forSaveInNameFile.add("watsonLatestEvalResultsIdx=0");
        
        
        forSaveInNameFile.add("spacyLatestTrainResultsIdx=0");
        forSaveInNameFile.add("spacyLatestTestResultsIdx=0");
        forSaveInNameFile.add("spacyLatestEvalResultsIdx=0");

        // false ScenarioIntent, null for all size
        tmpinfo = IntEntUtils.extractScenarioIntent(origAnnoCsvUrl, false, null);
        procInfo.add("  ");
        procInfo.addAll(tmpinfo);

        tline = tmpinfo.get(4);
        forSaveInNameFile.add("numOfDomainsIntents=" + tline.substring(tline.lastIndexOf(":")+1));
        
        // all unique entity names, giving the origial full csv file. 
        // Returned list is the entity info
        List<String> entities = IntEntUtils.extractUniqueEntities_OneDomain(origAnnoCsvUrl);
        procInfo.add("  ");
        procInfo.add("============ Info_Category: All Unique entity names ===================== ");
        procInfo.add("Entity size:" + entities.size());
        procInfo.add("EntityNames:" +Arrays.toString(entities.toArray()));
       
        Map<String, Integer> entitiesMap = IntEntUtils.extractUniqueEntitiesCounts_OneDomain(origAnnoCsvUrl);
        procInfo.add("  ");
        procInfo.add("============ Info_Category: Sample Counts of All Unique entity (so including duplicated values)   ===================== ");
        procInfo.add("Total Entity Sample counts:" + GeneralUtils.getTotalCounts(entitiesMap));
        procInfo.add("Entity Counts:" + GeneralUtils.prettyPrintMap2OneStr(entitiesMap));

        // entities separated by domain
        Map map = IntEntUtils.extractUniqueEntities_Sep(origAnnoCsvUrl, IntEntUtils.Col_Scenario, null);
        tmpinfo =  GeneralUtils.prettyPrintMapAsList(map);
        procInfo.add("  ");
        procInfo.add("============ Info_Category: Entity names in each Domain ===================== ");
        procInfo.addAll(tmpinfo);
       
       
        // entities separated by domain with intent
        map = IntEntUtils.extractUniqueEntities_Sep(origAnnoCsvUrl, 
                  IntEntUtils.Col_Scenario, IntEntUtils.Col_Intent);
        tmpinfo =  GeneralUtils.prettyPrintMapAsList(map);
        procInfo.add("  ");
        procInfo.add("============ Info_Category: Entity names in each DomainIntents ===================== ");
        procInfo.addAll(tmpinfo);
             
        // entities with values:
        // Get All Entity-Values from one file
        map = IntEntUtils.getEntityValueMap_OneDomain_RealAnno(origAnnoCsvUrl);
        // System.out.println(GeneralUtils.prettyPrintMap(map));
        tmpinfo =  GeneralUtils.prettyPrintMapAsList(map);
        procInfo_EV_All.add("  ");
        procInfo_EV_All.add("============ Info_Category: All Entity names and All Values in whole csv dataset. ===================== ");      
        procInfo_EV_All.addAll(tmpinfo);
             
        String timeStamp = GeneralUtils.getTimeStamp4Logging();
        String destRootDir = destRootDirTop + "/preprocessResults/autoGeneFromRealAnno/autoGene_" + timeStamp;

        GeneralUtils.checkAndCreateDirectory(destRootDir);
        
        String svConfigDir = destRootDir + "/UsedConfiguration" ;
        GeneralUtils.checkAndCreateDirectory(svConfigDir);
                        
        String sub0_Domn = "Domain";
        String sub1_DomnInt = "DomainIntent";
        String sub2_DINoDup = "DomainIntentNoDup";
        String sub3_DIShuffle = "DomainIntentNoDupShuffle";
        String sub4_DISubset = "DomainIntentNoDupShuffleSubset";
        String sub5_DITrain = "DomainIntentNoDupShuffleSubset_Train";
        String sub6_DITest = "DomainIntentNoDupShuffleSubset_Test";
        
        String destDomainDir0 = destRootDir + "/" + sub0_Domn;
        GeneralUtils.checkAndCreateDirectory(destDomainDir0);
        List<String> tmpInfo = createAllDomains_RealAnno(origAnnoCsvUrl, destDomainDir0 );
        procInfo.addAll(tmpInfo);
        
        // ==================== end of preparisons =====================
         
         // ==================== start of real process =====================
        // For real process. DomainIntent only keep neccessary headers.
        // added plain utterance: answer_from_anno column.
        // 1. create domain with intent
        String destBaseDir1 = destRootDir + "/" + sub1_DomnInt;
         GeneralUtils.checkAndCreateDirectory(destRootDir);
        GeneralUtils.checkAndCreateDirectory(destBaseDir1);
        
        procInfo.add("  ");
        procInfo.add("origAnnoCsvUrl = " + origAnnoCsvUrl);
                
        // also remove double spaces, do validation inside the method.
        tmpInfo = createAllDomainsIntents_RealAnno(IntentTier, origAnnoCsvUrl, destBaseDir1);
        
        procInfo.add("  ");
        procInfo.addAll(tmpInfo);
        
        // 2. remove duplicated answers.
        String destBaseDir2 = destRootDir + "/" + sub2_DINoDup;
        GeneralUtils.checkAndCreateDirectory(destBaseDir2);
        tmpInfo = removeDuplication_AllFiles_RealAnno(destBaseDir1, destBaseDir2);
        procInfo.add("  ");
        procInfo.addAll(tmpInfo);

        
        // 3. shuffle the domain with intent
        String destBaseDir3 = destRootDir + "/" + sub3_DIShuffle;
        GeneralUtils.checkAndCreateDirectory(destBaseDir3);
        tmpInfo = shuffleUserUtterances_RealAnno(destBaseDir2, destBaseDir3);
        procInfo.add("  ");
        procInfo.addAll(tmpInfo);
        
        
        // 4. generate subset for train and test
        String destBaseDir4 = destRootDir + "/" + sub4_DISubset;
        GeneralUtils.checkAndCreateDirectory(destBaseDir4);
        tmpInfo = createSubset_RealAnno(destBaseDir3, destBaseDir4, maxNumUtt,minNumUtt);
        procInfo.add("  ");
        procInfo.addAll(tmpInfo);
        
        // 5. generate train set and test set, including pure text file of test set.
        String destBaseDir_Train = destRootDir + "/" + sub5_DITrain;
        String destBaseDir_Test = destRootDir + "/" + sub6_DITest;
        GeneralUtils.checkAndCreateDirectory(destBaseDir_Train);
        GeneralUtils.checkAndCreateDirectory(destBaseDir_Test);
        
        tmpInfo = createTrainsetTestset_ReallAnno(destBaseDir4, destBaseDir_Train, destBaseDir_Test,
                trainPercent, testPercent);
        procInfo.add("  ");
        procInfo.addAll(tmpInfo);
        procInfo.add("");
        
        // 6. annotate testset for later performance evaluation use
        // Step1:  create annotation: ground truth. only need once
        String annoSrcDir = destBaseDir_Test + "/csv";
        String annoDestDir = destBaseDir_Test + "/annotated";
        annotateTestData_All_RealAnno(annoSrcDir, annoDestDir);
        
        if(numKFold != 0 ) {
            //srcBaseDir: DomainIntentNoDupShuffleSubset
            tmpInfo = createTrainsetTestset_CrossValidation(destRootDir, destBaseDir4, numKFold);
            procInfo.add("  ");
            procInfo.addAll(tmpInfo);
            procInfo.add("");
        }
        
        procInfo.add("  ");
        procInfo.add("============ Info_Category: Entity names and Values in each DomainIntent ===================== ");      

        // get entities with values in each DomainIntent
        // to get EntityValues for each Domain, just change destBaseDir1 to destBaseDir0
        List<String> fileNames = GeneralUtils.getAllFileNames(destBaseDir1); 
        for (int i = 0; i < fileNames.size(); ++i) {
            String url = destBaseDir1 + "/" + fileNames.get(i);
            Map<String, List<String>> domainMap = IntEntUtils.getEntityValueMap_OneDomain_RealAnno(url);
            tmpinfo =  GeneralUtils.prettyPrintMapAsList(domainMap);
            procInfo.add("");
            procInfo.add(" -------- in DomainIntent: " + fileNames.get(i) + " --------");
            procInfo.addAll(tmpinfo);
        }
        
        String trainsetBaseDir = destRootDir + "/" + sub5_DITrain;
        fileNames = GeneralUtils.getAllFileNames(trainsetBaseDir); 
        Map<String, List<String>> allEntValsInTrainset = new LinkedHashMap();
        Map<String, List<String>> allEntValsInTrainsetNoDup = new LinkedHashMap();

        procInfo.add("  ");
        procInfo.add("============ Info_Category: Entity names and Counts in trainset (Including dup values) ===================== ");      
        
        // Notes this Info_Category is written in another file for procInfo_EV_All
        // rather than in the file procInfo. So should remove this category info from procInfo.
        
        Map<String, Integer> entitiesMapAll = new LinkedHashMap();
        for (int i = 0; i < fileNames.size(); ++i) {
            String url = trainsetBaseDir + "/" + fileNames.get(i);
        
            Map<String, Integer> entMap1 = IntEntUtils.extractUniqueEntitiesCounts_OneDomain(url);
            entitiesMapAll = GeneralUtils.mergeMapCounts(entitiesMapAll, entMap1);
        }

        procInfo_EV_All.add("Total number of Entitties in trainset (Including dup values):" + entitiesMapAll.size() );
        procInfo_EV_All.add("Total Entity Sample counts in trainset (Including dup values):" + GeneralUtils.getTotalCounts(entitiesMapAll));
        procInfo_EV_All.add("Entity Counts in trainset (Including dup values):" + GeneralUtils.prettyPrintMap2OneStr(entitiesMapAll));

        procInfo.add("  ");
        procInfo.add("============ Info_Category: Entity names and Values in trainset ===================== ");      

        for (int i = 0; i < fileNames.size(); ++i) {
            String url = trainsetBaseDir + "/" + fileNames.get(i);
            Map<String, List<String>> domainMap = IntEntUtils.getEntityValueMap_OneDomain_RealAnno(url);
            allEntValsInTrainset = GeneralUtils.mergeMapWithDup(allEntValsInTrainset, domainMap);
            allEntValsInTrainsetNoDup = GeneralUtils.mergeMap(allEntValsInTrainsetNoDup, domainMap);
            
            tmpinfo =  GeneralUtils.prettyPrintMapAsList(domainMap);
            procInfo.add("");
            procInfo.add(" -------- in Trainset " + fileNames.get(i) + " --------");
            procInfo.addAll(tmpinfo);
        }

        procInfo.add("  ");
        procInfo.add("============ Info_Category: Entity names and Values in trainset: All ===================== ");      
        tmpinfo =  GeneralUtils.prettyPrintMapAsList(allEntValsInTrainset);
        procInfo.addAll(tmpinfo);
        
        procInfo.add("  ");
        procInfo.add("============ Info_Category: Entity names and Values in trainset: All NoDup ===================== ");      
        tmpinfo =  GeneralUtils.prettyPrintMapAsList(allEntValsInTrainsetNoDup);
        procInfo.addAll(tmpinfo);

        procInfo.add(" ================ END OF DomainIntent EntityValues ============ ");
        
        // save preprocess info to a file entities, values, size, domain/intent names
        String infoFileDir = destRootDirTop + "/preprocessResults/preprocessInfoSaved";
        GeneralUtils.checkAndCreateDirectory(infoFileDir);
        String prefix = "ExtractedDataInfo";
        String infoFileUri = infoFileDir + "/" + prefix + "_" + timeStamp + ".txt";
        GeneralUtils.saveToFile(infoFileUri, procInfo);
        
        String infoFileEVAllUri = infoFileDir + "/EntityNameValuesCountsAll_" + timeStamp + ".txt";
        GeneralUtils.saveToFile(infoFileEVAllUri, procInfo_EV_All);
        
        // save generated file names info (appending to file)
        //destRootDir;
        //and other related like xxxTrain, xxxTest, for easy use by other components
        List<String> nameInfo = new ArrayList<>();
        String item = "# ---------- written by PreprocessRealData.java ----------";
        nameInfo.add(item);
        
        item = "=A_TASK_START=";
        nameInfo.add("# -------------------------------");
        nameInfo.add(item);
        nameInfo.add("# ---- The starting point for each generated data from original csv file. ----");
        nameInfo.add(" ");
        
        item = "serviceName_RASA=" + IntEntUtils.ServiceName_RASA;
        nameInfo.add(item);
        
        item = "serviceName_APIAI=" + IntEntUtils.ServiceName_APIAI;
        nameInfo.add(item);

        item = "serviceName_LUIS=" + IntEntUtils.ServiceName_LUIS;
        nameInfo.add(item);
        
        item = "serviceName_WATSON=" + IntEntUtils.ServiceName_WATSON;
        nameInfo.add(item);
        
        item = "timeStamp=" + timeStamp;
        nameInfo.add(item);
                
        nameInfo.add(" ");
        nameInfo.add("origAnnoCsvUrl = " + origAnnoCsvUrl);
        nameInfo.addAll(forSaveInNameFile);
        nameInfo.add(" ");
        nameInfo.add("detailedInfoFile = " + infoFileUri);
        
        item = "preprocessOutBaseDir=" + destRootDir;
        nameInfo.add(item);
        item = "preprocessOutDir_Sub0Domn=" + sub0_Domn;
        nameInfo.add(item);
        item = "preprocessOutDir_Sub1DomnInt=" + sub1_DomnInt;
        nameInfo.add(item);
        item = "preprocessOutDir_Sub2DINoDup=" + sub2_DINoDup;
        nameInfo.add(item);
        item = "preprocessOutDir_Sub3Shuffle=" + sub3_DIShuffle;
        nameInfo.add(item);
        item = "preprocessOutDir_Sub4Subset=" + sub4_DISubset;
        nameInfo.add(item);
        item = "preprocessOutDir_Sub5Train=" + sub5_DITrain;
        nameInfo.add(item);
        item = "preprocessOutDir_Sub6Test=" + sub6_DITest;
        nameInfo.add(item);
        item = "preprocessSubsetMaxNumUtt=" + maxNumUtt;
        nameInfo.add(item);
        item = "preprocessSubsetMiniNumUtt=" + minNumUtt;
        nameInfo.add(item);

        item = "preprocessTrainSizeRatio=" + trainPercent;
        nameInfo.add(item);

        item = "preprocessTestSizeRatio=" + testPercent;
        nameInfo.add(item);

        item = "testSetPureTextDir=" + destBaseDir_Test + "/text";
        nameInfo.add(item);
        item = "testSetCsvDir=" + destBaseDir_Test + "/csv";
        nameInfo.add(item);
        item = "testSetAnnotated=" + annoDestDir;
        nameInfo.add(item);
        
        String nameInfoFileDir = destRootDirTop + "/allGeneFileNames";
        String nameInfoFile = "allGeneFileNames.txt";

        // true for appending. null for using default encoding.
        GeneralUtils.saveToTextFile(nameInfoFileDir, nameInfoFile, nameInfo, true, null);
        
            // save the used configuration
        String svConfigUri = svConfigDir + "/Config4Prep_" + timeStamp + ".txt" ;            
        try {
            File svfile = new File(svConfigUri);
            FileOutputStream fileOut = new FileOutputStream(svfile);
            configProperties.store(fileOut, "Used Configuration for " + timeStamp);
            fileOut.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(-1);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }                        
        
                
    }
    
    private void annotateTestData_All_RealAnno(String uttsDir, String destBaseDir) {
        
        List<String> fileNames = GeneralUtils.getAllFileNames(uttsDir);
        
        for (int i = 0; i < fileNames.size(); i++) {
            String fname = fileNames.get(i);
            String uttFileUri = uttsDir + "/" + fname;
            String intent = fname.substring(0, fname.lastIndexOf("."));
            String domainName = intent;
                    
            annotateTestData_OneDomain_RealAnno(domainName, intent, uttFileUri,  destBaseDir);
        }
                
    }
  
    private void annotateTestData_OneDomain_RealAnno(String domainName, String intent,
            String csvFile, String destBaseDir) {
        
        List<String> annoUttList = IntEntUtils.getAnnotatedUttsFromCsv(csvFile);
        List<String> plainUttList = IntEntUtils.getPlainUttsFromCsv(csvFile);
        JsonArray annoData = new JsonArray();

        JsonArray entitiesJA = new JsonArray();
        for (int i = 0; i < annoUttList.size(); ++i) {
            String annoUtt = annoUttList.get(i);
            String plainUtt = plainUttList.get(i);
            //TODO use similar way as before to use entityKeyValueMap.
            // but first need extract the map from each intent domain
            entitiesJA = IntEntUtils.getEntitiesOfUtt_RealAnno(annoUtt, plainUtt,
                         IntEntUtils.ServiceName_RASA); // as long as not LUIS
            
            JsonObject uttJO = new JsonObject();
            uttJO.addProperty("text", plainUtt);
            uttJO.addProperty("intent", intent);
            uttJO.add("entities", entitiesJA);
            annoData.add(uttJO);
        }
        //System.out.println("anno domain:" + domainName + "annotated utt size:"+ annoData.size());
        JsonObject annoJO = new JsonObject();
        annoJO.add("test_data_annotation", annoData);

        String destDir = destBaseDir + "/" + domainName;
        GeneralUtils.checkAndCreateDirectory(destDir);

        String annoFile = csvFile.substring(csvFile.lastIndexOf("/") + 1, csvFile.lastIndexOf("."));
        annoFile += "_anno"; // should not have suffix. it is module name
        GeneralUtils.saveJsonStrToFile(annoFile, destDir, annoJO.toString(), true);

    }

 
    private void mergeOrigWithNewFiles(String srcFile, String mergeFilesDir, 
            String[] mergeDomainNames, String destFileSuffix) {

        List<String> resList = GeneralUtils.readFromTextFile(srcFile);
                
        for (int i = 0; i < mergeDomainNames.length; ++i) {
            String csvUri = mergeFilesDir + "/" + mergeDomainNames[i] + ".csv";
            //System.out.println("adding file:" + csvUri);
            List<String> lines = GeneralUtils.readFromTextFile(csvUri);
            resList.addAll(lines.subList(1, lines.size()));
        }
        String srcFilePre = srcFile.substring(0, srcFile.lastIndexOf("."));
        
        String resFile = srcFilePre + "_" + destFileSuffix + ".csv";
        //String resUri = destDir + "/" + resFile;
        GeneralUtils.saveToFile(resFile, resList);
    }
    
    
    private boolean checkBracketSequenceMatch(String annoUtt) {
        // check cases: start with ], end with [, continuous [[ or ]]
        List<String> brs = new ArrayList();
        boolean hasError = false;
        for(int i = 0; i < annoUtt.length(); ++i) {
            char ch = annoUtt.charAt(i);
            if(ch == '[' || ch == ']') {
                brs.add(String.valueOf(ch));
            }
        }
        
        if(brs.isEmpty()) return false; // no error
        if(brs.get(0).equals("]")) {
            hasError = true;
            System.err.println("\n annoUtt starts with ]:" + annoUtt);
        }
        
        if(brs.get(brs.size()-1).equals("[")) {
            hasError = true;
            System.err.println("\n annoUtt ends with [:" + annoUtt);
        }
        // check continuous [[ or ]]
        for(int i = 0; i < brs.size()-1; ++i) {
            if(brs.get(i).equals("[") && brs.get(i+1).equals("[")) {
               hasError = true;
               System.err.println("\n Two [[: " + annoUtt);
            }
            if(brs.get(i).equals("]") && brs.get(i+1).equals("]")) {
               hasError = true;
               System.err.println("\n Two ]]: " + annoUtt);
            }
        }

        return hasError;
    }
    
    private boolean validateAnnotationLabel_RealAnno(String csvUri) {
        //List<String> annoUtts = RealDataCsvUtil.getColumnContentFromCsv(csvAnno, 
        //        RealDataCsvUtil.AnnoAnswerColumnName);
        boolean hasError = false;
        List<String> annoUtts = IntEntUtils.getAnnotatedUttsFromCsv(csvUri);
        for (int i = 0; i < annoUtts.size(); ++i) {
            String annoUtt = annoUtts.get(i);
            boolean hasErr2 = checkBracketSequenceMatch(annoUtt);
            
            if(hasErr2) hasError = true;
            
            int countColon = annoUtt.replaceAll("[^:]", "").length();
            //System.out.println(" char1:" + charCount);
            int countLeftBr = annoUtt.replaceAll("[^\\[]", "").length();
            int countRightBr = annoUtt.replaceAll("[^\\]]", "").length();
            if (countColon == countLeftBr && countColon == countRightBr) {
                // ok, nothing to do
            } else {
                hasError = true;
                System.err.println("\n Not Valid: num of label chars not match!" + annoUtt);
            }
        }
        
        // now validate ] immediately followed by chars, or vice verse
        for (int i = 0; i < annoUtts.size(); ++i) {
            String annoUtt = annoUtts.get(i);
            //if(!validateAnnoUtt(annoUtt))
            //    return false;
            if( !validateAnnoUtt_RealAnno(annoUtt) )
                hasError = true;
        }
                
        // check empty lines of annotation.
        
        List<Map<String, String>> rawData = IntEntUtils.parseCSVFile(csvUri);
        String[] headerArr = IntEntUtils.getHeadersOfCsv(csvUri);
        for (int i = 0; i < rawData.size(); ++i) {
            Map<String, String> lineMap = rawData.get(i);
            String tab = IntEntUtils.TAB;
            String anno = lineMap.get(IntEntUtils.Col_AnswerAnnotation).trim();
            
            if(anno == null || anno.equals("") ) {
                String line = "";
                for (int j = 0; j < headerArr.length; ++j) {
                    String item = lineMap.get(headerArr[j]).trim();
                    line += item;
                    if (j < headerArr.length - 1) {
                        line += tab;
                    }
                }
                hasError = true;
                System.err.println("Empty Anno:" + line);
            }
        }

        if(hasError)  {
            System.out.println("\n\n Validation Finished, Has Errors!\n");
           return false;
        }  else {
            System.out.println("\n\n Validation Finished, Success!\n");
            return true;
        }
    }

    private boolean validateAnnoUtt_RealAnno(String utt) {

        boolean hasError = false;
        String regex1 = "(\\S+\\[)"; // including "Chars[", "]["
        Pattern p = Pattern.compile(regex1);
        Matcher m = p.matcher(utt);
        while (m.find()) {
            String mat = m.group(1);
            hasError = true;
            System.err.println("Invalid, No Space:" + m.group(1));
            System.err.println("Utt:" +utt);
        }

        String regex2 = "(\\]\\S+)"; // "]Chars"
        p = Pattern.compile(regex2);
        m = p.matcher(utt);
        while (m.find()) {
            String mat = m.group(1);
            // consider "followed by punct" is valid. TODO, make this generic.
            // TODO check escape chars
            if(!mat.startsWith("],") && !mat.startsWith("].")
                    && !mat.startsWith("]!") && !mat.startsWith("]?")) {
                hasError = true;
            
            System.err.println("Invalid, No Space:" + m.group(1));
            System.err.println("Utt:" +utt);
            }
        }
        
        if(hasError)
          return false; // invalid
        else
            return true; // valid.
    }
    
    private List<String> createTrainsetTestset_ReallAnno(String srcBaseDir, String trainBaseDir,
            String testBaseDir, int trainPercent, int testPercent) {

        List<String> infoList = new ArrayList<>();
        infoList.add("============ Info_Category: ==== createTrainsetTestset_ReallAnno ====");
        infoList.add("srcBaseDir=" + srcBaseDir);
        infoList.add("trainBaseDir=" + trainBaseDir);
        infoList.add("testBaseDir=" + testBaseDir);
        infoList.add("trainPercent=" + trainPercent);
        
        // the srcBaseDir is from sub4_DISubset = "DomainIntentNoDupShuffleSubset";
        List<String> fileNames = GeneralUtils.getAllFileNames(srcBaseDir);

        // Get maximum length of the file names for output print format
        int maxFileNameLen = 0;
        for (int i = 0; i < fileNames.size(); ++i) {
            int len = fileNames.get(i).length();
            if (len > maxFileNameLen) maxFileNameLen = len;
        }
        
        //int trainPercent = 80;
        int totalTrain = 0;
        int totalTest = 0;
        int totalSize = 0;
        int totalIntents = 0;
        for (int i = 0; i < fileNames.size(); ++i) {
            String filename = fileNames.get(i);
            String url = srcBaseDir + "/" + filename;
            
            List<String> ansList = GeneralUtils.readFromTextFile(url); // from csv file
            totalSize += ansList.size();
            totalIntents += 1;
            int total = ansList.size();
            int trainsize = total * trainPercent / 100;
                   
            // real data from csv file where first line is header.
            List<String> trainsets = new ArrayList(ansList.subList(0, trainsize)); // [s,e)
            int testSize = total * testPercent / 100;
            int testsetStart = total - testSize;
            int testsetEnd = testsetStart + testSize;
            List<String> testsets = new ArrayList(ansList.subList(testsetStart, testsetEnd)); // [s,e)
            testsets.add(0, ansList.get(0)); // insert the header
            totalTrain += trainsets.size()-1;
            totalTest += testsets.size()-1;
            String space = GeneralUtils.getSpace(maxFileNameLen+5-filename.length());
            String msg = "Intent File: " + filename + space + ", Total:" + total + ", Train:" + trainsize + ", Test:" + testSize;
            System.out.println(msg);
            infoList.add(msg);
            
            String trainUrl = trainBaseDir + "/" + filename;
            GeneralUtils.saveToFile(trainUrl, trainsets);

            String testUrl = testBaseDir + "/csv/" + filename;
            GeneralUtils.checkAndCreateDirectory(testBaseDir + "/csv");

            GeneralUtils.saveToFile(testUrl, testsets);

            // get the pure text testset for querying trained models.
            List<String> textTestset = IntEntUtils.getPlainUttsFromCsv(testUrl);
            String textBaseDir = testBaseDir + "/text";
            GeneralUtils.checkAndCreateDirectory(textBaseDir);

            String fileBaseName = filename.substring(0, filename.lastIndexOf("."));
            String textTestUrl = textBaseDir + "/" + fileBaseName + ".txt";
            GeneralUtils.saveToFile(textTestUrl, textTestset);
            
            // annotated testsets will be created on the top by calling annotateTestData_All_RealAnno
        }
        
        String msg = "\n total subset size:" + totalSize + ", total num of intents:" + totalIntents;
        System.out.println(msg);
        infoList.add(msg);
        
        msg = "\n total trainset size:" + totalTrain + ", total testset size:" + totalTest;
        System.out.println(msg);
        infoList.add(msg);
        
        return infoList;
    }


    private List<String> createTrainsetTestset_CrossValidation(String destRootDir, String srcBaseDir,
            int kfold) {
        
        // destRootDir is the taskBaseDir which has the preprocessed data: DomainIntentNoDupShuffleSubset
        // destDir: $taskBaseDir/CrossValidation/KFlod_1/testset, trainset
        // Currrently srcBaseDir is also contained in srcBaseDir.
        
        List<String> infoList = new ArrayList<>();
        infoList.add("============ Info_Category: ==== createTrainsetTestset_CrossValidation ====");
        infoList.add("destRootDir=" + destRootDir);
        infoList.add("srcBaseDir=" + srcBaseDir);
        infoList.add("num of kFold=" + kfold);
        
        String crossValiDir = destRootDir + "/CrossValidation";
        // the srcBaseDir is from sub4_DISubset = "DomainIntentNoDupShuffleSubset";
        List<String> fileNames = GeneralUtils.getAllFileNames(srcBaseDir);

        // Get maximum length of the file names for output print format
        int maxFileNameLen = 0;
        for (int i = 0; i < fileNames.size(); ++i) {
            int len = fileNames.get(i).length();
            if (len > maxFileNameLen) maxFileNameLen = len;
        }
        
        for(int k = 0; k < kfold; ++k) {
            int totalTrain = 0;
            int totalTest = 0;
            int totalSize = 0;
            int totalIntents = 0;

            int kNum = k+1;
           String destTrainBaseDir = crossValiDir + "/KFold_" + kNum + "/trainset";
           String destTestBaseDir = crossValiDir + "/KFold_" + kNum + "/testset";
           GeneralUtils.checkAndCreateDirectory(destTrainBaseDir);
           GeneralUtils.checkAndCreateDirectory(destTestBaseDir);
           infoList.add("\n ======== kFold = " + k + " ========");
           for (int i = 0; i < fileNames.size(); ++i) {
                String filename = fileNames.get(i);
                String url = srcBaseDir + "/" + filename;

                List<String> ansList = GeneralUtils.readFromTextFile(url); // from csv file
                // real data from csv file where first line is header.
                String header = ansList.get(0); // will be added to the trainsets and the testsets
                ansList.remove(0); // remove the header

                int fullSize = ansList.size();
                totalSize += fullSize;
                totalIntents += 1;

                int testsetSize = fullSize / kfold;
                int testsetStart = k*testsetSize;
                int testsetEnd = testsetStart + testsetSize;
                if(k == kfold-1) testsetEnd = fullSize; // last kfold

                List<String> testsets = new ArrayList(ansList.subList(testsetStart, testsetEnd)); // [s,e)

                // now fullset - testsets = trainsets
                // trainsub1 = 0--testsetStart, trainsub2 = testsetEnd -- fullsetEnd
                List<String> trainsub1 = new ArrayList(ansList.subList(0, testsetStart)); // [s,e)
                List<String> trainsub2 = new ArrayList(ansList.subList(testsetEnd, fullSize)); // [s,e)
                List<String> trainsets = new ArrayList();
                trainsets.addAll(trainsub1);
                trainsets.addAll(trainsub2);

                int trainSize = trainsets.size(); // not include the header
                int testSize = testsets.size(); // not include the header

                testsets.add(0, header); // insert the header
                trainsets.add(0, header); // insert the header


                totalTrain += trainSize;
                totalTest += testSize;
                String space = GeneralUtils.getSpace(maxFileNameLen+5-filename.length());
                String msg = "Intent File: " + filename + space + ", Total:" + fullSize + ", Train:" + trainSize + ", Test:" + testSize;
                System.out.println(msg);
                infoList.add(msg);

                String trainUrl = destTrainBaseDir + "/" + filename;
                GeneralUtils.saveToFile(trainUrl, trainsets);

                String testUrl = destTestBaseDir + "/csv/" + filename;
                GeneralUtils.checkAndCreateDirectory(destTestBaseDir + "/csv");

                GeneralUtils.saveToFile(testUrl, testsets);

                // get the pure text testset for querying trained models.
                List<String> textTestset = IntEntUtils.getPlainUttsFromCsv(testUrl);
                String textBaseDir = destTestBaseDir + "/text";
                GeneralUtils.checkAndCreateDirectory(textBaseDir);

                String fileBaseName = filename.substring(0, filename.lastIndexOf("."));
                String textTestUrl = textBaseDir + "/" + fileBaseName + ".txt";
                GeneralUtils.saveToFile(textTestUrl, textTestset);

            }

              // create "annotated" for final evaluation use
            String annoSrcDir = destTestBaseDir + "/csv";
            String annoDestDir = destTestBaseDir + "/annotated";
            annotateTestData_All_RealAnno(annoSrcDir, annoDestDir);

            String msg = "\n total subset size:" + totalSize + ", total num of intents:" + totalIntents;
            System.out.println(msg);
            infoList.add(msg);

            msg = "\n total trainset size:" + totalTrain + ", total testset size:" + totalTest;
            System.out.println(msg);
            infoList.add(msg);
            infoList.add( "-------- End of KFold: "  + k + " --------");
        }
        
        return infoList;
    }
    
    /**
     * Separate original annotated csv file according to domains like alarm,
     * music, calendar etc. They are used for references/visual checking etc.
     * The real process will use domain_intents.
     *
     * This separated domains will keep all headers, added plain utts from anno.
     *
     * @param srcAnnoCsvUrl
     * @param destBaseDir
     */
    private List<String> createAllDomains_RealAnno(String srcAnnoCsvUrl, String destBaseDir) {

        List<Map<String, String>> rawData = IntEntUtils.parseCSVFile(srcAnnoCsvUrl);
        
        List<String> infoList = new ArrayList<>();
        infoList.add("==== From createAllDomains_RealAnno ====");
        infoList.add("srcAnnoCsvUrl=" + srcAnnoCsvUrl);
        infoList.add("destBaseDir=" + destBaseDir);
        String tmpmsg = "process annotation, List<Map> size:" + rawData.size();
        infoList.add(tmpmsg);
        
        Map<String, List<String>> sceAnsMap = new LinkedHashMap<>();
        String header = IntEntUtils.getHeaderStrOfCsv(srcAnnoCsvUrl);

        String[] headerArr = IntEntUtils.getHeadersOfCsv(srcAnnoCsvUrl);

        List<String> ansList = null;
        String tab = IntEntUtils.TAB;
        header += tab + "answer_from_anno";
        int numLong = 0;
        for (int i = 0; i < rawData.size(); ++i) {
            Map<String, String> lineMap = rawData.get(i);
              String status = lineMap.get(IntEntUtils.Col_Status);
              if(status != null && status.toLowerCase().startsWith(IntEntUtils.Tag_IRR.toLowerCase()))
                continue;

            String line = "";
            boolean ignoreLine = false;
            for (int j = 0; j < headerArr.length; ++j) {
                //System.out.println("orig header:" + headerArr[j] );
                String item = lineMap.get(headerArr[j]).trim();
                // quote each item (i.e. column) 
                if(!item.startsWith("\""))
                    item = "\"" + item;
                if(!item.endsWith("\""))
                    item = item + "\"";
                
                line += item;
                if (j < headerArr.length - 1) {
                    line += tab;
                }
            }
            
            String answeranno = lineMap.get("answer_annotation").trim();
            if(answeranno.isEmpty())
                answeranno = "null";
            
            String uttFromAnno = IntEntUtils.getPlainUttFromAnnoUtt(answeranno);

            String[] plainUttArr = uttFromAnno.split("\\s+");
            if(plainUttArr.length > maxNumLongUtt) {
                infoList.add("Ignored,too long:" + uttFromAnno);
                numLong++;
                continue;
            }
            
            String key = lineMap.get("scenario").trim();

            ansList = sceAnsMap.get(key);
            if (ansList == null) {
                ansList = new ArrayList<>();
                ansList.add(header);
                sceAnsMap.put(key, ansList);
            }
            
            if(!uttFromAnno.startsWith("\""))
                uttFromAnno = "\"" + uttFromAnno;
            if(!uttFromAnno.endsWith("\""))
                uttFromAnno = uttFromAnno + "\"";
            
            line += tab + uttFromAnno;
            if(line.contains("cricket players")) {
                System.out.println(" =====" + line);
                System.out.println(" =====" + plainUttArr.length);
                System.out.println(" =====" + maxNumLongUtt);
            }
            ansList.add(line);
        }

        // save to files
        GeneralUtils.checkAndCreateDirectory(destBaseDir);
        for (Map.Entry<String, List<String>> entry : sceAnsMap.entrySet()) {
            String domainName = entry.getKey();
            List<String> ans = entry.getValue();
            tmpmsg = "Scenario:" + domainName + ", size:" + ans.size();
            //System.out.println(tmpmsg);
            
            String fileUri = destBaseDir + "/" + domainName + ".csv";
            GeneralUtils.saveToFile(fileUri, ans);
        }
        
        infoList.add("\n Total num of Ignored utt:" + numLong);
        infoList.add("");
        
        return infoList;
    }

    
    /**
     * To create scenario+intent as a processing domain (with one intent)
     * 
     * Only keep necessary headers, while allDomains above will keep all headers.
     * 
     * Checking empty cell e.g. added utterances by the annotator may not have 
     * answerid info etc
     * 
     * Also check MODE column to remove IRR (irrelevant utterances)
     * 
     * 
     * @param srcAnnoCsvUrl
     * @param destBaseDir 
     */
    private List<String> createAllDomainsIntents_RealAnno(String tier, String srcAnnoCsvUrl, String destBaseDir) {

        List<Map<String, String>> rawData = IntEntUtils.parseCSVFile(srcAnnoCsvUrl);

        List<String> infoList = new ArrayList<>();
        infoList.add("==== From createAllDomainsIntents_RealAnno ====");
        infoList.add("srcAnnoCsvUrl=" + srcAnnoCsvUrl);
        infoList.add("destBaseDir=" + destBaseDir);
        String tmpmsg = "process annotation, List<Map> size:" + rawData.size();
        infoList.add(tmpmsg);

        Map<String, List<String>> sceAnsMap = new LinkedHashMap<>();
        List<String> ansList = null;
        String tab = IntEntUtils.TAB;
        String header = "answerid" + tab + "scenario" + tab + "intent" + tab + "answer_annotation"
                + tab + "answer_from_anno" + tab + "answer_from_user";
        // java regex: \S ==> A non-whitespace character: [^\s]
        //String regex = "\\[\\s*\\S+\\s*:"; // for [person_phone: etc.
        for (int i = 0; i < rawData.size(); ++i) {
            Map<String, String> lineMap = rawData.get(i);

            String status = lineMap.get("status").trim();
            //if(status.equalsIgnoreCase(IntEntUtils.Tag_IRR) || status.equalsIgnoreCase(IntEntUtils.Tag_IRR_XL)) {
            if(status.toLowerCase().startsWith(IntEntUtils.Tag_IRR.toLowerCase()) ) {                        
                // ignore the IRR -- irrelevant user answers
                continue;
            }
            
            String scenario = lineMap.get("scenario").trim();

            String intent = lineMap.get("intent").trim();
            String answerid = lineMap.get("answerid").trim();
            if(answerid.equals(""))
                answerid = "x"; // annotator added answers
            
            String answeranno = lineMap.get("answer_annotation").trim();
            String answeruser = lineMap.get("answer").trim();
            if(answeruser.equals(""))
                answeruser = "xxx"; // annotator added answers
            // all other columns are already made sure no empty
            if (scenario.startsWith("\"")) {
                scenario = scenario.substring(1);
            }
            if (scenario.endsWith("\"")) {
                scenario = scenario.substring(0, scenario.length() - 1);
            }
            scenario = scenario.trim();

            if (intent.startsWith("\"")) {
                intent = intent.substring(1);
            }
            if (intent.endsWith("\"")) {
                intent = intent.substring(0, intent.length() - 1);
            }
            intent = intent.trim();
            String key = "";
            if(tier.equalsIgnoreCase("TIER1")) {
                key = scenario;
            } else if(tier.equalsIgnoreCase("TIER2") ) {

                if(intent.contains("_")) {
                    String t = intent.substring(0, intent.indexOf("_"));
                    key = scenario + "_" + t;
                } else {
                    key = scenario + "_" + intent;
                }
            } else if(tier.equalsIgnoreCase("TIER3") ) { // currently it is full intent
                key = scenario + "_" + intent;
            } else {
                // not handled
                System.out.println("Not handled tier!");
                System.exit(-1);
            }
            
            // replace all double or more spaces and apostrophe to just one.
            answeranno = GeneralUtils.replaceMoreSepcialCharsToOne(answeranno, " ");
            answeranno = GeneralUtils.replaceMoreSepcialCharsToOne(answeranno, "'");

            String uttFromAnno = IntEntUtils.getPlainUttFromAnnoUtt(answeranno);

            String[] plainUttArr = uttFromAnno.split("\\s+");
            if(plainUttArr.length > maxNumLongUtt) {
                // Already added and save in createAllDomains_RealAnno()
                //infoList.add("Ignored,too long:" + uttFromAnno);
                System.out.println("createAllDomainIntents: Ignored,too long:" + uttFromAnno);
                //numLong++;
                continue;
            }
            
            if (!answeranno.startsWith("\"")) {
                answeranno = "\"" + answeranno;
            }
            if (!uttFromAnno.startsWith("\"")) {
                uttFromAnno = "\"" + uttFromAnno;
            }
            if (!answeruser.startsWith("\"")) {
                answeruser = "\"" + answeruser;
            }

            if (!answeranno.endsWith("\"")) {
                answeranno = answeranno + "\"";
            }
            if (!uttFromAnno.endsWith("\"")) {
                uttFromAnno = uttFromAnno + "\"";
            }
            if (!answeruser.endsWith("\"")) {
                answeruser = answeruser + "\"";
            }

            String line = answerid + tab + scenario + tab + intent + tab + answeranno
                    + tab + uttFromAnno + tab + answeruser;
            
            ansList = sceAnsMap.get(key);
            if (ansList == null) {
                ansList = new ArrayList<>();
                ansList.add(header);
                sceAnsMap.put(key, ansList);
            }
            
            ansList.add(line);

        }

        // save to files
        //String destBaseDir = "resources/inputdataReal/dataset2/preprocess/autoGeneFromAnno/DomainsIntentsWithDup";
        GeneralUtils.checkAndCreateDirectory(destBaseDir);
        for (Map.Entry<String, List<String>> entry : sceAnsMap.entrySet()) {
            String domainName = entry.getKey();
            List<String> ans = entry.getValue();
            tmpmsg = "domain:" + domainName + ", size:" + ans.size();
            infoList.add(tmpmsg);
            
            //System.out.println(tmpmsg);
            String fileUri = destBaseDir + "/" + domainName + ".csv";
            GeneralUtils.saveToFile(fileUri, ans);
        }

        return infoList;
    }

    
    /**
     * Remove duplicated lines based on plain utts which are normalized from 
     * annotated answers. Do not reply on the annotated answers as they are
     * not normalized e.g. for same annotation, some may contains space, some may not.
     * 
     * 
     * @param srcBaseDir
     * @param destBaseDir 
     */
    private List<String> removeDuplication_AllFiles_RealAnno(String srcBaseDir, String destBaseDir) {

        List<String> infoList = new ArrayList<>();
        infoList.add("==== From removeDuplication_AllFiles_RealAnno ====");
        infoList.add("srcBaseDir=" + srcBaseDir);
        infoList.add("destBaseDir" + destBaseDir);

        List<String> fileNames = GeneralUtils.getAllFileNames(srcBaseDir);
        for (int i = 0; i < fileNames.size(); ++i) {
            String csvUri = srcBaseDir + "/" + fileNames.get(i);
            infoList.add("file:" + fileNames.get(i));
            
            String destUri = destBaseDir + "/" + fileNames.get(i);
            List<String> infoOneFile = removeDuplication_OneFile_RealAnno(csvUri, destUri);
            infoList.addAll(infoOneFile);

        }
        return infoList;
    }

        private  List<String> removeDuplication_OneFile_RealAnno(String csvUri, String destUri) {

            List<String> infoList = new ArrayList<>();
            
            List<String> plainUtts = IntEntUtils.getPlainUttsFromCsv(csvUri);
            // based on plain utts
            
            List<String> origCsv = GeneralUtils.readFromTextFile(csvUri);

            //System.out.println("Orig size:" + origCsv.size());
            List<String> wholeNoDup = new ArrayList<>();

            wholeNoDup.add(origCsv.get(0)); // add header

            List<String> annoUttsNoDup = new ArrayList<>();
            for (int j = 0; j < plainUtts.size(); ++j) { // not contain header
                String plainUtt = plainUtts.get(j);
                String[] tokens = plainUtt.split("\\s+");
                
                if(ExcludeSingleWord && tokens.length == 1)
                    continue;
                
                if (!annoUttsNoDup.contains(plainUtts.get(j))) {
                    annoUttsNoDup.add(plainUtts.get(j));
                    wholeNoDup.add(origCsv.get(j + 1)); // origCsv include header
                }
            }

            int diff = origCsv.size() - wholeNoDup.size();
            String tmpmsg = "Orig size: " + origCsv.size() + ", NoDup size: " 
                    + wholeNoDup.size()+ ", Diff: " + diff + "\n";
            
            if(diff != 0)
                infoList.add(tmpmsg); // so only save it when there is a diff.
            
            // save this intent domain to file
            GeneralUtils.saveToFile(destUri, wholeNoDup);
            
            return infoList;
            
        }

        
    /**
     * First line is the header
     */
    private List<String> shuffleUserUtterances_RealAnno(String srcBaseDir, String destBaseDir) {

        List<String> infoList = new ArrayList<>();
        infoList.add("==== shuffleUserUtterances_RealAnno ====");
        infoList.add("srcBaseDir=" + srcBaseDir);
        infoList.add("destBaseDir" + destBaseDir);

        List<String> fileNames = GeneralUtils.getAllFileNames(srcBaseDir);
        for (int i = 0; i < fileNames.size(); ++i) {
            String url = srcBaseDir + "/" + fileNames.get(i);
            List<String> ansList = GeneralUtils.readFromTextFile(url);
            List<String> ansListNoHeader = ansList.subList(1, ansList.size());
            Collections.shuffle(ansListNoHeader);
            ansListNoHeader.add(0, ansList.get(0));
            String destUrl = destBaseDir + "/" + fileNames.get(i);
            GeneralUtils.checkAndCreateDirectory(destBaseDir);
            GeneralUtils.saveToFile(destUrl, ansListNoHeader);
        }
        
        return infoList;
    }

    /**
     * Create a subset of whole data to be used for train and test because the
     * whole set may be too big.
     *
     * @param srcBaseDir src dir contain all csv files.
     * @param destBaseDir dest dir for csv files of subset.
     * @param maxNumUtt, int maximum number of utterance to use.
     */
    private List<String> createSubset_RealAnno(String srcBaseDir, String destBaseDir, int maxiNumUtt, int minNumUtt) {
       
        List<String> infoList = new ArrayList<>();
        infoList.add("==== createSubset_RealAnno ====");
        infoList.add("srcBaseDir=" + srcBaseDir);
        infoList.add("destBaseDir=" + destBaseDir);
        infoList.add("maxiNumUtt=" + maxiNumUtt);
        infoList.add("minNumUtt=" + minNumUtt);
        
        //System.out.print(" -----------only all ----");
        //System.out.print(this.OnlyIncludeIntents.toString());
        
        //List<String> exceptDomainIntents = new ArrayList<String>(Arrays.asList(this.FinalExceptIntents));        
        List<String> fileNames = null;
        if(this.OnlyIncludeIntents != null && this.OnlyIncludeIntents.size() != 0)  {
            fileNames = this.OnlyIncludeIntents;
            System.out.print(" -----------only all ----inside ");
            System.out.print(this.OnlyIncludeIntents.toString());

        } else {
            fileNames = GeneralUtils.getAllFileNames(srcBaseDir);
        } 

        
        for (int i = 0; i < fileNames.size(); ++i) {
            String filename = fileNames.get(i);
            System.out.print("File name only include:" + filename);
            if(!filename.endsWith(".csv")) filename = filename + ".csv";
            String url = srcBaseDir + "/" + filename;
            if(!(new File(url).exists())) {// in the case FinalIncludeIntents
                System.out.println("Sorry, the file does not exist:" + url);
                System.exit(-1);
            }
            
            String domainIntent = filename;
            if(filename.endsWith(".csv"))
                domainIntent = filename.substring(0,filename.lastIndexOf(".csv"));
            
            if(FinalExceptIntents != null && FinalExceptIntents.size() != 0
                    && FinalExceptIntents.contains(domainIntent)) {
                String msg = "PreDefined to Skip:" + filename + "\n";
                System.out.println(msg);
                infoList.add(msg);
                
                continue;
            }
            
            List<String> ansList = GeneralUtils.readFromTextFile(url); // from csv file
            if (ansList.size() < minNumUtt) {
                String msg1 = "Total size is less than" + minNumUtt + ". Real Size:" + ansList.size();
                String msg2 = "File Skipped:" + filename + "\n";
                infoList.add(msg1);
                infoList.add(msg2);
                continue;
            }

            int numUttToUse = ansList.size();
            if (numUttToUse > maxiNumUtt) {
                numUttToUse = maxiNumUtt;
            }
            
            // just take first part to use, as it has already been shuffled.
            List<String> subList = ansList.subList(0, numUttToUse);

            // save it.
            // String destUrl = destBaseDir + "/" + fileNames.get(i);
            String destUrl = destBaseDir + "/" + filename;
            GeneralUtils.checkAndCreateDirectory(destBaseDir);
            GeneralUtils.saveToFile(destUrl, subList);
        }
        
        return infoList;
    }

    private void reorganiseCsv() {
        // based on conditions. e.g. extract all query related intents for testing.
        String srcBaseDir = "resources/inputdataReal/finalAnnotated/ollydemo";
        String srcCsvFile = "final-utt-anno-full-v3.csv";    
        String csvUri = srcBaseDir + "/" + srcCsvFile;
        List<String> csvlines = GeneralUtils.readFromTextFile(csvUri);

        List<Map<String, String>> rawData = IntEntUtils.parseCSVFile(csvUri);
        //String[] headerArr = IntEntUtils.getHeadersOfCsv(csvUri);
        List<String> resCsvLines = new ArrayList<>();
        resCsvLines.add(csvlines.get(0)); // header
        for (int i = 0; i < rawData.size(); ++i) {
            Map<String, String> lineMap = rawData.get(i);
            //String tab = IntEntUtils.TAB;
            String intentCol = lineMap.get(IntEntUtils.Col_Intent).trim();
            if(intentCol.contains("query")) {
                resCsvLines.add(csvlines.get(i+1)); // Map range: 1..N, orig csv: 0...N
            }
        }
        String destFile = srcCsvFile.substring(0, srcCsvFile.lastIndexOf(".")) + "_Gene_AllQuery.csv";
        String destUri = srcBaseDir + "/" + destFile;
        GeneralUtils.saveToFile(destUri, resCsvLines);
    }
        
    public void loadConfig() {

        try {
            File file = new File(CONFIG_FILE);
            FileInputStream fileInput = new FileInputStream(file);
            configProperties = new Properties();
            configProperties.load(fileInput);
            fileInput.close();
            
            origAnnoCsvUrl = configProperties.getProperty("origAnnoCsvUrl");
            destRootDirTop = configProperties.getProperty("destRootDirTop");
            String sMaxNumUtt = configProperties.getProperty("maxNumUtt"); // default=500
            maxNumUtt = Integer.parseInt(sMaxNumUtt);

            String sMinNumUtt = configProperties.getProperty("minNumUtt"); // default = 10
            minNumUtt = Integer.parseInt(sMinNumUtt);

            String irrFlag = configProperties.getProperty("flagPrefixForIgnoreUtterance"); // default IRR
            IntEntUtils.Tag_IRR = irrFlag;

            String sLongUtt = configProperties.getProperty("maxNumLongUtt"); // default = 30
            if(sLongUtt != null && !sLongUtt.equals(""))
                maxNumLongUtt = Integer.parseInt(sLongUtt);
            
            
            String sTrain = configProperties.getProperty("trainsetPercent"); // defaut=80
            trainPercent = Integer.parseInt(sTrain);

            String sTest = configProperties.getProperty("testsetPercent"); // defaut=20
            testPercent = Integer.parseInt(sTest);

            String sKFold = configProperties.getProperty("numKFold");
            numKFold = Integer.parseInt(sKFold);
            
            String single = configProperties.getProperty("ExcludeSingleWord");
            ExcludeSingleWord = true;
            if(single.toLowerCase().equals("false"))
               ExcludeSingleWord = false;

            IntentTier = configProperties.getProperty("IntentTier");
            String exclude = configProperties.getProperty("ExcludeIntents");
            String onlyIncl = configProperties.getProperty("OnlyIncludeIntents");
            if(exclude != null && !exclude.equals("")) {
                String[] exclArr = exclude.split("\\s*,\\s*");
                this.FinalExceptIntents = new ArrayList<String>(Arrays.asList(exclArr)); 
            }
           if(onlyIncl != null && !onlyIncl.equals("")) {            
                String[] inclArr = onlyIncl.split("\\s*,\\s*");
                 System.out.println("In load size len: "+ inclArr.length);
                this.OnlyIncludeIntents = new ArrayList<String>(Arrays.asList(inclArr)); 
                System.out.println("In load size: "+this.OnlyIncludeIntents.size()); 
           }
                      
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(-1);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
        
    /**
     * the main method.
     *
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) {
        PreprocessRealData p = new PreprocessRealData();
    }

}
