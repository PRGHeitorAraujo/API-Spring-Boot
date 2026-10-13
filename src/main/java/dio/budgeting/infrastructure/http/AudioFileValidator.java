package dio.budgeting.infrastructure.http;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Set;

/**
 * Valida o arquivo antes de enviá-lo para a transcrição — evita gastar uma chamada paga
 * com um arquivo vazio ou num formato que o Whisper não aceita.
 */
@Component
public class AudioFileValidator {
    /** Formatos aceitos pela API de transcrição da OpenAI. */
    static final Set<String> SUPPORTED_EXTENSIONS =
            Set.of("flac", "m4a", "mp3", "mp4", "mpeg", "mpga", "oga", "ogg", "wav", "webm");

    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidAudioFileException("Envie um arquivo de áudio no campo 'file'.");
        }
        String name = file.getOriginalFilename();
        String extension = extensionOf(name);
        if (!SUPPORTED_EXTENSIONS.contains(extension)) {
            throw new InvalidAudioFileException(
                    "Formato de áudio não suportado: '" + name + "'. Use um destes: " + String.join(", ", SUPPORTED_EXTENSIONS.stream().sorted().toList()) + ".");
        }
    }

    private static String extensionOf(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
