package com.tuckersoft.branchengine.user;

import com.tuckersoft.branchengine.common.ApiException;
import com.tuckersoft.branchengine.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final Set<String> VALID_ROLES = Set.of("ROLE_USER", "ROLE_ADMIN");

    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    public UserResponse me() {
        return UserResponse.from(currentUserService.getCurrentUser());
    }

    public List<UserResponse> findAll() {
        return userRepository.findAll(Sort.by("id")).stream().map(UserResponse::from).toList();
    }

    @Transactional
    public UserResponse updateRole(Long id, String role) {
        User target = userRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Usuario no encontrado"));
        if (!VALID_ROLES.contains(role)) {
            throw ApiException.badRequest("El rol debe ser ROLE_USER o ROLE_ADMIN");
        }
        User current = currentUserService.getCurrentUser();
        if (current.getId().equals(target.getId())) {
            throw ApiException.badRequest("Un administrador no puede cambiar su propio rol");
        }
        target.setRole(role);
        return UserResponse.from(userRepository.save(target));
    }
}
