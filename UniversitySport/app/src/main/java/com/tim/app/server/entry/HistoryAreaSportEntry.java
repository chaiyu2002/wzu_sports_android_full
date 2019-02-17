package com.tim.app.server.entry;

/**
 * @创建者 倪军
 * @创建时间 2017/8/1
 * @描述
 */

public class HistoryAreaSportEntry extends HistorySportEntry {

    private static final long serialVersionUID = 6187447685293862071L;
    private String areaSport;
    private FixLocationOutdoorSportPoint locationPoint;

    public FixLocationOutdoorSportPoint getLocationPoint() {
        return locationPoint;
    }

    public void setLocationPoint(FixLocationOutdoorSportPoint locationPoint) {
        this.locationPoint = locationPoint;
    }

    public String getAreaSport() {
        return areaSport;
    }

    public void setAreaSport(String areaSport) {
        this.areaSport = areaSport;
    }

}
