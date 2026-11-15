package com.vct.creditcard.ui;

import com.vct.creditcard.model.CustomerRanking;
import com.vct.creditcard.model.MerchantSales;
import com.vct.creditcard.model.Money;
import com.vct.creditcard.service.Services;

import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** Shows one of the management reports. */
final class ReportWindow extends JDialog {

    private static final long serialVersionUID = 1L;
    private static final int TOP = 10;
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);

    enum Kind {
        BEST_CUSTOMERS("Our best customers", "Customers with no debt, ranked by how much they spend"),
        WORST_CUSTOMERS("Our worst customers", "Customers in debt, ranked by how much of their credit limit they use"),
        MERCHANT_OF_THE_MONTH("Merchant of the month", "Merchants ranked by net sales (purchases minus refunds)");

        private final String title;
        private final String description;

        Kind(String title, String description) {
            this.title = title;
            this.description = description;
        }
    }

    private final transient Services services;
    private final DefaultTableModel model = new DefaultTableModel() {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JLabel headline = new JLabel(" ");
    private final JTable table = new JTable(model);

    ReportWindow(JFrame owner, Services services, Kind kind) {
        super(owner, kind.title, ModalityType.MODELESS);
        this.services = services;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel top = new JPanel(new BorderLayout(0, 6));
        top.add(Ui.title(kind.title), BorderLayout.NORTH);
        top.add(new JLabel(kind.description), BorderLayout.CENTER);

        table.setFillsViewportHeight(true);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(680, 260));

        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.add(top, BorderLayout.NORTH);
        content.add(scroll, BorderLayout.CENTER);
        headline.setFont(Ui.HEADING_FONT);
        content.add(headline, BorderLayout.SOUTH);

        if (kind == Kind.MERCHANT_OF_THE_MONTH) {
            JComboBox<YearMonth> monthBox = new JComboBox<>();
            YearMonth now = YearMonth.now();
            for (int i = 0; i < 12; i++) {
                monthBox.addItem(now.minusMonths(i));
            }
            monthBox.setRenderer((list, value, index, selected, focus) ->
                    new javax.swing.DefaultListCellRenderer().getListCellRendererComponent(
                            list, value == null ? "" : value.format(MONTH_FORMAT), index, selected, focus));
            monthBox.addActionListener(e -> loadMerchants((YearMonth) monthBox.getSelectedItem()));
            JPanel monthRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            monthRow.add(new JLabel("Month:"));
            monthRow.add(monthBox);
            top.add(monthRow, BorderLayout.SOUTH);
            loadMerchants(now);
        } else {
            loadCustomers(kind);
        }

        setContentPane(Ui.padded(content));
        pack();
        setLocationRelativeTo(owner);
    }

    private void loadCustomers(Kind kind) {
        Ui.attempt(this, () -> {
            List<CustomerRanking> rows = kind == Kind.BEST_CUSTOMERS
                    ? services.reports().bestCustomers(TOP)
                    : services.reports().worstCustomers(TOP);
            model.setDataVector(new Object[0][], new Object[] {"#", "Customer", "Type", "Total spent", "Debt", "Credit used"});
            int rank = 1;
            for (CustomerRanking r : rows) {
                BigDecimal used = r.debt().multiply(BigDecimal.valueOf(100)).divide(r.creditLimit(), 1, RoundingMode.HALF_UP);
                model.addRow(new Object[] {rank++, r.name(), r.type().label(), Money.format(r.totalSpent()),
                        Money.format(r.debt()), used.toPlainString() + "%"});
            }
            Ui.narrowFirstColumn(table);
            headline.setText(rows.isEmpty() ? "No customers in this list yet." : "#1: " + rows.get(0).name());
        });
    }

    private void loadMerchants(YearMonth month) {
        Ui.attempt(this, () -> {
            List<MerchantSales> rows = services.reports().merchantSales(month);
            model.setDataVector(new Object[0][], new Object[] {"#", "Merchant", "Net sales", "Transactions"});
            int rank = 1;
            for (MerchantSales r : rows) {
                model.addRow(new Object[] {rank++, r.name(), Money.format(r.netSales()), r.transactions()});
            }
            Ui.narrowFirstColumn(table);
            headline.setText(rows.isEmpty()
                    ? "No sales in " + month.format(MONTH_FORMAT) + "."
                    : "Merchant of " + month.format(MONTH_FORMAT) + ": " + rows.get(0).name() + " 🏆");
        });
    }
}
