package com.tihiyn.car_park_bot;

import java.util.Set;

public class Notification {
    private String start;
    private String finish;
    private String enterprise;
    private String regNum;
    private Set<String> managers;

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getFinish() {
        return finish;
    }

    public void setFinish(String finish) {
        this.finish = finish;
    }

    public String getEnterprise() {
        return enterprise;
    }

    public void setEnterprise(String enterprise) {
        this.enterprise = enterprise;
    }

    public String getRegNum() {
        return regNum;
    }

    public void setRegNum(String regNum) {
        this.regNum = regNum;
    }

    public Set<String> getManagers() {
        return managers;
    }

    public void setManagers(Set<String> managers) {
        this.managers = managers;
    }
}
