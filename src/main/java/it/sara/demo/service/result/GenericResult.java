package it.sara.demo.service.result;

import lombok.Getter;
import lombok.Setter;

/**
 * Marker base type for service-layer results. Anchors the layer boundary so
 * the web tier can be sure no concrete service result type leaks past the service.
 */
@Getter
@Setter
public class GenericResult {
}
