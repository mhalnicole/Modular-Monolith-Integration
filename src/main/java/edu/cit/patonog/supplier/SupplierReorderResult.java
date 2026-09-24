package edu.cit.patonog.supplier;

public record SupplierReorderResult(
    boolean success,
    String buyerRef,
    String poNumber,
    int cases,
    int units,
    SupplierOrderStatus status,
    String message
) {}
