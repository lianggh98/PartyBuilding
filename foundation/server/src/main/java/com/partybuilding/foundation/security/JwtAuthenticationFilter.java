package com.partybuilding.foundation.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.partybuilding.foundation.domain.PartyOrg;
import com.partybuilding.foundation.domain.SysUser;
import com.partybuilding.foundation.mapper.PartyOrgMapper;
import com.partybuilding.foundation.mapper.SysUserMapper;
import com.partybuilding.foundation.service.RoleBinding;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final SysUserMapper sysUserMapper;
    private final PartyOrgMapper partyOrgMapper;

    public JwtAuthenticationFilter(JwtService jwtService, SysUserMapper sysUserMapper, PartyOrgMapper partyOrgMapper) {
        this.jwtService = jwtService;
        this.sysUserMapper = sysUserMapper;
        this.partyOrgMapper = partyOrgMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            try {
                Long userId = jwtService.parseUserId(header.substring(7));
                SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getId, userId)
                        .eq(SysUser::getEnabled, 1));
                PartyOrg org = user == null || user.getOrgId() == null ? null : partyOrgMapper.selectById(user.getOrgId());
                if (user != null && RoleBinding.matches(user, org)) {
                    AuthUser principal = new AuthUser(
                            user.getId(),
                            user.getUsername(),
                            user.getDisplayName(),
                            user.getRoleCode(),
                            user.getOrgId());
                    var authentication = new UsernamePasswordAuthenticationToken(
                            principal, null, authorities(principal));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (RuntimeException ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    private List<SimpleGrantedAuthority> authorities(AuthUser user) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.role().name()));
        authorities.add(new SimpleGrantedAuthority("org:read"));
        authorities.add(new SimpleGrantedAuthority("member:read"));
        if (user.role().canWrite()) {
            authorities.add(new SimpleGrantedAuthority("org:write"));
            authorities.add(new SimpleGrantedAuthority("member:write"));
        }
        return authorities;
    }
}
