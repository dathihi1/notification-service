package com.badat.notificationservice.service;
/**
 * Strategy Pattern Interface for Multi-Channel Notification Senders
 * Each implementation handles a specific notification channel
 */
public interface NotificationSender {

    /**
     * Get the channel type this sender handles
     */
    String getChannelType();

    /**
     * Send notification through the specific channel
     *
     * @param recipient - target address (email, phone, token, URL, user ID)
     * @param title - notification title
     * @param content - notification content/message
     * @param metadata - additional channel-specific data
     * @return true if sent successfully, false otherwise
     */
    boolean send(String recipient, String title, String content, java.util.Map<String, Object> metadata);

    /**
     * Health check for the channel
     * @return true if channel is available and functioning
     */
    default boolean isHealthy() {
        return true; // Default implementation
    }
}