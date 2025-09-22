package main;

import model.*;

import javax.mail.Folder;
import javax.mail.Message;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // --- 1️⃣ Login UI ---
            JTextField emailField = new JTextField();
            JPasswordField passwordField = new JPasswordField();

            Object[] message = {"Email:", emailField, "App Password:", passwordField};
            int option = JOptionPane.showConfirmDialog(null, message, "Login to Gmail",
                    JOptionPane.OK_CANCEL_OPTION);
            if (option != JOptionPane.OK_OPTION) System.exit(0);

            String email = emailField.getText();
            String password = new String(passwordField.getPassword());
            String host = "imap.gmail.com";

            // --- 2️⃣ Initialize EmailClient ---
            EmailClient client = EmailClient.getInstance();

            // --- 3️⃣ Create GUI ---
            JFrame frame = new JFrame("Gmail Client");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1000, 600);
            frame.setLayout(new BorderLayout());

            // Folder tree
            DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Folders");
            DefaultTreeModel treeModel = new DefaultTreeModel(rootNode);
            JTree folderTree = new JTree(treeModel);
            JScrollPane folderScroll = new JScrollPane(folderTree);

            // Email list
            DefaultListModel<String> emailListModel = new DefaultListModel<>();
            JList<String> emailList = new JList<>(emailListModel);
            JScrollPane emailScroll = new JScrollPane(emailList);

            // Email content
            JTextArea emailContentArea = new JTextArea();
            emailContentArea.setEditable(false);
            JScrollPane contentScroll = new JScrollPane(emailContentArea);

            // Split panes
            JSplitPane rightSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, emailScroll, contentScroll);
            rightSplit.setDividerLocation(300);
            JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, folderScroll, rightSplit);
            mainSplit.setDividerLocation(200);
            frame.add(mainSplit, BorderLayout.CENTER);

            // "+" button to add folder
            JButton addButton = new JButton("+");
            addButton.addActionListener(e -> {
                String folderName = JOptionPane.showInputDialog(frame, "Enter new folder name:");
                String keyword = JOptionPane.showInputDialog(frame, "Enter keyword:");
                if (folderName != null && !folderName.isBlank()) {
                    MailFolder newFolder = new MailFolder(folderName);
                    client.addFolder(newFolder);
                    client.addRule(new SortingRule(keyword, newFolder));

                    DefaultMutableTreeNode node = new DefaultMutableTreeNode(newFolder) {
                        @Override
                        public boolean isLeaf() { return false; }
                        @Override
                        public String toString() { return ((MailFolder) getUserObject()).getName(); }
                    };
                    treeModel.insertNodeInto(node, rootNode, rootNode.getChildCount());
                    treeModel.reload();

                    // re-apply rules immediately
                    client.applyRulesToAllEmails();
                }
            });
            frame.add(addButton, BorderLayout.NORTH);

            // Create modal dialog
            JDialog loadingDialog = new JDialog(frame, "Loading Emails", true);
            loadingDialog.setLayout(new BorderLayout());
            loadingDialog.setSize(400, 100);
            loadingDialog.setLocationRelativeTo(frame);

            JLabel loadingLabel = new JLabel("Reading data from Gmail... Please wait.", JLabel.CENTER);
            loadingLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            loadingDialog.add(loadingLabel, BorderLayout.NORTH);

            JProgressBar progressBar = new JProgressBar(0, 100);
            progressBar.setStringPainted(true);
            loadingDialog.add(progressBar, BorderLayout.CENTER);

            // Disable folder tree initially
            folderTree.setEnabled(false);


            // --- 4️⃣ Show GUI ---
            frame.setVisible(true);

            // --- 5️⃣ Load folders into tree ---
            for (MailFolder folder : client.getFolders()) {
                DefaultMutableTreeNode node = new DefaultMutableTreeNode(folder) {
                    @Override public boolean isLeaf() { return false; }
                    @Override public String toString() { return ((MailFolder) getUserObject()).getName(); }
                };
                treeModel.insertNodeInto(node, rootNode, rootNode.getChildCount());
            }
            treeModel.reload();

            // --- 6️⃣ Folder selection listener ---
            folderTree.addTreeSelectionListener(e -> {
                DefaultMutableTreeNode selectedNode =
                        (DefaultMutableTreeNode) folderTree.getLastSelectedPathComponent();
                if (selectedNode == null) return;

                if (selectedNode.getUserObject() instanceof MailFolder folder) {
                    // Use EmailIterator and batch updates
                    SwingUtilities.invokeLater(() -> {
                        emailListModel.clear();
                        EmailIterator it = new EmailIterator(folder);
                        List<String> batch = new ArrayList<>();
                        while (it.hasNext()) {
                            Email mail = (Email) it.next();
                            batch.add(mail.getSubject() + " - " + mail.getSender());
                        }
                        batch.forEach(emailListModel::addElement);
                    });
                }
            });


            // --- 7️⃣ Email selection listener ---
            emailList.addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) {
                    int index = emailList.getSelectedIndex();
                    if (index >= 0) {
                        DefaultMutableTreeNode selectedNode =
                                (DefaultMutableTreeNode) folderTree.getLastSelectedPathComponent();
                        if (selectedNode != null && selectedNode.getUserObject() instanceof MailFolder folder) {
                            Email mail = folder.getEmails().get(index);
                            emailContentArea.setText(
                                    "From: " + mail.getSender() + "\n" +
                                            "Subject: " + mail.getSubject() + "\n\n" +
                                            mail.getBody()
                            );
                        }
                    }
                }
            });

            // --- 8️⃣ Observer for new emails ---
            client.addObserver((emailObj, folder) -> SwingUtilities.invokeLater(() -> {
                DefaultMutableTreeNode selectedNode =
                        (DefaultMutableTreeNode) folderTree.getLastSelectedPathComponent();
                if (selectedNode != null && selectedNode.getUserObject() == folder) {
                    emailListModel.addElement(emailObj.getSubject() + " - " + emailObj.getSender());
                }
                JOptionPane.showMessageDialog(frame,
                        "New email from " + emailObj.getSender(),
                        "New Email",
                        JOptionPane.INFORMATION_MESSAGE);
            }));

            // --- 9️⃣ Fetch emails in background ---
            SwingWorker<Void, Integer> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() {
                    try {
                        client.connect(host, email, password);
                        Folder imapInbox = client.getStore().getFolder("INBOX");
                        imapInbox.open(Folder.READ_ONLY);

                        int totalMessages = imapInbox.getMessageCount();
                        Message[] messages = imapInbox.getMessages();

                        for (int i = 0; i < messages.length; i++) {
                            Message msg = messages[i];
                            String body = client.getTextFromMessage(msg);
                            Email emailObj = new Email(msg.getSubject(), msg.getFrom()[0].toString(), body);

                            // Instead of setUniqueId, just get it when needed
                            String uid = emailObj.getUniqueId();
                            client.getInboxFolder().addEmail(emailObj);
                            client.getKnownEmails().add(uid);

                            // Apply rules immediately
                            client.applyRules(emailObj);

                            // Update progress
                            int progress = (int) ((i + 1) * 100.0 / totalMessages);
                            publish(progress);
                        }


                        imapInbox.close(false);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return null;
                }

                @Override
                protected void process(List<Integer> chunks) {
                    // Update progress bar with the last value
                    int latestProgress = chunks.get(chunks.size() - 1);
                    progressBar.setValue(latestProgress);
                }

                @Override
                protected void done() {
                    loadingDialog.dispose();
                    folderTree.setEnabled(true);

            // Optionally, load first folder emails into list
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) folderTree.getLastSelectedPathComponent();
            if (selectedNode != null && selectedNode.getUserObject() instanceof MailFolder folder) {
                emailListModel.clear();
                EmailIterator it = new EmailIterator(folder);
                while (it.hasNext()) {
                    Email mail = (Email) it.next();
                    emailListModel.addElement(mail.getSubject() + " - " + mail.getSender());
                }
            }
        }
    };
    folderTree.setEnabled(false); // disable folder tree while loading
    worker.execute();
    loadingDialog.setVisible(true); // blocks until done()



            // --- 10 Start auto-refresh ---
            new Timer(30_000, ev -> new SwingWorker<List<Email>, Void>() {
            @Override
            protected List<Email> doInBackground() {
                return client.fetchNewEmails(host, email, password);
            }

            @Override
            protected void done() {
                try {
                    List<Email> emails = get();
                    if (!emails.isEmpty()) {
                        // batch GUI update using iterator
                        DefaultMutableTreeNode selectedNode =
                                (DefaultMutableTreeNode) folderTree.getLastSelectedPathComponent();
                        if (selectedNode != null && selectedNode.getUserObject() instanceof MailFolder folder) {
                            List<String> batch = new ArrayList<>();
                            EmailIterator it = new EmailIterator(folder);
                            while (it.hasNext()) {
                                Email mail = (Email) it.next();
                                batch.add(mail.getSubject() + " - " + mail.getSender());
                            }
                            SwingUtilities.invokeLater(() -> {
                                emailListModel.clear();
                                batch.forEach(emailListModel::addElement);
                            });
                        }
                        JOptionPane.showMessageDialog(frame, emails.size() + " new email(s) received!");
                    }
                } catch (Exception ignored) {}
            }
        }).start();

        });
    }
}
