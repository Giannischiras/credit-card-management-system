package com.vct.creditcard.ui;

import com.vct.creditcard.model.CardAccount;
import com.vct.creditcard.model.CardTransaction;
import com.vct.creditcard.model.Employee;
import com.vct.creditcard.service.Services;

import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.List;

/** Lets a company see what each of its employees bought with the company card. */
final class EmployeeHistoryDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private final transient Services services;
    private final transient CardAccount company;
    private final JComboBox<String> employeeBox = new JComboBox<>();
    private final TransactionTableModel model = new TransactionTableModel();
    private final JLabel summary = new JLabel(" ");

    EmployeeHistoryDialog(JFrame owner, Services services, CardAccount company) {
        super(owner, "Transaction history of employees - " + company.name(), ModalityType.MODELESS);
        this.services = services;
        this.company = company;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        employeeBox.setEditable(true);
        Ui.attempt(this, () -> {
            for (Employee employee : services.accounts().employees(company)) {
                employeeBox.addItem(employee.name());
            }
        });

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filters.add(new JLabel("Name of employee:"));
        filters.add(employeeBox);
        filters.add(Ui.button("See history", this::load));

        JTable table = new JTable(model);
        table.setFillsViewportHeight(true);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(720, 240));

        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.add(filters, BorderLayout.NORTH);
        content.add(scroll, BorderLayout.CENTER);
        content.add(summary, BorderLayout.SOUTH);
        setContentPane(Ui.padded(content));

        if (employeeBox.getItemCount() > 0) {
            load();
        }
        pack();
        setLocationRelativeTo(owner);
    }

    private void load() {
        Object selected = employeeBox.getEditor().getItem();
        String name = selected == null ? "" : selected.toString();
        Ui.attempt(this, () -> {
            List<CardTransaction> rows = services.transactions().employeeHistory(company, name);
            model.setRows(rows);
            summary.setText(rows.isEmpty() ? "No purchases by " + name.strip() + "." : rows.size() + " purchases");
        });
    }
}
