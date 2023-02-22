package place.sita.labelle.swing;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

public abstract class HiperList<T> {

    private JList<T> swingList;
    private T currentT;

    public HiperList(JButton add, JButton remove, JButton save) {
        swingList = new JList<>();
        swingList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        swingList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) {
                    return;
                }
                int index = swingList.getSelectedIndex();
                if (index == -1) {
                    return;
                }
                currentT = getBackingList().get(index);
                onSelected(currentT);
            }
        });

        // add
        if (add != null) {

            add.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int id = Instant.now().getNano();
                    getBackingList().add(defaultTFactory(id));
                    sync();
                    swingList.setSelectedIndex(getBackingList().size() - 1);
                    onChange();
                }
            });
        }

        // delete
        if (remove != null) {
            remove.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int currentIndex = getBackingList().indexOf(currentT);
                    if (currentIndex != -1) {
                        getBackingList().remove(currentIndex);
                    }
                    int indexBeforeRemoval = swingList.getSelectedIndex();
                    sync();
                    if (indexBeforeRemoval != -1) {
                        if (getBackingList().size() != 0) {
                            swingList.setSelectedIndex(indexBeforeRemoval);
                        }
                    }
                    onChange();
                }
            });
        }

        // save
        if (save != null) {
            save.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (currentT != null) {
                        T t = updatedTValue(currentT);
                        int currentIndex = getBackingList().indexOf(currentT);
                        if (currentIndex != -1) {
                            getBackingList().set(currentIndex, t);
                            sync();
                            currentT = t;
                        }
                        onChange();
                    }
                }
            });
        }
    }

    public void sync() {
        int selectedIndex = swingList.getSelectedIndex();
        List<T> backingList = getBackingList();
        if (backingList == null) {
            backingList = new ArrayList<>();
        }
        swingList.setListData(backingList.toArray(generator()));
        if (selectedIndex != -1) {
            if (selectedIndex < backingList.size()) {
                swingList.setSelectedIndex(selectedIndex);
                currentT = backingList.get(selectedIndex);
            } else {
                currentT = null;
            }
        } else {
            currentT = null;
        }
        onSelected(currentT);
    }

    protected abstract List<T> getBackingList();

    protected abstract IntFunction<T[]> generator();

    protected abstract void onSelected(T selected);

    protected abstract T defaultTFactory(int probablyUniqueId);

    protected abstract T updatedTValue(T currentT);

    public JList<T> getSwingList() {
        return swingList;
    }

    public T getCurrentT() {
        return currentT;
    }

    public abstract void onChange();
}
