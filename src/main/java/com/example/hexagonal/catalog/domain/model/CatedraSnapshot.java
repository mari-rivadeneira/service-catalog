package com.example.hexagonal.catalog.domain.model;

import java.util.List;

public class CatedraSnapshot {

    private long version;
    private List<ProfessionalCategory> categories;
    private List<Professional> professionals;
    private List<WeeklySchedule> schedules;

    public CatedraSnapshot() {
    }

    public CatedraSnapshot(long version,
                           List<ProfessionalCategory> categories,
                           List<Professional> professionals,
                           List<WeeklySchedule> schedules) {
        this.version = version;
        this.categories = categories;
        this.professionals = professionals;
        this.schedules = schedules;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public List<ProfessionalCategory> getCategories() {
        return categories;
    }

    public void setCategories(List<ProfessionalCategory> categories) {
        this.categories = categories;
    }

    public List<Professional> getProfessionals() {
        return professionals;
    }

    public void setProfessionals(List<Professional> professionals) {
        this.professionals = professionals;
    }

    public List<WeeklySchedule> getSchedules() {
        return schedules;
    }

    public void setSchedules(List<WeeklySchedule> schedules) {
        this.schedules = schedules;
    }
}