package com.ecommerce.userservice.service;

import com.ecommerce.userservice.dto.UserRegistrationDto;
import com.ecommerce.userservice.dto.UserResponseDto;
import com.ecommerce.userservice.entity.User;
import com.ecommerce.userservice.exception.UserAlreadyExistsException;
import com.ecommerce.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private UserRegistrationDto registration;

    @BeforeEach
    void setUp() {
        registration = new UserRegistrationDto("a@b.com", "password123", "A", "B");
    }

    private User existingUser() {
        User user = new User();
        user.setId(1L);
        user.setEmail("a@b.com");
        user.setPassword("hashed");
        user.setFirstName("A");
        user.setLastName("B");
        return user;
    }

    @Test
    void registrationStoresTheHashNotTheRawPassword() {
        when(userRepository.existsByEmail("a@b.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.registerUser(registration);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertEquals("hashed", saved.getValue().getPassword());
        assertNotEquals("password123", saved.getValue().getPassword());
    }

    @Test
    void registrationRejectsDuplicateEmail() {
        when(userRepository.existsByEmail("a@b.com")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> userService.registerUser(registration));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registrationResponseNeverExposesThePassword() {
        when(userRepository.existsByEmail("a@b.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User user = inv.getArgument(0);
            user.setId(1L);
            return user;
        });

        UserResponseDto response = userService.registerUser(registration);

        assertEquals("a@b.com", response.getEmail());
        assertEquals(1L, response.getId());
    }

    @Test
    void loadUserByUsernameRejectsUnknownEmail() {
        when(userRepository.findByEmail("gone@b.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> userService.loadUserByUsername("gone@b.com"));
    }

    @Test
    void loadUserByUsernameReturnsTheEntityItself() {
        User user = existingUser();
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));

        assertEquals(user, userService.loadUserByUsername("a@b.com"));
    }

    @Test
    void updateLeavesOmittedFieldsUntouched() {
        User user = existingUser();
        user.setCity("Chicago");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserRegistrationDto patch = new UserRegistrationDto();
        patch.setFirstName("Changed");

        UserResponseDto updated = userService.updateUser(1L, patch);

        assertEquals("Changed", updated.getFirstName());
        assertEquals("Chicago", updated.getCity());
        assertEquals("B", updated.getLastName());
    }

    @Test
    void updateDoesNotChangeEmailOrPassword() {
        User user = existingUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserRegistrationDto patch = new UserRegistrationDto("attacker@b.com", "newpassword", null, null);
        userService.updateUser(1L, patch);

        assertEquals("a@b.com", user.getEmail());
        assertEquals("hashed", user.getPassword());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void updateRejectsUnknownId() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> userService.updateUser(99L, registration));
    }

    @Test
    void deleteRejectsUnknownId() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> userService.deleteUser(99L));
        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    void getByEmailRejectsUnknownEmail() {
        when(userRepository.findByEmail("gone@b.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> userService.getUserByEmail("gone@b.com"));
    }
}
