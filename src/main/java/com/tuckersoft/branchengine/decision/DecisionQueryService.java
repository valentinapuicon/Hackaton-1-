package com.tuckersoft.branchengine.decision;

import com.tuckersoft.branchengine.common.ApiException;
import com.tuckersoft.branchengine.security.CurrentUserService;
import com.tuckersoft.branchengine.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DecisionQueryService {

    private final DecisionRepository decisionRepository;
    private final RealityLogRepository realityLogRepository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public DecisionPageResponse list(String branchType, String impactLevel, String status,
                                     Long playthroughId, int page, int size) {
        User user = currentUserService.getCurrentUser();
        Specification<Decision> spec = (r, q, cb) -> cb.conjunction();
        if (!currentUserService.isAdmin(user)) {
            Long uid = user.getId();
            spec = spec.and((r, q, cb) -> cb.equal(r.get("playthrough").get("user").get("id"), uid));
        }
        if (branchType != null) spec = spec.and((r, q, cb) -> cb.equal(r.get("branchType"), branchType));
        if (impactLevel != null) spec = spec.and((r, q, cb) -> cb.equal(r.get("impactLevel"), impactLevel));
        if (status != null) spec = spec.and((r, q, cb) -> cb.equal(r.get("status"), status));
        if (playthroughId != null) {
            spec = spec.and((r, q, cb) -> cb.equal(r.get("playthrough").get("id"), playthroughId));
        }
        int safePage = Math.max(page, 0);
        int safeSize = size < 1 ? 10 : size;
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"));
        Page<Decision> result = decisionRepository.findAll(spec, PageRequest.of(safePage, safeSize, sort));
        return new DecisionPageResponse(
                result.getContent().stream().map(DecisionResponse::from).toList(),
                result.getTotalElements(), result.getTotalPages(), result.getNumber(), result.getSize());
    }

    @Transactional(readOnly = true)
    public DecisionResponse findById(Long id) {
        return DecisionResponse.from(loadForRead(id));
    }

    @Transactional(readOnly = true)
    public List<RealityLogResponse> realityLogs(Long id) {
        Decision d = loadForRead(id);
        return realityLogRepository.findByDecisionIdOrderByCreatedAtAscIdAsc(d.getId())
                .stream().map(RealityLogResponse::from).toList();
    }

    // Lectura: duenio o admin (supervisor). Cualquier otro -> 403
    private Decision loadForRead(Long id) {
        Decision d = decisionRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Decision no encontrada"));
        User user = currentUserService.getCurrentUser();
        boolean owner = d.getPlaythrough().getUser().getId().equals(user.getId());
        if (!owner && !currentUserService.isAdmin(user)) {
            throw ApiException.forbidden("No puedes ver una decision ajena");
        }
        return d;
    }
}
