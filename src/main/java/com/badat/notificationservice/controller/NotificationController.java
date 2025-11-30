package com.badat.notificationservice.controller;

import com.badat.notificationservice.dto.ApiResponse;
import com.badat.notificationservice.service.EmailTaskService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class NotificationController {
    EmailTaskService emailTaskService;

    @PostMapping("/send")
    public ApiResponse<Void> sendNotification() {
        emailTaskService.send();
        return ApiResponse.<Void>builder().build();
    }
}
