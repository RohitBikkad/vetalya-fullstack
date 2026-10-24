package com.pet.vetalya.dto;

public record CodeExchangeRequest(
        String code,
        String deviceId
) {}
