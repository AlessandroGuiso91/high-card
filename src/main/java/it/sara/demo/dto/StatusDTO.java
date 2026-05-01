package it.sara.demo.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Status envelope carried by every response and error body: numeric code,
 * human-readable message, and a per-request traceId for log correlation.
 */
@Getter
@Setter
public class StatusDTO {
    private int code;
    private String message;
    private String traceId;
}
