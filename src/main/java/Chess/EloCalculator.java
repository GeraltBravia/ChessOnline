package Chess;

/**
 * Class tính toán điểm Elo theo công thức chuẩn của FIDE
 */
public class EloCalculator {

    // Hằng số K-factor (32 cho người chơi dưới 2100 Elo)
    private static final int K_FACTOR = 32;

    /**
     * Tính toán thay đổi Elo cho người thắng và người thua
     * @param winnerElo Điểm Elo của người thắng
     * @param loserElo Điểm Elo của người thua
     * @return Mảng chứa [thay đổi Elo người thắng, thay đổi Elo người thua]
     */
    public static int[] calculateEloChange(int winnerElo, int loserElo) {
        // Tính xác suất thắng của người thắng
        double expectedWinner = 1.0 / (1.0 + Math.pow(10, (loserElo - winnerElo) / 400.0));

        // Tính xác suất thắng của người thua
        double expectedLoser = 1.0 / (1.0 + Math.pow(10, (winnerElo - loserElo) / 400.0));

        // Tính thay đổi Elo
        int winnerChange = (int) Math.round(K_FACTOR * (1.0 - expectedWinner));
        int loserChange = (int) Math.round(K_FACTOR * (0.0 - expectedLoser));

        return new int[]{winnerChange, loserChange};
    }

    /**
     * Tính toán thay đổi Elo khi hòa
     * @param player1Elo Điểm Elo của người chơi 1
     * @param player2Elo Điểm Elo của người chơi 2
     * @return Mảng chứa [thay đổi Elo người chơi 1, thay đổi Elo người chơi 2]
     */
    public static int[] calculateDrawEloChange(int player1Elo, int player2Elo) {
        // Tính xác suất thắng của người chơi 1
        double expectedPlayer1 = 1.0 / (1.0 + Math.pow(10, (player2Elo - player1Elo) / 400.0));

        // Tính xác suất thắng của người chơi 2
        double expectedPlayer2 = 1.0 / (1.0 + Math.pow(10, (player1Elo - player2Elo) / 400.0));

        // Tính thay đổi Elo (hòa = 0.5 điểm)
        int player1Change = (int) Math.round(K_FACTOR * (0.5 - expectedPlayer1));
        int player2Change = (int) Math.round(K_FACTOR * (0.5 - expectedPlayer2));

        return new int[]{player1Change, player2Change};
    }
}