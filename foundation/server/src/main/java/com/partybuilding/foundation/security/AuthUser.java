package com.partybuilding.foundation.security;

import com.partybuilding.foundation.domain.RoleCode;

public record AuthUser(Long id, String username, String displayName, RoleCode role, Long orgId) {
}
