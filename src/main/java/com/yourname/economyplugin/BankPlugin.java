package com.yourname.economyplugin;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BankPlugin extends JavaPlugin {

    // Enum for account types
    public enum AccountType {
        Asset, Liability, EQUITY, Expense, Revenue
    }

    // General ledger entry class
    public class LedgerEntry {
        private UUID entryId;
        private String accountName;
        private double transactionAmount;
        private LocalDateTime transactionDate;
        private String note;

        public LedgerEntry(String accountName, double transactionAmount, String note) {
            this.entryId = UUID.randomUUID();
            this.accountName = accountName;
            this.transactionAmount = transactionAmount;
            this.transactionDate = LocalDateTime.now();
            this.note = note;
        }

        // Getters
        public UUID getEntryId() { return entryId; }
        public String getAccountName() { return accountName; }
        public double getTransactionAmount() { return transactionAmount; }
        public LocalDateTime getTransactionDate() { return transactionDate; }
        public String getNote() { return note; }

        @Override
        public String toString() {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return String.format("[%s] %s: %.2f - %s",
                    transactionDate.format(formatter),
                    accountName,
                    transactionAmount,
                    note);
        }
    }

    // Account class
    public class Account {
        private String accountName;
        private AccountType accountType;
        private double balance;

        public Account(String accountName, AccountType accountType) {
            this.accountName = accountName;
            this.accountType = accountType;
            this.balance = 0.0;
        }

        // Getters and setters
        public String getAccountName() { return accountName; }
        public AccountType getAccountType() { return accountType; }
        public double getBalance() { return balance; }

        public void updateBalance(double amount) {
            // Expense and Revenue accounts don't maintain balance
            if (accountType != AccountType.Expense && accountType != AccountType.Revenue) {
                this.balance += amount;
            }
        }

        @Override
        public String toString() {
            return String.format("%s (%s): %.2f", accountName, accountType, balance);
        }
    }

    // In-memory storage
    private Map<String, Account> accounts = new HashMap<>();
    private List<LedgerEntry> ledgerEntries = new ArrayList<>();

    @Override
    public void onEnable() {
        getLogger().info("Bank plugin has been enabled!");

        // Initialize default accounts
        initializeAccounts();

        // Register commands
        getCommand("bank").setExecutor(this);
    }

    @Override
    public void onDisable() {
        getLogger().info("Bank plugin has been disabled!");
        // Here we would save data to SQLite if implemented
    }

    private void initializeAccounts() {
        // Create default accounts - using descriptive names as identifiers
        createAccount("Expenses", AccountType.Expense);
        createAccount("Revenue", AccountType.Revenue);
        createAccount("Bank_Reserves", AccountType.Asset); // Main bank account
        createAccount("Inventory", AccountType.Asset); // Physical items value
        createAccount("MrDragon64", AccountType.Liability);
        createAccount("Accounts_Receivable", AccountType.Asset);
        createAccount("Accounts_Payable", AccountType.Liability);

        getLogger().info("Default accounts initialized!");
    }

    private Account createAccount(String accountName, AccountType accountType) {
        Account account = new Account(accountName, accountType);
        accounts.put(accountName, account);
        return account;
    }

    private LedgerEntry recordTransaction(String accountName, double amount, String note) {
        // Verify the account exists
        Account account = accounts.get(accountName);
        if (account == null) {
            return null;
        }

        // Create ledger entry
        LedgerEntry entry = new LedgerEntry(accountName, amount, note);
        ledgerEntries.add(entry);

        // Update account balance
        account.updateBalance(amount);

        return entry;
    }

    private String formatAccountName(String accountName) {
        // Replace underscores with spaces and capitalize words nicely
        return accountName.replace('_', ' ');
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("bank")) {
            if (args.length == 0) {
                sendHelp(sender);
                return true;
            }

            String subCommand = args[0].toLowerCase();

            switch (subCommand) {
                case "accounts":
                    return listAccounts(sender);
                case "ledger":
                    return viewLedger(sender, args);
                case "transaction":
                    return handleTransaction(sender, args);
                case "balance":
                    return checkBalance(sender, args);
                case "account":
                    return getAccountInfo(sender, args);
                case "hello":
                    // Easter egg command for testing
                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        player.sendMessage(ChatColor.GREEN + "Hello " + player.getName() + "!");
                    } else {
                        sender.sendMessage("Hello console user!");
                    }
                    return true;
                default:
                    sendHelp(sender);
                    return true;
            }
        }
        return false;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== Bank Plugin Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/bank accounts" + ChatColor.WHITE + " - List all accounts");
        sender.sendMessage(ChatColor.YELLOW + "/bank ledger [accountName]" + ChatColor.WHITE + " - View general ledger entries");
        sender.sendMessage(ChatColor.YELLOW + "/bank transaction <accountName> <amount> <note>" + ChatColor.WHITE + " - Record a transaction");
        sender.sendMessage(ChatColor.YELLOW + "/bank balance [accountName]" + ChatColor.WHITE + " - Check account balance");
        sender.sendMessage(ChatColor.YELLOW + "/bank account <accountName>" + ChatColor.WHITE + " - Get detailed account info");
        sender.sendMessage(ChatColor.YELLOW + "/bank hello" + ChatColor.WHITE + " - Simple greeting command");
    }

    private boolean listAccounts(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== Bank Accounts ===");

        for (Account account : accounts.values()) {
            String displayName = formatAccountName(account.getAccountName());
            String message = ChatColor.GREEN + account.getAccountName() + ":" +
                           ChatColor.WHITE + " (" +
                           account.getAccountType() + ")";

            if (account.getAccountType() != AccountType.Expense &&
                account.getAccountType() != AccountType.Revenue) {
                message += ChatColor.YELLOW + " Balance: $" + String.format("%.2f", account.getBalance());
            }

            sender.sendMessage(message);
        }

        return true;
    }

    private boolean viewLedger(CommandSender sender, String[] args) {
        sender.sendMessage(ChatColor.GOLD + "=== General Ledger ===");
        // Filter by account if specified
        String accountFilter = null;
        if (args.length >= 2) {
            accountFilter = args[1];
            String upperCaseAccountName = accountFilter.toUpperCase();
            boolean accountFound = false;

            // Check for account in a case-insensitive way
            for (String account : accounts.keySet()) {
                if (account.equalsIgnoreCase(accountFilter)) {
                    accountFilter = account; // Use the actual case from the accounts map
                    accountFound = true;
                    break;
                }
            }

            if (!accountFound) {
                sender.sendMessage(ChatColor.RED + "Account not found: " + accountFilter);
                return true;
            }
            sender.sendMessage(ChatColor.YELLOW + "Filtering for account: " + formatAccountName(accountFilter));
        }

        int count = 0;
        for (LedgerEntry entry : ledgerEntries) {
            if (accountFilter == null || entry.getAccountName().equalsIgnoreCase(accountFilter)) {
                String amountColor = entry.getTransactionAmount() >= 0 ? ChatColor.GREEN.toString() : ChatColor.RED.toString();
                sender.sendMessage(
                    ChatColor.GRAY + entry.getTransactionDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + " " +
                    ChatColor.YELLOW + formatAccountName(entry.getAccountName()) + " " +
                    amountColor + String.format("$%.2f", entry.getTransactionAmount()) + " " +
                    ChatColor.WHITE + entry.getNote()
                );
                count++;
            }
        }

        if (count == 0) {
            sender.sendMessage(ChatColor.YELLOW + "No ledger entries found.");
        }
        return true;
    }
    private boolean handleTransaction(CommandSender sender, String[] args) {
        // Format: /bank transaction <accountName> <amount> <note>
        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "Usage: /bank transaction <accountName> <amount> <note>");
            return true;
        }

        String accountName = args[1].toUpperCase();
        double amount;

        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid amount. Please enter a valid number.");
            return true;
        }

        // Combine remaining args for note
        StringBuilder noteBuilder = new StringBuilder();
        for (int i = 3; i < args.length; i++) {
            if (i > 3) noteBuilder.append(" ");
            noteBuilder.append(args[i]);
        }
        String note = noteBuilder.toString();

        // Check if account exists
        if (!accounts.containsKey(accountName)) {
            sender.sendMessage(ChatColor.RED + "Account not found: " + accountName);
            return true;
        }

        // Record transaction
        LedgerEntry entry = recordTransaction(accountName, amount, note);
        if (entry == null) {
            sender.sendMessage(ChatColor.RED + "Failed to record transaction.");
            return true;
        }

        sender.sendMessage(ChatColor.GREEN + "Transaction recorded successfully:");
        sender.sendMessage(ChatColor.YELLOW + entry.toString());

        // Show updated balance if applicable
        Account account = accounts.get(accountName);
        if (account.getAccountType() != AccountType.Expense &&
            account.getAccountType() != AccountType.Revenue) {
            sender.sendMessage(ChatColor.YELLOW + "New balance: $" +
                              String.format("%.2f", account.getBalance()));
        }

        return true;
    }

    private boolean checkBalance(CommandSender sender, String[] args) {
        // Format: /bank balance [accountName]
        if (args.length >= 2) {
            String accountName = args[1].toUpperCase();

            // Check if account exists
            if (!accounts.containsKey(accountName)) {
                sender.sendMessage(ChatColor.RED + "Account not found: " + accountName);
                return true;
            }

            Account account = accounts.get(accountName);

            if (account.getAccountType() == AccountType.Expense ||
                account.getAccountType() == AccountType.Revenue) {
                sender.sendMessage(ChatColor.YELLOW + formatAccountName(account.getAccountName()) +
                                  " (" + account.getAccountType() + ") does not maintain a balance.");
                return true;
            }

            sender.sendMessage(ChatColor.GOLD + "=== Account Balance ===");
            sender.sendMessage(ChatColor.GREEN + formatAccountName(account.getAccountName()) + ": $" +
                              String.format("%.2f", account.getBalance()));

        } else {
            // Show all balances
            sender.sendMessage(ChatColor.GOLD + "=== Account Balances ===");

            for (Account account : accounts.values()) {
                if (account.getAccountType() != AccountType.Expense &&
                    account.getAccountType() != AccountType.Revenue) {
                    sender.sendMessage(ChatColor.GREEN + formatAccountName(account.getAccountName()) +
                                      ChatColor.WHITE + " (" + account.getAccountType() + "): " +
                                      ChatColor.YELLOW + "$" + String.format("%.2f", account.getBalance()));
                }
            }
        }

        return true;
    }

    private boolean getAccountInfo(CommandSender sender, String[] args) {
        // Format: /bank account <accountName>
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /bank account <accountName>");
            return true;
        }

        String accountName = args[1];
        String upperCaseAccountName = accountName.toUpperCase();
        boolean accountFound = false;

        // Check for account in a case-insensitive way
        for (String account : accounts.keySet()) {
            if (account.equalsIgnoreCase(accountName)) {
                accountName = account; // Use the actual case from the accounts map
                accountFound = true;
                break;
            }
        }

        if (!accountFound) {
            sender.sendMessage(ChatColor.RED + "Account not found: " + accountName);
            return true;
        }

        Account account = accounts.get(accountName);

        sender.sendMessage(ChatColor.GOLD + "=== Account Information ===");
        sender.sendMessage(ChatColor.GREEN + "Name: " + ChatColor.WHITE + formatAccountName(account.getAccountName()));
        sender.sendMessage(ChatColor.GREEN + "Type: " + ChatColor.WHITE + account.getAccountType());

        if (account.getAccountType() != AccountType.Expense &&
            account.getAccountType() != AccountType.Revenue) {
            sender.sendMessage(ChatColor.GREEN + "Balance: " + ChatColor.YELLOW + "$" +
                              String.format("%.2f", account.getBalance()));
        } else {
            sender.sendMessage(ChatColor.YELLOW + "This account type does not maintain a balance.");
        }

        // Show recent transactions
        sender.sendMessage(ChatColor.GREEN + "Recent Transactions:");

        int count = 0;
        for (int i = ledgerEntries.size() - 1; i >= 0 && count < 5; i--) {
            LedgerEntry entry = ledgerEntries.get(i);
            if (entry.getAccountName().equals(accountName)) {
                String amountColor = entry.getTransactionAmount() >= 0 ? ChatColor.GREEN.toString() : ChatColor.RED.toString();
                sender.sendMessage(
                    ChatColor.GRAY + entry.getTransactionDate().format(DateTimeFormatter.ofPattern("MM-dd HH:mm")) + " " +
                    amountColor + String.format("$%.2f", entry.getTransactionAmount()) + " " +
                    ChatColor.WHITE + entry.getNote()
                );
                count++;
            }
        }

        if (count == 0) {
            sender.sendMessage(ChatColor.YELLOW + "No transactions found for this account.");
        }

        return true;
    }
}
