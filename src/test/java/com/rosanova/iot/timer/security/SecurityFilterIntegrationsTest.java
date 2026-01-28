package com.rosanova.iot.timer.security;

import com.rosanova.iot.timer.utils.impl.HMACSHA256SignatureUtilImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;

import static org.mockito.Mockito.*;

class SecurityFilterIntegrationsTest {

    private SecurityFilter securityFilter;
    private HMACSHA256SignatureUtilImpl hashingUtil;

    // Una chiave di 64 caratteri come richiesto dal tuo costruttore
    private final String secretKey = "1234567890123456789012345678901234567890123456789012345678901234";

    @BeforeEach
    void setUp() {
        // Usiamo l'implementazione reale invece di un mock per l'integrazione
        hashingUtil = Mockito.spy(new HMACSHA256SignatureUtilImpl(secretKey));
        hashingUtil.computeSecretKey(); // Inizializza ipad e opad

        securityFilter = new SecurityFilter(hashingUtil);
    }

    @Test
    void testDoFilterInternal_Success() throws ServletException, IOException {
        // 1. Setup dei Mock
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        // 2. Simulazione dati validi
        String timestamp = String.valueOf(System.currentTimeMillis());
        // Assicuriamoci che il timestamp sia lungo 13 caratteri per matchare TIMESTAP_LENGTH_PLUS_ONE
        // Nota: se System.currentTimeMillis() fosse più corto, andrebbe fatto il padding

        String hash = hashingUtil.computeHMACSHA256(timestamp);
        String validToken = timestamp + hash;

        when(request.getRequestURI()).thenReturn("/authenticated/dashboard");
        Cookie sessionCookie = new Cookie("TIMER_SESSION_TOKEN", validToken);
        when(request.getCookies()).thenReturn(new Cookie[]{sessionCookie});

        // 3. Esecuzione
        securityFilter.doFilter(request, response, filterChain);

        // 4. Verifica: il filtro deve chiamare filterChain.doFilter e NON dare errore
        verify(filterChain, times(1)).doFilter(request, response);
        verify(response, never()).sendError(anyInt());
    }

    @Test
    void testDoFilterInternal_ExpiredToken() throws ServletException, IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        // Creiamo un timestamp vecchio di 10 minuti (limite è 5)
        long oldTime = System.currentTimeMillis() - (600_000);
        String timestamp = String.valueOf(oldTime);
        String hash = hashingUtil.computeHMACSHA256(timestamp);
        String expiredToken = timestamp + hash;

        when(request.getRequestURI()).thenReturn("/authenticated/test");
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("TIMER_SESSION_TOKEN", expiredToken)});

        securityFilter.doFilter(request, response, filterChain);

        // Verifica: deve ritornare 403 Forbidden
        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void testDoFilterInternal_InvalidHash() throws ServletException, IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        String timestamp = String.valueOf(System.currentTimeMillis());
        String fakeHash = "A".repeat(44); // Un hash falso della lunghezza corretta
        String invalidToken = timestamp + fakeHash;

        when(request.getRequestURI()).thenReturn("/authenticated/test");
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("TIMER_SESSION_TOKEN", invalidToken)});

        securityFilter.doFilter(request, response, filterChain);

        // Verifica: deve ritornare 401 Unauthorized per hash non corrispondente
        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    void testDoFilterInternal_PublicPath_ShouldIgnoreFilter() throws ServletException, IOException {
        // Setup
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        // Path che non inizia con /authenticated
        when(request.getRequestURI()).thenReturn("/api/public/status");

        // Esecuzione
        securityFilter.doFilter(request, response, filterChain);

        // Verifica: il filtro passa la mano senza guardare i cookie
        verify(filterChain).doFilter(request, response);
        verify(hashingUtil,Mockito.never()).computeHMACSHA256(Mockito.any()); // Verifica che non venga calcolato nessun hash
    }

    @Test
    void testDoFilterInternal_NoCookies_ShouldReturnUnauthorized() throws ServletException, IOException {
        // Setup
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        when(request.getRequestURI()).thenReturn("/authenticated/admin");
        when(request.getCookies()).thenReturn(null); // Nessun cookie presente

        // Esecuzione
        securityFilter.doFilter(request, response, filterChain);

        // Verifica
        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void testDoFilterInternal_WrongCookieName_ShouldReturnUnauthorized() throws ServletException, IOException {
        // Setup
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        when(request.getRequestURI()).thenReturn("/authenticated/settings");
        // Cookie presente ma con nome sbagliato
        Cookie wrongCookie = new Cookie("WRONG_TOKEN_NAME", "some-value");
        when(request.getCookies()).thenReturn(new Cookie[]{wrongCookie});

        // Esecuzione
        securityFilter.doFilter(request, response, filterChain);

        // Verifica
        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    void testDoFilterInternal_MalformedTimestamp_ShouldReturnUnauthorized() throws ServletException, IOException {
        // Setup
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        // Creiamo un token dove i primi 13 caratteri non sono numerici
        String malformedTimestamp = "ABCDEFGHIJKLM";
        String hash = hashingUtil.computeHMACSHA256("something");
        String fullToken = malformedTimestamp + hash;

        when(request.getRequestURI()).thenReturn("/authenticated/profile");
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("TIMER_SESSION_TOKEN", fullToken)});

        // Esecuzione
        securityFilter.doFilter(request, response, filterChain);

        // Verifica: il catch del NumberFormatException deve gestire l'errore
        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED);
    }
}
