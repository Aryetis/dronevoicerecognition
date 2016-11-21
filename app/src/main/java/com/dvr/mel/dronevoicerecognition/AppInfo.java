package com.dvr.mel.dronevoicerecognition;

import android.util.ArrayMap;

import java.io.File;
import java.io.Serializable;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by leo on 16/11/16.
 */

public class AppInfo implements Serializable{
    public static File baseDir, corpusGlobalDir;
    public static Set<String> referencesCorpora = new HashSet<>();
    public static Set<String> usersCorpora = new HashSet<>();
    public static List<String> commands = new ArrayList<>();
    public static Map<String, Corpus> corpusMap = new HashMap<>();

    public File _baseDir, _corpusGlobalDir;
    public Set<String> _referencesCorpora = new HashSet<>();
    public Set<String> _usersCorpora = new HashSet<>();
    public List<String> _commands = new ArrayList<>();
    public Map<String, Corpus> _corpusMap = new HashMap<>();


    public AppInfo() {    }

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

    public void updateFromStaticVariables() {
        this._baseDir = new File(baseDir.getAbsolutePath());
        this._corpusGlobalDir = new File(corpusGlobalDir.getAbsolutePath());
        this._referencesCorpora = new HashSet<>(referencesCorpora);
        this._usersCorpora = new HashSet<>(usersCorpora);
        this._commands = new ArrayList<>(commands);
        this._corpusMap = new HashMap<>(corpusMap);
    }

    public void updateToStaticVariables() {
        baseDir = new File(this._baseDir.getAbsolutePath());
        corpusGlobalDir = new File(this._corpusGlobalDir.getAbsolutePath());
        referencesCorpora = new HashSet<>(this._referencesCorpora);
        usersCorpora = new HashSet<>(this._usersCorpora);
        commands = new ArrayList<>(this._commands);
        corpusMap = new HashMap<>(this._corpusMap);
    }
}
