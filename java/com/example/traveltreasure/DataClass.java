package com.example.traveltreasure;

public class DataClass {

    private String dataname;
    private String datadate;
    private String dataimage;

    public String getDataname() {
        return dataname;
    }

    public String getDatadate() {
        return datadate;
    }

    public String getDataimage() {
        return dataimage;
    }

    public DataClass(String dataname, String datadate, String dataimage) {
        this.dataname = dataname;
        this.datadate = datadate;
        this.dataimage = dataimage;
    }
    public DataClass(){

    }
}
