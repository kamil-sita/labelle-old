package place.sita.labelle;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.formdev.flatlaf.FlatIntelliJLaf;
import net.miginfocom.layout.AC;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.io.FileUtils;
import org.imgscalr.Scalr;
import place.sita.labelle.def.*;
import place.sita.labelle.dreambooth.Concept;
import place.sita.labelle.swing.CopyValueFromBeforeHiperlist;
import place.sita.labelle.swing.HiperList;
import place.sita.labelle.swing.SimpleUpdateKeyListener;
import place.sita.labelle.swing.ValueCopiableHiperlist;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Gui {

    private Categories categories = new Categories(new ArrayList<>());
    private ImagesCategories imagesCategories = new ImagesCategories(new ArrayList<>());


    private JButton addCategory = new JButton("Add category");
    private JButton deleteCategory = new JButton("Delete category");
    private JButton copyCategoryButton = new JButton("Copy category [MANUALLY DISABLED]");
    private JButton saveCategoryButton = new JButton("Save category");


    private JButton addCategoryValue = new JButton("Add category value");
    private JButton deleteCategoryValue = new JButton("Delete category value");
    private JButton copyCategoryValueButton = new JButton("Copy category value");
    private JButton saveCategoryValueButton = new JButton("Save category value");

    //
    private JTextField categoryName = new JTextField();
    private JCheckBox isRequired = new JCheckBox();
    private JCheckBox isDescriptive = new JCheckBox();

    //
    private JTextField categoryValueDisplayedName = new JTextField();
    private JTextField categoryValueTaughtName = new JTextField();
    private JTextField categoryValueCounterTaughtName = new JTextField();

    private JFrame parent;

    private HiperList<Category> categoryHiperList = new ValueCopiableHiperlist<Category>(
        addCategory,
        deleteCategory,
        saveCategoryButton,
        copyCategoryButton
    ) {
        @Override
        protected List<Category> getBackingList() {
            return categories.categories();
        }

        @Override
        protected IntFunction<Category[]> generator() {
            return Category[]::new;
        }

        @Override
        protected void onSelected(Category selected) {
            onCategorySelected(selected);
            categoryValueHiperList.sync();
        }

        @Override
        protected Category defaultTFactory(int probablyUniqueId) {
            return new Category(UUID.randomUUID(), "New category " + probablyUniqueId, false, true, new ArrayList<>());
        }

        @Override
        protected Category updatedTValue(Category currentT) {
            return new Category(
                currentT.categoryUuid(),
                categoryName.getText(),
                isRequired.isSelected(),
                isDescriptive.isSelected(),
                currentT.categoryValues()
            );
        }

        @Override
        public void onChange() {
            syncCategoriesToImageCategories();
        }
    };

    private HiperList<CategoryValue> categoryValueHiperList = new ValueCopiableHiperlist<>(
        addCategoryValue,
        deleteCategoryValue,
        saveCategoryValueButton,
        copyCategoryValueButton
    ) {
        @Override
        protected List<CategoryValue> getBackingList() {
            Category selected = categoryHiperList.getCurrentT();
            if (selected != null) {
                return selected.categoryValues();
            }
            return null;
        }

        @Override
        protected IntFunction<CategoryValue[]> generator() {
            return CategoryValue[]::new;
        }

        @Override
        protected void onSelected(CategoryValue selected) {
            onCategoryValueSelected(selected);
        }

        @Override
        protected CategoryValue defaultTFactory(int probablyUniqueId) {
            return new CategoryValue(
                UUID.randomUUID(),
                "displayed " + probablyUniqueId,
                "taught",
                "countertaught"
            );
        }

        @Override
        protected CategoryValue updatedTValue(CategoryValue currentT) {
            return new CategoryValue(
                currentT.categoryValueUuid(),
                categoryValueDisplayedName.getText(),
                categoryValueTaughtName.getText(),
                categoryValueCounterTaughtName.getText()
            );
        }

        @Override
        public void onChange() {
            syncCategoriesToImageCategories();
        }
    };

    public void run() throws IOException, UnsupportedLookAndFeelException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        FlatIntelliJLaf.setup();

        JFrame frame = new JFrame();
        parent = frame;

        frame.setTitle("Labelle");

        frame.setLocationRelativeTo(null);
        frame.setSize(1600, 900);
        frame.setVisible(true);

        JTabbedPane tabbedPane = new JTabbedPane();
        frame.setContentPane(tabbedPane);
        tabbedPane.addTab("Import/Export", makeImportExport());
        tabbedPane.addTab("Class designer", makeClassDesigner());
        tabbedPane.addTab("File preprocessor", makeFilePreprocessor());
        tabbedPane.addTab("Categorizer", makeCategorizer());
        tabbedPane.addTab("Dreambooth exporter", makeDreamboothTab());

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    private JComponent makeImportExport() {
        JPanel panel = new JPanel(false);

        MigLayout migLayout = new MigLayout(
            new LC(),
            new AC().size("80lp:80lp:80lp", 0).size("80lp:200:240lp", 1),
            new AC().size("20lp:20lp:20lp", 0, 1, 2)
        );

        panel.setLayout(migLayout);

        JButton importCatButton = new JButton("Import categories");
        importCatButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                int result = fileChooser.showOpenDialog(parent);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    try {
                        categories = new ObjectMapper().readValue(selectedFile, Categories.class);
                        syncCategoriesToImageCategories();
                        categoryHiperList.sync();
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        });
        JButton exportCatButton = new JButton("Export categories");
        exportCatButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                int result = fileChooser.showSaveDialog(parent);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    try {
                        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(selectedFile, categories);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        });

        JButton importImgButton = new JButton("Import categorized images");
        importImgButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                int result = fileChooser.showOpenDialog(parent);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    try {
                        imagesCategories = new ObjectMapper().readValue(selectedFile, ImagesCategories.class);
                        syncCategoriesToImageCategories();
                        imageCategoriesHiperList.sync();
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        });
        JButton exportImgButton = new JButton("Export categorized images");
        exportImgButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                int result = fileChooser.showSaveDialog(parent);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    try {
                        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(selectedFile, imagesCategories);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        });

        panel.add(importCatButton, new CC().wrap());
        panel.add(exportCatButton, new CC().wrap());
        panel.add(importImgButton, new CC().wrap());
        panel.add(exportImgButton, new CC().wrap());

        return panel;
    }

    private Map<UUID, Category> categoryCache = new HashMap<>();
    private Map<UUID, CategoryValue> categoryValueCache = new HashMap<>();
    private List<UUID> categoryOrder = new ArrayList<>();

    private void syncCategoriesToImageCategories() {
        System.out.println("resyncing");
        // restore cache
        recreateCache();

        //
        syncImageCategoriesValue();

        //
        buildGuiForCategorizer();

        rebuildListRunnable.run();
    }

    private void recreateCache() {
        categoryCache.clear();
        categoryValueCache.clear();
        categoryOrder.clear();

        categories.categories()
            .forEach(category -> {
                categoryOrder.add(category.categoryUuid());
                categoryCache.put(category.categoryUuid(), category);
                category.categoryValues()
                    .forEach(categoryValue -> {
                            categoryValueCache.put(categoryValue.categoryValueUuid(), categoryValue);
                        }
                    );
            });
    }

    private void syncImageCategoriesValue() {
        imagesCategories.imageCategories()
            .forEach(imageCategories -> {
                Map<UUID, ImageCategoriesValue> previousValuesCache = new HashMap<>();
                imageCategories.imageCategoriesValues().forEach(imageCategoriesValue -> {
                    previousValuesCache.put(imageCategoriesValue.categoryUuid(), imageCategoriesValue);
                });

                imageCategories.imageCategoriesValues().clear();
                categoryOrder.forEach(categoryUuid -> {
                    Category category = categoryCache.get(categoryUuid);

                    if (previousValuesCache.containsKey(categoryUuid)) {
                        ImageCategoriesValue value = previousValuesCache.get(categoryUuid);
                        if (categoryValueCache.containsKey(value.categoryValueUuid())) {
                            imageCategories.imageCategoriesValues().add(value);
                        } else {
                            imageCategories.imageCategoriesValues().add(
                                new ImageCategoriesValue(
                                    value.imageCategoriesValueUuid(),
                                    value.categoryUuid(),
                                    value.categoryValueUuid(),
                                    value.descriptiveModifier()
                                )
                            );
                        }
                    } else {
                        if (category.required() && category.categoryValues().isEmpty()) {
                            throw new RuntimeException("Required category cannot be empty");
                        }
                        if (!category.required()) {
                            imageCategories.imageCategoriesValues().add(new ImageCategoriesValue(
                                UUID.randomUUID(),
                                categoryUuid,
                                null,
                                "[]"
                            ));
                        } else {
                            imageCategories.imageCategoriesValues().add(new ImageCategoriesValue(
                                UUID.randomUUID(),
                                categoryUuid,
                                category.categoryValues().get(0).categoryValueUuid(),
                                "[]"
                            ));
                        }
                    }
                });
            });
    }



    private boolean lastState;
    private Runnable rebuildListRunnable;
    private JTextField searchTextField;
    private JCheckBox nonNullValuesCheckBox;
    private ImageCategories last;

    private void buildGuiForCategorizer() {
        MigLayout migLayout = new MigLayout(
            new LC().fill(),
            new AC().fill(),
            new AC().size("60lp:60lp:60", 0, 1).size("20lp:20lp:20lp", 2, 4, 5).fill(3)
        );
        JPanel jPanel = new JPanel(migLayout);

        imageDescriptor.setEditable(false);
        imageDescriptor.setLineWrap(true);
        imageDescriptor.setWrapStyleWord(true);
        imageDescriptorCounterTaught.setEditable(false);
        imageDescriptorCounterTaught.setLineWrap(true);
        imageDescriptorCounterTaught.setWrapStyleWord(true);
        jPanel.add(imageDescriptor, new CC().growY().wrap());
        jPanel.add(imageDescriptorCounterTaught, new CC().growY().wrap());

        JButton copyFromPreviousButton = new JButton("Copy from previous");
        copyFromPreviousButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                imageCategoriesHiperList.copyFromPrevious();
            }
        });
        jPanel.add(copyFromPreviousButton, new CC().wrap());


        List<JComponent> componentsToDisable = new ArrayList<>();
        List<Consumer<ImageCategories>> imageCategoriesChangeConsumers = new ArrayList<>();

        MigLayout elementsPanelLayout = new MigLayout();
        JScrollPane scrollPane = new JScrollPane(
            null,
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );
        scrollPane.getVerticalScrollBar().setUnitIncrement(32);

        rebuildListRunnable = new Runnable() {
            @Override
            public void run() {
                JPanel elementsPanel = new JPanel(elementsPanelLayout);
                System.out.println("rebuilding the list");
                componentsToDisable.clear();
                imageCategoriesChangeConsumers.clear();
                var current = imageCategoriesHiperList.getCurrentT();
                if (current == null) {
                    categoryOrder.forEach(categoryUuid -> {
                        buildCategorizerLine(elementsPanel, componentsToDisable, imageCategoriesChangeConsumers, categoryUuid);
                    });
                } else {
                    categoryOrder.forEach(categoryUuid -> {
                        Category category = categoryCache.get(categoryUuid);
                        if (!category.name().toLowerCase().contains(searchTextField.getText().toLowerCase())) {
                            return;
                        }
                        ImageCategoriesValue imageCategoriesValue = findIcvByCategoryUuid(
                            current,
                            categoryUuid
                        );
                        if (nonNullValuesCheckBox.isSelected() && imageCategoriesValue.categoryValueUuid() == null) {
                            return;
                        }

                        buildCategorizerLine(elementsPanel, componentsToDisable, imageCategoriesChangeConsumers, categoryUuid);
                    });
                }

                categorizerEnabledState.accept(lastState);
                scrollPane.setViewportView(elementsPanel);
                categorizedUpdateToCurrentImage.accept(last);
            }
        };


        jPanel.add(scrollPane, new CC().wrap());

        categorizerEnabledState = aBoolean -> {
            lastState = aBoolean;
            componentsToDisable.forEach(component -> {
                component.setEnabled(aBoolean);
            });
        };
        categorizerEnabledState.accept(false);

        imageCategoriesVerticalSplit.setRightComponent(jPanel);
        nonNullValuesCheckBox = new JCheckBox("Show only non null");
        nonNullValuesCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                rebuildListRunnable.run();
            }
        });
        jPanel.add(nonNullValuesCheckBox, new CC().wrap());

        searchTextField = new JTextField();
        searchTextField.addKeyListener(new SimpleUpdateKeyListener() {
            @Override
            public void sync() {
                rebuildListRunnable.run();
            }
        });
        jPanel.add(searchTextField);

        categorizedUpdateToCurrentImage = new Consumer<ImageCategories>() {
            @Override
            public void accept(ImageCategories imageCategories) {
                last = imageCategories;
                if (imageCategories != null) {
                    imageCategoriesChangeConsumers.forEach(c -> {
                        c.accept(imageCategories);
                    });
                }
            }
        };
        rebuildListRunnable.run();
    }

    private void buildCategorizerLine(
        JPanel elementsPanel,
        List<JComponent> componentsToDisable,
        List<Consumer<ImageCategories>> imageCategoriesChangeConsumers,
        UUID categoryUuid
    ) {
        Category category = categoryCache.get(categoryUuid);

        addCategoryName(elementsPanel, category);

        addCategoryValueSelector(elementsPanel, componentsToDisable, imageCategoriesChangeConsumers, categoryUuid, category);

        addModifier(elementsPanel, componentsToDisable, imageCategoriesChangeConsumers, categoryUuid, category);

        addCleanMistakesButton(elementsPanel, categoryUuid, category, componentsToDisable);
    }

    private static void addCategoryUuid(
        JPanel elementsPanel,
        UUID categoryUuid
    ) {
        var uuidTf = new JTextField(categoryUuid.toString());
        uuidTf.setEditable(false);
        elementsPanel.add(uuidTf);
    }

    private static void addCategoryName(
        JPanel elementsPanel,
        Category category
    ) {
        JLabel categoryNameLabel = new JLabel(category.name());
        Color c = generateLabelColor(category);
        categoryNameLabel.setForeground(c);
        Font f = categoryNameLabel.getFont();
        categoryNameLabel.setFont(f.deriveFont(f.getStyle() ^ Font.BOLD));
        elementsPanel.add(categoryNameLabel);
    }

    private void addCategoryValueSelector(
        JPanel elementsPanel,
        List<JComponent> componentsToDisable,
        List<Consumer<ImageCategories>> imageCategoriesChangeConsumers,
        UUID categoryUuid,
        Category category
    ) {
        if (category.required()
            && category.categoryValues().size() == 2
            && category.categoryValues().get(0).displayedValue().equalsIgnoreCase("yes")
            && category.categoryValues().get(1).displayedValue().equalsIgnoreCase("no")
        ) {
            addCheckboxSelector(elementsPanel, componentsToDisable, imageCategoriesChangeConsumers, categoryUuid, category);
        } else {
            addComboBoxSelector(elementsPanel, componentsToDisable, imageCategoriesChangeConsumers, categoryUuid, category);
        }
    }

    private void addCheckboxSelector(
        JPanel elementsPanel,
        List<JComponent> componentsToDisable,
        List<Consumer<ImageCategories>> imageCategoriesChangeConsumers,
        UUID categoryUuid,
        Category category
    ) {
        JCheckBox checkBox = new JCheckBox();
        elementsPanel.add(checkBox);
        componentsToDisable.add(checkBox);
        checkBox.addActionListener(e -> {
            CategoryValue yesValue = getValueByName("yes", category);
            CategoryValue noValue = getValueByName("no", category);

            boolean isChecked = checkBox.isSelected();

            update(categoryUuid, original -> new ImageCategoriesValue(
                original.imageCategoriesValueUuid(),
                original.categoryUuid(),
                (isChecked ? yesValue : noValue).categoryValueUuid(),
                original.descriptiveModifier()
            ));
        });
        imageCategoriesChangeConsumers.add(new Consumer<ImageCategories>() {
            @Override
            public void accept(ImageCategories imageCategories) {
                if (imageCategories == null) {
                    return;
                }
                ImageCategoriesValue actualIcv = findIcvByCategoryUuid(imageCategories, categoryUuid);

                UUID categoryValueUuid = actualIcv.categoryValueUuid();
                if (categoryValueUuid == null) {
                    throw new RuntimeException("This category requires value, error in data");
                }
                CategoryValue categoryValue =  categoryValueCache.get(categoryValueUuid);
                if (categoryValue.displayedValue().equalsIgnoreCase("yes")) {
                    checkBox.setSelected(true);
                } else {
                    checkBox.setSelected(false);
                }
            }
        });
    }

    private void addComboBoxSelector(
        JPanel elementsPanel,
        List<JComponent> componentsToDisable,
        List<Consumer<ImageCategories>> imageCategoriesChangeConsumers,
        UUID categoryUuid,
        Category category
    ) {
        List<CategoryValueForDisplay> cvfdList;
        CategoryValueForDisplay[] displayValue;
        if (category.required()) {
            cvfdList = category.categoryValues()
                .stream()
                .map(CategoryValueForDisplay::new)
                .collect(Collectors.toList());
        } else {
            List<CategoryValue> categoryValues = new ArrayList<>();
            categoryValues.add(null);
            categoryValues.addAll(category.categoryValues());
            cvfdList = categoryValues
                .stream()
                .map(categoryValue -> {
                    if (categoryValue == null) {
                        return null;
                    } else {
                        return new CategoryValueForDisplay(categoryValue);
                    }
                })
                .collect(Collectors.toList());
        }
        displayValue = cvfdList.toArray(CategoryValueForDisplay[]::new);

        JComboBox<CategoryValueForDisplay> comboBox = new JComboBox<>(displayValue);
        elementsPanel.add(comboBox);
        componentsToDisable.add(comboBox);
        comboBox.addActionListener(e -> {
            Object item = comboBox.getSelectedItem();
            CategoryValueForDisplay cvfd = (CategoryValueForDisplay) item;

            update(categoryUuid, original -> new ImageCategoriesValue(
                original.imageCategoriesValueUuid(),
                original.categoryUuid(),
                (cvfd == null ? null : cvfd.categoryValue().categoryValueUuid()),
                original.descriptiveModifier()
            ));
        });

        List<CategoryValueForDisplay> finalCvfdList = cvfdList;
        imageCategoriesChangeConsumers.add(imageCategories -> {
            ImageCategoriesValue actualIcv = findIcvByCategoryUuid(imageCategories, categoryUuid);
            CategoryValue categoryValue =  categoryValueCache.get(actualIcv.categoryValueUuid());
            if (categoryValue == null) {
                comboBox.setSelectedIndex(0);
            } else {
                CategoryValueForDisplay categoryValueForDisplay = finalCvfdList
                    .stream()
                    .filter(cvfd -> {
                        if (cvfd == null) {
                            return false;
                        }
                        return Objects.equals(cvfd.categoryValue().categoryValueUuid(), categoryValue.categoryValueUuid());
                    })
                    .findFirst()
                    .get();
                int indexOf = finalCvfdList.indexOf(categoryValueForDisplay);
                comboBox.setSelectedIndex(indexOf);
            }
        });
    }

    private void addModifier(
        JPanel elementsPanel,
        List<JComponent> componentsToDisable,
        List<Consumer<ImageCategories>> imageCategoriesChangeConsumers,
        UUID categoryUuid,
        Category category
    ) {
        JTextField textField = new JTextField("[]");
        textField.addKeyListener(new SimpleUpdateKeyListener() {
            @Override
            public void sync() {
                update(categoryUuid, original -> new ImageCategoriesValue(
                        original.imageCategoriesValueUuid(),
                        original.categoryUuid(),
                        original.categoryValueUuid(),
                        textField.getText()
                ));
            }
        });
        imageCategoriesChangeConsumers.add(imageCategories -> {
            ImageCategoriesValue actualIcv = findIcvByCategoryUuid(imageCategories, categoryUuid);
            textField.setText(actualIcv.descriptiveModifier());
        });
        if (category.descriptive()) {
            textField.setEditable(true);
        }
        textField.setEnabled(false);
        if (category.descriptive()) {
            componentsToDisable.add(textField);
        }
        elementsPanel.add(textField);
    }

    private void addCleanMistakesButton(
        JPanel elementsPanel,
        UUID categoryUuid,
        Category category,
        List<JComponent> componentsToDisable
    ) {
        JButton removeBefore = new JButton("Remove before");
        JButton removeAfter = new JButton("Remove after");
        elementsPanel.add(removeBefore);
        elementsPanel.add(removeAfter, new CC().wrap());
        if (category.required()) {
            removeBefore.setEnabled(false);
            removeAfter.setEnabled(false);
            return;
        }
        componentsToDisable.add(removeBefore);
        componentsToDisable.add(removeAfter);

        removeBefore.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ImageCategories current = imageCategoriesHiperList.getCurrentT();

                for (int i = 0; i < imagesCategories.imageCategories().size(); i++) {
                    ImageCategories at = imagesCategories.imageCategories().get(i);
                    if (at.equals(current)) {
                        break;
                    }
                    setToNullValue(at, categoryUuid);
                }
            }
        });

        removeAfter.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ImageCategories current = imageCategoriesHiperList.getCurrentT();
                boolean next = false;

                for (int i = 0; i < imagesCategories.imageCategories().size(); i++) {
                    ImageCategories at = imagesCategories.imageCategories().get(i);
                    if (next) {
                        setToNullValue(at, categoryUuid);
                    }
                    if (at.equals(current)) {
                        next = true;
                    }
                }
            }
        });

    }

    private void setToNullValue(ImageCategories at, UUID categoryUuid) {
        updateFor(categoryUuid, icv -> new ImageCategoriesValue(
            icv.imageCategoriesValueUuid(),
            icv.categoryUuid(),
            null,
            icv.descriptiveModifier()
        ),
            at);
    }

    private void update(UUID categoryUuid, Function<ImageCategoriesValue, ImageCategoriesValue> creator) {
        ImageCategories current = imageCategoriesHiperList.getCurrentT();
        if (current == null) {
            return;
        }
        updateFor(categoryUuid, creator, current);
    }

    private void updateFor(UUID categoryUuid, Function<ImageCategoriesValue, ImageCategoriesValue> creator, ImageCategories updateFor) {
        ImageCategoriesValue icv = findIcvByCategoryUuid(updateFor, categoryUuid);
        int indexOf = updateFor.imageCategoriesValues().indexOf(icv);

        updateFor.imageCategoriesValues().set(indexOf, creator.apply(icv));
        reCalcCurrentImageCategories();
    }

    private static ImageCategoriesValue findIcvByCategoryUuid(ImageCategories imageCategories, UUID categoryUuid) {
        return imageCategories
            .imageCategoriesValues()
            .stream()
            .filter(icv -> Objects.equals(icv.categoryUuid(), categoryUuid))
            .findFirst()
            .get();
    }

    private static Color generateLabelColor(Category category) {
        String categoryNameLowerCase = category.name().toLowerCase(Locale.ROOT);
        String firstLetter = categoryNameLowerCase.substring(0, 1);
        int rgb = Color.HSBtoRGB((float) (Math.PI * Math.abs(firstLetter.hashCode() % 1000)), 0.8f, 0.5f);
        Color c1 = new Color(rgb);
        int rgb2 = Color.HSBtoRGB((float) (Math.PI * Math.abs(categoryNameLowerCase.hashCode() % 1000)), 0.8f, 0.5f);
        Color c2 = new Color(rgb2);
        Color effectiveColor = blend(c1, c2);
        return effectiveColor;
    }


    public static Color blend(Color c1, Color c2) {
        float ratio = 0.8f;

        int rgb1 = c1.getRGB();
        int a1 = (rgb1 >> 24 & 0xff);
        int r1 = ((rgb1 & 0xff0000) >> 16);
        int g1 = ((rgb1 & 0xff00) >> 8);
        int b1 = (rgb1 & 0xff);
        int rgb2 = c2.getRGB();
        int a2 = (rgb2 >> 24 & 0xff);
        int r2 = ((rgb2 & 0xff0000) >> 16);
        int g2 = ((rgb2 & 0xff00) >> 8);
        int b2 = (rgb2 & 0xff);

        int a = (int) ( a1 * ratio + (1 - ratio) * a2);
        int r = (int) ( r1 * ratio + (1 - ratio) * r2);
        int g = (int) ( g1 * ratio + (1 - ratio) * g2);
        int b = (int) ( b1 * ratio + (1 - ratio) * b2);

        return new Color(a << 24 | r << 16 | g << 8 | b);
    }

    private void reCalcCurrentImageCategories() {
        ImageCategories current = imageCategoriesHiperList.getCurrentT();
        if (current == null) {
            imageDescriptor.setText("");
            imageDescriptorCounterTaught.setText("");
        } else {
            String taughtValueStr = createTaughtValueStrExact(current);
            String counterTaughtValueStr = createCounterTaughtValueStrExact(current);
            imageDescriptor.setText(taughtValueStr);
            imageDescriptorCounterTaught.setText(counterTaughtValueStr);
        }
    }

    private String createTaughtValueStrExact(ImageCategories current) {
        Map<UUID, String> modifiersForThisImageCategories = new HashMap<>();
        Map<UUID, UUID> categoryForCategoryValue = new HashMap<>();
        current.imageCategoriesValues().forEach(icv -> {
            modifiersForThisImageCategories.put(
                icv.categoryUuid(),
                icv.descriptiveModifier()
            );
            categoryForCategoryValue.put(
                icv.categoryValueUuid(),
                icv.categoryUuid()
            );
        });

        List<CategoryValue> categories = getCategoryValuesForIcv(current);

        rearrangeCategories(current, categories);

        return createTaughtValueString(modifiersForThisImageCategories, categoryForCategoryValue, categories);
    }


    private String createCounterTaughtValueStrExact(ImageCategories current) {
        Map<UUID, String> modifiersForThisImageCategories = new HashMap<>();
        Map<UUID, UUID> categoryForCategoryValue = new HashMap<>();
        current.imageCategoriesValues().forEach(icv -> {
            modifiersForThisImageCategories.put(
                icv.categoryUuid(),
                icv.descriptiveModifier()
            );
            categoryForCategoryValue.put(
                icv.categoryValueUuid(),
                icv.categoryUuid()
            );
        });

        List<CategoryValue> categories = getCategoryValuesForIcv(current);

        rearrangeCategories(current, categories);

        return createTaughtValueStrExact(modifiersForThisImageCategories, categoryForCategoryValue, categories);
    }

    private List<CategoryValue> getCategoryValuesForIcv(ImageCategories current) {
        List<CategoryValue> categories = current
            .imageCategoriesValues()
            .stream()
            .map(icv -> categoryValueCache.get(icv.categoryValueUuid()))
            .collect(Collectors.toList());
        return categories;
    }

    private static void rearrangeCategories(ImageCategories current, List<CategoryValue> categories) {
        var uuid = current.imageCategoryUuid();
        var randomLong = uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits();

        Random random = new Random(randomLong);

        for (int i = 0; i < categories.size() - 1; i++) {
            if (random.nextDouble() < 0.3) {
                var tmp = categories.get(i + 1);
                categories.set(i + 1, categories.get(i));
                categories.set(i, tmp);
            }
        }
    }

    private static String createTaughtValueStrExact(Map<UUID, String> modifiersForThisImageCategories, Map<UUID, UUID> categoryForCategoryValue, List<CategoryValue> categories) {
        StringBuilder counterTaughtValue = new StringBuilder();
        categories
            .stream()
            .forEach(categoryValue -> {
                if (categoryValue == null) {
                    return;
                }

                UUID categoryUuid = categoryForCategoryValue.get(categoryValue.categoryValueUuid());

                String val = modifiersForThisImageCategories.get(categoryUuid).replace("[]", categoryValue.counterTaughtValue());
                if (val.isBlank()) {
                    return;
                }
                counterTaughtValue.append(val).append(", ");
            });

        String counterTaughtValueStr = counterTaughtValue.toString();
        if (counterTaughtValueStr.length() > 2) {
            counterTaughtValueStr = counterTaughtValue.substring(0, counterTaughtValueStr.length() - 2);
        }
        return counterTaughtValueStr;
    }

    private static String createTaughtValueString(Map<UUID, String> modifiersForThisImageCategories, Map<UUID, UUID> categoryForCategoryValue, List<CategoryValue> categories) {
        StringBuilder taughtValue = new StringBuilder();
        categories
            .stream()
            .forEach(categoryValue -> {
                if (categoryValue == null) {
                    return;
                }

                UUID categoryUuid = categoryForCategoryValue.get(categoryValue.categoryValueUuid());

                String val = modifiersForThisImageCategories.get(categoryUuid).replace("[]", categoryValue.taughtValue());
                if (val.isBlank()) {
                    return;
                }
                taughtValue.append(val).append(", ");
            });
        String taughtValueStr = taughtValue.toString();
        if (taughtValueStr.length() > 2) {
            taughtValueStr = taughtValueStr.substring(0, taughtValueStr.length() - 2);
        }
        return taughtValueStr;
    }

    private static CategoryValue getValueByName(String type, Category category) {
        return category.categoryValues()
            .stream()
            .filter(cv -> cv.displayedValue().equalsIgnoreCase(type))
            .findFirst()
            .get();
    }

    private Consumer<Boolean> categorizerEnabledState;
    private Consumer<ImageCategories> categorizedUpdateToCurrentImage;

    private JComponent makeClassDesigner() {
        MigLayout allLayout = new MigLayout(
            new LC().fill(),
            new AC().grow(),
            new AC().grow()
        );
        JPanel panel = new JPanel(allLayout);

        // left

        // todo copying is bugged and applies non-null label to all images, which has potential of ruining categories
        copyCategoryButton.setEnabled(false);
        JPanel leftPanel = listPanel(addCategory, deleteCategory, copyCategoryButton, categoryHiperList);

        // right


        MigLayout aboveLayout = new MigLayout(
            new LC(),
            new AC().size("90lp:90lp:90lp", 0).size("200lp:200lp:600lp", 1),
            new AC()
        );
        JPanel abovePanel = new JPanel(aboveLayout);

        abovePanel.add(new JLabel("Category name: "));
        abovePanel.add(categoryName, new CC().grow().wrap());
        abovePanel.add(new JLabel("Is required: "));
        abovePanel.add(isRequired, new CC().wrap());
        abovePanel.add(new JLabel("Is descriptive: "));
        abovePanel.add(isDescriptive, new CC().wrap());
        abovePanel.add(saveCategoryButton);
        onCategorySelected(null);

        JPanel bottomLeftPanel = listPanel(addCategoryValue, deleteCategoryValue, copyCategoryValueButton, categoryValueHiperList);


        MigLayout bottomRightLayout = new MigLayout(
            new LC(),
            new AC().size("90lp:90lp:90lp", 0).size("200lp:200lp:600lp", 1),
            new AC()
        );
        JPanel bottomRightPanel = new JPanel(bottomRightLayout);

        bottomRightPanel.add(new JLabel("Displayed: "));
        bottomRightPanel.add(categoryValueDisplayedName, new CC().grow().wrap());
        bottomRightPanel.add(new JLabel("Taught: "));
        bottomRightPanel.add(categoryValueTaughtName, new CC().grow().wrap());
        bottomRightPanel.add(new JLabel("Contertaught: "));
        bottomRightPanel.add(categoryValueCounterTaughtName, new CC().grow().wrap());
        bottomRightPanel.add(saveCategoryValueButton);
        onCategoryValueSelected(null);

        JSplitPane bottomVertSplitPane = new JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT,
            bottomLeftPanel,
            bottomRightPanel
        );

        JSplitPane horizontalSplitPane = new JSplitPane(
            JSplitPane.VERTICAL_SPLIT,
            abovePanel,
            bottomVertSplitPane
        );
        horizontalSplitPane.setDividerLocation(0.5);


        JSplitPane verticalSplitPane = new JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT,
            leftPanel,
            horizontalSplitPane
        );
        verticalSplitPane.setDividerLocation(0.4);
        panel.add(verticalSplitPane, new CC().growY().growX());
        return panel;
    }

    private JComponent makeFilePreprocessor() {
        MigLayout allLayout = new MigLayout(
            new LC().fill(),
            new AC().grow(0.5f, 0, 3).size("70lp:70lp:70lp", 1).size("530lp:530lp:530lp", 2),
            new AC().size("20lp:20lp:20lp", 0, 1, 2, 3, 4).fill(5).grow()
        );
        JPanel panel = new JPanel(allLayout);

        JTextField inputPathTextField = new JTextField();
        JTextField outputPathTextField = new JTextField();
        JTextField statusTextField = new JTextField();
        statusTextField.setEditable(false);
        JButton runButton = new JButton("Run");
        JProgressBar progressBar = new JProgressBar();
        JTextArea filtersTextArea = new JTextArea();

        panel.add(new JLabel("Input path:"), new CC().cell(1, 0));
        panel.add(inputPathTextField, new CC().cell(2, 0).grow());
        panel.add(new JLabel("Output path:"), new CC().cell(1, 1));
        panel.add(outputPathTextField, new CC().cell(2, 1).grow());
        panel.add(new JLabel("Status:"), new CC().cell(1, 2));
        panel.add(statusTextField, new CC().cell(2, 2).grow());
        panel.add(new JLabel("Run:"), new CC().cell(1, 3));
        panel.add(runButton, new CC().cell(2, 3).grow());
        panel.add(new JLabel("Progress:"), new CC().cell(1, 4));
        panel.add(progressBar, new CC().cell(2, 4).grow());
        panel.add(new JLabel("Filters:"), new CC().cell(1, 5));
        panel.add(filtersTextArea, new CC().cell(2, 5).grow());

        runButton.addActionListener(e -> {
            String inputPath = inputPathTextField.getText();
            String outputPath = outputPathTextField.getText();

            File inputPathFile = new File(inputPath);
            File outputPathFile = new File(outputPath);

            List<Pattern> patterns = Arrays.stream(filtersTextArea.getText().split("\n"))
                .filter(string -> !string.isBlank())
                .map(string -> Pattern.compile(".*" + Pattern.quote(string) + ".*"))
                .toList();

            if (!inputPathFile.isDirectory()) {
                statusTextField.setText(inputPath + " is not a directory");
                return;
            }

            if (!outputPathFile.isDirectory()) {
                statusTextField.setText(outputPath + " is not a directory");
                return;
            }

            new Thread(() -> {
                List<Path> files = new ArrayList<>();

                try {
                    try (Stream<Path> stream = Files.walk(Paths.get(inputPath))) {
                        stream
                            .filter(Files::isRegularFile)
                            .forEach(files::add);
                    }
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        statusTextField.setText(ex.getMessage());
                    });
                    ex.printStackTrace();
                    return;
                }

                SwingUtilities.invokeLater(() -> {
                    statusTextField.setText("discovered " + files.size() + " files");
                    progressBar.setMaximum(files.size());
                });

                fileloop: for (int i = 0; i < files.size(); i++) {
                    Path path = files.get(i);
                    final int l = i;
                    SwingUtilities.invokeLater(() -> {
                        statusTextField.setText("loaded: " + path.toFile().getName());
                        progressBar.setValue(l);
                    });

                    String fileName = path.getFileName().toString();
                    for (Pattern pattern : patterns) {
                        if (pattern.matcher(fileName).matches()) {
                            SwingUtilities.invokeLater(() -> {
                                statusTextField.setText("skipped " + fileName + " because it matches " + pattern);
                                System.err.println("skipped " + fileName + " because it matches " + pattern);
                                progressBar.setMaximum(files.size());
                            });
                            continue fileloop;
                        }
                    }

                    try {
                        BufferedImage img = ImageIO.read(path.toFile());
                        if (img == null) {
                            SwingUtilities.invokeLater(() -> {
                                statusTextField.setText("skipped " + fileName + " because couldn't load");
                                System.err.println("skipped " + fileName + " because couldn't load");
                                progressBar.setMaximum(files.size());
                            });
                            continue fileloop;
                        }
                        BufferedImage cropped;
                        if (img.getWidth() > img.getHeight()) {
                            int cutTo = img.getHeight();
                            int diff = (img.getWidth() - cutTo)/2;
                            cropped = Scalr.crop(img, diff, 0, cutTo, cutTo);
                        } else {
                            int cutTo = img.getWidth();
                            int diff = (img.getHeight() - cutTo)/2;
                            cropped = Scalr.crop(img, 0, diff, cutTo, cutTo);
                        }

                        BufferedImage out = Scalr.resize(cropped, Scalr.Method.ULTRA_QUALITY, 512);
                        UUID uuid = UUID.randomUUID();

                        long pseudoUniqueIdLong = uuid.getLeastSignificantBits() ^ uuid.getMostSignificantBits();
                        int pseudoUniqueId = ((int) pseudoUniqueIdLong >> 16) ^ ((int) pseudoUniqueIdLong);
                        String pseudoUniqueIdHex = String.format("%08X", pseudoUniqueId);
                        ImageIO.write(out, "png", new File(outputPath + "/" + path.getFileName() + "_" + pseudoUniqueIdHex + "_cs.png"));
                        SwingUtilities.invokeLater(() -> {
                            statusTextField.setText("processed: " + path.toFile().getName());
                            progressBar.setValue(l);
                        });
                    } catch (IOException ex) {
                        SwingUtilities.invokeLater(() -> {
                            System.err.println("failed to load: " + path.toFile().getName());
                            statusTextField.setText("failed to load: " + path.toFile().getName());
                        });
                    }

                }

            }).start();


        });

        return panel;
    }

    private JComponent makeDreamboothTab() {
        MigLayout allLayout = new MigLayout(
            new LC().fill(),
            new AC().grow(0.5f, 0, 3).size("70lp:70lp:70lp", 1).size("530lp:530lp:530lp", 2),
            new AC().size("20lp:20lp:20lp", 0, 1, 2, 3, 4).fill(5).grow()
        );
        JPanel panel = new JPanel(allLayout);

        JTextField inputPathTextField = new JTextField();
        JTextField outputPathTextField = new JTextField();
        JTextField statusTextField = new JTextField();
        statusTextField.setEditable(false);
        JButton runButton = new JButton("Run");
        JProgressBar progressBar = new JProgressBar();
        JTextArea filtersTextArea = new JTextArea();

        panel.add(new JLabel("Input path:"), new CC().cell(1, 0));
        panel.add(inputPathTextField, new CC().cell(2, 0).grow());
        panel.add(new JLabel("Output path:"), new CC().cell(1, 1));
        panel.add(outputPathTextField, new CC().cell(2, 1).grow());
        panel.add(new JLabel("Status:"), new CC().cell(1, 2));
        panel.add(statusTextField, new CC().cell(2, 2).grow());
        panel.add(new JLabel("Run:"), new CC().cell(1, 3));
        panel.add(runButton, new CC().cell(2, 3).grow());
        panel.add(new JLabel("Progress:"), new CC().cell(1, 4));
        panel.add(progressBar, new CC().cell(2, 4).grow());
        panel.add(new JLabel("Filters:"), new CC().cell(1, 5));
        panel.add(filtersTextArea, new CC().cell(2, 5).grow());

        runButton.addActionListener(e -> {
            String inputPath = inputPathTextField.getText();
            String outputPath = outputPathTextField.getText();

            List<String> patterns = Arrays.stream(filtersTextArea.getText().split("\n"))
                .filter(string -> !string.isBlank())
                .map(String::toLowerCase)
                .toList();

            new Thread(() -> {
                final int[] i = {0};
                List<Concept> concepts = new ArrayList<>();
                imagesCategories
                    .imageCategories()
                    .forEach(icv -> {
                        i[0]++;
                        String lowCaseIcvPath = icv.path().toLowerCase();
                        if (patterns.isEmpty() || patterns.stream().anyMatch(lowCaseIcvPath::startsWith)) {
                            String path0 = "file_" + Math.abs(icv.path().hashCode());
                            String path1 = "file_" + Math.abs(createCounterTaughtValueStrExact(icv).hashCode());
                            String fPath0 = "/content/data/tr/" + path0;
                            String fPath1 = "/content/data/ct/" + path1;
                            concepts.add(new Concept(
                                createTaughtValueStrExact(icv),
                                createCounterTaughtValueStrExact(icv),
                                fPath0,
                                fPath1
                            ));
                            try {
                                FileUtils.copyFile(
                                    new File(inputPath + icv.path()),
                                    new File(outputPath +  "/content/data/tr/" + path0 + "/" + icv.path())
                                );
                            } catch (IOException ex) {
                                throw new RuntimeException(ex);
                            }
                        } else {
                            SwingUtilities.invokeLater(() -> {
                                statusTextField.setText("skipped " + lowCaseIcvPath + " because not on filters list");
                            });
                        }
                        SwingUtilities.invokeLater(() -> {
                            progressBar.setValue(i[0]);
                            progressBar.setMaximum(imagesCategories.imageCategories().size());
                        });
                    });

                File selectedFile = new File(outputPath + "concept_list.json");
                try {
                    new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(selectedFile, concepts);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }

            }).start();

        });

        return panel;
    }

    private JTextField basePath = new JTextField();

    private JSplitPane imageCategoriesVerticalSplit;

    private CopyValueFromBeforeHiperlist<ImageCategories> imageCategoriesHiperList;

    private JTextArea imageDescriptor = new JTextArea();
    private JTextArea imageDescriptorCounterTaught = new JTextArea();

    private JComponent makeCategorizer() {
        MigLayout allLayout = new MigLayout(
            new LC().fill().debug(),
            new AC().grow(),
            new AC().grow()
        );
        JPanel panel = new JPanel(allLayout);

        JLabel image = new JLabel("an image will be here");

        JButton copyFromBefore = new JButton("Copy previous");
        JButton removeButton = new JButton("Remove");

        imageCategoriesHiperList = new CopyValueFromBeforeHiperlist<ImageCategories>(
            null,
            removeButton,
            null,
            copyFromBefore,
            (template, value) -> new ImageCategories(
                value.imageCategoryUuid(),
                value.path(),
                template.imageCategoriesValues().stream()
                    .map(icv -> new ImageCategoriesValue(
                        UUID.randomUUID(),
                        icv.categoryUuid(),
                        icv.categoryValueUuid(),
                        icv.descriptiveModifier()
                    ))
                    .collect(Collectors.toList())
            )
        ) {

            @Override
            protected List<ImageCategories> getBackingList() {
                return imagesCategories.imageCategories();
            }

            @Override
            protected IntFunction<ImageCategories[]> generator() {
                return ImageCategories[]::new;
            }

            @Override
            protected void onSelected(ImageCategories selected) {
                if (selected != null) {
                    new Thread(() -> {
                        try {
                            BufferedImage loadedImage = ImageIO.read(new File(basePath.getText() + selected.path()));
                            var ii = new ImageIcon(Scalr.resize(loadedImage, Scalr.Method.SPEED, 512, (BufferedImageOp) null));
                            SwingUtilities.invokeLater(() -> {
                                image.setText("");
                                image.setIcon(ii);
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }).start();


                    categorizerEnabledState.accept(true);
                } else {
                    categorizerEnabledState.accept(false);
                }
                categorizedUpdateToCurrentImage.accept(selected);
                reCalcCurrentImageCategories();
            }

            @Override
            protected ImageCategories defaultTFactory(int probablyUniqueId) {
                throw new RuntimeException("No support for adding images manually");
            }

            @Override
            protected ImageCategories updatedTValue(ImageCategories currentT) {
                throw new RuntimeException("No support for updating images manually");
            }

            @Override
            public void onChange() {
            }
        };

        JButton importFromPathButton = new JButton("Import");
        importFromPathButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    try (Stream<Path> stream = Files.walk(Paths.get(basePath.getText()))) {
                        stream
                            .filter(Files::isRegularFile)
                            .filter(path -> path.getFileName().toString().endsWith("png"))
                            .forEach(path -> {
                                List<ImageCategoriesValue> imageCategoriesValues = new ArrayList<>();

                                categoryOrder
                                    .stream()
                                    .forEach(categoryUuid -> {
                                        Category category = categoryCache.get(categoryUuid);
                                        if (category.categoryValues().isEmpty()) {
                                            if (category.required()) {
                                                throw new RuntimeException("Category cannot be required and have no values");
                                            }
                                            imageCategoriesValues.add(
                                                new ImageCategoriesValue(
                                                    UUID.randomUUID(),
                                                    categoryUuid,
                                                    null,
                                                    "[]"
                                                )
                                            );
                                        } else {
                                            CategoryValue categoryValue = category.categoryValues().get(0);
                                            imageCategoriesValues.add(
                                                new ImageCategoriesValue(
                                                    UUID.randomUUID(),
                                                    categoryUuid,
                                                    categoryValue.categoryValueUuid(),
                                                    "[]"
                                                )
                                            );
                                        }
                                    });

                                imagesCategories.imageCategories().add(
                                    new ImageCategories(
                                        UUID.randomUUID(),
                                        path.getFileName().toString(),
                                        imageCategoriesValues
                                    )
                                );
                            });
                    }
                    syncCategoriesToImageCategories();
                    imageCategoriesHiperList.sync();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        MigLayout listPanelMigLayout = new MigLayout(
            new LC().fill(),
            new AC().size("90lp:90lp:90lp", 0, 2).fill(1).size("20lp:150lp:500lp", 1),
            new AC().size("20lp:20lp:20", 0, 1, 2).fill(3)
        );
        JPanel listPanel = new JPanel(listPanelMigLayout);
        listPanel.add(new JLabel("Path:"));
        listPanel.add(basePath);
        listPanel.add(importFromPathButton, new CC().growX().wrap());
        listPanel.add(removeButton, new CC().growX().wrap());
        JButton sort = new JButton("Sort");
        listPanel.add(sort, new CC().spanX().growX().wrap());
        sort.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                imagesCategories.imageCategories().sort(new Comparator<ImageCategories>() {
                    @Override
                    public int compare(ImageCategories o1, ImageCategories o2) {
                        return String.CASE_INSENSITIVE_ORDER.compare(o1.path(), o2.path());
                    }
                });
                imageCategoriesHiperList.sync();
            }
        });
        addHiperlist(imageCategoriesHiperList, listPanel);

        MigLayout imageMigLayout = new MigLayout(
            new LC().fill(),
            new AC().size("512lp:512lp:512lp"),
            new AC().size("512lp:512lp:512lp")
        );

        JPanel imagePanel = new JPanel(imageMigLayout);
        imagePanel.add(image, new CC().grow());

        JSplitPane horizontalSplit = new JSplitPane(
            JSplitPane.VERTICAL_SPLIT,
            image,
            listPanel
        );

        imageCategoriesVerticalSplit = new JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT,
            horizontalSplit,
            null
        );

        panel.add(imageCategoriesVerticalSplit, new CC().growY().growX());

        return panel;
    }

    private static JPanel listPanel(JButton add, JButton delete, JButton copy, HiperList<?> hiperList) {
        MigLayout leftLayout = new MigLayout(
            new LC().fill(),
            new AC().fill(),
            new AC().size("20lp:20lp:20lp", 0, 1, 2).fill(3)
        );
        JPanel leftPanel = new JPanel(leftLayout);

        leftPanel.add(add, new CC().wrap());
        leftPanel.add(delete, new CC().wrap());
        leftPanel.add(copy, new CC().wrap());
        addHiperlist(hiperList, leftPanel);
        return leftPanel;
    }

    private static void addHiperlist(HiperList<?> hiperList, JPanel leftPanel) {
        JScrollPane scrollPane = new JScrollPane(
            hiperList.getSwingList(),
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );
        leftPanel.add(
            scrollPane,
            new CC()
                .growX()
                .spanX()
                .spanY()
                .growY()
        );
    }

    private void onCategorySelected(Category category) {
        boolean enabled = category != null;
        categoryName.setEnabled(enabled);
        isRequired.setEnabled(enabled);
        isDescriptive.setEnabled(enabled);
        saveCategoryButton.setEnabled(enabled);
        if (category != null) {
            categoryName.setText(category.name());
            isRequired.setSelected(category.required());
            isDescriptive.setSelected(category.descriptive());
        }
    }

    private void onCategoryValueSelected(CategoryValue categoryValue) {
        boolean enabled = categoryValue != null;
        categoryValueDisplayedName.setEnabled(enabled);
        categoryValueTaughtName.setEnabled(enabled);
        categoryValueCounterTaughtName.setEnabled(enabled);
        saveCategoryValueButton.setEnabled(enabled);
        if (categoryValue != null) {
            categoryValueDisplayedName.setText(categoryValue.displayedValue());
            categoryValueTaughtName.setText(categoryValue.taughtValue());
            categoryValueCounterTaughtName.setText(categoryValue.counterTaughtValue());
        }
    }

}
