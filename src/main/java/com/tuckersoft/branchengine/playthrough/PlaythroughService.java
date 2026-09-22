package com.tuckersoft.branchengine.playthrough;

import com.tuckersoft.branchengine.common.ApiException;
import com.tuckersoft.branchengine.decision.Decision;
import com.tuckersoft.branchengine.decision.DecisionRepository;
import com.tuckersoft.branchengine.node.StoryNode;
import com.tuckersoft.branchengine.node.StoryNodeRepository;
import com.tuckersoft.branchengine.security.CurrentUserService;
import com.tuckersoft.branchengine.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlaythroughService {

    private final PlaythroughRepository playthroughRepository;
    private final StoryNodeRepository nodeRepository;
    private final DecisionRepository decisionRepository;
    private final CurrentUserService currentUserService;

    @Transactional
    public PlaythroughResponse create(PlaythroughRequest req) {
        User user = currentUserService.getCurrentUser();
        StoryNode node = nodeRepository.findByNodeCode(req.startNodeCode())
                .orElseThrow(() -> ApiException.notFound("Nodo de inicio no encontrado"));
        if (playthroughRepository.existsByPlayerTag(req.playerTag())) {
            throw ApiException.conflict("El playerTag ya existe");
        }
        if (node.getCurrentBranches() >= node.getBranchCapacity()) {
            throw ApiException.badRequest("El nodo esta lleno");
        }
        Instant now = Instant.now();
        Playthrough p = new Playthrough();
        p.setPlayerTag(req.playerTag());
        p.setUser(user);
        p.setCurrentNode(node);
        p.setStartNodeCode(node.getNodeCode());
        p.setLucidity(100);
        p.setControlLevel(0);
        p.setStatus("ACTIVA");
        p.setEndingCode(null);
        p.setCreatedAt(now);
        p.setUpdatedAt(now);

        node.setCurrentBranches(node.getCurrentBranches() + 1);
        nodeRepository.save(node);

        return PlaythroughResponse.from(playthroughRepository.save(p));
    }

    @Transactional(readOnly = true)
    public List<PlaythroughResponse> findAll() {
        User user = currentUserService.getCurrentUser();
        List<Playthrough> list = currentUserService.isAdmin(user)
                ? playthroughRepository.findAllByOrderByCreatedAtDescIdDesc()
                : playthroughRepository.findByUserIdOrderByCreatedAtDescIdDesc(user.getId());
        return list.stream().map(PlaythroughResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PlaythroughResponse findById(Long id) {
        return PlaythroughResponse.from(loadForRead(id));
    }

    @Transactional(readOnly = true)
    public PathResponse path(Long id) {
        Playthrough p = loadForRead(id);
        List<Decision> decisions =
                decisionRepository.findByPlaythroughIdAndResolvedNodeCodeIsNotNullOrderByCreatedAtAscIdAsc(p.getId());
        List<PathStep> steps = new ArrayList<>();
        int order = 1;
        for (Decision d : decisions) {
            steps.add(new PathStep(order++, d.getId(),
                    d.getNode() != null ? d.getNode().getNodeCode() : null,
                    d.getResolvedNodeCode(), d.getBranchType(), d.getImpactLevel(), d.getCreatedAt()));
        }
        return new PathResponse(p.getId(), p.getPlayerTag(), p.getStatus(), p.getEndingCode(),
                p.getStartNodeCode(),
                p.getCurrentNode() != null ? p.getCurrentNode().getNodeCode() : null,
                steps);
    }

    // Lectura: el duenio o un admin (supervisor). Cualquier otro -> 403
    private Playthrough loadForRead(Long id) {
        Playthrough p = playthroughRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Partida no encontrada"));
        User user = currentUserService.getCurrentUser();
        boolean owner = p.getUser().getId().equals(user.getId());
        if (!owner && !currentUserService.isAdmin(user)) {
            throw ApiException.forbidden("No puedes ver una partida ajena");
        }
        return p;
    }
}
