package com.example.filter;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.utils.Const;
import com.example.utils.JwtUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 用于对请求头中Jwt令牌进行校验的工具，为当前请求添加用户验证信息
 * 并将用户的ID存放在请求对象属性中，方便后续使用
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Resource
    JwtUtils utils;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        DecodedJWT jwt = utils.resolveJwt(authorization);
        if (jwt != null) {
            // 1. 从 JWT 中解析出用户详细信息
            UserDetails user = utils.toUser(jwt);
            // 2. 创建一个用户名和密码身份验证令牌，包含用户详细信息和权限
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
            // 3. 设置请求详细信息到身份验证令牌
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            // 4. 将身份验证信息设置到安全上下文中
            SecurityContextHolder.getContext().setAuthentication(authentication);
            // 5. 将用户ID存放到请求对象属性中，方便后续使用
            request.setAttribute(Const.ATTR_USER_ID, utils.toId(jwt));
        }

        filterChain.doFilter(request, response);
    }
}
