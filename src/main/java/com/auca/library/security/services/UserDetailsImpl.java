package com.auca.library.security.services;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.auca.library.model.User;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class UserDetailsImpl implements UserDetails {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String fullName;
    private String email;
    private String identifier;
    private String location;
    private String userType;
    private boolean mustChangePassword;

    @JsonIgnore
    private String password;

    private boolean emailVerified;

    private Collection<? extends GrantedAuthority> authorities;

    public UserDetailsImpl(Long id, String fullName, String email, String identifier, String userType, String location,
                           String password, boolean emailVerified, boolean mustChangePassword,
                           Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.identifier = identifier;
        this.userType = userType;
        this.location = location;
        this.password = password;
        this.emailVerified = emailVerified;
        this.mustChangePassword = mustChangePassword;
        this.authorities = authorities;
    }

    public static UserDetailsImpl build(User user) {
        List<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName().name()))
                .collect(Collectors.toList());

        String userType = user.isStudent() ? "STUDENT" : "STAFF";
        String identifier = user.isStudent() ? user.getStudentId() : user.getEmployeeId();

        return new UserDetailsImpl(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                identifier,
                userType,
                user.getLocation().toString(),
                user.getPassword(),
                user.isEmailVerified(),
                user.isMustChangePassword(),
                authorities);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getLocation() {
        return location;
    }

    public String getUserType() {
        return userType;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email; // Spring Security uses this for authentication
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
        return emailVerified;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        UserDetailsImpl user = (UserDetailsImpl) o;
        return Objects.equals(id, user.id);
    }
}
