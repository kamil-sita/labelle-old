package place.sita.labelle.def;

import place.sita.labelle.Copiable;

import java.util.UUID;

public record CategoryValue(
    UUID categoryValueUuid,
    String displayedValue,
    String taughtValue,
    String counterTaughtValue
) implements Copiable<CategoryValue> {
    @Override
    public String toString() {
        return displayedValue + " (" + taughtValue + '/' + counterTaughtValue + ")";
    }

    @Override
    public CategoryValue copy() {
        return new CategoryValue(
            UUID.randomUUID(),
            displayedValue,
            taughtValue,
            counterTaughtValue
        );
    }
}
