package com.mikwee.timebrowser.fragments;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.mikwee.timebrowser.R;
import com.mikwee.timebrowser.adapters.FileChooserAdapter;

import org.jetbrains.annotations.NotNull;

public class FileChooserDialogFragment extends DialogFragment
        implements FileChooserAdapter.AdapterChannel {
    public static final String TAG = FileChooserDialogFragment.class.getSimpleName();

    //--------------Views
    private TextView location;
    private TextView noFiles;
    private FileChooserAdapter mAdapter;

    private Mode action;
    private boolean addingFolder = false;

    //The "mode" for the dialog
    public enum Mode {
        DOWNLOAD_CHOOSER,
        UPLOAD_CHOOSER
    }

    @Override
    @NotNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        //Get the type of dialog we want
        action = (Mode) getArguments().get(Mode.class.getSimpleName());

        //Get dialog button strings
        String positiveBtn = null;
        if (action == Mode.DOWNLOAD_CHOOSER) {
            positiveBtn = getString(R.string.dialog_chooser_download_positive);
        } else {
            positiveBtn = getString(R.string.dialog_chooser_upload_positive);
        }

        //Inflate dialog layout
        View v = getActivity().getLayoutInflater()
                .inflate(R.layout.dialog_chooser, null);

        //Init dialog views
        initViews(v);


        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(v)
                .setPositiveButton(positiveBtn, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (action == Mode.DOWNLOAD_CHOOSER)
                            ((DialogChooserCallback) getTargetFragment()).downloadPathChosen(mAdapter.getCurrentFolder());
                        else
                            ((DialogChooserCallback) getTargetFragment()).uploadFilesChosen(mAdapter.getCheckedFiles());
                    }
                })
                .setNegativeButton(getString(R.string.dialog_chooser_negative), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        FileChooserDialogFragment.this.getDialog().cancel();
                    }
                });

        return builder.create();
    }

    //Initialize dialog views
    private void initViews(View v) {


        LinearLayout addFolderWrapper;


        //Browsing location
        location = (TextView) v.findViewById(R.id.folder_location);
        //Empty background string
        noFiles = (TextView) v.findViewById(R.id.empty);


        addFolderWrapper = (LinearLayout) v.findViewById(R.id.add_folder_wrapper);

        //--------------------------RecyclerView
        final RecyclerView mRecyclerView = (RecyclerView) v.findViewById(R.id.dir_list);

        //decide which layout manager use
        if (action == Mode.UPLOAD_CHOOSER) {
            mRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
            ((TextView) v.findViewById(R.id.dialog_chooser_title)).setText(R.string.dialog_chooser_upload_title);
        } else {
            mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            ((TextView) v.findViewById(R.id.dialog_chooser_title)).setText(R.string.dialog_chooser_download_title);
        }

        mAdapter = new FileChooserAdapter(this, action);
        mRecyclerView.setAdapter(mAdapter);

        //--------------------------Back Dir button
        ((ImageView) v.findViewById(R.id.back)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAdapter.backward();
            }
        });

        //If upload chooser we don't need to create a new file_folder
        if (action == Mode.UPLOAD_CHOOSER) {
            addFolderWrapper.setVisibility(View.GONE);
            return;
        }


        //Add file_folder Views
        final TextView addFolderTV = (TextView) v.findViewById(R.id.tv_add_folder);
        final ImageView addFolderIcon = (ImageView) v.findViewById(R.id.iv_add_folder);
        final EditText addFolderET = (EditText) v.findViewById(R.id.et_add_folder);

        addFolderWrapper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (addingFolder) {
                    String folderName = addFolderET.getText().toString();
                    //if foldername is not empty
                    if (!folderName.equals("")) {
                        addingFolder = false;

                        addFolderET.setVisibility(View.GONE);
                        addFolderTV.setVisibility(View.VISIBLE);
                        addFolderIcon.setImageResource(R.drawable.ic_add);

                        //Add file_folder and refresh
                        ((FileChooserAdapter) mRecyclerView.getAdapter()).addFolder(folderName);
                    } else {
                        Toast.makeText(getContext(), getString(R.string.empty_dir), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    addingFolder = true;
                    addFolderET.setVisibility(View.VISIBLE);
                    addFolderTV.setVisibility(View.GONE);

                    addFolderIcon.setImageResource(R.drawable.ic_check);
                    addFolderET.requestFocus();

                }
            }
        });

    }


    //--------------Callbacks from adapter

    @Override //Update location on navbar OK
    public void updateLocation(String url) {
        location.setText(url);
        noFiles.setVisibility(View.GONE);
    }

    @Override //called to inform no files in folder OK
    public void noFiles() {
        noFiles.setVisibility(View.VISIBLE);
    }


    //Interface to communicate back to Fragment
    public interface DialogChooserCallback {

        void downloadPathChosen(String targetUrl);

        void uploadFilesChosen(String[] files);

    }


}

