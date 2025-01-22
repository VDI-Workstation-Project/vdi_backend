package com.hmws.global.authentication;

import com.hmws.global.authentication.dto.AuthUserDto;
import com.hmws.usermgmt.constant.UserRole;
import com.hmws.usermgmt.constant.UserType;
import com.hmws.usermgmt.domain.UserData;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class UserDetailsImpl implements UserDetails {

    private final AuthUserDto authUser;

    public UserDetailsImpl(AuthUserDto authUser) {
        this.authUser = authUser;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        // UserType 기반 권한
        authorities.add(new SimpleGrantedAuthority(authUser.getUserType().getAuthority()));
        // UserRole 기반 권한
        authorities.add(new SimpleGrantedAuthority("ROLE_" + authUser.getUserRole().name()));
        return authorities;
    }

    public UserRole getUserRole() {
        return authUser.getUserRole();
    }

    public UserType getUserType() {
        return authUser.getUserType();
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return authUser.getUsername();
    }

    public AuthUserDto getAuthUser() {
        return authUser;
    }
}
