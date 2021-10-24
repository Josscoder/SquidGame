package jossc.squidgame.state;

import cn.nukkit.plugin.PluginBase;
import jossc.game.state.GameState;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

public class EndGame extends GameState {

  public EndGame(PluginBase plugin) {
    super(plugin);
  }

  @NotNull
  @Override
  public Duration getDuration() {
    return Duration.ZERO;
  }

  @Override
  protected void onStart() {

  }

  @Override
  public void onUpdate() {

  }

  @Override
  protected void onEnd() {

  }
}
