package com.handa.tms.identity.web;

import com.handa.tms.identity.domain.AppUser;
import com.handa.tms.identity.domain.AppUserRepository;
import com.handa.tms.identity.auth.PasswordHasher;
import com.handa.tms.platform.core.error.ConflictException;
import com.handa.tms.platform.core.error.NotFoundException;
import com.handa.tms.platform.security.rbac.PermissionEvaluator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final AppUserRepository users;
    private final PasswordHasher hasher;
    private final PermissionEvaluator perms;

    @GetMapping
    @Transactional(readOnly = true)
    public Page<UserSummary> list(Pageable pageable) {
        perms.requireAll("user", "read");
        return users.findAll(pageable).map(UserSummary::from);
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public UserSummary get(@PathVariable UUID id) {
        perms.requireAll("user", "read");
        return UserSummary.from(users.findById(id).orElseThrow(() -> new NotFoundException("user", id)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public UserSummary create(@Valid @RequestBody CreateUserRequest req) {
        perms.requireAll("user", "create");
        if (users.existsByEmail(req.email())) {
            throw new ConflictException("email already taken: " + req.email());
        }
        AppUser u = AppUser.create(req.email(), hasher.hash(req.password()), req.fullName());
        return UserSummary.from(users.save(u));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void deactivate(@PathVariable UUID id) {
        perms.requireAll("user", "delete");
        AppUser u = users.findById(id).orElseThrow(() -> new NotFoundException("user", id));
        u.setStatus("closed");
    }

    public record CreateUserRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 12, max = 200) String password,
            @Size(max = 200) String fullName) {}

    public record UserSummary(UUID id, String email, String fullName, String status, List<UUID> branchIds) {
        static UserSummary from(AppUser u) {
            return new UserSummary(u.getId(), u.getEmail(), u.getFullName(), u.getStatus(), List.copyOf(u.getBranchIds()));
        }
    }
}
