package it.sara.demo.service.database;

import it.sara.demo.service.database.model.User;

import java.util.ArrayList;
import java.util.List;

/**
 * In-memory user store. Empty at startup: data is populated exclusively through
 * the validated boundary ({@code PUT /user/v1/user}). The previous static seeder
 * has been removed because it inserted records that violated the validation rules
 * enforced at the web layer (e.g. {@code "+39" + i} as phone, digits in names),
 * bypassing the boundary entirely and producing data that no real client could submit.
 */
public class FakeDatabase {

    public static final List<User> TABLE_USER = new ArrayList<>();

    private FakeDatabase() {
    }
}
