package dio.budgeting.infrastructure.http;

import dio.budgeting.infrastructure.ai.BudgetAssistant;
import dio.budgeting.infrastructure.http.request.AssistantTextRequest;
import dio.budgeting.infrastructure.http.response.AssistantResponse;
import jakarta.validation.Valid;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Endpoints do assistente de IA.
 * <ul>
 *     <li>POST /transactions/ai — áudio entra, áudio (mp3) sai. Fluxo original do projeto.</li>
 *     <li>POST /transactions/ai/audio-to-text — áudio entra, JSON com transcrição + resposta sai (sem gerar voz).</li>
 *     <li>POST /transactions/ai/text — texto entra, JSON sai. Ótimo para testar o tool calling rapidamente.</li>
 * </ul>
 */
@RestController
@RequestMapping("/transactions/ai")
public class AssistantController {
    static final String TRANSCRIPTION_HEADER = "X-Transcription";
    static final String REPLY_HEADER = "X-Assistant-Reply";
    private static final MediaType AUDIO_MPEG = MediaType.parseMediaType("audio/mpeg");

    private final BudgetAssistant assistant;
    private final AudioFileValidator audioFileValidator;

    public AssistantController(BudgetAssistant assistant, AudioFileValidator audioFileValidator) {
        this.assistant = assistant;
        this.audioFileValidator = audioFileValidator;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Resource> voice(@RequestParam("file") MultipartFile file) {
        audioFileValidator.validate(file);
        var interaction = assistant.handleAudio(file.getResource());
        byte[] audio = assistant.speak(interaction.reply());

        return ResponseEntity.ok()
                .contentType(AUDIO_MPEG)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("resposta.mp3").build().toString())
                // Cabeçalhos extras para depuração (URL-encoded, pois cabeçalhos HTTP só aceitam ASCII)
                .header(TRANSCRIPTION_HEADER, urlEncode(interaction.transcription()))
                .header(REPLY_HEADER, urlEncode(interaction.reply()))
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, TRANSCRIPTION_HEADER + ", " + REPLY_HEADER)
                .body(new ByteArrayResource(audio));
    }

    @PostMapping(value = "/audio-to-text", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AssistantResponse audioToText(@RequestParam("file") MultipartFile file) {
        audioFileValidator.validate(file);
        return AssistantResponse.from(assistant.handleAudio(file.getResource()));
    }

    @PostMapping(value = "/text", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AssistantResponse text(@Valid @RequestBody AssistantTextRequest request) {
        return AssistantResponse.from(assistant.handleText(request.message().strip()));
    }

    private static String urlEncode(String value) {
        return value == null ? "" : URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
