package com.vct.creditcard.ui;

import com.vct.creditcard.model.CardAccount;
import com.vct.creditcard.model.Merchant;
import com.vct.creditcard.service.BankingException;
import com.vct.creditcard.service.Services;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.Optional;

/** Start screen: log in, register, or open one of the reports. */
public final class MainMenu extends JFrame {

    private static final long serialVersionUID = 1L;

    private final transient Services services;

    public MainMenu(Services services) {
        super("VCT Credit Card Company");
        this.services = services;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(0, 18));
        root.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        root.add(header(), BorderLayout.NORTH);
        root.add(actions(), BorderLayout.CENTER);
        setContentPane(root);

        pack();
        setResizable(false);
        setLocationRelativeTo(null);
    }

    private JPanel header() {
        JPanel header = new JPanel(new BorderLayout(18, 0));
        header.add(new JLabel(new CardIcon(170)), BorderLayout.WEST);
        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(Box.createVerticalGlue());
        text.add(Ui.title("VCT Credit Card Company"));
        text.add(Box.createVerticalStrut(6));
        text.add(new JLabel("Card accounts, purchases, refunds and merchant commissions"));
        text.add(Box.createVerticalGlue());
        header.add(text, BorderLayout.CENTER);
        return header;
    }

    private JPanel actions() {
        JPanel columns = new JPanel(new GridLayout(1, 3, 16, 0));
        columns.add(column("Log in as",
                Ui.button("Individual", () -> login(Role.INDIVIDUAL)),
                Ui.button("Company", () -> login(Role.COMPANY)),
                Ui.button("Merchant", () -> login(Role.MERCHANT))));
        columns.add(column("Register as",
                Ui.button("Individual", () -> register(Role.INDIVIDUAL)),
                Ui.button("Company", () -> register(Role.COMPANY)),
                Ui.button("Merchant", () -> register(Role.MERCHANT))));
        columns.add(column("Reports",
                Ui.button("Best customers", () -> new ReportWindow(this, services, ReportWindow.Kind.BEST_CUSTOMERS).setVisible(true)),
                Ui.button("Worst customers", () -> new ReportWindow(this, services, ReportWindow.Kind.WORST_CUSTOMERS).setVisible(true)),
                Ui.button("Merchant of the month", () -> new ReportWindow(this, services, ReportWindow.Kind.MERCHANT_OF_THE_MONTH).setVisible(true))));
        return columns;
    }

    private static JPanel column(String title, JButton... buttons) {
        JPanel column = new JPanel(new GridLayout(0, 1, 0, 10));
        column.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(title),
                BorderFactory.createEmptyBorder(8, 10, 10, 10)));
        for (JButton button : buttons) {
            column.add(button);
        }
        return column;
    }

    private void login(Role role) {
        String name = JOptionPane.showInputDialog(this, role.namePrompt(), "Log in as " + role.label().toLowerCase(),
                JOptionPane.QUESTION_MESSAGE);
        if (name == null || name.isBlank()) {
            return;
        }
        Ui.attempt(this, () -> {
            if (role == Role.MERCHANT) {
                Optional<Merchant> merchant = services.merchants().find(name);
                if (merchant.isEmpty()) {
                    throw new BankingException("\"" + name.strip() + "\" is not a merchant of VCT.");
                }
                new MerchantDashboard(services, merchant.get()).setVisible(true);
            } else {
                Optional<CardAccount> account = services.accounts().find(role.accountType(), name);
                if (account.isEmpty()) {
                    throw new BankingException("\"" + name.strip() + "\" is not a client of our credit card company.");
                }
                new CustomerDashboard(services, account.get()).setVisible(true);
            }
        });
    }

    private void register(Role role) {
        new RegisterDialog(this, services, role).setVisible(true);
    }
}
