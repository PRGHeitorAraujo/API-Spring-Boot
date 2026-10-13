package dio.budgeting.infrastructure.http;

import dio.budgeting.infrastructure.ai.AssistantInteraction;
import dio.budgeting.infrastructure.ai.AssistantUnavailableException;
import dio.budgeting.infrastructure.ai.BudgetAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Testa a camada HTTP do assistente com a IA "mockada" — roda sem OPENAI_API_KEY. */
class AssistantControllerTest {
    private BudgetAssistant assistant;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        assistant = mock(BudgetAssistant.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AssistantController(assistant, new AudioFileValidator()))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void should_answerTextMessage() throws Exception {
        when(assistant.handleText("Gastei 80 reais no mercado"))
                .thenReturn(new AssistantInteraction("Gastei 80 reais no mercado", "Registrei 80 reais em Mercado."));

        mockMvc.perform(post("/transactions/ai/text")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\": \"  Gastei 80 reais no mercado  \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transcription").value("Gastei 80 reais no mercado"))
                .andExpect(jsonPath("$.reply").value("Registrei 80 reais em Mercado."));
    }

    @Test
    void should_return400_when_textMessageIsBlank() throws Exception {
        mockMvc.perform(post("/transactions/ai/text")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\": \"   \"}"))
                .andExpect(status().isBadRequest());

        verify(assistant, never()).handleText(anyString());
    }

    @Test
    void should_returnTranscriptionAndReply_forAudio() throws Exception {
        when(assistant.handleAudio(any()))
                .thenReturn(new AssistantInteraction("Gastei 40 reais na farmácia", "Registrei 40 reais em Farmácia."));

        mockMvc.perform(multipart("/transactions/ai/audio-to-text")
                        .file(new MockMultipartFile("file", "gasto.m4a", "audio/mp4", new byte[]{1, 2, 3})))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transcription").value("Gastei 40 reais na farmácia"))
                .andExpect(jsonPath("$.reply").value("Registrei 40 reais em Farmácia."));
    }

    @Test
    void should_returnMp3_withDebugHeaders_forVoiceFlow() throws Exception {
        when(assistant.handleAudio(any()))
                .thenReturn(new AssistantInteraction("Gastei 40 reais", "Registrei 40 reais."));
        when(assistant.speak("Registrei 40 reais.")).thenReturn(new byte[]{9, 9, 9});

        mockMvc.perform(multipart("/transactions/ai")
                        .file(new MockMultipartFile("file", "gasto.mp3", "audio/mpeg", new byte[]{1, 2, 3})))
                .andExpect(status().isOk())
                .andExpect(content().bytes(new byte[]{9, 9, 9}))
                .andExpect(header().string(AssistantController.TRANSCRIPTION_HEADER, "Gastei%2040%20reais"))
                .andExpect(header().string("Content-Disposition", containsString("resposta.mp3")));
    }

    @Test
    void should_return400_when_audioIsEmpty() throws Exception {
        mockMvc.perform(multipart("/transactions/ai/audio-to-text")
                        .file(new MockMultipartFile("file", "gasto.m4a", "audio/mp4", new byte[0])))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("arquivo de áudio")));

        verify(assistant, never()).handleAudio(any());
    }

    @Test
    void should_return400_when_audioFormatIsNotSupported() throws Exception {
        mockMvc.perform(multipart("/transactions/ai/audio-to-text")
                        .file(new MockMultipartFile("file", "notas.txt", "text/plain", new byte[]{1})))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("não suportado")));

        verify(assistant, never()).handleAudio(any());
    }

    @Test
    void should_return502_when_aiProviderFails() throws Exception {
        when(assistant.handleText(anyString()))
                .thenThrow(new AssistantUnavailableException("Falha na etapa de chat do assistente de IA.", new RuntimeException("401")));

        mockMvc.perform(post("/transactions/ai/text")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\": \"quanto gastei?\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.detail", containsString("OPENAI_API_KEY")));
    }
}
