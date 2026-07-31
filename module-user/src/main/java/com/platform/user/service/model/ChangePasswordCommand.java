package com.platform.user.service.model;

public record ChangePasswordCommand(String currentPassword, String newPassword, String confirmNewPassword) {
}
