package com.badat.notificationservice.service.sender;

import com.badat.notificationservice.entity.NotificationMessage;
import com.badat.notificationservice.service.NotificationSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import jakarta.mail.internet.MimeMessage;
import java.util.List;
import java.util.Map;

/**
 * Email Notification Sender Implementation
 * Handles EMAIL channel notifications using Spring JavaMailSender
 */
@Slf4j
public class EmailNotificationSender implements NotificationSender {

    private final JavaMailSender mailSender;
    private final String fromEmail = "dat2801zz@gmail.com"; // Should be configurable

    public EmailNotificationSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public String getChannelType() {
        return NotificationMessage.ChannelType.EMAIL.toString();
    }

    @Override
    public boolean send(String recipient, String title, String content, Map<String, Object> metadata) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(recipient);
            helper.setSubject(title);
            helper.setText(content, true); // true = HTML
            helper.setFrom(fromEmail);

            // Handle metadata (CC, BCC)
            if (metadata != null) {
                // CC
                if (metadata.containsKey("cc")) {
                    Object ccObj = metadata.get("cc");
                    if (ccObj instanceof List) {
                        List<?> ccList = (List<?>) ccObj;
                        helper.setCc(ccList.stream().map(Object::toString).toArray(String[]::new));
                    } else if (ccObj instanceof String[]) {
                        helper.setCc((String[]) ccObj);
                    } else if (ccObj instanceof String) {
                        helper.setCc((String) ccObj);
                    }
                }

                // BCC
                if (metadata.containsKey("bcc")) {
                    Object bccObj = metadata.get("bcc");
                    if (bccObj instanceof List) {
                        List<?> bccList = (List<?>) bccObj;
                        helper.setBcc(bccList.stream().map(Object::toString).toArray(String[]::new));
                    } else if (bccObj instanceof String[]) {
                        helper.setBcc((String[]) bccObj);
                    } else if (bccObj instanceof String) {
                        helper.setBcc((String) bccObj);
                    }
                }
            }

            mailSender.send(message);
            log.info("📧 Email sent successfully to: {}", recipient);
            return true;

        } catch (Exception e) {
            log.error("💥 Failed to send email to {}: {}", recipient, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean isHealthy() {
        try {
            // Check connection to SMTP server
            // Note: JavaMailSender doesn't expose a direct connection check,
            // but we can try to create a MimeMessage to ensure configuration is valid
            mailSender.createMimeMessage();
            return true;
        } catch (Exception e) {
            log.error("❌ Email service health check failed: {}", e.getMessage());
            return false;
        }
    }
}