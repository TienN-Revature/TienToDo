package com.revature.TienToDo.utility;
import com.revature.TienToDo.dto.ApiError;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
//import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler{
    private static final Logger logger = LoggerFactory.getLogger(JwtAccessDeniedHandler.class);

    //@Autowired
    //private ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {

        logger.warn("Access denied for {} {} — {}",
                request.getMethod(),
                request.getRequestURI(),
                accessDeniedException.getMessage());

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"status\":403,\"message\":\"Access denied. You do not have permission to access this resource.\"}"
        );

    }
}
