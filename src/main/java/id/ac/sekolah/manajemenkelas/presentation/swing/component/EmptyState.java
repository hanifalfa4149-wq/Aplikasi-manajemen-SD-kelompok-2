package id.ac.sekolah.manajemenkelas.presentation.swing.component;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import id.ac.sekolah.manajemenkelas.presentation.swing.theme.BotanicalIcons;
import id.ac.sekolah.manajemenkelas.presentation.swing.theme.BotanicalTheme;

/**
 * Komponen keadaan kosong yang rapi saat tabel atau daftar tidak memiliki data.
 */
public class EmptyState extends JPanel {

    private final JLabel iconLabel;
    private final JLabel titleLabel;
    private final JLabel descriptionLabel;
    private final JPanel actionSlot;

    public EmptyState(String title, String description) {
        setOpaque(false);
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(4, 16, 8, 16);

        iconLabel = new JLabel(BotanicalIcons.get(BotanicalIcons.Type.DETAIL, 36, BotanicalTheme.SAGE));
        add(iconLabel, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(2, 16, 4, 16);
        titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(BotanicalTheme.FONT_SUBTITLE);
        titleLabel.setForeground(BotanicalTheme.TEXT_PRIMARY);
        add(titleLabel, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, 16, 12, 16);
        descriptionLabel = new JLabel(
                "<html><div style='text-align:center;width:360px'>" + escapeHtml(description) + "</div></html>",
                SwingConstants.CENTER);
        descriptionLabel.setFont(BotanicalTheme.FONT_BODY);
        descriptionLabel.setForeground(BotanicalTheme.TEXT_SECONDARY);
        add(descriptionLabel, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(4, 16, 4, 16);
        actionSlot = new JPanel();
        actionSlot.setOpaque(false);
        add(actionSlot, gbc);
    }

    public void setMessage(String title, String description) {
        titleLabel.setText(title);
        descriptionLabel.setText(
                "<html><div style='text-align:center;width:360px'>" + escapeHtml(description) + "</div></html>");
    }

    public void setActionButton(PrimaryButton button) {
        actionSlot.removeAll();
        if (button != null) {
            actionSlot.add(button);
        }
        revalidate();
        repaint();
    }

    private static String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}

