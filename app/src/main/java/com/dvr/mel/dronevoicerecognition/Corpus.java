package com.dvr.mel.dronevoicerecognition;
/**
 * Created by Evan on 16/11/2016.
 *
 */

class Corpus {
    private static Corpus _lastReference = null;

    private String mName;
    private String mDescription;
    private boolean mReference = false;
    private boolean mHasDescription = false;


    public Corpus(String name, String description){
        this.mName = name;
        this.mDescription = description;
        mHasDescription = !mDescription.isEmpty();
        // If first ever created it is the reference
        if(_lastReference == null) this.setAsReference();
    }

    public Corpus(String name)
    {
        this.mName = name;
        this.mDescription = "";
        this.mHasDescription = false;
    }

    public String getName() {
        return mName;
    }

    public String getDescription() {
        return mDescription;
    }

    public void setDescription(String description) {
        this.mDescription = description;
        mHasDescription = !description.isEmpty();
    }

    public boolean hasDescription() {
        return mHasDescription;
    }

    public boolean isReference(){
        return mReference;
    }

    public void setAsReference() {
        // If there is another reference
        if (_lastReference != null)
            // Last is not reference anymore
            _lastReference.mReference = false;
        // Self is now a reference
        this.mReference = true;
        // Self is the last reference
        _lastReference = this;
    }

}
