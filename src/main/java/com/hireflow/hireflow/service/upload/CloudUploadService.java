package com.hireflow.hireflow.service.upload;

import com.hireflow.hireflow.dto.response.CloudinarySignatureResponse;

public interface CloudUploadService {

    /**
     * Generate a short-lived, signed upload authorisation.
     * The client uses the returned parameters to upload a file directly to the
     * cloud provider — the backend never receives the file itself.
     *
     * Implementations: CloudinaryUploadServiceImpl (Cloudinary), ...
     */
    CloudinarySignatureResponse generatePdfUploadSignature();
}
