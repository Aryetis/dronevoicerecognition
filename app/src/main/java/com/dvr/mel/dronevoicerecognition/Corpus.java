package com.dvr.mel.dronevoicerecognition;

import android.os.Parcelable;

import java.io.Serializable;

/**
 * Created by Evan on 16/11/2016.
 *
 */

class Corpus implements Serializable{
    private String mName;
    private String mDisplayName;
    private boolean mReference = false;
    private boolean mHasDisplayName = false;


    public Corpus(String name, String displayName){
        this.mName = name;
        this.mDisplayName = displayName;
        mHasDisplayName = !mDisplayName.isEmpty();
    }

    public Corpus(String name)
    {
        this.mName = name;
        this.mDisplayName = name;
        this.mHasDisplayName = false;
    }

    public String getName() {
        return mName;
    }

    public String getDisplayName() {
        return mDisplayName;
    }

    public void setDisplayName(String displayName) {
        this.mDisplayName = displayName;
        mHasDisplayName = !displayName.isEmpty();
    }

    public boolean hasDisplayName() {
        return mHasDisplayName;
    }

    public boolean isReference(){
        return mReference;
    }

    public void setAsReference() {
        // Self is now a reference
        this.mReference = true;
        // Self is to be added to the set of corpora
        CorpusInfo.referencesCorpora.add(this.getName());
    }

    public void unsetReference() {
        // Self is now a reference
        this.mReference = false;
        // Self is to be added to the set of corpora
        CorpusInfo.referencesCorpora.remove(this.getName());
    }

}
