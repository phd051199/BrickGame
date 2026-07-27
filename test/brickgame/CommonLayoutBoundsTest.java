package brickgame;

public final class CommonLayoutBoundsTest {
    private CommonLayoutBoundsTest() {
    }

    public static void main(String[] args) {
        for (int i = 0; i < MachineProfile.ALL.length; i++) {
            MachineProfile profile = MachineProfile.ALL[i];
            LayoutMetrics layout = new LayoutMetrics(profile, 320, 240);
            check(layout.boardColumns == profile.boardColumns,
                    profile.id + " columns");
            check(layout.boardRows == profile.boardRows,
                    profile.id + " rows");
            check(layout.boardWidth == layout.boardColumns * layout.boardCell,
                    profile.id + " width");
            check(layout.boardHeight == layout.boardRows * layout.boardCell,
                    profile.id + " height");
            check(layout.boardY >= 0, profile.id + " top");
            check(240 - layout.boardY - layout.boardHeight >= 0,
                    profile.id + " bottom");
            check(Math.abs(layout.boardY
                    - (240 - layout.boardY - layout.boardHeight)) <= 1,
                    profile.id + " vertical center");
            check(layout.boardX > layout.statusX + layout.statusWidth,
                    profile.id + " status overlap");
            check(layout.panelX > layout.boardX + layout.boardWidth,
                    profile.id + " panel overlap");
            check(layout.panelX + layout.panelWidth <= 320,
                    profile.id + " panel overflow");
            check(layout.panelWidth >= 100,
                    profile.id + " panel width");
        }

        check(new LayoutMetrics(MachineProfile.ALL[0], 320, 240).boardCell == 11,
                "20-row cell");
        check(new LayoutMetrics(MachineProfile.ALL[5], 320, 240).boardCell == 14,
                "16-row cell");
        check(new LayoutMetrics(MachineProfile.ALL[2], 320, 240).boardCell == 19,
                "12-row cell");
        check(new LayoutMetrics(MachineProfile.ALL[8], 320, 240).boardCell == 21,
                "11-row cell");
        check(new LayoutMetrics(MachineProfile.ALL[9], 320, 240).boardCell == 16,
                "14-row cell");
        System.out.println("All native-height 320x240 layout tests passed");
    }

    private static void check(boolean value, String name) {
        if (!value) {
            throw new AssertionError(name);
        }
    }
}
