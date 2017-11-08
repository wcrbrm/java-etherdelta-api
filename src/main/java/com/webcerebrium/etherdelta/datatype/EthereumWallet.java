package com.webcerebrium.etherdelta.datatype;

import com.google.common.base.Strings;
import com.webcerebrium.etherdelta.api.EtherdeltaApiException;
import lombok.Data;
import org.web3j.crypto.Credentials;

@Data
public class EthereumWallet {
    public String address;
    public String privateKey;

    public EthereumWallet(String privateKey, String address) {
        this.address = address;
        this.privateKey = privateKey;
    }

    public EthereumWallet validate() throws EtherdeltaApiException {
        if (Strings.isNullOrEmpty(address)) {
            throw new EtherdeltaApiException("ETHERDELTA_WALLET_ADDRESS, ETHERDELTA_WALLET_PRIVATE_KEY is not set");
        }
        if (!this.address.startsWith("0x")) {
            throw new EtherdeltaApiException("Wallet address should start with 0x " + this.address);
        }
        // length check
        if (this.address.length() != 42) {
            throw new EtherdeltaApiException("Wallet address should contain 42 characters");
        }
        if (!Strings.isNullOrEmpty(privateKey) && this.privateKey.length() != 64) {
            throw new EtherdeltaApiException("Wallet private key should contain 64 characters");
        }
        return this;
    }

    public Credentials getWeb3Credentials() {
        return Credentials.create(this.privateKey, this.address);
    }
}
