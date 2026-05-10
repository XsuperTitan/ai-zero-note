# AI Zero Notes (MVP)

MVP pipeline:

1. Upload MP3 file and/or text content from Vue3 frontend
2. Backend transcribes audio (when provided)
3. Backend merges audio transcription with uploaded text content
4. Backend summarizes merged content with chat model
4. Backend exports a markdown file and returns a download URL

## Tech Stack

- Backend: Java 21, Spring Boot 3
- Frontend: Vue 3 + Vite + TypeScript
- AI APIs: OpenAI Whisper + Chat Completions

## Project Structure

```text
ai-no-note/
├── backend/
└── frontend/
```

## Backend Run

```bash
cd backend
mvn spring-boot:run
```

`backend/.env.local` is auto-loaded by Spring Boot if present.

Optional environment variables:

- `ASR_DEFAULT_PROVIDER` (`whisper` or `challenger`)
- `ASR_WHISPER_BASE_URL`, `ASR_WHISPER_API_KEY`, `ASR_WHISPER_MODEL`
- `ASR_CHALLENGER_BASE_URL`, `ASR_CHALLENGER_API_KEY`, `ASR_CHALLENGER_MODEL`
- `STEPFUN_BASE_URL`, `STEPFUN_API_KEY`, `STEPFUN_ASR_MODEL` (mapped to challenger ASR)
- `SUMMARY_BASE_URL`, `SUMMARY_API_KEY`, `SUMMARY_MODEL`, `SUMMARY_USER_PROMPT`
- `DEEPSEEK_BASE_URL`, `DEEPSEEK_API_KEY`, `DEEPSEEK_MODEL` (fallbacks for summary)
- `APP_OUTPUT_DIR` (default: `outputs`)
- `APP_MAX_UPLOAD_SIZE_BYTES` (default: `26214400`)

### Compare ASR Accuracy

The backend now supports comparing Whisper vs your challenger ASR on the same MP3:

```bash
curl -X POST "http://localhost:8080/api/notes/compare-transcription" \
  -F "file=@/absolute/path/demo.mp3" \
  -F "referenceText=your ground truth transcript"
```

- With `referenceText`, response includes WER for both providers (lower is better).
- Without `referenceText`, response returns both transcripts but cannot score objective accuracy.

## Frontend Run

```bash
cd frontend
npm install
npm run dev
```

Open [http://localhost:5173](http://localhost:5173).

## Current MVP Limits

- Only `.mp3` upload is supported.
- No auth or task history yet.
- API-first implementation (no local ASR model yet).
