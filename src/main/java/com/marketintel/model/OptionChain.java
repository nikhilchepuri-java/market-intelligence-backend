package com.marketintel.model;

import java.time.LocalDate;
import java.util.List;

public record OptionChain(
        String underlying,
        LocalDate expiration,
        List<OptionContract> contracts) {
}
