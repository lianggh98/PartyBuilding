package com.partybuilding.foundation.web.dto;

import jakarta.validation.constraints.NotBlank;

public record RenameRequest(@NotBlank(message = "请填写组织名称") String name) {
}
