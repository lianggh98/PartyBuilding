package com.partybuilding.foundation.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record MemberRequest(
        @NotBlank(message = "请填写姓名") String name,
        String gender,
        String phone,
        String memberNo,
        @NotNull(message = "请选择党支部") Long orgId,
        String status,
        LocalDate joinedAt
) {
}
