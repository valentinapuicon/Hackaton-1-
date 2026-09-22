package com.tuckersoft.branchengine.decision;

import com.tuckersoft.branchengine.common.ApiException;
import com.tuckersoft.branchengine.node.StoryNode;
import com.tuckersoft.branchengine.node.StoryNodeRepository;
import com.tuckersoft.branchengine.playthrough.Playthrough;
import com.tuckersoft.branchengine.playthrough.PlaythroughRepository;
import com.tuckersoft.branchengine.security.CurrentUserService;
import com.tuckersoft.branchengine.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DecisionService {

    private final PlaythroughRepository playthroughRepository;
    private final StoryNodeRepository nodeRepository;
    private final DecisionRepository decisionRepository;
    private final CurrentUserService currentUserService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public DecisionResponse create(DecisionRequest req, String simulateHeader) {
        // 1. Usuario del token; la partida debe ser suya (sin excepcion para admin)
        User user = currentUserService.getCurrentUser();
        Playthrough p = playthroughRepository.findById(req.playthroughId())
                .orElseThrow(() -> ApiException.notFound("Partida no encontrada"));
        if (!p.getUser().getId().equals(user.getId())) {
            throw ApiException.forbidden("Solo el duenio de la partida puede decidir");
        }
        // 2. La partida debe estar ACTIVA
        if ("FINALIZADA".equals(p.getStatus())) {
            throw ApiException.conflict("La partida ya esta FINALIZADA");
        }

        // 3. Clasificar y derivar departamento y consecuencia
        String branch = DecisionRules.classify(req.rawInput());
        String impact = req.impactLevel();
        StoryNode source = p.getCurrentNode();
        Instant now = Instant.now();

        Decision d = new Decision();
        d.setPlaythrough(p);
        d.setNode(source);
        d.setRawInput(req.rawInput());
        d.setBranchType(branch);
        d.setImpactLevel(impact);
        d.setHandlerUnit(DecisionRules.handlerUnit(branch));
        d.setOutcomeCode(DecisionRules.outcomeCode(branch));
        d.setCreatedAt(now);
        d.setUpdatedAt(now);

        // 4. Entrada corrupta: se guarda con ERROR, no toca la partida ni publica evento
        if ("ENTRADA_CORRUPTA".equals(branch)) {
            d.setResolvedNodeCode(null);
            d.setStatus("ERROR");
            return DecisionResponse.from(decisionRepository.save(d));
        }

        // 5a. Stats segun impacto, con limites 0..100
        int lucidity = DecisionRules.clamp(p.getLucidity() + DecisionRules.lucidityDelta(impact));
        int control = DecisionRules.clamp(p.getControlLevel() + DecisionRules.controlDelta(impact));
        p.setLucidity(lucidity);
        p.setControlLevel(control);

        // 5b. Nodo destino: glitch si RUPTURA o CRITICO; si no, primary
        boolean glitch = "RUPTURA_CUARTA_PARED".equals(branch) || "CRITICO".equals(impact);
        String target = source == null ? null
                : (glitch ? source.getGlitchBranchCode() : source.getPrimaryBranchCode());
        d.setResolvedNodeCode(target);

        // 5c. Estado de la partida, en este orden exacto
        if (control >= 100) {
            finish(p, "ENDING_PAC_SYMBOL");
        } else if (lucidity <= 0) {
            finish(p, "ENDING_WHITE_BEAR");
        } else {
            Optional<StoryNode> targetNode = target == null
                    ? Optional.empty() : nodeRepository.findByNodeCode(target);
            if (targetNode.isEmpty()) {
                finish(p, "ENDING_NETFLIX_CUT");
            } else {
                p.setStatus("ACTIVA");
                p.setCurrentNode(targetNode.get());
            }
        }
        p.setUpdatedAt(now);

        // 6-7. Guardar partida y decision
        playthroughRepository.save(p);
        d.setStatus("REGISTRADA");
        Decision saved = decisionRepository.save(d);

        // 8. Publicar evento (el listener corre despues del COMMIT)
        eventPublisher.publishEvent(new DecisionCommittedEvent(
                saved.getId(), p.getUser().getEmail(), p.getUser().getDisplayName(), p.getPlayerTag(),
                branch, impact, saved.getHandlerUnit(), saved.getOutcomeCode(),
                source != null ? source.getNodeCode() : null, target,
                p.getStatus(), p.getLucidity(), p.getControlLevel(), p.getEndingCode(),
                saved.getCreatedAt(), saved.getRawInput(), "MAIL_FAILURE".equals(simulateHeader)));

        return DecisionResponse.from(saved);
    }

    private void finish(Playthrough p, String endingCode) {
        p.setStatus("FINALIZADA");
        p.setEndingCode(endingCode);
    }
}
