package com.mikwee.timebrowser.fragments;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.mikwee.timebrowser.R;
import com.mikwee.timebrowser.utils.FileInfoExtended;
import com.mikwee.timebrowser.utils.Utils;

import org.jetbrains.annotations.NotNull;


public class InfoDialogFragment extends DialogFragment {
    public static final String TAG = InfoDialogFragment.class.getSimpleName();

    //Views
    private View v;
    private ProgressBar infoProgress;
    private View infoOverlay;

    @Override
    @NotNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        //create the layout
        v = getActivity().getLayoutInflater()
                .inflate(R.layout.dialog_file_info, null);

        // inflate progress until file is passed
        infoProgress = v.findViewById(R.id.info_progress);
        infoOverlay = v.findViewById(R.id.info_overlay);


        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(v).setNegativeButton(getString(R.string.dialog_info_close),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        InfoDialogFragment.this.getDialog().cancel();
                    }
                });

        return builder.create();
    }

    //Set file info once loading is finished
    public void refreshLayout(FileInfoExtended fi) {

        // remove progress and overlay
        infoOverlay.setVisibility(View.GONE);
        infoProgress.setVisibility(View.GONE);

        // fill in the text fields
        ((TextView) v.findViewById(R.id.file_name)).setText(fi.fileName);
        ((TextView) v.findViewById(R.id.file_full_name)).setText(fi.fileFullPath);
        ((TextView) v.findViewById(R.id.file_size)).setText(Utils.humanReadableByteCount(fi.fileSize, false));

        ((TextView) v.findViewById(R.id.create_time)).setText(fi.createTime);
        ((TextView) v.findViewById(R.id.last_modified)).setText(fi.lastModified);

        if (!fi.isHidden)
            (v.findViewById(R.id.is_hidden)).setVisibility(View.GONE);

        if (!fi.isWritable)
            (v.findViewById(R.id.is_writable)).setVisibility(View.GONE);

        if (!fi.isAccessDenied)
            (v.findViewById(R.id.is_access_denied)).setVisibility(View.GONE);

    }

}