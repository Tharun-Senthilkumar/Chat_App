package com.example.chat_app.Fragments;


import android.app.Activity;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;



import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.chat_app.Model.User;
import com.example.chat_app.R;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;


public class ProfileFragment extends Fragment
{
    CircleImageView image_profile;
    TextView username;

    DatabaseReference reference;
    FirebaseUser fuser;

    StorageReference storageReference;
    private static final int IMAGE_REQUEST =1;
    private Uri imageUri;
    private StorageTask uploadTask;
    private static final int RESULT_OK = Activity.RESULT_OK;


    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private View view;

    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        view = inflater.inflate(R.layout.fragment_profile, container, false);
        image_profile=view.findViewById(R.id.profile_image);
        username=view.findViewById(R.id.username);
        storageReference = FirebaseStorage.getInstance().getReference("uploads");
        fuser = FirebaseAuth.getInstance().getCurrentUser();
        reference = FirebaseDatabase.getInstance().getReference("Users").child(fuser.getUid());

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        imageUri = result.getData().getData();
                        if (uploadTask != null && uploadTask.isInProgress()) {
                            Toast.makeText(getContext(), "Upload is in progress", Toast.LENGTH_SHORT).show();
                        } else {
                            uploadImage(view); // Use the class-level view variable here
                        }
                    }
                });
        reference.addValueEventListener(new ValueEventListener()
        {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                User user =dataSnapshot.getValue(User.class);
                username.setText(user.getUsername());
                if (user.getImageURL().equals("default"))
                {
                    image_profile.setImageResource(R.mipmap.ic_launcher);
                }
                else
                {
                    Glide.with(getContext()).load(user.getImageURL()).into(image_profile);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        image_profile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                openImage();
            }
        });

        return view;
    }

    private void openImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        imagePickerLauncher.launch(intent);
    }
    String getFileExtension(Uri uri)
    {
        ContentResolver contentResolver=getContext().getContentResolver();
        MimeTypeMap mimeTypeMap =MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri));
    }
    private void uploadImage(View rootView) {
        // Show the ProgressBar when starting the upload
        ProgressBar progressBar = rootView.findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.VISIBLE);

        if (imageUri != null) {
            final StorageReference fileReference = storageReference.child(System.currentTimeMillis() + "." + getFileExtension(imageUri));

            // Listen to the progress of the upload task
            uploadTask = fileReference.putFile(imageUri);
            uploadTask.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(@NonNull UploadTask.TaskSnapshot taskSnapshot) {
                    // Get the progress percentage and update the ProgressBar
                    double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                    progressBar.setProgress((int) progress);
                }
            });

            // Continue with the upload task and get the download URL
            uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return fileReference.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()) {
                        Uri downloadUri = task.getResult();
                        String mUri = downloadUri.toString();

                        // Update the user's profile with the new image URL in Firebase Realtime Database
                        FirebaseUser fuser = FirebaseAuth.getInstance().getCurrentUser();
                        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users").child(fuser.getUid());
                        HashMap<String, Object> map = new HashMap<>();
                        map.put("imageURL", mUri);
                        reference.updateChildren(map);
                        // Hide the ProgressBar
                        progressBar.setVisibility(View.GONE);

                        // Show a success message to the user
                        Toast.makeText(getContext(), "Image upload successful!", Toast.LENGTH_SHORT).show();
                    } else {
                        // Image upload failed
                        // Hide the ProgressBar
                        progressBar.setVisibility(View.GONE);

                        // Show an error message to the user
                        Toast.makeText(getContext(), "Image upload failed!", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else {
            // No image selected
            // Hide the ProgressBar
            progressBar.setVisibility(View.GONE);

            // Show a message to the user
            Toast.makeText(getContext(), "No image selected", Toast.LENGTH_SHORT).show();
        }
    }


}