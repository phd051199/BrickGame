package brickgame;

/** Pixel-width assertions for the fixed Nokia status panels. */
public final class LayoutBoundsTest {

    public static void main(String[] args) {
        BitmapFont font = new BitmapFont();
        BrickNumberFont numbers = new BrickNumberFont();
        int index;
        for (index = 0; index < GameCatalog.COUNT; index++) {
            require(font.textWidth(GameCatalog.name(index)) <= 100,
                "portrait name overflow: " + GameCatalog.name(index));
            require(font.textWidth(GameCatalog.label(index)) <= 100,
                "portrait label overflow: " + GameCatalog.label(index));
            require(font.textWidth(GameCatalog.name(index)) <= 70,
                "compact name overflow: " + GameCatalog.name(index));
        }
        require(font.textWidth("RUNNING") <= 78, "running status box");
        require(font.textWidth("PAUSED") <= 78, "paused status box");
        require(numbers.width(6, 1) <= 54, "six-digit score width");
        require(numbers.width(2, 1) <= 20, "two-digit status width");
        require(font.textWidth("100%") <= 29, "battery percentage width");
        assertLayout(320, 240);
        assertLayout(240, 320);
        System.out.println("Layout bounds test passed");
    }

    private static void assertLayout(int width, int height) {
        LayoutMetrics layout = new LayoutMetrics(width, height);
        require(layout.caseX >= 0 && layout.caseY >= 0,
            "case origin " + width + "x" + height);
        require(layout.caseX + layout.caseWidth <= width,
            "case width " + width + "x" + height);
        require(layout.caseY + layout.caseHeight <= height,
            "case height " + width + "x" + height);
        require(layout.panelX - (layout.boardX + layout.boardWidth) >= 6,
            "board/panel gap " + width + "x" + height);
        require(layout.panelX + layout.panelWidth <= width,
            "panel overflow " + width + "x" + height);
        require(layout.boardCell >= 11,
            "integer board scale too small " + width + "x" + height);
        if (layout.landscape320) {
            require(layout.caseX == 0 && layout.caseY == 0,
                "landscape LCD starts at canvas origin");
            require(layout.caseWidth == width && layout.caseHeight == height,
                "landscape LCD fills canvas");
            require(layout.boardY - 1 >= 8,
                "top content padding");
            require(layout.statusX >= 8,
                "left content padding");
            require(width - (layout.panelX + layout.panelWidth) >= 8,
                "right content padding");
            require(layout.statusWidth >= 26,
                "device status rail width");
            require(layout.panelWidth >= 138,
                "balanced landscape panel width");
            require(184 + BrickNumberFont.DIGIT_HEIGHT < layout.panelHeight,
                "landscape life value bottom padding");
        } else if (layout.widePanel) {
            require(11 + layout.previewCell * 4 + 20 <= 72,
                "wide preview/stats padding " + width + "x" + height);
            require(58 + BrickNumberFont.DIGIT_HEIGHT < 79,
                "wide score/speed spacing " + width + "x" + height);
            require(92 + BrickNumberFont.DIGIT_HEIGHT < 112,
                "wide speed/level spacing " + width + "x" + height);
            require(125 + BrickNumberFont.DIGIT_HEIGHT < 145,
                "wide level/life spacing " + width + "x" + height);
        } else {
            require(61 + layout.previewCell * 4 < 102,
                "narrow preview/score spacing " + width + "x" + height);
            require(115 + BrickNumberFont.DIGIT_HEIGHT < 140,
                "narrow score/stats spacing " + width + "x" + height);
            require(153 + BrickNumberFont.DIGIT_HEIGHT < 177,
                "narrow stats/life spacing " + width + "x" + height);
            require(190 + BrickNumberFont.DIGIT_HEIGHT < 211,
                "narrow life/status spacing " + width + "x" + height);
        }
    }

    private static void require(boolean value, String message) {
        if (!value) {
            throw new IllegalStateException(message);
        }
    }
}
