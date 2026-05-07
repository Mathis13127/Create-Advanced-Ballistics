package com.pixel.create_ordnance.commands;

import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.minecraft.resources.ResourceLocation;

public class OrdnanceCommandUtils {

    /**
     * Suggests strings that contain the input as a substring (case-insensitive).
     */
    public static CompletableFuture<Suggestions> suggestSubstring(Iterable<String> candidates,
            SuggestionsBuilder builder) {
        String remaining = builder.getRemaining().toLowerCase();
        for (String candidate : candidates) {
            if (candidate.toLowerCase().contains(remaining)) {
                builder.suggest(candidate);
            }
        }
        return builder.buildFuture();
    }

    /**
     * Suggests resource locations where the full ID contains the input as a
     * substring (case-insensitive).
     */
    public static CompletableFuture<Suggestions> suggestResourceSubstring(Iterable<ResourceLocation> candidates,
            SuggestionsBuilder builder) {
        String remaining = builder.getRemaining().toLowerCase();
        for (ResourceLocation res : candidates) {
            String fullId = res.toString();
            if (fullId.toLowerCase().contains(remaining)) {
                builder.suggest(fullId);
            }
        }
        return builder.buildFuture();
    }
}