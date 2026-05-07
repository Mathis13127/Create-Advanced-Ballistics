package com.pixel.create_ordnance.mechanics.scanning;

/**
 * Thrown when the ProjectileAnalyzer encounters a violation of the strict data
 * protocol.
 * Examples: Unknown block in registry, multiple tails, multiple payloads, or
 * malformed structure.
 * This ensures that no physics calculations are performed on corrupt data.
 */
public class ProjectileAnalysisException extends RuntimeException {

    public ProjectileAnalysisException(String message) {
        super(message);
    }

    public ProjectileAnalysisException(String message, Throwable cause) {
        super(message, cause);
    }
}
