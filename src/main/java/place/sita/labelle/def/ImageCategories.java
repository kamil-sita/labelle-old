package place.sita.labelle.def;

import java.util.List;
import java.util.UUID;

public record ImageCategories(
    UUID imageCategoryUuid,
    String path,
    List<ImageCategoriesValue> imageCategoriesValues
) {

    @Override
    public String toString() {
        return path;
    }
}
