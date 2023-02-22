package place.sita.labelle.def;

import java.util.UUID;

public record ImageCategoriesValue(
    UUID imageCategoriesValueUuid,
    UUID categoryUuid,
    UUID categoryValueUuid,
    String descriptiveModifier
) {
}
