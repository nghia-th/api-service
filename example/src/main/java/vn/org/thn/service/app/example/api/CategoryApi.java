package vn.org.thn.service.app.example.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.org.thn.service.app.example.dto.CategoryRequest;
import vn.org.thn.service.app.example.entity.Category;
import vn.org.thn.service.app.example.service.CategoryService;
import vn.org.thn.service.base.controller.BaseCtl;
import vn.org.thn.service.base.dto.page.PageRequest;
import vn.org.thn.service.base.dto.page.PageResponse;
import vn.org.thn.service.base.response.ApiResponse;

import java.util.List;

/**
 * DB test API #1 (auto-increment id). Try {@link #search} first in Swagger - it's the one that
 * exercises paging/filtering, not just plain CRUD.
 */
@RestController
@RequestMapping("/example/category")
public class CategoryApi extends BaseCtl {

    @Autowired
    private CategoryService categoryService;

    @PostMapping
    public ResponseEntity<ApiResponse<Category>> create(@RequestBody CategoryRequest request) {
        return ok(categoryService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Category>> update(@PathVariable Long id, @RequestBody CategoryRequest request) {
        return ok(categoryService.update(id, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Category>> get(@PathVariable Long id) {
        return ok(categoryService.get(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ok();
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Category>>> list() {
        return ok(categoryService.list());
    }

    /** Filtered/paged search - query params: {@code keyword} (matched against name), {@code active}, plus {@link PageRequest}'s page/size/sortBy/sortDirection. */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<Category>>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean active,
            PageRequest pageRequest) {
        return ok(categoryService.search(keyword, active, pageRequest));
    }
}
