package brickgame;

/** Integer-only layout calculated from the actual Nokia canvas size. */
final class LayoutMetrics {

    final int screenWidth;
    final int screenHeight;
    final int caseX;
    final int caseY;
    final int caseWidth;
    final int caseHeight;
    final int boardX;
    final int boardY;
    final int boardCell;
    final int boardWidth;
    final int boardHeight;
    final int panelX;
    final int panelY;
    final int panelWidth;
    final int panelHeight;
    final int panelCenter;
    final boolean widePanel;
    final int previewCell;
    final boolean landscape320;
    final int statusX;
    final int statusWidth;
    final int statusCenter;

    LayoutMetrics(int width, int height) {
        screenWidth = width;
        screenHeight = height;

        /* The 320x240 LCD fills the complete canvas; only its content keeps
         * internal padding, so real devices no longer show a dark outer rim. */
        if (width == 320 && height == 240) {
            landscape320 = true;
            caseX = 0;
            caseY = 0;
            caseWidth = width;
            caseHeight = height;

            boardCell = 11;
            boardWidth = boardCell * 10;
            boardHeight = boardCell * 20;
            boardX = 50;
            boardY = 10;

            statusX = 10;
            statusWidth = boardX - statusX - 8;
            statusCenter = statusX + statusWidth / 2;

            panelX = boardX + boardWidth + 8;
            panelY = boardY;
            panelWidth = width - 8 - panelX;
            panelHeight = boardHeight;
            panelCenter = panelX + panelWidth / 2;
            widePanel = true;
            previewCell = 9;
            return;
        }

        landscape320 = false;
        statusX = 0;
        statusWidth = 0;
        statusCenter = 0;

        int margin = width >= 200 ? 4 : 2;
        int gap = width >= 200 ? 6 : 3;
        int minPanel = width >= 220 ? 76 : 56;
        int byHeight = (height - margin * 2 - 8) / 20;
        int byWidth = (width - margin * 2 - gap - minPanel - 8) / 10;
        int cell = byHeight < byWidth ? byHeight : byWidth;
        if (cell < 4) {
            cell = 4;
        }

        boardCell = cell;
        boardWidth = cell * 10;
        boardHeight = cell * 20;
        caseWidth = width - margin * 2;
        caseHeight = boardHeight + 8;
        caseX = margin;
        caseY = (height - caseHeight) / 2;
        boardX = caseX + 4;
        boardY = caseY + 4;
        panelX = boardX + boardWidth + gap;
        panelY = boardY;
        panelWidth = caseX + caseWidth - 4 - panelX;
        panelHeight = boardHeight;
        panelCenter = panelX + panelWidth / 2;
        widePanel = panelWidth >= 128;
        previewCell = widePanel ? 9 : 7;
    }
}
