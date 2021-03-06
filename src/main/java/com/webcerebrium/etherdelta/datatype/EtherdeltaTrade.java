package com.webcerebrium.etherdelta.datatype;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonObject;
import com.webcerebrium.etherdelta.api.EtherdeltaApiException;
import com.webcerebrium.etherdelta.api.EtherdeltaConfig;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Data
public class EtherdeltaTrade {

    public EthereumToken token = null;
    public BigDecimal price = null;
    public BigDecimal amount = null;
    public String txHash = "";
    public Date date = null;
    public EtherdeltaOrderSide side = null;
    public BigDecimal amountBase;
    public String buyer;
    public String seller;
    public String tokenAddr = "";

    public EtherdeltaTrade() {
    }

    private void jsonExpect(JsonObject obj, Set<String> fields) throws EtherdeltaApiException {
        Set<String> missing = new HashSet<>();
        for (String f: fields) { if (!obj.has(f) || obj.get(f).isJsonNull()) missing.add(f); }
        if (missing.size() > 0) {
            log.warn("Missing fields {} in {}", missing.toString(), obj.toString());
            throw new EtherdeltaApiException("Missing fields " + missing.toString());
        }
    }

    private BigDecimal safeDecimal(JsonObject obj, String field) {
        if (obj.has(field) && obj.get(field).isJsonPrimitive() && obj.get(field) != null) {
            try {
                return obj.get(field).getAsBigDecimal();
            } catch (java.lang.NumberFormatException nfe) {
                log.info("Number format exception in field={} value={} trade={}", field, obj.get(field), obj.toString());
            }
        }
        return null;
    }

    public EtherdeltaTrade(EtherdeltaConfig config, JsonObject obj) throws EtherdeltaApiException {
        jsonExpect(obj, ImmutableSet.of("tokenAddr" ,"price", "amount", "side", "buyer", "seller", "date"));

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

        this.txHash = obj.get("txHash").getAsString();
        this.price = safeDecimal(obj, "price");
        this.amount = safeDecimal(obj, "amount");
        this.side = EtherdeltaOrderSide.valueOf(obj.get("side").getAsString().toUpperCase());
        this.amountBase = safeDecimal(obj, "amount");
        this.buyer = obj.get("buyer").getAsString();
        this.seller = obj.get("seller").getAsString();
        String dateString = obj.get("date").getAsString();
        this.date = IsoDate.parse(dateString);
    }

}
