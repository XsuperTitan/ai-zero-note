# AI Zero Notes

AI Zero Notes supports two input paths:

1. Audio/text mixed input (MP3 + text content) to generate Markdown notes.
2. Video link parsing to extract metadata, key screenshots, and video text context for note generation.

## Implemented Features

- Mixed note pipeline:
  - Upload `.mp3` and/or text (`.txt` / `.md` / pasted text)
  - ASR transcription (Whisper or challenger provider)
  - LLM summarization into structured JSON + final Markdown
  - Markdown file export and download
- ASR comparison endpoint:
  - Compare Whisper vs challenger transcription
  - Optional WER scoring with reference text
- Video parsing module:
  - Parse video metadata (`title`, `duration`, `uploader`, `thumbnail`)
  - Extract key screenshots (interval-first with scene-change fallback)
  - Dynamic screenshot count strategy based on video duration
  - Screenshot download endpoint
  - Video-to-text endpoint (`/api/video/text`) with dual-channel fallback:
    - First attempt: subtitle extraction
    - Fallback: audio extraction + existing ASR transcription
- Robust external-tool handling:
  - Auto-detect executable paths for `yt-dlp` / `ffmpeg`
  - Optional YouTube cookies support (`cookies-from-browser` or cookie file)
  - Process timeout and cleaner error messages

## Tech Stack

- Backend: Java 21, Spring Boot 3
- Frontend: Vue 3 + Vite + TypeScript
- AI APIs: ASR + Chat Completions (configurable)
- Video tools: `yt-dlp`, `ffmpeg`

## Project Structure

```text
ai-no-note/
├── backend/
└── frontend/
```

## Prerequisites

- Java 21+
- Maven 3.9+
- Node.js 18+
- `yt-dlp` and `ffmpeg` installed on backend host

## Run Backend

```bash
cd backend
mvn spring-boot:run
```

`backend/.env.local` is auto-loaded by Spring Boot if present.

## Run Frontend

```bash
cd frontend
npm install
npm run dev
```

Open [http://localhost:5173](http://localhost:5173).

## Configuration

Common optional environment variables:

- ASR:
  - `ASR_DEFAULT_PROVIDER` (`auto`, `whisper`, `challenger`)
  - `ASR_WHISPER_BASE_URL`, `ASR_WHISPER_API_KEY`, `ASR_WHISPER_MODEL`
  - `ASR_CHALLENGER_BASE_URL`, `ASR_CHALLENGER_API_KEY`, `ASR_CHALLENGER_MODEL`
  - `STEPFUN_BASE_URL`, `STEPFUN_API_KEY`, `STEPFUN_ASR_MODEL`
- Summary:
  - `SUMMARY_BASE_URL`, `SUMMARY_API_KEY`, `SUMMARY_MODEL`, `SUMMARY_USER_PROMPT`
  - `DEEPSEEK_BASE_URL`, `DEEPSEEK_API_KEY`, `DEEPSEEK_MODEL`
- Storage:
  - `APP_OUTPUT_DIR` (default: `outputs`)
  - `APP_MAX_UPLOAD_SIZE_BYTES` (default: `26214400`)
- Video:
  - `VIDEO_YT_DLP_PATH` (default: `yt-dlp`)
  - `VIDEO_FFMPEG_PATH` (default: `ffmpeg`)
  - `VIDEO_FRAMES_SUBDIR` (default: `video-frames`)
  - `VIDEO_DEFAULT_MAX_COUNT` (default: `12`)
  - `VIDEO_MAX_FRAME_COUNT` (default: `24`)
  - `VIDEO_DEFAULT_SCENE_THRESHOLD` (default: `0.4`)
  - `VIDEO_DEFAULT_INTERVAL_SEC` (default: `10`)
  - `VIDEO_PROCESS_TIMEOUT_SECONDS` (default: `300`)
  - `VIDEO_CLEANUP_RETENTION_DAYS` (default: `7`)
  - `VIDEO_YT_DLP_COOKIES_FROM_BROWSER` (example: `chrome`)
  - `VIDEO_YT_DLP_COOKIE_FILE` (absolute path to exported cookies file)

Example (`backend/.env.local`):

```properties
VIDEO_YT_DLP_COOKIES_FROM_BROWSER=chrome
```

## API Overview

Notes API:

- `POST /api/notes/process`
- `POST /api/notes/process-mixed`
- `POST /api/notes/compare-transcription`
- `GET /api/notes/asr-status`
- `GET /api/notes/download/{fileName}`

Video API:

- `GET /api/video/meta?url=...`
- `GET /api/video/frames?url=...`
- `GET /api/video/text?url=...`
- `GET /api/video/download/{taskId}/{fileName}`

## Compare ASR Accuracy

```bash
curl -X POST "http://localhost:8080/api/notes/compare-transcription" \
  -F "file=@/absolute/path/demo.mp3" \
  -F "referenceText=your ground truth transcript"
```

- With `referenceText`, response includes WER for both providers (lower is better).
- Without `referenceText`, response returns both transcripts without objective score.

## Current MVP Limits

- No auth or task history yet.
- Video subtitle availability depends on source platform/video.
- Video text extraction may require valid cookies for some YouTube links.
