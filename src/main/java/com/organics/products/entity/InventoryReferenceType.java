package com.organics.products.entity;

public enum InventoryReferenceType {

    ORDER,       // Change happened because of an order

    CART,        // Change happened because of cart action

    ADMIN,       // Manual stock change by admin or store manager

    RETURN,      // Stock returned after order cancellation / refund

    ADJUSTMENT   // Manual correction (damage, loss, audit fix)
}
