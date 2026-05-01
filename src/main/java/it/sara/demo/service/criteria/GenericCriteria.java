package it.sara.demo.service.criteria;

import lombok.Getter;
import lombok.Setter;

/**
 * Marker base type for service-layer input criteria. Web request DTOs are
 * translated into {@code Criteria} subclasses by the assemblers.
 */
@Getter
@Setter
public class GenericCriteria {
}
