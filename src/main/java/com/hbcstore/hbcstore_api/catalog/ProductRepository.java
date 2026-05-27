package com.hbcstore.hbcstore_api.catalog;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {
    @Override
    @EntityGraph(attributePaths = {"category", "brand", "productImages", "productAttributes"})
    List<Product> findAll();

    @Override
    @EntityGraph(attributePaths = {"category", "brand", "productImages", "productAttributes"})
    Optional<Product> findById(Long id);

    @EntityGraph(attributePaths = {"category", "brand", "productImages", "productAttributes"})
    @Query("""
            select p from Product p
            left join p.category c
            left join p.brand b
            where lower(p.name) like lower(concat('%', :keyword, '%'))
               or lower(coalesce(p.description, '')) like lower(concat('%', :keyword, '%'))
               or lower(coalesce(c.name, '')) like lower(concat('%', :keyword, '%'))
               or lower(coalesce(b.name, '')) like lower(concat('%', :keyword, '%'))
            """)
    List<Product> search(@Param("keyword") String keyword);
}
