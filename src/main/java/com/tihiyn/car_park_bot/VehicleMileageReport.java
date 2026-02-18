package com.tihiyn.car_park_bot;

import java.time.ZonedDateTime;
import java.util.Map;

public class VehicleMileageReport {
    private Long vehicleId;
    private String name;
    private String period;
    private ZonedDateTime begin;
    private ZonedDateTime end;
    private Map<String, Long> result;

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public ZonedDateTime getBegin() {
        return begin;
    }

    public void setBegin(ZonedDateTime begin) {
        this.begin = begin;
    }

    public ZonedDateTime getEnd() {
        return end;
    }

    public void setEnd(ZonedDateTime end) {
        this.end = end;
    }

    public Map<String, Long> getResult() {
        return result;
    }

    public void setResult(Map<String, Long> result) {
        this.result = result;
    }
}
