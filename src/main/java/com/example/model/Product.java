package com.example.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    private String name;
    private String price;
    private String description;
    private String imageUrl;
    private String availability;
    private String category;
    private String storeName;      // ADD THIS
    private String productUrl;     // ADD THIS

    @Override
    public String toString() {
        return "Product{" +
                "name='" + name + '\'' +
                ", price='" + price + '\'' +
                ", category='" + category + '\'' +
                ", availability='" + availability + '\'' +
                ", storeName='" + storeName + '\'' +
                '}';
    }
}