package com.vct.creditcard.ui;

import com.vct.creditcard.service.BankingException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Shared look and helpers for all windows. */
final class Ui {

    static final Font TITLE_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 22);
    static final Font HEADING_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 16);
    static final Font VALUE_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 15);

    @FunctionalInterface
    interface Action {
        void run() throws BankingException, SQLException;
    }

    private Ui() {
    }

    /** Runs an action and turns business errors and database errors into dialogs. */
    static void attempt(Component parent, Action action) {
        try {
            action.run();
        } catch (BankingException e) {
            showError(parent, e.getMessage());
        } catch (SQLException e) {
            e.printStackTrace();
            showError(parent, "Database error: " + e.getMessage());
        }
    }

    static void showError(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    static void showInfo(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Information", JOptionPane.INFORMATION_MESSAGE);
    }

    static JButton button(String text, Runnable onClick) {
        JButton button = new JButton(text);
        button.addActionListener(e -> onClick.run());
        return button;
    }

    static JLabel title(String text) {
        JLabel label = new JLabel(text);
        label.setFont(TITLE_FONT);
        return label;
    }

    /** A two-column form: labels on the left, fields on the right. */
    static JPanel form(String[] labels, JComponent[] fields) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 4, 6, 8);
        for (int row = 0; row < labels.length; row++) {
            c.gridy = row;
            c.gridx = 0;
            c.weightx = 0;
            c.anchor = GridBagConstraints.LINE_START;
            c.fill = GridBagConstraints.NONE;
            JLabel label = new JLabel(labels[row]);
            label.setFont(label.getFont().deriveFont(Font.BOLD));
            panel.add(label, c);
            c.gridx = 1;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            panel.add(fields[row], c);
        }
        return panel;
    }

    /** Makes the first ("#") column of a table narrow. */
    static void narrowFirstColumn(javax.swing.JTable table) {
        if (table.getColumnCount() > 0) {
            table.getColumnModel().getColumn(0).setMaxWidth(48);
        }
    }

    static JPanel padded(JComponent content) {
        JPanel panel = new JPanel(new java.awt.BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        panel.add(content);
        return panel;
    }

    /** Parses an ISO date ({@code yyyy-MM-dd}); an empty field gives {@code null}. */
    static LocalDate parseOptionalDate(String text) throws BankingException {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(text.strip());
        } catch (DateTimeParseException e) {
            throw new BankingException("\"" + text.strip() + "\" is not a valid date. Use the format YYYY-MM-DD.");
        }
    }
}
