package com.vct.creditcard.ui;

import com.vct.creditcard.model.Merchant;
import com.vct.creditcard.model.Money;
import com.vct.creditcard.service.Services;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;

/** Account overview for a merchant: profit, commission owed to VCT and sales history. */
final class MerchantDashboard extends JFrame {

    private static final long serialVersionUID = 1L;
    private static final String ACCOUNT_NUMBER = "Account number:";
    private static final String COMMISSION = "Commission to VCT:";
    private static final String PROFIT = "Total profit:";
    private static final String DEBT = "Commission owed:";

    private final transient Services services;
    private transient Merchant merchant;
    private final JLabel welcome = Ui.title("");
    private final InfoPanel info = new InfoPanel(ACCOUNT_NUMBER, COMMISSION, PROFIT, DEBT);

    MerchantDashboard(Services services, Merchant merchant) {
        super("VCT - " + merchant.name());
        this.services = services;
        this.merchant = merchant;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel buttons = new JPanel(new GridLayout(0, 2, 10, 10));
        buttons.add(Ui.button("Pay commission", this::payDebt));
        buttons.add(Ui.button("Sales history", () -> new HistoryWindow(this, "Sales history - " + this.merchant.name(),
                (from, to) -> services.transactions().merchantHistory(this.merchant, from, to)).setVisible(true)));
        JButton delete = Ui.button("Delete account", this::deleteAccount);
        delete.setForeground(new Color(0xC6, 0x28, 0x28));
        buttons.add(delete);

        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.add(welcome, BorderLayout.NORTH);
        content.add(info, BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);
        setContentPane(Ui.padded(content));

        show(merchant);
        pack();
        setLocationRelativeTo(null);
    }

    private void show(Merchant current) {
        merchant = current;
        welcome.setText("Welcome, " + current.name());
        info.set(ACCOUNT_NUMBER, current.accountNumber());
        info.set(COMMISSION, current.commissionRate().movePointRight(2).stripTrailingZeros().toPlainString() + "% per sale");
        info.set(PROFIT, Money.format(current.totalProfit()));
        info.set(DEBT, Money.format(current.debt()));
    }

    private void payDebt() {
        String input = JOptionPane.showInputDialog(this,
                "You owe VCT " + Money.format(merchant.debt()) + " in commissions.\nAmount you want to pay (€):",
                "Pay commission", JOptionPane.QUESTION_MESSAGE);
        if (input == null) {
            return;
        }
        Ui.attempt(this, () -> {
            show(services.merchants().payDebt(merchant, Money.parse(input)));
            Ui.showInfo(this, "Payment received. Thank you!");
        });
    }

    private void deleteAccount() {
        int answer = JOptionPane.showConfirmDialog(this,
                "Delete the merchant account of " + merchant.name() + "? This cannot be undone.",
                "Delete account", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        Ui.attempt(this, () -> {
            services.merchants().delete(merchant);
            Ui.showInfo(this, "Account deleted successfully.");
            dispose();
        });
    }
}
