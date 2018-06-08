package com.mikwee.timebrowser.adapters;

import android.content.Context;
import android.os.Environment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.mikwee.timebrowser.R;
import com.mikwee.timebrowser.fragments.FileChooserDialogFragment;
import com.mikwee.timebrowser.glide.GlideApp;
import com.mikwee.timebrowser.utils.Utils;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;


public class FileChooserAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final String TAG = FileChooserAdapter.class.getSimpleName();


    //The dialog "mode" (upload or download)
    private FileChooserDialogFragment.Mode mode;
    //Message tunnel to transfer info
    private AdapterChannel adapterChannel;
    //Current File we are in
    private File currentFile;

    private List<String> filePaths = new ArrayList<>();
    private File[] files;

    private Context c;


    public FileChooserAdapter(FileChooserDialogFragment fragment, FileChooserDialogFragment.Mode mode) {
        this.mode = mode;
        this.adapterChannel = (AdapterChannel) fragment;
        this.c = fragment.getContext();

        //Get the base directory
        File f = Environment.getExternalStorageDirectory();
        //Navigate to base directory
        forward(f);
    }

    @Override //Create viewHolder OK
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        switch (viewType) {
            case 0:
                View vDownload = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.dialog_chooser_download_item, parent, false);
                return new ViewHolderDownload(vDownload);

            default:
                View vUpload = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.dialog_chooser_upload_item, parent, false);
                return new ViewHolderUpload(vUpload);
        }
    }

    @Override //get view type based on our mode OK
    public int getItemViewType(int position) {
        return mode.ordinal();
    }

    @Override //Bind info to right viewholder OK
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        switch (holder.getItemViewType()) {
            case 0:
                ((ViewHolderDownload) holder).setInfo(position);
                break;
            default:
                ((ViewHolderUpload) holder).setInfo(position);
                break;
        }

    }

    @Override //OK
    public int getItemCount() {
        return files.length;
    }

    //---------------------------- INNER CLASSES AND INTERFACES

    //Defines methods the fragment must implement and handle
    public interface AdapterChannel {

        void updateLocation(String url);

        void noFiles();
    }


    //---------------------------- DOWNLOAD UTILS --------------------------------------------------

    public class ViewHolderDownload extends RecyclerView.ViewHolder {
        private TextView dirName;
        private File f;
        private ImageView imageView;

        ViewHolderDownload(View v) {
            super(v);

            imageView = (ImageView) v.findViewById(R.id.dialog_choser_image);

            dirName = (TextView) v.findViewById(R.id.dir_name);

            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //move into directory
                    forward(f);
                }
            });

        }

        //Populate view with file info
        public void setInfo(int position) {
            f = files[position];

            String mime = "file_folder";
            if (!f.isDirectory()) {
                mime = Utils.getMIMEbyFileName(f.getName());
            }

            //Set file icon
            GlideApp.with(c)
                    .load(Utils.getImageByMIME(mime))
                    .placeholder(Utils.getImageByMIME(mime))
                    .into(imageView);

            dirName.setText(f.getName());
        }
    }

    //Add directory to file
    public void addFolder(String folderName) {
        File f = new File(currentFile, folderName);
        //try creating new file_folder
        if (f.mkdir())
            forward(currentFile);
        else
            Toast.makeText(c, c.getString(R.string.dialog_chooser_error_creating_folder), Toast.LENGTH_SHORT).show();
    }

    //Get folder we are in
    public String getCurrentFolder() {
        return currentFile.getAbsolutePath();
    }

    //---------------------------- UPLOAD UTILS ----------------------------------------------------

    public class ViewHolderUpload extends RecyclerView.ViewHolder {
        //Views
        private ImageView fileType;
        private TextView dirName;
        private CheckBox checkBox;
        //File info
        private File f;

        private ViewHolderUpload(View v) {
            super(v);
            //Init views
            fileType = (ImageView) v.findViewById(R.id.iv_file_type);
            dirName = (TextView) v.findViewById(R.id.dir_name);
            checkBox = (CheckBox) v.findViewById(R.id.check_box);
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (f.isDirectory())
                        forward(f);
                }
            });

        }

        //Set info about a specific row
        public void setInfo(int position) {
            f = files[position];

            //Set File name
            dirName.setText(f.getName());

            //Set appropriate icon
            if (f.isDirectory()) {
                //Set file_folder icon
                Picasso.with(c)
                        .load(R.drawable.ic_folder)
                        .into(fileType);
            } else {

                String mime = Utils.getMIMEbyFileName(f.getName());

                if (mime.startsWith("image")) {
                    Picasso.with(c)
                            .load(f)
                            .placeholder(R.drawable.ic_image_file)
                            .error(R.drawable.ic_image_file)
                            .resize(120, 120)
                            .centerCrop()
                            .into(fileType);
                } else {
                    Picasso.with(c)
                            .load(Utils.getImageByMIME(mime))
                            .into(fileType);
                }
            }

            //In some cases, it will prevent unwanted situations
            checkBox.setOnCheckedChangeListener(null);
            //Set if item was checked or not
            checkBox.setChecked(filePaths.contains(f.getAbsolutePath()));
            //On check status change
            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                    if (isChecked)//Add item to list if is being checked
                        filePaths.add(f.getAbsolutePath());
                    else          //Remove it from list otherwise
                        filePaths.remove(f.getAbsolutePath());
                }
            });
        }
    }

    //---------------------------- OTHER UTILS -----------------------------------------------------

    //Move into directory
    private void forward(File directory) {
        currentFile = directory;
        adapterChannel.updateLocation(directory.getAbsolutePath());

        //if we are in download mode
        if (mode == FileChooserDialogFragment.Mode.DOWNLOAD_CHOOSER) {
            //Filter only directories
            files = directory.listFiles(new FileFilter() {
                public boolean accept(File f) {
                    return f.isDirectory();
                }
            });
        } else //else show all files
            files = directory.listFiles();


        //show empty string
        if (files.length == 0)
            adapterChannel.noFiles();


        //notify data changed
        notifyDataSetChanged();
    }

    //Navigate to previous directory
    public void backward() {
        File f = currentFile.getParentFile();
        if (f != null)
            forward(f);
    }


    public String[] getCheckedFiles() {
        return filePaths.toArray(new String[filePaths.size()]);
    }

}
