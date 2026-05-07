package com.pixel.create_ordnance.mechanics.scanning;

/**
 * A fatal error thrown when a projectile structure violates the Strict Geometry
 * Protocol.
 * 
 * <p>
 * This extends {@link Error} rather than {@link Exception} to bypass standard
 * try-catch blocks
 * and force a "Hard Crash" of the game or server thread.
 * </p>
 */
public class CriticalGeometryError extends Error {

    public CriticalGeometryError(String message) {
        super(message);
    }

    @Override
    public String getMessage() {
        return "\n\n" +
                "==========================================================\n" +
                "       [ORDNANCE] FATAL PROJECTILE GEOMETRY ERROR          \n" +
                "==========================================================\n" +
                super.getMessage() + "\n" +
                "==========================================================\n";
    }
}
