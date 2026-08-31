package vn.org.thn.service.app.example.repository;

import org.springframework.stereotype.Repository;
import vn.org.thn.service.app.example.entity.StockLevel;
import vn.org.thn.service.app.example.entity.StockLevelId;
import vn.org.thn.service.base.db.mybatis.repository.BaseRepositoryImpl;

@Repository
public class StockLevelRepository extends BaseRepositoryImpl<StockLevel, StockLevelId> {
}
