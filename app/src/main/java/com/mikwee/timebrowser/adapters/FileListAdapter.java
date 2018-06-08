package com.mikwee.timebrowser.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.mikwee.timebrowser.R;
import com.mikwee.timebrowser.fragments.BrowserFragment;
import com.mikwee.timebrowser.utils.FileInfo;
import com.mikwee.timebrowser.utils.Utils;

import java.util.Arrays;

public class FileListAdapter extends RecyclerView.Adapter<FileListAdapter.ViewHolder> {

    public final static String TAG = FileListAdapter.class.getSimpleName();

    private FileInfo[] mDataSet;
    private Bitmap[] savedImages;
    private BrowserFragment fragment;
    private NewFileListAdapter.LayoutType layoutType;
    private Context context;


    public FileListAdapter(FileInfo[] dataSet, BrowserFragment fragment, NewFileListAdapter.LayoutType layoutType) {
        this.mDataSet = dataSet;
        this.fragment = fragment;
        this.layoutType = layoutType;
        this.savedImages = new Bitmap[mDataSet.length];
        this.context = fragment.getContext();
    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        //Set layout depensing on layout manager
        int id = R.layout.browser_file_item;
        if (layoutType == NewFileListAdapter.LayoutType.LAYOUT_GRID)
            id = R.layout.browser_file_item_grid;

        // Create a new view.
        View v = LayoutInflater.from(viewGroup.getContext())
                .inflate(id, viewGroup, false);

        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        holder.setInfo(mDataSet[position]);
    }

    @Override // Return the size of your dataset (invoked by the layout manager)
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
        this.mDataSet = newFils;
        this.savedImages = new Bitmap[newFils.length];
        notifyDataSetChanged();
    }

    //AsyncTask to load image in imageview
  /*  private class ImageLoaderAsyncTask extends AsyncTask<FileInfo, Void, Bitmap> {

        private ImageView image;
        private int pos;

        ImageLoaderAsyncTask(ImageView i, int pos) {
            this.image = i;
            this.pos = pos;
        }

        @Override
        protected Bitmap doInBackground(FileInfo... fileInfos) {


            Glide.with(context)
                    .load(file.fileStream)
                    .asBitmap()
                    .into(100, 100). // Width and height
                    get();
            // .thumbnail(0.5f) todo lean to implement because standard doesn't work
                                    .into(imageView);
            }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            image.setImageBitmap(bitmap);
        }
    }
*/

    class ViewHolder extends RecyclerView.ViewHolder {

        //Views
        private TextView fileNameTV;
        private TextView accessDeniedTV;
        private TextView fileSizeTV;
        private TextView fileOptions;
        private ImageView imageView;
        //File info
        private FileInfo file;

        private ViewHolder(View v) {
            super(v);

            //Get views from layout
            fileNameTV = (TextView) v.findViewById(R.id.dir_name);
            accessDeniedTV = (TextView) v.findViewById(R.id.access_denied);
            fileSizeTV = (TextView) v.findViewById(R.id.file_size);
            fileOptions = (TextView) v.findViewById(R.id.file_options);

            if (layoutType == NewFileListAdapter.LayoutType.LAYOUT_GRID)
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

                if (layoutType == NewFileListAdapter.LayoutType.LAYOUT_LINEAR)
                    fileNameTV.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_folder, 0, 0, 0);
                else
                    imageView.setImageResource(R.drawable.ic_folder);

            } else { //File is not directory

                String mime = Utils.getMIMEbyFileName(s);
                if (layoutType == NewFileListAdapter.LayoutType.LAYOUT_LINEAR) {
                    fileNameTV.setCompoundDrawablesWithIntrinsicBounds(Utils.getImageByMIME(mime), 0, 0, 0);

                } else {

                    imageView.setImageResource(Utils.getImageByMIME(mime));
                    imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);

                    if (mime.startsWith("image")) {
                        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

                        Bitmap tempB = savedImages[getAdapterPosition()];

                        if (tempB == null) {
                            // new ImageLoaderAsyncTask(imageView, getAdapterPosition()).execute(file);

                           /*
                            Glide.with(context)
                                    .load(file.fileStream)
                                    .asBitmap()
                                    .into(new SimpleTarget<Bitmap>(100, 100) {
                                        @Override
                                        public void onResourceReady(Bitmap resource, GlideAnimation glideAnimation) {
                                            savedImages[getAdapterPosition()] = resource;
                                            imageView.setImageBitmap(resource);
                                        }
                                    });
                                    */
                        } else {
                            imageView.setImageBitmap(tempB);
                        }

                    }


                }
            }

            //set file size info if present
            if (file.fileSize == 0) {
                fileSizeTV.setVisibility(View.GONE);
            } else {
                fileSizeTV.setVisibility(View.VISIBLE);
                fileSizeTV.setText(Utils.humanReadableByteCount(file.fileSize, false));
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