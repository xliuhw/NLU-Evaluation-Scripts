/**
 * ************************************************************
 * @(#)IntEntUtils.java
 *
 *
 *              Copyright(C) 2016 iLab MACS
 *              Heriot-Watt University
 *
 *                  All rights reserved
 *
 *   Version: 0.1  Created on 01 March 2017
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import uk.ac.hw.macs.ilab.nlu.util.Pair;
import uk.ac.hw.macs.ilab.nlu.util.GeneralUtils;

/**
 * Utilities for Intents/Entities manipulations
 *
 * @author X.Liu, V.Rieser
 */
public class IntEntUtils {

    public static final String ServiceName_RASA = "RASA";
    public static final String ServiceName_APIAI = "APIAI";
    public static final String ServiceName_LUIS = "LUIS";
    public static final String ServiceName_WATSON = "Watson";    

    
    public static final String Col_AnswerAnnotation = "answer_annotation";
    public static final String Col_AnswerFromAnno = "answer_from_anno";
    public static final String Col_SuggestedEntities = "suggested_entities";
    public static final String Col_Scenario = "scenario";
    public static final String Col_Intent = "intent";
    public static final String Col_Status = "status"; // info from Annotators.
    public static final String Col_Answerid = "answerid";

    public static final String TAB = ";";
    
    public static String Tag_IRR = "IRR"; // e.g. "IRR_XL" "IRR_XR". Will be overwriten by config file.


    public static String getHeaderStrOfCsv(String csvURI) {
        List<String> csvlines = GeneralUtils.readFromTextFile(csvURI);
        return csvlines.get(0);
    }

    public static String[] getHeadersOfCsv(String csvURI) {
        List<String> csvlines = GeneralUtils.readFromTextFile(csvURI);
        String[] headers = csvlines.get(0).split(";");
        for (int i = 0; i < headers.length; ++i) {
            String h = headers[i];
            if (h.startsWith("\"")) {
                h = h.substring(1);
            }
            if (h.endsWith("\"")) {
                h = h.substring(0, h.length() - 1);
            }
            headers[i] = h;
        }
        return headers;
    }

    public static boolean checkCsvContainHeader(String csvURI, String header) {
        List<String> csvlines = GeneralUtils.readFromTextFile(csvURI);

        String[] headers = csvlines.get(0).split(";");
        List<String> headerArr = new ArrayList<String>(Arrays.asList(headers));
        if (headerArr.contains(header)) {
            return true;
        } else {
            return false;
        }

    }

    public static List<Map<String, String>> parseCSVFile(String csvURI) {
        // logger.debug("raw csvURI:" + csvURI);

        List<String> csvlines = GeneralUtils.readFromTextFile(csvURI);

        List<Map<String, String>> csvRowMapList = new ArrayList<>();

        String[] headers = csvlines.get(0).split(";");

        for (int i = 1; i < csvlines.size(); ++i) {
            String line = csvlines.get(i);
            String[] row_toks = GeneralUtils.splitWithCommaNotInsideQuote(line, ";");
            //System.out.println("row_toks size:" + row_toks.length);
            if (row_toks.length != headers.length) {
                System.out.println("i:" + i + ", line:" + line);
            }

            if (row_toks.length > headers.length) {
                // check if it is wrong split due to delimiters inside the text or not quoted.
                // assume headers do not have this issues because each header is one word.
                System.out.println("WARNING, row_toks size:" + row_toks.length);
                System.out.println("WARNING, Wrong Split:" + i + "=" + line);
            }
            Map<String, String> row_map = new LinkedHashMap<>();

            for (int j = 0; j < headers.length; ++j) {
                String header = headers[j].trim();
                if (header.startsWith("\"")) {
                    header = header.substring(1);
                }
                if (header.endsWith("\"")) {
                    header = header.substring(0, header.length() - 1);
                }
                String tok = row_toks[j].trim();
                if (tok.startsWith("\"")) {
                    tok = tok.substring(1);
                }
                if (tok.endsWith("\"")) {
                    tok = tok.substring(0, tok.length() - 1);
                }

                // convert scenario name and intent name to lower case
                // for others, like user's original answer, keep them unchanged.
                if (header.equals(IntEntUtils.Col_Scenario) || header.equals(IntEntUtils.Col_Intent)) {
                    tok = tok.toLowerCase();
                }

                row_map.put(header.trim(), tok.trim());
            }

            csvRowMapList.add(row_map);

        }
        return csvRowMapList;
    }

    
    /**
     * Get the line map from the plain utt which e.g. may come from testset.
     * TODO check the testset generation has used same way to get the plain utt
     * @param rawCsvMapList
     * @return Map of <Header, Value> for one line of utterance
     */
    //public static Map<String, String> getLineMapByPlainUtt(List<Map<String, String>> rawCsvMapList, String utt) {
    public static int getLineMapByPlainUtt(List<Map<String, String>> rawCsvMapList, String utt) {
        Map<String, String> resMap = null;
        for(int i = 0; i < rawCsvMapList.size(); ++i) {
            Map<String, String>  linemap = rawCsvMapList.get(i);
            String annoUtt = linemap.get(Col_AnswerAnnotation);
            String plainUtt = getPlainUttFromAnnoUtt(annoUtt);
            if(plainUtt.equalsIgnoreCase(utt))  {
                
                //resMap = linemap;
                //break;
                return i;
            }
        }
        return -1;
    }
    
    
    public static List<String> getAnnotatedUttsFromCsv(String csvUri) {
        List<String> annoList = getColumnContentFromCsv(csvUri, Col_AnswerAnnotation, true);
        return annoList;
    }

    public static List<String> getPlainUttsFromCsv(String csvUri) {
        List<String> annoList = null;
       // if (checkCsvContainHeader(csvUri, Col_AnswerFromAnno)) {
       //     annoList = getColumnContentFromCsv(csvUri, Col_AnswerFromAnno, true);
       // } else {
            annoList = getPlainUttAllFromAnnoUttAll(csvUri);
        //}

        return annoList;
    }

    /**
     * Get plain an utt from annotated utt.
     *
     * @param annoUtt
     * @return
     */
    public static String getPlainUttFromAnnoUtt(String annoUtt) {
        // java regex: \S ==> A non-whitespace character: [^\s]
        String regex = "\\[\\s*\\S+\\s*:"; // for [person_phone: etc.

        String plainUtt = annoUtt.replaceAll(regex, "");
        plainUtt = plainUtt.replaceAll("\\]", "");

        // normalize
        // replace all double or more spaces and apostrophe to just one.
        plainUtt = GeneralUtils.replaceMoreSepcialCharsToOne(plainUtt, " ");
        plainUtt = GeneralUtils.replaceMoreSepcialCharsToOne(plainUtt, "'");
        plainUtt = plainUtt.trim();

        return plainUtt;
    }

    /**
     * Get all plain utt list from annotated answer list.
     *
     * @param csvUri
     * @return
     */
    public static List<String> getPlainUttAllFromAnnoUttAll(String csvUri) {
        List<String> annoList = getColumnContentFromCsv(csvUri, Col_AnswerAnnotation, true); // true consider IRR_XX
        List<String> plainList = new ArrayList<>();
        for (int i = 0; i < annoList.size(); ++i) {
            String annoUtt = annoList.get(i);
            String plainUtt = getPlainUttFromAnnoUtt(annoUtt);
            plainList.add(plainUtt);
        }
        return plainList;
    }

    /**
     * Get content of the header column name specified. Pure content, no header
     * in the returned list.
     *
     * @param csvUri, string of csv URI.
     * @param columnName, string of column name (i.e. header name)
     *
     * @return List of string of all content lines.
     */
    public static List<String> getColumnContentFromCsv(String csvUri, String columnName, boolean checkStatusCol) {

        List<Map<String, String>> dataMap = parseCSVFile(csvUri); // pure data, no header line
        List<String> colContents = new ArrayList<>();
        for (int i = 0; i < dataMap.size(); ++i) {
            Map<String, String> lineMap = dataMap.get(i);
            if (checkStatusCol && !columnName.equals(IntEntUtils.Col_Status)) {
                String status = lineMap.get(IntEntUtils.Col_Status);
                if (status != null && status.toLowerCase().startsWith(IntEntUtils.Tag_IRR.toLowerCase())) {
                    continue; // if status starts with "IRR_"
                }
            }

            String colContent = "";
            if (lineMap.get(columnName) != null) {
                colContent = lineMap.get(columnName).trim();
            }

            colContents.add(colContent);
        }
        return colContents;
    }

    /**
     * Get the entity names from annotated utterance, keep it if duplicated.
     *
     * @param annoUtt string of annotated utterance
     * @return a list of entity names
     */
    public static List<String> getEntityNamesFromAnnoInUtt(String annoUtt) {
        List<Pair<String, String>> listPairs = getEntityValuesFromAnnoInUtt(annoUtt);
        List<String> entities = new ArrayList<>();
        for (int i = 0; i < listPairs.size(); ++i) {
            entities.add(listPairs.get(i).getElement0());
        }
        return entities;
    }

    public static List<Pair<String, String>> getEntityValuesFromAnnoInUtt(String annoUtt) {
        // java regex: \S ==> A non-whitespace character: [^\s]
        String regex = "\\[\\s*(\\S+)\\s*:\\s*([^\\]]+)\\s*\\]";
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(annoUtt);
        List<Pair<String, String>> entValList = new ArrayList<>();
        while (m.find()) {
            String entity = m.group(1).trim();
            String value = m.group(2).trim();
            // the annotation required: [person: john's]
            // remove astrophe, e.g. john's, son's. the tokenizer will also consider it when processing plain utt 
            if (value.endsWith("'s")) {
                value = value.substring(0, value.length() - 2);
                value = value.trim();
            }

            entValList.add(new Pair(entity, value));
        }

        return entValList;
    }

    /**
     * Get the char index of whole annotated values from original utt.
     *
     * @param annoUtt annotated utt
     * @return list of all index of annotated values.
     */
    public static List<Integer> getCharIndexOfAnnoValuesInUtt(String annoUtt, String origUtt) {

        List<Pair<String, String>> entVals = getEntityValuesFromAnnoInUtt(annoUtt);

        int crntTokIdx = 0;
        List<Integer> idxs = new ArrayList<>();
        for (int i = 0; i < entVals.size(); ++i) {
            String val = entVals.get(i).getElement1();
            // values shouldn't contain punctuations, already processed.
            //String[] valArr = val.split("\\s+|\\.|\\!|\\?"); // not contains 's in entVals
            List<String> valToks = tokenizer(val);
            // the entity value list from annoUtt is in order, get the first 
            // tokens match starting from current position is the right index.
            //System.out.println("val:" + val + ",crnt tok idx:" + crntTokIdx);
            int charIdx = getIndexOfTokensFromString(origUtt, val, crntTokIdx, true);
            //int idx = 0;
            idxs.add(charIdx);
            int end = charIdx + val.length(); //NB this end is idxOfLastChar + 1
            //System.out.println("val:" + val + ", char start:" + charIdx + ", char end:" + end);
            int tokIdx = getIndexOfTokensFromString(origUtt, val, crntTokIdx, false);

            //crntTokIdx = tokIdx + valArr.length;
            crntTokIdx = tokIdx + valToks.size();
            //System.out.println("tokIdx:" + tokIdx + ", crntTokIdx:" + crntTokIdx );            
        }
        return idxs;
    }

    /**
     * Get the starting index of tokens of words in the given utterance.
     *
     * @param utt the string of given utterance
     * @param words string of words which may have multiple words (tokens)
     * @param start int of starting point in utterance to check.
     * @param getCharIndex, true to get the character index, false for token
     * index (number of tokens)
     * @return the char index or token index.
     *
     */
    public static int getIndexOfTokensFromString(String utt, String words, int start, boolean getCharIndex) {
        List<String> uttList = tokenizer(utt);
        //System.out.println("getIndexOfTo, utt list:" + uttList.toString());
        List<String> wordsList = tokenizer(words);
        //System.out.println("getIndexOfTo, word list:" + wordsList.toString());
        if (uttList.size() < wordsList.size()) {
            return -1;
        }

        int end = uttList.size() - wordsList.size() + 1;
        for (int i = start; i < end; ++i) {
            boolean allTokensEQ = true;
            for (int j = 0; j < wordsList.size(); ++j) {
                if (!uttList.get(i + j).equals(wordsList.get(j))) {
                    allTokensEQ = false;
                    break;
                }
            }
            if (allTokensEQ) {
                int idx = 0;
                //System.out.println("i=" + i + ", utt list:" + uttList.toString());
                for (int k = 0; k < i; ++k) {
                    // apostrophe inside word was split but there is no space in original word
                    int wordAndSpaceLen = uttList.get(k).length() + 1;
                    if (uttList.get(k).contains("'")
                            // consider other punctuations like ,.?!
                            // e.g. I'll take my coffee, black. assume there is no space between word and punct
                            || uttList.get(k).equals(",") || uttList.get(k).equals(".")
                            || uttList.get(k).equals("?") || uttList.get(k).equals("!")) { // TODO make it generic for all possible punctuations.
                        wordAndSpaceLen = uttList.get(k).length();
                    }

                    //idx += uttList.get(k).length() + 1; // assume no double space in utterance
                    idx += wordAndSpaceLen;
                }
                // utt contains all tokens of words
                if (getCharIndex) {
                    return idx; // return char index: number of chars/letters
                } else {
                    return i; // return token index: number of tokens
                }
            }
        }
        return -1;

    }

    /**
     * Get the starting index of tokens of words in the given utterance.
     *
     * @param utt the string of given utterance
     * @param words string of words which may have multiple words (tokens)
     * @param start int of starting point in utterance to check.
     * @param getCharIndex, true to get the character index, false for token
     * index (number of tokens)
     * @return the char index or token index.
     *
     */
    public static int getIndexOfTokensFromString_OLD(String utt, String words, int start, boolean getCharIndex) {
        String[] uttArr = utt.split("\\s+|\\.|\\!|\\?|'"); // apostrophe
        //System.out.println("Utt Tokens:" + Arrays.toString(uttArr));
        String[] wordsArr = words.split("\\s+|\\.|\\!|\\?");
        if (uttArr.length < wordsArr.length) {
            return -1;
        }

        int end = uttArr.length - wordsArr.length + 1;
        for (int i = start; i < end; ++i) {
            boolean allTokensEQ = true;
            for (int j = 0; j < wordsArr.length; ++j) {
                if (!uttArr[i + j].equals(wordsArr[j])) {
                    allTokensEQ = false;
                    break;
                }
            }
            if (allTokensEQ) {
                int idx = 0;
                for (int k = 0; k < i; ++k) {
                    idx += uttArr[k].length() + 1; // assume no double space in utterance
                }
                // utt contains all tokens of words
                if (getCharIndex) {
                    return idx; // return char index: number of chars/letters
                } else {
                    return i; // return token index: number of tokens
                }
            }
        }
        return -1;

    }

    public static Map<String, List<String>> getEntityValueMap_OneDomain_RealAnno(String annoCsvUri) {
        Map<String, List<String>> entityValueMap = new LinkedHashMap<>();

        List<String> annoUtts = getColumnContentFromCsv(annoCsvUri, Col_AnswerAnnotation, true); // true consider IRR_xxx
        for (int i = 0; i < annoUtts.size(); ++i) {
            List<Pair<String, String>> entValList = getEntityValuesFromAnnoInUtt(annoUtts.get(i));
            for (int j = 0; j < entValList.size(); ++j) {
                String entity = entValList.get(j).getElement0();
                String value = entValList.get(j).getElement1();
                List<String> values = entityValueMap.get(entity);
                if (values == null) {
                    values = new ArrayList<>();
                    entityValueMap.put(entity, values);
                }
                if (!values.contains(value)) {
                    values.add(value);
                }
            }
        }
        return entityValueMap;
    }

    /**
     * To get all Entity Values from a set of DomainIntent files in the
     * specified direcory. The entity values will be merged from each
     * DomainIntent csv file. Duplicated values are ignored.
     *
     * This is equivalent to call getEntityValueMap_OneDomain_RealAnno with
     * original one full csv file.
     *
     * @param srcBaseDir the src dir containing all DomainIntent csv files.
     *
     * @return the map of EntityName--Values.
     */
    public static Map<String, List<String>> getEntityValueMap_AllDomain_RealAnno(String srcBaseDir) {

        Map<String, List<String>> evMapAll = new LinkedHashMap<>();

        List<String> fileNames = GeneralUtils.getAllFileNames(srcBaseDir);
        //List<String> allEntities = new ArrayList<>();
        for (int i = 0; i < fileNames.size(); ++i) {
            String url = srcBaseDir + "/" + fileNames.get(i);
            Map<String, List<String>> domainMap = getEntityValueMap_OneDomain_RealAnno(url);

            for (Map.Entry<String, List<String>> entry : domainMap.entrySet()) {
                String key = entry.getKey();
                List<String> domainValues = entry.getValue();
                List<String> valuesAll = evMapAll.get(key);
                if (evMapAll.containsKey(key)) {
                    // merge values list
                    Set<String> domainSetValues = new HashSet(domainValues);
                    Set<String> allSet = new HashSet(valuesAll);
                    allSet.addAll(domainSetValues);  // so exlcude the same values
                    List<String> allList = new ArrayList(allSet);
                    evMapAll.put(key, allList); // will replace old values list
                } else {
                    evMapAll.put(key, domainValues);
                }

            }
        }

        return evMapAll;
    }

    public static List<String> extractScenarioIntent(String csvUri, boolean scenarioOnly, Integer maxNum) {

        List<String> infoList = new ArrayList<>();
        infoList.add("==== IntEntUtils.extractScenarioIntent ====");
        infoList.add("scenarioOnly=" + scenarioOnly);

        List<Map<String, String>> rawData = parseCSVFile(csvUri);
        // headers: answerid, userid, scenario, intent, mode, answer,notes, question
        String msg = "process for annotation, List<Map> size:" + rawData.size();
        System.out.println(msg);
        infoList.add(msg);

        Set<String> sceIntents = new HashSet<>();
        int maxSize = rawData.size();
        if (maxNum != null) {
            maxSize = maxNum;
        }

        infoList.add("maxSize used for processing:" + maxSize);

        for (int i = 0; i < maxSize; ++i) {
            Map<String, String> lineMap = rawData.get(i);

            String status = lineMap.get(IntEntUtils.Col_Status);
            if (status != null && status.toLowerCase().startsWith(IntEntUtils.Tag_IRR.toLowerCase())) {
                continue;
            }

            String scenario = lineMap.get("scenario").trim();

            String intent = lineMap.get("intent").trim();

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

            String sceIntent = scenario + "_" + intent;
            if (scenarioOnly) {
                sceIntent = scenario;
            }

            sceIntents.add(sceIntent);

        }

        TreeSet ts = new TreeSet(sceIntents); // sorted
        String msg1 = "domain/intents size:" + ts.size();
        String msg2 = "domain/intents:" + ts.toString();
        System.out.println(msg1);
        System.out.println(msg2);
        infoList.add(msg1);
        infoList.add(msg2);

        return infoList;
    }

    /**
     * Extracted unique content of col1 separated by co2 and/or col3 e.g.
     * extract suggested_entities separated by scenario/intent
     *
     * @param srcAnnoCsvUrl
     * @param col1, column name whose contents to be extracted
     * @param col2, column based on to separate col1, not null.
     * @param col3, second based column, could be null.
     *
     */
    public static Map<String, List<String>> extractUniqueContentOfCol_Sep(String srcAnnoCsvUrl, String col1, String col2, String col3) {

        List<Map<String, String>> rawData = IntEntUtils.parseCSVFile(srcAnnoCsvUrl);

        Map<String, List<String>> sceAnsMap = new LinkedHashMap<>();

        List<String> ansList = null;

        for (int i = 0; i < rawData.size(); ++i) {
            Map<String, String> lineMap = rawData.get(i);

            String key = "";
            key = lineMap.get(col2).trim();
            if (col3 != null) {
                key += "_" + lineMap.get(col3).trim();
            }

            ansList = sceAnsMap.get(key);
            if (ansList == null) {
                ansList = new ArrayList<>();
                sceAnsMap.put(key, ansList);
            }

            String sugg = lineMap.get(col1).trim();
            // some contains more
            String[] suggs = sugg.split(",");
            for (int j = 0; j < suggs.length; j++) {
                // System.out.println("sugg ent:" + suggs[j].trim());
                String ent = suggs[j].trim();
                // "
                if (ent.startsWith("\"")) {
                    ent = ent.substring(1);
                }
                if (ent.endsWith("\"")) {
                    ent = ent.substring(0, ent.length() - 1);
                }
                if (!ansList.contains(ent)) {
                    ansList.add(ent);
                }
            }

        }
        return sceAnsMap;
    }

    public static Map<String, List<String>> extractUniqueEntities_Sep(String srcAnnoCsvUrl, String col1, String col2) {

        List<Map<String, String>> rawData = IntEntUtils.parseCSVFile(srcAnnoCsvUrl);

        Map<String, List<String>> sceAnsMap = new LinkedHashMap<>();

        List<String> ansList = null;

        for (int i = 0; i < rawData.size(); ++i) {
            Map<String, String> lineMap = rawData.get(i);

            String status = lineMap.get(IntEntUtils.Col_Status);
            if (status != null && status.toLowerCase().startsWith(IntEntUtils.Tag_IRR.toLowerCase())) {
                continue;
            }

            String key = "";
            key = lineMap.get(col1).trim();
            if (col2 != null) {
                key += "_" + lineMap.get(col2).trim();
            }

            ansList = sceAnsMap.get(key);
            if (ansList == null) {
                ansList = new ArrayList<>();
                sceAnsMap.put(key, ansList);
            }

            String annoUtt = lineMap.get(Col_AnswerAnnotation).trim();

            List<Pair<String, String>> entValInUtt = getEntityValuesFromAnnoInUtt(annoUtt);
            for (int k = 0; k < entValInUtt.size(); ++k) {
                String entity = entValInUtt.get(k).getElement0();
                if (!ansList.contains(entity)) {
                    ansList.add(entity);
                }
            }

        }
        return sceAnsMap;
    }

    public static void extractUniqueContentOfCol(String csvUri, String columnName) {

        List<Map<String, String>> rawData = parseCSVFile(csvUri);
        // headers: answerid, userid, scenario, intent, mode, answer,notes, question
        System.out.println("process for annotation, List<Map> size:" + rawData.size());

        Set<String> entNames = new HashSet<>();
        for (int i = 0; i < rawData.size(); ++i) {
            Map<String, String> lineMap = rawData.get(i);
            //String sugg = lineMap.get("suggested_entities").trim();
            String sugg = lineMap.get(columnName).trim();
            // some contains more
            String[] suggs = sugg.split(",");
            for (int j = 0; j < suggs.length; j++) {
                // System.out.println("sugg ent:" + suggs[j].trim());
                String ent = suggs[j].trim();
                // "
                if (ent.startsWith("\"")) {
                    ent = ent.substring(1);
                }
                if (ent.endsWith("\"")) {
                    ent = ent.substring(0, ent.length() - 1);
                }

                entNames.add(ent);
            }

        }

        TreeSet ts = new TreeSet(entNames); // sorted
        System.out.println("Extracted " + columnName + " size:" + ts.size());
        System.out.println("Extracted " + columnName + ":" + ts.toString());
    }

    /**
     * From multiple domain-with-intent files to get all unique entity names
     *
     */
    public static List<String> extractUniqueEntitiesFromAnno_AllDomain(String srcBaseDir) {
        //String srcBaseDir = "resources/inputdataReal/dataset2/preprocess/autoGeneFromAnno/DomainsIntentsShuffle";
        List<String> fileNames = GeneralUtils.getAllFileNames(srcBaseDir);
        List<String> allEntities = new ArrayList<>();
        for (int i = 0; i < fileNames.size(); ++i) {
            String url = srcBaseDir + "/" + fileNames.get(i);
            List<String> entities = IntEntUtils.extractUniqueEntities_OneDomain(url); // entities in one domain
            System.out.println("entities in " + fileNames.get(i) + ":" + entities.toString() + "\n");
            for (int j = 0; j < entities.size(); ++j) {
                if (!allEntities.contains(entities.get(j))) {
                    allEntities.add(entities.get(j));
                }
            }
        }

        System.out.println("Final all entities:" + allEntities.toString());
        return allEntities;
    }

    /**
     * From one domain-with-intent file to get all unique entity names
     *
     */
    public static List<String> extractUniqueEntities_OneDomain(String csvUri) {

        List<String> annoUtts = getAnnotatedUttsFromCsv(csvUri);

        return extractUniqueEntities_OneDomain(annoUtts);
    }

    public static Map<String, Integer> extractUniqueEntitiesCounts_OneDomain(String csvUri) {

        List<String> annoUtts = getAnnotatedUttsFromCsv(csvUri);

        return extractUniqueEntitiesCounts_OneDomain(annoUtts);
    }

    public static List<String> extractUniqueEntities_OneDomain(List<String> annoUtts) {

        List<String> entities = new ArrayList<>();

        for (int j = 0; j < annoUtts.size(); ++j) {
            List<Pair<String, String>> entValInUtt = getEntityValuesFromAnnoInUtt(annoUtts.get(j));
            for (int k = 0; k < entValInUtt.size(); ++k) {
                String entity = entValInUtt.get(k).getElement0();
                if (!entities.contains(entity)) {
                    entities.add(entity);
                }
            }
        }
        Collections.sort(entities);
        //System.out.println("EntityNames:" + Arrays.toString(entities.toArray()));

        return entities; // list of entity names
    }

    public static Map<String, Integer> extractUniqueEntitiesCounts_OneDomain(List<String> annoUtts) {

        List<String> entities = new ArrayList<>();
        Map<String, Integer> entCounts = new LinkedHashMap();

        for (int j = 0; j < annoUtts.size(); ++j) {
            List<Pair<String, String>> entValInUtt = getEntityValuesFromAnnoInUtt(annoUtts.get(j));
            for (int k = 0; k < entValInUtt.size(); ++k) {
                String entity = entValInUtt.get(k).getElement0();
                if (!entities.contains(entity)) {
                    entities.add(entity);
                    entCounts.put(entity, 1);
                } else {
                    Integer count = entCounts.get(entity);
                    count += 1;
                    entCounts.put(entity, count);
                }
            }
        }
        Collections.sort(entities);
        System.out.println("Entitiy size:" + entities.size());
        System.out.println("EntitiyNames:" + Arrays.toString(entities.toArray()));

        return entCounts;
    }

    /**
     * Get the entity JsonArray from the utterance and the dictionary.
     *
     * @param utt string of the utterance
     * @param keyEntNames, dictionary-- entity-values map, e.g. slot-values for
     * a specific domain.
     *
     * @return The JsonArray of entity Object.
     */
    //public static JsonArray getEntitiesOfUtt_RealAnno(String annoUtt, String plainUtt, String serviceName) {
    public static JsonArray getEntitiesOfUtt_RealAnno(String annoUtt, String plainUtt, String serviceName) {

        List<Pair<String, String>> entValList = IntEntUtils.getEntityValuesFromAnnoInUtt(annoUtt);

        List<Integer> charIdxes = IntEntUtils.getCharIndexOfAnnoValuesInUtt(annoUtt, plainUtt);
        String startLabel = "start";
        String endLabel = "end";
        String entityLabel = "entity";
        String valueLabel = "value";
        if (serviceName.equals(IntEntUtils.ServiceName_LUIS)) {
            startLabel = "startPos";
            endLabel = "endPos";
        }
        int crntTokIdx = 0; // used when it is for IntEntUtils.ServiceName_LUIS
        List<JsonObject> entityList = new ArrayList<>();
        for (int i = 0; i < entValList.size(); ++i) {
            String ent = entValList.get(i).getElement0();
            String val = entValList.get(i).getElement1();
            int start = charIdxes.get(i);
            int end = start + val.length();
            JsonObject entJO = new JsonObject();
            // e.g. "text": "show me chinese restaurants", 
            // "start": 8,  "end": 15,
            String entityName = ent;
            if (serviceName.equals(IntEntUtils.ServiceName_LUIS)) {

                if (PrepareLuisCorpora.createHierarchicalEnitiy) {
                    entityName = IntEntUtils.getHierarchicalKey(PrepareLuisCorpora.hierMapAllDomain, ent);
                }

                /*
                // get start point in number of tokens
               start = getIndexOfTokensFromString(plainUtt, val, crntTokIdx, false); // false for token index
               
               List<String> wordToks = tokenizer(val);

                int numWordToks = wordToks.size();  
                
                crntTokIdx = start + numWordToks; // crntTokIdx for next starting checking point.
                // Luis use token number as the index
                // e.g. "text": "show me chinese restaurants", 
                // "start": 2,  "end": 2,

                if (PrepareLuisCorpora.createHierarchicalEnitiy) {
                    entityName = IntEntUtils.getHierarchicalKey(PrepareLuisCorpora.hierMapAllDomain, ent);
                }
                
                // 15June2017, LUIS does not tokenize apostrophe
                // start, end and crntTokIdx above consider an apostrophe is a seprated token
                // now consider apostrophe tokenized
                // now merge apostrophes for LUIS. assume "start" is not an apostrophe
                int charIdxOfStart = 0;
                for(int k = 0; k < start; ++k) {
                    charIdxOfStart += uttToks.get(k).length() + 1; // 1 for space
                }
                //TODO use uttToksNoApos for dealing with apostrophe
                String startPart = plainUtt.substring(0, charIdxOfStart);
                int countApostro = startPart.replaceAll("[^']", "").length(); // num of apostr before "start"
                start = start - countApostro;
                int countApostroInVal = val.replaceAll("[^']", "").length(); // num of apostr in val
                // end index is inclusive in Luis
                end = start + numWordToks - 1 - countApostroInVal;
                // the current entity value does not include apostrophe, add it back for LUIS
                String aposVal = "";
                System.out.println("start=" + start + ", end=" + end);
                System.out.println("noAposTokens=" + uttToksNoApos.toString());
                for(int k = start; k <= end; ++k) {
                    aposVal += uttToksNoApos.get(k) + " ";
                }
                aposVal = aposVal.trim();
                val = aposVal;
                 */
                end = end - 1;
            }

            entJO.addProperty(startLabel, start); // from 0
            entJO.addProperty(endLabel, end);  // so last index char is not part of the value
            if (!serviceName.equals(IntEntUtils.ServiceName_LUIS)) {
                entJO.addProperty(valueLabel, val);
            }

            entJO.addProperty(valueLabel, val); // dup with above??

            entJO.addProperty(entityLabel, entityName);

            entityList.add(entJO);
        }

        // sort entity list by ascending order of entity start position in the utterance.
        Collections.sort(entityList, new CompareListJsonObject(startLabel));
        if (!checkEntityListOverlap(entityList, serviceName).isEmpty()) {
        //if (!checkEntityListOverlap(entityList).isEmpty()) {
            System.out.println("== Failed ==\n");
            System.out.println("\n== Utt:" + plainUtt);
            System.out.println("Entities:" + entityList.toString());
            System.exit(0);
        }

        JsonArray entJA = new JsonArray();
        for (int i = 0; i < entityList.size(); ++i) {
            JsonObject jo = entityList.get(i);
            entJA.add(jo);
        }

        return entJA;
    }

    /**
     * Return the string if overlap, otherwise empty string
     *
     * @param examJA examples JsonArray
     * @return string or empty
     */
    public static String checkEntityListOverlap(List<JsonObject> examJL, String serviceName) {

        if (examJL == null || examJL.size() < 2) {
            return "";
        }

        List<Integer> s_e = new LinkedList<>();
        String utt = "FakeUtt4LUIS";
        if (!serviceName.equals(IntEntUtils.ServiceName_LUIS)) {
            utt = examJL.get(0).get("value").getAsString();
        }

        for (int i = 0; i < examJL.size(); ++i) {

            JsonObject obj = examJL.get(i);

            int start;
            int end;
            if (serviceName.equals(IntEntUtils.ServiceName_LUIS)) {
                // and Luis uses number of tokens as index
                start = obj.get("startPos").getAsInt();
                end = obj.get("endPos").getAsInt();
            } else {
                start = obj.get("start").getAsInt();
                end = obj.get("end").getAsInt();
            }

            if (start < 0) {
                System.out.println("\n\n++++ Overlap ++++");
                System.out.println("\n++++ Something Wrong! start index is -1 ++++");
                System.out.println("\n Returned utt: " + utt + "\n\n");
                return utt;
            }

            s_e.add(start);
            s_e.add(end);

        }

        // check now
        for (int i = 0; i < s_e.size() - 1; i++) {
            int s = s_e.get(i).intValue();
            int e = s_e.get(i + 1).intValue();
            if ((serviceName.equals(IntEntUtils.ServiceName_LUIS) && (e < s))
                    || (!serviceName.equals(IntEntUtils.ServiceName_LUIS) && (e <= s))) {
                System.out.println("\n\n++++ Overlap ++++");
                System.out.println("\n start: " + s + ", end:" + e);
                System.out.println("\n first utt in the list returned: " + utt + "\n\n");
                return utt;
            } // overlap
        }

        return "";

    }
     
    /**
     * Call standard tokenizer but consider "'" the apostrophe for inside a word
     * e.g. "John's" to keep it if it is false
     *
     * @param utt to be tokenized.
     * @param splitAposInsideWord, true for separate it, false for keep it.
     *
     * @return list of tokens.
     */
    //Do not use this for now as this will also affect others like getIndexOfUtt...
    public static List<String> tokenizer(String utt, boolean splitAposInsideWord) {

        // excludingChars means do not separate them, keep them.
        // String punc = "[\\p{Punct}&&[^#@_" + excludingChars + "]]"; 
        // TODO add excludingChars as param, check the process for apostrophe.
        String punc = "[\\p{Punct}&&[^#@_]]";

        List<String> toks = null;

        toks = tokenizer(utt, punc, splitAposInsideWord);

        return toks;

    }

    public static List<String> tokenizer(String utt) {

        String punc = "[\\p{Punct}&&[^#@_]]"; // default tokens to use, excluding # @ and _

        List<String> toks = tokenizer(utt, punc, true); // defaut to separate apostrophe inside a word
        //List<String> toks = tokenizer(utt, punc);
        return toks;
    }

    /**
     * From prebuilt map of map: <HierKey, <NormalKey, ValueList>> get the
     * HierKey, given NormalKey. Used by LUIS.
     *
     * @param map, map of map
     * @param uttkey, string of normal key
     * @return String of Hier key.
     */
    public static String getHierarchicalKey(Map<String, Map<String, List<String>>> map, String uttkey) {
        String hierKey = "";
        boolean found = false;
        // LUIS hierarchical key format:  "entity": "hier1_food::foodcuisine",
        for (Map.Entry<String, Map<String, List<String>>> entry : map.entrySet()) {
            String pkey = entry.getKey(); // parent key
            Map<String, List<String>> valueMap = entry.getValue();
            for (Map.Entry<String, List<String>> cEntry : valueMap.entrySet()) {
                String ckey = cEntry.getKey(); // child key
                if (ckey.equals(uttkey)) {
                    hierKey = pkey + "::" + uttkey;
                    found = true;
                    break;
                }
            }
            if (found) {
                break;
            }
        }
        if (!found) {
            System.err.println("\n\n === Can not find the parent key for " + uttkey + "!!\n\n");
            hierKey = "Unknown";
        }

        return hierKey;
    }
    
    /**
     * Tokenize the utterance string for the named entity recognition. It is
     * mainly white-space based but preserve the punctuations and apostrophe
     * etc. which is similar to MITIE, Apache OpenNLP, NLTK and Stanford
     * tokenizer.
     *
     * MITIE examples:
     *
     * Input utterances:
     *
     * "I said, 'what're you? Crazy?'" said Sandowsky. "I can't afford to do
     * that." I said, 'what're you? Crazy?' said Sandowsky. I can't afford to do
     * that. I haven't called my mum recently. I need to go to work at 7
     * o'clock. what's tom's house? what's tom's house ? pda, tell your name!
     * Texas' weather Mr. Hastings' pen for goodness' sake. this is seven a.m.
     *
     * Tokenized: ['"', 'i', 'said', ',', "'what", "'re", 'you', '?', 'crazy',
     * '?', "'", '"', 'said', 'sandowsky', '.', '"', 'i', 'can', "'t", 'afford',
     * 'to', 'do', 'that', '.', '"'] Tokenized: ['i', 'said', ',', "'what",
     * "'re", 'you', '?', 'crazy', '?', "'", 'said', 'sandowsky', '.', 'i',
     * 'can', "'t", 'afford', 'to', 'do', 'that', '.'] Tokenized: ['i', 'haven',
     * "'t", 'called', 'my', 'mum', 'recently', '.'] Tokenized: ['i', 'need',
     * 'to', 'go', 'to', 'work', 'at', '7', 'o', "'clock", '.'] Tokenized:
     * ['what', "'s", 'tom', "'s", 'house', '?'] Tokenized: ['what', "'s",
     * 'tom', "'s", 'house', '?']
     *
     * Tokenized: ['texas', "'", 'weather'] Tokenized: ['mr', '.', 'hastings',
     * "'", 'pen'] Tokenized: ['for', 'goodness', "'", 'sake', * '.'] Tokenized:
     * ['this', 'is', 'seven', 'a.m.']
     *
     * More examples:
     *
     * "text": "what is mary s.'s birthday", RASA: [u'what', u'is', u'mary',
     * u's.', u"'s", u'birthday'] Ours: [what, is, mary, s., 's, birthday]
     *
     * "text": "tell me the time in g. m. t. plus five", RASA: [u'tell', u'me',
     * u'the', u'time', u'in', u'g.', u'm.', u't.', u'plus', u'five'] Ours:
     * [tell, me, the, time, in, g., m., t., plus, five]
     *
     * Ours: utt = "this is u. s. country."; [this, is, u., s., country, .] utt
     * = "this is u.s. country."; [this, is, u.s., country, .]
     *
     *
     * @param utt, string of input
     * @return String array of tokens.
     */
    public static List<String> tokenizer(String utt, String punc, boolean splitApostrophe) {
        // public static List<String> tokenizer(String utt, String punc) {
        /*
          1. \\s+ splits on a group of whitespace, so if the characters are whitespace characters, 
             we will remove those characters and split at that location. (note: I am assuming 
             that a string of hello  world should result in ["hello", "world"] 
             rather than ["hello", "", "world"])
          2. (?=\\p{Punct}) is a lookahead that splits if the next character is 
              a punctuation character, but it doesn't remove the character.
          3. (?<=\\p{Punct}) is a lookbehind that splits if the last character is 
              a punctuation character.        
          4. \\W\\p{Punct} matches a non-word character followed by a punctuation character. 
          5. \\p{Punct}\\W matches a punctuation character followed by a non-word character. 
             
            So each lookaround matches iff there is a punctuation character which is not 
            in the middle of a word.
          6. (?<=^\\p{Punct}) lookbehind to see if a punctuation is in the beginning of the utt,
             split it but keep the punctuation.
        
          7. lookahead to see if a punctuation is in the end of the utt, split and keep the punc.
        
        \p{Punct}	Punctuation: One of !"#$%&'()*+,-./:;<=>?@[\]^_`{|}~
         
        http://docs.oracle.com/javase/6/docs/api/java/util/regex/Pattern.html#cc
        
        [\p{Punct}&&[^()]] : punct except for ( )
        
         */
        //String regex = "\\s+|(?<=^\\p{Punct})|(?=\\p{Punct}$)|(?=\\W\\p{Punct}|\\p{Punct}\\W)|(?<=\\W\\p{Punct}|\\p{Punct}\\W})";        
        //String punc = "[\\p{Punct}&&[^#@_]]";
        String wp = "\\W" + punc;
        String pw = punc + "\\W";
        String reg1 = "\\s+|(?<=^" + punc + ")|(?=" + punc + "$)";
        String reg2 = "(?=" + wp + "|" + pw + ")";
        String reg3 = "(?<=" + wp + "|" + pw + ")";
        String regex = reg1 + "|" + reg2 + "|" + reg3;

        String[] uttArr = utt.split(regex);
        //System.out.println("Initial Regex Tokens:" + Arrays.toString(uttArr));
        // now deal with apostrophe inside a word.
        List<String> toks = new ArrayList<>();

        for (int i = 0; i < uttArr.length; ++i) {

            if (uttArr[i].trim().length() == 2) {
                String s1 = uttArr[i].trim().substring(0, 1);
                String s2 = uttArr[i].trim().substring(1, 2);
                // if both two chars are non-letter, separate them
                // e.g. [what, is, mary, s, .', s, birthday]
                if (!isLetter(s1) && !isLetter(s2)) {
                    toks.add(s1);
                    toks.add(s2);
                } else {
                    toks.add(uttArr[i].trim());
                }
                continue;
            }

            if (uttArr[i].trim().startsWith("'")) {
                // already separated apostrophe in utt due to irregular spelling,
                // e.g. "check john 's number"
                // Not check ending with apostrophe as it already separated by regex above.
                toks.add(uttArr[i]);
                continue;
            }

            //process apostrophe from "John's" to get "John" and "'s"
            // above regexp process does not separate punc inside a word, only separate
            // punc at before or end of the word. 
            String[] warr = uttArr[i].split("'");
            //System.out.println("w Tokens size:" + warr.length);
            if (warr.length == 1) {
                // remove empty token, e.g. like case where there are spaces 
                // between a word and punctuation which results in empty token
                // e.g. "what's tom's house ?" which normally shouldn't be a case.
                if (!(warr[0].trim().equals(""))) {
                    // in real process/analysis, should do the lemmatization
                    // to get the lemma e.g. "haven't" -- "haven" -- "have" (or "have not"?)
                    // "shouldn't" -- "shouldn" -- "should" (or "should not"?)
                    toks.add(warr[0]);
                }

            } else if (splitApostrophe) {
                toks.add(warr[0]);
                // assume only one apostrophe inside one word
                String apos = "'" + warr[1];
                toks.add(apos);
            } else {
                toks.add(uttArr[i]);
            }

        }

        // post process for sinlge char with dot e.g. to get "g. m. t."
        // this will align with RASA NLU's tokenization.
        // Haven't found better way to put this in the regular expression above.
        // check if ch is a letter
        List<String> newtoks = new ArrayList<>();
        boolean lastTokAdded = false;
        for (int i = 0; i < toks.size() - 1; ++i) {
            String tok1 = toks.get(i).trim();
            String tok2 = toks.get(i + 1).trim();
            if (tok1.equals("'") && tok2.length() == 1 && isLetter(tok2)) {
                // merge this two, e.g. get: "'s", "'t", "'d"
                newtoks.add(tok1 + tok2);
                if (i == toks.size() - 2) // check for last token.
                {
                    lastTokAdded = true; // last token has added to newtoks list.
                }
                i++;

            } else if (tok1.length() > 2 && tok2.equals(".")) {
                // "U.S" and "." --> "U.S."
                char ch = tok1.charAt(tok1.length() - 2); // second last char
                if (ch == '.') {
                    newtoks.add(tok1 + tok2);
                    if (i == toks.size() - 2) // check for last token.
                    {
                        lastTokAdded = true; // last token has added to newtoks list.
                    }
                    i++;

                } else {
                    newtoks.add(tok1);
                }
            } else if (tok1.length() == 1 && tok2.equals(".")) {
                if (isLetter(tok1)) {
                    newtoks.add(tok1 + tok2);
                    if (i == toks.size() - 2) // check for last token.
                    {
                        lastTokAdded = true; // last token has added to newtoks list.
                    }
                    i++;

                } else {
                    newtoks.add(tok1);
                }
            } else {
                newtoks.add(tok1);
            }
        }
        if (toks.size() > 0 && !lastTokAdded) {
            newtoks.add(toks.get(toks.size() - 1)); // add last one.
        }
        //System.out.println("Final Tokens:" + toks.toString());
       // System.out.println("Final new Tokens:" + newtoks.toString());
        return newtoks;

    }

    public static boolean isLetter(String str) {
        char ch = str.charAt(0);
        if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')) {
            return true;
        } else {
            return false;
        }

    }

    public static String removeAnnoEntityInUtt(String annoUtt, String[] startChars) {
        String utt = annoUtt;
        for (int i = 0; i < startChars.length; ++i) {
            String startChar = startChars[i];
            utt = removeAnnoEntityInUtt(utt, startChar);
        }
        return utt;
    }

    /**
     * Remove all annotated entities specified with starting chars from the
     * utterance. e.g. remove wh_ annotation: [wh_what: what] to just leave
     * "what"
     *
     * @return the processed string utterance.
     */
    public static String removeAnnoEntityInUtt(String annoUtt, String startChars) {
        String regex = "(\\[\\s*" + startChars + "\\S+\\s*:\\s*)([^\\]]+\\s*\\])";
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(annoUtt);
        while (m.find()) {
            String value = m.group(2).trim();
            String val = value.substring(0, value.length() - 1);
            annoUtt = annoUtt.replace(m.group(1), "");
            annoUtt = annoUtt.replace(m.group(2), val);
        }
        annoUtt = annoUtt.replaceAll("  ", " ");
        return annoUtt;

    }

    public static Map<String, String> getPreprocessedInfo(String infoFileUri, String flagLine) {
        List<String> lines = GeneralUtils.readFromTextFile(infoFileUri);
        // get index of the flagLine from the list
        if (!lines.contains(flagLine)) {
            System.out.println("Seems the data not yet prepocessed! File:" + infoFileUri);
            System.exit(-1);
            //return null;
        }

        Map<String, String> map = new LinkedHashMap<>();

        for (int i = lines.size() - 1; i >= 0; --i) {
            String line = lines.get(i);
            if (line.equals(flagLine)) {
                break;
            } else {
                String key = line.substring(0, line.indexOf("=")).trim();
                String value = line.substring(line.indexOf("=") + 1).trim(); // to the end of line
                // only add the first one in reversed order, not overwrite it by new one which is previously saved.
                if (!map.containsKey(key)) {
                    map.put(key, value);
                }
            }
        }

        return map;
    }

    public static Set<Pair> intersectionPair(final Set<Pair> first, final Set<Pair> second) {
        final Set<Pair> copy = new HashSet<>(first);
        // Pair's equal method compare both element0 and element1
        copy.retainAll(second);

        return copy;
    }

    /**
     * Each pair includes startIndex and endIndex of a token or tokens. This
     * method check if the index is overlapped, consider the same if yes,
     * otherwise differs.
     *
     * Result is a list so to keep the multiple overlap.
     *
     * @param first set of pair of integer <star, end>
     * @param second set of pair of integer <star, end>
     * @return the intersect list.
     */
    public static List<Pair> intersectionIndexOverlap(final Set<Pair> first, final Set<Pair> second) {
        List<Pair> res = new ArrayList<Pair>();
        for (Pair p1 : first) {
            for (Pair p2 : second) {
                // overlap : start1 <= end2 or end1 >= start2
                int start1 = (int) p1.getElement0();
                int end1 = (int) p1.getElement1();
                int start2 = (int) p2.getElement0();
                int end2 = (int) p2.getElement1();

                if ((start1 >= start2 && start1 <= end2) || (end1 >= start2 && end1 <= end2)) {
                    // overlap
                    //System.out.println("Added overlap p1:" + p1.toString());
                    res.add(p1);
                    // break; do not break in order to keep possible multiple overlap
                }
            }
        }
        return res;
    }

    public static Set<Integer> intersectionInt(final Set<Integer> first, final Set<Integer> second) {
        final Set<Integer> copy = new HashSet<>(first);
        copy.retainAll(second);
        return copy;
    }

    public static Set<String> intersectionStr(final Set<String> first, final Set<String> second) {
        final Set<String> copy = new HashSet<>(first);
        copy.retainAll(second);
        return copy;
    }

    static class CompareListString implements Comparator<String> {

        public int compare(String o1, String o2) {
            //return Integer.compare(o1.length(), o2.length());

            return Integer.compare(o2.length(), o1.length()); // longest first
        }
    }

    static class CompareListJsonObject implements Comparator<JsonObject> {

        String propertyName = "";

        CompareListJsonObject(String propName) {
            propertyName = propName;
        }

        public int compare(JsonObject o1, JsonObject o2) {
            //int o1start = o1.get("start").getAsInt();
            int o1start = o1.get(propertyName).getAsInt();
            int o2start = o2.get(propertyName).getAsInt();
            return Integer.compare(o1start, o2start); // by ascending.

        }
    }

    static class ComparatorWithRegex implements Comparator<String> {

        public int compare(String o1, String o2) {

            Pattern p = Pattern.compile(o1); // o1 is the patter containing regex
            Matcher m = p.matcher(o2); // o2 is the real utt, to check if it matches regex
            if (m.find()) {
                return 0; // equals to
            } else {
                return -1; // less than. 1: greater than
            }
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

    }

}
