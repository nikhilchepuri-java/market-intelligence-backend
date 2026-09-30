package com.marketintel.model;

import java.time.LocalDate;

public record IvSkew(
        LocalDate expiration,
        double averageCallIv,
        double averagePutIv,
        double putCallSkew) {}
