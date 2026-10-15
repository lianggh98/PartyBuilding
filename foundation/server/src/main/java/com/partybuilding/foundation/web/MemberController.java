package com.partybuilding.foundation.web;

import com.partybuilding.foundation.common.ApiResponse;
import com.partybuilding.foundation.service.MemberService;
import com.partybuilding.foundation.web.dto.MemberRequest;
import com.partybuilding.foundation.web.dto.MemberResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('member:read')")
    public ApiResponse<List<MemberResponse>> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long orgId) {
        return ApiResponse.ok(memberService.list(name, orgId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('member:read')")
    public ApiResponse<MemberResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(memberService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('member:write')")
    public ApiResponse<MemberResponse> create(@Valid @RequestBody MemberRequest request) {
        return ApiResponse.ok(memberService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('member:write')")
    public ApiResponse<MemberResponse> update(@PathVariable Long id, @Valid @RequestBody MemberRequest request) {
        return ApiResponse.ok(memberService.update(id, request));
    }
}
