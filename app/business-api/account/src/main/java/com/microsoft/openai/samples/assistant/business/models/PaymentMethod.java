package com.microsoft.openai.samples.assistant.business.models;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PaymentMethod(
    String id,
    String type,
    String activationDate,
    String expirationDate,
    String availableBalance,
    // card number is valued only for credit card type - never exposed directly
    @JsonIgnore
    String cardNumber
) {
    @JsonProperty("maskedCardNumber")
    public String maskedCardNumber() {
        if (cardNumber == null) {
            return null;
        }
        String digits = cardNumber.replaceAll("\\D", "");
        if (digits.length() < 4) {
            return "****";
        }
        return "************" + digits.substring(digits.length() - 4);
    }
}

