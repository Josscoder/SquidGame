package jossc.squidgame.phase;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.player.PlayerMoveEvent;
import cn.nukkit.item.Item;
import cn.nukkit.level.Position;
import cn.nukkit.math.Vector3;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.ConfigSection;
import cn.nukkit.utils.TextFormat;
import java.time.Duration;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import jossc.squidgame.map.RedLightGreenLightMap;
import lombok.Getter;
import net.josscoder.gameapi.user.User;
import net.josscoder.gameapi.util.MathUtils;
import net.josscoder.gameapi.util.VectorUtils;
import org.citizen.attributes.CitizenSkin;
import org.citizen.entity.Citizen;

public class RedLightGreenLight extends Microgame {

  private boolean canWalk = true;

  @Getter
  private Citizen doll = null;

  public RedLightGreenLight(Duration duration) {
    super(duration);
    generateDoll();
  }

  @Override
  public String getName() {
    return "Red light, Green light";
  }

  @Override
  public String getInstruction() {
    return "To win you must reached the goal";
  }

  @Override
  public void setupMap(Config config) {
    ConfigSection section = config.getSection("maps.greenLightRedLightMap");

    map =
      new RedLightGreenLightMap(
        game,
        section.getString("name"),
        VectorUtils.stringToVector(section.getString("safeSpawn")),
        VectorUtils.stringToVector(section.getString("goalCornerOne")),
        VectorUtils.stringToVector(section.getString("goalCornerTwo")),
        VectorUtils.stringToVector(section.getString("dollPosition"))
      );
  }

  @Override
  public List<String> getScoreboardLines(User user) {
    List<String> lines = super.getScoreboardLines(user);

    lines.add(
      canWalk
        ? "⨂ " + TextFormat.GREEN + "Green Light"
        : "⨀ " + TextFormat.RED + "Red Light"
    );

    return lines;
  }

  @Override
  public void onGameStart() {
    spawnDoll();
    singDoll();
  }

  private void generateDoll() {
    if (!(map instanceof RedLightGreenLightMap)) {
      return;
    }

    Vector3 dollPosition = ((RedLightGreenLightMap) map).getDollPosition();

    if (dollPosition == null) {
      return;
    }

    doll = new Citizen();
    doll.setPosition(
      Position.fromObject(dollPosition.add(0.5, 1, 0.5), map.toLevel())
    );
    doll.setSkin(
      CitizenSkin.from(game.skinDataPathToFile().toPath().resolve("doll.png"))
    );
    doll.setScale(3.5f);
    doll.lookAt(map.getSafeSpawn());

    game.getCitizenLibrary().getCitizenFactory().add(doll);
  }

  private void spawnDoll() {
    if (doll == null) {
      return;
    }

    getOnlinePlayers().forEach(player -> doll.spawnTo(player));
  }

  private void singDoll() {
    int time = MathUtils.nextInt(2, 5);

    Predicate<? super Player> condition = player -> !isRoundWinner(player);

    broadcastMessage("&l&a» &r&fGreen light!", condition);
    broadcastTitle("&aGreen Light!", "&fYou can move.", condition);
    broadcastSound("mob.ghast.moan", condition);

    if (doll != null) {
      doll.lookAt(map.getSafeSpawn());
      doll.setYaw(doll.getYaw() - 180);
      doll.updateCurrentPosition();
    }

    giveGreenWool();

    canWalk = true;

    schedule(
      () -> {
        broadcastMessage("&l&c» &r&fRed light!", condition);
        broadcastTitle("&cRed Light!", "You can not move.", condition);
        broadcastSound("mob.blaze.hit", condition);

        if (doll != null) {
          doll.lookAt(map.getSafeSpawn());
        }

        giveRedWool();

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

  private void giveWool(int meta) {
    getRoundLosers()
      .forEach(
        loser -> {
          for (int i = 0; i <= 8; i++) {
            loser.getInventory().setItem(i, Item.get(Item.WOOL, meta));
          }
          User user = userFactory.get(loser);

          if (user != null) {
            user.updateInventory();
          }
        }
      );
  }

  private void giveGreenWool() {
    giveWool(5);
  }

  private void giveRedWool() {
    giveWool(14);
  }

  @EventHandler
  public void onMove(PlayerMoveEvent event) {
    if (!(map instanceof RedLightGreenLightMap)) {
      return;
    }

    Player player = event.getPlayer();

    if (
      !isRoundWinner(player) &&
      !canWalk &&
      !isReadyToEnd() &&
      !player.isSpectator()
    ) {
      lose(player, true);

      return;
    }

    if (
      ((RedLightGreenLightMap) map).isTheGoal(player) && !isRoundWinner(player)
    ) {
      win(player);
    }
  }

  @Override
  public void onGameUpdate() {}

  @Override
  public void onGameEnd() {
    despairDoll();
  }

  private void despairDoll() {
    getOnlinePlayers()
      .stream()
      .filter(player -> doll.getViewers().contains(player))
      .collect(Collectors.toList())
      .forEach(player -> doll.despairFrom(player));
  }
}
