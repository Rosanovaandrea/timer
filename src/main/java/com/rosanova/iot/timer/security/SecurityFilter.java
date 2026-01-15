package com.rosanova.iot.timer.security;

import com.rosanova.iot.timer.utils.HMACSHA256SignatureUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

//@Component
@RequiredArgsConstructor
public class SecurityFilter extends OncePerRequestFilter {

    private final HMACSHA256SignatureUtil hashingUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

          if(!request.getServletPath().startsWith("/autheticated")) {
              filterChain.doFilter(request, response);
              return;
          }

           Cookie[] cookies = request.getCookies();

           String token = null;

           if (cookies == null) {response.sendError(HttpServletResponse.SC_UNAUTHORIZED); return;}

           for(Cookie cookie : cookies) {
               if(cookie.getName().equals("JSESSIONID")) {
                  token = cookie.getValue();
                  break;
               }
           }

           if ( !(token != null  && !token.isEmpty() && token.length() != 77)) { response.sendError(HttpServletResponse.SC_UNAUTHORIZED); return;}

           String hash = token.substring(14);
           String time = token.substring(0,14);

           String hashTime = hashingUtil.computeHMACSHA256(time);

           if (!compareString(hash, hashTime)) { response.sendError(HttpServletResponse.SC_UNAUTHORIZED); }

           long timeToCheck = Long.parseLong(time);

           if(System.currentTimeMillis()-timeToCheck > 300_000) { response.sendError(HttpServletResponse.SC_FORBIDDEN); return;}

           filterChain.doFilter(request, response);
    }

    public boolean compareString( String sentHash, String hash) {
        char[] hashChars = new char[64];
        int diff = 0;

        for (int i = 0; i < sentHash.length(); i++) {
            hashChars[i] = sentHash.charAt(i);
        }

        for (int i = 0; i < hash.length(); i++) {
            diff |= (hashChars[i] ^ hash.charAt(i));
        }

        return diff == 0;
    }
}
