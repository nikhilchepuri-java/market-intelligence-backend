package com.marketintel.model;

import java.time.LocalDate;

public record IvSurfacePoint(
        LocalDate expiration,
        OptionType optionType,
        double strike,
        double moneyness,
        double impliedVolatility) {}
