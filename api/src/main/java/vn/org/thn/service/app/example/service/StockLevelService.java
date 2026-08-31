package vn.org.thn.service.app.example.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.org.thn.service.app.example.dto.StockLevelRequest;
import vn.org.thn.service.app.example.entity.StockLevel;
import vn.org.thn.service.app.example.repository.StockLevelRepository;
import vn.org.thn.service.base.IBase;
import vn.org.thn.service.base.db.mybatis.query.FieldValue;
import vn.org.thn.service.base.exception.BusinessException;
import vn.org.thn.service.base.exception.CommonErrorCode;

import java.util.List;

/**
 * Service for {@link StockLevel} - DB test entity #2 (composite key). Every method here goes
 * through the {@code *ByIds} family ({@code findByIds}/{@code existsByIds}/{@code deleteByIds}) -
 * the same 3 methods whose CGLIB proxy warning was just fixed by dropping {@code final} in
 * {@code BaseRepositoryImpl}, so a working call here is also a live check of that fix.
 */
@Service
public class StockLevelService extends IBase {

    @Autowired
    private StockLevelRepository stockLevelRepository;

    /** Upsert: {@code save()} decides INSERT vs UPDATE by checking whether this (warehouseCode, sku) pair already exists. */
    public StockLevel save(StockLevelRequest request) {
        StockLevel entity = new StockLevel();
        entity.setWarehouseCode(request.getWarehouseCode());
        entity.setSku(request.getSku());
        entity.setQuantity(request.getQuantity() == null ? 0 : request.getQuantity());
        entity.setUnitPrice(request.getUnitPrice());
        return stockLevelRepository.save(entity);
    }

    public StockLevel get(String warehouseCode, String sku) {
        StockLevel entity = stockLevelRepository.findByIds(
                FieldValue.of(StockLevel::getWarehouseCode, warehouseCode),
                FieldValue.of(StockLevel::getSku, sku));
        if (entity == null) {
            throw new BusinessException(CommonErrorCode.NOT_FOUND,
                    "StockLevel not found: " + warehouseCode + "/" + sku);
        }
        return entity;
    }

    public boolean exists(String warehouseCode, String sku) {
        return stockLevelRepository.existsByIds(
                FieldValue.of(StockLevel::getWarehouseCode, warehouseCode),
                FieldValue.of(StockLevel::getSku, sku));
    }

    public void delete(String warehouseCode, String sku) {
        int affected = stockLevelRepository.deleteByIds(
                FieldValue.of(StockLevel::getWarehouseCode, warehouseCode),
                FieldValue.of(StockLevel::getSku, sku));
        if (affected == 0) {
            throw new BusinessException(CommonErrorCode.NOT_FOUND,
                    "StockLevel not found: " + warehouseCode + "/" + sku);
        }
    }

    public List<StockLevel> list() {
        return stockLevelRepository.findAll();
    }
}
