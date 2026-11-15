package com.vct.creditcard.ui;

import com.vct.creditcard.model.CardAccount;
import com.vct.creditcard.model.CardTransaction;
import com.vct.creditcard.model.Employee;
import com.vct.creditcard.model.Money;
import com.vct.creditcard.service.BankingException;
import com.vct.creditcard.service.Services;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Buy something from a merchant, or return a product. */
final class TransactionDialog extends JDialog {

    private static final long serialVersionUID = 1L;
    private static final String NO_EMPLOYEE = "(none - the CEO)";

    private final transient Services services;
    private final transient CardAccount account;
    private final CustomerDashboard dashboard;
    private final JComboBox<String> merchantBox = new JComboBox<>();
    private final JTextField amountField = new JTextField(16);
    private final JTextField dateField = new JTextField(LocalDate.now().toString(), 16);
    private final JComboBox<String> employeeBox = new JComboBox<>();

    TransactionDialog(CustomerDashboard dashboard, Services services, CardAccount account) {
        super(dashboard, "New transaction", true);
        this.dashboard = dashboard;
        this.services = services;
        this.account = account;

        merchantBox.setEditable(true);
        Ui.attempt(this, () -> services.merchants().names().forEach(merchantBox::addItem));

        List<String> labels = new ArrayList<>(List.of("Merchant:", "Amount (€):", "Date (YYYY-MM-DD):"));
        List<JComponent> fields = new ArrayList<>(List.of(merchantBox, amountField, dateField));
        if (account.isCompany()) {
            employeeBox.addItem(NO_EMPLOYEE);
            Ui.attempt(this, () -> {
                for (Employee employee : services.accounts().employees(account)) {
                    employeeBox.addItem(employee.name());
                }
            });
            labels.add("Bought by employee:");
            fields.add(employeeBox);
        }

        JButton buy = Ui.button("Buy", this::buy);
        JButton giveBack = Ui.button("Return product", this::giveBack);
        giveBack.setForeground(new Color(0xC6, 0x28, 0x28));
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.add(giveBack);
        buttons.add(buy);

        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.add(Ui.title("Transactions"), BorderLayout.NORTH);
        content.add(Ui.form(labels.toArray(String[]::new), fields.toArray(JComponent[]::new)), BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);
        setContentPane(Ui.padded(content));
        getRootPane().setDefaultButton(buy);
        pack();
        setLocationRelativeTo(dashboard);
    }

    private void buy() {
        Ui.attempt(this, () -> {
            CardTransaction purchase = services.transactions().purchase(
                    account, merchant(), Money.parse(amountField.getText()), date(), employee());
            finish("Purchase of " + Money.format(purchase.amount()) + " at " + purchase.merchantName() + " was successful.");
        });
    }

    private void giveBack() {
        Ui.attempt(this, () -> {
            BigDecimal amount = Money.parse(amountField.getText());
            CardTransaction refund = services.transactions().refund(account, merchant(), amount, date());
            finish(Money.format(refund.amount()) + " was returned to your card.");
        });
    }

    private void finish(String message) {
        Ui.showInfo(this, message);
        dashboard.refresh();
        dispose();
    }

    private String merchant() {
        Object selected = merchantBox.getEditor().getItem();
        return selected == null ? "" : selected.toString();
    }

    private LocalDate date() throws BankingException {
        LocalDate date = Ui.parseOptionalDate(dateField.getText());
        if (date == null) {
            throw new BankingException("Please enter the date of the transaction.");
        }
        return date;
    }

    private String employee() {
        Object selected = employeeBox.getSelectedItem();
        return selected == null || NO_EMPLOYEE.equals(selected) ? null : selected.toString();
    }
}
