import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.util.ArrayList;

public class TeenBudgetTracker {

    static ArrayList<Month> months = new ArrayList<>();
    static Month currentMonth;
    static DefaultListModel<Transaction> listModel = new DefaultListModel<>();
    static final String DATA_FILE = System.getProperty("user.home") + "/TeenBudgetTrackerData.csv";

    public static void main(String[] args) {
        // Load data first
        loadData();

        JFrame frame = new JFrame("Teen Budget Tracker");
        frame.setSize(1100, 500);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        JTextField amountField = new JTextField(5);
        JTextField categoryField = new JTextField(7);

        JButton addIncome = new JButton("Add Income");
        JButton addExpense = new JButton("Add Expense");
        JButton deleteTransaction = new JButton("Delete Transaction");
        JButton newMonth = new JButton("New Month");
        JButton deleteMonth = new JButton("Delete Month");
        JComboBox<String> monthBox = new JComboBox<>();

        // Populate monthBox if there is existing data
        for (Month m : months) monthBox.addItem(m.name);
        if (!months.isEmpty()) {
            currentMonth = months.get(0);
            monthBox.setSelectedIndex(0);
            for (Transaction t : currentMonth.transactions) listModel.addElement(t);
        }

        JPanel topPanel = new JPanel();
        topPanel.add(new JLabel("Amount"));
        topPanel.add(amountField);
        topPanel.add(new JLabel("Category"));
        topPanel.add(categoryField);
        topPanel.add(addIncome);
        topPanel.add(addExpense);
        topPanel.add(deleteTransaction);
        topPanel.add(newMonth);
        topPanel.add(deleteMonth);
        topPanel.add(monthBox);

        frame.add(topPanel, BorderLayout.NORTH);

        // Transactions list
        JList<Transaction> transactionList = new JList<>(listModel);
        Color darkGreen = new Color(0, 100, 0);
        Color darkRed = new Color(139, 0, 0);

        transactionList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel label = new JLabel(value.toString(), SwingConstants.CENTER);
            label.setOpaque(true);
            label.setForeground(value.isExpense() ? darkRed : darkGreen);
            label.setBackground(isSelected ? new Color(200, 200, 200) : Color.WHITE);
            label.setFont(new Font("SansSerif", Font.PLAIN, 16));
            return label;
        });

        JScrollPane transactionScroll = new JScrollPane(transactionList);
        transactionScroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                "Transactions",
                TitledBorder.CENTER,
                TitledBorder.TOP,
                new Font("SansSerif", Font.BOLD, 18)
        ));
        transactionScroll.setPreferredSize(new Dimension(0, 250));

        // Statistics box (centered)
        JTextPane statsArea = new JTextPane();
        statsArea.setEditable(false);
        statsArea.setFont(new Font("SansSerif", Font.BOLD, 16));
        statsArea.setOpaque(false);
        statsArea.setHighlighter(null);

        StyledDocument doc = statsArea.getStyledDocument();
        SimpleAttributeSet center = new SimpleAttributeSet();
        StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);

        JScrollPane statsScroll = new JScrollPane(statsArea);
        statsScroll.getViewport().setBackground(Color.WHITE);
        statsScroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                "Statistics",
                TitledBorder.CENTER,
                TitledBorder.TOP,
                new Font("SansSerif", Font.BOLD, 18)
        ));
        statsScroll.setPreferredSize(new Dimension(0, 250));

        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        centerPanel.add(transactionScroll);
        centerPanel.add(statsScroll);

        frame.add(centerPanel, BorderLayout.CENTER);

        // Stats update logic
        Runnable updateStats = () -> {
            if (currentMonth == null) {
                statsArea.setText("");
                return;
            }
            statsArea.setText(
                    "Starting Balance: " + currentMonth.startingBalance + "\n" +
                            "Total Income: " + currentMonth.totalIncome() + "\n" +
                            "Total Expense: " + -currentMonth.totalExpense() + "\n" +
                            "Net Balance: " + (currentMonth.totalIncome() + currentMonth.totalExpense()) + "\n\n" +
                            "Average Spent: " + currentMonth.averageSpent() + "\n" +
                            "Highest Spend: " + currentMonth.highestSpendCategory() + "\n" +
                            "Lowest Spend: " + currentMonth.lowestSpendCategory()
            );

            doc.setParagraphAttributes(0, doc.getLength(), center, false);
        };

        // Button actions
        addIncome.addActionListener(e -> {
            if (currentMonth == null) {
                JOptionPane.showMessageDialog(frame, "You must create a month first!", "No Month Selected", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                double amount = Double.parseDouble(amountField.getText());
                Transaction t = new Transaction(amount, categoryField.getText());
                currentMonth.addTransaction(t);
                listModel.addElement(t);
                amountField.setText("");
                categoryField.setText("");
                updateStats.run();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame, "Please enter a valid number for amount.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
            }
        });

        addExpense.addActionListener(e -> {
            if (currentMonth == null) {
                JOptionPane.showMessageDialog(frame, "You must create a month first!", "No Month Selected", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                double amount = -Double.parseDouble(amountField.getText());
                Transaction t = new Transaction(amount, categoryField.getText());
                currentMonth.addTransaction(t);
                listModel.addElement(t);
                amountField.setText("");
                categoryField.setText("");
                updateStats.run();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame, "Please enter a valid number for amount.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
            }
        });

        deleteTransaction.addActionListener(e -> {
            int index = transactionList.getSelectedIndex();
            if (index >= 0) {
                currentMonth.removeTransaction(index);
                listModel.remove(index);
                updateStats.run();
            }
        });

        newMonth.addActionListener(e -> {
            String name = JOptionPane.showInputDialog("Month name");
            if (name == null || name.isEmpty()) return;

            Month m = new Month(name);

            // Carry over leftover balance from previous month
            if (currentMonth != null) {
                double leftover = currentMonth.totalIncome() + currentMonth.totalExpense();
                m.startingBalance = leftover; // store starting balance
                if (leftover != 0) {
                    m.addTransaction(new Transaction(leftover, "Carry-over"));
                }
            }

            months.add(m);
            currentMonth = m;
            monthBox.addItem(name);
            monthBox.setSelectedItem(name);
            listModel.clear();
            updateStats.run();
        });

        deleteMonth.addActionListener(e -> {
            int index = monthBox.getSelectedIndex();
            if (index < 0) return;

            months.remove(index);
            monthBox.removeItemAt(index);

            if (months.isEmpty()) {
                currentMonth = null;
                listModel.clear();
                statsArea.setText("");
            } else {
                currentMonth = months.get(0);
                monthBox.setSelectedIndex(0);
                listModel.clear();
                for (Transaction t : currentMonth.transactions)
                    listModel.addElement(t);
                updateStats.run();
            }
        });

        monthBox.addActionListener(e -> {
            int i = monthBox.getSelectedIndex();
            if (i < 0) return;
            currentMonth = months.get(i);
            listModel.clear();
            for (Transaction t : currentMonth.transactions)
                listModel.addElement(t);
            updateStats.run();
        });

        // Save data when closing
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                saveData();
            }
        });

        frame.setVisible(true);
        updateStats.run();
    }

    // CSV save
    static void saveData() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(DATA_FILE))) {
            for (Month m : months) {
                for (Transaction t : m.transactions) {
                    pw.println(m.name + "," + t.amount + "," + t.category);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // CSV load
    static void loadData() {
        File file = new File(DATA_FILE);
        if (!file.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            Month lastMonth = null;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",", 3);
                if (parts.length < 3) continue;
                String monthName = parts[0];
                double amount = Double.parseDouble(parts[1]);
                String category = parts[2];

                if (lastMonth == null || !lastMonth.name.equals(monthName)) {
                    lastMonth = new Month(monthName);
                    months.add(lastMonth);
                }
                lastMonth.addTransaction(new Transaction(amount, category));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
