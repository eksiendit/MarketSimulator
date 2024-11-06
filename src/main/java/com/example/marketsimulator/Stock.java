package com.example.marketsimulator;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;

public class Stock {
    private final SimpleStringProperty name;
    private final SimpleDoubleProperty price;

    public Stock(String name, double price) {
        this.name = new SimpleStringProperty(name);
        this.price = new SimpleDoubleProperty(price);
    }

    public Stock() {
        this.name = new SimpleStringProperty("");
        this.price = new SimpleDoubleProperty(0);
    }


    public String getName() {
        return name.get();
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public SimpleStringProperty nameProperty() {
        return name;
    }

    public double getPrice() {
        return price.get();
    }

    public void setPrice(double price) {
        this.price.set(price);
    }

    public SimpleDoubleProperty priceProperty() {
        return price;
    }
}

