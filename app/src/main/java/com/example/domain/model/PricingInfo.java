package com.example.domain.model;

import java.io.Serializable;

public class PricingInfo implements Serializable {
    public double promptPricePerMillion;
    public double completionPricePerMillion;
    public String currency = "USD";

    public PricingInfo() {}

    public PricingInfo(double promptPricePerMillion, double completionPricePerMillion, String currency) {
        this.promptPricePerMillion = promptPricePerMillion;
        this.completionPricePerMillion = completionPricePerMillion;
        this.currency = currency;
    }
}
