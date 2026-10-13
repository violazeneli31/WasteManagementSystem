package com.greenbin.wms.finance;

/** FR_29: the ways a citizen can pay. */
public enum PaymentMethod {
    CREDIT_CARD("Credit card"), BANK_TRANSFER("Bank transfer"), DIGITAL_WALLET("Digital wallet"), CASH("Cash");

    private final String label;
    PaymentMethod(String label) { this.label = label; }
    @Override public String toString() { return label; }
}
