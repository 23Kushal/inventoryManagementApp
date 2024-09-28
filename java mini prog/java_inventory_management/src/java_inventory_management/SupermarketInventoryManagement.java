package java_inventory_management;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SupermarketInventoryManagement extends JFrame {
    private static final long serialVersionUID = 1L;

    private static final String DB_URL = "jdbc:mysql://localhost:3306/supermarket";
    private static final String USER = "root";
    private static final String PASS = "";

    private static final String ADMIN_PASSWORD = "admin123";

    public SupermarketInventoryManagement() {
        setTitle("Supermarket Inventory Management System");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(3, 1, 10, 10));

        JButton addItemButton = new JButton("Add Item");
        JButton billingButton = new JButton("Billing");
        JButton viewInventoryButton = new JButton("View Inventory");

        addItemButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                authenticateAndExecute(() -> openAddItemDialog());
            }
        });

        billingButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                authenticateAndExecute(() -> openBillingDialog());
            }
        });

        viewInventoryButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                authenticateAndExecute(() -> openViewInventoryDialog());
            }
        });

        panel.add(addItemButton);
        panel.add(billingButton);
        panel.add(viewInventoryButton);

        add(panel);
    }

    private void authenticateAndExecute(Runnable action) {
        String password = JOptionPane.showInputDialog(this, "Enter Password:", "Authentication", JOptionPane.PLAIN_MESSAGE);
        if (ADMIN_PASSWORD.equals(password)) {
            action.run();
        } else {
            JOptionPane.showMessageDialog(this, "Invalid Password", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openAddItemDialog() {
        JDialog dialog = new JDialog(this, "Add Item", true);
        dialog.setSize(300, 200);
        dialog.setLayout(new GridLayout(5, 2, 10, 10));
        dialog.setLocationRelativeTo(this);

        JLabel itemCodeLabel = new JLabel("Item Code:");
        JTextField itemCodeField = new JTextField();
        JLabel itemNameLabel = new JLabel("Item Name:");
        JTextField itemNameField = new JTextField();
        JLabel priceLabel = new JLabel("Price:");
        JTextField priceField = new JTextField();
        JLabel quantityLabel = new JLabel("Quantity:");
        JTextField quantityField = new JTextField();
        JButton addButton = new JButton("Add");

        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int itemCode = Integer.parseInt(itemCodeField.getText());
                String itemName = itemNameField.getText();
                double price = Double.parseDouble(priceField.getText());
                int quantity = Integer.parseInt(quantityField.getText());

                try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
                     PreparedStatement stmt = conn.prepareStatement("INSERT INTO inventory (item_code, item_name, price, quantity) VALUES (?, ?, ?, ?)")) {
                    stmt.setInt(1, itemCode);
                    stmt.setString(2, itemName);
                    stmt.setDouble(3, price);
                    stmt.setInt(4, quantity);
                    stmt.executeUpdate();
                    JOptionPane.showMessageDialog(dialog, "Item Added Successfully", "Success", JOptionPane.INFORMATION_MESSAGE);
                    dialog.dispose();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(dialog, "Error Adding Item", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        dialog.add(itemCodeLabel);
        dialog.add(itemCodeField);
        dialog.add(itemNameLabel);
        dialog.add(itemNameField);
        dialog.add(priceLabel);
        dialog.add(priceField);
        dialog.add(quantityLabel);
        dialog.add(quantityField);
        dialog.add(new JLabel());
        dialog.add(addButton);

        dialog.setVisible(true);
    }

    private void openBillingDialog() {
        JDialog dialog = new JDialog(this, "Billing", true);
        dialog.setSize(300, 200);
        dialog.setLayout(new GridLayout(4, 2, 10, 10));
        dialog.setLocationRelativeTo(this);

        JLabel itemCodeLabel = new JLabel("Item Code:");
        JTextField itemCodeField = new JTextField();
        JLabel quantityLabel = new JLabel("Quantity:");
        JTextField quantityField = new JTextField();
        JButton addItemButton = new JButton("Add Item");

        List<BilledItem> billedItems = new ArrayList<>();

        addItemButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int itemCode = Integer.parseInt(itemCodeField.getText());
                int quantity = Integer.parseInt(quantityField.getText());

                try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
                     PreparedStatement stmt = conn.prepareStatement("SELECT item_name, price, quantity FROM inventory WHERE item_code = ?")) {
                    stmt.setInt(1, itemCode);
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        String itemName = rs.getString("item_name");
                        double price = rs.getDouble("price");
                        int availableQuantity = rs.getInt("quantity");

                        if (quantity <= availableQuantity) {
                            double totalCost = price * quantity;
                            billedItems.add(new BilledItem(itemCode, itemName, quantity, price, totalCost));
                            JOptionPane.showMessageDialog(dialog, "Item Added to Bill", "Success", JOptionPane.INFORMATION_MESSAGE);

                            try (PreparedStatement updateStmt = conn.prepareStatement("UPDATE inventory SET quantity = quantity - ? WHERE item_code = ?")) {
                                updateStmt.setInt(1, quantity);
                                updateStmt.setInt(2, itemCode);
                                updateStmt.executeUpdate();
                            }
                        } else {
                            JOptionPane.showMessageDialog(dialog, "Insufficient Quantity", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    } else {
                        JOptionPane.showMessageDialog(dialog, "Item Not Found", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(dialog, "Error Adding Item to Bill", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        JButton generateBillButton = new JButton("Generate Bill");
        generateBillButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openBillWindow(billedItems);
                dialog.dispose();
            }
        });

        dialog.add(itemCodeLabel);
        dialog.add(itemCodeField);
        dialog.add(quantityLabel);
        dialog.add(quantityField);
        dialog.add(addItemButton);
        dialog.add(new JLabel());
        dialog.add(generateBillButton);

        dialog.setVisible(true);
    }

    private void openBillWindow(List<BilledItem> billedItems) {
        JDialog billDialog = new JDialog(this, "Bill", true);
        billDialog.setSize(400, 300);
        billDialog.setLayout(new BorderLayout());
        billDialog.setLocationRelativeTo(this);

        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        billDialog.add(scrollPane, BorderLayout.CENTER);

        StringBuilder sb = new StringBuilder();
        sb.append("Item Code\tItem Name\tQuantity\tPrice\tTotal Cost\n");
        double subtotal = 0;
        for (BilledItem item : billedItems) {
            sb.append(item.getItemCode()).append("\t")
                    .append(item.getItemName()).append("\t")
                    .append(item.getQuantity()).append("\t")
                    .append(item.getPrice()).append("\t")
                    .append(item.getTotalCost()).append("\n");
            subtotal += item.getTotalCost();
        }
        sb.append("\nSubtotal: $").append(subtotal);
        textArea.setText(sb.toString());

        billDialog.setVisible(true);
    }

    private void openViewInventoryDialog() {
        JDialog dialog = new JDialog(this, "View Inventory", true);
        dialog.setSize(400, 300);
        dialog.setLayout(new BorderLayout());
        dialog.setLocationRelativeTo(this);

        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        dialog.add(scrollPane, BorderLayout.CENTER);

        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM inventory")) {
            StringBuilder sb = new StringBuilder();
            sb.append("Item Code\tItem Name\tPrice\tQuantity\n");
            while (rs.next()) {
                sb.append(rs.getInt("item_code")).append("\t")
                        .append(rs.getString("item_name")).append("\t")
                        .append(rs.getDouble("price")).append("\t")
                        .append(rs.getInt("quantity")).append("\n");
            }
            textArea.setText(sb.toString());
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(dialog, "Error Retrieving Inventory", "Error", JOptionPane.ERROR_MESSAGE);
        }

        dialog.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SupermarketInventoryManagement app = new SupermarketInventoryManagement();
            app.setVisible(true);
        });
    }

    private static class BilledItem {
        private final int itemCode;
        private final String itemName;
        private final int quantity;
        private final double price;
        private final double totalCost;

        public BilledItem(int itemCode, String itemName, int quantity, double price, double totalCost) {
            this.itemCode = itemCode;
            this.itemName = itemName;
            this.quantity = quantity;
            this.price = price;
            this.totalCost = totalCost;
        }

        public int getItemCode() {
            return itemCode;
        }

        public String getItemName() {
            return itemName;
        }

        public int getQuantity() {
            return quantity;
        }

        public double getPrice() {
            return price;
        }

        public double getTotalCost() {
            return totalCost;
        }
    }
}
