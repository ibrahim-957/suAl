package com.delivery.SuAl.controller;

import com.delivery.SuAl.model.enums.ReceiverType;
import com.delivery.SuAl.model.request.notification.NotificationRequest;
import com.delivery.SuAl.model.response.notification.NotificationResponse;
import com.delivery.SuAl.model.response.wrapper.ApiResponse;
import com.delivery.SuAl.model.response.wrapper.PageResponse;
import com.delivery.SuAl.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/api/notifications")
@RequiredArgsConstructor
@Slf4j
@Validated
public class NotificationController {
    private final NotificationService notificationService;

    @PostMapping
    public ResponseEntity<ApiResponse<NotificationResponse>> createNotification(
            @Valid @RequestBody NotificationRequest request) {
        NotificationResponse response = notificationService.createNotification(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<NotificationResponse>> getNotificationById(@PathVariable Long id) {
        NotificationResponse response = notificationService.getNotificationById(id);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(response));
    }

    @GetMapping("/receiver")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getNotificationsByReceiver(
            @RequestParam ReceiverType receiverType,
            @RequestParam Long receiverId
            ){
        List<NotificationResponse> responses = notificationService.getNotificationsByReceiver(receiverType, receiverId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(responses));
    }

    @GetMapping("/receiver/unread")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getUnreadNotifications(
            @RequestParam ReceiverType receiverType,
            @RequestParam Long receiverId
    ){
        List<NotificationResponse> responses = notificationService.getUnreadNotifications(receiverType, receiverId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(responses));
    }

    @GetMapping("/receiver/paginated")
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> getNotificationsPaginated(
            @RequestParam ReceiverType receiverType,
            @RequestParam Long receiverId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") String direction
    ){
        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        Pageable pageable = PageRequest.of(page, size, sortDirection, sortBy);

        PageResponse<NotificationResponse> pageResponse = notificationService.getNotificationsByReceiverPaginated(receiverType, receiverId, pageable);
        return ResponseEntity.ok(ApiResponse.success(pageResponse));
    }

    @GetMapping("/receiver/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadNotificationsCount(
            @RequestParam ReceiverType receiverType,
            @RequestParam Long receiverId
    ){
        Long count = notificationService.getUnreadCount(receiverType, receiverId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(count));
    }

    @PatchMapping("/{id}/mark-read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(@PathVariable Long id) {
        NotificationResponse response = notificationService.markAsRead(id);
        return  ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(response));
    }

    @PatchMapping("/receiver/mark-all-read")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @RequestParam ReceiverType receiverType,
            @RequestParam Long receiverId) {
        notificationService.markAllAsRead(receiverType, receiverId);
        return  ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(@PathVariable Long id) {
        notificationService.deleteNotification(id);
        return   ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(null));
    }
}
