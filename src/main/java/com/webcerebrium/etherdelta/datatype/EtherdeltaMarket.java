package com.webcerebrium.etherdelta.datatype;

import com.google.gson.JsonObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedList;
import java.util.List;

@Data
@Slf4j
public class EtherdeltaMarket {

    EtherdeltaOrderBook orderbook = new EtherdeltaOrderBook();
    List<EtherdeltaTrade> trades = new LinkedList<>();
    List<EtherdeltaFund> funds = new LinkedList<>();

    public EtherdeltaMarket() {
    }

    public EtherdeltaMarket(JsonObject obj) {
    }

}
