package com.vct.creditcard;

import com.formdev.flatlaf.FlatLightLaf;
import com.vct.creditcard.db.Database;
import com.vct.creditcard.db.DemoData;
import com.vct.creditcard.db.SchemaInitializer;
import com.vct.creditcard.service.BankingException;
import com.vct.creditcard.service.Services;
import com.vct.creditcard.ui.MainMenu;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.sql.SQLException;

/** Entry point: prepares the database and opens the main menu. */
public final class App {

    private App() {
    }

    public static void main(String[] args) {
        FlatLightLaf.setup();
        Database db = Database.fromEnvironment();
        try {
            SchemaInitializer.createSchema(db);
            if (DemoData.seedIfEmpty(db)) {
                System.out.println("Created a new database with demo data at " + db.url());
            }
        } catch (SQLException | BankingException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "Could not connect to MySQL at " + db.url() + "\n" + e.getMessage()
                            + "\n\nIs MySQL running? Set CCC_DB_HOST, CCC_DB_USER and CCC_DB_PASSWORD "
                            + "if your server uses different settings.",
                    "Database error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        Services services = Services.create(db);
        SwingUtilities.invokeLater(() -> new MainMenu(services).setVisible(true));
    }
}
