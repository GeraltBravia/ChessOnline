
package Chess;


public abstract class Player {
    protected boolean whiteSide;
    protected boolean humanPlayer;

    public boolean isWhiteSide() {
        return this.whiteSide;
    }

    public boolean isHumanPlayer() {
        return this.humanPlayer;
    }
}
