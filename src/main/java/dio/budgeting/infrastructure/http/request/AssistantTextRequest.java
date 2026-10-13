package dio.budgeting.infrastructure.http.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AssistantTextRequest(
        @NotBlank(message = "é obrigatória")
        @Size(max = 1000, message = "deve ter no máximo 1000 caracteres")
        String message) {
}
