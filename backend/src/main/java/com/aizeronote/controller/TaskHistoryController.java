package com.aizeronote.controller;

import com.aizeronote.model.dto.history.NoteJobDetailDto;
import com.aizeronote.model.dto.history.NoteJobSummaryDto;
import com.aizeronote.model.dto.history.VideoTaskDetailDto;
import com.aizeronote.model.dto.history.VideoTaskSummaryDto;
import com.aizeronote.service.TaskHistoryService;
import com.aizeronote.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/history")
public class TaskHistoryController {

    private final UserService userService;
    private final TaskHistoryService taskHistoryService;

    public TaskHistoryController(UserService userService, TaskHistoryService taskHistoryService) {
        this.userService = userService;
        this.taskHistoryService = taskHistoryService;
    }

    @GetMapping("/notes")
    public ResponseEntity<Page<NoteJobSummaryDto>> listNotes(
            HttpServletRequest request,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Long userId = userService.getLoginUser(request).getId();
        return ResponseEntity.ok(taskHistoryService.listNotes(userId, pageable));
    }

    @GetMapping("/notes/{noteId}")
    public ResponseEntity<NoteJobDetailDto> getNote(HttpServletRequest request, @PathVariable String noteId) {
        Long userId = userService.getLoginUser(request).getId();
        return taskHistoryService.getNote(userId, noteId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/video-tasks")
    public ResponseEntity<Page<VideoTaskSummaryDto>> listVideoTasks(
            HttpServletRequest request,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Long userId = userService.getLoginUser(request).getId();
        return ResponseEntity.ok(taskHistoryService.listVideoTasks(userId, pageable));
    }

    @GetMapping("/video-tasks/{taskId}")
    public ResponseEntity<VideoTaskDetailDto> getVideoTask(HttpServletRequest request, @PathVariable String taskId) {
        Long userId = userService.getLoginUser(request).getId();
        return taskHistoryService.getVideoTask(userId, taskId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
