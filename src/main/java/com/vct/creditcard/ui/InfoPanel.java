package com.vct.creditcard.ui;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.LinkedHashMap;
import java.util.Map;

/** A list of "label: value" rows whose values can be updated. */
final class InfoPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final Map<String, JLabel> values = new LinkedHashMap<>();

    InfoPanel(String... labels) {
        super(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 0, 5, 24);
        c.anchor = GridBagConstraints.LINE_START;
        for (int row = 0; row < labels.length; row++) {
            c.gridy = row;
            c.gridx = 0;
            JLabel label = new JLabel(labels[row]);
            label.setFont(Ui.VALUE_FONT.deriveFont(Font.BOLD));
            add(label, c);
            c.gridx = 1;
            JLabel value = new JLabel("-");
            value.setFont(Ui.VALUE_FONT);
            add(value, c);
            values.put(labels[row], value);
        }
    }

    void set(String label, Object value) {
        values.get(label).setText(String.valueOf(value));
    }
}
