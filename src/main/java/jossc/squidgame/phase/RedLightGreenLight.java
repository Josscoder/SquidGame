package jossc.squidgame.phase;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.player.PlayerMoveEvent;
import java.time.Duration;
import jossc.game.Game;
import jossc.game.utils.math.MathUtils;

public class RedLightGreenLight extends Microgame {

  private boolean canWalk = true;

  public RedLightGreenLight(Game game) {
    super(game, Duration.ofMinutes(5));
  }

  @Override
  public String getName() {
    return "Red light & Green light";
  }

  @Override
  public String getInstruction() {
    return "To win you must reached the goal";
  }

  @Override
  public void onGameStart() {
    singDoll();
  }

  @Override
  public void onGameUpdate() {}

  @Override
  public void onGameEnd() {
    schedule(() -> getRoundLosers().forEach(this::lose), 40);
  }

  @EventHandler
  public void onMove(PlayerMoveEvent event) {
    Player player = event.getPlayer();

    if (!roundWinners.contains(player) && !canWalk && !isReadyToEnd()) {
      lose(player);
    }
  }

  private void singDoll() {
    int time = MathUtils.nextInt(2, 5);

    broadcastMessage("&l&a» &r&fGreen light!");
    broadcastTitle("&aGreen Light!", "&fYou can move.");
    broadcastSound("mob.ghast.moan");
    canWalk = true;

    schedule(
      () -> {
        broadcastMessage("&l&c» &r&fRed light!");
        broadcastTitle("&cRed Light!", "&fYou can not move.");
        broadcastSound("mob.blaze.hit");

        schedule(
          () -> {
            canWalk = false;
            int waitTime = MathUtils.nextInt(2, 5);

            schedule((this::singDoll), waitTime * 20);
          },
          20
        );
      },
      time * 20
    );
  }
}
