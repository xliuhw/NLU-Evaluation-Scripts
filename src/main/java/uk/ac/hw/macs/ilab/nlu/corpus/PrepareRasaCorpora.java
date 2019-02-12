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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import static uk.ac.hw.macs.ilab.nlu.corpus.PreprocessRealData.CONFIG_FILE;
import uk.ac.hw.macs.ilab.nlu.util.GeneralUtils;

/**
 * To generate RASA NLU input data corpora for training the RASA NLU.
 *
 *
 * Versions: -- 09/11/2017, changed RASA Config generator for latest RASA 0.10.4
 *
 * @author X.Liu, V.Rieser
 */
public class PrepareRasaCorpora {

    Map<String, List<String>> featureWords;
    
    // projectName -- ForTrainedModel: Defines a project name to train new models for 
    //                                 and to refer to when using the http server. 
   
    String projectName = "CsvNormFinalised";
    // useTrainset now is handled in PreprocessRealData by trainsetPercent and testsetPercent
    //boolean useTrainset = true; // Trainset 80%, Fullset 100% of subset for training--normall for deployment
    String destRootDirTop = ".";
    String regexFeatureFile = "resources/feature-words/feature-words.txt";
    String trainsetPercent = "Null"; //used for file label
    
    int numKFold = 0; // 0: do not create data for cross validation. Overwritten by config file
    
    public PrepareRasaCorpora() {
        
        loadConfig();
        
        System.out.println("==== PrepareRasaCorpora ==== ");
        System.out.println("destRootDirTop = " + destRootDirTop);
        System.out.println("projectName = " + projectName);
        System.out.println("trainsetPercent = " + trainsetPercent);
        System.out.println("regexFeatureFile = " + regexFeatureFile); 
        System.out.println("====  Starting process ........");

        if(regexFeatureFile != null && !regexFeatureFile.equals(""))
            loadFeatureWords(regexFeatureFile);
        
        DoProcessing();
        
        //mergeTestset_Manually();
    }
    
    private void mergeTestset_Manually() {
        
        String srcRoot = "/home/ilabh4m5/projects/emotech/Dialogue/CodesJava/PrepareDataForRasaNLUV2/preprocessResults";
        String testSub = "autoGeneFromRealAnno/autoGene_2017_08_21-15_48_01_873/DomainIntentNoDupShuffleSubset_Test/annotated";

        String destSub = "out4RasaReal/rasa_2017_08_21-15_48_01_873_trainset";
        String timeStamp = "2017_08_21-15_48_01_873";
        String srcTestDir = srcRoot+"/" + testSub;
        
        String destDir = srcRoot + "/" + destSub + "/mergedTestset";
        String savedFile =  mergeAllRasaJsonFilesTestset(srcTestDir, destDir, timeStamp) ;
        
        System.out.println("Saved file:" + savedFile);
        
    }
    
    private void DoProcessing() {

        // Step1:
        String nameInfoFileDir = destRootDirTop + "/allGeneFileNames";
        String nameInfoFile = "allGeneFileNames.txt";
        String infoFile = nameInfoFileDir + "/" + nameInfoFile;
        System.out.println("Get info from saved file:" + infoFile);
        String flagLine = "=A_TASK_START=";
        Map<String, String> infoMap = IntEntUtils.getPreprocessedInfo(infoFile, flagLine);
        String timeStamp = infoMap.get("timeStamp");
        // e.g. ./preprocessResults/autoGeneFromRealAnno/autoGene_2018_03_13-11_32_44_015
        String geneBaseDir = infoMap.get("preprocessOutBaseDir");
      
        // trainsetDir will be _Sub4Subset, i.e. full set train, when trainPercent=100
        //String srcDir = infoMap.get("preprocessOutDir_Sub5Train"); 
        String trainsetDir = infoMap.get("preprocessOutDir_Sub5Train"); 
        String fileNameLabel = trainsetPercent + "Train";

        System.out.println("Prepare data: rasa_" + timeStamp + "_" + fileNameLabel);
        String trainsetBaseDir = geneBaseDir + "/" + trainsetDir;
        String destBaseDir = destRootDirTop + "/preprocessResults/out4RasaReal/rasa_json_" + timeStamp + "_" + fileNameLabel;
       
        String testsetAnnoDir = infoMap.get("testSetAnnotated");        

        List<String> nameInfo = processForOneTrainset(trainsetBaseDir, destBaseDir, 
                timeStamp, testsetAnnoDir, fileNameLabel, numKFold );

        // true for appending. null for using default encoding.
        GeneralUtils.saveToTextFile(nameInfoFileDir, nameInfoFile, nameInfo, true, null);
        
        System.out.println("Updated name info file: " + nameInfoFile);
        
        if(numKFold != 0) {
            String srcTaskBaseDir = geneBaseDir;
            String destTaskBaseDir = destBaseDir;
            
            nameInfo = processForCrossValidation(srcTaskBaseDir, destTaskBaseDir,
            timeStamp);
        }
        
    }
    
    private  List<String> processForOneTrainset(String trainsetBaseDir, String destBaseDir, 
            String timeStamp, String testsetAnnoDir, String fileNameLabel, int numKFold ) {
        
        // e.g. trainsetBaseDir: geneBaseDir + DomainIntentNoDupShuffleSubset_Train
        // destBaseDir0: preprocessResults/out4RasaReal/rasa_json_" + timeStamp + "_" + fileNameLabel;
        // For Normal: destBaseDir = destBaseDir0
        // For cross-validation destBaseDir = destBaseDir0+CrossValidation/KFold_i/
        String destDir = destBaseDir + "/separated";
        createRasaSeparatedDomainData_All_RealAnno(trainsetBaseDir, destDir);

        // Step2: to merge all domain data
        System.out.println("Merge RASA JSON files ........");
        String mergeSrcDir = destDir;
        String outPath = destBaseDir + "/merged";
        //String fileLabel = fileNameLabel;
        
        JsonArray regexJA = null;
        if(featureWords != null && !featureWords.isEmpty())
            regexJA = generateRegexFeatures();
                
        String mergedFile = mergeAllRasaJsonFiles(mergeSrcDir, outPath, fileNameLabel, 
                timeStamp, regexJA);

        // Step3: to check entities overlap
        System.out.println("\n Do validation ........");
        checkEntityOverlap(mergedFile);
        
        // merge testset annotated files
       // String testsetAnnoDir = infoMap.get("testSetAnnotated");
        String testsetMergeDir = destBaseDir + "/mergedTestset";
        // if do not add regex_features, just pass the param with null
        String mergedTestsetFile = mergeAllRasaJsonFilesTestset(testsetAnnoDir, testsetMergeDir, 
                timeStamp);

        // Step4: genereate RASA train config file
        // avail pipeline name: spacy_sklearn, mitie, mitie_sklearn
        System.out.println("Generate RASA config files ........");
        String rasaPipelineName1 = "spacy_sklearn"; // use ner_crf and sklearn intent recog.
        String confPrefix = "configLatest_" + fileNameLabel + "_" + rasaPipelineName1;
        String trainConfigDir = destBaseDir + "/rasaTrainConfig";
        String trainConfigFile1 = confPrefix + "_" + timeStamp + ".json";
        List<String> configList = generateRasaTrainConfigFile(timeStamp, mergedFile, fileNameLabel, rasaPipelineName1);
        GeneralUtils.saveToTextFile(trainConfigDir, trainConfigFile1, configList, false, null);

        String rasaPipelineName2 = "mitie"; // use mitie for entity and intent recog
        trainConfigDir = destBaseDir + "/rasaTrainConfig";
        confPrefix = "configLatest_" + fileNameLabel + "_" + rasaPipelineName2;
        String trainConfigFile2 = confPrefix + "_" + timeStamp + ".json";
        configList = generateRasaTrainConfigFile(timeStamp, mergedFile, fileNameLabel, rasaPipelineName2);
        GeneralUtils.saveToTextFile(trainConfigDir, trainConfigFile2, configList, false, null);

        System.out.println("All data are saved in " + destBaseDir);
        
        List<String> nameInfo = new ArrayList<>();
        if( numKFold == 0) { // for nomal process---Non CrossValidation
            String item = "prepareRasaOutDir=" + destBaseDir;
            nameInfo.add("");
            nameInfo.add("# ---- written by PrepareRasaCorpora.java ----");
            nameInfo.add("currentTime = " + GeneralUtils.getTimeStamp4Logging());
            nameInfo.add(item);

            nameInfo.add("projectNameInConfigFile = " + this.projectName);
            nameInfo.add(item);

            item = "prepareRasaMergedFile=" + mergedFile;
            nameInfo.add(item);

            item = "prepareRasaMergedTestsetFile=" + mergedTestsetFile;
            nameInfo.add(item);

            item = "rasaTrainConfigDir=" + trainConfigDir;
            nameInfo.add(item);

            item = "rasaTrainConfigFile_" + rasaPipelineName1 + "=" + trainConfigFile1;
            nameInfo.add(item);

            item = "rasaTrainConfigFile_" + rasaPipelineName2 + "=" + trainConfigFile2;
            nameInfo.add(item);

            nameInfo.add("");

            item = "rasaActuallyUsedTrainset=" + fileNameLabel;
            nameInfo.add(item);

            item = "rasaProjectName=" + projectName;
            nameInfo.add(item);

            item = "rasaDestDirRootTop=" + destRootDirTop;
            nameInfo.add(item);

            nameInfo.add("");
            
        } else { // Cross Validation
            // TODO
        }
        
        return nameInfo;
        
    }
    
    private  List<String> processForCrossValidation(String srcTaskBaseDir, String destTaskBaseDir,
            String timeStamp) {
        // taskBaseDir corresponds to each time generating data with unique timeStamp. e.g. 
        // srcTaskBaseDir: preprocessResults/autoGeneFromRealAnno_autoGene_timestamp/
        // destTaskBaseDir: preprocessResults/out4XXXReal/XXX-timestamp/, XXX is ServiceName: Rasa etc.
        String srcCvBaseDir = srcTaskBaseDir + "/CrossValidation";
        String destCvBaseDir = destTaskBaseDir + "/CrossValidation";
        
        List<String> kFoldDirs = GeneralUtils.getAllSubdirNames(srcCvBaseDir);// all KFold_i
        for(int i = 0; i < kFoldDirs.size(); ++i) {
            String kFoldName = kFoldDirs.get(i);
            String srciFoldDir = srcCvBaseDir + "/" + kFoldName;
            String trainsetBaseDir = srciFoldDir + "/trainset";
            String testsetBaseDir = srciFoldDir + "/testset";
            String destBaseDir = destCvBaseDir + "/"+ kFoldName;
            String fileNameLabel = kFoldName; // KFold_1, KFold_2 etc.
            String testsetAnnoDir = testsetBaseDir + "/annotated";
            
            //For cross-validation destBaseDir = destTaskBaseDir+CrossValidation/KFold_i/
            processForOneTrainset(trainsetBaseDir, destBaseDir, timeStamp,
              testsetAnnoDir, fileNameLabel, 1 );// here last param can be anything > 0
        }
        
        List<String> nameInfo = new ArrayList<>();
        return nameInfo; //TODO add something
    }
    
    private void loadFeatureWords(String fwFileUri) {
       // String fwFileUri = "resources/feature-words/feature-words.txt";
        
        List<String> lines = GeneralUtils.readFromTextFile(fwFileUri);
        //Map<String, List<String>> domainNameValMap = new LinkedHashMap();
        featureWords = new LinkedHashMap();
        for (int i = 0; i < lines.size(); ++i) {
            String line = lines.get(i).trim();
            if(line.endsWith("=")) continue;
            String key = line.substring(0, line.indexOf("=")).trim();
            String values = line.substring(line.indexOf("=") + 1).trim();
            String[] valuesArr = values.split("\\s*,\\s*");
            List<String> valuesList = Arrays.asList(valuesArr);
            List<String> existVals = featureWords.get(key);
            if (existVals == null) {
                existVals = new LinkedList();
                featureWords.put(key, existVals);
            }
            existVals.addAll(valuesList);
        }        
    }

    private JsonArray generateRegexFeatures() {
        // "regex_features": [ { "name": "naxxx", "pattern": "patxxx" },
        // new functionality:
        // "regex_features": [ { "name": "naxxx", "intent": "intxxx", "pattern": "patxxx" },
        JsonArray regJA = new JsonArray();
        for (Map.Entry<String, List<String>> entry : featureWords.entrySet()) {
            String key = entry.getKey();
            key = key + "_key_";
            List<String> words = entry.getValue();
            for(int i = 0; i < words.size(); ++i) {
                String word = words.get(i);
                JsonObject itemJO = new JsonObject();
                String name = key + i;
                itemJO.addProperty("name", name);
                itemJO.addProperty("pattern", word);
                regJA.add(itemJO);
            }
        }
        
        return regJA;
    }
    
    private List<String> generateRasaTrainConfigFile(String timeStamp, String mergedFile,
            String actualTrainset, String pipelineName) {
        // for now, a quick way to generated this file. Ideally to use JsonObject

        // Here is a list of the existing templates, ready to use, created by RASA by default
        // template name -- corresponding pipeline
        // spacy_sklearn	["nlp_spacy", "ner_crf", "ner_synonyms", "intent_featurizer_spacy", "intent_classifier_sklearn"]
        // mitie	["nlp_mitie", "tokenizer_mitie", "ner_mitie", "ner_synonyms", "intent_classifier_mitie"]
        // mitie_sklearn	["nlp_mitie", "tokenizer_mitie", "ner_mitie", "ner_synonyms", "intent_featurizer_mitie", "intent_classifier_sklearn"]
        // keyword	["intent_classifier_keyword"]    
        List<String> lines = new ArrayList<>();
        String line = "{";
        lines.add(line);

        // project: Defines a project name to train new models for and to refer to when using the http server. 
        line = "\"project\"" + ": " + "\"" + this.projectName + "\",";
        lines.add(line);

        // use "name" param to give the trained model name. not use defaul name "model_xxxx-xxxx"
        // time stamp in default name is the training finish time which could also be useful
        // todo: to check if need the finished time.
        // The model will always be saved in the path {project_path}/{project_name}/{model_name}
        String model = "model_" + actualTrainset + "_" + pipelineName + "_" + timeStamp;
        line = "\"fixed_model_name\"" + ": " + "\"" + model + "\",";
        lines.add(line);
        if (pipelineName.equals("spacy_sklearn")) {
            // todo: just use the pipelineName as others below.
            //OLD, "tokenizer_mitie" causes CRF index-out-out-range when training.
            // Now use RASA predefined Pipeline 09/11/2017
            // line = "\"pipeline\": [\"nlp_spacy\", \"intent_featurizer_spacy\", \"tokenizer_mitie\", \"intent_classifier_sklearn\", \"ner_crf\"],";
            line = "\"pipeline\": [\"nlp_spacy\", \"tokenizer_spacy\", \"intent_entity_featurizer_regex\", \"intent_featurizer_spacy\", \"ner_crf\", \"ner_synonyms\",  \"intent_classifier_sklearn\"],";
        } else {
            line = "\"pipeline\":\"" + pipelineName + "\",";
        }

        lines.add(line);
        line = "\"num_threads\": 8,";
        lines.add(line);
        if (pipelineName.equals("mitie") || pipelineName.equals("mitie_sklearn")) {
            line = "\"mitie_file\": \"./MITIE-models/english/total_word_feature_extractor.dat\",";
            lines.add(line);
        }
        line = "\"path\" : \"./trained_models\",";
        lines.add(line);
        line = "\"data\"" + ": " + "\"" + mergedFile + "\"";
        lines.add(line);

        line = "}";
        lines.add(line);

        return lines;

    }

    private void createRasaSeparatedDomainData_All_RealAnno(String srcBaseDir, String destBaseDir) {

        List<String> fileNames = GeneralUtils.getAllFileNames(srcBaseDir);

        System.out.println("Total number of Intents:" + fileNames.size());

        for (int i = 0; i < fileNames.size(); i++) {
            String fname = fileNames.get(i);

            String uttFileUri = srcBaseDir + "/" + fname;
            String intent = fname.substring(0, fname.lastIndexOf("."));
            String domainName = intent;
            createRasaJson_OneDomain_RealAnno(domainName, intent, uttFileUri, destBaseDir);
        }

    }

    private void checkEntityOverlap(String fileURI) {

        JsonObject jo = GeneralUtils.readJsonFile(fileURI);
        JsonObject joRasa = jo.get("rasa_nlu_data").getAsJsonObject();

        boolean overlap = false;
        if (joRasa.get("common_examples") != null) {
            JsonArray jaComm = joRasa.get("common_examples").getAsJsonArray();
            String utt1 = checkEntityArrayOverlap(jaComm);

            if (!utt1.isEmpty()) {
                System.out.println(" ====== Overlap =====");
                System.out.println(utt1);
                System.out.println(" ===== Overlap ======");
                overlap = true;
            }
        }
        JsonArray jaEntity = joRasa.get("entity_examples").getAsJsonArray();
        String utt3 = checkEntityArrayOverlap(jaEntity);
        if (!utt3.isEmpty()) {
            System.out.println(" ====== Overlap =====");
            System.out.println(utt3);
            System.out.println(" ===== Overlap ======");
            overlap = true;
        }

        if (!overlap) {
            System.out.println("\n ====== Validation Success! ===== \n");
        }
    }

    /**
     * Return the string if overlap, otherwise empty string
     *
     * @param examJA examples JsonArray
     * @return string or empty
     */
    private String checkEntityArrayOverlap(JsonArray examJA) {
        for (JsonElement je : examJA) {
            JsonObject obj = je.getAsJsonObject();   // examples element: text, intent, entities.
            JsonArray entJA = obj.get("entities").getAsJsonArray(); // for one utt.

            if (entJA == null || entJA.size() < 2) {
                continue;
            }
            String utt = obj.get("text").getAsString();

            // for more entities, check if their start/end point overlap
            List<Integer> s_e = new LinkedList<>();
            for (JsonElement je1 : entJA) { // for one utt
                JsonObject obj1 = je1.getAsJsonObject();
                int start = obj1.get("start").getAsInt();
                int end = obj1.get("end").getAsInt();
                if (start < 0 || end > utt.length()) { // if last char for end, end=utt.length()
                    // something wrong
                    String msg = "Wrong Index: start: " + start + ", end:" + end;
                    return msg + "\n" + utt;
                }
                s_e.add(start);
                s_e.add(end);
            }

            // check now
            for (int i = 0; i < s_e.size() - 1; i++) {
                int s = s_e.get(i).intValue();
                int e = s_e.get(i + 1).intValue();
                if (e <= s) {
                    //return entJA.toString();
                    return utt;
                } // overlap
            }
        }
        return "";

    }

    public String mergeAllRasaJsonFilesTestset(String fileDir, String outPath,
            String timeStamp) {

        File folder = new File(fileDir);
        File[] listOfFiles = folder.listFiles();
        //System.out.println("total num:" + listOfFiles.length);

        List<String> fullFiles = new ArrayList<>();

        for (int i = 0; i < listOfFiles.length; i++) {
            String fname = listOfFiles[i].getName();
            String fileUri = "";
            if (listOfFiles[i].isFile()) {
                continue;
            }
            String fileUri1 = fileDir + "/" + fname; // fname is directory name
            File folder1 = new File(fileUri1);
            File[] listOfFiles1 = folder1.listFiles();
            fileUri = fileUri1 + "/" + listOfFiles1[0].getName();

            fullFiles.add(fileUri);
        }

        JsonObject jo1 = GeneralUtils.readJsonFile(fullFiles.get(0));
        JsonArray mergedJA = jo1.getAsJsonArray("test_data_annotation");

        for (int i = 1; i < fullFiles.size(); i++) {
            //System.out.println("A Rasa Json file:" + fullFiles.get(i));
            JsonObject jo2 = GeneralUtils.readJsonFile(fullFiles.get(i));
            JsonArray ja2 = jo2.getAsJsonArray("test_data_annotation");

            mergedJA.addAll(ja2);
        }

        JsonObject exampJO = new JsonObject();

        exampJO.add("common_examples", mergedJA);

        exampJO.add("entity_examples", new JsonArray());
        exampJO.add("intent_examples", new JsonArray());

        JsonObject rasaDataJO = new JsonObject();
        rasaDataJO.add("rasa_nlu_data", exampJO);

        String jaStr = rasaDataJO.toString();

        //
        // true -- with timestamp on file name
        String filePrefix = "RasaNluTestset_Merged";
        if (timeStamp != null) {
            filePrefix += "_" + timeStamp;
        }
        // use provided timeStamp to the file name for consistence 
        // now last param is false to not use new time stamp in the save method.
        String savedFile = GeneralUtils.saveJsonStrToFile(filePrefix, outPath, jaStr, false);

        return savedFile;
    }

    /**
     * Return saved merged file URI.
     *
     * @param fileDir file directory contains separated json files.
     *
     * @return String of saved file URI.
     */
    public String mergeAllRasaJsonFiles(String fileDir, String outPath, String destFileLabel,
            String timeStamp, JsonArray regexJA) {

        File folder = new File(fileDir);
        File[] listOfFiles = folder.listFiles();
        //System.out.println("total num:" + listOfFiles.length);

        List<String> fullFiles = new ArrayList<>();

        for (int i = 0; i < listOfFiles.length; i++) {
            String fname = listOfFiles[i].getName();
            //if (listOfFiles[i].isFile()) {
            String fileUri = "";
            if (listOfFiles[i].isDirectory()) {
                continue;
            }
            fileUri = fileDir + "/" + fname;

            fullFiles.add(fileUri);
        }

        JsonObject jo1 = GeneralUtils.readJsonFile(fullFiles.get(0));
        jo1 = jo1.get("rasa_nlu_data").getAsJsonObject();
                
        for (int i = 1; i < fullFiles.size(); i++) {
            //System.out.println("A Rasa Json file:" + fullFiles.get(i));
            JsonObject jo2 = GeneralUtils.readJsonFile(fullFiles.get(i));
            jo2 = jo2.get("rasa_nlu_data").getAsJsonObject();
            jo1 = mergeTwoRasaJsonObjs(jo1, jo2);
        }

        if(regexJA != null ) {
           jo1.add("regex_features", regexJA);
        }
       
        JsonObject rasaDataJO = new JsonObject();
        rasaDataJO.add("rasa_nlu_data", jo1);

        String jaStr = rasaDataJO.toString();
        //
        // true -- with timestamp on file name
        String filePrefix = "RasaNlu_" + "Merged_" + destFileLabel;
        if (timeStamp != null) {
            filePrefix += "_" + timeStamp;
        }
        // use provided timeStamp to the file name for consistence 
        // now last param is false to not use new time stamp in the save method.
        String savedFile = GeneralUtils.saveJsonStrToFile(filePrefix, outPath, jaStr, false);

        return savedFile;
    }

    public JsonObject mergeTwoRasaJsonObjs(JsonObject jo1, JsonObject jo2) {

        JsonArray commJA1 = jo1.getAsJsonArray("common_examples");
        JsonArray commJA2 = jo2.getAsJsonArray("common_examples");
        if (commJA1 != null && commJA2 != null) {
            commJA1.addAll(commJA2);
        } else if (commJA1 != null && commJA2 == null) {
            // nothing, keep commJA1
        } else if (commJA1 == null && commJA2 != null) {
            commJA1 = commJA2;
        } else {
            // both null. keep commJA1 to use
        }

        JsonArray entityJA1 = jo1.getAsJsonArray("entity_examples");
        JsonArray entityJA2 = jo2.getAsJsonArray("entity_examples");
        if (entityJA1 != null && entityJA2 != null) {
            entityJA1.addAll(entityJA2);
            //entityJA1 = mergeEntities(entityJA1, entityJA2);
        } else if (entityJA1 != null && entityJA2 == null) {
            // nothing, keep JA1
        } else if (entityJA1 == null && entityJA2 != null) {
            entityJA1 = entityJA2;
        } else {
            // both null. keep JA1 to use
        }

        JsonArray intentJA1 = jo1.getAsJsonArray("intent_examples");
        JsonArray intentJA2 = jo2.getAsJsonArray("intent_examples");

        if (intentJA1 != null && intentJA2 != null) {
            intentJA1.addAll(intentJA2);

        } else if (intentJA1 != null && intentJA2 == null) {
            // nothing, keep JA1
        } else if (intentJA1 == null && intentJA2 != null) {
            intentJA1 = intentJA2;
        } else {
            // both null. keep JA1 to use
        }

        JsonObject sluDataJO = new JsonObject();

        if (commJA1 != null) {
            sluDataJO.add("common_examples", commJA1); // entity JsonArray
        }

        if (entityJA1 != null) {
            sluDataJO.add("entity_examples", entityJA1);
        }
        if (intentJA1 != null) {
            sluDataJO.add("intent_examples", intentJA1);
        }

        return sluDataJO;
    }

    private JsonArray mergeEntities(JsonArray entityJA1, JsonArray entityJA2) {
        List<JsonObject> joList = new ArrayList();

        for (JsonElement je : entityJA1) {
            JsonObject obj = je.getAsJsonObject();
            joList.add(obj);
        }
        for (JsonElement je : entityJA2) {
            JsonObject obj = je.getAsJsonObject();
            joList.add(obj);
        }

        JsonArray sortedJA = new JsonArray();
        Collections.sort(joList, new IntEntUtils.CompareListJsonObject("start"));
        for (int i = 0; i < joList.size(); ++i) {
            JsonObject jo = joList.get(i);
            sortedJA.add(jo);
        }

        return sortedJA;
    }

    /**
     * generate Rasa Json data from annotated real data with intents and
     * entities.
     *
     * @param domainName
     * @param intent
     * @param uttFile
     * @param totalUttTopLimit
     * @param ratioForTrain
     * @param outPath
     */
    public void createRasaJson_OneDomain_RealAnno(String domainName, String intent, String uttFile,
            String outPath) {

        // In real data file, file name is the intent name
        // and trainset have been already preprocessed to a whole set to use.
        // (also same for testset)
        List<String> annoUtts = IntEntUtils.getColumnContentFromCsv(uttFile,
                IntEntUtils.Col_AnswerAnnotation, true);

        List<String> plainUtts = IntEntUtils.getColumnContentFromCsv(uttFile,
                IntEntUtils.Col_AnswerFromAnno, true);

        JsonArray entiDataJA = new JsonArray();
        JsonArray intentExamJA = new JsonArray();

        for (int i = 0; i < annoUtts.size(); ++i) {
            String annoUtt = annoUtts.get(i);
            String plainUtt = plainUtts.get(i);
            //JsonArray entJA = IntEntUtils.getEntitiesOfUtt_RealAnno(annoUtt, plainUtt,
            //        IntEntUtils.ServiceName_RASA);
            JsonArray entJA = IntEntUtils.getEntitiesOfUtt_RealAnno(annoUtt, plainUtt,
                    IntEntUtils.ServiceName_RASA);
            JsonObject entiDataItemJO = new JsonObject();
            entiDataItemJO.addProperty("text", plainUtt);
            entiDataItemJO.addProperty("intent", intent);
            entiDataItemJO.add("entities", entJA);
            entiDataJA.add(entiDataItemJO);

            // for intents
            JsonObject intentItemJO = new JsonObject();
            intentItemJO.addProperty("text", plainUtt);
            intentItemJO.addProperty("intent", intent);
            intentExamJA.add(intentItemJO);
        }

        JsonObject sluDataJO = new JsonObject();
        // 04 July 2017
        //sluDataJO.add("entity_examples", entiDataJA);        
        //sluDataJO.add("intent_examples", intentExamJA);
        // WARNING:rasa_nlu.converters:DEPRECATION warning: Data file contains 'intent_examples' 
        // or 'entity_examples' which will be removed in the future. 
        // Consider putting all your examples into the 'common_examples' section.

        sluDataJO.add("common_examples", entiDataJA);
        sluDataJO.add("entity_examples", new JsonArray());
        sluDataJO.add("intent_examples", new JsonArray());

        JsonObject rasaDataJO = new JsonObject();
        rasaDataJO.add("rasa_nlu_data", sluDataJO);

        String jaStr = rasaDataJO.toString();
        //String outPath = "resources/out4RasaSeparated";
        GeneralUtils.saveJsonStrToFile("RasaNlu_" + domainName, outPath, jaStr, true);
        // System.out.println("new size:" + newsents.size());

    }

    public void loadConfig() {

        try {
            File file = new File(CONFIG_FILE);
            FileInputStream fileInput = new FileInputStream(file);
            Properties properties = new Properties();
            properties.load(fileInput);
            fileInput.close();

            destRootDirTop = properties.getProperty("destRootDirTop");

            trainsetPercent = properties.getProperty("trainsetPercent");
            
            projectName = properties.getProperty("projectName");
            regexFeatureFile  = properties.getProperty("rasaRegexFeatureFile");

            String sKFold = properties.getProperty("numKFold");
            numKFold = Integer.parseInt(sKFold);
                    
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
        PrepareRasaCorpora pc = new PrepareRasaCorpora();
    }

}
