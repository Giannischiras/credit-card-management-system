package com.vct.creditcard.ui;

import com.vct.creditcard.model.CardTransaction;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/** Transaction history with an optional date range. */
final class HistoryWindow extends JDialog {

    private static final long serialVersionUID = 1L;

    @FunctionalInterface
    interface Loader {
        List<CardTransaction> load(LocalDate from, LocalDate to) throws SQLException;
    }

    private final transient Loader loader;
    private final JTextField fromField = new JTextField(10);
    private final JTextField toField = new JTextField(10);
    private final TransactionTableModel model = new TransactionTableModel();
    private final JLabel summary = new JLabel();

    HistoryWindow(Window owner, String title, Loader loader) {
        super(owner, title, ModalityType.MODELESS);
        this.loader = loader;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filters.add(new JLabel("From:"));
        filters.add(fromField);
        filters.add(new JLabel("To:"));
        filters.add(toField);
        filters.add(Ui.button("See history", this::load));
        filters.add(new JLabel("(YYYY-MM-DD, leave empty for all)"));

        JTable table = new JTable(model);
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(760, 300));

        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.add(filters, BorderLayout.NORTH);
        content.add(scroll, BorderLayout.CENTER);
        content.add(summary, BorderLayout.SOUTH);
        setContentPane(Ui.padded(content));

        load();
        pack();
        setLocationRelativeTo(owner);
    }

    private void load() {
        Ui.attempt(this, () -> {
            List<CardTransaction> rows = loader.load(
                    Ui.parseOptionalDate(fromField.getText()), Ui.parseOptionalDate(toField.getText()));
            model.setRows(rows);
            summary.setText(rows.size() + (rows.size() == 1 ? " transaction" : " transactions"));
        });
    }
}
