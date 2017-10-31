package com.webcerebrium.etherdelta.datatype;

import lombok.extern.slf4j.Slf4j;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Class incapsulating ISO8601 date parsing from string
 */
@Slf4j
public class IsoDate {

    public static Date parse(final String iso8601string) {
        String s = iso8601string; //.replace("Z", "+00:00");
        try {
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").parse(s);
        } catch (ParseException e ) {
            log.warn("Date '{}' parse exception {}", s, e.getMessage());
        }
        return null;
    }

}
