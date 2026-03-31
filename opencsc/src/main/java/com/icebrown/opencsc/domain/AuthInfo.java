package com.icebrown.opencsc.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthInfo {
    private String mode;
    private String expression;
    private List<AuthObjectType> objects;
}
