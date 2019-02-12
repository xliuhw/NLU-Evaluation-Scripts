/**
 * ************************************************************
 * @(#)GeneralUtils.java
 *
 *
 *              Copyright(C) 2016 iLab MACS
 *              Heriot-Watt University
 *
 *                  All rights reserved
 *
 *   Version: 0.1  Created on 2 Dec 2016
 *
 *   Author: Dr Xingkun Liu, Dr Verena Rieser
 *
 *   Project: Emotech/TheDataLab
 *
 *************************************************************
 */
package uk.ac.hw.macs.ilab.nlu.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Various convenient methods are in this class.
 *
 * @author Xingkun Liu
 */
public class GeneralUtils {

    // public static Logger logger = Logger.getRootLogger();
    public static final String fsep = System.getProperty("file.separator");

    /**
     * check and create a directory if it does not exist
     *
     * @param dirURI the directory URI
     * @since 15/11/2012
     */
    public static void checkAndCreateDirectory(String dirURI) {
        if ((new File(dirURI)).exists()) {
            //logger.debug("the directory already exists.");
        } else {
            // create the directory
            (new File(dirURI)).mkdirs();
            // logger.info("Directory " + dirURI + " created!");
        }
    }

    public static List<String> listToLowerCase(List<String> origList) {
        List<String> newList = new ArrayList<>();
        for (int i = 0; i < origList.size(); ++i) {
            String utt = origList.get(i).toLowerCase();
            utt = utt.replaceAll("  ", " "); // remove double spaces

            // if(utt.endsWith(".") || utt.endsWith("!") || utt.endsWith("?")
            //         || utt.endsWith(";") || utt.endsWith(","))
            //     utt = utt.substring(0, utt.length()-1);
            newList.add(utt);
        }

        return newList;

    }

    /**
     * Read from a text file line by line, skipping the comment lines starting
     * with "#" and empty lines.
     *
     * @param sFileName string of file URI
     * @return List of string.
     */
    public static List<String> readFromTextFile(String sFileName) {

        File inputFile = new File(sFileName);
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(inputFile));
        } catch (IOException e) {
            //logger.debug(" Couldn't read file: " + sFileName + " Error: " + e);
            e.printStackTrace();
            System.exit(-1);
        }
        String line = "";
        List<String> lines = new ArrayList<>();

        try {
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("#") || line.isEmpty()) {
                    continue;
                }
                //System.out.println(line);
                lines.add(line);
            }

        } catch (java.io.IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        return lines;
    }

    /**
     * Read text file to a map. each line has format key = value
     *
     * an empty line or a line starting with # is an comment line and will be
     * ignored.
     *
     * @param sFileName, text file URI
     * @return Map<String, String>
     */
    public static Map<String, String> readMapFromTextFile(String sFileName) {

        File inputFile = new File(sFileName);
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(inputFile));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        String line = "";
        Map<String, String> lineMap = new LinkedHashMap<>();

        try {
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("#") || line.isEmpty()) {
                    continue;
                }
                if (line.indexOf("=") <= 0) {
                    System.err.println("\n\n WARNING: a line does not contain sign: =, line:" + line + "\n\n");
                    continue;
                }
                String key = line.substring(0, line.indexOf("="));
                String value = line.substring(line.indexOf("=") + 1);
                lineMap.put(key, value);
            }

        } catch (java.io.IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        return lineMap;
    }

    public static Set<String> readFromTextFileAllLowCaseNoDup(String sFileName) {

        File inputFile = new File(sFileName);
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(inputFile));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        String line = "";
        Set<String> lines = new HashSet<>();

        try {
            while ((line = br.readLine()) != null) {
                line = line.trim();
                lines.add(line.toLowerCase());
            }

        } catch (java.io.IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        return lines;
    }

    /**
     * Save list of text string to a file in fileURI. Will Overwritten existing
     * file if there is any.
     *
     * @param fileURI the file uri.
     * @param list list of string lines.
     * @return true or false.
     */
    public static boolean saveToFile(String fileURI, List<String> list) {

        // now open the file and write the info
        File outputFile = new File(fileURI);
        PrintStream out = null;
        try {
            out = new PrintStream(new FileOutputStream(outputFile));

            for (int i = 0; i < list.size(); i++) {
                out.println(list.get(i));
            }
            out.flush();
            out.close();
        } catch (IOException e) {
            // logger.debug(" Couldn't write file, Error is :" + e);
            e.printStackTrace();
            System.exit(-1);
            //return false;
        }
        return true;
    }

    /**
     * save list of text lines to a file with option of Append or Overwritten.
     * bAppending = true for appending to the end of file, otherwise overwritten
     * the existing file.
     *
     * The method will first check if the directory exists, if not, will create
     * it.
     *
     * @param filePath file path
     * @param fileName file name
     * @param contents list of text lines
     * @param bAppending boolean: true to append to the file, false to
     * overwritten i
     * @param encoding the encoding for writing. null for using default JVM
     * encoding.
     *
     */
    public static void saveToTextFile(String filePath, String fileName, List<String> contents, boolean bAppending, String encoding) {

        checkAndCreateDirectory(filePath);

        String fileURI = filePath + fsep + fileName;

        //String encoding = null;
        BufferedWriter out = null;

        try {
            // Create an OutputStreamWriter that uses the default character encoding.
            if (encoding == null) {
                out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileURI, bAppending)));
                String defaultEncoding = getDefaultCharSet();
                //logger.info("Using JAVA VM default encoding " + defaultEncoding);
                encoding = defaultEncoding;

            } else {
                out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileURI, bAppending), encoding));
            }

        } catch (UnsupportedEncodingException ue) {

            System.out.println("Not supported : " + ue.getMessage());
            System.exit(-1);
        } catch (IOException e) {
            e.printStackTrace();
            //System.out.println(e.getMessage());
            System.exit(-1);
        }

        try {
            for (int i = 0; i < contents.size(); i++) {
                String strToSave = contents.get(i);
                out.write(strToSave);
                out.newLine(); // Write system dependent end of line.
            }
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
            //System.out.println(e.getMessage());
            System.exit(-1);
        }

    }

    /**
     * To get the JAVA VM default encoding name.
     *
     * @return String of default encoding name
     */
    public static String getDefaultCharSet() {
        OutputStreamWriter writer = new OutputStreamWriter(new ByteArrayOutputStream());
        String enc = writer.getEncoding();
        return enc;
    }

    public static JsonObject readFileToJson(String fileName) {

        // Read from File to String
        JsonObject jsonObject = null;

        try {
            JsonParser parser = new JsonParser();
            JsonElement jsonElement = parser.parse(new FileReader(fileName));
            jsonObject = jsonElement.getAsJsonObject();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(-1);

        } catch (IOException ioe) {
            ioe.printStackTrace();
            System.exit(-1);
        }

        return jsonObject;
    }

    /**
     * Pretty print Json object by Gson.
     *
     * @param <T> input type normally String or JsonElement
     * @param inputJson The Json Object
     * @return String
     */
    public static <T> String prettyPrintJson(T inputJson) {

        JsonElement elem;
        if (inputJson instanceof String) {
            JsonParser parser = new JsonParser();
            elem = parser.parse((String) inputJson);
        } else {
            elem = (JsonElement) inputJson;
        }

        //   Gson gson = new GsonBuilder() .setLenient() .create();
        //  Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        // .disableHtmlEscaping() to to disable HTML escaping, avoiding e.g. "<" to \u003c
        Gson gson = new GsonBuilder().setLenient().setPrettyPrinting().disableHtmlEscaping().create();

        String prettyStr = gson.toJson(elem);
        // logger.info(prettyStr);
        return prettyStr;

    }

    public static String getTimeStamp4Logging() {
        Date sysDate = new Date();
        java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("yyyy_MM_dd-HH_mm_ss_SSS");
        String sDt = df.format(sysDate);
        return sDt;
    }

    public static String getTimeStamp4LoggingShort() {
        Date sysDate = new Date();
        java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("yyyyMMddHHmmssSSS");
        String sDt = df.format(sysDate);
        return sDt;
    }

    public static String saveJsonStrToFile(String moduleName, String filePath,
            String jsonStr, boolean withTimeStamp) {

        String prt = GeneralUtils.prettyPrintJson(jsonStr);
        String fileName = moduleName + ".json";
        if (withTimeStamp) {
            //Date sysDate = new Date();
            //java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("yyyy_MM_dd-HH_mm_ss_SSS");
            //String sDt = df.format(sysDate);
            String sDt = getTimeStamp4LoggingShort();
            fileName = moduleName + "_" + sDt + ".json";
        }

        GeneralUtils.checkAndCreateDirectory(filePath);
        String fileURI = filePath + "/" + fileName;
        List<String> contents = new ArrayList<>();

        contents.add(prt);

        GeneralUtils.saveToFile(fileURI, contents);

        return fileURI;
    }

    public static <K, V extends Comparable<? super V>>
            List<Map.Entry<K, V>> entriesSortedByValues(Map<K, V> map) {

        List<Map.Entry<K, V>> sortedEntries = new ArrayList<Map.Entry<K, V>>(map.entrySet());

        Collections.sort(sortedEntries,
                new Comparator<Map.Entry<K, V>>() {
            @Override
            public int compare(Map.Entry<K, V> e1, Map.Entry<K, V> e2) {
                return e2.getValue().compareTo(e1.getValue());
            }
        }
        );

        return sortedEntries;
    }

    /**
     * Recursively Copy whole directory except for specified file. If destDir
     * does not exist, it will be created. if exceptFile = "", copy all.
     *
     * @param srcPath a File of source path
     * @param dstPath a File of destination path
     * @param exceptFile a string of Exception File which will not be copied.
     *
     * @return a string of error messages
     */
    public static String copyDirectory(File srcPath, File dstPath, String exceptFile) {
        // throws IOException {
        if (srcPath.isDirectory()) {

            if (!dstPath.exists()) {

                dstPath.mkdir();
            }

            String files[] = srcPath.list();

            for (int i = 0; i < files.length; i++) {
                if (!files[i].equals(exceptFile)) {
                    copyDirectory(new File(srcPath, files[i]), new File(dstPath, files[i]), "");
                }
            }
        } else if (!srcPath.exists()) {
            String msg = "File or directory does not exist.";
            System.out.println(msg);
            return msg;
        } else {
            InputStream in = null;
            try {
                in = new FileInputStream(srcPath);
                OutputStream out = new FileOutputStream(dstPath);
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                in.close();
                out.close();
            } catch (IOException ex) {
                ex.printStackTrace();
                String msg = "IOException occurred when copy files.";
                System.out.println(msg);

                //return msg;
                System.exit(-1);

            } finally {
                try {
                    in.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                    System.exit(-1);
                }
            }

        }

        //logger.debug("Directory copied.");
        return "SUCCESS";
    }

    public static void copyFile(File in, File out)
            throws IOException {
        FileChannel inChannel = new FileInputStream(in).getChannel();
        FileChannel outChannel = new FileOutputStream(out).getChannel();
        try {
            inChannel.transferTo(0, inChannel.size(),
                    outChannel);
        } catch (IOException e) {
            //throw e;
            e.printStackTrace();
            System.exit(-1);
        } finally {
            if (inChannel != null) {
                inChannel.close();
            }
            if (outChannel != null) {
                outChannel.close();
            }
        }
    }

    public static JsonObject readJsonFile(String fileName) {

        // Read from File to String
        JsonObject jsonObject = null;

        try {
            JsonParser parser = new JsonParser();
            JsonElement jsonElement = parser.parse(new FileReader(fileName));
            jsonObject = jsonElement.getAsJsonObject();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(-1);

        } catch (IOException ioe) {
            ioe.printStackTrace();
            System.exit(-1);
        }

        return jsonObject;
    }

    public static Map<String, List<String>> getEntityMapFromMapFile(String fileUri) {
        // read raw file
        List<String> slotword = GeneralUtils.readFromTextFile(fileUri);
        // it is converted to lower case below.

        // key: SLOT_NAME, SLOT_FOOD etc
        Map<String, List<String>> entityKeyNamesMap = new LinkedHashMap<>();
        String keyChar = " ";
        for (int i = 0; i < slotword.size(); ++i) {
            String line = slotword.get(i).trim();
            if (line.startsWith("#") || line.isEmpty()) {
                continue;
            }
            if (line.endsWith(";")) {
                line = line.substring(0, line.length() - 1);
            }

            if (line.contains(":")) {
                keyChar = ":";
            }

            line = line.toLowerCase();

            String slot = line.substring(0, line.indexOf(keyChar)).trim();
            slot = slot.toLowerCase();
            System.out.println("slot=" + slot);
            String word = line.substring(line.indexOf(keyChar) + 1).trim();
            word = word.toLowerCase();
            //System.out.println("word="+word);
            String phrArr[] = word.split("\\|");
            List<String> list = new ArrayList<String>(Arrays.asList(phrArr));
            List<String> list1 = new ArrayList<>();
            for (int j = 0; j < list.size(); ++j) {
                // remove quote of the value
                String val = list.get(j).trim();
                if (val.startsWith("\"")) {
                    val = val.substring(1);
                }
                if (val.endsWith("\"")) {
                    val = val.substring(0, val.length() - 1);
                }
                val = val.trim();

                if (!val.equals("")) {
                    list1.add(val);
                }
            }

            System.out.println("word list size=" + list1.size());
            System.out.println("word list =" + list1.toString());
            if (entityKeyNamesMap.containsKey(slot)) {
                entityKeyNamesMap.get(slot).addAll(list1);
            } else {
                //List<String> words = new ArrayList<>();
                // words.add(word);
                entityKeyNamesMap.put(slot, list1);
            }

        }
        return entityKeyNamesMap;
    }

    public static String prettyPrintList(List<String> list) {
        String str = "[";
        for (int i = 0; i < list.size(); ++i) {
            str += list.get(i);
            if (i < list.size() - 1) {
                str += ", ";
            }
        }
        str += "]";
        return str;
    }

    public static String prettyPrintMap(Map<String, List<String>> map) {
        String str = "";
        //for (Map.Entry<String, List<String>> entry : map.entrySet()) {
        //   String key = entry.getKey();
        //   List<String> values = entry.getValue();
        //  str += key + ": \n" + "    " + prettyPrintList(values) + "\n";
        //}
        //return str;

        // sorted map keys
        SortedSet<String> keys = new TreeSet<String>(map.keySet());
        for (String key : keys) {
            List<String> values = map.get(key);
            //System.out.println("domain:" + key + ", values:" + values.toString());
            str += key + ": " + "Value Size:" + values.size() + ". \n" + "    " + prettyPrintList(values) + "\n\n";
        }
        return str;
    }

    public static String prettyPrintMap2OneStr(Map<String, Integer> map) {
        String str = "";

        // sorted map keys
        SortedSet<String> keys = new TreeSet<String>(map.keySet());
        for (String key : keys) {
            Integer values = map.get(key);
            //System.out.println("domain:" + key + ", values:" + values.toString());
            str += key + ": " + values + ", ";
        }
        
        str = str.trim();
        if(str.endsWith(",")) // remove last coma
            str = str.substring(0, str.length()-1);
        
        return str;
    }

    public static int getTotalCounts(Map<String, Integer> map) {
        // sorted map keys
        SortedSet<String> keys = new TreeSet<String>(map.keySet());
        int count = 0;
        for (String key : keys) {
            Integer values = map.get(key);
            count += values.intValue();
        }
        return count;
    }

    public static Map<String, Integer> mergeMapCounts(Map<String, Integer> map1,
            Map<String, Integer> map2) {

        for (Map.Entry<String, Integer> entry : map2.entrySet()) {
            String key = entry.getKey();
            Integer m2Vals = entry.getValue();
            Integer m1Vals = map1.get(key);
            if (m1Vals == null) {
                //m1Vals = new ArrayList();
                map1.put(key, m2Vals);
            } else {
                map1.put(key, m1Vals + m2Vals);
            }

        }

        return map1;
    }

    /**
     * Merge map2 to map1. The list values will be unique.
     *
     * @param map1
     * @param map2
     * @return merged map1
     */
    public static Map<String, List<String>> mergeMap(Map<String, List<String>> map1,
            Map<String, List<String>> map2) {

        for (Map.Entry<String, List<String>> entry : map2.entrySet()) {
            String key = entry.getKey();
            List<String> values = entry.getValue();
            List<String> m1Vals = map1.get(key);
            if (m1Vals == null) {
                m1Vals = new ArrayList();
                map1.put(key, m1Vals);
            }

            for (int i = 0; i < values.size(); ++i) {
                if (!m1Vals.contains(values.get(i))) {
                    m1Vals.add(values.get(i));
                }
            }

        }
        return map1;
    }

    public static Map<String, List<String>> mergeMapWithDup(Map<String, List<String>> map1,
            Map<String, List<String>> map2) {

        for (Map.Entry<String, List<String>> entry : map2.entrySet()) {
            String key = entry.getKey();
            List<String> values = entry.getValue();
            List<String> m1Vals = map1.get(key);
            if (m1Vals == null) {
                m1Vals = new ArrayList();
                map1.put(key, m1Vals);
            }

            m1Vals.addAll(values); // allow duplicated values

        }
        return map1;
    }

    public static List<String> prettyPrintMapAsList(Map<String, List<String>> map) {

        List<String> list = new ArrayList<>();
        // sorted map keys
        SortedSet<String> keys = new TreeSet<String>(map.keySet());
        for (String key : keys) {
            List<String> values = map.get(key);
            //System.out.println("domain:" + key + ", values:" + values.toString());
            list.add(key + ": " + "Value Size:" + values.size() + ".");
            list.add(prettyPrintList(values) + "\n");
        }

        return list;
    }

    public static String prettyPrintMapMap(Map<String, Map<String, List<String>>> map) {
        String str = "";
        for (Map.Entry<String, Map<String, List<String>>> entry : map.entrySet()) {
            String key = entry.getKey();
            Map<String, List<String>> values = entry.getValue();
            str += "parent key:" + key + ": \n" + "    " + prettyPrintMap(values) + "\n";
        }

        return str;
    }

    /**
     * Check if a JsonArray of strings contains the string specified. JsonArray
     * of String: [str1, str2, ...]
     *
     * @param ja JsonArray of string.
     * @param str specified string to check
     * @return true or false.
     */
    public static boolean isJsonArrayContainString(JsonArray ja, String str) {
        boolean found = false;
        for (int i = 0; i < ja.size(); ++i) {
            String item = ja.get(i).getAsString();
            if (item.equals(str)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get file name (without path) in the directory specified. Assume only one
     * file there as it returns the first file in the list. It can contain
     * directories which not affect the file name returned.
     *
     * @param domainDir the directory to check.
     *
     * @return String of the file name.
     */
    public static String getFirstFileName_OLD(String domainDir) {
        File folder = new File(domainDir);
        File[] listOfFiles = folder.listFiles(); 
        
        // listFiles() could return null: if non directory, I/O error (may be out of memory issue)
        
        //System.out.println("total num:" + listOfFiles.length);
        String fname = "";
        for (int i = 0; i < listOfFiles.length; ++i) {
            if (listOfFiles[i].isDirectory()) {
                System.out.println("First file is a directory!");
                continue;
            } else { // assume each domain directory only contains one file.
                fname = listOfFiles[i].getName();
                break;
            }
        }
        return fname;
    }

    public static String getFirstFileName(String domainDir) {
        List<String> filenames = getAllFileNames_NIO2(domainDir);
        if(filenames == null || filenames.size() == 0)
            return null;
        else
            return filenames.get(0);
        
    }
    /**
     * Use NIO.2
     * 
     * @param directory
     * @return 
     */
    public static List<String> getAllFileNames_NIO2(String directory) {
        List<String> fileNames = new ArrayList<>();
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(directory))) {
            for (Path path : directoryStream) {
                String fileName = path.getFileName().toString();
                fileNames.add(fileName);
                System.out.println("filename:" + fileName);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        
        return fileNames;
    }

    /**
     * Get all file names in the directory specified. Ignore the subdirectories
     * inside.
     *
     * @param baseDir the directory to look for inside.
     * @return a list of file names
     */
    public static List<String> getAllFileNames(String baseDir) {
        File folder = new File(baseDir);
        File[] listOfFiles = folder.listFiles();

        List<String> fileNames = new ArrayList<>();
        for (int i = 0; i < listOfFiles.length; ++i) {
            if (listOfFiles[i].isDirectory()) {
                //System.out.println("It is a directory!");
                continue;
            } else {
                String fname = listOfFiles[i].getName();
                fileNames.add(fname);
            }
        }
        return fileNames;
    }

    public static List<String> getAllSubdirNames(String domainDir) {
        System.out.println("===== " + domainDir);
        File folder = new File(domainDir);
        File[] listOfFiles = folder.listFiles();

        List<String> names = new ArrayList<>();
        for (int i = 0; i < listOfFiles.length; ++i) {
            if (listOfFiles[i].isDirectory()) {
                String name = listOfFiles[i].getName();
                names.add(name);

            } else { // assume each domain directory only contains one file.
                //System.out.println("it is a file!");
                continue;
            }
        }
        return names;
    }

    public static String[] splitWithCommaNotInsideQuote(String line, String delimiter) {

        line = line.trim();
        // remove it if line ends with the delimiter, otherwise it will have extra item 
        // in the result array. The standard split(delimiter) does not have it.
        // new issue: last items are empty, so just more delimiters there
        //if(line.endsWith(delimiter))
        //    line = line.substring(0,line.length()-delimiter.length());

        String otherThanQuote = " [^\"] ";
        String quotedString = String.format(" \" %s* \" ", otherThanQuote);
        String regex = String.format("(?x) "
                + // enable comments, ignore white spaces
                // ",                         "+ // match a comma
                delimiter
                + // match a provided delimiter
                "(?=                       "
                + // start positive look ahead
                "  (?:                     "
                + //   start non-capturing group 1
                "    %s*                   "
                + //     match 'otherThanQuote' zero or more times
                "    %s                    "
                + //     match 'quotedString'
                "  )*                      "
                + //   end group 1 and repeat it zero or more times
                "  %s*                     "
                + //   match 'otherThanQuote'
                "  $                       "
                + // match the end of the string
                ")                         ", // stop positive look ahead
                otherThanQuote, quotedString, otherThanQuote);

        String[] tokens = line.split(regex, -1);

        return tokens;
    }

    public static String replaceMoreSepcialCharsToOne(String utt, String ch) {
        // mainly for double spaces, apostrophe etc.
        String doubleCh = ch + ch;
        while (utt.contains(doubleCh)) {
            utt = utt.replaceAll(doubleCh, ch);
        }
        return utt;
    }

    public static List<String> removeDuplicates(List<String> srcList) {
        System.out.println("Rules size before removing dup:" + srcList.size());
        List<String> newList = new ArrayList<>();

        for (int i = 0; i < srcList.size(); ++i) {
            String item = srcList.get(i);

            if (newList.contains(item)) {
                System.out.println("Duplicated item:" + item);
                continue;
            }
            newList.add(item);
        }

        System.out.println("Rules size after removing dup:" + newList.size());
        int num = srcList.size() - newList.size();
        System.out.println("Number of duplicated rules: " + num);

        return newList;
    }

    public static boolean isInteger(String str) {
        if (str == null) {
            return false;
        }
        try {
            /* int i = */ Integer.parseInt(str);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    public static boolean isDouble(String str) {
        if (str == null) {
            return false;
        }
        try {
            /* int i = */ Double.parseDouble(str);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    public static boolean isStrNumeric(String str) {
        if (str == null) {
            return false;
        }
        try {
            /* int i = */ Integer.parseInt(str);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    /**
     * This method merges any number of arrays of any count.
     *
     * Usage:
     *
     * String[] a = {"This ", "is ", "just "}; String[] b = {"a ", "test ",
     * "case "}; String[] c = {"to ", "test "}; String[] d = {"array ", "merge
     * "};
     *
     * printArray(merge(a, b)); printArray(merge(a, b, c)); printArray(merge(a,
     * b, c, d));
     *
     * @param arrays
     * @return merged array
     */
    public static String[] merge(String[]... arrays) {
        // Count the number of arrays passed for merging and the total size of resulting array
        int arrCount = 0;
        int count = 0;
        for (String[] array : arrays) {
            arrCount++;
            count += array.length;
        }
        System.out.println("Arrays passed for merging : " + arrCount);
        System.out.println("Array size of resultig array : " + count);

        // Create new array and copy all array contents
        String[] mergedArray = (String[]) java.lang.reflect.Array.newInstance(arrays[0][0].getClass(), count);
        int start = 0;
        for (String[] array : arrays) {
            System.arraycopy(array, 0, mergedArray, start, array.length);
            start += array.length;
        }
        return mergedArray;
    }

    public static String getSpace(int count) {
        String space = "";
        for (int i = 0; i < count; i++) {
            space += " ";
        }

        return space;
    }



    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
      String dir = "./";
      getAllFileNames_NIO2(dir);
        
    }
        
}
