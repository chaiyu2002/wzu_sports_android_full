package com.tim.app.server.entry;


import java.io.Serializable;

/**
 * 运动场馆信息
 */
public class SportInfo implements Serializable {

    private static final long serialVersionUID = 6187447685293862071L;
    private String feild;//运动场馆
    private String desc;//描述
    private int targetTime;//达标时间
    private int sportCount;//运动人数

    public String getFeild() {
        return feild;
    }

    public void setFeild(String feild) {
        this.feild = feild;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public int getTargetTime() {
        return targetTime;
    }

    public void setTargetTime(int targetTime) {
        this.targetTime = targetTime;
    }


    public int getSportCount() {
        return sportCount;
    }

    public void setSportCount(int sportCount) {
        this.sportCount = sportCount;
    }
}
