package com.microsoft.projectoxford.face.samples;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.contract.Face;
import com.microsoft.projectoxford.face.contract.SimilarFace;
import com.microsoft.projectoxford.face.samples.helper.SampleApp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

public class FaceChoose extends AppCompatActivity {

    public final static String TAG = "FaceChoose";

    public final static int MALE = 0;
    public final static int FEMALE = 1;

    ImageView maleImage;
    ImageView femaleImage;
    Button mButton;
    ProgressDialog mDetectDialog ;
    Button mResultButton;

    Bitmap mBitmap;
    ProgressDialog mProgressDialog;


    ArrayList<String> mImagePaths = new ArrayList<>();
    ArrayList<PhotoWithFace> mPhotoWithFaces = new ArrayList<>();
    PhotoWithFaceManager mPhotoWithFaceManager;

    ArrayList<String> matchedImagePaths ;

    private PhotoWithFace mMaleFace,mFemaleFace;

    private boolean maleSelected = false;
    private boolean femaleSelected = false;
    private boolean hasMatched = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_choose);

        new AlertDialog.Builder(FaceChoose.this)
                .setTitle("提示")
                .setMessage("请选择两张包含单个人脸的照片并点击开始匹配")
                .setPositiveButton("ok",null)
                .create()
                .show();

        new SearchAllPhotoTask().execute("ll");

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle("detecting...");
        mProgressDialog.setCancelable(true);
        mResultButton = (Button) findViewById(R.id.result_button);

        mDetectDialog = new ProgressDialog(this);
        mDetectDialog.setCancelable(false);

        maleImage = (ImageView) findViewById(R.id.male_image);
        femaleImage = (ImageView) findViewById(R.id.female_image);
        mButton = (Button) findViewById(R.id.matching_button);

        mButton.setClickable(false);

        maleImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectImageInAlbum(MALE);
            }
        });

        femaleImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectImageInAlbum(FEMALE);
            }
        });

        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                new MultiDetectionTask().execute(mImagePaths);
                hasMatched = true;

            }
        });

        mResultButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMaleFace != null && mFemaleFace != null && hasMatched==true) {
                    mPhotoWithFaceManager = new PhotoWithFaceManager(mPhotoWithFaces,mMaleFace,mFemaleFace);

                    mProgressDialog.setTitle("提示");
                    mProgressDialog.setMessage("正在查找合照，请稍候...");
                    mProgressDialog.show();
                    mPhotoWithFaceManager.getMatchedFaces();

                }else {
                    new AlertDialog.Builder(FaceChoose.this)
                            .setTitle("提示")
                            .setMessage("请先选择两张包含单个人脸的照片并点击开始匹配")
                            .setNegativeButton("cancel",null)
                            .setPositiveButton("ok",null)
                            .create()
                            .show();
                }
            }
        });
    }

    public void selectImageInAlbum(int category) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, category);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode==RESULT_OK){

            Uri imageUri = data.getData();
            Log.e("Face",imageUri.toString());
            String path = getRealPathFromURI(imageUri);
            Log.e(TAG, imageUri.toString());
            Log.e("Face", path);

            mBitmap = BitmapFactory.decodeFile(path);

            ByteArrayOutputStream baos = null ;
            try{
                baos = new ByteArrayOutputStream();
                mBitmap.compress(Bitmap.CompressFormat.JPEG, 30, baos);
            }finally{
                try {
                    if(baos != null)
                        baos.close() ;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (mBitmap == null) {
                Log.e("Face", "Image resolve failed");
            }

            if (requestCode==0){
                maleSelected = true;
                maleImage.setImageBitmap(mBitmap);
                new DetectionTask().execute(path);
            }else if (requestCode==1){
                femaleSelected = true;
                femaleImage.setImageBitmap(mBitmap);
                new DetectionTask().execute(path);
            }
        }
    }

    public InputStream bitmapToInputStream(Bitmap bitmap){

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());
        return inputStream;

    }

    public String getRealPathFromURI(Uri contentUri) {
        String res = null;
        String[] proj = { MediaStore.Images.Media.DATA };
        Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
        if(cursor.moveToFirst()){
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            res = cursor.getString(column_index);
        }
        cursor.close();
        return res;
    }


    private class SearchAllPhotoTask extends AsyncTask<String,String,ArrayList<Uri>>{

        @Override
        protected ArrayList<Uri> doInBackground(String...parm) {

            Uri mImageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            ContentResolver contentResolver = FaceChoose.this.getContentResolver();
            Cursor mCursor = contentResolver.query(mImageUri, null,
                    MediaStore.Images.Media.MIME_TYPE + "=? or "
                            + MediaStore.Images.Media.MIME_TYPE + "=?",
                    new String[] { "image/jpeg", "image/png" },
                    MediaStore.Images.Media.DATE_MODIFIED);

            int count = mCursor.getCount();
            Log.e(TAG, count + " photos detected");

            mCursor.moveToFirst();
            for (int i = 0; i < count; i++) {
                String path = mCursor.getString(mCursor
                        .getColumnIndex(MediaStore.Images.Media.DATA));
                mImagePaths.add(path);
                Log.e(TAG, path);
                mCursor.moveToNext();
            }
            return null;
        }

        @Override
        protected void onPostExecute(ArrayList<Uri> uris) {
            mButton.setClickable(true);
        }
    }

    private class MultiDetectionTask extends AsyncTask<ArrayList<String>,String,Face[]>{

        private boolean mSucceed = true;
        private ArrayList<Face> mFaces = new ArrayList<>();

        @Override
        protected Face[] doInBackground(ArrayList<String>... params) {
            FaceServiceClient faceServiceClient = SampleApp.getFaceServiceClient();
            try {
                publishProgress("Detecting...");
                // Start detection.
                for (int i = 0 ;i < params[0].size();i++){

                    publishProgress("正在检测第"+(i+1)+"张照片"+",共有"+params[0].size()+"张照片待检测");

                    Bitmap bitmap = MyImageLoader.getSmallBitmap(params[0].get(i));
                    InputStream inputStream = bitmapToInputStream(bitmap);
                    Face[] faces = faceServiceClient.detect(
                            inputStream,
                            true,/*返回人脸id*/
                            false,
                            null
                    );

                    Log.e(TAG,faces.length+"");

                    //把至少含有两张人脸的照片加入待分组队列
                    if (faces.length>=2){
                        ArrayList<UUID> faceIDs = new ArrayList<>();
                        for (int j = 0 ;j < faces.length;j++){
                            faceIDs.add(faces[j].faceId);
                            Log.e(TAG,faces[j].faceId.toString());
                        }
                        mPhotoWithFaces.add(new PhotoWithFace(params[0].get(i),faceIDs));
                    }

                }

            } catch (Exception e) {
                mSucceed = false;
                publishProgress(e.getMessage());
                return null;
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            mDetectDialog.setMessage("Detecting...");
            mDetectDialog.show();
        }

        @Override
        protected void onProgressUpdate(String... values) {
            mDetectDialog.setMessage(values[0]);
        }

        @Override
        protected void onPostExecute(Face[] faces) {
            mDetectDialog.dismiss();
            Toast.makeText(FaceChoose.this,"检测完成",Toast.LENGTH_SHORT).show();
        }
    }

    private class DetectionTask extends AsyncTask<String, String, Face[]> {
        private boolean mSucceed = true;

        @Override
        protected Face[] doInBackground(String... params) {
            // Get an instance of face service client to detect faces in image.

            Bitmap bitmap = MyImageLoader.getSmallBitmap(params[0]);
            InputStream inputStream = bitmapToInputStream(bitmap);

            FaceServiceClient faceServiceClient = SampleApp.getFaceServiceClient();
            try {
                publishProgress("Detecting...");

                // Start detection.
                Face[] faces =  faceServiceClient.detect(
                        inputStream,  /* Input stream of image to detect */
                        true,       /* Whether to return face ID */
                        false,       /* Whether to return face landmarks */
                        /* Which face attributes to analyze, currently we support:
                           age,gender,headPose,smile,facialHair */
                        null);
                if (faces.length==1){
                    ArrayList<UUID> faceIDs = new ArrayList<>();
                    for (int j = 0 ;j < faces.length;j++){
                        faceIDs.add(faces[j].faceId);
                        Log.e(TAG,faces[j].faceId.toString());
                    }
                    if (maleSelected==true){
                        mMaleFace = new PhotoWithFace(params[0],faceIDs);
                    }else {
                        mFemaleFace = new PhotoWithFace(params[0],faceIDs);
                    }
                    maleSelected = false;
                    femaleSelected = false;
                }
                return faces;
            } catch (Exception e) {
                mSucceed = false;
                publishProgress(e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
            mProgressDialog.show();
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            mProgressDialog.setMessage(progress[0]);
            Log.e(TAG, progress[0]);

        }

        @Override
        protected void onPostExecute(Face[] result) {
            if (mSucceed) {
                mProgressDialog.dismiss();
                if (result.length==0){
                    Toast.makeText(FaceChoose.this,"你所选择的照片不包含人脸，请重新选择",Toast.LENGTH_SHORT).show();
                }else if (result.length>1){
                    Toast.makeText(FaceChoose.this,"你所选择的照片包含多张人脸，请重新选择",Toast.LENGTH_SHORT).show();
                }else {
                    Toast.makeText(FaceChoose.this,"检测人脸成功！",Toast.LENGTH_SHORT).show();
                }
                Log.e(TAG,"Response: Success. Detected " + (result == null ? 0 : result.length)
                        + " face(s) in ");
            }

            // Show the result on screen when detection is done.
//            setUiAfterDetection(result, mSucceed);
        }
    }


    /**
     * Created by CXC on 2016/4/5.
     */
    public class PhotoWithFaceManager {

        private static final String TAG = "PhotoWithFaceManager";
        private ArrayList<PhotoWithFace> mPhotoWithFaces;
        private PhotoWithFace mMaleFace, mFemaleFace;

        private MyMap faceIDMap = new MyMap();

        private boolean hasMatched = false;

        private ArrayList<UUID> mMaleMatchedFaces = new ArrayList<>();
        private ArrayList<UUID> mFemaleMatchedFaces = new ArrayList<>();

        private ArrayList<UUID> mFaceIDs = new ArrayList<>();
        private ArrayList<String> mMatchedImagePaths = new ArrayList<>();

        public PhotoWithFaceManager(ArrayList<PhotoWithFace> photoWithFaces, PhotoWithFace maleFace, PhotoWithFace femaleFace) {

            mPhotoWithFaces = photoWithFaces;
            mMaleFace = maleFace;
            mFemaleFace = femaleFace;

            toMyMap();

        }

        private void toMyMap() {
            System.gc();
            for (int i = 0; i < mPhotoWithFaces.size(); i++) {
                ArrayList<UUID> faceIDs = mPhotoWithFaces.get(i).getFaceID();
                String imagePath = mPhotoWithFaces.get(i).getImagePath();
                for (int j = 0; j < faceIDs.size(); j++) {
                    faceIDMap.put(faceIDs.get(j), imagePath);
                    mFaceIDs.add(faceIDs.get(j));
                }
            }

            Log.e(TAG, "faceIDMap count " + faceIDMap.size() + "");

        }


        private void findSimilarFace() {
            findMaleFace();
        }

        public void getMatchedFaces() {

            findSimilarFace();//检测相似人脸

//            return mMatchedImagePaths;

        }

        private void match() {
            ArrayList<String> maleImagePaths = new ArrayList<>();
            ArrayList<String> femaleImagePaths = new ArrayList<>();

            for (int i = 0; i < mMaleMatchedFaces.size(); i++) {
                if (faceIDMap.containsKey(mMaleMatchedFaces.get(i))) {
                    maleImagePaths.add(faceIDMap.get(mMaleMatchedFaces.get(i)));
                }
            }

            for (int i = 0; i < mFemaleMatchedFaces.size(); i++) {
                if (faceIDMap.containsKey(mFemaleMatchedFaces.get(i))) {
                    femaleImagePaths.add(faceIDMap.get(mFemaleMatchedFaces.get(i)));
                }
            }

            for (int i = 0; i < maleImagePaths.size(); i++) {
                Log.e(TAG, "maleImagePath " + maleImagePaths.get(i));
            }

            for (int i = 0; i < femaleImagePaths.size(); i++) {
                Log.e(TAG, "femaleImagePath " + femaleImagePaths.get(i));
            }

            ArrayList<String> matchedImagePaths = new ArrayList(maleImagePaths);
            matchedImagePaths.retainAll(femaleImagePaths);

            Log.e(TAG, "matchImagePaths " + matchedImagePaths.size());

            mMatchedImagePaths = matchedImagePaths;
            Log.e(TAG, "mMatchImagePaths " + mMatchedImagePaths.size());
            for (int i = 0 ; i < mMatchedImagePaths.size(); i++) {
                Log.e(TAG, mMatchedImagePaths.get(i));
            }

            mProgressDialog.dismiss();
            Intent intent = new Intent(FaceChoose.this,ResultActivity.class);
            Bundle bundle = new Bundle();
            bundle.putStringArrayList("result",mMatchedImagePaths);
            intent.putExtra("bundle_result",bundle);
            startActivity(intent);


        }

        private void findMaleFace() {
            mFaceIDs.add(0, mMaleFace.getFaceID().get(0));
            UUID[] uuids = new UUID[mFaceIDs.size()];
            mFaceIDs.toArray(uuids);
            new FindMaleFaceTask().execute(uuids);
        }

        private void findFemaleFace() {

            if (mMaleMatchedFaces.size() != 0) {
                mFaceIDs.remove(0);
                mFaceIDs.add(0, mFemaleFace.getFaceID().get(0));
                UUID[] uuids = new UUID[mFaceIDs.size()];
                mFaceIDs.toArray(uuids);
                new FindFemaleFaceTask().execute(uuids);
            }
        }

        private class FindMaleFaceTask extends AsyncTask<UUID, String, SimilarFace[]> {
            private boolean mSucceed = true;

            @Override
            protected SimilarFace[] doInBackground(UUID... params) {
                // Get an instance of face service client to detect faces in image.
                FaceServiceClient faceServiceClient = SampleApp.getFaceServiceClient();
                try {
                    UUID[] faceIds = Arrays.copyOfRange(params, 1, params.length);
                    // Start find similar faces.
                    return faceServiceClient.findSimilar(
                            params[0],
                            faceIds,      /* The first face ID to verify */
                            faceIds.length);     /* The second face ID to verify */
                } catch (Exception e) {
                    mSucceed = false;
                    publishProgress(e.getMessage());
                    return null;
                }
            }


            @Override
            protected void onProgressUpdate(String... values) {
                // Show the status of background find similar face task on screen.
            }

            @Override
            protected void onPostExecute(SimilarFace[] result) {
                if (mSucceed) {
                    for (int i = 0; i < result.length; i++) {
                        mMaleMatchedFaces.add(result[i].faceId);
                        Log.e(TAG, result[i].faceId.toString());
                    }
                    Log.e(TAG, "mMaleMatchedFaces count " + mMaleMatchedFaces.size());
                    findFemaleFace();
                }
            }
        }

        private class FindFemaleFaceTask extends AsyncTask<UUID, String, SimilarFace[]> {
            private boolean mSucceed = true;

            @Override
            protected SimilarFace[] doInBackground(UUID... params) {
                // Get an instance of face service client to detect faces in image.
                FaceServiceClient faceServiceClient = SampleApp.getFaceServiceClient();
                try {
                    UUID[] faceIds = Arrays.copyOfRange(params, 1, params.length);
                    // Start find similar faces.
                    return faceServiceClient.findSimilar(
                            params[0],
                            faceIds,      /* The first face ID to verify */
                            faceIds.length);     /* The second face ID to verify */
                } catch (Exception e) {
                    mSucceed = false;
                    publishProgress(e.getMessage());
                    return null;
                }
            }


            @Override
            protected void onProgressUpdate(String... values) {
                // Show the status of background find similar face task on screen.
            }

            @Override
            protected void onPostExecute(SimilarFace[] result) {
                if (mSucceed) {
                    for (int i = 0; i < result.length; i++) {
                        mFemaleMatchedFaces.add(result[i].faceId);
                        Log.e(TAG, result[i].faceId.toString());
                    }
                    Log.e(TAG, "mFemaleMatchedFaces count " + mMaleMatchedFaces.size());
                    match();
                    hasMatched = true;
                }
            }
        }

        public ArrayList<PhotoWithFace> getPhotoWithFaces() {
            return mPhotoWithFaces;
        }

    }
}