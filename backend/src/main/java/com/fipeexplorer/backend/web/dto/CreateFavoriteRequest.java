package com.fipeexplorer.backend.web.dto;

import jakarta.validation.constraints.NotNull;

public record CreateFavoriteRequest(
        @NotNull Long priceEntryId
) {
}
