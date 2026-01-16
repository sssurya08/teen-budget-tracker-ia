import java.util.ArrayList;
public class Month {
    String name;
    ArrayList<Transaction> transactions;
    double startingBalance;

    public Month(String name) {
        this.name = name;
        transactions = new ArrayList<>();
        startingBalance = 0;
    }

    public void addTransaction(Transaction t) {
        transactions.add(t);
    }

    public void removeTransaction(int index) {
        transactions.remove(index);
    }

    public double totalIncome() {
        double sum = 0;
        for (Transaction t : transactions)
            if (!t.isExpense()) sum += t.amount;
        return sum;
    }

    public double totalExpense() {
        double sum = 0;
        for (Transaction t : transactions)
            if (t.isExpense()) sum += t.amount;
        return sum;
    }

    public double averageSpent() {
        double sum = 0;
        int count = 0;
        for (Transaction t : transactions)
            if (t.isExpense()) {
                sum += -t.amount;
                count++;
            }
        return count == 0 ? 0 : sum / count;
    }

    public String highestSpendCategory() {
        double max = 0;
        String cat = "";
        for (Transaction t : transactions)
            if (t.isExpense() && -t.amount > max) {
                max = -t.amount;
                cat = t.category;
            }
        return cat;
    }

    public String lowestSpendCategory() {
        double min = Double.MAX_VALUE;
        String cat = "";
        for (Transaction t : transactions)
            if (t.isExpense() && -t.amount < min) {
                min = -t.amount;
                cat = t.category;
            }
        return cat;
    }
}