package be.RedSwick.skyblock.moderation;

/**
 * État global du serveur (ouvert/fermé).
 * Quand fermé, seuls les FONDATEUR et ADMIN peuvent se connecter.
 */
public class ServerState {

    public static final ServerState INSTANCE = new ServerState();

    private boolean closed = false;

    public boolean isClosed()           { return closed; }
    public void    setClosed(boolean v) { this.closed = v; }
}