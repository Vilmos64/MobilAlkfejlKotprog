package com.example.kotprog;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;

public class CreateItemActivity extends AppCompatActivity {

    private static final int SELECT_PICTURE = 1;
    private static final int TAKE_PICTURE = 2;
    private FirebaseFirestore mFirestore;
    private CollectionReference mItems;
    final private int REQUEST_CODE_ASK_PERMISSIONS = 123;
    private Bitmap bitmap;
    private FirebaseStorage mStorage;
    private StorageReference storageRef;

    Button selectImageButton;

    Button backButton;
    ImageView previewImageView;
    Spinner typeSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_item);

        mFirestore = FirebaseFirestore.getInstance();
        mStorage = FirebaseStorage.getInstance();

        mItems = mFirestore.collection("Items");
        storageRef = mStorage.getReference();

        selectImageButton = findViewById(R.id.selectImageButton);
        previewImageView = findViewById(R.id.previewImageView);
        backButton = findViewById(R.id.backButton);

        typeSpinner = findViewById(R.id.itemTypeSpinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.itemType, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(adapter);
    }

    public void back(View view){
        Intent intent = new Intent(this, ShopListActivity.class);
        startActivity(intent);
    }
    public void createItem(View view) {
        EditText nameET = findViewById(R.id.itemNameEditText);
        String name = nameET.getText().toString();
        EditText descET = findViewById(R.id.itemDescEditText);
        String desc = descET.getText().toString();
        Spinner typeSpinner = findViewById(R.id.itemTypeSpinner);
        String type = typeSpinner.getSelectedItem().toString();
        EditText ratingET = findViewById(R.id.itemRatingEditText);
        float rating = Float.parseFloat(ratingET.getText().toString());
        EditText priceET = findViewById(R.id.itemPriceEditText);
        String price = priceET.getText().toString();
        int imageResource = 0;

        String path = "custom.jpg";

        if (name.isEmpty() || desc.isEmpty() || type.isEmpty() || price.isEmpty() || bitmap == null){
            Log.d("e", name + " " + desc + " " + type + " " + price + " " + bitmap);
            Toast.makeText(this, "Hibás adatok!", Toast.LENGTH_SHORT).show();
            return;
        }

        StorageReference mountainsRef = storageRef.child(path);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();

        UploadTask uploadTask = mountainsRef.putBytes(data);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.d("e", "Failed upload!");
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                //Valami
            }
        });

        mItems.add(new ShoppingItem(name, desc, price, rating, imageResource, type, 0,
                path));
        Toast.makeText(this, "Szőnyeg létrehozva!", Toast.LENGTH_SHORT).show();

        NotificationHelper nh = new NotificationHelper(this);
        nh.send("Új szőnyeg elérhető: " + name + "!");
    }

    public void imageChooser(View view) {

        // create an instance of the
        // intent of the type image
        Intent i = new Intent();
        i.setType("image/*");
        i.setAction(Intent.ACTION_GET_CONTENT);

        // pass the constant to compare it
        // with the returned requestCode
        startActivityForResult(Intent.createChooser(i, "Select Picture"), SELECT_PICTURE);
    }

    // this function is triggered when user
    // selects the image from the imageChooser
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {

            // compare the resultCode with the
            // SELECT_PICTURE constant
            if (requestCode == SELECT_PICTURE) {
                // Get the url of the image from data
                Uri selectedImageUri = data.getData();

                Bundle b = data.getExtras();
                try{
                    bitmap =  MediaStore.Images.Media.getBitmap(
                            this.getContentResolver(),
                            selectedImageUri);
                }
                catch (Exception e){

                }

                if (null != selectedImageUri) {
                    // update the preview image in the layout
                    previewImageView.setImageURI(selectedImageUri);
                }
            }

            if(requestCode == TAKE_PICTURE && resultCode == RESULT_OK) {
                Bundle b = data.getExtras();
                Bitmap img = (Bitmap) b.get("data");
                bitmap = img;
                previewImageView.setImageBitmap(img);
            }
        }
    }

    public void openCamera(View view) {
        checkUserPermission();
    }

    void checkUserPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_ASK_PERMISSIONS);
                return;
            }
        }

        takePicture();
    }

    private void takePicture() {
        Intent intent = new Intent();
        intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, TAKE_PICTURE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    takePicture();
                } else {
                    Toast.makeText(this, "Permission denied!", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}