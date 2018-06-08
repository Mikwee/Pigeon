package com.mikwee.timebrowser.utils;

import java.util.Comparator;

public class FileInfo implements Comparable {
    public String fileFullPath;
    public String fileName;
    public long fileSize;
    public boolean isDirectory;

    @Override
    public boolean equals(Object obj) {
        if (obj.getClass() != FileInfo.class)
            return false;

        FileInfo f = (FileInfo) obj;
        return f.fileFullPath.equals(this.fileFullPath);

    }

    public int compareTo(Object obj) {
        FileInfo fi = (FileInfo) obj;
        if (this.isDirectory && !fi.isDirectory) {
            return -1;
        }
        if (this.isDirectory || !fi.isDirectory) {
            return this.fileName.compareTo(fi.fileName);
        }
        return 1;
    }

    //compare files based if they are directory or files

    public static Comparator<FileInfo> DirectoryFirstComparator = new Comparator<FileInfo>() {


        @Override
        public int compare(FileInfo fruit1, FileInfo fruit2) {
            if (fruit1.isDirectory) {
                if (fruit2.isDirectory)
                    return fruit1.fileName.compareTo(fruit2.fileName);
                else
                    return -1;

            } else if (fruit2.isDirectory) {
                return 1;
            } else {
                return fruit1.fileName.compareTo(fruit2.fileName);
            }
        }

    };

    public static Comparator<FileInfo> FilesFirstComparator = new Comparator<FileInfo>() {


        @Override
        public int compare(FileInfo fruit1, FileInfo fruit2) {
            if (fruit1.isDirectory) {
                if (fruit2.isDirectory)
                    return fruit1.fileName.compareTo(fruit2.fileName);
                else
                    return 1;

            } else if (fruit2.isDirectory) {
                return -1;
            } else {
                return fruit1.fileName.compareTo(fruit2.fileName);
            }
        }

    };

    public static Comparator<FileInfo> FileSizeComparatorAscending = new Comparator<FileInfo>() {

        @Override
        public int compare(FileInfo o1, FileInfo o2) {
            if (o1.fileSize > o2.fileSize)
                return 1;
            else if (o1.fileSize < o2.fileSize)
                return -1;
            else return 0;
        }
    };


    public static Comparator<FileInfo> FileSizeComparatorDescending = new Comparator<FileInfo>() {

        @Override
        public int compare(FileInfo o1, FileInfo o2) {
            if (o1.fileSize > o2.fileSize)
                return -1;
            else if (o1.fileSize < o2.fileSize)
                return 1;
            else return 0;
        }
    };
}

