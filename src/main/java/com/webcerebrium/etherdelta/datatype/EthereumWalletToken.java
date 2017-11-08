package com.webcerebrium.etherdelta.datatype;

import com.google.gson.JsonObject;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class EthereumWalletToken {
    String token;
    BigDecimal balance = BigDecimal.ZERO;

    public EthereumWalletToken(String token) {
        this.token = token;
        this.balance = BigDecimal.ZERO;
    }

    public EthereumWalletToken(String token, BigDecimal amount) {
        this.token = token;
        this.balance = amount;
    }

    public EthereumWalletToken(JsonObject obj) {
        this.token = obj.get("token").getAsString();
        this.balance = obj.get("balance").getAsBigDecimal();
    }

    public boolean isEmpty() {
        return BigDecimal.valueOf(1e-4).compareTo(this.balance) > 0;
    }

}
