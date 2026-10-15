package com.partybuilding.foundation.web;

import com.partybuilding.foundation.common.ApiResponse;
import com.partybuilding.foundation.service.OrgService;
import com.partybuilding.foundation.web.dto.OrgNode;
import com.partybuilding.foundation.web.dto.OrgRequest;
import com.partybuilding.foundation.web.dto.RenameRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orgs")
public class OrgController {

    private final OrgService orgService;

    public OrgController(OrgService orgService) {
        this.orgService = orgService;
    }

    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('org:read')")
    public ApiResponse<List<OrgNode>> tree() {
        return ApiResponse.ok(orgService.tree());
    }

    @GetMapping
    @PreAuthorize("hasAuthority('org:read')")
    public ApiResponse<List<OrgNode>> flat() {
        return ApiResponse.ok(orgService.flat());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('org:write')")
    public ApiResponse<OrgNode> create(@Valid @RequestBody OrgRequest request) {
        return ApiResponse.ok(orgService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('org:write')")
    public ApiResponse<OrgNode> rename(@PathVariable Long id, @Valid @RequestBody RenameRequest request) {
        return ApiResponse.ok(orgService.rename(id, request.name()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('org:write')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        orgService.delete(id);
        return ApiResponse.ok(null);
    }
}
