package com.tuckersoft.branchengine.decision;

import java.text.Normalizer;

public final class DecisionRules {

    private DecisionRules() {}

    public static String normalize(String rawInput) {
        return Normalizer.normalize(rawInput, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase();
    }

    // Reglas en orden: la primera que se cumple gana
    public static String classify(String rawInput) {
        String t = normalize(rawInput);
        if (t.chars().noneMatch(c -> c >= 'a' && c <= 'z')) return "ENTRADA_CORRUPTA";
        if (containsAny(t, "netflix", "camara", "espectador", "videojuego")) return "RUPTURA_CUARTA_PARED";
        if (containsAny(t, "vigilan", "simbolo", "conspiracion")) return "SOSPECHA";
        if (containsAny(t, "rechaza", "destruye", "desobedece", "renuncia")) return "REBELDIA";
        return "OBEDIENCIA";
    }

    public static String handlerUnit(String branchType) {
        return switch (branchType) {
            case "OBEDIENCIA" -> "Mesa de Guion";
            case "REBELDIA" -> "Control de Continuidad";
            case "SOSPECHA" -> "Oficina de Seguridad";
            case "RUPTURA_CUARTA_PARED" -> "Departamento Netflix";
            default -> "Archivo de Errores";
        };
    }

    public static String outcomeCode(String branchType) {
        return switch (branchType) {
            case "OBEDIENCIA" -> "ADVANCE_MAIN_PATH";
            case "REBELDIA" -> "FORK_TIMELINE";
            case "SOSPECHA" -> "INJECT_WHITE_BEAR_SYMBOL";
            case "RUPTURA_CUARTA_PARED" -> "BREAK_FOURTH_WALL";
            default -> "DISCARD_INPUT";
        };
    }

    public static int lucidityDelta(String impactLevel) {
        return switch (impactLevel) {
            case "LEVE" -> -5;
            case "MODERADO" -> -15;
            case "GRAVE" -> -30;
            case "CRITICO" -> -40;
            default -> 0;
        };
    }

    public static int controlDelta(String impactLevel) {
        return switch (impactLevel) {
            case "LEVE" -> 5;
            case "MODERADO" -> 10;
            case "GRAVE" -> 20;
            case "CRITICO" -> 45;
            default -> 0;
        };
    }

    public static int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private static boolean containsAny(String text, String... words) {
        for (String w : words) {
            if (text.contains(w)) return true;
        }
        return false;
    }
}
