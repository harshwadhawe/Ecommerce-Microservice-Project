package com.ecommerce.userservice.controller;

import com.ecommerce.userservice.dto.UserResponseDto;
import com.ecommerce.userservice.entity.User;
import com.ecommerce.userservice.exception.UserAlreadyExistsException;
import com.ecommerce.userservice.security.JwtAuthenticationEntryPoint;
import com.ecommerce.userservice.security.JwtAuthenticationFilter;
import com.ecommerce.userservice.security.JwtUtil;
import com.ecommerce.userservice.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    private static final String REGISTRATION = """
            {"email":"a@b.com","password":"password123","firstName":"A","lastName":"B"}""";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    private UserResponseDto userResponse() {
        User user = new User();
        user.setId(1L);
        user.setEmail("a@b.com");
        user.setFirstName("A");
        user.setLastName("B");
        return new UserResponseDto(user);
    }

    @Test
    void registrationReturnsCreated() throws Exception {
        when(userService.registerUser(any())).thenReturn(userResponse());

        mockMvc.perform(post("/api/users/register").contentType(MediaType.APPLICATION_JSON).content(REGISTRATION))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("a@b.com"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void duplicateEmailMapsToConflict() throws Exception {
        when(userService.registerUser(any())).thenThrow(new UserAlreadyExistsException("User already exists with email: a@b.com"));

        mockMvc.perform(post("/api/users/register").contentType(MediaType.APPLICATION_JSON).content(REGISTRATION))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("User already exists with email: a@b.com"));
    }

    @Test
    void registrationRejectsMalformedEmail() throws Exception {
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTRATION.replace("a@b.com", "not-an-email")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").exists());
    }

    @Test
    void registrationRejectsShortPassword() throws Exception {
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTRATION.replace("password123", "12345")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.password").exists());
    }

    @Test
    void loginReturnsTokenAndUser() throws Exception {
        org.springframework.security.core.userdetails.User principal =
                new org.springframework.security.core.userdetails.User("a@b.com", "hashed", Collections.emptyList());
        when(authenticationManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList()));
        when(jwtUtil.generateToken(any())).thenReturn("a.b.c");
        when(userService.getUserByEmail("a@b.com")).thenReturn(userResponse());

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@b.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("a.b.c"))
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("a@b.com"));
    }

    @Test
    void badCredentialsDoNotLeakWhetherTheAccountExists() throws Exception {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@b.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("An unexpected error occurred"));
    }

    @Test
    void unknownUserMapsToNotFound() throws Exception {
        when(userService.getUserById(99L)).thenThrow(new UsernameNotFoundException("User not found with id: 99"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/users/99"))
                .andExpect(status().isNotFound());
    }
}
