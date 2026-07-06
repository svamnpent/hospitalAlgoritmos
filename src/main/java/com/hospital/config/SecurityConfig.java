package com.hospital.config;

import com.hospital.dao.UsuarioDAO;
import com.hospital.entidades.Empleado;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private UsuarioDAO usuarioDAO;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/css/**", "/login").permitAll()
                        .requestMatchers("/recepcionista/**").hasRole("RECEPCIONISTA")
                        .requestMatchers("/medico/**").hasRole("MEDICO")
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/contador/**").hasRole("CONTADOR")
                        .requestMatchers("/rrhh/**").hasRole("RRHH")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .successHandler((req, res, auth) -> {
                            String role = auth.getAuthorities().iterator().next().getAuthority();
                            switch (role) {
                                case "ROLE_RECEPCIONISTA" -> res.sendRedirect("/recepcionista");
                                case "ROLE_MEDICO"        -> res.sendRedirect("/medico");
                                case "ROLE_ADMIN"         -> res.sendRedirect("/admin");
                                case "ROLE_CONTADOR"      -> res.sendRedirect("/contador");  // 👈 AGREGAR
                                case "ROLE_RRHH"          -> res.sendRedirect("/rrhh");      // 👈 AGREGAR
                                default                   -> res.sendRedirect("/login?error");
                            }
                        })
                        .failureUrl("/login?error")
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login")
                );

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            // Busca en BD igual que el original UsuarioDAO
            Empleado emp = usuarioDAO.obtenerPersonalPorCredenciales(username, "");
            // La validación real de password la hace Spring Security con el PasswordEncoder
            // Necesitamos cargar por username; ver nota en UsuarioDAO
            Empleado empReal = usuarioDAO.obtenerPersonalPorUsername(username);
            if (empReal == null) throw new UsernameNotFoundException("Usuario no encontrado");

            String roleName = switch (empReal.getIdRol()) {
                case 1 -> "ROLE_ADMIN";
                case 2 -> "ROLE_RECEPCIONISTA";
                case 3 -> "ROLE_MEDICO";
                case 4 -> "ROLE_CONTADOR";
                case 5 -> "ROLE_RRHH";
                default -> "ROLE_USER";
            };

            // Guardamos el empleado en el principal para usarlo en controllers
            return new EmpleadoDetails(empReal, List.of(new SimpleGrantedAuthority(roleName)));
        };
    }

    @Bean
    @SuppressWarnings("deprecation")
    public PasswordEncoder passwordEncoder() {
        // El proyecto original guarda passwords en texto plano
        return NoOpPasswordEncoder.getInstance();
    }
}
