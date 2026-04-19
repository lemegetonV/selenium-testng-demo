package com.demowebshop.models;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

/** Root shape of {@code testdata/users.json} — wraps the named user entries. */
@Data
@Builder
@Jacksonized
public class UsersFile {

    private final User defaultUser;
}
