package place.sita.labelle.swing;

import place.sita.labelle.Copiable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public abstract class ValueCopiableHiperlist<T extends Copiable<T>> extends HiperList<T> {

    public ValueCopiableHiperlist(JButton add, JButton remove, JButton save, JButton copy) {
        super(add, remove, save);
        // copy
        if (copy != null) {
            copy.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int selectedIndex = getSwingList().getSelectedIndex();
                    if (selectedIndex != -1) {
                        getBackingList().add(getBackingList().get(selectedIndex).copy());
                        sync();
                        getSwingList().setSelectedIndex(getBackingList().size() - 1);
                    }
                }
            });
        }
    }
}
