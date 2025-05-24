package com.hospital.util;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Component
public class StringToDateConverter implements Converter<String, Date> {

    @Override
    public Date convert(String source) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        dateFormat.setLenient(false); // Strict parsing
        try {
            return dateFormat.parse(source);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Invalid date format: " + source + ". Expected format: yyyy-MM-dd", e);
        }
    }
}