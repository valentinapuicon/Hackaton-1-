package com.tuckersoft.branchengine.node;

import com.tuckersoft.branchengine.common.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NodeService {

    private final StoryNodeRepository nodeRepository;

    @Transactional
    public NodeResponse create(NodeRequest req) {
        if (nodeRepository.existsByNodeCode(req.nodeCode())) {
            throw ApiException.conflict("El nodeCode ya existe");
        }
        StoryNode n = new StoryNode();
        n.setNodeCode(req.nodeCode());
        n.setTitle(req.title());
        n.setSceneText(req.sceneText());
        n.setBranchCapacity(req.branchCapacity());
        n.setCurrentBranches(0);
        n.setPrimaryBranchCode(req.primaryBranchCode());
        n.setGlitchBranchCode(req.glitchBranchCode());
        n.setCreatedAt(Instant.now());
        return NodeResponse.from(nodeRepository.save(n));
    }

    @Transactional(readOnly = true)
    public List<NodeResponse> findAll() {
        return nodeRepository.findAll(Sort.by("id")).stream().map(NodeResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public NodeResponse findById(Long id) {
        return nodeRepository.findById(id).map(NodeResponse::from)
                .orElseThrow(() -> ApiException.notFound("Nodo no encontrado"));
    }
}
