package place.sita.labelle.def;

import place.sita.labelle.Copiable;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public record Category (
    UUID categoryUuid,
    String name,
    boolean required,
    boolean descriptive,
    List<CategoryValue> categoryValues
) implements Copiable<Category> {

    @Override
    public String toString() {
        return ""
            + name + ", "
            + (required ? "required" : "optional") + ", "
            + (descriptive ? "descriptive" : "plain");
    }


    @Override
    public Category copy() {
        return new Category(
            UUID.randomUUID(),
            name,
            required,
            descriptive,
            categoryValues.stream().map(CategoryValue::copy).collect(Collectors.toList())
        );
    }
}
