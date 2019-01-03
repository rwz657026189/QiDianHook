package com.rwz.qidianhook.entity.request;

import java.util.ArrayList;

public class VIPTextEntity {

    private int Code;
    private long BookID;
    private ArrayList<String> ChapterIDs;

    public int getCode() {
        return Code;
    }

    public long getBookID() {
        return BookID;
    }

    public ArrayList<String> getChapterIDs() {
        return ChapterIDs;
    }
}
