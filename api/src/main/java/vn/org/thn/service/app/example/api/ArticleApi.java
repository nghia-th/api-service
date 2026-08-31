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
import org.springframework.web.bind.annotation.RestController;
import vn.org.thn.service.app.example.dto.ArticleRequest;
import vn.org.thn.service.app.example.entity.Article;
import vn.org.thn.service.app.example.service.ArticleService;
import vn.org.thn.service.base.controller.BaseCtl;
import vn.org.thn.service.base.response.ApiResponse;

import java.util.List;

/**
 * DB test API #4 ({@code extends BaseEntity}). {@code POST /{id}/view} is the one worth trying
 * to see a partial-column {@code UpdateBuilder} UPDATE rather than a full-row {@code save()}.
 */
@RestController
@RequestMapping("/example/article")
public class ArticleApi extends BaseCtl {

    @Autowired
    private ArticleService articleService;

    @PostMapping
    public ResponseEntity<ApiResponse<Article>> create(@RequestBody ArticleRequest request) {
        return ok(articleService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Article>> update(@PathVariable Long id, @RequestBody ArticleRequest request) {
        return ok(articleService.update(id, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Article>> get(@PathVariable Long id) {
        return ok(articleService.get(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        articleService.delete(id);
        return ok();
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Article>>> list() {
        return ok(articleService.list());
    }

    @PostMapping("/{id}/view")
    public ResponseEntity<ApiResponse<Article>> view(@PathVariable Long id) {
        return ok(articleService.incrementViews(id));
    }
}
