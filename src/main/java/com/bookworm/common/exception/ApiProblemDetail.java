package com.bookworm.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiProblemDetail {

    @Builder.Default
    private Instant timestamp = Instant.now();

    private Integer status;

    private String errorCode;

    private String title;

    private String message;

    private String path;

    private URI type;

    private String traceId;

    @Builder.Default
    private List<FieldErrorDto> errors = new ArrayList<>();

    public void addFieldError(String field, Object rejectedValue, String message) {
        if (this.errors == null) {
            this.errors = new ArrayList<>();
        }
        this.errors.add(FieldErrorDto.builder()
                .field(field)
                .rejectedValue(rejectedValue)
                .message(message)
                .build());
    }
}
