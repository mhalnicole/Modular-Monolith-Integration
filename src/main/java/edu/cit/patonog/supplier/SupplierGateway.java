package edu.cit.patonog.supplier;

public interface SupplierGateway {
    SupplierReorderResult reorder(String productId, int unitsNeeded);
}