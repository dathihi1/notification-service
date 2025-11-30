package com.badat.notificationservice.service;

import com.badat.notificationservice.entity.EmailTask;
import com.badat.notificationservice.repository.EmailTaskRepository;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class EmailTaskService {
    JavaMailSender mailSender;
    EmailTaskRepository emailTaskRepository;

    // 1. Hàm nhận request (Producer) -> Lưu vào DB
    public void scheduleEmail(String recipient, String subject, String body) {
        EmailTask task = EmailTask.builder()
                .recipient(recipient)
                .subject(subject)
                .body(body)
                .status(EmailTask.Status.PENDING)
                .retryCount(0)
                .build();
        emailTaskRepository.save(task);
        log.info("Đã lưu email task cho: {}", recipient);
    }

    // 2. Cronjob xử lý (Consumer)
    // Chạy mỗi 5 giây (5000ms). Transactional bắt buộc để giữ Lock DB
    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void processEmailQueue() {
        // Lấy danh sách task và KHÓA dòng
        List<EmailTask> tasks = emailTaskRepository.findPendingTasksWithLock();

        if (tasks.isEmpty()) {
            return;
        }

        log.info("Tìm thấy {} task cần xử lý...", tasks.size());

        for (EmailTask task : tasks) {
            try {
                // Gửi mail thực tế
                sendEmail(task.getRecipient(), task.getSubject(), task.getBody());

                // Cập nhật trạng thái thành công
                task.setStatus(EmailTask.Status.SENT);

            } catch (Exception e) {
                log.error("Lỗi gửi mail id {}: {}", task.getId(), e.getMessage());
                // Xử lý lỗi: Tăng retry hoặc đánh dấu FAILED
                task.setRetryCount(task.getRetryCount() + 1);
                if (task.getRetryCount() >= 3) {
                    task.setStatus(EmailTask.Status.FAILED);
                } else {
                    task.setStatus(EmailTask.Status.RETRY); // Để lần sau quét lại (nếu bạn sửa query)
                }
            }
            // Repository sẽ tự động update status xuống DB khi transaction kết thúc
        }
    }

    private void sendEmail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
        log.info("Đã gửi mail tới: {}", to);
    }

    public void send() {
        for(int i = 0; i < 15; i++){
            String email = "dung171120" + i + "@gmail.com";
            scheduleEmail(email, "Test", "Test mail");
        }
    }
}
