package com.ooad.cosmetics.repository;

import com.ooad.cosmetics.entity.CatalogStatus;
import com.ooad.cosmetics.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    List<Category> findByStatusOrderByNameAsc(CatalogStatus status);
}
