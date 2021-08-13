package com.example.khuscholarshipnotifier;

public class Item {

    private String mTitle;   //공지 제목
    private String mLink;    //상세 정보 페이지 링크
    private String mCampus;  //국제 혹은 서울
    private String mDate;    //공지 등록일
    private boolean hasFile;    //첨부파일 존재 여부
    private String mType;   //장학 종류(교내,대외,기타)

    public Item(String Title, String Link, String Campus, String Date, boolean hasFile, String Type) {
        this.mTitle = Title;
        this.mLink = Link;
        this.mCampus = Campus;
        this.mDate = Date;
        this.hasFile = hasFile;
        this.mType = Type;
    }

    public String getmTitle() {
        return mTitle;
    }

    public void setmTitle(String mTitle) {
        this.mTitle = mTitle;
    }

    public String getmLink() {
        return mLink;
    }

    public void setmLink(String mLink) {
        this.mLink = mLink;
    }

    public String getmCampus() {
        return mCampus;
    }

    public void setmCampus(String mCampus) {
        this.mCampus = mCampus;
    }

    public String getmDate() {
        return mDate;
    }

    public void setmDate(String mDate) {
        this.mDate = mDate;
    }

    public boolean getHasFile() {
        return hasFile;
    }

    public void setHasFile(boolean hasFile) {
        this.hasFile = hasFile;
    }

    public String getmType() {
        return mType;
    }

    public void setmType(String mType) {
        this.mType = mType;
    }
}
