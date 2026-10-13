# 💸 Budgeting AI — Assistente financeiro por voz com Spring AI

![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0-6DB33F?logo=springboot&logoColor=white)
![Spring AI](https://img.shields.io/badge/Spring_AI-2.0-6DB33F)
![Gradle](https://img.shields.io/badge/Gradle-Wrapper-02303A?logo=gradle&logoColor=white)

API REST para registrar e consultar despesas, com um assistente de IA capaz de
entender comandos por texto ou voz e responder também em áudio.

Projeto do **Desafio de Projeto Spring AI (DIO)**, evoluído a partir do
[projeto final da trilha Spring Boot](https://github.com/digitalinnovationone/dio-spring-boot-learning-track/tree/main/05-spring-ai).

Você manda um áudio dizendo *"gastei 80 reais no mercado"* ou perguntando *"quanto eu gastei essa semana?"*.
A API transcreve a fala, a IA entende a intenção, chama uma **função real da aplicação** (tool calling)
para salvar ou consultar transações no banco e devolve a resposta **em áudio**.

## Sumário

- [O que o projeto faz](#o-que-o-projeto-faz)
- [Arquitetura](#arquitetura)
- [Tecnologias](#tecnologias)
- [Como executar](#como-executar)
- [Como testar](#como-testar-o-fluxo-principal)
- [Endpoints](#endpoints)
- [Testes](#testes)
- [Próximos passos](#próximos-passos)

---

## O que o projeto faz

```
 áudio ──► Whisper (speech-to-text) ──► ChatClient + Tools ──► Use cases ──► MySQL
                                              │
 mp3   ◄── gpt-4o-mini-tts (text-to-speech) ◄─┘ resposta em texto
```

1. Recebe um arquivo de áudio do cliente (`multipart/form-data`);
2. Transforma o áudio em texto com o `TranscriptionModel` (Whisper);
3. Envia o texto para o `ChatClient`, que decide qual **tool** chamar;
4. A tool é um **use case** da aplicação (o mesmo usado pelos endpoints REST);
5. O use case cria ou consulta transações no banco;
6. A resposta final é convertida em voz com o `TextToSpeechModel` e volta como `.mp3`.

### Arquitetura

O projeto mantém as camadas da trilha (DDD + Clean Architecture). A IA **não fura** as regras de negócio:
ela só consegue agir através dos use cases.

```
src/main/java/dio/budgeting
├── domain/            # Transaction, Category, regras de validação, contrato do repositório
├── application/       # Use cases — usados pelo REST e expostos como @Tool para a IA
│   ├── PersistTransactionUseCase         (tool: persist-transaction)
│   ├── ListTransactionsByCategoryUseCase (tool: list-transactions-by-category)
│   ├── ListRecentTransactionsUseCase     (tool: list-recent-transactions)   ← novo
│   └── SummarizeExpensesUseCase          (tool: summarize-expenses)         ← novo
└── infrastructure/
    ├── ai/            # BudgetAssistant: orquestra transcrição → chat → voz   ← novo
    ├── http/          # Controllers, validação de entrada, tratamento de erros
    └── persistence/   # Entidade e repositório JPA
```

---

## Melhorias implementadas

| # | Melhoria | Por quê |
|---|----------|---------|
| 1 | **Correção do valor em reais** | No projeto base, o valor era salvo em centavos (`8000`) mas exibido como se fosse reais (`8000.0`). Agora `TransactionOutput` converte com `BigDecimal.valueOf(cents, 2)` → `80.00`. Tem teste de regressão. |
| 2 | **Validações antes de salvar** | Regras no domínio (`Transaction`): descrição obrigatória e com até 255 caracteres, valor maior que zero e até R$ 1.000.000,00, categoria obrigatória. Na borda HTTP também há Bean Validation (`@Valid`). Quando a IA tenta salvar algo inválido, o Spring AI devolve a mensagem de erro para o modelo, que explica o problema para a pessoa em vez de quebrar. |
| 3 | **Novas consultas financeiras / novas tools** | `summarize-expenses` (total gasto e total por categoria, com filtro "últimos N dias") e `list-recent-transactions` (últimos gastos). Agora dá pra perguntar *"quanto gastei este mês?"* ou *"em que gastei mais essa semana?"*. |
| 4 | **Novas categorias** | Além de `GROCERIES`, `PHARMA` e `AUTO`: `TRANSPORT`, `RESTAURANT`, `LEISURE`, `HOUSING` e `OTHER`. |
| 5 | **Prompt de sistema melhor** | Regras explícitas de conversão para centavos (com exemplos), guia de categorias, "não invente valores — pergunte", e formato pensado para voz (curto, sem markdown/emoji). |
| 6 | **Endpoints REST novos e melhores** | `GET /transactions` (últimas), `GET /transactions/summary`, `GET /transactions/categories`, `POST /transactions/ai/text` (testa a IA sem áudio) e `POST /transactions/ai/audio-to-text` (áudio → JSON). |
| 7 | **Erros padronizados** | `@RestControllerAdvice` responde no formato *Problem Details* (RFC 9457), em português: 400 para dados inválidos/categoria inexistente/áudio inválido, 422 para regra de negócio, 413 para arquivo grande, 502 se a OpenAI falhar. |
| 8 | **Validação do áudio** | Arquivo vazio ou em formato não suportado pelo Whisper é recusado **antes** de chamar a API paga. |
| 9 | **Responsabilidades separadas + auditoria** | A lógica de IA saiu do controller e foi para `BudgetAssistant`. Cada etapa (transcrição, chat, voz) é logada com o tempo gasto, e cada transação ganhou `createdAt`. |
| 10 | **Testes** | Testes de domínio, use cases, controllers (MockMvc) e contexto completo com H2 — **rodam sem chave da OpenAI e sem Docker**. Testes de integração com a IA real (`*IT`) rodam quando `OPENAI_API_KEY` está definida. |

---

## Tecnologias

- **Java 21** e **Spring Boot 4.0**
- **Spring AI 2.0** (OpenAI): `ChatClient`, Tool Calling (`@Tool`), `TranscriptionModel` (whisper-1), `TextToSpeechModel` (gpt-4o-mini-tts)
- **Spring Web**, **Spring Data JPA**, **Bean Validation**
- **MySQL** via **Docker Compose** (sobe sozinho com `spring-boot-docker-compose`)
- **JUnit 5**, **AssertJ**, **Mockito**, **MockMvc**, **H2** nos testes
- **Gradle** (wrapper incluso) e **Lombok**

---

## Como executar

### Pré-requisitos

- **JDK 21** instalado e configurado em `JAVA_HOME`
- **Docker Desktop** rodando (para o MySQL)
- Uma **chave da OpenAI** com créditos

Confirme a versão ativa antes de iniciar:

```bash
java -version
```

### 1. Defina a chave da OpenAI

**Windows (PowerShell)**
```powershell
$env:OPENAI_API_KEY="sk-..."
```

**Linux / macOS**
```bash
export OPENAI_API_KEY="sk-..."
```

### 2. Suba a aplicação

```bash
# Windows
gradlew.bat bootRun

# Linux / macOS
./gradlew bootRun
```

O Spring Boot sobe o MySQL do `compose.yml` automaticamente e a API fica em `http://localhost:8080`.

### 3. Rode os testes

```bash
gradlew.bat test      # Windows
./gradlew test        # Linux / macOS
```

Sem `OPENAI_API_KEY`, os testes que chamam a OpenAI são **pulados** e o resto roda normalmente.
Com a chave definida, os testes `*IT` também rodam (custam alguns centavos).

---

## Como testar o fluxo principal

O arquivo [`requests.http`](requests.http) tem todas as requisições prontas (IntelliJ ou extensão REST Client do VS Code).
Existem áudios de exemplo em `src/test/resources/audio/`.

### Fluxo completo: áudio → áudio

```bash
curl -X POST http://localhost:8080/transactions/ai \
  -F "file=@src/test/resources/audio/recording-1.m4a" \
  -D - -o resposta.mp3
```

O `resposta.mp3` traz a resposta falada. Os cabeçalhos `X-Transcription` e `X-Assistant-Reply`
mostram (URL-encoded) o que foi entendido e o que a IA respondeu.

### Áudio → JSON (sem gerar voz, mais barato para depurar)

```bash
curl -X POST http://localhost:8080/transactions/ai/audio-to-text \
  -F "file=@src/test/resources/audio/recording-1.m4a"
```

```json
{
  "transcription": "Gastei 80 reais no mercado.",
  "reply": "Registrei 80 reais em compras no mercado, na categoria Mercado."
}
```

### Texto → JSON (testa o tool calling sem áudio)

```bash
curl -X POST http://localhost:8080/transactions/ai/text \
  -H "Content-Type: application/json" \
  -d '{"message": "Paguei 30 reais de Uber e 45 e 50 na farmácia"}'

curl -X POST http://localhost:8080/transactions/ai/text \
  -H "Content-Type: application/json" \
  -d '{"message": "Quanto eu gastei essa semana?"}'
```

### Confirmando no banco pelos endpoints REST

```bash
curl http://localhost:8080/transactions/summary?lastDays=7
```

```json
{
  "period": "últimos 7 dias",
  "transactionCount": 3,
  "total": 155.50,
  "byCategory": [
    { "category": "GROCERIES", "transactionCount": 1, "total": 80.00 },
    { "category": "PHARMA",    "transactionCount": 1, "total": 45.50 },
    { "category": "TRANSPORT", "transactionCount": 1, "total": 30.00 }
  ]
}
```

### Exemplo de erro padronizado

```bash
curl -X POST http://localhost:8080/transactions \
  -H "Content-Type: application/json" \
  -d '{"description": "", "category": "GROCERIES", "amount": -10}'
```

```json
{
  "type": "about:blank",
  "title": "Dados inválidos",
  "status": 400,
  "detail": "amount deve ser maior que zero; description é obrigatória",
  "instance": "/transactions"
}
```

---

## Endpoints

| Método | Rota | Descrição |
|--------|------|-----------|
| `POST` | `/transactions` | Cria transação (`amount` em centavos) |
| `GET` | `/transactions?limit=10` | Últimas transações |
| `GET` | `/transactions/{category}` | Transações de uma categoria |
| `GET` | `/transactions/summary?lastDays=30` | Total gasto e total por categoria |
| `GET` | `/transactions/categories` | Categorias disponíveis |
| `POST` | `/transactions/ai` | **Áudio → áudio (mp3)** — fluxo principal |
| `POST` | `/transactions/ai/audio-to-text` | Áudio → JSON com transcrição e resposta |
| `POST` | `/transactions/ai/text` | Texto → JSON com resposta |

> Nas respostas, valores estão em **reais** (`80.00`). Na criação via REST, `amount` é em **centavos** (`8000`),
> como no projeto original — guardar dinheiro como inteiro evita erros de arredondamento.

---

## O que aprendi

- **IA conectada a uma aplicação de verdade**: com `@Tool`, o modelo não "inventa" — ele chama um método Java
  que passa pelas mesmas regras de negócio do REST. Reaproveitar os use cases como tools manteve a arquitetura limpa.
- **A descrição da tool é parte do código**: nomes, descrições e exemplos (`80 reais = 8000`) mudam muito a precisão
  do modelo. O prompt de sistema funciona como uma especificação.
- **Validação continua sendo responsabilidade da aplicação**: o modelo pode errar; o domínio precisa barrar dados
  inválidos. E, quando barra, o Spring AI devolve o erro para o modelo, que consegue explicar o problema ao usuário.
- **Testar IA tem camadas**: a maior parte do código (domínio, use cases, HTTP) é testada sem IA, de forma rápida e
  determinística; só os testes de integração falam com a OpenAI.
- **Bugs aparecem em detalhes**: a confusão centavos × reais do projeto base só ficou visível escrevendo um teste.
- **Áudio em APIs REST**: `multipart/form-data` na entrada, `byte[]`/`Resource` na saída, e como expor metadados por cabeçalhos.

---

## Testes

A suíte cobre regras de domínio, casos de uso, controllers e o contexto Spring com banco H2 em memória.

```bash
# Windows
gradlew.bat test

# Linux / macOS
./gradlew test
```

Os testes que acessam a OpenAI são identificados pelo sufixo `IT` e só são executados quando
`OPENAI_API_KEY` está disponível no ambiente.

---

## Próximos passos

- Memória de conversa (`ChatMemory`) para perguntas de continuação ("e no mês passado?")
- Editar e excluir transações por voz
- Autenticação e transações por usuário
- Documentação OpenAPI/Swagger
