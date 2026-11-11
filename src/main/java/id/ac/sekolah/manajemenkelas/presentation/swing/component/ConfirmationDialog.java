package id.ac.sekolah.manajemenkelas.presentation.swing.component;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.KeyEvent;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import id.ac.sekolah.manajemenkelas.presentation.swing.theme.BotanicalIcons;
import id.ac.sekolah.manajemenkelas.presentation.swing.theme.BotanicalTheme;

/**
 * Dialog konfirmasi modal bergaya Claymorphism untuk aksi destruktif atau penting (logout, nonaktif).
 */
public class ConfirmationDialog extends JDialog {

    private boolean confirmed = false;

    private ConfirmationDialog(
            Window owner,
            String title,
            String message,
            String confirmLabel,
            boolean destructive) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(BotanicalTheme.SOFT_IVORY);
        root.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        ClayCard card = new ClayCard(new BorderLayout(14, 14));
        JLabel iconLabel = new JLabel(BotanicalIcons.get(
                destructive ? BotanicalIcons.Type.WARNING : BotanicalIcons.Type.DETAIL,
                28,
                destructive ? BotanicalTheme.DANGER : BotanicalTheme.EVERGREEN));
        card.add(iconLabel, BorderLayout.WEST);

        JPanel textPanel = new JPanel(new BorderLayout(0, 6));
        textPanel.setOpaque(false);
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(BotanicalTheme.FONT_SUBTITLE);
        titleLabel.setForeground(BotanicalTheme.TEXT_PRIMARY);
        JLabel bodyLabel = new JLabel("<html><body style='width:320px'>" + escapeHtml(message) + "</body></html>");
        bodyLabel.setFont(BotanicalTheme.FONT_BODY);
        bodyLabel.setForeground(BotanicalTheme.TEXT_SECONDARY);
        textPanel.add(titleLabel, BorderLayout.NORTH);
        textPanel.add(bodyLabel, BorderLayout.CENTER);
        card.add(textPanel, BorderLayout.CENTER);

        root.add(card, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);

        SecondaryButton cancelButton = new SecondaryButton("Batal");
        cancelButton.addActionListener(e -> dispose());

        PrimaryButton confirmButton = destructive
                ? PrimaryButton.danger(confirmLabel)
                : new PrimaryButton(confirmLabel);
        confirmButton.addActionListener(e -> {
            confirmed = true;
            dispose();
        });

        actions.add(cancelButton);
        actions.add(confirmButton);
        root.add(actions, BorderLayout.SOUTH);

        setContentPane(root);
        pack();
        setLocationRelativeTo(owner);

        // Escape untuk menutup dialog
        getRootPane().registerKeyboardAction(
                e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        // Fokus default ke Batal bila destruktif agar aman dari accidental Enter
        getRootPane().setDefaultButton(destructive ? cancelButton : confirmButton);
    }

    public static boolean showConfirm(
            Component parent,
            String title,
            String message,
            String confirmLabel,
            boolean destructive) {
        Window owner = parent != null ? SwingUtilities.getWindowAncestor(parent) : null;
        ConfirmationDialog dialog = new ConfirmationDialog(owner, title, message, confirmLabel, destructive);
        dialog.setVisible(true);
        return dialog.confirmed;
    }

    private static String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\n", "<br>");
    }
}

