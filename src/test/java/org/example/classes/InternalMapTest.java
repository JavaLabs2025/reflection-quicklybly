package org.example.classes;

import java.util.Map;
import org.example.generator.Generatable;

@Generatable
public class InternalMapTest {
    private Map<Product, String> items;

    public Map<Product, String> getItems() {
        return items;
    }

    public void setItems(Map<Product, String> items) {
        this.items = items;
    }
}
