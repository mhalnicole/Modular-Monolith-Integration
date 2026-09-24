package edu.cit.patonog.supplier;

import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
class SupplierSkuTranslator {

    record SkuMapping(String supplierSku, int packSize) {}

    private final Map<String, SkuMapping> mappings = new ConcurrentHashMap<>();

    SupplierSkuTranslator() {
        mappings.put("P100", new SkuMapping("ZAX-1614", 20));
        mappings.put("P200", new SkuMapping("ZAX-1252", 10));
        mappings.put("P300", new SkuMapping("ZAX-4488", 10));
    }

    void registerMapping(String internalProductId, String supplierSku, int packSize) {
        if (internalProductId != null && supplierSku != null && packSize > 0) {
            mappings.put(internalProductId.trim(), new SkuMapping(supplierSku.trim(), packSize));
        }
    }

    SkuMapping getMapping(String internalProductId) {
        if (internalProductId == null) return null;
        return mappings.get(internalProductId.trim());
    }

    int calculateCases(String internalProductId, int unitsNeeded) {
        SkuMapping mapping = getMapping(internalProductId);
        int packSize = (mapping != null && mapping.packSize() > 0) ? mapping.packSize() : 1;
        return (int) Math.ceil((double) unitsNeeded / packSize);
    }

    int calculateReplenishedUnits(String internalProductId, int cases) {
        SkuMapping mapping = getMapping(internalProductId);
        int packSize = (mapping != null && mapping.packSize() > 0) ? mapping.packSize() : 1;
        return cases * packSize;
    }
}
