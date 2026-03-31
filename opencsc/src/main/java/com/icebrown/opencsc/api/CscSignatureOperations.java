package com.icebrown.opencsc.api;

import com.icebrown.opencsc.model.request.SignHashRequest;
import com.icebrown.opencsc.model.response.SignHashResponse;

public interface CscSignatureOperations {
    SignHashResponse signHash(SignHashRequest request);
}
