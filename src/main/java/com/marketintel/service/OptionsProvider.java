package com.marketintel.service;

import com.marketintel.model.OptionChain;

import java.time.LocalDate;
import java.util.List;

public interface OptionsProvider {
    List<LocalDate> expirations(String underlying);
    OptionChain chain(String underlying, LocalDate expiration);
}
