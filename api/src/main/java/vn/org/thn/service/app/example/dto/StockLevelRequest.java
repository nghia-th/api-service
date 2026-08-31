package vn.org.thn.service.app.example.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class StockLevelRequest {

    private String warehouseCode;
    private String sku;
    private Integer quantity;
    private BigDecimal unitPrice;
}
