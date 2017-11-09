package com.webcerebrium.etherdelta.datatype;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.webcerebrium.etherdelta.api.EtherdeltaApiException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@Slf4j
public class EtherdeltaEtherscanTxlist {
    List<EtherdeltaEtherscanTx> txList = new LinkedList<>();

    public EtherdeltaEtherscanTxlist() {
    }

    public EtherdeltaEtherscanTxlist(JsonObject response) throws EtherdeltaApiException {
        if (!response.has("result")) {
            throw new EtherdeltaApiException("No result in response");
        }
        JsonArray result = response.get("result").getAsJsonArray();
        for (JsonElement item : result) {
            txList.add(new EtherdeltaEtherscanTx(item.getAsJsonObject()));
        }
    }

    public Set<Long> getBlocks() {
        Set<Long> blocks = new HashSet<>();
        for (EtherdeltaEtherscanTx tx: txList) {
            blocks.add(tx.getBlockNumber());
        }
        return blocks;
    }

    public Map<String, Long> getMethodsStat() {
        Map<String, Long> stat = new HashMap<>();
        for (EtherdeltaEtherscanTx tx: txList) {
            String method = tx.getMethodName();
            if (!stat.containsKey(method)) stat.put(method, 0L);
            stat.put(method, stat.get(method).longValue() + 1);
        }
        return stat;
    }
}
