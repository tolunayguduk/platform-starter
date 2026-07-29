package com.platform.user.profile;

public record ChangePasswordRequest(String currentPassword, String newPassword, String confirmNewPassword) {
}