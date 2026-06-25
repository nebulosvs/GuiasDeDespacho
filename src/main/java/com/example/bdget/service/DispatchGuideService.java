package com.example.bdget.service;

import java.time.LocalDate;
import java.util.List;

import com.example.bdget.dto.DispatchGuideRequestDto;
import com.example.bdget.dto.DispatchGuideResponseDto;
import com.example.bdget.dto.DispatchGuideUpdateDto;

public interface DispatchGuideService {

    DispatchGuideResponseDto createGuide(DispatchGuideRequestDto request);

    DispatchGuideResponseDto uploadGuideToS3(Long id);

    byte[] downloadGuide(Long id);

    DispatchGuideResponseDto updateGuide(Long id, DispatchGuideUpdateDto request);

    void deleteGuide(Long id);

    List<DispatchGuideResponseDto> getGuideHistory(String transportista, LocalDate fecha);
}
