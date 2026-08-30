package vn.org.thn.service.app.example.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.org.thn.service.app.example.dto.StockLevelRequest;
import vn.org.thn.service.app.example.entity.StockLevel;
import vn.org.thn.service.app.example.service.StockLevelService;
import vn.org.thn.service.base.controller.BaseCtl;
import vn.org.thn.service.base.response.ApiResponse;

import java.util.List;

/**
 * DB test API #2 (composite key {@code warehouseCode + sku}). Both key parts are path variables
 * on every by-id endpoint - there is no single "id" to put in the URL.
 */
@RestController
@RequestMapping("/example/stock-level")
public class StockLevelApi extends BaseCtl {

    @Autowired
    private StockLevelService stockLevelService;

    /** Upsert - creates the (warehouseCode, sku) row if new, or overwrites quantity/unitPrice if it already exists. */
    @PostMapping
    public ResponseEntity<ApiResponse<StockLevel>> save(@RequestBody StockLevelRequest request) {
        return ok(stockLevelService.save(request));
    }

    @GetMapping("/{warehouseCode}/{sku}")
    public ResponseEntity<ApiResponse<StockLevel>> get(@PathVariable String warehouseCode, @PathVariable String sku) {
        return ok(stockLevelService.get(warehouseCode, sku));
    }

    @GetMapping("/{warehouseCode}/{sku}/exists")
    public ResponseEntity<ApiResponse<Boolean>> exists(@PathVariable String warehouseCode, @PathVariable String sku) {
        return ok(stockLevelService.exists(warehouseCode, sku));
    }

    @DeleteMapping("/{warehouseCode}/{sku}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String warehouseCode, @PathVariable String sku) {
        stockLevelService.delete(warehouseCode, sku);
        return ok();
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<StockLevel>>> list() {
        return ok(stockLevelService.list());
    }
}
