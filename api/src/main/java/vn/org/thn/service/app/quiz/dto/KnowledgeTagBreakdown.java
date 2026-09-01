package vn.org.thn.service.app.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * One group in {@link AttemptReportResponse#getByKnowledgeTag()} - this is the feature the whole
 * product concept centers on (see {@code claude/hieu-bai-app-phan-tich.md}: "not just a score, but
 * which knowledge area the mistakes are in"). {@code knowledgeTag} is the literal Vietnamese label
 * "Chưa phân loại" ("Uncategorized") for questions with no tag - a report label the Parent reads,
 * not code prose, same reasoning as task 4's import template/error text - see the LANGUAGE NOTE in
 * {@code QuestionImportService}.
 */
@Data
@AllArgsConstructor
public class KnowledgeTagBreakdown {
    private String knowledgeTag;
    private int correctCount;
    private int totalCount;
}
