package com.indian.iyciadmin.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.indian.iyciadmin.Adapters.NotificationAdapter;
import com.indian.iyciadmin.R;
import com.indian.iyciadmin.UploadNotification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GalleryFragment extends Fragment implements NotificationAdapter.OnItemClickListener {
    private static final String TAG = "NotificationFragment";
    SwipeRefreshLayout swipeLayout;
    RecyclerView notificationRv;
    private DatabaseReference mDatabaseRef;
    private NotificationAdapter mAdapter;
    private ProgressBar mProgressCircle;
    private List<UploadNotification> mUploads;
    private LinearLayoutManager mlayoutManager;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_gallery, container, false);

        notificationRv = view.findViewById(R.id.notification_rv);
        swipeLayout = view.findViewById(R.id.notification_swipe_container);
        mProgressCircle = view.findViewById(R.id.progressbarId);
        mlayoutManager = new LinearLayoutManager(getContext());
        notificationRv.setLayoutManager(mlayoutManager);
        mUploads = new ArrayList<>();
        mDatabaseRef = FirebaseDatabase.getInstance().getReference("Notification");

        loadNotification();

        swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadNotification();
                mAdapter.notifyDataSetChanged();
                swipeLayout.setRefreshing(false);
            }
        });

        return view;
    }

    private void loadNotification() {

        mDatabaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                mUploads.clear();
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    UploadNotification uploadNotification = postSnapshot.getValue(UploadNotification.class);
                    mUploads.add(uploadNotification);
                }
                Collections.reverse(mUploads);
                mAdapter = new NotificationAdapter(getContext(), mUploads);

                notificationRv.setAdapter(mAdapter);
                mAdapter.setOnItemClickListener(GalleryFragment.this);
                mProgressCircle.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getContext(), databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                mProgressCircle.setVisibility(View.INVISIBLE);
            }
        });
    }

    @Override
    public void onItemClick(int position) {

    }

    @Override
    public void onDeleteClick(final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setMessage("Delete Permanently?")
                .setPositiveButton("delete", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Query queryRef = mDatabaseRef.orderByChild("time").equalTo(mUploads.get(position).getTime());
                        queryRef.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                for (DataSnapshot child : snapshot.getChildren()) {
                                    if (child.getKey() != null) {
                                        mDatabaseRef.child(child.getKey()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Toast.makeText(getContext(), "deleted successfully", Toast.LENGTH_SHORT).show();
                                                loadNotification();
                                            }
                                        });
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });

                    }
                })
                .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                }).show();

    }
}