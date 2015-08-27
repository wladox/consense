package com.postnikoff.consense.geo;

import java.util.Date;

/**
 * Created by CodeX on 27.08.2015.
 */
public class MyGeofence {

    private Integer geofenceId;
    private String 	name;

    private double 	latitude;
    private double 	longitude;
    private Integer radius;

    private long 	duration;
    private Date created;

    public MyGeofence() {}

    public Integer getGeofenceId() {
        return geofenceId;
    }
    public void setGeofenceId(Integer geofenceId) {
        this.geofenceId = geofenceId;
    }
    public double getLatitude() {
        return latitude;
    }
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }
    public double getLongitude() {
        return longitude;
    }
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
    public Integer getRadius() {
        return radius;
    }
    public void setRadius(Integer radius) {
        this.radius = radius;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public long getDuration() {
        return duration;
    }
    public void setDuration(long duration) {
        this.duration = duration;
    }
    public Date getCreated() {
        return created;
    }
    public void setCreated(Date created) {
        this.created = created;
    }
}
