package com.tuckersoft.branchengine.decision;

import com.tuckersoft.branchengine.node.StoryNode;
import com.tuckersoft.branchengine.node.StoryNodeRepository;
import com.tuckersoft.branchengine.playthrough.Playthrough;
import com.tuckersoft.branchengine.playthrough.PlaythroughRepository;
import com.tuckersoft.branchengine.security.CurrentUserService;
import com.tuckersoft.branchengine.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DecisionServiceTest {

    @Mock PlaythroughRepository playthroughRepository;
    @Mock StoryNodeRepository nodeRepository;
    @Mock DecisionRepository decisionRepository;
    @Mock CurrentUserService currentUserService;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks DecisionService decisionService;

    User owner;
    StoryNode origin;
    Playthrough playthrough;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(1L);
        owner.setEmail("qa@tuckersoft.test");
        owner.setDisplayName("Ada Lovelace");
        owner.setRole("ROLE_USER");

        origin = node("NODE-A", "NODE-B", "NODE-G");

        playthrough = new Playthrough();
        playthrough.setId(10L);
        playthrough.setUser(owner);
        playthrough.setPlayerTag("STEFAN-01");
        playthrough.setStartNodeCode("NODE-A");
        playthrough.setCurrentNode(origin);
        playthrough.setLucidity(100);
        playthrough.setControlLevel(0);
        playthrough.setStatus("ACTIVA");

        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(playthroughRepository.findById(10L)).thenReturn(Optional.of(playthrough));
        when(nodeRepository.findByNodeCode("NODE-B")).thenReturn(Optional.of(node("NODE-B", null, null)));
        when(nodeRepository.findByNodeCode("NODE-G")).thenReturn(Optional.of(node("NODE-G", null, null)));
        when(decisionRepository.save(any(Decision.class))).thenAnswer(inv -> {
            Decision d = inv.getArgument(0);
            d.setId(99L);
            return d;
        });
    }

    private StoryNode node(String code, String primary, String glitch) {
        StoryNode n = new StoryNode();
        n.setNodeCode(code);
        n.setPrimaryBranchCode(primary);
        n.setGlitchBranchCode(glitch);
        return n;
    }

    private DecisionResponse decide(String rawInput, String impact) {
        return decisionService.create(new DecisionRequest(10L, rawInput, impact), null);
    }

    @Test
    void precedencia_ruptura_cuarta_pared_antes_que_rebeldia() {
        DecisionResponse r = decide("Stefan destruye la camara", "LEVE");
        assertEquals("RUPTURA_CUARTA_PARED", r.branchType());
    }

    @Test
    void texto_sin_letras_es_entrada_corrupta_y_no_toca_la_partida() {
        DecisionResponse r = decide("1234 5678 !!??", "GRAVE");
        assertEquals("ENTRADA_CORRUPTA", r.branchType());
        assertEquals("ERROR", r.status());
        assertNull(r.resolvedNodeCode());
        assertEquals(100, playthrough.getLucidity());
        assertEquals(0, playthrough.getControlLevel());
        assertEquals("ACTIVA", playthrough.getStatus());
        assertSame(origin, playthrough.getCurrentNode());
        verify(playthroughRepository, never()).save(any());
    }

    @Test
    void critico_resta_40_suma_45_y_respeta_limites() {
        DecisionResponse r = decide("Stefan acepta la oferta de Mohan", "CRITICO");
        assertEquals(60, r.lucidity());
        assertEquals(45, r.controlLevel());

        playthrough.setStatus("ACTIVA");
        playthrough.setCurrentNode(origin);
        playthrough.setLucidity(30);
        playthrough.setControlLevel(80);
        DecisionResponse r2 = decide("Stefan acepta otra vez la oferta", "CRITICO");
        assertEquals(0, r2.lucidity());
        assertEquals(100, r2.controlLevel());
    }

    @Test
    void control_100_termina_con_pac_symbol_aunque_lucidez_sea_0() {
        playthrough.setLucidity(40);
        playthrough.setControlLevel(55);
        DecisionResponse r = decide("Stefan acepta la oferta de Mohan", "CRITICO");
        assertEquals(0, r.lucidity());
        assertEquals(100, r.controlLevel());
        assertEquals("FINALIZADA", r.playthroughStatus());
        assertEquals("ENDING_PAC_SYMBOL", r.endingCode());
    }

    @Test
    void publica_evento_una_vez_en_decision_normal_y_cero_en_corrupta() {
        decide("Stefan acepta la oferta de Mohan", "LEVE");
        verify(eventPublisher, times(1)).publishEvent(any(DecisionCommittedEvent.class));

        clearInvocations(eventPublisher);
        decide("#### 1234 !!!!", "LEVE");
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }
}
