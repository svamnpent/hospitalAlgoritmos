package com.hospital.config;

import com.hospital.entidades.Empleado;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class EmpleadoDetails implements UserDetails {

    private final Empleado empleado;
    private final List<GrantedAuthority> authorities;

    public EmpleadoDetails(Empleado empleado, List<GrantedAuthority> authorities) {
        this.empleado = empleado;
        this.authorities = authorities;
    }

    public Empleado getEmpleado() { return empleado; }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String getPassword() { return empleado.getPassword(); }
    @Override public String getUsername() { return empleado.getUsername(); }
    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return true; }
}
