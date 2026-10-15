package com.partybuilding.foundation.domain;

public enum RoleCode {
    SYSTEM_ADMIN,
    COMMITTEE_ADMIN,
    GENERAL_ADMIN,
    BRANCH_ADMIN,
    BRANCH_VIEWER;

    public boolean canWrite() {
        return this != BRANCH_VIEWER;
    }

    public boolean isSystemAdmin() {
        return this == SYSTEM_ADMIN;
    }
}
