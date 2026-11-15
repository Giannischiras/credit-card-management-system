package com.vct.creditcard.ui;

import com.vct.creditcard.model.CardAccount;
import com.vct.creditcard.model.Money;
import com.vct.creditcard.service.Services;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.util.Optional;

/** Account overview for an individual or a company, with all the actions they can take. */
final class CustomerDashboard extends JFrame {

    private static final long serialVersionUID = 1L;
    private static final String ACCOUNT_NUMBER = "Account number:";
    private static final String EXPIRATION = "Expiration date:";
    private static final String CREDIT_LIMIT = "Credit limit:";
    private static final String DEBT = "Debt:";
    private static final String AVAILABLE = "Available credit:";

    private final transient Services services;
    private transient CardAccount account;
    private final JLabel welcome = Ui.title("");
    private final InfoPanel info = new InfoPanel(ACCOUNT_NUMBER, EXPIRATION, CREDIT_LIMIT, DEBT, AVAILABLE);

    CustomerDashboard(Services services, CardAccount account) {
        super("VCT - " + account.name());
        this.services = services;
        this.account = account;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel buttons = new JPanel(new GridLayout(0, 2, 10, 10));
        buttons.add(Ui.button("New transaction", () -> new TransactionDialog(this, services, this.account).setVisible(true)));
        buttons.add(Ui.button("Pay debt", this::payDebt));
        buttons.add(Ui.button("History", this::openHistory));
        if (account.isCompany()) {
            buttons.add(Ui.button("Employee transactions",
                    () -> new EmployeeHistoryDialog(this, services, this.account).setVisible(true)));
        }
        javax.swing.JButton delete = Ui.button("Delete account", this::deleteAccount);
        delete.setForeground(new Color(0xC6, 0x28, 0x28));
        buttons.add(delete);

        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.add(welcome, BorderLayout.NORTH);
        content.add(info, BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);
        setContentPane(Ui.padded(content));

        show(account);
        pack();
        setLocationRelativeTo(null);
    }

    /** Reloads the account from the database after an action changed it. */
    void refresh() {
        Ui.attempt(this, () -> {
            Optional<CardAccount> current = services.accounts().find(account.type(), account.name());
            if (current.isPresent()) {
                show(current.get());
            } else {
                dispose();
            }
        });
    }

    private void show(CardAccount current) {
        account = current;
        welcome.setText(current.isCompany() ? "Welcome, CEO of " + current.name() : "Welcome, " + current.name());
        info.set(ACCOUNT_NUMBER, current.accountNumber());
        info.set(EXPIRATION, current.expirationDate());
        info.set(CREDIT_LIMIT, Money.format(current.creditLimit()));
        info.set(DEBT, Money.format(current.debt()));
        info.set(AVAILABLE, Money.format(current.availableBalance()));
    }

    private void payDebt() {
        String input = JOptionPane.showInputDialog(this,
                "Your debt is " + Money.format(account.debt()) + ".\nAmount you want to repay (€):",
                "Pay debt", JOptionPane.QUESTION_MESSAGE);
        if (input == null) {
            return;
        }
        Ui.attempt(this, () -> {
            show(services.accounts().payDebt(account, Money.parse(input)));
            Ui.showInfo(this, "Payment received. Thank you!");
        });
    }

    private void openHistory() {
        new HistoryWindow(this, "Transaction history - " + account.name(),
                (from, to) -> services.transactions().customerHistory(account, from, to)).setVisible(true);
    }

    private void deleteAccount() {
        int answer = JOptionPane.showConfirmDialog(this,
                "Delete the account of " + account.name() + "? This cannot be undone.",
                "Delete account", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        Ui.attempt(this, () -> {
            services.accounts().delete(account);
            Ui.showInfo(this, "Account deleted successfully.");
            dispose();
        });
    }
}
