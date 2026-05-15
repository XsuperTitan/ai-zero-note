ALTER TABLE guidance_session
    ADD COLUMN progress_video_id VARCHAR(64) NULL AFTER status;

UPDATE guidance_session SET status = 'PROFILED' WHERE status = 'PROFILE_READY';
