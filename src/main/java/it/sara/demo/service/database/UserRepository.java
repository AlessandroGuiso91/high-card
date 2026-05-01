package it.sara.demo.service.database;

import it.sara.demo.service.database.model.User;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Persistence abstraction over the in-memory {@code FakeDatabase}. Encapsulates
 * the read/write API the service layer relies on; the static list is an
 * implementation detail that can be replaced with a real datastore without
 * touching callers.
 */
@Component
public class UserRepository {

    public boolean save(User user) {
        user.setGuid(java.util.UUID.randomUUID().toString());
        FakeDatabase.TABLE_USER.add(user);
        return true;
    }

    public List<User> getAll() {
        return FakeDatabase.TABLE_USER;
    }

    /**
     * Returns users whose first name, last name or email contains {@code query}
     * (case-insensitive). When {@code query} is {@code null} or blank, returns all users.
     */
    public List<User> findMatching(String query) {

        if (query == null || query.isBlank()) {
            return getAll();
        }

        String q = query.toLowerCase();

        return FakeDatabase.TABLE_USER.stream()
                .filter(u -> matches(u, q))
                .toList();
    }

    private boolean matches(User user, String lowercaseQuery) {
        return containsIgnoreCase(user.getFirstName(), lowercaseQuery)
                || containsIgnoreCase(user.getLastName(),  lowercaseQuery)
                || containsIgnoreCase(user.getEmail(),     lowercaseQuery);
    }

    private boolean containsIgnoreCase(String field, String lowercaseQuery) {
        return field != null && field.toLowerCase().contains(lowercaseQuery);
    }
}
