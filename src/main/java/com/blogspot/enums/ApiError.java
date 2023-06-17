package com.blogspot.enums;

public enum ApiError {

    PARAMETER_INVALID,

    ENTITY_NOT_FOUND,

    HEADER_INVALID,

    AUTH_AUTHORIZATION_INVALID,

    AUTH_USER_LOCKED,

    AUTH_SIGNIN_REQUIRED,

    AUTH_SIGNIN_FAILED,

    PERMISSION_DENIED,

    USER_FORBIDDEN,

    USER_NOT_FOUND,

    USER_EMAIL_EXIST,

    RATE_LIMIT,

    TIMEOUT,

    SECURITY_ANTI_SPAM,

    OPERATION_FAILED,

    INTERNAL_SERVER_ERROR;

}