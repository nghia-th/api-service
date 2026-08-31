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
import vn.org.thn.service.app.example.dto.TagRequest;
import vn.org.thn.service.app.example.entity.Tag;
import vn.org.thn.service.app.example.service.TagService;
import vn.org.thn.service.base.controller.BaseCtl;
import vn.org.thn.service.base.response.ApiResponse;

import java.util.List;

/**
 * DB test API #3 (client-supplied {@code String} id, no {@code @GeneratedValue}). One
 * {@code POST} handles both create and update - call it twice with the same {@code id} in the
 * body to see the upsert in action.
 */
@RestController
@RequestMapping("/example/tag")
public class TagApi extends BaseCtl {

    @Autowired
    private TagService tagService;

    @PostMapping
    public ResponseEntity<ApiResponse<Tag>> save(@RequestBody TagRequest request) {
        return ok(tagService.save(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Tag>> get(@PathVariable String id) {
        return ok(tagService.get(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        tagService.delete(id);
        return ok();
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Tag>>> list() {
        return ok(tagService.list());
    }
}
