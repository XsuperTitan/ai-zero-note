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
  - Vision text extraction is triggered after screenshot generation:
    - Select screenshots in UI
    - Click "图生文提取并注入Text content"
- Picture note (sketch-note style PNG), async after notes exist:
  - Uses text buffer or generated note preview as source (`POST /api/notes/image-note-jobs`), poll status, then download PNG
  - Backend providers: OpenAI Images API (`IMAGE_NOTES_PROVIDER=openai`) or Alibaba WAN / DashScope sync path (`IMAGE_NOTES_PROVIDER=wanx`)
  - Errors such as **`Image notes are disabled`** are emitted **before** any image HTTP call — they indicate configuration/state, not a failed upstream model round-trip
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

Configuration reads **`backend/.env.local` from disk** (`spring.config.import` plus an early `EnvironmentPostProcessor`). Changes in the editor are ignored until **you save the file** (otherwise the JVM only sees stale or missing keys).

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
- Vision:
  - `VISION_ENABLED` (default: `true`)
  - `VISION_BASE_URL`, `VISION_API_KEY`, `VISION_MODEL`, `VISION_PROMPT`
  - `VISION_MAX_FRAMES` (default: `30`)
- Picture / image notes (optional):
  - `IMAGE_NOTES_ENABLED` (`true` / `false`); omitting it leaves YAML default off unless you configure WAN implicitly (provider + keys — see below)
  - `IMAGE_NOTES_PROVIDER`: `openai` (default) or `wanx` (DashScope WAN sync HTTP)
  - `IMAGE_NOTES_API_KEY`; if unset, `VISION_API_KEY` is used via `application.yml` chaining for many setups
  - `IMAGE_NOTES_BASE_URL` (OpenAI-compatible root, OpenAI branch)
  - `IMAGE_NOTES_WAN_BASE_URL` (WAN HTTP root; defaults in YAML to DashScope when unset)
  - `IMAGE_NOTES_MODEL`, `IMAGE_NOTES_SIZE`, `IMAGE_NOTES_MAX_SOURCE_CHARS`
  - Troubleshooting: on failure before generation, logs include a **`[image-notes GATE]`** line (`diskFlat.IMAGE_NOTES_*` reflects **parsed disk file**, not unsaved IDE buffers).
- Storage:
  - `APP_OUTPUT_DIR` (default: `outputs`)
  - `APP_MAX_UPLOAD_SIZE_BYTES` (default: `26214400`)
- Video:
  - `VIDEO_YT_DLP_PATH` (default: `yt-dlp`)
  - `VIDEO_FFMPEG_PATH` (default: `ffmpeg`)
  - `VIDEO_FRAMES_SUBDIR` (default: `video-frames`)
  - `VIDEO_DEFAULT_MAX_COUNT` (default: `40`)
  - `VIDEO_MAX_FRAME_COUNT` (default: `60`)
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

Picture notes (WAN + vision key chaining), after saving the file:

```properties
IMAGE_NOTES_ENABLED=true
IMAGE_NOTES_PROVIDER=wanx
# VISION_API_KEY or IMAGE_NOTES_API_KEY supplies the WAN API key when using wanx
```

## API Overview

Notes API:

- `POST /api/notes/process`
- `POST /api/notes/process-mixed`
- `POST /api/notes/compare-transcription`
- `GET /api/notes/asr-status`
- `GET /api/notes/download/{fileName}`
- `POST /api/notes/image-note-jobs` (JSON `{ "sourceText": "..." }`; session cookie auth)
- `GET /api/notes/image-note-jobs/{jobId}`
- `GET /api/notes/image-download/{fileName}`

Video API:

- `GET /api/video/meta?url=...`
- `GET /api/video/frames?url=...`
- `GET /api/video/text?url=...`
- `GET /api/video/vision-text?url=...&taskId=...&fileName=...`
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

- Session-based login/register; persisted note/video task history endpoints exist — scope is still MVP-level.
- Video subtitle availability depends on source platform/video.
- Video text extraction may require valid cookies for some YouTube links.
