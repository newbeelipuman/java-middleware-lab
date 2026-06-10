package study.middleware.rediscache;

import java.math.BigDecimal;

public record UpdateProductRequest(String name, BigDecimal price) {
}

