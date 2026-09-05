package vn.org.thn.service.app.quiz.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.org.thn.service.app.quiz.dto.PracticeGenerateRequest;
import vn.org.thn.service.app.quiz.dto.QuestionResponse;
import vn.org.thn.service.app.quiz.dto.TestCreateFromLessonsRequest;
import vn.org.thn.service.app.quiz.dto.TestCreateRequest;
import vn.org.thn.service.app.quiz.dto.TestDetailResponse;
import vn.org.thn.service.app.quiz.dto.TestResponse;
import vn.org.thn.service.app.quiz.entity.Attempt;
import vn.org.thn.service.app.quiz.entity.Lesson;
import vn.org.thn.service.app.quiz.entity.Question;
import vn.org.thn.service.app.quiz.entity.QuestionType;
import vn.org.thn.service.app.quiz.entity.Student;
import vn.org.thn.service.app.quiz.entity.Subject;
import vn.org.thn.service.app.quiz.entity.Test;
import vn.org.thn.service.app.quiz.entity.TestQuestion;
import vn.org.thn.service.app.quiz.entity.TestStatus;
import vn.org.thn.service.app.quiz.entity.TestType;
import vn.org.thn.service.app.quiz.exception.QuizErrorCode;
import vn.org.thn.service.app.quiz.repository.AttemptRepository;
import vn.org.thn.service.app.quiz.repository.LessonRepository;
import vn.org.thn.service.app.quiz.repository.QuestionRepository;
import vn.org.thn.service.app.quiz.repository.StudentRepository;
import vn.org.thn.service.app.quiz.repository.TestQuestionRepository;
import vn.org.thn.service.app.quiz.repository.TestRepository;
import vn.org.thn.service.app.quiz.security.CurrentUser;
import vn.org.thn.service.base.IBase;
import vn.org.thn.service.base.exception.BusinessException;
import vn.org.thn.service.base.exception.CommonErrorCode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Test creation/listing/deletion for the currently logged-in Parent (task 5). Creating a Test
 * always assigns it to a Student in the same call - there is no separate "assign" step in v1
 * (see {@code docs/dev/05-tao-giao-bai-kiem-tra.md}).
 * <p>
 * {@code studentId} ownership reuses {@link StudentService#getOwnedOrThrow} and every element of
 * {@code questionIds} reuses {@link QuestionService#getOwnedOrThrow} (both package-private, same
 * reuse pattern as {@code LessonService}/{@code SubjectService}) - a Test itself does have its own
 * {@code parentId} column (unlike Lesson/Question), so this class's own {@code getOwnedOrThrow}
 * checks it directly rather than resolving through another entity.
 * <p>
 * {@code generatePractice}/{@code generatePracticeForStudent} (added 2026-09-01, "On tap kien
 * thuc") reuse every field/table above unchanged - a practice Test is just a regular {@link Test}
 * row with {@code testType = }{@link TestType#PRACTICE}, and its {@link TestQuestion} rows are
 * picked randomly instead of from an explicit {@code questionIds} list. See {@link TestType}'s
 * javadoc for why a "retake" always creates a brand-new Test rather than resetting an existing
 * one.
 */
@Service
public class TestService extends IBase {

    /** Default question count for a generated practice Test when {@code questionCount} is not supplied (or is non-positive) - capped at the Subject's actual pool size either way, see {@link #doGeneratePractice}. */
    private static final int DEFAULT_PRACTICE_QUESTION_COUNT = 10;

    @Autowired
    private TestRepository testRepository;

    @Autowired
    private TestQuestionRepository testQuestionRepository;

    @Autowired
    private AttemptRepository attemptRepository;

    @Autowired
    private StudentService studentService;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private SubjectService subjectService;

    @Autowired
    private LessonService lessonService;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Transactional
    public TestResponse create(TestCreateRequest request) {
        Long parentId = CurrentUser.get().userId();
        studentService.getOwnedOrThrow(request.getStudentId(), parentId);
        // Every questionId must belong to this parent too - checked one by one rather than a
        // single batch query, matching this codebase's existing ownership-check style (see
        // StudentService/SubjectService/LessonService); question lists are small (one Test's
        // worth), so the extra round trips are not a concern in v1.
        for (Long questionId : request.getQuestionIds()) {
            questionService.getOwnedOrThrow(questionId, parentId);
        }

        LocalDateTime now = LocalDateTime.now();
        Test test = new Test();
        test.setParentId(parentId);
        test.setStudentId(request.getStudentId());
        test.setName(request.getName());
        test.setStatus(TestStatus.ASSIGNED.name());
        // Every INSERT writes every non-primary-key column straight from this object (see
        // QuestionService's javadoc on the same InsertExecutor behavior for UPDATE) - the
        // V8__test_type.sql DEFAULT only backfills pre-existing rows, it does NOT apply to a
        // fresh INSERT that supplies its own (even null) value, so testType must be set
        // explicitly here exactly like status is, or this would insert NULL and violate the
        // column's NOT NULL constraint.
        test.setTestType(TestType.REGULAR.name());
        test.setCreatedAt(now);
        test.setUpdatedAt(now);
        test.setCreatedBy("parent:" + parentId);
        test.setUpdatedBy("parent:" + parentId);
        test = testRepository.save(test);

        int orderIndex = 0;
        for (Long questionId : request.getQuestionIds()) {
            TestQuestion testQuestion = new TestQuestion();
            testQuestion.setTestId(test.getId());
            testQuestion.setQuestionId(questionId);
            testQuestion.setOrderIndex(orderIndex++);
            testQuestionRepository.save(testQuestion);
        }

        logInfo("Test created: id={}, studentId={}, parentId={}, questionCount={}",
                test.getId(), test.getStudentId(), parentId, request.getQuestionIds().size());
        return TestResponse.from(test);
    }

    /**
     * Creates and assigns a REGULAR Test the same way {@link #create} does, but scoped to whole
     * Lessons instead of one Lesson (2026-09-05, item 3/11 of the 11-item batch request - see
     * {@link TestCreateFromLessonsRequest}'s javadoc for the full design, including the
     * 2026-09-06 revision below).
     * <p>
     * <b>2026-09-06 revision</b> - the original version of this method auto-included EVERY
     * Question under every selected Lesson, with no way for the Parent to leave any out. Per the
     * user's follow-up feedback after testing ("chưa thấy danh sách câu hỏi ... để phụ huynh chọn
     * câu hỏi cho đề kiểm tra"), {@code lessonIds} is now only the SCOPE (which Questions are
     * allowed to be picked from), and {@code request.getQuestionIds()} carries the Parent's
     * actual hand-picked set - same "server re-validates, never trusts the client" posture as
     * every other submit in this class: each {@code questionId} is checked against the combined
     * pool of the (already ownership/classroom-checked) {@code lessonIds} before being used, so a
     * tampered request naming a Question from an un-selected Lesson is rejected exactly like a
     * tampered {@code lessonId} always was. The final display order is still shuffled
     * server-side, unchanged from the pre-revision behavior.
     * <p>
     * Each {@code lessonId} is ownership-checked via {@link LessonService#getOwnedOrThrow} AND
     * cross-checked against the student's OWN classroom (its owning Subject's {@code
     * classroomId} must equal {@code student.getClassroomId()}) - same rigor as {@link
     * #generatePractice}'s subject/classroom check, guarding against a tampered request naming a
     * Lesson from a different Classroom than the assignee actually studies in (the frontend only
     * ever offers Lessons already restricted to the student's own classroom, so this is
     * defense-in-depth, not something a normal use of the picker could trigger).
     */
    @Transactional
    public TestResponse createFromLessons(TestCreateFromLessonsRequest request) {
        Long parentId = CurrentUser.get().userId();
        Student student = studentService.getOwnedOrThrow(request.getStudentId(), parentId);

        // Pool cho phép chọn câu hỏi (2026-09-06 revision) - KHÔNG còn là "danh sách sẽ đưa hết
        // vào đề" nữa, chỉ dùng để validate request.getQuestionIds() bên dưới.
        Set<Long> allowedQuestionIds = new HashSet<>();
        for (Long lessonId : request.getLessonIds()) {
            Lesson lesson = lessonService.getOwnedOrThrow(lessonId, parentId);
            Subject subject = subjectService.getById(lesson.getSubjectId());
            if (!subject.getClassroomId().equals(student.getClassroomId())) {
                throw new BusinessException(CommonErrorCode.FORBIDDEN, "This lesson is not in the student's classroom");
            }
            allowedQuestionIds.addAll(questionRepository.query().eq(Question::getLessonId, lessonId).list()
                    .stream().map(Question::getId).toList());
        }
        if (allowedQuestionIds.isEmpty()) {
            throw new BusinessException(QuizErrorCode.LESSON_SELECTION_NO_QUESTIONS);
        }

        // LinkedHashSet chỉ để loại trùng nếu request lỡ gửi trùng id, thứ tự không quan trọng vì
        // shuffle ngay bên dưới. Mỗi id PHẢI nằm trong allowedQuestionIds - không tin request.
        List<Long> questionIds = new ArrayList<>(new LinkedHashSet<>(request.getQuestionIds()));
        for (Long questionId : questionIds) {
            if (!allowedQuestionIds.contains(questionId)) {
                throw new BusinessException(CommonErrorCode.FORBIDDEN, "This question is not in the selected lessons");
            }
        }
        Collections.shuffle(questionIds);

        LocalDateTime now = LocalDateTime.now();
        Test test = new Test();
        test.setParentId(parentId);
        test.setStudentId(request.getStudentId());
        test.setName(request.getName());
        test.setStatus(TestStatus.ASSIGNED.name());
        test.setTestType(TestType.REGULAR.name());
        test.setCreatedAt(now);
        test.setUpdatedAt(now);
        test.setCreatedBy("parent:" + parentId);
        test.setUpdatedBy("parent:" + parentId);
        test = testRepository.save(test);

        int orderIndex = 0;
        for (Long questionId : questionIds) {
            TestQuestion testQuestion = new TestQuestion();
            testQuestion.setTestId(test.getId());
            testQuestion.setQuestionId(questionId);
            testQuestion.setOrderIndex(orderIndex++);
            testQuestionRepository.save(testQuestion);
        }

        logInfo("Test created from lessons: id={}, studentId={}, parentId={}, lessonCount={}, questionCount={}",
                test.getId(), test.getStudentId(), parentId, request.getLessonIds().size(), questionIds.size());
        return TestResponse.from(test);
    }

    /** Test detail with its questions in {@code orderIndex} order, each as the full Parent-facing {@link QuestionResponse} (including the correct choice). */
    public TestDetailResponse get(Long id) {
        Long parentId = CurrentUser.get().userId();
        Test test = getOwnedOrThrow(id, parentId);

        List<QuestionResponse> questions = testQuestionRepository.query()
                .eq(TestQuestion::getTestId, id)
                .list()
                .stream()
                .sorted((a, b) -> Integer.compare(a.getOrderIndex(), b.getOrderIndex()))
                .map(testQuestion -> questionService.get(testQuestion.getQuestionId()))
                .toList();

        return TestDetailResponse.from(test, questions);
    }

    /** Every Test belonging to the current Parent, optionally narrowed to one Student - {@code studentId == null} means "every student" (see task 5 spec). */
    public List<TestResponse> list(Long studentId) {
        Long parentId = CurrentUser.get().userId();
        if (studentId != null) {
            studentService.getOwnedOrThrow(studentId, parentId);
        }
        // .eq() is a no-op when the value is null (see BaseConditionBuilder), so this one query
        // works unchanged whether studentId was supplied or not.
        return testRepository.query()
                .eq(Test::getParentId, parentId)
                .eq(Test::getStudentId, studentId)
                .list()
                .stream().map(TestResponse::from).toList();
    }

    @Transactional
    public void delete(Long id) {
        Long parentId = CurrentUser.get().userId();
        Test test = getOwnedOrThrow(id, parentId);

        if (attemptRepository.query().eq(Attempt::getTestId, id).exists()) {
            throw new BusinessException(QuizErrorCode.TEST_HAS_ATTEMPTS);
        }
        testQuestionRepository.delete().eq(TestQuestion::getTestId, id).execute();
        testRepository.deleteById(test.getId());
        logInfo("Test deleted: id={}, parentId={}", test.getId(), parentId);
    }

    /** Loads the Test with id {@code id}, throwing if it doesn't exist or doesn't belong to {@code parentId}. Package-private so task 7's {@code ReportService} can reuse it. */
    Test getOwnedOrThrow(Long id, Long parentId) {
        Test test = testRepository.findById(id);
        if (test == null) {
            throw new BusinessException(CommonErrorCode.NOT_FOUND, "Test not found");
        }
        if (!test.getParentId().equals(parentId)) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN, "This test does not belong to the current parent");
        }
        return test;
    }

    /**
     * Parent-facing "On tap kien thuc" generation - {@code POST /api/parent/tests/practice}.
     * studentId/subjectId are both re-validated against the current Parent exactly like {@link
     * #create}, plus one extra check {@code #create} does not need: the Subject must be in the
     * SAME Classroom as the Student (a Subject only makes sense as "on tap" material for a
     * Student actually studying it) - {@code #create} never needed this check because a Parent
     * hand-picks explicit questionIds there, so a mismatched pick is just an unusual manual
     * choice, not a random one this method would make silently on the Parent's behalf.
     */
    @Transactional
    public TestResponse generatePractice(PracticeGenerateRequest request) {
        Long parentId = CurrentUser.get().userId();
        Student student = studentService.getOwnedOrThrow(request.getStudentId(), parentId);
        Subject subject = subjectService.getOwnedOrThrow(request.getSubjectId(), parentId);
        if (!subject.getClassroomId().equals(student.getClassroomId())) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN, "This subject is not in the student's classroom");
        }
        return doGeneratePractice(parentId, student.getId(), subject, request.getName(), request.getQuestionCount(), "parent:" + parentId);
    }

    /**
     * Student self-service "On tap kien thuc" generation, called from {@code
     * StudentAttemptService#generatePractice} (package-private, same cross-service reuse pattern
     * as {@link #getOwnedOrThrow}) - studentId is always the caller's own id (CurrentUser, read by
     * the caller before this is invoked), so there is no Student ownership check here, only that
     * subjectId belongs to the SAME parent that owns this student and is in the student's own
     * classroom.
     */
    @Transactional
    TestResponse generatePracticeForStudent(Long studentId, Long subjectId, String name, Integer questionCount) {
        Student student = studentRepository.findById(studentId);
        if (student == null) {
            throw new BusinessException(CommonErrorCode.NOT_FOUND, "Student not found");
        }
        Subject subject = subjectService.getOwnedOrThrow(subjectId, student.getParentId());
        if (!subject.getClassroomId().equals(student.getClassroomId())) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN, "This subject is not in your classroom");
        }
        return doGeneratePractice(student.getParentId(), student.getId(), subject, name, questionCount, "student:" + studentId);
    }

    /**
     * Shared by both entry points above - picks {@code questionCount} (default {@link
     * #DEFAULT_PRACTICE_QUESTION_COUNT}, capped at the pool size) random Questions from every
     * Lesson under {@code subject}, then creates a Test + TestQuestion rows exactly like {@link
     * #create} does, tagged {@link TestType#PRACTICE} instead of {@link TestType#REGULAR}.
     */
    private TestResponse doGeneratePractice(Long parentId, Long studentId, Subject subject, String requestedName, Integer requestedCount, String actor) {
        List<Long> pool = questionIdsOfSubject(subject.getId());
        if (pool.isEmpty()) {
            throw new BusinessException(QuizErrorCode.SUBJECT_NO_QUESTIONS);
        }

        // Collections.shuffle + take-the-first-N is a simple, unbiased way to pick N distinct
        // random elements out of pool without replacement - no existing random-selection
        // precedent elsewhere in this codebase to match, this is the first.
        List<Long> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled);
        int count = (requestedCount == null || requestedCount <= 0) ? DEFAULT_PRACTICE_QUESTION_COUNT : requestedCount;
        count = Math.min(count, shuffled.size());
        List<Long> questionIds = shuffled.subList(0, count);

        String name = (requestedName == null || requestedName.isBlank())
                ? "Ôn tập " + subject.getName()
                : requestedName;

        LocalDateTime now = LocalDateTime.now();
        Test test = new Test();
        test.setParentId(parentId);
        test.setStudentId(studentId);
        test.setName(name);
        test.setStatus(TestStatus.ASSIGNED.name());
        test.setTestType(TestType.PRACTICE.name());
        test.setCreatedAt(now);
        test.setUpdatedAt(now);
        test.setCreatedBy(actor);
        test.setUpdatedBy(actor);
        test = testRepository.save(test);

        int orderIndex = 0;
        for (Long questionId : questionIds) {
            TestQuestion testQuestion = new TestQuestion();
            testQuestion.setTestId(test.getId());
            testQuestion.setQuestionId(questionId);
            testQuestion.setOrderIndex(orderIndex++);
            testQuestionRepository.save(testQuestion);
        }

        logInfo("Practice test generated: id={}, studentId={}, subjectId={}, parentId={}, questionCount={}",
                test.getId(), studentId, subject.getId(), parentId, questionIds.size());
        return TestResponse.from(test);
    }

    /**
     * Every Question id under every Lesson of {@code subjectId} - MyBatis has no real JOIN in
     * this codebase, so this resolves Subject -&gt; Lesson -&gt; Question sequentially, exactly
     * the same "collect ids at each step, guard {@code .in()} against an empty/null collection"
     * shape {@code StudentLessonService} already uses for its own Lesson -&gt; Question -&gt;
     * TestQuestion -&gt; Test traversal (see {@code lesson-content-feature-backend-2026-09-01.md}) -
     * {@code .in()} is a silent no-op on an empty collection, which would otherwise be
     * misread as "no filter" instead of "no matches" and leak every Question in the table.
     * <p>
     * SPEAKING questions are filtered out here (added 2026-09-01, "speaking question" feature) -
     * per the user's explicit answer when that feature was scoped ("khong - chi random cau trac
     * nghiem"), "On tap kien thuc" only ever picks auto-graded MULTIPLE_CHOICE questions, never a
     * question that needs the Student to record their voice and the Parent to review it by hand.
     */
    private List<Long> questionIdsOfSubject(Long subjectId) {
        List<Long> lessonIds = lessonRepository.query().eq(Lesson::getSubjectId, subjectId).list()
                .stream().map(Lesson::getId).toList();
        if (lessonIds.isEmpty()) {
            return List.of();
        }
        return questionRepository.query().in(Question::getLessonId, lessonIds).list()
                .stream()
                .filter(question -> !QuestionType.SPEAKING.name().equals(question.getQuestionType()))
                .map(Question::getId).toList();
    }
}
