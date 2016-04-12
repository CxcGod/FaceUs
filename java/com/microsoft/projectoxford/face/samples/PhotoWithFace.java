package com.microsoft.projectoxford.face.samples;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by CXC on 2016/4/5.
 */
public class PhotoWithFace {

    private String mImagePath;
    private ArrayList<UUID> mFaceID;

    public PhotoWithFace(String imagePath, ArrayList<UUID> faceID){

        mImagePath = imagePath;
        mFaceID = faceID;

    }

    public String getImagePath() {
        return mImagePath;
    }

    public void setImageUri(String imagePath) {
        mImagePath = imagePath;
    }

    public ArrayList<UUID> getFaceID() {
        return mFaceID;
    }

    public void setFaceID(ArrayList<UUID> faceID) {
        mFaceID = faceID;
    }
}
