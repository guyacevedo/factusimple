package com.guyacevedo.factusimple.features.auth.service;

import com.guyacevedo.factusimple.features.user.entity.User;

/**
 * Delivers the password reset link. The raw token only leaves the server
 * through here (by email), never in an API response.
 *
 * In production there must be an implementation that really sends email
 * (Spring Mail, Resend, SES...). Without one the app does not start, on purpose.
 */
public interface PasswordResetMailer {

    void send(User user, String rawToken);
}