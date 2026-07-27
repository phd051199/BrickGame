package brickgame;

/** Validates every vendored common block map without any LCD/SVG renderer. */
public final class CommonMapSmokeTest {
    private CommonMapSmokeTest() {
    }

    public static void main(String[] args) throws Exception {
        byte[] allOn = new byte[512];
        for (int i = 0; i < allOn.length; i++) {
            allOn[i] = (byte) 0xFF;
        }
        short[] board = new short[20];
        byte[] preview = new byte[4];

        for (int i = 0; i < MachineProfile.ALL.length; i++) {
            MachineProfile profile = MachineProfile.ALL[i];
            CommonLcdMap map = CommonLcdMap.load(profile.mapPath());
            int mapped = map.decode(allOn, true, board, preview);
            int boardCells = 0;
            for (int row = 0; row < board.length; row++) {
                int bits = board[row] & 0xFFFF;
                if ((bits & ~0x03FF) != 0) {
                    throw new AssertionError(profile.id + ": board overflow");
                }
                for (int column = 0; column < 10; column++) {
                    if ((bits & (1 << column)) != 0) {
                        boardCells++;
                    }
                }
            }
            int previewCells = 0;
            for (int row = 0; row < preview.length; row++) {
                int bits = preview[row] & 255;
                if ((bits & ~15) != 0) {
                    throw new AssertionError(profile.id + ": preview overflow");
                }
                for (int column = 0; column < 4; column++) {
                    if ((bits & (1 << column)) != 0) {
                        previewCells++;
                    }
                }
            }
            if (mapped < 80 || boardCells < 70 || boardCells > 200
                    || previewCells > 16) {
                throw new AssertionError(profile.id + ": invalid common map counts "
                        + mapped + "/" + boardCells + "/" + previewCells);
            }
            map.decode(allOn, false, board, preview);
            for (int row = 0; row < board.length; row++) {
                if (board[row] != 0) {
                    throw new AssertionError(profile.id + ": disabled display not blank");
                }
            }
        }
        System.out.println("All common block map tests passed");
    }
}
