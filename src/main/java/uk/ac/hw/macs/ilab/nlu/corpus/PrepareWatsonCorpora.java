/**
 * ************************************************************
 * @(#)PrepareWatsonCorpora.java
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import static uk.ac.hw.macs.ilab.nlu.corpus.PreprocessRealData.CONFIG_FILE;
import uk.ac.hw.macs.ilab.nlu.util.GeneralUtils;

/**
 * To generate Watson NLU input data: one merged file for intent, one for entity.
 *
 *
 * @author X.Liu, V.Rieser
 */
public class PrepareWatsonCorpora {
    
    String destRootDirTop = ".";
    int numKFold = 0; // 0: do not create data for cross validation. Overwritten by config file
    
    public PrepareWatsonCorpora() {

        loadConfig();
        
         // Step0: get related file info
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
         String destBaseDir = destRootDirTop + "/preprocessResults/out4WatsonReal/Watson_" + timeStamp + "_" + fileNameLabel;
         
        List<String> nameInfo = processForOneTrainset(srcTrainsetBaseDir, destBaseDir, 
            timeStamp, fileNameLabel, 0 );
        
        // true for appending. null for using default encoding.
        GeneralUtils.saveToTextFile(nameInfoFileDir, nameInfoFile, nameInfo, true, null);    

        if(numKFold != 0) {
            String srcTaskBaseDir = geneBaseDir;
            String destTaskBaseDir = destBaseDir;
            nameInfo = processForCrossValidation(srcTaskBaseDir, destTaskBaseDir,timeStamp);           
        }
        
    }

    private  List<String> processForOneTrainset(String srcTrainsetBaseDir, String destBaseDir, 
            String timeStamp, String fileNameLabel, int numKFold ) {
        // timeStamp is not used here but keep it for consistency with other service and potential later use.
        // e.g. trainsetBaseDir: geneBaseDir + DomainIntentNoDupShuffleSubset_Train
        // destBaseDir0: preprocessResults/out4RasaReal/rasa_json_" + timeStamp + "_" + fileNameLabel;
        // For Normal: destBaseDir = destBaseDir0
        // For cross-validation destBaseDir = destBaseDir0+CrossValidation/KFold_i/
        String destSepaBaseDir = destBaseDir + "/separated";
        GeneralUtils.checkAndCreateDirectory(destSepaBaseDir);

        String destMergedBaseDir = destBaseDir + "/merged";
        GeneralUtils.checkAndCreateDirectory(destMergedBaseDir);

        String destDirIntent = destSepaBaseDir + "/intents";
         GeneralUtils.checkAndCreateDirectory(destDirIntent);
        
         // Step 1: generate Watson intents
         createWatsonSeparatedIntents_AllDomain_RealAnno(srcTrainsetBaseDir, destDirIntent);

        // Step2: to merge watson intents
         String mergeSrcDir = destDirIntent;
         String outPath = destMergedBaseDir + "/intents";
         GeneralUtils.checkAndCreateDirectory(outPath);
         
         String mergedIntentsFile = mergeWatsonIntentsFiles(mergeSrcDir, outPath, timeStamp);

        // Step3: generate Watson entities -- merged all domain-intents
        String destDirEntity = destMergedBaseDir + "/entities";
        GeneralUtils.checkAndCreateDirectory(destDirEntity);        
        String entityFile = "watsonEntities_" + timeStamp + ".csv";
        String mergedEntitiesFile = createWatsonEntities_AllDomain_RealAnno(srcTrainsetBaseDir, 
                destDirEntity,entityFile);

                
        List<String> nameInfo = new ArrayList<>();
        if( numKFold == 0) { // for nomal process---Non CrossValidation
            String item = "prepareWatsonOutDir=" + destBaseDir;
            nameInfo.add("");
            nameInfo.add("# ---- Watson ----");
            nameInfo.add("currentTime = " + GeneralUtils.getTimeStamp4Logging() );

            nameInfo.add(item);

            item = "prepareWatsonMergedIntents=" + mergedIntentsFile;
            nameInfo.add(item);    

            item = "prepareWatsonMergedEntities=" + mergedEntitiesFile;
            nameInfo.add(item);    

            nameInfo.add(""); 

            item = "watsonActuallyUsedTrainset=" + fileNameLabel;
            nameInfo.add(item);    

            nameInfo.add(""); 
        } else {
            //todo
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
            //String testsetBaseDir = srciFoldDir + "/testset";
            String destBaseDir = destCvBaseDir + "/"+ kFoldName;
            String fileNameLabel = kFoldName; // KFold_1, KFold_2 etc.
            //String testsetAnnoDir = testsetBaseDir + "/annotated";
            
            //For cross-validation destBaseDir = destTaskBaseDir+CrossValidation/KFold_i/
            processForOneTrainset(trainsetBaseDir, destBaseDir, timeStamp,
              fileNameLabel, 1 );// here last param can be anything > 0
        }
        
        List<String> nameInfo = new ArrayList<>();
        return nameInfo; //TODO add something
    }
    
    private void createWatsonSeparatedIntents_AllDomain_RealAnno(String srcBaseDir, 
            String destBaseDir) {
       
        List<String> fileNames = GeneralUtils.getAllFileNames(srcBaseDir);
        
        System.out.println("total num:" + fileNames.size());

        for (int i = 0; i < fileNames.size(); i++) {
            String fname = fileNames.get(i);

            String uttFileUri = srcBaseDir + "/" + fname;
            String intent = fname.substring(0, fname.lastIndexOf("."));
            String domainName = intent;
            createWatsonIntents_OneDomain_RealAnno(domainName, intent, uttFileUri, destBaseDir);
        }
               
    }

    /**
     * generate Watson intents for one domain
     * 
     * @param domainName
     * @param intent
     * @param uttFile
     * @param totalUttTopLimit
     * @param ratioForTrain
     * @param outPath 
     */
    public void createWatsonIntents_OneDomain_RealAnno(String domainName,  String intent, String uttFile, 
              String outPath)  {        
 //         
        // In real data file, file name is the intent name
        // and trainset have been already preprocessed to a whole set to use.
        // (also same for testset)
        List<String> annoUtts = IntEntUtils.getColumnContentFromCsv(uttFile, 
                IntEntUtils.Col_AnswerAnnotation,true);
        
        List<String> plainUtts = IntEntUtils.getColumnContentFromCsv(uttFile, 
                IntEntUtils.Col_AnswerFromAnno,true);
               
        List<String> wIntents = new ArrayList();
        for (int i = 0; i < annoUtts.size(); ++i) {
           //String annoUtt = annoUtts.get(i);
           String plainUtt = plainUtts.get(i);
           String wUtt = plainUtt.replaceAll(",", " "); // Watson intents complains comma
           wUtt = wUtt.replaceAll("  ", " "); // double spaces to one space
           String wLine = wUtt +"," + intent;
           wIntents.add(wLine);
        }
        
        // destBaseDir + "separated/intents" 
        String wIntentsFileUri = outPath +"/" + "WatsonNluIntents_" + domainName + ".csv";
        GeneralUtils.saveToFile(wIntentsFileUri, wIntents);
        
    }
    
    /**
     * Return saved merged file URI.
     * 
     * @param fileDir file directory contains separated json files.
     * 
     * @return String of saved file URI.
     */
    public String mergeWatsonIntentsFiles(String srcBaseDir, String outPath,
            String timeStamp) {
        
        //  srsBasedDir: basedDir + "separated/intents"
        List<String> fileNames = GeneralUtils.getAllFileNames(srcBaseDir);
        
        List<String> allIntents = new ArrayList();
        
        for (int i = 0; i < fileNames.size(); i++) {
            String fileUri = srcBaseDir + "/" + fileNames.get(i);
            List<String> oneDomain = GeneralUtils.readFromTextFile(fileUri);
            allIntents.addAll(oneDomain);
        }
        
        
        String wMergedFileUri = outPath +"/" + "WatsonNluIntentsMerged_" + timeStamp + ".csv";
        GeneralUtils.saveToFile(wMergedFileUri, allIntents);
        
        return wMergedFileUri;
    }
    
    public String createWatsonEntities_AllDomain_RealAnno(String srcBaseDir,  String destBaseDir,
            String uttFile)  { 

        Map<String, List<String>> entMap = IntEntUtils.getEntityValueMap_AllDomain_RealAnno(srcBaseDir);
        List<String> wLines = new ArrayList();
            for (Map.Entry<String, List<String>> entry : entMap.entrySet()) {
                String key = entry.getKey();
                List<String> values = entry.getValue();
                List<String> entVals = new ArrayList();
                for(int i =0; i < values.size(); ++i) {
                    entVals.add(key + "," + values.get(i)); // Watson format: entity, value, synonym1,2,3...
                }
                wLines.addAll(entVals);
            }
        // save to file "WatsonNluEntities_" + dddd + ".csv";
        // the entities file is actually merged file. in destBaseDir + "/merged/entities"
        
        String wEntitiesFileUri = destBaseDir +"/" + uttFile;
        
        GeneralUtils.saveToFile(wEntitiesFileUri, wLines);
        return wEntitiesFileUri;
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
        PrepareWatsonCorpora pc = new PrepareWatsonCorpora();
    }

}
