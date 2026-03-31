package com.icebrown.opencsc.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthObjectType {
    private String type;
    private String id;
    private String label;
    private String description;
    private String format;
    private String generator;
}
