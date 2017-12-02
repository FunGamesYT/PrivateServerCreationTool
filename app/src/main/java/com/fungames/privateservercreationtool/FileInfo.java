package com.fungames.privateservercreationtool;

/**
 * Created by Fabian on 26.11.2017.
 */

public class FileInfo {
    private String fileName;
    private String filePath;

    public FileInfo(String fileName, String filePath) {
        this.fileName = fileName;
        this.filePath = filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    @Override
    public String toString() {
        return fileName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FileInfo fileInfo = (FileInfo) o;

        if (!fileName.equals(fileInfo.fileName)) return false;
        return filePath.equals(fileInfo.filePath);
    }

    @Override
    public int hashCode() {
        int result = fileName.hashCode();
        result = 31 * result + filePath.hashCode();
        return result;
    }
}
