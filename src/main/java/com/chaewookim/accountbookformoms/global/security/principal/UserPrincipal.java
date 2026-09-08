package com.chaewookim.accountbookformoms.global.security.principal;

import com.chaewookim.accountbookformoms.domain.user.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

@Getter
public class UserPrincipal implements UserDetails, OAuth2User {

    private final User user;
    private Map<String, Object> attributes;
    private final boolean newSocialSignup;

    private UserPrincipal(User user, Map<String, Object> attributes, boolean newSocialSignup) {
        this.user = user;
        this.attributes = attributes;
        this.newSocialSignup = newSocialSignup;
    }

    public static UserPrincipal create(User user) {
        return new UserPrincipal(user, null, false);
    }

    public static UserPrincipal create(User user, Map<String, Object> attributes) {
        return new UserPrincipal(user, attributes, false);
    }

    public static UserPrincipal create(User user, Map<String, Object> attributes, boolean newSocialSignup) {
        return new UserPrincipal(user, attributes, newSocialSignup);
    }

    public Long getUserId() {
        return user.getId();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority(user.getRole().name()));
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public String getName() {
        return String.valueOf(user.getId());
    }

    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
