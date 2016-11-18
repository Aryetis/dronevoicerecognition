package com.dvr.mel.dronevoicerecognition;

import java.io.File;
import java.io.Serializable;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by leo on 16/11/16.
 */

public class CorpusInfo implements Serializable{
    public static File baseDir, corpusGlobalDir;
    public static File wordTestingDir;
    public static List<String> referencesCorpora = new ArrayList<>();
    public static List<String> usersCorpora = new ArrayList<>();
    public static List<String> commands = new ArrayList<>();
    public static Map<String, Corpus> corpusMap = new HashMap<>();

    public File _baseDir, _corpusGlobalDir;
    public File _wordTestingDir;
    public List<String> _referencesCorpora = new ArrayList<>();
    public List<String> _usersCorpora = new ArrayList<>();
    public List<String> _commands = new ArrayList<>();
    public Map<String, Corpus> _corpusMap = new HashMap<>();


    public CorpusInfo() {
        _baseDir = baseDir;
        _corpusGlobalDir = corpusGlobalDir;
        _wordTestingDir = wordTestingDir;
        _referencesCorpora = referencesCorpora;
        _usersCorpora = usersCorpora;
        _commands = commands;
    }

    public static void clean(String corpusName) {
        // Delete all files related to the corpus designed by corpusName
        File corpusToDelete = new File(corpusGlobalDir, corpusName);
        deleteDirectory(corpusToDelete);
    }

    private static void deleteDirectory(File directory) {
        if (directory.isDirectory())
            for (File f : directory.listFiles())
                deleteDirectory(f);

        directory.delete();
    }

    public static void addCorpus(String name, Corpus corpus) {
        usersCorpora.add(name);
        corpusMap.put(name, corpus);
    }

    public static String sanitarizeName(String name){
        name = name.replace(' ', '_').replace('*', '_');
        name = name.toLowerCase();

        StringBuilder sb = new StringBuilder(name.length());
        name = Normalizer.normalize(name, Normalizer.Form.NFD);
        for (char c : name.toCharArray()) {
            if (c <= '\u007F') sb.append(c);
        }
        return sb.toString();
    }
}
