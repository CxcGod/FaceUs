package com.microsoft.projectoxford.face.samples;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by CXC on 2016/4/10.
 */
public class MyMap {

    private ArrayList<UUID> mUUIDs = new ArrayList<>();
    private ArrayList<String> mPaths = new ArrayList<>();

    public MyMap(){
    }

    public boolean containsKey(UUID key){
        for (int i = 0 ; i < mUUIDs.size();i++) {
            if (key.toString().equals(mUUIDs.get(i).toString())){
                return true;
            }
        }
        return false;
    }

    public void put(UUID uuid,String path){
        mUUIDs.add(uuid);
        mPaths.add(path);
    }

    public int size(){
        return mPaths.size();
    }

    public String get(UUID key){
        int index = mUUIDs.indexOf(key);
        return mPaths.get(index);
    }

}
