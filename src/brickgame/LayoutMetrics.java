package brickgame;

final class LayoutMetrics {
    final int screenWidth;
    final int screenHeight;
    final int boardColumns;
    final int boardRows;
    final int boardColumnOffset;
    final int boardRowOffset;
    final int boardX;
    final int boardY;
    final int boardCell;
    final int boardWidth;
    final int boardHeight;
    final int panelX;
    final int panelY;
    final int panelWidth;
    final int panelCenter;
    final int previewCell;
    final int statusX;
    final int statusWidth;
    final int statusCenter;

    LayoutMetrics(MachineProfile profile, int width, int height) {
        this(width, height, profile.boardColumns, profile.boardRows,
                profile.boardColumnOffset, profile.boardRowOffset);
    }

    LayoutMetrics(int width, int height) {
        this(width, height, 10, 20, 0, 0);
    }

    private LayoutMetrics(int width, int height, int columns, int rows,
            int columnOffset, int rowOffset) {
        screenWidth = width;
        screenHeight = height;
        boardColumns = columns;
        boardRows = rows;
        boardColumnOffset = columnOffset;
        boardRowOffset = rowOffset;

        int margin = width == 320 && height == 240 ? 4 : 3;
        int gap = width >= 240 ? 6 : 3;
        int minimumPanel = width == 320 && height == 240 ? 100 : 68;

        if (width == 320 && height == 240) {
            statusX = margin;
            statusWidth = 24;
            statusCenter = statusX + statusWidth / 2;
            boardX = statusX + statusWidth + gap;
        } else {
            statusX = 0;
            statusWidth = 0;
            statusCenter = 0;
            boardX = margin;
        }

        int byHeight = (height - margin * 2) / rows;
        int byWidth = (width - boardX - gap - minimumPanel - margin) / columns;
        int cell = byHeight < byWidth ? byHeight : byWidth;
        if (cell < 4) {
            cell = 4;
        }

        boardCell = cell;
        boardWidth = columns * cell;
        boardHeight = rows * cell;
        boardY = (height - boardHeight) / 2;
        panelX = boardX + boardWidth + gap;
        panelY = boardY;
        panelWidth = width - margin - panelX;
        panelCenter = panelX + panelWidth / 2;

        int preview = (panelWidth - 64) / 4;
        if (preview > 9) {
            preview = 9;
        }
        if (preview < 6) {
            preview = 6;
        }
        previewCell = preview;
    }
}
