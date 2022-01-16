package jossc.squidgame.phase;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.item.EntityFishingHook;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.entity.EntityDamageByChildEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.item.ItemFishingRod;
import cn.nukkit.math.Vector3;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.ConfigSection;
import cn.nukkit.utils.TextFormat;
import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import jossc.squidgame.team.ITeam;
import jossc.squidgame.team.Team;
import net.josscoder.gameapi.Game;
import net.josscoder.gameapi.map.GameMap;
import net.josscoder.gameapi.util.VectorUtils;

public class TugOfWar extends Microgame implements ITeam {

  private boolean canReciveDamage = false;

  public TugOfWar(Game game, Duration duration) {
    super(game, duration);
  }

  @Override
  public String getName() {
    return "Tug Of War";
  }

  @Override
  public String getInstruction() {
    return "Throw players from the other team into the void, remember that the people who don't fall will win!";
  }

  @Override
  protected void onStart() {
    super.onStart();

    getNeutralPlayers()
      .forEach(
        player -> {
          Team sortedTeam = getSortedTeams(true).get(0);

          sortedTeam.add(player);
        }
      );
  }

  @Override
  public void setupMap(Config config) {
    ConfigSection section = config.getSection("maps.tugOfWarMap");

    map =
      new GameMap(
        game,
        section.getString("name"),
        VectorUtils.stringToVector(section.getString("safeSpawn"))
      );

    map.setSpawns(RED, getTeamSpawns(section, RED.toLowerCase()));
    map.setSpawns(BLUE, getTeamSpawns(section, BLUE.toLowerCase()));
  }

  private List<Vector3> getTeamSpawns(ConfigSection section, String id) {
    List<Vector3> spawns = new LinkedList<>();

    for (int i = 1; i <= game.getMaxPlayers(); i++) {
      spawns.add(
        VectorUtils.stringToVector(section.getString(id + ".spawns." + i))
      );
    }

    return spawns;
  }

  @Override
  public void onGameStart() {
    getNeutralUsers()
      .forEach(
        user -> {
          Player player = user.getPlayer();

          for (int i = 0; i <= 8; i++) {
            player.getInventory().setItem(i, new ItemFishingRod());
          }
          user.updateInventory();

          Team team = getTeam(player);

          if (team != null) {
            boolean isRed = team.getId().equalsIgnoreCase(RED);
            String teamColor = "&" + (isRed ? "c" : "9");

            player.sendMessage(
              TextFormat.colorize(
                "&l" +
                teamColor +
                "»&r&f Your team is " +
                teamColor +
                team.getId().toUpperCase()
              )
            );
          }
        }
      );

    canReciveDamage = true;
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  @Override
  public void onDamage(EntityDamageEvent event) {
    Entity entity = event.getEntity();

    if (!(entity instanceof Player) || !canReciveDamage) {
      super.onDamage(event);

      return;
    }

    Player player = (Player) entity;

    if (
      !(
        event instanceof EntityDamageByChildEntityEvent &&
        event.getCause().equals(EntityDamageEvent.DamageCause.PROJECTILE)
      )
    ) {
      if (
        event.getCause().equals(EntityDamageEvent.DamageCause.FALL) &&
        player.getGamemode() != Player.CREATIVE
      ) {
        lose(player);
      } else {
        super.onDamage(event);
      }

      return;
    }

    Entity damager = ((EntityDamageByChildEntityEvent) event).getChild();

    if (damager instanceof EntityFishingHook) {
      Entity shootingEntity =
        ((EntityDamageByChildEntityEvent) event).getDamager();

      if (
        !(shootingEntity instanceof Player) ||
        isTeamMember((Player) shootingEntity, player)
      ) {
        super.onDamage(event);
      }

      return;
    }

    super.onDamage(event);
  }

  @Override
  public boolean isReadyToEnd() {
    return super.isReadyToEnd() || thereIsATeamWithoutMembers();
  }

  @Override
  public void onGameUpdate() {}

  @Override
  public void onGameEnd() {
    canReciveDamage = false;
  }
}
