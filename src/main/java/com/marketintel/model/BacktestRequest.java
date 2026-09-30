package com.marketintel.model;
import jakarta.validation.constraints.*;
import java.time.Instant;
public record BacktestRequest(@NotBlank String symbol,@NotNull Instant from,@NotNull Instant to,@Positive double initialCapital,@Positive double positionFraction,@PositiveOrZero double transactionCostBps,@NotBlank String strategy) {}