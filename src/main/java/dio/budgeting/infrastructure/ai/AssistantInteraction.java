package dio.budgeting.infrastructure.ai;

/**
 * Resultado de uma interação com o assistente.
 *
 * @param transcription o que a pessoa disse (texto transcrito do áudio, ou o texto enviado)
 * @param reply         resposta final gerada pela IA
 */
public record AssistantInteraction(String transcription, String reply) {
}
