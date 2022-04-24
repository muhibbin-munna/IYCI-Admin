package com.indian.iyciadmin.ui;

import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.indian.iyciadmin.MainActivity;
import com.indian.iyciadmin.R;
import com.indian.iyciadmin.UploadNotification;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class HomeFragment extends Fragment {


    EditText titleEditText, bodyEditText, senderIdEditText;
    Button sendNotificationButton, updateButton, downloadbutton;
    String AUTH_KEY;
    TextView senderIdTv, totaluserTv;
    private DatabaseReference mDatabaseRef, senderIdRef, userRef;
    ProgressDialog pd;
    private static String URL = "https://indian-youth-career-info.firebaseio.com/.json";

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
                View root = inflater.inflate(R.layout.fragment_home, container, false);
        titleEditText = root.findViewById(R.id.title);
        bodyEditText = root.findViewById(R.id.body);
        senderIdEditText = root.findViewById(R.id.senderId);
        sendNotificationButton = root.findViewById(R.id.sendButton);
        updateButton = root.findViewById(R.id.updateButton);
        senderIdTv = root.findViewById(R.id.senderIdTv);
        totaluserTv = root.findViewById(R.id.totaluserTv);
        downloadbutton = root.findViewById(R.id.downloadbutton);
        pd = new ProgressDialog(getContext());

        mDatabaseRef = FirebaseDatabase.getInstance().getReference("Notification");
        senderIdRef = FirebaseDatabase.getInstance().getReference("SenderId");
        userRef = FirebaseDatabase.getInstance().getReference("Users");


        senderIdRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String senderIdText;
                senderIdText = snapshot.getValue().toString().toLowerCase();
                senderIdTv.setText("Sender ID : " + senderIdText);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int totalUser;
                totalUser = (int) snapshot.getChildrenCount();
                totaluserTv.setText("Total Users : " + totalUser);
                Log.d("TAG", "onDataChange: " + totalUser);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        sendNotificationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getContext(), "Notification Sent", Toast.LENGTH_SHORT).show();
                sendPushNotification();
                uploadNotification();
            }
        });
        updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateSenderId();
            }
        });

        downloadbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                new JsonTask().execute("https://indian-youth-career-info.firebaseio.com/.json");

                if (checkPermission()) {
                    makeAPICall();
                    pd.setMessage("Please wait");
                    pd.setCancelable(false);
                    pd.show();
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        requestPermissions(new String[]{WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE}, 111);
                    }
                }
            }
        });
        return  root;
    }

    private boolean checkPermission() {
        int write = ContextCompat.checkSelfPermission(getContext(),WRITE_EXTERNAL_STORAGE);
        int read =  ContextCompat.checkSelfPermission(getContext(),READ_EXTERNAL_STORAGE);
        return write== PackageManager.PERMISSION_GRANTED && read == PackageManager.PERMISSION_GRANTED;
    }

    private void makeAPICall()
    {
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    //download the json onto the device.
                    URL url = new URL(URL);
                    URLConnection connection = url.openConnection();
                    //read the data and store it in a byte array first.
                    //dont use this code for big json file
                    //as long as its a json and not very long, this will work just fine!!!
                    InputStream inStream = connection.getInputStream();
                    ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
                    int readByte = 0;
                    //read the bytes one-by-one from the inputstream to the buffer.
                    while(true)
                    {
                        readByte = inStream.read();
                        if(readByte == -1)
                        {
                            break;
                        }
                        byteOutStream.write(readByte);
                    }
                    byteOutStream.flush();
                    inStream.close();
                    byteOutStream.close();
                    byte[] response = byteOutStream.toByteArray();
                    //now response byte array is the complete json in the biary form. We will save this stuff to file.
//                    File cacheDir = MainActivity.this.getCacheDir();
                    String root = Environment.getExternalStorageDirectory().toString();
                    File jsonFile = new File(root + "/IYCI");
                    if (!jsonFile.exists()) {
                        jsonFile.mkdirs();
                    }
                    String name = "json_"+System.currentTimeMillis()+ ".json";
                    jsonFile = new File(jsonFile,name);
                    FileOutputStream outStream = new FileOutputStream(jsonFile);
                    //write the whole data into the file
                    for(int i = 0; i < response.length; i++)
                    {
                        outStream.write(response[i]);
                    }
                    Log.e("status - ","API call is complete!!");
                    Toast.makeText(getContext(), "Completed", Toast.LENGTH_SHORT).show();
                    //this should do the trick of saving all the stuff in the file with file name CachedResponse!!
                    //let see if we can retrieve the stuff!!!
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
                if (pd.isShowing()){
                    pd.dismiss();
                }



            }
        }).start();
        Toast.makeText(getActivity(), "Completed", Toast.LENGTH_SHORT).show();
    }

    private void updateSenderId() {
        String senderidstring;
        senderidstring = senderIdEditText.getText().toString().toLowerCase();
        senderIdRef.setValue(senderidstring).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Toast.makeText(getActivity(), "Updated", Toast.LENGTH_SHORT).show();
                senderIdEditText.getText().clear();
            }
        });

    }

    private void uploadNotification() {
        String title,body;
        title = titleEditText.getText().toString();
        body = bodyEditText.getText().toString();
        final String uploadId = mDatabaseRef.push().getKey();
        UploadNotification uploadNotification = new UploadNotification(title,body,System.currentTimeMillis());
        mDatabaseRef.child(uploadId).setValue(uploadNotification).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                Toast.makeText(getContext(), "Notification Uploaded", Toast.LENGTH_SHORT).show();
                titleEditText.getText().clear();
                bodyEditText.getText().clear();
            }
        });
    }

    private void sendPushNotification() {
        AUTH_KEY = "AAAAp9IMwCk:APA91bF1IyEJtdbcqGSx-pIjqfAmLpGTRBaoYGUBMyqeFWZrJR31wmEXMTiolwO_YFaHqvMkNHo16cCSmpMcRXUy_z-D9rtDtM6Pu4oibUB-Zhe0xFN8VN_LOLqGjI495aqeHekSooCC";
        FirebaseMessaging.getInstance().subscribeToTopic("all");

        new Thread(new Runnable() {
            @Override
            public void run() {
                String title,body;
                title = titleEditText.getText().toString();
                body = bodyEditText.getText().toString();
                pushNotification(title,body);

            }
        }).start();
    }

    private void pushNotification(String title, String body) {
        JSONObject jPayload = new JSONObject();
        JSONObject jNotification = new JSONObject();
        JSONObject jData = new JSONObject();
        try {
            // notification can not put when app is in background
            jNotification.put("title", title);
            jNotification.put("body", body);
            jNotification.put("sound", "default");
            jNotification.put("badge", "1");
            jNotification.put("icon", "logo1");
            jNotification.put("color", "#000000");

            //to token of any deivce
            jPayload.put("to", "/topics/all");

            // data can put when app is in background
            jData.put("goto_which", "NotificationFragment");
//            jData.put("user_id", mCurrentUserId);

            jPayload.put("priority", "high");
            jPayload.put("notification", jNotification);
            jPayload.put("data", jData);
//            jPayload.put("image", "https://media.sproutsocial.com/uploads/2017/02/10x-featured-social-media-image-size.png");


            URL url = new URL("https://fcm.googleapis.com/fcm/send");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "key=" + AUTH_KEY);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            // Send FCM message content.
            OutputStream outputStream = conn.getOutputStream();
            outputStream.write(jPayload.toString().getBytes());

            // Read FCM response.
            InputStream inputStream = conn.getInputStream();
            final String resp = convertStreamToString(inputStream);

        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
    }

    private String convertStreamToString(InputStream is) {
        Scanner s = new Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next().replace(",", ",\n") : "";
    }
}