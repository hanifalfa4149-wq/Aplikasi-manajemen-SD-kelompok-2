package id.ac.sekolah.manajemenkelas.presentation.swing.component;

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import id.ac.sekolah.manajemenkelas.presentation.swing.theme.BotanicalIcons;
import id.ac.sekolah.manajemenkelas.presentation.swing.theme.BotanicalTheme;

/**
 * Kontainer CardLayout seragam untuk state LOADING, EMPTY, ERROR, dan CONTENT
 * pada setiap layar CRUD maupun laporan.
 */
public class StateDeckPanel extends JPanel {

    private static final String CARD_CONTENT = "CONTENT";
    private static final String CARD_LOADING = "LOADING";
    private static final String CARD_EMPTY = "EMPTY";
    private static final String CARD_ERROR = "ERROR";

    private final CardLayout cardLayout = new CardLayout();
    private final LoadingIndicator loadingIndicator = new LoadingIndicator();
    private final EmptyState emptyState;
    private final JLabel errorTitle = new JLabel("Gagal memuat data", SwingConstants.CENTER);
    private final JLabel errorDetail = new JLabel("", SwingConstants.CENTER);
    private final SecondaryButton retryButton = new SecondaryButton("Coba Lagi");

    public StateDeckPanel(Component contentComponent, String emptyTitle, String emptyDesc) {
        setOpaque(false);
        setLayout(cardLayout);

        add(contentComponent, CARD_CONTENT);

        JPanel loadingWrap = new JPanel(new GridBagLayout());
        loadingWrap.setOpaque(false);
        loadingWrap.add(loadingIndicator);
        add(loadingWrap, CARD_LOADING);

        emptyState = new EmptyState(emptyTitle, emptyDesc);
        add(emptyState, CARD_EMPTY);

        add(buildErrorPanel(), CARD_ERROR);
        showContent();
    }

    private JPanel buildErrorPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(4, 16, 8, 16);

        panel.add(new JLabel(BotanicalIcons.get(BotanicalIcons.Type.WARNING, 36, BotanicalTheme.DANGER)), gbc);

        gbc.gridy++;
        errorTitle.setFont(BotanicalTheme.FONT_SUBTITLE);
        errorTitle.setForeground(BotanicalTheme.DANGER.darker());
        panel.add(errorTitle, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, 16, 12, 16);
        errorDetail.setFont(BotanicalTheme.FONT_BODY);
        errorDetail.setForeground(BotanicalTheme.TEXT_SECONDARY);
        panel.add(errorDetail, gbc);

        gbc.gridy++;
        panel.add(retryButton, gbc);
        return panel;
    }

    public void setRetryAction(Runnable retryAction) {
        for (var listener : retryButton.getActionListeners()) {
            retryButton.removeActionListener(listener);
        }
        if (retryAction != null) {
            retryButton.addActionListener(e -> retryAction.run());
            retryButton.setVisible(true);
        } else {
            retryButton.setVisible(false);
        }
    }

    public EmptyState getEmptyState() {
        return emptyState;
    }

    public void showLoading(String message) {
        loadingIndicator.start(message);
        cardLayout.show(this, CARD_LOADING);
    }

    public void showEmpty(String title, String description) {
        loadingIndicator.stop();
        emptyState.setMessage(title, description);
        cardLayout.show(this, CARD_EMPTY);
    }

    public void showEmpty() {
        loadingIndicator.stop();
        cardLayout.show(this, CARD_EMPTY);
    }

    public void showError(String message) {
        loadingIndicator.stop();
        errorDetail.setText("<html><div style='text-align:center;width:380px'>"
                + (message == null ? "Terjadi kendala saat mengakses data." : message)
                + "</div></html>");
        cardLayout.show(this, CARD_ERROR);
    }

    public void showContent() {
        loadingIndicator.stop();
        cardLayout.show(this, CARD_CONTENT);
    }
}

