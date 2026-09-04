package vn.org.thn.service.app.quiz.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.org.thn.service.app.quiz.dto.AdminParentSummary;
import vn.org.thn.service.app.quiz.dto.AdminSetActiveRequest;
import vn.org.thn.service.app.quiz.dto.ParentRegisterRequest;
import vn.org.thn.service.app.quiz.entity.Attempt;
import vn.org.thn.service.app.quiz.entity.AttemptAnswer;
import vn.org.thn.service.app.quiz.entity.Choice;
import vn.org.thn.service.app.quiz.entity.Classroom;
import vn.org.thn.service.app.quiz.entity.Lesson;
import vn.org.thn.service.app.quiz.entity.Parent;
import vn.org.thn.service.app.quiz.entity.Question;
import vn.org.thn.service.app.quiz.entity.RefreshToken;
import vn.org.thn.service.app.quiz.entity.Student;
import vn.org.thn.service.app.quiz.entity.Subject;
import vn.org.thn.service.app.quiz.entity.Test;
import vn.org.thn.service.app.quiz.entity.TestQuestion;
import vn.org.thn.service.app.quiz.exception.QuizErrorCode;
import vn.org.thn.service.app.quiz.repository.AttemptAnswerRepository;
import vn.org.thn.service.app.quiz.repository.AttemptRepository;
import vn.org.thn.service.app.quiz.repository.ChoiceRepository;
import vn.org.thn.service.app.quiz.repository.ClassroomRepository;
import vn.org.thn.service.app.quiz.repository.LessonRepository;
import vn.org.thn.service.app.quiz.repository.ParentRepository;
import vn.org.thn.service.app.quiz.repository.QuestionRepository;
import vn.org.thn.service.app.quiz.repository.RefreshTokenRepository;
import vn.org.thn.service.app.quiz.repository.StudentRepository;
import vn.org.thn.service.app.quiz.repository.SubjectRepository;
import vn.org.thn.service.app.quiz.repository.TestQuestionRepository;
import vn.org.thn.service.app.quiz.repository.TestRepository;
import vn.org.thn.service.app.quiz.security.CurrentUser;
import vn.org.thn.service.app.quiz.security.Role;
import vn.org.thn.service.base.IBase;
import vn.org.thn.service.base.exception.BusinessException;
import vn.org.thn.service.base.exception.CommonErrorCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Admin's management of Parent accounts (2026-09-04): list every Parent, create one by hand
 * (skips the normal self-registration auto-login - an Admin-created Parent is NOT auto-logged-in,
 * unlike {@code AuthService#registerParent}), activate/deactivate one, and permanently delete one.
 * <p>
 * <b>Deactivation</b> ({@link #setActive}) flips {@link Parent#isActive()} - checked at login time
 * by {@code AuthService}, and additionally cuts off every ALREADY-logged-in session (Parent's own
 * and every one of their Students') within the same call, by reusing {@code
 * AuthService#invalidateSessions(Long, Role)} - the exact same force-logout mechanism a
 * Parent/Student can trigger on themselves via {@code SessionApi}, just triggered by an Admin
 * instead. See {@link Parent#isActive()}'s javadoc - this is the method that javadoc refers to.
 * <p>
 * <b>Deletion</b> ({@link #deleteCascade}) is a full, unconditional cascade - per the user's
 * explicit decision when this feature was scoped ("Xoa han toan bo (cascade) - khong chan"),
 * deleting a Parent removes EVERY piece of data under them with no blocking rule (no "Parent has
 * data, refuse" check like {@code TEST_HAS_ATTEMPTS}/{@code QUESTION_HAS_ATTEMPTS} elsewhere in
 * this codebase - those exist to protect a PARENT's own history from THEIR OWN accidental delete
 * clicks; an Admin deliberately removing an entire account is a different, deliberate action).
 * MyBatis has no real JOIN/ON-DELETE-CASCADE wired up in this codebase (see {@code
 * TestService#questionIdsOfSubject}'s javadoc), so this walks the whole ownership graph by hand,
 * collecting ids top-down (Parent -&gt; Classroom -&gt; Subject -&gt; Lesson -&gt; Question -&gt;
 * Choice; Parent -&gt; Student -&gt; Attempt -&gt; AttemptAnswer; Parent -&gt; Test -&gt;
 * TestQuestion; Parent/Student -&gt; RefreshToken) and then deletes bottom-up (leaf tables first)
 * so no foreign key is ever violated. <b>Every {@code .in()} call below is guarded against an
 * empty/null id list first</b> - {@code .in()} is a silent no-op on an empty collection (see
 * {@code BaseConditionBuilder#in}'s javadoc), which would otherwise be misread as "no filter" and
 * delete every row in that table system-wide instead of none - the exact same guard convention
 * {@code TestService}/{@code StudentLessonService} already use for read-only traversals, just
 * applied here to deletes, where getting it wrong would be catastrophic instead of merely wrong.
 */
@Service
public class AdminParentService extends IBase {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    private ParentRepository parentRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ClassroomRepository classroomRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private ChoiceRepository choiceRepository;

    @Autowired
    private TestRepository testRepository;

    @Autowired
    private TestQuestionRepository testQuestionRepository;

    @Autowired
    private AttemptRepository attemptRepository;

    @Autowired
    private AttemptAnswerRepository attemptAnswerRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private AuthService authService;

    /** Every Parent in the system, most-recently-created-first is NOT guaranteed (no ORDER BY here, same "whatever the DB returns" as {@code BaseRepositoryImpl#findFirst}) - the Admin UI sorts/filters client-side. */
    public List<AdminParentSummary> list() {
        return parentRepository.findAll().stream().map(AdminParentSummary::from).toList();
    }

    /** Admin-created Parent - unlike {@code AuthService#registerParent}, does NOT auto-login (an Admin is creating an account for someone else, not for themselves). */
    @Transactional
    public AdminParentSummary create(ParentRegisterRequest request) {
        if (parentRepository.query().eq(Parent::getEmail, request.getEmail()).exists()) {
            throw new BusinessException(QuizErrorCode.EMAIL_TAKEN);
        }

        Long adminId = CurrentUser.get().userId();
        LocalDateTime now = LocalDateTime.now();
        String actor = "admin:" + adminId;

        Parent parent = new Parent();
        parent.setFullName(request.getFullName());
        parent.setEmail(request.getEmail());
        parent.setPassword(passwordEncoder.encode(request.getPassword()));
        parent.setPhone(request.getPhone());
        parent.setActive(true);
        parent.setCreatedAt(now);
        parent.setUpdatedAt(now);
        parent.setCreatedBy(actor);
        parent.setUpdatedBy(actor);
        parent = parentRepository.save(parent);

        logInfo("Parent created by Admin: id={}, email={}, adminId={}", parent.getId(), parent.getEmail(), adminId);
        return AdminParentSummary.from(parent);
    }

    /**
     * Activates or deactivates {@code parentId}. On deactivation, immediately force-logs-out the
     * Parent AND every one of their Students (not just flips the flag for the next login check) -
     * see this class's javadoc, "Deactivation".
     */
    @Transactional
    public AdminParentSummary setActive(Long parentId, AdminSetActiveRequest request) {
        Parent parent = getOwnedOrThrow(parentId);
        Long adminId = CurrentUser.get().userId();

        parent.setActive(request.isActive());
        parent.setUpdatedAt(LocalDateTime.now());
        parent.setUpdatedBy("admin:" + adminId);
        parent = parentRepository.save(parent);

        if (!request.isActive()) {
            authService.invalidateSessions(parent.getId(), Role.PARENT);
            List<Long> studentIds = studentIdsOf(parentId);
            for (Long studentId : studentIds) {
                authService.invalidateSessions(studentId, Role.STUDENT);
            }
        }

        logInfo("Parent active flag changed by Admin: id={}, active={}, adminId={}", parentId, request.isActive(), adminId);
        return AdminParentSummary.from(parent);
    }

    /**
     * Permanently deletes {@code parentId} and EVERY piece of data under them - see this class's
     * javadoc, "Deletion", for the full id-collection-then-bottom-up-delete shape and why every
     * {@code .in()} call is guarded.
     */
    @Transactional
    public void deleteCascade(Long parentId) {
        getOwnedOrThrow(parentId);
        Long adminId = CurrentUser.get().userId();

        // --- 1. Collect every id in the ownership graph first (read-only, order doesn't matter). ---
        List<Long> studentIds = studentIdsOf(parentId);
        List<Long> classroomIds = classroomRepository.query().eq(Classroom::getParentId, parentId).list()
                .stream().map(Classroom::getId).toList();
        List<Long> subjectIds = classroomIds.isEmpty() ? List.of()
                : subjectRepository.query().in(Subject::getClassroomId, classroomIds).list()
                        .stream().map(Subject::getId).toList();
        List<Long> lessonIds = subjectIds.isEmpty() ? List.of()
                : lessonRepository.query().in(Lesson::getSubjectId, subjectIds).list()
                        .stream().map(Lesson::getId).toList();
        List<Long> questionIds = lessonIds.isEmpty() ? List.of()
                : questionRepository.query().in(Question::getLessonId, lessonIds).list()
                        .stream().map(Question::getId).toList();
        List<Long> testIds = testRepository.query().eq(Test::getParentId, parentId).list()
                .stream().map(Test::getId).toList();

        // --- 2. Delete bottom-up (leaf tables first) so no foreign key is ever violated. ---
        if (!studentIds.isEmpty()) {
            List<Long> attemptIds = attemptRepository.query().in(Attempt::getStudentId, studentIds).list()
                    .stream().map(Attempt::getId).toList();
            if (!attemptIds.isEmpty()) {
                attemptAnswerRepository.delete().in(AttemptAnswer::getAttemptId, attemptIds).execute();
            }
            attemptRepository.delete().in(Attempt::getStudentId, studentIds).execute();
        }
        if (!testIds.isEmpty()) {
            testQuestionRepository.delete().in(TestQuestion::getTestId, testIds).execute();
        }
        testRepository.delete().eq(Test::getParentId, parentId).execute();
        if (!questionIds.isEmpty()) {
            choiceRepository.delete().in(Choice::getQuestionId, questionIds).execute();
        }
        if (!lessonIds.isEmpty()) {
            questionRepository.delete().in(Question::getLessonId, lessonIds).execute();
        }
        if (!subjectIds.isEmpty()) {
            lessonRepository.delete().in(Lesson::getSubjectId, subjectIds).execute();
        }
        if (!classroomIds.isEmpty()) {
            subjectRepository.delete().in(Subject::getClassroomId, classroomIds).execute();
        }
        classroomRepository.delete().eq(Classroom::getParentId, parentId).execute();
        refreshTokenRepository.delete().eq(RefreshToken::getUserId, parentId).eq(RefreshToken::getRole, Role.PARENT.name()).execute();
        if (!studentIds.isEmpty()) {
            refreshTokenRepository.delete().in(RefreshToken::getUserId, studentIds).eq(RefreshToken::getRole, Role.STUDENT.name()).execute();
        }
        studentRepository.delete().eq(Student::getParentId, parentId).execute();
        parentRepository.deleteById(parentId);

        logInfo("Parent deleted (cascade) by Admin: id={}, adminId={}, studentsDeleted={}, classroomsDeleted={}, " +
                        "subjectsDeleted={}, lessonsDeleted={}, questionsDeleted={}, testsDeleted={}",
                parentId, adminId, studentIds.size(), classroomIds.size(), subjectIds.size(), lessonIds.size(),
                questionIds.size(), testIds.size());
    }

    private List<Long> studentIdsOf(Long parentId) {
        return studentRepository.query().eq(Student::getParentId, parentId).list()
                .stream().map(Student::getId).toList();
    }

    private Parent getOwnedOrThrow(Long parentId) {
        Parent parent = parentRepository.findById(parentId);
        if (parent == null) {
            throw new BusinessException(CommonErrorCode.NOT_FOUND, "Parent not found");
        }
        return parent;
    }
}
