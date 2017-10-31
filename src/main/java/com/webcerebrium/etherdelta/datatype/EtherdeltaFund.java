package com.webcerebrium.etherdelta.datatype;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonObject;
import com.webcerebrium.etherdelta.api.EtherdeltaApiException;
import com.webcerebrium.etherdelta.api.EtherdeltaMainConfig;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Data
@Slf4j
public class EtherdeltaFund {

    public EthereumToken token = null;
    public String txHash = "";
    public Date date = null;
    public EtherdeltaFundKind kind;
    public String user = "";
    public BigDecimal amount = null;
    public BigDecimal balance = null;
    public String tokenAddr = "";

    public EtherdeltaFund() {
    }

    private void jsonExpect(JsonObject obj, Set<String> fields) throws EtherdeltaApiException {
        Set<String> missing = new HashSet<>();
        for (String f: fields) { if (!obj.has(f) || obj.get(f).isJsonNull()) missing.add(f); }
        if (missing.size() > 0) {
            log.warn("Missing fields {} in {}", missing.toString(), obj.toString());
            throw new EtherdeltaApiException("Missing fields " + missing.toString());
        }
    }

    public EtherdeltaFund(EtherdeltaMainConfig config, JsonObject obj) throws EtherdeltaApiException {
        jsonExpect(obj, ImmutableSet.of("tokenAddr", "txHash", "user", "balance", "amount", "kind", "date"));

        tokenAddr = obj.get("tokenAddr").getAsString();
        if (config == null) {
            log.warn("main config is not set");
        } else {
            if (config.getTokens().containsKey(tokenAddr)) {
                this.token = config.getTokens().get(tokenAddr);
            } else {
                log.debug("tokenAddr is not found by address '{}'", tokenAddr);
                this.token = null;
            }
        }

        this.txHash = obj.get("txHash").getAsString();
        this.user = obj.get("user").getAsString();
        this.balance = obj.get("balance").getAsBigDecimal();
        this.amount = obj.get("amount").getAsBigDecimal();
        this.kind = EtherdeltaFundKind.valueOf(obj.get("kind").getAsString().toUpperCase());

        String dateString = obj.get("date").getAsString();
        this.date = IsoDate.parse(dateString);

    }
}
