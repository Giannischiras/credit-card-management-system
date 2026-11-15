package com.vct.creditcard.ui;

import com.vct.creditcard.model.CardTransaction;
import com.vct.creditcard.model.Money;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/** Shows a list of transactions in a {@link javax.swing.JTable}. */
final class TransactionTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;
    private static final String[] COLUMNS = {"Date", "Customer", "Merchant", "Employee", "Type", "Amount"};

    private transient List<CardTransaction> rows = new ArrayList<>();

    void setRows(List<CardTransaction> transactions) {
        rows = new ArrayList<>(transactions);
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    @Override
    public Object getValueAt(int row, int column) {
        CardTransaction t = rows.get(row);
        return switch (column) {
            case 0 -> t.date();
            case 1 -> t.customerName();
            case 2 -> t.merchantName();
            case 3 -> t.employeeName() == null ? "" : t.employeeName();
            case 4 -> t.type().label();
            default -> Money.format(t.amount());
        };
    }
}
