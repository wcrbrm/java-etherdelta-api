package com.webcerebrium.etherdelta.datatype;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonObject;
import com.webcerebrium.etherdelta.api.EtherdeltaApiException;
import com.webcerebrium.etherdelta.api.EtherdeltaMainConfig;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Data
@Slf4j
public class EtherdeltaReturnTicker {

    EthereumToken token;
    String symbol = "";
    String tokenAddr = "";
    BigDecimal quoteVolume = null;
    BigDecimal baseVolume = null;
    BigDecimal last = null;
    BigDecimal percentChange = null;
    BigDecimal bid = null;
    BigDecimal ask = null;

    private void jsonExpect(JsonObject obj, Set<String> fields) throws EtherdeltaApiException {
        Set<String> missing = new HashSet<>();
        for (String f: fields) { if (!obj.has(f) || obj.get(f).isJsonNull()) missing.add(f); }
        if (missing.size() > 0) {
            log.warn("Missing fields {} in {}", missing.toString(), obj.toString());
            throw new EtherdeltaApiException("Missing fields " + missing.toString());
        }
    }

    private BigDecimal safeDecimal(JsonObject obj, String field) {
        if (obj.has(field) && obj.get(field).isJsonPrimitive()) {
            return obj.get(field).getAsBigDecimal();
        }
        return null;
    }

    public EtherdeltaReturnTicker(EtherdeltaMainConfig config, String symbol, JsonObject obj) throws EtherdeltaApiException {
        this.symbol = symbol;
        jsonExpect(obj, ImmutableSet.of("tokenAddr"));

        tokenAddr = obj.get("tokenAddr").getAsString();
        if (config == null) {
            log.warn("Main config is not set, token name cannot be detected from address");
        } else {
            if (config.getTokens().containsKey(tokenAddr)) {
                this.token = config.getTokens().get(tokenAddr);
            } else {
                log.debug("Token is not found by address '{}'", tokenAddr);
                this.token = null;
            }
        }

        quoteVolume = safeDecimal(obj, "quoteVolume");
        baseVolume = safeDecimal(obj, "baseVolume");
        last = safeDecimal(obj, "last");
        percentChange = safeDecimal(obj, "percentChange");
        bid = safeDecimal(obj, "bid");
        ask = safeDecimal(obj, "ask");
    }

    public String getPlainText() {
        StringBuffer sb = new StringBuffer();
        sb.append(symbol).append("\t")
          .append(bid).append("\t")
          .append(ask).append("\t")
          .append(percentChange).append("\t")
          .append(quoteVolume).append("\t")
        ;
        return sb.toString();
    }
}
