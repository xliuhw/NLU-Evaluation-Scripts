/**
 * ************************************************************
 * @(#)PrepareApiaiCorpora.java
 *
 *
 *              Copyright(C) 2016 iLab MACS
 *              Heriot-Watt University
 *
 *                  All rights reserved
 *
 *   Version: 0.1  Created on 22 Feb 2017
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import static uk.ac.hw.macs.ilab.nlu.corpus.PreprocessRealData.CONFIG_FILE;
import uk.ac.hw.macs.ilab.nlu.util.GeneralUtils;

/**
 *
 * @author X.Liu
 */
public class PrepareApiaiCorpora {

    String destRootDirTop = ".";
    int numKFold = 0; // 0: do not create data for cross validation. Overwritten by config file
    
    public PrepareApiaiCorpora() {
        
        loadConfig();
        
        String nameInfoFileDir = destRootDirTop + "/allGeneFileNames";
        String nameInfoFile = "allGeneFileNames.txt";
         
        String infoFile = nameInfoFileDir + "/" + nameInfoFile;
        String flagLine = "=A_TASK_START=";
        Map<String, String> infoMap = IntEntUtils.getPreprocessedInfo(infoFile, flagLine);
        String timeStamp = infoMap.get("timeStamp");
        String geneBaseDir = infoMap.get("preprocessOutBaseDir");
        // for full set train, use preprocessOutDir_Sub4Subset
       // String trainsetDir = infoMap.get("preprocessOutDir_Sub4Subset");
        String trainsetDir = infoMap.get("preprocessOutDir_Sub5Train");
        
        String fileLabel = "fullset";
        if(trainsetDir.endsWith("_Train"))
            fileLabel = "trainset";
        
        String srcTrainsetBaseDir = geneBaseDir + "/" + trainsetDir;
        String destBaseDir = destRootDirTop + "/preprocessResults/out4ApiaiReal/Apiai_" +fileLabel+"_"+ timeStamp;
        String sampleAgentFile = "resources/apiaiSampleAgent/agent.json";

        List<String> nameInfo = processForOneTrainset(srcTrainsetBaseDir, destBaseDir, 
            timeStamp, sampleAgentFile, fileLabel, 0 );
        
        // true for appending. null for using default encoding.
        GeneralUtils.saveToTextFile(nameInfoFileDir, nameInfoFile, nameInfo, true, null);
        
        if(numKFold != 0) {
            String srcTaskBaseDir = geneBaseDir;
            String destTaskBaseDir = destBaseDir;
            nameInfo = processForCrossValidation(srcTaskBaseDir, destTaskBaseDir,
            timeStamp,sampleAgentFile);           
        }
        
    }

    private  List<String> processForOneTrainset(String srcTrainsetBaseDir, String destBaseDir, 
            String timeStamp, String sampleAgentFile, String fileNameLabel, int numKFold ) {
        // timeStamp is not used here but keep it for consistency with other service and potential later use.
        // e.g. trainsetBaseDir: geneBaseDir + DomainIntentNoDupShuffleSubset_Train
        // destBaseDir0: preprocessResults/out4RasaReal/rasa_json_" + timeStamp + "_" + fileNameLabel;
        // For Normal: destBaseDir = destBaseDir0
        // For cross-validation destBaseDir = destBaseDir0+CrossValidation/KFold_i/
        String destSepaBaseDir = destBaseDir + "/separated";
        GeneralUtils.checkAndCreateDirectory(destSepaBaseDir);
                
        createApiaiSeparatedDomainData_RealAnno(srcTrainsetBaseDir, destSepaBaseDir, sampleAgentFile);

        String srcMergeBaseDir = destSepaBaseDir;
        String destMergeBaseDir =  destBaseDir +  "/merged";
        GeneralUtils.checkAndCreateDirectory(destMergeBaseDir);
        
        mergeAll4Apiai_RealAnno(srcMergeBaseDir,destMergeBaseDir);
        
        List<String> nameInfo = new ArrayList<>();
        if( numKFold == 0) { // for nomal process---Non CrossValidation
            nameInfo.add("# ---- APIAI ----");
            nameInfo.add("currentTime = " + GeneralUtils.getTimeStamp4Logging() );
            String item = "prepareApiaiOutDir=" + destBaseDir;
            nameInfo.add(item);

            item = "prepareApiaiMergedDir=" + destMergeBaseDir;
            nameInfo.add(item);    

            item = "prepareApiaiUsedSampleAgent=" + sampleAgentFile;
            nameInfo.add(item);    

            item = "apiaiActuallyUsedTrainset=" + fileNameLabel;
            nameInfo.add(item);    

            nameInfo.add(""); 
        } else {
            // todo
            
        }
        return nameInfo;
    }
    
    private  List<String> processForCrossValidation(String srcTaskBaseDir, String destTaskBaseDir,
            String timeStamp, String sampleAgentFile) {
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
            //String testsetBaseDir = srciFoldDir + "/testset";
            String destBaseDir = destCvBaseDir + "/"+ kFoldName;
            String fileNameLabel = kFoldName; // KFold_1, KFold_2 etc.
            //String testsetAnnoDir = testsetBaseDir + "/annotated";
            
            //For cross-validation destBaseDir = destTaskBaseDir+CrossValidation/KFold_i/
            processForOneTrainset(trainsetBaseDir, destBaseDir, timeStamp,
              sampleAgentFile, fileNameLabel, 1 );// here last param can be anything > 0
        }
        
        List<String> nameInfo = new ArrayList<>();
        return nameInfo; //TODO add something
    }
    
    private void createApiaiSeparatedDomainData_RealAnno(String srcDir, String destBaseDir, String sampleAgentFile) {
        
        List<String> fileNames = GeneralUtils.getAllFileNames(srcDir);

        for (int i = 0; i < fileNames.size(); i++) {
            String fname = fileNames.get(i);

            String csvFileUri = srcDir + "/" + fname;
            String intent = fname.substring(0, fname.lastIndexOf("."));
            String domainName = intent;

            createApiaiJson_OneDomain_RealAnno(domainName, sampleAgentFile, destBaseDir, intent,
                    csvFileUri);
        }

    }

    // todo: merge xx_Real_IntentOnly with xxx_RealAnno
    public void mergeAll4Apiai_RealAnno(String srcBaseDir, String destBaseDir) {

        List<String> subdirNames = GeneralUtils.getAllSubdirNames(srcBaseDir);

        String srcAgent = srcBaseDir + "/" + subdirNames.get(0) + "/agent.json"; // just use any of them
        String dstAgent = destBaseDir + "/agent.json";

        try {
            GeneralUtils.copyFile(new File(srcAgent), new File(dstAgent));
        } catch (IOException ex) {
            Logger.getLogger(PrepareApiaiCorpora.class.getName()).log(Level.SEVERE, null, ex);
        }

        String destDirEnt = destBaseDir + "/" + "entities";
        String destDirInt = destBaseDir + "/" + "intents";
        GeneralUtils.checkAndCreateDirectory(destDirEnt);
        GeneralUtils.checkAndCreateDirectory(destDirInt);

        for (int i = 0; i < subdirNames.size(); ++i) {
            String srcDirEnt = srcBaseDir + "/" + subdirNames.get(i) + "/entities";
            GeneralUtils.copyDirectory(new File(srcDirEnt), new File(destDirEnt), "");

            String srcDirInt = srcBaseDir + "/" + subdirNames.get(i) + "/intents";
            GeneralUtils.copyDirectory(new File(srcDirInt), new File(destDirInt), "");
        }

    }

    public void createApiaiJson_OneDomain_RealAnno(String domainName,
            String sampleAgentFile, String destBaseDir,
            String intent, String csvFileUri) {

        String newAgentDesc = "this is new agent for auto-annoated data - " + domainName;

        String destDir = destBaseDir + "/" + domainName;

        Map<String, List<String>> entityValueMap = IntEntUtils.getEntityValueMap_OneDomain_RealAnno(csvFileUri);

        createApiaiAgent(newAgentDesc, sampleAgentFile, destDir);

        String destDir_entities = destDir + "/" + "entities";

        //if(entityValueMap != null)
        createApiaiEntities_RealAnno(destDir_entities, entityValueMap);

        List<String> annoUttList = IntEntUtils.getAnnotatedUttsFromCsv(csvFileUri);
        List<String> plainUttList = IntEntUtils.getPlainUttsFromCsv(csvFileUri);

        String destDir_Intent = destDir + "/" + "intents";
        createApiaiIntent_RealAnno(intent, annoUttList, plainUttList, destDir_Intent);

    }

    
    public void createApiaiAgent(String newAgentDesc, String sampleAgentFile, String destDir) {
        JsonObject sampAgent = GeneralUtils.readFileToJson(sampleAgentFile);
        //description
        sampAgent.addProperty("description", newAgentDesc);
        // do not append timestamp to file name
        GeneralUtils.saveJsonStrToFile("agent", destDir, sampAgent.toString(), false);

    }
    
        
    /**
     * Create Apiai json data format for real annotated data. It is the same as
     * previous grammar based.
     *
     * @param destDir
     * @param entityValueMap
     */
    public void createApiaiEntities_RealAnno(String destDir, Map<String, List<String>> entityValueMap) {

        for (Map.Entry<String, List<String>> entry : entityValueMap.entrySet()) {
            // one entity for creating one json file
            String entityName = entry.getKey();
            JsonArray entriesJA = new JsonArray();
            List<String> values = entry.getValue();
            System.out.println("entity values:" + values.toString());
            for (String value : values) {
                JsonObject entryJO = new JsonObject();
                entryJO.addProperty("value", value);
                JsonArray synonymJA = new JsonArray();
                synonymJA.add(value); // not consider other synonyms for now
                entryJO.add("synonyms", synonymJA);
                entriesJA.add(entryJO);
            }

            JsonObject entityJO = new JsonObject();
            String uuid = UUID.randomUUID().toString();
            entityJO.addProperty("id", uuid);
            entityJO.addProperty("name", entityName);
            entityJO.addProperty("isOverridable", true);
            entityJO.add("entries", entriesJA);
            entityJO.addProperty("isEnum", Boolean.FALSE);
            entityJO.addProperty("automatedExpansion", Boolean.TRUE);

            GeneralUtils.saveJsonStrToFile(entityName, destDir, entityJO.toString(), false);

            // now save this entity to file
        }

    }

    
    /**
     * To create one intent data file for API.AI. One intent is one domain and
     * in one csv file in real annotated data.
     *
     * @param intent
     * @param uttList
     * @param entityValueMap
     * @return
     */
    public void createApiaiIntent_RealAnno(String intent, List<String> annoUttList,
            List<String> plainUttList, String destDir) {

        JsonObject intentJO = new JsonObject();

        JsonArray userSaysArr = new JsonArray();

        for (int i = 0; i < annoUttList.size(); ++i) {
            JsonObject uttJO = createApiaiIntent_ObjOfUtt_RealAnno(annoUttList.get(i),
                    plainUttList.get(i));
            userSaysArr.add(uttJO);
        }

        System.out.println("prepare intent file.");
        String uuid = UUID.randomUUID().toString();
        intentJO.add("userSays", userSaysArr);
        intentJO.addProperty("id", uuid);
        intentJO.addProperty("name", intent);
        intentJO.addProperty("auto", Boolean.TRUE);
        intentJO.add("contexts", new JsonArray());
        JsonArray responsesJA = null;

        // get all entity names in this intent domain.
        List<String> entityList = null;
        entityList = IntEntUtils.extractUniqueEntities_OneDomain(annoUttList);

        responsesJA = createResponsesJA(entityList); // enities in this corpus of the intent.
        intentJO.add("responses", responsesJA);
        intentJO.addProperty("webhookUsed", false);
        intentJO.addProperty("webhookForSlotFilling", false);
        intentJO.addProperty("fallbackIntent", false);
        intentJO.add("events", new JsonArray());

        GeneralUtils.saveJsonStrToFile(intent, destDir, intentJO.toString(), false);

    }

    private JsonArray createResponsesJA(List<String> entityNameList) {
        JsonObject respJO = new JsonObject();
        respJO.addProperty("resetContexts", Boolean.FALSE);
        respJO.add("affectedContexts", new JsonArray());
        JsonArray paramJA = new JsonArray();
        if (entityNameList != null && !entityNameList.isEmpty()) {
            paramJA = createParametersJA(entityNameList);
        }

        respJO.add("parameters", paramJA);
        JsonArray messagesJA = new JsonArray();
        JsonObject messageJO = new JsonObject();
        messageJO.addProperty("type", 0);
        messageJO.add("speech", new JsonArray());
        messagesJA.add(messageJO);
        respJO.add("messages", messagesJA);

        JsonArray respJA = new JsonArray();
        respJA.add(respJO);

        return respJA;

    }

    private JsonArray createParametersJA(List<String> entityNameList) {
        JsonArray paramsJA = new JsonArray();
        for (int i = 0; i < entityNameList.size(); ++i) {
            String entName = entityNameList.get(i);
            JsonObject entJO = new JsonObject();
            String atEnt = "@" + entName;
            entJO.addProperty("dataType", atEnt);
            entJO.addProperty("name", entName);
            String dEnt = "$" + entName;
            entJO.addProperty("value", dEnt);
            paramsJA.add(entJO);
        }
        return paramsJA;
    }

    
    private JsonObject createApiaiIntent_ObjOfUtt_RealAnno(String annoUtt, String plainUtt) {
        // id, data[], isTemplate, count.
        System.out.println("\n\n annotate utt:" + annoUtt);
        System.out.println("\n plain utt:" + plainUtt);
        JsonArray entInUtt = null;

        entInUtt = IntEntUtils.getEntitiesOfUtt_RealAnno(annoUtt, plainUtt,
                IntEntUtils.ServiceName_APIAI);
        System.out.println("annotate utt, entities:" + entInUtt.toString());
        //int crntPos = 0;
        String crntUtt = plainUtt;
        JsonArray dataArr = new JsonArray();
        if (entInUtt == null || entInUtt.size() == 0) {
            JsonObject uttObj = new JsonObject();
            uttObj.addProperty("text", plainUtt);
            dataArr.add(uttObj);
        } else {
            for (int i = 0; i < entInUtt.size(); ++i) {
                JsonObject entItem = entInUtt.get(i).getAsJsonObject();
                String entityName = entItem.get("entity").getAsString();
                String entityValue = entItem.get("value").getAsString();
                System.out.println("in loop ent name:" + entityName + "value:" + entityValue);
                //String utt1 = crntUtt.substring(crntPos, crntUtt.indexOf(entityValue));
                String utt1 = crntUtt.substring(0, crntUtt.indexOf(entityValue));
                System.out.println("utt1:" + utt1 + "|");
                JsonObject utt1Obj = new JsonObject();
                utt1Obj.addProperty("text", utt1);
                JsonObject entityObj = new JsonObject();
                entityObj.addProperty("text", entityValue);
                entityObj.addProperty("alias", entityName);
                String atEntName = "@" + entityName;
                entityObj.addProperty("meta", atEntName);
                entityObj.addProperty("userDefined", true);
                dataArr.add(utt1Obj);
                dataArr.add(entityObj);
                crntUtt = crntUtt.substring(crntUtt.indexOf(entityValue) + entityValue.length());
                System.out.println("crntUtt:" + crntUtt);
                System.out.println("crntUtt:empty" + crntUtt.isEmpty());
            }

            if (!crntUtt.isEmpty()) {
                JsonObject lastPartUtt = new JsonObject();
                lastPartUtt.addProperty("text", crntUtt);
                dataArr.add(lastPartUtt);
            }
        }

        JsonObject uttJO = new JsonObject();
        String uuid = UUID.randomUUID().toString();
        uttJO.addProperty("id", uuid);
        uttJO.add("data", dataArr);
        uttJO.addProperty("isTemplate", false);
        uttJO.addProperty("count", 0);

        System.out.println("Finished annotateOneUtterance.");
        return uttJO;
    }
    
    public void loadConfig() {

        try {
            File file = new File(CONFIG_FILE);
            FileInputStream fileInput = new FileInputStream(file);
            Properties properties = new Properties();
            properties.load(fileInput);
            fileInput.close();

            destRootDirTop = properties.getProperty("destRootDirTop");

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
        PrepareApiaiCorpora pc = new PrepareApiaiCorpora();
    }

}
