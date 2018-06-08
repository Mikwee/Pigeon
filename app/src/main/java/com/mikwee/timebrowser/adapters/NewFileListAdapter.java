package com.mikwee.timebrowser.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.mikwee.timebrowser.R;
import com.mikwee.timebrowser.components.SelectedBar;
import com.mikwee.timebrowser.fragments.BrowserFragment;
import com.mikwee.timebrowser.glide.GlideApp;
import com.mikwee.timebrowser.glide.SmbFileModel;
import com.mikwee.timebrowser.utils.FileInfo;
import com.mikwee.timebrowser.utils.Utils;

import java.util.Arrays;

import jcifs.smb.NtlmPasswordAuthentication;

public class NewFileListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final static String TAG = FileListAdapter.class.getSimpleName();


    private FileInfo[] mDataSet;
    private BrowserFragment fragment;
    private SelectedBar selection;
    private LayoutType layoutType;
    private Context context;
    private static NtlmPasswordAuthentication auth;


    public NewFileListAdapter(FileInfo[] dataSet, BrowserFragment fragment, LayoutType layoutType) {
        this.mDataSet = dataSet;
        this.fragment = fragment;
        this.layoutType = layoutType;

        this.context = fragment.getContext();
        this.auth = fragment.getAuth();
        this.selection = fragment.selectedBar;

    }


    @Override //OK
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {

        //Set layout depending on layout manager
        if (layoutType == LayoutType.LAYOUT_GRID) {
            View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.browser_file_item_grid, viewGroup, false);
            return new ViewHolderGrid(v);
        } else {
            View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.browser_file_item, viewGroup, false);
            return new ViewHolderLinear(v);
        }

    }

    @Override //OK
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
        if (layoutType == LayoutType.LAYOUT_GRID) {
            ((ViewHolderGrid) holder).setInfo(mDataSet[position]);
        } else {
            ((ViewHolderLinear) holder).setInfo(mDataSet[position]);
        }
    }

    @Override //OK
    public int getItemCount() {
        return mDataSet.length;
    }


    //sort items with selected order and rearrange items
    public void sortBy(Utils.Sort sortType) {
        switch (sortType) {
            case SIZE_ASCENDING:
                Arrays.sort(mDataSet, FileInfo.FileSizeComparatorAscending);
                break;

            case SIZE_DESCENDING:
                Arrays.sort(mDataSet, FileInfo.FileSizeComparatorDescending);
                break;

            case FILES_FIRST:
                Arrays.sort(mDataSet, FileInfo.FilesFirstComparator);
                break;

            case FOLDER_FIRST:
                Arrays.sort(mDataSet, FileInfo.DirectoryFirstComparator);
                break;
        }
        notifyDataSetChanged();
    }

    //directory files are changed, update
    public void updateFilesList(FileInfo[] newFils) {
        this.auth = fragment.getAuth();
        this.mDataSet = newFils;
        notifyDataSetChanged();
    }


    //-----------------------------------ENUMS AND CLASSES------------------------------------------


    public enum LayoutType {
        LAYOUT_GRID,
        LAYOUT_LINEAR
    }

    //Viewholder for grid layout
    public class ViewHolderGrid extends RecyclerView.ViewHolder {

        //Views
        private TextView fileNameTV, fileOptions;
        private ImageView imageView;
        private View fileType;
        private CheckBox checkBox;

        //File info
        private FileInfo file;

        private ViewHolderGrid(View v) {
            super(v);

            //Get views from layout
            fileNameTV = (TextView) v.findViewById(R.id.dir_name);
            fileOptions = (TextView) v.findViewById(R.id.file_options);
            imageView = (ImageView) v.findViewById(R.id.bigIcon);
            fileType = v.findViewById(R.id.underline);
            checkBox = (CheckBox) v.findViewById(R.id.selected_checkbox);

            //react to general click
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    //If we are in "Selection mode"
                    if (selection.inSelection()) {

                        //if checkbox is already checked
                        if (checkBox.isChecked()) {

                            //remove selection
                            selection.removeSelectedFile(file);
                            checkBox.setChecked(false);

                        } else { //checkbox not clicked

                            //select file
                            selection.addSelectedFile(file);
                            checkBox.setChecked(true);
                        }

                    } else {

                        //If item is a directory
                        if (file.isDirectory)
                            fragment.brows(file.fileFullPath); //Brows inside
                        else
                            openMenu();

                    }


                }
            });


            v.setOnLongClickListener(
                    new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {

                            //If we are in "Selection mode"  and  checkbox is not already checked
                            if (!selection.inSelection() || !checkBox.isChecked()) {
                                selection.addSelectedFile(file);
                                checkBox.setChecked(true);
                            }
                            return true;
                        }
                    }
            );

            //react to menu click
            fileOptions.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    openMenu();
                }
            });

            //clicks handled by parent
            checkBox.setClickable(false);


        }

        //Set info to each item
        private void setInfo(FileInfo file) {
            this.file = file;

            //set file name
            fileNameTV.setText(file.fileName);


            String mime = "file_folder";
            if (!file.isDirectory) {
                mime = Utils.getMIMEbyFileName(file.fileName);
            }


            fileType.setBackgroundColor(Utils.getColorByMIME(mime, context));
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);

            //set Icon
            if (mime.startsWith("image")) {
                GlideApp.with(context)
                        .load(new SmbFileModel(auth, file.fileFullPath))
                        .placeholder(R.drawable.file_img)
                        .centerCrop()
                        .override(100, 100)
                        .into(imageView);
                //todo callback in case of loading error
            } else {
                GlideApp.with(context)
                        .load(Utils.getImageByMIME(mime))
                        .placeholder(Utils.getImageByMIME(mime))
                        .into(imageView);
            }

            //If we are in selection mode
            if (selection.inSelection()) {

                //make checkbox visible
                showSelection();

                if (selection.getSelectedFiles().contains(file))
                    checkBox.setChecked(true);
                else
                    checkBox.setChecked(false);
            }

        }

        //Open menu with actions for each file
        private void openMenu() {
            //creating a popup menu
            PopupMenu popup = new PopupMenu(fragment.getContext(), fileOptions);

            //inflating menu from xml resource
            popup.inflate(R.menu.menu_file_actions);

            //adding click listener
            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    switch (item.getItemId()) {

                        case R.id.action_info:
                            fragment.showFileInfo(file);
                            break;

                        case R.id.action_share:
                            // fragment.openFile(file.fileFullPath);
                            break;

                        case R.id.action_download:
                            fragment.showDownloadChooser();
                            break;

                        case R.id.action_delete:
                            fragment.deleteFile(file.fileFullPath);
                            break;
                    }
                    return false;
                }
            });

            //displaying the popup
            popup.show();
        }

        //Show checkbox
        public void showSelection() {
            checkBox.setVisibility(View.VISIBLE);
            fileOptions.setVisibility(View.GONE);
        }

        //Hide checkbox
        public void hideSelection() {
            checkBox.setChecked(false);
            checkBox.setVisibility(View.GONE);
            fileOptions.setVisibility(View.VISIBLE);
        }

    }


    //Viewholder for linear layout
    private class ViewHolderLinear extends RecyclerView.ViewHolder {

        //Views
        private TextView fileNameTV, fileOptions;
        private ImageView imageView;

        //File info
        private FileInfo file;

        private ViewHolderLinear(View v) {
            super(v);

            //Get views from layout
            fileNameTV = (TextView) v.findViewById(R.id.dir_name);
            fileOptions = (TextView) v.findViewById(R.id.file_options);

            if (layoutType == LayoutType.LAYOUT_GRID)
                imageView = (ImageView) v.findViewById(R.id.bigIcon);

            //react to general click
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    //If item is a directory
                    if (file.isDirectory) {
                        //Brows inside
                        fragment.brows(file.fileFullPath);
                    } else { //If item is a file
                        openMenu();
                    }
                }
            });

            //react to menu click
            fileOptions.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    openMenu();
                }
            });


        }

        //Set info to each row
        private void setInfo(FileInfo file) {
            this.file = file;
            String s = file.fileName;

            //Is file directory?
            if (file.isDirectory) {

                if (layoutType == LayoutType.LAYOUT_LINEAR)
                    fileNameTV.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_folder, 0, 0, 0);
                else
                    imageView.setImageResource(R.drawable.ic_folder);

            } else { //File is not directory

                String mime = Utils.getMIMEbyFileName(s);
                if (layoutType == LayoutType.LAYOUT_LINEAR) {
                    fileNameTV.setCompoundDrawablesWithIntrinsicBounds(Utils.getImageByMIME(mime), 0, 0, 0);

                } else {

                    imageView.setImageResource(Utils.getImageByMIME(mime));
                    imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);

                    if (mime.startsWith("image")) {
                        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

                        Bitmap tempB = null; // = savedImages[getAdapterPosition()];

                        if (tempB == null) {
                            Glide.with(context)
                                    .load(file.fileFullPath) //this doesn't work
                                    .into(imageView);
                        } else {
                            imageView.setImageBitmap(tempB);
                        }

                    }


                }
            }

            //set directory name
            fileNameTV.setText(file.fileName);

        }

        //Open menu with actions for each file
        private void openMenu() {
            //creating a popup menu
            PopupMenu popup = new PopupMenu(fragment.getContext(), fileOptions);

            //inflating menu from xml resource
            popup.inflate(R.menu.menu_file_actions);

            //adding click listener
            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    switch (item.getItemId()) {
                        /*
                        case R.id.action_open:
                            fragment.openFile(file.fileFullPath);
                            break;*/
                        case R.id.action_download:
                            fragment.showDownloadChooser();
                            break;
                        case R.id.action_info:
                            fragment.showFileInfo(file);
                            break;
                        case R.id.action_delete:
                            fragment.deleteFile(file.fileFullPath);
                            break;
                    }

                    return false;
                }
            });
            //displaying the popup
            popup.show();
        }

    }


}