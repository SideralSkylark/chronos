package com.timetable.timetable.common.logging;

import com.timetable.timetable.security.UserPrincipal;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class LoggingContextFilter implements Filter {

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse res = (HttpServletResponse) response;

    String correlationId = req.getHeader("X-Correlation-Id");
    if (correlationId == null || correlationId.isBlank()) {
      correlationId = UUID.randomUUID().toString();
    }

    MDC.put("correlationId", correlationId);
    res.setHeader("X-Correlation-Id", correlationId);

    MDC.put("ip", req.getRemoteAddr());

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
      Object principal = auth.getPrincipal();

      if (principal instanceof UserPrincipal userPrincipal) {
        MDC.put("userId", String.valueOf(userPrincipal.getId()));
        MDC.put("userAgent", req.getHeader("User-Agent"));
      } else {
        MDC.put("userId", auth.getName());
      }
    }

    try {
      chain.doFilter(request, response);
    } finally {
      MDC.clear();
    }
  }
}
