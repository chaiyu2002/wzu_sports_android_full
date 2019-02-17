package com.tim.app.server.entry;

import java.io.Serializable;

/**
 * @创建者 倪军
 * @创建时间 2017/8/2
 * @描述
 */

public class HistorySportEntry implements Serializable {

    private static final long serialVersionUID = 6187447685293862071L;

    private long id;
    private int studentId;
    private int sportId;
    private int costTime;
    private long targetFinishedTime;
    private long startTime;
    private int kcalConsumed;
    private boolean qualified;
    private boolean isValid;
    private boolean isVerified;
    private int qualifiedCostTime;
    private long minCostTime;
    private long createdAt;
    private long updatedAt;
    private long endedAt;
    private boolean endedBy;
    private String sportDate;
    private String sportName;
    private String areaName;
    private int type;//类型：1，跑步运动；2，区域运动。

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getStudentId() {
        return studentId;
    }

    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    public int getSportId() {
        return sportId;
    }

    public void setSportId(int sportId) {
        this.sportId = sportId;
    }

    public int getCostTime() {
        return costTime;
    }

    public void setCostTime(int costTime) {
        this.costTime = costTime;
    }

    public long getTargetFinishedTime() {
        return targetFinishedTime;
    }

    public void setTargetFinishedTime(long targetFinishedTime) {
        this.targetFinishedTime = targetFinishedTime;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public int getKcalConsumed() {
        return kcalConsumed;
    }

    public void setKcalConsumed(int kcalConsumed) {
        this.kcalConsumed = kcalConsumed;
    }

    public boolean isQualified() {
        return qualified;
    }

    public void setQualified(boolean qualified) {
        this.qualified = qualified;
    }

    public boolean isValid() {
        return isValid;
    }

    public void setValid(boolean valid) {
        isValid = valid;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public void setVerified(boolean verified) {
        isVerified = verified;
    }

    public int getQualifiedCostTime() {
        return qualifiedCostTime;
    }

    public void setQualifiedCostTime(int qualifiedCostTime) {
        this.qualifiedCostTime = qualifiedCostTime;
    }

    public long getMinCostTime() {
        return minCostTime;
    }

    public void setMinCostTime(long minCostTime) {
        this.minCostTime = minCostTime;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public long getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(long endedAt) {
        this.endedAt = endedAt;
    }

    public boolean isEndedBy() {
        return endedBy;
    }

    public void setEndedBy(boolean endedBy) {
        this.endedBy = endedBy;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getSportDate() {
        return sportDate;
    }

    public void setSportDate(String sportDate) {
        this.sportDate = sportDate;
    }

    public String getSportName() {
        return sportName;
    }

    public void setSportName(String sportName) {
        this.sportName = sportName;
    }

    public String getAreaSport() {
        return areaName;
    }

    public void setAreaSport(String areaSport) {
        this.areaName = areaSport;
    }

    @Override
    public String toString() {
        return "HistorySportEntry{" +
                "id=" + id +
                ", studentId=" + studentId +
                ", costTime=" + costTime +
                ", targetFinishedTime=" + targetFinishedTime +
                ", startTime=" + startTime +
                ", kcalConsumed=" + kcalConsumed +
                ", qualified=" + qualified +
                ", isValid=" + isValid +
                ", qualifiedCostTime=" + qualifiedCostTime +
                ", minCostTime=" + minCostTime +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", endedAt=" + endedAt +
                ", endedBy=" + endedBy +
                ", type=" + type +
                '}';
    }
}
