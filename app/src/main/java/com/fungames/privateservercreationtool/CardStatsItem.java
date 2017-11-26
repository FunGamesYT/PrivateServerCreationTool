package com.fungames.privateservercreationtool;

/**
 * Created by Fabian on 26.11.2017.
 */

public class CardStatsItem {
    private String fileName;
    private String filePath;

    public CardStatsItem(String fileName, String filePath) {
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
}
