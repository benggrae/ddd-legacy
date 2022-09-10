package kitchenpos.fake;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import kitchenpos.domain.Product;
import kitchenpos.domain.ProductRepository;

public class InMemoryProductRepository implements ProductRepository {
    Map<UUID, Product> database = new HashMap<>();

    @Override
    public List<Product> findAllByIdIn(List<UUID> ids) {
        return ids.stream()
                .map((id) -> database.get(id))
                .collect(Collectors.toList());
    }

    @Override
    public Product save(Product product) {
        product.setId(UUID.randomUUID());
        database.put(product.getId(), product);
        return database.get(product.getId());
    }

    @Override
    public Optional<Product> findById(UUID productId) {
        return Optional.ofNullable(database.get(productId));
    }

    @Override
    public List<Product> findAll() {
        return new ArrayList<>(database.values());
    }
}
