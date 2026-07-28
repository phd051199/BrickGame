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

        boolean e72 = width == 320 && height == 240;
        int margin = e72 ? 4 : 3;
        int statusGap = e72 ? 4 : 0;
        int panelGap = width >= 240 ? 6 : 3;
        int minimumStatus = e72 ? 34 : 0;
        int maximumStatus = e72 ? 58 : 0;
        int minimumPanel = e72 ? 100 : 68;
        int initialBoardX = margin + minimumStatus + statusGap;

        int byHeight = (height - margin * 2) / rows;
        int byWidth = (width - initialBoardX - panelGap
                - minimumPanel - margin) / columns;
        int cell = byHeight < byWidth ? byHeight : byWidth;
        if (cell < 4) {
            cell = 4;
        }

        boardCell = cell;
        boardWidth = columns * cell;
        boardHeight = rows * cell;

        if (e72) {
            int available = width - margin * 2 - statusGap - panelGap
                    - boardWidth;
            int expandedStatus = available - minimumPanel;
            if (expandedStatus > maximumStatus) {
                expandedStatus = maximumStatus;
            }
            if (expandedStatus < minimumStatus) {
                expandedStatus = minimumStatus;
            }
            statusX = margin;
            statusWidth = expandedStatus;
            statusCenter = statusX + statusWidth / 2;
            boardX = statusX + statusWidth + statusGap;
        } else {
            statusX = 0;
            statusWidth = 0;
            statusCenter = 0;
            boardX = margin;
        }

        boardY = (height - boardHeight) / 2;
        panelX = boardX + boardWidth + panelGap;
        panelY = e72 ? margin : boardY;
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
