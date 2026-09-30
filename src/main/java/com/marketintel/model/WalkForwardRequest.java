package com.marketintel.model;
import jakarta.validation.constraints.*;
public record WalkForwardRequest(@NotBlank String symbol,@Min(30) int trainingSize,@Min(5) int testSize,@Min(1) int step,@Min(2) int folds,@Positive double learningRate,@Min(1) int epochs) {}