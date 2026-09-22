package com.kovanlabs.wellness.service;

public interface InactivityDetectionService {

    /**
     * Called whenever a step/activity sync occurs from Health Connect.
     * Checks if actual step count increased (real physical movement).
     * If real steps increased, resets inactivity timer and notificationSent flag.
     */
    void registerStepSync(Long userId, long currentSteps);

    /**
     * Evaluates inactivity condition for a user and triggers Spring AI + WebSocket STOMP
     * if inactivity threshold is met and notification has not been sent.
     */
    void evaluateInactivityForUser(Long userId);

    /**
     * Scheduled background check across all registered users.
     */
    void checkAllUsersInactivity();
}
