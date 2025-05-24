package com.hospital.util;

import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

import java.sql.Time;

@ControllerAdvice
public class WebBindingInitializer {

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(Time.class, new TimeEditor());
    }
}