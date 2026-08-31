package vn.org.thn.service.app.quiz.security;

/** The two account types in quiz-service, carried in the JWT's "role" claim and used to gate {@code /api/parent/**} vs {@code /api/student/**}. */
public enum Role {
    PARENT,
    STUDENT
}
