package id.ac.sekolah.manajemenkelas.presentation.swing.component;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import id.ac.sekolah.manajemenkelas.presentation.swing.theme.BotanicalTheme;

/**
 * Kelas dasar dialog formulir modal Claymorphism dengan dukungan Escape-to-close,
 * Enter-to-save, pesan error inline/banner, dan eksekusi SwingWorker.
 */
public abstract class ModalFormDialog extends JDialog {

    protected final FeedbackBanner feedbackBanner = new FeedbackBanner();
    protected final PrimaryButton saveButton;
    protected final SecondaryButton cancelButton = new SecondaryButton("Batal");
    private final JPanel sectionsPanel = new JPanel();
    private boolean saved = false;

    protected ModalFormDialog(Component ownerComponent, String title, String saveLabel, int width, int height) {
        super(resolveOwner(ownerComponent), title, ModalityType.APPLICATION_MODAL);
        this.saveButton = new PrimaryButton(saveLabel == null ? "Simpan" : saveLabel);
        initBaseLayout(title, width, height);
    }

    private static Window resolveOwner(Component component) {
        return component != null ? SwingUtilities.getWindowAncestor(component) : null;
    }

    private void initBaseLayout(String title, int width, int height) {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(Math.min(width, 460), Math.min(height, 360)));

        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(BotanicalTheme.SOFT_IVORY);
        root.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        // Header judul dialog
        JPanel header = new JPanel(new BorderLayout(0, 8));
        header.setOpaque(false);
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(BotanicalTheme.FONT_TITLE);
        titleLabel.setForeground(BotanicalTheme.TEXT_PRIMARY);
        header.add(titleLabel, BorderLayout.NORTH);
        header.add(feedbackBanner, BorderLayout.SOUTH);
        root.add(header, BorderLayout.NORTH);

        sectionsPanel.setOpaque(false);
        sectionsPanel.setLayout(new BoxLayout(sectionsPanel, BoxLayout.Y_AXIS));

        JScrollPane scroll = new JScrollPane(sectionsPanel);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        root.add(scroll, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout(12, 0));
        footer.setOpaque(false);
        JLabel requiredHint = new JLabel("* Kolom wajib diisi");
        requiredHint.setFont(BotanicalTheme.FONT_SMALL);
        requiredHint.setForeground(BotanicalTheme.TEXT_SECONDARY);
        footer.add(requiredHint, BorderLayout.WEST);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        cancelButton.addActionListener(e -> dispose());
        saveButton.addActionListener(e -> onSubmit());
        buttons.add(cancelButton);
        buttons.add(saveButton);
        footer.add(buttons, BorderLayout.EAST);
        root.add(footer, BorderLayout.SOUTH);

        setContentPane(root);
        setSize(width, height);
        setLocationRelativeTo(getOwner());

        getRootPane().registerKeyboardAction(
                e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().setDefaultButton(saveButton);
    }

    /**
     * Menambahkan kelompok field logis dalam ClayCard terpisah agar form panjang tetap rapi.
     */
    protected void addSectionCard(String sectionTitle, JComponent... fieldsInTwoColumns) {
        ClayCard card = new ClayCard(new BorderLayout(0, 10));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Short.MAX_VALUE));

        if (sectionTitle != null && !sectionTitle.isBlank()) {
            JLabel heading = new JLabel(sectionTitle);
            heading.setFont(BotanicalTheme.FONT_SUBTITLE);
            heading.setForeground(BotanicalTheme.EVERGREEN);
            card.add(heading, BorderLayout.NORTH);
        }

        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.5;
        gbc.anchor = GridBagConstraints.NORTHWEST;

        int row = 0;
        int col = 0;
        for (JComponent comp : fieldsInTwoColumns) {
            boolean fullSpan = Boolean.TRUE.equals(comp.getClientProperty("fullSpan"));
            if (fullSpan && col != 0) {
                row++;
                col = 0;
            }
            gbc.gridx = col;
            gbc.gridy = row;
            gbc.gridwidth = fullSpan ? 2 : 1;
            gbc.insets = new Insets(4, col == 0 ? 0 : 8, 4, (col == 1 || fullSpan) ? 0 : 8);
            grid.add(comp, gbc);

            if (fullSpan) {
                row++;
                col = 0;
            } else {
                col++;
                if (col > 1) {
                    col = 0;
                    row++;
                }
            }
        }

        card.add(grid, BorderLayout.CENTER);
        if (sectionsPanel.getComponentCount() > 0) {
            sectionsPanel.add(Box.createVerticalStrut(12));
        }
        sectionsPanel.add(card);
    }

    protected static <T extends JComponent> T fullWidth(T component) {
        component.putClientProperty("fullSpan", Boolean.TRUE);
        return component;
    }

    protected <R> void executeSave(Callable<R> task, Consumer<R> afterSave) {
        feedbackBanner.clear();
        AsyncTaskRunner.run(
                task,
                () -> {
                    saveButton.setEnabled(false);
                    cancelButton.setEnabled(false);
                    saveButton.setText("Menyimpan...");
                },
                result -> {
                    saved = true;
                    if (afterSave != null) {
                        afterSave.accept(result);
                    }
                    dispose();
                },
                error -> {
                    saveButton.setEnabled(true);
                    cancelButton.setEnabled(true);
                    saveButton.setText("Simpan");
                    feedbackBanner.showError(error);
                });
    }

    protected abstract void onSubmit();

    public boolean isSaved() {
        return saved;
    }
}
