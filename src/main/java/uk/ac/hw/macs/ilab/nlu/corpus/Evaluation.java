/**
 * ************************************************************
 * @(#)Evaluation.java
 *
 *
 *              Copyright(C) 2016 iLab MACS
 *              Heriot-Watt University
 *
 *                  All rights reserved
 *
 *   Version: 0.1  Created on 28 Feb 2017
 *
 *   Author: Dr Xingkun Liu, Dr Verena Rieser
 *
 *   Project: Emotech/TheDataLab
 *
 *************************************************************
 */
package uk.ac.hw.macs.ilab.nlu.corpus;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import static uk.ac.hw.macs.ilab.nlu.corpus.PreprocessRealData.CONFIG_FILE;
import uk.ac.hw.macs.ilab.nlu.util.Pair;
import uk.ac.hw.macs.ilab.nlu.util.GeneralUtils;

/**
 * To evaluate 
 *
 *
 * @author X.Liu, V.Rieser
 */
public class Evaluation {

    public boolean RasaLatestVersion = true;
    String destRootDirTop = ".";
    int numRankingCheck = 3; // check top number of intent ranking in RASA
    String RasaPipeline = "spacy_sklearn";
    
    public Evaluation() {

        loadConfig();

        System.out.println("\n==== Evaluation ==== ");
        System.out.println("destRootDirTop = " + destRootDirTop);
        System.out.println("RasaLatestVersion = " + RasaLatestVersion);
        System.out.println("numRankingCheck = " + numRankingCheck);
        System.out.println("RasaPipeline = " + RasaPipeline);
        
        System.out.println("\n====  Starting process ........\n");

        doEvaluation_Manually_Top();
        
        //doEvaluation_Manually_CV_top();
        
      
    }

    private void doEvaluation_Manually_CV_top() {
        //String serviceName = IntEntUtils.ServiceName_RASA;
        //String serviceName = IntEntUtils.ServiceName_APIAI;
        //String serviceName = IntEntUtils.ServiceName_LUIS;
        String serviceName = IntEntUtils.ServiceName_WATSON;

        String srcProjRoot = "./";

        String timeStamp = "2018_03_22-13_01_25_169";

        String evalDestRoot = "./evaluateResults/KFolds-Test-N1";
        int numKFold = 10; 
        for (int i = 1; i <= numKFold; ++i) {
            String kfold = String.valueOf(i);
            doEvaluation_Manually_CV(serviceName, srcProjRoot,timeStamp, kfold, evalDestRoot, null );
        }
    }
    
    
    public void doEvaluation_Manually_Top() {
        //String srcRoot = "/home/xl127/projects/emotech-new-2018/Data-Release/PrepareDataForRasaLucine";
        String srcRoot = "./";
        String evalDestRoot = srcRoot + "/evaluateResults";
        String testsetTimeStamp  = "2018_03_16-09_18_47_204";
        String testResRootDir = srcRoot;
        String testResSubDir ="testResults/RASA/RASA_TestResults_80Train_spacy_sklearn_2018_03_16-09_18_47_204-1";
        //String testResSubDir ="testResults/APIAI/APIAI_TestResults_trainset_2018_03_16-09_18_47_204-1";
        //String testResSubDir ="testResults/LUIS/LUIS_TestResults_trainset_2018_03_16-09_18_47_204-1";
        //String testResSubDir ="testResults/Watson/Watson_TestResults_trainset_2018_03_16-09_18_47_204-1";
                
        String serviceName = IntEntUtils.ServiceName_RASA;
        //String serviceName = IntEntUtils.ServiceName_APIAI;
        //String serviceName = IntEntUtils.ServiceName_LUIS;
        //String serviceName = IntEntUtils.ServiceName_WATSON;
        String destSubDir = "evalResults_N1";
        
        doEvaluation_Manually(serviceName, srcRoot,testResRootDir, testResSubDir,
                testsetTimeStamp, evalDestRoot, destSubDir, null );
        
    }
    
    public void doEvaluation_Manually(String serviceName, String srcRoot, String testResRoot, String testResSubDir,
            String timeStamp, String evalDestRoot, String destSubDir, List<String> evalEntityNames ) {
        // destSubDir is used to distinguish the same model with different testsets
        // e.g. gram model tested by gram testset and realdata testset.
        // it could be "Normal", or "GramModel_xxx" "RealDataModel_xxx"
        // 
        // timeStamp: the groundTruth timeStamp
        
        String testResRootDir = srcRoot;  
        if(testResRoot != null)
            testResRootDir = testResRoot;
        
        String queryResultDomainBaseDir = testResRootDir + "/" + testResSubDir;

        String preprocessBaseDir = srcRoot + "/preprocessResults/autoGeneFromRealAnno/autoGene_" + timeStamp;
        String annoDomainBaseDir = preprocessBaseDir + "/DomainIntentNoDupShuffleSubset_Test/annotated";
        
        int crntEvalIdx = 1;

        String suff = "trainset";
        
        if(serviceName.equals(IntEntUtils.ServiceName_RASA))
            suff += "_" + RasaPipeline;
        
        suff += "_" + timeStamp;

       // String destDir = evalDestRoot + "/evaluateResults_Manual/" + serviceName
        String destDir1 = evalDestRoot  +"/"+ serviceName + "/" + destSubDir;
        String destDir = destDir1  + "/" + serviceName + "_EvalRes_" + suff + "-" + String.valueOf(crntEvalIdx);

        GeneralUtils.checkAndCreateDirectory(destDir);
        
        // ---------------------------
        // use Confusion Tables;
        //String fullSetDir = "DomainIntentNoDupShuffleSubset";
        String testSetCsvDir = "DomainIntentNoDupShuffleSubset_Test/csv";
        String evalIntentSetDir = preprocessBaseDir + "/" + testSetCsvDir;
        String evalEntitySetDir = evalIntentSetDir;
         // specify which entities to eval, e.g. gram-real cross eval where gram contain less entities.
       // if(evalEntityNameDir != null)
       //      evalEntitySetDir = evalEntityNameDir;
        
        List<String> evalEntities = evalEntityNames;
        if(evalEntityNames == null)
            evalEntities = IntEntUtils.extractUniqueEntitiesFromAnno_AllDomain(evalEntitySetDir);
        
        List<List<String>> confInt = evaluateIntentsByConfTable(serviceName, evalIntentSetDir,
                annoDomainBaseDir, queryResultDomainBaseDir);

        String destFileConfIntNum = serviceName + "_IntentConfRes_Nums_" + timeStamp + ".txt";
        String destFileConfIntNumCsv = serviceName + "_IntentConfRes_NumsCsv_" + timeStamp + ".csv";
        // true for appending. null for using default encoding.
        // false for overwritting it since each evaluation file is separated
        GeneralUtils.saveToTextFile(destDir, destFileConfIntNum, confInt.get(0), false, null);
        GeneralUtils.saveToTextFile(destDir+"/csv", destFileConfIntNumCsv, confInt.get(1), false, null);

        String destFileConfIntScore = serviceName + "_IntentConfRes_Scores_" + timeStamp + ".txt";
        String destFileConfIntScoreCsv = serviceName + "_IntentConfRes_ScoresCsv_" + timeStamp + ".csv";
        
        // true for appending. null for using default encoding.
        // false for overwritting it since each evaluation file is separated
        GeneralUtils.saveToTextFile(destDir, destFileConfIntScore, confInt.get(2), false, null);
        GeneralUtils.saveToTextFile(destDir+"/csv", destFileConfIntScoreCsv, confInt.get(3), false, null);

        // ---------------------------
        // calculate confusion table scores for entities
        List<List<String>> confEnt = evaluateEntitiesByConfTable(serviceName, evalEntities,
                annoDomainBaseDir, queryResultDomainBaseDir);

        String destFileConfEntNum = serviceName + "_EntityConfRes_Nums_" + timeStamp + ".txt";
        String destFileConfEntNumCsv = serviceName + "_EntityConfRes_NumsCsv_" + timeStamp + ".csv";
        // true for appending. null for using default encoding.
        // false for overwritting it since each evaluation file is separated
        GeneralUtils.saveToTextFile(destDir, destFileConfEntNum, confEnt.get(0), false, null);
        GeneralUtils.saveToTextFile(destDir+"/csv", destFileConfEntNumCsv, confEnt.get(1), false, null);

        String destFileConfEntScore = serviceName + "_EntityConfRes_Scores_" + timeStamp + ".txt";
        String destFileConfEntScoreCsv = serviceName + "_EntityConfRes_ScoresCsv_" + timeStamp + ".csv";
        GeneralUtils.saveToTextFile(destDir, destFileConfEntScore, confEnt.get(2), false, null);
        GeneralUtils.saveToTextFile(destDir+"/csv", destFileConfEntScoreCsv, confEnt.get(3), false, null);
        
        // ---------------------------
        System.out.println("\nEvaluationMethod2, intent results nums are saved in " + destFileConfIntNum);
        System.out.println("\nEvaluationMethod2, intent results scores are saved in " + destFileConfIntScore);        
        System.out.println("\nEvaluationMethod2, entity results nums are saved in " + destFileConfEntNum);
        System.out.println("\nEvaluationMethod2, entity results scores are saved in " + destFileConfEntScore);
        System.out.println("\n");
        
        System.out.println("Finished doEvaluation_Manually! \n");
    }

    /**
     * Evalation for Cross Validation
     * 
     * @param serviceName
     * @param srcProjRoot, the src project package root dir.
     * @param testResSubDir
     * @param timeStamp
     * @param evalDestRoot
     * @param destSubDir
     * @param evalEntityNames: null as default: testsetcsv dir
     */
    public void doEvaluation_Manually_CV(String serviceName, String srcProjRoot,
            String timeStamp, String kfold, String evalDestRoot, List<String> evalEntityNames ) {
        // destSubDir is used to distinguish the same model with different testsets
        // e.g. gram model tested by gram testset and realdata testset.
        // it could be "Normal", or "GramModel_xxx" "RealDataModel_xxx"
        // 
        // timeStamp: the groundTruth timeStamp
        
        // testResDir: testResults/APIAI/APIAI_TestResults_KFold_2018_03_22-13_01_25_169-1/KFold_1
        // evalResDir : rootDir + "/evaluateResults_CV/APIAI/APIAI_EvalRes_2018_03_16-09_18_47_204-1"
        //         + "/KFold_1"
        
       // String testResRootDir = srcRoot; 
        String crntIdx = "1"; // for now, for CV it is always 1
        String testResSubDir = "testResults/" + serviceName + "/" + serviceName + "_TestResults_KFold_"
                + timeStamp + "-" + crntIdx + "/KFold_" + kfold;
        String queryResultDomainBaseDir = srcProjRoot + "/" + testResSubDir;

        String preprocessBaseDir = srcProjRoot + "/preprocessResults/autoGeneFromRealAnno/autoGene_" + timeStamp;
        String srcKFoldBaseDir = preprocessBaseDir + "/CrossValidation/KFold_" + kfold;
        String annoDomainBaseDir = srcKFoldBaseDir + "/testset/annotated";
        
        String testSetCsvDir = srcKFoldBaseDir + "/testset/csv";
        String evalIntentSetDir = testSetCsvDir;
        String evalEntitySetDir = evalIntentSetDir;

        
        int crntEvalIdx = 1;

        String suff = "KFold";
        
       // if(serviceName.equals(IntEntUtils.ServiceName_RASA))
       //     suff += "_" + RasaPipeline;
        
        suff += "_" + timeStamp;

       // String destDir = evalDestRoot + "/evaluateResults_Manual/" + serviceName
        String destDir = evalDestRoot  +"/"+ serviceName + "/"
                + "/" + serviceName + "_EvalRes_" + suff + "-" + String.valueOf(crntEvalIdx);
        
        destDir += "/CV/KFold_" + kfold;
        
        GeneralUtils.checkAndCreateDirectory(destDir);
        
        List<String> evalEntities = evalEntityNames;
        if(evalEntityNames == null)
            evalEntities = IntEntUtils.extractUniqueEntitiesFromAnno_AllDomain(evalEntitySetDir);
        
        List<List<String>> confInt = evaluateIntentsByConfTable(serviceName, evalIntentSetDir,
                annoDomainBaseDir, queryResultDomainBaseDir);

        String destFileConfIntNum = serviceName + "_IntentConfRes_Nums_" + timeStamp + ".txt";
        String destFileConfIntNumCsv = serviceName + "_IntentConfRes_NumsCsv_" + timeStamp + ".csv";
        // true for appending. null for using default encoding.
        // false for overwritting it since each evaluation file is separated
        GeneralUtils.saveToTextFile(destDir, destFileConfIntNum, confInt.get(0), false, null);
        GeneralUtils.saveToTextFile(destDir+"/csv", destFileConfIntNumCsv, confInt.get(1), false, null);

        String destFileConfIntScore = serviceName + "_IntentConfRes_Scores_" + timeStamp + ".txt";
        String destFileConfIntScoreCsv = serviceName + "_IntentConfRes_ScoresCsv_" + timeStamp + ".csv";
        
        // true for appending. null for using default encoding.
        // false for overwritting it since each evaluation file is separated
        GeneralUtils.saveToTextFile(destDir, destFileConfIntScore, confInt.get(2), false, null);
        GeneralUtils.saveToTextFile(destDir+"/csv", destFileConfIntScoreCsv, confInt.get(3), false, null);

        // ---------------------------
        // calculate confusion table scores for entities
        List<List<String>> confEnt = evaluateEntitiesByConfTable(serviceName, evalEntities,
                annoDomainBaseDir, queryResultDomainBaseDir);

        String destFileConfEntNum = serviceName + "_EntityConfRes_Nums_" + timeStamp + ".txt";
        String destFileConfEntNumCsv = serviceName + "_EntityConfRes_NumsCsv_" + timeStamp + ".csv";
        // true for appending. null for using default encoding.
        // false for overwritting it since each evaluation file is separated
        GeneralUtils.saveToTextFile(destDir, destFileConfEntNum, confEnt.get(0), false, null);
        GeneralUtils.saveToTextFile(destDir+"/csv", destFileConfEntNumCsv, confEnt.get(1), false, null);

        String destFileConfEntScore = serviceName + "_EntityConfRes_Scores_" + timeStamp + ".txt";
        String destFileConfEntScoreCsv = serviceName + "_EntityConfRes_ScoresCsv_" + timeStamp + ".csv";
        GeneralUtils.saveToTextFile(destDir, destFileConfEntScore, confEnt.get(2), false, null);
        GeneralUtils.saveToTextFile(destDir+"/csv", destFileConfEntScoreCsv, confEnt.get(3), false, null);
        
        // ---------------------------
        System.out.println("\nEvaluationMethod2, intent results nums are saved in " + destFileConfIntNum);
        System.out.println("\nEvaluationMethod2, intent results scores are saved in " + destFileConfIntScore);        
        System.out.println("\nEvaluationMethod2, entity results nums are saved in " + destFileConfEntNum);
        System.out.println("\nEvaluationMethod2, entity results scores are saved in " + destFileConfEntScore);
        System.out.println("\n");
        
        System.out.println("Finished doEvaluation_Manually! \n");
    }
    
    private String getRecognisedIntent(String serviceName, JsonObject itemJO) {
        String recgIntent = null;
        if (serviceName.equals(IntEntUtils.ServiceName_APIAI)) {
            recgIntent = getRecognisedIntent_Apiai(itemJO);
        } else if (serviceName.equals(IntEntUtils.ServiceName_RASA)) {
            if (RasaLatestVersion) {
                recgIntent = getRecognisedIntent_Rasa_Latest(itemJO);
            } else {
                recgIntent = getRecognisedIntent_Rasa_Stable(itemJO);
            }                
        } else if (serviceName.equals(IntEntUtils.ServiceName_LUIS)) {
            recgIntent = getRecognisedIntent_Luis(itemJO);

        } else if (serviceName.equals(IntEntUtils.ServiceName_WATSON)) {
            recgIntent = getRecognisedIntent_Watson(itemJO);
        } else {
            // nothing for now
            System.err.println("\n ERROR: Not yet supported service:" + serviceName + "\n");               
        }
        return recgIntent;
    }

    private String getRecognisedIntentConf(String serviceName, JsonObject itemJO) {
        String conf = null;
        if (serviceName.equals(IntEntUtils.ServiceName_RASA) && RasaLatestVersion) {
           
            if (!itemJO.get("intent").isJsonObject() || itemJO.get("intent").isJsonNull()) {
                return null;
            }
           // String intent = itemJO.get("intent").getAsString();
           conf = ((JsonObject) itemJO.get("intent")).get("confidence").getAsString();
        
           
        } else if (serviceName.equals(IntEntUtils.ServiceName_APIAI) ) {
            // Apiai: "score": 0.4399999976158142, 
            JsonObject resJO = itemJO.get("result").getAsJsonObject();
            conf = resJO.get("score").getAsString();
            
        } else if (serviceName.equals(IntEntUtils.ServiceName_LUIS) ) {
            // Luis: "confidence": 0.999577165,
            conf = itemJO.get("confidence").getAsString();
            
        } else if (serviceName.equals(IntEntUtils.ServiceName_WATSON) ) {                          
            JsonArray intentsJA = itemJO.getAsJsonArray("intents");
            // occassionally Watson intents array is empty
            if(intentsJA.size() > 0) {
                JsonObject intJO = intentsJA.get(0).getAsJsonObject();
                conf = intJO.get("confidence").getAsString();

            } else {
                conf ="0";
            }
            
        } else {
            System.err.println("\n ERROR: Not yet supported service:" + serviceName + "\n");   
        }
        
        if(conf != null) {
            double confd = Double.parseDouble(conf);
            String confs = new DecimalFormat("#.###").format(confd);
            return confs;
        } else {
            return null;
        }
        
    }

    private String getQueryUtterance(String serviceName, JsonObject itemJO) {
        String queryUtt = "";
        if (serviceName.equals(IntEntUtils.ServiceName_APIAI)) {
            queryUtt = getQueryUtterance_Apiai(itemJO);
        } else if (serviceName.equals(IntEntUtils.ServiceName_RASA)) {
            queryUtt = getQueryUtterance_Rasa(itemJO);
        } else if (serviceName.equals(IntEntUtils.ServiceName_LUIS)) {
            queryUtt = getQueryUtterance_Luis(itemJO);

        } else if (serviceName.equals(IntEntUtils.ServiceName_WATSON)) {
            queryUtt = getQueryUtterance_Watson(itemJO);

        } else {
            // nothing for now
            System.err.println("\n ERROR: Not yet supported service:" + serviceName + "\n");  
        }

        return queryUtt;
    }

    private JsonArray getRecognisedEntities(String serviceName, JsonObject itemJO) {
        JsonArray recgEntitiesJA = null;

        if (serviceName.equals(IntEntUtils.ServiceName_APIAI)) {
            recgEntitiesJA = getRecognisedEntities_Apiai(itemJO);

        } else if (serviceName.equals(IntEntUtils.ServiceName_RASA)) {
            recgEntitiesJA = getRecognisedEntities_Rasa(itemJO);

        } else if (serviceName.equals(IntEntUtils.ServiceName_LUIS)) {
            recgEntitiesJA = getRecognisedEntities_Luis(itemJO);

        } else if (serviceName.equals(IntEntUtils.ServiceName_WATSON)) {
            recgEntitiesJA = getRecognisedEntities_Watson(itemJO);

        } else {
            // nothing for now
            System.err.println("\n ERROR: Not yet supported service:" + serviceName + "\n"); 
        }
        return recgEntitiesJA;
    }
    
    private String getRankNameInfo(List<String> intentRankingNames, int numRankingCheck) {
        if(intentRankingNames == null || intentRankingNames.size() == 0 ||numRankingCheck == 1) 
            return "";
        int maxNum = numRankingCheck;
        if(maxNum > intentRankingNames.size())
            maxNum = intentRankingNames.size();
        
        String res = "";
        for(int i = 1; i < maxNum; ++i) {
            res += intentRankingNames.get(i);
            if(i < maxNum-1)
                res += ",";
        }
        return res;
    }
        
    private List<List<String>> evaluateEntitiesByConfTable(String serviceName, List<String> evalEntities,
            String testsetAnnoDomainBaseDir,
            String queryResultDomainBaseDir) {

        List<String> domainDirNames = GeneralUtils.getAllSubdirNames(testsetAnnoDomainBaseDir);

        Map<String, List<Integer>> evalMap = new LinkedHashMap();
        int maxEntityNameLen = 0;
        
        for (int ik = 0; ik < evalEntities.size(); ++ik) {
            String evalEntity = evalEntities.get(ik);
            if (evalEntity.startsWith("e_")) { // grammar generated data entity starting with "e_"
                evalEntity = evalEntity.substring(2);
            }            
           
            List<Integer> aTbl = new ArrayList<Integer>();
            if (maxEntityNameLen < evalEntity.length()) {
                maxEntityNameLen = evalEntity.length();
            }
            
            domainDirNames = GeneralUtils.getAllSubdirNames(testsetAnnoDomainBaseDir);
            int numDomains = domainDirNames.size();

            String resultDomainDir = queryResultDomainBaseDir;
            for (int k = 0; k < numDomains; ++k) {

                JsonArray annoJA = null;
                String domainName = domainDirNames.get(k);
                String annoDomainDir = testsetAnnoDomainBaseDir + "/" + domainName;
                JsonObject annoJO = getAnnotatedDomainData(annoDomainDir);
                annoJA = annoJO.get("test_data_annotation").getAsJsonArray();
                resultDomainDir = queryResultDomainBaseDir + "/" + domainName;                

                String fileUri = resultDomainDir + "/" + GeneralUtils.getFirstFileName(resultDomainDir);

                JsonObject resJO = GeneralUtils.readJsonFile(fileUri);
                JsonArray resJA = resJO.get("results").getAsJsonArray();

                if(annoJA.size() != resJA.size()) {
                    System.err.println("\nAnnotated testset size is different from test results size!");
                    System.err.println("Annotated testset size:" + annoJA.size());
                    System.err.println("Testing results size:" + resJA.size() + "\n");
                    System.exit(-1);
                }
                
                JsonArray recgEntitiesJA = null;
                for (int i = 0; i < resJA.size(); ++i) { // each result is for one utterance

                    JsonObject itemJO = resJA.get(i).getAsJsonObject();
                    
                    String queryUtt = getQueryUtterance(serviceName, itemJO);
                    recgEntitiesJA = getRecognisedEntities(serviceName, itemJO);

                    // get from ground truth.
                    JsonObject annoUttJO = annoJA.get(i).getAsJsonObject();
                    String annoUtt = annoUttJO.get("text").getAsString();
                                    
                    if(!annoUtt.equalsIgnoreCase(queryUtt)) { // so queryUtt should be in same sequence as annoUtt
                        System.err.println("\nAnnotated utt is different from testing utt!");
                        System.err.println("Anno utt:" + annoUtt);
                        System.err.println("Test utt:" + queryUtt + "\n");
                        System.exit(-1);
                    }
                                        
                    JsonArray annoEntitiesJA = annoUttJO.getAsJsonArray("entities");

                    //aTbl = createTableOfConfusion4Entity_AnyOccr(serviceName, evalEntity, annoEntitiesJA, recgEntitiesJA, aTbl);
                    aTbl = createTableOfConfusion4Entity_MulOccr(serviceName, evalEntity, annoEntitiesJA, recgEntitiesJA, aTbl);                    
                }

            } // end of evaluating one entity in all domains

            evalMap.put(evalEntity, aTbl);

        } // end of all evalClass -- entities

        // print out the map: Entity Confusion Table values         
        List<List<String>> confTableNums = getConfTableNums("Entity", maxEntityNameLen, evalMap);
        List<List<String>> confTableScores = calculateScores("Entity", maxEntityNameLen, evalMap);

        //TODO could put them in one list and write to just one file
        List<List<String>> finalRes = new ArrayList();
        finalRes.add(confTableNums.get(0));
        finalRes.add(confTableNums.get(1));
        finalRes.add(confTableScores.get(0));
        finalRes.add(confTableScores.get(1));
        
        return finalRes;

    }

    /**
     * Calculate Intent F1-scores via Confusion Tables.
     *
     * @param serviceName
     * @param fullIntentDir
     * @param testsetAnnoDomainBaseDir
     * @param queryResultDomainBaseDir
     * @return List of a list: [confNumsList, confScoresList]
     */
    private List<List<String>> evaluateIntentsByConfTable(String serviceName, String evalSetDir,
            String testsetAnnoDomainBaseDir,
            String queryResultDomainBaseDir) {

        //get ground truth -- annotated utterances. They are in the same order as sending for query.
        List<String> domainDirNames = GeneralUtils.getAllSubdirNames(testsetAnnoDomainBaseDir);

        List<String> evalIntents = getAllIntentForEvaluate(evalSetDir);

        Map<String, List<Integer>> evalMap = new LinkedHashMap();
        int maxIntentNameLen = 0; // used for output spaces for line alignment
        for (int ik = 0; ik < evalIntents.size(); ++ik) {
            String evalIntent = evalIntents.get(ik);
            List<Integer> aTbl = new ArrayList<Integer>();
            if (maxIntentNameLen < evalIntent.length()) {
                maxIntentNameLen = evalIntent.length();
            }

            int numDomains = domainDirNames.size();
            String resultDomainDir = queryResultDomainBaseDir;
            for (int k = 0; k < numDomains; ++k) {

                JsonArray annoJA = null;
                String domainName = domainDirNames.get(k);
                String annoDomainDir = testsetAnnoDomainBaseDir + "/" + domainName;
                System.out.println("annoDomainDir:" + annoDomainDir);
                JsonObject annoJO = getAnnotatedDomainData(annoDomainDir);
                annoJA = annoJO.get("test_data_annotation").getAsJsonArray();
                resultDomainDir = queryResultDomainBaseDir + "/" + domainName;                

                String fileUri = resultDomainDir + "/" + GeneralUtils.getFirstFileName(resultDomainDir);

                JsonObject resJO = GeneralUtils.readJsonFile(fileUri);
                JsonArray resJA = resJO.get("results").getAsJsonArray();

                if(annoJA.size() != resJA.size()) {
                    System.err.println("\nAnnotated testset size is different from test results size!");
                    System.err.println("Annotated testset size:" + annoJA.size());
                    System.err.println("Testing results size:" + resJA.size() + "\n");
                    System.exit(-1);
                }

                for (int i = 0; i < resJA.size(); ++i) { // each result is for one utterance

                    JsonObject itemJO = resJA.get(i).getAsJsonObject();
                    String queryUtt = getQueryUtterance(serviceName, itemJO);
                    
                    // get from ground truth.
                    JsonObject annoUttJO = annoJA.get(i).getAsJsonObject();
                    String annoUtt = annoUttJO.get("text").getAsString();
                    if(!annoUtt.equalsIgnoreCase(queryUtt)) { // so queryUtt should be in same sequence as annoUtt
                        System.err.println("\nAnnotated utt is different from testing utt!");
                        System.err.println("Anno utt:" + annoUtt);
                        System.err.println("Test utt:" + queryUtt + "\n");
                        System.exit(-1);
                    }

                    String recgIntent = null;
                    recgIntent = getRecognisedIntent(serviceName, itemJO);
                    
                    String annoIntent = "";
                    if (annoUttJO.get("intent") != null) {
                        annoIntent = annoUttJO.get("intent").getAsString();
                    }

                    aTbl = createTableOfConfusion(evalIntent, annoIntent, recgIntent, aTbl);

                }
            } // end of evaluating one intent in all demains

            evalMap.put(evalIntent, aTbl);

        } // end of all intents

        
        // print out the map: Intents Confusion Table values        
        List<List<String>> confTableNums = getConfTableNums("Intent", maxIntentNameLen, evalMap);
        
        //TODO could put them in one list and write to just one file
        List<List<String>> confTableScores = calculateScores("Intent", maxIntentNameLen, evalMap);
        List<List<String>> finalRes = new ArrayList();
        finalRes.add(confTableNums.get(0));
        finalRes.add(confTableNums.get(1));
        finalRes.add(confTableScores.get(0));
        finalRes.add(confTableScores.get(1));
        
        return finalRes;
    }

    private List<List<String>> getConfTableNums(String evalType, int nameMaxLen, Map<String, List<Integer>> evalMap) {
        // evalType: Intent or Entity
        List<String> strRes = new ArrayList();
        List<String> strResCsv = new ArrayList();
        String tab = ";";
        String msg = "\n ====  Confusion Table values " + evalType + " ====\n";
        System.out.println(msg);
        strRes.add(msg);

        msg = "TP = True Positives, FP = False Positives, FN = False Negatives, TN = True Negatives\n";
        System.out.println(msg);
        strRes.add(msg);
        
        int maxItemLen = 5;
        String sepSpace = getSpace(4);
        
        String space = "";
        space = getSpace(nameMaxLen-evalType.length());
        String ts = getSpace(maxItemLen -2);
        msg = space + evalType + "   TP" +ts + sepSpace + "FP"+ts + sepSpace  + "FN" +ts + sepSpace + "TN";
        System.out.println(msg);
        strRes.add(msg);
        String msgCsv = evalType +tab+ "TP" +tab+ "FP"+tab +"FN" +tab+ "TN";
        strResCsv.add(msgCsv);
        
        List<String> irrEvalNames =  new ArrayList();
        
        // sort the map keys
        SortedSet<String> keys = new TreeSet<String>(evalMap.keySet());
        for (String key : keys) { 
            List<Integer> values = evalMap.get(key);
                    
            String tp = values.get(0).toString();
            String tps = getSpace(maxItemLen - tp.length());
            String fp = values.get(1).toString();
            String fps = getSpace(maxItemLen - fp.length());
            String fn = values.get(2).toString();
            String fns = getSpace(maxItemLen - fn.length());
            String tn = values.get(3).toString();
            
            String line = tp + tps + sepSpace + fp + fps + sepSpace + fn +fns+sepSpace + tn;
            space = getSpace(nameMaxLen - key.length());

            msg = space + key + " : " + line;
            String lineCsv = key + tab + tp + tab + fp + tab + fn +tab + tn;
            
            if(values.get(0) == 0.0 && values.get(2) == 0.0) {
                // tp=0 and fn=0, not defined in testset, not report it
                irrEvalNames.add(msg);
            } else {

                System.out.println(msg);
                strRes.add(msg);
                strResCsv.add(lineCsv);
            }
            
        }
        
        msg = " ---------------- \n";
        System.out.println(msg);
        strRes.add(msg);
        int size = evalMap.size() - irrEvalNames.size();
        msg = "\n     Number of items evaluated: " + size + "\n";
        System.out.println(msg);
        strRes.add(msg);
        
        
        if(irrEvalNames.size() != 0) {
            msg = "\n    Items that are not defined in the testset:\n" ;
            System.out.println(msg);
            strRes.add(msg);
            
            for(int i = 0; i < irrEvalNames.size(); ++i) {
                msg = irrEvalNames.get(i);
                System.out.println(msg);
                strRes.add(msg);
            }
            msg = "\n";
            System.out.println(msg);
            strRes.add(msg);            
        }
        
        List<List<String>> strResFinal = new ArrayList();
        strResFinal.add(strRes);
        strResFinal.add(strResCsv);
        
        return strResFinal;
    }
    
    private List<List<String>> calculateScores(String evalType, int nameMaxLen, Map<String, List<Integer>> evalMap) {
        // evalType: Intent or Entity
        // The evalMap is the Confusion Matrix
        
        List<String> strRes = new ArrayList();
        List<String> strResCsv = new ArrayList();
        String tab = ";";
        
        String msg = "\n        ====  Performance Scores for " + evalType + " ====\n";
        System.out.println(msg);
        strRes.add(msg);
        
        if(evalType.equals("Entity")) {
            msg = "        (Only considered entity type matching, not the value!)  \n";
            System.out.println(msg);
            strRes.add(msg);
        }

        int maxItemLen = 6;
        int iSepSpc = 5;
        int tMaxItmeLen = maxItemLen + iSepSpc;
        
        String sepSpace = getSpace(iSepSpc);
        
        String space = "";
        space = getSpace(nameMaxLen-evalType.length());
        String tPre = "Precision";
        String tPreS = getSpace(tMaxItmeLen - tPre.length());
        String tRec = "Recall";
        String tRecS = getSpace(tMaxItmeLen - tRec.length());
        String tF1 = "F1-Score";
        String tF1S = getSpace(tMaxItmeLen - tF1.length());        
        String tAcc = "Accurary";
        
        msg = space + evalType +getSpace(3)+ tPre + tPreS + tRec + tRecS + tF1 + tF1S + tAcc;
        System.out.println(msg);
        strRes.add(msg);

        String msgCsv = evalType + tab+ tPre + tab + tRec + tab + tF1 +tab + tAcc;
        strResCsv.add(msgCsv);
        
        List<String> irrEvalNames =  new ArrayList();
        
        double tprAll = 0.0;
        double ppvAll = 0.0;
        double accAll = 0.0;
        double f1All = 0.0;

        // sort the map keys
        SortedSet<String> keys = new TreeSet<String>(evalMap.keySet());
        for (String key : keys) { 
            String evalName = key;
            List<Integer> tbl = evalMap.get(key);
            
            if (tbl == null || tbl.isEmpty()) {
                //strRes.add("ERROR ERROR ERROR!");
                //return strRes;
                return null;
            }

            double tp = (double) tbl.get(0); // True Positives
            double fp = (double) tbl.get(1); // False Positives
            double fn = (double) tbl.get(2); // False Negatives
            double tn = (double) tbl.get(3); // True Negatives

            // sensitivity, recall, hit rate, or true positive rate (TPR)
            double tpr;
            if((tp + fn) == 0.0) {
                // (tp + fn) == 0.0, i.e. tp=0 and fn=0, this means evalName is not defined
                // in the truth set i.e. in the testset, which means we didn't evaluate it,
                // (it was defined in the fullset but not appears in testset, 
                // fullset=trainset+testset)
                // so it is irrelevant to what we want to evaluate and removed from the report.
                //tpr = specNum;
                //tprAll = specNum; 
                irrEvalNames.add(evalName);
                
                continue; // evalName is irrelevant, not report.
            } else {
                tpr = tp / (tp + fn);
                //if(tprAll != specNum) // if previously had NaN, tprAll is a NaN: Infinite
                   tprAll += tpr; // xxxAll is for calculating average
            }

            //precision or positive predictive value (PPV)
            double ppv = tp / (tp + fp);
            
            if((tp + fp) == 0.0) {
                // (tp + fp) == 0.0, i.e. tp=0, fp=0, this means it didn't recognise any instance
                // of evalName from the testset. The recall = 0; we treat the precision is also 0.
                ppv = 0.0; // recall above is alsp zero since tp = 0.
            } else {
                ppv = tp / (tp + fp);
                ppvAll += ppv;
            }
            
            double f1;
            if(ppv == 0.0 || tpr == 0.0 ) {
                f1 = 0.0;
            } else {
                f1 = 2 * ppv * tpr / (ppv + tpr);
                f1All += f1;
            }
            
            // accurary
            double acc = (tp + tn) / (tp + tn + fp + fn);
            if((tp + tn + fp + fn) == 0.0 ) {
                // this shouldn't be a case as the worst case is tn = total num of test samples/utts
                System.err.println("\n  Error of the number of test samples!\n");
            }
            
            accAll += acc;
            
            String sPpv, sTpr, sF1 = "";
            sPpv = new DecimalFormat("#.####").format(ppv);
            sTpr = new DecimalFormat("#.####").format(tpr);
            sF1 = new DecimalFormat("#.####").format(f1);
            
            String sAcc = new DecimalFormat("#.####").format(acc);
            
            sPpv = sPpv + getSpace(maxItemLen-sPpv.length());
            sTpr = sTpr + getSpace(maxItemLen-sTpr.length());
            sF1 = sF1 + getSpace(maxItemLen-sF1.length());
            sAcc = sAcc + getSpace(maxItemLen-sAcc.length());
                        
            int numSpace = nameMaxLen - evalName.length();

            space = getSpace(numSpace);

            String res = space + evalName + " : " + sPpv + sepSpace + sTpr + sepSpace + sF1 
                    + sepSpace +  sAcc;
            System.out.println(res);
            strRes.add(res);
            
            msgCsv =  evalName + tab + sPpv + tab + sTpr + tab + sF1 + tab +  sAcc;
            strResCsv.add(msgCsv);
        }

        int size = evalMap.size() - irrEvalNames.size();

        String sPpvAll, sTprAll,sF1All, sAccAll;
        sPpvAll = new DecimalFormat("#.###").format(ppvAll / size);
        sTprAll = new DecimalFormat("#.###").format(tprAll / size);        
        sF1All = new DecimalFormat("#.###").format(f1All / size);
        
        sAccAll = new DecimalFormat("#.###").format(accAll / size);

        sPpvAll = sPpvAll + getSpace(maxItemLen- sPpvAll.length());
        sTprAll = sTprAll + getSpace(maxItemLen-sTprAll.length());
        sF1All = sF1All + getSpace(maxItemLen-sF1All.length());
        sAccAll = sAccAll + getSpace(maxItemLen-sAccAll.length());
        
        msg = "";
        System.out.println(msg);
        strRes.add(msg);
        
        String aveType = " === Average ===";
        if(nameMaxLen > msg.length())
            aveType = getSpace(nameMaxLen-aveType.length()) + aveType;
        
        String ave = aveType + " : " + sPpvAll + sepSpace + sTprAll + sepSpace + sF1All + sepSpace + sAccAll;
        
        System.out.println(ave);
        strRes.add(ave);
        
        msg = "\n     Number of items evaluated: " + size + "\n";
        System.out.println(msg);
        strRes.add(msg);
        
        String aveCsv = "Average" + tab + sPpvAll + tab + sTprAll + tab + sF1All + tab + sAccAll; 
        strResCsv.add(aveCsv);
        
        if(irrEvalNames.size() != 0) {
            String names= "";
            for(int i = 0; i < irrEvalNames.size(); ++i) {
                names += irrEvalNames.get(i) + "  ";
            }
            names = names.trim();
            
            msg = "\n    Items that are not defined in the testset:\n" + "    " + names + "\n";
            System.out.println(msg);
            strRes.add(msg);
        }
        
        List<List<String>> strResFinal = new ArrayList();
        strResFinal.add(strRes);
        strResFinal.add(strResCsv);
        
        return strResFinal;

    }

    private String getSpace(int count)  {
      String space = "";
      for(int i = 0; i < count; i++)
            space += " ";
      
       return space;
    }
    
    
    private List<String> getAllIntentForEvaluate(String srcDir) {
        // use DomainIntentNoDupShuffleSubset to get all Intents, since it contains trainset and testset
        System.out.println("getAllIntentForEvaluate srcDir:" + srcDir);
        File folder = new File(srcDir);
        File[] listOfFiles = folder.listFiles();

        List<String> fileNames = new ArrayList<>();
        for (int i = 0; i < listOfFiles.length; ++i) {
            if (listOfFiles[i].isDirectory()) {
                continue;
            } else {
                String fname = listOfFiles[i].getName();
                if (fname.endsWith(".csv")) {
                    fname = fname.substring(0, fname.length() - 4);
                }
                fileNames.add(fname);
            }
        }
        return fileNames;

    }

    /**
     * Create the Confusion Table.
     *                        predicted--Class
     *                    EvalCls (Yes)   NonEvalCls (No)
     * Actual--Class  
     *   EvalCls (Yes)         TP            FN 
     *   NonEvalCls (No)       FP            TN
     * 
     * tp: True Positives fp : False Positives fn : False Negatives tn : True
     * Negatives
     *
     *
     * @param evalClass
     * @param truthClass
     * @param recgClass
     * @param crntTable
     * @return
     */
    private List<Integer> createTableOfConfusion(String evalClass, String truthClass, String recgClass,
            List<Integer> crntTable) {
        // truthClass: actual truth class, recgClass: predicted class
        if (truthClass == null || recgClass == null) {
            return crntTable;
        }

        int tp = 0; // True Positives
        int fp = 0; // False Positives
        int fn = 0; // False Negatives
        int tn = 0; // True Negatives
        if (!crntTable.isEmpty()) {
            tp = crntTable.get(0);
            fp = crntTable.get(1);
            fn = crntTable.get(2);
            tn = crntTable.get(3);
        }

        if (truthClass.equalsIgnoreCase(evalClass)) {
            if (recgClass.equalsIgnoreCase(evalClass)) {
                tp += 1;
            } else {
                fn += 1;
            }
        } else if (recgClass.equalsIgnoreCase(evalClass)) {
            fp += 1;
        } else {
            tn += 1;
        }

        List<Integer> newTbl = new ArrayList<Integer>();
        newTbl.add(tp);
        newTbl.add(fp);
        newTbl.add(fn);
        newTbl.add(tn);

        return newTbl;

    }

    private List<Integer> createTableOfConfusion4Entity_AnyOccr(String serviceName, String evalClass,
            JsonArray truthJA, JsonArray recgJA, List<Integer> crntTable) {
        // truthClass: actual truth class, recgClass: predicted class
        // For Entity: evalClass is the entity type, i.e. the entity name
        //             truthJA and recgJA are for one utterance, may contains more entity-values.

        int tp = 0; // True Positives
        int fp = 0; // False Positives
        int fn = 0; // False Negatives
        int tn = 0; // True Negatives
        if (!crntTable.isEmpty()) {
            tp = crntTable.get(0);
            fp = crntTable.get(1);
            fn = crntTable.get(2);
            tn = crntTable.get(3);
        }
        // for entity evalClass, it only consider the entity type i.e. we called Entity,
        // not the values which are not possible for our cases as they may many different
        // values for each Entity.
        if (hasEntity(truthJA, evalClass)) {
            // for comparing entity True Positive could consider EntityType and values
            // or just EntityType for loose comparison
            if (hasEntityInBoth(serviceName, truthJA, recgJA, evalClass)) { // both has evalEntity and values are same
                tp += 1;
            } else {
                fn += 1;
            }
        } else if (hasEntity(recgJA, evalClass)) {
            fp += 1;
        } else {
            tn += 1;
        }

        List<Integer> newTbl = new ArrayList<Integer>();
        newTbl.add(tp);
        newTbl.add(fp);
        newTbl.add(fn);
        newTbl.add(tn);

        return newTbl;

    }

    private List<Integer> createTableOfConfusion4Entity_MulOccr(String serviceName, String evalClass,
            JsonArray truthJA, JsonArray recgJA, List<Integer> crntTable) {
        // truthClass: actual truth class, recgClass: predicted class
        // For Entity: evalClass is the entity type, i.e. the entity name
        //             truthJA and recgJA are for one utterance, may contains more entity-values.

        int tp = 0; // True Positives
        int fp = 0; // False Positives
        int fn = 0; // False Negatives
        int tn = 0; // True Negatives
        if (!crntTable.isEmpty()) {
            tp = crntTable.get(0);
            fp = crntTable.get(1);
            fn = crntTable.get(2);
            tn = crntTable.get(3);
        }
        // for entity evalClass, it only consider the entity type i.e. we called Entity,
        // not the values which are not possible for our cases as they may many different
        // values for each Entity.
        if (hasEntity(truthJA, evalClass)) {
            // for comparing entity True Positive could consider EntityType and values
            // or just EntityType for loose comparison
            // now consider type and partial tokesn match, and more occurances of same entity type
            List<Integer> entMatch = entityPartialTokensMatch_MulOccr(serviceName, truthJA, recgJA,evalClass);
            //System.out.println("entMatch:" + evalClass + ": " +  entMatch.get(0) + "--" +  entMatch.get(1) +"--" +  entMatch.get(2));
            // entMatch has tp, fp, fn
            tp += entMatch.get(0).intValue();
            fp += entMatch.get(1).intValue();
            fn += entMatch.get(2).intValue();
            
        } else if (hasEntity(recgJA, evalClass)) {
            int occr = getEntityOccrNum(evalClass, recgJA); // evalClass is the entity type name
            fp += occr;
        } else {
            tn += 1;
        }

        List<Integer> newTbl = new ArrayList<Integer>();
        newTbl.add(tp);
        newTbl.add(fp);
        newTbl.add(fn);
        newTbl.add(tn);

        return newTbl;

    }

    // check if entity JA contains the specified entity name
    private boolean hasEntity(JsonArray entitiesJA, String entityName) {
        // No Need to consider values here, because:
        // When considering value, the base value should be truth value which should
        // come from truthJA, so:
        //    1. when calling with truthJA as param, the value is from itself, so no need compare.
        //    2. the time to call with recgJA as param is only when truthJA not containing entityName
        //       so value here doesn't matter either since it is not in the truth

        for (int i = 0; i < entitiesJA.size(); ++i) {
            JsonObject itemJO = entitiesJA.get(i).getAsJsonObject();

            // to lowerCase to make sure to equalIgnoreCase
            String entity = itemJO.get("entity").getAsString().toLowerCase().trim();
            if (entity.startsWith("e_")) { // grammar generated data entity starting with "e_"
                entity = entity.substring(2);
            }            
            //String value = itemJO.get("value").getAsString().toLowerCase().trim();
            if (entity.equalsIgnoreCase(entityName)) {
                return true;
            }
        }

        return false;

    }

    // check if both entitiesJA have the specified entity name
    // entitiesJA1 is for the truth entitiesJA.
    private boolean hasEntityInBoth(String serviceName, JsonArray entitiesJA1, JsonArray entitiesJA2, String entityName) {
        String entity1 = "";
        String value1 = "";

        for (int i = 0; i < entitiesJA1.size(); ++i) {
            JsonObject itemJO = entitiesJA1.get(i).getAsJsonObject();

            // to lowerCase to make sure to equalIgnoreCase
            entity1 = itemJO.get("entity").getAsString().toLowerCase().trim();
            if (entity1.startsWith("e_")) { // grammar generated data entity starting with "e_"
                entity1 = entity1.substring(2);
            }
            
            value1 = itemJO.get("value").getAsString().toLowerCase().trim();
            //System.out.println("final entity1:" + entity1 + ", value1:" + value1);
            if (entity1.equalsIgnoreCase(entityName)) {
                break;
            }
        }

        if (entity1.equals("")) {
            return false; // truth entitiesJA1 does not have the specified one
        }

        String entity2 = "";
        String value2 = "";

        for (int i = 0; i < entitiesJA2.size(); ++i) {
            JsonObject itemJO = entitiesJA2.get(i).getAsJsonObject();
            // to lowerCase to make sure to equalIgnoreCase
            entity2 = itemJO.get("entity").getAsString().toLowerCase().trim();
            if (entity2.startsWith("e_")) { // grammar generated data entity starting with "e_"
                entity2 = entity2.substring(2);
            }            
            value2 = itemJO.get("value").getAsString().toLowerCase().trim();
            if (entity2.equalsIgnoreCase(entityName)) {
                break; // A bug here. If there are other entities in the utt, it will not be entity2.equals("")
            }
        }

        if (entity2.equals("")) { // A bug, see above when there are other entities in the utt.
            return false; // entitiesJA2 does not have the specified one
        }

        // now both entity name are same. For loose check, just return true
        // for strick check, check values in addition
        //if (value1.equals(value2)) { // this is before 30 July 2018
        if (entityPartialTokensMatch_AnyOccr(serviceName, entitiesJA1,entitiesJA2, entityName)) { // this is new on 30 July 2018
            // both type match and partial tokens match
            return true;
        } else {
            return false;
        }
              
    }

    private boolean checkTokensOverlapBySubstring(String str1, String str2) {
        if(str1.length() > str2.length()) {
           if(str1.indexOf(str2) >= 0)
              return true;
           else 
               return false;
        } else {
           if(str2.indexOf(str1) >= 0)
              return true;
           else 
               return false;
        }
    }
    
    private boolean checkTokensOverlapBySubstring(JsonArray entitiesJA1, JsonArray entitiesJA2, String entityName) {
        for (int i = 0; i < entitiesJA1.size(); ++i) { // multiple same entities in Golden
            JsonObject itemJO = entitiesJA1.get(i).getAsJsonObject();
            String entity = itemJO.get("entity").getAsString().toLowerCase().trim();
            if (!entity.equalsIgnoreCase(entityName)) continue;
            
            String gldValue = itemJO.get("value").getAsString();
            for (int j = 0; j < entitiesJA2.size(); ++j) {
                // entitiesJA2 is for testResults and extracted from each service
                JsonObject itemJOUtt = entitiesJA2.get(j).getAsJsonObject();
                String uttEntity = itemJOUtt.get("entity").getAsString().toLowerCase().trim();
                if(!uttEntity.equalsIgnoreCase(entityName)) continue;
                String uttValue = itemJOUtt.get("value").getAsString();
                if(checkTokensOverlapBySubstring(gldValue, uttValue) )
                    return true;
            }
        }
        return false;
        
    }
    
    /**
     * Check Entity type match and values/tokens partial match.
     * 
     * 
     * 
     * @param entitiesJA1
     * @param entitiesJA2
     * @param entityName
     * @return 
     */
    private boolean entityPartialTokensMatch_AnyOccr(String serviceName, JsonArray entitiesJA1, JsonArray entitiesJA2, String entityName) {
    
        if(serviceName.equals(IntEntUtils.ServiceName_APIAI)) {
            return checkTokensOverlapBySubstring(entitiesJA1,entitiesJA2,entityName);
        }
        // Entity in Golden utt, check if index value overlap in Rater's utt tokens.
        // same entity name may occurs multiple times in one utterance, 
        // if any occurrance matching, considering they are matching.
        List<Pair<Integer, Integer>> goldenEnts = getEntityIndex_TypePlusTokens(entityName, entitiesJA1); 
        for (int gi = 0; gi < goldenEnts.size(); ++gi) {

            int start1 = goldenEnts.get(gi).getElement0().intValue();
            int end1 = goldenEnts.get(gi).getElement1().intValue();
            for (int i = 0; i < entitiesJA2.size(); ++i) {
                // entitiesJA2 is for testResults and extracted from each service
                JsonObject itemJO = entitiesJA2.get(i).getAsJsonObject();
                String uttEntity = itemJO.get("entity").getAsString().toLowerCase().trim();
                Integer[] ind2 = getValueIndexOfOneEvalEntity(serviceName,itemJO);
                //String entity = itemJO.get("entity").getAsString().toLowerCase().trim();
                // ensured it is not null since APIAI is checked above.
                int start2 = ind2[0].intValue();
                int end2 = ind2[1].intValue();
                if ((start1 >= start2 && start1 <= end2) || (end1 >= start2 && end1 <= end2)) {
                    // overlap
                    if(uttEntity.equalsIgnoreCase(entityName))  {// entityType and partial token match
                        System.out.println("Overlap And Entity Is Same, Matching!: " + entityName + ". And entity type is the same.");
                        return true;
                    } else {
                        System.out.println("Overlap But Entity Not Same, Not Matching!: " + entityName + ", start="
                            + start2 + ", end=" + end2);
                        
                    }
                }
            } 
        // then check if next golden type matches any of the uttEntities for both type and partial tokens
        } // for next golden entity
        return false;
        
    }
    
    /**
     * Check Entity type match and values/tokens partial match.
     * 
     * 
     * 
     * @param entitiesJA1
     * @param entitiesJA2
     * @param entityName
     * @return 
     */
    private List<Integer> entityPartialTokensMatch_MulOccr(String serviceName, JsonArray entitiesJA1, JsonArray entitiesJA2, String entityName) {
    
        // Entity in Golden utt, check if index value overlap in Rater's utt tokens.
        // same entity name may occurs multiple times in one utterance, 
        // if any occurrance matching, considering they are matching.
        //List<Pair<Integer, Integer>> goldenEnts = getEntityIndex_TypePlusTokens(entityName, entitiesJA1); 
        int tp = 0;
        int fp = 0;
        int fn = 0;
        for (int j = 0; j < entitiesJA1.size(); ++j) {
            JsonObject itemJOGld = entitiesJA1.get(j).getAsJsonObject();
            String entityGld = itemJOGld.get("entity").getAsString().trim();
            
            if (!entityGld.equalsIgnoreCase(entityName)) continue;
            String valueGld = itemJOGld.get("value").getAsString().trim();
            
            int start1 = itemJOGld.get("start").getAsInt();
            int end1 = itemJOGld.get("end").getAsInt();
            
            boolean isTP = false;
            for (int i = 0; i < entitiesJA2.size(); ++i) {
                // entitiesJA2 is for testResults and extracted from each service
                JsonObject itemJO = entitiesJA2.get(i).getAsJsonObject();
                String uttEntity = itemJO.get("entity").getAsString().trim();
                String uttValue = itemJO.get("value").getAsString().trim();
                
                if(serviceName.equals(IntEntUtils.ServiceName_APIAI)) {
                    // Apiai does not have token start/end index
                    if(uttEntity.equalsIgnoreCase(entityName) && 
                        checkTokensOverlapBySubstring(valueGld, uttValue) ) {
                        tp++;
                        isTP = true;
                        // "break" to not count second occr as TP, 
                        // e.g. Golden: p_name="john and bill", pred1: p_name="john" pred2: p_name="bill"
                        break;
                    }
                } else {
                    Integer[] ind2 = getValueIndexOfOneEvalEntity(serviceName,itemJO);
                    int start2 = ind2[0].intValue();
                    int end2 = ind2[1].intValue();
                    if ((start1 >= start2 && start1 <= end2) || (end1 >= start2 && end1 <= end2)) {
                        // overlap
                        if(uttEntity.equalsIgnoreCase(entityName))  {// entityType and partial token match
                            //System.out.println("Overlap And Entity Is Same, Matching!: " + entityName + ". And entity type is the same.");
                            tp++;
                            isTP = true;
                            // "break" to not count second occr as TP, 
                            // e.g. Golden: p_name="john and bill", pred1: p_name="john" pred2: p_name="bill"
                            
                            break;
                            
                        }
                    }
                }
            }
            // None of JA2 match golden value: not predicted it, it is fn
            if(!isTP)
               fn++;
            
        // then check if next golden type matches any of the uttEntities for both type and partial tokens
        } // for next golden entity
        
        // check if predicting more than goldens, then calc fp
        int entOccrNum = getEntityOccrNum(entityName, entitiesJA2);
        
        if(entOccrNum > 0) {
            fp = entOccrNum - tp;
        }
        
        List<Integer> entMatch = new ArrayList();
        entMatch.add(tp);
        entMatch.add(fp);
        entMatch.add(fn);
        
        return entMatch;
        
    }

    private Integer[] getValueIndexOfOneEvalEntity(String serviceName,JsonObject itemJO ) {
        // annoTestset, Rasa and Luis has properties: entity/value/start/end
        // LUIS' "entity": "Hier2::weather_descriptor" has been extracted to the real entity
        //        and added start/end index
        // Apiai does not have properties start/end as it's query result has no info of them
        // Watson has: entity/value, then "location": [19, 27],
        
        
        int start = -1;
        int end = -1;
        if(serviceName.equals(IntEntUtils.ServiceName_APIAI)) {
            // it doesn't have index, use substring to check overlap
            return null;
        } else if(serviceName.equals(IntEntUtils.ServiceName_WATSON)) {
            start = itemJO.getAsJsonArray("location").get(0).getAsInt();
            end = itemJO.getAsJsonArray("location").get(1).getAsInt();
        } else {
            start = itemJO.get("start").getAsInt();
            end = itemJO.get("end").getAsInt();
        }
        Integer[] ind = new Integer[2];
        ind[0] = start;
        ind[1] = end;
        return ind;
    }
    
    // same entity name may occurs multiple times in one utterance.
    private List<Pair<Integer, Integer>> getEntityIndex_TypePlusTokens(String entityName, JsonArray uttEntitiesJA) {
        // already made sure entityName is in the JA
        ArrayList<Pair<Integer, Integer>> s_e = new ArrayList();
        
        for (int i = 0; i < uttEntitiesJA.size(); ++i) {
            JsonObject itemJO = uttEntitiesJA.get(i).getAsJsonObject();
            String entity = itemJO.get("entity").getAsString().toLowerCase().trim();
            if (entity.equalsIgnoreCase(entityName)) {
                int start = itemJO.get("start").getAsInt();
                int end = itemJO.get("end").getAsInt();
                Pair<Integer, Integer> se1 = new Pair(start, end);
                s_e.add(se1);

            }
        }
       return s_e;

    }
    
    // same entity name may occurs multiple times in one utterance.
    private int getEntityOccrNum(String entityName, JsonArray uttEntitiesJA) {

        int num =0;       
        for (int i = 0; i < uttEntitiesJA.size(); ++i) {
            JsonObject itemJO = uttEntitiesJA.get(i).getAsJsonObject();
            String entity = itemJO.get("entity").getAsString().toLowerCase().trim();
            if (entity.equalsIgnoreCase(entityName)) {
                num++;
            }
        }
       return num;

    }

    private boolean containedInRankings(String annoIntent, List<String> intentRankingNames,
            int numRankingCheck) {
        int numCheck = numRankingCheck;
        if (numCheck > intentRankingNames.size()) {
            numCheck = intentRankingNames.size();
        }
        for (int i = 0; i < numCheck; ++i) {
            if (annoIntent.equalsIgnoreCase(intentRankingNames.get(i))) {
                return true;
            }
        }
        return false;
    }

    private String getQueryUtterance_Rasa(JsonObject itemJO) {
        String utt = itemJO.get("text").getAsString();
        return utt;
    }

    private String getRecognisedIntent_Rasa_Stable(JsonObject itemJO) {
        String intent = itemJO.get("intent").getAsString();
        return intent;
    }

    private String getRecognisedIntent_Rasa_Latest(JsonObject itemJO) {

        if (!itemJO.get("intent").isJsonObject() || itemJO.get("intent").isJsonNull()) {
            return null;
        }

        JsonObject intentJO = itemJO.get("intent").getAsJsonObject();
        String intent = intentJO.get("name").getAsString();
        
        return intent;
        
    }

    private List<List<String>> getIntentRanking_Rasa_Latest(JsonObject itemJO) {

        JsonArray rankingJA = itemJO.getAsJsonArray("intent_ranking");
        if (rankingJA == null) {
            return null; // for entity-only trained models, there is no intents.
        }
        List<String> intNames = new ArrayList();
        List<String> intConfs = new ArrayList();
        for (int i = 0; i < rankingJA.size(); ++i) {
            JsonObject rankJO = (JsonObject) rankingJA.get(i);
            String intName = rankJO.get("name").getAsString();
            String conf = rankJO.get("confidence").getAsString();
            //double dconf= Double.parseDouble(conf);
            intNames.add(intName);
            intConfs.add(conf);
        }
        List<List<String>> intRankings = new ArrayList();
        intRankings.add(intNames);
        intRankings.add(intConfs);

        return intRankings;
    }

    private String getQueryUtterance_Apiai(JsonObject itemJO) {
        JsonObject resJO = itemJO.get("result").getAsJsonObject();
        String utt = resJO.get("resolvedQuery").getAsString();
        return utt;
    }


    private String getQueryUtterance_Luis(JsonObject itemJO) {
        String utt = itemJO.get("text").getAsString();
        return utt;
    }

    private String getQueryUtterance_Watson(JsonObject itemJO) {
        String utt = ((JsonObject)itemJO.get("input")).get("text").getAsString();
        return utt;
    }
    
    private String getRecognisedIntent_Apiai(JsonObject itemJO) {
        String recgIntent = "";
        JsonObject resJO = itemJO.get("result").getAsJsonObject();
        if (resJO.get("metadata") == null) {
            return recgIntent;
        }

        JsonObject metaJO = resJO.get("metadata").getAsJsonObject();

        if (metaJO.get("intentName") != null) {
            recgIntent = metaJO.get("intentName").getAsString();
        }
        System.out.println("recgIntent:" + recgIntent);
        return recgIntent;
    }


    private String getRecognisedIntent_Luis(JsonObject itemJO) {
        String intent = itemJO.get("intent").getAsString();
        return intent;
    }

    private String getRecognisedIntent_Watson(JsonObject itemJO) {
        JsonArray intents = itemJO.get("intents").getAsJsonArray();
        String intent = "";
        // occassionally Watson intents array is empty
        if(intents.size() > 0) {
            intent = ((JsonObject)intents.get(0)).get("intent").getAsString();
        } else {
           System.out.println("Recognised Watson intents size:" + intents.size());
           JsonObject inputJO = itemJO.get("input").getAsJsonObject();
           String utt = inputJO.get("text").getAsString();
           System.out.println("Utt: " + utt);
        }
    
        return intent;
    }

    private String removeAppendingIndex(String phrase) {
        if(phrase.contains("_")) {
            String idx = phrase.substring(phrase.lastIndexOf("_") + 1).trim();
            if(GeneralUtils.isStrNumeric(idx)) {
                phrase = phrase.substring(0, phrase.lastIndexOf("_")).trim();
            }
        }
        return phrase;
    }
    
    private JsonArray getRecognisedEntities_Apiai(JsonObject itemJO) {
        // entities are in parameters
        JsonArray recgEntitiesJA = new JsonArray();
        JsonObject resJO = itemJO.get("result").getAsJsonObject();
        if (resJO.get("parameters") == null) {
            return recgEntitiesJA;
        }

        JsonObject paramJO = resJO.get("parameters").getAsJsonObject();
        // regEntNum: number of entities recognised in one utterance
        //int numEntRecg = 0;

        for (Map.Entry<String, JsonElement> entry : paramJO.entrySet()) {
            String entity = entry.getKey();
            String value = entry.getValue().getAsString();
            if (value != null && !value.isEmpty()) {
                JsonObject recgEntJO = new JsonObject();
                recgEntJO.addProperty("entity", entity);
                recgEntJO.addProperty("value", value);
                recgEntitiesJA.add(recgEntJO);
                System.out.println("Recg entity:" + entity + ", Recg value:" + value);
            }
        }
        return recgEntitiesJA;
    }

    private JsonArray getRecognisedEntities_Rasa(JsonObject itemJO) {
        JsonArray entitiesJA = itemJO.get("entities").getAsJsonArray();
        return entitiesJA;

    }

    private JsonArray getRecognisedEntities_Watson(JsonObject itemJO) {
        JsonArray entitiesJA = itemJO.get("entities").getAsJsonArray();
        // contains extra "confidence" and "location" items.
        // could just extract entity and value info but doesn't matter
        return entitiesJA;

    }

    private JsonArray getRecognisedEntities_Luis(JsonObject itemJO) {
        JsonArray entitiesJA = itemJO.get("entities").getAsJsonArray();
        // LUIS entities contain: "entity": "Hier2::slot_pricerange", 

        JsonArray recgEntitiesJA = new JsonArray();
        for (int i = 0; i < entitiesJA.size(); ++i) {
            JsonObject entJO = entitiesJA.get(i).getAsJsonObject();

            String entity = entJO.get("entity").getAsString(); // e.g. "Hier2::slot_pricerange"
            String value = entJO.get("value").getAsString(); // as standard
            if (entity.contains("::")) {
                entity = entity.substring(entity.indexOf("::") + 2);
                System.out.println("\n === got: Luis real entity:" + entity);
            } else {
                System.out.println("\n ==== ERROR: Should be Hierarchical entity but not found! ==== \n");
                System.exit(-1);
            }
            if (entity.startsWith("e_")) { // grammar generated data entity starting with "e_"
                entity = entity.substring(2);
            }
            
            if (value != null && !value.isEmpty()) {
                JsonObject recgEntJO = new JsonObject();
                int start = entJO.get("start").getAsInt();
                int end = entJO.get("end").getAsInt();
                double score = entJO.get("score").getAsDouble();
                
                recgEntJO.addProperty("entity", entity);
                recgEntJO.addProperty("value", value);
                recgEntJO.addProperty("start", start);
                recgEntJO.addProperty("end", end);
                recgEntJO.addProperty("score", score);
                
                recgEntitiesJA.add(recgEntJO);
                System.out.println("Recg entity:" + entity + ", Recg value:" + value);

            }
        }
        return recgEntitiesJA;

    }

    private Map<String, List<String>> getEntityValuesFromResults(JsonArray entitiesJA) {

        Map<String, List<String>> entValsMap = new LinkedHashMap<>();

        for (int i = 0; i < entitiesJA.size(); ++i) {
            JsonObject entJO = entitiesJA.get(i).getAsJsonObject();

            String entity = entJO.get("entity").getAsString();
            if (entity.startsWith("e_")) { // grammar generated data entity starting with "e_"
                entity = entity.substring(2);
            }            
            String value = entJO.get("value").getAsString();
            String entVal = value;
            List<String> valList = entValsMap.get(entity);
            if (valList == null) {
                valList = new ArrayList<>();
                entValsMap.put(entity, valList);
            }

            if (!valList.contains(entVal));
            valList.add(entVal);

        }
        return entValsMap;

    }

    private JsonObject getAnnotatedDomainData(String domainDir) {
        String fileUri = domainDir + "/" + GeneralUtils.getFirstFileName(domainDir);
        JsonObject jo = GeneralUtils.readJsonFile(fileUri);
        return jo;
    }

        
    private int evaluateEntities(JsonArray definedEntities, JsonArray identifiedEntities, String serviceName) {
        // return number of correctly recognised entities
        Set<Pair> identEnt = new HashSet<>();
        for (int i = 0; i < identifiedEntities.size(); ++i) {
            JsonObject itemJO = identifiedEntities.get(i).getAsJsonObject();
            // to lowerCase to make sure to equalIgnoreCase
            String entity = itemJO.get("entity").getAsString().toLowerCase().trim();
            if (entity.startsWith("e_")) { // grammar generated data entity starting with "e_"
                entity = entity.substring(2);
            }

            String value = itemJO.get("value").getAsString().toLowerCase().trim();
            //System.out.println("final entity:" + entity + ", value:" + value);
            identEnt.add(new Pair(entity, value));
        }

        Set<Pair> defEnt = new HashSet<>();
        for (int i = 0; i < definedEntities.size(); ++i) {
            JsonObject itemJO = definedEntities.get(i).getAsJsonObject();
            // to lowerCase to make sure to equalIgnoreCase
            String entity = itemJO.get("entity").getAsString().toLowerCase().trim();
            if (entity.startsWith("e_")) { // grammar generated data entity starting with "e_"
                entity = entity.substring(2);
            }

            String value = itemJO.get("value").getAsString().toLowerCase().trim();
            defEnt.add(new Pair(entity, value));
        }
        // Pair's equal method compare both element0 and element1, i.e. key and value
        Set<Pair> intersect = IntEntUtils.intersectionPair(defEnt, identEnt);

        return intersect.size();

    }

    public void loadConfig() {

        try {
            File file = new File(CONFIG_FILE);
            FileInputStream fileInput = new FileInputStream(file);
            Properties properties = new Properties();
            properties.load(fileInput);
            fileInput.close();

            destRootDirTop = properties.getProperty("destRootDirTop");

            String s = properties.getProperty("RasaLatestVersion");
            RasaLatestVersion = true;
            if (s.toLowerCase().equals("false")) {
                RasaLatestVersion = false;
            }

            RasaPipeline = properties.getProperty("RasaPipeline");
            
            String sRank = properties.getProperty("numRankingCheck");
            numRankingCheck = Integer.parseInt(sRank);

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
    public static void main(String[] args) throws IOException {
        new Evaluation();
    }

}
