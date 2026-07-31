package com.platform.user.controller.model;

public record ChangePasswordRequestDto(String currentPassword, String newPassword, String confirmNewPassword) {
}
