package com.webcerebrium.etherdelta.datatype;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.webcerebrium.etherdelta.api.EtherdeltaApiException;
import com.webcerebrium.etherdelta.api.EtherdeltaConfig;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Unlike other exchanges, etherdelta exchange orders are not grouped,
 * therefore order contains
 */

@Slf4j
@Data
public class EtherdeltaOrderBook {

    List<EtherdeltaOrder> sells = new ArrayList<>();
    List<EtherdeltaOrder> buys = new ArrayList<>();

    public EtherdeltaOrderBook() {
    }

    public void reset() {
        sells.clear();
        buys.clear();
    }

    private void jsonExpect(JsonObject obj, Set<String> fields) throws EtherdeltaApiException {
        Set<String> missing = new HashSet<>();
        for (String f: fields) { if (!obj.has(f) || obj.get(f).isJsonNull()) missing.add(f); }
        if (missing.size() > 0) {
            log.warn("Missing fields {} in {}", missing.toString(), obj.toString());
            throw new EtherdeltaApiException("Missing fields " + missing.toString());
        }
    }

    public EtherdeltaOrderBook(EtherdeltaConfig config, JsonObject obj) throws EtherdeltaApiException {
        jsonExpect(obj, ImmutableSet.of("buys", "sells"));

        JsonArray buys = obj.get("buys").getAsJsonArray();
        JsonArray sells = obj.get("sells").getAsJsonArray();
        log.debug("on ORDERS, {} buys, {} sells", buys.size(), sells.size());
        for (JsonElement elem: buys) {
            EtherdeltaOrder buyOrder = new EtherdeltaOrder(config, EtherdeltaOrderSide.BUY, elem.getAsJsonObject());
            log.debug("BUY ORDER {}", buyOrder.toString());
            this.buys.add(buyOrder);
        }
        for (JsonElement elem: sells) {
            EtherdeltaOrder sellOrder = new EtherdeltaOrder(config, EtherdeltaOrderSide.SELL, elem.getAsJsonObject());
            log.debug("SELL ORDER {}", sellOrder.toString());
            this.sells.add(sellOrder);
        }
    }

    public List<EtherdeltaOrder> getAllOrders() {
        List<EtherdeltaOrder> newList = new LinkedList<>();
        newList.addAll(sells);
        newList.addAll(buys);
        return newList;
    }

    private List<EtherdeltaOrder> sublist(List<EtherdeltaOrder> list, int nLimit) {
        int si = 0;
        List<EtherdeltaOrder> result = new LinkedList<>();
        for (EtherdeltaOrder s: list) {
            result.add(s);
            si ++;
            if (si >= nLimit) break;
        }
        return result;
    }

    public String getPlainText(int nLimit) {
        StringBuffer sb = new StringBuffer();
        sb.append("\r\n");
        for (EtherdeltaOrder s: Lists.reverse(sublist(sells, nLimit))) {
            sb.append(s.getPlainText()).append("\r\n");
        }
        for (EtherdeltaOrder b: sublist(buys, nLimit)) {
            sb.append(b.getPlainText()).append("\r\n");
        }
        return sb.toString();
    }

    public BigDecimal getBestSellPrice() {
        if (sells.size() == 0) return null;
        return sells.get(0).getPrice();
    }

    public BigDecimal getBestSellQty() {
        if (sells.size() == 0) return BigDecimal.ZERO;
        return sells.get(0).getAmount();
    }

    public BigDecimal getBestBuyPrice() {
        if (buys.size() == 0) return null;
        return buys.get(0).getPrice();
    }
    public BigDecimal getBestBuyQty() {
        if (buys.size() == 0) return BigDecimal.ZERO;
        return buys.get(0).getAmount();
    }




}
