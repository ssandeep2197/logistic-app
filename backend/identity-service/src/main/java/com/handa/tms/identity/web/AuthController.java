package com.handa.tms.identity.web;

import com.handa.tms.identity.auth.AuthService;
import com.handa.tms.platform.core.auth.AuthPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService auth;

    /**
     * Bootstrap a new tenant with its first admin user.  Public endpoint.
     * In prod this should be rate-limited at the gateway and gated by an
     * invite token; for dev it's open.
     */
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse signup(@Valid @RequestBody SignupRequest req) {
        var r = auth.signup(req.tenantSlug(), req.tenantName(), req.email(), req.password(), req.fullName());
        return new TokenResponse(r.tenantId(), r.userId(), r.accessToken(), r.refreshToken());
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest req) {
        var r = auth.login(req.tenantSlug(), req.email(), req.password());
        return new TokenResponse(r.tenantId(), r.userId(), r.accessToken(), r.refreshToken());
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest req) {
        var r = auth.refresh(req.refreshToken());
        return new TokenResponse(r.tenantId(), r.userId(), r.accessToken(), r.refreshToken());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest req) {
        auth.logout(req.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal AuthPrincipal me) {
        return new MeResponse(
                me.userId(), me.tenantId(), me.email(),
                List.copyOf(me.permissions()),
                List.copyOf(me.branchIds()));
    }

    // ─── DTOs ────────────────────────────────────────────────────────────────

    public record SignupRequest(
            @NotBlank @Size(min = 2, max = 64) String tenantSlug,
            @NotBlank @Size(max = 200) String tenantName,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 12, max = 200) String password,
            @Size(max = 200) String fullName) {}

    public record LoginRequest(
            @NotBlank String tenantSlug,
            @NotBlank @Email String email,
            @NotBlank String password) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}

    public record TokenResponse(UUID tenantId, UUID userId, String accessToken, String refreshToken) {}

    public record MeResponse(
            UUID userId, UUID tenantId, String email,
            List<String> permissions, List<UUID> branchIds) {}
}
