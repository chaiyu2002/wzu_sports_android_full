package com.tim.app.server.entry;


import java.io.Serializable;

/**
 * 首页的运动对象
 */
public class SportEntry implements Serializable {
    private static final long serialVersionUID = 6187447685293862071L;
    public   static final  int AREA_SPORT = 1;
    public static  final  int RUNNING_SPORT = 2;

    private int id = 1;//运动id，默认值为1
    private String name;//名称
    private int qualifiedCostTime;//目标时间
    private int acquisitionInterval;
    private int participantNum;//参加人数
    private int qualifiedDistance;//目标距离
    private String targetSpeed;//目标速度
    private String imgUrl;//背景图片地址
    private int bgDrawableId;//背景图片id
    private int stepThreshold;//计步阈值
    private int type;//运动方式

    public int getAcquisitionInterval() {
        return acquisitionInterval;
    }

    public void setAcquisitionInterval(int acquisitionInterval) {
        this.acquisitionInterval = acquisitionInterval;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getParticipantNum() {
        return participantNum;
    }

    public void setParticipantNum(int participantNum) {
        this.participantNum = participantNum;
    }
    public int getQualifiedDistance() {
        return qualifiedDistance;
    }

    public void setQualifiedDistance(int qualifiedDistance) {
        this.qualifiedDistance = qualifiedDistance;
    }

    public int getStepThreshold() {
        return stepThreshold;
    }

    public void setStepThreshold(int stepThreshold) {
        this.stepThreshold = stepThreshold;
    }

    public String getTargetSpeed() {
        return targetSpeed;
    }

    public void setTargetSpeed(String targetSpeed) {
        this.targetSpeed = targetSpeed;
    }

    public int getTargetTime() {
        return qualifiedCostTime;
    }

    public void setTargetTime(int targetTime) {
        this.qualifiedCostTime = targetTime;
    }

    public String getImgUrl() {
        return imgUrl;
    }

    public void setImgUrl(String imgUrl) {
        this.imgUrl = imgUrl;
    }

    public int getBgDrawableId() {
        return bgDrawableId;
    }

    public void setBgDrawableId(int bgDrawableId) {
        this.bgDrawableId = bgDrawableId;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "SportEntry{" +
                "name='" + name + '\'' +
                ", participantNum=" + participantNum +
                ", qualifiedDistance=" + qualifiedDistance +
                ", targetSpeed='" + targetSpeed + '\'' +
                ", qualifiedCostTime=" + qualifiedCostTime +
                ", imgUrl='" + imgUrl + '\'' +
                ", bgDrawableId=" + bgDrawableId +
                ", type=" + type +
                ", id=" + id +
                ", acquisitionInterval=" + acquisitionInterval +
                '}';
    }
}
