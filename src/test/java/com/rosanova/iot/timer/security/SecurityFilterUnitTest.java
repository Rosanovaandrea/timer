package com.rosanova.iot.timer.security;

import com.rosanova.iot.timer.utils.HMACSHA256SignatureUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

@ExtendWith(MockitoExtension.class)
class SecurityFilterUnitTest {

    @Mock
    FilterChain filterChain;

    @Mock
    HttpServletResponse response;

    @Mock
    HttpServletRequest request;

    @Mock
    HMACSHA256SignatureUtil hashingUtils;


    SecurityFilter securityFilter;

    @BeforeEach
    void init (){
        securityFilter = Mockito.spy(new SecurityFilter(hashingUtils));
    }

    @Test
    void securityFilterAllRight() throws ServletException, IOException {
        String authenticatedPath= "/authenticated";
        String authenticationToken = "1737719000000ABCDE1234567890ABCDE1234567890ABCDE123456789";

        String cookieName = "TIMER_SESSION_TOKEN";
        Mockito.doReturn(authenticatedPath).when(request).getRequestURI();
        Cookie authentication = new Cookie(cookieName,authenticationToken);
        Cookie[] cookiesArray = {authentication};
        Mockito.doReturn(cookiesArray).when(request).getCookies();
        Mockito.doReturn("ABCDE1234567890ABCDE1234567890ABCDE123456789").when(hashingUtils).computeHMACSHA256("1737719000000");
        Mockito.doReturn(1737719000001L).when(securityFilter).getCurrentTimeMillis();

        securityFilter.doFilterInternal(request,response,filterChain);


        Mockito.verify(securityFilter).getCurrentTimeMillis();
        Mockito.verify(securityFilter).compareString(Mockito.any(),Mockito.any());
        Mockito.verify(request).getCookies();
        Mockito.verify(request).getRequestURI();
        Mockito.verify(hashingUtils).computeHMACSHA256(Mockito.any());
        Mockito.verify(response,Mockito.never()).sendError(Mockito.anyInt());
        Mockito.verify(filterChain).doFilter(request,response);
    }

    @Test
    void securityFilterNoProtectedPath() throws ServletException, IOException {
        String authenticatedPath= "/public";

        Mockito.doReturn(authenticatedPath).when(request).getRequestURI();


        securityFilter.doFilterInternal(request,response,filterChain);


        Mockito.verify(securityFilter,Mockito.never()).getCurrentTimeMillis();
        Mockito.verify(securityFilter,Mockito.never()).compareString(Mockito.any(),Mockito.any());
        Mockito.verify(request,Mockito.never()).getCookies();
        Mockito.verify(request).getRequestURI();
        Mockito.verify(hashingUtils,Mockito.never()).computeHMACSHA256(Mockito.any());
        Mockito.verify(response,Mockito.never()).sendError(Mockito.anyInt());
        Mockito.verify(filterChain).doFilter(request,response);
    }

    @Test
    void securityFilterErrorNoCookies() throws ServletException, IOException {
        String authenticatedPath= "/authenticated";
        Mockito.doReturn(authenticatedPath).when(request).getRequestURI();
        Mockito.doReturn(null).when(request).getCookies();

        securityFilter.doFilterInternal(request,response,filterChain);


        Mockito.verify(securityFilter,Mockito.never()).getCurrentTimeMillis();
        Mockito.verify(securityFilter,Mockito.never()).compareString(Mockito.any(),Mockito.any());
        Mockito.verify(request).getCookies();
        Mockito.verify(request).getRequestURI();
        Mockito.verify(hashingUtils,Mockito.never()).computeHMACSHA256(Mockito.any());
        Mockito.verify(response).sendError(Mockito.anyInt());
        Mockito.verify(filterChain,Mockito.never()).doFilter(request,response);
    }

    @Test
    void securityFilterErrorNoToken() throws ServletException, IOException {
        String authenticatedPath= "/authenticated";
        Mockito.doReturn(authenticatedPath).when(request).getRequestURI();
        Mockito.doReturn(new Cookie[0]).when(request).getCookies();

        securityFilter.doFilterInternal(request,response,filterChain);


        Mockito.verify(securityFilter,Mockito.never()).getCurrentTimeMillis();
        Mockito.verify(securityFilter,Mockito.never()).compareString(Mockito.any(),Mockito.any());
        Mockito.verify(request).getCookies();
        Mockito.verify(request).getRequestURI();
        Mockito.verify(hashingUtils,Mockito.never()).computeHMACSHA256(Mockito.any());
        Mockito.verify(response).sendError(Mockito.anyInt());
        Mockito.verify(filterChain,Mockito.never()).doFilter(request,response);
    }

    @Test
    void securityFilterErrorTokenTooSHort() throws ServletException, IOException {
        String authenticatedPath= "/authenticated";
        String authenticationToken = "1737719000000ABCDE1";
        String cookieName = "TIMER_SESSION_TOKEN";
        Mockito.doReturn(authenticatedPath).when(request).getRequestURI();
        Cookie authentication = new Cookie(cookieName,authenticationToken);
        Cookie[] cookiesArray = {authentication};
        Mockito.doReturn(cookiesArray).when(request).getCookies();

        securityFilter.doFilterInternal(request,response,filterChain);


        Mockito.verify(securityFilter,Mockito.never()).getCurrentTimeMillis();
        Mockito.verify(securityFilter,Mockito.never()).compareString(Mockito.any(),Mockito.any());
        Mockito.verify(request).getCookies();
        Mockito.verify(request).getRequestURI();
        Mockito.verify(hashingUtils,Mockito.never()).computeHMACSHA256(Mockito.any());
        Mockito.verify(response).sendError(Mockito.anyInt());
        Mockito.verify(filterChain,Mockito.never()).doFilter(request,response);
    }

    @Test
    void securityFilterDifferentHash() throws ServletException, IOException {
        String authenticatedPath= "/authenticated";
        String authenticationToken = "1737719000000ABCDE1234567890ABCDE1234567890ABCDE123456789";

        String cookieName = "TIMER_SESSION_TOKEN";
        Mockito.doReturn(authenticatedPath).when(request).getRequestURI();
        Cookie authentication = new Cookie(cookieName,authenticationToken);
        Cookie[] cookiesArray = {authentication};
        Mockito.doReturn(cookiesArray).when(request).getCookies();
        Mockito.doReturn("ABCDE1234567890ABCDE1234567890ABCDE123456781").when(hashingUtils).computeHMACSHA256("1737719000000");


        securityFilter.doFilterInternal(request,response,filterChain);


        Mockito.verify(securityFilter,Mockito.never()).getCurrentTimeMillis();
        Mockito.verify(securityFilter).compareString(Mockito.any(),Mockito.any());
        Mockito.verify(request).getCookies();
        Mockito.verify(request).getRequestURI();
        Mockito.verify(hashingUtils).computeHMACSHA256(Mockito.any());
        Mockito.verify(response).sendError(Mockito.anyInt());
        Mockito.verify(filterChain,Mockito.never()).doFilter(request,response);
    }

    @Test
    void securityFilterNptValidTime() throws ServletException, IOException {
        String authenticatedPath= "/authenticated";
        String authenticationToken = "1737719000000ABCDE1234567890ABCDE1234567890ABCDE123456789";

        String cookieName = "TIMER_SESSION_TOKEN";
        Mockito.doReturn(authenticatedPath).when(request).getRequestURI();
        Cookie authentication = new Cookie(cookieName,authenticationToken);
        Cookie[] cookiesArray = {authentication};
        Mockito.doReturn(cookiesArray).when(request).getCookies();
        Mockito.doReturn("ABCDE1234567890ABCDE1234567890ABCDE123456789").when(hashingUtils).computeHMACSHA256("1737719000000");
        Mockito.doReturn(1937719000001L).when(securityFilter).getCurrentTimeMillis();

        securityFilter.doFilterInternal(request,response,filterChain);


        Mockito.verify(securityFilter).getCurrentTimeMillis();
        Mockito.verify(securityFilter).compareString(Mockito.any(),Mockito.any());
        Mockito.verify(request).getCookies();
        Mockito.verify(request).getRequestURI();
        Mockito.verify(hashingUtils).computeHMACSHA256(Mockito.any());
        Mockito.verify(response).sendError(Mockito.anyInt());
        Mockito.verify(filterChain,Mockito.never()).doFilter(request,response);
    }
    @Test
    void securityFilterErrorTokenFromFuture() throws ServletException, IOException {
        // 1. Setup dati: Timestamp nel "futuro" rispetto al server
        String futureTimestamp = "1737719000000";
        String validHash = "ABCDE1234567890ABCDE1234567890ABCDE123456789";
        String authenticationToken = futureTimestamp + validHash;
        String authenticatedPath = "/authenticated";
        String cookieName = "TIMER_SESSION_TOKEN";

        // 2. Mocking delle dipendenze
        Mockito.doReturn(authenticatedPath).when(request).getRequestURI();
        Cookie authentication = new Cookie(cookieName, authenticationToken);
        Mockito.doReturn(new Cookie[]{authentication}).when(request).getCookies();

        // L'hash deve essere corretto per superare il primo controllo
        Mockito.doReturn(validHash).when(hashingUtils).computeHMACSHA256(futureTimestamp);

        // Simuliamo che il server sia "indietro": il tempo attuale è minore del timestamp del token
        // Timestamp token: ...000, Tempo Server: ...999 (1 millisecondo nel passato)
        Mockito.doReturn(1737718999999L).when(securityFilter).getCurrentTimeMillis();

        // 3. Esecuzione
        securityFilter.doFilterInternal(request, response, filterChain);

        // 4. Verifiche
        // Deve aver verificato il tempo
        Mockito.verify(securityFilter).getCurrentTimeMillis();
        // Deve aver dato errore (403 Forbidden) perché expiration < 0
        Mockito.verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
        // Non deve aver mai proseguito nella catena
        Mockito.verify(filterChain, Mockito.never()).doFilter(request, response);
    }
}