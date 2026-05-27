package com.hbcstore.hbcstore_api.user;

import com.hbcstore.hbcstore_api.user.dto.UserAdminResponse;
import com.hbcstore.hbcstore_api.user.dto.UserRequest;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UserAdminResponse> getAll() {
        return userRepository.findAll().stream()
                .map(UserAdminResponse::from)
                .toList();
    }

    public UserAdminResponse getById(Long id) {
        return UserAdminResponse.from(findUser(id));
    }

    @Transactional
    public UserAdminResponse create(UserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already exists");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }

        User user = new User();
        apply(user, request);
        user.setPassword(passwordEncoder.encode(request.password()));
        return UserAdminResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserAdminResponse update(Long id, UserRequest request) {
        User user = findUser(id);

        userRepository.findByEmail(request.email()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new IllegalArgumentException("Email already exists");
            }
        });

        apply(user, request);
        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }
        return UserAdminResponse.from(user);
    }

    @Transactional
    public void delete(Long id) {
        User user = findUser(id);
        user.setStatus(User.UserStatus.BANNED);
    }

    private void apply(User user, UserRequest request) {
        user.setEmail(request.email());
        user.setFullName(request.fullName());
        user.setPhoneNumber(request.phoneNumber());
        user.setAddress(request.address());
        user.setRole(request.role() == null ? User.Role.CUSTOMER : request.role());
        user.setStatus(request.status() == null ? User.UserStatus.ACTIVE : request.status());
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}
