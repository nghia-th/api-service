package vn.org.thn.service.app.example.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.org.thn.service.app.example.dto.TagRequest;
import vn.org.thn.service.app.example.entity.Tag;
import vn.org.thn.service.app.example.repository.TagRepository;
import vn.org.thn.service.base.IBase;
import vn.org.thn.service.base.exception.BusinessException;
import vn.org.thn.service.base.exception.CommonErrorCode;

import java.util.List;

/**
 * Service for {@link Tag} - DB test entity #3 (client-supplied {@code String} id, no
 * {@code @GeneratedValue}). {@link #save} is a true upsert: the first call with a given id
 * INSERTs, a later call with the same id UPDATEs - no separate create/update branch needed.
 */
@Service
public class TagService extends IBase {

    @Autowired
    private TagRepository tagRepository;

    public Tag save(TagRequest request) {
        Tag entity = new Tag();
        entity.setId(request.getId());
        entity.setLabel(request.getLabel());
        entity.setPriority(request.getPriority() == null ? 0 : request.getPriority());
        return tagRepository.save(entity);
    }

    public Tag get(String id) {
        Tag entity = tagRepository.findById(id);
        if (entity == null) {
            throw new BusinessException(CommonErrorCode.NOT_FOUND, "Tag not found: " + id);
        }
        return entity;
    }

    public List<Tag> list() {
        return tagRepository.findAll();
    }

    public void delete(String id) {
        tagRepository.deleteById(id);
    }
}
