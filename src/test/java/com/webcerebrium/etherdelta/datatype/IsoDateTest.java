package com.webcerebrium.etherdelta.datatype;

import org.junit.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import static org.junit.Assert.assertNotNull;

public class IsoDateTest {

    @Test
    public void testParsing() throws Exception {
        String s = "2017-10-28T18:35:38.70Z";
        Date date = IsoDate.parse(s);

        assertNotNull("Parsed Date should not be null", date);
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTime(date);
    }
}
