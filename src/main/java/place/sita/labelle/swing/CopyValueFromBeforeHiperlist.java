package place.sita.labelle.swing;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public abstract class CopyValueFromBeforeHiperlist<T> extends HiperList<T> {

    private final CopyingFunction<T> copyingFunction;

    public CopyValueFromBeforeHiperlist(JButton add, JButton remove, JButton save, JButton copyFromBefore, CopyingFunction<T> copyingFunction) {
        super(add, remove, save);
        this.copyingFunction = copyingFunction;
        // copy
        if (copyFromBefore != null) { //todo dead code
            copyFromBefore.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int selectedIndex = getSwingList().getSelectedIndex();
                    if (selectedIndex > 0) {
                        T previous = getBackingList().get(selectedIndex - 1);
                        T current = getBackingList().get(selectedIndex);
                        T newCurrent = copyingFunction.copy(previous, current);
                        getBackingList().set(selectedIndex, newCurrent);
                        sync();
                        getSwingList().setSelectedIndex(selectedIndex);
                    }
                }
            });
        }
    }

    public void copyFromPrevious() {
        int selectedIndex = getSwingList().getSelectedIndex();
        if (selectedIndex > 0) {
            T previous = getBackingList().get(selectedIndex - 1);
            T current = getBackingList().get(selectedIndex);
            T newCurrent = copyingFunction.copy(previous, current);
            getBackingList().set(selectedIndex, newCurrent);
            sync();
            getSwingList().setSelectedIndex(selectedIndex);
        }
    }

    public interface CopyingFunction<T> {

        T copy(T template, T value);

    }

}
