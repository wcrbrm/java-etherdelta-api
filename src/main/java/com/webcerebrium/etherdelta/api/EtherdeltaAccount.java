package com.webcerebrium.etherdelta.api;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;


@Slf4j
public class EtherdeltaAccount {

    @Getter @Setter
    List<EthereumWalletToken> tokens = null;
    boolean locked = true;

    public EtherdeltaAccount() {
    }

    public EtherdeltaAccount(List<EthereumWalletToken> tokens) {
        this.tokens = tokens;
    }

    public EtherdeltaAccount released() {
        locked = false; // without release it will be false trade
        return this;
    }

    public static EtherdeltaAccount mockOf(String coin) {
        return (new EtherdeltaAccount()).reset().add(coin, BigDecimal.valueOf(100000));
    }
    public static EtherdeltaAccount mockOf(String coin, BigDecimal balance) {
        return (new EtherdeltaAccount()).reset().add(coin, balance);
    }

    public BigDecimal getBalanceOf(String coin) {
        if (tokens == null) return BigDecimal.ZERO;
        for (int i = 0; i < tokens.size(); i ++) {
            EthereumWalletToken wallet = tokens.get(i);
            if (wallet.getToken().equals(coin)) return wallet.getBalance();
        }
        return BigDecimal.ZERO;
    }

    public EtherdeltaAccount reset() {
        if (tokens == null) tokens = new LinkedList<>();
        tokens.clear();
        return this;
    }

    public EtherdeltaAccount add(String coin, BigDecimal amount) {
        if (tokens == null) tokens = new LinkedList<>();

        for (int i = 0; i < tokens.size(); i++) {
            EthereumWalletToken wallet = tokens.get(i);
            if (wallet != null && wallet.getToken().equals(coin)) {
                // log.debug("adding {} to {} balance, {} existing", amount, coin, wallet.getBalance());
                wallet.setBalance(wallet.getBalance().add(amount));
                return this;
            }
        }
        // log.debug("adding {} to {} balance", amount, coin);
        tokens.add(new EthereumWalletToken(coin, amount));
        return this;
    }

    public EtherdeltaAccount set(String coin, BigDecimal amount) {
        if (tokens == null) tokens = new LinkedList<>();

        for (int i = 0; i < tokens.size(); i++) {
            EthereumWalletToken wallet = tokens.get(i);
            if (wallet.getToken().equals(coin)) {
                wallet.setBalance(amount);
                return this;
            }
        }
        tokens.add(new EthereumWalletToken(coin, amount));
        return this;
    }

    public String walletsAsString() {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < tokens.size(); i++) {
            EthereumWalletToken wallet = tokens.get(i);
            if (!wallet.isEmpty()) {
                sb.append(wallet.getToken()).append(":").append(wallet.getBalance()).append(" ");
            }
        }
        return sb.toString();
    }
}
