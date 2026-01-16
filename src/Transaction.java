public class Transaction {
    double amount;
    String category;

    public Transaction(double amount, String category) {
        this.amount = amount;
        this.category = category;
    }

    public boolean isExpense() {
        return amount < 0;
    }

    public String toString() {
        return (amount >= 0 ? "+" : "") + amount + " - " + category;
    }
}