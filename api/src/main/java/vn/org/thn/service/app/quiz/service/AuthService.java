package vn.org.thn.service.app.quiz.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import vn.org.thn.service.app.quiz.dto.ParentAuthResponse;
import vn.org.thn.service.app.quiz.dto.ParentLoginRequest;
import vn.org.thn.service.app.quiz.dto.ParentRegisterRequest;
import vn.org.thn.service.app.quiz.dto.ParentResponse;
import vn.org.thn.service.app.quiz.dto.StudentAuthResponse;
import vn.org.thn.service.app.quiz.dto.StudentLoginRequest;
import vn.org.thn.service.app.quiz.dto.StudentResponse;
import vn.org.thn.service.app.quiz.entity.Parent;
import vn.org.thn.service.app.quiz.entity.Student;
import vn.org.thn.service.app.quiz.exception.QuizErrorCode;
import vn.org.thn.service.app.quiz.repository.ParentRepository;
import vn.org.thn.service.app.quiz.repository.StudentRepository;
import vn.org.thn.service.app.quiz.security.JwtUtil;
import vn.org.thn.service.app.quiz.security.Role;
import vn.org.thn.service.base.IBase;
import vn.org.thn.service.base.exception.BusinessException;

import java.time.LocalDateTime;

/**
 * Auth for both roles: Parent self-registration/login, Student login. There is no Student
 * self-register - students are created only via {@code POST /api/parent/students} (task 2, not
 * implemented yet), so this class has no {@code registerStudent}.
 * <p>
 * Password hashing uses a plain {@link BCryptPasswordEncoder} instance, not a full Spring
 * Security context - {@code base} has no auth infra yet (see {@code
 * claude/base-module-status.md}), and pulling in all of Spring Security for one encoder class
 * would be overkill for this module's minimal-dependency style (see task 1 spec).
 * <p>
 * Blank-field checks are no longer done here - {@code @Valid} + {@code @NotBlank}/{@code
 * @Email}/{@code @Size} on the request DTOs (enforced in {@code AuthApi}) reject those before
 * this class's methods ever run, via {@code base}'s {@code GlobalExceptionHandler}.
 */
@Service
public class AuthService extends IBase {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    private ParentRepository parentRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private JwtUtil jwtUtil;

    /** Registers a new Parent and auto-logs them in (returns a token immediately - simpler than requiring a separate login call right after register). */
    public ParentAuthResponse registerParent(ParentRegisterRequest request) {
        if (parentRepository.query().eq(Parent::getEmail, request.getEmail()).exists()) {
            throw new BusinessException(QuizErrorCode.EMAIL_TAKEN);
        }

        LocalDateTime now = LocalDateTime.now();
        Parent parent = new Parent();
        parent.setFullName(request.getFullName());
        parent.setEmail(request.getEmail());
        parent.setPassword(passwordEncoder.encode(request.getPassword()));
        parent.setPhone(request.getPhone());
        parent.setCreatedAt(now);
        parent.setUpdatedAt(now);
        parent.setCreatedBy(request.getEmail());
        parent.setUpdatedBy(request.getEmail());
        parent = parentRepository.save(parent);

        logInfo("Parent registered: id={}, email={}", parent.getId(), parent.getEmail());
        return new ParentAuthResponse(jwtUtil.generate(parent.getId(), Role.PARENT), ParentResponse.from(parent));
    }

    public ParentAuthResponse loginParent(ParentLoginRequest request) {
        Parent parent = parentRepository.query().eq(Parent::getEmail, request.getEmail()).one();
        // Same error for "no such email" and "wrong password" - never reveal which one it was (see task 1 spec).
        if (parent == null || !passwordEncoder.matches(request.getPassword(), parent.getPassword())) {
            throw new BusinessException(QuizErrorCode.INVALID_CREDENTIALS);
        }
        return new ParentAuthResponse(jwtUtil.generate(parent.getId(), Role.PARENT), ParentResponse.from(parent));
    }

    public StudentAuthResponse loginStudent(StudentLoginRequest request) {
        Student student = studentRepository.query().eq(Student::getUsername, request.getUsername()).one();
        if (student == null || !passwordEncoder.matches(request.getPassword(), student.getPassword())) {
            throw new BusinessException(QuizErrorCode.INVALID_CREDENTIALS);
        }
        return new StudentAuthResponse(jwtUtil.generate(student.getId(), Role.STUDENT), StudentResponse.from(student));
    }
}
