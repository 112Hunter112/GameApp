package com.parth.sportsapp.sportsbackend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;

/**
 * Serialize Spring Data {@code Page} responses through a stable DTO envelope
 * ({@code PagedModel}) instead of the raw {@code PageImpl}. The raw form has no
 * guaranteed JSON structure across Spring versions; VIA_DTO pins it down so the
 * mobile client can rely on a consistent shape.
 *
 * Response shape becomes:
 * {
 *   "content": [ ... ],
 *   "page": { "size": 20, "number": 0, "totalElements": 3, "totalPages": 1 }
 * }
 */
@Configuration
@EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.VIA_DTO)
public class WebConfig {
}
