package com.partybuilding.foundation.web.dto;

import com.partybuilding.foundation.domain.OrgType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OrgRequest(
        Long parentId,
        @NotBlank(message = "请填写组织名称") String name,
        @NotNull(message = "请选择组织类型") OrgType orgType,
        Integer sortNo
) {
}
