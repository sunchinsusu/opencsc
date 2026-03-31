package com.icebrown.opencsc.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KeyInfo {
    private String status;
    private List<String> algo;
    private Integer len;
    private String curve;
}
