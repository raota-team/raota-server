package com.raota.agent.application.ramenshop.command;

/**
 * Application-level input for an AI ramen shop search.
 *
 * <p>The HTTP request is intentionally kept out of the application layer so
 * batch jobs, evaluation runs, and future adapters can reuse the same
 * service contract without constructing presentation DTOs.</p>
 */
public record AiRamenShopSearchCommand(String query, Long memberId) {
}
