package com.example.cw1.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.DayOfWeek;
import java.time.LocalTime;

public class DroneWeeklyAvailability {

    @JsonProperty("day")
    private DayOfWeek day;

    @JsonProperty("from")
    private String from;  // "HH:mm"

    @JsonProperty("to")
    private String to;    // "HH:mm"

    public DayOfWeek getDay() {
        return day;
    }

    public void setDay(DayOfWeek day) {
        this.day = day;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }


    public LocalTime getFromAsLocalTime() {
        return LocalTime.parse(from);
    }

    public LocalTime getToAsLocalTime() {
        return LocalTime.parse(to);
    }


    public boolean matches(java.time.LocalDate date, java.time.LocalTime time) {
        if (day != date.getDayOfWeek()) return false;
        return !time.isBefore(getFromAsLocalTime()) &&
                time.isBefore(getToAsLocalTime());
    }
}
