package com.organics.products.entity;

public enum InventoryTransactionType {

    IN,        // Stock added to inventory (supplier / admin action)

    SOLD,      // Stock permanently reduced because an order was placed

    RESERVE,   // Stock temporarily blocked when user adds item to cart

    RELEASE    // Reserved stock released back (cart expired / item removed)
}

