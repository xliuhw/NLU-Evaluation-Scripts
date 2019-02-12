/**
 * ************************************************************
 * @(#)PrepareLuisCorpora.java
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import static uk.ac.hw.macs.ilab.nlu.corpus.PreprocessRealData.CONFIG_FILE;
import uk.ac.hw.macs.ilab.nlu.util.GeneralUtils;

/**
 *
 * @author X.Liu
 */
public class PrepareLuisCorpora {

    // Hierarchical map of all domain
    public static Map<String, Map<String, List<String>>> hierMapAllDomain;
    int numOfChildrenEntity = 5; // to group this number of children entity as one layer.
    public static boolean createHierarchicalEnitiy = true;
    
    String destRootDirTop = ".";
    int numKFold = 0; // 0: do not create data for cross validation. Overwritten by config file
    
    public PrepareLuisCorpora() {
     
      doProcess();
    }
    
    
    private void doProcess() {
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

        String fileNameLabel = "fullset";
        if(trainsetDir.endsWith("_Train"))
           fileNameLabel = "trainset";
        
        String srcTrainsetBaseDir = geneBaseDir + "/" + trainsetDir;
        String destBaseDir = destRootDirTop + "/preprocessResults/out4LuisReal/Luis_" +fileNameLabel +"_"+ timeStamp;
       // String destSepaBaseDir = destBaseDir + "/separated";
       // GeneralUtils.checkAndCreateDirectory(destSepaBaseDir);

        Map<String, List<String>> oMap = IntEntUtils.getEntityValueMap_AllDomain_RealAnno(srcTrainsetBaseDir);
        hierMapAllDomain = convert2HierarchicalMap(oMap, this.numOfChildrenEntity);
        
        String sCrntTimeShort = GeneralUtils.getTimeStamp4LoggingShort();
        
        List<String> nameInfo = processForOneTrainset(srcTrainsetBaseDir, destBaseDir, 
            timeStamp, sCrntTimeShort, fileNameLabel, 0 );
        
        // true for appending. null for using default encoding.
        GeneralUtils.saveToTextFile(nameInfoFileDir, nameInfoFile, nameInfo, true, null);    

        if(numKFold != 0) {
            String srcTaskBaseDir = geneBaseDir;
            String destTaskBaseDir = destBaseDir;
            nameInfo = processForCrossValidation(srcTaskBaseDir, destTaskBaseDir,
            timeStamp, sCrntTimeShort);           
        }
    
    }
    
    private  List<String> processForOneTrainset(String srcTrainsetBaseDir, String destBaseDir, 
            String timeStamp, String sCrntTimeShort, String fileNameLabel, int numKFold ) {
        // timeStamp is not used here but keep it for consistency with other service and potential later use.
        // e.g. trainsetBaseDir: geneBaseDir + DomainIntentNoDupShuffleSubset_Train
        // destBaseDir0: preprocessResults/out4RasaReal/rasa_json_" + timeStamp + "_" + fileNameLabel;
        // For Normal: destBaseDir = destBaseDir0
        // For cross-validation destBaseDir = destBaseDir0+CrossValidation/KFold_i/
        String destSepaBaseDir = destBaseDir + "/separated";
        GeneralUtils.checkAndCreateDirectory(destSepaBaseDir);
       // RealAnno Step2:
       createLuisSeparatedDomainData_All_RealAnno(srcTrainsetBaseDir, destSepaBaseDir, timeStamp, sCrntTimeShort);
       
       // RealAnno Step3:
       String srcMergePath = destSepaBaseDir;
       String destMergePath = destBaseDir + "/merged";
       String mergeFilePrefix = "Luis_" + fileNameLabel + "_" + timeStamp + "_" + sCrntTimeShort;
       
       String mergedFileUri = mergeLuisTrainData(srcMergePath, destMergePath, mergeFilePrefix);
       String uttSizeInfo = getUttSizeInfoFromMergedFile(mergedFileUri);
       
        List<String> nameInfo = new ArrayList<>();
        if(numKFold == 0) {
            nameInfo.add("# ---- LUIS ----");
            nameInfo.add("currentTime = " + GeneralUtils.getTimeStamp4Logging() );
            String item = "prepareLuisOutDir=" + destBaseDir;
            nameInfo.add(item);

            item = "prepareLuisMergedDir=" + destMergePath;
            nameInfo.add(item);    

            item = "luisMergedFileUri=" + mergedFileUri;
            nameInfo.add(item);    

            item ="MergedTrainUttInfo=" + uttSizeInfo;
            nameInfo.add(item);

            item = "luisActuallyUsedTrainset=" + fileNameLabel;
            nameInfo.add(item);    

            nameInfo.add(""); 
        } else {
            //toto
            
        }
        return nameInfo;
        
    }
    
    private  List<String> processForCrossValidation(String srcTaskBaseDir, String destTaskBaseDir,
            String timeStamp, String sCrntTimeShort) {
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

            Map<String, List<String>> oMap = IntEntUtils.getEntityValueMap_AllDomain_RealAnno(trainsetBaseDir);
            hierMapAllDomain = convert2HierarchicalMap(oMap, this.numOfChildrenEntity);
            
            //For cross-validation destBaseDir = destTaskBaseDir+CrossValidation/KFold_i/
            processForOneTrainset(trainsetBaseDir, destBaseDir, timeStamp,
              sCrntTimeShort, fileNameLabel, 1 );// here last param can be anything > 0
        }
        
        List<String> nameInfo = new ArrayList<>();
        return nameInfo; //TODO add something
    }
    
    private void createLuisSeparatedDomainData_All_RealAnno(String srcBaseDir, String destBaseDir,
    String geneDataTime, String crntTimeShort) {

        List<String> fileNames = GeneralUtils.getAllFileNames(srcBaseDir);
        
        for (int i = 0; i < fileNames.size(); i++) {
            String fname = fileNames.get(i);
            
            String uttFileUri = srcBaseDir + "/" + fname; // in realAnno, it is a csv file
            String intent = fname.substring(0, fname.lastIndexOf("."));
            String domainName = intent;
        
            createLuisJson_OneDomain_RealAnno(domainName, destBaseDir, intent, uttFileUri,
                    geneDataTime, crntTimeShort);             
            
        }
    }

    private String getUttSizeInfoFromMergedFile(String mergedFileUri) {
                     
        JsonObject aJO = GeneralUtils.readJsonFile(mergedFileUri);
        JsonArray uttsJA = aJO.get("utterances").getAsJsonArray();
        List<String> noDup = new ArrayList<>();
        for(int i = 0; i < uttsJA.size(); ++i) {
            JsonObject itemJO = uttsJA.get(i).getAsJsonObject();
            String text = itemJO.get("text").getAsString().trim();
            if( !noDup.contains(text))
                noDup.add(text);
            
           // System.out.println("i= " + i + ", Utt= " + text);
        }
       
        System.out.println("\n Origia size= " + uttsJA.size());
        System.out.println("\n No Dup size= " + noDup.size());
        
        String sizeInfo = "utt size: " + uttsJA.size() + ", NoDup utt size:" + noDup.size();
        return sizeInfo;
    }
    
    private String getUttSizeInfoFromMergedFile_Test(String mergedFileUri) {
                     
        JsonObject aJO = GeneralUtils.readJsonFile(mergedFileUri);
        JsonArray uttsJA = aJO.get("utterances").getAsJsonArray();
        List<String> noDup = new ArrayList<>();
        for(int i = 0; i < uttsJA.size(); ++i) {
            JsonObject itemJO = uttsJA.get(i).getAsJsonObject();
            String text = itemJO.get("text").getAsString().trim();
            if( !noDup.contains(text))
                noDup.add(text);
            
            System.out.println("i= " + i + ", Utt= " + text);
        }
       
        System.out.println("\n Origia size= " + uttsJA.size());
        System.out.println("\n No Dup size= " + noDup.size());
        
        String sizeInfo = "utt size: " + uttsJA.size() + ", NoDup utt size:" + noDup.size();
        return sizeInfo;
    }

    
    /**
     * Return saved merged file URI.
     * 
     * @param fileDir file directory contains separated json files.
     * 
     * @return String of saved file URI.
     */
    public String mergeLuisTrainData(String srcFileDir, String destBaseDir, String mergeFilePrefix) {
        File folder = new File(srcFileDir);
        File[] listOfFiles = folder.listFiles();
        System.out.println("total num:" + listOfFiles.length);
        
        List<String> fullFiles = new ArrayList<>();
        
        for (int i = 0; i < listOfFiles.length; i++) {
            String fname = listOfFiles[i].getName();
            if (listOfFiles[i].isDirectory()) {
                continue;
            }
            String fileUri = srcFileDir + "/" + fname;
            fullFiles.add(fileUri);
        }
        
        System.out.println("total fullFiles num:" + fullFiles.size());
        System.out.println("file0:" + fullFiles.get(0));
        
        JsonObject jo1 = GeneralUtils.readJsonFile(fullFiles.get(0));
        //jo1 = jo1.get("rasa_nlu_data").getAsJsonObject();
        for (int i = 1; i < fullFiles.size(); i++) {
            System.out.println("A Luis Json file:" + fullFiles.get(i));
            JsonObject jo2 = GeneralUtils.readJsonFile(fullFiles.get(i));
            //jo2 = jo2.get("rasa_nlu_data").getAsJsonObject();
            jo1 = mergeTwoLuisJsonObjs(jo1, jo2);
        }
                
        //JsonObject rasaDataJO = new JsonObject();
        //rasaDataJO.add("rasa_nlu_data", jo1);
        
       // String jaStr = rasaDataJO.toString();
        String jaStr = jo1.toString();
       // String outPath = "resources/out4Luis/merged";
        // true -- with timestamp on file name
        // false -- without new timestamp, only use info in mergeFilePrefix
        String savedFile = GeneralUtils.saveJsonStrToFile(mergeFilePrefix, destBaseDir, jaStr, false);
        
        return savedFile;
    
    }
    
    public JsonObject mergeTwoLuisJsonObjs(JsonObject jo1, JsonObject jo2) {
        
        JsonArray intentsJA1 = jo1.getAsJsonArray("intents");
        JsonArray intentsJA2 = jo2.getAsJsonArray("intents");
        intentsJA1.addAll(intentsJA2);
        
        // remove duplicated intent items
        Set<JsonObject> setJO = new HashSet<>();
        for (int i = 0; i < intentsJA1.size(); ++i) {
            JsonObject tJO = intentsJA1.get(i).getAsJsonObject();
            setJO.add(tJO);
        }
        JsonArray newIntents = new JsonArray();
        Iterator it = setJO.iterator();
        while(it.hasNext()) {
            newIntents.add((JsonObject)it.next());
        }
        jo1.add("intents", newIntents);
        
        JsonArray entitiesJA1 = jo1.getAsJsonArray("entities");
        JsonArray entitiesJA2 = jo2.getAsJsonArray("entities");
        
        JsonArray newEntities = new JsonArray();
        if( entitiesJA1.size() != 0 || entitiesJA2.size() != 0) {
            if(this.createHierarchicalEnitiy) {
                newEntities = mergeTwoLuisHierarchicalEntities(entitiesJA1, entitiesJA2);
            } else {
                entitiesJA1.addAll(entitiesJA2);
                // remove duplicated entities items
                Set<JsonObject> setEntJO = new HashSet<>();
                for (int i = 0; i < entitiesJA1.size(); ++i) {
                    JsonObject tJO = entitiesJA1.get(i).getAsJsonObject();
                    setEntJO.add(tJO);
                }
                newEntities = new JsonArray();
                it = setEntJO.iterator();
                while(it.hasNext()) {
                    newEntities.add((JsonObject)it.next());
                }            
            }
        }

        jo1.add("entities", newEntities); //this will replace existing entities

        // process utterances
        JsonArray uttJA1 = jo1.getAsJsonArray("utterances");
        JsonArray uttJA2 = jo2.getAsJsonArray("utterances");
        uttJA1.addAll(uttJA2);
        
        return jo1;
    }
    
    public JsonArray mergeTwoLuisHierarchicalEntities(JsonArray ja1, JsonArray ja2) {
        
        // check if ja2 items are in ja1, if not, add it, then remove it at the end.
        List<String> addedKeys = new ArrayList<>();
        for (int j = 0; j < ja2.size(); ++j) {
            JsonObject itemjo2 = ja2.get(j).getAsJsonObject();
            String parentKey2 = itemjo2.get("name").getAsString();     
            // if item2 is not in ja1, add it to ja1, remember added name
            if(!isContainParentKey(ja1, parentKey2)) {
                ja1.add(itemjo2);
                addedKeys.add(parentKey2);
            }
        }
       System.out.println(" === added keys ====");
       System.out.println(GeneralUtils.prettyPrintList(addedKeys));
       
      
        // remove added keys from ja2
        System.out.println("check reamining ...........");
        for (int j = 0; j < addedKeys.size(); ++j) {
            
            String addedKey = addedKeys.get(j);
            for (int k = 0; k < ja2.size(); ++k) {
                JsonObject itemjo2 = ja2.get(k).getAsJsonObject();
                String parentKey2 = itemjo2.get("name").getAsString();   
                // if added to ja1 before, then remove it.
                if(parentKey2.equals(addedKey)) {
                    ja2.remove(itemjo2);
                    break;
                }
            }
            
        }
                
        // now remaining items in ja2 are also in ja1, merge them
        for (int j = 0; j < ja2.size(); ++j) {
            JsonObject itemjo2 = ja2.get(j).getAsJsonObject();
            String parentKey2 = itemjo2.get("name").getAsString();     
            // 
            JsonArray children2 = itemjo2.getAsJsonArray("children");
            JsonArray children1 = getExistingChildren(ja1, parentKey2);
            // children1.addAll(children2); // this will add duplicated ones
            for (int k = 0; k < children2.size(); ++k) {
                String ch2 = children2.get(k).getAsString();
                if(!GeneralUtils.isJsonArrayContainString(children1, ch2))
                    children1.add(ch2);
            }
            
        }

        return ja1;
    }
    
    public void createLuisJson_OneDomain_RealAnno(String domainName, String destDir,
            String intent, String uttFileUri, String geneDataTime, String crntTimeShort) {
        //Date sysDate = new Date();
        //java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("yyyy_MM_dd-HH_mm_ss_SSS");
        //String sDt = df.format(sysDate);
        String agentName = "LuisMerged_" + crntTimeShort;
        JsonObject luisJO = new JsonObject();
        //luisJO.addProperty("luis_schema_version", "1.3.1");
        luisJO.addProperty("luis_schema_version", "2.1.0");
        
        luisJO.addProperty("versionId", "0.1");
        luisJO.addProperty("name", agentName);
        String desc = "Xing Luis RealAnno Auto at " + geneDataTime;
        luisJO.addProperty("desc", desc);
        luisJO.addProperty("culture", "en-us");
        List<String> intentList = new ArrayList<>();
        intentList.add(intent);
        JsonArray intents = createLuisIntents(intentList);
        luisJO.add("intents", intents);

        //Map<String, List<String>> entityValueMap = null;

        List<String> entKeyList = IntEntUtils.extractUniqueEntities_OneDomain(uttFileUri);

        JsonArray entities = createLuisEntitiesHierar(entKeyList);

        // this is all entities in this domain (i.e. intent. one domain with only one intent
        // entities in utterance below are entities only in that utterance.
        luisJO.add("entities", entities); 
        luisJO.add("composites", new JsonArray());
        luisJO.add("closedLists", new JsonArray());
        luisJO.add("bing_entities", new JsonArray());
        luisJO.add("actions", new JsonArray());
        luisJO.add("model_features", new JsonArray());
        luisJO.add("regex_features", new JsonArray());

        List<String> annoUttList = IntEntUtils.getAnnotatedUttsFromCsv(uttFileUri);
        List<String> plainUttList = IntEntUtils.getPlainUttsFromCsv(uttFileUri);
        
        JsonArray utts = createLuisJson_AllUtts_RealAnno(annoUttList, plainUttList, intent);
        luisJO.add("utterances", utts);


        GeneralUtils.saveJsonStrToFile(domainName, destDir, luisJO.toString(), false);// false = no timestamp

    }

    private JsonArray createLuisIntents(List<String> itemList) {
        JsonArray ja = new JsonArray();
        for (int i = 0; i < itemList.size(); ++i) {
            JsonObject intentJO = new JsonObject();
            intentJO.addProperty("name", itemList.get(i));
            ja.add(intentJO);   
        }
        return ja;
    }

    /**
     * Create entities of all defined in this domain, not the ones only referenced in the utterances
     * This is for the Hierarchical entity type.
     * 
     * @param itemList
     * @return JsonsArray of the entities.
     */
    private JsonArray createLuisEntitiesHierar(List<String> itemList) {
        JsonArray hierEntJA = new JsonArray();
        for (int i = 0; i < itemList.size(); ++i) {
            String entity = itemList.get(i);
            String parentKey = getPredefinedParentKey(entity);

            JsonArray children = getExistingChildren(hierEntJA, parentKey);
            
            if(children == null) {
                JsonObject entJO = new JsonObject();
                entJO.addProperty("name", parentKey);
                children = new JsonArray();
                children.add(entity);
                entJO.add("children",children);
                hierEntJA.add(entJO);
            } else {
                children.add(entity);
            }
            
        }
        return hierEntJA;
    }

    private JsonArray getExistingChildren(JsonArray entityJA, String parentKey) {
        JsonArray children = null;
        for(int i = 0; i < entityJA.size(); ++i) {
            JsonObject itemJO = entityJA.get(i).getAsJsonObject();
            String pkey = itemJO.get("name").getAsString();
            if(pkey.equals(parentKey)) {
                children = itemJO.get("children").getAsJsonArray();
                break;
            }
        }
        return children;
    }
    
    private boolean isContainParentKey(JsonArray entityJA, String parentKey) {
        boolean found = false;
        for(int i = 0; i < entityJA.size(); ++i) {
            JsonObject itemJO = entityJA.get(i).getAsJsonObject();
            String pkey = itemJO.get("name").getAsString();
            //System.out.println(parentKey + "=" + pkey + ":" + pkey.equals(parentKey));
            if(pkey.equals(parentKey)) {
                found = true;
                break;
            }
        }
        return found;
    }

    private String getPredefinedParentKey(String childEntity) {
        //Map<String, Map<String, List<String>>> hierMapAllDomain;
         for (Map.Entry<String, Map<String, List<String>>> entry : this.hierMapAllDomain.entrySet()) {
            String key = entry.getKey();
            Map<String, List<String>> parentValues = entry.getValue();
            Set<String> childrenKeys = parentValues.keySet();
            if(childrenKeys.contains(childEntity)) 
                return key;
         }
         System.err.println("==== ERROR: Can not get parent key for " + childEntity);
         System.exit(-1);
         return "Unknown";
        
    }
    
    
    private JsonArray createLuisJson_AllUtts_RealAnno(List<String> annoUttList,
                               List<String> plainUttList, String intent) {
        JsonArray ja = new JsonArray();
        for (int i = 0; i < annoUttList.size(); ++i) {
            JsonObject uttJO = createLuisJson_OneUtt_RealAnno(annoUttList.get(i), plainUttList.get(i), intent);

            ja.add(uttJO);
        }
        return ja;
    }


    private JsonObject createLuisJson_OneUtt_RealAnno(String annoUtt, String plainUtt, String intent) {

        System.out.println("\n\n annotate utt:" + annoUtt);

        JsonObject uttObj = new JsonObject();
        uttObj.addProperty("text", plainUtt);
        uttObj.addProperty("intent", intent);

        JsonArray entInUtt = IntEntUtils.getEntitiesOfUtt_RealAnno(annoUtt, plainUtt, 
                    IntEntUtils.ServiceName_LUIS);
       
        System.out.println("annotate utt, entities:" + entInUtt.toString());
            
        uttObj.add("entities", entInUtt);

        System.out.println("Finished annotateOneUtterance.");
        return uttObj;
    }

    
    /**
     * LUIS app limits the number of entities to 10 (intents to 80) at the moment
     * (10.03.2017)
     * 
     * This is to convert a standard map to a map of map which will be used to 
     * produce Hierarchical Entities in LUIS app.
     * 
     * @param origMap the original entity value map.
     * @param num int, number of children key, maximum = 10
     * @return Map of Map.
     */
    private Map<String, Map<String, List<String>>> convert2HierarchicalMap(
            Map<String, List<String>> origMap, int num) {
        int i = 0;
        int p = 0;
        String parentKeyPref = "Hier";
        Map<String, Map<String, List<String>>> resMap = new LinkedHashMap();
        Map<String, List<String>> tmpMap = new LinkedHashMap();
        for (Map.Entry<String, List<String>> entry : origMap.entrySet()) {
            String key = entry.getKey();
            List<String> values = entry.getValue();
            tmpMap.put(key, values);
            i++;
            if(i % num == 0) {
                p++;
                String parentKey = parentKeyPref + p;
                resMap.put(parentKey, tmpMap);
                //tmpMap.clear();
                tmpMap = new LinkedHashMap();
            }
        }
        
        if(!tmpMap.isEmpty()) { // add remains.
           p++; 
           String parentKey = parentKeyPref + p;
           resMap.put(parentKey, tmpMap);
        }
       
        String str = GeneralUtils.prettyPrintMapMap(resMap);
        System.out.println(str);
        
        return resMap;

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
        PrepareLuisCorpora pc = new PrepareLuisCorpora();
    }

}
