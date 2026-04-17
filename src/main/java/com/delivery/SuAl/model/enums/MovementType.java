package com.delivery.SuAl.model.enums;

public enum MovementType {
    PURCHASE,               //→ stock came in from a supplier invoice
    SALE,                   //→ stock went out for a customer order
    TRANSFER_IN,            //→ stock arrived from another warehouse
    TRANSFER_OUT,           //→ stock left to another warehouse
    RETURN_TO_SUPPLIER,     //→ defective/excess stock sent back to supplier
    RETURN_FROM_CUSTOMER,   //→ customer returned product, stock back in
}
