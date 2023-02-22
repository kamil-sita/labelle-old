package place.sita.labelle.def;

public record CategoryValueForDisplay(
    CategoryValue categoryValue
) {

    @Override
    public String toString() {
        return categoryValue.displayedValue();
    }
}
