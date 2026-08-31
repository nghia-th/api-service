package vn.org.thn.service.app.example.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.org.thn.service.app.example.dto.CategoryRequest;
import vn.org.thn.service.app.example.entity.Category;
import vn.org.thn.service.app.example.repository.CategoryRepository;
import vn.org.thn.service.base.IBase;
import vn.org.thn.service.base.db.mybatis.query.QueryBuilder;
import vn.org.thn.service.base.dto.page.PageRequest;
import vn.org.thn.service.base.dto.page.PageResponse;
import vn.org.thn.service.base.exception.BusinessException;
import vn.org.thn.service.base.exception.CommonErrorCode;

import java.util.List;

/**
 * Service for {@link Category} - DB test entity #1 (auto-increment id). {@link #search} is the
 * one worth trying first: it exercises {@code QueryBuilder}'s {@code like}/{@code eq}/
 * {@code orderByDesc}/{@code page}/{@code pageResult} in one call.
 */
@Service
public class CategoryService extends IBase {

    @Autowired
    private CategoryRepository categoryRepository;

    public Category create(CategoryRequest request) {
        Category entity = new Category();
        apply(entity, request);
        return categoryRepository.save(entity);
    }

    public Category update(Long id, CategoryRequest request) {
        Category entity = getOrThrow(id);
        apply(entity, request);
        return categoryRepository.save(entity);
    }

    public Category get(Long id) {
        return getOrThrow(id);
    }

    public List<Category> list() {
        return categoryRepository.findAll();
    }

    public void delete(Long id) {
        categoryRepository.deleteById(id);
    }

    /** Filters by {@code name} (LIKE, case rules per DB collation) and/or {@code active} (exact match), newest first, paged. */
    public PageResponse<Category> search(String keyword, Boolean active, PageRequest pageRequest) {
        QueryBuilder<Category> query = categoryRepository.query();
        if (keyword != null && !keyword.isBlank()) {
            query.like(Category::getName, keyword);
        }
        if (active != null) {
            query.eq(Category::getActive, active);
        }
        query.orderByDesc(Category::getId);
        query.page(pageRequest.getPage() + 1, pageRequest.getSize());
        return query.pageResult();
    }

    private Category getOrThrow(Long id) {
        Category entity = categoryRepository.findById(id);
        if (entity == null) {
            throw new BusinessException(CommonErrorCode.NOT_FOUND, "Category not found: " + id);
        }
        return entity;
    }

    private void apply(Category entity, CategoryRequest request) {
        entity.setCode(request.getCode());
        entity.setName(request.getName());
        entity.setActive(request.getActive() == null ? Boolean.TRUE : request.getActive());
    }
}
