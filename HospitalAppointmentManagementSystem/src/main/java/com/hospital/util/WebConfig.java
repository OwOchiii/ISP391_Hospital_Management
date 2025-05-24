package com.hospital.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@ControllerAdvice
public class WebConfig {

    @Autowired
    private ConversionService conversionService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(Date.class, new CustomDateEditor(new SimpleDateFormat("yyyy-MM-dd"), true));
    }

    // Alternatively, use the converter
    /*
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.addCustomFormatter(new DateFormatter());
    }
    */

    // Custom Editor (alternative to Converter)
    private static class CustomDateEditor extends java.beans.PropertyEditorSupport {
        private final SimpleDateFormat dateFormat;
        private final boolean allowEmpty;

        public CustomDateEditor(SimpleDateFormat dateFormat, boolean allowEmpty) {
            this.dateFormat = dateFormat;
            this.allowEmpty = allowEmpty;
        }

        @Override
        public void setAsText(String text) throws IllegalArgumentException {
            if (this.allowEmpty && (text == null || text.isEmpty())) {
                setValue(null);
                return;
            }
            try {
                setValue(dateFormat.parse(text));
            } catch (ParseException e) {
                throw new IllegalArgumentException("Invalid date format: " + text + ". Expected format: yyyy-MM-dd", e);
            }
        }

        @Override
        public String getAsText() {
            Date value = (Date) getValue();
            return value != null ? dateFormat.format(value) : "";
        }
    }
}