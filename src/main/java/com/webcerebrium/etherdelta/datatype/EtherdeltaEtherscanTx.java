package com.webcerebrium.etherdelta.datatype;

import com.google.gson.JsonObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;

@Data
@Slf4j
public class EtherdeltaEtherscanTx {
    Long blockNumber;
    Long timeStamp;
    String hash;
    Long nonce;
    String blockHash;
    Long transactionIndex;
    String from;
    String to;
    BigDecimal value;
    BigInteger gas;
    BigInteger gasPrice;
    String input;
    String contractAddress;
    BigInteger cumulativeGasUsed;
    BigInteger gasUsed;
    Long confirmations;

    public EtherdeltaEtherscanTx() {
    }

    public EtherdeltaEtherscanTx(JsonObject obj) {
        blockNumber  = obj.get("blockNumber").getAsLong();
        timeStamp  = obj.get("timeStamp").getAsLong();
        hash = obj.get("hash").getAsString();
        nonce = obj.get("nonce").getAsLong();
        blockHash  = obj.get("blockHash").getAsString();
        transactionIndex  = obj.get("transactionIndex").getAsLong();
        from = obj.get("from").getAsString();
        to = obj.get("to").getAsString();
        value = obj.get("value").getAsBigDecimal();
        gas = obj.get("gas").getAsBigInteger();
        gasPrice = obj.get("gasPrice").getAsBigInteger();
        input = obj.get("input").getAsString();
        contractAddress = obj.get("contractAddress").getAsString();
        cumulativeGasUsed = obj.get("cumulativeGasUsed").getAsBigInteger();
        gasUsed = obj.get("gasUsed").getAsBigInteger();
        confirmations  = obj.get("confirmations").getAsLong();
    }

    public String getMethod() {
        if (input == null || input.length() < 10) return "";
        return input.substring(0, 10);
    }

    public List<String> getParameters() {
        List<String> list = new LinkedList<>();
        if (input == null || input.length() > 10) {
            int index = 10;
            while (input.length() > index) {
                String next32 = input.substring(index, index + 32);
                list.add(next32);
                index += 32;
            }
        }
        return list;
    }

    public String getMethodName() {
        String m = getMethod();
        if (m.equals("0xd0e30db0")) return "deposit";
        if (m.equals("0x2e1a7d4d")) return "withdraw";
        if (m.equals("0x338b5dea")) return "depositToken";
        if (m.equals("0x9e281a98")) return "withdrawToken";
        if (m.equals("0x0a19b14a")) return "trade";
        if (m.equals("0x278b8c0e")) return "cancelOrder";
        if (m.equals("0x0b927666")) return "order";
        log.warn("Unknown EtherDelta contract method {} TxHash={}", m, hash);
        return "";
    }
}
