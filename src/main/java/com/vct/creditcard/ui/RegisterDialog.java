package com.vct.creditcard.ui;

import com.vct.creditcard.model.CardAccount;
import com.vct.creditcard.model.Merchant;
import com.vct.creditcard.model.Money;
import com.vct.creditcard.service.MerchantService;
import com.vct.creditcard.service.Services;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.math.BigDecimal;

/** Opens a new card account or merchant account. */
final class RegisterDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private final transient Services services;
    private final Role role;
    private final JTextField nameField = new JTextField(18);
    private final JTextField creditLimitField = new JTextField(18);

    RegisterDialog(JFrame owner, Services services, Role role) {
        super(owner, "Register as " + role.label().toLowerCase(), true);
        this.services = services;
        this.role = role;

        JComponent[] fields;
        String[] labels;
        if (role == Role.MERCHANT) {
            String rate = MerchantService.DEFAULT_COMMISSION_RATE.movePointRight(2).stripTrailingZeros().toPlainString();
            labels = new String[] {"Name:", "Commission:"};
            fields = new JComponent[] {nameField, new JLabel(rate + "% of every sale is paid to VCT")};
        } else {
            labels = new String[] {role == Role.COMPANY ? "Company name:" : "Full name:", "Credit limit (€):"};
            fields = new JComponent[] {nameField, creditLimitField};
        }

        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.add(Ui.title("Register"), BorderLayout.NORTH);
        content.add(Ui.form(labels, fields), BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.add(Ui.button("Cancel", this::dispose));
        buttons.add(Ui.button("Submit", this::submit));
        content.add(buttons, BorderLayout.SOUTH);

        setContentPane(Ui.padded(content));
        getRootPane().setDefaultButton((javax.swing.JButton) buttons.getComponent(1));
        pack();
        setLocationRelativeTo(owner);
    }

    private void submit() {
        Ui.attempt(this, () -> {
            if (role == Role.MERCHANT) {
                Merchant merchant = services.merchants().register(nameField.getText());
                Ui.showInfo(this, "Registration successful!\nYour merchant account number is "
                        + merchant.accountNumber() + ".");
            } else {
                BigDecimal limit = Money.parse(creditLimitField.getText());
                CardAccount account = services.accounts().register(role.accountType(), nameField.getText(), limit);
                Ui.showInfo(this, "Registration successful!\nAccount number: " + account.accountNumber()
                        + "\nExpiration date: " + account.expirationDate()
                        + "\nCredit limit: " + Money.format(account.creditLimit()));
            }
            dispose();
        });
    }
}
