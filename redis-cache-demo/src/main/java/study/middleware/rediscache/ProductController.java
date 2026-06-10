package study.middleware.rediscache;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> find(@PathVariable long id) {
        return ResponseEntity.of(productService.find(id));
    }

    @PutMapping("/{id}")
    public Product update(@PathVariable long id, @RequestBody UpdateProductRequest request) {
        return productService.update(id, request);
    }
}

