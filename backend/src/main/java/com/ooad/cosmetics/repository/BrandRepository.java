package com.ooad.cosmetics.repository;

import com.ooad.cosmetics.entity.Brand;
import com.ooad.cosmetics.entity.CatalogStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BrandRepository extends JpaRepository<Brand, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    List<Brand> findByStatusOrderByNameAsc(CatalogStatus status);
}
