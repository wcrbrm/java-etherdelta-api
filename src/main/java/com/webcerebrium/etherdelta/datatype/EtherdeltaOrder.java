package com.webcerebrium.etherdelta.datatype;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonObject;
import com.webcerebrium.etherdelta.api.EtherdeltaApiException;
import com.webcerebrium.etherdelta.api.EtherdeltaConfig;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Slf4j
@Data
public class EtherdeltaOrder {

    public EtherdeltaOrderSide side = null;

    public EthereumToken get;
    public BigDecimal amountGet;
    public EthereumToken give;
    public BigDecimal amountGive;

    public String id;
    public BigDecimal amount;
    public BigDecimal price;

    public String tokenGet;
    public String tokenGive;

    public BigInteger expires;
    public BigInteger nonce;
    public BigInteger v;

    public String r; // some tx
    public String s; // some tx
    public String user; // some wallet

    public Date updated;

    public BigDecimal availableVolume = null;
    public BigDecimal ethAvailableVolume = null;
    public BigDecimal availableVolumeBase = null;
    public BigDecimal ethAvailableVolumeBase = null;
    public BigDecimal amountFilled = null;

    public EtherdeltaOrder() {
        Locale.setDefault(new Locale("en", "US"));
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
        if (obj.has(field) && obj.get(field).isJsonPrimitive()) {
            try {
                return obj.get(field).getAsBigDecimal();
            } catch (NumberFormatException nfe) {
                log.info("Number format exception in field={} value={} trade={}", field, obj.get(field), obj.toString());
            }
        }
        return null;
    }

    public EtherdeltaOrder(EtherdeltaConfig config, EtherdeltaOrderSide side, JsonObject obj) throws EtherdeltaApiException {
        Locale.setDefault(new Locale("en", "US"));

        jsonExpect(obj, ImmutableSet.of("id", "amount",
                "price", "amountGet", "amountGive", "tokenGet", "tokenGive", "updated"));

        this.side = side;
        id = obj.get("id").getAsString();

        nonce = obj.get("nonce").getAsBigInteger();
        expires = obj.get("expires").getAsBigInteger();
        v = obj.get("v").getAsBigInteger();

        r = obj.get("r").getAsString();
        s = obj.get("s").getAsString();
        user = obj.get("user").getAsString();

        availableVolume = safeDecimal(obj, "availableVolume");
        ethAvailableVolume = safeDecimal(obj, "ethAvailableVolume");
        availableVolumeBase = safeDecimal(obj, "availableVolumeBase");
        ethAvailableVolumeBase = safeDecimal(obj, "ethAvailableVolumeBase");
        amountFilled = safeDecimal(obj, "amountFilled");

        amount = obj.get("amount").getAsBigDecimal().abs();
        price = obj.get("price").getAsBigDecimal();
        amountGet = obj.get("amountGet").getAsBigDecimal().abs();
        amountGive = obj.get("amountGive").getAsBigDecimal().abs();

        tokenGet = obj.get("tokenGet").getAsString();
        tokenGive = obj.get("tokenGive").getAsString();
        if (config == null) {
            log.warn("main config is not set");
        } else {
            if (config.getTokens().containsKey(tokenGet)) {
                this.get = config.getTokens().get(tokenGet);
            } else {
                log.debug("tokenGet is not found by address '{}'", tokenGet);
                this.get = null;
            }
            if (config.getTokens().containsKey(tokenGive)) {
                this.give = config.getTokens().get(tokenGive);
            } else {
                log.debug("tokenGive is not found by address '{}'", tokenGive);
                this.give = null;
            }
        }

        String dateString = obj.get("updated").getAsString();
        this.updated = IsoDate.parse(dateString);
    }

    /**
     * @return number of decimals for current market
     */
    public int getDecimals() {
        if (side == EtherdeltaOrderSide.BUY) {
            return get.getDecimals();
        } else if (side == EtherdeltaOrderSide.SELL) {
            return give.getDecimals();
        }
        return 18;
    }

    /**
     * @return quantity to be purchased/sold under this order book, taking decimals into consideration
     */
    public BigDecimal getAbsAmount() {
        return amount.multiply(BigDecimal.valueOf(Math.pow(10, - getDecimals())));
    }


    public String getPlainText() {
        StringBuffer sb = new StringBuffer();
        sb.append(String.format("%6s ", side)).append(" ");
        sb.append(formatPrice(price)).append(" ");
        sb.append(formattedAmount()).append(" ").append(user);
        return sb.toString();
    }

    /**
     * @return nice looking quantity to be purchased
     */
    private String formattedAmount() {
        String fmt = "###,###.######";
        DecimalFormat formatter = new DecimalFormat(fmt);
        return String.format("%12s", formatter.format(getAbsAmount()));
    }

    private String formatPrice(BigDecimal price) {
        return String.format("%20.12f", price);
    }

    public BigDecimal getEthVolume() {
        return this.getAbsAmount().multiply(this.getPrice());
    }
}
