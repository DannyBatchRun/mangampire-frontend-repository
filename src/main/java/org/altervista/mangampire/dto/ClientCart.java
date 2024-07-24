package org.altervista.mangampire.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class ClientCart {

    private String cartFormatted;
    private double totalPrice;

    public ClientCart() {

    }

    public ClientCart(String cartFormatted, double totalPrice) {
        this.cartFormatted = cartFormatted;
        this.totalPrice = totalPrice;
    }
    public void reset() {
        this.cartFormatted = null;
        this.totalPrice = 0;
    }
}