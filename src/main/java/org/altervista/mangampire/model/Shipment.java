package org.altervista.mangampire.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Data
public class Shipment {
    private String name;
    private String surname;
    private String address;
    private String city;
    private int postalCode;
    public Shipment() {

    }
    public Shipment(String name, String surname, String address, String city, int postalCode) {
        this.name = name;
        this.surname = surname;
        this.address = address;
        this.city = city;
        this.postalCode = postalCode;
    }

}
