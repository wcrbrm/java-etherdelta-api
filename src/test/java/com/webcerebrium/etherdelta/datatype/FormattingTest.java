package com.webcerebrium.etherdelta.datatype;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Locale;

@Slf4j
public class FormattingTest {

    @Test
    public void testFormat() throws Exception {

        Locale.setDefault(new Locale("en", "US"));
        BigDecimal num = BigDecimal.valueOf(0.002);
        DecimalFormat formatter = new DecimalFormat("###.######");
        String f = String.format("%30.8f", num);

        log.warn("Result {}", f);
    }
}
