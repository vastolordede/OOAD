package com.ooad.cosmetics.service;

import com.ooad.cosmetics.entity.CatalogStatus;
import com.ooad.cosmetics.entity.Product;
import com.ooad.cosmetics.entity.ProductVariant;

/**
 * Quy tắc "có được bán hay không", dùng chung cho Cart và Checkout.
 * Một variant chỉ được bán khi variant, product, category và brand đều ACTIVE.
 */
public final class VariantAvailability {

    private VariantAvailability() {
    }

    public static boolean isSellable(ProductVariant variant) {
        Product product = variant.getProduct();
        return variant.getStatus() == CatalogStatus.ACTIVE
                && product.getStatus() == CatalogStatus.ACTIVE
                && product.getCategory().getStatus() == CatalogStatus.ACTIVE
                && product.getBrand().getStatus() == CatalogStatus.ACTIVE;
    }
}
