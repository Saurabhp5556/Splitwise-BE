package splitwise.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import splitwise.dto.AuthResponse;
import splitwise.dto.CreateUserRequest;
import splitwise.dto.LoginRequest;
import splitwise.exception.SplitwiseException;
import splitwise.model.User;
import splitwise.repository.UserRepository;
import splitwise.security.JwtTokenProvider;

import java.util.UUID;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(CreateUserRequest request) {
        // Check if user already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new SplitwiseException("USER_ALREADY_EXISTS", "User with this email already exists");
        }

        // Create new user
        User user = new User();
        user.setUserId("u_" + UUID.randomUUID().toString());
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setMobile(request.getMobile());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole() != null ? request.getRole() : "USER");

        userRepository.save(user);

        // Generate tokens
        String accessToken = tokenProvider.generateAccessToken(user.getUserId(), user.getRole());
        String refreshToken = tokenProvider.generateRefreshToken(user.getUserId());

        return new AuthResponse(accessToken, refreshToken, user.getUserId(), user.getRole());
    }

    public AuthResponse login(LoginRequest request) {
        // Find user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new SplitwiseException("INVALID_CREDENTIALS", "Invalid email or password"));

        // Authenticate
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getUserId(), request.getPassword())
        );

        // Generate tokens
        String accessToken = tokenProvider.generateAccessToken(user.getUserId(), user.getRole());
        String refreshToken = tokenProvider.generateRefreshToken(user.getUserId());

        return new AuthResponse(accessToken, refreshToken, user.getUserId(), user.getRole());
    }

    public AuthResponse refreshToken(String refreshToken) {
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new SplitwiseException("INVALID_TOKEN", "Invalid or expired refresh token");
        }

        String userId = tokenProvider.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new SplitwiseException("USER_NOT_FOUND", "User not found"));

        String newAccessToken = tokenProvider.generateAccessToken(user.getUserId(), user.getRole());
        String newRefreshToken = tokenProvider.generateRefreshToken(user.getUserId());

        return new AuthResponse(newAccessToken, newRefreshToken, user.getUserId(), user.getRole());
    }
}