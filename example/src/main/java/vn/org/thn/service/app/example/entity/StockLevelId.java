package vn.org.thn.service.app.example.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * Composite id for {@link StockLevel}: (warehouseCode, sku). Field names must match
 * {@link StockLevel}'s exactly - {@code BaseRepositoryImpl#withCompositeId} reads them by field
 * name via reflection to build the WHERE clause (same mechanism as
 * {@code vn.org.thn.service.base.i18n.TranslateId}).
 * <p>
 * {@code @Data} does not generate a constructor here since two explicit constructors already
 * exist on this class.
 */
@Data
public class StockLevelId implements Serializable {

    private String warehouseCode;
    private String sku;

    public StockLevelId() {
    }

    public StockLevelId(String warehouseCode, String sku) {
        this.warehouseCode = warehouseCode;
        this.sku = sku;
    }
}
