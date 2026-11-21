package com.interview.shortlink.dto;

import com.interview.shortlink.domain.LinkStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeStatusRequest(@NotNull LinkStatus status) {
}
