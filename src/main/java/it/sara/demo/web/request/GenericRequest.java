package it.sara.demo.web.request;

import lombok.Getter;
import lombok.Setter;

/**
 * Marker base type for inbound web payloads. Anchors the layer boundary so the
 * service tier can be sure no concrete request type leaks past the web layer.
 */
@Getter
@Setter
public class GenericRequest {
}
