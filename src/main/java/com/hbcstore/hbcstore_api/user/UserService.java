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

    public List<UserAdminResponse> getAll(String principalEmail) {
        requireAdmin(principalEmail);
        return userRepository.findAll().stream()
                .map(UserAdminResponse::from)
                .toList();
    }

    public UserAdminResponse getById(Long id, String principalEmail) {
        User actor = requireActor(principalEmail);
        User target = findUser(id);
        if (actor.getRole() != User.Role.ADMIN && !actor.getId().equals(target.getId())) {
            throw new SecurityException("Bạn không có quyền xem người dùng này");
        }
        return UserAdminResponse.from(target);
    }

    @Transactional
    public UserAdminResponse create(UserRequest request, String principalEmail) {
        requireAdmin(principalEmail);
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email đã tồn tại");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new IllegalArgumentException("Mật khẩu là bắt buộc");
        }

        User user = new User();
        apply(user, request, true);
        user.setPassword(passwordEncoder.encode(request.password()));
        return UserAdminResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserAdminResponse update(Long id, UserRequest request, String principalEmail) {
        User actor = requireActor(principalEmail);
        User user = findUser(id);

        boolean isAdmin = actor.getRole() == User.Role.ADMIN;
        if (!isAdmin && !actor.getId().equals(user.getId())) {
            throw new SecurityException("Bạn không có quyền cập nhật người dùng này");
        }

        userRepository.findByEmail(request.email()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new IllegalArgumentException("Email đã tồn tại");
            }
        });

        apply(user, request, isAdmin);
        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }
        return UserAdminResponse.from(user);
    }

    @Transactional
    public void delete(Long id, String principalEmail) {
        requireAdmin(principalEmail);
        User user = findUser(id);
        user.setStatus(User.UserStatus.BANNED);
    }

    private void apply(User user, UserRequest request, boolean canManageRole) {
        user.setEmail(request.email());
        user.setFullName(request.fullName());
        user.setPhoneNumber(request.phoneNumber());
        user.setAddress(request.address());
        if (canManageRole) {
            user.setRole(request.role() == null ? User.Role.CUSTOMER : request.role());
            user.setStatus(request.status() == null ? User.UserStatus.ACTIVE : request.status());
        }
    }

    private User requireActor(String principalEmail) {
        if (principalEmail == null || principalEmail.isBlank()) {
            throw new SecurityException("Bạn chưa đăng nhập");
        }
        return userRepository.findByEmailIgnoreCase(principalEmail.trim())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
    }

    private void requireAdmin(String principalEmail) {
        User actor = requireActor(principalEmail);
        if (actor.getRole() != User.Role.ADMIN) {
            throw new SecurityException("Bạn không có quyền thực hiện thao tác này");
        }
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
    }
}
