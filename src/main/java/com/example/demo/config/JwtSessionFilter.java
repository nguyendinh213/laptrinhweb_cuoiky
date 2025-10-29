package com.example.demo.config;

import com.example.demo.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtSessionFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        // Check if user is already authenticated
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            
            // Try to get JWT token from session
            String token = (String) request.getSession().getAttribute("jwt_token");
            
            if (token != null) {
                try {
                    String username = jwtUtil.extractUsername(token);
                    String role = jwtUtil.extractRole(token);
                    
                    if (jwtUtil.validateToken(token, username)) {
                        // Set authentication in SecurityContext
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                username, null, Collections.singletonList(new SimpleGrantedAuthority(role)));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    } else {
                        // Token is invalid, remove from session
                        request.getSession().removeAttribute("jwt_token");
                        request.getSession().removeAttribute("username");
                        request.getSession().removeAttribute("role");
                    }
                } catch (Exception e) {
                    // Token is invalid, remove from session
                    request.getSession().removeAttribute("jwt_token");
                    request.getSession().removeAttribute("username");
                    request.getSession().removeAttribute("role");
                }
            }
        }
        
        chain.doFilter(request, response);
    }
}
