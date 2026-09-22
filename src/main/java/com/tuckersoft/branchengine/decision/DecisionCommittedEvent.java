package com.tuckersoft.branchengine.decision;

import java.time.Instant;

// Lleva todo lo que el listener necesita: en su hilo ya no hay usuario autenticado
public record DecisionCommittedEvent(Long decisionId, String recipientEmail, String displayName,
                                     String playerTag, String branchType, String impactLevel,
                                     String handlerUnit, String outcomeCode, String sourceNodeCode,
                                     String resolvedNodeCode, String playthroughStatus,
                                     Integer lucidity, Integer controlLevel, String endingCode,
                                     Instant createdAt, String rawInput, boolean simulateFailure) {}
