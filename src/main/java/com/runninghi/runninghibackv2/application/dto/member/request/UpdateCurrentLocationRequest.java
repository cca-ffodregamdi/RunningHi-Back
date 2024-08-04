package com.runninghi.runninghibackv2.application.dto.member.request;

import lombok.Getter;

@Getter
public record UpdateCurrentLocationRequest(
        double latitude,
        double longitude
) {
}
