package com.webcerebrium.etherdelta.datatype;

import com.google.gson.JsonObject;
import lombok.Data;

@Data
public class EthereumToken {

    String name = "";
    int decimals = 18;
    String address = "";

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

    public String toString() {
        return this.getName();
    }

}
