package com.rwz.qidianhook.entity.response;

import com.rwz.qidianhook.config.Constance;

public class VIPTextResponseEntity {

    private int Code;
    private long BookID;
    private long ChapterID;
    private String Content;

    public VIPTextResponseEntity(long bookID, long chapterID, String content) {
        Code = Constance.DOWN_VIP_TXT_DECRYPT_RESULT;
        BookID = bookID;
        ChapterID = chapterID;
        Content = content;
    }

    public int getCode() {
        return Code;
    }

    public long getBookID() {
        return BookID;
    }

    public long getChapterID() {
        return ChapterID;
    }

    public String getContent() {
        return Content;
    }
}
