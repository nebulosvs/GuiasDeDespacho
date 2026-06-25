package com.example.bdget.service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.bdget.dto.DispatchGuideRequestDto;
import com.example.bdget.dto.DispatchGuideResponseDto;
import com.example.bdget.dto.DispatchGuideUpdateDto;
import com.example.bdget.exception.ResourceNotFoundException;
import com.example.bdget.model.DispatchGuide;
import com.example.bdget.repository.DispatchGuideRepository;

@Service
public class DispatchGuideServiceImpl implements DispatchGuideService {

    @Autowired
    private DispatchGuideRepository dispatchGuideRepository;

    @Autowired
    private DispatchGuideFileService guideFileService;

    @Autowired
    private EfsStorageService efsStorageService;

    @Autowired
    private AwsService awsService;

    @Value("${cloud.aws.s3.bucket-name:}")
    private String bucketName;

    // =========================
    // CREATE
    // =========================
    @Override
    @Transactional
    public DispatchGuideResponseDto createGuide(DispatchGuideRequestDto request) {

        DispatchGuide guide = new DispatchGuide();
        guide.setTransportista(request.getTransportista().trim());
        guide.setFecha(request.getFecha());
        guide.setPedidoId(request.getPedidoId().trim());
        guide.setOrigen(request.getOrigen().trim());
        guide.setDestino(request.getDestino().trim());
        guide.setDescripcion(request.getDescripcion());

        guide.setFileName("PENDIENTE");

        guide = dispatchGuideRepository.save(guide);

        guide.setFileName(guideFileService.buildFileName(guide.getId())); // .txt

        String efsPath = guideFileService.buildEfsRelativePath(guide);

        byte[] content = guideFileService.generateGuideContent(guide); // TXT

        efsStorageService.writeFile(efsPath, content);

        guide.setEfsPath(efsPath);
        guide.setS3Key(guideFileService.buildS3Key(guide));

        guide = dispatchGuideRepository.save(guide);

        return toResponse(guide);
    }

    // =========================
    // UPLOAD S3
    // =========================
    @Override
    @Transactional
    public DispatchGuideResponseDto uploadGuideToS3(Long id) {

        validateBucketConfigured();

        DispatchGuide guide = findGuideOrThrow(id);

        if (!efsStorageService.fileExists(guide.getEfsPath())) {
            throw new ResourceNotFoundException("La guia no existe en EFS: " + guide.getEfsPath());
        }

        byte[] content = efsStorageService.readFile(guide.getEfsPath());

        awsService.uploadFile(
                bucketName,
                guide.getS3Key(),
                content,
                "text/plain"
        );

        guide.setUploadedToS3(true);
        guide = dispatchGuideRepository.save(guide);

        return toResponse(guide);
    }

    // =========================
    // DOWNLOAD
    // =========================
    @Override
    public byte[] downloadGuide(Long id) {

        DispatchGuide guide = findGuideOrThrow(id);

        if (guide.isUploadedToS3()) {

            validateBucketConfigured();

            if (!awsService.objectExists(bucketName, guide.getS3Key())) {
                throw new ResourceNotFoundException("Guia no encontrada en S3: " + guide.getS3Key());
            }

            return awsService.downloadS3File(bucketName, guide.getS3Key());
        }

        if (!efsStorageService.fileExists(guide.getEfsPath())) {
            throw new ResourceNotFoundException("Guia no encontrada en EFS: " + guide.getEfsPath());
        }

        return efsStorageService.readFile(guide.getEfsPath());
    }

    // =========================
    // UPDATE
    // =========================
    @Override
    @Transactional
    public DispatchGuideResponseDto updateGuide(Long id, DispatchGuideUpdateDto request) {

        DispatchGuide guide = findGuideOrThrow(id);

        if (request.getOrigen() != null && !request.getOrigen().isBlank()) {
            guide.setOrigen(request.getOrigen().trim());
        }

        if (request.getDestino() != null && !request.getDestino().isBlank()) {
            guide.setDestino(request.getDestino().trim());
        }

        if (request.getDescripcion() != null) {
            guide.setDescripcion(request.getDescripcion());
        }

        byte[] updatedContent = guideFileService.generateGuideContent(guide);

        efsStorageService.writeFile(guide.getEfsPath(), updatedContent);

        if (guide.isUploadedToS3()) {
            validateBucketConfigured();

            awsService.uploadFile(
                    bucketName,
                    guide.getS3Key(),
                    updatedContent,
                    "text/plain"
            );
        }

        guide = dispatchGuideRepository.save(guide);

        return toResponse(guide);
    }

    // =========================
    // DELETE
    // =========================
    @Override
    @Transactional
    public void deleteGuide(Long id) {

        DispatchGuide guide = findGuideOrThrow(id);

        if (guide.isUploadedToS3()) {

            validateBucketConfigured();

            if (awsService.objectExists(bucketName, guide.getS3Key())) {
                awsService.deleteObject(bucketName, guide.getS3Key());
            }
        }

        if (guide.getEfsPath() != null) {
            efsStorageService.deleteFile(guide.getEfsPath());
        }

        dispatchGuideRepository.delete(guide);
    }

    // =========================
    // HISTORY
    // =========================
    @Override
    public List<DispatchGuideResponseDto> getGuideHistory(String transportista, LocalDate fecha) {

        List<DispatchGuide> guides;

        if (transportista != null && !transportista.isBlank() && fecha != null) {
            guides = dispatchGuideRepository.findByTransportistaAndFechaOrderByCreatedAtDesc(
                    transportista.trim(), fecha);

        } else if (transportista != null && !transportista.isBlank()) {
            guides = dispatchGuideRepository.findByTransportistaOrderByCreatedAtDesc(transportista.trim());

        } else if (fecha != null) {
            guides = dispatchGuideRepository.findByFechaOrderByCreatedAtDesc(fecha);

        } else {
            guides = dispatchGuideRepository.findAll();
        }

        return guides.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // =========================
    // HELPERS
    // =========================
    private DispatchGuide findGuideOrThrow(Long id) {
        return dispatchGuideRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Guia de despacho no encontrada con id: " + id));
    }

    private void validateBucketConfigured() {
        if (bucketName == null || bucketName.isBlank()) {
            throw new RuntimeException("El bucket S3 no esta configurado. Defina AWS_S3_BUCKET_NAME.");
        }
    }

    private DispatchGuideResponseDto toResponse(DispatchGuide guide) {
        DispatchGuideResponseDto response = new DispatchGuideResponseDto();
        response.setId(guide.getId());
        response.setTransportista(guide.getTransportista());
        response.setFecha(guide.getFecha());
        response.setPedidoId(guide.getPedidoId());
        response.setOrigen(guide.getOrigen());
        response.setDestino(guide.getDestino());
        response.setDescripcion(guide.getDescripcion());
        response.setFileName(guide.getFileName());
        response.setS3Key(guide.getS3Key());
        response.setEfsPath(guide.getEfsPath());
        response.setUploadedToS3(guide.isUploadedToS3());
        response.setCreatedAt(guide.getCreatedAt());
        response.setUpdatedAt(guide.getUpdatedAt());
        return response;
    }
}