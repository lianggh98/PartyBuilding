package com.partybuilding.foundation.web.dto;

public record LoginResponse(String token, UserProfile user) {
}
