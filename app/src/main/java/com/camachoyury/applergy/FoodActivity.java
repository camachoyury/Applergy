package com.camachoyury.applergy;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

/**
 * Created by yury on 8/14/16.
 */

public class FoodActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks,  GoogleApiClient.OnConnectionFailedListener{


    public static final String TAG = "Applergy#FoodActivity";
    @BindView(R.id.food_photo)
    ImageView imageView;
    @BindView(R.id.food_name)
    EditText editTextFoodName;
    @BindView(R.id.food_type)
    EditText editTextFoodType;
    @BindView(R.id.send_food)
    Button buttonSend;
    @BindView(R.id.food_linearlayout)
    LinearLayout linearLayout;

    private ProgressDialog mProgressDialog;
    FirebaseStorage storage;
    private static final int CAMERA_REQUEST = 1888;
    private static final int RC_STORAGE_PERMS = 102;

    private StorageReference mStorageRef;
    private DatabaseReference mDatabase;
    private Uri uriFoodPhoto;
    private Uri savedFodUrl;
    String captureImagePath;
    private SharedPreferences mSharedPreferences;

    public static final String ANONYMOUS = "anonymous";
    private String mUsername;
    private String mPhotoUrl;
    private FirebaseUser mFirebaseUser;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseRemoteConfig mFirebaseRemoteConfig;
    private GoogleApiClient mGoogleApiClient;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food);
        ButterKnife.bind(this);

        storage = FirebaseStorage.getInstance();
        mStorageRef = FirebaseStorage.getInstance().getReference();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mUsername = ANONYMOUS;

        // Inicializamos Firebase Auth
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();

        if (mFirebaseUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        } else {
            mUsername = mFirebaseUser.getDisplayName();
            mPhotoUrl = mFirebaseUser.getPhotoUrl().toString();
        }


        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API)
                .build();


    }
    @AfterPermissionGranted(RC_STORAGE_PERMS)
    @OnClick(R.id.food_photo)
    void takePicture(){

            //obteniendo permisos
            String[] perms = {android.Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
            if (!EasyPermissions.hasPermissions(this, perms)) {
                EasyPermissions.requestPermissions(this, getString(R.string.rationale_storage),
                        RC_STORAGE_PERMS, perms);
                return;
            }
        File dir = new File(Environment.getExternalStorageDirectory() + "/photos");
        File file = new File(dir, new Date().getTime() + ".jpg");
        try {

            if (!dir.exists()) {
                dir.mkdir();
            }
            boolean created = file.createNewFile();
            Log.d(TAG, "file.createNewFile:" + file.getAbsolutePath() + ":" + created);
        } catch (IOException e) {
            Log.e(TAG, "file.createNewFile" + file.getAbsolutePath() + ":FAILED", e);
        }

       captureImagePath = file.getAbsolutePath();

        uriFoodPhoto = FileProvider.getUriForFile(this,
                "com.camachoyury.applergy.fileprovider", file);
//
        //lanzar la camara
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, uriFoodPhoto);

        startActivityForResult(takePictureIntent, CAMERA_REQUEST);

    }

    @OnClick(R.id.send_food)
    void sendFood(){
        if (validateData()){
            uploadFromUri(uriFoodPhoto);
        }
    }

    private boolean validateData() {

        boolean result = true;
        if (editTextFoodName ==null || editTextFoodName.length()==0){
            Snackbar.make(linearLayout , "Food name not be empty", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            result = false;
        }
        if (editTextFoodType ==null || editTextFoodType.length()==0){
            Snackbar.make(linearLayout , "Food type not be empty", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
            result = false;
        }
        return result;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAMERA_REQUEST ) {
            if (requestCode == CAMERA_REQUEST && resultCode == RESULT_OK) {
                File file = new File(captureImagePath);
                imageView.setImageURI(Uri.fromFile(file));
            }
        }
    }


    private void uploadFromUri(Uri fileUri) {
        Log.d(TAG, "uploadFromUri:src:" + fileUri.toString());
        // traer la referenica las fotos/<FILENAME>.jpg
        final StorageReference photoRef = mStorageRef.child("food_images")
                .child(fileUri.getLastPathSegment());

        showProgressDialog();

        Log.d(TAG, "uploadFromUri:dst:" + photoRef.getPath());

        //subir la imagen
        photoRef.putFile(fileUri)
                .addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // subio sn errores
                        Log.d(TAG, "uploadFromUri:onSuccess");

                        // obtener la URL de referencia
                        savedFodUrl= taskSnapshot.getMetadata().getDownloadUrl();
                        //Guardar el objeto Foodd
                        saveData(savedFodUrl);

                        hideProgressDialog();


                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        // Fallo
                        Log.w(TAG, "uploadFromUri:onFailure", exception);

                        Toast.makeText(FoodActivity.this, "Error: upload failed",
                                Toast.LENGTH_SHORT).show();

                    }
                });
    }

    private void saveData(Uri savedFodUrl) {
        Log.d(TAG,savedFodUrl.toString());
        String ingredients  = "Trigo";
        Food food = new Food(savedFodUrl.toString(),
                editTextFoodName.getText().toString(),
                "LUCHETTI",
                editTextFoodType.getText().toString(),"Huevo",
                ingredients, FirebaseAuth.getInstance().getCurrentUser());
        mDatabase.child("food").push().setValue(food);

        editTextFoodName.setText("");
        editTextFoodType.setText("");
        imageView.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.camera_icon));


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {}

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {}

    private void showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setMessage("Loading...");
            mProgressDialog.setIndeterminate(true);
        }

        mProgressDialog.show();
    }

    private void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
