package com.icebrown.opencsc.model.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignHashResponse {
    private List<String> signatures;
    private String responseID;
}
