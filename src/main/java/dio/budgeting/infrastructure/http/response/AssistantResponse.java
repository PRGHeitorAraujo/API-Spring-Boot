package dio.budgeting.infrastructure.http.response;

import dio.budgeting.infrastructure.ai.AssistantInteraction;

public record AssistantResponse(String transcription, String reply) {
    public static AssistantResponse from(AssistantInteraction interaction) {
        return new AssistantResponse(interaction.transcription(), interaction.reply());
    }
}
