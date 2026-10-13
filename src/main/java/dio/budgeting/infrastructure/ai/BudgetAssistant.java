package dio.budgeting.infrastructure.ai;

import dio.budgeting.application.ListRecentTransactionsUseCase;
import dio.budgeting.application.ListTransactionsByCategoryUseCase;
import dio.budgeting.application.PersistTransactionUseCase;
import dio.budgeting.application.SummarizeExpensesUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Orquestra o fluxo de IA: áudio -> texto -> ChatClient (com tool calling) -> resposta -> voz.
 * <p>
 * No projeto base essa lógica ficava dentro do controller. Aqui ela foi extraída para um
 * componente próprio: o controller só cuida de HTTP e este componente só cuida de IA.
 * Cada etapa é registrada em log (auditoria) com o tempo gasto.
 */
@Component
public class BudgetAssistant {
    private static final Logger log = LoggerFactory.getLogger(BudgetAssistant.class);

    static final String EMPTY_TRANSCRIPTION_REPLY =
            "Não consegui entender o áudio. Pode repetir falando o valor e onde foi o gasto?";

    private final TranscriptionModel transcriptionModel;
    private final TextToSpeechModel textToSpeechModel;
    private final ChatClient chatClient;

    public BudgetAssistant(TranscriptionModel transcriptionModel,
                           TextToSpeechModel textToSpeechModel,
                           ChatClient.Builder chatClientBuilder,
                           @Value("classpath:prompts/system-message.st") Resource systemPrompt,
                           PersistTransactionUseCase persistTransactionUseCase,
                           ListTransactionsByCategoryUseCase listTransactionsByCategoryUseCase,
                           ListRecentTransactionsUseCase listRecentTransactionsUseCase,
                           SummarizeExpensesUseCase summarizeExpensesUseCase) throws IOException {
        this.transcriptionModel = transcriptionModel;
        this.textToSpeechModel = textToSpeechModel;
        this.chatClient = chatClientBuilder
                .defaultSystem(systemPrompt.getContentAsString(StandardCharsets.UTF_8))
                .defaultTools(
                        persistTransactionUseCase,
                        listTransactionsByCategoryUseCase,
                        listRecentTransactionsUseCase,
                        summarizeExpensesUseCase)
                .build();
    }

    /** Fluxo completo de áudio, sem a etapa de voz: transcreve e gera a resposta. */
    public AssistantInteraction handleAudio(Resource audio) {
        String transcription = transcribe(audio);
        if (transcription == null || transcription.isBlank()) {
            log.info("[assistant] transcrição vazia, IA não foi chamada");
            return new AssistantInteraction("", EMPTY_TRANSCRIPTION_REPLY);
        }
        return new AssistantInteraction(transcription, reply(transcription));
    }

    /** Fluxo de texto: útil para testar o tool calling sem gastar com transcrição/voz. */
    public AssistantInteraction handleText(String message) {
        return new AssistantInteraction(message, reply(message));
    }

    public String transcribe(Resource audio) {
        return timed("transcrição", () -> {
            String text = transcriptionModel.transcribe(audio);
            return text == null ? "" : text.strip();
        });
    }

    public String reply(String userMessage) {
        String reply = timed("chat", () -> chatClient.prompt().user(userMessage).call().content());
        log.info("[assistant] usuário: \"{}\" | resposta: \"{}\"", userMessage, reply);
        return reply;
    }

    public byte[] speak(String text) {
        return timed("voz", () -> textToSpeechModel.call(text));
    }

    private <T> T timed(String step, java.util.function.Supplier<T> action) {
        long start = System.nanoTime();
        try {
            T result = action.get();
            log.info("[assistant] etapa '{}' concluída em {} ms", step, (System.nanoTime() - start) / 1_000_000);
            return result;
        } catch (RuntimeException e) {
            log.error("[assistant] etapa '{}' falhou após {} ms", step, (System.nanoTime() - start) / 1_000_000, e);
            throw new AssistantUnavailableException("Falha na etapa de " + step + " do assistente de IA.", e);
        }
    }
}
