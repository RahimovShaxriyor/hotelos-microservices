package com.hotelos.identity.exception;

import org.springframework.http.HttpStatus;

public class IdentityBusinessException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    public IdentityBusinessException(String code, HttpStatus status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static IdentityBusinessException userNotFound(String message) {
        return new IdentityBusinessException("USER_NOT_FOUND", HttpStatus.NOT_FOUND, message);
    }

    public static IdentityBusinessException userAlreadyExists(String message) {
        return new IdentityBusinessException("USER_ALREADY_EXISTS", HttpStatus.CONFLICT, message);
    }

    public static IdentityBusinessException invitationNotFound(String message) {
        return new IdentityBusinessException("INVITATION_NOT_FOUND", HttpStatus.NOT_FOUND, message);
    }

    public static IdentityBusinessException invitationNotActive(String message) {
        return new IdentityBusinessException("INVITATION_NOT_ACTIVE", HttpStatus.CONFLICT, message);
    }

    public static IdentityBusinessException invitationAlreadyActive(String message) {
        return new IdentityBusinessException("INVITATION_ALREADY_ACTIVE", HttpStatus.CONFLICT, message);
    }

    public static IdentityBusinessException invitationAlreadyAccepted(String message) {
        return new IdentityBusinessException("INVITATION_ALREADY_ACCEPTED", HttpStatus.CONFLICT, message);
    }

    public static IdentityBusinessException invalidOrExpiredInvitation(String message) {
        return new IdentityBusinessException("INVALID_OR_EXPIRED_INVITATION", HttpStatus.BAD_REQUEST, message);
    }

    public static IdentityBusinessException roleNotAssignable(String message) {
        return new IdentityBusinessException("ROLE_NOT_ASSIGNABLE", HttpStatus.BAD_REQUEST, message);
    }

    public static IdentityBusinessException cannotDisableSelf(String message) {
        return new IdentityBusinessException("CANNOT_DISABLE_SELF", HttpStatus.BAD_REQUEST, message);
    }

    public static IdentityBusinessException cannotDemoteSelf(String message) {
        return new IdentityBusinessException("CANNOT_DEMOTE_SELF", HttpStatus.BAD_REQUEST, message);
    }

    public static IdentityBusinessException cannotDisableLastOwner(String message) {
        return new IdentityBusinessException("CANNOT_DISABLE_LAST_OWNER", HttpStatus.CONFLICT, message);
    }

    public static IdentityBusinessException cannotDisableLastAdmin(String message) {
        return new IdentityBusinessException("CANNOT_DISABLE_LAST_ADMIN", HttpStatus.CONFLICT, message);
    }

    public static IdentityBusinessException cannotRemoveLastOwner(String message) {
        return new IdentityBusinessException("CANNOT_REMOVE_LAST_OWNER", HttpStatus.CONFLICT, message);
    }

    public static IdentityBusinessException cannotRemoveLastAdmin(String message) {
        return new IdentityBusinessException("CANNOT_REMOVE_LAST_ADMIN", HttpStatus.CONFLICT, message);
    }

    public static IdentityBusinessException accountAlreadyDisabled(String message) {
        return new IdentityBusinessException("ACCOUNT_ALREADY_DISABLED", HttpStatus.CONFLICT, message);
    }

    public static IdentityBusinessException accountAlreadyActive(String message) {
        return new IdentityBusinessException("ACCOUNT_ALREADY_ACTIVE", HttpStatus.CONFLICT, message);
    }

    public static IdentityBusinessException accessDenied(String message) {
        return new IdentityBusinessException("ACCESS_DENIED", HttpStatus.FORBIDDEN, message);
    }
}
