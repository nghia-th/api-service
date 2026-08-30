package vn.org.thn.service.app.example.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.org.thn.service.app.example.dto.ArticleRequest;
import vn.org.thn.service.app.example.entity.Article;
import vn.org.thn.service.app.example.repository.ArticleRepository;
import vn.org.thn.service.base.IBase;
import vn.org.thn.service.base.exception.BusinessException;
import vn.org.thn.service.base.exception.CommonErrorCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for {@link Article} - DB test entity #4 ({@code extends BaseEntity}). As documented on
 * {@code BaseEntity}, none of the 4 audit fields are auto-populated, so {@link #create}/
 * {@link #update} set them by hand. {@link #incrementViews} goes through {@code UpdateBuilder}
 * (a partial-column UPDATE) instead of a full-row {@code save()}, to exercise that builder too -
 * every other entity/service in this batch only ever calls {@code save()}.
 */
@Service
public class ArticleService extends IBase {

    @Autowired
    private ArticleRepository articleRepository;

    public Article create(ArticleRequest request) {
        Article entity = new Article();
        entity.setTitle(request.getTitle());
        entity.setContent(request.getContent());
        entity.setPublishedAt(LocalDateTime.now());

        LocalDateTime now = LocalDateTime.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setCreatedBy(request.getAuthor());
        entity.setUpdatedBy(request.getAuthor());

        return articleRepository.save(entity);
    }

    public Article update(Long id, ArticleRequest request) {
        Article entity = getOrThrow(id);
        entity.setTitle(request.getTitle());
        entity.setContent(request.getContent());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUpdatedBy(request.getAuthor());
        return articleRepository.save(entity);
    }

    public Article get(Long id) {
        return getOrThrow(id);
    }

    public List<Article> list() {
        return articleRepository.findAll();
    }

    public void delete(Long id) {
        articleRepository.deleteById(id);
    }

    /** Bumps {@code views} by 1 via a targeted {@code UPDATE article SET views = ? WHERE id = ?} instead of rewriting the whole row. */
    public Article incrementViews(Long id) {
        Article entity = getOrThrow(id);
        long newViews = (entity.getViews() == null ? 0L : entity.getViews()) + 1;
        articleRepository.update().set(Article::getViews, newViews).eq(Article::getId, id).execute();
        entity.setViews(newViews);
        return entity;
    }

    private Article getOrThrow(Long id) {
        Article entity = articleRepository.findById(id);
        if (entity == null) {
            throw new BusinessException(CommonErrorCode.NOT_FOUND, "Article not found: " + id);
        }
        return entity;
    }
}
