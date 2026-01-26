package com.rosanova.iot.timer.security;

import com.rosanova.iot.timer.error.FilterSecurityException;
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

    private final static String TOKEN_NAME = "TIMER_SESSION_TOKEN";
    private final static int TOKEN_DURATION = 300_000; // 5 minutes
    private static final int HASH_LENGTH_MAX = 64; //key length
    private static final int TOKEN_LENGTH = 57; // key + timestamp
    private static final String URL_PROTECTED_PATH = "/authenticated";
    private static final int TIMESTAP_LENGTH_PLUS_ONE = 13;

    private final HMACSHA256SignatureUtil hashingUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) {


        try {
            if (!request.getServletPath().startsWith(URL_PROTECTED_PATH)) {
                filterChain.doFilter(request, response);
                return;
            }

            Cookie[] cookies = request.getCookies();

            String token = null;

            if (cookies == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(TOKEN_NAME)) {
                    token = cookie.getValue();
                    break;
                }
            }

            if (!(token != null && token.length() == TOKEN_LENGTH)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            String hash = token.substring(TIMESTAP_LENGTH_PLUS_ONE);
            String time = token.substring(0, TIMESTAP_LENGTH_PLUS_ONE);

            String hashTime = hashingUtil.computeHMACSHA256(time);

            if (!compareString(hash, hashTime)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            long timeToCheck;

            try {
                timeToCheck = Long.parseLong(time);
            }catch (NumberFormatException nfe) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            if (getCurrentTimeMillis() - timeToCheck > TOKEN_DURATION) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            filterChain.doFilter(request, response);

        }catch (Exception e){
            System.err.println("ERRORE NON PREVISTO FILTRO AUTENTICAZIONE" + e.getMessage());
                throw new FilterSecurityException("ERRORE FILTRO AUTENTICAZIONE");
        }
    }

    public long getCurrentTimeMillis(){
        return System.currentTimeMillis();
    }

    public boolean compareString( String sentHash, String hash) {
        char[] hashChars = new char[HASH_LENGTH_MAX];
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
