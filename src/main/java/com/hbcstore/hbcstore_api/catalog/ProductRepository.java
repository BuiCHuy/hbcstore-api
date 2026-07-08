package com.hbcstore.hbcstore_api.catalog;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

public interface ProductRepository extends JpaRepository<Product, Long> {
    @Override
    @EntityGraph(attributePaths = {"category", "subcategory", "brand", "productImages", "productAttributes"})
    List<Product> findAll();

    @Override
    @EntityGraph(attributePaths = {"category", "subcategory", "brand", "productImages", "productAttributes"})
    Page<Product> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"category", "subcategory", "brand", "productImages", "productAttributes"})
    Optional<Product> findById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = {"category", "subcategory", "brand", "productImages", "productAttributes"})
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

    @EntityGraph(attributePaths = {"category", "subcategory", "brand", "productImages", "productAttributes"})
    @Query("""
            select p from Product p
            left join p.category c
            left join p.brand b
            where lower(p.name) like lower(concat('%', :keyword, '%'))
               or lower(coalesce(p.description, '')) like lower(concat('%', :keyword, '%'))
               or lower(coalesce(c.name, '')) like lower(concat('%', :keyword, '%'))
               or lower(coalesce(b.name, '')) like lower(concat('%', :keyword, '%'))
            """)
    Page<Product> search(@Param("keyword") String keyword, Pageable pageable);

    @EntityGraph(attributePaths = {"category", "subcategory", "brand", "productImages", "productAttributes"})
    @Query("""
            select distinct p from Product p
            left join p.category c
            left join p.brand b
            where (:keyword is null
                or lower(p.name) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(p.description, '')) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(c.name, '')) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(b.name, '')) like lower(concat('%', :keyword, '%')))
              and (:categoryId is null or c.id = :categoryId)
              and (:subcategoryId is null or p.subcategory.id = :subcategoryId)
              and (:grade is null or exists (
                    select 1 from ProductAttribute pa
                    where pa.product = p
                      and lower(pa.attributeName) = 'grade'
                      and upper(trim(pa.attributeValue)) = upper(trim(:grade))
              ))
            """)
    List<Product> searchWithFilters(
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            @Param("subcategoryId") Long subcategoryId,
            @Param("grade") String grade
    );

    @EntityGraph(attributePaths = {"category", "subcategory", "brand", "productImages", "productAttributes"})
    @Query("""
            select distinct p from Product p
            left join p.category c
            left join p.brand b
            where (:keyword is null
                or lower(p.name) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(p.description, '')) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(c.name, '')) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(b.name, '')) like lower(concat('%', :keyword, '%')))
              and (:categoryId is null or c.id = :categoryId)
              and (:subcategoryId is null or p.subcategory.id = :subcategoryId)
              and (:grade is null or exists (
                    select 1 from ProductAttribute pa
                    where pa.product = p
                      and lower(pa.attributeName) = 'grade'
                      and upper(trim(pa.attributeValue)) = upper(trim(:grade))
              ))
            """)
    Page<Product> searchWithFilters(
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            @Param("subcategoryId") Long subcategoryId,
            @Param("grade") String grade,
            Pageable pageable
    );

    @Query("""
            select upper(trim(pa.attributeValue)), count(distinct p.id)
            from Product p
            join p.productAttributes pa
            where lower(pa.attributeName) = 'grade'
              and (:categoryId is null or p.category.id = :categoryId)
              and p.status <> com.hbcstore.hbcstore_api.catalog.Product.ProductStatus.INACTIVE
            group by upper(trim(pa.attributeValue))
            order by count(distinct p.id) desc
            """)
    List<Object[]> findGradeFacets(@Param("categoryId") Long categoryId);
}
