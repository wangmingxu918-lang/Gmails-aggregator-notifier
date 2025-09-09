package main;

import model.*;
import javax.swing.*;
import java.util.List;
import java.awt.BorderLayout;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;


public class Main {

    public static void main(String[] args) {

        // --- 1️⃣ Login UI ---
        JTextField emailField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        String[] providers = {"Gmail", "Outlook", "Yahoo", "Custom"};
        JComboBox<String> providerBox = new JComboBox<>(providers);

        Object[] message = {
                "Email:", emailField,
                "App Password:", passwordField,
                "Provider:", providerBox
        };

        int option = JOptionPane.showConfirmDialog(null, message, "Login to your email",
                JOptionPane.OK_CANCEL_OPTION);
        if (option != JOptionPane.OK_OPTION) {
            System.exit(0);
        }

        String email = emailField.getText();
        String password = new String(passwordField.getPassword());
        String provider = (String) providerBox.getSelectedItem();

        // Determine IMAP host
        String host;
        switch (provider) {
            case "Gmail" -> host = "imap.gmail.com";
            case "Outlook" -> host = "outlook.office365.com";
            case "Yahoo" -> host = "imap.mail.yahoo.com";
            case "Custom" -> host = JOptionPane.showInputDialog("Enter IMAP server:");
            default -> host = "imap.gmail.com";
        }

        // --- 2️⃣ Initialize EmailClient ---
        EmailClient client = EmailClient.getInstance();

        MailFolder inbox = new MailFolder("Inbox");
        MailFolder work = new MailFolder("Work");
        MailFolder school = new MailFolder("School");

        client.addRule(new SortingRule("project", work));
        client.addRule(new SortingRule("school", school));

        // --- 3️⃣ Create GUI ---
        JFrame frame = new JFrame("Email Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 600);
        frame.setLayout(new BorderLayout());

        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Folders");
        DefaultTreeModel treeModel = new DefaultTreeModel(rootNode);
        JTree folderTree = new JTree(treeModel);
        JScrollPane scrollPane = new JScrollPane(folderTree);
        frame.add(scrollPane, BorderLayout.CENTER);

        // Add default folders
        for (MailFolder folder : List.of(inbox, work, school)) {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(folder.getName());
            treeModel.insertNodeInto(node, rootNode, rootNode.getChildCount());
        }

        // "+" button to add folders
        JButton addButton = new JButton("+");
        addButton.addActionListener(e -> {
            String folderName = JOptionPane.showInputDialog(frame, "Enter new folder name:");
            if (folderName != null && !folderName.isBlank()) {
                MailFolder newFolder = new MailFolder(folderName);
                client.addRule(new SortingRule("", newFolder));
                DefaultMutableTreeNode node = new DefaultMutableTreeNode(folderName);
                treeModel.insertNodeInto(node, rootNode, rootNode.getChildCount());
                treeModel.reload();
            }
        });
        frame.add(addButton, BorderLayout.NORTH);

        // --- 4️⃣ Observer for notifications ---
        client.addObserver((emailObj, folder) -> {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(frame,
                    "New email '" + emailObj.getSubject() + "' in folder '" + folder.getName() + "'");
            });
        });


        frame.setVisible(true);

        // --- 5️⃣ Connect and fetch emails ---
        new Thread(() -> {
            try {
                client.connect(host, email, password);
                client.fetchEmails(); // triggers observer notifications
            } catch (Exception e) {
                e.printStackTrace();  // <-- Add this line to see the full stack trace
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(frame,
                        "Login failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE));
                System.exit(1);
            }
        }).start();
    }
}
