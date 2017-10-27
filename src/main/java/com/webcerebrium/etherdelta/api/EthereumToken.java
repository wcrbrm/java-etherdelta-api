package com.webcerebrium.etherdelta.api;

import com.google.gson.JsonObject;
import lombok.Data;

@Data
public class EthereumToken {

    int decimals = 18;
    String address = "";
    String name = "";

    public EthereumToken() {
    }

    public EthereumToken(JsonObject obj) {
        if (obj.has("name") && obj.get("name").isJsonPrimitive()) {
            this.name = obj.get("name").getAsString();
        }
        if (obj.has("addr") && obj.get("addr").isJsonPrimitive()) {
            this.address = obj.get("addr").getAsString();
        }
        if (obj.has("decimals") && obj.get("decimals").isJsonPrimitive()) {
            this.decimals = (int) obj.get("decimals").getAsLong();
        }
    }

}
