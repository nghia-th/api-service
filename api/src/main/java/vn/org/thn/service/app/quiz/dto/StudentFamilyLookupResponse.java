package vn.org.thn.service.app.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * Response body for {@code POST /api/auth/student/family}. {@code students} is an empty list -
 * NEVER an error - both when {@code parentIdentifier} matches no Parent at all and when it
 * matches a Parent with zero Students, on purpose: see {@code AuthService#lookupStudentFamily}'s
 * javadoc for why this class deliberately cannot distinguish the two (same "one shared outcome,
 * never reveal which" philosophy as {@code QuizErrorCode#INVALID_CREDENTIALS}).
 */
@Data
@AllArgsConstructor
public class StudentFamilyLookupResponse {
    private List<StudentFamilyMember> students;
}
