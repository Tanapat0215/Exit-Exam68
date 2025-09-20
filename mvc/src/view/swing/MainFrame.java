package view.swing;

import controller.CrowdfundController;
import model.Project;
import model.RewardTier;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public class MainFrame extends JFrame {
    private final CrowdfundController controller;
    private JTextField userField;
    private JButton loginBtn;
    private JTextField searchField;
    private JComboBox<String> categoryCombo;
    private JComboBox<String> sortCombo;
    private JTable table;
    private DefaultTableModel tableModel;
    private JLabel statusBar;

    // Detail
    private JLabel lblName, lblCat, lblTarget, lblDeadline, lblRaised, lblProgress;
    private JProgressBar progressBar;
    private DefaultListModel<String> tierListModel;
    private JList<String> tierList;
    private JButton pledgeBtn, refreshBtn, statsBtn;

    public MainFrame() throws Exception {
        setTitle("Crowdfund MVC (Swing + CSV)");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 680);
        setLocationRelativeTo(null);

        controller = new CrowdfundController(Path.of("data"));
        initUI();
        reloadCategories();
        reloadTable();
    }

    private void initUI() {
        setLayout(new BorderLayout());

        // Top login/search bar
        JPanel top = new JPanel(new BorderLayout(8, 8));
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT));
        userField = new JTextField("alice", 12);
        loginBtn = new JButton("Login");
        loginBtn.addActionListener(e -> {
            String u = userField.getText().trim();
            if (controller.login(u)) {
                status("Logged in as " + controller.getCurrentUser().username);
            } else {
                status("Login failed");
            }
        });
        left.add(new JLabel("Username:"));
        left.add(userField);
        left.add(loginBtn);
        top.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        searchField = new JTextField(18);
        sortCombo = new JComboBox<>(new String[]{"Default","Ending Soon","Raised (High→Low)","Newest Id"});
        categoryCombo = new JComboBox<>(new String[]{"All"});
        JButton searchBtn = new JButton("Search");
        searchBtn.addActionListener(e -> reloadTable());
        right.add(new JLabel("Search:"));
        right.add(searchField);
        right.add(new JLabel("Category:"));
        right.add(categoryCombo);
        right.add(new JLabel("Sort:"));
        right.add(sortCombo);
        right.add(searchBtn);
        top.add(right, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);

        // Split: table (left) & detail (right)
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setResizeWeight(0.55);
        split.setDividerLocation(0.55);

        // Table
        tableModel = new DefaultTableModel(new Object[]{"ID","Name","Category","Target","Deadline","Raised","Progress"}, 0){
            @Override public boolean isCellEditable(int r, int c){ return false; }
        };
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) showSelectedDetail();
            }
        });
        split.setLeftComponent(new JScrollPane(table));

        // Detail panel
        JPanel detail = new JPanel();
        detail.setLayout(new BoxLayout(detail, BoxLayout.Y_AXIS));
        detail.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        lblName = new JLabel("Name: -");
        lblCat = new JLabel("Category: -");
        lblTarget = new JLabel("Target: -");
        lblDeadline = new JLabel("Deadline: -");
        lblRaised = new JLabel("Raised: -");
        lblProgress = new JLabel("Progress: -");
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        detail.add(lblName);
        detail.add(lblCat);
        detail.add(lblTarget);
        detail.add(lblDeadline);
        detail.add(lblRaised);
        detail.add(lblProgress);
        detail.add(progressBar);
        detail.add(Box.createVerticalStrut(10));

        tierListModel = new DefaultListModel<>();
        tierList = new JList<>(tierListModel);
        detail.add(new JLabel("Reward Tiers:"));
        detail.add(new JScrollPane(tierList));

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pledgeBtn = new JButton("Pledge...");
        pledgeBtn.addActionListener(e -> onPledge());
        refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> reloadTable());
        statsBtn = new JButton("Stats");
        statsBtn.addActionListener(e -> onStats());
        btns.add(statsBtn);
        btns.add(refreshBtn);
        btns.add(pledgeBtn);
        detail.add(btns);

        split.setRightComponent(detail);

        add(split, BorderLayout.CENTER);

        statusBar = new JLabel("Ready");
        add(statusBar, BorderLayout.SOUTH);
    }

    private void reloadCategories() {
        categoryCombo.removeAllItems();
        categoryCombo.addItem("All");
        Set<String> cats = controller.listCategories();
        for (String c : cats) categoryCombo.addItem(c);
    }

    private void reloadTable() {
        tableModel.setRowCount(0);
        String kw = searchField.getText();
        String cat = (String) categoryCombo.getSelectedItem();
        String sort = (String) sortCombo.getSelectedItem();
        List<Project> list = controller.search(kw, cat, sort);
        for (Project p : list) {
            int progress = (int)Math.round(p.progress()*100);
            tableModel.addRow(new Object[]{
                p.projectId, p.name, p.category, p.target, p.deadline, p.raised, progress + "%"
            });
        }
        if (tableModel.getRowCount() > 0) table.setRowSelectionInterval(0, 0);
    }

    private void showSelectedDetail() {
        int r = table.getSelectedRow();
        if (r < 0) return;
        String id = table.getValueAt(r, 0).toString();
        Project p = controller.getProject(id);
        if (p == null) return;
        lblName.setText("Name: " + p.name);
        lblCat.setText("Category: " + p.category);
        lblTarget.setText("Target: " + p.target);
        lblDeadline.setText("Deadline: " + p.deadline + (p.deadline.isAfter(LocalDate.now()) ? " (valid)" : " (expired)"));
        lblRaised.setText("Raised: " + p.raised);
        int progress = (int)Math.round(p.progress()*100);
        lblProgress.setText("Progress: " + progress + "%");
        progressBar.setValue(progress);

        tierListModel.clear();
        for (RewardTier t : controller.getTiers(id)) {
            tierListModel.addElement(t.tierName + " | min " + t.minAmount + " | quota " + t.quota);
        }
    }

    private void onPledge() {
        int r = table.getSelectedRow();
        if (r < 0) { status("Select a project first."); return; }
        String id = table.getValueAt(r, 0).toString();
        String amountStr = JOptionPane.showInputDialog(this, "Amount:");
        if (amountStr == null) return;
        String tier = JOptionPane.showInputDialog(this, "Tier name (blank = none):", "");
        try {
            long amount = Long.parseLong(amountStr.trim());
            String msg = controller.pledge(id, amount, tier == null ? "" : tier.trim());
            status(msg);
            reloadTable();
        } catch (NumberFormatException ex) {
            status("Invalid amount.");
        } catch (IOException ex) {
            status("I/O error: " + ex.getMessage());
        }
    }

    private void onStats() {
        long success = controller.getSuccessCount();
        long rejected = controller.getRejectedCount();
        JOptionPane.showMessageDialog(this, "Successful pledges: " + success + "\nRejected pledges: " + rejected,
                "Summary Stats", JOptionPane.INFORMATION_MESSAGE);
    }

    private void status(String s) { statusBar.setText(s); }
}
