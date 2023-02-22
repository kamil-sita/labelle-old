package place.sita.labelle.dreambooth;

public record Concept(
    String instance_prompt,
    String class_prompt,
    String instance_data_dir,
    String class_data_dir
) {
}
