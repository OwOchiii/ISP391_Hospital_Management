package com.hospital.util;

import java.beans.PropertyEditorSupport;
import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class TimeEditor extends PropertyEditorSupport {
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        try {
            long ms = timeFormat.parse(text).getTime();
            setValue(new Time(ms));
        } catch (ParseException e) {
            throw new IllegalArgumentException("Invalid time format: " + text, e);
        }
    }

    @Override
    public String getAsText() {
        Time value = (Time) getValue();
        return value != null ? timeFormat.format(value) : "";
    }
}